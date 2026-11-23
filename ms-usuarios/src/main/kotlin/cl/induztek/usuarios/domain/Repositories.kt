package cl.induztek.usuarios.domain

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun existsByRut(rut: String): Boolean
    fun existsByCorreoPersonal(correo: String): Boolean
    fun findByCorreoInterno(correo: String): Usuario?
    @Query("select u.id from Usuario u where u.correoInterno = :correo")
    fun buscarIdPorCorreoInterno(correo: String): Long?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    fun bloquear(id: Long): Usuario?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.rolCodigo = cl.induztek.usuarios.domain.Rol.ADMIN order by u.id")
    fun bloquearAdministradores(): List<Usuario>
}

interface SesionRepository : JpaRepository<Sesion, String> {
    @Query("select s from Sesion s join fetch s.usuario u where s.tokenHash = :hash and s.expiraEn > :ahora and u.activo = true and u.rolCodigo is not null")
    fun buscarVigente(hash: String, ahora: Instant): Sesion?
    @Modifying @Query("delete from Sesion s where s.usuario.id = :id")
    fun revocarUsuario(id: Long)
    @Modifying @Query("delete from Sesion s where s.expiraEn <= :ahora")
    fun eliminarExpiradas(ahora: Instant)
}

interface CodigoRepository : JpaRepository<CodigoRecuperacion, Long>
