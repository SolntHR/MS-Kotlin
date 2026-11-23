package cl.induztek.instalaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.induztek.instalaciones.model.TipoEquipo;

public interface TipoEquipoRepository extends JpaRepository<TipoEquipo, Long> {

    List<TipoEquipo> findAllByOrderByIdAsc();
}
