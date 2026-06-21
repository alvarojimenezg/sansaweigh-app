package cl.sansaweigh.sansaweighapp.dto;

import cl.sansaweigh.sansaweighapp.entities.EstadoPesaje;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos de entrada para transicionar el estado de un registro de pesaje.
 * El servicio valida que la transición sea permitida por la máquina de estados:
 * INGRESADO -> PESADO -> APROBADO|RECHAZADO -> DESPACHADO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRegistroPesajeRequest {

    @NotNull(message = "nuevoEstado es obligatorio")
    private EstadoPesaje nuevoEstado;
}
