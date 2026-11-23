package cl.induztek.usuarios.model;

import java.util.List;

public final class PreguntasSeguridad {

    public static final List<String> LISTA = List.of(
            "¿Cuál es el nombre de tu primera mascota?",
            "¿En qué ciudad nacieron tus padres?",
            "¿Cuál es tu comida favorita?",
            "¿Cuál fue el modelo de tu primer auto?",
            "¿Cuál es el nombre de tu mejor amigo de la infancia?");

    private PreguntasSeguridad() {
    }

    public static boolean esValida(String pregunta) {
        return pregunta != null && LISTA.contains(pregunta.trim());
    }
}
