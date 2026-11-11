# Ficha técnica — Feed de publicaciones de contactos

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-19, UC-22, UC-43/UC-44 (conexiones).

## 1. Objetivo

Mostrar en el feed las publicaciones de los contactos activos del perfil,
incluyendo opcionalmente las publicaciones propias.

## 2. Endpoint

```http
GET /publicaciones/feed
```

Parámetros opcionales:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `page` | entero | Página, desde 0. Default 0 |
| `size` | entero | Tamaño de página, 1 a 50. Default 10 |
| `incluirPropias` | boolean | Incluye las publicaciones del perfil activo. Default `true` |

Ejemplos:

```http
GET /publicaciones/feed
```

```http
GET /publicaciones/feed?page=0&size=10&incluirPropias=false
```

## 3. Respuesta

Se reutiliza `PaginaResponse<PublicacionResponse>`.

```json
{
  "contenido": [
    {
      "idPublicacion": 1,
      "titulo": "Editorial Otoño",
      "descripcion": "Producción realizada en Buenos Aires.",
      "fechaPublicacion": "2026-11-10T14:30:00",
      "estado": "ACTIVA",
      "autor": { "...": "..." },
      "proyecto": {
        "idProyecto": 25,
        "nombre": "Campaña Otoño",
        "puedeVerProyecto": true
      },
      "ubicacion": { "...": "..." },
      "imagenes": [ "..." ],
      "colaboradores": [],
      "cantidadMeGusta": 0,
      "cantidadComentarios": 0,
      "meGusta": false
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

## 4. Reglas

- Requiere perfil activo seleccionado; si no hay, `404`.
- Solo incluye publicaciones `ACTIVA`.
- Solo incluye perfiles `ACTIVO` y usuarios `ACTIVO`.
- Solo incluye contactos con conexión `ACTIVA`.
- Las conexiones `ELIMINADA` no aparecen.
- `incluirPropias=true` agrega las publicaciones del perfil activo.
- `incluirPropias=false` devuelve solo contactos.
- Sin contactos y sin propias → `200` con página vacía.
- Orden: `fechaPublicacion DESC`, luego `idPublicacion DESC`.
- Si el viewer no puede ver el proyecto asociado, `proyecto` se devuelve `null`.

## 5. Errores

| HTTP | Caso |
|---|---|
| `401` | Sin sesión |
| `403` | Sin rol `USUARIO` |
| `404` | No hay perfil activo o el perfil no pertenece al usuario |
| `409` | Perfil en baja |
| `500` | Error interno |

## 6. Frontend

- Consumir el endpoint al cargar el feed o al refrescar.
- Mantener `page` y `size`.
- Mostrar estado vacío:
  > “Todavía no hay publicaciones de tus contactos.”
- Si el usuario quiere ver solo contactos, enviar `incluirPropias=false`.
- Reutilizar la tarjeta de publicación de UC-19/UC-22.
- No duplicar publicaciones propias si ya se muestran en otra sección.
- Si `proyecto` es `null`, ocultar el botón “Ver proyecto”.
