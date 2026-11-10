# Controles de Base de Datos — Mensajería privada (UC-42)

> **Alcance:** chat 1 a 1 entre perfiles con conexión activa.
> **Esquema canónico:** `docs/3_diseño/ModaLinkBD.sql`.
> **Migración:** `docs/3_diseño/migraciones/2026-11-09_uc42_mensajes_privados.sql`.
> **Chat grupal (UC-41):** ver `docs/calidad/controles_chat_grupal.md`.

## 1. Tablas

### 1.1 `conversacion`

| Columna | Tipo | Regla |
|---|---|---|
| `id_conversacion` | `int8` identity | PK |
| `id_perfil_1` | `int8` | FK a `perfil`; menor del par |
| `id_perfil_2` | `int8` | FK a `perfil`; mayor del par |
| `fecha_creacion` | `timestamp` | Default `CURRENT_TIMESTAMP` |
| `fecha_ultimo_mensaje` | `timestamp` | Nula hasta el primer mensaje |

### 1.2 `mensaje`

| Columna | Tipo | Regla |
|---|---|---|
| `id_mensaje` | `int8` identity | PK |
| `id_conversacion` | `int8` | FK a `conversacion` |
| `id_perfil_emisor` | `int8` | FK a `perfil` |
| `contenido` | `varchar(1000)` | No vacío, máximo 1000 caracteres |
| `fecha_envio` | `timestamp` | Default `CURRENT_TIMESTAMP` |
| `leido` | `boolean` | `false` por defecto; leído por la contraparte |
| `fecha_lectura` | `timestamp` | Nula si no leído; obligatoria si leído |

## 2. Constraints e índices

| Nombre | Tipo | Regla |
|---|---|---|
| `chk_conversacion_orden` | CHECK | `id_perfil_1 < id_perfil_2` |
| `UQ_conversacion_par` | UNIQUE | Un par de perfiles por conversación |
| `chk_mensaje_contenido` | CHECK | `char_length(btrim(contenido)) BETWEEN 1 AND 1000` |
| `chk_mensaje_lectura` | CHECK | Coherencia `leido`/`fecha_lectura` |
| `IDX_conversacion_perfil_1` | índice | Lista por perfil 1 y último mensaje |
| `IDX_conversacion_perfil_2` | índice | Lista por perfil 2 y último mensaje |
| `IDX_mensaje_conversacion` | índice | Historial por conversación y fecha |
| `IDX_mensaje_no_leido` | índice parcial | Mensajes no leídos por conversación |
| `UQ_notificacion_conversacion_no_leida` | índice único parcial | Una notificación no leída por conversación |

## 3. Triggers

| Trigger | Tabla | Regla |
|---|---|---|
| `trg_validar_conversacion` | `conversacion` | Perfiles de usuarios distintos, ambos activos, conexión `ACTIVA` al crear, par y fecha de creación inmutables |
| `trg_validar_mensaje` | `mensaje` | Conversación existente, emisor participante, conexión activa, perfiles/usuarios activos, contenido válido; sólo permite cambiar `leido`/`fecha_lectura`; no permite borrar |
| `trg_actualizar_ultimo_mensaje` | `mensaje` | `AFTER INSERT`: actualiza `fecha_ultimo_mensaje` con el mayor timestamp |

## 4. Notificación

- Tipo: `MENSAJE_PRIVADO_RECIBIDO`, categoría `SOCIAL`, prioridad `MEDIA`.
- `perfil_contexto`: perfil receptor.
- `entidad_tipo`: `CONVERSACION`.
- `entidad_id`: `idConversacion`.
- `link`: `/mensajes?conversacion={idConversacion}`.
- Una sola notificación **no leída** por conversación. Se crea cuando la conversación
  pasa de leída a no leída; los mensajes siguientes no generan otra mientras queden
  mensajes no leídos.
- Al marcar toda la conversación como leída se marca la notificación como leída.
- Si se marca la notificación como leída sin abrir el chat, el contador de
  conversaciones no leídas sigue. Mientras la conversación siga teniendo mensajes no
  leídos no se crea otra notificación; después de leer toda la conversación, un mensaje
  nuevo puede generar una nueva.
- Si `PATCH /leido` se hace con `hastaIdMensaje` y quedan mensajes posteriores no
  leídos, la notificación **no** se marca como leída.
- No se audita el contenido de los mensajes.

## 5. Escritura efímera

- `POST /conversaciones/{id}/escribiendo` no inserta filas ni usa triggers.
- El evento SSE `escribiendo` vive en memoria y solo se envía a la contraparte.
- No genera notificación ni auditoría; si se pierde, el cliente lo oculta por TTL.
- La validación es la misma que la del envío: perfil activo participante, conexión
  `ACTIVA` y contraparte activa.

## 5. Lectura

- `leido = false` significa que la contraparte todavía no leyó el mensaje.
- `leido = true` significa leído por la contraparte; el emisor ve la confirmación.
- El contador `conversacionesNoLeidas` cuenta conversaciones con al menos un mensaje
  no leído de la contraparte; no cuenta mensajes.
- `PATCH /conversaciones/{id}/leido` puede recibir `hastaIdMensaje` para no marcar un
  mensaje que llegó después de abrir el chat.

## 6. Casos de prueba

| Caso | Resultado esperado |
|---|---|
| Crear conversación con conexión activa | Permitido |
| Crear conversación sin conexión activa | Falla por trigger |
| Crear conversación entre perfiles del mismo usuario | Falla por trigger |
| Crear dos conversaciones para el mismo par | Falla por índice único |
| Insertar mensaje con emisor ajeno | Falla por trigger |
| Insertar mensaje sin conexión activa | Falla por trigger |
| Insertar mensaje vacío o de más de 1000 caracteres | Falla por CHECK |
| Insertar mensaje leído | Falla por trigger; el alta debe ser no leída |
| Editar contenido o emisor | Falla por trigger |
| Eliminar mensaje | Falla por trigger |
| Marcar mensaje como leído | Permitido; completa `fecha_lectura` |
| Volver un mensaje leído a no leído | Falla por trigger |
| Insertar dos notificaciones no leídas de la misma conversación | Falla por índice único |
| Marcar conversación como leída | Mensajes y notificación quedan leídos |
| Eliminar conexión | Historial visible; nuevos mensajes fallan por trigger |
| Enviar mensaje | No genera auditoría |
