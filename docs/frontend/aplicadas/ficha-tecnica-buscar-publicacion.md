# Ficha técnica — Buscar publicación (UC-22)

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-19 (alta), UC-14 (ver perfil) y UC-22.

## 1. Objetivo

Permitir buscar publicaciones dentro del portfolio de un perfil usando
filtros opcionales por rango de fechas, proyecto asociado, título y
descripción.

## 2. Endpoint

```http
GET /perfiles/{idPerfil}/publicaciones
```

Parámetros opcionales:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `page` | entero | Página, desde 0. Default 0 |
| `size` | entero | Tamaño de página, 1 a 50. Default 10 |
| `fechaDesde` | `YYYY-MM-DD` | Primer día incluido |
| `fechaHasta` | `YYYY-MM-DD` | Último día incluido |
| `idProyecto` | long | Proyecto asociado |
| `titulo` | string | Búsqueda parcial, case-insensitive |
| `descripcion` | string | Búsqueda parcial, case-insensitive |

Todos los filtros son opcionales y se combinan con `AND`.

### 2.1 Detalle por ID para notificaciones

```http
GET /publicaciones/{idPublicacion}/detalle
```

Devuelve el detalle de una publicación visible para el perfil activo, sin
exigir que sea propia. Es el endpoint que debe usar el frontend al abrir la
ruta `/publicaciones/{id}` desde una notificación.

- Requiere sesión y rol `USUARIO`; no es anónimo.
- Devuelve `PublicacionResponse`.
- Si la publicación es propia, mantiene el mismo payload que
  `GET /publicaciones/{id}`.
- Si es de otro perfil, solo la devuelve si está `ACTIVA` y el autor es
  visible según UC-14.
- El proyecto asociado se incluye solo si el viewer puede verlo.
- Si la publicación no existe, no está `ACTIVA` o el autor no es visible,
  devuelve `404`.
- `GET /publicaciones/{id}` sigue siendo exclusivo del dueño y se mantiene
  para el formulario de edición de UC-21.

## 3. Opciones de proyecto para el selector

```http
GET /perfiles/{idPerfil}/publicaciones/proyectos
```

Devuelve únicamente los proyectos que tienen al menos una publicación
`ACTIVA` del perfil y que son visibles para el perfil activo del viewer.

```json
[
  {
    "idProyecto": 25,
    "nombre": "Campaña Otoño"
  }
]
```

Si no hay proyectos visibles con publicaciones, devuelve `200 []`.

## 4. Ejemplos

```http
GET /perfiles/7/publicaciones?page=0&size=10
```

```http
GET /perfiles/7/publicaciones?fechaDesde=2026-01-01&fechaHasta=2026-12-31
```

```http
GET /perfiles/7/publicaciones?idProyecto=25&titulo=editorial
```

```http
GET /perfiles/7/publicaciones?titulo=otoño&descripcion=campaña
```

## 5. Respuesta

Se mantiene `PaginaResponse<PublicacionResponse>`.

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

## 6. Reglas

- Solo devuelve publicaciones `ACTIVA`.
- Aplica las reglas de visibilidad del perfil de UC-14.
- En el portfolio, si el perfil consultado aparece como colaborador
  `AUTOMATICA` de otra publicación, esa publicación no se incluye. Solo se
  incluyen colaboraciones `MANUAL` o `AMBOS`.
- Cada colaborador de la respuesta incluye `origen`
  (`MANUAL`, `AUTOMATICA` o `AMBOS`). El autor de la publicación tiene
  `origen: null`.
- Orden: `fechaPublicacion` descendente y luego `idPublicacion` descendente.
- Si el viewer no puede ver el proyecto, `proyecto` se devuelve `null`.
- Si no hay resultados, devuelve `200` con `contenido: []`.
- Si `fechaDesde > fechaHasta`, devuelve `400`.

## 7. Errores

| HTTP | Caso |
|---|---|
| `400` | Rango de fechas inválido |
| `401` | Sin sesión |
| `403` | Sin rol `USUARIO` |
| `404` | Perfil no existe o no es visible |
| `500` | Error interno |

## 8. Frontend

- Agregar filtros sobre el portfolio:
  - rango de fechas;
  - selector de proyecto;
  - título;
  - descripción.
- Enviar los filtros como query params.
- Mantener los filtros en la URL.
- Reutilizar la tarjeta de publicación de UC-19.
- Para abrir una publicación por ID desde una notificación, usar
  `GET /publicaciones/{idPublicacion}/detalle`.
- Mostrar estado vacío:
  > “No se encontraron publicaciones con esos filtros.”
- Mostrar error bajo el rango si el backend responde `400`.
- Limpiar filtros debe volver a la primera página.
- La paginación debe conservar los filtros activos.
