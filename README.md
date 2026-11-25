# Induztek Protocolos — Microservicios
 
Backend de la aplicación móvil **Induztek Protocolos** (DSY1105 – Desarrollo de Aplicaciones Móviles, Duoc UC). Expone, a través de un único **API Gateway**, los servicios que la app Android consume con Retrofit: usuarios y recuperación de contraseña, instalaciones y equipos, instrumentos y calibraciones, y visitas y protocolos de prueba.
 
## Integrantes — Equipo 014
 
| Integrante | Usuario GitHub |
|---|---|
| Solange Hernandez | [@SolntHR](https://github.com/SolntHR) |
| Matias Mazzo | [@mmazzo1411](https://github.com/mmazzo1411) |
 
Aplicación móvil: [AppProtocolos_EQ014](https://github.com/SolntHR/AppProtocolos_EQ014)
 
## Arquitectura
 
```
App Android (Retrofit)
        │  HTTPS (túnel al puerto 8090)
        ▼
  api-gateway :8090 ──── eureka-server :8761 (registro y descubrimiento)
        │
        ├── /usuarios/**                                   → ms-usuarios      :8081 → MySQL induztek_usuarios
        ├── /regiones /comunas /instalaciones
        │   /tipos-equipo /equipos                          → ms-instalaciones :8082 → MySQL induztek_instalaciones
        ├── /instrumentos/**                                → ms-instrumentos  :8083 → MySQL induztek_instrumentos
        └── /visitas /protocolos /tipos-prueba /evidencias  → ms-protocolo     :8084 → MySQL induztek_protocolos
```
 
| Proyecto | Puerto | Base de datos | Consulta a |
|---|---|---|---|
| eureka-server | 8761 | — | — |
| api-gateway | 8090 | — | todos |
| ms-usuarios | 8081 | induztek_usuarios | — |
| ms-instalaciones | 8082 | induztek_instalaciones | ms-usuarios |
| ms-instrumentos | 8083 | induztek_instrumentos | — |
| ms-protocolo | 8084 | induztek_protocolos | ms-usuarios, ms-instalaciones, ms-instrumentos |
 
- Cada microservicio tiene **su propia base de datos** y se registra en Eureka.
- La comunicación entre microservicios usa `WebClient` con `@LoadBalanced`, que resuelve cada servicio por su nombre en Eureka, sin dependencias circulares.
- Tecnologías: Java 21, Spring Boot 4, Spring Cloud (Gateway y Netflix Eureka), Spring Data JPA, MySQL, Bean Validation, springdoc-openapi (Swagger), JUnit 5, Mockito y JaCoCo.
## Qué hace cada microservicio
 
| Microservicio | Responsabilidad |
|---|---|
| ms-usuarios | Login, registro público (cuenta pendiente hasta que un jefe técnico asigna el perfil), gestión de usuarios, cambio de perfil con contraseña del jefe y recuperación de contraseña por **código al correo** o por **pregunta de seguridad**. Contraseñas, respuestas y códigos se guardan con **SHA-256 y sal**. |
| ms-instalaciones | Regiones y comunas, instalaciones de cada cliente, tipos de equipo y equipos con TAG único por instalación. |
| ms-instrumentos | Instrumentos de medición, calibraciones y vigencia (vigente, por vencer, vencida). |
| ms-protocolo | Visitas (asignar, editar, eliminar, participantes, recepción y acta), protocolos de prueba con pruebas y mediciones, cambios de estado (formalizar, observar, anular), historial por equipo, indicadores y **fotos de evidencia**. |
 
## Requisitos
 
- **Java 21**
- **MySQL** en `localhost:3306` (usuario `root`, sin contraseña). Cada microservicio crea su base al iniciar.
## Ejecución
 
Iniciar en este orden, cada uno en su carpeta:
 
1. `eureka-server`
2. `ms-usuarios`
3. `ms-instalaciones`
4. `ms-instrumentos`
5. `ms-protocolo`
6. `api-gateway`
```
mvnw spring-boot:run
```
 
- Panel de Eureka: http://localhost:8761 (los cinco servicios deben aparecer **UP**).
- Punto de entrada único: http://localhost:8090 (desde el emulador Android: http://10.0.2.2:8090).
- **Swagger** de cada microservicio: `http://localhost:<puerto>/swagger-ui.html`.
- Todos los endpoints con ejemplos de JSON para Retrofit: [`docs/API.md`](docs/API.md).
### Conectar la app desde un celular

La app apunta a un túnel persistente de dev tunnels que publica el puerto 8090:

```
winget install Microsoft.devtunnel
devtunnel user login -g
devtunnel host induztek-eq014
```

La URL (`https://fjfh1k06-8090.brs.devtunnels.ms/`) es siempre la misma, en cualquier computador, mientras se inicie sesión con la cuenta que creó el túnel.

## Variables de entorno
 
Todas son opcionales; los valores por defecto sirven para ejecutar en local.
 
| Variable | Microservicio | Uso | Por defecto |
|---|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | todos los ms | Conexión a MySQL | `localhost:3306`, `root`, vacía |
| `EUREKA_URL` | todos | Dirección de Eureka | `http://localhost:8761/eureka/` |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | ms-usuarios | Cuenta que envía los correos | vacías (no se envían correos) |
| `MAIL_HOST`, `MAIL_PORT` | ms-usuarios | Servidor SMTP | `smtp.gmail.com`, `587` |
| `CORREO_DOMINIO` | ms-usuarios | Dominio de los correos de acceso generados | `induztek.ejemplo` |
| `EVIDENCIAS_DIR` | ms-protocolo | Carpeta de las fotos de evidencia | `uploads/evidencias` |
 
### Envío de correos (API de correo)
 
ms-usuarios envía dos correos al **correo personal** del usuario:
 
- **Activación**: cuando el jefe técnico crea una cuenta o asigna perfil a una cuenta pendiente. Incluye el correo de acceso y un código para crear la contraseña (vence en 24 horas).
- **Recuperación**: código de 6 dígitos para restablecer la contraseña (vence en 10 minutos, máximo 5 intentos).
Para Gmail se usa una **contraseña de aplicación** (requiere verificación en 2 pasos):
 
```powershell
$env:MAIL_USERNAME="cuenta@gmail.com"
$env:MAIL_PASSWORD="clavedeaplicacion"
.\mvnw spring-boot:run
```
 
Las credenciales **nunca** se guardan en el repositorio. Si el envío falla, la consola de ms-usuarios muestra el motivo.
 
## Usuarios de prueba
 
ms-usuarios los crea al iniciar por primera vez. Contraseña de todos: `Induztek#2026`.
 
| Perfil | Correo de acceso |
|---|---|
| Jefe técnico | `respinoza@induztek.ejemplo` |
| Ingeniero de pruebas | `acontreras@induztek.ejemplo` |
| Técnico de terreno | `mgodoy@induztek.ejemplo` (respuesta de seguridad: `firulais`) |
| Cliente | `mfuentes@envpacifico.ejemplo` |
 
Sus correos personales son ficticios: para probar el envío de correos, crea un usuario con un correo personal real.
 
Si ya existía la base `induztek_usuarios` de una versión anterior, bórrala antes de iniciar ms-usuarios (`DROP DATABASE induztek_usuarios;`), porque la tabla `usuario` cambió.
 
## Probar en Swagger

La app cifra la contraseña antes de enviarla (SHA-256 del correo en minúsculas seguido de la contraseña), así que en Swagger el campo `contrasena` del login va **ya cifrado**. Valores de `Induztek#2026` para los usuarios de prueba:

| Usuario | Id | `contrasena` en `POST /usuarios/login` |
|---|---|---|
| `respinoza@induztek.ejemplo` (jefe técnico) | 7 | `09b1ce95bef87d014de1194ea6ab9e2fa3da38fbe73aaf378b5377c34cd32638` |
| `acontreras@induztek.ejemplo` (ingeniera) | 5 | `95ba2535cb9228ee79385f211fe8b7be8b6fdfc40694bec361d6b23e388ab75f` |
| `mgodoy@induztek.ejemplo` (técnico) | 1 | `283b8fc4e7e99238a3c61d9b3b937bc95476e4f0acefad4f038e054b15cdc8a5` |
| `mfuentes@envpacifico.ejemplo` (cliente) | 8 | `3fd11708ffa28470e6d673ebe95f13623c382b4e6dfe2613b87deba5ffee4c50` |

Ejemplo:

```json
{
  "correo": "respinoza@induztek.ejemplo",
  "contrasena": "09b1ce95bef87d014de1194ea6ab9e2fa3da38fbe73aaf378b5377c34cd32638"
}
```

El cambio de perfil usa el mismo valor cifrado en `contrasena` (la del jefe). El resto de los endpoints no requiere sesión: las escrituras llevan el `usuarioId` en el cuerpo o en la consulta.

## Códigos de respuesta
 
| Código | Significado |
|---|---|
| 200 / 201 | Correcto / creado |
| 204 | Lista vacía o acción sin contenido de respuesta |
| 400 | Datos inválidos; el campo `campos` indica qué dato falló |
| 401 | Credenciales o código incorrectos |
| 403 | El perfil no tiene permiso |
| 404 | No existe |
| 405 | Método no permitido |
| 409 | Duplicado (RUT, correo, TAG, OT) |
| 429 | Demasiados intentos |
| 503 | Otro microservicio no está disponible |
 
Formato de error:
 
```json
{
  "fecha": "2026-11-24T20:30:00",
  "estado": 400,
  "error": "Bad Request",
  "mensaje": "Hay datos inválidos",
  "ruta": "/usuarios",
  "campos": { "rut": "El RUT no es válido" }
}
```
 
## Pruebas y cobertura
 
En la carpeta de cada microservicio:
 
```
mvnw clean verify
```
 
`verify` falla si la cobertura de líneas baja del **80 %**. Reporte: `target/site/jacoco/index.html`.
 
| Microservicio | Pruebas | Cobertura de líneas |
|---|---|---|
| ms-usuarios | 79 | 99 % |
| ms-instalaciones | 31 | 96 % |
| ms-instrumentos | 15 | 99 % |
| ms-protocolo | 29 | 96 % |
 
> Todos los datos del proyecto son ficticios y fueron creados con fines académicos.
 