package cl.induztek.protocolo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.induztek.protocolo.model.TipoPrueba;

public interface TipoPruebaRepository extends JpaRepository<TipoPrueba, Long> {

    @EntityGraph(attributePaths = "parametros")
    List<TipoPrueba> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "parametros")
    @Query("""
            select t from TipoPrueba t
            where t.id in (select r.tipoPrueba.id from TipoEquipoPrueba r where r.tipoEquipoId = :tipoEquipoId)
            order by t.id
            """)
    List<TipoPrueba> aplicablesA(@Param("tipoEquipoId") Long tipoEquipoId);
}
