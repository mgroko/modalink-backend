# Ficha técnica — Eliminar conexión (UC-44)

> **Estado:** backend implementado.
> **Rama:** `modulo-conexiones`.
> **Casos de uso relacionados:** UC-43 (envío/cancelación), UC-44 bis (listar red), UC-45 (gestión de solicitudes).

## 1. Objetivo

Permitir que un perfil elimine una conexión de su red de contactos. La baja es
**lógica**: la conexión se conserva con su historial y deja de mostrarse en la
red activa.

## 2. Endpoint

### `DELETE /conexiones/{idConexion}`

Requiere sesión con rol `USUARIO` y perfil activo seleccionado.

### Response `204 No Content`

Sin cuerpo.

### Ejemplo

```http
DELETE /conexiones/55
```

## 3. Semántica

- Sólo puede eliminar un perfil que participa de la conexión.
- La conexión pasa de `ACTIVA` a `ELIMINADA`.
- Se registra `fechaFinConexion` (la completa el servicio y el trigger si llega nula).
- La solicitud de origen permanece `ACEPTADA`; no se modifica.
- La conexión deja de aparecer en `GET /conexiones`.
- Se crean notificaciones `CONEXION_ELIMINADA` con `entidadTipo = CONEXION` y
  `entidadId = idConexion`:
  - para el perfil que ejecuta la acción: “Eliminaste la conexión con '…'”;
  - para la contraparte: “La conexión con '…' fue eliminada”.
- Sólo reciben notificación usuarios `ACTIVO`. Las contrapartes en
  `PENDIENTE_BAJA`, `BAJA` o `DESHABILITADO` se omiten.
- El `link` de la notificación es `/mi-red?tab=conexiones`.
- El trigger `trg_auditar_conexion` registra `CONEXION_ELIMINADA` con acción
  `UPDATE` y los valores anterior/nuevo.
- La fila se bloquea con `PESSIMISTIC_WRITE` para que dos eliminaciones
  simultáneas no apliquen la baja dos veces.

## 4. Errores

| HTTP | `codigo` | Cuándo |
|---|---|---|
| `404` | — | La conexión no existe o el perfil activo no participa de ella |
| `409` | `CONEXION_NO_ACTIVA` | La conexión ya estaba `ELIMINADA` |
| `404` | — | No hay perfil activo en la sesión |

## 5. Flujo sugerido en el frontend

1. En cada tarjeta de la pestaña **Conexiones**, ofrecer “Eliminar conexión”.
2. Pedir confirmación al usuario.
3. Enviar `DELETE /conexiones/{idConexion}`.
4. Si responde `204`, quitar la tarjeta del listado y refrescar el contador.
5. Si responde `409 CONEXION_NO_ACTIVA`, informar que la conexión ya fue
   eliminada y refrescar el listado.
6. Si responde `404`, informar que la conexión no está disponible.

## 6. Consideraciones

- La baja lógica preserva el historial y permite auditoría; no se borra la fila
  de `conexion`.
- La eliminación no vuelve a habilitar el envío automáticamente desde la UI: el
  par queda libre para una nueva solicitud (UC-43) porque el índice único de
  conexión activa sólo aplica a `ACTIVA`.
- El trigger `fn_validar_conexion` valida perfiles y usuarios activos únicamente
  al crear la conexión; una baja es válida aunque la contraparte ya esté inactiva.
- La migración asociada es
  `docs/3_diseño/migraciones/2026-11-09_uc44_eliminar_conexion.sql`.
