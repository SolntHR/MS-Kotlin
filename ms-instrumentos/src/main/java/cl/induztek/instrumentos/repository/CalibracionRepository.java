package cl.induztek.instrumentos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.induztek.instrumentos.model.Calibracion;

public interface CalibracionRepository extends JpaRepository<Calibracion, Long> {

    List<Calibracion> findByInstrumentoIdOrderByFechaCalibracionDesc(Long instrumentoId);

    Optional<Calibracion> findFirstByInstrumentoIdOrderByFechaVencimientoDesc(Long instrumentoId);

    @Query("""
            select c from Calibracion c
            where c.fechaVencimiento = (
                select max(c2.fechaVencimiento) from Calibracion c2 where c2.instrumento = c.instrumento)
            """)
    List<Calibracion> ultimasCalibraciones();

    @Query("""
            select count(c) > 0 from Calibracion c
            where c.instrumento.id = :instrumentoId
              and c.fechaCalibracion <= :fecha
              and c.fechaVencimiento >= :fecha
            """)
    boolean vigenteEn(@Param("instrumentoId") Long instrumentoId, @Param("fecha") LocalDate fecha);
}
