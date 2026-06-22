package cl.sansaweigh.sansaweighapp.controllers;

import cl.sansaweigh.sansaweighapp.dto.CreateRegistroPesajeRequest;
import cl.sansaweigh.sansaweighapp.dto.RegistroPesajeResponse;
import cl.sansaweigh.sansaweighapp.dto.UpdateRegistroPesajeRequest;
import cl.sansaweigh.sansaweighapp.services.RegistroPesajeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/registros")
public class RegistroPesajeController {

    private final RegistroPesajeService registroPesajeService;

    @Autowired
    public RegistroPesajeController(RegistroPesajeService registroPesajeService) {
        this.registroPesajeService = registroPesajeService;
    }

    @PostMapping
    public ResponseEntity<RegistroPesajeResponse> createRegistro(@Valid @RequestBody CreateRegistroPesajeRequest request) {
        RegistroPesajeResponse response = registroPesajeService.createRegistro(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<RegistroPesajeResponse> updateEstadoPut(
            @PathVariable String id,
            @Valid @RequestBody UpdateRegistroPesajeRequest request) {
        RegistroPesajeResponse response = registroPesajeService.updateEstado(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<RegistroPesajeResponse> updateEstadoPatch(
            @PathVariable String id,
            @Valid @RequestBody UpdateRegistroPesajeRequest request) {
        RegistroPesajeResponse response = registroPesajeService.updateEstado(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RegistroPesajeResponse>> getRegistrosByFecha(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        List<RegistroPesajeResponse> response = registroPesajeService.getRegistrosByFecha(desde, hasta);
        return ResponseEntity.ok(response);
    }
}
