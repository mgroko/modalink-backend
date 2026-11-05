# Ficha Técnica Frontend: Eliminar Invitación a Proyecto (UC-32)

Especificación de la API, confirmación y manejo de errores para que el Director
que envió una invitación pendiente pueda eliminarla.

No modifica fichas técnicas existentes.

---

## 1. Endpoints y autenticación

### Invitación general

- **Método:** `DELETE`
- **URL:** `/proyectos/{idProyecto}/invitaciones/general/{idInvitacion}`
- **Respuesta exitosa:** `204 No Content`.

### Invitación por actividad

- **Método:** `DELETE`
- **URL:** `/proyectos/{idProyecto}/invitaciones/actividad/{idInvitacion}`
- **Respuesta exitosa:** `204 No Content`.

### Común

- **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header
  `X-XSRF-TOKEN`).
- **Perfil activo:** obligatorio en la sesión. El frontend no envía
  `idPerfilActivo`.
- **Permiso efectivo:** `ELIMINAR_INVITACION` en el proyecto.
- **Rol:** Director activo del proyecto.
- **Remitente:** el perfil activo debe ser quien envió la invitación.
- **Estado:** sólo `PENDIENTE`.
- **Borrado:** físico, sin notificación al destinatario.

---

## 2. Flujo de pantalla

### 2.1 Entrada

Desde el listado de UC-30:

1. El Director visualiza una invitación enviada y pendiente.
2. El ítem debe tener `puedeEliminar = true`.
3. Se muestra la acción **Eliminar invitación**.

No ofrecer la acción si:

- `puedeEliminar` es `false`.
- La invitación está `ACEPTADA` o `RECHAZADA`.
- El ítem corresponde a una invitación recibida.

### 2.2 Confirmación

Antes de enviar el `DELETE`, mostrar un modal con:

- Proyecto.
- Tipo de invitación.
- Perfil destinatario.
- Fecha de envío.
- Mensaje, si existe.

El modal debe pedir confirmación explícita. No eliminar con un solo clic.

### 2.3 Baja

1. El Director confirma.
2. El frontend envía el `DELETE` según el `tipo` de la invitación.
3. Con `204`, cerrar el modal, mostrar confirmación y refrescar el listado.
4. La invitación debe desaparecer del listado de UC-30.

---

## 3. Selección del endpoint

| `tipo` de UC-30 | Endpoint |
|---|---|
| `GENERAL` | `/proyectos/{idProyecto}/invitaciones/general/{idInvitacion}` |
| `ACTIVIDAD` | `/proyectos/{idProyecto}/invitaciones/actividad/{idInvitacion}` |

En ambos casos se usa el `idProyecto` y el `idInvitacion` devueltos por UC-30.

---

## 4. Errores

| Caso | HTTP | Acción en frontend |
|---|---|---|
| Sin perfil activo | `404` | Redirigir a selección de perfil. |
| Invitación inexistente | `404` | Cerrar el modal, refrescar el listado y mostrar que ya no existe. |
| Invitación de otro proyecto | `404` | Refrescar el listado. |
| Sin permiso `ELIMINAR_INVITACION` | `403` | Ocultar la acción; no reintentar. |
| El perfil no es Director | `403` | Ocultar la acción; no reintentar. |
| El perfil no es el remitente | `403` | Ocultar la acción; no reintentar. |
| Invitación no pendiente | `409` | Refrescar el listado y mostrar el estado actual. |
| Error inesperado | `5xx` | Mostrar error genérico y permitir reintentar. |

Para `409`, el backend devuelve el código `INVITACION_NO_PENDIENTE`.

---

## 5. Checklist de aceptación

- [ ] Sólo se muestra “Eliminar” cuando `puedeEliminar = true`.
- [ ] No se muestra para invitaciones recibidas.
- [ ] No se muestra para invitaciones aceptadas o rechazadas.
- [ ] Se muestra un modal de confirmación con los datos de la invitación.
- [ ] Se usa el endpoint correcto según `tipo`.
- [ ] El botón se deshabilita durante la petición.
- [ ] Con `204` se cierra el modal y se refresca el listado.
- [ ] Un `404` refresca el listado y no reintenta.
- [ ] Un `403` no se reintenta.
- [ ] Un `409` refresca el estado y no insiste con el borrado.
- [ ] La invitación eliminada desaparece de UC-30.
- [ ] No se muestra una notificación de eliminación: UC-32 no la genera.
