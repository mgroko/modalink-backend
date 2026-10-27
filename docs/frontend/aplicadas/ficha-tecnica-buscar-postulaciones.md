# Ficha Técnica Frontend — UC-74 Buscar postulaciones

Especificación de la API para que un **director de proyecto** consulte las
postulaciones de su proyecto, con filtros, paginación y estado de cupo.

---

## 1. Resumen rápido

| Aspecto | Valor |
| :--- | :--- |
| Método | `GET` |
| Ruta | `/proyectos/{idProyecto}/postulaciones/buscar` |
| Autenticación | Cookie `jwt` + header `X-XSRF-TOKEN` |
| Perfil activo | Requerido |
| Autorización | Director activo del proyecto con `GESTIONAR_POSTULACIONES` |
| Respuesta | `PaginaResponse<PostulacionBusquedaResponse>` |
| Errores esperables | `400`, `401`, `403`, `404` |

---

## 2. Endpoint

```http
GET /proyectos/105/postulaciones/buscar
```

Parámetros opcionales:

| Parámetro | Tipo | Descripción |
| :--- | :--- | :--- |
| `estado` | `string` repetible | `PENDIENTE`, `ACEPTADA`, `RECHAZADA` |
| `tipo` | `string` | `GENERAL` o `ACTIVIDAD` |
| `nombrePerfil` | `string` | Búsqueda parcial por nombre artístico |
| `idProfesion` | `long` | Profesión del postulante |
| `fechaDesde` | `YYYY-MM-DD` | Fecha de postulación desde |
| `fechaHasta` | `YYYY-MM-DD` | Fecha de postulación hasta |
| `page` | `int` | Página, desde `0` (default `0`) |
| `size` | `int` | Tamaño de página (default `20`) |
| `todos` | `boolean` | Si es `true`, ignora paginación (default `false`) |

Ejemplos:

```http
GET /proyectos/105/postulaciones/buscar?estado=PENDIENTE
GET /proyectos/105/postulaciones/buscar?tipo=ACTIVIDAD&estado=PENDIENTE&estado=ACEPTADA
GET /proyectos/105/postulaciones/buscar?nombrePerfil=Luna&page=0&size=10
```

---

## 3. Orden

1. `PENDIENTE`
2. `ACEPTADA`
3. `RECHAZADA`
4. Dentro de cada estado, `fechaPostulacion` descendente.

La búsqueda está acotada al proyecto: no devuelve postulaciones de otros
proyectos aunque el perfil sea director en ellos.

---

## 4. Respuesta

```json
{
  "contenido": [
    {
      "idPostulacion": 77,
      "tipo": "GENERAL",
      "estado": "PENDIENTE",
      "fechaPostulacion": "2026-10-27T10:15:00",
      "idPerfil": 12,
      "nombreArtistico": "Luna",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "idRequerimiento": 30,
      "descripcionRequerimiento": "Vacante para desfile",
      "idActividad": null,
      "nombreActividad": null,
      "cantidadRequerida": 2,
      "cubiertos": 1,
      "faltantes": 1,
      "satisfecho": false,
      "aportaCupo": false,
      "puedeAceptar": true,
      "puedeRechazar": true
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

### Campos derivados

| Campo | Significado |
| :--- | :--- |
| `cantidadRequerida` | Cupo total del requerimiento. |
| `cubiertos` | Miembros/asignaciones activos que ocupan el requerimiento. |
| `faltantes` | `max(0, cantidadRequerida - cubiertos)`. |
| `satisfecho` | `cubiertos >= cantidadRequerida`. |
| `aportaCupo` | `true` si la postulación está `ACEPTADA` y su miembro/asignación sigue activa. |
| `puedeAceptar` | Sugerencia de UI; el backend revalida siempre. |
| `puedeRechazar` | La postulación está `PENDIENTE` y el proyecto permite gestionar. |

> `puedeAceptar` y `puedeRechazar` son ayudas visuales. Un `409` del endpoint de
> gestión sigue siendo la fuente de verdad.

---

## 5. Reglas de autorización

- El perfil activo debe pertenecer al usuario autenticado y estar `ACTIVO`.
- Debe ser director activo del proyecto.
- Debe tener el permiso `GESTIONAR_POSTULACIONES`.

Un miembro `MIEMBRO` que no sea director recibe `403`.

---

## 6. Errores

| HTTP | Caso | Mensaje / origen | Acción UI |
| :---: | :--- | :--- | :--- |
| `400` | Rango de fechas inválido | `La fecha desde no puede ser posterior a la fecha hasta.` | Corregir filtros |
| `401` | Sin sesión | Seguridad | Redirigir a login |
| `403` | No es director o no tiene permiso | `AccesoDenegadoProyectoException` | No mostrar el apartado |
| `404` | Sin perfil activo | `PerfilActivoNoSeleccionadoException` | Seleccionar perfil |
| `404` | Proyecto inexistente | `ProyectoNoEncontradoException` | Volver al listado |

---

## 7. Recomendaciones de UI/UX

- Mostrar tabs o chips por estado, con `PENDIENTE` seleccionado por defecto.
- Usar `puedeAceptar`/`puedeRechazar` para habilitar acciones, pero refrescar
  ante cualquier `409`.
- Mostrar `x / y` (cubiertos / cantidad) y una etiqueta “Cupo completo” cuando
  `satisfecho = true`.
- Si `aportaCupo = false` y `estado = ACEPTADA`, indicar que el miembro ya no
  está activo; el cupo puede estar libre.
- En postulaciones de actividad, mostrar el nombre de la actividad.
- Mantener `estado` en mayúsculas (`PENDIENTE`) y no mezclarlo con el estado del
  proyecto (`Publicado`/`Confirmado`).
- No ofrecer invitaciones todavía: UC-31 no está implementado.

---

## 8. Checklist de aceptación

- [ ] El endpoint se llama solo para un proyecto concreto.
- [ ] Se envían filtros opcionales correctamente, incluido `estado` repetible.
- [ ] Se respeta la paginación y `todos`.
- [ ] Se muestra el orden `PENDIENTE` → `ACEPTADA` → `RECHAZADA`.
- [ ] Se muestran `cubiertos`, `faltantes` y `satisfecho`.
- [ ] Se distingue una aceptada con miembro inactivo mediante `aportaCupo`.
- [ ] Un `403` no se reintenta.
- [ ] Un `400` de fechas muestra el mensaje del backend.
- [ ] No se muestran postulaciones de otros proyectos.
