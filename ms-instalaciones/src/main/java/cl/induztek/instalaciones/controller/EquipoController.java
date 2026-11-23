package cl.induztek.instalaciones.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.induztek.instalaciones.dto.EquipoRequest;
import cl.induztek.instalaciones.dto.EquipoResponse;
import cl.induztek.instalaciones.service.EquipoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/equipos")
@RequiredArgsConstructor
@Tag(name = "Equipos", description = "Equipos eléctricos de cada instalación")
public class EquipoController {

    private final EquipoService equipoService;

    @GetMapping
    @Operation(summary = "Lista equipos filtrando por instalación, cliente o tipo")
    public ResponseEntity<List<EquipoResponse>> listar(@RequestParam(required = false) Long instalacionId,
            @RequestParam(required = false) Long clienteId, @RequestParam(required = false) Long tipoEquipoId) {
        return Respuestas.lista(equipoService.listar(instalacionId, clienteId, tipoEquipoId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un equipo")
    public EquipoResponse obtener(@PathVariable Long id) {
        return equipoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un equipo; el TAG no se repite dentro de la instalación")
    public EquipoResponse crear(@Valid @RequestBody EquipoRequest request) {
        return equipoService.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un equipo")
    public EquipoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EquipoRequest request) {
        return equipoService.actualizar(id, request);
    }
}
