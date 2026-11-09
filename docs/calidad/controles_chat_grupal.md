# Controles de Base de Datos — Chat grupal (UC-41)

> **Alcance:** chat de texto único por proyecto.
> **Esquema canónico:** `docs/3_diseño/ModaLinkBD.sql`.
> **Migración:** `docs/3_diseño/migraciones/2026-11-09_uc41_mensajes_grupales.sql`.

## 1. Tabla `mensaje_proyecto`

| Columna | Tipo | Regla |
|---|---|---|
| `id_mensaje_proyecto` | `int8` identity | PK |
| `id_proyecto` | `int8` | FK a `proyecto` |
| `id_perfil_emisor` | `int8` | FK a `perfil` |
| `contenido` | `varchar(1000)` | No vacío, máximo 1000 caracteres |
| `fecha_envio` | `timestamp` | Default `CURRENT_TIMESTAMP` |

## 2. Constraints e índices

| Nombre | Tipo | Regla |
|---|---|---|
| `chk_mensaje_proyecto_contenido` | CHECK | `char_length(btrim(contenido)) BETWEEN 1 AND 1000` |
| `IDX_mensaje_proyecto_historial` | índice | Historial por proyecto y fecha descendente |
| `UQ_notificacion_mensaje_grupal_no_leida` | índice único parcial | Una notificación no leída por usuario, perfil y proyecto |

## 3. Trigger `trg_validar_mensaje_proyecto`

| Operación | Regla |
|---|---|
| `INSERT` | El proyecto debe existir y estar `PUBLICADO` o `CONFIRMADO`. El emisor debe ser miembro `ACTIVO` del proyecto. El perfil y el usuario del emisor deben estar `ACTIVO`. |
| `UPDATE` | Rechazado. Los mensajes son inmutables. |
| `DELETE` | Rechazado. El historial no se borra. |

## 4. Notificación

- Tipo: `MENSAJE_GRUPAL_RECIBIDO`.
- Categoría: `PROYECTO`.
- Prioridad: `MEDIA`.
- `entidad_tipo`: `PROYECTO`.
- `entidad_id`: `idProyecto`.
- `id_proyecto`: `idProyecto`.
- `perfil_contexto`: perfil destinatario.
- Se crea una sola notificación no leída por participante y proyecto.
- No se notifica al emisor ni a otros perfiles del mismo usuario.
- Al abrir el chat se marca como leída.
- No hay estado leído por mensaje.

## 5. Casos de prueba

| Caso | Resultado esperado |
|---|---|
| Insertar mensaje de miembro activo en proyecto publicado | Permitido |
| Insertar mensaje de no miembro | Falla por trigger |
| Insertar mensaje de perfil o usuario inactivo | Falla por trigger |
| Insertar mensaje en proyecto `BORRADOR`, `FINALIZADO` o `CANCELADO` | Falla por trigger |
| Insertar mensaje vacío o de más de 1000 caracteres | Falla por `CHECK` |
| Modificar un mensaje | Falla por trigger |
| Eliminar un mensaje | Falla por trigger |
| Enviar dos mensajes sin leer | Una sola notificación no leída por participante |
| Abrir el chat | La notificación queda leída |
| Enviar después de leer | Se crea una nueva notificación no leída |
| Proyecto finalizado | Historial visible; envío rechazado |
| Perfil ajeno al proyecto | `403` sin acceso al chat |
| Enviar mensaje | No genera auditoría |
