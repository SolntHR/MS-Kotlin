# API de Induztek Protocolos

Todas las llamadas de la app pasan por el **API Gateway** (puerto 8090). Base URL:

- Emulador sin túnel: `http://10.0.2.2:8090/`
- Con túnel de VS Code: `https://<tu-tunel>-8090.<region>.devtunnels.ms/`

Los JSON usan `camelCase`. Las fechas van en ISO: `2026-11-24` y `2026-11-24T10:30:00`.

## Reglas comunes

| Código | Cuándo |
|---|---|
| 200 | Consulta o actualización correcta |
| 201 | Recurso creado |
| 202 | Solicitud aceptada (envío de código) |
| 204 | Acción sin cuerpo, o **lista vacía** |
| 400 | Datos inválidos (ver `campos`) |
| 401 | Correo o contraseña incorrectos |
| 403 | Sin permiso o cuenta sin perfil |
| 404 | No existe el recurso o la ruta |
| 405 | Método HTTP no permitido en esa ruta |
| 409 | Duplicado (RUT, correo, TAG, número de serie) |
| 429 | Demasiados intentos: esperar 15 minutos |
| 503 | Otro microservicio no responde |

En Retrofit, toda función que devuelve una lista debe ser nulable, porque una respuesta 204 llega como `null`:

```kotlin
@GET("instrumentos")
suspend fun listar(): List<InstrumentoDto>?
```

y en el repositorio: `api.listar() ?: emptyList()`.

### Formato de error

```json
{
  "fecha": "2026-11-24T10:30:00",
  "estado": 400,
  "error": "Bad Request",
  "mensaje": "Faltan datos para registrar el protocolo. Revisa los campos marcados.",
  "ruta": "/protocolos",
  "campos": {
    "temperaturaC": "Ingresa la temperatura ambiente",
    "pruebas[0].instrumentoId": "Selecciona el instrumento"
  }
}
```

`campos` es `null` cuando el error no es de un campo. La app pinta en rojo cada clave de `campos`.

### Contraseñas

La app nunca envía la contraseña en texto plano. Envía `SHA-256(correoDeAcceso en minúsculas + contraseña)` en hexadecimal (64 caracteres), calculado con `Seguridad.cifrarContrasena(correo, contrasena)`. El servidor vuelve a cifrarla con una sal aleatoria.

Ejemplo: `respinoza@induztek.ejemplo` + `Induztek#2026` → `09b1ce95bef87d014de1194ea6ab9e2fa3da38fbe73aaf378b5377c34cd32638`.

### Límite de intentos

| Acción | Máximo cada 15 minutos (por correo) |
|---|---|
| Login | 10 (se reinicia al entrar bien) |
| Registro | 5 |
| Pedir código | 5 |
| Verificar o usar código | 10 |
| Pregunta de seguridad | 10 |

Al superar el máximo responde **429** con `"mensaje": "Demasiados intentos. Espera 15 minutos antes de volver a intentarlo."`.

### Cuentas de prueba

Todas usan la contraseña `Induztek#2026`.

| Perfil | Correo |
|---|---|
| Jefe técnico | `respinoza@induztek.ejemplo` |
| Ingeniero | `acontreras@induztek.ejemplo`, `smorales@induztek.ejemplo` |
| Técnico | `mgodoy@induztek.ejemplo`, `jtapia@induztek.ejemplo`, `crdon@induztek.ejemplo`, `dvalenzuela@induztek.ejemplo` |
| Cliente | `jarancibia@svcentral.ejemplo`, `pnavarro@cerroalto.ejemplo`, `cmendez@losaromos.ejemplo` |

---

## 1. Usuarios (ms-usuarios)

Perfiles (`rol`): `TECNICO`, `INGENIERO`, `JEFE_TECNICO`, `CLIENTE`, `SIN_ASIGNAR`.

### Objeto usuario

```json
{
  "id": 7,
  "rolCodigo": "JEFE_TECNICO",
  "rolNombre": "Jefe técnico",
  "rut": "13.871.905-7",
  "nombre": "Ricardo Espinoza Lagos",
  "correo": "respinoza@induztek.ejemplo",
  "correoPersonal": "respinoza@induztek.ejemplo",
  "telefono": "+56 9 9345 1187",
  "activo": true,
  "tieneContrasena": true,
  "giro": null,
  "contactoNombre": null,
  "contactoCargo": null
}
```

- `correo`: correo de acceso, lo genera el servidor (inicial del nombre + primer apellido).
- `correoPersonal`: adonde llegan los códigos.
- `tieneContrasena`: `false` mientras la cuenta no se active.

### POST `/usuarios/login`

```json
{ "correo": "respinoza@induztek.ejemplo", "contrasena": "09b1ce95bef87d014de1194ea6ab9e2fa3da38fbe73aaf378b5377c34cd32638" }
```

- 200: objeto usuario.
- 401: datos incorrectos, o cuenta aún sin contraseña.
- 403: cuenta desactivada o sin perfil.
- 429: demasiados intentos.

### POST `/usuarios/registro`

No lleva contraseña: la cuenta queda pendiente hasta que el Jefe asigne un perfil.

```json
{
  "nombre": "Valentina Muñoz Pérez",
  "rut": "11.111.111-1",
  "correoPersonal": "vale.munoz@gmail.com",
  "telefono": "+56 9 5555 6666",
  "preguntaSeguridad": "¿Cuál es tu comida favorita?",
  "respuestaSeguridad": "sushi"
}
```

201:

```json
{
  "id": 12,
  "correoInterno": "vmunoz@induztek.ejemplo",
  "estado": "PENDIENTE_ASIGNACION_ROL",
  "mensaje": "Tu cuenta quedó pendiente. Cuando el Jefe técnico te asigne un perfil, recibirás en vale.munoz@gmail.com un código para crear tu contraseña."
}
```

- Si el correo generado ya existe, se agrega un número: `vmunoz2@induztek.ejemplo`.
- 400 si hay campos inválidos. El teléfono debe llegar como `+56 9 XXXX XXXX`.
- 409 si el RUT o el correo personal ya están registrados.

### GET `/usuarios/preguntas`

200:

```json
["¿Cuál es el nombre de tu primera mascota?", "¿En qué ciudad nacieron tus padres?", "¿Cuál es tu comida favorita?", "¿Cuál fue el modelo de tu primer auto?", "¿Cuál es el nombre de tu mejor amigo de la infancia?"]
```

### GET `/usuarios?rol=SIN_ASIGNAR&activo=true&buscar=godoy`

- Los tres filtros son opcionales.
- `buscar` busca en el nombre, el RUT, el correo y el correo personal.
- 200: lista de usuarios. 204: sin resultados.

### GET `/usuarios/{id}`

200 usuario / 404.

### POST `/usuarios` (el Jefe crea una cuenta)

```json
{
  "rol": "INGENIERO",
  "nombre": "Pedro Rojas Vera",
  "rut": "15.906.334-8",
  "correoPersonal": "pedro.rojas@gmail.com",
  "telefono": "+56 9 1111 2222",
  "giro": null,
  "contactoNombre": null,
  "contactoCargo": null,
  "preguntaSeguridad": "¿Cuál es tu comida favorita?",
  "respuestaSeguridad": "porotos"
}
```

- 201: devuelve el usuario con `"correo": "projas@induztek.ejemplo"` y `"tieneContrasena": false`. Además se envía un código de activación al correo personal.
- 400 si `rol` es `SIN_ASIGNAR`. 409 si hay duplicado.

### PUT `/usuarios/{id}`

```json
{ "nombre": "Matías Godoy Herrera", "rut": "17.284.519-3", "telefono": "+56 9 8123 4567", "giro": null, "contactoNombre": null, "contactoCargo": null }
```

200 usuario.

### PATCH `/usuarios/{id}/rol`

```json
{ "rol": "TECNICO" }
```

200 usuario. Si la cuenta estaba en `SIN_ASIGNAR` y no tiene contraseña, se envía el código de activación (vence en 24 horas) al correo personal.

### PATCH `/usuarios/{id}/activo`

```json
{ "activo": false }
```

200 usuario.

### PUT `/usuarios/{id}/contrasena`

`actual` y `nueva` van cifradas, igual que en el login.

```json
{ "actual": "<hash actual>", "nueva": "<hash nuevo>" }
```

204. 401 si la actual no coincide. 400 si la nueva es igual a la actual.

## 2. Recuperar o activar la cuenta

En `correo` se puede enviar el correo de acceso o el personal. La `nuevaContrasena` se cifra **con el correo de acceso**.

### Por pregunta de seguridad

**POST `/usuarios/recuperacion/pregunta/verificar`**

```json
{ "correo": "mgodoy@induztek.ejemplo", "pregunta": "¿Cuál es el nombre de tu primera mascota?", "respuesta": "firulais" }
```

204 si coincide. 400 con un mensaje genérico si no.

**POST `/usuarios/recuperacion/pregunta`**

Los mismos campos más la contraseña nueva:

```json
{ "correo": "mgodoy@induztek.ejemplo", "pregunta": "¿Cuál es el nombre de tu primera mascota?", "respuesta": "firulais", "nuevaContrasena": "<hash>" }
```

204.

### Por código al correo personal

**POST `/usuarios/recuperacion/codigo`**

```json
{ "correo": "mgodoy@induztek.ejemplo" }
```

202:

```json
{ "mensaje": "Si el correo pertenece a una cuenta activa, enviamos un código a su correo personal que vence en 10 minutos." }
```

- La respuesta es siempre la misma, exista o no la cuenta.
- No se envía otro código si el anterior se pidió hace menos de 1 minuto.

**POST `/usuarios/recuperacion/codigo/verificar`**

```json
{ "correo": "mgodoy@induztek.ejemplo", "codigo": "482913" }
```

204 / 400.

**POST `/usuarios/recuperacion/codigo/restablecer`**

```json
{ "correo": "mgodoy@induztek.ejemplo", "codigo": "482913", "nuevaContrasena": "<hash>" }
```

204.

- El código queda usado.
- Tras 5 códigos incorrectos hay que pedir uno nuevo.
- Este mismo endpoint activa una cuenta nueva con el código de activación.

## 3. Instalaciones y equipos (ms-instalaciones)

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/regiones` | `[{ "id": 1, "nombre": "Región Metropolitana" }]` |
| GET | `/regiones/{id}/comunas` | `[{ "id": 1, "nombre": "Santiago", "regionId": 1, "regionNombre": "Región Metropolitana" }]` |
| GET | `/comunas?regionId=1` | igual que la anterior |
| GET | `/tipos-equipo` | `[{ "id": 1, "nombre": "Transformador", "clasificacion": "PRIMARIO" }]` |
| GET | `/tipos-equipo/{id}` | un tipo |
| GET | `/instalaciones?clienteId=8` | lista de instalaciones |
| GET | `/instalaciones/{id}` | una instalación |
| GET | `/instalaciones/{id}/equipos` | equipos de la instalación |
| POST | `/instalaciones` | 201 |
| PUT | `/instalaciones/{id}` | 200 |
| GET | `/equipos?instalacionId=&clienteId=&tipoEquipoId=` | lista de equipos |
| GET | `/equipos/{id}` | un equipo |
| POST | `/equipos` | 201 (409 si el TAG se repite en la instalación) |
| PUT | `/equipos/{id}` | 200 |

**Instalación (request):**

```json
{ "clienteId": 8, "nombre": "Planta Norte", "ubicacion": "Av. Industrial 1200", "comunaId": 1, "observacionesAcceso": "Portería 2" }
```

La respuesta agrega `id`, `clienteNombre`, `comunaNombre` y `regionNombre`. El `clienteId` debe ser un usuario `CLIENTE` activo (si no, 400).

**Equipo (request):**

```json
{
  "instalacionId": 1, "tag": "TR-01", "tipoEquipoId": 1,
  "marca": "ABB", "modelo": "RESIBLOC", "numeroSerie": "AB-2231", "anio": 2018,
  "observaciones": null,
  "potenciaKva": 1500.0, "tensionKv": 12.0, "corrienteA": 72.2, "maxContactosUohm": 50.0
}
```

La respuesta agrega `id`, `instalacionNombre`, `clienteId`, `tipoEquipoNombre` y `clasificacion`. El TAG se guarda en mayúsculas.

## 4. Instrumentos (ms-instrumentos)

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/instrumentos?estado=VIGENTE` | lista ordenada |
| GET | `/instrumentos/{id}` | un instrumento |
| POST | `/instrumentos` | 201 (409 si el número de serie se repite) |
| PUT | `/instrumentos/{id}` | 200 |
| GET | `/instrumentos/{id}/calibraciones` | historial de calibraciones |
| POST | `/instrumentos/{id}/calibraciones` | 201 |
| GET | `/instrumentos/{id}/vigencia?fecha=2026-11-24` | `{ "instrumentoId": 1, "fecha": "2026-11-24", "vigente": true, "fechaVencimiento": "2027-03-01" }` |

Los valores de `estado` son `VIGENTE`, `POR_VENCER` (30 días o menos), `VENCIDA` y `SIN_CALIBRACION`.

**Instrumento (request):**

```json
{ "nombreTipo": "Megóhmetro 5 kV", "marca": "Megger", "modelo": "MIT525", "numeroSerie": "MIT-0045821" }
```

**Instrumento (response):**

```json
{
  "id": 1, "nombreTipo": "Megóhmetro 5 kV", "marca": "Megger", "modelo": "MIT525", "numeroSerie": "MIT-0045821",
  "fechaCalibracion": "2026-03-01", "fechaVencimiento": "2027-03-01", "diasRestantes": 97,
  "estado": "VIGENTE", "estadoEtiqueta": "Vigente"
}
```

**Calibración (request):**

```json
{ "fechaCalibracion": "2026-11-20", "fechaVencimiento": "2027-11-20", "certificado": "CERT-2026-118" }
```

## 5. Visitas, protocolos y evidencias (ms-protocolo)

Quien hace la acción va como `usuarioId`: en el cuerpo o en la URL (`?usuarioId=`). El servidor revisa su perfil en ms-usuarios.

### Visitas

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/visitas?usuarioId=1&abiertas=true` | visitas del usuario (el Jefe ve todas, el cliente las suyas) |
| GET | `/visitas/{id}` | una visita |
| POST | `/visitas` | 201. Solo INGENIERO o JEFE_TECNICO |
| GET | `/visitas/{id}/equipos-disponibles?protocoloId=` | equipos sin protocolo en esa visita |
| POST | `/visitas/{id}/recepcion` | registra quién recibe y cierra la visita |
| GET | `/visitas/{id}/acta?usuarioId=1` | acta con visita, recepción y protocolos |

**Visita (request):**

```json
{
  "usuarioId": 5, "numeroOt": "OT-2026-0150", "instalacionId": 1, "fecha": "2026-11-25",
  "tipoVisita": "PRUEBAS_Y_MANT", "ingenieroCargoId": 5, "participantes": [1, 2]
}
```

`tipoVisita` puede ser `SOLO_PRUEBAS` o `PRUEBAS_Y_MANT`.

**Visita (response):**

```json
{
  "id": 3, "numeroOt": "OT-2026-0150", "instalacionId": 1, "instalacionNombre": "Planta Norte",
  "clienteId": 8, "clienteNombre": "Sanitaria Valle Central S.A.", "fecha": "2026-11-25",
  "tipoVisita": "PRUEBAS_Y_MANT", "tipoVisitaEtiqueta": "Pruebas y mantenimiento",
  "ingenieroCargoId": 5, "ingenieroNombre": "Andrea Contreras Silva",
  "participantes": [{ "id": 1, "nombre": "Matías Godoy Herrera" }],
  "cerrada": false, "fechaHoraCierre": null, "totalProtocolos": 0, "protocolosRegistrados": 0
}
```

**Recepción (request):**

```json
{ "receptorNombre": "Juan Arancibia", "receptorRut": "12.345.678-5", "receptorCargo": "Jefe de planta" }
```

### Catálogo de pruebas

**GET `/tipos-prueba?tipoEquipoId=1`**

```json
[{ "id": 1, "codigo": "AISLACION", "nombre": "Resistencia de aislación", "parametros": [{ "id": 1, "codigo": "LECTURA_1MIN", "nombre": "Lectura a 1 min", "unidad": "MΩ" }] }]
```

### Protocolos

Estados (`estado`): `BORRADOR`, `REGISTRADO_TERRENO`, `OBSERVADO`, `FORMALIZADO`, `ANULADO`.

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/protocolos?estado=&equipoId=&visitaId=&usuarioId=` | lista resumida |
| GET | `/protocolos/indicadores` | `{ "total": 12, "porEstado": { "FORMALIZADO": 5 }, "promedioDiasFormalizacion": 2.1 }` |
| GET | `/protocolos/{id}` | detalle completo |
| POST | `/protocolos` | 201 |
| PUT | `/protocolos/{id}` | 200. Solo quien lo creó, en Borrador u Observado |
| PATCH | `/protocolos/{id}/estado` | 200 |
| GET | `/protocolos/equipo/{equipoId}/historial` | protocolos anteriores del equipo |
| GET | `/protocolos/equipo/{equipoId}/mediciones-anteriores?antesDe=&excluir=` | últimas mediciones para comparar |

**Protocolo (request).** Con `"registrar": false` se guarda como Borrador sin validar todo. Con `"registrar": true` se valida completo y pasa a `REGISTRADO_TERRENO`; si falta algo, responde 400 con `campos`.

```json
{
  "usuarioId": 1, "visitaId": 3, "equipoId": 1,
  "temperaturaC": 18.5, "humedadPct": 55.0,
  "descripcionMantenimiento": "Limpieza de aisladores", "observaciones": null,
  "registrar": true,
  "pruebas": [{
    "tipoPruebaId": 1, "elemento": "Fase H1-X1", "momento": "UNICA",
    "realizada": true, "motivoNoRealizada": null,
    "instrumentoId": 1, "resultado": "APROBADO", "comentario": null,
    "ingresadoManualmente": false,
    "mediciones": [{ "parametroId": 1, "valor": 2500.0 }]
  }]
}
```

- `resultado` puede ser `APROBADO`, `OBSERVADO` o `RECHAZADO`.
- El instrumento debe estar vigente en la fecha de la prueba.
- `descripcionMantenimiento` es obligatoria en visitas `PRUEBAS_Y_MANT`.

**Detalle (response).** Además de los datos del request, trae:

- `id` (ej. `PT-2026-0001`), `numero`, `numeroOt`, `tipoVisita`, `equipoTag`, `tipoEquipoNombre`, `instalacionNombre`, `clienteNombre`;
- `usuarioRegistraId`, `usuarioRegistraNombre`, `fechaHoraPrueba`, `estado`, `estadoEtiqueta`, `protocoloReemplazadoId`;
- `pruebas` (cada medición con `parametroCodigo`, `parametroNombre` y `unidad`);
- `historial`: `[{ "estadoAnterior": "BORRADOR", "estadoNuevo": "REGISTRADO_TERRENO", "usuarioId": 1, "usuarioNombre": "Matías Godoy Herrera", "fechaHora": "...", "motivo": null }]`;
- `evidencias`.

**Cambiar estado (botón Formalizar del Ingeniero o Jefe):**

```json
{ "usuarioId": 5, "estado": "FORMALIZADO", "motivo": null }
```

- `FORMALIZADO` y `OBSERVADO` solo se aplican desde `REGISTRADO_TERRENO`.
- `ANULADO` se aplica desde cualquier estado menos Formalizado o Anulado.
- `OBSERVADO` y `ANULADO` exigen `motivo` (400 con `campos.motivo`).
- 403 si el usuario no es INGENIERO ni JEFE_TECNICO.

### Evidencias (fotos)

| Método | Ruta | Respuesta |
|---|---|---|
| POST | `/protocolos/{id}/evidencias` (multipart) | 201 |
| GET | `/protocolos/{id}/evidencias` | lista |
| GET | `/evidencias/{id}/archivo` | la imagen |
| DELETE | `/evidencias/{id}?usuarioId=1` | 204 |

Reglas: JPG, PNG o WEBP de hasta 5 MB, con un máximo de 10 fotos por protocolo.

Partes del multipart: `archivo` (la imagen), `usuarioId` y `descripcion` (opcional).

```kotlin
@Multipart
@POST("protocolos/{id}/evidencias")
suspend fun subirEvidencia(
    @Path("id") id: String,
    @Part archivo: MultipartBody.Part,
    @Part("usuarioId") usuarioId: RequestBody,
    @Part("descripcion") descripcion: RequestBody?
): EvidenciaDto
```

**Evidencia (response):**

```json
{ "id": 4, "nombreOriginal": "foto1.jpg", "tipoContenido": "image/jpeg", "tamano": 183422, "descripcion": "Placa", "usuarioId": 1, "fechaHora": "2026-11-24T10:41:00", "url": "/evidencias/4/archivo" }
```
