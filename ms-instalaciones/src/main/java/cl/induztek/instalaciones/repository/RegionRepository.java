package cl.induztek.instalaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.induztek.instalaciones.model.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderByNombreAsc();
}
