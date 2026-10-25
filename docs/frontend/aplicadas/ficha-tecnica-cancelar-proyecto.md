# Ficha Técnica Frontend: Cancelar Proyecto (UC-35)

Especificación de la API y de la UI para cancelar un proyecto en estado
`Publicado` o `Confirmado`. Incluye el motivo obligatorio, efectos colaterales,
visibilidad posterior, errores y recomendaciones de integración para el dashboard
del proyecto (`/proyectos/:id`) y el catálogo de proyectos propios.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `POST`
* **URL:** `/proyectos/{idProyecto}/cancelar`
* **Path param:** `idProyecto` (`long`, obligatorio)
* **Query params:** ninguno
* **Content-Type:** `application/json`
* **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`).
* **Perfil activo requerido:** Sí. El backend resuelve `idPerfilActivo` desde la
  sesión; el frontend **no** lo envía en el body ni en la URL.
* **Permisos:** el perfil activo debe ser **Director activo** del proyecto y tener
  el permiso `CANCELAR_PROYECTO`.
* **Respuesta exitosa:** `200 OK` con el `ProyectoResponse` completo y actualizado
  (mismo contrato que `GET /proyectos/{idProyecto}`).

---

## 2. Precondiciones de Estado

| Estado del proyecto | ¿Se puede cancelar? | Resultado si se intenta |
| :--- | :---: | :--- |
| `Borrador` | No | `409 Conflict` |
| `Publicado` | **Sí** | `200 OK` |
| `Confirmado` | **Sí** | `200 OK` |
| `Finalizado` | No | `409 Conflict` |
| `Cancelado` | No | `409 Conflict` |

> La cancelación **no es idempotente**: un segundo intento sobre el mismo proyecto
> responde `409 Conflict`.

---

## 3. Request

### 3.1 Payload

```json
{
  "motivoCancelacion": "Falta de presupuesto para continuar con la producción."
}
```

### 3.2 Matriz de campos

| Campo | Tipo | Obligatorio | Reglas backend | Notas UI/UX |
| :--- | :--- | :---: | :--- | :--- |
| `motivoCancelacion` | `string` | **Sí** | No puede ser vacío ni contener solo espacios. Máximo **200 caracteres**. Se persiste y devuelve **sin espacios al inicio/final**. | Textarea con contador `(x/200)`, `maxlength="200"` y validación antes de habilitar la confirmación. |

---

## 4. Efectos de la Cancelación

La operación es transaccional. Si una parte falla, no se aplica ningún cambio.

1. El proyecto pasa a estado `Cancelado` y se guarda el motivo normalizado.
2. Las actividades en estado `PENDIENTE` o `EN_CURSO` pasan a `CANCELADA`.
   Las actividades `FINALIZADA` se conservan.
3. Los bloqueos de agenda derivados de esas actividades dejan de mostrarse
   automáticamente, porque el calendario solo calcula bloqueos de proyectos activos.
4. Las postulaciones **pendientes** de proyecto y de actividad pasan a `RECHAZADA`.
   Las postulaciones ya `ACEPTADA` no se modifican.
5. Las invitaciones **pendientes** de proyecto y de actividad pasan a `RECHAZADA`.
   Las invitaciones ya `ACEPTADA` no se modifican.
6. Se registran notificaciones para:
   * los integrantes activos del equipo (deduplicados por usuario),
   * los postulantes pendientes rechazados,
   * los destinatarios de invitaciones pendientes rechazadas.

> En esta iteración el backend **no expone un endpoint de lectura de notificaciones**.
> El frontend no debe consultar `/notificaciones` todavía.

---

## 5. Visibilidad Posterior a la Cancelación

| Escenario | Comportamiento |
| :--- | :--- |
| Búsqueda global (`GET /proyectos/buscar`) | El proyecto cancelado **no aparece**, ni siquiera para sus miembros. |
| Proyectos propios (`GET /proyectos/propios`) | **Sí aparece** para quien fue miembro, incluso si luego quedó como exmiembro. |
| Detalle (`GET /proyectos/{id}`) | Visible para miembros activos y para **exmiembros** del proyecto cancelado. Un no miembro que nunca participó recibe `403`. |
| `elegibleParaPostularse` | Siempre `false` para proyectos cancelados. |
| Acciones de edición/publicación/cancelación | Deben ocultarse o deshabilitarse en el frontend por estado. |

> El campo `permisos` puede seguir incluyendo permisos de director después de la
> cancelación, porque pertenecen al rol y no al estado. **Para decidir qué acciones
> mostrar, combinar siempre `permisos` con `estado`.**

---

## 6. Respuesta Exitosa (`200 OK`)

Ejemplo de respuesta para el director que acaba de cancelar el proyecto:

```json
{
  "idProyecto": 105,
  "nombre": "Colección Madoka",
  "descripcion": "Colección cápsula de verano inspirada en tonos tierra y texturas naturales.",
  "fechaInicio": "2026-06-01",
  "fechaEntregaEstipulada": "2027-03-15",
  "estado": "Cancelado",
  "privacidad": "PUBLICO",
  "aceptaPostulacionGral": true,
  "ubicacion": {
    "idUbicacion": 50,
    "direccion": null,
    "codigoPostal": null,
    "latitud": -27.3676,
    "longitud": -55.8989,
    "ciudad": {
      "idCiudad": 12,
      "idExterno": "3300801",
      "fuenteApi": "GEOREF",
      "nombre": "Posadas",
      "provincia": {
        "idProvincia": 3,
        "idExterno": "54",
        "fuenteApi": "GEOREF",
        "nombre": "Misiones",
        "pais": { "idPais": 1, "codigoIso": "AR", "nombre": "Argentina" }
      }
    }
  },
  "idDirector": 4,
  "nombreDirector": "Luna Diseños",
  "objetivos": [
    {
      "idObjetivo": 1,
      "nombre": "Presentar la colección en pasarela",
      "descripcion": "Desfile de cierre con 12 looks."
    }
  ],
  "requerimientosGral": [
    {
      "idRequerimientoGral": 10,
      "cantidad": 2,
      "descripcion": "Vacante para desfile.",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "caracteristicas": [],
      "habilidades": [],
      "satisfecho": false
    }
  ],
  "moodboard": {
    "idMoodboard": 77,
    "descripcion": "Paleta cálida de arena y terracota.",
    "fechaCreacion": "2026-06-01T10:00:00",
    "imagenes": []
  },
  "rolUsuario": "DIRECTOR",
  "esMiembro": true,
  "permisos": [
    "ASIGNAR_ACTIVIDAD",
    "CANCELAR_PROYECTO",
    "CONFIRMAR_PROYECTO",
    "CREAR_ACTIVIDAD",
    "ELIMINAR_ACTIVIDAD",
    "ELIMINAR_INTEGRANTE",
    "ELIMINAR_INVITACION",
    "FINALIZAR_PROYECTO",
    "GESTIONAR_POSTULACIONES",
    "INVITAR_MIEMBRO",
    "MODIFICAR_ACTIVIDAD",
    "MODIFICAR_PROYECTO",
    "PUBLICAR_PROYECTO",
    "VER_PROYECTO"
  ],
  "elegibleParaPostularse": false,
  "motivoCancelacion": "Falta de presupuesto para continuar con la producción."
}
```

> La cancelación **no elimina** la ubicación, los objetivos, los requerimientos ni
> el moodboard del proyecto. El ejemplo conserva esos datos; solo cambian el estado,
> el motivo, las actividades, las postulaciones/invitaciones pendientes y las
> notificaciones.

### 6.1 Campo `motivoCancelacion`

Se agregó al final de `ProyectoResponse`:

| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `motivoCancelacion` | `string` \| `null` | Motivo ingresado al cancelar. Es `null` para proyectos que no están cancelados. |

Este campo está presente en:

* `GET /proyectos/{idProyecto}`
* `POST /proyectos`
* `POST /proyectos/{idProyecto}/publicar`
* `PUT /proyectos/{idProyecto}`

> El listado `GET /proyectos/propios` usa otro DTO (`ProyectoPropioResponse`) y no
> incluye `motivoCancelacion`. Actualizar el typedef/contrato del frontend y
> contemplar `null` en todas las vistas que rendericen el detalle del proyecto.

### 6.2 Caso exmiembro

Un exmiembro que abre el detalle de un proyecto cancelado recibe el proyecto con:

```json
{
  "...": "mismos campos del proyecto",
  "estado": "Cancelado",
  "motivoCancelacion": "Fin del proyecto",
  "rolUsuario": null,
  "esMiembro": false,
  "permisos": [],
  "elegibleParaPostularse": false
}
```

El acceso es válido: no tratar `esMiembro: false` como `403` en la vista de detalle.

---

## 7. Manejo de Errores

### 7.1 Formato general

```json
{
  "message": "Solo se puede cancelar un proyecto en estado Publicado o Confirmado. Estado actual: Borrador.",
  "httpStatus": 409,
  "timestamp": 1760000000000
}
```

### 7.2 Errores de validación (`400`)

```json
{
  "message": "Validación fallida",
  "errores": {
    "motivoCancelacion": "El motivo de cancelación es obligatorio."
  },
  "httpStatus": 400,
  "timestamp": 1760000000000
}
```

### 7.3 Tabla de errores

| HTTP | Caso | Origen / Mensaje | Acción frontend |
| :---: | :--- | :--- | :--- |
| `400` | Motivo vacío o ausente | `El motivo de cancelación es obligatorio.` | Mostrar error sobre el textarea; mantener el modal abierto. |
| `400` | Motivo mayor a 200 caracteres | `El motivo de cancelación no puede superar los 200 caracteres.` | Validar con `maxlength` y contador. |
| `403` | Sin permiso o sin membresía activa | `No tienes el permiso 'CANCELAR_PROYECTO' en este proyecto o no eres un miembro activo.` | Mostrar aviso; refrescar el proyecto. |
| `403` | No es director activo | `Debes ser Director activo del proyecto para realizar esta acción.` | Ocultar la acción si el rol no es director. |
| `404` | Sin perfil activo | `No hay un perfil activo seleccionado en la sesión.` | Abrir el flujo de selección de perfil. |
| `404` | Perfil no encontrado | `El perfil no fue encontrado` | Aviso y volver a seleccionar perfil. |
| `404` | Proyecto inexistente | `El proyecto con id {id} no existe.` | Estado "proyecto no encontrado" con botón volver. |
| `409` | Estado no cancelable | `Solo se puede cancelar un proyecto en estado Publicado o Confirmado. Estado actual: {estado}.` | Cerrar el modal, mostrar el mensaje y refrescar el proyecto. |
| `409` | Perfil en baja | `El perfil no se encuentra activo.` | Aviso de perfil inactivo. |
| `401` | Sesión ausente o inválida | Seguridad | Redirigir a login (interceptor `http.js`). |
| `≥ 500` | Error inesperado | — | Mensaje genérico y opción de reintentar. |

---

## 8. Consideraciones de UI/UX

### 8.1 Cuándo mostrar la acción

Mostrar el botón **Cancelar proyecto** solo si se cumple todo:

```js
permisos.includes('CANCELAR_PROYECTO') &&
['PUBLICADO', 'CONFIRMADO'].includes(normalizarEstadoProyecto(proyecto.estado))
```

No mostrar la acción para `Borrador`, `Finalizado` ni `Cancelado`.

### 8.2 Modal de confirmación

1. Mostrar el nombre del proyecto y una advertencia clara:
   *"Esta acción cancelará el proyecto y no puede deshacerse."*
2. Textarea obligatorio para el motivo, con:
   * `maxlength="200"`,
   * contador `(x/200)`,
   * `trim()` antes de enviar,
   * botón de confirmación deshabilitado si está vacío.
3. Botón de confirmación con estado `loading` mientras se ejecuta la petición.
4. No cerrar el modal ante errores `400`; mostrar el mensaje y conservar el texto.

### 8.3 Después de un `200`

1. Reemplazar el estado local con la respuesta del endpoint (no hace falta otro `GET`).
2. Cerrar el modal y mostrar un toast de éxito.
3. En el dashboard:
   * mostrar un banner o sección **"Proyecto cancelado"** con `motivoCancelacion`,
   * ocultar/deshabilitar **Editar**, **Publicar** y **Cancelar**,
   * ocultar **Postularme**,
   * mantener visible el resto de la información como historial.
4. En listados, mostrar el badge `Cancelado`.
5. Si el usuario vuelve a **Búsqueda global**, el proyecto ya no debe aparecer; en
   **Proyectos propios** debe seguir apareciendo.

### 8.4 Después de un `409`

1. Mostrar el `message` del backend.
2. Refrescar el proyecto con `GET /proyectos/{idProyecto}`.
3. Cerrar el modal si el nuevo estado ya no permite cancelar.

### 8.5 Manejo de `motivoCancelacion` en la vista de detalle

* Si `normalizarEstadoProyecto(proyecto.estado) === 'CANCELADO'` y
  `motivoCancelacion` tiene valor, mostrarlo en un bloque destacado.
* Si es `null` o vacío, mostrar un texto de respaldo del tipo
  *"Sin motivo registrado"* (solo para datos históricos).

---

## 9. Integración Sugerida

### 9.1 Servicio

Agregar en `src/services/proyectoService.js`:

```js
cancelar(idProyecto, motivoCancelacion) {
  return http.post(`/proyectos/${idProyecto}/cancelar`, {
    motivoCancelacion: motivoCancelacion.trim(),
  })
}
```

### 9.2 Constantes y utilidades a reutilizar

* `normalizarEstadoProyecto()` para comparar estados.
* `EstadoProyectoBadge.vue` para el badge `Cancelado`.
* `BaseAlert` o el patrón de alertas existente para el banner de cancelación.
* `useToast()` (vue-toastification) para éxito/error.
* Patrón de `AbortController` y retry solo si la implementación del dashboard ya lo usa.

### 9.3 No enviar

* `idPerfilActivo`
* `idUsuario`
* `estado`
* `motivo` con otro nombre de campo

El único campo del body es `motivoCancelacion`.

---

## 10. Checklist de Aceptación

- [ ] El botón solo aparece con permiso `CANCELAR_PROYECTO` y estado `Publicado` o `Confirmado`.
- [ ] El modal exige un motivo no vacío de hasta 200 caracteres.
- [ ] El request usa exactamente `{ "motivoCancelacion": "..." }`.
- [ ] Un `200` actualiza el proyecto local con `estado: "Cancelado"` y el motivo.
- [ ] El dashboard muestra el motivo y oculta las acciones incompatibles.
- [ ] El proyecto desaparece de la búsqueda global y permanece en proyectos propios.
- [ ] Un exmiembro puede abrir el detalle del proyecto cancelado.
- [ ] Los errores `400`, `403`, `404` y `409` se muestran sin cerrar el modal.
- [ ] El contrato contempla `motivoCancelacion: null` para proyectos no cancelados.
