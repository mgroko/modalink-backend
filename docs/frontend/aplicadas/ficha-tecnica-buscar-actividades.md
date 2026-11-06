# Ficha Técnica Frontend: Buscar Actividades (UC-52)

> **Nota (2026-10-27):** este endpoint sigue siendo la búsqueda de actividades
> para candidatos y otras vistas. La pestaña **Planificación** usa
> `GET /proyectos/{idProyecto}/actividades` (tabla liviana con responsables) y la
> pestaña **Cronograma** usa `GET /proyectos/{idProyecto}/cronograma`.
> Ver `ficha-tecnica-planificacion-actividades.md` y
> `ficha-tecnica-visualizar-cronograma.md`.

Especificación de la API para buscar actividades dentro de la planificación de un
proyecto, aplicando filtros opcionales, paginación y la misma regla de visibilidad
que el detalle del proyecto (UC-25).

**Estado backend:** implementado y testeado.  
**Última actualización:** 2026-10-27.

---

## 1. Contexto funcional

El usuario, con un **perfil activo seleccionado**, busca actividades de un proyecto
para consultarlas o seleccionarlas en flujos posteriores de planificación.

Características principales:

- La búsqueda está **acotada a un proyecto**: siempre se indica `idProyecto` en la URL.
- Todos los filtros son **opcionales** y se combinan con `AND`.
- Sin filtros, devuelve todas las actividades visibles del proyecto.
- El orden es fijo: `fechaHoraInicio ASC` y, como desempate, `idActividad ASC`.
- Cada actividad incluye un resumen de sus requerimientos y de sus predecesoras.
- No hay resultados de tipo "encontrado/no encontrado": sin coincidencias se
  devuelve `200 OK` con una página vacía.

---

## 2. Endpoint y autenticación

| Dato | Valor |
| :--- | :--- |
| **Método** | `GET` |
| **URL** | `/proyectos/{idProyecto}/actividades/buscar` |
| **Content-Type** | `application/json` (respuesta) |
| **Autenticación** | Sesión válida (JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`). |
| **Perfil activo** | Obligatorio. Se resuelve desde la sesión; el frontend **no** envía `idPerfilActivo`. |
| **Permisos específicos** | Ninguno adicional. Basta con que el proyecto sea visible para el perfil activo. |

---

## 3. Visibilidad y acceso

Antes de buscar, el backend valida que el perfil activo pueda ver el proyecto:

| Situación del perfil | ¿Puede buscar en el proyecto? |
| :--- | :--- |
| Miembro **activo** del proyecto | Sí, en cualquier estado y privacidad. |
| Exmiembro de un proyecto **CANCELADO** con membresía histórica | Sí. |
| No miembro, proyecto **PUBLICO** y estado distinto de `BORRADOR` o `CANCELADO` | Sí. |
| No miembro, proyecto `BORRADOR` o `PRIVADO` | No: `403`. |
| No miembro, proyecto `CANCELADO` | No: `403`, salvo el caso de exmiembro. |

Si el proyecto no existe: `404` (`El proyecto con id {idProyecto} no existe.`).

---

## 4. Query params

| Parámetro | Tipo | Obligatorio | Default | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `nombre` | `string` | No | — | Coincidencia parcial, case-insensitive, sobre el nombre de la actividad. |
| `descripcion` | `string` | No | — | Coincidencia parcial, case-insensitive, sobre la descripción de la actividad. |
| `requerimiento` | `string` | No | — | Busca en el requerimiento de la actividad: nombre/código de profesión, descripción del requerimiento, nombre/código de habilidad o nombre/código de característica técnica. |
| `duracionMin` | `int` | No | — | Duración mínima en **minutos**, inclusive. |
| `duracionMax` | `int` | No | — | Duración máxima en **minutos**, inclusive. |
| `estado` | `enum` | No | — | Uno o varios estados: `PENDIENTE`, `EN_CURSO`, `FINALIZADA`, `CANCELADA`. Se envía repetido: `estado=PENDIENTE&estado=EN_CURSO`. |
| `page` | `int` | No | `0` | Página, base 0. |
| `size` | `int` | No | `20` | Tamaño de página. |
| `todos` | `boolean` | No | `false` | `true` devuelve todos los resultados sin paginar. También se evita paginar si `size <= 0`. |

### Semántica de filtros

- Los textos usan `LIKE` parcial e insensible a mayúsculas/minúsculas.
- Los caracteres `%`, `_` y `\` se escapan: se buscan como texto literal.
- `duracionMin` y `duracionMax` se comparan contra `duracionMinutos` de la actividad.
- `duracionMin` no puede ser mayor que `duracionMax`; valores negativos producen `400`.
- Los filtros de texto vacíos o en blanco se ignoran.
- `estado` acepta múltiples valores; si se omite, no filtra por estado.
- El filtro `requerimiento` no devuelve los requerimientos completos: solo se usa
  para decidir si la actividad coincide. El detalle resumido se devuelve igualmente
  en el campo `requerimientos`.

### Ejemplos

```http
GET /proyectos/105/actividades/buscar?nombre=montaje&estado=PENDIENTE&page=0&size=20
```

```http
GET /proyectos/105/actividades/buscar?requerimiento=modelo&duracionMin=60&duracionMax=120
```

```http
GET /proyectos/105/actividades/buscar?estado=FINALIZADA&estado=CANCELADA&todos=true
```

---

## 5. Respuesta exitosa (`200 OK`)

Formato `PaginaResponse` estándar:

```json
{
  "contenido": [
    {
      "idActividad": 10,
      "idProyecto": 105,
      "nombreProyecto": "Desfile Primavera",
      "nombre": "Montaje",
      "descripcion": "Montar escenario",
      "duracionMinutos": 90,
      "fechaHoraInicio": "2026-11-01T09:00:00",
      "fechaHoraFin": "2026-11-01T10:30:00",
      "estado": "PENDIENTE",
      "requerimientos": [
        {
          "idRequerimiento": 7,
          "cantidad": 2,
          "descripcion": "Dos modelos",
          "idProfesion": 2,
          "nombreProfesion": "Modelo",
          "cubiertos": 1,
          "faltantes": 1,
          "satisfecho": false,
          "puedePostularse": true
        }
      ],
      "predecesoras": [
        {
          "idActividad": 5,
          "nombre": "Casting"
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

### Campos de la actividad

| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `idActividad` | `number` | Identificador de la actividad. |
| `idProyecto` | `number` | Proyecto al que pertenece la actividad. |
| `nombreProyecto` | `string` | Nombre del proyecto, útil para migas de pan o títulos. |
| `nombre` | `string` | Nombre de la actividad. |
| `descripcion` | `string \| null` | Descripción de la actividad. |
| `duracionMinutos` | `number` | Duración en minutos. |
| `fechaHoraInicio` | `string` (ISO local) | Inicio de la actividad, por ejemplo `2026-11-01T09:00:00`. |
| `fechaHoraFin` | `string` (ISO local) | Calculado por el backend como `fechaHoraInicio + duracionMinutos`. |
| `estado` | `string` | `PENDIENTE`, `EN_CURSO`, `FINALIZADA` o `CANCELADA`. |
| `requerimientos` | `array` | Resumen de requerimientos de la actividad. Puede ser `[]`. |
| `predecesoras` | `array` | Actividades predecesoras. Puede ser `[]`. |

### Campos de `requerimientos[]`

| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `idRequerimiento` | `number` | Identificador del requerimiento de actividad. |
| `cantidad` | `number` | Cantidad solicitada. |
| `descripcion` | `string \| null` | Descripción del requerimiento. |
| `idProfesion` | `number` | Identificador de la profesión requerida. |
| `nombreProfesion` | `string` | Nombre de la profesión requerida. |
| `cubiertos` | `number` | Asignaciones activas vinculadas al requerimiento. |
| `faltantes` | `number` | `max(0, cantidad - cubiertos)`. |
| `satisfecho` | `boolean` | `true` si el cupo está completo. |
| `puedePostularse` | `boolean` | **Fuente de verdad por requerimiento.** El backend ya evaluó proyecto `Publicado`/`Confirmado`, actividad `PENDIENTE`, rol (`DIRECTOR` excluido, `MIEMBRO` permitido), profesión y cupo. El front sólo debe hacer `puedePostularse === true`. |

### Elegibilidad y límites de `puedePostularse`

`puedePostularse` es elegibilidad estructural. **No** incluye:

- que el perfil ya se haya postulado a ese requerimiento,
- la disponibilidad de calendario para el rango factible de la actividad.

Esos casos siguen respondiendo `409` al crear la postulación. La UI debe usar
`puedePostularse` para mostrar u ocultar la acción y manejar el `409` como red de
seguridad, sin recalcular profesión, cupo, rol ni estado.

### Campos de `predecesoras[]`

| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `idActividad` | `number` | Identificador de la actividad predecesora. |
| `nombre` | `string` | Nombre de la actividad predecesora. |

### Sin resultados

```json
{
  "contenido": [],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 0,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

No es un error: mostrar el estado "sin resultados" conservando los filtros.

---

## 6. Errores

| Código HTTP | Excepción / causa | Mensaje / comportamiento | Acción frontend |
| :---: | :--- | :--- | :--- |
| **400** | `RangoDuracionInvalidoException` | `La duración mínima y máxima no pueden ser negativas.` | Validar que la duración sea `>= 0` antes de enviar. |
| **400** | `RangoDuracionInvalidoException` | `La duración mínima no puede ser mayor que la duración máxima.` | Validar el rango en el formulario. |
| **400** | Tipo de dato inválido | `duracionMin`/`duracionMax` no numéricos o `estado` fuera del enum. | Usar `select` de estados y campos numéricos. |
| **403** | `AccesoDenegadoProyectoException` | `No tienes acceso a este proyecto.` | Ocultar el acceso o volver al detalle/listado; no insistir. |
| **404** | `PerfilActivoNoSeleccionadoException` | `No hay un perfil activo seleccionado en la sesión.` | Invitar a elegir un perfil activo. |
| **404** | `ProyectoNoEncontradoException` | `El proyecto con id {idProyecto} no existe.` | Volver al listado de proyectos. |
| **401/403** | Seguridad | Sesión ausente o inválida. | Redirigir a login. |

> El orden de validación es: perfil activo → existencia del proyecto → visibilidad.
> Por eso, sin perfil activo se devuelve `404` aunque el proyecto tampoco exista.

---

## 7. Consideraciones de UX

1. **Entrada natural:** abrir la búsqueda desde el detalle del proyecto o desde la
   planificación, precargando `idProyecto`.
2. **Filtros opcionales:** permitir buscar sin completar ningún campo; el botón
   *Buscar* puede estar siempre habilitado.
3. **Estados:** usar multi-select o chips para `PENDIENTE`, `EN_CURSO`,
   `FINALIZADA` y `CANCELADA`. El valor enviado debe ser el nombre del enum en
   mayúsculas.
4. **Duración:** mostrar la unidad "minutos" y validar `duracionMin <= duracionMax`.
5. **Debounce:** sugerido 300–400 ms en los inputs de texto
   (`nombre`, `descripcion`, `requerimiento`).
6. **Resultados:** mostrar nombre, descripción, fechas, duración, estado, proyecto,
   resumen de requerimientos y predecesoras. Las predecesoras pueden enlazar al
   detalle de la actividad si ese flujo está disponible.
7. **Paginación:** usar `page`/`size`; para exportar o "ver todas" usar
   `todos=true`.
8. **Sin coincidencias:** mostrar mensaje de "sin resultados" sobre la tabla o
   listado, manteniendo visibles los filtros aplicados.
9. **Errores de visibilidad:** un `403` significa que el proyecto no es accesible
   para el perfil activo; no reintentar automáticamente con otros parámetros.
10. **Formato de fechas:** el backend devuelve `LocalDateTime` sin zona horaria
    (`YYYY-MM-DDTHH:mm:ss`). Formatear en el cliente según el locale, sin asumir
    zona horaria ni sufijo `Z`.
11. **Gating de postulación:** usar `requerimiento.puedePostularse`, no
    `elegibleParaPostularse`. Si `puedePostularse = false` por profesión y no por
    cupo, no mostrar “Cupo completo”: usar un mensaje genérico como “No disponible
    para tu perfil”. El `409` de duplicado o disponibilidad sigue siendo posible.
