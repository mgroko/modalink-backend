# Ficha técnica — Alta publicación (UC-19)

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-19, UC-22 (lectura mínima), UC-29 y UC-37.

## 1. Objetivo

Permitir que el perfil activo cree una publicación con 1 a 10 imágenes,
título obligatorio y descripción, ubicación y proyecto asociado opcionales.
La publicación queda visible en el portfolio del perfil.

## 2. Endpoints

### Alta

```http
POST /publicaciones
Content-Type: multipart/form-data
```

| Campo | Obligatorio | Descripción |
|---|---|---|
| `titulo` | Sí | 1 a 150 caracteres |
| `descripcion` | No | Máximo 1000 caracteres |
| `idProyecto` | No | Proyecto `CONFIRMADO` o `FINALIZADO` donde el perfil sea miembro activo |
| `localidadId` | No | ID de localidad del catálogo Georef |
| `provinciaId` | No | ID de provincia del catálogo Georef |
| `archivos` | Sí | 1 a 10 imágenes JPEG, PNG o WebP de hasta 10 MB |

Respuesta `201 Created`:

```json
{
  "idPublicacion": 1,
  "titulo": "Editorial Otoño",
  "descripcion": "Producción realizada en Buenos Aires.",
  "fechaPublicacion": "2026-11-10T14:30:00",
  "estado": "ACTIVA",
  "autor": {
    "idPerfil": 7,
    "nombreArtistico": "Luna Diseños",
    "profesion": "Diseñadora",
    "fotoUrl": "/uploads/perfiles/perfil_....webp"
  },
  "proyecto": {
    "idProyecto": 25,
    "nombre": "Campaña Otoño",
    "puedeVerProyecto": true
  },
  "ubicacion": {
    "idCiudad": 12,
    "idExterno": "0602800000",
    "fuenteApi": "GEOREF",
    "nombre": "CABA",
    "provincia": {
      "idProvincia": 1,
      "idExterno": "06",
      "fuenteApi": "GEOREF",
      "nombre": "CABA",
      "pais": {
        "idPais": 1,
        "codigoIso": "AR",
        "nombre": "Argentina"
      }
    }
  },
  "imagenes": [
    {
      "idImagen": 100,
      "url": "/uploads/publicaciones/publicacion_....webp",
      "orden": 0
    }
  ],
  "colaboradores": [],
  "cantidadMeGusta": 0,
  "cantidadComentarios": 0,
  "meGusta": false
}
```

`proyecto` y `ubicacion` pueden ser `null`.

### Lectura mínima del portfolio

```http
GET /perfiles/{idPerfil}/publicaciones?page=0&size=10
```

Respuesta: `PaginaResponse<PublicacionResponse>`.

Solo devuelve publicaciones `ACTIVA` de perfiles y usuarios activos. Si el
viewer no puede ver el proyecto asociado, el campo `proyecto` se devuelve
`null` para no filtrar información de proyectos privados.

## 3. Errores

| HTTP | Caso |
|---|---|
| `400` | Título vacío, sin archivos, más de 10 imágenes, archivo inválido, dimensiones excedidas |
| `403` | Sin rol `USUARIO` |
| `404` | No hay perfil activo, perfil/proyecto inexistente o no visible |
| `409` | Proyecto no asociable: no es `CONFIRMADO`/`FINALIZADO` o el perfil no es miembro activo |
| `409` | Perfil en baja |

## 4. Reglas de negocio

- El autor es el perfil activo de la sesión.
- El proyecto asociado es opcional.
- Solo se asocian proyectos `CONFIRMADO` o `FINALIZADO`.
- El perfil debe ser miembro activo del proyecto.
- La ubicación usa el catálogo Georef.
- Las imágenes se guardan en `uploads/publicaciones/` y se ordenan por el orden de carga.
- La primera imagen se usa como portada en el collage.
- Las publicaciones colaborativas, likes, comentarios y edición quedan fuera de esta iteración.

## 5. Frontend

- Enviar el formulario como `multipart/form-data`.
- Mantener los datos cargados si el backend responde `400`.
- Validar en cliente: título, cantidad de archivos y tamaño.
- Mostrar progreso de subida.
- Al crear, insertar la tarjeta en el portfolio o refrescar
  `GET /perfiles/{idPerfil}/publicaciones`.
- El collage, el “+N”, el “ver más” y la antigüedad relativa (“hace 2 h”) se
  resuelven en el frontend.
- Mostrar el botón “Ver proyecto” solo si `proyecto.puedeVerProyecto` es `true`.
