# Commits propuestos

No se han creado commits. Los siguientes grupos permiten revisar y registrar los cambios desde GitHub Desktop.

| Nombre corto | Descripción corta | Archivos principales |
|---|---|---|
| `chore: iniciar backend Kotlin con gateway y Eureka` | Agrega estructura Maven, ejecutables independientes, descubrimiento y rutas de usuarios. | `pom.xml`, wrapper, `.mvn/`, `.gitignore`, `eureka-server/`, `api-gateway/` |
| `feat: implementar usuarios y autenticación segura` | Agrega registro compatible con Android, roles ADMIN, hashes con sal, sesiones y recuperación por correo. | `ms-usuarios/pom.xml`, `ms-usuarios/src/main/` |
| `test: verificar registro roles y recuperación` | Cubre validaciones, permisos, login, códigos, fallos SMTP y revocación con JaCoCo. | `ms-usuarios/src/test/` |
| `docs: documentar API y ejecución del backend` | Explica arquitectura, contrato Android, pruebas y ejecución con Java o Docker. | `README.md`, `docs/`, `scripts/`, `.env.example`, `Dockerfile`, `.dockerignore`, `compose.yml` |

El plugin de cobertura y su umbral están en `ms-usuarios/pom.xml`; se pueden incluir en el grupo de pruebas al seleccionar líneas. Si se prefiere un único commit: **`feat: agregar usuarios y autenticación para Induztek`**. Descripción: **Implementa registro, roles administrados y recuperación por correo con gateway, pruebas y documentación.**
