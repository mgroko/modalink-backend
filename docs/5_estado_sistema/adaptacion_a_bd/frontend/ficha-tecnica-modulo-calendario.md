# Ficha técnica — Módulo de Calendario (jornada, bloqueos y actividades)

**Audiencia:** agente/equipo de frontend (modalink-frontend).
**Origen:** especificación del módulo `calendario` (5 endpoints) contrastada contra el código actual del backend.
**Estado:** implementado y testeado en backend (`CalendarioControllerTest`, `CalendarioServiceTest`, `CalendarioMapperTest`, `ConfigJornadaRequestValidationTest`, `MarcarNoDisponibleRequestValidationTest`).

> **Nota de nombres (H-08):** los campos de jornada son **`horaInicioManana`, `horaFinManana`, `horaInicioTarde`, `horaFinTarde`**. No existen `horaInicio`/`horaFin`, ni `horario…`, ni `maniana`.
>
> **Contrato de jornada en profundidad:** la semántica corrido/partido, las 4 reglas de negocio, los 7 mensajes de error y el checklist de migración de `PUT /calendario/jornada` están especificados en [`ficha-tecnica-jornada-partida.md`](../../../frontend/ficha-tecnica-jornada-partida.md). Esta ficha los resume y agrega el resto del módulo (perfil público, bloqueos, errores, UI/UX).

---

## 1. Visión general

El módulo expone la agenda del usuario en tres capas:

| Capa | Fuente | Persistida | Editable por el usuario |
| :--- | :--- | :--- | :--- |
| **Jornada laboral** | `jornada_agenda` (1 fila por día laborable) | Sí | Sí, vía `PUT /calendario/jornada` |
| **Bloqueos manuales** | `bloqueo_agenda` | Sí | Sí, vía `POST`/`DELETE /calendario/bloqueos*` |
| **Bloqueos por actividad** | Calculados de `actividad` + margen de la agenda | **No** | **No** (solo lectura) |

El backend **no almacena "disponibilidad" como tal**: devuelve las tres capas y el frontend las superpone para pintar el calendario.

**Estado inicial de todo usuario** (trigger `fn_crear_agenda`, `ModaLinkBD.sql:1668-1706`): al registrarse se le crea una `agenda` con `margen_actividad_min` = `AGENDA_MARGEN_ACTIVIDAD_MIN` de `configuracion_sistema` (o `30` si falta/inválida) y **jornada corrida L–V de 09:00 a 18:00** (sáb/dom ausentes = no laborables). El frontend puede asumir que `GET /calendario` siempre trae al menos 5 días.

---

## 2. Seguridad y contexto de autenticación

Fuente: `security/SecurityConfig.java`.

| Punto | Comportamiento real |
| :--- | :--- |
| Sesión | Cookie **`jwt` HttpOnly**. Enviar con `credentials: 'include'` / `withCredentials: true` (ver `guia-integracion-perfil-activo.md`). |
| ¿Endpoints públicos? | **Ninguno.** `SecurityConfig` solo hace `permitAll` en `/auth/**`, `/error`, `/uploads/**` y aplica `anyRequest().authenticated()`. **`GET /calendario/perfil/{id}` también requiere sesión válida**: es "público" en el sentido de que cualquiera autenticado puede verlo, no que sea anónimo. |
| Autorización por rol | No hay `@PreAuthorize` en `CalendarioController`: cualquier usuario autenticado accede a los 5 endpoints. |
| CSRF | Habilitado (`CookieCsrfTokenRepository.withHttpOnlyFalse()`). `POST`, `PUT` y `DELETE` requieren el token en la cabecera **`X-XSRF-TOKEN`** (cookie `XSRF-TOKEN`, expuesta por `CsrfCookieFilter`). `GET` no lo necesita. |
| `idUsuario` | Se toma del `Authentication.getPrincipal()` (subject del JWT), no de un path/query param: `CalendarioController.java:68-70`. |

---

## 3. Endpoints

### 3.1 `GET /calendario` — calendario del usuario autenticado

| Método | Ruta | Auth | Éxito |
| :--- | :--- | :--- | :--- |
| GET | `/calendario` | Requiere sesión | `200 OK` |

**Precondiciones** (`CalendarioService.obtener`): el usuario debe existir y estar en estado `ACTIVO`; debe existir su `agenda`.

**Respuesta `200` — `CalendarioResponse`:**

```json
{
  "jornada": {
    "margenActividadMinutos": 30,
    "dias": [
      {
        "diaSemana": 1,
        "horaInicioManana": "09:00:00",
        "horaFinManana": null,
        "horaInicioTarde": null,
        "horaFinTarde": "18:00:00"
      },
      {
        "diaSemana": 3,
        "horaInicioManana": "09:00:00",
        "horaFinManana": "13:00:00",
        "horaInicioTarde": "15:00:00",
        "horaFinTarde": "19:00:00"
      }
    ]
  },
  "bloqueosManuales": [
    {
      "idBloqueo": 1,
      "fechaHoraInicio": "2026-10-12T09:00:00",
      "fechaHoraFin": "2026-10-12T11:00:00",
      "motivo": "Reunión con cliente"
    }
  ],
  "actividades": [
    {
      "idActividad": 5,
      "nombre": "Diseño UI",
      "fechaHoraInicio": "2026-10-12T08:30:00",
      "fechaHoraFin": "2026-10-12T11:30:00"
    }
  ]
}
```

**Reglas de contenido:**

- `jornada.dias` viene **ordenado por `diaSemana`**; `bloqueosManuales` **ordenado por `fechaHoraInicio`**.
- Los días ausentes de `jornada.dias` **son días no laborables** (sáb/dom en el estado inicial).
- `jornada.margenActividadMinutos` = `agenda.margen_actividad_min` (`NOT NULL DEFAULT 30`, `chk_margen_positivo >= 0`): siempre un entero ≥ 0, nunca `null`.
- **`actividades[]` ya viene con el margen aplicado** (`CalendarioMapper.toBloqueoActividadResponse`): `fechaHoraInicio` está *restada* y `fechaHoraFin` *sumada* en `margenActividadMinutos`. El ejemplo de arriba usa margen 30 sobre una actividad 09:00–11:00. **No vuelvas a sumar/restar el margen en el front.**
- `actividades[]` solo incluye actividades de proyectos en estado **`PUBLICADO` o `CONFIRMADO`**, de membresías con `estado_participacion = 'ACTIVO'` (`ActividadRepository.findActividadesDeUsuario`, `CalendarioService.ESTADOS_ACTIVOS`).

---

### 3.2 `GET /calendario/perfil/{id}` — calendario visible desde un perfil

| Método | Ruta | Auth | Éxito |
| :--- | :--- | :--- | :--- |
| GET | `/calendario/perfil/{id}` | **Requiere sesión** (no anónimo) | `200 OK` |

Mismo `CalendarioResponse` que §3.1, **sin diferencias de contrato**: el cuerpo devuelto es idéntico al de la ruta privada.

| Campo | Valor en esta ruta | Evidencia |
| :--- | :--- | :--- |
| `bloqueosManuales[].motivo` | **siempre informado** (motivo público) | `CalendarioService.obtenerPublico` usa `CalendarioMapper.toBloqueoResponse`; test `CalendarioControllerTest.obtenerPorPerfil_devuelve200ConCuerpo` |

Todo lo demás (`idBloqueo`, fechas, `actividades[].nombre`, jornada) **se devuelve igual** que en la ruta privada. Ojo con esto al diseñar la UI pública: el nombre de la actividad del proyecto **no se oculta** y el motivo de los bloqueos manuales **sí se muestra** (visible para cualquier usuario autenticado con sesión).

`id` es el `idUsuario` dueño de la agenda, no un `idPerfil`. Las mismas precondiciones de §3.1 aplican sobre ese usuario (debe existir, estar `ACTIVO` y tener agenda).

---

### 3.3 `PUT /calendario/jornada` — configurar jornada laboral

| Método | Ruta | Auth | CSRF | Éxito |
| :--- | :--- | :--- | :--- | :--- |
| PUT | `/calendario/jornada` | Requiere sesión | **Sí** | `200 OK` |

**Semántica: reemplazo total.** Los días presentes en `dias` definen los días laborables; un día omitido pasa a ser no laborable; el margen se actualiza junto con ellos. La sincronización es por *diff* en backend (las filas sin cambios conservan su `id`).

**Body — `ConfigJornadaRequest`:**

```json
{
  "margenActividadMinutos": 30,
  "dias": [
    { "diaSemana": 1, "horaInicioManana": "09:00", "horaFinManana": null,
      "horaInicioTarde": null, "horaFinTarde": "18:00" },
    { "diaSemana": 3, "horaInicioManana": "09:00", "horaFinManana": "13:00",
      "horaInicioTarde": "15:00", "horaFinTarde": "19:00" }
  ]
}
```

| Campo | Tipo | Obligatorio | Reglas |
| :--- | :--- | :--- | :--- |
| `margenActividadMinutos` | number (int) | Sí | `@Min(0)`, no negativo. Buffer a cada lado de cada actividad. |
| `dias` | array | Sí, **no vacío** (`@NotEmpty`) | Máx. 7 elementos, `diaSemana` sin repetir. |
| `dias[].diaSemana` | number (int) | Sí | ISO 8601: `1` Lunes … `7` Domingo. |
| `dias[].horaInicioManana` | string hora | Sí | Inicio de la jornada. |
| `dias[].horaFinManana` | string hora o `null` | **Solo si es partida** | Fin del bloque de la mañana. |
| `dias[].horaInicioTarde` | string hora o `null` | **Solo si es partida** | Inicio del bloque de la tarde. |
| `dias[].horaFinTarde` | string hora | Sí | Fin de la jornada. |

**Formato de horas:** el backend **acepta** `"HH:mm"` y `"HH:mm:ss"` y **devuelve siempre** `"HH:mm:ss"` (p. ej. `"09:00:00"`). Normaliza antes de comparar.

**Corrido vs. partido** (detalle completo en `ficha-tecnica-jornada-partida.md` §3):

| Tipo | Representación |
| :--- | :--- |
| Corrido | `horaInicioManana` + `horaFinTarde`; el par del mediodía en `null` |
| Partido | Las 4 horas en orden estricto `inicioMañana < finMañana < inicioTarde < finTarde` |
| Solo mañana / solo tarde | Se modela como **corrido** con ese rango |

⚠️ **El par del mediodía va completo o no va.** Enviar `horaFinManana` con `horaInicioTarde: null` (o al revés) devuelve `400`. Este es el error más fácil de cometer al rellenar formularios.

**Respuesta `200` — `ConfigJornadaResponse`:** mismo objeto que el request (`{ margenActividadMinutos, dias[] }`) con horas en `"HH:mm:ss"`.

---

### 3.4 `POST /calendario/bloqueos` — marcar período como no disponible (UC-18)

| Método | Ruta | Auth | CSRF | Éxito |
| :--- | :--- | :--- | :--- | :--- |
| POST | `/calendario/bloqueos` | Requiere sesión | **Sí** | `200 OK` |

**Body — `MarcarNoDisponibleRequest`:**

```json
{
  "fechaHoraInicio": "2026-10-12T09:00:00",
  "fechaHoraFin": "2026-10-12T11:00:00",
  "motivo": "Capacitación interna"
}
```

| Campo | Tipo | Obligatorio | Reglas |
| :--- | :--- | :--- | :--- |
| `fechaHoraInicio` | string fecha-hora (ISO-8601 local, sin zona) | Sí (`@NotNull`) | — |
| `fechaHoraFin` | string fecha-hora | Sí (`@NotNull`) | Debe ser **estrictamente posterior** a `fechaHoraInicio`. |
| `motivo` | string | Sí (`@NotBlank`) | `1`–`200` caracteres (`@Size(max=200)`). Obligatorio: la columna `bloqueo_agenda.motivo` es `NOT NULL`. |

**Reglas de negocio (en este orden, `CalendarioService.marcarNoDisponible`):**

1. `fechaHoraFin > fechaHoraInicio` → si no, `400` `RangoInvalidoException`.
2. **No debe solapar** un bloqueo calculado por actividad (con margen aplicado) → si solapa, `409` `HorarioComprometidoException`.
3. **No debe solapar** otro bloqueo manual → si solapa, `400` `BloqueoSolapadoException`.

La detección de solape es estricta (`fin > inicio` y `finB > inicioA`), por lo que dos bloques que **se tocan** en un instante (el uno termina a las 11:00, el otro empieza a las 11:00) **sí están permitidos**.

**Respuesta `200` — `BloqueoResponse`:**

```json
{ "idBloqueo": 3, "fechaHoraInicio": "2026-10-12T09:00:00",
  "fechaHoraFin": "2026-10-12T11:00:00", "motivo": "Capacitación interna" }
```

---

### 3.5 `DELETE /calendario/bloqueos/{idBloqueo}` — liberar un bloqueo (UC-17)

| Método | Ruta | Auth | CSRF | Éxito |
| :--- | :--- | :--- | :--- | :--- |
| DELETE | `/calendario/bloqueos/{idBloqueo}` | Requiere sesión | **Sí** | **`204 No Content`** (cuerpo vacío) |

- El bloqueo debe **pertenecer a la agenda del usuario autenticado**; si no existe o es de otro usuario → `404`.
- Si el bloqueo **solapa una actividad** de proyecto activo → `409` (no se puede liberar un horario comprometido).

No devuelve body: el frontend debe retirar el bloqueo del estado local y re-fetchear o invalidar la caché.

---

## 4. Modelos de datos

### 4.1 `CalendarioResponse`
- `jornada` (`ConfigJornadaResponse`)
- `bloqueosManuales` (`List<BloqueoResponse>`)
- `actividades` (`List<BloqueoActividadResponse>`)

### 4.2 `ConfigJornadaResponse`
- `margenActividadMinutos` (Integer, ≥ 0, nunca `null`)
- `dias` (`List<JornadaDiaResponse>`, ordenado por `diaSemana`)

### 4.3 `ConfigJornadaRequest`
- `margenActividadMinutos` (Integer, `@NotNull`, `@Min(0)`)
- `dias` (`List<JornadaDiaRequest>`, `@NotEmpty`, `@Valid`)

### 4.4 `JornadaDiaRequest` / `JornadaDiaResponse`
- `diaSemana` (Integer, `@NotNull` en request, 1–7 ISO 8601)
- `horaInicioManana` (LocalTime, `@NotNull` en request)
- `horaFinManana` (LocalTime, nullable, solo jornada partida)
- `horaInicioTarde` (LocalTime, nullable, solo jornada partida)
- `horaFinTarde` (LocalTime, `@NotNull` en request)

### 4.5 `BloqueoResponse`
- `idBloqueo` (Long)
- `fechaHoraInicio` (LocalDateTime)
- `fechaHoraFin` (LocalDateTime)
- `motivo` (String, máx. 200; **siempre informado, también en `GET /calendario/perfil/{id}`**)

### 4.6 `BloqueoActividadResponse`
- `idActividad` (Long)
- `nombre` (String)
- `fechaHoraInicio` / `fechaHoraFin` (LocalDateTime, **ya extendidos por `margenActividadMinutos` a cada lado**)
- *Nota:* bloques derivados, no persistidos; se recalculan en cada `GET`.

---

## 5. Errores

### 5.1 Formato de los cuerpos de error

**Error de negocio** (`GlobalExceptionHandler.buildErrorResponse`):

```json
{ "message": "<mensaje literal>", "httpStatus": 400, "timestamp": 1788000000000 }
```

**Error de validación de campos** (`MethodArgumentNotValidException`):

```json
{
  "message": "Validación fallida",
  "errores": { "fechaHoraFin": "must not be null" },
  "httpStatus": 400,
  "timestamp": 1788000000000
}
```

> Los `403` de sesión/CSRF los emite el filtro de Spring Security, **no** `GlobalExceptionHandler`: no siguen este shape (llegan con `status`/`error`/`path` del body de error por defecto de Spring Boot). Trátalos como errores genéricos: `403` por sesión → volver a login; `403` por CSRF → reenviar el header `X-XSRF-TOKEN`.

### 5.2 Matriz de códigos del módulo

| Código | Causa | `message` (mostrar tal cual) | Endpoints |
| :---: | :--- | :--- | :--- |
| `200` | Operación exitosa | — | GET, PUT, POST |
| `204` | Bloqueo liberado (cuerpo vacío) | — | DELETE |
| `400` | Bean Validation (ver §5.3) | `Validación fallida` + `errores` | PUT, POST |
| `400` | `fin <= inicio` del bloqueo | `La fecha y hora de fin debe ser posterior a la de inicio.` | POST |
| `400` | Bloqueo manual solapado | `El periodo se superpone con un bloqueo existente de tu calendario.` | POST |
| `400` | Reglas de jornada (7 mensajes, ver §5.4) | ver §5.4 | PUT |
| `401` | Usuario inexistente o estado ≠ `ACTIVO` (incluye `PENDIENTE_BAJA`) | `Usuario no encontrado.` | Todos |
| `403` | Sin sesión / JWT inválido o ausente | *Body por defecto de Spring Boot (sin `message`)* | Todos |
| `403` | Sin token CSRF en `POST`/`PUT`/`DELETE` | *Body por defecto de Spring Boot (sin `message`)* | PUT, POST, DELETE |
| `404` | No existe la agenda del usuario | `No se encontró la agenda del usuario.` | Todos |
| `404` | Bloqueo inexistente o ajeno a la agenda | `El bloqueo no existe o no pertenece a tu calendario.` | DELETE |
| `409` | Horario comprometido con una actividad | `El periodo ya se encuentra bloqueado automáticamente por una actividad de un proyecto activo.` | POST |
| `409` | Intento de liberar horario comprometido | `No se puede marcar como disponible un horario comprometido con una actividad de un proyecto activo.` | DELETE |

> **Nota sobre `401` vs `403`:** el `401` del módulo viene de `UsuarioNoEncontradoException` (usuario `PENDIENTE_BAJA` pasa el filtro JWT porque `permiteAcceso()` lo admite, pero `CalendarioService.usuarioActivo` exige `ACTIVO`). La **ausencia** de sesión produce `403`, no `401`: `SecurityConfig` no declara `authenticationEntryPoint`, y el default de `ExceptionHandlingConfigurer.createDefaultEntryPoint` es `Http403ForbiddenEntryPoint`.

### 5.3 Mensajes de Bean Validation por campo

**`PUT /calendario/jornada`** (`ConfigJornadaRequest` / `JornadaDiaRequest`):

| Clave en `errores` | `message` |
| :--- | :--- |
| `margenActividadMinutos` | `El margen por actividad es obligatorio.` |
| `margenActividadMinutos` | `El margen por actividad no puede ser negativo.` |
| `dias` | `must not be empty` |
| `dias[i].diaSemana` | `must not be null` |
| `dias[i].horaInicioManana` | `must not be null` |
| `dias[i].horaFinTarde` | `must not be null` |

**`POST /calendario/bloqueos`** (`MarcarNoDisponibleRequest`):

| Clave en `errores` | `message` |
| :--- | :--- |
| `fechaHoraInicio` | `must not be null` |
| `fechaHoraFin` | `must not be null` |
| `motivo` (nulo o en blanco) | `El motivo del bloqueo es obligatorio.` |
| `motivo` (> 200) | `El motivo no puede superar los 200 caracteres.` |

### 5.4 Los 7 mensajes de `JornadaInvalidaException` (`400`)

Texto literal, tomado de `CalendarioService.validarJornada` / `validarHorarios`:

1. `El día de la semana debe estar entre 1 (Lunes) y 7 (Domingo).`
2. `El horario de fin debe ser posterior al horario de inicio.`
3. `Para una jornada partida se deben informar el fin del bloque de la mañana y el inicio del bloque de la tarde; para una jornada de corrido, ninguno de los dos.`
4. `El fin del bloque de la mañana debe ser posterior a su inicio.`
5. `El bloque de la tarde debe comenzar después del fin del bloque de la mañana.`
6. `El fin del bloque de la tarde debe ser posterior a su inicio.`
7. `No se puede repetir el mismo día de la semana en la jornada.`

---

## 6. Consideraciones de UI/UX

### 6.1 Visualización del calendario
- **Vista mes/semana** estilo Google Calendar; superponer las tres capas de §1 con colores diferenciados.
- **Bloques de actividad**: solo lectura, semitransparentes, etiquetados con `nombre`. Pintar el rango **ya tal cual viene** del backend (el margen está aplicado).
- **Jornada laboral**: resaltar `horaInicioManana`/`horaFinTarde` de cada día; los días ausentes se pintan como no laborables.
- **Conflictos**: el backend los rechaza al guardar; en lectura conviene resaltar solapes entre bloques manuales y actividades para explicar el `409`.

### 6.2 Configuración de jornada (`ConfigJornadaForm`)
- Selector de día `1–7` (o Lunes–Domingo) y time pickers para los 4 horarios.
- **Toggle "¿Jornada partida?" por día.** Apagado → 2 inputs y enviar `horaFinManana`/`horaInicioTarde` en `null`. Encendido → 4 inputs.
- Tras un `GET`, inferir el estado del toggle con `horaFinManana != null && horaInicioTarde != null`.
- No re-implementar las reglas de §5.4 en el front; espejarlas solo para mejor UX y mostrar el `message` del `400` si llega.
- Input numérico para margen (minutos, mínimo 0).

### 6.3 Bloqueos manuales (`BloqueoModal`)
- Date-time pickers de inicio/fin + input de motivo (obligatorio, máx. 200).
- Validación client-side de `fin > inicio` y de no solapar con lo ya pintado (el backend también lo valida y responde `400`/`409`).
- Listado de bloqueos con botón **"Disponibilizar"** → `DELETE` → retirar del estado local (respuesta `204` sin body).
- Mostrar el `message` literal de `409`/`400` tal cual, sin traducir.

### 6.4 Rutas sugeridas

```
/calendario               → calendario del usuario autenticado (vista principal)
/calendario/perfil/{id}   → calendario visible desde el perfil de otro usuario (solo lectura)
```

---

## 7. Flujos de trabajo comunes

### Flujo 1 — Configurar jornada laboral
1. Usuario entra a `/calendario` → `GET /calendario`.
2. Abre `ConfigJornadaForm`; por defecto trae L–V corrido 09:00–18:00.
3. Elige tipo por día (corrida/partida) y define horarios.
4. Ajusta el margen por actividad.
5. `PUT /calendario/jornada` (con `X-XSRF-TOKEN`).
6. `200` → refrescar la vista con la respuesta (ya trae `HH:mm:ss`).
7. `400` → mostrar `message` o el mapa `errores`.

### Flujo 2 — Agregar bloqueo personal
1. Clic en "Bloquear tiempo" sobre un hueco de la vista.
2. Modal con inicio/fin/motivo → `POST /calendario/bloqueos`.
3. `200` → pintar el bloque con la respuesta (`idBloqueo` real).
4. `409` → "ese horario ya está comprometido por una actividad"; `400` → solape con otro bloqueo o `fin <= inicio`.

### Flujo 3 — Gestionar actividades de proyecto
1. El sistema pinta `actividades[]` automáticamente (solo proyectos `PUBLICADO`/`CONFIRMADO`).
2. Los bloques son de solo lectura: no hay endpoint para editarlos.
3. Si el usuario intenta bloquear encima → recibirá `409` y debe elegir otro rango.

### Flujo 4 — Ver calendario desde un perfil ajeno
1. Navegar a `/calendario/perfil/{id}` con la sesión propia (no es una ruta anónima).
2. Pintar bloqueos manuales **con su motivo** (siempre viene informado) y actividades con su nombre.

---

## 8. Estado y caching

- **React Query** (o equivalente) para `GET /calendario` con caché automática.
- Invalidar tras: `PUT /calendario/jornada`, `POST /calendario/bloqueos`, `DELETE /calendario/bloqueos/{id}`.
- No cachear `GET /calendario/perfil/{id}` de forma indefinida: depende de la agenda ajena.
- Estado local para el formulario/modal abierto; `localStorage` para la preferencia de vista (mes vs. semana).
- `DELETE` no devuelve cuerpo: la invalidación de caché es obligatoria, no opcional.

---

## 9. Referencias de implementación Backend

| Aspecto | Archivo |
| :--- | :--- |
| Endpoint y manejo de `Authentication` | `calendario/controlador/CalendarioController.java` |
| Reglas de negocio, validaciones y solapes | `calendario/servicio/CalendarioService.java` (`obtener`, `obtenerPublico`, `configurarJornada`, `marcarNoDisponible`, `marcarDisponible`, `validarJornada`, `validarHorarios`, `solapaActividad`, `usuarioActivo`) |
| DTOs de jornada | `calendario/dto/ConfigJornadaRequest.java`, `ConfigJornadaResponse.java`, `JornadaDiaRequest.java`, `JornadaDiaResponse.java` |
| DTOs de bloqueos | `calendario/dto/CalendarioResponse.java`, `BloqueoResponse.java`, `BloqueoActividadResponse.java`, `MarcarNoDisponibleRequest.java` |
| Mapeo de bloqueos | `calendario/mapper/CalendarioMapper.java` (`toBloqueoResponse`) |
| Excepciones del módulo | `calendario/exception/JornadaInvalidaException.java`, `RangoInvalidoException.java`, `BloqueoSolapadoException.java`, `BloqueoNoEncontradoException.java`, `HorarioComprometidoException.java`, `AgendaNoEncontradaException.java` |
| Formato de error (400/401/404/409) | `common/exception/GlobalExceptionHandler.java` |
| Sesión, CSRF y rutas públicas | `security/SecurityConfig.java`, `security/CsrfCookieFilter.java`, `security/JwtAuthenticationFilter.java` |
| Actividades que alimentan `actividades[]` | `repositorio/ActividadRepository.java` (`findActividadesDeUsuario`) |
| Checks `chk_jornada_*`, margen y agenda inicial | `docs/3_diseño/ModaLinkBD.sql` (tablas `jornada_agenda`, `agenda`; trigger `fn_crear_agenda`) |
| Tests de contrato | `src/test/java/org/mgroko/backend/calendario/**` |
