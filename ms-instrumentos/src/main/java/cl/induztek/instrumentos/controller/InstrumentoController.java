package cl.induztek.instrumentos.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
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

import cl.induztek.instrumentos.dto.CalibracionRequest;
import cl.induztek.instrumentos.dto.CalibracionResponse;
import cl.induztek.instrumentos.dto.InstrumentoRequest;
import cl.induztek.instrumentos.dto.InstrumentoResponse;
import cl.induztek.instrumentos.dto.VigenciaResponse;
import cl.induztek.instrumentos.model.EstadoCalibracion;
import cl.induztek.instrumentos.service.InstrumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/instrumentos")
@RequiredArgsConstructor
@Tag(name = "Instrumentos", description = "Instrumentos de medición y sus calibraciones")
public class InstrumentoController {

    private final InstrumentoService instrumentoService;

    @GetMapping
    @Operation(summary = "Lista los instrumentos con su última calibración, del más próximo a vencer al más lejano")
    public ResponseEntity<List<InstrumentoResponse>> listar(@RequestParam(required = false) EstadoCalibracion estado) {
        return Respuestas.lista(instrumentoService.listar(estado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un instrumento")
    public InstrumentoResponse obtener(@PathVariable Long id) {
        return instrumentoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un instrumento")
    public InstrumentoResponse crear(@Valid @RequestBody InstrumentoRequest request) {
        return instrumentoService.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un instrumento")
    public InstrumentoResponse actualizar(@PathVariable Long id, @Valid @RequestBody InstrumentoRequest request) {
        return instrumentoService.actualizar(id, request);
    }

    @GetMapping("/{id}/calibraciones")
    @Operation(summary = "Historial de calibraciones del instrumento")
    public ResponseEntity<List<CalibracionResponse>> calibraciones(@PathVariable Long id) {
        return Respuestas.lista(instrumentoService.calibraciones(id));
    }

    @PostMapping("/{id}/calibraciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una nueva calibración")
    public CalibracionResponse registrarCalibracion(@PathVariable Long id,
            @Valid @RequestBody CalibracionRequest request) {
        return instrumentoService.registrarCalibracion(id, request);
    }

    @GetMapping("/{id}/vigencia")
    @Operation(summary = "Indica si el instrumento tenía calibración vigente en una fecha (por defecto hoy)")
    public VigenciaResponse vigencia(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return instrumentoService.vigencia(id, fecha);
    }
}
