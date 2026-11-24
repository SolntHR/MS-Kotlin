package cl.induztek.protocolo.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.protocolo.client.InstalacionClient;
import cl.induztek.protocolo.client.UsuarioClient;
import cl.induztek.protocolo.dto.ActaResponse;
import cl.induztek.protocolo.dto.ConteoProtocolos;
import cl.induztek.protocolo.dto.ParticipanteResponse;
import cl.induztek.protocolo.dto.RecepcionRequest;
import cl.induztek.protocolo.dto.RecepcionResponse;
import cl.induztek.protocolo.dto.VisitaRequest;
import cl.induztek.protocolo.dto.VisitaResponse;
import cl.induztek.protocolo.dto.remoto.EquipoRemoto;
import cl.induztek.protocolo.dto.remoto.InstalacionRemota;
import cl.induztek.protocolo.dto.remoto.UsuarioRemoto;
import cl.induztek.protocolo.exception.AccesoDenegadoException;
import cl.induztek.protocolo.exception.ConflictoException;
import cl.induztek.protocolo.exception.RecursoNoEncontradoException;
import cl.induztek.protocolo.exception.SolicitudInvalidaException;
import cl.induztek.protocolo.model.EstadoProtocolo;
import cl.induztek.protocolo.model.Protocolo;
import cl.induztek.protocolo.model.RecepcionActa;
import cl.induztek.protocolo.model.Visita;
import cl.induztek.protocolo.repository.ProtocoloRepository;
import cl.induztek.protocolo.repository.VisitaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VisitaService {

    static final String FECHA_PASADA = "La fecha de la visita no puede ser anterior a hoy";

    private final VisitaRepository visitaRepository;
    private final ProtocoloRepository protocoloRepository;
    private final UsuarioClient usuarioClient;
    private final InstalacionClient instalacionClient;
    private final Permisos permisos;
    private final ProtocoloService protocoloService;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public List<VisitaResponse> listar(Long usuarioId, Boolean abiertas) {
        UsuarioRemoto usuario = permisos.activo(usuarioId);
        Map<Long, InstalacionRemota> instalaciones = instalacionClient.instalaciones();
        List<Visita> visitas;
        if (usuario.es(Permisos.JEFE_TECNICO)) {
            visitas = visitaRepository.todas();
        } else if (usuario.es(Permisos.CLIENTE)) {
            List<Long> propias = instalaciones.values().stream()
                    .filter(instalacion -> usuarioId.equals(instalacion.clienteId()))
                    .map(InstalacionRemota::id).toList();
            visitas = propias.isEmpty() ? List.of() : visitaRepository.deInstalaciones(propias).stream()
                    .filter(Visita::estaCerrada).toList();
        } else {
            visitas = visitaRepository.deUsuario(usuarioId);
        }
        Map<Long, List<ConteoProtocolos>> conteos = protocoloRepository.contarPorVisita().stream()
                .collect(Collectors.groupingBy(ConteoProtocolos::visitaId));
        Map<Long, String> nombres = usuarioClient.nombres();
        return visitas.stream()
                .filter(visita -> abiertas == null || abiertas != visita.estaCerrada())
                .map(visita -> respuesta(visita, instalaciones.get(visita.getInstalacionId()), nombres,
                        conteos.getOrDefault(visita.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public VisitaResponse obtener(Long id) {
        return respuesta(buscar(id));
    }

    @Transactional
    public VisitaResponse crear(VisitaRequest request) {
        permisos.revisor(request.usuarioId());
        if (request.fecha().isBefore(LocalDate.now(reloj))) {
            throw new SolicitudInvalidaException(FECHA_PASADA);
        }
        String numeroOt = request.numeroOt().trim().toUpperCase();
        if (visitaRepository.existsByNumeroOtIgnoreCase(numeroOt)) {
            throw new ConflictoException("Ya existe una visita con la OT " + numeroOt);
        }
        Visita visita = Visita.builder().build();
        aplicar(visita, numeroOt, request);
        return respuesta(visitaRepository.save(visita));
    }

    @Transactional
    public VisitaResponse actualizar(Long id, VisitaRequest request) {
        permisos.revisor(request.usuarioId());
        Visita visita = buscar(id);
        if (visita.estaCerrada()) {
            throw new SolicitudInvalidaException("La visita ya está cerrada y no se puede editar");
        }
        if (!request.fecha().equals(visita.getFecha()) && request.fecha().isBefore(LocalDate.now(reloj))) {
            throw new SolicitudInvalidaException(FECHA_PASADA);
        }
        String numeroOt = request.numeroOt().trim().toUpperCase();
        if (!numeroOt.equalsIgnoreCase(visita.getNumeroOt()) && visitaRepository.existsByNumeroOtIgnoreCase(numeroOt)) {
            throw new ConflictoException("Ya existe una visita con la OT " + numeroOt);
        }
        if (!request.instalacionId().equals(visita.getInstalacionId()) && tieneProtocolos(id)) {
            throw new SolicitudInvalidaException("No se puede cambiar la instalación: la visita ya tiene protocolos");
        }
        aplicar(visita, numeroOt, request);
        return respuesta(visitaRepository.save(visita));
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        permisos.revisor(usuarioId);
        Visita visita = buscar(id);
        if (visita.estaCerrada() || !visita.getFecha().isAfter(LocalDate.now(reloj))) {
            throw new SolicitudInvalidaException("Solo se pueden eliminar visitas programadas (con fecha futura)");
        }
        if (tieneProtocolos(id)) {
            throw new SolicitudInvalidaException("La visita tiene protocolos y no se puede eliminar");
        }
        visitaRepository.delete(visita);
    }

    private boolean tieneProtocolos(Long visitaId) {
        return !protocoloRepository.findByVisitaIdOrderByFechaHoraPruebaAsc(visitaId).isEmpty();
    }

    private void aplicar(Visita visita, String numeroOt, VisitaRequest request) {
        instalacionClient.instalacion(request.instalacionId());
        UsuarioRemoto ingeniero = permisos.activo(request.ingenieroCargoId());
        if (!ingeniero.es(Permisos.INGENIERO, Permisos.JEFE_TECNICO)) {
            throw new SolicitudInvalidaException("El encargado de la visita debe ser Ingeniero o Jefe técnico");
        }
        Set<Long> participantes = new LinkedHashSet<>(request.participantes());
        for (Long participanteId : participantes) {
            UsuarioRemoto participante = permisos.activo(participanteId);
            if (!participante.es(Permisos.TECNICO, Permisos.INGENIERO, Permisos.JEFE_TECNICO)) {
                throw new SolicitudInvalidaException(participante.nombre() + " no es personal de terreno");
            }
        }
        visita.setNumeroOt(numeroOt);
        visita.setInstalacionId(request.instalacionId());
        visita.setFecha(request.fecha());
        visita.setTipoVisita(request.tipoVisita());
        visita.setIngenieroCargoId(ingeniero.id());
        visita.getParticipantes().clear();
        visita.getParticipantes().addAll(participantes);
    }

    @Transactional(readOnly = true)
    public List<EquipoRemoto> equiposDisponibles(Long visitaId, String protocoloEditando) {
        Visita visita = buscar(visitaId);
        Set<Long> ocupados = protocoloRepository.findByVisitaIdOrderByFechaHoraPruebaAsc(visitaId).stream()
                .filter(protocolo -> !protocolo.getId().equals(protocoloEditando))
                .map(Protocolo::getEquipoId)
                .collect(Collectors.toSet());
        return instalacionClient.equiposDeInstalacion(visita.getInstalacionId()).stream()
                .filter(equipo -> !ocupados.contains(equipo.id()))
                .toList();
    }

    @Transactional
    public VisitaResponse registrarRecepcion(Long id, RecepcionRequest request) {
        Visita visita = buscar(id);
        if (visita.estaCerrada()) {
            throw new SolicitudInvalidaException("El acta de esta visita ya fue recibida");
        }
        List<Protocolo> protocolos = protocoloRepository.findByVisitaIdOrderByFechaHoraPruebaAsc(id);
        if (protocolos.stream().anyMatch(protocolo -> protocolo.getEstado() == EstadoProtocolo.BORRADOR)) {
            throw new SolicitudInvalidaException("Quedan protocolos en borrador. Regístralos o anúlalos antes de cerrar");
        }
        if (protocolos.stream().noneMatch(protocolo -> protocolo.getEstado().vigente())) {
            throw new SolicitudInvalidaException("La visita no tiene protocolos registrados");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        visita.setRecepcion(RecepcionActa.builder()
                .nombre(request.receptorNombre().trim())
                .rut(request.receptorRut().trim().toUpperCase())
                .cargo(request.receptorCargo().trim())
                .fechaHora(ahora)
                .build());
        visita.setFechaHoraCierre(ahora);
        return respuesta(visitaRepository.save(visita));
    }

    @Transactional(readOnly = true)
    public ActaResponse acta(Long id, Long usuarioId) {
        Visita visita = buscar(id);
        UsuarioRemoto usuario = permisos.activo(usuarioId);
        if (usuario.es(Permisos.CLIENTE)) {
            InstalacionRemota instalacion = instalacionClient.instalacion(visita.getInstalacionId());
            if (!usuarioId.equals(instalacion.clienteId()) || !visita.estaCerrada()) {
                throw new AccesoDenegadoException("Esta acta no está disponible para tu cuenta");
            }
        }
        return new ActaResponse(respuesta(visita), RecepcionResponse.desde(visita.getRecepcion()),
                protocoloRepository.findByVisitaIdOrderByFechaHoraPruebaAsc(id).stream()
                        .filter(protocolo -> protocolo.getEstado() != EstadoProtocolo.ANULADO)
                        .map(protocolo -> protocoloService.detalle(protocolo.getId()))
                        .toList());
    }

    Visita buscar(Long id) {
        return visitaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la visita " + id));
    }

    private VisitaResponse respuesta(Visita visita) {
        List<ConteoProtocolos> conteos = protocoloRepository.contarPorVisita().stream()
                .filter(conteo -> conteo.visitaId().equals(visita.getId())).toList();
        return respuesta(visita, instalacionClient.instalaciones().get(visita.getInstalacionId()),
                usuarioClient.nombres(), conteos);
    }

    private VisitaResponse respuesta(Visita visita, InstalacionRemota instalacion, Map<Long, String> nombres,
            List<ConteoProtocolos> conteos) {
        long total = conteos.stream().filter(c -> c.estado() != EstadoProtocolo.ANULADO)
                .mapToLong(ConteoProtocolos::cantidad).sum();
        long registrados = conteos.stream().filter(c -> c.estado().vigente())
                .mapToLong(ConteoProtocolos::cantidad).sum();
        return new VisitaResponse(visita.getId(), visita.getNumeroOt(), visita.getInstalacionId(),
                instalacion == null ? null : instalacion.nombre(),
                instalacion == null ? null : instalacion.clienteId(),
                instalacion == null ? null : instalacion.clienteNombre(),
                visita.getFecha(), visita.getTipoVisita().name(), visita.getTipoVisita().getEtiqueta(),
                visita.getIngenieroCargoId(), nombres.get(visita.getIngenieroCargoId()),
                visita.getParticipantes().stream().map(id -> new ParticipanteResponse(id, nombres.get(id))).toList(),
                visita.estaCerrada(), visita.getFechaHoraCierre(), total, registrados);
    }
}
