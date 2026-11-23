package cl.induztek.usuarios.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class HashService {

    private final SecureRandom aleatorio = new SecureRandom();
    private final String salSimulada = generarSal();
    private final String hashSimulado = hash(generarSal(), salSimulada);

    public String generarSal() {
        byte[] bytes = new byte[16];
        aleatorio.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String hash(String valor, String sal) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] resultado = digest.digest((sal + valor).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resultado);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    public String hashCliente(String correo, String contrasena) {
        String sal = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        return hash(contrasena, sal);
    }

    public boolean coincide(String valor, String sal, String hashGuardado) {
        if (valor == null || sal == null || hashGuardado == null) {
            return false;
        }
        byte[] calculado = hash(valor, sal).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(calculado, hashGuardado.getBytes(StandardCharsets.UTF_8));
    }

    public void simularVerificacion(String valor) {
        coincide(valor == null ? "" : valor, salSimulada, hashSimulado);
    }

    public String normalizarRespuesta(String respuesta) {
        return respuesta == null ? "" : respuesta.trim().toLowerCase(Locale.ROOT);
    }

    public String generarCodigo() {
        return String.format("%06d", aleatorio.nextInt(1_000_000));
    }
}
