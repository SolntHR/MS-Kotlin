package cl.induztek.instrumentos.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.instrumentos.dto.CalibracionRequest;
import cl.induztek.instrumentos.dto.CalibracionResponse;
import cl.induztek.instrumentos.dto.InstrumentoRequest;
import cl.induztek.instrumentos.dto.InstrumentoResponse;
import cl.induztek.instrumentos.dto.VigenciaResponse;
import cl.induztek.instrumentos.exception.ConflictoException;
import cl.induztek.instrumentos.exception.RecursoNoEncontradoException;
import cl.induztek.instrumentos.exception.SolicitudInvalidaException;
import cl.induztek.instrumentos.model.Calibracion;
import cl.induztek.instrumentos.model.EstadoCalibracion;
import cl.induztek.instrumentos.model.Instrumento;
import cl.induztek.instrumentos.repository.CalibracionRepository;
import cl.induztek.instrumentos.repository.InstrumentoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstrumentoService {

    private static final Comparator<InstrumentoResponse> POR_VENCIMIENTO = Comparator.comparing(
            InstrumentoResponse::fechaVencimiento, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final InstrumentoRepository instrumentoRepository;
    private final CalibracionRepository calibracionRepository;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public List<InstrumentoResponse> listar(EstadoCalibracion estado) {
        Map<Long, Calibracion> ultimas = calibracionRepository.ultimasCalibraciones().stream()
                .collect(Collectors.toMap(c -> c.getInstrumento().getId(), Function.identity(),
                        (primera, segunda) -> primera));
        return instrumentoRepository.findAll().stream()
                .map(instrumento -> respuesta(instrumento, ultimas.get(instrumento.getId())))
                .filter(respuesta -> estado == null || respuesta.estado().equals(estado.name()))
                .sorted(POR_VENCIMIENTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstrumentoResponse obtener(Long id) {
        Instrumento instrumento = buscar(id);
        return respuesta(instrumento, calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(id)
                .orElse(null));
    }

    @Transactional
    public InstrumentoResponse crear(InstrumentoRequest request) {
        String serie = request.numeroSerie().trim();
        if (instrumentoRepository.existsByNumeroSerieIgnoreCase(serie)) {
            throw new ConflictoException("Ya existe un instrumento con el número de serie " + serie);
        }
        Instrumento instrumento = Instrumento.builder().numeroSerie(serie).build();
        aplicar(instrumento, request);
        return respuesta(instrumentoRepository.save(instrumento), null);
    }

    @Transactional
    public InstrumentoResponse actualizar(Long id, InstrumentoRequest request) {
        Instrumento instrumento = buscar(id);
        String serie = request.numeroSerie().trim();
        if (instrumentoRepository.existsByNumeroSerieIgnoreCaseAndIdNot(serie, id)) {
            throw new ConflictoException("Ya existe un instrumento con el número de serie " + serie);
        }
        instrumento.setNumeroSerie(serie);
        aplicar(instrumento, request);
        return respuesta(instrumentoRepository.save(instrumento),
                calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(id).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<CalibracionResponse> calibraciones(Long id) {
        buscar(id);
        return calibracionRepository.findByInstrumentoIdOrderByFechaCalibracionDesc(id).stream()
                .map(CalibracionResponse::desde).toList();
    }

    @Transactional
    public CalibracionResponse registrarCalibracion(Long id, CalibracionRequest request) {
        Instrumento instrumento = buscar(id);
        LocalDate hoy = LocalDate.now(reloj);
        if (request.fechaCalibracion().isAfter(hoy)) {
            throw new SolicitudInvalidaException("La fecha de calibración no puede ser futura");
        }
        if (!request.fechaVencimiento().isAfter(request.fechaCalibracion())) {
            throw new SolicitudInvalidaException("El vencimiento debe ser posterior a la fecha de calibración");
        }
        calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(id).ifPresent(ultima -> {
            if (request.fechaCalibracion().isBefore(ultima.getFechaCalibracion())) {
                throw new SolicitudInvalidaException("La calibración no puede ser anterior a la última registrada ("
                        + ultima.getFechaCalibracion() + ")");
            }
        });
        Calibracion calibracion = Calibracion.builder()
                .instrumento(instrumento)
                .fechaCalibracion(request.fechaCalibracion())
                .fechaVencimiento(request.fechaVencimiento())
                .certificado(request.certificado() == null || request.certificado().isBlank()
                        ? null : request.certificado().trim())
                .build();
        return CalibracionResponse.desde(calibracionRepository.save(calibracion));
    }

    @Transactional(readOnly = true)
    public VigenciaResponse vigencia(Long id, LocalDate fecha) {
        buscar(id);
        LocalDate dia = fecha == null ? LocalDate.now(reloj) : fecha;
        LocalDate vencimiento = calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(id)
                .map(Calibracion::getFechaVencimiento).orElse(null);
        return new VigenciaResponse(id, dia, calibracionRepository.vigenteEn(id, dia), vencimiento);
    }

    private void aplicar(Instrumento instrumento, InstrumentoRequest request) {
        instrumento.setNombreTipo(request.nombreTipo().trim());
        instrumento.setMarca(request.marca().trim());
        instrumento.setModelo(request.modelo().trim());
    }

    private Instrumento buscar(Long id) {
        return instrumentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el instrumento " + id));
    }

    private InstrumentoResponse respuesta(Instrumento instrumento, Calibracion ultima) {
        LocalDate calibrado = ultima == null ? null : ultima.getFechaCalibracion();
        LocalDate vence = ultima == null ? null : ultima.getFechaVencimiento();
        Long dias = vence == null ? null : ChronoUnit.DAYS.between(LocalDate.now(reloj), vence);
        EstadoCalibracion estado = EstadoCalibracion.segun(dias);
        return new InstrumentoResponse(instrumento.getId(), instrumento.getNombreTipo(), instrumento.getMarca(),
                instrumento.getModelo(), instrumento.getNumeroSerie(), calibrado, vence, dias, estado.name(),
                estado.getEtiqueta());
    }
}
