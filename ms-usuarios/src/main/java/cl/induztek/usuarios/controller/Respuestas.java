package cl.induztek.usuarios.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;

final class Respuestas {

    private Respuestas() {
    }

    static <T> ResponseEntity<List<T>> lista(List<T> datos) {
        return datos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(datos);
    }
}
