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
| Base de datos      | PostgreSQL                                   |
| Migraciones        | Flyway                                       |
| Autenticación      | JWT (JJWT 0.13.0) en cookies HTTP-only + CSRF vía `CookieCsrfTokenRepository` |
| Hashing de contraseñas | BCrypt                                   |
| Testing            | JUnit 5, Spring Security Test, Testcontainers (PostgreSQL), JaCoCo |
| Build              | Maven                                        |

Frontend relacionado: [modalink-frontend](https://github.com/mgroko/modalink-frontend) (Vue 3 + Vite).

## Estructura del proyecto

```
src/main/java/org/mgroko/backend/
├── Application.java
├── auth/                # Controlador, servicio y mapper de autenticación
│   ├── dto/             # LoginRequest, RegistroRequest, AuthResponse, UsuarioResponse
│   └── exception/       # Excepciones de dominio (correo/DNI duplicado, edad inválida, etc.)
├── common/exception/    # GlobalExceptionHandler y ErrorResponse
├── controlador/         # Controladores generales (HomeController)
├── modelo/               # Entidades JPA del dominio (Usuario, Perfil, Proyecto, Actividad, etc.)
│   └── enums/           # Estados y enumerados del dominio
├── repositorio/          # Repositorios Spring Data JPA
└── security/             # Configuración de seguridad, filtros JWT y CSRF

src/main/resources/
├── application.properties
└── db/migration/         # Scripts Flyway (V1, V2, V3...)

src/test/java/org/mgroko/backend/   # Pruebas unitarias e integración (auth y security)
```

El modelo (`modelo/`) ya incluye buena parte del dominio completo de ModaLink (proyectos, actividades, postulaciones, invitaciones, requerimientos, agenda), aunque en esta rama solo está implementada la lógica de **registro y login**.

## Requisitos previos

- JDK 25
- Maven 3.9+
- PostgreSQL 14+ corriendo localmente
- Una base de datos llamada `proyectoMVP` (o el nombre que definas, ver configuración)

## Configuración

La configuración por defecto vive en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/proyectoMVP
spring.datasource.username=postgres
spring.datasource.password=postgres

server.port=8080

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.schemas=public
```

Variables de entorno soportadas (con valores por defecto para desarrollo):

| Variable              | Descripción                              | Default (dev)                                      |
|-----------------------|-------------------------------------------|-----------------------------------------------------|
| `JWT_SECRET`          | Clave de firma de los tokens JWT           | `cambiar-este-valor-en-produccion-por-uno-largo-y-random` |
| `JWT_EXPIRATION_MS`   | Tiempo de expiración del JWT en ms          | `3600000` (1 hora)                                   |

⚠️ En producción, `JWT_SECRET` debe reemplazarse por un valor largo y aleatorio, y la cookie `jwt` debe marcarse como `secure(true)`.

CORS está habilitado solo para `http://localhost:5173` (frontend en desarrollo con Vite).

## Cómo levantar el proyecto

1. Crear la base de datos en PostgreSQL:
   ```sql
   CREATE DATABASE "proyectoMVP";
   ```
2. Ajustar credenciales en `application.properties` si difieren de `postgres` / `postgres`.
3. Ejecutar la aplicación (Flyway aplica las migraciones automáticamente al arrancar):
   ```bash
   ./mvnw spring-boot:run
   ```
4. La API queda disponible en `http://localhost:8080`.

## Endpoints disponibles

Todos bajo el prefijo `/auth` (público, no requiere autenticación previa salvo `/auth/me`):

| Método | Endpoint         | Descripción                                                        |
|--------|-------------------|---------------------------------------------------------------------|
| POST   | `/auth/registro`  | Registra un nuevo usuario. Valida correo/DNI duplicados, edad mínima (18) y rol global. Devuelve el JWT en una cookie HTTP-only. |
| POST   | `/auth/login`     | Autentica con correo y contraseña. Devuelve el JWT en cookie HTTP-only. |
| GET    | `/auth/me`        | Devuelve los datos del usuario autenticado (según el JWT de la cookie). |

### Gestión de perfiles (requiere autenticación)

| Método | Endpoint    | Descripción                                                                 |
|--------|-------------|-----------------------------------------------------------------------------|
| POST   | `/perfiles` | Crea un perfil profesional vinculado al usuario autenticado. Valida que el usuario no tenga ya un perfil para esa profesión, que las características técnicas correspondan a la profesión elegida y las longitudes de nombre artístico/biografía. Devuelve `201 Created` con el perfil creado. |
| GET    | `/usuarios/me/perfiles` | Devuelve los perfiles del usuario autenticado (dashboard). Equivale a `GET /admin/usuarios/{id}/perfiles` pero restringido al propio usuario. |

### Catálogos (requiere autenticación)

| Método | Endpoint    | Descripción                                                                 |
|--------|-------------|-----------------------------------------------------------------------------|
| GET    | `/profesiones` | Busca profesiones por nombre (`?nombre=`). Sin filtro devuelve todas. Usado por UC-10 para el formulario de creación de perfil. |
| GET    | `/profesiones/{id}/caracteristicas-tecnicas` | Busca las características técnicas de la profesión indicada, con filtros opcionales `codigo` y/o `unidad` (combinables con AND). Sin filtros devuelve todas las de esa profesión. Valida que la profesión exista. |

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

- [ ] Recuperación de contraseña (ya en desarrollo en el frontend)
- [ ] Autenticación con Google OAuth
- [ ] Gestión de perfiles y proyectos
- [ ] Postulaciones e invitaciones a proyectos/actividades
- [ ] Agenda y bloqueo de disponibilidad
