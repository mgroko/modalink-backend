# Ficha Técnica Frontend: Búsqueda de Proyectos (UC-29)

Especificación de la API para el listado/búsqueda de proyectos con criterios de filtrado y paginación parametrizable.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `GET`
* **URL:** `/proyectos/buscar`
* **Content-Type:** `application/json` (respuesta)
* **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`).
* **Permisos:** cualquier sesión válida (usuario, director de proyecto o miembro de proyecto). No requiere perfil activo.
* **Cota de tiempo:** 2 s (paso 3 del UC-29).

---

## 2. Query Params

| Parámetro | Tipo | Obligatorio | Default | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `nombre` | `string` | **Al menos uno de los tres** | — | Coincidencia parcial del nombre del proyecto, insensible a mayúsculas. |
| `estado` | `enum` | **Al menos uno de los tres** | — | `BORRADOR` \| `PUBLICADO` \| `CONFIRMADO` \| `FINALIZADO` \| `CANCELADO`. |
| `director` | `string` | **Al menos uno de los tres** | — | Coincidencia parcial sobre nombre artístico o nombre/apellido del director activo del proyecto. |
| `page` | `int` | No | `0` | Página (base 0). |
| `size` | `int` | No | `20` | Tamaño de página. |
| `todos` | `boolean` | No | `false` | `true` (o `size ≤ 0`) devuelve todos los resultados sin paginar. |

Ejemplo:

```
GET /proyectos/buscar?nombre=desfile&estado=PUBLICADO&director=luna&page=0&size=20
```

---

## 3. Respuesta Exitosa (`200 OK`)

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

## 4. Manejo de Errores

| Código HTTP | Excepción Backend | Causa / Mensaje | Acción Frontend |
| :---: | :--- | :--- | :--- |
| **400** | `CriterioBusquedaInvalidoException` | Ningún criterio informado (o todos en blanco): *"Debes indicar al menos un criterio de búsqueda: nombre, estado o director."* | Resaltar los campos de filtro y pedir al menos uno. |
| **400** | `MethodArgumentTypeMismatch` | `estado` con valor fuera del enum. | Validar el select de estados. |
| **200** (lista vacía) | — | Sin coincidencias: `contenido: []`, `totalElementos: 0`. | **No** es error: mostrar estado "sin resultados" y volver al paso 1 del UC (mantener filtros visibles). |
| **401/403** | Seguridad | Sesión ausente o inválida. | Redirigir a login. |

---

## 5. Consideraciones de UX

1. **Al menos un criterio:** habilitar el botón *Buscar* solo si `nombre`, `estado` o `director` tienen valor (coincide con la validación del backend).
2. **Búsqueda sin resultados:** el UC indica notificar al actor y volver al paso 1 o finalizar; mostrar el mensaje sobre la tabla conservando los filtros.
3. **Paginación:** usar `page`/`size` estándar; para exportar o "ver todo", pedir con `todos=true`.
4. **Debounce:** sugerido 300–400 ms en los inputs de texto (nombre/director) respetando la cota de 2 s del UC.
