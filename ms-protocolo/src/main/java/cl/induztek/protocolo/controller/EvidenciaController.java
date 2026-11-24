package cl.induztek.protocolo.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.induztek.protocolo.dto.ArchivoEvidencia;
import cl.induztek.protocolo.dto.EvidenciaResponse;
import cl.induztek.protocolo.service.EvidenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Evidencias", description = "Fotos que respaldan las pruebas de un protocolo")
public class EvidenciaController {

    private final EvidenciaService evidenciaService;

    @PostMapping(value = "/protocolos/{id}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sube una foto (JPG, PNG o WEBP, máximo 5 MB)")
    public EvidenciaResponse subir(@PathVariable String id, @RequestParam Long usuarioId,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam(required = false) String descripcion) {
        return evidenciaService.subir(id, usuarioId, archivo, descripcion);
    }

    @GetMapping("/protocolos/{id}/evidencias")
    @Operation(summary = "Lista las fotos de un protocolo")
    public ResponseEntity<List<EvidenciaResponse>> listar(@PathVariable String id) {
        return Respuestas.lista(evidenciaService.listar(id));
    }

    @GetMapping("/evidencias/{id}/archivo")
    @Operation(summary = "Descarga la imagen")
    public ResponseEntity<byte[]> archivo(@PathVariable Long id) {
        ArchivoEvidencia archivo = evidenciaService.archivo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(archivo.nombreOriginal()).build().toString())
                .body(archivo.contenido());
    }

    @DeleteMapping("/evidencias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quita una foto de un protocolo en Borrador u Observado")
    public void eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        evidenciaService.eliminar(id, usuarioId);
    }
}
