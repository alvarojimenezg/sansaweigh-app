package cl.sansaweigh.sansaweighapp.services.impl;

import cl.sansaweigh.sansaweighapp.dto.CreateRegistroPesajeRequest;
import cl.sansaweigh.sansaweighapp.dto.RegistroPesajeResponse;
import cl.sansaweigh.sansaweighapp.dto.UpdateRegistroPesajeRequest;
import cl.sansaweigh.sansaweighapp.entities.CategoriaPeso;
import cl.sansaweigh.sansaweighapp.entities.EstadoPesaje;
import cl.sansaweigh.sansaweighapp.entities.RegistroPesaje;
import cl.sansaweigh.sansaweighapp.exceptions.BusinessRuleException;
import cl.sansaweigh.sansaweighapp.exceptions.RegistroNoEncontradoException;
import cl.sansaweigh.sansaweighapp.repositories.RegistroPesajeRepository;
import cl.sansaweigh.sansaweighapp.services.WeighingRulesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroPesajeServiceImplTest {

    @Mock
    private RegistroPesajeRepository repository;

    @Mock
    private WeighingRulesService rulesService;

    @InjectMocks
    private RegistroPesajeServiceImpl service;

    @Test
    void createRegistro_happyPath_clasificaYPersiste() {
        CreateRegistroPesajeRequest request = new CreateRegistroPesajeRequest(101L, "PKG-1", 10.0);
        when(rulesService.deKgToSansa(10.0)).thenReturn(7.48);
        when(rulesService.clasificarPackage(7.48)).thenReturn(CategoriaPeso.LIVIANO);
        when(repository.save(any(RegistroPesaje.class))).thenAnswer(inv -> {
            RegistroPesaje r = inv.getArgument(0);
            r.setId("generated-id");
            return r;
        });

        RegistroPesajeResponse response = service.createRegistro(request);

        assertThat(response.getId()).isEqualTo("generated-id");
        assertThat(response.getBalanzaId()).isEqualTo(101);
        assertThat(response.getPaqueteId()).isEqualTo("PKG-1");
        assertThat(response.getPesoSansas()).isEqualTo(7.48);
        assertThat(response.getCategoria()).isEqualTo(CategoriaPeso.LIVIANO);
        assertThat(response.getEstado()).isEqualTo(EstadoPesaje.INGRESADO);
        assertThat(response.getHistorialTransiciones()).hasSize(1);
    }

    @Test
    void createRegistro_reglaDeNegocioFalla_propagaExcepcion() {
        CreateRegistroPesajeRequest request = new CreateRegistroPesajeRequest(7L, "PKG-2", 100.0);
        when(rulesService.deKgToSansa(100.0)).thenReturn(74.8);
        when(rulesService.clasificarPackage(74.8)).thenReturn(CategoriaPeso.PESADO);
        doThrow(new BusinessRuleException("horario nocturno"))
                .when(rulesService).validarRestriccionTiempo(any(), any());

        assertThatThrownBy(() -> service.createRegistro(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nocturno");
    }

    @Test
    void updateEstado_transicionValida_actualizaEstado() {
        RegistroPesaje existente = new RegistroPesaje();
        existente.setId("id1");
        existente.setEstado(EstadoPesaje.INGRESADO);
        when(repository.findById("id1")).thenReturn(Optional.of(existente));
        when(repository.save(any(RegistroPesaje.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRegistroPesajeRequest request = new UpdateRegistroPesajeRequest(EstadoPesaje.PESADO);
        RegistroPesajeResponse response = service.updateEstado("id1", request);

        assertThat(response.getEstado()).isEqualTo(EstadoPesaje.PESADO);
        assertThat(response.getHistorialTransiciones()).isNotEmpty();
    }

    @Test
    void updateEstado_registroNoEncontrado_lanzaExcepcion() {
        when(repository.findById("noexiste")).thenReturn(Optional.empty());
        UpdateRegistroPesajeRequest request = new UpdateRegistroPesajeRequest(EstadoPesaje.PESADO);

        assertThatThrownBy(() -> service.updateEstado("noexiste", request))
                .isInstanceOf(RegistroNoEncontradoException.class)
                .hasMessageContaining("noexiste");
    }

    @Test
    void getRegistrosByFecha_devuelveListaMapeada() {
        LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);
        RegistroPesaje reg = new RegistroPesaje();
        reg.setId("r1");
        reg.setEstado(EstadoPesaje.INGRESADO);
        when(repository.findByCreatedAtBetween(desde, hasta)).thenReturn(List.of(reg));

        List<RegistroPesajeResponse> result = service.getRegistrosByFecha(desde, hasta);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("r1");
    }
}
