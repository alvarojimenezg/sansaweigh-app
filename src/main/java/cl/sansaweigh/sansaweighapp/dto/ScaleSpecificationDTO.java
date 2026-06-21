package cl.sansaweigh.sansaweighapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shape del JSON que entrega la API externa de especificaciones de balanza
 * (simulada con Mockoon). Se mapea hacia/desde la entidad ScaleSpecification
 * que se cachea en Redis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleSpecificationDTO {

    private String id;

    private String name;

    private String brand;

    private Double maxCapacity;

    private Double precision;

    private Double lastCalibrationOffset;
}
