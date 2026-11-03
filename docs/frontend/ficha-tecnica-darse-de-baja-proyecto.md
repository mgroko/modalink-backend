# Ficha Técnica Frontend: Darse de Baja de Proyecto (UC-40)

Especificación de la API, flujo de pantalla, reglas de negocio y manejo de
errores para que un integrante se dé de baja voluntariamente de un proyecto.

---

## 1. Resumen rápido

- **Método:** `POST`
- **URL:** `/proyectos/{idProyecto}/baja`
- **Body:** `{ "motivo": "..." }`
- **Respuesta exitosa:** `204 No Content`
- **Autenticación:** JWT en cookie HTTP-only + CSRF.
- **Perfil activo:** obligatorio. El frontend no envía `idPerfil`.
- **Motivo:** obligatorio, máximo 100 caracteres.
- **Efecto:** la membresía pasa a `BAJA_VOLUNTARIA` y las asignaciones se
  conservan como historial.

---

## 2. Endpoint y contexto de autenticación

### 2.1 Endpoint

```http
POST /proyectos/{idProyecto}/baja
Content-Type: application/json
```

```json
{
  "motivo": "Decidí enfocarme en otros proyectos"
}
```

- **Path param:** `idProyecto` (`long`, obligatorio).
- **Body:** `motivo` (`string`, obligatorio, máximo 100).
- **Respuesta:** `204 No Content`, sin body.
- No se envía `idPerfil`, `idMiembro`, `idUsuario` ni `motivoBaja` en el body.

### 2.2 Autenticación común

- JWT en cookie HTTP-only `jwt`.
- CSRF en header `X-XSRF-TOKEN`; la cookie `XSRF-TOKEN` no es HTTP-only.
- Todas las peticiones deben usar `credentials: "include"`.
- El backend resuelve el perfil activo desde la sesión.
- El backend no usa el header `Authorization`.

### 2.3 Precondiciones

- Usuario y perfil activos.
- Membresía activa en el proyecto.
- Proyecto en estado `Publicado` o `Confirmado`.
- Si el perfil es Director, debe existir al menos otro Director activo.
- El motivo no puede estar vacío ni superar los 100 caracteres.

---

## 3. Cómo saber si mostrar la acción

Consultar el detalle del proyecto:

```http
GET /proyectos/{idProyecto}
```

El response incluye:

```json
{
  "idProyecto": 100,
  "estado": "Publicado",
  "esMiembro": true,
  "rolUsuario": "MIEMBRO",
  "puedeDarseDeBaja": true
}
```

Regla de UI:

```js
const mostrarDarseDeBaja = proyecto.puedeDarseDeBaja === true
```

No recalcular en el frontend:

- estado del proyecto,
- membresía activa,
- último Director.

Si `puedeDarseDeBaja` es `false`, ocultar la acción. Si el usuario intenta
ejecutarla igualmente, el backend responde `409`.

---

## 4. Request

### 4.1 Ejemplo

```http
POST /proyectos/100/baja
Content-Type: application/json

{
  "motivo": "Decidí enfocarme en otros proyectos"
}
```

### 4.2 Matriz del motivo

| Campo | Tipo | Obligatorio | Reglas backend | Notas UI/UX |
| :--- | :--- | :---: | :--- | :--- |
| `motivo` | `string` | Sí | No vacío, no solo espacios, máximo **100 caracteres**, se guarda con `trim()` | Textarea con `maxlength="100"`, contador `(x/100)` y confirmación deshabilitada si está vacío |

---

## 5. Efectos de la baja

La operación es transaccional. Si falla una parte, no se aplica ningún cambio.

1. La membresía **no se borra**. Pasa a `estadoParticipacion = BAJA_VOLUNTARIA`.
2. Se guarda `motivoBaja` con el texto normalizado.
3. Se guarda `fechaBaja` con la fecha y hora de la operación.
4. **No se borran las asignaciones a actividades.** Se conservan para
   auditoría y trazabilidad.
5. Las consultas operativas ignoran a los miembros que ya no están `ACTIVO`:
   - el profesional deja de contar como recurso actual;
   - la vacante del requerimiento se reabre automáticamente;
   - sus actividades dejan de aparecer en calendario y home.
6. El proyecto no se cancela ni se modifican sus actividades.
7. No se recalculan fechas del cronograma.
8. Se notifica a todos los directores activos restantes con el tipo
   `INTEGRANTE_BAJA_VOLUNTARIA`.
9. No se notifica al perfil que se dio de baja.

---

## 6. Historial de profesionales por actividad

Las asignaciones conservadas se muestran como historial según el estado actual
de la actividad:

| Estado de la actividad | Historial de profesionales | Cupo |
| :--- | :--- | :--- |
| `PENDIENTE` | No se muestra el exmiembro | Solo cuenta miembros activos; la vacante queda abierta |
| `EN_CURSO` | Sí, con `estadoParticipacion = BAJA_VOLUNTARIA` y `activo = false` | Solo cuenta miembros activos; la vacante queda abierta |
| `FINALIZADA` | Sí, como histórico | Cuenta todas las asignaciones; es un cupo histórico |
| `CANCELADA` | Sí, como histórico | Cuenta todas las asignaciones; es un cupo histórico |

Los endpoints de actividad pueden devolver profesionales históricos en:

- `responsables` de `GET /proyectos/{idProyecto}/actividades`;
- `profesionalesAsignados` de `GET /proyectos/{idProyecto}/actividades/{idActividad}`.

Cada profesional incluye `estadoParticipacion` y `activo`. Mostrar una etiqueta
tipo **“Baja voluntaria”** cuando `activo = false`.

---

## 7. Flujo de pantalla

### 7.1 Entrada

Desde el detalle del proyecto (`/proyectos/:id`), en la sección de acciones del
integrante:

- Consultar `GET /proyectos/{idProyecto}`.
- Mostrar la acción **Darse de baja** solo si `puedeDarseDeBaja === true`.

### 7.2 Confirmación

Abrir un modal con:

1. Nombre del proyecto.
2. Advertencia de que la baja desvincula al integrante del proyecto.
3. Aclaración de que las asignaciones quedan como historial y las vacantes se
   reabren.
4. Textarea obligatorio para el motivo.
5. Contador `(x/100)`.
6. Botón de confirmación deshabilitado si el motivo está vacío o supera 100.
7. Estado `loading` mientras se envía.

Texto sugerido:

> ¿Querés darte de baja del proyecto **{nombre}**? Esta acción desvincula tu
> perfil del proyecto. Tus asignaciones quedarán como historial y las vacantes
> se reabrirán automáticamente.

### 7.3 Después del `204`

- Cerrar el modal.
- Mostrar toast de éxito.
- Refrescar `GET /proyectos/propios`.
- Redirigir a “Mis proyectos” o home.
- Si el proyecto es privado, no volver al detalle: el backend responderá `403`.
- Si el proyecto es público, el detalle sigue visible como no miembro.

---

## 8. Errores

### 8.1 Formato general

```json
{
  "message": "El motivo de la baja es obligatorio.",
  "httpStatus": 400,
  "timestamp": 1760000000000
}
```

### 8.2 Errores de validación (`400`)

```json
{
  "message": "Validación fallida",
  "errores": {
    "motivo": "El motivo de la baja es obligatorio."
  },
  "httpStatus": 400,
  "timestamp": 1760000000000
}
```

### 8.3 Tabla de errores

| HTTP | Caso | Acción frontend |
| :---: | :--- | :--- |
| `400` | Motivo vacío, ausente o solo espacios | Mostrar error sobre el textarea; mantener el modal abierto |
| `400` | Motivo mayor a 100 caracteres | Validar con `maxlength` y contador |
| `401` | Sesión ausente o inválida | Redirigir al login |
| `403` | Sin perfil activo o sin membresía activa | Cerrar el modal y refrescar el proyecto |
| `404` | Proyecto inexistente | Estado “proyecto no encontrado” |
| `409` | Proyecto no está Publicado/Confirmado | Cerrar el modal y refrescar el proyecto |
| `409` | Membresía ya dada de baja o eliminada | Cerrar el modal y refrescar |
| `409` | Último Director activo | Mostrar aviso y refrescar |
| `≥ 500` | Error inesperado | Mensaje genérico y reintentar |

---

## 9. Integración sugerida

### 9.1 Servicio

```js
export function darseDeBajaProyecto(idProyecto, motivo) {
  return http.post(`/proyectos/${idProyecto}/baja`, {
    motivo: motivo.trim(),
  })
}
```

### 9.2 Uso en el componente

```js
const puedeDarseDeBaja = proyecto.puedeDarseDeBaja === true

async function confirmarBaja() {
  if (!motivo.trim()) return

  await proyectoService.darseDeBajaProyecto(idProyecto, motivo)
  toast.success('Te diste de baja del proyecto')
  await recargarProyectosPropios()
  router.push('/proyectos')
}
```

### 9.3 No enviar

- `idPerfilActivo`
- `idUsuario`
- `idMiembro`
- `idProyecto` en el body
- `motivoBaja` como nombre de campo

---

## 10. Notificación

- **Tipo:** `INTEGRANTE_BAJA_VOLUNTARIA`
- **Título:** “Baja voluntaria de integrante”
- **Destinatarios:** directores activos restantes.
- **Mensaje:** incluye el nombre del integrante, el proyecto y el motivo.
- **Entidad:** `PROYECTO` con el `idProyecto`.

Si el usuario que se dio de baja era Director, no recibe la notificación; la
reciben los demás directores.

---

## 11. Checklist de aceptación

- [ ] La acción se muestra solo si `puedeDarseDeBaja === true`.
- [ ] El modal exige un motivo no vacío de hasta 100 caracteres.
- [ ] El request usa `POST /proyectos/{idProyecto}/baja` con body `{ "motivo": "..." }`.
- [ ] Un `204` cierra el modal, muestra éxito y refresca proyectos propios.
- [ ] Se redirige fuera del detalle si el proyecto es privado.
- [ ] Los errores `400`, `403`, `404` y `409` se muestran sin cerrar el modal.
- [ ] El botón no se muestra para el último Director.
- [ ] Las actividades `EN_CURSO`, `FINALIZADA` y `CANCELADA` muestran históricos con `activo = false`.
- [ ] Las actividades `PENDIENTE` no muestran históricos.
- [ ] Los históricos no se cuentan como personal actual.
- [ ] La notificación `INTEGRANTE_BAJA_VOLUNTARIA` no navega automáticamente a un proyecto privado.
