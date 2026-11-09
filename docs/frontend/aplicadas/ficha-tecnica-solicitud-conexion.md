# Ficha técnica — Solicitud de conexión (UC-43)

> **Estado:** backend implementado.
> **Rama:** `modulo-conexiones`.
> **Casos de uso relacionados:** UC-43 (implementado), UC-44, UC-45 y UC-46 (esquema preparado).

## 1. Objetivo

Permitir que el perfil activo envíe una solicitud de conexión a otro perfil
registrado y activo, y que el frontend muestre la acción correcta según la
relación existente.

## 2. Endpoint

### `POST /solicitudes-conexion`

Requiere sesión con rol `USUARIO` y perfil activo seleccionado. El remitente
sale del JWT; el body sólo indica el perfil destinatario.

**Request**

```json
{
  "idPerfilDestinatario": 42
}
```

**Response `201 Created`**

```json
{
  "idSolicitudConexion": 100,
  "estado": "PENDIENTE",
  "fechaEnvio": "2026-11-08T12:00:00",
  "fechaRespuesta": null,
  "remitente": {
    "idPerfil": 7,
    "nombreArtistico": "Ana Moda",
    "nombre": "Ana",
    "apellido": "Pérez",
    "idProfesion": 3,
    "nombreProfesion": "Diseñadora"
  },
  "destinatario": {
    "idPerfil": 42,
    "nombreArtistico": "Juan Estilo",
    "nombre": "Juan",
    "apellido": "Gómez",
    "idProfesion": 5,
    "nombreProfesion": "Fotógrafo"
  }
}
```

## 3. Estado de la relación en el perfil

`GET /perfiles/{idPerfil}` incorpora tres campos nuevos:

| Campo | Tipo | Descripción |
|---|---|---|
| `estadoConexion` | string | `NINGUNA`, `SOLICITUD_ENVIADA`, `SOLICITUD_RECIBIDA` o `CONECTADO` |
| `idSolicitudConexion` | number \| null | ID de la solicitud pendiente, cuando corresponde |
| `idConexion` | number \| null | ID de la conexión vigente, cuando corresponde |

### Acción sugerida por estado

| `estadoConexion` | Acción en el perfil |
|---|---|
| `NINGUNA` | Mostrar “Conectar” |
| `SOLICITUD_ENVIADA` | Mostrar “Solicitud enviada” y deshabilitar el envío |
| `SOLICITUD_RECIBIDA` | Mostrar “Aceptar” / “Rechazar” |
| `CONECTADO` | Mostrar “Eliminar conexión” |

Para el perfil propio, el backend devuelve `NINGUNA` y los IDs en `null`.

## 4. Errores

| HTTP | `codigo` | Cuándo |
|---|---|---|
| `400` | — | Body inválido o falta `idPerfilDestinatario` |
| `403` | `ROL_NO_PERMITIDO` | El usuario no tiene rol `USUARIO` |
| `404` | — | No hay perfil activo, o el perfil destinatario no existe/no es visible |
| `409` | `SOLICITUD_CONEXION_INVALIDA` | Autorequest o perfiles del mismo usuario |
| `409` | `SOLICITUD_CONEXION_PENDIENTE` | Ya existe una solicitud pendiente en cualquier dirección |
| `409` | `CONEXION_YA_EXISTENTE` | Ya existe una conexión activa |

## 5. Notificaciones

Al crear la solicitud se genera una notificación para el usuario dueño del
perfil destinatario:

- `tipo`: `SOLICITUD_CONEXION_RECIBIDA`
- `titulo`: “Solicitud de conexión recibida”
- `entidadTipo`: `SOLICITUD_CONEXION`
- `entidadId`: ID de la solicitud
- `idPerfilContexto`: perfil destinatario
- `leida`: `false`

El frontend puede usar `entidadTipo` y `entidadId` para navegar al listado de
solicitudes. La ruta exacta queda a definir con el equipo de frontend.

## 6. Consideraciones de UI

- El botón “Conectar” debe ocultarse o deshabilitarse si el perfil consultado
  es el perfil activo.
- Si el usuario tiene varios perfiles, la solicitud se envía siempre desde el
  perfil activo. Mostrar el nombre artístico del remitente en el modal de
  confirmación ayuda a evitar confusiones.
- Si el backend responde `409 SOLICITUD_CONEXION_PENDIENTE`, refrescar el
  detalle del perfil para actualizar `estadoConexion`.
- Si responde `409 CONEXION_YA_EXISTENTE`, refrescar el detalle y mostrar el
  estado `CONECTADO`.
- El envío debe enviar el token CSRF correspondiente, como el resto de los
  `POST` de la aplicación.

## 7. Fuera de esta entrega

- Pantalla de solicitudes recibidas/enviadas (UC-46).
- Aceptar/rechazar solicitudes (UC-45).
- Eliminar conexión (UC-44).
- Listado de la red de contactos.
