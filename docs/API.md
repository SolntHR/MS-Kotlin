# Contrato HTTP de usuarios y autenticación v1

Base de desarrollo: `http://localhost:8080/api/v1` vía gateway o `http://localhost:8081/api/v1` directo. JSON UTF-8 y `Content-Type: application/json`. Fechas ISO 8601 UTC. Los IDs son enteros de 64 bits; Android debe preferir `Long`. Swagger interactivo: `/swagger-ui/index.html`; especificación: `/v3/api-docs`.

## Endpoints

| Método y ruta relativa a `/api/v1` | Acceso | Éxito | Errores propios |
|---|---|---|---|
| `POST /auth/registro` | Público | `201` cuenta pendiente y Location | `400` campos/JSON; `409` duplicado; `429` límite |
| `POST /auth/login` | Público | `200` token y usuario | `400` formato; `401` credenciales; `403` cuenta no habilitada; `429` límite |
| `POST /auth/recuperacion/solicitar` | Público | `202` mensaje genérico | `400` formato; `429` límite; `503` SMTP |
| `POST /auth/recuperacion/restablecer` | Público | `204` sin cuerpo | `400` validación/código; `429` límite |
| `POST /auth/logout` | Bearer | `204` sin cuerpo | `401` token |
| `GET /usuarios/me` | Bearer | `200` perfil propio | `401` token |
| `GET /usuarios?pagina=0&tamano=20` | ADMIN | `200` página | `400` paginación |
| `GET /usuarios/{id}` | ADMIN | `200` usuario | `400` ID; `404` ausente |
| `PATCH /usuarios/{id}/rol` | ADMIN | `200` usuario actualizado | `400` rol; `404` ausente; `409` último ADMIN |
| `PATCH /usuarios/{id}/activo` | ADMIN | `200` usuario actualizado | `400` JSON; `404` ausente; `409` rol requerido/último ADMIN |

Todas las rutas privadas pueden devolver `401` sin sesión o `403` sin permiso. Operaciones con BD pueden devolver `503` por indisponibilidad, o `500` por un fallo inesperado. Un JSON mal formado, campos desconocidos o enum inválido producen `400`; métodos no permitidos, `405`; tipo de contenido incorrecto, `415`. Un token inválido enviado incluso a una ruta pública produce `401`: omitir Authorization en registro/login/recuperación. Eureka sin instancias disponibles produce `503` desde gateway, cuyo cuerpo de error puede tener el formato de Spring Cloud; el cliente debe tratar ese código como indisponibilidad.

## Registro compatible con Android

`RegistroUsuarioRepository.kt` define `SolicitudRegistroUsuario` con estos seis campos; `RegistroViewModel.kt` ya normaliza RUT, teléfono y nombre y espera `ResultadoRegistroUsuario.Creado(correoInterno)`.

```json
{
  "nombre": "Ana Pérez Soto",
  "rut": "123456785",
  "telefono": "+56 9 1234 5678",
  "correoPersonal": "ana@example.org",
  "preguntaSeguridad": "¿Cuál es el nombre de tu primera mascota?",
  "respuestaSeguridad": "Luna"
}
```

Respuesta `201 Created` y `Location: /api/v1/usuarios/42`:

```json
{
  "id": 42,
  "correoInterno": "aperez.12345678@induztek.ejemplo",
  "estado": "PENDIENTE_ASIGNACION_ROL"
}
```

La lectura de Location requiere ADMIN; el usuario público obtiene solo esta confirmación. El correo es un identificador de login, no un buzón provisionado. El sufijo deriva del RUT para evitar colisiones. El dominio se configura con `INTERNAL_EMAIL_DOMAIN`. La sugerencia visual de Android no es el valor definitivo. Guardar siempre el `correoInterno` respondido.

No agregar `rolCodigo`, `activo`, contraseña ni hash al cuerpo de registro. Se rechazan como campos desconocidos. La cuenta no puede iniciar sesión hasta que un ADMIN asigne rol y su titular establezca contraseña mediante código.

### Validaciones

| Campo | Regla |
|---|---|
| nombre | Obligatorio, hasta 150 caracteres, al menos nombre y apellido; letras, espacios, apóstrofe o guion |
| rut | Obligatorio, hasta 15 caracteres de entrada; se quitan puntos y guion; cuerpo de 7–8 dígitos y DV módulo 11; único |
| telefono | Celular chileno `9XXXXXXXX`, admite `+56 9 XXXX XXXX`; se guarda formateado |
| correoPersonal | Obligatorio, email válido de hasta 254 caracteres; se guarda en minúsculas, único |
| preguntaSeguridad | Una de las cinco preguntas exactas de Android |
| respuestaSeguridad | No vacía, hasta 128 caracteres; se normalizan espacios/mayúsculas para verificar, se guarda solo hash con sal |
| nuevaContrasena | Entre 12 y 128 caracteres, no vacía; confirmación idéntica; no se recorta ni convierte a minúsculas |

Preguntas aceptadas:

1. ¿Cuál es el nombre de tu primera mascota?
2. ¿En qué ciudad nacieron tus padres?
3. ¿Cuál es tu comida favorita?
4. ¿Cuál fue el modelo de tu primer auto?
5. ¿Cuál es el nombre de tu mejor amigo de la infancia?

## Administración

Iniciar sesión con el ADMIN inicial definido en variables de entorno y enviar su Bearer:

```http
PATCH /api/v1/usuarios/42/rol
Authorization: Bearer TOKEN_ADMIN
Content-Type: application/json

{"rolCodigo":"TECNICO"}
```

Roles: `ADMIN`, `TECNICO`, `INGENIERO`, `JEFE_TECNICO`, `CLIENTE`. Asignar rol habilita la cuenta y revoca sus sesiones y códigos previos. Solo ADMIN puede asignar roles; JEFE_TECNICO no implica ADMIN. `PATCH /usuarios/42/activo` recibe `{"activo":false}` o `{"activo":true}`. Activar requiere rol. No se permite eliminar al último administrador activo.

Un usuario público tiene esta forma:

```json
{
  "id": 42,
  "nombre": "Ana Pérez Soto",
  "rut": "123456785",
  "telefono": "+56 9 1234 5678",
  "correoPersonal": "ana@example.org",
  "correoInterno": "aperez.12345678@induztek.ejemplo",
  "rolCodigo": "TECNICO",
  "activo": true,
  "credencialesConfiguradas": false,
  "creadoEn": "2026-11-23T13:00:00Z"
}
```

`credencialesConfiguradas=false` indica que aún falta establecer la contraseña. La lista usa `{ "contenido": [...], "pagina": 0, "tamano": 20, "totalElementos": 1, "totalPaginas": 1 }`. `pagina >= 0`, `tamano` entre 1 y 100, orden ascendente por ID.

## Primera contraseña y recuperación

1. Con la cuenta habilitada, enviar correo **interno**, pregunta y respuesta:

```http
POST /api/v1/auth/recuperacion/solicitar
Content-Type: application/json

{
  "correo": "aperez.12345678@induztek.ejemplo",
  "preguntaSeguridad": "¿Cuál es el nombre de tu primera mascota?",
  "respuestaSeguridad": "luna"
}
```

Respuesta `202`: `{"mensaje":"Si los datos coinciden con una cuenta habilitada, recibirás un código en tu correo personal"}`. El código se envía exclusivamente al correo personal almacenado; no se recibe un destinatario alternativo ni se devuelve el código en HTTP. Los datos que no coinciden, cuentas pendientes y desactivadas reciben el mismo mensaje sin enviar correo.

2. El titular lee el código y establece contraseña:

```http
POST /api/v1/auth/recuperacion/restablecer
Content-Type: application/json

{
  "correo": "aperez.12345678@induztek.ejemplo",
  "codigo": "12345678",
  "nuevaContrasena": "Una clave nueva segura",
  "confirmacion": "Una clave nueva segura"
}
```

Respuesta `204`, sin cuerpo. Código de ocho dígitos, diez minutos de vigencia, cinco errores permitidos, un solo uso. Reenvío después de sesenta segundos; el código anterior queda invalidado. Los contadores de intentos se guardan aunque la respuesta sea error. La cuenta debe conservar rol y estar activa. Después de cambiar la clave se requiere nuevo login; todas las sesiones previas se revocan.

## Login, perfil y logout

```http
POST /api/v1/auth/login
Content-Type: application/json

{"correo":"aperez.12345678@induztek.ejemplo","contrasena":"Una clave nueva segura"}
```

Respuesta `200`:

```json
{
  "accessToken": "TOKEN_OPACO_DE_43_CARACTERES",
  "tokenType": "Bearer",
  "expiresIn": 43200,
  "usuario": { "id": 42, "rolCodigo": "TECNICO" }
}
```

En este ejemplo se abrevia `usuario`; la respuesta real incluye todos los campos del usuario público. No hay refresh token. Al caducar a las doce horas, repetir login. Para `GET /usuarios/me`, `POST /auth/logout` y otras rutas privadas: `Authorization: Bearer <accessToken>`. Logout devuelve `204` y revoca esa sesión.

## Errores

```json
{
  "codigo": "VALIDACION",
  "mensaje": "Revisa los campos enviados",
  "errores": { "rut": "RUT inválido, revisa el dígito verificador" },
  "timestamp": "2026-11-23T13:00:00Z"
}
```

`errores` puede estar vacío. No interpretar el texto del mensaje como código. Códigos de negocio: `VALIDACION`, `SOLICITUD_INVALIDA`, `USUARIO_DUPLICADO`, `CREDENCIALES_INVALIDAS`, `CUENTA_NO_HABILITADA`, `CODIGO_INVALIDO`, `DEMASIADOS_INTENTOS`, `CORREO_NO_DISPONIBLE`, `TOKEN_INVALIDO`, `AUTENTICACION_REQUERIDA`, `ACCESO_DENEGADO`, `USUARIO_NO_ENCONTRADO`, `ULTIMO_ADMINISTRADOR`, `ROL_REQUERIDO`, `SERVICIO_NO_DISPONIBLE`, `ERROR_INTERNO`.

## Handoff para el chat Android

Implementar un adaptador HTTP de `RegistroUsuarioRepository` y sustituir `RegistroUsuarioNoDisponibleRepository` en `AppViewModelFactory`. En `201`, deserializar `correoInterno` y devolver `ResultadoRegistroUsuario.Creado`; el ViewModel ya presenta la confirmación y limpia los datos. El resultado actual solo distingue `Creado` y `NoDisponible`: ampliar su modelo para mostrar errores de campo `400` y duplicados `409`. No reemplazar todos los fallos con el mensaje «registro todavía no disponible».

Login y recuperación siguen usando `UsuarioRepository` local. Sustituir la comparación SQL de contraseña y el cambio local por los endpoints anteriores; agregar el paso del código enviado al correo. La validación Android actual permite seis caracteres; actualizarla al mínimo de doce del servidor y comunicar ese requisito. El modelo Android `Usuario` exige `passwordHash`: crear un DTO público que no lo requiera; `correoInterno` puede mapearse al campo visual `correo`. Los roles se deben leer del servidor y agregar `ADMIN` al catálogo y navegación correspondiente. Las restricciones reales de autorización permanecen en backend.

Guardar el token mediante almacenamiento protegido de Android, omitirlo en logs y usar un interceptor HTTP para las rutas privadas. Tratar `401` como sesión inválida, `403` como acceso denegado, `429` como límite temporal y `5xx` o desconexión como indisponibilidad. No considerar un usuario SQLite como prueba de autenticación remota. Mantener los datos locales de terreno como caché separada de la identidad del servidor.
