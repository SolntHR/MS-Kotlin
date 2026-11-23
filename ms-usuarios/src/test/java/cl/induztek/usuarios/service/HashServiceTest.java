package cl.induztek.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashServiceTest {

    private final HashService hashService = new HashService();

    @Test
    void elHashEsSha256EnHexadecimalYSeRepiteConLaMismaSal() {
        String sal = hashService.generarSal();
        String hash = hashService.hash("123456", sal);
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
        assertThat(hashService.hash("123456", sal)).isEqualTo(hash);
    }

    @Test
    void distintaSalProduceDistintoHash() {
        assertThat(hashService.hash("123456", hashService.generarSal()))
                .isNotEqualTo(hashService.hash("123456", hashService.generarSal()));
    }

    @Test
    void coincideSoloConLaContrasenaCorrecta() {
        String sal = hashService.generarSal();
        String hash = hashService.hash("Induztek#2026", sal);
        assertThat(hashService.coincide("Induztek#2026", sal, hash)).isTrue();
        assertThat(hashService.coincide("otra", sal, hash)).isFalse();
        assertThat(hashService.coincide(null, sal, hash)).isFalse();
        assertThat(hashService.coincide("Induztek#2026", null, hash)).isFalse();
    }

    @Test
    void normalizaRespuestasSinImportarMayusculasNiEspacios() {
        assertThat(hashService.normalizarRespuesta("  Firulais ")).isEqualTo("firulais");
        assertThat(hashService.normalizarRespuesta(null)).isEmpty();
    }

    @Test
    void elHashDeLaAppUsaElCorreoComoSalYCoincideConKotlin() {
        String esperado = "09b1ce95bef87d014de1194ea6ab9e2fa3da38fbe73aaf378b5377c34cd32638";

        assertThat(hashService.hashCliente("respinoza@induztek.ejemplo", "Induztek#2026")).isEqualTo(esperado);
        assertThat(hashService.hashCliente("  RESPINOZA@Induztek.ejemplo ", "Induztek#2026")).isEqualTo(esperado);
        assertThat(hashService.hashCliente(null, "x")).hasSize(64);
    }

    @Test
    void laVerificacionSimuladaNoFallaConNiSinValor() {
        hashService.simularVerificacion(null);
        hashService.simularVerificacion("cualquier-cosa");
        assertThat(hashService.coincide(null, "sal", "hash")).isFalse();
    }

    @Test
    void generaCodigosYSalesConElLargoEsperado() {
        assertThat(hashService.generarCodigo()).matches("\\d{6}");
        assertThat(hashService.generarSal()).hasSize(32);
    }
}
