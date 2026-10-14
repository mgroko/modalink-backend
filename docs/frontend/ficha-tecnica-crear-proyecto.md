# Ficha Técnica Frontend: Pantalla "Crear Proyecto" (UC-24)

Esta especificación detalla la interacción con la API, validaciones, modelos de datos, manejo de errores y consideraciones de UX/UI para el desarrollo del formulario y flujo de creación de proyectos.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `POST`
* **URL:** `/proyectos`
* **Content-Type:** `application/json`
* **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`).
* **Contexto de Perfil Requerido:**
  * La solicitud debe tener un **perfil activo seleccionado** en el contexto de la sesión (`idPerfilActivo`).
  * Si el usuario no tiene un perfil activo seleccionado o el perfil se encuentra en estado de baja, el backend rechaza la petición.

---

## 2. Estructura de Datos (Payloads)

### Request Payload (`POST /proyectos`)

```json
{
  "nombre": "Colección Primavera-Verano",
  "descripcion": "Diseño y confección de prendas sostenibles para la nueva temporada.",
  "privacidad": "PUBLICO",
  "fechaEntregaEstipulada": "2026-12-15",
  "aceptaPostulacionGral": true,
  "ubicacion": {
    "localidadId": "06049010000",
    "provinciaId": "06"
  },
  "objetivos": [
    {
      "nombre": "Diseño de bocetos iniciales",
      "descripcion": "Elaborar los primeros 10 bocetos de las prendas clave."
    }
  ],
  "requerimientosGral": [
    {
      "cantidad": 2,
      "idProfesion": 2,
      "descripcion": "Modelos para pasarela",
      "caracteristicas": [
        { "idCaracteristica": 1, "valorMin": 160, "valorMax": 180 },
        { "idCaracteristica": 5, "valores": [1, 4] }
      ],
      "habilidades": [1, 4]
    }
  ],
  "moodboard": {
    "descripcion": "Paleta fría, telas de lino, inspiración nórdica."
  }
}
```

---

## 3. Matriz de Campos y Validaciones

| Campo | Tipo / Formato | Obligatorio | Reglas / Restricciones Backend | Notas UI/UX sugeridas |
| :--- | :--- | :---: | :--- | :--- |
| `nombre` | `string` | **Sí** | Máx. 50 caracteres. No blanco. Único para el perfil director activo. | Input con contador `(x/50)`. |
| `descripcion` | `string` | **Sí** | Máx. 200 caracteres. No blanco. | Textarea con contador `(x/200)`. |
| `privacidad` | `enum` | **Sí** | `PUBLICO` \| `PRIVADO` (mayúsculas). | Segmented Control / Radio. |
| `fechaEntregaEstipulada` | `YYYY-MM-DD` | No | Fecha ISO. Si se envía, no puede ser anterior a hoy. | Datepicker con `min = hoy`. |
| `aceptaPostulacionGral` | `boolean` | No | Default `false`. | Switch. |
| `ubicacion` | `object` | No | Si se envía, `localidadId` no puede estar en blanco. | Autocompletado Georef. |
| `objetivos[]` | `array` | No | Lista dinámica. | Sección +/- objetivos. |
| `objetivos[].nombre` | `string` | Sí (por ítem) | Máx. 100 caracteres. | Input. |
| `objetivos[].descripcion` | `string` | No | Máx. 300 caracteres. | Textarea. |
| `requerimientosGral[]` | `array` | No | Lista de requerimientos de personal. | Sección +/- requerimientos. |
| `requerimientosGral[].cantidad` | `int` | **Sí** | > 0 (`@Positive`). | Number input. |
| `requerimientosGral[].idProfesion` | `long` | **Sí** | Debe existir. | Select de profesiones. |
| `requerimientosGral[].descripcion` | `string` | No | Máx. 200 caracteres. | Input. |
| `requerimientosGral[].caracteristicas[]` | `array` | No | Sin duplicados por `idCaracteristica`. La característica debe pertenecer a la profesión del requerimiento. | Selector filtrado por profesión. |
| `requerimientosGral[].caracteristicas[].idCaracteristica` | `long` | **Sí** | Debe existir. | Select. |
| `requerimientosGral[].caracteristicas[].valorMin` / `valorMax` | `number` | Condicional | **NUMERICO**: ambos obligatorios, ≥ 0, `min ≤ max`. **No-NUMERICO**: deben ser `null`. | Rango solo visible si tipo = NUMERICO. |
| `requerimientosGral[].caracteristicas[].valores[]` | `array<long>` | Condicional | **ENUMERADO**: ≥ 1 valor existente y perteneciente a la característica. **No-ENUMERADO**: debe ser vacío/`null`. | Checklist de valores solo si tipo = ENUMERADO. |
| `requerimientosGral[].habilidades[]` | `array<long>` | No | Cada habilidad debe existir. | Multi-select de habilidades. |
| `moodboard` | `object` | No | 1:1 con el proyecto. Solo la descripción; **las imágenes se suben después** (sección 7). | Sección moodboard. |
| `moodboard.descripcion` | `string` | No | Máx. 200 caracteres. | Textarea. |

---

## 4. Respuesta Exitosa (`201 Created`)

```json
{
  "idProyecto": 105,
  "nombre": "Colección Primavera-Verano",
  "descripcion": "Diseño y confección de prendas sostenibles para la nueva temporada.",
  "fechaInicio": null,
  "fechaEntregaEstipulada": "2026-12-15",
  "estado": "Borrador",
  "privacidad": "PUBLICO",
  "aceptaPostulacionGral": true,
  "ubicacion": { "idUbicacion": 12, "localidad": "Azul", "provincia": "Buenos Aires" },
  "idDirector": 4,
  "nombreDirector": "Juan Perez",
  "objetivos": [
    { "idObjetivo": 210, "nombre": "Diseño de bocetos iniciales", "descripcion": "..." }
  ],
  "requerimientosGral": [
    {
      "idRequerimientoGral": 30,
      "cantidad": 2,
      "descripcion": "Modelos para pasarela",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "caracteristicas": [
        {
          "idCaracteristica": 1,
          "codigo": "ALTURA",
          "tipoDato": "NUMERICO",
          "valorMin": 160,
          "valorMax": 180,
          "valores": []
        },
        {
          "idCaracteristica": 5,
          "codigo": "COLOR_OJOS",
          "tipoDato": "ENUMERADO",
          "valorMin": null,
          "valorMax": null,
          "valores": [1, 4]
        }
      ],
      "habilidades": [1, 4]
    }
  ],
  "moodboard": {
    "idMoodboard": 12,
    "descripcion": "Paleta fría, telas de lino, inspiración nórdica.",
    "fechaCreacion": "2026-09-01T10:00:00",
    "imagenes": []
  }
}
```

---

## 5. Manejo de Errores y Excepciones

| Código HTTP | Excepción Backend | Causa / Mensaje de Error | Acción Frontend Recomendada |
| :---: | :--- | :--- | :--- |
| **400** | `MethodArgumentNotValidException` | Fallos de formulario (vacíos, largos máximos, `cantidad ≤ 0`, `idProfesion` null). Body: `{ "errores": {campo: msg} }`. | Resaltar inputs con mensaje inline. |
| **400** | `FechaEntregaInvalidaException` | `fechaEntregaEstipulada` anterior a la fecha actual. | Error en campo de fecha de entrega. |
| **400** | `RequerimientoInvalidoException` | Reglas de requerimiento: rango invertido/negativo, NUMERICO sin rango, ENUMERADO sin valores, característica duplicada, rango en no-NUMERICO. | Mostrar `message` bajo la sección de requerimientos. |
| **400** | `DataIntegrityViolation` | Restricción de BD no pre-validada (red de seguridad). | Alerta genérica. |
| **404** | `PerfilActivoNoSeleccionadoException` | Sin perfil activo en sesión. | Modal de selección de perfil. |
| **404** | `PerfilNoEncontradoException` | Perfil inexistente o ajeno a la cuenta. | Re-sincronizar sesión. |
| **404** | `HabilidadNoEncontradaException` | Habilidad inexistente en `habilidades[]`. | Error en el multi-select de habilidades. |
| **404** | `ProfesionNoEncontradaException` | `idProfesion` inexistente. | Error en select de profesión. |
| **409** | `NombreProyectoDuplicadoException` | Nombre ya usado por el perfil director. | Error inline en `nombre`. |
| **403** | `PerfilEnBajaException` | Perfil dado de baja. | Alerta bloqueante. |

---

## 6. Consideraciones de UX

1. **Requerimientos dinámicos:** botón `+ Agregar requerimiento`; cada fila filtra características/habilidades por la profesión seleccionada.
2. **Coherencia tipo de dato:** según `tipoDato` de la característica seleccionada (la respuesta del catálogo de características lo incluye), mostrar rango (NUMERICO) o checklist de valores (ENUMERADO); ocultar el control inaplicable.
3. **Fecha de inicio calculada:** `fechaInicio` no se envía al crear; el backend la deriva de las actividades de la planificación y responde `null` hasta entonces. **Validación reactiva:** `min` del datepicker de `fechaEntregaEstipulada` = hoy.
4. **Navegación post-creación:** usar `idProyecto` retornado para ir al dashboard del proyecto (estado inicial: `Borrador`).

---

## 7. Subida de imágenes del moodboard

### 7.1 Flujo (paso 4 del wizard)

Las imágenes del moodboard **no viajan en `POST /proyectos`**. El wizard retiene los archivos en el cliente (previews locales; añadir o quitar una imagen es 100% UI, sin llamadas al backend) y al pulsar **Finalizar** ejecuta dos llamadas en secuencia:

1. `POST /proyectos` (secciones 1-6 de esta ficha), incluyendo `moodboard.descripcion` si el usuario escribió una → respuesta con `idProyecto`.
2. `POST /proyectos/{idProyecto}/moodboard/imagenes` con los archivos retenidos (esta sección).

Si la llamada 1 falla, no se ejecuta la 2. Si la 2 falla, el proyecto queda creado sin imágenes: reintentar **solo** la llamada 2 con el `idProyecto` existente.

### 7.2 Endpoint

* **Método:** `POST`
* **URL:** `/proyectos/{idProyecto}/moodboard/imagenes`
* **Content-Type:** `multipart/form-data`
* **Campo de archivos:** `archivos` — de **1 a 10** archivos en una sola petición (el nombre del campo debe ser exactamente `archivos`).
* **Límites:** formatos JPG, PNG o WEBP; máx. **10 MB por imagen**; máx. **10 imágenes** en total (incluidas las ya subidas al moodboard); la petición completa puede pesar hasta **110 MB**.
* **Autenticación:** idéntica a `POST /proyectos` (JWT + X-XSRF-TOKEN + perfil activo). El perfil activo debe ser miembro **activo** del proyecto con permiso `MODIFICAR_PROYECTO` (hoy: el Director).
* El backend **re-codifica cada imagen a WebP** (mismo pipeline que la foto de perfil) y la guarda como `uploads/perfiles/moodboard_<UUID>.webp`.
* Si el proyecto se creó **sin** `moodboard`, el backend **crea el moodboard** al subir las imágenes (la descripción queda `null`); si ya existía, las imágenes se agregan a las existentes.

### 7.3 Respuesta exitosa (`200 OK`) — `MoodboardResponse`

```json
{
  "idMoodboard": 12,
  "descripcion": "Paleta fría, telas de lino, inspiración nórdica.",
  "fechaCreacion": "2026-09-01T10:00:00",
  "imagenes": [
    { "idImagen": 501, "url": "/uploads/perfiles/moodboard_8f2c1a02-....webp" },
    { "idImagen": 502, "url": "/uploads/perfiles/moodboard_3ab19c44-....webp" }
  ]
}
```

* `imagenes` viene ordenado por `idImagen` ascendente (orden de subida).
* El mismo objeto `moodboard.imagenes` se devuelve después en `GET /proyectos/{idProyecto}` (dashboard del proyecto).

### 7.4 Manejo de Errores

| Código HTTP | Excepción Backend | Causa / Mensaje | Acción Frontend |
| :---: | :--- | :--- | :--- |
| **400** | `LimiteImagenesMoodboardExcedidoException` | Imágenes existentes + nuevas superan 10. | Quitar imágenes del lote/preview. |
| **400** | `FormatoImagenInvalidoException` | Archivo > 10 MB, formato no admitido o contenido no válido. | Marcar el archivo inválido en el dropzone. |
| **400** | `ArchivoVacioException` | Sin archivos en la petición o archivo vacío. | Validar en el dropzone antes de Finalizar. |
| **400** | `MaxUploadSizeExceededException` | La petición supera los 110 MB totales. | Dividir el lote en varias subidas. |
| **400** | `DimensionesImagenExcedidasException` | Lado > 8000 px o > 24 megapíxeles. | Avisar al usuario y descartar la imagen. |
| **403** | `AccesoDenegadoProyectoException` | El perfil activo no tiene `MODIFICAR_PROYECTO` en el proyecto. | No mostrar la sección / volver al paso anterior. |
| **404** | `ProyectoNoEncontradoException` | `idProyecto` inexistente. | Reintentar la subida solo si el proyecto existe. |
| **404** | `PerfilActivoNoSeleccionadoException` | La sesión no tiene perfil activo. | Modal de selección de perfil. |

### 7.5 Consideraciones de UX

1. **Previews locales:** generar los thumbnails con `URL.createObjectURL`; los archivos solo salen en Finalizar.
2. **Validación en cliente (espejo del backend):** formatos JPG/PNG/WEBP, ≤ 10 MB por imagen, ≤ 10 imágenes; mantener Finalizar habilitado solo si el lote cumple todo.
3. **Progreso:** Finalizar puede tardar (subida + re-codificación de hasta 10 imágenes): spinner/progreso y bloquear doble envío.
4. **Reintento:** si falla la subida de imágenes, el proyecto ya existe: reintentar solo `POST /proyectos/{id}/moodboard/imagenes`; no repetir `POST /proyectos`.
5. **Moodboard opcional:** si no hay descripción **ni** imágenes, no enviar el objeto `moodboard` en `POST /proyectos` ni llamar al endpoint de imágenes.
