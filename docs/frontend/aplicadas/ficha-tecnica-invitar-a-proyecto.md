# Ficha Técnica Frontend: Invitar a Proyecto (UC-31)

Especificación de la API, flujo de pantalla, validaciones y manejo de errores para
que un Director de proyecto invite a un perfil al proyecto de forma general o
asociada a un requerimiento de actividad.

---

## 1. Endpoints y autenticación

### Invitación general

- **Método:** `POST`
- **URL:** `/proyectos/{idProyecto}/invitaciones/general`
- **Content-Type:** `application/json`
- **Respuesta exitosa:** `201 Created` con `InvitacionResponse`.

### Invitación por actividad

- **Método:** `POST`
- **URL:** `/proyectos/{idProyecto}/invitaciones/actividad`
- **Content-Type:** `application/json`
- **Respuesta exitosa:** `201 Created` con `InvitacionResponse`.

### Común

- **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header
  `X-XSRF-TOKEN`).
- **Perfil activo:** obligatorio en la sesión. El frontend no envía
  `idPerfilActivo`.
- **Permiso efectivo:** `INVITAR_MIEMBRO` en el proyecto. Hoy lo posee el rol
  **Director de proyecto**.
- **Precondiciones del proyecto:**
  - Estado `Publicado` o `Confirmado`.
  - El perfil activo debe ser Director activo del proyecto.
- **Precondiciones de la actividad:**
  - Estado `Pendiente` o `En curso`.
  - El requerimiento debe pertenecer al proyecto.
  - El perfil destinatario debe tener la profesión requerida.

---

## 2. Cambios de modelo y base de datos

Esta ficha reemplaza cualquier nota previa sobre UC-31 en las fichas de
postulaciones. Las fichas existentes no se modifican; este documento concentra
los cambios propuestos.

| Cambio | Detalle |
|---|---|
| `invitacion_gral.id_proyecto` | Nueva columna obligatoria con FK a `proyecto`. La invitación general se asocia directamente al proyecto. |
| `invitacion_gral.id_requerimiento_gral` | Pasa a ser opcional. Se conserva para invitaciones asociadas a un requerimiento general o registros históricos. |
| `invitacion_gral.mensaje` | Pasa de `varchar(50)` a `varchar(200)`. |
| Índice único parcial | `UQ_invitacion_gral_pendiente` sobre `(id_proyecto, id_perfil_destinatario)` para invitaciones `PENDIENTE`. |
| Índices de actividad | `IDX_invitacion_actividad_destinatario` y `IDX_invitacion_actividad_requerimiento`. |
| Tipo de notificación | Se agrega `INVITACION_RECIBIDA` a `notificacion_tipo` y a los títulos del backend. |
| Migración | `docs/3_diseño/migraciones/2026-11-04_uc31_invitar_proyecto.sql`. |

### Reglas de backend asociadas

- Invitación general: se asocia al proyecto, sin requerimiento.
- Invitación por actividad: se asocia al requerimiento y deriva el proyecto.
- La invitación nace en estado `PENDIENTE`.
- Se notifica únicamente al perfil destinatario.
- El bloqueo pesimista del proyecto evita duplicados concurrentes.
- Se bloquean duplicados pendientes aunque sean de distinto tipo.
- La aceptación de la invitación pertenece a UC-28 y no forma parte de esta
  ficha.

### Documentos de referencia que no se modifican

- `ficha-tecnica-buscar-postulaciones.md`
- `ficha-tecnica-gestionar-postulaciones.md`
- `ficha-tecnica-postulaciones-unificada.md`

La navegación hacia invitaciones debe implementarse desde esta ficha y no
desde las bandejas de postulaciones.

---

## 3. Flujo de pantalla

### 3.1 Entrada

Desde el detalle o la gestión del proyecto, el Director selecciona
**Invitar a proyecto**.

Antes de habilitar la acción, usar `GET /proyectos/{idProyecto}` y comprobar:

- `permisos` contiene `INVITAR_MIEMBRO`.
- `estado` es `Publicado` o `Confirmado`.

Si falta el permiso o el estado no lo permite, ocultar o deshabilitar la acción.

### 3.2 Elección del tipo de invitación

Mostrar dos opciones:

1. **Invitación general**: invita al proyecto sin asociar un requerimiento.
2. **Invitación por actividad**: invita a cubrir un requerimiento de actividad.

### 3.3 Invitación general

1. Buscar el perfil destinatario con `GET /perfiles/buscar`.
2. Mostrar la información del perfil.
3. Pedir un mensaje opcional (máximo 200 caracteres).
4. Confirmar.
5. Enviar `POST /proyectos/{idProyecto}/invitaciones/general`.

### 3.4 Invitación por actividad

1. Buscar el requerimiento de actividad con
   `GET /proyectos/{idProyecto}/requerimientos-actividad/buscar`.
2. Mostrar actividad, profesión, cantidad y características del requerimiento.
3. Buscar el perfil destinatario con `GET /perfiles/buscar`.
4. Mostrar la información del perfil.
5. Pedir un mensaje opcional.
6. Confirmar.
7. Enviar `POST /proyectos/{idProyecto}/invitaciones/actividad`.

### 3.5 Después del alta

Con `201`:

- Cerrar el formulario o modal.
- Mostrar confirmación.
- Refrescar el proyecto y, si corresponde, la bandeja de invitaciones
  (UC-30, todavía no implementado).
- No asumir que la invitación ya fue aceptada: queda en estado `PENDIENTE`.

---

## 4. Request payload

### Invitación general

```json
{
  "idPerfilDestinatario": 10,
  "mensaje": "Te invito a participar del proyecto."
}
```

`mensaje` es opcional y puede enviarse como `null` u omitirse. Si se envía vacío,
el backend lo guarda como `null`.

### Invitación por actividad

```json
{
  "idRequerimientoActividad": 20,
  "idPerfilDestinatario": 10,
  "mensaje": "Te invito a cubrir este requerimiento."
}
```

No enviar `idProyecto`, `idPerfilRemitente`, `estado` ni `fechaEnvio`: los
resuelve el backend.

---

## 5. Response payload

```json
{
  "idInvitacion": 123,
  "tipo": "GENERAL",
  "estado": "PENDIENTE",
  "fechaEnvio": "2026-11-04T12:00:00",
  "idProyecto": 5,
  "idRequerimientoGral": null,
  "idRequerimientoActividad": null,
  "idPerfilRemitente": 11,
  "idPerfilDestinatario": 10,
  "mensaje": "Te invito a participar del proyecto."
}
```

Para una invitación por actividad:

- `tipo` es `ACTIVIDAD`.
- `idRequerimientoActividad` contiene el requerimiento elegido.
- `idRequerimientoGral` es `null`.

---

## 6. Búsqueda de requerimientos de actividad

- **Método:** `GET`
- **URL:** `/proyectos/{idProyecto}/requerimientos-actividad/buscar`
- **Autenticación:** perfil activo.
- **Visibilidad:** misma regla que el proyecto.

Parámetros opcionales:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `idActividad` | `Long` | Filtra por actividad. |
| `idProfesion` | `Long` | Filtra por profesión requerida. |
| `idCaracteristica` | `Long` | Filtra por característica técnica. |
| `idValorCaracteristica` | `Long` | Filtra por valor enumerado. |
| `idsHabilidades` | `Long[]` | Filtra por al menos una habilidad. |
| `page` | `int` | Página, por defecto `0`. |
| `size` | `int` | Tamaño, por defecto `20`. |
| `todos` | `boolean` | Si es `true`, no pagina. |

La respuesta es un `PaginaResponse` con:

```json
{
  "contenido": [
    {
      "idRequerimiento": 20,
      "cantidad": 1,
      "descripcion": "Una modelo",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "idActividad": 40,
      "nombreActividad": "Prueba de vestuario",
      "estadoActividad": "PENDIENTE",
      "idProyecto": 5,
      "nombreProyecto": "Desfile 2026",
      "caracteristicas": [],
      "habilidades": []
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 1,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

---

## 7. Validaciones y errores

| Caso | HTTP | Acción en frontend |
|---|---|---|
| Falta `idPerfilDestinatario` o `idRequerimientoActividad` | `400` | Marcar el campo y no reintentar automáticamente. |
| Mensaje mayor a 200 caracteres | `400` | Mostrar el límite y recortar o impedir el envío. |
| Sin perfil activo | `404` | Redirigir a la selección de perfil. |
| Proyecto inexistente | `404` | Volver al listado de proyectos. |
| Perfil destinatario inexistente | `404` | Refrescar la búsqueda de perfiles. |
| Requerimiento inexistente o archivado | `404` | Refrescar la búsqueda de requerimientos. |
| Sin permiso o no es Director | `403` | Ocultar la acción; no reintentar. |
| Proyecto en estado inválido | `409` | Refrescar el proyecto y ocultar la acción si corresponde. |
| Perfil destinatario inactivo | `409` | Informar y volver a buscar otro perfil. |
| Auto-invitación | `409` | Informar que no puede invitarse a sí mismo. |
| Perfil ya es miembro activo | `409` | Informar y volver a buscar otro perfil. |
| Invitación pendiente duplicada | `409` | Informar que ya existe una invitación activa para ese perfil en el proyecto. |
| Profesión incompatible | `409` | Informar y filtrar la búsqueda por la profesión del requerimiento. |
| Actividad en estado inválido | `409` | Refrescar la actividad y elegir otra. |

El backend devuelve el código `INVITACION_DUPLICADA` en el cuerpo cuando el
conflicto es por duplicado.

---

## 8. Checklist de aceptación

- [ ] La acción sólo se muestra a Directores con `INVITAR_MIEMBRO`.
- [ ] Sólo se habilita en proyectos `Publicado` o `Confirmado`.
- [ ] Se puede elegir invitación general o por actividad.
- [ ] La búsqueda de perfiles usa `GET /perfiles/buscar`.
- [ ] La búsqueda de requerimientos usa
  `GET /proyectos/{idProyecto}/requerimientos-actividad/buscar`.
- [ ] El mensaje es opcional y tiene un contador de 200 caracteres.
- [ ] Se muestra confirmación antes de enviar.
- [ ] El botón se deshabilita durante la petición.
- [ ] Un `409` refresca los datos y no se reintenta automáticamente.
- [ ] Un `403` no se reintenta.
- [ ] Con `201` se muestra la invitación creada en estado `PENDIENTE`.
- [ ] No se muestra la invitación como aceptada: la aceptación pertenece a UC-28.
