# Ficha técnica - Historial de proyectos del perfil

> **Estado:** backend implementado y testeado. Frontend pendiente.
> **Casos de uso relacionados:** UC-14 (extensión), UC-35, UC-37.
> **Endpoint principal:** `GET /perfiles/{idPerfil}/proyectos/historial`

## 1. Objetivo

Mostrar en la ficha de un perfil los proyectos en los que participó y que ya
cerraron, tanto **finalizados** como **cancelados**. Cada ítem muestra el
nombre del proyecto, su estado, el rol desempeñado y la fecha de cierre.

El historial se calcula a partir de las membresías del perfil en
`miembros_proyecto` y del estado/fechas de `proyecto`. No existe una tabla de
historial separada.

## 2. Endpoint

```http
GET /perfiles/{idPerfil}/proyectos/historial?page=0&size=10
```

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---:|---|
| `idPerfil` | path, entero | Sí | Perfil cuyo historial se consulta. |
| `page` | query, entero | No | Página, desde 0. Default `0`. |
| `size` | query, entero | No | Tamaño de página, 1 a 50. Default `10`. |

Requiere sesión iniciada con rol `USUARIO`.

## 3. Respuesta

Se devuelve `PaginaResponse<HistorialProyectoPerfilResponse>`.

```json
{
  "contenido": [
    {
      "idProyecto": 25,
      "nombre": "Editorial Otoño",
      "estado": "Finalizado",
      "rol": "Modelo",
      "rolCodigo": "MODELO",
      "fechaFinalizacion": "2026-03-15T18:00:00",
      "fechaCancelacion": null,
      "puedeVerProyecto": true
    },
    {
      "idProyecto": 26,
      "nombre": "Campaña Cancelada",
      "estado": "Cancelado",
      "rol": "Modelo",
      "rolCodigo": "MODELO",
      "fechaFinalizacion": null,
      "fechaCancelacion": "2026-04-01T10:00:00",
      "puedeVerProyecto": false
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 10,
  "totalElementos": 2,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

### Campos

| Campo | Tipo | Descripción |
|---|---|---|
| `idProyecto` | entero | Identificador del proyecto. |
| `nombre` | string | Nombre visible del proyecto. |
| `estado` | string | `Finalizado` o `Cancelado`. |
| `rol` | string/null | Rol visible del perfil en el proyecto. |
| `rolCodigo` | string/null | Código de profesión o `DIRECTOR`. |
| `fechaFinalizacion` | ISO-8601/null | Fecha de finalización, si aplica. |
| `fechaCancelacion` | ISO-8601/null | Fecha de cancelación, si aplica. |
| `puedeVerProyecto` | boolean | Indica si el perfil activo puede abrir el detalle del proyecto. |

## 4. Reglas de negocio

- Solo se incluyen proyectos en estado `FINALIZADO` o `CANCELADO`.
- Solo se incluyen membresías con `estado_participacion = ACTIVO`.
- No se incluyen miembros con `BAJA_VOLUNTARIA` o `ELIMINADO`.
- El orden es por fecha de cierre descendente:
  - `fecha_finalizacion` para finalizados;
  - `fecha_cancelacion` para cancelados;
  - si ambas son nulas, el proyecto queda al final.
- El rol se resuelve con esta prioridad:
  1. profesión del requerimiento general del proyecto;
  2. `Director` si el rol de proyecto es director;
  3. profesión del perfil;
  4. `null`.
- `puedeVerProyecto` se calcula con las reglas de visibilidad de proyectos:
  - miembro activo;
  - exmiembro de proyecto cancelado;
  - proyecto público;
  - proyecto privado visible por contacto con un director.
- El historial incluye proyectos privados, pero el frontend no debe mostrar el
  enlace al proyecto si `puedeVerProyecto = false`.
- Si el perfil no es visible según UC-14, el endpoint responde `404`.
- Un perfil sin historial devuelve `200` con página vacía.

## 5. Errores

| HTTP | Caso |
|---|---|
| `401` | Sin sesión. |
| `403` | Sin rol `USUARIO`. |
| `404` | Perfil inexistente, en baja o no visible. |
| `500` | Error interno. |

## 6. Frontend

- Consumir el endpoint al abrir la sección “Historial de proyectos” del perfil.
- Mantener `page` y `size`.
- Mostrar el estado como badge:
  - `Finalizado`;
  - `Cancelado`.
- Mostrar la fecha como “Mes y año” usando:
  - `fechaFinalizacion` si el estado es `Finalizado`;
  - `fechaCancelacion` si el estado es `Cancelado`.
- Si la fecha correspondiente es `null`, mostrar solo el badge y omitir la fecha.
- Mostrar `rol` entre el nombre del proyecto y la productora/datos secundarios.
- No mostrar “productora”: no existe en el modelo actual.
- Si `puedeVerProyecto = true`, permitir abrir el detalle del proyecto.
- Si `puedeVerProyecto = false`, mostrar la entrada como texto no clickeable.
- Estado vacío sugerido:
  > “Todavía no hay proyectos finalizados o cancelados en este perfil.”
- No duplicar este historial con el de publicaciones: son secciones distintas.

## 7. Notas de implementación

- Servicio: `ListarHistorialProyectosPerfilService`.
- Controller: `PerfilProyectoController`.
- DTO: `HistorialProyectoPerfilResponse`.
- Repositorio: `MiembroProyectoRepository.buscarHistorialProyectosDePerfil`.
- `proyecto.fecha_cancelacion` se completa al cancelar el proyecto.
- Los proyectos cancelados anteriores a la migración pueden tener
  `fecha_cancelacion = null`; el frontend debe tolerarlo.
