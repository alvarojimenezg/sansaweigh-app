package cl.sansaweigh.sansaweighapp.services.impl;

import cl.sansaweigh.sansaweighapp.entities.CategoriaPeso;
import cl.sansaweigh.sansaweighapp.entities.EstadoPesaje;
import cl.sansaweigh.sansaweighapp.exceptions.BusinessRuleException;
import cl.sansaweigh.sansaweighapp.exceptions.IllegalWeighingStateException;
import cl.sansaweigh.sansaweighapp.services.WeighingRulesService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class WeighingRulesServiceImpl implements WeighingRulesService{
    private static final double CONVERSION_RATIO = 1.337;

    @Override
    public double deKgToSansa(double kg) {
        if (kg < 0) {
            throw new IllegalArgumentException("El peso no puede ser negativo");
        }
        return kg / CONVERSION_RATIO;
    }

    @Override
    public CategoriaPeso clasificarPackage(double Sansas) {
        if (Sansas <= 10) {
            return CategoriaPeso.LIVIANO;
        } else if (Sansas <= 50) {
            return CategoriaPeso.MEDIANO;
        } else {
            return CategoriaPeso.PESADO;
        }
    }

    @Override
    public void validarRestriccionTiempo(CategoriaPeso categoria, LocalTime tiempoActual) {
        if (categoria == CategoriaPeso.PESADO) {
            // Entre las 20:00 (inclusive) y antes de las 06:00
            if (!tiempoActual.isBefore(LocalTime.of(20, 0)) || tiempoActual.isBefore(LocalTime.of(6, 0))) {
                throw new BusinessRuleException("No se permite procesar paquetes PESADOS en horario nocturno (20:00 - 06:00).");
            }
        }
    }

    private boolean esPrimo(int numero) {
        if (numero <= 1) return false;
        if (numero == 2 || numero == 3) return true;
        if (numero % 2 == 0) return false;

        for (int i = 3; i * i <= numero; i += 2) {
            if (numero % i == 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void validarRestriccionPrimos(int balanzaId, CategoriaPeso categoria, LocalDate data) {
        if (categoria == CategoriaPeso.PESADO) {
            if (data.getDayOfMonth() % 2 != 0 && esPrimo(balanzaId)) {
                throw new BusinessRuleException("Las balanzas con ID primo no pueden registrar paquetes PESADOS en días impares.");
            }
        }
    }

    @Override
    public void validarTranscursosEstados(EstadoPesaje estadoActual, EstadoPesaje estadoSig) {
        boolean esValido = false;

        if (estadoActual == null && estadoSig == EstadoPesaje.INGRESADO) {
            esValido = true;
        } else if (estadoActual != null) {
            switch (estadoActual) {
                case INGRESADO:
                    esValido = (estadoSig == EstadoPesaje.PESADO);
                    break;
                case PESADO:
                    esValido = (estadoSig == EstadoPesaje.APROBADO || estadoSig == EstadoPesaje.RECHAZADO);
                    break;
                case APROBADO:
                case RECHAZADO:
                    esValido = (estadoSig == EstadoPesaje.DESPACHADO);
                    break;
                case DESPACHADO:
                    esValido = false; // Estado terminal
                    break;
            }
        }

        if (!esValido) {
            throw new IllegalWeighingStateException(
                    "Transición de estado inválida. No se puede pasar de " + estadoActual + " a " + estadoSig
            );
        }
    }


}
