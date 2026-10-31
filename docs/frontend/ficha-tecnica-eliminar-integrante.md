# Ficha Técnica Frontend: Eliminar Integrante de Proyecto (UC-38)

Especificación de la API, flujo de pantalla, validaciones, efectos sobre asignaciones y vacantes, historial de profesionales y manejo de errores para eliminar un integrante de un proyecto.

---

## 1. Endpoint y contexto de autenticación

### 1.1 Eliminar integrante

- **Método:** `DELETE`
- **URL:** `/proyectos/{idProyecto}/miembros/{idMiembro}`
- **Path params:**
  - `idProyecto` (`long`, obligatorio)
  - `idMiembro` (`long`, obligatorio) — es el `idMiembro` que devuelve `GET /proyectos/{idProyecto}/miembros`
- **Body:**

```json
{
  "motivo": "Falta de compromiso con el cronograma"
}
```

- **Content-Type:** `application/json`
- **Respuesta exitosa:** `204 No Content`, sin body.
- **Permiso efectivo:** `ELIMINAR_INTEGRANTE` en el proyecto.
- **Rol requerido:** Director activo del proyecto.

### 1.2 Autenticación común

- JWT en cookie HTTP-only `jwt`.
- CSRF en header `X-XSRF-TOKEN`; la cookie `XSRF-TOKEN` no es HTTP-only.
- Todas las peticiones deben usar `credentials: "include"`.
- Perfil activo obligatorio en la sesión. El frontend **no** envía `idPerfilActivo`.
- El backend no usa el header `Authorization`.

### 1.3 Precondiciones

- Perfil activo y usuario activo.
- Perfil con membresía activa en el proyecto.
- Permiso `VER_PROYECTO` para consultar el listado de miembros.
- Permiso `ELIMINAR_INTEGRANTE` para ejecutar la baja.
- Rol Director activo.
- Proyecto en estado `Publicado` o `Confirmado`.
- Miembro objetivo con `estadoParticipacion = ACTIVO`.
- El miembro objetivo no es el propio perfil activo.
- El miembro objetivo no es el último Director activo.

El backend valida el permiso antes que el estado del proyecto. Sin permiso responde `403` aunque el proyecto no admita la operación.

---

## 2. Flujo de pantalla

### 2.1 Entrada

Desde el dashboard del proyecto (`/proyectos/:id`), sección **Integrantes**, el Director selecciona la acción **Eliminar integrante**.

Antes de mostrar el botón, consultar:

```http
GET /proyectos/{idProyecto}/miembros?todos=true
```

Cada fila incluye:

| Campo | Tipo | Uso |
| :--- | :--- | :--- |
| `idMiembro` | `long` | Identificador para el `DELETE` |
| `idPerfil` | `long` | Identificador del perfil |
| `idUsuario` | `long` | Usuario asociado |
| `nombreArtistico` | `string` | Nombre visible |
| `nombreUsuario` / `apellidoUsuario` | `string` | Nombre real |
| `idProfesion` / `nombreProfesion` | `long` / `string` | Profesión |
| `rol` | `string` | `DIRECTOR` o `MIEMBRO` |
| `estadoParticipacion` | `string` | `ACTIVO` para los miembros listados |
| `eliminable` | `boolean` | **Usar este campo para habilitar/ocultar la acción** |
| `motivoNoEliminable` | `string \| null` | Motivo si `eliminable = false` |

Valores de `motivoNoEliminable`:

| Valor | Significado |
| :--- | :--- |
| `SIN_PERMISO` | El actor no es Director o no tiene `ELIMINAR_INTEGRANTE` |
| `PROYECTO_NO_PERMITE_BAJA` | El proyecto no está `Publicado` ni `Confirmado` |
| `AUTOELIMINACION` | El miembro es el propio perfil activo |
| `ULTIMO_DIRECTOR` | Es el único Director activo del proyecto |
| `null` | El miembro es eliminable |

El frontend **no debe recalcular** estas reglas. Si `eliminable` es `false`, ocultar o deshabilitar la acción y usar `motivoNoEliminable` para el tooltip.

### 2.2 Selección y confirmación

1. Mostrar información básica del proyecto: nombre, estado y rol del actor.
2. Listar miembros activos con `GET /proyectos/{idProyecto}/miembros?todos=true`.
3. Al seleccionar un miembro, mostrar su información:
   - nombre artístico,
   - nombre y apellido,
   - profesión,
   - rol.
   - Opcional: si el perfil está activo, se puede consultar `GET /perfiles/{idPerfil}` para ampliar la información. Si ese endpoint responde `404`, usar solo los datos de la lista de miembros.
4. Abrir un modal de confirmación con:
   - advertencia de que la acción desvincula al integrante del proyecto,
   - textarea obligatorio para el motivo,
   - contador `(x/100)`,
   - botón de confirmación deshabilitado si el motivo está vacío.
5. Confirmar con el `DELETE`.

### 2.3 Después del `204`

- Cerrar el modal.
- Mostrar toast de éxito.
- Refrescar `GET /proyectos/{idProyecto}/miembros?todos=true`.
- Refrescar `GET /proyectos/{idProyecto}` si se muestran requerimientos generales.
- Si la pantalla de planificación está abierta, refrescar:
  - `GET /proyectos/{idProyecto}/actividades`,
  - el detalle de la actividad que se esté visualizando.
- No reutilizar en el estado local al miembro eliminado.
- Si se eliminó a otro Director, los `eliminable` de los restantes pueden cambiar. Refrescar la lista.

---

## 3. Request

### 3.1 Ejemplo

```http
DELETE /proyectos/105/miembros/900
Content-Type: application/json

{
  "motivo": "Falta de compromiso con el cronograma"
}
```

### 3.2 Matriz del motivo

| Campo | Tipo | Obligatorio | Reglas backend | Notas UI/UX |
| :--- | :--- | :---: | :--- | :--- |
| `motivo` | `string` | Sí | No puede ser vacío ni contener solo espacios. Máximo **100 caracteres**. Se normaliza quitando espacios al inicio y al final. Se persiste en la membresía. | Textarea con `maxlength="100"`, contador `(x/100)`, `trim()` antes de enviar y confirmación deshabilitada si está vacío. |

El motivo se guarda en la membresía (`motivoBaja`) y se incluye en la notificación al perfil eliminado.

---

## 4. Efectos de la eliminación

La operación es transaccional. Si falla una parte, no se aplica ningún cambio.

1. La membresía **no se borra físicamente**. Pasa a `estadoParticipacion = ELIMINADO`.
2. Se guarda el motivo en `motivoBaja`.
3. Se guarda `fechaBaja` con la fecha y hora de la operación.
4. **No se borran las asignaciones a actividades.** Se conservan para auditoría y trazabilidad.
5. Las consultas operativas ignoran a los miembros que ya no están `ACTIVO`, por lo que:
   - el profesional deja de contar como recurso actual,
   - la vacante del requerimiento se reabre automáticamente,
   - el profesional deja de aparecer en su calendario operativo.
6. El proyecto no se cancela ni se modifican sus actividades.
7. No se recalculan fechas del cronograma.
8. Se genera una notificación al perfil eliminado con el tipo `INTEGRANTE_ELIMINADO`.

### 4.1 Historial de profesionales por actividad

Las asignaciones conservadas se muestran como historial según el estado actual de la actividad:

| Estado de la actividad | Historial de profesionales | Cupo |
| :--- | :--- | :--- |
| `PENDIENTE` | No se muestra al miembro eliminado | Solo cuenta miembros activos; la vacante queda abierta |
| `EN_CURSO` | Sí, con `estadoParticipacion = ELIMINADO` y `activo = false` | Solo cuenta miembros activos; la vacante queda abierta |
| `FINALIZADA` | Sí, como histórico | Cuenta todas las asignaciones; es un cupo histórico |
| `CANCELADA` | Sí, como histórico | Cuenta todas las asignaciones; es un cupo histórico |

Los endpoints de actividad (`GET /proyectos/{idProyecto}/actividades` y `GET /proyectos/{idProyecto}/actividades/{idActividad}`) pueden devolver profesionales históricos en:

- `responsables` (listado de planificación),
- `profesionalesAsignados` (detalle de actividad).

Cada profesional incluye:

| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `idAsignacionActividad` | `long` | Identificador de la asignación |
| `idMiembroProyecto` | `long` | Membresía histórica |
| `idPerfil` | `long` | Perfil |
| `idUsuario` | `long` | Usuario |
| `nombreArtistico` | `string` | Nombre artístico |
| `idProfesion` / `nombreProfesion` | `long` / `string` | Profesión |
| `estadoParticipacion` | `string` | `ACTIVO`, `ELIMINADO` o `BAJA_VOLUNTARIA` |
| `activo` | `boolean` | `true` solo si sigue activo en el proyecto |

El frontend debe mostrar una etiqueta del tipo **“Eliminado del proyecto”** cuando `activo = false`. No debe tratar a un histórico como recurso actual.

---

## 5. Notificación

- **Tipo:** `INTEGRANTE_ELIMINADO`
- **Título para el frontend:** “Integrante eliminado”
- **Destinatario:** únicamente el perfil eliminado.
- **Mensaje:** incluye el nombre del proyecto y el motivo.
- **Entidad:** `PROYECTO` con el `idProyecto`.

El perfil eliminado deja de ser miembro activo. Si el proyecto está `Publicado` o `Confirmado`, `GET /proyectos/{idProyecto}` puede responder `403`. Por eso, la notificación **no debe navegar automáticamente** al proyecto; mostrar el mensaje y, si se intenta abrir, manejar el `403`.

---

## 6. Errores

### 6.1 Formato general

```json
{
  "message": "El motivo de eliminación es obligatorio.",
  "httpStatus": 400,
  "timestamp": 1760000000000
}
```

### 6.2 Errores de validación (`400`)

```json
{
  "message": "Validación fallida",
  "errores": {
    "motivo": "El motivo de eliminación es obligatorio."
  },
  "httpStatus": 400,
  "timestamp": 1760000000000
}
```

### 6.3 Tabla de errores

| HTTP | Caso | Mensaje esperado | Acción frontend |
| :---: | :--- | :--- | :--- |
| `400` | Motivo vacío, ausente o solo espacios | `El motivo de eliminación es obligatorio.` | Mostrar error sobre el textarea; mantener el modal abierto |
| `400` | Motivo mayor a 100 caracteres | `El motivo de eliminación no puede superar los 100 caracteres.` | Validar con `maxlength` y contador |
| `401` | Sesión ausente o inválida | Seguridad | Redirigir a login |
| `403` | Sin permiso o sin membresía activa | `No tienes el permiso 'ELIMINAR_INTEGRANTE' en este proyecto o no eres un miembro activo.` | Ocultar la acción y refrescar el proyecto |
| `403` | No es Director activo | `Debes ser Director activo del proyecto para realizar esta acción.` | Ocultar la acción |
| `404` | Sin perfil activo | `No hay un perfil activo seleccionado en la sesión.` | Abrir selección de perfil |
| `404` | Perfil no encontrado | `El perfil no fue encontrado` | Aviso y volver a seleccionar perfil |
| `404` | Proyecto inexistente | `El proyecto con id {id} no existe.` | Estado “proyecto no encontrado” |
| `404` | Miembro inexistente o de otro proyecto | `El miembro de proyecto con id {id} no existe.` | Cerrar el modal y refrescar la lista |
| `409` | Miembro ya eliminado o con baja voluntaria | `El miembro ya no tiene una participación activa en el proyecto. Estado actual: {estado}.` | Cerrar el modal y refrescar la lista |
| `409` | Proyecto en estado no permitido | `Solo se puede eliminar integrantes en proyectos Publicados o Confirmados. Estado actual: {estado}.` | Cerrar el modal y refrescar el proyecto |
| `409` | Autoeliminación | `No podés eliminarte a vos mismo del proyecto. Usá la opción Darse de baja (UC-40).` | Ocultar la acción para el propio perfil |
| `409` | Último Director activo | `El proyecto debe conservar al menos un director activo.` | Mostrar aviso y refrescar la lista |
| `≥ 500` | Error inesperado | — | Mensaje genérico y reintentar |

---

## 7. Integración sugerida

### 7.1 Servicio

Agregar en `src/services/proyectoService.js`:

```js
eliminarIntegrante(idProyecto, idMiembro, motivo) {
  return http.delete(`/proyectos/${idProyecto}/miembros/${idMiembro}`, {
    data: { motivo: motivo.trim() },
  })
}
```

Importante: Axios usa `data` para enviar body en `DELETE`. No usar `params` para el motivo.

### 7.2 Uso en el componente

```js
const puedeEliminar = miembro.eliminable === true

async function confirmarEliminacion() {
  if (!motivo.trim()) return
  await proyectoService.eliminarIntegrante(
    idProyecto,
    miembroSeleccionado.idMiembro,
    motivo,
  )
  toast.success('Integrante eliminado del proyecto')
  await recargarMiembros()
}
```

### 7.3 No enviar

- `idPerfilActivo`
- `idUsuario`
- `idProyecto` en el body
- `idMiembro` en el body
- `motivoBaja` como nombre de campo (el campo es `motivo`)

---

## 8. Consideraciones de UI/UX

### 8.1 Cuándo mostrar la acción

Usar exclusivamente `eliminable` de cada fila:

```js
const mostrarAccion = miembro.eliminable === true
```

No recalcular permiso, estado del proyecto, último Director ni autoeliminación en el frontend.

Si `eliminable` es `false`:

- ocultar el botón, o
- mostrarlo deshabilitado con un tooltip según `motivoNoEliminable`.

### 8.2 Modal de confirmación

1. Mostrar el nombre del proyecto y del integrante.
2. Advertir que la acción desvincula al integrante y que no se puede deshacer desde la UI.
3. Explicar que:
   - se conservarán las asignaciones históricas,
   - se reabrirán las vacantes correspondientes,
   - se enviará una notificación al perfil eliminado.
4. Textarea obligatorio con `maxlength="100"` y contador.
5. Botón de confirmación con estado `loading`.
6. No cerrar el modal ante `400` o `409`; mostrar el mensaje y conservar el texto.

### 8.3 Visualización de históricos

- En actividades `EN_CURSO`, `FINALIZADA` y `CANCELADA`, mostrar a los profesionales con `activo = false` con una etiqueta “Eliminado del proyecto” o “Baja voluntaria”.
- En actividades `PENDIENTE`, no mostrar históricos.
- No sumar históricos a los indicadores de personal actual.
- En actividades `EN_CURSO`, la vacante sigue abierta aunque el histórico aparezca en la lista.
- En actividades `FINALIZADA` o `CANCELADA`, el cupo es histórico y no debe ofrecerse como acción de cobertura.

### 8.4 Notificación

- Mostrar la notificación con el título “Integrante eliminado”.
- No navegar automáticamente al proyecto.
- Si el usuario intenta abrir el proyecto y recibe `403`, mostrar un aviso de que ya no participa en él.

---

## 9. Checklist de aceptación

- [ ] La acción se muestra solo si `eliminable === true`.
- [ ] El modal exige un motivo no vacío de hasta 100 caracteres.
- [ ] El request usa `DELETE /proyectos/{idProyecto}/miembros/{idMiembro}` con body `{ "motivo": "..." }`.
- [ ] Un `204` cierra el modal y refresca la lista de miembros.
- [ ] Se refresca el proyecto y, si corresponde, la planificación.
- [ ] Los errores `400`, `403`, `404` y `409` se muestran sin cerrar el modal.
- [ ] El propio perfil y el último Director no muestran la acción.
- [ ] Las actividades `EN_CURSO`, `FINALIZADA` y `CANCELADA` muestran históricos con `activo = false`.
- [ ] Las actividades `PENDIENTE` no muestran históricos.
- [ ] Los históricos no se cuentan como personal actual.
- [ ] La notificación `INTEGRANTE_ELIMINADO` no navega automáticamente al proyecto.
