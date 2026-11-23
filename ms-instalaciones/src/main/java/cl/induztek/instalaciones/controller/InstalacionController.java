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

import cl.induztek.instalaciones.dto.EquipoResponse;
import cl.induztek.instalaciones.dto.InstalacionRequest;
import cl.induztek.instalaciones.dto.InstalacionResponse;
import cl.induztek.instalaciones.service.EquipoService;
import cl.induztek.instalaciones.service.InstalacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/instalaciones")
@RequiredArgsConstructor
@Tag(name = "Instalaciones", description = "Instalaciones de los clientes")
public class InstalacionController {

    private final InstalacionService instalacionService;
    private final EquipoService equipoService;

    @GetMapping
    @Operation(summary = "Lista las instalaciones, opcionalmente de un cliente")
    public ResponseEntity<List<InstalacionResponse>> listar(@RequestParam(required = false) Long clienteId) {
        return Respuestas.lista(instalacionService.listar(clienteId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una instalación")
    public InstalacionResponse obtener(@PathVariable Long id) {
        return instalacionService.obtener(id);
    }

    @GetMapping("/{id}/equipos")
    @Operation(summary = "Lista los equipos de una instalación")
    public ResponseEntity<List<EquipoResponse>> equipos(@PathVariable Long id) {
        instalacionService.obtener(id);
        return Respuestas.lista(equipoService.listar(id, null, null));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una instalación validando el cliente en ms-usuarios")
    public InstalacionResponse crear(@Valid @RequestBody InstalacionRequest request) {
        return instalacionService.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una instalación")
    public InstalacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody InstalacionRequest request) {
        return instalacionService.actualizar(id, request);
    }
}
