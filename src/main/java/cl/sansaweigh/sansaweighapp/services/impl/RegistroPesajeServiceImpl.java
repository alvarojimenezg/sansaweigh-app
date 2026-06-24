package cl.sansaweigh.sansaweighapp.services.impl;

import cl.sansaweigh.sansaweighapp.dto.CreateRegistroPesajeRequest;
import cl.sansaweigh.sansaweighapp.dto.RegistroPesajeResponse;
import cl.sansaweigh.sansaweighapp.dto.UpdateRegistroPesajeRequest;
import cl.sansaweigh.sansaweighapp.entities.CategoriaPeso;
import cl.sansaweigh.sansaweighapp.entities.EstadoPesaje;
import cl.sansaweigh.sansaweighapp.entities.RegistroPesaje;
import cl.sansaweigh.sansaweighapp.exceptions.RegistroNoEncontradoException;
import cl.sansaweigh.sansaweighapp.repositories.RegistroPesajeRepository;
import cl.sansaweigh.sansaweighapp.services.RegistroPesajeService;
import cl.sansaweigh.sansaweighapp.services.WeighingRulesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistroPesajeServiceImpl implements RegistroPesajeService {

    private RegistroPesajeRepository repository;
    private WeighingRulesService rulesService;

    @Autowired
    public RegistroPesajeServiceImpl(RegistroPesajeRepository repository, WeighingRulesService rulesService) {
        this.repository = repository;
        this.rulesService = rulesService;
    }


    private RegistroPesajeResponse mapToResponse(RegistroPesaje entity) {
        RegistroPesajeResponse response = new RegistroPesajeResponse();
        response.setId(entity.getId());
        response.setBalanzaId(entity.getBalanzaId());
        response.setPaqueteId(entity.getPaqueteId());
        response.setPesoSansas(entity.getPesoSansas());
        response.setCategoria(entity.getCategoria());
        response.setEstado(entity.getEstado());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getHistorialTransiciones() != null) {
            response.setHistorialTransiciones(new ArrayList<>(entity.getHistorialTransiciones()));
        } else {
            response.setHistorialTransiciones(new ArrayList<>());
        }

        return response;
    }


    @Override
    public RegistroPesajeResponse createRegistro(CreateRegistroPesajeRequest request) {
        // CORRECCIÓN: Primero convertimos los Kg del request a Sansas
        double pesoSansas = rulesService.deKgToSansa(request.getPesoKg()); //

        // Ahora clasificamos usando el peso en Sansas correcto
        CategoriaPeso categoria = rulesService.clasificarPackage(pesoSansas); //

        int balanzaId = request.getBalanzaId() != null ? request.getBalanzaId().intValue() : 0;

        rulesService.validarRestriccionTiempo(categoria, LocalTime.now());
        rulesService.validarRestriccionPrimos(balanzaId, categoria, LocalDate.now());

        rulesService.validarTranscursosEstados(null, EstadoPesaje.INGRESADO);

        RegistroPesaje registro = new RegistroPesaje();
        registro.setBalanzaId(balanzaId);
        registro.setPaqueteId(request.getPaqueteId());
        registro.setPesoSansas(pesoSansas);
        registro.setCategoria(categoria);
        registro.setEstado(EstadoPesaje.INGRESADO);
        LocalDateTime now = LocalDateTime.now();
        registro.setCreatedAt(now);
        registro.setUpdatedAt(now);
        registro.getHistorialTransiciones().add("Creado en estado INGRESADO a las " + now.toString());
        registro = repository.save(registro);

        return mapToResponse(registro);
    }

    @Override
    public RegistroPesajeResponse updateEstado(String id, UpdateRegistroPesajeRequest request) {

        RegistroPesaje registro = repository.findById(id)
                .orElseThrow(() -> new RegistroNoEncontradoException("Registro no encontrado con ID: " + id));
        
        rulesService.validarTranscursosEstados(registro.getEstado(), request.getNuevoEstado());

        registro.setEstado(request.getNuevoEstado());
        registro.setUpdatedAt(LocalDateTime.now());
        registro.getHistorialTransiciones().add("Transición a " + request.getNuevoEstado() + " a las " + registro.getUpdatedAt().toString());
        registro = repository.save(registro);

        return mapToResponse(registro);
    }

    @Override
    public List<RegistroPesajeResponse> getRegistrosByFecha(LocalDateTime desde, LocalDateTime hasta) {
        return repository.findByCreatedAtBetween(desde, hasta).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


}
