# Induztek Protocolos — Microservicios

Backend de la aplicación móvil Induztek Protocolos (DSY1105).

| Proyecto | Puerto | Base de datos | Consulta a |
|---|---|---|---|
| eureka-server | 8761 | — | — |
| api-gateway | 8090 | — | todos |
| ms-usuarios | 8081 | induztek_usuarios | — |
| ms-instalaciones | 8082 | induztek_instalaciones | ms-usuarios |
| ms-instrumentos | 8083 | induztek_instrumentos | — |
| ms-protocolo | 8084 | induztek_protocolos | ms-usuarios, ms-instalaciones, ms-instrumentos |

## Requisitos

- Java 21
- MySQL en `localhost:3306` (usuario `root`, sin contraseña). Cada microservicio crea su base al iniciar.

## Ejecución

Iniciar en este orden: `eureka-server`, `ms-usuarios`, `ms-instalaciones`, `ms-instrumentos`, `ms-protocolo` y `api-gateway`.

```
mvnw spring-boot:run
```

- Panel de Eureka: http://localhost:8761
- Punto de entrada único: http://localhost:8090 (desde el emulador Android: http://10.0.2.2:8090)
- Swagger de cada microservicio: `http://localhost:<puerto>/swagger-ui.html`
- Todos los endpoints con ejemplos de JSON para Retrofit: [`docs/API.md`](docs/API.md)

Variables de entorno opcionales: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `EUREKA_URL`; en ms-usuarios `MAIL_USERNAME`, `MAIL_PASSWORD` y `CORREO_DOMINIO`; en ms-protocolo `EVIDENCIAS_DIR`.

Si ya existía la base `induztek_usuarios` de una versión anterior, bórrala antes de iniciar ms-usuarios (`DROP DATABASE induztek_usuarios;`), porque la tabla `usuario` cambió.

## Códigos de respuesta

Los endpoints que devuelven listas responden `204 No Content` cuando no hay resultados. Errores: 400 validación, 401 credenciales, 403 permiso, 404 no existe, 405 método no permitido, 409 duplicado, 429 demasiados intentos, 503 otro microservicio no disponible.

## Pruebas y cobertura

```
mvnw clean verify
```

Reporte de cobertura: `target/site/jacoco/index.html` (mínimo exigido: 80 % de líneas).

| Microservicio | Pruebas | Cobertura de líneas |
|---|---|---|
| ms-usuarios | 77 | 99 % |
| ms-instalaciones | 32 | 96 % |
| ms-instrumentos | 16 | 99 % |
| ms-protocolo | 27 | 96 % |
