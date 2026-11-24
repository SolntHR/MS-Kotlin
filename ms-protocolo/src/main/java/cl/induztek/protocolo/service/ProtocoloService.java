package cl.induztek.protocolo.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.protocolo.client.InstalacionClient;
import cl.induztek.protocolo.client.InstrumentoClient;
import cl.induztek.protocolo.client.UsuarioClient;
import cl.induztek.protocolo.dto.CambioEstadoRequest;
import cl.induztek.protocolo.dto.EvidenciaResponse;
import cl.induztek.protocolo.dto.HistorialResponse;
import cl.induztek.protocolo.dto.IndicadoresResponse;
import cl.induztek.protocolo.dto.MedicionAnteriorResponse;
import cl.induztek.protocolo.dto.MedicionRequest;
import cl.induztek.protocolo.dto.ProtocoloDetalleResponse;
import cl.induztek.protocolo.dto.ProtocoloRequest;
import cl.induztek.protocolo.dto.ProtocoloResumenResponse;
import cl.induztek.protocolo.dto.PruebaRequest;
import cl.induztek.protocolo.dto.PruebaResponse;
import cl.induztek.protocolo.dto.remoto.EquipoRemoto;
import cl.induztek.protocolo.dto.remoto.InstalacionRemota;
import cl.induztek.protocolo.dto.remoto.UsuarioRemoto;
import cl.induztek.protocolo.exception.AccesoDenegadoException;
import cl.induztek.protocolo.exception.ConflictoException;
import cl.induztek.protocolo.exception.RecursoNoEncontradoException;
import cl.induztek.protocolo.exception.RegistroIncompletoException;
import cl.induztek.protocolo.exception.SolicitudInvalidaException;
import cl.induztek.protocolo.model.EstadoProtocolo;
import cl.induztek.protocolo.model.HistorialEstado;
import cl.induztek.protocolo.model.Medicion;
import cl.induztek.protocolo.model.Parametro;
import cl.induztek.protocolo.model.Protocolo;
import cl.induztek.protocolo.model.Prueba;
import cl.induztek.protocolo.model.Resultado;
import cl.induztek.protocolo.model.TipoPrueba;
import cl.induztek.protocolo.model.TipoVisita;
import cl.induztek.protocolo.model.Visita;
import cl.induztek.protocolo.repository.EvidenciaRepository;
import cl.induztek.protocolo.repository.HistorialEstadoRepository;
import cl.induztek.protocolo.repository.ParametroRepository;
import cl.induztek.protocolo.repository.ProtocoloRepository;
import cl.induztek.protocolo.repository.TipoEquipoPruebaRepository;
import cl.induztek.protocolo.repository.TipoPruebaRepository;
import cl.induztek.protocolo.repository.VisitaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProtocoloService {

    private static final Set<EstadoProtocolo> VIGENTES =
            EnumSet.of(EstadoProtocolo.REGISTRADO_TERRENO, EstadoProtocolo.OBSERVADO, EstadoProtocolo.FORMALIZADO);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ProtocoloRepository protocoloRepository;
    private final VisitaRepository visitaRepository;
    private final TipoPruebaRepository tipoPruebaRepository;
    private final ParametroRepository parametroRepository;
    private final TipoEquipoPruebaRepository tipoEquipoPruebaRepository;
    private final HistorialEstadoRepository historialEstadoRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final UsuarioClient usuarioClient;
    private final InstalacionClient instalacionClient;
    private final InstrumentoClient instrumentoClient;
    private final Permisos permisos;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public List<ProtocoloResumenResponse> listar(EstadoProtocolo estado, Long equipoId, Long visitaId,
            Long usuarioId) {
        List<Protocolo> protocolos = protocoloRepository.buscar(estado, equipoId, visitaId, usuarioId);
        if (protocolos.isEmpty()) {
            return List.of();
        }
        Map<Long, EquipoRemoto> equipos = instalacionClient.equipos();
        Map<Long, InstalacionRemota> instalaciones = instalacionClient.instalaciones();
        Map<Long, String> nombres = usuarioClient.nombres();
        return protocolos.stream().map(protocolo -> resumen(protocolo, equipos.get(protocolo.getEquipoId()),
                instalaciones.get(protocolo.getVisita().getInstalacionId()), nombres)).toList();
    }

    @Transactional(readOnly = true)
    public ProtocoloDetalleResponse detalle(String id) {
        Protocolo protocolo = buscar(id);
        Map<Long, EquipoRemoto> equipos = instalacionClient.equipos();
        Map<Long, InstalacionRemota> instalaciones = instalacionClient.instalaciones();
        Map<Long, String> nombres = usuarioClient.nombres();
        EquipoRemoto equipo = equipos.get(protocolo.getEquipoId());
        InstalacionRemota instalacion = instalaciones.get(protocolo.getVisita().getInstalacionId());
        List<HistorialResponse> historial = protocolo.getHistorial().stream()
                .map(h -> new HistorialResponse(h.getEstadoAnterior() == null ? null : h.getEstadoAnterior().name(),
                        h.getEstadoNuevo().name(), h.getUsuarioId(), nombres.get(h.getUsuarioId()), h.getFechaHora(),
                        h.getMotivo()))
                .toList();
        List<EvidenciaResponse> evidencias = evidenciaRepository.findByProtocoloIdOrderByFechaHoraAsc(id).stream()
                .map(EvidenciaResponse::desde).toList();
        return new ProtocoloDetalleResponse(protocolo.getId(), protocolo.getNumero(), protocolo.getVisita().getId(),
                protocolo.getVisita().getNumeroOt(), protocolo.getVisita().getTipoVisita().name(),
                protocolo.getEquipoId(), equipo == null ? null : equipo.tag(),
                equipo == null ? null : equipo.tipoEquipoNombre(), instalacion == null ? null : instalacion.nombre(),
                instalacion == null ? null : instalacion.clienteNombre(), protocolo.getUsuarioRegistraId(),
                nombres.get(protocolo.getUsuarioRegistraId()), protocolo.getFechaHoraPrueba(),
                protocolo.getTemperaturaC(), protocolo.getHumedadPct(), protocolo.getDescripcionMantenimiento(),
                protocolo.getObservaciones(), protocolo.getEstado().name(), protocolo.getEstado().getEtiqueta(),
                protocolo.getProtocoloReemplazadoId(),
                protocolo.getPruebas().stream().map(PruebaResponse::desde).toList(), historial, evidencias);
    }

    @Transactional
    public ProtocoloDetalleResponse crear(ProtocoloRequest request) {
        UsuarioRemoto usuario = permisos.personal(request.usuarioId());
        Visita visita = visitaRepository.findById(request.visitaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la visita " + request.visitaId()));
        if (visita.estaCerrada()) {
            throw new SolicitudInvalidaException("La visita ya está cerrada");
        }
        if (!usuario.es(Permisos.JEFE_TECNICO) && !visita.participa(usuario.id())) {
            throw new AccesoDenegadoException("La visita no está asignada a tu usuario");
        }
        EquipoRemoto equipo = instalacionClient.equipo(request.equipoId());
        if (!visita.getInstalacionId().equals(equipo.instalacionId())) {
            throw new SolicitudInvalidaException("El equipo no pertenece a la instalación de la visita");
        }
        if (protocoloRepository.existsByVisitaIdAndEquipoId(visita.getId(), equipo.id())) {
            throw new ConflictoException("Ya existe un protocolo para " + equipo.tag() + " en esta visita");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String id = UUID.randomUUID().toString();
        Protocolo protocolo = Protocolo.builder()
                .id(id)
                .numero(generarNumero(id, ahora))
                .visita(visita)
                .equipoId(equipo.id())
                .usuarioRegistraId(usuario.id())
                .fechaHoraPrueba(ahora)
                .build();
        protocolo.registrarCambio(EstadoProtocolo.BORRADOR, usuario.id(), ahora, "Creación del protocolo");
        guardarContenido(protocolo, request, equipo, usuario.id(), ahora);
        return detalle(protocoloRepository.save(protocolo).getId());
    }

    @Transactional
    public ProtocoloDetalleResponse actualizar(String id, ProtocoloRequest request) {
        Protocolo protocolo = buscar(id);
        permisos.personal(request.usuarioId());
        if (!protocolo.getUsuarioRegistraId().equals(request.usuarioId())) {
            throw new AccesoDenegadoException("Solo quien creó el protocolo puede modificarlo");
        }
        if (!protocolo.getEstado().editable()) {
            throw new SolicitudInvalidaException("Solo se pueden modificar protocolos en Borrador u Observado");
        }
        if (protocolo.getVisita().estaCerrada()) {
            throw new SolicitudInvalidaException("La visita ya está cerrada");
        }
        EquipoRemoto equipo = instalacionClient.equipo(protocolo.getEquipoId());
        guardarContenido(protocolo, request, equipo, request.usuarioId(), LocalDateTime.now(reloj));
        return detalle(protocoloRepository.save(protocolo).getId());
    }

    @Transactional
    public ProtocoloDetalleResponse cambiarEstado(String id, CambioEstadoRequest request) {
        Protocolo protocolo = buscar(id);
        permisos.revisor(request.usuarioId());
        EstadoProtocolo actual = protocolo.getEstado();
        EstadoProtocolo nuevo = request.estado();
        switch (nuevo) {
            case FORMALIZADO, OBSERVADO -> {
                if (actual != EstadoProtocolo.REGISTRADO_TERRENO) {
                    throw new SolicitudInvalidaException("Solo se puede revisar un protocolo Registrado en terreno");
                }
            }
            case ANULADO -> {
                if (actual == EstadoProtocolo.FORMALIZADO || actual == EstadoProtocolo.ANULADO) {
                    throw new SolicitudInvalidaException("Un protocolo " + actual.getEtiqueta() + " no se puede anular");
                }
            }
            default -> throw new SolicitudInvalidaException("Transición no permitida");
        }
        String motivo = request.motivo() == null || request.motivo().isBlank() ? null : request.motivo().trim();
        if ((nuevo == EstadoProtocolo.OBSERVADO || nuevo == EstadoProtocolo.ANULADO) && motivo == null) {
            throw new RegistroIncompletoException(Map.of("motivo", "Indica el motivo"));
        }
        protocolo.registrarCambio(nuevo, request.usuarioId(), LocalDateTime.now(reloj), motivo);
        return detalle(protocoloRepository.save(protocolo).getId());
    }

    @Transactional(readOnly = true)
    public List<ProtocoloResumenResponse> historialEquipo(Long equipoId) {
        List<Protocolo> protocolos = protocoloRepository.findByEquipoIdAndEstadoInOrderByFechaHoraPruebaDesc(equipoId,
                VIGENTES);
        if (protocolos.isEmpty()) {
            return List.of();
        }
        Set<String> reemplazados = new HashSet<>();
        protocolos.stream().map(Protocolo::getProtocoloReemplazadoId).filter(r -> r != null).forEach(reemplazados::add);
        Map<Long, EquipoRemoto> equipos = instalacionClient.equipos();
        Map<Long, InstalacionRemota> instalaciones = instalacionClient.instalaciones();
        Map<Long, String> nombres = usuarioClient.nombres();
        return protocolos.stream().filter(protocolo -> !reemplazados.contains(protocolo.getId()))
                .map(protocolo -> resumen(protocolo, equipos.get(equipoId),
                        instalaciones.get(protocolo.getVisita().getInstalacionId()), nombres))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicionAnteriorResponse> medicionesAnteriores(Long equipoId, LocalDateTime antesDe,
            String excluir) {
        LocalDateTime limite = antesDe == null ? LocalDateTime.now(reloj) : antesDe;
        List<Protocolo> anteriores = protocoloRepository
                .findByEquipoIdAndEstadoInOrderByFechaHoraPruebaDesc(equipoId, VIGENTES).stream()
                .filter(protocolo -> !protocolo.getId().equals(excluir))
                .filter(protocolo -> protocolo.getFechaHoraPrueba().isBefore(limite))
                .toList();
        Set<String> reemplazados = new HashSet<>();
        anteriores.stream().map(Protocolo::getProtocoloReemplazadoId).filter(r -> r != null).forEach(reemplazados::add);
        Map<String, String> protocoloPorClave = new LinkedHashMap<>();
        List<MedicionAnteriorResponse> resultado = new ArrayList<>();
        for (Protocolo protocolo : anteriores) {
            if (reemplazados.contains(protocolo.getId())) {
                continue;
            }
            for (Prueba prueba : protocolo.getPruebas()) {
                if (!prueba.isRealizada()) {
                    continue;
                }
                String clave = prueba.getTipoPrueba().getId() + "|" + prueba.getElemento();
                String primero = protocoloPorClave.putIfAbsent(clave, protocolo.getId());
                if (primero != null && !primero.equals(protocolo.getId())) {
                    continue;
                }
                for (Medicion medicion : prueba.getMediciones()) {
                    resultado.add(new MedicionAnteriorResponse(prueba.getTipoPrueba().getId(), prueba.getElemento(),
                            protocolo.getNumero(), protocolo.getFechaHoraPrueba(),
                            medicion.getParametro().getCodigo(), medicion.getValor(), medicion.getUnidad()));
                }
            }
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public IndicadoresResponse indicadores() {
        Map<String, Long> porEstado = new LinkedHashMap<>();
        long total = 0;
        for (EstadoProtocolo estado : EstadoProtocolo.values()) {
            long cantidad = protocoloRepository.countByEstado(estado);
            porEstado.put(estado.name(), cantidad);
            total += cantidad;
        }
        List<HistorialEstado> formalizaciones = historialEstadoRepository.findByEstadoNuevo(EstadoProtocolo.FORMALIZADO);
        Double promedio = formalizaciones.isEmpty() ? null : Math.round(formalizaciones.stream()
                .mapToDouble(h -> Duration.between(h.getProtocolo().getFechaHoraPrueba(), h.getFechaHora()).toMinutes()
                        / 1440.0)
                .average().orElse(0) * 10) / 10.0;
        return new IndicadoresResponse(total, porEstado, promedio);
    }

    Protocolo buscar(String id) {
        return protocoloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el protocolo " + id));
    }

    private void guardarContenido(Protocolo protocolo, ProtocoloRequest request, EquipoRemoto equipo, Long usuarioId,
            LocalDateTime ahora) {
        protocolo.setTemperaturaC(request.temperaturaC());
        protocolo.setHumedadPct(request.humedadPct());
        protocolo.setDescripcionMantenimiento(limpiar(request.descripcionMantenimiento()));
        protocolo.setObservaciones(limpiar(request.observaciones()));
        protocolo.getPruebas().clear();
        List<PruebaRequest> pruebas = request.pruebasOVacio();
        for (int i = 0; i < pruebas.size(); i++) {
            protocolo.agregarPrueba(construirPrueba(pruebas.get(i), i, equipo));
        }
        if (request.esRegistro()) {
            validarCompleto(protocolo, request);
            EstadoProtocolo anterior = protocolo.getEstado();
            protocolo.registrarCambio(EstadoProtocolo.REGISTRADO_TERRENO, usuarioId, ahora,
                    anterior == EstadoProtocolo.OBSERVADO ? "Corregido por el técnico" : null);
        }
    }

    private Prueba construirPrueba(PruebaRequest request, int indice, EquipoRemoto equipo) {
        TipoPrueba tipo = tipoPruebaRepository.findById(request.tipoPruebaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tipo de prueba " + request.tipoPruebaId()));
        if (!tipoEquipoPruebaRepository.existsByTipoEquipoIdAndTipoPruebaId(equipo.tipoEquipoId(), tipo.getId())) {
            throw new SolicitudInvalidaException(
                    "La prueba " + tipo.getNombre() + " no corresponde a un " + equipo.tipoEquipoNombre());
        }
        Prueba prueba = Prueba.builder()
                .tipoPrueba(tipo)
                .elemento(request.elemento().trim())
                .momento(limpiar(request.momento()))
                .realizada(request.fueRealizada())
                .motivoNoRealizada(request.fueRealizada() ? null : limpiar(request.motivoNoRealizada()))
                .instrumentoId(request.fueRealizada() ? request.instrumentoId() : null)
                .resultado(request.fueRealizada() ? request.resultado() : null)
                .comentario(limpiar(request.comentario()))
                .ingresadoManualmente(request.esManual())
                .build();
        if (!request.fueRealizada()) {
            return prueba;
        }
        Set<Long> usados = new HashSet<>();
        int orden = 1;
        for (MedicionRequest medicion : request.medicionesOVacio()) {
            Parametro parametro = parametroRepository.findById(medicion.parametroId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el parámetro " + medicion.parametroId()));
            if (!parametro.getTipoPrueba().getId().equals(tipo.getId())) {
                throw new SolicitudInvalidaException("pruebas[" + indice + "]: el parámetro " + parametro.getNombre()
                        + " no pertenece a " + tipo.getNombre());
            }
            if (!usados.add(parametro.getId())) {
                throw new SolicitudInvalidaException("pruebas[" + indice + "]: el parámetro " + parametro.getNombre()
                        + " está repetido");
            }
            prueba.agregarMedicion(Medicion.builder().parametro(parametro).valor(medicion.valor())
                    .unidad(parametro.getUnidad()).orden(orden++).build());
        }
        return prueba;
    }

    private void validarCompleto(Protocolo protocolo, ProtocoloRequest request) {
        Map<String, String> faltantes = new LinkedHashMap<>();
        if (request.temperaturaC() == null) {
            faltantes.put("temperaturaC", "Ingresa la temperatura ambiente");
        }
        if (request.humedadPct() == null) {
            faltantes.put("humedadPct", "Ingresa la humedad relativa");
        }
        if (protocolo.getVisita().getTipoVisita() == TipoVisita.PRUEBAS_Y_MANT
                && protocolo.getDescripcionMantenimiento() == null) {
            faltantes.put("descripcionMantenimiento", "Describe el mantenimiento realizado");
        }
        List<Prueba> pruebas = protocolo.getPruebas();
        if (pruebas.stream().noneMatch(Prueba::isRealizada)) {
            faltantes.put("pruebas", "Registra al menos una prueba realizada");
        }
        LocalDateTime fecha = protocolo.getFechaHoraPrueba();
        for (int i = 0; i < pruebas.size(); i++) {
            Prueba prueba = pruebas.get(i);
            String base = "pruebas[" + i + "].";
            if (!prueba.isRealizada()) {
                if (prueba.getMotivoNoRealizada() == null) {
                    faltantes.put(base + "motivoNoRealizada", "Indica por qué no se realizó la prueba");
                }
                continue;
            }
            if (prueba.getInstrumentoId() == null) {
                faltantes.put(base + "instrumentoId", "Selecciona el instrumento");
            } else if (!instrumentoClient.vigencia(prueba.getInstrumentoId(), fecha.toLocalDate()).vigente()) {
                faltantes.put(base + "instrumentoId",
                        "El instrumento no tenía calibración vigente el " + fecha.format(FECHA));
            }
            if (prueba.getResultado() == null) {
                faltantes.put(base + "resultado", "Selecciona el resultado");
            } else if (prueba.getResultado() != Resultado.APROBADO && prueba.getComentario() == null) {
                faltantes.put(base + "comentario", "Explica por qué el resultado no es Aprobado");
            }
            if (prueba.getMediciones().isEmpty()) {
                faltantes.put(base + "mediciones", "Ingresa las mediciones de la prueba");
            }
        }
        if (!faltantes.isEmpty()) {
            throw new RegistroIncompletoException(faltantes);
        }
    }

    private String generarNumero(String id, LocalDateTime ahora) {
        String candidato = id.replace("-", "");
        for (int intento = 0; intento < 5; intento++) {
            String numero = "PT-" + ahora.getYear() + "-" + candidato.substring(0, 8).toUpperCase(Locale.ROOT);
            if (!protocoloRepository.existsByNumero(numero)) {
                return numero;
            }
            candidato = UUID.randomUUID().toString().replace("-", "");
        }
        throw new ConflictoException("No se pudo generar un número de protocolo único");
    }

    private ProtocoloResumenResponse resumen(Protocolo protocolo, EquipoRemoto equipo, InstalacionRemota instalacion,
            Map<Long, String> nombres) {
        List<Prueba> realizadas = protocolo.getPruebas().stream().filter(Prueba::isRealizada).toList();
        String resultado = realizadas.stream().map(Prueba::getResultado).filter(r -> r != null)
                .max(Comparator.naturalOrder()).map(Resultado::name).orElse(null);
        return new ProtocoloResumenResponse(protocolo.getId(), protocolo.getNumero(), protocolo.getVisita().getId(),
                protocolo.getVisita().getNumeroOt(), protocolo.getEquipoId(), equipo == null ? null : equipo.tag(),
                equipo == null ? null : equipo.tipoEquipoNombre(),
                instalacion == null ? null : instalacion.nombre(), instalacion == null ? null : instalacion.clienteNombre(),
                protocolo.getUsuarioRegistraId(), nombres.get(protocolo.getUsuarioRegistraId()),
                protocolo.getFechaHoraPrueba(), protocolo.getEstado().name(), protocolo.getEstado().getEtiqueta(),
                resultado, lectura(realizadas, "LECTURA_1MIN"), lectura(realizadas, "LECTURA_10MIN"),
                protocolo.getProtocoloReemplazadoId());
    }

    private Double lectura(List<Prueba> pruebas, String codigo) {
        return pruebas.stream().flatMap(prueba -> prueba.getMediciones().stream())
                .filter(medicion -> codigo.equals(medicion.getParametro().getCodigo()))
                .map(Medicion::getValor).findFirst().orElse(null);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
