# Ficha técnica — Eliminar solicitud de colaboración (UC-64)

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-61 (envío), UC-62/63 (aceptar/rechazar),
> UC-65 (búsqueda) y UC-22 (publicaciones).

## 1. Objetivo

Permitir que el perfil activo que envió una solicitud de colaboración en estado
`PENDIENTE` la elimine. La solicitud se borra físicamente, desaparece de la
búsqueda de UC-65 y se elimina la notificación de solicitud recibida del
destinatario.

## 2. Endpoint

```http
DELETE /solicitudes-colaboracion/{idSolicitudColaboracion}
```

Sin body.

**Respuesta `204 No Content`**

No devuelve body.

## 3. Reglas de negocio

- Sólo puede eliminar el **perfil remitente** de la solicitud.
- La solicitud debe seguir en estado `PENDIENTE`.
- El perfil activo debe estar `ACTIVO` y pertenecer al usuario autenticado.
- La baja es **física**: se elimina la fila de `solicitud_colaboracion`.
- Se elimina la notificación `SOLICITUD_COLABORACION_RECIBIDA` del usuario y
  perfil destinatario, asociada a la publicación.
- No se notifica al destinatario sobre la eliminación.
- No se audita, consistente con UC-61/62/63.
- La publicación puede estar `ACTIVA` u `OCULTA`: la eliminación no revalida su
  estado ni la visibilidad de la contraparte.
- Después de eliminar, el índice único parcial queda libre y el perfil puede
  volver a enviar una solicitud al mismo destinatario y publicación.

## 4. Errores

| HTTP | `codigo` | Cuándo |
|---|---|---|
| `403` | `SOLICITUD_COLABORACION_NO_PERMITIDA` | La solicitud pertenece a otro perfil |
| `404` | — | La solicitud no existe o no hay perfil activo |
| `409` | `SOLICITUD_COLABORACION_NO_PENDIENTE` | La solicitud ya fue aceptada o rechazada |

## 5. Notificación

Se elimina la notificación del destinatario:

| Campo | Valor |
|---|---|
| `tipo` | `SOLICITUD_COLABORACION_RECIBIDA` |
| `entidadTipo` | `PUBLICACION` |
| `entidadId` | `idPublicacion` |
| `usuarioDestino` | Usuario del perfil destinatario |
| `perfilContexto` | Perfil destinatario |

El borrado se acota al usuario, perfil y publicación para no afectar a otros
destinatarios invitados a la misma publicación.

## 6. Frontend

1. En la vista de solicitudes enviadas de UC-65, mostrar la acción “Eliminar”
   sólo cuando `puedeEliminar` sea `true`.
2. Pedir confirmación mostrando la publicación y el perfil destinatario.
3. Ejecutar `DELETE /solicitudes-colaboracion/{id}`.
4. Si responde `204`, quitar la solicitud del listado y refrescar UC-65.
5. Si responde `409 SOLICITUD_COLABORACION_NO_PENDIENTE`, refrescar el listado:
   la solicitud ya fue aceptada o rechazada por el destinatario.
6. Si responde `403` o `404`, refrescar el listado y volver al estado seguro.

## 7. Consideraciones

- La eliminación no agrega ni quita colaboradores de la publicación.
- No se modifica el estado de la publicación.
- No se envía una notificación nueva.
- Si el destinatario tenía abierta la solicitud, al refrescar deja de verla.
- La operación bloquea la fila con lock pesimista para no competir con
  UC-62/63. Si el destinatario acepta primero, la eliminación responde `409`;
  si la eliminación gana, la aceptación posterior responde `404`.
