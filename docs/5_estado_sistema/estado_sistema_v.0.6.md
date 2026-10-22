# Estado del Sistema — Versión 0.6 (Backend)

> **Fase 5: Estado del Sistema (Proceso Unificado)**  
> **Fecha de corte:** 22 de octubre de 2026  
> **Versión:** v0.6  
> **Commit de referencia:** `8198034` — *Se inserta seed de tipo de notificación para actividades modificadas*  
> **Cambios locales:** UC-50 en desarrollo, fases 1 a 5 completadas, sin commitear.  
> **Alcance:** exclusivamente **backend**. No se documenta implementación de frontend, aunque se referencian los contratos HTTP que consume.  
> **Fuente de verdad de BD:** `docs/3_diseño/ModaLinkBD.sql` (script canónico, sin Flyway).  
> **Espejo de pruebas:** `src/test/resources/ModaLinkBD.sql` (idéntico al canónico al momento del relevamiento).

---

## 1. Resumen ejecutivo

La versión **v0.6** consolida el estado del backend de ModaLink después de la adaptación al modelo CASE y del primer bloque funcional de notificaciones in-app, e incorpora el desarrollo en curso de **UC-50 — Modificar actividad de planificación**, con sus fases 1 a 5 completadas.

Indicadores relevados:

| Indicador | Evidencia actual |
|---|---:|
| Script SQL canónico | 3.343 líneas |
| Tablas | 63 |
| Triggers | 29 |
| Funciones PL/pgSQL | 27 |
| Índices | 41 (30 únicos + 11 no únicos) |
| Claves foráneas | 96 |
| Archivos Java de producción | 383 |
| Controladores REST | 21 |
| Endpoints REST | 72 |
| Archivos en `modelo/` | 45 (entidades y claves embebidas) |
| Repositorios | 37 |
| Archivos de prueba | 122 |
| Métodos `@Test` | 1.015 |
| Tests ejecutados en la suite completa | 937 (0 fallos) |
| Casos de uso oficiales implementados y testeados | 28 de 74 (37,8 %) |
| Casos de uso en progreso | 1 (UC-50, fases 1-5 de 6) |

**Hitos principales de esta versión:**

1. Se completó la adaptación del backend al modelo CASE: enums en `UPPERCASE`, corrección de columnas, normalización de ubicación, catálogos, agenda, profesiones, características técnicas, unidades de medida e imágenes.
2. Se implementaron y testearon los UC-24, UC-25, UC-29, UC-29 bis (derivado), UC-35, UC-36, UC-49 y UC-52 del módulo de proyectos.
3. Se incorporó el modelo de notificaciones in-app: tablas, catálogo de tipos, checks, índices, triggers y configuración.
4. Se expuso la primera API de lectura de notificaciones con listado paginado, resumen de no leídas, marcado individual y masivo, aislamiento por usuario y contexto de perfil.
5. Se integró la generación de notificaciones en la cancelación de proyectos y se ampliaron los triggers de hitos, asignación de actividad y control de cupo.
6. Se agregaron pruebas unitarias, de controlador y de integración para el módulo de notificaciones.
7. Se completaron las fases 1 a 5 de UC-50: contrato `PUT` de reemplazo total, `GET` de detalle con profesionales asignados, validaciones de listas, detección de ciclos con `409` antes de persistir, prioridad del `403` de permisos sobre el estado del proyecto, bloqueo de requerimientos con postulaciones o invitaciones, CPM hacia adelante y hacia atrás, y `estadoCronograma` derivado con tolerancia a desborde. En la fase 5 se agrego la matriz estricta de transiciones con `409` y notificaciones after-commit cuyos fallos no revierten la actividad.

**Pendientes relevantes del backend:**

- Cerrar formalmente UC-50 fase 6: documentacion final y aprobacion de la suite definitiva; la regresion completa ya se ejecuto con 937 tests y 0 fallos.
- Purga y retención automática de notificaciones.
- Entrega por email, outbox y preferencias por canal.
- Filtro de solo no leídas y exposición del intervalo de polling configurable.
- Implementación de los UC restantes, especialmente publicaciones, contratos, conexiones y postulaciones/invitaciones.
- Sincronización de la documentación de estado y de las fichas técnicas con el código actual.
- Enforcement de cobertura en CI.

---

## 2. Arquitectura actual del backend

### 2.1. Stack técnico

| Componente | Versión / configuración |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.0 |
| Spring Web MVC | `spring-boot-starter-web` |
| Spring Security | `spring-boot-starter-security` |
| Spring Data JPA | `spring-boot-starter-data-jpa` |
| Bean Validation | `spring-boot-starter-validation` |
| PostgreSQL | Driver `org.postgresql:postgresql` |
| JWT | `jjwt-api`, `jjwt-impl`, `jjwt-jackson` 0.13.0 |
| Lombok | Dependencia `provided` |
| Testcontainers | BOM 1.21.3, PostgreSQL 17-alpine |
| Testing | JUnit 5, Mockito, AssertJ, `spring-boot-starter-test`, `spring-boot-starter-webmvc-test` |
| Cobertura | JaCoCo 0.8.14, solo `prepare-agent` + `report` |
| Migraciones | **Sin Flyway**; esquema gestionado por `ModaLinkBD.sql` |

### 2.2. Organización del código

El backend mantiene una organización por módulos funcionales y capas:

| Paquete / área | Rol |
|---|---|
| `auth` | Registro, login, logout, reactivación y sesión JWT. |
| `usuario` | Datos personales, solicitud de baja y reactivación de cuenta. |
| `admin` | Usuarios, características técnicas, unidades de medida y configuración de schedulers. |
| `perfiles` | Alta, edición, baja, activación, búsqueda, foto y características del perfil. |
| `calendario` | Agenda, jornada laboral y bloqueos de disponibilidad. |
| `proyectos` | Creación, publicación, modificación, cancelación, búsqueda, detalle y seguridad de proyectos. |
| `actividades` | Creación y búsqueda de actividades de planificación. |
| `notificaciones` | Lectura, resumen, marcado y contrato de generación de notificaciones. |
| `ubicacion` | Catálogo geográfico y ubicación del usuario. |
| `storage` | Almacenamiento local, procesamiento WebP y limpieza de imágenes. |
| `security` | JWT, filtro de autenticación, cookie JWT y CSRF. |
| `config` | Seeder del administrador y configuración web de archivos estáticos. |
| `common` | Paginación estándar, errores globales y utilidades de búsqueda. |

### 2.3. Persistencia y entorno de pruebas

- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate no modifica el esquema; la aplicación no arranca si entidad y base difieren.
- `docs/3_diseño/ModaLinkBD.sql` es el artefacto canónico del esquema, triggers, funciones, índices y seeds.
- `src/test/resources/ModaLinkBD.sql` es el espejo usado por Testcontainers. Al cierre de este informe ambos archivos tienen el mismo SHA-256 (`D8904C01D85FA91790EB7211B4F66B6A9FC4210DB59232568D5A855741FBA9A2`).
- Los tests de integración usan un contenedor PostgreSQL 17-alpine singleton y ejecutan el script SQL como `withInitScript`.
- No existe `src/main/resources/db/migration` ni dependencia Flyway.

### 2.4. Seguridad

- Sesión **STATELESS**.
- JWT en cookie `jwt`, `httpOnly=true`, `SameSite=Lax`, `secure=false` en desarrollo, `Max-Age=24h`.
- Expiración por defecto del token: `jwt.expiration-ms=3600000` (1 hora).
- CSRF con `CookieCsrfTokenRepository.withHttpOnlyFalse()`; los métodos mutantes requieren `X-XSRF-TOKEN`.
- CORS limitado a `http://localhost:5173`, con credenciales permitidas.
- `anyRequest().authenticated()`; solo `/auth/**`, `/error` y `/uploads/**` son públicos.
- El principal autenticado es el `idUsuario` como `String`; los controladores lo convierten con `Long.parseLong`.

### 2.5. Programación de tareas y archivos

- `@EnableScheduling` habilitado en `Application`.
- Schedulers existentes:
  - `BajaScheduler`: expiración de cuentas/perfiles y reactivación según configuración.
  - `DeshabilitacionScheduler`: reactivación automática de usuarios deshabilitados.
  - `LimpiezaImagenesScheduler`: limpieza de imágenes huérfanas.
- **No existe scheduler de purga de notificaciones.**
- Almacenamiento local en `uploads` y `perfiles`; procesamiento WebP; límites de 10 MB por imagen y 110 MB por request.
- No hay dependencias de correo ni WebSocket/SSE.

---

## 3. Estado de la base de datos

### 3.1. Evolución respecto de v0.4

La v0.4 describía un script de 2.724 líneas y 31 tablas, con un plan de adaptación pendiente. El estado actual evidencia la ejecución de ese plan:

| Aspecto | v0.4 | v0.5 |
|---|---|---|
| Líneas del SQL canónico | 2.724 | 3.343 |
| Tablas | 31 | 63 |
| Enums JPA | PascalCase / mixtos | `UPPERCASE` alineados al SQL |
| Flyway | Eliminado | Sin dependencia, sin `db/migration` |
| Notificaciones | Tabla básica | Modelo completo con catálogo, contexto, checks, índices y triggers |
| Tests de integración | Adaptación en curso | Testcontainers alineado al SQL canónico |

### 3.2. Modelo de notificaciones

El módulo de notificaciones se compone de las siguientes tablas:

| Tabla | Rol |
|---|---|
| `notificacion_tipo` | Catálogo de códigos, nombres, categoría, prioridad y estado activo. |
| `notificacion` | Fuente de verdad in-app por usuario destinatario y perfil de contexto opcional. |
| `notificacion_entrega` | Outbox previsto para entrega por canal; **sin código Java asociado**. |
| `notificacion_preferencia` | Preferencias por usuario, tipo y canal; **sin código Java asociado**. |

Columnas y reglas principales de `notificacion`:

- `id_notificacion` identity, PK.
- `tipo` `NOT NULL` con FK a `notificacion_tipo(codigo)`.
- `mensaje` `NOT NULL`.
- `entidad_tipo` / `entidad_id` polimórficos, ambos nulos o ambos presentes.
- `leida` `NOT NULL DEFAULT false`.
- `fecha_leida` nullable y check de coherencia con `leida`.
- `origen` `NOT NULL DEFAULT 'SERVICIO'` con valores `TRIGGER`, `SERVICIO`, `SCHEDULER`.
- `fecha_generada` `NOT NULL DEFAULT CURRENT_TIMESTAMP`.
- `id_usuario_destino` `NOT NULL` con FK a `usuario`.
- `id_perfil_contexto` nullable con FK a `perfil` y trigger que valida pertenencia al usuario destinatario.

Índices relevantes:

- `idx_notif_usuario_fecha (id_usuario_destino, fecha_generada DESC, id_notificacion DESC)`.
- `idx_notif_usuario_no_leida` parcial `WHERE leida = false`.
- `idx_notif_entidad (entidad_tipo, entidad_id)`.
- `idx_notif_usuario_perfil_fecha (id_usuario_destino, id_perfil_contexto, fecha_generada DESC)`.
- `idx_notif_purga` parcial `WHERE leida = true`.

Catálogo inicial de tipos:

- `HITO_CUMPLIDO`.
- `ACTIVIDAD_ASIGNADA`.
- `PROYECTO_CANCELADO`.
- `POSTULACION_RECHAZADA`.
- `INVITACION_RECHAZADA`.

Generación en base de datos:

| Trigger | Función | Efecto |
|---|---|---|
| `trg_derivar_cumplimiento_hito` | `fn_derivar_cumplimiento_hito` | Al cumplirse un hito, notifica a los perfiles activos del proyecto. |
| `trg_notificar_asignacion_actividad` | `fn_notificar_asignacion_actividad` | Al asignar una actividad, notifica al perfil asignado. |
| `trg_control_cupo_postulacion_actividad` | `fn_control_cupo_postulacion_actividad` | Al cubrirse el cupo, rechaza pendientes y les notifica. |
| `trg_control_cupo_postulacion_gral` | `fn_control_cupo_postulacion_gral` | Mismo comportamiento para postulaciones generales. |
| `trg_validar_notificacion_perfil_contexto` | `fn_validar_notificacion_perfil_contexto` | Impide asociar un perfil de contexto que no pertenece al usuario destinatario. |

Los triggers usan `NOT EXISTS` para evitar duplicados lógicos por usuario/perfil/tipo/entidad. No existe un índice único que elimine por completo la ventana de carrera ante inserciones concurrentes.

### 3.3. Configuración de notificaciones ya sembrada

El SQL canónico incluye estas claves en `configuracion_sistema`:

| Clave | Valor | Uso previsto |
|---|---:|---|
| `NOTIFICACION_RETENCION_DIAS` | 90 | Retención de leídas antes de purgar. |
| `NOTIFICACION_PURGA_HORA` | 3 | Hora del job de purga. |
| `NOTIFICACION_PURGA_MINUTO` | 30 | Minuto del job de purga. |
| `NOTIFICACION_PURGA_LOTE` | 1000 | Tamaño de lote de purga. |
| `NOTIFICACION_PAGINA_MAX` | 50 | Tope de `size` en el listado. |
| `NOTIFICACION_POLLING_SEGUNDOS` | 45 | Intervalo de polling para el frontend. |
| `NOTIFICACION_EMAIL_REINTENTOS_MAX` | 5 | Reintentos de email. |
| `NOTIFICACION_EMAIL_BACKOFF_MINUTOS` | 15 | Backoff de email. |

**Estado real:** solo están sembradas. El backend actual no lee ninguna de ellas. El listado usa un máximo hardcodeado de 100, no 50, y no expone el intervalo de polling.

---

## 4. Inventario de API del backend

Se relevaron **21 controladores** y **72 endpoints**.

| Módulo | Controladores | Endpoints | Rutas principales |
|---|---|---:|---|
| Autenticación | `AuthController` | 5 | `POST /auth/registro`, `/login`, `/logout`, `/reactivar-cuenta`; `GET /auth/me` |
| Administración | 6 controladores | 24 | `/admin/usuarios`, `/admin/caracteristicas-tecnicas`, `/admin/unidades-medida`, `/admin/configuracion/schedulers/{deshabilitacion,baja}` |
| Perfiles | 3 controladores | 13 | `/perfiles`, `/perfiles/buscar`, `/perfiles/{id}`, `/perfiles/{id}/foto`, `/profesiones`, `/profesiones/{id}/caracteristicas-tecnicas` |
| Ubicación | 2 controladores | 5 | `/ubicaciones/provincias`, `/ubicaciones/localidades`, `/usuario/ubicacion` |
| Calendario | `CalendarioController` | 5 | `GET /calendario`, `PUT /calendario/jornada`, `POST/DELETE /calendario/bloqueos` |
| Proyectos y actividades | 3 controladores | 11 | `/proyectos`, `/proyectos/buscar`, `/proyectos/propios`, `/proyectos/{id}`, publicar, cancelar, modificar; actividades, detalle de actividad y moodboard |
| Notificaciones | `NotificacionController` | 4 | `GET /notificaciones`, `GET /notificaciones/resumen`, `PATCH /notificaciones/{id}/leida`, `PATCH /notificaciones/leidas` |
| Usuario | 3 controladores | 3 | `PUT /usuario/datos-personales`, `POST /usuario/solicitar-baja`, `POST /usuario/reactivar-cuenta` |
| Inicio / soporte | `HomeController` | 2 | `GET /health`, `GET /home/resumen` |

### 4.1. Contrato HTTP de notificaciones

| Método | Ruta | Parámetros | Respuesta |
|---|---|---|---|
| `GET` | `/notificaciones` | `idPerfilContexto?`, `page=0`, `size=20` | `PaginaResponse<NotificacionResponse>` |
| `GET` | `/notificaciones/resumen` | `idPerfilContexto?` | `{ total, noLeidas }` |
| `PATCH` | `/notificaciones/{idNotificacion}/leida` | — | `NotificacionResponse` actualizada |
| `PATCH` | `/notificaciones/leidas` | `idPerfilContexto?` | `{ actualizadas }` |

Características del contrato:

- El usuario destinatario siempre se obtiene del JWT; no se acepta por parámetro.
- Si se informa `idPerfilContexto`, el servicio valida que el perfil pertenezca al usuario autenticado.
- El listado ordena por `fechaGenerada DESC` y `idNotificacion DESC`.
- El marcado individual devuelve la notificación completa; el masivo devuelve la cantidad actualizada.
- No se expone `usuarioDestino`.
- No existe endpoint de creación, borrado ni filtro por estado `leida`.

---

## 5. Estado de casos de uso

### 5.1. Resumen corregido

El archivo `docs/5_estado_sistema/estado_uc.md` contiene la tabla detallada por UC. Su resumen final está desactualizado: indica 25 UC implementados y testeados y 4 en proyectos, pero la tabla detallada registra 7 UC oficiales de proyectos implementados y testeados. El conteo correcto, excluyendo el caso derivado UC-29 bis, es:

| Módulo | Total UC | Implementado y testeado | Implementado sin tests | En progreso | No iniciado |
|---|---:|---:|---:|---:|---:|
| Gestión de usuarios y administración | 15 | 12 | 0 | 0 | 3 |
| Gestión de perfiles y disponibilidad | 13 | 9 | 0 | 0 | 4 |
| Gestión de publicaciones | 11 | 0 | 0 | 0 | 11 |
| Gestión de proyectos | 25 | 7 | 0 | 1 | 17 |
| Gestión de contratos | 5 | 0 | 0 | 0 | 5 |
| Gestión de conexiones y mensajería | 5 | 0 | 0 | 0 | 5 |
| **Total** | **74** | **28** | **0** | **1** | **45** |

**Avance global corregido:** 28 de 74 UC implementados y testeados = **37,8 %**; además, UC-50 se encuentra en progreso con las fases 1 a 5 de 6 completadas.

> El escenario **UC-29 bis — Buscar proyectos propios** es un caso derivado del desarrollo, con endpoint y tests propios, pero no forma parte del catálogo numerado. No se computa en el total de 74. Si se incluye como fila de seguimiento, los UC implementados ascienden a 29 filas.

### 5.2. UC implementados y testeados

**Usuarios y administración (12):** UC-01, UC-02, UC-03, UC-04, UC-05, UC-06, UC-07, UC-08, UC-57, UC-58, UC-59 y UC-74.

**Perfiles y disponibilidad (9):** UC-10, UC-11, UC-12, UC-13, UC-14, UC-16, UC-17, UC-18 y UC-71.

**Proyectos (7):**

| UC | Nombre | Evidencia backend |
|---|---|---|
| UC-24 | Crear proyecto | `POST /proyectos`; proyecto en Borrador, director, planificación y requerimientos. |
| UC-25 | Publicar proyecto | `POST /proyectos/{id}/publicar`; validación de información básica y permisos. |
| UC-29 | Buscar proyecto | `GET /proyectos/buscar`; filtros y visibilidad por membresía/privacidad. |
| UC-35 | Cancelar proyecto | `POST /proyectos/{id}/cancelar`; motivo obligatorio, rechazo de pendientes y notificaciones. |
| UC-36 | Modificar proyecto | `PUT /proyectos/{id}`; reglas de estado, permisos y requerimientos. |
| UC-49 | Crear actividad | `POST /proyectos/{id}/actividades`; programación regresiva básica. |
| UC-52 | Buscar actividades | `GET /proyectos/{id}/actividades/buscar`; filtros y visibilidad. |

**Derivado no computado:** UC-29 bis — `GET /proyectos/propios`.

### 5.3. UC en progreso

| UC | Nombre | Estado | Evidencia backend |
|---|---|---|---|
| UC-50 | Modificar actividad de planificación | Fases 1-5 de 6 completadas | `PUT /proyectos/{id}/actividades/{idActividad}` con reemplazo total; `GET` de detalle con profesionales asignados; listas obligatorias y sin elementos nulos; ciclos detectados antes de persistir con `409`; permiso validado antes del estado del proyecto; requerimientos con postulaciones o invitaciones bloqueados con `409`; CPM hacia adelante y hacia atrás; `estadoCronograma` derivado y modificación tolerante a desborde. Fase 5: matriz estricta de estados y notificaciones after-commit con fallos aislados. Pendiente: cierre formal de la fase 6 y documentacion final. |

> UC-50 no se computa como implementado y testeado hasta completar la fase 6.

### 5.4. UC no iniciados

Quedan 45 UC oficiales sin implementación:

- Publicaciones: UC-19 a UC-22, UC-61 a UC-66 y UC-69.
- Proyectos: UC-26 a UC-28, UC-30 a UC-34, UC-37, UC-38, UC-40, UC-41, UC-51, UC-60, UC-70, UC-72 y UC-73.
- Contratos: UC-39, UC-53, UC-54, UC-55 y UC-68.
- Conexiones y mensajería: UC-42 a UC-46.
- Usuarios: UC-09, UC-56 y UC-67.
- Perfiles: UC-15, UC-23, UC-47 y UC-48.

### 5.5. Observaciones documentales

- `estado_uc.md` subestima el avance de proyectos y el total global.
- La columna de tests de UC-13 está vacía aunque el estado declarado es “Implementado y testado”.
- `docs/frontend/aplicadas/ficha-tecnica-cancelar-proyecto.md` afirma que el backend no expone `/notificaciones` y que las notificaciones se deduplican por usuario; ambas afirmaciones quedaron desactualizadas.
- `docs/5_estado_sistema/estado_sistema_v.0.3.md` sigue mencionando la consolidación del motor de notificaciones como pendiente. Se deja intacto por decisión de versionado; v0.6 documenta el avance de UC-50.
- `docs/calidad/plan_de_pruebas.md` y `docs/calidad/controles_bd.md` están vacíos (0 bytes).
- `docs/calidad/informe-estrategia-notificaciones.md` existe pero está sin commitear al cierre de este relevamiento.

---

## 6. Módulo de notificaciones — detalle del último hito

### 6.1. Componentes backend

| Capa | Clases / artefactos |
|---|---|
| Controlador | `notificaciones/controlador/NotificacionController.java` |
| Servicio | `notificaciones/servicio/NotificacionService.java` |
| Repositorio | `repositorio/NotificacionRepository.java` |
| Entidad | `modelo/Notificacion.java` |
| DTOs | `NotificacionResponse`, `NotificacionResumenResponse`, `NotificacionMarcadoResponse` |
| Mapper | `notificaciones/mapper/NotificacionMapper.java` |
| Excepción | `NotificacionNoEncontradaException` + handler `404` en `GlobalExceptionHandler` |
| Generación Java | `CancelarProyectoService` |
| Generación SQL | Triggers de hitos, asignación y cupo |

### 6.2. Semántica de lectura

- El destinatario es siempre el **usuario** autenticado.
- El **perfil de contexto** es opcional y permite filtrar notificaciones de un perfil específico.
- `NotificacionService.listar` valida la pertenencia del perfil antes de consultar.
- `NotificacionService.resumen` devuelve total y no leídas, global o por perfil.
- `marcarLeida` usa una búsqueda por `idNotificacion + idUsuario`; una notificación ajena responde `404`.
- `marcarTodasLeidas` actualiza solo no leídas y setea `fechaLeida`.
- Los updates masivos son JPQL con `clearAutomatically` y `flushAutomatically`.

### 6.3. Generación

- **Cancelación de proyecto:** notifica al equipo activo, postulantes e invitados rechazados; cada notificación lleva `id_perfil_contexto` cuando existe perfil.
- **Hito cumplido:** trigger notifica a todos los perfiles activos del proyecto.
- **Actividad asignada:** trigger notifica al perfil asignado.
- **Cupo cubierto:** triggers rechazan postulaciones pendientes y notifican a los rechazados.
- **Origen:** `SERVICIO` para Java y `TRIGGER` para SQL.

### 6.4. Pruebas del módulo

- `NotificacionServiceTest`: 11 tests unitarios de listado, resumen, ownership, marcado y creación.
- `NotificacionControllerTest`: 5 tests de contrato MVC con filtros de seguridad deshabilitados.
- `NotificacionIntegrationTest`: 4 tests contra PostgreSQL real; aislamiento por usuario/perfil, orden, conteos, marcado y rechazo del trigger por perfil ajeno.
- `CancelarProyectoServiceTest`: cobertura de generación con perfil de contexto.
- `CancelarProyectoIntegrationTest`: cobertura de notificación al equipo y multi-perfil.

### 6.5. Limitaciones conocidas del backend

1. **Multi-perfil:** la cancelación deduplica por perfil, no por usuario. Un usuario con dos perfiles recibe dos notificaciones del mismo evento. Esto difiere de la recomendación del informe de estrategia y afecta el badge global si el frontend consulta sin perfil.
2. **Notificaciones sin contexto:** el overload `crear(usuario, tipo, mensaje, entidadTipo, entidadId)` genera `perfilContexto=null`; esas notificaciones no aparecen al filtrar por un perfil.
3. **Sin filtro de no leídas:** el listado no acepta `leida=false`.
4. **Paginación:** el máximo real es 100 hardcodeado; la configuración sembrada indica 50.
5. **Polling:** no se expone `NOTIFICACION_POLLING_SEGUNDOS`; el frontend no tiene un valor provisto por API.
6. **Purga:** no existe scheduler ni servicio de retención; las claves de configuración no se usan.
7. **Email/outbox:** `notificacion_entrega` y `notificacion_preferencia` existen en el esquema, pero no hay entidades, repositorios, servicios ni scheduler.
8. **N+1 potencial:** el mapper accede a `perfilContexto` lazy por cada notificación al listar; conviene proyección o `@EntityGraph`.
9. **Validación de contenido:** el servicio no valida tipo, mensaje ni destinatario; los errores se resuelven por constraints de BD.
10. **Idempotencia:** los triggers usan `NOT EXISTS`, pero no hay índice único que evite duplicados bajo concurrencia.
11. **Tests de seguridad:** el test de controlador usa `addFilters=false`; no cubre CSRF, `401`, ni el flujo JWT completo.
12. **Cobertura de triggers:** no hay tests de integración específicos para los triggers de hito, asignación y cupo.

---

## 7. Calidad y pruebas

### 7.1. Distribución de pruebas

| Módulo / área | Archivos de prueba | Métodos `@Test` |
|---|---:|---:|
| `admin` | 18 | 182 |
| `perfiles` | 21 | 139 |
| `auth` | 5 | 134 |
| `proyectos` | 12 | 129 |
| `repositorio` | 27 | 125 |
| `ubicacion` | 8 | 65 |
| `calendario` | 5 | 64 |
| `storage` | 4 | 37 |
| `usuario` | 6 | 33 |
| `actividades` | 6 | 51 |
| `security` | 3 | 17 |
| `notificaciones` | 3 | 21 |
| `common` | 2 | 12 |
| `config` | 1 | 3 |
| `home` | 1 | 3 |
| **Total** | **122** | **1.015** |

> El total incluye la clase base abstracta de integración, que no aporta métodos `@Test`.

> Suite completa ejecutada el 22/10/2026 con `mvn -q test`: **937 tests, 0 fallos**.

### 7.2. Integración continua y cobertura

- CI: `.github/workflows/tests.yml`.
- Runner: Ubuntu, Java 25 Temurin.
- Comando: `mvn -B verify`.
- JaCoCo genera reporte, pero **no ejecuta `jacoco:check`**; la regla del 80 % no está enforced.
- No hay umbral de cobertura por paquete.
- `plan_de_pruebas.md` y `controles_bd.md` están vacíos.

### 7.3. Estrategia de pruebas observada

- Unitarios de servicio con Mockito.
- Tests de controlador con `@WebMvcTest` y `MockMvc`.
- Tests de validación de DTOs.
- Tests de mappers.
- Tests de integración con Testcontainers y `@Transactional`.
- Tests de triggers y constraints en integración para casos seleccionados.

---

## 8. Deuda técnica y próximos pasos sugeridos

| Prioridad | Área | Evidencia | Acción sugerida |
|---|---|---|---|
| Alta | Semántica multi-perfil | `CancelarProyectoService.java:192` y test de multi-perfil | Definir si el producto quiere una notificación por usuario o una por perfil; ajustar badge, listado y documentación. |
| Alta | Purga y retención | Claves `NOTIFICACION_PURGA_*` sin uso | Implementar scheduler configurable con el patrón de `BajaScheduler`. |
| Alta | Filtro de no leídas | `NotificacionController` no recibe `leida` | Agregar filtro al repositorio/servicio/controlador. |
| Media | Configuración de paginación y polling | `NOTIFICACION_PAGINA_MAX=50` vs hardcode 100; polling sin exponer | Leer claves desde `ConfiguracionSistemaService` y exponerlas. |
| Media | Notificaciones sin contexto | Overload de 5 argumentos | Incluirlas en vistas por perfil o eliminarlas del contrato. |
| Media | N+1 en listado | Mapper con relación lazy | Usar proyección o `@EntityGraph`. |
| Media | Email/outbox | Tablas sin código | Implementar entidades, repositorios, servicio de entrega y scheduler. |
| Media | Cobertura de tests de seguridad | `addFilters=false` | Agregar tests de CSRF, JWT y 401/404 end-to-end. |
| Media | Idempotencia | `NOT EXISTS` sin índice único | Evaluar índice único lógico o control transaccional. |
| Baja | Validación de dominio | `crear(...)` sin validaciones | Validar tipo/mensaje/destinatario o mapear errores de integridad a 4xx. |
| Baja | Documentación | Varias fichas y estados desactualizados | Sincronizar `estado_uc.md`, fichas de cancelación y estado del sistema. |
| Baja | Cobertura enforced | JaCoCo sin `check` | Agregar umbral gradual, empezando por `notificaciones`. |

### 8.1. Próximo corte sugerido (v0.7)

1. Cerrar UC-50: fase 6 (suite definitiva, regresion completa y documentacion final).
2. Implementar UC-51 para completar el ciclo de actividades.
3. Cerrar la deuda del backend de notificaciones: filtro de no leídas, configuración de polling/paginación, purga y pruebas de seguridad.
4. Implementar UC-26 a UC-28 y UC-30 a UC-34 para habilitar el ciclo completo de postulaciones e invitaciones.
5. Sincronizar la documentación de estado y las fichas técnicas.
6. Evaluar el primer incremento de cobertura enforced.

---

## 9. Changelog v0.5 → v0.6

### UC-50 — Modificar actividad de planificación (fases 1 a 5)

- Se implementó el `PUT /proyectos/{id}/actividades/{idActividad}` como reemplazo total del formulario.
- Se agregó `GET /proyectos/{id}/actividades/{idActividad}` con ubicación, requerimientos, predecesoras y profesionales asignados.
- `predecesoras` y `requerimientos` pasaron a ser obligatorios; `descripcion` y `ubicacion` son opcionales y `null` borra.
- Se validan listas y elementos nulos con `400`, sin llegar al servicio.
- Se detectan ciclos de predecesoras con DFS en memoria antes de persistir, incluso con actividades no pendientes; la nueva `CicloActividadException` responde `409`.
- El trigger `trg_dependencia_sin_ciclos` se mantiene como red de seguridad y su violación también se mapea a `409`.
- El permiso `MODIFICAR_ACTIVIDAD` se valida antes del estado del proyecto, por lo que un usuario sin permiso recibe `403` sin filtrar el estado.
- Se reemplazó `esSatisfecho` por `tieneVinculos`: cualquier postulación o invitación asociada a un requerimiento, sin importar su estado, bloquea la modificación y la omisión con `409 RequerimientoConVinculosException`.
- Se implementó CPM en dos pasadas: programación hacia adelante desde `proyecto.fechaInicio` y cálculo de inicios tardíos hacia atrás desde la fecha de entrega.
- Se agregó `estadoCronograma` (`NORMAL`, `ALERTA_DESBORDE`) a la respuesta de detalle y modificación.
- El `PUT` usa `recalcularTolerandoDesborde`: si el fin calculado supera la fecha de entrega, responde `200` con alerta y conserva la modificación. El `GET` deriva el estado desde las fechas persistidas.
- `estadoCronograma` no se persiste como columna; no se modificó ningún archivo SQL.

### Fase 5 - Matriz de estados y notificaciones

- Se implemento la matriz estricta de transiciones: `PENDIENTE` solo puede conservarse o pasar a `EN_CURSO`/`CANCELADA`; `EN_CURSO` solo puede conservarse o pasar a `FINALIZADA`/`CANCELADA`; `FINALIZADA` y `CANCELADA` no se modifican.
- Una transicion fuera de la matriz responde `409` con `TransicionEstadoActividadInvalidaException`, sin persistir cambios.
- La notificacion `ACTIVIDAD_MODIFICADA` se desacoplo del servicio mediante `ActividadModificadaEvent` y `ApplicationEventPublisher`.
- El listener usa `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` y `@Transactional(propagation = REQUIRES_NEW)`; una notificacion por perfil asignado, sin duplicados.
- Los fallos de notificacion se registran con `WARN` y no revierten la actividad ya confirmada.
- Se agregaron pruebas de servicio, listener, MVC, handler global e integracion con Testcontainers para el flujo after-commit.
- No se modifico ningun archivo SQL.

### Calidad

- Se agregaron pruebas unitarias, MVC y de integración para UC-50.
- Estado actual: 122 archivos de prueba, 1.015 métodos `@Test` y 937 tests ejecutados en la suite completa, sin fallos.

### Documentación

- Se crea este `estado_sistema_v.0.6.md`.
- Se actualiza `docs/frontend/ficha-tecnica-modificar-actividad.md` con el contrato y el avance de UC-50.
- `docs/5_estado_sistema/estado_sistema_v.0.3.md` se deja intacto por decisión de versionado.

### Base heredada v0.4 → v0.5

#### Base de datos

- Adaptación completa del modelo CASE: enums en `UPPERCASE`, columnas y constraints alineados.
- Normalización del módulo de ubicación y catálogos geográficos.
- Ajustes de agenda, jornada, bloqueos y configuración de márgenes.
- Auditorías y correcciones de características técnicas, profesiones y unidades de medida.
- Incorporación de `polaroid_perfil` y triggers de protección.
- Nuevo modelo de notificaciones: `notificacion_tipo`, `notificacion`, `notificacion_entrega`, `notificacion_preferencia`.
- Triggers de hitos, asignación, cupo y validación de perfil de contexto.
- Índices y checks de notificaciones.
- Seeds de tipos de notificación y claves de configuración.

#### Backend

- UC-24 Crear proyecto.
- UC-25 Publicar proyecto.
- UC-29 Buscar proyecto y UC-29 bis Buscar proyectos propios.
- UC-35 Cancelar proyecto.
- UC-36 Modificar proyecto.
- UC-49 Crear actividad.
- UC-52 Buscar actividades.
- UC-74 Gestionar unidades de medida.
- Gestión de imágenes: fotos de perfil, moodboard, procesamiento WebP y limpieza.
- Primera API de notificaciones in-app con contexto de perfil.

#### Calidad

- Alineación de tests de integración a Testcontainers y al SQL canónico.
- Nuevas pruebas unitarias, de controlador, DTO, mapper e integración.
- Estado al cierre de v0.5: 117 archivos de prueba y 966 métodos `@Test`.

#### Documentación

- Se creó `estado_sistema_v.0.5.md`.
- Quedaron pendientes de sincronización `estado_uc.md`, las fichas de cancelación y los documentos de calidad.

---

## 10. Evidencia y método de relevamiento

- Commit analizado: `81980346c5d576bde21f5e092803448686bdd437` (2026-10-22 08:40:30 -0300).
- Working tree al corte: cambios locales de UC-50 (fases 1 a 5) sin commitear.
- Conteos de SQL obtenidos por inspección de `docs/3_diseño/ModaLinkBD.sql`.
- Conteos de Java, controladores, endpoints, entidades, repositorios y tests obtenidos por búsqueda sobre `src/main/java` y `src/test/java`.
- Verificación de espejo SQL por SHA-256 de ambos archivos.
- Suite completa ejecutada con `mvn -q test`: 937 tests, 0 fallos (22/10/2026).
- El alcance excluye explícitamente el frontend: los endpoints se documentan como contrato del backend, no como implementación de UI.

---

**Fin del documento — v0.6 (solo backend).**
