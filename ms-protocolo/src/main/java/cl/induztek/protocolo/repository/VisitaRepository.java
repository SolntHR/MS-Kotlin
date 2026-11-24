package cl.induztek.protocolo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.induztek.protocolo.model.Visita;

public interface VisitaRepository extends JpaRepository<Visita, Long> {

    @EntityGraph(attributePaths = "participantes")
    @Query("select v from Visita v order by v.fecha desc, v.numeroOt")
    List<Visita> todas();

    @EntityGraph(attributePaths = "participantes")
    @Query("""
            select distinct v from Visita v left join v.participantes p
            where v.ingenieroCargoId = :usuarioId or p = :usuarioId
            order by v.fecha desc, v.numeroOt
            """)
    List<Visita> deUsuario(@Param("usuarioId") Long usuarioId);

    @EntityGraph(attributePaths = "participantes")
    @Query("select v from Visita v where v.instalacionId in :instalaciones order by v.fecha desc, v.numeroOt")
    List<Visita> deInstalaciones(@Param("instalaciones") List<Long> instalaciones);

    boolean existsByNumeroOtIgnoreCase(String numeroOt);
}
