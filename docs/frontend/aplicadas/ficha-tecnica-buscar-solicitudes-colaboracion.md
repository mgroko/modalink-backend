# Ficha técnica — Buscar solicitudes de colaboración (UC-65)

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-61 (envío), UC-62/63 (aceptar/rechazar),
> UC-64 (eliminar), UC-22 (publicaciones) y UC-14 (visibilidad de perfiles).

## 1. Objetivo

Permitir que el perfil activo busque las solicitudes de colaboración que envió
o recibió, con filtros por dirección, perfiles, estado, rango de fechas y
publicación. El resultado incluye la publicación asociada y flags para las
acciones de UC-62/63/64.

## 2. Endpoint

```http
GET /solicitudes-colaboracion
```

Parámetros opcionales:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `direccion` | `RECIBIDA` / `ENVIADA` | Si se omite, incluye ambas |
| `idPerfilEmisor` | long | Filtra por perfil remitente |
| `idPerfilReceptor` | long | Filtra por perfil destinatario |
| `estado` | `PENDIENTE` / `ACEPTADA` / `RECHAZADA` | Si se omite, `PENDIENTE` |
| `todosLosEstados` | boolean | `true` ignora el estado por defecto |
| `fechaDesde` | `YYYY-MM-DD` | Primer día incluido |
| `fechaHasta` | `YYYY-MM-DD` | Último día incluido |
| `idPublicacion` | long | Filtra por publicación |
| `page` | int | Default 0 |
| `size` | int | Default 20, máximo 100 |

La búsqueda siempre se limita al perfil activo: solicitudes enviadas por él o
recibidas por él.

### 2.1 Publicaciones para el selector del filtro

```http
GET /solicitudes-colaboracion/publicaciones?direccion=RECIBIDA|ENVIADA
```

Devuelve las publicaciones distintas que tienen solicitudes del perfil activo
en la dirección indicada, sin paginar. Es la fuente recomendada para el
selector de `idPublicacion`:

- `direccion` es opcional; si se omite, devuelve ambas.
- En `ENVIADA` devuelve publicaciones propias.
- En `RECIBIDA` devuelve publicaciones de los perfiles remitentes.
- Solo incluye publicaciones `ACTIVA` y aplica las mismas reglas de
  visibilidad que la búsqueda.
- No depende de `estado` ni de fechas, para que las opciones no desaparezcan
  al aplicar otros filtros.
- Orden: título ascendente y luego `idPublicacion` ascendente.

```json
[
  {
    "idPublicacion": 99,
    "titulo": "Editorial Otoño",
    "portadaUrl": "/uploads/publicaciones/portada.webp",
    "estado": "ACTIVA"
  }
]
```

Sin resultados devuelve `200 []`. Sin perfil activo devuelve `404`.

Combinaciones inválidas:

- `todosLosEstados=true` junto con `estado` → `400`.
- `fechaDesde > fechaHasta` → `400`.
- IDs de filtro menores o iguales a cero → `400`.

## 3. Respuesta

```http
200 OK
PaginaResponse<SolicitudColaboracionBusquedaResponse>
```

```json
{
  "contenido": [
    {
      "idSolicitudColaboracion": 1,
      "estado": "PENDIENTE",
      "fechaEnvio": "2026-11-10T11:00:00",
      "direccion": "RECIBIDA",
      "idPerfilRemitente": 7,
      "idPerfilDestinatario": 8,
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
      },
      "publicacion": {
        "idPublicacion": 99,
        "titulo": "Editorial Otoño",
        "portadaUrl": "/uploads/publicaciones/portada.webp",
        "estado": "ACTIVA"
      },
      "puedeAceptar": true,
      "puedeRechazar": true,
      "puedeEliminar": false
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 1,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

### Flags

| Flag | Regla |
|---|---|
| `puedeAceptar` | `PENDIENTE` y el perfil activo es el destinatario |
| `puedeRechazar` | `PENDIENTE` y el perfil activo es el destinatario |
| `puedeEliminar` | `PENDIENTE` y el perfil activo es el remitente |

Para solicitudes `ACEPTADA` o `RECHAZADA`, los tres flags son `false`.

## 4. Visibilidad

Se ocultan las solicitudes cuando:

- la contraparte está en baja o su usuario no está activo;
- la publicación no está `ACTIVA`.

La solicitud sigue existiendo en la base, pero no se devuelve en la búsqueda.
La conexión entre los perfiles no se revalida en UC-65: si la conexión se
eliminó pero ambos perfiles siguen activos, la solicitud histórica se muestra.

## 5. Orden y paginación

```text
fechaEnvio DESC, idSolicitudColaboracion DESC
```

Sin resultados devuelve `200` con `contenido: []`.

## 6. Errores

| HTTP | Caso |
|---|---|
| `400` | `todosLosEstados` + `estado`, rango de fechas inválido o IDs no positivos |
| `403` | Sin rol `USUARIO` |
| `404` | Sin perfil activo |

## 7. Frontend

- Por defecto, consultar sin `estado` para ver las pendientes.
- Para ver el historial completo, enviar `todosLosEstados=true`.
- Usar `direccion` para las pestañas “Recibidas” / “Enviadas”.
- Poblar el selector de publicación con
  `GET /solicitudes-colaboracion/publicaciones?direccion=...` usando la misma
  dirección de la pestaña activa.
- Los filtros de perfil son por ID; usar el buscador de perfiles o la red de
  contactos para obtenerlos.
- Mostrar la portada de la publicación y los datos de la contraparte.
- Habilitar las acciones de UC-62/63/64 según los flags.
- Si la página llega vacía, mostrar el estado vacío sin reintentar.

## 8. Eliminar solicitud (UC-64)

Cuando `puedeEliminar` sea `true`, habilitar la acción “Eliminar” y pedir
confirmación antes de ejecutar:

```http
DELETE /solicitudes-colaboracion/{idSolicitudColaboracion}
```

La operación devuelve `204 No Content`, elimina la solicitud y borra la
notificación de solicitud recibida del destinatario. Si responde `409`,
refrescar la búsqueda: la solicitud ya fue aceptada o rechazada. Ver
[`ficha-tecnica-eliminar-solicitud-colaboracion.md`](ficha-tecnica-eliminar-solicitud-colaboracion.md).
