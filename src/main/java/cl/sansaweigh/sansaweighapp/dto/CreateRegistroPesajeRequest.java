package cl.sansaweigh.sansaweighapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos de entrada para crear un nuevo registro de pesaje.
 * La categoría y el estado no se reciben: los deriva el servicio aplicando
 * las reglas de negocio (clasificación por peso, máquina de estados).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRegistroPesajeRequest {

    @NotNull(message = "balanzaId es obligatorio")
    @Positive(message = "balanzaId debe ser positivo")
    private Long balanzaId;

    @NotBlank(message = "paqueteId es obligatorio")
    private String paqueteId;

    @NotNull(message = "pesoSansas es obligatorio")
    @Positive(message = "pesoSansas debe ser positivo")
    private Double pesoSansas;
}
