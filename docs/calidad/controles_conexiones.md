# Controles de Base de Datos — Red de contactos (UC-43)

> **Alcance:** esquema base de UC-43 y preparación de UC-44, UC-45 y UC-46.
> **Esquema canónico:** `docs/3_diseño/ModaLinkBD.sql`.
> **Migración:** `docs/3_diseño/migraciones/2026-11-08_uc43_solicitud_conexion.sql`.

## 1. Tablas

### 1.1 `solicitud_conexion`

| Columna | Tipo | Regla |
|---|---|---|
| `id_solicitud_conexion` | `int8` identity | PK |
| `id_perfil_remitente` | `int8` | FK a `perfil`, obligatorio |
| `id_perfil_destinatario` | `int8` | FK a `perfil`, obligatorio |
| `estado` | `varchar(20)` | `PENDIENTE`, `ACEPTADA`, `RECHAZADA` |
| `fecha_envio` | `timestamp` | Default `CURRENT_TIMESTAMP` |
| `fecha_respuesta` | `timestamp` | Nula en `PENDIENTE`; obligatoria en estados resueltos |

### 1.2 `conexion`

| Columna | Tipo | Regla |
|---|---|---|
| `id_conexion` | `int8` identity | PK |
| `id_perfil_1` | `int8` | FK a `perfil`, menor del par |
| `id_perfil_2` | `int8` | FK a `perfil`, mayor del par |
| `id_solicitud_conexion` | `int8` | FK única a la solicitud aceptada |
| `estado` | `varchar(20)` | `ACTIVA`, `ELIMINADA` |
| `fecha_inicio_conexion` | `timestamp` | Default `CURRENT_TIMESTAMP` |
| `fecha_fin_conexion` | `timestamp` | Nula en `ACTIVA`; obligatoria en `ELIMINADA` |

## 2. Constraints e índices

| Nombre | Tipo | Regla |
|---|---|---|
| `chk_solicitud_conexion_no_auto` | CHECK | Remitente distinto de destinatario |
| `chk_solicitud_conexion_estado` | CHECK | Estado válido |
| `chk_solicitud_conexion_fechas` | CHECK | Coherencia estado/fecha de respuesta |
| `UQ_solicitud_conexion_pendiente` | UNIQUE parcial | Una solicitud pendiente por par, sin importar la dirección |
| `chk_conexion_orden` | CHECK | `id_perfil_1 < id_perfil_2` |
| `chk_conexion_estado` | CHECK | Estado válido |
| `chk_conexion_fechas` | CHECK | Coherencia estado/fecha de fin |
| `UQ_conexion_solicitud` | UNIQUE | Una conexión por solicitud aceptada |
| `UQ_conexion_activa` | UNIQUE parcial | Una conexión activa por par |

## 3. Triggers

| Trigger | Tabla | Regla |
|---|---|---|
| `trg_validar_solicitud_conexion` | `solicitud_conexion` | Perfiles activos, usuarios activos, usuarios distintos, estado inicial y transiciones |
| `trg_validar_conexion` | `conexion` | Solicitud origen `ACEPTADA`, mismo par, perfiles activos, usuarios distintos y transiciones |

### 3.1 Reglas de transición

- `solicitud_conexion`: sólo `PENDIENTE → ACEPTADA` o `PENDIENTE → RECHAZADA`.
- `conexion`: sólo `ACTIVA → ELIMINADA`.
- Los perfiles y la fecha de envío/inicio no se pueden modificar.
- La fecha de respuesta/fin se completa con `clock_timestamp()` si llega nula.

## 4. Notificaciones

Tipos agregados a `notificacion_tipo`:

| Código | Categoría | Prioridad |
|---|---|---|
| `SOLICITUD_CONEXION_RECIBIDA` | `SOCIAL` | `ALTA` |
| `SOLICITUD_CONEXION_ACEPTADA` | `SOCIAL` | `MEDIA` |
| `SOLICITUD_CONEXION_RECHAZADA` | `SOCIAL` | `MEDIA` |
| `CONEXION_ELIMINADA` | `SOCIAL` | `MEDIA` |

## 5. Casos de prueba de base de datos

| Caso | Resultado esperado |
|---|---|
| Insertar solicitud con remitente = destinatario | Falla por CHECK |
| Insertar dos solicitudes pendientes entre el mismo par, misma dirección | Falla por índice único |
| Insertar dos solicitudes pendientes entre el mismo par, dirección inversa | Falla por índice único |
| Insertar solicitud entre perfiles del mismo usuario | Falla por trigger |
| Insertar solicitud con perfil inactivo | Falla por trigger |
| Insertar solicitud cuando ya existe una conexión activa | Falla por trigger |
| Insertar conexión con par desordenado | Falla por CHECK |
| Insertar conexión desde solicitud `PENDIENTE` | Falla por trigger |
| Insertar dos conexiones activas para el mismo par | Falla por índice único |
| Eliminar lógicamente una conexión y crear otra para el mismo par | Permitido |

## 6. Cancelación de solicitud (extensión UC-43)

La cancelación de una solicitud enviada se resuelve con **borrado físico**:

- Sólo el perfil remitente puede cancelar.
- Sólo se permite mientras la solicitud esté `PENDIENTE`.
- Se elimina la fila de `solicitud_conexion`.
- Se elimina la notificación `SOLICITUD_CONEXION_RECIBIDA` asociada
  (`tipo + entidad_tipo + entidad_id`).
- El índice único parcial queda liberado, por lo que el par puede volver a
  enviar una solicitud.
- No se crea una notificación de cancelación.
- Si existiera una conexión que referencia la solicitud, el borrado fallaría
  por FK; el flujo de negocio lo impide porque sólo cancela solicitudes
  `PENDIENTE`.

## 7. Búsqueda de solicitudes (UC-46)

- La búsqueda se acota siempre al perfil activo.
- Se apoya en los índices existentes:
  - `(id_perfil_destinatario, fecha_envio DESC)`
  - `(id_perfil_remitente, fecha_envio DESC)`
- El filtro de texto usa `unaccent(lower(columna))` y un patrón normalizado en
  Java, por lo que es case-insensitive e insensible a acentos.
- La extensión `unaccent` se crea con
  `CREATE EXTENSION IF NOT EXISTS unaccent;` en el esquema canónico y en la
  migración `2026-11-09_uc46_buscar_solicitudes_conexion.sql`.
- No se crean tablas ni columnas nuevas.
- La consulta se pagina en base de datos y luego se cargan las asociaciones de
  la página con `@EntityGraph` para evitar N+1.
- Sin resultados se devuelve `200` con página vacía.
- El tamaño máximo de página es 100.

## 8. Gestión de solicitudes (UC-45)

- Solo el perfil destinatario puede aceptar o rechazar.
- La solicitud se bloquea con `PESSIMISTIC_WRITE` para serializar aceptar,
  rechazar y cancelar.
- Aceptar:
  - actualiza la solicitud a `ACEPTADA` y `fecha_respuesta`;
  - hace `saveAndFlush` de la solicitud antes de insertar la conexión, porque
    `trg_validar_conexion` exige que la solicitud ya esté `ACEPTADA`;
  - inserta `conexion` con par canónico y `fecha_inicio_conexion`;
  - crea dos notificaciones `SOLICITUD_CONEXION_ACEPTADA`;
  - el trigger `trg_auditar_conexion` registra `CONEXION_CREADA`.
- Rechazar:
  - actualiza la solicitud a `RECHAZADA` y `fecha_respuesta`;
  - no crea conexión;
  - crea una sola notificación `SOLICITUD_CONEXION_RECHAZADA` para el remitente;
  - no registra auditoría.
- Si ya existe una conexión activa, aceptar y rechazar devuelven `409`.
- Si el remitente, destinatario o usuario no está activo, devuelve `409`.

## 9. Auditoría de conexiones

El trigger `trg_auditar_conexion` registra:

| Operación | Evento | Acción |
|---|---|---|
| `INSERT` en `conexion` | `CONEXION_CREADA` | `INSERT` |
| `UPDATE` de `ACTIVA` a `ELIMINADA` | `CONEXION_ELIMINADA` | `UPDATE` |
| `DELETE` físico de `conexion` | `CONEXION_ELIMINADA` | `DELETE` |

No se audita `solicitud_conexion`: ni el envío, ni la cancelación, ni el
rechazo generan eventos de auditoría.

La migración asociada es
`docs/3_diseño/migraciones/2026-11-09_uc45_auditoria_conexiones.sql`.
