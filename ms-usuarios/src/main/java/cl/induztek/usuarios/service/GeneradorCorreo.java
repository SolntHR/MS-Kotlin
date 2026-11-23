package cl.induztek.usuarios.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cl.induztek.usuarios.repository.UsuarioRepository;

@Component
public class GeneradorCorreo {

    private final UsuarioRepository usuarioRepository;
    private final String dominio;

    public GeneradorCorreo(UsuarioRepository usuarioRepository,
            @Value("${app.correo.dominio:induztek.ejemplo}") String dominio) {
        this.usuarioRepository = usuarioRepository;
        this.dominio = dominio;
    }

    public String generar(String nombreCompleto) {
        String base = base(nombreCompleto);
        String correo = base + "@" + dominio;
        int numero = 2;
        while (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            correo = base + numero + "@" + dominio;
            numero++;
        }
        return correo;
    }

    static String base(String nombreCompleto) {
        List<String> partes = Arrays.stream(limpiar(nombreCompleto).split(" "))
                .filter(parte -> !parte.isBlank())
                .toList();
        if (partes.isEmpty()) {
            return "usuario";
        }
        if (partes.size() == 1) {
            return partes.get(0);
        }
        String apellido = partes.get(Math.max(1, partes.size() - 2));
        return partes.get(0).charAt(0) + apellido;
    }

    private static String limpiar(String texto) {
        String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", "").trim();
    }
}
