package cl.induztek.protocolo.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cl.induztek.protocolo.dto.ArchivoEvidencia;
import cl.induztek.protocolo.dto.EvidenciaResponse;
import cl.induztek.protocolo.dto.remoto.UsuarioRemoto;
import cl.induztek.protocolo.exception.AccesoDenegadoException;
import cl.induztek.protocolo.exception.RecursoNoEncontradoException;
import cl.induztek.protocolo.exception.SolicitudInvalidaException;
import cl.induztek.protocolo.model.EstadoProtocolo;
import cl.induztek.protocolo.model.Evidencia;
import cl.induztek.protocolo.model.Protocolo;
import cl.induztek.protocolo.repository.EvidenciaRepository;
import cl.induztek.protocolo.repository.ProtocoloRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EvidenciaService {

    static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    static final int MAXIMO_POR_PROTOCOLO = 10;
    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final EvidenciaRepository evidenciaRepository;
    private final ProtocoloRepository protocoloRepository;
    private final AlmacenamientoService almacenamientoService;
    private final Permisos permisos;
    private final Clock reloj;

    @Transactional
    public EvidenciaResponse subir(String protocoloId, Long usuarioId, MultipartFile archivo, String descripcion) {
        Protocolo protocolo = protocolo(protocoloId);
        UsuarioRemoto usuario = permisos.personal(usuarioId);
        if (protocolo.getEstado() == EstadoProtocolo.FORMALIZADO || protocolo.getEstado() == EstadoProtocolo.ANULADO) {
            throw new SolicitudInvalidaException("No se pueden agregar fotos a un protocolo "
                    + protocolo.getEstado().getEtiqueta());
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new SolicitudInvalidaException("Selecciona una imagen");
        }
        String extension = EXTENSIONES.get(archivo.getContentType());
        if (extension == null) {
            throw new SolicitudInvalidaException("Solo se aceptan imágenes JPG, PNG o WEBP");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException("La imagen supera el tamaño máximo de 5 MB");
        }
        if (evidenciaRepository.findByProtocoloIdOrderByFechaHoraAsc(protocoloId).size() >= MAXIMO_POR_PROTOCOLO) {
            throw new SolicitudInvalidaException("El protocolo ya tiene el máximo de " + MAXIMO_POR_PROTOCOLO + " fotos");
        }
        String nombreAlmacenado = UUID.randomUUID() + extension;
        try {
            almacenamientoService.guardar(nombreAlmacenado, archivo.getInputStream());
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer la imagen recibida", ex);
        }
        String original = archivo.getOriginalFilename() == null || archivo.getOriginalFilename().isBlank()
                ? nombreAlmacenado : archivo.getOriginalFilename();
        Evidencia evidencia = Evidencia.builder()
                .protocolo(protocolo)
                .nombreOriginal(original.length() > 150 ? original.substring(0, 150) : original)
                .nombreAlmacenado(nombreAlmacenado)
                .tipoContenido(archivo.getContentType())
                .tamano(archivo.getSize())
                .descripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim())
                .usuarioId(usuario.id())
                .fechaHora(LocalDateTime.now(reloj))
                .build();
        return EvidenciaResponse.desde(evidenciaRepository.save(evidencia));
    }

    @Transactional(readOnly = true)
    public List<EvidenciaResponse> listar(String protocoloId) {
        protocolo(protocoloId);
        return evidenciaRepository.findByProtocoloIdOrderByFechaHoraAsc(protocoloId).stream()
                .map(EvidenciaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ArchivoEvidencia archivo(Long id) {
        Evidencia evidencia = buscar(id);
        return new ArchivoEvidencia(evidencia.getNombreOriginal(), evidencia.getTipoContenido(),
                almacenamientoService.leer(evidencia.getNombreAlmacenado()));
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Evidencia evidencia = buscar(id);
        UsuarioRemoto usuario = permisos.personal(usuarioId);
        if (!evidencia.getProtocolo().getEstado().editable()) {
            throw new SolicitudInvalidaException("Solo se pueden quitar fotos de protocolos en Borrador u Observado");
        }
        if (!usuario.es(Permisos.JEFE_TECNICO) && !evidencia.getUsuarioId().equals(usuarioId)) {
            throw new AccesoDenegadoException("Solo quien subió la foto puede quitarla");
        }
        evidenciaRepository.delete(evidencia);
        almacenamientoService.eliminar(evidencia.getNombreAlmacenado());
    }

    private Protocolo protocolo(String id) {
        return protocoloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el protocolo " + id));
    }

    private Evidencia buscar(Long id) {
        return evidenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la evidencia " + id));
    }
}
