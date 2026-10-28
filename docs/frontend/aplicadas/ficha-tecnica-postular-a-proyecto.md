# Ficha Técnica Frontend: Dar de Alta Postulación a Proyecto (UC-26)

Especificación de la API y de la UI para que un perfil activo se postule a un
**requerimiento general** de proyecto o a un **requerimiento de actividad**.
Incluye cómo obtener los datos necesarios, contratos de request/response,
reglas de negocio, disponibilidad provisional, errores y recomendaciones de
integración para la vista de detalle del proyecto (`/proyectos/:id`).

> **Alcance:** UC-26 da de alta la postulación en estado `PENDIENTE` y notifica
> a los directores. La gestión de postulaciones está implementada en UC-34 y el
> listado del proyecto en UC-74:
> [`ficha-tecnica-gestionar-postulaciones.md`](./ficha-tecnica-gestionar-postulaciones.md)
> y [`ficha-tecnica-buscar-postulaciones.md`](./ficha-tecnica-buscar-postulaciones.md).
> Sigue sin existir un endpoint para que el **postulante** consulte el estado de
> sus propias postulaciones.

---

## 1. Resumen rápido

| Aspecto | Valor |
| :--- | :--- |
| Métodos | `POST` (dos endpoints, uno por tipo de requerimiento) |
| Autenticación | Cookie `jwt` + header `X-XSRF-TOKEN` |
| Perfil activo | Requerido; lo resuelve el backend desde la sesión |
| Body | Sólo el ID del requerimiento |
| Éxito | `201 Created` + `PostulacionResponse` |
| Estado inicial | `PENDIENTE` |
| Errores esperables | `400`, `404`, `409`, `401` |

---

## 2. Endpoints y Contexto de Autenticación

* **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header
  `X-XSRF-TOKEN`).
* **Perfil activo requerido:** Sí. El backend toma `idPerfilActivo` del contexto
  de autenticación; el frontend **no** envía `idPerfil` ni `idUsuario`.
* **Content-Type:** `application/json`.
* **Respuesta exitosa:** `201 Created` con `PostulacionResponse`. No se devuelve
  header `Location`.

### 2.1 Postulación a requerimiento general

* **Método:** `POST`
* **URL:** `/proyectos/{idProyecto}/postulaciones/general`
* **Path param:** `idProyecto` (`long`, obligatorio)
* **Body:**

```json
{
  "idRequerimientoGral": 30
}
```

### 2.2 Postulación a requerimiento de actividad

* **Método:** `POST`
* **URL:** `/proyectos/{idProyecto}/postulaciones/actividad`
* **Path param:** `idProyecto` (`long`, obligatorio)
* **Body:**

```json
{
  "idRequerimientoActividad": 20
}
```

> El `idProyecto` del path debe coincidir con el proyecto al que pertenece el
> requerimiento. Si no coincide, el backend responde `404` (no `409`).

---

## 3. Datos que el frontend necesita antes de postularse

### 3.1 Detalle del proyecto

`GET /proyectos/{idProyecto}` devuelve el `ProyectoResponse`. Campos relevantes
para UC-26:

| Campo | Tipo | Uso en el frontend |
| :--- | :--- | :--- |
| `estado` | `string` | Valores con formato de presentación: `"Borrador"`, `"Publicado"`, `"Confirmado"`, `"Finalizado"`, `"Cancelado"`. `"Publicado"` y `"Confirmado"` aceptan postulaciones si el requerimiento tiene faltantes; el resto no. |
| `aceptaPostulacionGral` | `boolean` | Si es `false`, no mostrar postulación a requerimientos generales. |
| `esMiembro` | `boolean` | Para postulación **general**, si es `true` no se muestra la acción. En **actividades**, un miembro activo `MIEMBRO` sí puede postularse; el `DIRECTOR` no. |
| `elegibleParaPostularse` | `boolean` | **Resumen global histórico, deprecado para el gating.** Es `true` si el proyecto está `Publicado`/`Confirmado`, el perfil no es miembro activo, acepta postulaciones generales y la profesión coincide con **algún** requerimiento. No evalúa cupo ni sirve para decidir por requerimiento. Usar `requerimientosGral[].puedePostularse`. |
| `rolUsuario` | `string` \| `null` | `null` para no miembros. |
| `requerimientosGral` | `array` | Lista de requerimientos generales (ver 3.2). |

> **Fuente de verdad por requerimiento:** `requerimiento.puedePostularse`.
> `elegibleParaPostularse` queda sólo como resumen/compatibilidad durante el
> rollout. No usarlo para habilitar el botón de un requerimiento concreto.
>
> `elegibleParaPostularse` **no contempla requerimientos de actividad**. Para
> actividades, el backend expone `puedePostularse` en cada requerimiento de
> `/actividades/buscar` (ver 3.3).

### 3.2 Requerimientos generales

Cada elemento de `requerimientosGral`:

```json
{
  "idRequerimientoGral": 30,
  "cantidad": 2,
  "descripcion": "Vacante para desfile.",
  "idProfesion": 2,
  "nombreProfesion": "Modelo",
  "caracteristicas": [],
  "habilidades": [],
  "cubiertos": 0,
  "faltantes": 2,
  "satisfecho": false,
  "puedePostularse": true
}
```

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `idRequerimientoGral` | `long` | Es el valor que se envía en el body. |
| `cantidad` | `int` | Cupo solicitado. |
| `cubiertos` | `int` | Miembros/asignaciones **activas** vinculadas al requerimiento. El front no lo deriva. |
| `faltantes` | `int` | `max(0, cantidad - cubiertos)`. |
| `satisfecho` | `boolean` | **Cupo completo** (`cubiertos >= cantidad`). Con `cantidad > 1` y `cubiertos = 1`, `satisfecho = false` y el botón debe seguir visible. |
| `puedePostularse` | `boolean` | **Fuente de verdad para este requerimiento.** El backend ya evaluó estado del proyecto, membresía, `aceptaPostulacionGral`, profesión y cupo. El front sólo debe hacer `puedePostularse === true`. No recalcular la regla. |

### 3.3 Actividades y sus requerimientos

`GET /proyectos/{idProyecto}/actividades/buscar` devuelve
`PaginaResponse<ActividadBusquedaResponse>`.

Parámetros útiles:

| Parámetro | Valor sugerido | Descripción |
| :--- | :--- | :--- |
| `estado` | `PENDIENTE` | Repetible (`estado=PENDIENTE&estado=...`) para filtrar por estado. |
| `todos` | `true` | Devuelve todas las actividades sin paginar. |
| `page`, `size` | — | Se ignoran si `todos=true`. |

Ejemplo:

```
GET /proyectos/105/actividades/buscar?estado=PENDIENTE&todos=true
```

Cada actividad:

```json
{
  "idActividad": 200,
  "idProyecto": 105,
  "nombreProyecto": "Colección Madoka",
  "nombre": "Prueba de vestuario",
  "descripcion": "Prueba con las modelos seleccionadas.",
  "duracionMinutos": 120,
  "fechaHoraInicio": "2026-11-02T09:00:00",
  "fechaHoraFin": "2026-11-02T11:00:00",
  "estado": "PENDIENTE",
  "requerimientos": [
    {
      "idRequerimiento": 20,
      "cantidad": 1,
      "descripcion": "Una modelo",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "cubiertos": 0,
      "faltantes": 1,
      "satisfecho": false,
      "puedePostularse": true
    }
  ],
  "predecesoras": []
}
```

| Campo | Notas |
| :--- | :--- |
| `estado` | Enum crudo en mayúsculas: `PENDIENTE`, `EN_CURSO`, `FINALIZADA`, `CANCELADA`. **Sólo `PENDIENTE` admite postulaciones.** |
| `requerimientos[].idRequerimiento` | Es el valor que se envía como `idRequerimientoActividad`. |
| `requerimientos[].cubiertos` | Asignaciones activas vinculadas al requerimiento. |
| `requerimientos[].faltantes` | `max(0, cantidad - cubiertos)`. |
| `requerimientos[].satisfecho` | `true` si el cupo está completo. |
| `requerimientos[].puedePostularse` | **Fuente de verdad para este requerimiento de actividad.** Ya incluye proyecto `Publicado`/`Confirmado`, actividad `PENDIENTE`, rol (`DIRECTOR` excluido, `MIEMBRO` permitido), profesión y cupo. El front sólo debe hacer `puedePostularse === true`. |
| `duracionMinutos` | Duración que el backend usa para verificar disponibilidad. |

> `puedePostularse` **no incluye** que el perfil ya se haya postulado ni la
> disponibilidad de calendario. Esas siguen siendo validaciones del backend y
> pueden responder `409`; la UI debe tratarlas como red de seguridad y no
> recalcular la elegibilidad.

> No confundir el formato de `estado` del proyecto (`"Publicado"`) con el de la
> actividad (`"PENDIENTE"`) ni con el de la postulación (`"PENDIENTE"`). El
> proyecto usa nombres de presentación; los otros dos, el enum crudo.

---

## 4. Reglas de Negocio y Precondiciones

### 4.1 Estado del proyecto

| Estado | ¿Acepta postulaciones? | Resultado |
| :--- | :---: | :--- |
| `Publicado` | **Sí** | `201 Created` si el requerimiento está insatisfecho |
| `Borrador` | No | `409 Conflict` |
| `Confirmado` | **Sí, si el requerimiento está insatisfecho** | `201 Created`; permite reforzar el equipo tras una baja |
| `Finalizado` | No | `409 Conflict` |
| `Cancelado` | No | `409 Conflict` |

> El backend compara el estado real, no el texto mostrado. El frontend debe
> habilitar postulaciones en `"Publicado"` y `"Confirmado"` siempre que el
> requerimiento tenga `faltantes > 0`.

### 4.2 Reglas comunes a ambos flujos

1. El perfil activo debe existir, pertenecer al usuario autenticado y estar
   `ACTIVO`.
2. **Requerimiento general:** el perfil **no** debe ser miembro activo del
   proyecto. **Actividad:** un miembro activo `MIEMBRO` **sí** puede postularse;
   un `DIRECTOR` no puede postularse nunca.
3. La profesión del perfil debe coincidir con la del requerimiento.
4. El requerimiento no debe estar satisfecho. El cupo se mide por miembros o
   asignaciones **activos** vinculados al requerimiento, no por postulaciones
   aceptadas. Usar `satisfecho`/`faltantes`.
5. El perfil no debe haberse postulado antes a ese requerimiento, **sin importar
   el estado de la postulación anterior** (regla garantizada por índice único en
   la base). Un segundo intento responde `409`.
6. El proyecto debe estar `Publicado` o `Confirmado`.

### 4.3 Reglas específicas

| Flujo | Regla adicional |
| :--- | :--- |
| General | El proyecto debe tener `aceptaPostulacionGral = true`. |
| General | Un miembro activo del proyecto no puede postularse. |
| Actividad | La actividad debe estar `PENDIENTE`. |
| Actividad | Un `MIEMBRO` activo puede postularse si la profesión coincide. |
| Actividad | Un `DIRECTOR` no puede postularse. |
| Actividad | Se verifica disponibilidad del perfil en el rango factible de la actividad (ver sección 5). |

### 4.4 Matriz de decisión para habilitar la acción

**Regla vigente (Fase 3):** usar `requerimiento.puedePostularse`.

| Caso | Condición frontend |
| :--- | :--- |
| Postular a requerimiento general | `requerimiento.puedePostularse === true`. |
| Postular a actividad | `requerimiento.puedePostularse === true`. |
| No mostrar la acción | `requerimiento.puedePostularse !== true`, o el requerimiento ya fue postulado en esta sesión. |
| Fallback durante el rollout | Si `puedePostularse` no viene (backend anterior), usar la regla histórica: general `proyecto.elegibleParaPostularse === true && requerimiento.faltantes > 0`; actividad `(proyecto.estado === 'Publicado' \|\| 'Confirmado') && actividad.estado === 'PENDIENTE' && requerimiento.faltantes > 0 && (!proyecto.esMiembro \|\| proyecto.rolUsuario === 'MIEMBRO')`. |

> `puedePostularse` no reemplaza el manejo de `409`: el perfil puede ya haberse
> postulado, haber cambiado el cupo o no tener disponibilidad. El backend sigue
> siendo la red de seguridad; el front no debe derivar esas reglas.
>
> `elegibleParaPostularse` queda **deprecado** para el gating. Si se usa, que sea
> sólo como resumen visual o fallback de un backend viejo.

---

## 5. Disponibilidad y Campo `tentativa`

El backend verifica la disponibilidad **en el momento de crear la postulación**.
No existe un endpoint separado de pre-verificación.

Regla aplicada:

* Debe existir un hueco libre **continuo** de al menos `duracionMinutos` dentro
  del rango factible de la actividad.
* Se descuentan la jornada laboral, los bloqueos manuales del calendario y las
  actividades ya asignadas (con su margen).
* Si el perfil no tiene jornada configurada, no hay disponibilidad (caso
  defensivo; todo perfil se crea con jornada por defecto).

### 5.1 Disponibilidad provisional

| Situación del proyecto | Rango evaluado | `tentativa` |
| :--- | :--- | :---: |
| Sin fecha de inicio confirmada | `[ahora, fecha de entrega]` | `true` |
| Con fecha de inicio confirmada | `[ES, LF]` del CPM | `false` |

Cuando `tentativa` es `true`, la verificación es **provisional**: el backend
volverá a verificar al confirmar el proyecto (UC-60) y al asignar la actividad
(UC-70). No es un error ni requiere acción del frontend; sólo puede mostrarse un
aviso informativo:

> *"Tu disponibilidad fue verificada de forma provisoria. Se confirmará cuando
> el proyecto defina su fecha de inicio."*

---

## 6. Respuesta Exitosa (`201 Created`)

### 6.1 Contrato `PostulacionResponse`

```json
{
  "idPostulacion": 77,
  "tipo": "GENERAL",
  "estado": "PENDIENTE",
  "fechaPostulacion": "2026-10-26T10:15:00",
  "idProyecto": 105,
  "idRequerimiento": 30,
  "tentativa": false
}
```

| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `idPostulacion` | `long` | ID de la fila creada. En postulación general es `id_postulacion_gral`; en actividad, `id_postulacion`. |
| `tipo` | `string` | `GENERAL` o `ACTIVIDAD`. |
| `estado` | `string` | Siempre `PENDIENTE` al crear. |
| `fechaPostulacion` | `string` (ISO-8601) | Fecha/hora del servidor. |
| `idProyecto` | `long` | Proyecto del requerimiento. |
| `idRequerimiento` | `long` | `idRequerimientoGral` o `idRequerimientoActividad` según `tipo`. |
| `tentativa` | `boolean` | `true` si la disponibilidad se evaluó de forma provisional. Siempre `false` en postulaciones generales. |

### 6.2 Ejemplo de postulación a actividad

```json
{
  "idPostulacion": 88,
  "tipo": "ACTIVIDAD",
  "estado": "PENDIENTE",
  "fechaPostulacion": "2026-10-26T10:20:00",
  "idProyecto": 105,
  "idRequerimiento": 20,
  "tentativa": true
}
```

---

## 7. Manejo de Errores

### 7.1 Formato general

```json
{
  "message": "Ya existe una postulación de este perfil a este requerimiento.",
  "httpStatus": 409,
  "timestamp": 1761473700000
}
```

### 7.2 Error de validación (`400`)

```json
{
  "message": "Validación fallida",
  "errores": {
    "idRequerimientoActividad": "El requerimiento de actividad es obligatorio."
  },
  "httpStatus": 400,
  "timestamp": 1761473700000
}
```

### 7.3 Tabla de errores

| HTTP | Caso | Mensaje / origen | Acción frontend |
| :---: | :--- | :--- | :--- |
| `400` | Falta el ID del requerimiento en el body | `El requerimiento de actividad es obligatorio.` / `El requerimiento general es obligatorio.` | Error de integración: no debería ocurrir si el botón está asociado a un requerimiento. Loguear. |
| `401` | Sesión ausente o expirada | Seguridad | Redirigir a login (interceptor `http.js`). |
| `404` | Sin perfil activo | `No hay un perfil activo seleccionado en la sesión.` | Abrir el flujo de selección de perfil. |
| `404` | Perfil no encontrado o de otro usuario | `El perfil no fue encontrado` | Aviso y volver a seleccionar perfil. |
| `404` | Requerimiento inexistente | `El requerimiento general {id} no existe.` / `El requerimiento de actividad {id} no existe.` | Refrescar el proyecto/actividades; el requerimiento pudo eliminarse. |
| `404` | Requerimiento de otro proyecto | `El requerimiento no pertenece al proyecto indicado.` | Bug de integración: verificar que se envía el `idProyecto` correcto. |
| `409` | Proyecto en estado no habilitado | `El proyecto ya no acepta postulaciones.` | Refrescar el proyecto y ocultar la acción. |
| `409` | Proyecto sin postulaciones generales | `El proyecto no acepta postulaciones a requerimientos generales.` | Refrescar el proyecto. |
| `409` | Perfil ya es miembro activo en postulación general | `Ya sos miembro activo de este proyecto.` | Refrescar el proyecto (cambió `esMiembro`). |
| `409` | Director intenta postularse a actividad | `Un director no puede postularse a una actividad.` | Ocultar la acción para directores. |
| `409` | Profesión distinta | `Tu perfil no tiene la profesión solicitada por el requerimiento.` | Mostrar el mensaje; no ofrecer reintento. |
| `409` | Cupo general completo | `El requerimiento general ya está satisfecho.` | Refrescar el proyecto. |
| `409` | Cupo de actividad completo | `El requerimiento de actividad ya está satisfecho.` | Refrescar las actividades. |
| `409` | Postulación duplicada | `Ya existe una postulación de este perfil a este requerimiento.` | Deshabilitar la acción para ese requerimiento en la sesión actual. |
| `409` | Actividad no pendiente | `Sólo se puede postular a actividades pendientes.` | Refrescar las actividades. |
| `409` | Sin disponibilidad | `No tenés disponibilidad suficiente dentro del rango factible de la actividad.` | Mostrar el mensaje y ofrecer revisar el calendario. |
| `409` | Perfil inactivo | `El perfil no se encuentra activo.` | Aviso de perfil inactivo. |
| `≥ 500` | Error inesperado | — | Mensaje genérico y opción de reintentar. |

> Un `409` **no siempre significa que la acción esté mal**: puede indicar que el
> estado cambió (por ejemplo, el proyecto se confirmó o canceló). En todos los
> casos conviene refrescar el proyecto y/o las actividades y volver a evaluar los
> botones.

---

## 8. Consideraciones de UI/UX

### 8.1 Dónde mostrar la acción

En el detalle del proyecto (`/proyectos/:id`), en la sección de requerimientos:

* **Requerimientos generales:** un botón **Postularme** por requerimiento, visible
  sólo si `requerimiento.puedePostularse === true`.
* **Actividades:** un botón **Postularme** por cada requerimiento de actividad,
  visible sólo si `requerimiento.puedePostularse === true`.

No mostrar ninguna acción de postulación cuando `puedePostularse !== true`. El
backend ya excluye directores, proyectos no habilitados, actividades no
`PENDIENTE`, profesiones distintas y requerimientos con cupo completo. Si el
backend es anterior a Fase 3, usar la matriz de fallback de §4.4.

### 8.2 Flujo recomendado

1. El usuario elige **Postularme**.
2. Mostrar un modal de confirmación con:
   * nombre del proyecto,
   * nombre/descripción del requerimiento,
   * profesión solicitada,
   * si es actividad: nombre de la actividad y duración.
3. Botón **Confirmar postulación** con estado `loading`.
4. **Deshabilitar el botón mientras la petición está en curso** para evitar
   dobles envíos (el backend igualmente protege con el índice único).
5. En éxito, cerrar el modal, mostrar toast y deshabilitar la acción para ese
   requerimiento durante la sesión.

### 8.3 Después de un `201`

* Mostrar un toast de éxito:
  * General: *"Postulación enviada. El director del proyecto recibirá tu
    solicitud."*
  * Actividad: *"Postulación enviada para la actividad '{nombre}'."*
* Si `tentativa === true`, agregar el aviso de disponibilidad provisoria
  (sección 5.1).
* No es necesario recargar el proyecto: la respuesta no cambia campos visibles.
  Si la vista muestra contadores propios, refrescar `GET /proyectos/{id}` y/o el
  listado de actividades.
* Si el usuario es director, puede ir al apartado de postulaciones del proyecto
  (UC-74). No existe todavía un listado de “mis postulaciones” para el
  postulante.

> Limitación conocida: como no existe un endpoint para que el postulante consulte
> sus propias postulaciones, al recargar la página el botón puede volver a
> mostrarse. Un segundo intento responde `409` con el mensaje de duplicado;
> conviene tratarlo como estado esperado y deshabilitar el botón localmente.

### 8.4 Después de un `409`

1. Mostrar el `message` del backend tal cual.
2. Refrescar el proyecto (`GET /proyectos/{id}`) y, si aplica, las actividades.
3. Reescribir el estado de los botones según las reglas de la sección 4.4.
4. No cerrar el modal automáticamente si el error es de disponibilidad
   (`No tenés disponibilidad...`), para que el usuario pueda leerlo.

### 8.5 Accesibilidad y textos

* Usar `aria-busy` en el botón mientras carga.
* No depender sólo del color para indicar que un requerimiento está satisfecho;
  acompañar con texto o ícono.
* Evitar el término "solicitud" y "postulación" mezclados: en la UI usar
  **postulación** de forma consistente.

---

## 9. Integración Sugerida

### 9.1 Servicio

Agregar en `src/services/proyectoService.js`:

```js
postularARequerimientoGeneral(idProyecto, idRequerimientoGral) {
  return http.post(`/proyectos/${idProyecto}/postulaciones/general`, {
    idRequerimientoGral,
  })
},

postularARequerimientoActividad(idProyecto, idRequerimientoActividad) {
  return http.post(`/proyectos/${idProyecto}/postulaciones/actividad`, {
    idRequerimientoActividad,
  })
},
```

### 9.2 Tipos/constantes

```ts
type TipoPostulacion = 'GENERAL' | 'ACTIVIDAD'
type EstadoPostulacion = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA'

interface PostulacionResponse {
  idPostulacion: number
  tipo: TipoPostulacion
  estado: EstadoPostulacion
  fechaPostulacion: string
  idProyecto: number
  idRequerimiento: number
  tentativa: boolean
}
```

Utilidades existentes a reutilizar:

* `normalizarEstadoProyecto()` para comparar `"Publicado"`.
* `EstadoProyectoBadge.vue` para el badge del proyecto.
* `useToast()` (vue-toastification) para éxito/error.
* El patrón de `AbortController` del proyecto si ya se usa en la vista.

### 9.3 No enviar

* `idPerfil` ni `idPerfilActivo`
* `idUsuario`
* `estado`
* `tipo`
* `idProyecto` en el body (va en la URL)
* `tentativa`

---

## 10. Checklist de Aceptación

- [ ] El botón de postulación general sólo aparece con
      `requerimiento.puedePostularse === true`.
- [ ] El botón de postulación a actividad sólo aparece con
      `requerimiento.puedePostularse === true`.
- [ ] Un requerimiento con `puedePostularse = false` no muestra la acción,
      aunque el proyecto sea `Publicado`/`Confirmado` o `elegibleParaPostularse`
      sea `true`.
- [ ] Si el backend no envía `puedePostularse` (rollout), la UI usa el fallback
      de §4.4 y no se rompe.
- [ ] El botón se oculta con cupo completo (`satisfecho === true` o
      `faltantes === 0`) y se muestra el chip "Cupo completo" cuando corresponda.
- [ ] Si `puedePostularse = false` por un motivo distinto al cupo, no se muestra
      “Cupo completo” de forma engañosa.
- [ ] Tras un `409` de cupo completo, la acción queda oculta en la sesión de la
      vista aunque la respuesta no traiga `satisfecho`.
- [ ] El request general usa exactamente `{ "idRequerimientoGral": <id> }`.
- [ ] El request de actividad usa exactamente
      `{ "idRequerimientoActividad": <id> }`.
- [ ] No se envía `idPerfil` ni `idUsuario`.
- [ ] Un `201` muestra toast de éxito y deshabilita la acción para ese
      requerimiento.
- [ ] Si `tentativa === true`, se muestra el aviso de verificación provisoria.
- [ ] Un `409` muestra el mensaje del backend y refresca el proyecto/actividades.
- [ ] Un `404` de requerimiento refresca la vista y no se reintenta a ciegas.
- [ ] El botón se deshabilita mientras la petición está en curso.
- [ ] La vista contempla que `estado` del proyecto viene capitalizado
      (`"Publicado"`) y el de actividades/postulaciones en mayúsculas
      (`"PENDIENTE"`).
- [x] Un director puede navegar al apartado de postulaciones del proyecto
      (UC-74) desde el dashboard.
- [ ] No se consulta `/notificaciones`: el backend aún no expone lectura de
      notificaciones.

---

## 11. Notas y Pendientes

* **UC-34 (Gestionar postulaciones):** implementado. Ver
  [`ficha-tecnica-gestionar-postulaciones.md`](./ficha-tecnica-gestionar-postulaciones.md).
* **UC-74 (Buscar postulaciones):** implementado para el director del proyecto.
  Ver [`ficha-tecnica-buscar-postulaciones.md`](./ficha-tecnica-buscar-postulaciones.md).
* **UC-60 (Confirmar proyecto):** al confirmar, el proyecto pasa a `Confirmado`.
  Puede seguir aceptando postulaciones si un requerimiento queda insatisfecho
  por una baja o eliminación; además debe re-verificar la disponibilidad
  provisional de las postulaciones de actividad.
* **UC-70 (Asignar actividad):** vuelve a verificar disponibilidad al asignar.
* **Notificaciones:** UC-26 registra `POSTULACION_RECIBIDA` para todos los
  directores activos.
* **Cupo:** `satisfecho` significa **cupo completo** (`cubiertos >= cantidad`)
  según miembros/asignaciones activos. Usar `cubiertos` y `faltantes`; no inferir
  el cupo por postulaciones aceptadas.
* **UC-36 / cupo parcial (pendiente backend):** hoy UC-36 bloquea editar/eliminar
  sólo cuando `satisfecho === true`. Si un requerimiento tiene `cubiertos > 0` y
  `faltantes > 0`, el payload de modificación lo elimina y recrea, y puede chocar
  con la FK de miembros. El backend debe bloquear también cuando `cubiertos > 0`
  (o preservar el requerimiento). Mientras tanto la UI mantiene el bloqueo por
  `satisfecho` y usa el copy amplio *"Tiene miembros activos: no se puede editar
  ni eliminar"*.
* **Elegibilidad por requerimiento (implementada):** el backend expone
  `puedePostularse` en cada requerimiento general y de actividad. El front debe
  usarlo como fuente de verdad. `elegibleParaPostularse` queda deprecado para el
  gating y sólo se mantiene como resumen/compatibilidad.
* **Alcance de `puedePostularse`:** evalúa estado del proyecto/actividad,
  membresía/rol, profesión y cupo. **No** evalúa postulaciones duplicadas ni
  disponibilidad de calendario; esos casos siguen devolviendo `409` y deben
  manejarse como red de seguridad.
* **Rollout:** si el backend todavía no envía `puedePostularse`, el front usa el
  fallback histórico (§4.4). No es necesario desplegar front y backend a la vez.
* **UC-74/UC-34:** implementados en `/proyectos/:id/postulaciones`; el acceso
  se muestra al director con `GESTIONAR_POSTULACIONES`.
