package cl.sansaweigh.sansaweighapp.services.impl;

import cl.sansaweigh.sansaweighapp.entities.CategoriaPeso;
import cl.sansaweigh.sansaweighapp.entities.EstadoPesaje;
import cl.sansaweigh.sansaweighapp.exceptions.BusinessRuleException;
import cl.sansaweigh.sansaweighapp.exceptions.IllegalWeighingStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class WeighingRulesServiceImplTest {

    private WeighingRulesServiceImpl rules;

    @BeforeEach
    void setUp() {
        rules = new WeighingRulesServiceImpl();
    }

    // ---------- deKgToSansa ----------

    @Test
    void deKgToSansa_convierteKgASansas() {
        // 1.337 kg = 1 Sansa
        assertThat(rules.deKgToSansa(1.337)).isEqualTo(1.0, within(1e-9));
        assertThat(rules.deKgToSansa(13.37)).isEqualTo(10.0, within(1e-9));
    }

    @Test
    void deKgToSansa_pesoNegativo_lanzaIllegalArgument() {
        assertThatThrownBy(() -> rules.deKgToSansa(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- clasificarPackage (bordes incluidos) ----------

    @Test
    void clasificar_livianoIncluyendoBorde10() {
        assertThat(rules.clasificarPackage(5.0)).isEqualTo(CategoriaPeso.LIVIANO);
        assertThat(rules.clasificarPackage(10.0)).isEqualTo(CategoriaPeso.LIVIANO);
    }

    @Test
    void clasificar_medianoEntre10y50() {
        assertThat(rules.clasificarPackage(10.01)).isEqualTo(CategoriaPeso.MEDIANO);
        assertThat(rules.clasificarPackage(50.0)).isEqualTo(CategoriaPeso.MEDIANO);
    }

    @Test
    void clasificar_pesadoSobre50() {
        assertThat(rules.clasificarPackage(50.01)).isEqualTo(CategoriaPeso.PESADO);
        assertThat(rules.clasificarPackage(120.0)).isEqualTo(CategoriaPeso.PESADO);
    }

    // ---------- validarRestriccionTiempo ----------

    @Test
    void tiempo_pesadoDeDia_ok() {
        assertThatCode(() -> rules.validarRestriccionTiempo(CategoriaPeso.PESADO, LocalTime.of(12, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void tiempo_pesadoDeNoche_lanzaBusinessRule() {
        assertThatThrownBy(() -> rules.validarRestriccionTiempo(CategoriaPeso.PESADO, LocalTime.of(22, 0)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void tiempo_pesadoEnLimite2000_lanzaBusinessRule() {
        assertThatThrownBy(() -> rules.validarRestriccionTiempo(CategoriaPeso.PESADO, LocalTime.of(20, 0)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void tiempo_pesadoMadrugada_lanzaBusinessRule() {
        assertThatThrownBy(() -> rules.validarRestriccionTiempo(CategoriaPeso.PESADO, LocalTime.of(5, 59)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void tiempo_noPesadoDeNoche_ok() {
        assertThatCode(() -> rules.validarRestriccionTiempo(CategoriaPeso.LIVIANO, LocalTime.of(22, 0)))
                .doesNotThrowAnyException();
    }

    // ---------- validarRestriccionPrimos ----------

    @Test
    void primos_balanzaPrimaDiaImparPesado_lanzaBusinessRule() {
        // balanza 7 (primo), día 1 (impar)
        assertThatThrownBy(() -> rules.validarRestriccionPrimos(7, CategoriaPeso.PESADO, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void primos_balanzaPrimaDiaParPesado_ok() {
        // balanza 7 (primo), día 2 (par)
        assertThatCode(() -> rules.validarRestriccionPrimos(7, CategoriaPeso.PESADO, LocalDate.of(2026, 1, 2)))
                .doesNotThrowAnyException();
    }

    @Test
    void primos_balanzaNoPrimaDiaImparPesado_ok() {
        // balanza 8 (no primo), día 1 (impar)
        assertThatCode(() -> rules.validarRestriccionPrimos(8, CategoriaPeso.PESADO, LocalDate.of(2026, 1, 1)))
                .doesNotThrowAnyException();
    }

    @Test
    void primos_balanzaPrimaDiaImparNoPesado_ok() {
        assertThatCode(() -> rules.validarRestriccionPrimos(7, CategoriaPeso.LIVIANO, LocalDate.of(2026, 1, 1)))
                .doesNotThrowAnyException();
    }

    @Test
    void primos_balanza2EsPrimaDiaImparPesado_lanzaBusinessRule() {
        // 2 es primo (caso par): día 1 (impar)
        assertThatThrownBy(() -> rules.validarRestriccionPrimos(2, CategoriaPeso.PESADO, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void primos_balanza1NoEsPrimaDiaImparPesado_ok() {
        // 1 no es primo
        assertThatCode(() -> rules.validarRestriccionPrimos(1, CategoriaPeso.PESADO, LocalDate.of(2026, 1, 1)))
                .doesNotThrowAnyException();
    }

    @Test
    void primos_balanza9CompuestoImpar_ok() {
        // 9 = 3*3, compuesto impar -> ejercita el bucle de esPrimo y descarta la restricción
        assertThatCode(() -> rules.validarRestriccionPrimos(9, CategoriaPeso.PESADO, LocalDate.of(2026, 1, 1)))
                .doesNotThrowAnyException();
    }

    // ---------- validarTranscursosEstados ----------

    @Test
    void estados_transicionesValidas_ok() {
        assertThatCode(() -> rules.validarTranscursosEstados(null, EstadoPesaje.INGRESADO)).doesNotThrowAnyException();
        assertThatCode(() -> rules.validarTranscursosEstados(EstadoPesaje.INGRESADO, EstadoPesaje.PESADO)).doesNotThrowAnyException();
        assertThatCode(() -> rules.validarTranscursosEstados(EstadoPesaje.PESADO, EstadoPesaje.APROBADO)).doesNotThrowAnyException();
        assertThatCode(() -> rules.validarTranscursosEstados(EstadoPesaje.PESADO, EstadoPesaje.RECHAZADO)).doesNotThrowAnyException();
        assertThatCode(() -> rules.validarTranscursosEstados(EstadoPesaje.APROBADO, EstadoPesaje.DESPACHADO)).doesNotThrowAnyException();
        assertThatCode(() -> rules.validarTranscursosEstados(EstadoPesaje.RECHAZADO, EstadoPesaje.DESPACHADO)).doesNotThrowAnyException();
    }

    @Test
    void estados_saltoInvalido_lanzaIllegalState() {
        assertThatThrownBy(() -> rules.validarTranscursosEstados(EstadoPesaje.INGRESADO, EstadoPesaje.DESPACHADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    void estados_pesadoADespachado_lanzaIllegalState() {
        assertThatThrownBy(() -> rules.validarTranscursosEstados(EstadoPesaje.PESADO, EstadoPesaje.DESPACHADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    void estados_despachadoEsTerminal_lanzaIllegalState() {
        assertThatThrownBy(() -> rules.validarTranscursosEstados(EstadoPesaje.DESPACHADO, EstadoPesaje.INGRESADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }

    @Test
    void estados_nuevoSinIngresado_lanzaIllegalState() {
        assertThatThrownBy(() -> rules.validarTranscursosEstados(null, EstadoPesaje.PESADO))
                .isInstanceOf(IllegalWeighingStateException.class);
    }
}
