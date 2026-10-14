# Ficha Técnica Frontend: Búsqueda de Proyectos Propios (UC-29 bis)

Especificación de la API para el catálogo "Proyectos / Inicio": lista los proyectos en los que el **perfil activo** de la sesión participa como director o miembro, con filtros opcionales y paginación.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `GET`
* **URL:** `/proyectos/propios`
* **Content-Type:** `application/json` (respuesta)
* **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`).
* **Perfil activo requerido:** Sí. El id del perfil activo se resuelve en el backend desde la sesión; el frontend **no** lo envía como parámetro.
* **Cota de tiempo:** 2 s (misma cota de búsqueda de proyectos del UC-29).

---

## 2. Query Params

| Parámetro | Tipo | Obligatorio | Default | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `nombre` | `string` | No | — | Coincidencia parcial del nombre del proyecto, insensible a mayúsculas. |
| `estado` | `enum` | No | — | `BORRADOR` \| `PUBLICADO` \| `CONFIRMADO` \| `FINALIZADO` \| `CANCELADO`. |
| `rol` | `enum` | No | — | `DIRECTOR` \| `MIEMBRO`. Filtra proyectos según el rol del perfil activo en la membresía. |
| `page` | `int` | No | `0` | Página (base 0). |
| `size` | `int` | No | `20` | Tamaño de página. |
| `todos` | `boolean` | No | `false` | `true` (o `size ≤ 0`) devuelve todos los resultados sin paginar. |

A diferencia del UC-29 global, **no se requiere ningún criterio obligatorio**: sin parámetros se listan todos los proyectos propios (orden alfabético por nombre).

Ejemplo:

```
GET /proyectos/propios?nombre=desfile&rol=DIRECTOR&page=0&size=20
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
      "rolUsuario": "DIRECTOR",
      "proximaActividad": {
        "idActividad": 210,
        "nombre": "Ensayo general",
        "fechaHoraInicio": "2026-05-30T18:00:00",
        "duracionMinutos": 120
      }
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
* `rolUsuario`: enum `DIRECTOR` o `MIEMBRO` (rol del **perfil activo** en el proyecto).
* `proximaActividad`: la próxima actividad **futura** del perfil activo en ese proyecto (estado `PENDIENTE` o `EN_CURSO`, `fechaHoraInicio` posterior a "ahora"); puede ser `null` si no hay actividad futura asignada.
* Horario de la actividad: derivar de `fechaHoraInicio` + `duracionMinutos` (p. ej. `18:00–20:00`); el backend no envía la hora de fin calculada.

---

## 4. Manejo de Errores

| Código HTTP | Excepción Backend | Causa / Mensaje | Acción Frontend |
| :---: | :--- | :--- | :--- |
| **404** | `PerfilActivoNoSeleccionadoException` | La sesión no tiene perfil activo seleccionado: *"No hay un perfil activo seleccionado en la sesión."* | Invitar a elegir perfil antes de mostrar el catálogo. |
| **400** | `MethodArgumentTypeMismatch` | `estado` o `rol` con valor fuera del enum. | Validar los selects. |
| **200** (lista vacía) | — | Sin proyectos propios o sin coincidencias: `contenido: []`, `totalElementos: 0`. | **No** es error: mostrar estado "sin proyectos" / "sin resultados". |
| **401/403** | Seguridad | Sesión ausente o inválida. | Redirigir a login. |

---

## 5. Consideraciones de UX

1. **Sin criterio obligatorio:** el catálogo se puede cargar vacío de filtros; el botón *Buscar* (si existe) puede estar siempre habilitado o deshabilitarse solo si todos los filtros están en blanco y no se desea refrescar.
2. **Filtro de rol (checkbox "ambos/ninguno"):** según el mockup, el control de rol puede tener tres estados. Si el usuario marca "ambos" (o ninguno), **no se envía el parámetro `rol`**; solo enviarlo con `DIRECTOR` o `MIEMBRO` cuando haya una selección explícita.
3. **Tarjeta de proyecto:** mostrar nombre, estado (badge), fecha de inicio, chip del rol propio (`DIRECTOR`/`MIEMBRO`) y la próxima actividad (nombre + rango horario) o un placeholder "Sin actividad próxima".
4. **Paginación:** usar `page`/`size` estándar; para "ver todo", pedir con `todos=true`.
5. **Diferencia con UC-29:** `/proyectos/buscar` es búsqueda global (≥1 criterio); este endpoint es solo el catálogo del perfil activo. No reutilizar la validación de "al menos un criterio" aquí.
