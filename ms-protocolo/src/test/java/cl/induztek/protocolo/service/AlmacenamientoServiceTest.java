package cl.induztek.protocolo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cl.induztek.protocolo.exception.RecursoNoEncontradoException;

class AlmacenamientoServiceTest {

    @TempDir
    Path carpeta;

    @Test
    void guardaLeeYEliminaArchivos() {
        AlmacenamientoService servicio = new AlmacenamientoService(carpeta.resolve("evidencias").toString());

        servicio.guardar("foto.jpg", new ByteArrayInputStream(new byte[] {7, 8}));

        assertThat(servicio.leer("foto.jpg")).containsExactly(7, 8);
        servicio.eliminar("foto.jpg");
        assertThat(Files.exists(carpeta.resolve("evidencias/foto.jpg"))).isFalse();
        assertThatThrownBy(() -> servicio.leer("foto.jpg")).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void noPermiteSalirDeLaCarpetaDeEvidencias() {
        AlmacenamientoService servicio = new AlmacenamientoService(carpeta.toString());

        assertThatThrownBy(() -> servicio.leer("../secreto.txt")).isInstanceOf(IllegalArgumentException.class);
    }
}
