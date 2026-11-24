package cl.induztek.protocolo.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.induztek.protocolo.exception.RecursoNoEncontradoException;

@Service
public class AlmacenamientoService {

    private final Path directorio;

    public AlmacenamientoService(@Value("${app.evidencias.directorio}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    public void guardar(String nombre, InputStream contenido) {
        try {
            Files.createDirectories(directorio);
            Files.copy(contenido, resolver(nombre), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la evidencia", ex);
        }
    }

    public byte[] leer(String nombre) {
        Path archivo = resolver(nombre);
        if (!Files.exists(archivo)) {
            throw new RecursoNoEncontradoException("El archivo de la evidencia ya no existe");
        }
        try {
            return Files.readAllBytes(archivo);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer la evidencia", ex);
        }
    }

    public void eliminar(String nombre) {
        try {
            Files.deleteIfExists(resolver(nombre));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo eliminar la evidencia", ex);
        }
    }

    private Path resolver(String nombre) {
        Path archivo = directorio.resolve(nombre).normalize();
        if (!archivo.startsWith(directorio)) {
            throw new IllegalArgumentException("Nombre de archivo no permitido");
        }
        return archivo;
    }
}
