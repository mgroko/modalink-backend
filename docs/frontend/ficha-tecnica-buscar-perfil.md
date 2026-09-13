# Ficha Técnica Frontend: Búsqueda de Perfiles (UC-16)

Esta especificación técnica detalla la interacción con la API, modelos de datos, parámetros de filtrado, paginación parametrizable, casos de respuesta y recomendaciones de UX/UI para la implementación de la pantalla/componente de **Búsqueda de Perfiles**.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `GET`
* **URL:** `/perfiles/buscar`
* **Content-Type:** `application/json`
* **Autenticación requerida:** Sí (Bearer Token JWT o Cookie de sesión autenticada).
* **Restricción de Acceso:** Disponible para cualquier usuario con sesión activa en el sistema.
* **Filtros de Seguridad y Negocio aplicados por backend:**
  * Solo se retornan perfiles con `estado = "Activo"`.
  * Solo se retornan perfiles cuyo usuario propietario se encuentre en `estado = "Activo"`.
  * Perfiles o usuarios en estado `PendienteBaja`, `Deshabilitado` o `Baja` son excluidos automáticamente.

---

## 2. Parámetros de Consulta (Query Params)

Todos los parámetros de filtrado y paginación se envían como parámetros de consulta en la URL (`GET /perfiles/buscar?...`). Todos son **opcionales y combinables**.

### 2.1. Paginación y Control de Visualización

| Parámetro | Tipo | Default | Opciones / Descripción |
| :--- | :---: | :---: | :--- |
| `page` | `integer` | `0` | Número de página (0-indexed: `0` para la primera página, `1` para la segunda, etc.). |
| `size` | `integer` | `20` | Cantidad de resultados por página seleccionada por el usuario: `20`, `50`, etc. |
| `todos` | `boolean` | `false` | Si se envía `true` (o `size=0`), la API desactiva la paginación y retorna el total completo de perfiles coincidentes sin truncar. |

> **Nota para el selector de resultados en la UI:**
> Proveer un selector tipo dropdown o grupo de botones con las opciones:
> - **20 por página** (`size=20&todos=false`)
> - **50 por página** (`size=50&todos=false`)
> - **Ver todos** (`todos=true` o `size=0`)

---

### 2.2. Criterios de Filtrado

| Parámetro | Tipo | Descripción / Formato |
| :--- | :---: | :--- |
| `nombreArtistico` | `string` | Búsqueda parcial e insensible a mayúsculas/minúsculas sobre el nombre artístico del perfil. |
| `nombre` | `string` | Búsqueda parcial sobre el nombre de pila del usuario. |
| `apellido` | `string` | Búsqueda parcial sobre el apellido del usuario. |
| `idProfesion` | `integer` (Long) | ID de la profesión (ej: `1`). |
| `profesion` | `string` | Búsqueda parcial por nombre de la profesión (ej: `"Modelo"`, `"Fotógrafo"`). |
| `idGenero` | `integer` (Long) | ID del género del usuario. |
| `genero` | `string` | Código del género (ej: `"FEM"`, `"MASC"`). |
| `idUbicacion` | `integer` (Long) | ID de ubicación del usuario. |
| `localidad` | `string` | Búsqueda parcial por nombre de localidad (ej: `"Rosario"`). |
| `provincia` | `string` | Búsqueda parcial por nombre de provincia (ej: `"Santa Fe"`). |
| `idsHabilidades` | `array[integer]` | IDs de habilidades técnicas requeridas. En URL se envía como `idsHabilidades=1&idsHabilidades=3` o coma separada según el cliente HTTP. Retorna perfiles que posean al menos una de las habilidades especificadas. |
| `idCaracteristica` | `integer` (Long) | ID de la característica técnica a filtrar (ej: altura, color de ojos). |
| `valorCaracteristica` | `string` | Valor de texto para características de tipo numérico o texto abierto. |
| `idValorCaracteristica` | `integer` (Long) | ID del valor prefijado (para características de tipo `ENUMERADO`). |

---

## 3. Estructura de la Respuesta (`200 OK`)

La respuesta devuelve un objeto estructurado `PaginaResponse<PerfilBusquedaResponse>` con metadatos de navegación y la lista de perfiles:

### Ejemplo de Respuesta JSON

```json
{
  "contenido": [
    {
      "idPerfil": 12,
      "nombreArtistico": "Luna Valente",
      "biografia": "Modelo publicitaria y de pasarela con 5 años de trayectoria.",
      "estado": "Activo",
      "idProfesion": 2,
      "profesion": "Modelo",
      "idImagen": 45,
      "fotoUrl": "/uploads/perfiles/perfil_45.jpg",
      "idUsuario": 8,
      "nombreUsuario": "Valentina",
      "apellidoUsuario": "Gómez",
      "genero": "FEM",
      "localidad": "Rosario",
      "provincia": "Santa Fe",
      "habilidades": [
        "Pasarela",
        "Fotografía editorial",
        "Expresión corporal"
      ],
      "caracteristicas": [
        {
          "idCaracteristica": 1,
          "codigo": "ALTURA",
          "valor": "175",
          "idValor": null,
          "codigoValor": null,
          "colorHex": null
        },
        {
          "idCaracteristica": 2,
          "codigo": "COLOR_OJOS",
          "valor": null,
          "idValor": 14,
          "codigoValor": "VERDES",
          "colorHex": "#2e7d32"
        }
      ]
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

### Definición de Campos de `PaginaResponse`

| Campo | Tipo | Descripción |
| :--- | :---: | :--- |
| `contenido` | `Array<PerfilBusquedaResponse>` | Lista de perfiles que cumplen con los filtros de búsqueda. |
| `paginaActual` | `integer` | Índice de la página devuelta (0-indexed). |
| `tamanoPagina` | `integer` | Cantidad de elementos por página solicitada (`20`, `50`, o total si es sin paginar). |
| `totalElementos` | `integer` (Long) | Cantidad total de registros que coinciden con los criterios aplicados. |
| `totalPaginas` | `integer` | Total de páginas disponibles según el tamaño configurado. |
| `primera` | `boolean` | `true` si es la primera página. Útil para deshabilitar botón "Anterior". |
| `ultima` | `boolean` | `true` si es la última página. Útil para deshabilitar botón "Siguiente". |

---

## 4. Respuestas de Error y Códigos HTTP

| Código HTTP | Causa | Manejo en Frontend |
| :---: | :--- | :--- |
| `200 OK` (vacío) | No hay perfiles que coincidan con los criterios aplicados (`totalElementos: 0`, `contenido: []`). | Mostrar estado vacío ("Empty State") con opción de "Limpiar filtros". |
| `401 Unauthorized` | El usuario no tiene sesión iniciada o el token expiró. | Redirigir al usuario a la pantalla de Login. |
| `403 Forbidden` | Usuario deshabilitado o bloqueado. | Mostrar mensaje de cuenta inhabilitada. |
| `500 Internal Server Error` | Fallo no previsto del servidor. | Mostrar notificación Toast: *"Ocurrió un error al buscar perfiles. Inténtalo nuevamente."* |

---

## 5. Casos de Uso y Comportamiento UI/UX Recomendado

### 5.1. Barra de Búsqueda y Filtros
* **Debounce en Búsqueda por Texto:** Para búsquedas en tiempo real en los campos de texto (`nombreArtistico`, `nombre`, etc.), aplicar un debounce de **300ms a 400ms** antes de emitir la petición a la API para no sobrecargar el servidor.
* **Limpiar Filtros:** Botón visible que restablezca todos los filtros a sus valores iniciales y re-ejecute la búsqueda por defecto (`page=0`, `size=20`).

### 5.2. Selector de Paginación
* Ubicar en la cabecera o pie de la grilla un selector con:
  * `20 por página`
  * `50 por página`
  * `Ver todos`
* Al cambiar el tamaño de página (`size` o activar `todos`), se debe reiniciar a la página `0` (`page=0`).

### 5.3. Tarjeta de Perfil en la Grilla (Card View)
Cada elemento del array `contenido` contiene los datos listos para renderizar:
* **Avatar / Foto:** Utilizar `fotoUrl`. Si es `null`, mostrar avatar genérico con las iniciales de `nombreArtistico`.
* **Título:** `nombreArtistico`.
* **Subtítulo:** `profesion` (badge o texto destacado).
* **Nombre Real:** `nombreUsuario` `apellidoUsuario` (opcionalmente visible según diseño).
* **Ubicación:** `localidad, provincia` con ícono de geolocalización.
* **Badges de Habilidades:** Chips con los nombres de `habilidades` (mostrar hasta 3 o 4 y un "+N más").
* **Acción Principal:** Botón *"Ver Perfil"* que navegue a `/perfiles/:idPerfil`.

### 5.4. Estado sin Resultados (Empty State)
Cuando `totalElementos === 0`:
* Ilustración o ícono de búsqueda sin resultados.
* Mensaje amigable: *"No encontramos ningún perfil que coincida con tus criterios de búsqueda."*
* Botón de acción: *"Restablecer filtros"*.
