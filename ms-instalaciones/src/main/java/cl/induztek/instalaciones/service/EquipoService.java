package cl.induztek.instalaciones.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.instalaciones.dto.EquipoRequest;
import cl.induztek.instalaciones.dto.EquipoResponse;
import cl.induztek.instalaciones.exception.ConflictoException;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.model.Equipo;
import cl.induztek.instalaciones.model.Instalacion;
import cl.induztek.instalaciones.model.TipoEquipo;
import cl.induztek.instalaciones.repository.EquipoRepository;
import cl.induztek.instalaciones.repository.InstalacionRepository;
import cl.induztek.instalaciones.repository.TipoEquipoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final InstalacionRepository instalacionRepository;
    private final TipoEquipoRepository tipoEquipoRepository;

    @Transactional(readOnly = true)
    public List<EquipoResponse> listar(Long instalacionId, Long clienteId, Long tipoEquipoId) {
        return equipoRepository.buscar(instalacionId, clienteId, tipoEquipoId).stream()
                .map(EquipoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public EquipoResponse obtener(Long id) {
        return EquipoResponse.desde(buscar(id));
    }

    @Transactional
    public EquipoResponse crear(EquipoRequest request) {
        Instalacion instalacion = buscarInstalacion(request.instalacionId());
        String tag = normalizarTag(request.tag());
        if (equipoRepository.existsByInstalacionIdAndTagIgnoreCase(instalacion.getId(), tag)) {
            throw new ConflictoException("Ya existe el TAG " + tag + " en " + instalacion.getNombre());
        }
        Equipo equipo = new Equipo();
        equipo.setInstalacion(instalacion);
        aplicar(equipo, request, tag);
        return EquipoResponse.desde(equipoRepository.save(equipo));
    }

    @Transactional
    public EquipoResponse actualizar(Long id, EquipoRequest request) {
        Equipo equipo = buscar(id);
        Instalacion instalacion = buscarInstalacion(request.instalacionId());
        String tag = normalizarTag(request.tag());
        if (equipoRepository.existsByInstalacionIdAndTagIgnoreCaseAndIdNot(instalacion.getId(), tag, id)) {
            throw new ConflictoException("Ya existe el TAG " + tag + " en " + instalacion.getNombre());
        }
        equipo.setInstalacion(instalacion);
        aplicar(equipo, request, tag);
        return EquipoResponse.desde(equipoRepository.save(equipo));
    }

    private void aplicar(Equipo equipo, EquipoRequest request, String tag) {
        equipo.setTag(tag);
        equipo.setTipoEquipo(buscarTipo(request.tipoEquipoId()));
        equipo.setMarca(limpiar(request.marca()));
        equipo.setModelo(limpiar(request.modelo()));
        equipo.setNumeroSerie(limpiar(request.numeroSerie()));
        equipo.setAnio(request.anio());
        equipo.setObservaciones(limpiar(request.observaciones()));
        equipo.setPotenciaKva(request.potenciaKva());
        equipo.setTensionKv(request.tensionKv());
        equipo.setCorrienteA(request.corrienteA());
        equipo.setMaxContactosUohm(request.maxContactosUohm());
    }

    private Equipo buscar(Long id) {
        return equipoRepository.findWithDetalleById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo " + id));
    }

    private Instalacion buscarInstalacion(Long id) {
        return instalacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la instalación " + id));
    }

    private TipoEquipo buscarTipo(Long id) {
        return tipoEquipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tipo de equipo " + id));
    }

    private String normalizarTag(String tag) {
        return tag.trim().toUpperCase(Locale.ROOT);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
