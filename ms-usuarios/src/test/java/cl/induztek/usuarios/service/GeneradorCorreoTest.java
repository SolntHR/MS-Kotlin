package cl.induztek.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import cl.induztek.usuarios.repository.UsuarioRepository;

class GeneradorCorreoTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final GeneradorCorreo generador = new GeneradorCorreo(usuarioRepository, "induztek.ejemplo");

    @Test
    void usaLaInicialDelNombreYElPrimerApellidoSinTildes() {
        assertThat(GeneradorCorreo.base("Matías Godoy Herrera")).isEqualTo("mgodoy");
        assertThat(GeneradorCorreo.base("Ana María Núñez Soto")).isEqualTo("anunez");
        assertThat(GeneradorCorreo.base("Pedro Rojas")).isEqualTo("projas");
        assertThat(GeneradorCorreo.base("Cher")).isEqualTo("cher");
        assertThat(GeneradorCorreo.base("  ")).isEqualTo("usuario");
        assertThat(GeneradorCorreo.base(null)).isEqualTo("usuario");
    }

    @Test
    void agregaUnNumeroSiElCorreoYaExiste() {
        when(usuarioRepository.existsByCorreoIgnoreCase("mgodoy@induztek.ejemplo")).thenReturn(true);
        when(usuarioRepository.existsByCorreoIgnoreCase("mgodoy2@induztek.ejemplo")).thenReturn(true);

        assertThat(generador.generar("Matías Godoy Herrera")).isEqualTo("mgodoy3@induztek.ejemplo");
        assertThat(generador.generar("Pedro Rojas")).isEqualTo("projas@induztek.ejemplo");
    }
}
