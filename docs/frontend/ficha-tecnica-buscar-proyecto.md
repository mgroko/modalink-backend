# Ficha Técnica Frontend: Búsqueda de Proyectos (UC-29)

Especificación de la API para el listado/búsqueda de proyectos con criterios de filtrado y paginación parametrizable, aplicando la regla de visibilidad del proyecto.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `GET`
* **URL:** `/proyectos/buscar`
* **Content-Type:** `application/json` (respuesta)
* **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`).
* **Permisos:** cualquier sesión válida. **Requiere perfil activo seleccionado** (se resuelve desde la sesión; el frontend no envía el id).
* **Cota de tiempo:** 2 s (paso 3 del UC-29).

---

## 2. Visibilidad (regla base del listado)

Un proyecto aparece en los resultados si:

* el **perfil activo es miembro activo** del proyecto (cualquier estado o privacidad), **o**
* el proyecto **no está en Borrador** y su privacidad es **Público**.

Consecuencias prácticas:

* **Borrador** solo es visible para sus miembros (el director siempre ve sus borradores — permite los flujos UC-25/35/36/37).
* **Privado** (aún publicado) solo es visible para sus miembros. *Comportamiento interino aprobado por producto:* el requerimiento final (UC-25) lo hará visible también a la red de contactos del director cuando exista el módulo de contactos (UC-43..46, segunda iteración).
* Los proyectos invisibles se **omiten** del listado (no hay error 403 por elemento).

---

## 3. Query Params

| Parámetro | Tipo | Obligatorio | Default | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `nombre` | `string` | No | — | Coincidencia parcial del nombre del proyecto, insensible a mayúsculas. |
| `estado` | `enum` | No | — | `BORRADOR` \| `PUBLICADO` \| `CONFIRMADO` \| `FINALIZADO` \| `CANCELADO`. Filtrar `BORRADOR` sin ser miembro devuelve vacío (la visibilidad es un filtro AND aparte). |
| `director` | `string` | No | — | Coincidencia parcial sobre nombre artístico o nombre/apellido del director activo del proyecto. |
| `page` | `int` | No | `0` | Página (base 0). |
| `size` | `int` | No | `20` | Tamaño de página. |
| `todos` | `boolean` | No | `false` | `true` (o `size ≤ 0`) devuelve todos los resultados sin paginar. |

Todos los criterios de filtrado son **opcionales** (consistente con "Buscar perfil"). Sin parámetros, se listan todos los proyectos visibles para el perfil activo. Filtrar `BORRADOR` sin ser miembro devuelve vacío (la visibilidad es un filtro AND aparte).

Ejemplo:

```
GET /proyectos/buscar?nombre=desfile&estado=PUBLICADO&director=luna&page=0&size=20
```

---

## 4. Respuesta Exitosa (`200 OK`)

Formato `PaginaResponse` estándar:

```json
{
  "contenido": [
    {
      "idProyecto": 105,
      "nombre": "Desfile Primavera",
      "descripcion": "Colección primavera-verano",
      "fechaInicio": "2026-06-01",
      "estado": "Publicado",
      "privacidad": "PUBLICO",
      "aceptaPostulacionGral": true,
      "idDirector": 4,
      "nombreDirector": "Luna Diseños"
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

* `estado` se devuelve con la forma legible (`Borrador`, `Publicado`, `Confirmado`, `Finalizado`, `Cancelado`); el filtro de entrada usa el nombre del enum en mayúsculas.
* `idDirector` / `nombreDirector` pueden ser `null` si el proyecto no tiene director activo.

---

## 5. Manejo de Errores

| Código HTTP | Excepción Backend | Causa / Mensaje | Acción Frontend |
| :---: | :--- | :--- | :--- |
| **404** | `PerfilActivoNoSeleccionadoException` | La sesión no tiene perfil activo: *"No hay un perfil activo seleccionado en la sesión."* | Invitar a elegir perfil antes de buscar. |
| **400** | `MethodArgumentTypeMismatch` | `estado` con valor fuera del enum. | Validar el select de estados. |
| **200** (lista vacía) | — | Sin coincidencias o todo oculto por visibilidad: `contenido: []`, `totalElementos: 0`. | **No** es error: mostrar estado "sin resultados" y volver al paso 1 del UC (mantener filtros visibles). |
| **401/403** | Seguridad | Sesión ausente o inválida. | Redirigir a login. |

---

## 6. Consideraciones de UX

1. **Criterios opcionales:** búsqueda "vacía" (sin parámetros) lista todos los proyectos visibles para el perfil activo, consistente con "Buscar perfil". El botón *Buscar* puede estar siempre habilitado o deshabilitarse solo si todos los filtros están en blanco y no se desea refrescar.
2. **Búsqueda sin resultados:** el UC indica notificar al actor y volver al paso 1 o finalizar; mostrar el mensaje sobre la tabla conservando los filtros.
3. **Paginación:** usar `page`/`size` estándar; para exportar o "ver todo", pedir con `todos=true`.
4. **Debounce:** sugerido 300–400 ms en los inputs de texto (nombre/director).
