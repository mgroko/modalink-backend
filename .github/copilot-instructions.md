# ModaLink Backend — Contexto para GitHub Copilot

## Qué es este proyecto
ModaLink es una plataforma de networking profesional para la industria creativa/moda —
conceptualmente "un LinkedIn para producciones de moda". Proyecto académico final de la
materia de Ingeniería de Software (metodología UP).

Este repositorio es el **backend**: API REST en Java + Spring Boot + Spring Security +
PostgreSQL. No sirve HTML — todas las respuestas son JSON. El frontend es un repo
separado (Vue 3 SPA) que consume esta API vía Axios.

## Estado actual — Fase 0: Login y registro
El modelo de datos (entidades JPA) y la migración de Flyway ya están hechos y no hay
que tocarlos salvo que se indique lo contrario. **Lo que falta construir es toda la capa
de aplicación para registro y login.** Checklist de lo pendiente en esta fase:

- [ ] `UsuarioRepository` (Spring Data JPA) — `findByCorreo`, `existsByCorreo`, `existsByDni`
- [ ] `RolGlobalRepository`
- [ ] DTOs: `RegistroRequest`, `LoginRequest`, `AuthResponse`, `UsuarioResponse`
      (nunca devolver la entidad `Usuario` directamente — expone `passwordHash`)
- [ ] `AuthService` con `registrar()` y `login()`:
  - hashear contraseña con `BCryptPasswordEncoder`
  - asignar `RolGlobal` = "Usuario" por defecto al registrar
  - validar unicidad de `correo` y `dni` antes de persistir
  - `proveedorAuth = LOCAL` para estos registros (Google OAuth es una fase futura,
    no la implementes salvo que te lo pida explícitamente)
- [ ] Agregar dependencia JWT al `pom.xml` (a definir cuál — preguntame antes de elegir)
- [ ] Clase para generar/validar JWT (ej. `JwtService`) + filtro
      (`JwtAuthenticationFilter`) que lo intercepte en cada request
- [ ] Reescribir `SecurityConfig` (ver "Problemas conocidos" abajo) para:
  - sesión stateless (`SessionCreationPolicy.STATELESS`)
  - `permitAll()` en `/auth/registro` y `/auth/login`
  - resto de endpoints requiere JWT válido
  - configurar CORS para el origen del frontend (Vite, otro puerto)
- [ ] `AuthController`: `POST /auth/registro`, `POST /auth/login`
- [ ] `@ControllerAdvice` con manejo centralizado de errores — devolver JSON prolijo
      (no la página de error default de Spring) en validaciones fallidas, correo/dni
      duplicado, credenciales inválidas, etc.

No avances a fases siguientes (perfiles, proyectos, actividades) hasta que esta lista
esté completa y yo lo confirme.

## Problemas conocidos en el código actual (arreglar como parte de esta fase, no ignorar)

- Credenciales de PostgreSQL hardcodeadas (`postgres/postgres`) en
  `application.properties` y en el plugin de Flyway del `pom.xml`. Migrar a variables
  de entorno antes de seguir sumando configuración.

## Modelo de datos relevante para esta fase (no modificar sin confirmar conmigo)

- `Usuario`: `idUsuario`, `nombre`, `apellido`, `dni` (único), `fechaNacimiento`,
  `correo` (único), `passwordHash` (nullable — null si se registró solo vía Google),
  `proveedorAuth` (enum `LOCAL`/`GOOGLE`), `idExterno` (sub de Google, null si LOCAL),
  `estado` (enum `Activo`/`Deshabilitado`/`Inactivo`/`Baja`), `rolGlobal` (FK,
  obligatoria).
- `RolGlobal`: `idRolGlobal`, `nombre` (único). Ya sembrados vía Flyway:
  `"Administrador"`, `"Usuario"`.
- Un Administrador global no puede tener perfil creativo ni crear proyectos (regla de
  negocio a tener en cuenta más adelante, no aplica al registro básico).

## Cómo quiero trabajar con vos

- **Alcance acotado:** implementá lo que pido en el prompt puntual, siguiendo el
  checklist de arriba. Si para completar una tarea creés necesario tocar algo no
  mencionado (otra clase, otra dependencia), explicámelo primero y esperá confirmación.
- **No refactorices** código fuera del alcance de la tarea actual, aunque veas algo
  mejorable — sugerilo aparte, no lo apliques.
- **No agregues dependencias nuevas sin preguntar** (esto incluye la librería JWT —
  proponeme opciones, no elijas por tu cuenta).
- **No generes tests ni documentación adicional** que no pedí explícitamente.
- En tareas grandes o ambiguas, explicame tu plan en texto antes de generar o editar
  código, y esperá mi confirmación antes de aplicar cambios.
- Preferí explicaciones breves de *qué* hiciste y *por qué*, no un resumen extenso.
- Estoy aprendiendo a construir una API REST con JWT por primera vez (vengo de MVC con
  sesiones/vistas HTML) — si algo que proponés es un concepto nuevo para mí (ej. cómo
  funciona un filtro de seguridad, qué es un claim de JWT), marcámelo explícitamente
  en la respuesta en vez de asumir que ya lo sé.

## Convenciones del proyecto

- Nombres de tablas y columnas en español, snake_case (ya reflejado en las entidades
  vía `@Column(name = "...")` — no cambiar esas anotaciones).
- Entidades JPA en inglés/PascalCase estándar de Java.
- Separación estricta de capas: `controlador` (endpoints REST) → `servicio` (lógica de
  negocio) → `repositorio` (acceso a datos). Nunca lógica de negocio en el controlador,
  nunca acceso directo a repositorio desde el controlador.
- DTOs de request/response en un paquete `dto`, separados de las entidades.

## Comandos del proyecto
- Build: `mvn clean install`
- Correr: `mvn spring-boot:run`
- Tests: `mvn test`

## Qué NO hacer nunca
- No hardcodear credenciales ni secrets (incluido el secret de JWT) — usar variables
  de entorno.
- No devolver la entidad `Usuario` completa en ninguna respuesta HTTP.
- No implementar Google OAuth en esta fase salvo pedido explícito.
- No hacer `git push` ni cambios de configuración de CI sin que yo lo pida.
- No borrar ni modificar diagramas/documentación de la cátedra ya entregada, salvo
  pedido explícito.
