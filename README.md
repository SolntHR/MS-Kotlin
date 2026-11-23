# Microservicios Induztek

Backend en Kotlin y Spring Boot para AppProtocolos EQ014. Esta primera entrega implementa **usuarios y autenticación**, API Gateway y Eureka. La app Android y los documentos originales se consultaron sin modificarlos.

- [Arquitectura y requisitos](docs/ARQUITECTURA.md)
- [Contrato HTTP y coordinación con Android](docs/API.md)
- [Pruebas y verificación](docs/PRUEBAS.md)
- [Cambios propuestos para commits](docs/COMMITS.md)

## Requisitos

JDK 21 y acceso a Maven Central en la primera compilación. Maven Wrapper está incluido; no se necesita Maven instalado. PostgreSQL y Mailpit se pueden iniciar con Docker Compose. Para desarrollo sin Docker, usuarios usa una base H2 persistente en `data/usuarios` y necesita un SMTP accesible para enviar códigos. H2 no tiene consola HTTP habilitada.

Los scripts opcionales de configuración y prueba requieren PowerShell 7 para mantener la codificación UTF-8 de preguntas y respuestas.

## Compilar y probar

Desde la raíz del backend en PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Compila los tres ejecutables, ejecuta pruebas de usuarios y genera cobertura en `ms-usuarios/target/site/jacoco/index.html`. Las pruebas usan H2 en memoria, tiempo controlado y SMTP simulado. No requieren credenciales ni envían correos reales. Las dependencias se guardan dentro del backend en `.m2/`, excluido de Git.

En macOS/Linux: `sh mvnw clean verify`.

En Windows, detener previamente los procesos Java de estos servicios: un JAR en ejecución queda bloqueado y no permite `clean`.

## Configurar secretos

```powershell
Copy-Item .env.example .env
```

Editar `.env` y completar:

- `RECOVERY_CODE_SECRET`: secreto aleatorio de al menos 32 bytes, estable entre reinicios. En PowerShell 7: `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))`.
- `ADMIN_PASSWORD`: contraseña inicial de 12 a 128 caracteres.
- `ADMIN_SECURITY_ANSWER`: respuesta a «¿Cuál es el nombre de tu primera mascota?» para recuperar la cuenta inicial.
- `DB_PASSWORD`: contraseña de PostgreSQL si se utiliza Compose.

`ADMIN_EMAIL` es el identificador de login y destinatario de recuperación del administrador inicial. Usa un correo accesible al configurar SMTP real. El administrador se crea solo en una base vacía. En reinicios no se modifica su contraseña. Después del primer arranque se pueden retirar `ADMIN_PASSWORD` y `ADMIN_SECURITY_ANSWER` para ejecución con Java; Compose los exige al levantar su entorno de desarrollo.

## Ejecutar con Docker

```powershell
docker compose up --build -d
docker compose logs -f ms-usuarios api-gateway
```

La primera resolución `lb://ms-usuarios` puede tardar unos 30–90 segundos mientras Eureka publica el registro.

| Acceso | Dirección |
|---|---|
| API Gateway | http://localhost:8080 |
| Swagger de usuarios vía gateway | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Eureka | http://localhost:8761 |
| Correos de desarrollo Mailpit | http://localhost:8025 |

Usuarios y su base no publican puertos al host; el tráfico API entra por el gateway. Los datos persisten en el volumen `usuarios-data`. `docker compose down` detiene los servicios conservando ese volumen. Mailpit captura mensajes localmente; para entregar correo externo hay que configurar un proveedor SMTP.

## Ejecutar con Java sin Docker

Tras compilar, abrir tres terminales en la raíz. Para probar recuperación, tener SMTP de desarrollo en `localhost:1025` (por ejemplo Mailpit, con interfaz en `8025`).

Terminal 1:

```powershell
java -jar .\eureka-server\target\eureka-server-0.1.0-SNAPSHOT.jar
```

Terminal 2:

```powershell
. .\scripts\Load-Env.ps1
$env:EUREKA_ENABLED = 'true'
java -jar .\ms-usuarios\target\ms-usuarios-0.1.0-SNAPSHOT.jar
```

Terminal 3:

```powershell
java -jar .\api-gateway\target\api-gateway-0.1.0-SNAPSHOT.jar
```

Usuarios escucha en `8081`, gateway en `8080`, Eureka en `8761`. Para ejecutar únicamente usuarios, omitir Eureka y gateway y usar `http://localhost:8081`; por defecto su registro en Eureka está desactivado. Para ejecutar gateway sin Eureka: definir `EUREKA_ENABLED=false` y `USUARIOS_URI=http://localhost:8081` en su terminal.

`DB_USER` en `.env.example` vale `usuarios`; H2 permite crear ese usuario en una base nueva. Mantener el mismo usuario y contraseña para volver a abrir el archivo. Para PostgreSQL externo, configurar `DB_URL=jdbc:postgresql://host:5432/usuarios`, `DB_USER` y `DB_PASSWORD`. Flyway crea el esquema; Hibernate solo lo valida.

## Probar el flujo HTTP completo

Con gateway, usuarios y Mailpit en ejecución:

```powershell
. .\scripts\Load-Env.ps1
.\scripts\Smoke-Test.ps1
```

Registra una cuenta sintética, asigna `TECNICO` usando ADMIN, solicita el código, lo lee desde Mailpit, establece contraseña, inicia sesión, consulta perfil y confirma que logout invalida el token. Al terminar desactiva la cuenta de prueba. No imprime contraseñas ni tokens. Admite `-BaseUrl http://localhost:8081` para probar sin gateway.

## Integración Android

Emulador Android: base URL `http://10.0.2.2:8080/`; dispositivo físico: `http://IP_LOCAL_DEL_PC:8080/`, misma red y puerto autorizado en firewall. El acceso HTTP local y permiso de Internet se configuran desde el otro chat. Para despliegue externo se requiere HTTPS.

El registro recibe exactamente los seis campos de `SolicitudRegistroUsuario` y devuelve `201` con `correoInterno`. No recibe rol ni contraseña. Las adaptaciones de Retrofit, login, recuperación y almacenamiento del token se describen en [API.md](docs/API.md).

## Alcance pendiente

Los servicios de instalaciones/equipos, instrumentos y protocolos/visitas están identificados y aún deben implementarse. Gateway y Eureka son infraestructura: **no se cuentan como los tres microservicios de negocio exigidos en Config final.docx**. Docker/PostgreSQL y entrega a SMTP externo requieren validación donde estén disponibles; consulta [PRUEBAS.md](docs/PRUEBAS.md).

Compatibilidad verificada en [Spring Cloud](https://spring.io/projects/spring-cloud/) y [springdoc](https://springdoc.org/v2/faq.html): Boot 3.5.16, Cloud 2025.0.3 y springdoc 2.8.17. Hash PBKDF2 de [Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
