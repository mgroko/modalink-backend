# Ficha técnica — Chat grupal (UC-41)

> **Estado:** backend implementado.
> **Alcance:** chat de texto único por proyecto, con historial paginado, participantes, notificaciones y SSE.
> **Fuera de alcance:** adjuntos, edición, borrado, búsqueda, moderación y confirmación de lectura por mensaje.

## 1. Objetivo

Permitir que directores y miembros activos de un proyecto `PUBLICADO` o `CONFIRMADO`
intercambien mensajes de texto en un chat grupal asociado al proyecto.

Cada proyecto tiene un único chat implícito. Los miembros activos pueden seguir
leyendo el historial cuando el proyecto pasa a `FINALIZADO` o `CANCELADO`, pero no
pueden enviar mensajes nuevos.

## 2. Modelo

- `mensaje_proyecto`: mensaje asociado directamente a un proyecto.
- `id_proyecto`: proyecto al que pertenece el chat.
- `id_perfil_emisor`: perfil activo que envió el mensaje.
- `contenido`: texto de 1 a 1000 caracteres.
- `fecha_envio`: timestamp del envío.
- Los mensajes son inmutables: no se editan ni se eliminan.
- No se guarda estado leído por mensaje.
- No se audita el contenido.

## 3. Endpoints

Todos requieren sesión `USUARIO` y perfil activo.

| Método | Endpoint | Función |
|---|---|---|
| `GET` | `/proyectos/{idProyecto}/chat` | Abre el chat: proyecto, estado, `puedeEnviar` y participantes |
| `GET` | `/proyectos/{idProyecto}/chat/mensajes?page=0&size=50` | Historial paginado, más nuevo primero |
| `POST` | `/proyectos/{idProyecto}/chat/mensajes` | Envía un mensaje |
| `PATCH` | `/proyectos/{idProyecto}/chat/leido` | Marca como leídas las notificaciones del chat |
| `GET` | `/mensajes/stream` | SSE del perfil activo; incluye el evento `mensaje_grupal` |

### 3.1. Abrir chat

```http
GET /proyectos/12/chat
```

```json
{
  "idProyecto": 12,
  "nombreProyecto": "Campaña Primavera",
  "estadoProyecto": "PUBLICADO",
  "puedeEnviar": true,
  "participantes": [
    {
      "perfil": {
        "idPerfil": 10,
        "nombreArtistico": "Ana Moda",
        "nombre": "Ana",
        "apellido": "Pérez",
        "idProfesion": 2,
        "nombreProfesion": "Modelo",
        "fotoUrl": "https://..."
      },
      "rol": "DIRECTOR"
    }
  ]
}
```

`puedeEnviar = false` si el proyecto no está `PUBLICADO` o `CONFIRMADO`.
El historial sigue disponible para miembros activos en ese caso.

### 3.2. Historial

```http
GET /proyectos/12/chat/mensajes?page=0&size=50
```

```json
{
  "contenido": [
    {
      "idMensajeProyecto": 50,
      "idProyecto": 12,
      "contenido": "Hola equipo",
      "fechaEnvio": "2026-11-09T15:00:00",
      "idPerfilEmisor": 10,
      "perfilEmisor": {
        "idPerfil": 10,
        "nombreArtistico": "Ana Moda",
        "nombre": "Ana",
        "apellido": "Pérez",
        "idProfesion": 2,
        "nombreProfesion": "Modelo",
        "fotoUrl": "https://..."
      },
      "esMio": true
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

El historial se ordena del mensaje más nuevo al más viejo. El frontend puede
invertir la página para mostrarla en orden cronológico.

### 3.3. Enviar mensaje

```http
POST /proyectos/12/chat/mensajes
Content-Type: application/json

{ "contenido": "Hola equipo" }
```

- `201 Created` con el mensaje.
- `400 MENSAJE_INVALIDO` si está vacío, solo tiene espacios o supera 1000 caracteres.
- `403` si el perfil activo no es miembro activo del proyecto.
- `404 PROYECTO_NO_ENCONTRADO` si el proyecto no existe.
- `409 CHAT_GRUPAL_SOLO_LECTURA` si el proyecto no está `PUBLICADO` o `CONFIRMADO`.

### 3.4. Marcar leído

```http
PATCH /proyectos/12/chat/leido
```

- Marca como leídas las notificaciones `MENSAJE_GRUPAL_RECIBIDO` del usuario y
  perfil activo para ese proyecto.
- Es idempotente.
- Devuelve `204`.

## 4. Tiempo real

Se reutiliza `GET /mensajes/stream`. Además del evento `mensaje` del chat privado,
el chat grupal emite:

```text
event: mensaje_grupal
data: {
  "idProyecto": 12,
  "idPerfilEmisor": 10,
  "idPerfilesDestinatarios": [10, 20, 30],
  "mensaje": {
    "idMensajeProyecto": 50,
    "idProyecto": 12,
    "contenido": "Hola equipo",
    "fechaEnvio": "2026-11-09T15:00:00",
    "idPerfilEmisor": 10,
    "perfilEmisor": { "...": "..." }
  }
}
```

El mismo evento se envía al emisor y a todos los participantes activos, para que
las pestañas abiertas actualicen el chat. El frontend decide si el mensaje es
propio comparando `idPerfilEmisor` con el perfil activo.

- Timeout del stream: 30 minutos.
- Ping cada 20 segundos.
- Si se corta, reconectar y volver a consultar el historial.
- Bus en memoria para una instancia. Para múltiples instancias hace falta Redis
  Pub/Sub o PostgreSQL `LISTEN/NOTIFY`.

## 5. Notificaciones

- Tipo: `MENSAJE_GRUPAL_RECIBIDO`, categoría `PROYECTO`, prioridad `MEDIA`.
- `entidad_tipo`: `PROYECTO`.
- `entidad_id`: `idProyecto`.
- `id_proyecto`: `idProyecto`.
- `perfil_contexto`: perfil destinatario.
- `link`: `/proyectos/{idProyecto}/chat`.
- Se excluye al emisor y a los demás perfiles del mismo usuario.
- Se crea como máximo una notificación no leída por participante (perfil) y proyecto.
- Los mensajes siguientes no generan otra notificación mientras siga sin leer.
- Al abrir el chat se marca como leída.

## 6. Reglas de negocio

1. Solo miembros activos del proyecto pueden leer y enviar.
2. El emisor es el perfil activo de la sesión; no se acepta desde el body.
3. Hay un único chat por proyecto.
4. Se puede enviar solo en `PUBLICADO` o `CONFIRMADO`.
5. Los miembros activos pueden leer el historial después de `FINALIZADO` o `CANCELADO`.
6. Los exmiembros no acceden.
7. El contenido se recorta y debe tener entre 1 y 1000 caracteres.
8. Los mensajes son inmutables.
9. No se notifica al emisor.
10. No hay doble tilde ni conteo de mensajes no leídos.
11. No se audita el contenido.

## 7. Errores

| HTTP | Código | Cuándo |
|---|---|---|
| `400` | `MENSAJE_INVALIDO` | Vacío, solo espacios o más de 1000 caracteres |
| `403` | — | El perfil activo no es miembro activo |
| `404` | `PROYECTO_NO_ENCONTRADO` | El proyecto no existe |
| `409` | `CHAT_GRUPAL_SOLO_LECTURA` | El proyecto no admite mensajes nuevos |
| `409` | `CHAT_GRUPAL_NO_PERMITIDO` | Perfil o usuario no activo, o el mensaje no pudo persistirse |
| `401` | `NO_AUTENTICADO` | Sin sesión |
| `403` | `CSRF_INVALIDO` | `POST`/`PATCH` sin token CSRF |

## 8. Consideraciones de frontend

- Usar `EventSource` con `{ withCredentials: true }`.
- Mantener un solo stream por pestaña y escuchar `mensaje` y `mensaje_grupal`.
- Al abrir el chat, llamar a `GET /proyectos/{id}/chat`, `GET .../mensajes` y
  `PATCH .../leido`.
- Mantener el borrador del mensaje en el cliente. Si el backend responde `400`,
  mostrar el error y conservar el texto.
- Si `puedeEnviar = false`, deshabilitar el editor y mostrar el historial.
- No mostrar confirmación de lectura en mensajes grupales.
- El badge de notificaciones se actualiza con los endpoints existentes de
  notificaciones; el chat grupal no agrega un contador propio.
