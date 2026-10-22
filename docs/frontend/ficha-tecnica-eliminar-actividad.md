# Ficha Técnica Frontend: Eliminar Actividad de Planificación (UC-51)

Especificación de la API, flujo de confirmación, validaciones, efectos sobre el cronograma y manejo de errores para eliminar una actividad de un proyecto.

---

## 1. Endpoint y Contexto de Autenticación

### 1.1 Eliminar actividad

- **Método:** `DELETE`
- **URL:** `/proyectos/{idProyecto}/actividades/{idActividad}?motivo={motivo}`
- **Path params:**
  - `idProyecto` (`long`, obligatorio)
  - `idActividad` (`long`, obligatorio)
- **Query param:** `motivo` (`string`, obligatorio)
- **Respuesta exitosa:** `204 No Content`, sin body.
- **Permiso efectivo:** `ELIMINAR_ACTIVIDAD` en el proyecto.

### 1.2 Autenticación común

- JWT en cookie HTTP-only `jwt`.
- CSRF en header `X-XSRF-TOKEN`; la cookie `XSRF-TOKEN` no es HTTP-only.
- Todas las peticiones deben usar `credentials: "include"`.
- Perfil activo obligatorio en la sesión. El frontend no envía `idPerfilActivo`.
- El backend no usa el header `Authorization`.

### 1.3 Precondiciones

- Perfil activo, miembro activo y no dado de baja.
- Proyecto en estado `BORRADOR`, `PUBLICADO` o `CONFIRMADO`.
- Actividad existente y perteneciente a la planificación del proyecto.
- Actividad en estado `PENDIENTE` o `EN_CURSO`.

El backend valida el permiso `ELIMINAR_ACTIVIDAD` antes que el estado del proyecto. Sin permiso responde `403` aunque el proyecto no admita la operación.

---

## 2. Flujo de Pantalla

### 2.1 Entrada

Desde la búsqueda o el detalle de la planificación, el Director selecciona **Eliminar actividad**.

Antes de habilitar la acción, consultar `GET /proyectos/{idProyecto}` y comprobar:

- `permisos` contiene `ELIMINAR_ACTIVIDAD`.
- `estado` es `Borrador`, `Publicado` o `Confirmado`.
- La actividad no está `FINALIZADA` ni `CANCELADA`.

### 2.2 Confirmación

1. Mostrar información básica de la actividad: nombre, descripción, fechas, estado, requerimientos y profesionales asignados.
2. Solicitar el motivo de la eliminación.
3. Advertir que la acción elimina la actividad y recalcula el cronograma.
4. Advertir que, si la actividad tenía postulaciones o invitaciones activas, se eliminarán y se notificará a los afectados.
5. Confirmar con `DELETE` enviando el motivo como query param.

### 2.3 Después del `204`

- Cerrar el modal de confirmación.
- Refrescar la planificación completa porque las fechas de otras actividades pueden cambiar.
- Refrescar el detalle del proyecto y el estado del cronograma.
- No reutilizar la actividad eliminada en el estado local.

---

## 3. Request

### 3.1 Ejemplo

```http
DELETE /proyectos/100/actividades/55?motivo=Falta%20de%20presupuesto
```

### 3.2 Matriz del motivo

| Campo | Tipo | Obligatorio | Reglas backend | Notas UI/UX |
| :--- | :--- | :---: | :--- | :--- |
| `motivo` | `string` | Sí | No puede ser vacío ni contener solo espacios. Se normaliza quitando espacios al inicio y al final. | Textarea obligatorio con validación antes de habilitar la confirmación. |

Longitud máxima: `200` caracteres, igual que el motivo de cancelación de proyecto. El backend rechaza motivos más largos con `400`.

---

## 4. Efectos de la Eliminación

La operación es transaccional. Si falla una parte, no se aplica ningún cambio.

1. La actividad se elimina físicamente de la planificación.
2. Las predecesoras de la actividad se conectan directamente con sus sucesoras.
3. El cronograma se recalcula. Puede devolver estado `NORMAL` o `ALERTA_DESBORDE`.
4. Las asignaciones de la actividad se eliminan, pero los miembros continúan perteneciendo al proyecto.
5. Las postulaciones e invitaciones vinculadas a los requerimientos de la actividad se eliminan.
6. Se generan notificaciones para:
   - los miembros asignados a la actividad;
   - los postulantes afectados;
   - los invitados afectados.
7. Las notificaciones incluyen el motivo de la eliminación.

La eliminación de integrantes del proyecto no forma parte de esta iteración. Queda pendiente de UC-38.

---

## 5. Errores

| Código | Caso | Acción sugerida en UI |
| :--- | :--- | :--- |
| `400` | Motivo vacío o inválido | Mostrar el mensaje y mantener el modal abierto. |
| `403` | Sin perfil activo, sin membresía activa o sin `ELIMINAR_ACTIVIDAD` | Ocultar o deshabilitar la acción; no reintentar. |
| `404` | Proyecto, planificación o actividad inexistente | Cerrar el modal y refrescar la planificación. |
| `409` | Actividad `FINALIZADA` o `CANCELADA` | Informar que no se puede eliminar y refrescar el detalle. |
| `409` | Proyecto en estado no permitido | Informar que el proyecto no admite la operación. |

---

## 6. Preparación para UC-38 y UC-70

Cuando se implementen la asignación de actividades y la eliminación de integrantes:

- El frontend podrá consultar qué miembros participan únicamente de la actividad a eliminar.
- Para esos miembros se ofrecerán las opciones **Eliminar integrante del proyecto** o **Guardar integrante**.
- La baja del integrante se delegará en el futuro flujo de UC-38.
- Los miembros que tengan otras actividades asignadas en el mismo proyecto no deberán pasar por esa elección.

Hasta entonces, el backend elimina las asignaciones de la actividad y conserva a todos los miembros en el proyecto.
