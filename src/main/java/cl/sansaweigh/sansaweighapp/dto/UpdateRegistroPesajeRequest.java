package cl.sansaweigh.sansaweighapp.dto;

import cl.sansaweigh.sansaweighapp.entities.EstadoPesaje;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Datos de entrada para transicionar el estado de un registro de pesaje.
 * El servicio valida que la transición sea permitida por la máquina de estados:
 * INGRESADO -> PESADO -> APROBADO|RECHAZADO -> DESPACHADO.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRegistroPesajeRequest {

    @NotNull(message = "nuevoEstado es obligatorio")
    private EstadoPesaje nuevoEstado;
}
