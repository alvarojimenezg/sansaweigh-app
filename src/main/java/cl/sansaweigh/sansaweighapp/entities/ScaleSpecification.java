package cl.sansaweigh.sansaweighapp.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "ScaleSpecification", timeToLive = 120)
public class ScaleSpecification {

    @Id
    private String id;

    private String name;

    private String brand;

    private Double maxCapacity;

    private Double precision;

    private Double lastCalibrationOffset;
}
