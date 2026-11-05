# Ficha técnica — Historial de requerimientos y auditoría de bajas

> **Estado:** contrato nuevo para frontend  
> **Alcance:** protección de requerimientos, snapshots históricos, bajas de integrantes y auditoría de bajas por proyecto  
> **No reemplaza fichas anteriores:** las fichas ya aplicadas siguen siendo la referencia de los flujos base. Esta ficha describe únicamente el contrato nuevo y sus diferencias.

---

## 1. Objetivo

Este cambio separa la **operación actual** del **historial**:

- Un requerimiento activo puede editarse o eliminarse si ya no tiene miembros activos ni postulaciones/invitaciones pendientes.
- Cuando un miembro se da de baja o es eliminado, el backend crea una **copia inmutable** del requerimiento que ocupaba y mueve allí su historial.
- El requerimiento activo conserva su ID y sigue disponible para nuevas postulaciones/asignaciones.
- El Director puede consultar quién se dio de baja, cuándo, con qué motivo y qué actividades tenía asignadas.

El frontend **no debe intentar editar ni eliminar snapshots**. Los snapshots no se exponen en los endpoints operativos.

---

## 2. Resumen de cambios para el frontend

| Tema | Cambio |
|---|---|
| `protegido` en requerimientos | Ahora solo indica miembros/asignaciones `ACTIVO` o postulaciones/invitaciones `PENDIENTE` |
| Historial de miembros inactivos | Ya no bloquea la edición del requerimiento activo |
| Requerimiento omitido en un `PUT` | Se elimina si no está protegido; si tiene historial, el backend lo preserva en un snapshot |
| Requerimiento editado con historial | El activo se actualiza en el lugar; el historial queda en el snapshot |
| Snapshot | Copia inmutable; no se devuelve en detalle, búsqueda, postulación ni confirmación |
| Postulaciones históricas | Pueden mostrar el ID del snapshot en lugar del requerimiento activo |
| Eliminación de actividad | Solo permitida en estado `PENDIENTE` |
| Auditoría de bajas | Nuevo endpoint `GET /proyectos/{idProyecto}/auditoria/bajas` |
| Autor de la baja | Nuevo dato `autorBaja` en auditoría |

---

## 3. Nueva semántica de `protegido`

### 3.1. Requerimiento de actividad

`requerimientos[].protegido = true` cuando el requerimiento tiene:

- al menos una asignación de un miembro con `estadoParticipacion = ACTIVO`;
- o al menos una postulación `PENDIENTE`;
- o al menos una invitación `PENDIENTE`.

No protegen:

- asignaciones de miembros `BAJA_VOLUNTARIA` o `ELIMINADO`;
- postulaciones/invitaciones `ACEPTADA` de perfiles que ya no son miembros activos;
- postulaciones/invitaciones `RECHAZADA`;
- snapshots históricos.

### 3.2. Requerimiento general

`requerimientosGral[].protegido = true` cuando el requerimiento tiene:

- al menos un miembro `ACTIVO` vinculado;
- o al menos una postulación `PENDIENTE`;
- o al menos una invitación `PENDIENTE`.

No protegen:

- miembros históricos;
- postulaciones/invitaciones `ACEPTADA` de perfiles inactivos;
- postulaciones/invitaciones `RECHAZADA`.

### 3.3. Uso en UI

- Si `protegido === true`: deshabilitar edición y eliminación de ese requerimiento.
- Si `protegido === false`: permitir edición y eliminación.
- No bloquear todo el formulario por un requerimiento protegido.
- Mostrar badge “Protegido” o “Con personal activo/pendiente”.
- No usar `protegido` para decidir si hay historial: el backend preserva el historial automáticamente.

---

## 4. Modificación de actividad

**Endpoint:**

```http
PUT /proyectos/{idProyecto}/actividades/{idActividad}
```

### 4.1. Semántica de requerimientos

El `PUT` sigue siendo de reemplazo total para la lista `requerimientos`.

| Caso | Resultado |
|---|---|
| Requerimiento enviado sin cambios | No hace nada |
| Requerimiento protegido enviado con cambios | `409` |
| Requerimiento no protegido enviado con cambios | Se actualiza en el lugar. Si tenía historial legacy, primero se crea y repunta el snapshot |
| Requerimiento protegido omitido | Se conserva |
| Requerimiento no protegido omitido sin historial | Se elimina físicamente |
| Requerimiento no protegido omitido con historial | Se crea/reutiliza snapshot, se repunta el historial y se elimina el activo |

### 4.2. Consecuencias para el frontend

- Si el usuario elimina un requerimiento de la lista, el backend puede eliminar el activo y conservar el historial en un snapshot.
- Si el requerimiento editado tenía historial, el ID del requerimiento activo se mantiene.
- Los snapshots no aparecen en `requerimientos[]` de la respuesta.
- Después del `PUT`, refrescar la lista de requerimientos desde la respuesta.

### 4.3. Error de requerimiento protegido

```http
409 Conflict
```

```json
{
  "message": "No se puede modificar ni eliminar el requerimiento 30 porque tiene postulaciones pendientes o asignaciones de miembros activos.",
  "codigo": "REQUERIMIENTO_CON_VINCULOS",
  "idRequerimiento": 30,
  "httpStatus": 409,
  "timestamp": 1762312345678
}
```

---

## 5. Modificación de proyecto

**Endpoint:**

```http
PUT /proyectos/{idProyecto}
```

### 5.1. Semántica de requerimientos generales

`requerimientosGral` mantiene la misma lógica de reemplazo total.

| Caso | Resultado |
|---|---|
| Requerimiento enviado sin cambios | No hace nada |
| Requerimiento protegido enviado con cambios | `409` |
| Requerimiento no protegido enviado con cambios | Se actualiza en el lugar. Si tenía historial legacy, primero se crea y repunta el snapshot |
| Requerimiento protegido omitido | Se conserva |
| Requerimiento no protegido omitido sin historial | Se elimina físicamente |
| Requerimiento no protegido omitido con historial | Se crea/reutiliza snapshot, se repunta el historial y se elimina el activo |

### 5.2. Error de requerimiento general protegido

```http
409 Conflict
```

```json
{
  "message": "No se puede modificar ni eliminar el requerimiento 28 porque tiene miembros activos o postulaciones/invitaciones pendientes.",
  "codigo": "REQUERIMIENTO_CON_VINCULOS",
  "idRequerimientoGral": 28,
  "httpStatus": 409,
  "timestamp": 1762312345678
}
```

> El código es el mismo `REQUERIMIENTO_CON_VINCULOS`; el campo que identifica el requerimiento es `idRequerimientoGral`.

---

## 6. Snapshots e historial

### 6.1. Qué hace el backend al dar de baja o eliminar un integrante

1. Rechaza automáticamente sus postulaciones `PENDIENTE`.
2. Rechaza automáticamente las invitaciones `PENDIENTE` donde es destinatario.
3. Crea o reutiliza un snapshot del requerimiento general que ocupaba.
4. Crea o reutiliza snapshots de los requerimientos de actividad que ocupaba.
5. Repunta al snapshot:
   - la membresía general;
   - las asignaciones;
   - las postulaciones/invitaciones históricas de esos requerimientos.
6. Guarda el perfil autor de la baja/eliminación.

No se envían notificaciones por los rechazos automáticos.

### 6.2. Qué ve el frontend

- Los snapshots **no** aparecen en:
  - detalle de actividad;
  - detalle de proyecto;
  - búsqueda de actividades;
  - postulaciones activas;
  - confirmación de proyecto.
- El historial se consulta únicamente desde la auditoría de bajas.
- Las postulaciones históricas (`ACEPTADA` o `RECHAZADA`) pueden devolver el `idRequerimiento`/`idRequerimientoGral` del snapshot.
  - Para postulaciones `PENDIENTE`, el requerimiento siempre es el activo.
  - No usar el ID de una postulación histórica para editar o eliminar requerimientos.

### 6.3. Inmutabilidad

- Un snapshot no se puede modificar ni eliminar desde los endpoints normales.
- Si se elimina una actividad `PENDIENTE`, se eliminan también sus snapshots y su historial. El modal debe advertirlo.

---

## 7. Auditoría de bajas

### 7.1. Endpoint

```http
GET /proyectos/{idProyecto}/auditoria/bajas
```

### 7.2. Permisos

- Solo un **Director activo** del proyecto puede consultar.
- Si el perfil no es Director activo: `403 Forbidden`.
- Si el proyecto no existe: `404 Not Found`.

### 7.3. Parámetros

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---:|---|
| `estado` | `string` | No | `BAJA_VOLUNTARIA` o `ELIMINADO` |
| `idActividad` | `long` | No | Filtra por actividad y limita `actividades[]` a esa actividad |
| `fechaDesde` | `date` | No | Fecha inclusive sobre `fechaBaja` (`YYYY-MM-DD`) |
| `fechaHasta` | `date` | No | Fecha inclusive sobre `fechaBaja` (`YYYY-MM-DD`) |
| `page` | `int` | No | Default `0` |
| `size` | `int` | No | Default `20` |
| `todos` | `boolean` | No | Default `false`. Si es `true`, ignora paginación |

### 7.4. Respuesta

```json
{
  "contenido": [
    {
      "idMiembro": 55,
      "idPerfil": 12,
      "idUsuario": 7,
      "nombreArtistico": "Luna",
      "nombreUsuario": "Ana",
      "apellidoUsuario": "Gómez",
      "rol": "MIEMBRO",
      "estadoParticipacion": "BAJA_VOLUNTARIA",
      "motivoBaja": "Motivos personales",
      "fechaAlta": "2026-09-01T10:00:00",
      "fechaBaja": "2026-10-15T18:30:00",
      "autorBaja": {
        "idPerfil": 12,
        "idUsuario": 7,
        "nombreArtistico": "Luna",
        "nombre": "Ana",
        "apellido": "Gómez"
      },
      "actividades": [
        {
          "idActividad": 20,
          "nombreActividad": "Montaje",
          "estadoActividad": "EN_CURSO",
          "idRequerimiento": 30,
          "nombreProfesion": "Fotógrafo"
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

### 7.5. Campos

| Campo | Descripción |
|---|---|
| `idMiembro` | ID de la membresía histórica |
| `idPerfil` / `idUsuario` | Perfil y usuario que se dio de baja |
| `nombreArtistico` | Nombre artístico del miembro |
| `nombreUsuario` / `apellidoUsuario` | Datos del usuario |
| `rol` | `DIRECTOR` o `MIEMBRO` al momento de la baja |
| `estadoParticipacion` | `BAJA_VOLUNTARIA` o `ELIMINADO` |
| `motivoBaja` | Motivo registrado |
| `fechaAlta` / `fechaBaja` | Fechas de la membresía |
| `autorBaja` | Perfil que ejecutó la baja. En baja voluntaria, el propio miembro. Puede ser `null` en datos históricos previos |
| `actividades` | Actividades asignadas al miembro. Puede ser `[]` |

### 7.6. Ejemplos

```http
GET /proyectos/100/auditoria/bajas?estado=BAJA_VOLUNTARIA&page=0&size=20
```

```http
GET /proyectos/100/auditoria/bajas?idActividad=20&fechaDesde=2026-10-01&fechaHasta=2026-10-31
```

---

## 8. Errores relevantes

| HTTP | Situación | Código |
|---:|---|---|
| `400` | Perfil activo no seleccionado | Según handler existente |
| `403` | El perfil no es Director activo | `AccesoDenegadoProyectoException` |
| `404` | Proyecto no encontrado | Según handler existente |
| `409` | Requerimiento de actividad protegido | `REQUERIMIENTO_CON_VINCULOS` + `idRequerimiento` |
| `409` | Requerimiento general protegido | `REQUERIMIENTO_CON_VINCULOS` + `idRequerimientoGral` |
| `409` | Actividad no eliminable | `ActividadNoEliminableException` |
| `409` | Proyecto no permite eliminar actividades | `EstadoProyectoNoPermiteEliminarActividadesException` |

---

## 9. Recomendaciones de UI

### 9.1. Requerimientos

- Leer siempre `protegido` en el detalle del proyecto y de la actividad.
- Deshabilitar edición/eliminación solo para requerimientos protegidos.
- No bloquear el formulario completo.
- Al eliminar un requerimiento no protegido, no advertir sobre historial: el backend lo conserva automáticamente.
- Si se edita un requerimiento no protegido con historial, el ID se mantiene y el historial queda en el snapshot.

### 9.2. Eliminación de actividad

- Solo habilitar la acción para actividades `PENDIENTE`.
- Mostrar advertencia explícita:
  > “Se eliminará la actividad y su historial de participación. Esta acción no se puede deshacer.”
- Para `EN_CURSO`, `FINALIZADA` o `CANCELADA`, ocultar o deshabilitar la eliminación.

### 9.3. Auditoría

- Mostrar `autorBaja` como “Baja voluntaria” cuando coincida con el miembro, y como “Eliminado por …” cuando sea otro perfil.
- Si `autorBaja === null`, mostrar “No registrado”.
- Si `actividades` está vacío, mostrar “Sin actividades asignadas al momento de la baja”.
- Aclarar que la auditoría no muestra actividades eliminadas ni desasignaciones individuales.

### 9.4. Postulaciones históricas

- Si una postulación `ACEPTADA` o `RECHAZADA` muestra un requerimiento que no aparece en el detalle activo, es un snapshot histórico. Mostrarlo como solo lectura.
- No ofrecer acciones de aceptar/rechazar sobre postulaciones históricas.

---

## 10. Casos de prueba frontend

- [ ] Un miembro dado de baja ya no aparece como protección del requerimiento.
- [ ] Una asignación activa mantiene `protegido = true`.
- [ ] Una postulación pendiente mantiene `protegido = true`.
- [ ] Una postulación aceptada de un inactivo no bloquea la edición.
- [ ] Un requerimiento protegido muestra edición/eliminación deshabilitadas.
- [ ] Un requerimiento no protegido se puede editar y eliminar.
- [ ] Al omitir un requerimiento no protegido, desaparece de la respuesta del `PUT`.
- [ ] Al editar un requerimiento no protegido con historial, su ID se conserva.
- [ ] La auditoría muestra miembro, autor, motivo, fecha y actividades.
- [ ] El filtro por `idActividad` limita también `actividades[]`.
- [ ] Los filtros de fecha son inclusivos.
- [ ] Un no Director recibe `403`.
- [ ] La eliminación de actividad solo se ofrece en `PENDIENTE`.
- [ ] El modal de eliminación de actividad advierte que se borra el historial.

---

## 11. Notas de despliegue

- El backend requiere las columnas:
  - `requerimiento_actividad.fecha_archivado`
  - `requerimiento_actividad.id_requerimiento_origen`
  - `requerimiento_gral_proyecto.fecha_archivado`
  - `requerimiento_gral_proyecto.id_requerimiento_origen`
  - `miembros_proyecto.id_perfil_autor_baja`
- Migración aditiva:
  `scripts/migracion-historial-requerimientos-2026-11-04.sql`
- No se requiere cambio de payload para los endpoints existentes.
- El campo `protegido` mantiene el mismo nombre, pero cambia su semántica.
- Los snapshots no se exponen en endpoints operativos.

---

## 12. Limitaciones conocidas

- La auditoría no muestra desasignaciones individuales de actividades.
- La auditoría no muestra actividades eliminadas.
- La auditoría no muestra requerimientos generales como sección separada.
- `autorBaja` puede ser `null` en registros previos a este cambio.
- Los snapshots conservan referencias a catálogos; si cambia el nombre de una profesión, habilidad o actividad, el nombre mostrado puede cambiar.
- Habilidades y características usadas solo por snapshots siguen contando como “en uso” para el administrador.
