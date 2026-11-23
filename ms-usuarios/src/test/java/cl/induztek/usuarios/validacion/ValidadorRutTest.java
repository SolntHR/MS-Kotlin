package cl.induztek.usuarios.validacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidadorRutTest {

    private final RutValidoValidator validador = new RutValidoValidator();

    @Test
    void aceptaRutConDigitoVerificadorCorrecto() {
        assertThat(ValidadorRut.esValido("17.284.519-3")).isTrue();
        assertThat(ValidadorRut.esValido("15906334-8")).isTrue();
    }

    @Test
    void aceptaDigitoVerificadorKyCero() {
        assertThat(ValidadorRut.esValido("10.000.013-K")).isTrue();
        assertThat(ValidadorRut.esValido("11.111.111-1")).isTrue();
    }

    @Test
    void rechazaRutConDigitoIncorrectoOFormatoInvalido() {
        assertThat(ValidadorRut.esValido("17.284.519-4")).isFalse();
        assertThat(ValidadorRut.esValido("abc")).isFalse();
        assertThat(ValidadorRut.esValido(null)).isFalse();
    }

    @Test
    void formateaConPuntosYGuion() {
        assertThat(ValidadorRut.formatear("172845193")).isEqualTo("17.284.519-3");
        assertThat(ValidadorRut.formatear("1")).isEqualTo("1");
    }

    @Test
    void elValidadorDejaPasarVaciosParaQueLosRevise_NotBlank() {
        assertThat(validador.isValid(null, null)).isTrue();
        assertThat(validador.isValid(" ", null)).isTrue();
        assertThat(validador.isValid("17.284.519-3", null)).isTrue();
        assertThat(validador.isValid("17.284.519-4", null)).isFalse();
    }
}
