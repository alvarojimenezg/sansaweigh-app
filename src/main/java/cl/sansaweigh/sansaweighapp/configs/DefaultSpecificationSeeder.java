package cl.sansaweigh.sansaweighapp.configs;

import cl.sansaweigh.sansaweighapp.services.ScaleSpecificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Siembra la especificación de balanza por defecto (id "-1") en Redis al arrancar la app,
 * de modo que el fallback tenga siempre un valor que entregar cuando la API externa falle
 * y no exista caché. Si Redis no está disponible al inicio, se registra un aviso sin
 * impedir el arranque (la spec se recreará bajo demanda en el primer fallback).
 */
@Slf4j
@Component
public class DefaultSpecificationSeeder implements ApplicationRunner {

    private final ScaleSpecificationService scaleSpecificationService;

    public DefaultSpecificationSeeder(ScaleSpecificationService scaleSpecificationService) {
        this.scaleSpecificationService = scaleSpecificationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            scaleSpecificationService.getDefaultSpecification();
            log.info("Spec por defecto (id \"-1\") sembrada en Redis.");
        } catch (Exception ex) {
            log.warn("No se pudo sembrar la spec por defecto al arrancar (¿Redis disponible?): {}",
                    ex.getMessage());
        }
    }
}
