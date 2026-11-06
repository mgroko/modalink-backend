# Ficha Técnica Frontend: Eliminar Actividad de Planificación (UC-51)

Especificación de la API, flujo de decisiones sobre integrantes, validaciones y manejo de errores para eliminar una actividad de la planificación.

> **Actualización:** el `DELETE /proyectos/{idProyecto}/actividades/{idActividad}?motivo=...` anterior fue retirado. El frontend debe usar el endpoint de comando `POST /proyectos/{idProyecto}/actividades/{idActividad}/eliminar`, que permite resolver en una sola transacción qué hacer con los integrantes que participan exclusivamente de la actividad.

---

## 1. Endpoints

### 1.1 Listar integrantes exclusivos

```http
GET /proyectos/{idProyecto}/actividades/{idActividad}/miembros-exclusivos
```

- **Path params:**
  - `idProyecto` (`long`, obligatorio)
  - `idActividad` (`long`, obligatorio)
- **Autenticación:** JWT en cookie HTTP-only `jwt`, CSRF en `X-XSRF-TOKEN`, `credentials: "include"`.
- **Perfil activo:** obligatorio en la sesión. No se envía `idPerfilActivo`.
- **Permiso:** `ELIMINAR_ACTIVIDAD`.
- **Respuesta exitosa:** `200 OK` con una lista; puede ser vacía.

Ejemplo:

```json
[
  {
    "idAsignacionActividad": 30,
    "idMiembro": 20,
    "idPerfil": 10,
    "idUsuario": 2,
    "nombreArtistico": "Luna",
    "idProfesion": 2,
    "nombreProfesion": "Modelo",
    "puedeEliminar": true,
    "motivoNoEliminable": null
  }
]
```

`puedeEliminar = false` significa que el backend no aceptará `ELIMINAR` para ese miembro. El campo `motivoNoEliminable` puede ser:

| Valor | Significado |
|---|---|
| `AUTOELIMINACION` | El director no puede eliminarse a sí mismo |
| `ULTIMO_DIRECTOR` | Es el último director activo del proyecto |
| `PROYECTO_BORRADOR` | UC-38 no permite eliminar integrantes en Borrador |

La lista solo incluye miembros **activos** que participan únicamente de esta actividad dentro del proyecto. Los miembros con otras actividades no canceladas del mismo proyecto se conservan automáticamente y no aparecen.

### 1.2 Eliminar la actividad con decisiones

```http
POST /proyectos/{idProyecto}/actividades/{idActividad}/eliminar
Content-Type: application/json
```

- **Permiso:** `ELIMINAR_ACTIVIDAD`.
- **Permiso adicional:** si hay decisiones `ELIMINAR`, UC-38 valida `ELIMINAR_INTEGRANTE` y rol Director.
- **Respuesta exitosa:** `204 No Content`, sin body.
- **Operación transaccional:** si falla cualquier decisión, no se elimina la actividad ni se modifica ninguna membresía.

Request:

```json
{
  "motivo": "Falta de presupuesto",
  "decisiones": [
    {
      "idMiembro": 20,
      "accion": "ELIMINAR",
      "motivoBaja": "Falta de compromiso"
    },
    {
      "idMiembro": 21,
      "accion": "GUARDAR"
    }
  ]
}
```

| Campo | Tipo | Obligatorio | Reglas |
|---|---|:---:|---|
| `motivo` | `string` | Sí | No vacío; máximo 200 caracteres |
| `decisiones` | `array` | Sí | Puede ser `[]`; debe existir |
| `decisiones[].idMiembro` | `long` | Sí | Debe pertenecer al listado de exclusivos |
| `decisiones[].accion` | `ELIMINAR` \| `GUARDAR` | Sí | — |
| `decisiones[].motivoBaja` | `string` | No | Máximo 100 caracteres; si falta se usa `motivo` |

Reglas de `decisiones`:

- Debe haber **una decisión por cada integrante exclusivo activo**.
- Si no hay integrantes exclusivos, enviar `decisiones: []`.
- No se permiten decisiones duplicadas.
- No se permiten decisiones para miembros que no sean exclusivos.
- `ELIMINAR` solo se acepta si `puedeEliminar = true`.
- `GUARDAR` conserva al miembro en el proyecto; su asignación a la actividad se elimina junto con la actividad.

### 1.3 Fallback del motivo de baja

Si `motivoBaja` falta o está vacío:

1. se usa el `motivo` de la actividad;
2. si el `motivo` de la actividad supera los 100 caracteres, el backend responde `400` y el frontend debe solicitar un motivo de baja propio.

No se trunca el motivo automáticamente.

---

## 2. Flujo de pantalla

### 2.1 Entrada

Desde la planificación o el detalle de la actividad, el Director selecciona **Eliminar actividad**.

Antes de habilitar la acción, verificar:

- `permisos` contiene `ELIMINAR_ACTIVIDAD`;
- el proyecto está en `Borrador`, `Publicado` o `Confirmado`;
- la actividad no está `FINALIZADA` ni `CANCELADA`;
- si el proyecto está `Confirmado`, la actividad no está `EN_CURSO`.

### 2.2 Carga de integrantes exclusivos

Al abrir el modal de eliminación, llamar a:

```http
GET /proyectos/{idProyecto}/actividades/{idActividad}/miembros-exclusivos
```

- Si la lista está vacía, mostrar la confirmación simple con el motivo de la actividad.
- Si hay integrantes, mostrar una fila por cada uno con:
  - nombre artístico y profesión;
  - opción **Eliminar del proyecto**;
  - opción **Guardar en el proyecto**;
  - motivo de baja cuando se elige eliminar.

Si `puedeEliminar = false`, deshabilitar **Eliminar del proyecto** y mostrar el motivo correspondiente.

### 2.3 Confirmación

1. Solicitar el motivo de eliminación de la actividad.
2. Para cada integrante exclusivo, solicitar la decisión.
3. Si se elige eliminar, permitir un motivo de baja propio o dejar el campo vacío para usar el motivo de la actividad.
4. Enviar un único `POST` al endpoint de eliminación.
5. Si la respuesta es `204`, cerrar el modal y refrescar la planificación completa.

No usar el `DELETE` anterior: ya no existe.

---

## 3. Efectos de la eliminación

La operación es transaccional.

1. La actividad se elimina físicamente.
2. Las predecesoras se conectan directamente con sus sucesoras.
3. Los integrantes con decisión `ELIMINAR` pasan a `ELIMINADO` en el proyecto y reciben `INTEGRANTE_ELIMINADO`.
4. Los integrantes con decisión `GUARDAR` siguen activos en el proyecto y reciben una notificación de continuidad.
5. Los integrantes con otras actividades del proyecto se conservan automáticamente.
6. Se eliminan las asignaciones de la actividad, incluidas las históricas.
7. Se eliminan postulaciones e invitaciones vinculadas a sus requerimientos.
8. Se notifica a postulantes e invitados afectados.
9. Los requerimientos, dependencias e hitos vinculados se eliminan.
10. El cronograma se recalcula en lectura; no se persisten nuevas fechas.

Mensajes esperados:

- Integrante eliminado: `INTEGRANTE_ELIMINADO` con el motivo de baja.
- Integrante guardado: `ACTIVIDAD_ELIMINADA` indicando que la actividad fue eliminada y que continúa en el proyecto.
- Postulante/invitado: notificación de que su postulación o invitación quedó sin efecto.

---

## 4. Reglas por estado

### 4.1 Proyecto

| Estado | Eliminación |
|---|---|
| `BORRADOR` | Permitida. Los exclusivos solo pueden `GUARDAR` porque UC-38 no permite eliminar integrantes en Borrador. |
| `PUBLICADO` | Permitida. `ELIMINAR` o `GUARDAR`. |
| `CONFIRMADO` | Permitida si la actividad está `PENDIENTE`. Si está `EN_CURSO`, primero debe cancelarse. |
| `FINALIZADO` / `CANCELADO` | `409`. |

### 4.2 Actividad

| Estado | Eliminación |
|---|---|
| `PENDIENTE` | Permitida |
| `EN_CURSO` | Permitida en `BORRADOR`/`PUBLICADO`; en `CONFIRMADO` requiere cancelación previa |
| `FINALIZADA` / `CANCELADA` | `409` |

---

## 5. Errores

| HTTP | Caso | Acción frontend |
|---:|---|---|
| `400` | `motivo` vacío o mayor a 200 caracteres | Mostrar error inline |
| `400` | `decisiones` ausente, duplicada, incompleta o con miembro no exclusivo | Reabrir el modal y refrescar exclusivos |
| `400` | `motivoBaja` mayor a 100 caracteres | Error en la fila del integrante |
| `400` | Fallback al motivo de la actividad supera 100 caracteres | Pedir un motivo propio |
| `403` | Sin perfil activo, sin membresía o sin permiso | Ocultar o deshabilitar la acción |
| `404` | Proyecto, planificación, actividad o miembro inexistente | Cerrar el modal y refrescar |
| `409` | Actividad `FINALIZADA` o `CANCELADA` | Informar y refrescar |
| `409` | Proyecto no permitido | Informar y refrescar |
| `409` | Actividad `EN_CURSO` en proyecto `CONFIRMADO` | Ofrecer cancelar la actividad primero |
| `409` | Intento de eliminar al actor, al último director o integrante no permitido | Deshabilitar la opción o mostrar el motivo |

---

## 6. Ejemplos de request

### Sin integrantes exclusivos

```json
{
  "motivo": "Falta de presupuesto",
  "decisiones": []
}
```

### Con integrantes exclusivos

```json
{
  "motivo": "Falta de presupuesto",
  "decisiones": [
    {
      "idMiembro": 20,
      "accion": "ELIMINAR",
      "motivoBaja": "Falta de compromiso"
    },
    {
      "idMiembro": 21,
      "accion": "GUARDAR"
    }
  ]
}
```

---

## 7. Criterios de aceptación

- El frontend ya no usa `DELETE /proyectos/{idProyecto}/actividades/{idActividad}?motivo=...`.
- Sin integrantes exclusivos, el `POST` con `decisiones: []` devuelve `204`.
- Con integrantes exclusivos, el frontend exige una decisión por cada uno.
- Un integrante con `ELIMINAR` queda fuera del proyecto y recibe la notificación correspondiente.
- Un integrante con `GUARDAR` sigue activo y recibe el mensaje de continuidad.
- Una decisión inválida no elimina la actividad ni modifica membresías.
- En un proyecto `CONFIRMADO`, una actividad `EN_CURSO` no se puede eliminar sin cancelarla primero.
- El listado de exclusivos no incluye miembros con otras actividades del mismo proyecto.
