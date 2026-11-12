# Ficha técnica — Interactuar con publicación (UC-66)

> **Estado:** backend implementado y testeado.
> **Casos de uso relacionados:** UC-19 (alta), UC-21 (modificación), UC-22 (búsqueda), UC-61/62/63 (colaboradores) y UC-66.
> **Tipo de cambio:** endpoints nuevos + campo nuevo en `PublicacionResponse`.

## 1. Objetivo

Permitir que el perfil activo interactúe con una publicación:

- dar y quitar **Me gusta**;
- registrar **comentarios** de hasta 1000 caracteres;
- consultar los comentarios de forma paginada;
- recibir notificaciones cuando otro perfil interactúa con una publicación propia o en la que colabora.

La interacción siempre se realiza con el **perfil activo** de la sesión, no directamente con el usuario.

## 2. Resumen de cambios para el frontend

| Cambio | Impacto |
|---|---|
| `POST /publicaciones/{id}/me-gusta` | Nuevo. Dar Me gusta. |
| `DELETE /publicaciones/{id}/me-gusta` | Nuevo. Quitar Me gusta. |
| `POST /publicaciones/{id}/comentarios` | Nuevo. Crear comentario. |
| `GET /publicaciones/{id}/comentarios` | Nuevo. Listar comentarios paginados. |
| `PublicacionResponse.meGusta` | Campo nuevo. Indica si el perfil activo ya dio Me gusta. |
| `PublicacionResponse.cantidadMeGusta` | Ahora devuelve el conteo real; antes era `0`. |
| `PublicacionResponse.cantidadComentarios` | Ahora devuelve el conteo real; antes era `0`. |
| Notificaciones | Nuevos tipos `PUBLICACION_MEGUSTA` y `PUBLICACION_COMENTARIO`. |

`PublicacionResponse` se usa en alta, modificación, búsqueda, feed y detalle. El campo `meGusta` debe contemplarse en todas esas vistas.

## 3. Autenticación y CSRF

Todos los endpoints requieren:

- sesión iniciada;
- rol `USUARIO`;
- perfil activo seleccionado;
- cookies de sesión (`withCredentials: true`);
- token CSRF en `POST` y `DELETE`, usando el header habitual del proyecto (`X-XSRF-TOKEN`).

Si no hay perfil activo, el backend responde `404`.

## 4. Me gusta

### 4.1 Dar Me gusta

```http
POST /publicaciones/{idPublicacion}/me-gusta
```

Sin body.

Respuesta `200 OK`:

```json
{
  "idPublicacion": 99,
  "meGusta": true,
  "cantidadMeGusta": 5
}
```

La operación es **idempotente**: si el perfil ya había dado Me gusta, devuelve `200` con el mismo estado y no duplica la interacción ni la notificación.

### 4.2 Quitar Me gusta

```http
DELETE /publicaciones/{idPublicacion}/me-gusta
```

Sin body.

Respuesta `200 OK`:

```json
{
  "idPublicacion": 99,
  "meGusta": false,
  "cantidadMeGusta": 4
}
```

También es idempotente: si el perfil no había dado Me gusta, devuelve `200` con `meGusta: false` y el conteo actual.

### 4.3 Regla de uso en el frontend

El botón debe alternar según `meGusta`:

```text
si meGusta === true  -> DELETE /me-gusta
si meGusta === false -> POST   /me-gusta
```

No usar `POST` como toggle. El backend no alterna: agrega o es idempotente.

Tomar siempre `cantidadMeGusta` y `meGusta` de la respuesta como fuente de verdad. Si se hace actualización optimista, revertir ante error.

## 5. Comentarios

### 5.1 Crear comentario

```http
POST /publicaciones/{idPublicacion}/comentarios
Content-Type: application/json
```

Body:

```json
{
  "texto": "Muy buena publicación"
}
```

Reglas:

- obligatorio;
- no puede ser vacío ni contener solo espacios;
- máximo **1000 caracteres**;
- el backend guarda el texto con `trim()`.

Respuesta `201 Created`:

```json
{
  "idComentario": 10,
  "idPublicacion": 99,
  "autor": {
    "idPerfil": 7,
    "nombreArtistico": "Luna Diseños",
    "profesion": "Diseñadora",
    "fotoUrl": "/uploads/perfiles/luna.webp"
  },
  "texto": "Muy buena publicación",
  "fechaComentario": "2026-11-11T12:00:00"
}
```

### 5.2 Listar comentarios

```http
GET /publicaciones/{idPublicacion}/comentarios?page=0&size=10
```

Parámetros:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `page` | entero | Página, desde 0. Default `0` |
| `size` | entero | Tamaño, 1 a 50. Default `10` |

Orden: **más antiguo primero** (`fechaComentario ASC`, luego `idComentario ASC`).

Respuesta `200 OK`:

```json
{
  "contenido": [
    {
      "idComentario": 10,
      "idPublicacion": 99,
      "autor": {
        "idPerfil": 7,
        "nombreArtistico": "Luna Diseños",
        "profesion": "Diseñadora",
        "fotoUrl": "/uploads/perfiles/luna.webp"
      },
      "texto": "Muy buena publicación",
      "fechaComentario": "2026-11-11T12:00:00"
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 10,
  "totalElementos": 1,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

Si no hay comentarios, devuelve `200` con `contenido: []`.

## 6. Cambios en `PublicacionResponse`

Se agrega el campo:

```ts
meGusta: boolean
```

La respuesta queda, para la parte de interacción:

```json
{
  "idPublicacion": 99,
  "titulo": "Editorial Otoño",
  "cantidadMeGusta": 5,
  "cantidadComentarios": 3,
  "meGusta": true
}
```

Consideraciones:

- `meGusta` es relativo al **perfil activo**.
- Si no hay perfil activo, `meGusta` se devuelve `false`.
- Los contadores son reales en feed, búsqueda, alta, modificación y detalle.
- Al cambiar de perfil activo, hay que volver a consultar el detalle o el listado para recalcular `meGusta`.

## 7. Visibilidad y reglas de negocio

- Solo se puede interactuar con publicaciones `ACTIVA`.
- El autor de la publicación debe ser visible según las reglas de UC-14.
- Si la publicación no existe, no está `ACTIVA` o su autor no es visible, el backend responde `404`.
- El perfil activo debe pertenecer al usuario y estar activo.
- Se puede interactuar con una publicación propia.
- Los comentarios de perfiles que luego pasan a estado `Baja` se conservan y se muestran con los datos de autor almacenados.
- Los contadores no se recalculan en el frontend a partir de arreglos locales: usar los valores devueltos por el backend.

## 8. Notificaciones

Cuando un perfil interactúa con una publicación:

- se notifica al perfil dueño;
- se notifica a los colaboradores activos;
- se excluye al perfil que realizó la interacción;
- se genera una notificación por perfil;
- no se notifica si el perfil ya había dado Me gusta.

Tipos nuevos:

| Tipo | Título | Link |
|---|---|---|
| `PUBLICACION_MEGUSTA` | Nueva interacción en publicación | `/publicaciones/{idPublicacion}` |
| `PUBLICACION_COMENTARIO` | Nuevo comentario en publicación | `/publicaciones/{idPublicacion}` |

La bandeja de notificaciones ya devuelve `link`, `entidadTipo` y `entidadId`. Para estos tipos:

- `entidadTipo`: `PUBLICACION`;
- `entidadId`: ID de la publicación;
- abrir la ruta `/publicaciones/{idPublicacion}`;
- usar `GET /publicaciones/{idPublicacion}/detalle` para cargar la publicación;
- si el tipo es `PUBLICACION_COMENTARIO`, se puede hacer scroll a la sección de comentarios.

## 9. Errores

| HTTP | Caso |
|---|---|
| `400` | Comentario vacío, solo espacios o mayor a 1000 caracteres |
| `400` | Body inválido o JSON mal formado |
| `401` | Sin sesión |
| `403` | Sin rol `USUARIO` |
| `403` | `CSRF_INVALIDO`: falta o es inválido el token CSRF |
| `404` | Publicación inexistente, no `ACTIVA` o autor no visible |
| `404` | No hay perfil activo o el perfil no pertenece al usuario |
| `409` | Perfil activo en baja |
| `500` | Error interno |

Para un `404` de publicación, ocultar los controles de interacción y mostrar un estado de publicación no disponible.

## 10. Guía de integración en el frontend

### 10.1 Detalle de publicación

1. Cargar con `GET /publicaciones/{idPublicacion}/detalle`.
2. Inicializar:
   - `publicacion.meGusta`;
   - `publicacion.cantidadMeGusta`;
   - `publicacion.cantidadComentarios`.
3. Mostrar el botón de Me gusta en estado activo o inactivo.
4. Cargar la primera página de comentarios con `GET /publicaciones/{id}/comentarios`.

### 10.2 Botón de Me gusta

- Deshabilitar mientras hay una request en curso.
- Enviar `POST` o `DELETE` según `meGusta`.
- Actualizar `meGusta` y `cantidadMeGusta` con la respuesta.
- Mantener accesibilidad:
  - `aria-pressed`;
  - etiqueta “Me gusta” / “Quitar Me gusta”.
- Si se usa actualización optimista, revertir ante error.

### 10.3 Comentarios

- Mostrar el listado del más antiguo al más reciente.
- Implementar “Cargar más” incrementando `page` mientras `ultima === false`.
- El textarea debe:
  - tener máximo 1000 caracteres;
  - mostrar contador;
  - deshabilitar el envío si el texto está vacío o solo tiene espacios;
  - limpiar el contenido después de un `201`.
- Tras crear un comentario:
  - agregar el `ComentarioResponse` al final del listado cargado; o
  - recargar la última página si el usuario está viendo un tramo anterior;
  - incrementar `cantidadComentarios`.
- Mostrar el mensaje de error del backend si responde `400`.
- Evitar doble envío deshabilitando el botón mientras se procesa.

### 10.4 Tarjetas de publicación

En feed, búsqueda y portfolio:

- mostrar `cantidadMeGusta` y `cantidadComentarios`;
- mostrar el corazón lleno si `meGusta === true`;
- al hacer clic en el corazón, usar los endpoints de UC-66;
- si la tarjeta se reutiliza en varios listados, centralizar el estado en un store o composable para que el cambio se refleje en todas las vistas.

### 10.5 Cambio de perfil activo

Si el usuario cambia de perfil:

- descartar el estado local de `meGusta`;
- volver a consultar el detalle o listado;
- no reutilizar `meGusta` del perfil anterior.

## 11. Tipos TypeScript sugeridos

```ts
export interface MeGustaResponse {
  idPublicacion: number;
  meGusta: boolean;
  cantidadMeGusta: number;
}

export interface CrearComentarioRequest {
  texto: string;
}

export interface ComentarioResponse {
  idComentario: number;
  idPublicacion: number;
  autor: {
    idPerfil: number;
    nombreArtistico: string;
    profesion: string;
    fotoUrl: string | null;
  };
  texto: string;
  fechaComentario: string;
}

export interface PublicacionResponse {
  idPublicacion: number;
  titulo: string;
  descripcion: string | null;
  fechaPublicacion: string;
  estado: string;
  autor: {
    idPerfil: number;
    nombreArtistico: string;
    profesion: string;
    fotoUrl: string | null;
  };
  proyecto: {
    idProyecto: number;
    nombre: string;
    puedeVerProyecto: boolean;
  } | null;
  ubicacion: unknown | null;
  imagenes: Array<{
    idImagen: number;
    url: string;
    orden: number;
  }>;
  colaboradores: Array<{
    idPerfil: number;
    nombreArtistico: string;
    profesion: string;
    fotoUrl: string | null;
  }>;
  cantidadMeGusta: number;
  cantidadComentarios: number;
  meGusta: boolean;
}
```

## 12. Checklist de QA

- [ ] El detalle muestra el corazón lleno si `meGusta === true`.
- [ ] `POST` y `DELETE` actualizan el contador sin recargar toda la página.
- [ ] Un doble clic no genera dos requests simultáneas.
- [ ] Un comentario vacío o de 1001 caracteres muestra error y no se envía.
- [ ] Un comentario de exactamente 1000 caracteres se acepta.
- [ ] El listado de comentarios respeta el orden ascendente.
- [ ] “Cargar más” no duplica comentarios.
- [ ] Al crear un comentario se actualiza `cantidadComentarios`.
- [ ] Los cambios de Me gusta se reflejan en las tarjetas de feed y búsqueda.
- [ ] Al cambiar de perfil activo se refresca `meGusta`.
- [ ] Las notificaciones de UC-66 abren `/publicaciones/{id}`.
- [ ] Un `404` oculta los controles de interacción.

## 13. Fuera de alcance

UC-66 no incluye:

- eliminar o editar comentarios;
- listar los perfiles que dieron Me gusta;
- responder a comentarios;
- moderación de comentarios;
- reacciones distintas de Me gusta.

Estas funcionalidades requerirían endpoints y reglas adicionales.
