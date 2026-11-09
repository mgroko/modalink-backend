# Ficha técnica — Mensajería privada (UC-42)

> **Estado:** backend implementado.
> **Rama:** `uc42-mensajes-privados`.
> **Casos relacionados:** UC-43 a UC-46 (red de contactos).
> **Alcance:** solo chat. Sin adjuntos, edición, borrado, búsqueda, reportes ni moderación.

## 1. Objetivo

Permitir que el perfil activo envíe mensajes de texto a otro perfil con el que tiene
una **conexión activa**, consulte el historial y vea el estado leído/no leído.

El chat es entre perfiles, no entre usuarios. Un usuario con dos perfiles tiene
conversaciones separadas.

## 2. Modelo

- `conversacion`: par único y canónico de perfiles.
- `mensaje`: texto de hasta 1000 caracteres, emisor participante, fecha y estado de lectura.
- `leido` significa leído por la contraparte.
- Si se elimina la conexión, la conversación queda en **solo lectura**.
- No se audita el contenido.

## 3. Endpoints

| Método | Endpoint | Función |
|---|---|---|
| `GET` | `/conversaciones?page=0&size=20` | Lista conversaciones del perfil activo |
| `GET` | `/conversaciones/resumen` | Badge de conversaciones no leídas |
| `POST` | `/conversaciones` | Obtiene o crea la conversación con un perfil |
| `GET` | `/conversaciones/{idConversacion}/mensajes?page=0&size=50` | Historial paginado |
| `POST` | `/conversaciones/{idConversacion}/mensajes` | Envía un mensaje |
| `PATCH` | `/conversaciones/{idConversacion}/leido` | Marca la conversación como leída |
| `GET` | `/mensajes/stream` | Stream SSE del perfil activo |

Todos requieren sesión `USUARIO` y perfil activo.

### Crear u obtener conversación

```http
POST /conversaciones
Content-Type: application/json

{ "idPerfilDestinatario": 42 }
```

- `200` si la conversación ya existía.
- `201` si se creó.
- `409 CONVERSACION_NO_PERMITIDA` si no hay conexión activa, es el mismo perfil o
  es un perfil del mismo usuario.

### Enviar mensaje

```http
POST /conversaciones/12/mensajes
Content-Type: application/json

{ "contenido": "Hola, ¿cómo estás?" }
```

- `201 Created` con el mensaje.
- `400 MENSAJE_INVALIDO` si está vacío, solo tiene espacios o supera 1000 caracteres.
- `404 CONVERSACION_NO_ENCONTRADA` si no existe o el perfil activo no participa.
- `409 MENSAJE_NO_PERMITIDO` si la conexión dejó de estar activa.

### Historial

```json
{
  "contenido": [
    {
      "idMensaje": 50,
      "idConversacion": 12,
      "contenido": "Hola",
      "fechaEnvio": "2026-11-09T15:00:00",
      "idPerfilEmisor": 10,
      "perfilEmisor": {
        "idPerfil": 10,
        "nombreArtistico": "Ana Moda",
        "nombre": "Ana",
        "apellido": "Pérez",
        "idProfesion": 1,
        "nombreProfesion": "Modelo",
        "fotoUrl": "https://..."
      },
      "esMio": true,
      "leido": false,
      "fechaLectura": null
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 50,
  "totalElementos": 1,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

El historial se ordena del mensaje más nuevo al más viejo. El frontend puede invertir
la página para mostrarla en orden cronológico.

### Lista de conversaciones

```json
{
  "contenido": [
    {
      "idConversacion": 12,
      "perfil": { "...": "..." },
      "fechaCreacion": "2026-11-01T10:00:00",
      "fechaUltimoMensaje": "2026-11-09T15:00:00",
      "ultimoMensaje": "Hola, ¿cómo estás?",
      "tieneNoLeidos": true,
      "puedeEnviar": true
    }
  ]
}
```

`puedeEnviar = false` si la conexión fue eliminada o algún participante dejó de estar
activo. En ese caso el historial se puede leer, pero no enviar.

### Resumen

```json
{
  "totalConversaciones": 6,
  "conversacionesNoLeidas": 2
}
```

El badge cuenta **conversaciones** con al menos un mensaje no leído, no mensajes.

### Marcar leído

```http
PATCH /conversaciones/12/leido
Content-Type: application/json

{ "hastaIdMensaje": 50 }
```

- Marca los mensajes de la contraparte con `idMensaje <= hastaIdMensaje`.
- Si se omite `hastaIdMensaje`, marca todos los no leídos actuales.
- Marca la notificación de la conversación como leída **solo si ya no quedan mensajes
  no leídos**. Si se marcó solo una parte, la notificación sigue no leída.
- Devuelve `204`.
- Publica un evento SSE `conversacion_leida` para que el emisor actualice el doble tilde.

## 4. Tiempo real

### `GET /mensajes/stream`

- `Content-Type: text/event-stream`.
- Requiere perfil activo.
- Al conectar envía un evento `ready` con `idPerfil`.
- Evento `mensaje`:

```text
event: mensaje
data: {
  "idConversacion": 12,
  "idPerfilEmisor": 10,
  "idPerfilDestinatario": 20,
  "mensaje": { "...": "..." }
}
```

- Evento `conversacion_leida`:

```text
event: conversacion_leida
data: {
  "idConversacion": 12,
  "idPerfilLector": 20,
  "idPerfilOtro": 10,
  "idUltimoMensajeLeido": 50,
  "fechaLectura": "2026-11-09T15:05:00"
}
```

El mismo evento `mensaje` se envía al emisor y al receptor. El frontend decide si el
mensaje es propio comparando `idPerfilEmisor` con el perfil activo.

- Timeout del stream: 30 minutos.
- Ping cada 20 segundos.
- Si se corta, reconectar y volver a consultar el historial/lista.
- Bus en memoria para una instancia. Para múltiples instancias hace falta Redis Pub/Sub
  o PostgreSQL `LISTEN/NOTIFY`.

## 5. Reglas de negocio

1. Solo se chatea entre perfiles con conexión `ACTIVA`.
2. No se permite el mismo perfil ni dos perfiles del mismo usuario.
3. Ambos perfiles y usuarios deben estar `ACTIVO`.
4. El contenido se recorta y no puede quedar vacío; máximo 1000 caracteres.
5. El emisor es el perfil activo del JWT.
6. La conversación es única por par canónico.
7. Si la conexión se elimina, el historial es solo lectura.
8. Cada mensaje nace no leído.
9. Al leer la conversación se marcan los mensajes de la contraparte y la notificación.
10. Una notificación no leída por conversación: se crea cuando la conversación pasa de
    leída a no leída, no por cada mensaje.
11. El emisor no recibe notificación por su propio mensaje.
12. No se audita el contenido.
13. No hay adjuntos, edición, borrado ni búsqueda.

## 6. Errores

| HTTP | Código | Cuándo |
|---|---|---|
| `400` | `MENSAJE_INVALIDO` | Vacío, solo espacios o más de 1000 caracteres |
| `404` | `CONVERSACION_NO_ENCONTRADA` | No existe o el perfil activo no participa |
| `404` | `PERFIL_NO_ENCONTRADO` | Perfil destinatario inexistente |
| `409` | `CONVERSACION_NO_PERMITIDA` | Mismo usuario, sin conexión activa o perfil inactivo |
| `409` | `MENSAJE_NO_PERMITIDO` | La conexión dejó de estar activa |
| `401` | `NO_AUTENTICADO` | Sin sesión |
| `403` | `CSRF_INVALIDO` | `POST`/`PATCH` sin token CSRF |

## 7. Consideraciones de frontend

- Usar `EventSource` con `{ withCredentials: true }`.
- Mantener un solo stream por pestaña; al reconectar, refrescar historial y lista.
- Enviar `PATCH /leido` al abrir el chat y cuando llegue un mensaje mientras está abierto.
- Usar `hastaIdMensaje` con el último mensaje visible para no marcar mensajes nuevos.
- El badge de “Mensajes” usa `conversacionesNoLeidas`.
- Mostrar doble tilde cuando `leido = true` en los mensajes propios.
- Si `puedeEnviar = false`, ocultar o deshabilitar el editor y mostrar el historial.
