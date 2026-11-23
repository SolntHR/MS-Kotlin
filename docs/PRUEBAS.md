# Pruebas del backend

Ejecutar desde la raíz con JDK 21:

```powershell
.\mvnw.cmd clean verify
```

El build compila Eureka, gateway y usuarios. En usuarios se ejecutan pruebas unitarias y de integración HTTP con MockMvc, Flyway y H2 en memoria. El SMTP se simula para verificar destinatario, contenido y fallos sin entregar mensajes fuera del entorno.

## Cobertura funcional

| Archivo de pruebas | Riesgo cubierto |
|---|---|
| `RegistroValidatorTest.kt` | Dígito verificador, teléfono, nombres, normalización y preguntas de Android |
| `SecretServiceTest.kt` | Tokens aleatorios, HMAC ligado al usuario y sales distintas |
| `RateLimiterTest.kt` | Intentos máximos, separación por identidad y vencimiento de ventana |
| `RecoveryMailerTest.kt` | Correo al destinatario correcto y traducción de fallo SMTP |
| `RegistroApiTest.kt` | Contrato de seis campos, estado pendiente, duplicados y escalamiento por JSON |
| `LoginApiTest.kt` | Credenciales, cuentas inactivas, caducidad, logout y bloqueo de ataques |
| `RecuperacionApiTest.kt` | Primera clave, correo, código de un uso incluso con solicitudes simultáneas, vencimiento, cinco errores, reenvío y rollback SMTP |
| `RolesApiTest.kt` | Solo ADMIN, último administrador, activación y revocación por cambio de rol |
| `SecurityApiTest.kt` | Bearer inválido, acceso privado, Swagger público y errores HTTP |
| `UsuariosConsultaTest.kt` | Perfil público sin hashes, ficha y límites de paginación |
| `AdminBootstrapTest.kt` | Primer ADMIN seguro, reinicios idempotentes y prevención de elevación por bootstrap |

Los archivos Android no se modifican ni se ejecutan desde este backend. Este informe no sustituye la exigencia de pruebas unitarias de la app Android indicada en el documento de evaluación.

## Informes

- Resultados JUnit: `ms-usuarios/target/surefire-reports/`.
- Cobertura HTML: `ms-usuarios/target/site/jacoco/index.html`.
- Cobertura XML: `ms-usuarios/target/site/jacoco/jacoco.xml`.

## Resultado verificado el 23 de noviembre de 2026

Compilación limpia con Maven Wrapper y JDK 21: **BUILD SUCCESS** en los tres módulos. **48 pruebas, cero fallos, cero errores y cero omitidas**, distribuidas en once archivos: cuatro de pruebas unitarias y siete de integración.

| Medida JaCoCo de `ms-usuarios` | Resultado |
|---|---|
| Líneas | 332 de 343, **96,79 %** |
| Instrucciones | 3317 de 3641, **91,10 %** |
| Ramas | 175 de 215, **81,40 %** |

`verify` exige al menos 80 % de líneas mediante JaCoCo y fallará si se incumple. Los porcentajes corresponden a usuarios; no se atribuye esa cobertura a gateway, Eureka ni a Android.

Se probó además el flujo real por HTTP y SMTP local con los ejecutables Java, Eureka y Mailpit 1.27.8: registro, aprobación de rol, envío y lectura del código, primera contraseña, login, perfil, logout y rechazo del token revocado. Swagger se consultó pasando por el gateway y ambos clientes aparecieron en Eureka. Los procesos temporales se detuvieron tras la verificación; no se dejaron servicios ejecutándose ni credenciales iniciales preconfiguradas.

Docker no está disponible en esta máquina: no se ha ejecutado Compose ni validado la migración contra PostgreSQL real. Tampoco se ha probado entrega a un proveedor SMTP externo o la integración Retrofit desde Android. Esas verificaciones deben hacerse con dichos entornos; las pruebas actuales sí ejecutan Flyway y persistencia sobre H2 compatible con PostgreSQL.

## Prueba manual con SMTP local

Iniciar los servicios como indica README y usar:

```powershell
. .\scripts\Load-Env.ps1
.\scripts\Smoke-Test.ps1
```

La prueba usa la API oficial de [Mailpit](https://mailpit.axllent.org/docs/api-v1/) para recuperar únicamente el mensaje de la cuenta sintética que acaba de registrar. Verifica el flujo pasando por gateway, con HTTP y SMTP reales. No imprime tokens ni códigos. Deja esa cuenta de prueba desactivada para conservar la trazabilidad.

También se puede usar Swagger: registrar usuario, iniciar sesión ADMIN, pulsar Authorize con el token, asignar rol, solicitar código y leerlo en Mailpit, restablecer, iniciar sesión con el usuario y consultar `/usuarios/me`. Probar un segundo uso del código debe producir `400`; usar el token tras logout debe producir `401`.
