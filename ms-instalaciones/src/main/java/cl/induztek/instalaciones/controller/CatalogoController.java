package cl.induztek.instalaciones.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.induztek.instalaciones.dto.ComunaResponse;
import cl.induztek.instalaciones.dto.RegionResponse;
import cl.induztek.instalaciones.dto.TipoEquipoResponse;
import cl.induztek.instalaciones.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Catálogos", description = "Regiones, comunas y tipos de equipo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/regiones")
    @Operation(summary = "Lista las regiones")
    public ResponseEntity<List<RegionResponse>> regiones() {
        return Respuestas.lista(catalogoService.regiones());
    }

    @GetMapping("/regiones/{id}/comunas")
    @Operation(summary = "Lista las comunas de una región")
    public ResponseEntity<List<ComunaResponse>> comunasDeRegion(@PathVariable Long id) {
        return Respuestas.lista(catalogoService.comunas(id));
    }

    @GetMapping("/comunas")
    @Operation(summary = "Lista las comunas, opcionalmente filtradas por región")
    public ResponseEntity<List<ComunaResponse>> comunas(@RequestParam(required = false) Long regionId) {
        return Respuestas.lista(catalogoService.comunas(regionId));
    }

    @GetMapping("/tipos-equipo")
    @Operation(summary = "Lista los tipos de equipo")
    public ResponseEntity<List<TipoEquipoResponse>> tiposEquipo() {
        return Respuestas.lista(catalogoService.tiposEquipo());
    }

    @GetMapping("/tipos-equipo/{id}")
    @Operation(summary = "Obtiene un tipo de equipo")
    public TipoEquipoResponse tipoEquipo(@PathVariable Long id) {
        return catalogoService.tipoEquipo(id);
    }
}
