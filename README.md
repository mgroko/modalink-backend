# ModaLink — Backend

Backend de **ModaLink**, una plataforma de networking profesional para la industria creativa y de la moda. Permite gestionar múltiples perfiles, publicar trabajo, crear proyectos, reclutar equipos, coordinar agendas, firmar contratos, construir una red de contactos y comunicarse mediante chat privado y grupal.

Proyecto final de la carrera **Analista en Sistemas de Computación**, desarrollado con **Proceso Unificado (UP)**, foco en testing, validaciones de negocio en backend y persistencia, y trazabilidad mediante auditoría.

> **Estado actual (2026-11-26):** **76 de 76 casos de uso implementados y testeados (100 %)**.
>
> Módulos completos: autenticación y usuarios, administración, perfiles y disponibilidad, publicaciones, proyectos y equipo, actividades y cronograma, contratos, conexiones, presencia y mensajería.
>
> Extensiones fuera del catálogo numerado: detalle administrativo de solo lectura (perfiles, proyectos y contratos), colaboraciones automáticas por similitud de imágenes, presencia por perfil, informe ejecutivo de proyecto, tablero de casting virtual, normas de convivencia y manual in-app.
>
> Seguimiento detallado: [`docs/5_estado_sistema/estado_uc.md`](docs/5_estado_sistema/estado_uc.md).
> Prueba demo online: https://modalink.duckdns.org/ (rama deploy/azure)

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
| Coincidencia de imágenes | OpenCV (pHash, ORB y multicrop) para colaboraciones automáticas |
| Exportación PDF | OpenPDF |
| Manual in-app | CommonMark (Markdown → HTML) |
| Email | Spring Boot Mail (SMTP) + Mailpit en desarrollo |
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
├── config/              # Configuración de async, reloj y recursos web
├── conexiones/          # Solicitudes de conexión y red de contactos
├── contratos/           # Contratos: borrador, emisión, firma, baja, búsqueda y PDF
├── home/                # Resumen de inicio y oportunidades
├── invitaciones/        # Invitaciones generales y por actividad
├── manual/              # Manual in-app: índice, capítulos, búsqueda y errores frecuentes
├── mensajeria/          # Chat privado, chat grupal y SSE
├── modelo/              # Entidades JPA y enums del dominio
├── normas/              # Normas de convivencia versionadas
├── notificaciones/      # Bandeja, resumen y marcado de notificaciones
├── perfiles/            # Perfiles, T&C, habilidades, fotos y reseñas
├── polaroids/           # Polaroids del perfil y su orden
├── postulaciones/       # Postulaciones generales, por actividad y casting virtual
├── presencia/           # Heartbeat, snapshot y SSE de presencia
├── proyectos/           # Proyectos, miembros, cronograma, reemplazos, historial, informe y calificaciones
├── publicaciones/       # Publicaciones, interacciones, colaboradores y solicitudes de colaboración
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

Los capítulos Markdown de `docs/manual/` se empaquetan como recursos en `manual/` durante el build.

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
| `NOTIFICACIONES_EMAIL_HABILITADO` | Activa el scheduler de emails | `false` |
| `NOTIFICACIONES_EMAIL_REMITENTE` | Remitente de los emails | `no-responder@modalink.local` |
| `EMAIL_BASE_URL` | URL base pública del backend para las imágenes de los emails | `http://localhost:8080` |
| `MAIL_HOST` | Host SMTP (Mailpit en desarrollo) | `localhost` |
| `MAIL_PORT` | Puerto SMTP (Mailpit en desarrollo) | `1025` |
| `MAX_FILE_SIZE` | Tamaño máximo por archivo multipart | `10MB` |
| `MAX_REQUEST_SIZE` | Tamaño máximo del request multipart completo | `110MB` |
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
| `app.colaboracion-automatica.habilitada` | Activa la detección automática de colaboraciones por imágenes |
| `app.colaboracion-automatica.*` | Umbrales de pHash/ORB, multicrop y pools async/histórico |
| `IMAGEN_MAX_POR_MOODBOARD` | Máximo de imágenes por moodboard (`configuracion_sistema`) |
| `IMAGEN_MAX_POR_PUBLICACION` | Máximo de imágenes por publicación (`configuracion_sistema`) |
| `POLAROID_MAX_POR_PERFIL` | Máximo de polaroids por perfil (`configuracion_sistema`) |
| `COLABORACION_AUTOMATICA_MAX_PROPUESTAS_POR_IMAGEN` | Máximo de propuestas de colaboración automática por imagen (`configuracion_sistema`) |
| `CASTING_CANDIDATOS_POR_PAGINA` | Candidatos por página del tablero de casting virtual (`configuracion_sistema`) |
| `IMAGEN_SUBSAMPLING_HABILITADO`, `IMAGEN_SUBSAMPLING_UMBRAL` | Subsampling al decodificar imágenes grandes (`configuracion_sistema`) |
| `IMAGEN_EXIF_HABILITADO`, `IMAGEN_EXIF_SCAN_MAX_BYTES` | Orientación EXIF y límite de escaneo (`configuracion_sistema`) |
| `IMAGEN_WEBP_METHOD` | Método del encoder WebP, de 0 a 6 (`configuracion_sistema`) |
| `PERFIL_TYC_PLANTILLA` | Plantilla base de términos y condiciones por perfil (`configuracion_sistema`) |

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

Si la base ya existía antes del rechazo automático de postulaciones e
invitaciones, aplicar una vez la migración:

```bash
psql -U postgres -d modalink -f "docs/3_diseño/migraciones/migracion-rechazo-automatico-cupo.sql"
```

Si la base ya existía antes del versionado de términos y condiciones por
perfil, aplicar además:

```bash
psql -U postgres -d modalink -f "docs/3_diseño/migraciones/migracion-uc23-numero-version-tyc.sql"
```

### 2. Backend

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

### 3. Mailpit (solo desarrollo/testing)

Las notificaciones de prioridad ALTA se envían por email. En desarrollo se
capturan con [Mailpit](https://mailpit.axllent.org/), que no entrega correos
reales: los muestra en una UI web.

```bash
docker compose --profile mail up -d
```

- SMTP: `localhost:1025`
- UI: `http://localhost:8025`

Luego levantar el backend con el scheduler habilitado:

```bash
NOTIFICACIONES_EMAIL_HABILITADO=true mvn spring-boot:run
```

En PowerShell:

```powershell
$env:NOTIFICACIONES_EMAIL_HABILITADO="true"; mvn spring-boot:run
```

Variables opcionales:

```properties
MAIL_HOST=localhost
MAIL_PORT=1025
NOTIFICACIONES_EMAIL_REMITENTE=no-responder@modalink.local
```

El scheduler está deshabilitado por defecto. La política actual envía solo
notificaciones de prioridad `ALTA`; los tipos `MEDIA` y `BAJA` quedan solo
in-app. Las entregas se persisten en `notificacion_entrega` y los reintentos se
configuran con `NOTIFICACION_EMAIL_REINTENTOS_MAX` y
`NOTIFICACION_EMAIL_BACKOFF_MINUTOS` en `configuracion_sistema`.

Cada email incluye un token de un solo uso (60 minutos) que auto-inicia sesión
al abrir el enlace. El token viaja en el fragmento de la URL del frontend y se
intercambia en `POST /auth/email/validar`.

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
- **Última iteración:** migraciones de T&C versionado, baja definitiva, casting virtual, límites de imágenes, notificaciones de características técnicas, nombre femenino, asignación de requerimientos generales y PDF de contratos.
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
| `GET` | `/perfiles/activo` | Perfil activo del usuario |
| `PUT` | `/perfiles/{idPerfil}` | Editar perfil propio |
| `DELETE` | `/perfiles/{idPerfil}` | Solicitar baja del perfil |
| `POST` | `/perfiles/{idPerfil}/reactivar` | Reactivar perfil |
| `PATCH` | `/perfiles/{idPerfil}/activar` | Cambiar perfil activo |
| `GET` | `/usuarios/me/perfiles` | Perfiles del usuario autenticado |
| `POST` | `/perfiles/{idPerfil}/foto` | Subir foto de perfil |
| `DELETE` | `/perfiles/{idPerfil}/foto` | Eliminar foto de perfil |
| `GET/POST/DELETE` | `/perfiles/{idPerfil}/polaroids`, `/perfiles/{idPerfil}/polaroids/orden` | Gestionar polaroids y su orden |
| `GET/PUT` | `/perfiles/{idPerfil}/terminos-condiciones` | Consultar o guardar T&C |
| `GET` | `/perfiles/{idPerfil}/terminos-condiciones/historial`, `/historial/{idTyc}` | Historial de versiones de T&C del perfil |
| `GET/POST` | `/perfiles/{idPerfil}/habilidades` | Gestionar habilidades del perfil |
| `GET` | `/perfiles/{idPerfil}/calificaciones` | Reseñas del perfil |
| `GET` | `/perfiles/{idPerfil}/calificaciones/{idCalificacion}` | Detalle de una reseña |
| `GET` | `/profesiones` | Catálogo de profesiones |
| `GET` | `/profesiones/{id}/caracteristicas-tecnicas` | Características técnicas |
| `GET` | `/habilidades` | Catálogo de habilidades |
| `GET` | `/ubicaciones/provincias`, `/ubicaciones/localidades` | Catálogo geográfico |

Calendario y jornada:

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/calendario` | Agenda completa del perfil activo |
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
| `GET` | `/proyectos/{idProyecto}/informe` | Informe ejecutivo de estado y salud del proyecto (solo director) |
| `GET` | `/proyectos/{idProyecto}/informe/pdf` | Informe ejecutivo en PDF (solo director) |
| `PUT` | `/proyectos/{idProyecto}` | Modificar proyecto |
| `POST` | `/proyectos/{idProyecto}/publicar` | Publicar proyecto |
| `POST` | `/proyectos/{idProyecto}/confirmar` | Confirmar proyecto y planificar |
| `POST` | `/proyectos/{idProyecto}/finalizar` | Finalizar proyecto |
| `POST` | `/proyectos/{idProyecto}/cancelar` | Cancelar proyecto |
| `POST` | `/proyectos/{idProyecto}/reprogramar` | Reintentar reprogramación |
| `GET` | `/proyectos/{idProyecto}/miembros` | Miembros activos |
| `GET` | `/proyectos/{idProyecto}/miembros/tyc`, `/miembros/publico` | T&C y datos públicos de los miembros |
| `PUT` | `/proyectos/{idProyecto}/miembros/{idMiembro}/requerimiento-general` | Asignar o reasignar un requerimiento general |
| `DELETE` | `/proyectos/{idProyecto}/miembros/{idMiembro}` | Eliminar integrante |
| `POST` | `/proyectos/{idProyecto}/baja` | Baja voluntaria |
| `GET` | `/proyectos/{idProyecto}/historial` | Historial de miembros |
| `GET` | `/proyectos/{idProyecto}/historial/contratos` | Historial de contratos del proyecto |
| `GET/POST` | `/proyectos/{idProyecto}/calificaciones` | Calificaciones del equipo |
| `GET/POST` | `/proyectos/{idProyecto}/reemplazos...` | Reemplazo automático de integrantes |
| `POST/DELETE` | `/proyectos/{idProyecto}/requerimientos-generales`, `.../{idRequerimientoGral}` | Alta o baja de requerimientos generales |
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
| `GET` | `/proyectos/{idProyecto}/postulaciones/casting` | Tablero de casting virtual para modelos (paginado) |
| `POST` | `/proyectos/{idProyecto}/postulaciones/.../aceptar` | Aceptar postulación |
| `POST` | `/proyectos/{idProyecto}/postulaciones/.../rechazar` | Rechazar postulación |
| `POST` | `/proyectos/{idProyecto}/invitaciones/proyecto` | Invitar al proyecto |
| `POST` | `/proyectos/{idProyecto}/invitaciones/requerimiento-general` | Invitar a un requerimiento general |
| `POST` | `/proyectos/{idProyecto}/invitaciones/proyecto/batch`, `/requerimiento-general/batch` | Invitación masiva con resultado parcial |
| `POST` | `/invitaciones/proyecto/{id}/aceptar`, `/requerimiento-general/{id}/aceptar` | Aceptar invitación |
| `POST` | `/invitaciones/proyecto/{id}/rechazar`, `/requerimiento-general/{id}/rechazar` | Rechazar invitación |
| `GET` | `/invitaciones/buscar` | Buscar invitaciones |
| `GET` | `/requerimientos-generales/buscar` | Buscar requerimientos generales |
| `GET` | `/requerimientos-actividad/buscar` | Buscar requerimientos de actividad |
| `GET` | `/home/resumen` | Oportunidades y resumen de inicio |

> Se mantienen los alias `/invitaciones/general` y `/invitaciones/actividad` para invitaciones de proyecto y de actividad.

### Publicaciones, interacciones y colaboraciones

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/publicaciones` | Crear publicación (`multipart/form-data`, 1-10 imágenes de hasta 10 MB) |
| `GET` | `/publicaciones/{idPublicacion}` | Ver publicación |
| `GET` | `/publicaciones/{idPublicacion}/detalle` | Detalle con contadores e interacciones |
| `PUT` | `/publicaciones/{idPublicacion}` | Modificar publicación e imágenes |
| `DELETE` | `/publicaciones/{idPublicacion}` | Eliminar publicación o la etiqueta de colaborador |
| `GET` | `/publicaciones/feed` | Feed de contactos activos y publicaciones propias |
| `GET` | `/perfiles/{idPerfil}/publicaciones` | Buscar publicaciones del perfil |
| `GET` | `/perfiles/{idPerfil}/publicaciones/proyectos` | Proyectos para el filtro de publicaciones |
| `GET` | `/publicaciones/ocultas` | Publicaciones ocultas del perfil activo |
| `POST/DELETE` | `/publicaciones/{idPublicacion}/me-gusta` | Dar o quitar me gusta |
| `GET/POST` | `/publicaciones/{idPublicacion}/comentarios` | Listar o crear comentarios |
| `POST` | `/publicaciones/{idPublicacion}/solicitudes-colaboracion` | Enviar solicitud de colaboración |
| `POST` | `/publicaciones/{idPublicacion}/colaboradores/propios` | Agregar colaborador propio |
| `GET` | `/solicitudes-colaboracion` | Buscar solicitudes enviadas y recibidas |
| `GET` | `/solicitudes-colaboracion/publicaciones` | Publicaciones para el filtro de solicitudes |
| `POST` | `/solicitudes-colaboracion/{id}/aceptar` | Aceptar solicitud |
| `POST` | `/solicitudes-colaboracion/{id}/rechazar` | Rechazar solicitud |
| `DELETE` | `/solicitudes-colaboracion/{id}` | Eliminar solicitud enviada |
| `POST` | `/publicaciones/{idPublicacion}/reportes` | Reportar publicación |
| `GET` | `/colaboraciones-automaticas` | Propuestas de colaboración automática |
| `GET` | `/colaboraciones-automaticas/{id}` | Detalle de una propuesta |
| `POST` | `/colaboraciones-automaticas/{id}/confirmar` | Confirmar propuesta |
| `POST` | `/colaboraciones-automaticas/{id}/rechazar` | Rechazar propuesta |

Reglas generales de publicaciones:

- Cada publicación admite entre 1 y 10 imágenes y queda en estado `ACTIVA` u `OCULTA`.
- Solo el perfil dueño puede modificar o eliminar la publicación; los colaboradores la ven en modo lectura.
- Proyecto asociado solo si está `CONFIRMADO` o `FINALIZADO` y el perfil es miembro activo.
- Me gusta idempotente por perfil; comentarios obligatorios de hasta 1000 caracteres.
- Solicitudes de colaboración solo entre contactos activos y una `PENDIENTE` por publicación.
- Las colaboraciones automáticas comparan imágenes (pHash/ORB) y requieren confirmación del perfil para incorporar al colaborador.

### Contratos

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/proyectos/{idProyecto}/contratos/opciones` | Tipos de acuerdo y destinatarios |
| `POST` | `/proyectos/{idProyecto}/contratos/previsualizar` | Previsualizar contrato (opcional) |
| `POST` | `/proyectos/{idProyecto}/contratos` | Crear contrato en borrador |
| `POST` | `/proyectos/{idProyecto}/contratos/{idContrato}/emitir` | Emitir borrador y notificar a las partes |
| `GET/PUT/DELETE` | `/proyectos/{idProyecto}/contratos/{idContrato}` | Consultar, modificar o eliminar |
| `POST` | `/proyectos/{idProyecto}/contratos/{idContrato}/previsualizar` | Previsualizar cambios (opcional) |
| `POST` | `/proyectos/{idProyecto}/contratos/{idContrato}/firmar` | Firmar contrato |
| `POST` | `/proyectos/{idProyecto}/contratos/{idContrato}/rechazar` | Rechazar contrato |
| `GET` | `/contratos/buscar` | Búsqueda global de contratos |
| `GET` | `/contratos/proyectos` | Proyectos con contratos visibles |
| `GET` | `/proyectos/{idProyecto}/contratos/{idContrato}/eventos` | Historial del contrato |
| `GET` | `/proyectos/{idProyecto}/contratos/{idContrato}/pdf` | Exportar contrato a PDF |
| `GET` | `/proyectos/{idProyecto}/historial/contratos` | Historial de contratos del proyecto |
| `GET` | `/admin/contratos/{idContrato}/pdf` | Exportar contrato a PDF desde administración |

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
- Se audita el envío de mensajes privados y grupales (contenido y operador); las lecturas no se auditan.
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
| `GET` | `/admin/reportes-perfil/{idReporte}` | Detalle de reporte de perfil |
| `PATCH` | `/admin/reportes-perfil/{idReporte}/estado` | Resolver o descartar reporte |
| `GET` | `/admin/reportes-publicacion` | Bandeja de reportes de publicación |
| `GET` | `/admin/reportes-publicacion/{idReporte}` | Detalle de reporte de publicación |
| `PATCH` | `/admin/reportes-publicacion/{idReporte}/estado` | Resolver o descartar reporte |
| `PATCH` | `/admin/publicaciones/{idPublicacion}/estado` | Ocultar o reactivar publicación reportada |
| `GET` | `/publicaciones/ocultas` | Búsqueda específica de publicaciones ocultas del perfil activo |

Catálogos administrativos:

- `/admin/habilidades`
- `/admin/caracteristicas-tecnicas`
- `/admin/unidades-medida`
- `/admin/tipos-contrato`
- `/admin/configuracion/schedulers/deshabilitacion`
- `/admin/configuracion/schedulers/baja`
- `/admin/cronograma`
- `/admin/colaboraciones-automaticas/historicas`

Detalle administrativo de solo lectura:

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/admin/perfiles`, `/admin/perfiles/{idPerfil}` | Listado y detalle de perfiles con filtros y relaciones |
| `GET` | `/admin/perfiles/{idPerfil}/terminos-condiciones/historial`, `/historial/{idTyc}` | Historial de T&C del perfil |
| `GET` | `/admin/proyectos`, `/admin/proyectos/{idProyecto}` | Listado y detalle de proyectos en cualquier estado |
| `GET` | `/admin/contratos`, `/admin/contratos/{idContrato}`, `/admin/contratos/{idContrato}/eventos`, `/admin/contratos/{idContrato}/pdf` | Listado, detalle, eventos y PDF de contratos |
| `GET` | `/admin/publicaciones/{idPublicacion}` | Detalle de solo lectura de una publicación |

### Normas de convivencia y manual in-app

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/normas-convivencia/vigente` | Normas vigentes con versión y vigencia |
| `GET` | `/normas-convivencia/versiones`, `/versiones/{version}` | Versiones disponibles y contenido de una versión |
| `GET` | `/ayuda/indice` | Índice del manual filtrado por rol |
| `GET` | `/ayuda/capitulos/{id}` | Capítulo del manual |
| `GET` | `/ayuda/buscar` | Búsqueda en el manual |
| `GET` | `/ayuda/errores` | Catálogo de errores frecuentes |

Las normas son públicas y no requieren sesión. El manual requiere rol `ADMINISTRADOR` o `USUARIO`; sus capítulos Markdown se empaquetan desde `docs/manual/` durante el build.

## Testing

```bash
mvn test
mvn verify
```

- Tests unitarios de servicios, mappers y DTOs.
- Tests MVC con `@WebMvcTest` y MockMvc.
- Tests de integración con PostgreSQL real mediante Testcontainers.
- Tests de concurrencia en flujos críticos de conexiones y aceptación de postulaciones.
- Tests de renderizado de PDF (contratos, auditoría e informe de proyecto) y de migraciones.
- Reporte de cobertura con JaCoCo en `target/site/jacoco/index.html`.

Suite actual (2026-11-26):

```text
425 archivos de test y 3.151 casos declarados (@Test, @ParameterizedTest y @RepeatedTest)
```

> Los tests de integración requieren Docker disponible. Si Docker no está corriendo, los tests que extienden `AbstractPostgresIntegrationTest` fallan.

## Documentación

- Requisitos y casos de uso: [`docs/1_requisitos/casos_de_uso.md`](docs/1_requisitos/casos_de_uso.md)
- Estado de cada UC: [`docs/5_estado_sistema/estado_uc.md`](docs/5_estado_sistema/estado_uc.md)
- Análisis y diagramas de secuencia: [`docs/2_analisis/`](docs/2_analisis/)
- Diseño y esquema: [`docs/3_diseño/`](docs/3_diseño/)
- Controles y fichas de calidad: [`docs/3_diseño/temp/`](docs/3_diseño/temp/)
- Planes de prueba: [`docs/calidad/plan_de_pruebas.md`](docs/calidad/plan_de_pruebas.md) y [`docs/calidad/plan_pruebas_despliegue.md`](docs/calidad/plan_pruebas_despliegue.md)
- Manual de usuario: [`docs/manual/`](docs/manual/) ([PDF](docs/manual/Manual-ModaLink-v1.0.pdf))
- Fichas de frontend aplicadas: [`docs/frontend/aplicadas/`](docs/frontend/aplicadas/)
- Informe de testing: [`docs/6_presentaciones/tests.md`](docs/6_presentaciones/tests.md)
- Informe de colaboraciones automáticas: [`docs/6_presentaciones/informe-colaboraciones-automaticas.md`](docs/6_presentaciones/informe-colaboraciones-automaticas.md)
- Informe ejecutivo de proyecto: [`docs/4_pendiente/plan-implementacion-informe-proyecto.md`](docs/4_pendiente/plan-implementacion-informe-proyecto.md) y [`docs/4_pendiente/propuestas-informes.md`](docs/4_pendiente/propuestas-informes.md)

Fichas específicas:

- [`docs/3_diseño/temp/controles_mensajeria.md`](docs/3_diseño/temp/controles_mensajeria.md)
- [`docs/3_diseño/temp/controles_chat_grupal.md`](docs/3_diseño/temp/controles_chat_grupal.md)
- [`docs/3_diseño/temp/controles_conexiones.md`](docs/3_diseño/temp/controles_conexiones.md)
- [`docs/3_diseño/temp/controles_presencia.md`](docs/3_diseño/temp/controles_presencia.md)
- [`docs/3_diseño/temp/controles_notificaciones_email.md`](docs/3_diseño/temp/controles_notificaciones_email.md)
- [`docs/frontend/aplicadas/ficha-tecnica-email-login.md`](docs/frontend/aplicadas/ficha-tecnica-email-login.md)
- [`docs/frontend/aplicadas/ficha-tecnica-eliminar-publicacion.md`](docs/frontend/aplicadas/ficha-tecnica-eliminar-publicacion.md)
- [`docs/frontend/aplicadas/ficha-tecnica-interactuar-publicacion.md`](docs/frontend/aplicadas/ficha-tecnica-interactuar-publicacion.md)
- [`docs/frontend/aplicadas/ficha-tecnica-reportar-publicacion.md`](docs/frontend/aplicadas/ficha-tecnica-reportar-publicacion.md)
- [`docs/frontend/aplicadas/ficha-tecnica-colaboraciones-automaticas.md`](docs/frontend/aplicadas/ficha-tecnica-colaboraciones-automaticas.md)
- [`docs/frontend/aplicadas/ficha-tecnica-colaboraciones-automaticas-historicas.md`](docs/frontend/aplicadas/ficha-tecnica-colaboraciones-automaticas-historicas.md)

## Convenciones del proyecto

- Toda validación de negocio se implementa en backend y se refuerza en base de datos con constraints, checks, triggers o índices únicos.
- El frontend no contiene lógica de validación.
- Los controladores devuelven DTOs; no exponen entidades JPA.
- Las fechas se generan con `Clock` inyectado para facilitar tests deterministas.
- Los flujos críticos usan transacciones y locks pesimistas cuando hay riesgo de concurrencia.
- Las excepciones de dominio se manejan centralizadamente en `GlobalExceptionHandler`.
- Los cambios críticos se auditan mediante triggers de PostgreSQL.
- La mensajería se audita al enviar (contenido y operador); no se auditan las lecturas.
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
- [x] Notificaciones, reportes de perfil y publicación, auditoría y administración
- [x] Búsqueda global de requerimientos generales y de actividad
- [x] Publicaciones: alta, baja, búsqueda, modificación, interacción, envío/búsqueda, aceptación/rechazo y eliminación de solicitudes de colaboración
- [x] Publicaciones: reporte y moderación (UC-69)
- [x] Colaboraciones automáticas por similitud de imágenes
- [x] Términos y condiciones versionados por perfil
- [x] Informe ejecutivo de estado y salud de proyecto
- [x] Tablero de casting virtual
- [x] Normas de convivencia versionadas y manual in-app
- [x] Detalle administrativo de solo lectura de perfiles, proyectos y contratos
- [x] Manual de usuario y documentación final
