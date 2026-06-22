package cl.sansaweigh.sansaweighapp.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@ToString
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
