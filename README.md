# ModaLink — Backend

Backend de **ModaLink**, una plataforma de networking profesional para la industria creativa y de la moda. Permite gestionar múltiples perfiles, crear proyectos, reclutar equipos, coordinar agendas, firmar contratos y comunicarse mediante chat privado y grupal.

Proyecto final de la carrera **Analista en Sistemas de Computación**, desarrollado con **Proceso Unificado (UP)**, foco en testing, validaciones de negocio en backend y persistencia, y trazabilidad mediante auditoría.

> **Estado actual (2026-11-10):** **65 de 76 casos de uso implementados y testeados (85,5 %)**.
>
> Módulos completos: autenticación y usuarios, administración, perfiles y disponibilidad, proyectos y equipo, actividades y cronograma, contratos, conexiones, presencia y mensajería.
>
> Pendientes: los **11 UC del módulo de publicaciones** (`UC-19`, `UC-20`, `UC-21`, `UC-22`, `UC-61` a `UC-66` y `UC-69`).
>
> Seguimiento detallado: [`docs/5_estado_sistema/estado_uc.md`](docs/5_estado_sistema/estado_uc.md).

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1.0 (Web, Security, Data JPA, Validation, Actuator, AOP) |
| Base de datos | PostgreSQL 17 (Docker Compose) / PostgreSQL 14+ |
| Autenticación | JWT (JJWT 0.13.0) en cookies HTTP-only + CSRF |
| Autenticación federada | Google OAuth2 (Authorization Code) + `usuario_identidad` |
| Hashing de contraseñas | BCrypt |
| Imágenes | Thumbnailator, TwelveMonkeys (JPEG/WebP) y WebP ImageIO |
| Exportación PDF | OpenPDF |
| Testing | JUnit Jupiter, Mockito, MockMvc, Spring Security Test, Testcontainers (PostgreSQL 17), JaCoCo |
| Build | Maven 3.9+ |
| Frontend relacionado | [modalink-frontend](https://github.com/mgroko/modalink-frontend) (Vue 3 + Vite) |

## Estructura del proyecto

```text
src/main/java/org/mgroko/backend/
├── Application.java
├── admin/               # Usuarios, catálogos, configuración, dashboard y cronograma
├── actividades/         # Actividades, asignaciones, cronograma y requerimientos de actividad
├── auditoria/           # Consulta y exportación del log de auditoría
├── auth/                # Login local, registro, Google OAuth2 y sesión
├── calendario/          # Agenda, jornada laboral y bloqueos
├── common/              # DTOs compartidos, errores y excepciones globales
├── conexiones/          # Solicitudes de conexión y red de contactos
├── contratos/           # Generación, modificación, firma, baja y búsqueda de contratos
├── home/                # Resumen de inicio y oportunidades
├── invitaciones/        # Invitaciones generales y por actividad
├── mensajeria/          # Chat privado, chat grupal y SSE
├── modelo/              # Entidades JPA y enums del dominio
├── notificaciones/      # Bandeja, resumen y marcado de notificaciones
├── perfiles/            # Perfiles, T&C, habilidades, fotos y reseñas
├── postulaciones/       # Postulaciones generales y por actividad
├── presencia/           # Heartbeat, snapshot y SSE de presencia
├── proyectos/           # Proyectos, miembros, cronograma, reemplazos, historial y calificaciones
├── reemplazos/          # Reemplazo automático de integrantes
├── reportes/            # Reportes de perfil y publicación
├── requerimientos/      # Búsqueda global de requerimientos y elegibilidad
├── repositorio/         # Repositorios Spring Data JPA y proyecciones
├── security/            # JWT, CSRF, CORS y reglas de autorización
├── storage/             # Almacenamiento local y procesamiento de imágenes
├── ubicacion/           # Catálogo geográfico y ubicación del usuario
└── usuario/             # Datos personales, baja y reactivación de cuenta

src/main/resources/
├── application.properties
└── georef/

src/test/java/org/mgroko/backend/   # Tests unitarios, MVC e integración
src/test/resources/
├── ModaLinkBD.sql                  # Esquema canónico usado por Testcontainers
└── migraciones-test/
```

## Requisitos previos

- JDK 25
- Maven 3.9+
- PostgreSQL 14 o superior
- Docker (solo para tests de integración con Testcontainers)

## Configuración

La configuración base vive en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/modalink
spring.datasource.username=postgres
spring.datasource.password=postgres

server.port=8080
spring.jpa.hibernate.ddl-auto=validate
```

Variables de entorno principales:

| Variable | Descripción | Default (dev) |
|---|---|---|
| `JWT_SECRET` | Clave de firma de los JWT | Valor de desarrollo |
| `JWT_EXPIRATION_MS` | Expiración del JWT en ms | `3600000` (1 hora) |
| `GOOGLE_CLIENT_ID` | Client ID de Google Cloud | Placeholder |
| `GOOGLE_CLIENT_SECRET` | Client Secret de Google Cloud | Placeholder |
| `GOOGLE_REDIRECT_URI` | Redirect URI de Google | `http://localhost:8080/login/oauth2/code/google` |
| `FRONTEND_URL` | URL del frontend para CORS y redirecciones | `http://localhost:5173` |
| `COOKIE_SECURE` | Cookie `Secure` | `false` |
| `COOKIE_SAME_SITE` | `SameSite` de cookies | `Lax` |
| `ADMIN_EMAIL` | Correo del administrador inicial | `admin@modalink.com` |
| `ADMIN_PASSWORD` | Contraseña del administrador inicial | `admin123` |
| `ADMIN_DNI` | DNI del administrador inicial | `00000000` |
| `DB_API_USER` | Usuario restringido para auditoría en producción | Comentado |
| `DB_API_PASSWORD` | Password del usuario restringido | Comentado |

> ⚠️ En producción, cambiar `JWT_SECRET`, `ADMIN_PASSWORD` y las credenciales de base de datos. Usar `COOKIE_SECURE=true` con HTTPS y `COOKIE_SAME_SITE=None` si frontend y backend quedan en sitios distintos.

Propiedades funcionales relevantes:

| Propiedad | Descripción |
|---|---|
| `app.presencia.umbral-en-linea` | Tiempo sin heartbeat para considerar un perfil desconectado |
| `app.presencia.timeout-stream` | Timeout del SSE de presencia |
| `app.mensajeria.timeout-stream` | Timeout del SSE de mensajería |
| `app.mensajeria.intervalo-ping-ms` | Ping del SSE de mensajería |
| `agenda.reprogramacion-automatica` | Reprogramación al cambiar jornada/margen |
| `agenda.reconciliacion-cronograma.cron` | Reconciliación periódica de cronogramas |
| `reemplazos.max-candidatos` | Máximo de candidatos por vacante |

## Puesta en marcha

### 1. Base de datos

Con Docker Compose:

```bash
docker compose up -d
```

O manualmente:

```sql
CREATE DATABASE "modalink";
```

Luego cargar el esquema canónico:

```bash
psql -U postgres -d modalink -f "docs/3_diseño/ModaLinkBD.sql"
```

### 2. Backend

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

Verificación rápida:

```bash
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

> El proyecto no incluye Maven Wrapper (`mvnw`); usar `mvn`.

## Esquema y migraciones

- **Esquema canónico versionado:** `docs/3_diseño/ModaLinkBD.sql`.
- **Esquema de tests:** `src/test/resources/ModaLinkBD.sql`.
- **Migraciones incrementales:** `docs/3_diseño/migraciones/` (carpeta parcialmente ignorada por git).
- Hibernate usa `ddl-auto=validate`: la base debe coincidir exactamente con las entidades JPA.
- Antes de levantar una versión sobre una base existente, aplicar en orden las migraciones locales disponibles.
- Los tests de integración levantan PostgreSQL 17 con Testcontainers y cargan `src/test/resources/ModaLinkBD.sql`.

## Seguridad

- JWT firmado en cookie HTTP-only.
- CSRF mediante `CookieCsrfTokenRepository`; los `POST`, `PUT`, `PATCH` y `DELETE` requieren token.
- Endpoints públicos: login, registro, reactivación de cuenta, OAuth2, health y archivos subidos.
- `/admin/**` requiere rol `ADMINISTRADOR`.
- El resto de la API requiere rol `USUARIO` y, en la mayoría de los casos, un perfil activo.
- CORS habilitado para `FRONTEND_URL`.
- Validaciones de negocio en backend y en base de datos (checks, constraints, triggers e índices únicos parciales).

### Autenticación

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/auth/registro` | Registro local con validación de correo/DNI y edad mínima |
| `POST` | `/auth/login` | Login con correo y contraseña |
| `POST` | `/auth/logout` | Logout idempotente y limpieza de cookie |
| `GET` | `/auth/me` | Usuario autenticado |
| `GET` | `/auth/google` | Inicio de Google OAuth2 |
| `GET` | `/auth/google/pendiente` | Datos de Google para onboarding |
| `POST` | `/auth/google/completar` | Completa DNI, fecha de nacimiento y género |
| `POST` | `/auth/reactivar-cuenta` | Reactivación desde el login |

Para Google OAuth2, registrar como redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

## Módulos y endpoints principales

### Usuarios, cuenta y administración

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/admin/usuarios/buscar` | Búsqueda administrativa de usuarios |
| `PATCH` | `/admin/usuarios/{id}/habilitar` | Habilitar usuario |
| `PATCH` | `/admin/usuarios/{id}/deshabilitar` | Deshabilitar usuario con motivo y duración |
| `POST` | `/usuario/solicitar-baja` | Solicitar baja de cuenta |
| `POST` | `/usuario/reactivar-cuenta` | Reactivar cuenta dentro del plazo |
| `PUT` | `/usuario/datos-personales` | Modificar datos personales |
| `GET` | `/admin/dashboard/resumen` | Resumen del dashboard administrativo |
| `GET` | `/admin/auditoria` | Consulta paginada de auditoría |
| `GET` | `/admin/auditoria/export?formato=CSV\|PDF` | Exportación de auditoría |

### Perfiles, catálogos y disponibilidad

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/perfiles` | Crear perfil profesional |
| `GET` | `/perfiles/buscar` | Buscar perfiles |
| `GET` | `/perfiles/{idPerfil}` | Ver perfil |
| `PUT` | `/perfiles/{idPerfil}` | Editar perfil propio |
| `DELETE` | `/perfiles/{idPerfil}` | Solicitar baja del perfil |
| `POST` | `/perfiles/{idPerfil}/reactivar` | Reactivar perfil |
| `PATCH` | `/perfiles/{idPerfil}/activar` | Cambiar perfil activo |
| `GET` | `/usuarios/me/perfiles` | Perfiles del usuario autenticado |
| `POST` | `/perfiles/{idPerfil}/foto` | Subir foto de perfil |
| `GET/PUT` | `/perfiles/{idPerfil}/terminos-condiciones` | Consultar o guardar T&C |
| `GET/POST` | `/perfiles/{idPerfil}/habilidades` | Gestionar habilidades del perfil |
| `GET` | `/perfiles/{idPerfil}/calificaciones` | Reseñas del perfil |
| `GET` | `/profesiones` | Catálogo de profesiones |
| `GET` | `/profesiones/{id}/caracteristicas-tecnicas` | Características técnicas |
| `GET` | `/habilidades` | Catálogo de habilidades |
| `GET` | `/ubicaciones/provincias`, `/ubicaciones/localidades` | Catálogo geográfico |

Calendario y jornada:

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/calendario/perfil/{id}` | Agenda del perfil |
| `PUT` | `/calendario/jornada` | Configurar jornada laboral y margen |
| `POST` | `/calendario/bloqueos` | Crear bloqueo manual |
| `DELETE` | `/calendario/bloqueos/{idBloqueo}` | Eliminar bloqueo |

### Proyectos, equipo y actividades

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/proyectos` | Crear proyecto en borrador |
| `GET` | `/proyectos/buscar` | Buscar proyectos visibles |
| `GET` | `/proyectos/propios` | Proyectos del perfil activo |
| `GET` | `/proyectos/{idProyecto}` | Detalle del proyecto |
| `PUT` | `/proyectos/{idProyecto}` | Modificar proyecto |
| `POST` | `/proyectos/{idProyecto}/publicar` | Publicar proyecto |
| `POST` | `/proyectos/{idProyecto}/confirmar` | Confirmar proyecto y planificar |
| `POST` | `/proyectos/{idProyecto}/finalizar` | Finalizar proyecto |
| `POST` | `/proyectos/{idProyecto}/cancelar` | Cancelar proyecto |
| `POST` | `/proyectos/{idProyecto}/reprogramar` | Reintentar reprogramación |
| `GET` | `/proyectos/{idProyecto}/miembros` | Miembros activos |
| `DELETE` | `/proyectos/{idProyecto}/miembros/{idMiembro}` | Eliminar integrante |
| `POST` | `/proyectos/{idProyecto}/baja` | Baja voluntaria |
| `GET` | `/proyectos/{idProyecto}/historial` | Historial de miembros |
| `GET/POST` | `/proyectos/{idProyecto}/calificaciones` | Calificaciones del equipo |
| `GET/POST` | `/proyectos/{idProyecto}/reemplazos...` | Reemplazo automático de integrantes |
| `POST` | `/proyectos/{idProyecto}/actividades` | Crear actividad |
| `PUT` | `/proyectos/{idProyecto}/actividades/{idActividad}` | Modificar actividad |
| `PATCH` | `/proyectos/{idProyecto}/actividades/{idActividad}/estado` | Cambiar estado |
| `GET` | `/proyectos/{idProyecto}/actividades` | Tabla de planificación |
| `GET` | `/proyectos/{idProyecto}/actividades/buscar` | Buscar actividades |
| `GET` | `/proyectos/{idProyecto}/cronograma` | Cronograma CPM |
| `POST/DELETE` | `/proyectos/{idProyecto}/actividades/{idActividad}/asignaciones...` | Asignar o desasignar miembros |

### Postulaciones, invitaciones y requerimientos

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/proyectos/{idProyecto}/postulaciones/general` | Postularse a un requerimiento general |
| `POST` | `/proyectos/{idProyecto}/postulaciones/actividad` | Postularse a una actividad |
| `GET` | `/postulaciones/mias` | Postulaciones del perfil activo |
| `DELETE` | `/proyectos/{idProyecto}/postulaciones/...` | Dar de baja postulación pendiente |
| `GET` | `/proyectos/{idProyecto}/postulaciones/buscar` | Buscar postulaciones |
| `POST` | `/proyectos/{idProyecto}/postulaciones/.../aceptar` | Aceptar postulación |
| `POST` | `/proyectos/{idProyecto}/postulaciones/.../rechazar` | Rechazar postulación |
| `POST` | `/proyectos/{idProyecto}/invitaciones/general` | Invitar al proyecto |
| `POST` | `/proyectos/{idProyecto}/invitaciones/actividad` | Invitar a una actividad |
| `POST` | `/invitaciones/.../aceptar`, `/rechazar` | Gestionar invitación |
| `GET` | `/invitaciones/buscar` | Buscar invitaciones |
| `GET` | `/requerimientos-generales/buscar` | Buscar requerimientos generales |
| `GET` | `/requerimientos-actividad/buscar` | Buscar requerimientos de actividad |
| `GET` | `/home/resumen` | Oportunidades y resumen de inicio |

### Contratos

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/proyectos/{idProyecto}/contratos/opciones` | Tipos de acuerdo y destinatarios |
| `POST` | `/proyectos/{idProyecto}/contratos/previsualizar` | Previsualizar contrato |
| `POST` | `/proyectos/{idProyecto}/contratos` | Generar contrato |
| `GET/PUT/DELETE` | `/proyectos/{idProyecto}/contratos/{idContrato}` | Consultar, modificar o eliminar |
| `POST` | `/proyectos/{idProyecto}/contratos/{idContrato}/firmar` | Firmar contrato |
| `POST` | `/proyectos/{idProyecto}/contratos/{idContrato}/rechazar` | Rechazar contrato |
| `GET` | `/contratos/buscar` | Búsqueda global de contratos |
| `GET` | `/proyectos/{idProyecto}/contratos/{idContrato}/eventos` | Historial del contrato |

### Conexiones, presencia y mensajería

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/solicitudes-conexion` | Enviar solicitud de conexión |
| `GET` | `/solicitudes-conexion` | Buscar solicitudes |
| `POST` | `/solicitudes-conexion/{id}/aceptar` | Aceptar solicitud |
| `POST` | `/solicitudes-conexion/{id}/rechazar` | Rechazar solicitud |
| `DELETE` | `/solicitudes-conexion/{id}` | Cancelar solicitud enviada |
| `GET` | `/conexiones` | Red de contactos activa |
| `GET` | `/conexiones/resumen` | Resumen de la red |
| `DELETE` | `/conexiones/{idConexion}` | Eliminar conexión |
| `POST` | `/presencia/heartbeat` | Registrar actividad del perfil |
| `GET` | `/presencia` | Snapshot de presencia |
| `GET` | `/presencia/stream` | SSE de presencia |

Chat privado (UC-42):

| Método | Endpoint | Descripción |
|---|---|---|
| `GET/POST` | `/conversaciones` | Listar u obtener/crear conversación |
| `GET` | `/conversaciones/resumen` | Badge de conversaciones no leídas |
| `GET/POST` | `/conversaciones/{id}/mensajes` | Historial o envío de mensaje |
| `PATCH` | `/conversaciones/{id}/leido` | Marcar conversación como leída |
| `POST` | `/conversaciones/{id}/escribiendo` | Aviso efímero de escritura |
| `GET` | `/mensajes/stream` | SSE de mensajes privados y grupales |

Chat grupal (UC-41):

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/proyectos/{idProyecto}/chat` | Abrir chat y listar participantes |
| `GET/POST` | `/proyectos/{idProyecto}/chat/mensajes` | Historial o envío de mensaje |
| `PATCH` | `/proyectos/{idProyecto}/chat/leido` | Marcar notificaciones del proyecto como leídas |

Reglas generales de mensajería:

- El emisor siempre es el perfil activo de la sesión.
- El chat privado requiere una conexión `ACTIVA` entre ambos perfiles.
- El chat grupal requiere membresía activa en un proyecto `PUBLICADO` o `CONFIRMADO`.
- Los mensajes admiten hasta 1000 caracteres.
- El historial de un chat privado queda en solo lectura si se elimina la conexión.
- En el chat grupal se puede leer el historial después de finalizar o cancelar el proyecto, pero no enviar.
- No se audita el contenido de los mensajes.
- Los buses SSE son en memoria y asumen una única instancia. Para escalar horizontalmente hace falta Redis Pub/Sub o PostgreSQL `LISTEN/NOTIFY`.

### Notificaciones, reportes y administración

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/notificaciones` | Bandeja paginada |
| `GET` | `/notificaciones/resumen` | Total y no leídas |
| `PATCH` | `/notificaciones/{id}/leida` | Marcar una notificación como leída |
| `PATCH` | `/notificaciones/leidas` | Marcar todas como leídas |
| `POST` | `/perfiles/{idPerfil}/reportes` | Reportar perfil |
| `GET` | `/admin/reportes-perfil` | Bandeja de reportes de perfil |
| `PATCH` | `/admin/reportes-perfil/{id}/estado` | Resolver o descartar reporte |

Catálogos administrativos:

- `/admin/habilidades`
- `/admin/caracteristicas-tecnicas`
- `/admin/unidades-medida`
- `/admin/tipos-contrato`
- `/admin/configuracion/schedulers/baja`
- `/admin/cronograma`

## Testing

```bash
mvn test
mvn verify
```

- Tests unitarios de servicios, mappers y DTOs.
- Tests MVC con `@WebMvcTest` y MockMvc.
- Tests de integración con PostgreSQL real mediante Testcontainers.
- Tests de concurrencia en flujos críticos de conexiones y aceptación de postulaciones.
- Reporte de cobertura con JaCoCo en `target/site/jacoco/index.html`.

Última ejecución completa de la suite:

```text
2392 tests, 0 failures, 0 errors
```

> Los tests de integración requieren Docker disponible. Si Docker no está corriendo, los tests que extienden `AbstractPostgresIntegrationTest` fallan.

## Documentación

- Requisitos y casos de uso: [`docs/1_requisitos/casos_de_uso.md`](docs/1_requisitos/casos_de_uso.md)
- Estado de cada UC: [`docs/5_estado_sistema/estado_uc.md`](docs/5_estado_sistema/estado_uc.md)
- Análisis: [`docs/2_analisis/`](docs/2_analisis/)
- Diseño y esquema: [`docs/3_diseño/`](docs/3_diseño/)
- Controles de base de datos: [`docs/calidad/`](docs/calidad/)
- Fichas de frontend: [`docs/frontend/`](docs/frontend/)
- Informe de testing: [`docs/6_presentaciones/tests.md`](docs/6_presentaciones/tests.md)

Fichas específicas:

- [`docs/frontend/ficha-tecnica-mensajes-privados.md`](docs/frontend/ficha-tecnica-mensajes-privados.md)
- [`docs/frontend/ficha-tecnica-chat-grupal.md`](docs/frontend/ficha-tecnica-chat-grupal.md)
- [`docs/calidad/controles_mensajeria.md`](docs/calidad/controles_mensajeria.md)
- [`docs/calidad/controles_chat_grupal.md`](docs/calidad/controles_chat_grupal.md)
- [`docs/calidad/controles_conexiones.md`](docs/calidad/controles_conexiones.md)
- [`docs/calidad/controles_presencia.md`](docs/calidad/controles_presencia.md)

## Convenciones del proyecto

- Toda validación de negocio se implementa en backend y se refuerza en base de datos con constraints, checks, triggers o índices únicos.
- El frontend no contiene lógica de validación.
- Los controladores devuelven DTOs; no exponen entidades JPA.
- Las fechas se generan con `Clock` inyectado para facilitar tests deterministas.
- Los flujos críticos usan transacciones y locks pesimistas cuando hay riesgo de concurrencia.
- Las excepciones de dominio se manejan centralizadamente en `GlobalExceptionHandler`.
- Los cambios críticos se auditan mediante triggers de PostgreSQL.
- No se audita el contenido de mensajes.
- Cada módulo nuevo debe incluir tests unitarios, MVC y de integración antes de considerarse cerrado.

## Roadmap

- [x] Autenticación local, Google OAuth y gestión de cuenta
- [x] Perfiles, catálogos, T&C, habilidades, fotos y reseñas
- [x] Proyectos, requerimientos, postulaciones e invitaciones
- [x] Equipo, bajas, reemplazos, historial y calificaciones
- [x] Actividades, cronograma CPM, calendario, jornada y reprogramación
- [x] Contratos: generación, modificación, firma, baja y búsqueda
- [x] Conexiones, presencia y red de contactos
- [x] Mensajería privada y chat grupal
- [x] Notificaciones, reportes de perfil, auditoría y administración
- [x] Búsqueda global de requerimientos generales y de actividad
- [ ] Publicaciones (11 UC): alta, baja, modificación, búsqueda, colaboración, interacción y reporte
