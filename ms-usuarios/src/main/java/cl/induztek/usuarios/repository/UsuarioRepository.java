package cl.induztek.usuarios.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.induztek.usuarios.model.Rol;
import cl.induztek.usuarios.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByCorreoPersonalIgnoreCase(String correoPersonal);

    boolean existsByCorreoPersonalIgnoreCase(String correoPersonal);

    boolean existsByRutIgnoreCase(String rut);

    boolean existsByRutIgnoreCaseAndIdNot(String rut, Long id);

    @Query("""
            select u from Usuario u
            where (:rol is null or u.rol = :rol)
              and (:activo is null or u.activo = :activo)
              and (:texto is null
                   or lower(u.nombre) like lower(concat('%', :texto, '%'))
                   or lower(u.correo) like lower(concat('%', :texto, '%'))
                   or lower(u.correoPersonal) like lower(concat('%', :texto, '%'))
                   or lower(u.rut) like lower(concat('%', :texto, '%')))
            order by u.rol, u.nombre
            """)
    List<Usuario> buscar(@Param("rol") Rol rol, @Param("activo") Boolean activo, @Param("texto") String texto);
}
