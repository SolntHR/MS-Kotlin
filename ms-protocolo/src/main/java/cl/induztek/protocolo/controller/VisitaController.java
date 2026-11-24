package cl.induztek.protocolo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.induztek.protocolo.dto.ActaResponse;
import cl.induztek.protocolo.dto.RecepcionRequest;
import cl.induztek.protocolo.dto.VisitaRequest;
import cl.induztek.protocolo.dto.VisitaResponse;
import cl.induztek.protocolo.dto.remoto.EquipoRemoto;
import cl.induztek.protocolo.service.VisitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/visitas")
@RequiredArgsConstructor
@Tag(name = "Visitas", description = "Órdenes de trabajo en terreno y recepción del acta")
public class VisitaController {

    private final VisitaService visitaService;

    @GetMapping
    @Operation(summary = "Visitas del usuario: todas para el Jefe, las asignadas para el personal y las cerradas para el cliente")
    public ResponseEntity<List<VisitaResponse>> listar(@RequestParam Long usuarioId, @RequestParam(required = false) Boolean abiertas) {
        return Respuestas.lista(visitaService.listar(usuarioId, abiertas));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una visita")
    public VisitaResponse obtener(@PathVariable Long id) {
        return visitaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "El Ingeniero o el Jefe técnico asigna una visita a su equipo de terreno")
    public VisitaResponse crear(@Valid @RequestBody VisitaRequest request) {
        return visitaService.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita una visita programada o en curso (Ingeniero o Jefe técnico)")
    public VisitaResponse actualizar(@PathVariable Long id, @Valid @RequestBody VisitaRequest request) {
        return visitaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una visita programada sin protocolos (Ingeniero o Jefe técnico)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        visitaService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/equipos-disponibles")
    @Operation(summary = "Equipos de la instalación que aún no tienen protocolo en esta visita")
    public ResponseEntity<List<EquipoRemoto>> equiposDisponibles(@PathVariable Long id,
            @RequestParam(required = false) String protocoloId) {
        return Respuestas.lista(visitaService.equiposDisponibles(id, protocoloId));
    }

    @PostMapping("/{id}/recepcion")
    @Operation(summary = "Registra quién recibe el acta y cierra la visita")
    public VisitaResponse registrarRecepcion(@PathVariable Long id, @Valid @RequestBody RecepcionRequest request) {
        return visitaService.registrarRecepcion(id, request);
    }

    @GetMapping("/{id}/acta")
    @Operation(summary = "Acta de la visita con sus protocolos")
    public ActaResponse acta(@PathVariable Long id, @RequestParam Long usuarioId) {
        return visitaService.acta(id, usuarioId);
    }
}
