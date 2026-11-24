package cl.induztek.protocolo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.induztek.protocolo.model.TipoEquipoPrueba;

public interface TipoEquipoPruebaRepository extends JpaRepository<TipoEquipoPrueba, Long> {

    boolean existsByTipoEquipoIdAndTipoPruebaId(Long tipoEquipoId, Long tipoPruebaId);
}
