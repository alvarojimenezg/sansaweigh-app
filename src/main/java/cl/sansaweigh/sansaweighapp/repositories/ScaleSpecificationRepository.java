package cl.sansaweigh.sansaweighapp.repositories;

import cl.sansaweigh.sansaweighapp.entities.ScaleSpecification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Acceso a las especificaciones de balanza cacheadas en Redis.
 * Al extender CrudRepository, Spring Data provee save(), findById(), etc.
 * El TTL de 120s lo define la anotación @RedisHash de la entidad ScaleSpecification.
 */
@Repository
public interface ScaleSpecificationRepository extends CrudRepository<ScaleSpecification, String> {
}
