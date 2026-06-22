package cl.sansaweigh.sansaweighapp.dto;

import cl.sansaweigh.sansaweighapp.entities.CategoriaPeso;
import cl.sansaweigh.sansaweighapp.entities.EstadoPesaje;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representación de salida de un registro de pesaje hacia el cliente de la API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPesajeResponse {

    private String id;

    private int balanzaId;

    private String paqueteId;

    private Double pesoSansas;

    private CategoriaPeso categoria;

    private EstadoPesaje estado;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<String> historialTransiciones;
}
