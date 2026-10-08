# Ficha técnica — Jornada laboral corrida y partida (breaking change)

**Audiencia:** agente/equipo de frontend (modalink-frontend).
**Origen del cambio:** refactor del módulo calendario (jornada partida/corrido) con validación soportada por los checks `chk_jornada_*` de `docs/3_diseño/ModaLinkBD.sql`.
**Estado:** implementado y testeado en backend.

> **Nota de nombres (H-08):** los atributos fueron renombrados de `horario…`/`maniana` a `hora…`/`manana` para corresponder a las columnas de la BD. Los nombres válidos en el contrato son **`horaInicioManana`, `horaFinManana`, `horaInicioTarde`, `horaFinTarde`**.

## 1. Qué cambió

La jornada laboral de cada día deja de ser un único rango `horaInicio`/`horaFin` y pasa a
soportar **jornada partida** (bloques de mañana y tarde), manteniendo la opción de
**jornada de corrido**. Los campos viejos **ya no existen**: es un cambio de contrato
sin compatibilidad hacia atrás. Todo cliente que use `horaInicio`/`horaFin` debe migrarse.

| Antes (eliminar) | Ahora |
|---|---|
| `horaInicio` (obligatorio) | `horaInicioManana` (obligatorio) |
| `horaFin` (obligatorio) | `horaFinTarde` (obligatorio) |
| — | `horaFinManana` (opcional, ver §3) |
| — | `horaInicioTarde` (opcional, ver §3) |

## 2. Endpoints afectados

### `PUT /calendario/jornada` (requiere autenticación)

Reemplaza la jornada completa del usuario (semántica "reemplazo total", sin cambios:
los días presentes son los días laborables; un día ausente = no laborable).

**Body — `ConfigJornadaRequest`:**

```json
{
  "margenActividadMinutos": 60,
  "dias": [
    {
      "diaSemana": 1,
      "horaInicioManana": "09:00",
      "horaFinManana": null,
      "horaInicioTarde": null,
      "horaFinTarde": "18:00"
    }
  ]
}
```

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `margenActividadMinutos` | number (int) | Sí | Minutos de buffer a cada lado de una actividad. `@Min(0)` — no puede ser negativo. |
| `dias` | array | Sí, **no vacío** (`@NotEmpty`) | Máx. 7 elementos, `diaSemana` sin repetir. |
| `dias[].diaSemana` | number (int) | Sí | ISO 8601: `1` = Lunes … `7` = Domingo. |
| `dias[].horaInicioManana` | string hora | Sí | Inicio de la jornada (corrida o partida). |
| `dias[].horaFinManana` | string hora o `null` | Solo si partida | Fin del bloque de la mañana. |
| `dias[].horaInicioTarde` | string hora o `null` | Solo si partida | Inicio del bloque de la tarde. |
| `dias[].horaFinTarde` | string hora | Sí | Fin de la jornada (corrida o partida). |

**Formato de horas:** el backend acepta `"HH:mm"` y `"HH:mm:ss"` (deserialización ISO-8601),
y **devuelve siempre `"HH:mm:ss"`** (p.ej. `"09:00:00"`). Tenerlo en cuenta al
parsear/comparar.

**Respuesta 200 — `ConfigJornadaResponse`:** mismo objeto que el request
(`{ margenActividadMinutos, dias[] }`) con las horas en formato `"HH:mm:ss"`.

### `GET /calendario`

La sección `jornada` de la respuesta usa los mismos campos nuevos:

```json
{
  "jornada": { "margenActividadMinutos": 60, "dias": [ ... ] },
  "bloqueosManuales": [ ... ],
  "actividades": [ ... ]
}
```

`bloqueosManuales` y `actividades` **no cambiaron**.

## 3. Semántica corrido vs. partido (clave para la UI)

| Tipo | Cómo se representa | Ejemplo |
|---|---|---|
| **Corrido** | Solo `horaInicioManana` + `horaFinTarde`; el par del mediodía en `null` (u omitido al enviar) | 09:00–18:00 |
| **Partido** | Las 4 horas, en orden estricto | 09:00–13:00 y 15:00–19:00 |
| **Solo mañana / solo tarde / pocas horas** | Se modela como **corrido** con ese rango | 14:00–18:00 |

Reglas de negocio (validadas en backend y en base de datos; no re-implementar en el front,
solo espejarlas en la UI para buena UX):

1. `horaFinTarde` > `horaInicioManana`, siempre.
2. **El par del mediodía va completo o no va**: enviar solo `horaFinManana` o solo
   `horaInicioTarde` es rechazado.
3. Si es partida: `horaInicioManana` < `horaFinManana` < `horaInicioTarde`
   < `horaFinTarde` (estricto; no valen empates).
4. Un día puede pasar de corrido a partido y viceversa editándolo; el backend lo persiste
   por diff (sin recrear la fila).

**Patrón de UI sugerido:** toggle "¿Jornada partida?" por día (o global). Si está apagado,
mostrar 2 inputs (inicio/fin) y enviar el par del mediodía en `null`. Si está prendido,
mostrar 4 inputs. Después de un `GET`, inferir el estado del toggle con
`horaFinManana != null && horaInicioTarde != null`.

## 4. Errores

### 400 — validación de campos (Bean Validation)

Disparadores: `dias` vacío, `diaSemana`/`horaInicioManana`/`horaFinTarde` nulos.
Observación: nulos por campo (el JSON con campos ausentes también cuenta como nulo).

```json
{
  "message": "Validación fallida",
  "errores": { "dias[0].horaInicioManana": "must not be null" },
  "httpStatus": 400,
  "timestamp": 1788000000000
}
```

### 400 — reglas de jornada (`JornadaInvalidaException`)

```json
{ "message": "<motivo>", "httpStatus": 400, "timestamp": 1788000000000 }
```

Mensajes posibles (mostrar tal cual al usuario):

- `El día de la semana debe estar entre 1 (Lunes) y 7 (Domingo).`
- `El horario de fin debe ser posterior al horario de inicio.`
- `Para una jornada partida se deben informar el fin del bloque de la mañana y el inicio
  del bloque de la tarde; para una jornada de corrido, ninguno de los dos.`
- `El fin del bloque de la mañana debe ser posterior a su inicio.`
- `El bloque de la tarde debe comenzar después del fin del bloque de la mañana.`
- `El fin del bloque de la tarde debe ser posterior a su inicio.`
- `No se puede repetir el mismo día de la semana en la jornada.`

### Otros códigos del módulo (sin cambios)

- `401` sin sesión válida; `403` sin CSRF token en el PUT.
- Bloqueos (`POST/DELETE /calendario/bloqueos*`): sin cambios de contrato ni de errores.

## 5. Checklist de migración para el front

- [ ] Reemplazar todo uso de `horaInicio`/`horaFin` (y los nombres intermedios
      `horarioInicioManiana`/`horarioFinManiana`/etc.) por `horaInicioManana`/
      `horaFinManana`/`horaInicioTarde`/`horaFinTarde`.
- [ ] Manejar `horaFinManana`/`horaInicioTarde` potencialmente `null` al renderizar
      (no asumir 4 horas siempre presentes).
- [ ] Agregar UI para jornada partida (toggle + 2 inputs extra por día).
- [ ] Al armar el PUT, limpiar/normalizar: corrido ⇒ mediodía en `null`; nunca enviar solo
      una de las dos horas del mediodía.
- [ ] Normalizar el formato de hora al comparar (`"09:00:00"` del backend vs `"09:00"` local).
- [ ] Mapear los nuevos mensajes de error 400 (mostrar `message` directamente).

## 6. Ejemplo completo (corrido Lun + partido Mar)

```jsonc
// PUT /calendario/jornada
{
  "margenActividadMinutos": 45,
  "dias": [
    { "diaSemana": 1, "horaInicioManana": "09:00", "horaFinManana": null,
      "horaInicioTarde": null, "horaFinTarde": "18:00" },
    { "diaSemana": 2, "horaInicioManana": "09:00", "horaFinManana": "13:00",
      "horaInicioTarde": "15:00", "horaFinTarde": "19:00" }
  ]
}
```

---

## 7. Referencias de implementación Backend

| Aspecto | Archivo |
| :--- | :--- |
| Endpoint y DTOs | `calendario/controlador/CalendarioController.java` |
| Request/Response de jornada | `calendario/dto/ConfigJornadaRequest.java`, `ConfigJornadaResponse.java`, `JornadaDiaRequest.java`, `JornadaDiaResponse.java` |
| Validaciones y mensajes de error | `calendario/servicio/CalendarioService.java` (`validarJornada`, `validarHorarios`) |
| Mapeo de respuesta | `calendario/mapper/CalendarioMapper.java` |
| Checks de BD (`chk_jornada_*`) | `docs/3_diseño/ModaLinkBD.sql` (tabla `jornada_agenda`) |
| Manejo de errores (400/401/403) | `common/exception/GlobalExceptionHandler.java` |
