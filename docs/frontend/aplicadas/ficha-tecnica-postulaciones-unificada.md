# Ficha Técnica Frontend — Pestaña Postulaciones (unificada)

Especificación consolidada de la pestaña **Postulaciones** del detalle de
proyecto y de la pantalla global **Mis postulaciones**. Integra los contratos
de UC-26 (postularse), UC-27 (baja), UC-34 (gestionar), UC-52 (buscar
actividades) y UC-74 (buscar postulaciones), incluidos los cambios aditivos de
la última iteración.

---

## 1. Resumen

La pestaña `Postulaciones` reemplaza a la anterior `Actividades` y tiene dos
experiencias según el rol del perfil activo:

| Perfil | Experiencia |
| :--- | :--- |
| Director con `GESTIONAR_POSTULACIONES` | Gestiona las postulaciones del proyecto: tabs por estado con conteos, filtros, tabla/cards y acciones aceptar/rechazar. |
| Miembro `MIEMBRO` y no miembro | Ve un listado único con **todos los requerimientos del proyecto** (generales y de actividad) para postularse, y puede abrir el detalle de cada actividad. |

Además existe la pantalla global **Mis postulaciones**, donde el postulante ve
sus postulaciones y puede darlas de baja si están pendientes.

Todos los cambios de esta iteración son **aditivos**: no se renombraron ni se
quitaron campos, se mantiene `camelCase`, las fechas ISO-8601 locales y el
formato de error `{ message, httpStatus, timestamp }`.

---

## 2. Autenticación y reglas comunes

- Autenticación por cookie `jwt` + header `X-XSRF-TOKEN`.
- El backend resuelve `idUsuario` e `idPerfilActivo` desde la sesión.
- El frontend **no** envía `idPerfil`, `idUsuario` ni `idProyecto` en el body de
  las acciones de gestión.
- Sin perfil activo: `404` (`PerfilActivoNoSeleccionadoException`).
- Sin sesión: `401`.
- Proyecto privado o en borrador/cancelado: `403` para no miembros.
- Un miembro `MIEMBRO` no puede gestionar postulaciones: `403`.
- Los errores de negocio de gestión devuelven `409` con `message`.

---

## 3. Endpoints

| Método | Ruta | Uso |
| :--- | :--- | :--- |
| `GET` | `/proyectos/{idProyecto}/postulaciones/buscar` | Director: listar postulaciones con filtros, paginación y conteos por estado. |
| `POST` | `/proyectos/{idProyecto}/postulaciones/general/{idPostulacion}/aceptar` | Director: aceptar postulación general. |
| `POST` | `/proyectos/{idProyecto}/postulaciones/general/{idPostulacion}/rechazar` | Director: rechazar postulación general. |
| `POST` | `/proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/aceptar` | Director: aceptar postulación de actividad. |
| `POST` | `/proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/rechazar` | Director: rechazar postulación de actividad. |
| `POST` | `/proyectos/{idProyecto}/postulaciones/general` | Postulante: alta a requerimiento general. |
| `POST` | `/proyectos/{idProyecto}/postulaciones/actividad` | Postulante: alta a requerimiento de actividad. |
| `GET` | `/proyectos/{idProyecto}` | Requerimientos generales del proyecto. |
| `GET` | `/proyectos/{idProyecto}/actividades/buscar` | Actividades y sus requerimientos. |
| `GET` | `/proyectos/{idProyecto}/actividades/{idActividad}` | Detalle de actividad (ahora accesible a no miembros según visibilidad). |
| `GET` | `/postulaciones/mias` | Postulaciones emitidas por el perfil activo. |
| `DELETE` | `/proyectos/{idProyecto}/postulaciones/general/{idPostulacion}` | Baja de postulación general propia. |
| `DELETE` | `/proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}` | Baja de postulación de actividad propia. |

> `idPostulacion` de una postulación general y de una de actividad pertenecen a
> tablas distintas. Las rutas las distinguen por `general` o `actividad`.

---

## 4. Experiencia del director

### 4.1. Badge del encabezado

`GET /proyectos/{idProyecto}` incorpora:

```json
{
  "postulacionesPendientes": 2
}
```

Reglas:

- Solo viene informado para el director con `GESTIONAR_POSTULACIONES`.
- Es `null` para miembros, no miembros y directores sin ese permiso.
- Es `0` si el proyecto no tiene postulaciones pendientes.
- Sirve para el badge rojo de la tab `Postulaciones` sin abrirla.

### 4.2. Listado de postulaciones (UC-74)

```http
GET /proyectos/105/postulaciones/buscar
```

Parámetros opcionales:

| Parámetro | Tipo | Descripción |
| :--- | :--- | :--- |
| `estado` | `string` repetible | `PENDIENTE`, `ACEPTADA`, `RECHAZADA`. |
| `tipo` | `string` | `GENERAL` o `ACTIVIDAD`. |
| `nombrePerfil` | `string` | Búsqueda parcial por nombre artístico. |
| `idProfesion` | `long` | Profesión del postulante. |
| `fechaDesde` | `YYYY-MM-DD` | Fecha de postulación desde. |
| `fechaHasta` | `YYYY-MM-DD` | Fecha de postulación hasta. |
| `page` | `int` | Página, desde `0` (default `0`). |
| `size` | `int` | Tamaño de página (default `20`). |
| `todos` | `boolean` | Si es `true`, ignora la paginación. |

Ejemplos:

```http
GET /proyectos/105/postulaciones/buscar?estado=PENDIENTE&page=0&size=20
GET /proyectos/105/postulaciones/buscar?tipo=ACTIVIDAD&estado=PENDIENTE&estado=ACEPTADA
GET /proyectos/105/postulaciones/buscar?nombrePerfil=Luna&idProfesion=2
```

#### Respuesta

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
  "ultima": true,
  "conteosPorEstado": {
    "PENDIENTE": 2,
    "ACEPTADA": 3,
    "RECHAZADA": 1,
    "TOTAL": 6
  }
}
```

#### Conteos por estado

`conteosPorEstado` es la novedad principal para las tabs:

- Se calcula sobre **todo el proyecto**, no sobre la página.
- **Ignora** `page`, `size` y el filtro `estado`.
- **Respeta** `nombrePerfil`, `idProfesion`, `tipo`, `fechaDesde` y `fechaHasta`.
- `TOTAL` es `PENDIENTE + ACEPTADA + RECHAZADA`.
- Los cuatro valores son siempre números (`0` si no hay postulaciones).

Ejemplo de tabs:

```text
Pendiente · 2   Aceptada · 3   Rechazada · 1
```

#### Orden

1. `PENDIENTE`
2. `ACEPTADA`
3. `RECHAZADA`
4. Dentro de cada estado, `fechaPostulacion` descendente.

La búsqueda está acotada al proyecto: no devuelve postulaciones de otros
proyectos aunque el perfil sea director en ellos.

#### Campos derivados

| Campo | Significado |
| :--- | :--- |
| `cantidadRequerida` | Cupo total del requerimiento. |
| `cubiertos` | Miembros/asignaciones activos que ocupan el requerimiento. |
| `faltantes` | `max(0, cantidadRequerida - cubiertos)`. |
| `satisfecho` | `cubiertos >= cantidadRequerida`. |
| `aportaCupo` | `true` si está `ACEPTADA` y su miembro/asignación sigue activa. |
| `puedeAceptar` | Sugerencia de UI; el backend revalida siempre. |
| `puedeRechazar` | La postulación está `PENDIENTE` y el proyecto permite gestionar. |

> `puedeAceptar` y `puedeRechazar` son ayudas visuales. Un `409` del endpoint de
> gestión sigue siendo la fuente de verdad.

#### Autorización

- Perfil activo del usuario autenticado.
- Director activo del proyecto.
- Permiso `GESTIONAR_POSTULACIONES`.
- Un miembro `MIEMBRO` que no sea director recibe `403`.

### 4.3. Aceptar o rechazar (UC-34)

#### Flujo UI

1. El director pulsa **Aceptar** o **Rechazar** en una postulación pendiente.
2. Se abre un modal de confirmación con:
   - nombre del proyecto;
   - nombre artístico del postulante;
   - profesión;
   - requerimiento y cupo (`cubiertos` / `faltantes`);
   - si es de actividad, nombre y fecha/horario de la actividad.
3. Al confirmar, se envía el `POST` correspondiente (sin body).
4. El botón se deshabilita mientras la petición está en curso.
5. En éxito: cerrar modal, mostrar toast y refrescar listado + detalle del proyecto.

```http
POST /proyectos/105/postulaciones/general/77/aceptar
POST /proyectos/105/postulaciones/general/77/rechazar
POST /proyectos/105/postulaciones/actividad/88/aceptar
POST /proyectos/105/postulaciones/actividad/88/rechazar
```

#### Respuesta de gestión

```json
{
  "idPostulacion": 77,
  "tipo": "ACTIVIDAD",
  "estado": "ACEPTADA",
  "idProyecto": 105,
  "idRequerimiento": 20,
  "idPerfil": 12,
  "idMiembro": 88,
  "idAsignacionActividad": 45,
  "resultadoPreferencia": "EXACTO",
  "desvioMinutos": 0,
  "advertencias": []
}
```

En un rechazo, `idMiembro` y `idAsignacionActividad` son `null` y
`resultadoPreferencia` también puede ser `null`.

`resultadoPreferencia` puede ser:

| Valor | Significado |
| :--- | :--- |
| `SIN_PREFERENCIA` | La actividad no tiene horario de preferencia. |
| `EXACTO` | El inicio coincide con la hora preferida. |
| `DESVIO` | El inicio se desvía dentro de la tolerancia (`desvioMinutos`). |
| `SIN_LUGAR` | No hay hueco o queda fuera de tolerancia. |

`advertencias[]` son avisos no bloqueantes de posible sobrecompromiso:

```json
{
  "idActividad": 200,
  "nombreActividad": "Prueba de vestuario",
  "idProyecto": 101,
  "nombreProyecto": "Otra colección",
  "inicioPotencial": "2026-11-02T09:00:00",
  "finPotencial": "2026-11-02T11:00:00"
}
```

#### Efectos

- **Aceptar general:** crea un `MiembroProyecto` activo con rol `MIEMBRO`,
  vincula la membresía al requerimiento, cambia la postulación a `ACEPTADA` y
  notifica. Si se completa el cupo, las demás pendientes del mismo
  requerimiento se rechazan automáticamente.
- **Aceptar actividad:** revalida disponibilidad, crea o reutiliza la membresía,
  crea una `AsignacionActividad`, cambia a `ACEPTADA` y notifica.
- **Rechazar:** cambia a `RECHAZADA`, no crea miembro ni asignación, no libera
  cupo por sí mismo y notifica. La postulación **no se borra**.

#### Reglas de aceptación

- Proyecto `PUBLICADO` o `CONFIRMADO`.
- Requerimiento con `faltantes > 0`.
- Postulación `PENDIENTE`.
- Postulante activo.
- General: el postulante no puede ser ya miembro activo.
- Actividad: la actividad debe estar `PENDIENTE`, el postulante puede ser
  miembro `MIEMBRO` con la profesión requerida, un `DIRECTOR` no puede ser
  aceptado y no debe existir ya una asignación a esa actividad.
- Actividad: debe existir disponibilidad en el rango factible.

---

## 5. Experiencia de miembro y no miembro

### 5.1. Requerimientos generales del proyecto

`GET /proyectos/{idProyecto}` devuelve `requerimientosGral[]` también para no
miembros (si el proyecto es visible).

```json
{
  "requerimientosGral": [
    {
      "idRequerimientoGral": 30,
      "cantidad": 2,
      "descripcion": "Vacante para desfile.",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "caracteristicas": [
        {
          "idCaracteristica": 12,
          "codigo": "ALTURA",
          "tipoDato": "NUMERICO",
          "valorMin": 170,
          "valorMax": 180,
          "valores": []
        },
        {
          "idCaracteristica": 15,
          "codigo": "TALLE",
          "tipoDato": "ENUMERADO",
          "valorMin": null,
          "valorMax": null,
          "valores": [3]
        }
      ],
      "habilidades": [7, 9],
      "cubiertos": 0,
      "faltantes": 2,
      "satisfecho": false,
      "puedePostularse": true
    }
  ]
}
```

Uso en UI:

- Encabezado: `cantidad` + `nombreProfesion` (ej. `2 · Modelo`).
- Chips de características: numéricas como rango `valorMin–valorMax`;
  enumeradas traduciendo cada id de `valores` con el catálogo.
- Chips de habilidades traduciendo los ids.
- Cupo: `cubiertos / cantidad` + badge `Cupo completo` si `satisfecho`.
- Botón `Postularme` solo si `puedePostularse === true`.
- No usar `elegibleParaPostularse` para habilitar el botón por requerimiento.

### 5.2. Actividades y sus requerimientos

```http
GET /proyectos/105/actividades/buscar?estado=PENDIENTE&todos=true
```

Cada actividad:

```json
{
  "idActividad": 200,
  "idProyecto": 105,
  "nombreProyecto": "Colección Madoka",
  "nombre": "Prueba de vestuario",
  "descripcion": "Prueba con las modelos seleccionadas.",
  "duracionMinutos": 120,
  "fechaHoraInicio": "2026-11-02T09:00:00",
  "fechaHoraFin": "2026-11-02T11:00:00",
  "estado": "PENDIENTE",
  "requerimientos": [
    {
      "idRequerimiento": 20,
      "cantidad": 1,
      "descripcion": "Una modelo",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "cubiertos": 0,
      "faltantes": 1,
      "satisfecho": false,
      "puedePostularse": true,
      "caracteristicas": [
        {
          "idCaracteristica": 12,
          "codigo": "ALTURA",
          "tipoDato": "NUMERICO",
          "valorMin": 170,
          "valorMax": 180,
          "valores": []
        }
      ],
      "habilidades": [7, 9]
    }
  ],
  "predecesoras": []
}
```

Notas:

- `caracteristicas` y `habilidades` tienen la misma forma que en
  `requerimientosGral`; pueden ser `[]`.
- `valores` son ids; el front traduce con el catálogo de la profesión.
- Se devuelven **todas** las actividades `PENDIENTE` con **todos** sus
  requerimientos, incluidos los que tienen `puedePostularse = false` y
  `satisfecho = true`. El front necesita esos casos para mostrar
  `Cupo completo` y `No disponible para tu perfil`.
- `estado` es el enum crudo: `PENDIENTE`, `EN_CURSO`, `FINALIZADA`,
  `CANCELADA`. Solo `PENDIENTE` admite postulaciones.
- Visibilidad: proyecto público no borrador/cancelado → `200`; privado o
  borrador → `403`.

### 5.3. Detalle de actividad

```http
GET /proyectos/105/actividades/200
```

Cambio de esta iteración: el `GET` aplica la misma regla de visibilidad que
`actividades/buscar`.

- Proyecto público no borrador/cancelado → cualquier perfil activo.
- Proyecto privado, borrador o cancelado → solo miembros activos.
- `404` si el proyecto o la actividad no existen.
- `403` si el proyecto no es visible para el perfil.

La respuesta incluye:

- `idActividad`, `idProyecto`, `nombreProyecto`, `nombre`, `descripcion`,
  `estado`, `duracionMinutos`.
- `fechaHoraInicio`, `fechaHoraFin`, `inicioProyectado`, `finProyectado`,
  `tentativo`. Si `tentativo` es `true`, avisar que las fechas son proyectadas
  por CPM.
- `requerimientos[]` con `cantidad`, `descripcion`, `nombreProfesion`,
  `caracteristicas`, `habilidades`, `cubiertos`, `faltantes`, `satisfecho` y
  `puedePostularse` calculado con la misma regla que la búsqueda.
- `profesionalesAsignados[]`, `predecesoras[]` y datos de cronograma.

```json
{
  "idActividad": 200,
  "idProyecto": 105,
  "nombreProyecto": "Colección Madoka",
  "nombre": "Prueba de vestuario",
  "descripcion": "Prueba con las modelos seleccionadas.",
  "estado": "PENDIENTE",
  "duracionMinutos": 120,
  "fechaHoraInicio": "2026-11-02T09:00:00",
  "fechaHoraFin": "2026-11-02T11:00:00",
  "inicioProyectado": "2026-11-02T09:00:00",
  "finProyectado": "2026-11-02T11:00:00",
  "tentativo": false,
  "requerimientos": [
    {
      "idRequerimiento": 20,
      "cantidad": 1,
      "descripcion": "Una modelo",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "caracteristicas": [],
      "habilidades": [7],
      "cubiertos": 0,
      "faltantes": 1,
      "satisfecho": false,
      "puedePostularse": true
    }
  ]
}
```

### 5.4. Postularse (UC-26)

#### Requerimiento general

```http
POST /proyectos/105/postulaciones/general
Content-Type: application/json

{ "idRequerimientoGral": 30 }
```

#### Requerimiento de actividad

```http
POST /proyectos/105/postulaciones/actividad
Content-Type: application/json

{ "idRequerimientoActividad": 20 }
```

#### Respuesta

`201 Created`:

```json
{
  "idPostulacion": 88,
  "tipo": "ACTIVIDAD",
  "estado": "PENDIENTE",
  "fechaPostulacion": "2026-10-28T11:00:00",
  "idProyecto": 105,
  "idRequerimiento": 20,
  "tentativa": false
}
```

- `tentativa = true` indica que la disponibilidad se evaluó sobre una fecha de
  inicio estimada y debe re-verificarse al confirmar el proyecto (UC-60).
- El alta deja la postulación en `PENDIENTE` y notifica a los directores.

#### Reglas

- Proyecto `PUBLICADO` o `CONFIRMADO`.
- Requerimiento con `faltantes > 0`.
- Profesión del perfil activo coincide con la del requerimiento.
- General: el perfil no debe ser miembro activo del proyecto.
- Actividad: actividad `PENDIENTE`; miembro `MIEMBRO` permitido; `DIRECTOR`
  excluido.
- Sin postulación duplicada al mismo requerimiento.
- Debe existir disponibilidad en el rango factible (actividad).
- `puedePostularse` no incluye duplicados ni disponibilidad: esos siguen siendo
  validaciones del backend y pueden devolver `409`.

---

## 6. Mis postulaciones (UC-27)

```http
GET /postulaciones/mias?idProyecto=5&estado=PENDIENTE&tipo=GENERAL&page=0&size=20
```

Parámetros opcionales: `idProyecto`, `estado`, `tipo`, `page`, `size`, `todos`.

Respuesta:

```json
{
  "contenido": [
    {
      "idPostulacion": 88,
      "tipo": "ACTIVIDAD",
      "estado": "PENDIENTE",
      "fechaPostulacion": "2026-10-28T11:00:00",
      "idProyecto": 5,
      "nombreProyecto": "Editorial Costa",
      "idRequerimiento": 50,
      "cantidad": 1,
      "nombreProfesion": "Estilista",
      "descripcionRequerimiento": "Una estilista para el casting",
      "idActividad": 40,
      "nombreActividad": "Casting de otoño",
      "puedeDarseDeBaja": true
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

Notas:

- `cantidad` y `nombreProfesion` se devuelven tanto para generales como para
  actividades.
- Orden: `fechaPostulacion` descendente.
- `puedeDarseDeBaja` es `true` solo si `estado = PENDIENTE`.
- La pantalla puede mostrar el requerimiento como `2 · Modelo`.

Bajas:

```http
DELETE /proyectos/5/postulaciones/general/77
DELETE /proyectos/5/postulaciones/actividad/88
```

- `204 No Content` en éxito.
- Solo el perfil dueño de la postulación puede darla de baja.
- Solo se pueden dar de baja postulaciones `PENDIENTE`.
- La baja es lógica/física según UC-27; el listado se refresca después.

---

## 7. Estados y semántica

### Postulación

| Estado | Significado |
| :--- | :--- |
| `PENDIENTE` | Esperando decisión del director. |
| `ACEPTADA` | Aceptada; puede aportar cupo si el miembro/asignación sigue activo. |
| `RECHAZADA` | Rechazada; no se borra y no se puede volver a postular al mismo requerimiento. |

### Proyecto

`Borrador`, `Publicado`, `Confirmado`, `Finalizado`, `Cancelado` (formato de
presentación). Solo `Publicado` y `Confirmado` aceptan postulaciones.

### Actividad

`PENDIENTE`, `EN_CURSO`, `FINALIZADA`, `CANCELADA` (enum crudo). Solo
`PENDIENTE` acepta postulaciones.

> No mezclar el estado del proyecto (`"Publicado"`) con el de la actividad o la
> postulación (`"PENDIENTE"`).

---

## 8. Errores

| HTTP | Caso | Acción UI |
| :---: | :--- | :--- |
| `400` | Rango de fechas inválido | Corregir filtros y mostrar `message`. |
| `401` | Sin sesión | Redirigir a login. |
| `403` | No es director o no tiene `GESTIONAR_POSTULACIONES` | No mostrar el apartado; no reintentar. |
| `403` | Proyecto privado/borrador para no miembro | Mostrar aviso de acceso. |
| `404` | Sin perfil activo | Abrir selección de perfil. |
| `404` | Proyecto, actividad o postulación inexistente | Volver/refrescar. |
| `409` | Postulación ya gestionada | Refrescar listado y ocultar acciones. |
| `409` | Requerimiento satisfecho | Refrescar proyecto/listado. |
| `409` | Postulante ya es miembro (general) | Refrescar listado. |
| `409` | Director intenta postularse/aceptar actividad | No ofrecer la acción. |
| `409` | Actividad no pendiente | Refrescar actividades. |
| `409` | Sin disponibilidad | Mostrar mensaje y ofrecer revisar calendario. |
| `409` | Postulación duplicada | Refrescar listado. |
| `409` | Perfil inactivo | Avisar que el perfil no está activo. |

Formato:

```json
{
  "message": "Texto para el usuario",
  "httpStatus": 409,
  "timestamp": 1761744000000
}
```

---

## 9. Recomendaciones de UI/UX

### Director

- Mostrar tabs/chips por estado con los conteos de `conteosPorEstado`, con
  `PENDIENTE` seleccionado por defecto.
- Badge del encabezado con `postulacionesPendientes`.
- Usar `puedeAceptar`/`puedeRechazar` para habilitar acciones, pero refrescar
  ante cualquier `409`.
- Mostrar `x / y` (cubiertos / cantidad) y `Cupo completo` cuando
  `satisfecho = true`.
- Si `aportaCupo = false` y `estado = ACEPTADA`, indicar que el miembro ya no
  está activo y que el cupo puede estar libre.
- En postulaciones de actividad, mostrar el nombre de la actividad.
- Nunca aceptar/rechazar con un solo clic: usar modal de confirmación.
- Deshabilitar el botón mientras la petición está en curso.
- Tras un `200`, refrescar listado y detalle del proyecto.
- No ofrecer invitaciones todavía: UC-31 no está implementado.

### Miembro y no miembro

- Listado único: primero los requerimientos generales del proyecto y luego las
  actividades con sus requerimientos; o tabs `Proyecto` / `Actividades`.
- Mostrar chips de características y habilidades en ambos casos.
- Mostrar `Cupo completo` cuando `satisfecho = true` y
  `No disponible para tu perfil` cuando `puedePostularse = false` por profesión.
- No mostrar el botón de postular si `puedePostularse !== true`.
- Al abrir una actividad, reutilizar el detalle (`GET .../actividades/{id}`) y
  mostrar sus requerimientos con el mismo componente de chips.
- Si la actividad es tentativa, avisar que las fechas son proyectadas por CPM.
- Mantener el estado de la postulación en mayúsculas y con etiquetas claras.

### Mis postulaciones

- Cards o filas con proyecto, tipo, requerimiento (`cantidad · profesión`),
  actividad si corresponde, fecha y badge de estado.
- Acción `Dar de baja` solo si `puedeDarseDeBaja`.
- Estado vacío: “Todavía no te postulaste a ningún proyecto.”

---

## 10. Checklist de aceptación

### Director

- [ ] Solo se muestra la experiencia de gestión a directores con `GESTIONAR_POSTULACIONES`.
- [ ] Las tabs muestran `PENDIENTE`, `ACEPTADA`, `RECHAZADA` y `TOTAL` desde `conteosPorEstado`.
- [ ] Los conteos ignoran `estado`, `page` y `size`, pero respetan el resto de los filtros.
- [ ] El badge del encabezado usa `postulacionesPendientes` (o `conteosPorEstado.PENDIENTE`).
- [ ] Aceptar general usa `.../general/{id}/aceptar`.
- [ ] Aceptar actividad usa `.../actividad/{id}/aceptar`.
- [ ] Rechazar usa las rutas `.../rechazar`.
- [ ] Se muestra confirmación antes de cualquier acción.
- [ ] El botón se deshabilita durante la petición.
- [ ] Un `409` refresca listado/proyecto y oculta o actualiza acciones.
- [ ] Se muestra el cupo antes de aceptar.
- [ ] Se contemplan `resultadoPreferencia`, `desvioMinutos` y `advertencias`.

### Miembro / no miembro

- [ ] Se muestran todos los requerimientos generales del proyecto.
- [ ] Se muestran las actividades pendientes con todos sus requerimientos.
- [ ] Cada requerimiento muestra características y habilidades como chips.
- [ ] Se muestran los casos `satisfecho = true` y `puedePostularse = false`.
- [ ] El botón de postular depende de `puedePostularse === true`.
- [ ] Un no miembro puede abrir el detalle de una actividad de un proyecto público.
- [ ] Un no miembro recibe `403` en proyecto privado o borrador.
- [ ] Se distingue `tentativo = true` en actividades proyectadas.

### Mis postulaciones

- [ ] Se muestran `cantidad` y `nombreProfesion`.
- [ ] El orden es por fecha descendente.
- [ ] Solo se ofrece baja si `puedeDarseDeBaja = true`.
- [ ] Los `DELETE` usan la ruta del tipo correcto.

---

## 11. Compatibilidad

- Todos los campos nuevos son aditivos.
- `PaginaResponse` global no cambió; la búsqueda de postulaciones usa un DTO
  propio (`PostulacionesPaginaResponse`).
- `ProyectoResponse` incorpora `postulacionesPendientes` al final.
- `MiPostulacionResponse` incorpora `cantidad` y `nombreProfesion`.
- `RequerimientoActividadResumenResponse` incorpora `caracteristicas` y
  `habilidades`, con listas vacías por defecto.
- Los endpoints existentes conservan su autorización, salvo el `GET` de detalle
  de actividad, que pasa a la regla de visibilidad del proyecto.
- Sin cambios en el formato de error ni en las reglas de aceptación/rechazo.
