package cl.sansaweigh.sansaweighapp.services.impl;

import cl.sansaweigh.sansaweighapp.dto.ScaleSpecificationDTO;
import cl.sansaweigh.sansaweighapp.entities.ScaleSpecification;
import cl.sansaweigh.sansaweighapp.exceptions.ExternalScaleUnavailableException;
import cl.sansaweigh.sansaweighapp.integration.ExternalScaleClient;
import cl.sansaweigh.sansaweighapp.repositories.ScaleSpecificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScaleSpecificationServiceImplTest {

    @Mock
    private ScaleSpecificationRepository repository;

    @Mock
    private ExternalScaleClient externalScaleClient;

    @InjectMocks
    private ScaleSpecificationServiceImpl service;

    private ScaleSpecification spec(String id, String name) {
        ScaleSpecification s = new ScaleSpecification();
        s.setId(id);
        s.setName(name);
        s.setBrand("marca");
        s.setMaxCapacity(150.0);
        s.setPrecision(0.01);
        s.setLastCalibrationOffset(0.0);
        return s;
    }

    @Test
    void getScaleSpecification_cacheHit_devuelveDeRedisSinLlamarApi() {
        ScaleSpecification cached = spec("101", "Cacheada");
        when(repository.findById("101")).thenReturn(Optional.of(cached));

        ScaleSpecification result = service.getScaleSpecification("101");

        assertThat(result).isSameAs(cached);
        verifyNoInteractions(externalScaleClient);
        verify(repository, never()).save(any());
    }

    @Test
    void getScaleSpecification_cacheMiss_consultaApiYCachea() {
        when(repository.findById("202")).thenReturn(Optional.empty());
        ScaleSpecificationDTO dto = new ScaleSpecificationDTO("999", "Desde API", "ACME", 200.0, 0.05, 0.1);
        when(externalScaleClient.getScaleSpecifications("202")).thenReturn(dto);
        when(repository.save(any(ScaleSpecification.class))).thenAnswer(inv -> inv.getArgument(0));

        ScaleSpecification result = service.getScaleSpecification("202");

        // El id debe ser el scaleId solicitado (clave de caché), no el del DTO
        assertThat(result.getId()).isEqualTo("202");
        assertThat(result.getName()).isEqualTo("Desde API");
        assertThat(result.getBrand()).isEqualTo("ACME");
        verify(repository).save(any(ScaleSpecification.class));
    }

    @Test
    void getScaleSpecification_apiFalla_devuelveSpecPorDefecto() {
        when(repository.findById("777")).thenReturn(Optional.empty());
        when(externalScaleClient.getScaleSpecifications("777"))
                .thenThrow(new ExternalScaleUnavailableException("API caída", null));
        ScaleSpecification porDefecto = spec("-1", "Balanza por defecto");
        when(repository.findById("-1")).thenReturn(Optional.of(porDefecto));

        ScaleSpecification result = service.getScaleSpecification("777");

        assertThat(result.getId()).isEqualTo("-1");
        assertThat(result.getName()).isEqualTo("Balanza por defecto");
    }

    @Test
    void getDefaultSpecification_existe_laDevuelve() {
        ScaleSpecification porDefecto = spec("-1", "Balanza por defecto");
        when(repository.findById("-1")).thenReturn(Optional.of(porDefecto));

        ScaleSpecification result = service.getDefaultSpecification();

        assertThat(result).isSameAs(porDefecto);
        verify(repository, never()).save(any());
    }

    @Test
    void getDefaultSpecification_noExiste_laCreaYGuarda() {
        when(repository.findById("-1")).thenReturn(Optional.empty());
        when(repository.save(any(ScaleSpecification.class))).thenAnswer(inv -> inv.getArgument(0));

        ScaleSpecification result = service.getDefaultSpecification();

        assertThat(result.getId()).isEqualTo("-1");
        assertThat(result.getName()).isEqualTo("Balanza por defecto");
        assertThat(result.getBrand()).isEqualTo("SansaWeigh-Default");
        verify(repository).save(any(ScaleSpecification.class));
    }
}
