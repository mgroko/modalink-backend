# ModaLink — Backend

Backend de **ModaLink**, una plataforma de networking profesional para la industria creativa y de la moda. Permite gestionar múltiples perfiles, crear proyectos, reclutar equipos y coordinar agendas de trabajo.

Proyecto final de la carrera Analista en Sistemas de Computación, desarrollado siguiendo la metodología **Proceso Unificado (UP)**, con foco en testing y calidad.

> **Estado actual:**
> Etapa: **Fase 2 — Gestión de perfiles** (Panel de usuario, creación de perfiles, modificacion de perfiles, etc). 

## Stack tecnológico

| Componente        | Tecnología                                  |
|--------------------|----------------------------------------------|
| Lenguaje           | Java 25                                      |
| Framework          | Spring Boot 4.1.0 (Web, Security, Data JPA, Validation, Actuator) |
| Base de datos      | PostgreSQL                                   |                                      |
| Autenticación      | JWT (JJWT 0.13.0) en cookies HTTP-only + CSRF vía `CookieCsrfTokenRepository` |
| Autenticación federada | Google OAuth2 (Authorization Code) + `usuario_identidad` |
| Hashing de contraseñas | BCrypt                                   |
| Testing            | JUnit 5, Spring Security Test, Testcontainers (PostgreSQL), JaCoCo |
| Build              | Maven                                        |

Frontend relacionado: [modalink-frontend](https://github.com/mgroko/modalink-frontend) (Vue 3 + Vite).

## Estructura del proyecto

```
src/main/java/org/mgroko/backend/
├── Application.java
├── auth/                # Controlador, servicio y mapper de autenticación
│   ├── dto/             # LoginRequest, RegistroRequest, AuthResponse, UsuarioResponse, onboarding Google
│   ├── exception/       # Excepciones de dominio (correo/DNI duplicado, edad inválida, etc.)
│   └── oauth/           # Flujo Google OAuth2, ticket de registro y handlers
├── common/exception/    # GlobalExceptionHandler y ErrorResponse
├── controlador/         # Controladores generales (HomeController)
├── modelo/               # Entidades JPA del dominio (Usuario, Perfil, Proyecto, Actividad, etc.)
│   └── enums/           # Estados y enumerados del dominio
├── repositorio/          # Repositorios Spring Data JPA
└── security/             # Configuración de seguridad, filtros JWT y CSRF

src/main/resources/
├── application.properties

src/test/java/org/mgroko/backend/   # Pruebas unitarias e integración (auth y security)
```

El modelo (`modelo/`) ya incluye buena parte del dominio completo de ModaLink (proyectos, actividades, postulaciones, invitaciones, requerimientos, agenda), aunque en esta rama solo está implementada la lógica de **registro y login**.

## Requisitos previos

- JDK 25
- Maven 3.9+
- PostgreSQL 14+ corriendo localmente
- Una base de datos llamada `modalink` (o el nombre que definas, ver configuración)

## Configuración

La configuración por defecto vive en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/modalink
spring.datasource.username=postgres
spring.datasource.password=postgres

server.port=8080

```

Variables de entorno soportadas (con valores por defecto para desarrollo):

| Variable              | Descripción                              | Default (dev)                                      |
|-----------------------|-------------------------------------------|-----------------------------------------------------|
| `JWT_SECRET`          | Clave de firma de los tokens JWT           | `cambiar-este-valor-en-produccion-por-uno-largo-y-random` |
| `JWT_EXPIRATION_MS`   | Tiempo de expiración del JWT en ms          | `3600000` (1 hora)                                   |
| `GOOGLE_CLIENT_ID`    | Client ID de Google Cloud                   | `dev-google-client-id` (placeholder)                |
| `GOOGLE_CLIENT_SECRET`| Client Secret de Google Cloud               | `dev-google-client-secret` (placeholder)            |
| `GOOGLE_REDIRECT_URI` | Redirect URI registrado en Google           | `http://localhost:8080/login/oauth2/code/google`    |
| `FRONTEND_URL`        | URL del frontend para redirigir tras OAuth  | `http://localhost:5173`                             |
| `COOKIE_SECURE`       | Marca la cookie como `Secure`               | `false`                                             |
| `COOKIE_SAME_SITE`    | `SameSite` de las cookies                   | `Lax`                                               |

⚠️ En producción, `JWT_SECRET` debe reemplazarse por un valor largo y aleatorio, y las cookies deben usar `COOKIE_SECURE=true` (HTTPS). Si el frontend y el backend quedan en sitios distintos, usar `COOKIE_SAME_SITE=None`.

CORS está habilitado para `FRONTEND_URL` (por defecto `http://localhost:5173`, frontend en desarrollo con Vite).

## Cómo levantar el proyecto

1. Crear la base de datos en PostgreSQL:
   ```sql
   CREATE DATABASE "modalink";
   ```
2. Ajustar credenciales en `application.properties` si difieren de `postgres` / `postgres`.
   ```bash
   ./mvnw spring-boot:run
   ```
3. La API queda disponible en `http://localhost:8080`.

## Endpoints disponibles

Todos bajo el prefijo `/auth` (público, no requiere autenticación previa salvo `/auth/me`):

| Método | Endpoint         | Descripción                                                        |
|--------|-------------------|---------------------------------------------------------------------|
| POST   | `/auth/registro`  | Registra un nuevo usuario. Valida correo/DNI duplicados, edad mínima (18) y rol global. Devuelve el JWT en una cookie HTTP-only. |
| POST   | `/auth/login`     | Autentica con correo y contraseña. Devuelve el JWT en cookie HTTP-only. |
| GET    | `/auth/google`    | Inicia el flujo OAuth2 con Google (redirige al proveedor). |
| GET    | `/auth/google/pendiente` | Devuelve nombre, apellido y correo de Google para prellenar el onboarding (ticket en cookie HttpOnly). |
| POST   | `/auth/google/completar` | Completa DNI, fecha de nacimiento y género; crea la cuenta si no existe y devuelve el JWT. |
| GET    | `/auth/me`        | Devuelve los datos del usuario autenticado (según el JWT de la cookie). |

### Autenticación con Google (UC-09)

1. El frontend navega a `GET /auth/google`.
2. El backend redirige a Google con un `state` firmado y valida el código de autorización.
3. Al volver, el backend valida la identidad y busca al usuario:
   - Si ya tiene una identidad `GOOGLE`, inicia sesión.
   - Si existe un usuario local con el mismo correo verificado, vincula la identidad automáticamente.
   - Si no existe, emite un ticket de registro de 10 minutos y redirige al onboarding.
4. El onboarding pide DNI, fecha de nacimiento y género (Google no los provee) y crea la cuenta con `password_hash = NULL`.
5. Si la cuenta estaba `PENDIENTE_BAJA`, se reactiva automáticamente dentro del plazo.

La tabla `usuario_identidad` es la fuente de verdad de proveedores (`LOCAL`/`GOOGLE`); `usuario` conserva el correo canónico y el `password_hash` local.

#### Configurar Google Cloud (desarrollo local)

1. Crear un proyecto en Google Cloud Console.
2. Configurar la pantalla de consentimiento OAuth en modo **Testing** y agregar los usuarios de prueba.
3. Crear credenciales **OAuth 2.0 Client ID** de tipo **Web application**.
4. Agregar como *Authorized redirect URI*: `http://localhost:8080/login/oauth2/code/google`.
5. Exportar `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` antes de levantar el backend.

Propiedades equivalentes en `application.properties` (el archivo local está
ignorado por git):

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,email,profile
spring.security.oauth2.client.registration.google.redirect-uri=${GOOGLE_REDIRECT_URI:http://localhost:8080/login/oauth2/code/google}
spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code
spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth
spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
spring.security.oauth2.client.provider.google.user-info-uri=https://openidconnect.googleapis.com/v1/userinfo
spring.security.oauth2.client.provider.google.user-name-attribute=sub

app.frontend.url=${FRONTEND_URL:http://localhost:5173}
app.cookie.secure=${COOKIE_SECURE:false}
app.cookie.same-site=${COOKIE_SAME_SITE:Lax}
```

La aplicación arranca sin credenciales reales (usa placeholders), pero el flujo con Google recién funciona cuando se configuran.

#### Migración de base de datos

Antes de levantar esta versión sobre una base existente, ejecutar
`docs/3_diseño/migraciones/2026-11-06_uc09_google_oauth.sql` con la aplicación
detenida. La migración crea `usuario_identidad`, hace backfill de todos los
usuarios locales y recién después elimina `usuario.proveedor_auth` e
`usuario.id_externo`. La carpeta `migraciones/` está ignorada por git; el
archivo se conserva localmente.

### Gestión de perfiles (requiere autenticación)

| Método | Endpoint    | Descripción                                                                 |
|--------|-------------|-----------------------------------------------------------------------------|
| POST   | `/perfiles` | Crea un perfil profesional vinculado al usuario autenticado. Valida que el usuario no tenga ya un perfil para esa profesión, que las características técnicas correspondan a la profesión elegida y las longitudes de nombre artístico/biografía. Devuelve `201 Created` con el perfil creado. |
| GET    | `/perfiles/{idPerfil}` | Recupera los datos actuales de un perfil propio (UC-11, paso 2). Valida que el perfil pertenezca al usuario. |
| PUT    | `/perfiles/{idPerfil}` | Edita el perfil propio (UC-11): nombre artístico, biografía, foto de perfil y características técnicas. La profesión no es modificable (reforzado por trigger). Valida campos obligatorios y las características. |
| DELETE | `/perfiles/{idPerfil}` | Solicita la baja del perfil (UC-12): pasa a `PendienteBaja`, oculta el perfil a la comunidad y registra una cuenta regresiva de 30 días. Devuelve la fecha límite para reactivar. |
| POST   | `/perfiles/{idPerfil}/reactivar` | Reactiva un perfil `PendienteBaja` dentro de los 30 días (UC-12). Un perfil no activado a tiempo se da de baja definitivamente por tarea programada diaria. |
| GET    | `/perfiles/{idPerfil}/terminos-condiciones` | Devuelve la versión vigente de los T&C del perfil o la plantilla base si todavía no configuró (UC-23). |
| PUT    | `/perfiles/{idPerfil}/terminos-condiciones` | Guarda una nueva versión de los T&C del perfil propio (UC-23). Solo perfiles `Activo`; máximo 5000 caracteres. |
| GET    | `/usuarios/me/perfiles` | Devuelve los perfiles del usuario autenticado (dashboard). Equivale a `GET /admin/usuarios/{id}/perfiles` pero restringido al propio usuario. |

Los T&C se versionan: cada cambio real cierra la versión vigente y crea una nueva. La versión vigente se incluye en `GET /perfiles/{idPerfil}` y en las invitaciones; cuando el proyecto incorpora a un integrante con T&C configurados, el director debe aceptar esa versión y queda registrada en `id_tyc_aceptado`. La plantilla base se configura en `configuracion_sistema` con la clave `PERFIL_TYC_PLANTILLA`.

### Red de contactos (UC-43, requiere autenticación)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST   | `/solicitudes-conexion` | Envía una solicitud de conexión desde el perfil activo al perfil indicado en el body (`idPerfilDestinatario`). Crea la solicitud en estado `PENDIENTE` y una notificación `SOLICITUD_CONEXION_RECIBIDA` al perfil destinatario. Devuelve `201 Created`. |
| GET    | `/solicitudes-conexion` | Busca solicitudes recibidas o enviadas por el perfil activo. Filtros opcionales: `direccion`, `estado` (por defecto `PENDIENTE`), `nombre`, `apellido`, `nombreArtistico`, `idProfesion`, `fechaDesde`, `fechaHasta`. Devuelve una página de resultados. |
| POST   | `/solicitudes-conexion/{idSolicitudConexion}/aceptar` | Acepta una solicitud `PENDIENTE` recibida por el perfil activo. Crea la conexión y notifica a ambas partes. Devuelve `200` con `idConexion`. |
| POST   | `/solicitudes-conexion/{idSolicitudConexion}/rechazar` | Rechaza una solicitud `PENDIENTE` recibida por el perfil activo. Notifica solo al remitente. Devuelve `200` con `idConexion = null`. |
| DELETE | `/solicitudes-conexion/{idSolicitudConexion}` | Cancela una solicitud `PENDIENTE` enviada por el perfil activo. Elimina físicamente la solicitud y la notificación `SOLICITUD_CONEXION_RECIBIDA` del destinatario. Devuelve `204 No Content`. |

`GET /perfiles/{idPerfil}` incorpora los campos `estadoConexion` (`NINGUNA`, `SOLICITUD_ENVIADA`, `SOLICITUD_RECIBIDA`, `CONECTADO`), `idSolicitudConexion` e `idConexion` para que el frontend muestre la acción disponible.

Reglas de UC-43:

- El remitente es el perfil activo de la sesión; el destinatario se indica en el body.
- Ambos perfiles y sus usuarios deben estar `ACTIVO`; el destinatario debe ser visible según UC-14.
- No se permite conectar el mismo perfil ni dos perfiles del mismo usuario.
- No se permite si ya existe una conexión `ACTIVA` o una solicitud `PENDIENTE` en cualquier dirección.
- Una solicitud rechazada no bloquea un nuevo envío; sólo bloquea una solicitud pendiente.
- Sólo el perfil remitente puede cancelar su propia solicitud y sólo mientras esté `PENDIENTE`.
- La cancelación es física: el par queda libre y se puede volver a enviar la solicitud.
- La búsqueda de UC-46 se limita al perfil activo. Por defecto devuelve `PENDIENTE` y ordena por `fechaEnvio DESC`.
- Los filtros de nombre, apellido, nombre artístico y profesión se aplican a la contraparte y son case-insensitive e insensibles a acentos.
- La búsqueda sin resultados devuelve `200` con una página vacía.
- Sólo el perfil destinatario puede aceptar o rechazar una solicitud.
- Aceptar crea una conexión `ACTIVA` y notifica a ambas partes; rechazar notifica sólo al remitente.
- Las notificaciones de conexiones incluyen `link` para navegar a solicitudes o a la red de contactos.
- El alta y la baja de conexiones se auditan (`CONEXION_CREADA` / `CONEXION_ELIMINADA`); las solicitudes no se auditan.
- La base de datos refuerza las reglas con un índice único parcial, checks y triggers.

Antes de levantar esta versión sobre una base existente, ejecutar con la aplicación detenida
`docs/3_diseño/migraciones/2026-11-08_uc43_solicitud_conexion.sql`. La migración crea
`solicitud_conexion`, `conexion` y los tipos de notificación sociales. La tabla `conexion`
queda preparada para UC-44, UC-45 y UC-46.

Para UC-46 ejecutar además
`docs/3_diseño/migraciones/2026-11-09_uc46_buscar_solicitudes_conexion.sql`, que habilita
la extensión `unaccent` usada por la búsqueda insensible a acentos.

Para UC-45 ejecutar
`docs/3_diseño/migraciones/2026-11-09_uc45_auditoria_conexiones.sql`, que agrega los
eventos `CONEXION_CREADA` y `CONEXION_ELIMINADA` y el trigger de auditoría sobre `conexion`.

### Catálogos (requiere autenticación)

| Método | Endpoint    | Descripción                                                                 |
|--------|-------------|-----------------------------------------------------------------------------|
| GET    | `/profesiones` | Busca profesiones por nombre (`?nombre=`). Sin filtro devuelve todas. Usado por UC-10 para el formulario de creación de perfil. |
| GET    | `/profesiones/{id}/caracteristicas-tecnicas` | Busca las características técnicas de la profesión indicada, con filtros opcionales `codigo` y/o `unidad` (combinables con AND). Sin filtros devuelve todas las de esa profesión. Valida que la profesión exista. |

### Cuenta del usuario (requiere autenticación)

| Método | Endpoint    | Descripción                                                                 |
|--------|-------------|-----------------------------------------------------------------------------|
| POST   | `/usuario/solicitar-baja` | Solicita la baja de la cuenta (UC-07): pasa a `PendienteBaja`, deja los perfiles ocultos a la comunidad y registra una cuenta regresiva de 30 días. Devuelve la fecha límite para recuperar los datos. |
| POST   | `/usuario/reactivar-cuenta` | Reactiva la cuenta `PendienteBaja` dentro de los 30 días (UC-07). Una cuenta no reactivada a tiempo se da de baja definitivamente por tarea programada diaria. |
| PUT    | `/usuario/datos-personales` | Actualiza los datos personales del usuario autenticado (UC-08). |

Las validaciones de negocio (duplicados, edad mínima, existencia de rol) están implementadas tanto a nivel de base de datos (constraints/checks) como en el backend, siguiendo el criterio de la cátedra de no delegar validaciones al frontend.

## Testing

```bash
./mvnw test
```

- Pruebas unitarias e integración con JUnit 5 y Spring Security Test.
- Reporte de cobertura generado con JaCoCo tras `mvnw test` (`target/site/jacoco/index.html`).
- Cobertura actual concentrada en el módulo de autenticación y seguridad (`AuthController`, `AuthService`, `UsuarioMapper`, `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`).

## Convenciones del proyecto

- Toda validación de negocio debe implementarse tanto en la base de datos (triggers/checks) como en el backend; el frontend no contiene lógica de validación.
- Excepciones de dominio específicas por caso de error (ver `auth/exception`), manejadas centralizadamente por `GlobalExceptionHandler`.
- Cada módulo nuevo requiere pruebas completas antes de avanzar al siguiente, según el reglamento de la cátedra.
- Ver `.github/copilot-instructions.md` para las convenciones detalladas usadas por GitHub Copilot en este repo.

## Roadmap

- [x] Autenticación con Google OAuth
- [ ] Recuperación de contraseña (no hay UC definido; se eliminaron los restos del frontend)
- [ ] Gestión de perfiles y proyectos
- [ ] Postulaciones e invitaciones a proyectos/actividades
- [ ] Agenda y bloqueo de disponibilidad
