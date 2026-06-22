package cl.sansaweigh.sansaweighapp.controllers;

import cl.sansaweigh.sansaweighapp.entities.ScaleSpecification;
import cl.sansaweigh.sansaweighapp.services.ScaleSpecificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone las especificaciones de balanza vía ScaleSpecificationService.
 * Endpoint opcional (Fase 5) útil para verificar la estrategia de caché + fallback.
 */
@RestController
@RequestMapping("/balanzas")
public class ScaleSpecificationController {

    private final ScaleSpecificationService scaleSpecificationService;

    public ScaleSpecificationController(ScaleSpecificationService scaleSpecificationService) {
        this.scaleSpecificationService = scaleSpecificationService;
    }

    @GetMapping("/{scaleId}/specs")
    public ResponseEntity<ScaleSpecification> getSpecs(@PathVariable String scaleId) {
        return ResponseEntity.ok(scaleSpecificationService.getScaleSpecification(scaleId));
    }
}
