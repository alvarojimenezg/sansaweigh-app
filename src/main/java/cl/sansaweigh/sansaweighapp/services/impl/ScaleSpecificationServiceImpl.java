package cl.sansaweigh.sansaweighapp.services.impl;

import cl.sansaweigh.sansaweighapp.dto.ScaleSpecificationDTO;
import cl.sansaweigh.sansaweighapp.entities.ScaleSpecification;
import cl.sansaweigh.sansaweighapp.exceptions.ExternalScaleUnavailableException;
import cl.sansaweigh.sansaweighapp.integration.ExternalScaleClient;
import cl.sansaweigh.sansaweighapp.repositories.ScaleSpecificationRepository;
import cl.sansaweigh.sansaweighapp.services.ScaleSpecificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class ScaleSpecificationServiceImpl implements ScaleSpecificationService {

    /** Id convenido para la spec por defecto del fallback. */
    private static final String DEFAULT_ID = "-1";

    private final ScaleSpecificationRepository repository;
    private final ExternalScaleClient externalScaleClient;

    public ScaleSpecificationServiceImpl(ScaleSpecificationRepository repository,
                                         ExternalScaleClient externalScaleClient) {
        this.repository = repository;
        this.externalScaleClient = externalScaleClient;
    }

    @Override
    public ScaleSpecification getScaleSpecification(String scaleId) {
        // 1. Cache hit: ¿ya está en Redis?
        Optional<ScaleSpecification> cached = repository.findById(scaleId);
        if (cached.isPresent()) {
            log.debug("Cache hit en Redis para balanza {}", scaleId);
            return cached.get();
        }

        // 2. Cache miss: consultar la API externa y cachear.
        try {
            ScaleSpecificationDTO dto = externalScaleClient.getScaleSpecifications(scaleId);
            ScaleSpecification spec = toEntity(scaleId, dto);
            ScaleSpecification saved = repository.save(spec); // se cachea con TTL 120s
            log.debug("Specs de balanza {} obtenidas de la API y cacheadas", scaleId);
            return saved;
        } catch (ExternalScaleUnavailableException ex) {
            // 3. Fallback: la API falló y no había caché -> spec por defecto.
            log.warn("API externa no disponible para balanza {}. Usando spec por defecto. Causa: {}",
                    scaleId, ex.getMessage());
            return getDefaultSpecification();
        }
    }

    @Override
    public ScaleSpecification getDefaultSpecification() {
        // Si la spec por defecto expiró (TTL) o nunca se sembró, se vuelve a crear.
        return repository.findById(DEFAULT_ID)
                .orElseGet(() -> repository.save(buildDefault()));
    }

    private ScaleSpecification toEntity(String scaleId, ScaleSpecificationDTO dto) {
        ScaleSpecification spec = new ScaleSpecification();
        // Usamos scaleId como clave de caché para que el findById posterior la encuentre.
        spec.setId(scaleId);
        spec.setName(dto.getName());
        spec.setBrand(dto.getBrand());
        spec.setMaxCapacity(dto.getMaxCapacity());
        spec.setPrecision(dto.getPrecision());
        spec.setLastCalibrationOffset(dto.getLastCalibrationOffset());
        return spec;
    }

    private ScaleSpecification buildDefault() {
        ScaleSpecification spec = new ScaleSpecification();
        spec.setId(DEFAULT_ID);
        spec.setName("Balanza por defecto");
        spec.setBrand("SansaWeigh-Default");
        spec.setMaxCapacity(100.0);
        spec.setPrecision(0.1);
        spec.setLastCalibrationOffset(0.0);
        return spec;
    }
}
