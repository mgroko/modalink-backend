# Ficha técnica — Modificar publicación (UC-21)

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-19 (alta), UC-21, UC-22 (búsqueda) y UC-61 (invitar colaborador, pendiente).

## 1. Objetivo

Permitir que el perfil activo dueño de una publicación recupere sus datos y
modifique título, descripción, proyecto asociado, ubicación e imágenes. La
operación de modificación es atómica: los datos y las imágenes se persisten en
una sola transacción.

Los colaboradores se muestran en el detalle, pero la invitación de nuevos
colaboradores sigue siendo responsabilidad de UC-61.

## 2. Endpoints

### Detalle para edición

```http
GET /publicaciones/{idPublicacion}
```

Devuelve `200 OK` con `PublicacionResponse`. Solo accesible para el perfil
activo dueño de la publicación; cualquier otro caso responde `404`.

El campo `ubicacion` expone `idExterno` de la ciudad y de la provincia, que son
los valores que el formulario debe reenviar como `localidadId` y `provinciaId`.

Si el perfil ya no tiene visibilidad sobre el proyecto asociado, el detalle de
edición lo incluye igualmente con `puedeVerProyecto = false`, para que el
formulario pueda conservarlo.

### Modificación

```http
PUT /publicaciones/{idPublicacion}
Content-Type: multipart/form-data
```

| Campo | Obligatorio | Descripción |
|---|---|---|
| `titulo` | Sí | 1 a 150 caracteres |
| `descripcion` | No | Máximo 1000 caracteres |
| `idProyecto` | No | Proyecto `CONFIRMADO` o `FINALIZADO` donde el perfil activo sea miembro activo. `null` desvincula |
| `localidadId` | No | ID de localidad del catálogo Georef. Vacío elimina la ubicación |
| `provinciaId` | No | ID de provincia del catálogo Georef |
| `imagenesConservadas` | Sí | IDs de imágenes existentes que se conservan, en el orden final deseado. Puede ser vacío si se envían imágenes nuevas |
| `archivos` | No | Imágenes nuevas (0 a 10). Se agregan al final |

`imagenesConservadas` se envía repitiendo el campo, por ejemplo:

```bash
curl -X PUT http://localhost:8080/publicaciones/1 \
  -H "Cookie: modalink_token=..." \
  -F "titulo=Editorial Primavera" \
  -F "descripcion=Producción actualizada" \
  -F "idProyecto=25" \
  -F "localidadId=0602800000" \
  -F "provinciaId=06" \
  -F "imagenesConservadas=10" \
  -F "imagenesConservadas=12" \
  -F "archivos=@nueva1.jpg" \
  -F "archivos=@nueva2.jpg"
```

Respuesta `200 OK`:

```json
{
  "idPublicacion": 1,
  "titulo": "Editorial Primavera",
  "descripcion": "Producción actualizada",
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
  "ubicacion": { "...": "..." },
  "imagenes": [
    { "idImagen": 10, "url": "/uploads/publicaciones/a.webp", "orden": 0 },
    { "idImagen": 12, "url": "/uploads/publicaciones/b.webp", "orden": 1 },
    { "idImagen": 30, "url": "/uploads/publicaciones/nueva1.webp", "orden": 2 }
  ],
  "colaboradores": [],
  "cantidadMeGusta": 0,
  "cantidadComentarios": 0
}
```

## 3. Semántica de las imágenes

- El total final (`imagenesConservadas + archivos`) debe quedar entre **1 y 10**.
- Toda imagen conservada debe pertenecer a la publicación.
- Las imágenes existentes que no se incluyan en `imagenesConservadas` se
  eliminan: se desasocia la fila de `imagen_publicacion`, se borra la fila de
  `imagen` y el archivo físico se elimina después del commit.
- Las imágenes nuevas se procesan con el mismo pipeline de UC-19 y se agregan
  al final.
- Si la transacción falla, los archivos nuevos se eliminan automáticamente.
- El orden de `imagenesConservadas` define el orden de las imágenes que se
  mantienen. Las nuevas van después.

## 4. Reglas de negocio

- Solo el perfil activo dueño puede consultar y modificar la publicación.
- Una publicación de otro perfil responde `404` (no se revela su existencia).
- Perfil en baja: `409`.
- `fechaPublicacion`, `estado` y `autor` no se modifican.
- Se pueden editar publicaciones `ACTIVA` y `OCULTA`.
- Si el `idProyecto` recibido es el mismo que ya tiene la publicación, no se
  revalida la membresía ni el estado del proyecto. Esto permite editar
  publicaciones históricas cuyo autor ya dejó el proyecto.
- Si se elige un proyecto distinto, se exige que sea `CONFIRMADO` o
  `FINALIZADO` y que el perfil sea miembro activo.

## 5. Errores

| HTTP | Caso |
|---|---|
| `400` | Título vacío o mayor a 150, descripción mayor a 1000, sin imágenes finales, más de 10 imágenes, imagen que no pertenece a la publicación, archivo vacío o inválido |
| `403` | Sin rol `USUARIO` |
| `404` | Sin perfil activo, publicación inexistente o de otro perfil |
| `409` | Perfil en baja, proyecto no asociable o proyecto inexistente |

## 6. Frontend

- Al abrir el editor, llamar a `GET /publicaciones/{idPublicacion}` y precargar
  título, descripción, proyecto, ubicación e imágenes.
- Para el selector de ubicación, usar `ubicacion.idExterno` y
  `ubicacion.provincia.idExterno` como `localidadId` y `provinciaId`.
- Enviar siempre `imagenesConservadas` con los IDs de las imágenes que quedan.
  Si no se envía, el backend responde `400` para evitar borrados accidentales.
- Enviar el `PUT` como `multipart/form-data`.
- Mantener los datos cargados si el backend responde `400`.
- Deshabilitar el guardado si no queda ninguna imagen y no se agregó una nueva.
- El botón “Invitar colaborador” debe abrir UC-61; UC-21 no crea invitaciones.
