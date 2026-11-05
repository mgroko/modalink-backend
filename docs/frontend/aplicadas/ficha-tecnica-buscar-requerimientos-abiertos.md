# Ficha Técnica Frontend — Buscar requerimientos abiertos (UC-72/UC-73)

Especificación de los endpoints globales para descubrir requerimientos
generales y de actividad con cupo disponible, y del bloque de oportunidades del
Home.

---

## 1. Resumen rápido

| Aspecto | Valor |
| :--- | :--- |
| Métodos | `GET` |
| Rutas | `/requerimientos-generales/buscar`, `/requerimientos-actividad/buscar` |
| Autenticación | Cookie `jwt` + header `X-XSRF-TOKEN` |
| Perfil activo | Requerido |
| Respuesta | `PaginaResponse<RequerimientoOportunidadResponse>` |
| Home | `GET /home/resumen?limite=5` |
| Errores esperables | `400`, `401`, `404` |

Las dos rutas alimentan las pestañas **Generales** y **Actividades** del
buscador. No reemplazan el endpoint por proyecto
`GET /proyectos/{idProyecto}/requerimientos-actividad/buscar`, que sigue
existiendo para el flujo de invitaciones.

---

## 2. Alcance funcional

Un resultado es un **requerimiento abierto para postularse**:

- Requerimiento no archivado.
- Proyecto `Publicado` o `Confirmado` y visible para el perfil activo.
- Requerimiento general: el proyecto acepta postulaciones generales.
- Requerimiento de actividad: la actividad está `Pendiente`.
- Cupo disponible (`cantidad > cubiertos`).
- Sin filtros, se listan todas las oportunidades visibles. Las de la profesión
  del perfil activo se ordenan primero.
- Se pueden buscar otras profesiones. Se muestran, pero con
  `puedePostularse = false` y `motivoNoPostulable = PROFESION`.
- Las postulaciones previas no se ocultan: se devuelve `yaPostulado = true` y
  `estadoPostulacion`. La UI debe deshabilitar la acción.
- Las características técnicas y habilidades son filtros informativos; no
  bloquean la postulación.

---

## 3. Query params

| Parámetro | Tipo | Descripción |
| :--- | :--- | :--- |
| `idProfesion` | `long` | Profesión del requerimiento |
| `idProyecto` | `long` | Acotar a un proyecto |
| `idActividad` | `long` | Sólo actividades |
| `texto` | `string` | Proyecto, actividad, descripción o profesión |
| `idCaracteristica` | `long` | Requerimientos que exigen esa característica |
| `idValorCaracteristica` | `long` | Valor enumerado de la característica |
| `idsHabilidades` | `long` repetible | Habilidades requeridas (coincidencia por cualquiera) |
| `soloMiProfesion` | `boolean` | Filtra por la profesión del perfil activo |
| `soloPostulables` | `boolean` | Excluye miembros activos (general) y directores (actividad) |
| `excluirYaPostulados` | `boolean` | Excluye requerimientos ya postulados |
| `page` | `int` | Página, desde `0` (default `0`) |
| `size` | `int` | Tamaño de página (default `20`) |
| `todos` | `boolean` | Si es `true`, ignora la paginación |

Ejemplos:

```http
GET /requerimientos-generales/buscar?idProfesion=2&page=0&size=20
GET /requerimientos-actividad/buscar?texto=vestuario&soloMiProfesion=true
GET /requerimientos-actividad/buscar?idProyecto=105&idActividad=200
```

---

## 4. Respuesta

```json
{
  "contenido": [
    {
      "tipo": "ACTIVIDAD",
      "idRequerimiento": 20,
      "cantidad": 1,
      "descripcion": "Una modelo",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "idProyecto": 105,
      "nombreProyecto": "Colección Madoka",
      "estadoProyecto": "Publicado",
      "idActividad": 200,
      "nombreActividad": "Prueba de vestuario",
      "estadoActividad": "PENDIENTE",
      "fechaInicioProyecto": "2026-11-08T09:00:00",
      "fechaEntrega": "2026-11-20",
      "fechaHoraInicioActividad": "2026-11-10T09:00:00",
      "fechaHoraFinActividad": "2026-11-10T11:00:00",
      "duracionMinutos": 120,
      "cubiertos": 0,
      "faltantes": 1,
      "satisfecho": false,
      "caracteristicas": [],
      "habilidades": [],
      "puedePostularse": true,
      "motivoNoPostulable": null,
      "yaPostulado": false,
      "estadoPostulacion": null,
      "requiereVerificacionDisponibilidad": true
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

### Campos de decisión

| Campo | Uso |
| :--- | :--- |
| `puedePostularse` | Fuente de verdad estructural: proyecto, actividad, membresía/rol, profesión y cupo |
| `motivoNoPostulable` | `PROYECTO_NO_HABILITADO`, `PROYECTO_NO_ACEPTA_GENERAL`, `ACTIVIDAD_NO_PENDIENTE`, `MIEMBRO_ACTIVO`, `DIRECTOR`, `PROFESION`, `CUPO` |
| `yaPostulado` | Ya existe una postulación del perfil, sin importar su estado |
| `estadoPostulacion` | `PENDIENTE`, `ACEPTADA`, `RECHAZADA` cuando `yaPostulado = true` |
| `requiereVerificacionDisponibilidad` | Siempre `true` en actividades; la disponibilidad se valida al postularse |

La acción **Postularme** se habilita sólo con:

```text
puedePostularse === true && yaPostulado === false
```

`puedePostularse` no incluye postulaciones previas ni disponibilidad de
calendario. El `POST` de UC-26 sigue siendo la red de seguridad y puede
responder `409`.

---

## 5. Home

`GET /home/resumen?limite=5` agrega:

```json
{
  "perfilActivo": { },
  "proyectosDestacados": [],
  "publicacionesRecientes": [],
  "oportunidades": [ /* hasta 5 */ ],
  "totalOportunidadesGenerales": 4,
  "totalOportunidadesActividad": 7
}
```

Reglas del bloque:

- Sólo oportunidades de la profesión del perfil activo.
- Sólo `soloPostulables = true`.
- Las no postuladas se ordenan primero; las ya postuladas se muestran con su
  estado y acción deshabilitada.
- Orden: fecha más próxima, faltantes descendentes, tipo e id.
- Sin perfil activo, listas vacías y contadores en `0`.

“Ver todas” debe abrir la pestaña correspondiente del buscador con la profesión
precargada.

---

## 6. Errores

| HTTP | Caso | Acción frontend |
| :---: | :--- | :--- |
| `401` | Sesión ausente o expirada | Redirigir a login |
| `404` | Sin perfil activo seleccionado | Abrir selección de perfil |
| `404` | Perfil inexistente | Avisar y volver a seleccionar perfil |
| `409` | Perfil inactivo | Avisar que el perfil está en baja |
| `400` | Enum o parámetro inválido | Validar filtros en el cliente |
| `200` vacío | Sin coincidencias | Mostrar estado “sin resultados” conservando filtros |

---

## 7. UI sugerida

1. Pestañas **Generales** y **Actividades**.
2. Filtro de profesión precargado con la del perfil activo; permitir limpiarlo
   para explorar todas.
3. Chips de características y habilidades.
4. Tarjeta con proyecto, actividad, profesión, descripción, cupos restantes y
   fechas.
5. Botón **Postularme** sólo si `puedePostularse && !yaPostulado`.
6. Si `yaPostulado`, mostrar chip de estado y acción deshabilitada.
7. Si `motivoNoPostulable = PROFESION`, mostrar “Tu profesión no coincide”.
8. Si `motivoNoPostulable = MIEMBRO_ACTIVO` o `DIRECTOR`, explicar la regla.
9. En actividades, avisar que la disponibilidad se confirmará al postularse.
