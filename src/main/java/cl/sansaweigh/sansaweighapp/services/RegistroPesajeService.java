package cl.sansaweigh.sansaweighapp.services;

import cl.sansaweigh.sansaweighapp.dto.CreateRegistroPesajeRequest;
import cl.sansaweigh.sansaweighapp.dto.RegistroPesajeResponse;
import cl.sansaweigh.sansaweighapp.dto.UpdateRegistroPesajeRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistroPesajeService {

    //crear
    RegistroPesajeResponse createRegistro(CreateRegistroPesajeRequest request);

    //actualizar
    RegistroPesajeResponse updateEstado(String id, UpdateRegistroPesajeRequest request);

    //filtrar por fecha
    List<RegistroPesajeResponse> getRegistrosByFecha(LocalDateTime desde, LocalDateTime hasta);
}
