# Ficha técnica — Gestionar solicitudes de conexión (UC-45)

> **Estado:** backend implementado.
> **Rama:** `modulo-conexiones`.
> **Casos de uso relacionados:** UC-43 (envío/cancelación) y UC-46 (búsqueda).

## 1. Objetivo

Permitir que el perfil destinatario de una solicitud de conexión `PENDIENTE`
la acepte o la rechace.

## 2. Endpoints

### Aceptar

```http
POST /solicitudes-conexion/{idSolicitudConexion}/aceptar
```

Sin body.

**Response `200 OK`**

```json
{
  "idSolicitudConexion": 100,
  "estado": "ACEPTADA",
  "fechaRespuesta": "2026-11-09T10:30:00",
  "idConexion": 55
}
```

### Rechazar

```http
POST /solicitudes-conexion/{idSolicitudConexion}/rechazar
```

Sin body.

**Response `200 OK`**

```json
{
  "idSolicitudConexion": 100,
  "estado": "RECHAZADA",
  "fechaRespuesta": "2026-11-09T10:30:00",
  "idConexion": null
}
```

## 3. Errores

| HTTP | `codigo` | Cuándo |
|---|---|---|
| `403` | `SOLICITUD_CONEXION_NO_PERMITIDA` | El perfil activo no es el destinatario |
| `404` | — | La solicitud no existe o fue cancelada |
| `404` | — | No hay perfil activo en la sesión |
| `409` | `SOLICITUD_CONEXION_NO_PENDIENTE` | La solicitud ya fue aceptada o rechazada |
| `409` | `CONEXION_YA_EXISTENTE` | Ya existe una conexión activa entre ambos perfiles |
| `409` | — | El remitente, destinatario o usuario no está activo |

## 4. Reglas de negocio

- Solo el perfil **destinatario** puede aceptar o rechazar.
- Solo se gestionan solicitudes `PENDIENTE`.
- Ambos perfiles y usuarios deben estar `ACTIVO`.
- Aceptar:
  - crea una conexión `ACTIVA`;
  - asigna la fecha de inicio de conexión;
  - notifica a ambas partes;
  - genera auditoría `CONEXION_CREADA`.
- Rechazar:
  - no crea conexión;
  - notifica **solo al remitente**;
  - no genera auditoría.
- La notificación original `SOLICITUD_CONEXION_RECIBIDA` se conserva como historial.

## 5. Notificaciones y navegación

El backend devuelve el campo `link` en las notificaciones. Las rutas acordadas
para el frontend son:

| Tipo de notificación | Destinatario | `link` |
|---|---|---|
| `SOLICITUD_CONEXION_RECIBIDA` | Perfil destinatario | `/solicitudes-conexion?direccion=RECIBIDA` |
| `SOLICITUD_CONEXION_ACEPTADA` | Remitente y destinatario | `/red-contactos?idConexion={idConexion}` |
| `SOLICITUD_CONEXION_RECHAZADA` | Solo remitente | `/solicitudes-conexion?direccion=ENVIADA&estado=RECHAZADA` |
| `CONEXION_ELIMINADA` | Ambas partes (UC-44) | `/red-contactos` |

### Notificaciones al aceptar

| Destino | Tipo | `entidadTipo` | `entidadId` |
|---|---|---|---|
| Perfil remitente | `SOLICITUD_CONEXION_ACEPTADA` | `CONEXION` | `idConexion` |
| Perfil destinatario | `SOLICITUD_CONEXION_ACEPTADA` | `CONEXION` | `idConexion` |

### Notificación al rechazar

| Destino | Tipo | `entidadTipo` | `entidadId` |
|---|---|---|---|
| Perfil remitente | `SOLICITUD_CONEXION_RECHAZADA` | `SOLICITUD_CONEXION` | `idSolicitudConexion` |

El destinatario no recibe una notificación nueva al rechazar.

## 6. Flujo sugerido en el frontend

1. En el listado de UC-46, mostrar “Aceptar” y “Rechazar” cuando
   `puedeAceptar` y `puedeRechazar` sean `true`.
2. Pedir confirmación antes de ejecutar la acción.
3. Ejecutar el `POST` correspondiente.
4. Con la respuesta:
   - actualizar la fila a `ACEPTADA` o `RECHAZADA`;
   - si `idConexion` no es `null`, permitir navegar a `/red-contactos`;
   - refrescar el contador de notificaciones.
5. Si responde `409 SOLICITUD_CONEXION_NO_PENDIENTE`, refrescar el listado.
6. Si responde `409 CONEXION_YA_EXISTENTE`, refrescar y mostrar el estado de
   conexión actualizado.

## 7. Consideraciones

- Las rutas de navegación deben estar registradas en el router del frontend.
- El backend calcula el `link` al listar notificaciones, por lo que también
  aplica a notificaciones históricas.
- La auditoría de conexiones es automática por trigger; el frontend no debe
  enviar ningún dato adicional.
- Para UC-44, la baja lógica de una conexión generará `CONEXION_ELIMINADA` y
  la notificación correspondiente.
