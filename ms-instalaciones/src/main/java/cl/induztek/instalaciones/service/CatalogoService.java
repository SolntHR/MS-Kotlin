package cl.induztek.instalaciones.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.instalaciones.dto.ComunaResponse;
import cl.induztek.instalaciones.dto.RegionResponse;
import cl.induztek.instalaciones.dto.TipoEquipoResponse;
import cl.induztek.instalaciones.exception.RecursoNoEncontradoException;
import cl.induztek.instalaciones.repository.ComunaRepository;
import cl.induztek.instalaciones.repository.RegionRepository;
import cl.induztek.instalaciones.repository.TipoEquipoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService {

    private final RegionRepository regionRepository;
    private final ComunaRepository comunaRepository;
    private final TipoEquipoRepository tipoEquipoRepository;

    public List<RegionResponse> regiones() {
        return regionRepository.findAllByOrderByNombreAsc().stream().map(RegionResponse::desde).toList();
    }

    public List<ComunaResponse> comunas(Long regionId) {
        if (regionId == null) {
            return comunaRepository.findAllByOrderByNombreAsc().stream().map(ComunaResponse::desde).toList();
        }
        if (!regionRepository.existsById(regionId)) {
            throw new RecursoNoEncontradoException("No existe la región " + regionId);
        }
        return comunaRepository.findByRegionIdOrderByNombreAsc(regionId).stream().map(ComunaResponse::desde).toList();
    }

    public List<TipoEquipoResponse> tiposEquipo() {
        return tipoEquipoRepository.findAllByOrderByIdAsc().stream().map(TipoEquipoResponse::desde).toList();
    }

    public TipoEquipoResponse tipoEquipo(Long id) {
        return tipoEquipoRepository.findById(id).map(TipoEquipoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el tipo de equipo " + id));
    }
}
