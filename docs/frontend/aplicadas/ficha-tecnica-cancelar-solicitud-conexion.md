# Ficha técnica — Cancelar solicitud de conexión (extensión UC-43)

> **Estado:** backend implementado.
> **Rama:** `modulo-conexiones`.
> **Caso de uso base:** UC-43 (Enviar solicitud de conexión).

## 1. Objetivo

Permitir que el perfil activo que envió una solicitud de conexión en estado
`PENDIENTE` la cancele. La solicitud se elimina físicamente, se borra la
notificación de solicitud recibida del destinatario y el par de perfiles queda
libre para volver a enviar una solicitud.

## 2. Endpoint

### `DELETE /solicitudes-conexion/{idSolicitudConexion}`

Requiere sesión con rol `USUARIO` y perfil activo seleccionado.

**Path param**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `idSolicitudConexion` | number | ID de la solicitud enviada por el perfil activo |

**Response `204 No Content`**

No devuelve body.

## 3. Reglas de negocio

- Sólo puede cancelar el **perfil remitente** de la solicitud.
- La solicitud debe seguir en estado `PENDIENTE`.
- El perfil activo debe estar `ACTIVO` y pertenecer al usuario autenticado.
- La cancelación elimina la fila de `solicitud_conexion`.
- Se elimina la notificación `SOLICITUD_CONEXION_RECIBIDA` asociada a esa solicitud.
- Después de cancelar, el perfil puede volver a enviar una solicitud al mismo
  destinatario.
- No se notifica al destinatario sobre la cancelación; simplemente deja de ver
  la notificación de la solicitud recibida.

## 4. Errores

| HTTP | `codigo` | Cuándo |
|---|---|---|
| `403` | `SOLICITUD_CONEXION_NO_PERMITIDA` | La solicitud pertenece a otro perfil |
| `404` | — | La solicitud no existe o no hay perfil activo |
| `409` | `SOLICITUD_CONEXION_NO_PENDIENTE` | La solicitud ya fue aceptada o rechazada |

## 5. Flujo sugerido en el frontend

1. En la vista de solicitudes enviadas, mostrar la acción “Cancelar” sólo para
   solicitudes en estado `PENDIENTE`.
2. Pedir confirmación al usuario.
3. Ejecutar `DELETE /solicitudes-conexion/{id}`.
4. Si responde `204`, quitar la solicitud del listado y refrescar el detalle del
   perfil destino (`estadoConexion` vuelve a `NINGUNA`).
5. Si responde `409 SOLICITUD_CONEXION_NO_PENDIENTE`, refrescar el listado: la
   solicitud ya fue gestionada por el destinatario.
6. Si responde `403` o `404`, refrescar el listado y volver al estado seguro.

## 6. Consideraciones

- El envío de la solicitud y su cancelación usan el mismo recurso
  `/solicitudes-conexion`.
- La cancelación no genera una notificación nueva.
- La cancelación no requiere token CSRF adicional; usa el mismo esquema que el
  resto de los `DELETE` autenticados.
- Si el usuario tiene varios perfiles, sólo puede cancelar las solicitudes
  enviadas por el **perfil activo** en la sesión.
