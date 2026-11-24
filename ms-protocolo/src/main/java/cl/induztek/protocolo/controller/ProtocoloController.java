package cl.induztek.protocolo.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.induztek.protocolo.dto.CambioEstadoRequest;
import cl.induztek.protocolo.dto.IndicadoresResponse;
import cl.induztek.protocolo.dto.MedicionAnteriorResponse;
import cl.induztek.protocolo.dto.ProtocoloDetalleResponse;
import cl.induztek.protocolo.dto.ProtocoloRequest;
import cl.induztek.protocolo.dto.ProtocoloResumenResponse;
import cl.induztek.protocolo.model.EstadoProtocolo;
import cl.induztek.protocolo.service.ProtocoloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/protocolos")
@RequiredArgsConstructor
@Tag(name = "Protocolos", description = "Protocolos de prueba, sus mediciones y el flujo de estados")
public class ProtocoloController {

    private final ProtocoloService protocoloService;

    @GetMapping
    @Operation(summary = "Lista protocolos filtrando por estado, equipo, visita o usuario que registró")
    public ResponseEntity<List<ProtocoloResumenResponse>> listar(@RequestParam(required = false) EstadoProtocolo estado,
            @RequestParam(required = false) Long equipoId, @RequestParam(required = false) Long visitaId,
            @RequestParam(required = false) Long usuarioId) {
        return Respuestas.lista(protocoloService.listar(estado, equipoId, visitaId, usuarioId));
    }

    @GetMapping("/indicadores")
    @Operation(summary = "Cantidad de protocolos por estado y promedio de días hasta la formalización")
    public IndicadoresResponse indicadores() {
        return protocoloService.indicadores();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle del protocolo con pruebas, mediciones, historial y fotos")
    public ProtocoloDetalleResponse detalle(@PathVariable String id) {
        return protocoloService.detalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea el protocolo como borrador o, con registrar=true, lo registra en terreno")
    public ProtocoloDetalleResponse crear(@Valid @RequestBody ProtocoloRequest request) {
        return protocoloService.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica un protocolo en Borrador u Observado")
    public ProtocoloDetalleResponse actualizar(@PathVariable String id, @Valid @RequestBody ProtocoloRequest request) {
        return protocoloService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Formaliza, observa o anula un protocolo")
    public ProtocoloDetalleResponse cambiarEstado(@PathVariable String id,
            @Valid @RequestBody CambioEstadoRequest request) {
        return protocoloService.cambiarEstado(id, request);
    }

    @GetMapping("/equipo/{equipoId}/historial")
    @Operation(summary = "Protocolos vigentes de un equipo, del más reciente al más antiguo")
    public ResponseEntity<List<ProtocoloResumenResponse>> historialEquipo(@PathVariable Long equipoId) {
        return Respuestas.lista(protocoloService.historialEquipo(equipoId));
    }

    @GetMapping("/equipo/{equipoId}/mediciones-anteriores")
    @Operation(summary = "Últimas mediciones del equipo para comparar con la prueba actual")
    public ResponseEntity<List<MedicionAnteriorResponse>> medicionesAnteriores(@PathVariable Long equipoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime antesDe,
            @RequestParam(required = false) String excluir) {
        return Respuestas.lista(protocoloService.medicionesAnteriores(equipoId, antesDe, excluir));
    }
}
