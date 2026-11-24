package cl.induztek.protocolo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.induztek.protocolo.dto.TipoPruebaResponse;
import cl.induztek.protocolo.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tipos de prueba", description = "Pruebas eléctricas y sus parámetros")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/tipos-prueba")
    @Operation(summary = "Lista los tipos de prueba; con tipoEquipoId devuelve solo los que aplican a ese equipo")
    public ResponseEntity<List<TipoPruebaResponse>> tiposPrueba(@RequestParam(required = false) Long tipoEquipoId) {
        return Respuestas.lista(catalogoService.tiposPrueba(tipoEquipoId));
    }
}
