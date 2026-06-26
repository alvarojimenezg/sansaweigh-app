package cl.sansaweigh.sansaweighapp.services;

import cl.sansaweigh.sansaweighapp.entities.CategoriaPeso;
import cl.sansaweigh.sansaweighapp.entities.EstadoPesaje;

import java.time.LocalDate;
import java.time.LocalTime;

public interface WeighingRulesService {

    double deKgToSansa(double kg);
    CategoriaPeso clasificarPackage(double Sansas);
    void validarRestriccionTiempo(CategoriaPeso categoria, LocalTime tiempoActual);
    void validarRestriccionPrimos(int balanzaId, CategoriaPeso categoria, LocalDate data);
    void validarTranscursosEstados(EstadoPesaje estadoActual, EstadoPesaje estadoSig);
}
