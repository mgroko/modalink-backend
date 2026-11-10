# Controles de Base de Datos — Presencia por perfil

> **Alcance:** extensión de Mi red. La presencia es por perfil, no por usuario.
> **Esquema canónico:** `docs/3_diseño/ModaLinkBD.sql`.
> **Migración:** `docs/3_diseño/migraciones/2026-11-09_presencia_perfiles.sql`.

## 1. Columna

`perfil.ultima_actividad`:

| Columna | Tipo | Regla |
|---|---|---|
| `ultima_actividad` | `timestamp` | Nullable. Último heartbeat del perfil. No se expone en JSON. |

- No tiene default: un perfil sin actividad queda desconectado.
- No se agrega índice: la lectura se hace por PK y los filtros de presencia se evalúan
  como predicado; un índice sobre esta columna rompería las actualizaciones HOT y no
  aporta para el patrón de consulta actual.
- No se agrega trigger de auditoría. El trigger existente `trg_auditar_cambio_perfil`
  está acotado a `estado, nombre_artistico, biografia, foto_perfil, id_profesion`
  (`ModaLinkBD.sql:6748`), por lo que un `UPDATE` de `ultima_actividad` no lo dispara.

## 2. Escritura (heartbeat)

El heartbeat usa una actualización bulk:

```sql
UPDATE perfil
SET ultima_actividad = :ahora
WHERE id_perfil = :idPerfil
  AND id_usuario = :idUsuario
  AND estado = 'ACTIVO'
  AND (ultima_actividad IS NULL OR ultima_actividad < :limite);
```

- `:limite = :ahora - umbral`.
- La condición `ultima_actividad IS NULL OR ...` permite el primer heartbeat.
- La condición de throttle se evalúa en la base, por lo que funciona aunque haya más
  de una instancia escribiendo.
- Se usa `@Modifying(clearAutomatically = true, flushAutomatically = true)` para no
  dejar entidades obsoletas en el contexto de persistencia.
- **No** se implementa con `perfil.setUltimaActividad(...); save(perfil)`: Hibernate
  actualizaría todas las columnas y dispararía innecesariamente los triggers de perfil.
- El usuario y el perfil se validan antes con `PerfilActivoService` (usuario `ACTIVO`,
  perfil `ACTIVO`, pertenencia al usuario autenticado).
- El heartbeat no genera notificaciones ni auditoría.

## 3. Lectura (fórmula)

```text
enLinea(perfil) =
    perfil.estado = 'ACTIVO'
    AND perfil.usuario.estado = 'ACTIVO'
    AND perfil.ultima_actividad IS NOT NULL
    AND perfil.ultima_actividad >= ahora - umbral
```

- El umbral por defecto es 120 segundos (`app.presencia.umbral-en-linea`).
- El borde es inclusivo: exactamente en el umbral se considera en línea.
- `GET /presencia` usa una proyección con `idPerfil, estado, usuario.estado,
  ultima_actividad` para no generar N+1.
- Un perfil de otro usuario se ve desconectado si su usuario no está `ACTIVO`.
- Un perfil no activo del mismo usuario se ve desconectado; no se contagia la
  presencia del perfil activo.

## 4. Filtros y resumen

- `GET /conexiones?estadoPresencia=EN_LINEA|DESCONECTADO` aplica la fórmula sobre la
  contraparte de la conexión.
- `DESCONECTADO` es la negación de la fórmula, incluyendo perfiles sin actividad y
  usuarios inactivos.
- `GET /conexiones/resumen` devuelve:
  - total de conexiones activas;
  - total en línea;
  - conexiones agrupadas por profesión de la contraparte.
- El resumen usa una consulta nativa con `CASE` para resolver la contraparte según
  cuál de los dos perfiles sea el activo.

## 5. Tiempo real

- `GET /presencia/stream` usa `SseEmitter` con timeout de 30 minutos.
- El bus es en memoria (`SsePresenciaEventBus`) y asume una sola instancia.
- Se envía un snapshot inicial y luego eventos de cambio.
- El scheduler expira cada 5 segundos los perfiles cuyo heartbeat superó el umbral y
  publica el evento offline.
- El registro en memoria se actualiza en cada heartbeat, aunque la escritura en base
  se saltee por throttle. La lectura siempre usa la base.
- El ping SSE se envía cada 20 segundos.
- Para múltiples instancias hace falta Redis Pub/Sub o PostgreSQL `LISTEN/NOTIFY`.

## 6. Casos de prueba

| Caso | Resultado esperado |
|---|---|
| Perfil sin `ultima_actividad` | Desconectado |
| Perfil activo con heartbeat reciente | En línea |
| Perfil activo con heartbeat exactamente en el umbral | En línea |
| Perfil activo con heartbeat fuera del umbral | Desconectado |
| Perfil activo con usuario `PENDIENTE_BAJA` | Desconectado |
| Perfil `PENDIENTE_BAJA` con heartbeat reciente | Desconectado |
| Heartbeat dentro del throttle | No actualiza la base, pero mantiene el registro en línea |
| Heartbeat consecutivo | No genera auditoría |
| Logout | No marca offline; vence por umbral |
| Perfil dado de baja | Desconectado en la siguiente lectura; evento SSE al vencer |
| Dos dispositivos con el mismo perfil | En línea mientras uno tenga heartbeat |
| Dos dispositivos con perfiles distintos | Cada perfil tiene su propia presencia |
| `GET /conexiones?estadoPresencia=EN_LINEA` | Sólo contrapartes en línea |
| `GET /conexiones?estadoPresencia=DESCONECTADO` | Sólo contrapartes desconectadas |
| `GET /presencia` con más de 200 IDs | `400 IDS_PERFIL_INVALIDOS` |
| `GET /presencia/stream` sin IDs | `400` |
