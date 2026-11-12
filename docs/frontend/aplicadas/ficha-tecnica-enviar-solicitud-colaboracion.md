# Ficha técnica — Enviar solicitud de colaboración (UC-61)

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-61, UC-62/63 (aceptar/rechazar),
> UC-64 (eliminar solicitud enviada), UC-65 (buscar solicitudes) y
> UC-19/21 (publicaciones).

## 1. Objetivo

Permitir que el perfil activo dueño de una publicación invite a colaborar a
otro perfil que pertenece a su red de contactos. La solicitud queda en estado
`PENDIENTE` y el destinatario recibe una notificación.

UC-61 **no agrega** al colaborador. Solo crea la solicitud; la incorporación
efectiva corresponde a UC-62.

## 2. Endpoint

```http
POST /publicaciones/{idPublicacion}/solicitudes-colaboracion
Content-Type: application/json
```

Body:

```json
{
  "idPerfilDestinatario": 8
}
```

Respuesta `201 Created`:

```json
{
  "idSolicitudColaboracion": 1,
  "estado": "PENDIENTE",
  "fechaEnvio": "2026-11-10T20:30:00",
  "idPublicacion": 99,
  "tituloPublicacion": "Editorial Otoño",
  "remitente": {
    "idPerfil": 7,
    "nombreArtistico": "Luna Diseños",
    "profesion": "Diseñadora",
    "fotoUrl": "/uploads/perfiles/luna.webp"
  },
  "destinatario": {
    "idPerfil": 8,
    "nombreArtistico": "Sol",
    "profesion": "Fotógrafa",
    "fotoUrl": "/uploads/perfiles/sol.webp"
  }
}
```

El remitente y la publicación se resuelven desde la sesión y la ruta. El
cliente no los envía en el body.

## 3. Reglas de negocio

- El perfil activo debe estar `ACTIVO`.
- La publicación debe existir y pertenecer al perfil activo. Si no existe o es
  de otro perfil → `404`.
- La publicación debe estar `ACTIVA`. Si está `OCULTA` →
  `409 PUBLICACION_NO_DISPONIBLE`.
- El destinatario debe existir, estar `ACTIVO` y su usuario debe estar
  `ACTIVO`. Si no → `404`.
- No se puede invitar al mismo perfil ni a otro perfil del mismo usuario.
- Debe existir una `conexion` `ACTIVA` entre remitente y destinatario.
- El destinatario no debe estar ya en `colaborador_publicacion`.
- No debe existir otra solicitud `PENDIENTE` para la misma publicación y
  destinatario.
- Se permite reenviar si la solicitud anterior está `RECHAZADA`.
- La solicitud se crea en estado `PENDIENTE` con la fecha del servidor.
- No se envía mensaje libre: el esquema no tiene columna para eso.

## 4. Notificación

Se crea una notificación para el usuario y perfil del destinatario:

| Campo | Valor |
|---|---|
| `tipo` | `SOLICITUD_COLABORACION_RECIBIDA` |
| `categoria` | `SOCIAL` |
| `prioridad` | `ALTA` |
| `entidadTipo` | `PUBLICACION` |
| `entidadId` | `idPublicacion` |
| `link` | `/publicaciones/{idPublicacion}` |
| `perfilContexto` | Perfil destinatario |
| `origen` | `SERVICIO` |

Mensaje:

```text
El perfil '<nombre artístico del remitente>' te invitó a colaborar en la publicación '<título>'.
```

La notificación se registra en la misma transacción que la solicitud.

## 5. Errores

| HTTP | Código | Caso |
|---|---|---|
| `400` | — | Falta `idPerfilDestinatario` o no es positivo |
| `403` | — | Sin rol `USUARIO` |
| `404` | — | Sin perfil activo, publicación inexistente/ajena, destinatario inexistente o inactivo |
| `409` | `PUBLICACION_NO_DISPONIBLE` | Publicación `OCULTA` |
| `409` | `AUTOINVITACION` | Destinatario = remitente |
| `409` | `MISMO_USUARIO` | Otro perfil del mismo usuario |
| `409` | `DESTINATARIO_FUERA_DE_RED` | Sin conexión activa |
| `409` | `DESTINATARIO_YA_ES_COLABORADOR` | Ya es colaborador |
| `409` | `SOLICITUD_COLABORACION_PENDIENTE` | Ya existe una solicitud pendiente |

## 6. Base de datos

La tabla `solicitud_colaboracion` ya existía. UC-61 agrega:

- check de estados `PENDIENTE`, `ACEPTADA`, `RECHAZADA`;
- check de no autoinvitación;
- índice único parcial:

```sql
CREATE UNIQUE INDEX "UQ_solicitud_colaboracion_pendiente"
    ON solicitud_colaboracion (id_publicacion, id_perfil_destinatario)
    WHERE estado = 'PENDIENTE';
```

- índices por publicación, destinatario y remitente;
- tipo de notificación `SOLICITUD_COLABORACION_RECIBIDA`.

La migración está en:

```text
docs/3_diseño/migraciones/2026-11-10_uc61_solicitud_colaboracion.sql
```

No se agrega `fecha_respuesta` en esta iteración. Queda para UC-62/63.

## 7. Frontend

- Desde el detalle o la edición de una publicación propia, ofrecer “Invitar a
  colaborar”.
- Usar UC-16 (`GET /perfiles/{idPerfil}`) o la red de contactos para mostrar el
  perfil y pedir confirmación.
- Enviar el `POST` con `idPerfilDestinatario`.
- Mostrar los códigos de error devueltos para explicar por qué no se puede
  invitar.
- Tras el `201`, actualizar el estado de la publicación o mostrar un mensaje de
  solicitud enviada.
- La notificación del destinatario navega a `/publicaciones/{idPublicacion}`.
- La pantalla de solicitudes recibidas/enviadas corresponde a UC-65.
- Mientras la solicitud siga `PENDIENTE`, el remitente puede eliminarla con
  `DELETE /solicitudes-colaboracion/{id}` (UC-64,
  [`ficha-tecnica-eliminar-solicitud-colaboracion.md`](ficha-tecnica-eliminar-solicitud-colaboracion.md)).
  Tras eliminarla, puede volver a invitar al mismo perfil.
