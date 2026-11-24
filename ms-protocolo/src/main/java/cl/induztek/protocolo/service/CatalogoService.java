package cl.induztek.protocolo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.induztek.protocolo.dto.TipoPruebaResponse;
import cl.induztek.protocolo.repository.TipoPruebaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService {

    private final TipoPruebaRepository tipoPruebaRepository;

    public List<TipoPruebaResponse> tiposPrueba(Long tipoEquipoId) {
        return (tipoEquipoId == null ? tipoPruebaRepository.findAllByOrderByIdAsc()
                : tipoPruebaRepository.aplicablesA(tipoEquipoId)).stream().map(TipoPruebaResponse::desde).toList();
    }
}
