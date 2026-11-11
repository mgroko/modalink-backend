# Ficha técnica — Aceptar y rechazar solicitudes de colaboración (UC-62 / UC-63)

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-61 (envío), UC-62, UC-63, UC-65 (búsqueda) y UC-22 (portfolio).

## 1. Objetivo

Permitir que el perfil destinatario de una solicitud de colaboración la acepte
o la rechace. Al aceptar, el perfil se agrega como colaborador de la
publicación y esta aparece en su portfolio.

## 2. Endpoints

```http
POST /solicitudes-colaboracion/{idSolicitudColaboracion}/aceptar
POST /solicitudes-colaboracion/{idSolicitudColaboracion}/rechazar
```

Sin body.

Respuesta `200 OK` de aceptación:

```json
{
  "idSolicitudColaboracion": 1,
  "estado": "ACEPTADA",
  "fechaRespuesta": "2026-11-10T21:00:00",
  "idPublicacion": 99,
  "tituloPublicacion": "Editorial Otoño",
  "colaboradorAgregado": true
}
```

Respuesta de rechazo:

```json
{
  "idSolicitudColaboracion": 1,
  "estado": "RECHAZADA",
  "fechaRespuesta": "2026-11-10T21:00:00",
  "idPublicacion": 99,
  "tituloPublicacion": "Editorial Otoño",
  "colaboradorAgregado": false
}
```

## 3. Reglas de aceptación

1. Solo el perfil **destinatario** puede aceptar. El emisor u otro perfil
   reciben `403`.
2. La solicitud debe existir: si no, `404`.
3. La solicitud debe estar `PENDIENTE`: si ya fue gestionada, `409`.
4. La publicación debe estar `ACTIVA`.
5. El perfil emisor y su usuario deben estar activos.
6. El destinatario no debe estar ya en `colaborador_publicacion`.
7. Se agrega el destinatario como colaborador.
8. Se guarda el estado `ACEPTADA` y `fechaRespuesta`.
9. Se notifica al emisor.

## 4. Reglas de rechazo

1. Solo el perfil destinatario puede rechazar.
2. La solicitud debe existir y estar `PENDIENTE`.
3. Se permite rechazar aunque la publicación ya no esté visible.
4. Se guarda el estado `RECHAZADA` y `fechaRespuesta`.
5. No se agrega colaborador.
6. Se notifica al emisor.

## 5. Notificaciones

| Evento | Tipo | Destino | Link |
|---|---|---|---|
| Aceptación | `SOLICITUD_COLABORACION_ACEPTADA` | Perfil emisor | `/publicaciones/{idPublicacion}` |
| Rechazo | `SOLICITUD_COLABORACION_RECHAZADA` | Perfil emisor | `/publicaciones/{idPublicacion}` |

Mensajes:

```text
El perfil '<destinatario>' aceptó tu solicitud para colaborar en la publicación '<título>'.
El perfil '<destinatario>' rechazó tu solicitud para colaborar en la publicación '<título>'.
```

## 6. Portfolio colaborativo

Después de aceptar:

- la publicación aparece en `GET /perfiles/{idPerfil}/publicaciones` del
  colaborador;
- aparece en `GET /perfiles/{idPerfil}/publicaciones/proyectos`;
- sigue apareciendo en el portfolio del autor;
- el campo `autor` sigue siendo el dueño de la publicación;
- `colaboradores` incluye al nuevo colaborador.

El feed `GET /publicaciones/feed` no cambia: sigue basado en los autores de las
publicaciones de los contactos.

La publicación colaborativa se oculta si el dueño de la publicación no está
activo o si la publicación no está `ACTIVA`.

## 7. Errores

| HTTP | Código | Caso |
|---|---|---|
| `403` | `SOLICITUD_COLABORACION_NO_PERMITIDA` | El perfil activo no es el destinatario |
| `404` | — | Sin perfil activo o solicitud inexistente |
| `409` | `SOLICITUD_COLABORACION_NO_PENDIENTE` | La solicitud ya fue aceptada o rechazada |
| `409` | `PUBLICACION_NO_DISPONIBLE` | Aceptar una publicación que ya no está disponible |
| `409` | `DESTINATARIO_YA_ES_COLABORADOR` | El perfil ya es colaborador |

## 8. Frontend

- Usar los flags `puedeAceptar` y `puedeRechazar` de UC-65 para habilitar las
  acciones.
- Tras aceptar o rechazar, refrescar la búsqueda de UC-65.
- Tras aceptar, refrescar el portfolio del perfil activo y el selector de
  proyectos.
- Mostrar el `fechaRespuesta` en el historial.
- Si el backend responde `409`, volver a consultar la solicitud: pudo haber
  sido gestionada en otra sesión.
