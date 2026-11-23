package cl.induztek.usuarios

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import java.time.Clock

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class UsuariosApplication {
    @Bean fun clock(): Clock = Clock.systemUTC()
    @Bean fun passwordEncoder(): PasswordEncoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()
}

fun main(args: Array<String>) { runApplication<UsuariosApplication>(*args) }
