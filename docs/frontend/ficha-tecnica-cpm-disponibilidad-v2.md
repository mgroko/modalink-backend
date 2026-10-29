# Ficha técnica v2 — CPM, disponibilidad y preferencia horaria

**Proyecto:** ModaLink
**Backend:** Spring Boot
**Fecha:** 2026-10-28
**Tipo:** actualización incremental del contrato de disponibilidad/CPM.
**Ficha anterior:** `docs/frontend/ficha-tecnica-cpm-disponibilidad-fase-3.md`.

> **Importante:** esta ficha **no reemplaza** la ficha anterior. La ficha v1 sigue siendo válida en todo lo que no se contradiga con este documento. El frontend ya la aplicó; esta v2 describe **solo los cambios y campos nuevos** que debe integrar.

---

## 1. Resumen de cambios

| Tema | Antes | Ahora |
|---|---|---|
| Proyecto sin `fechaEntrega` | CPM parcial con `LF^M`; disponibilidad con ventana reducida | **No se calcula CPM ni disponibilidad**. `GET /cronograma` y `GET /disponibilidad` responden `400`. |
| Postulación sin disponibilidad | `409` | Se mantiene `409`. No hay camino informativo que permita postularse sin hueco. |
| Aceptación | Evaluaba solo al postulante | Evalúa **postulante + todos los asignados a esa misma actividad**. Si no hay hueco común, `409`. |
| Horario de preferencia | Se ignoraba; se elegía el primer hueco | Se elige exacto → desvío dentro de tolerancia → sin lugar. |
| Resultado de preferencia | No existía | `resultadoPreferencia`, `desvioMinutos`, `inicioPreferido`. |
| Diagnóstico multi-miembro | Solo detalle individual | Se agrega `miembrosBloqueantes`. |
| Sobrecompromiso | No se advertía | `advertencias` en la aceptación, sin bloquear. |
| Notificaciones | No había aviso de ajuste ni sobrecompromiso | Nuevos tipos `AJUSTE_HORARIO` y `SOBRECOMPROMISO`. |

---

## 2. Regla nueva: sin fecha de entrega no hay CPM

Si `planificacion.fechaEntrega` es `null`:

- `GET /proyectos/{idProyecto}/cronograma` → `400 Bad Request`.
- `GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad` → `400 Bad Request`.
- La postulación a actividad → `400`/`409` según el flujo; no se evalúa disponibilidad.
- `CrearActividad`, `ModificarActividad` y `ObtenerActividad` **no fallan**, pero devuelven:
  - `estadoCronograma = "SIN_FECHA_ENTREGA"`;
  - `inicioProyectado = null`;
  - `finProyectado = null`;
  - `critica = null`;
  - `holguraMakespan = null`;
  - `holguraEntrega = null`;
  - `rangoInicio = null`;
  - `rangoEjecucion = null`;
  - `conflicto = null`;
  - `motivoConflicto = null`;
  - `parcial = null`.

Mensaje esperado del backend:

```text
El proyecto no tiene fecha de entrega definida. No es posible calcular el cronograma.
Complete la fecha de entrega para continuar.
```

### Uso en frontend

- Si el proyecto está en `BORRADOR` y no tiene entrega, mostrar un estado vacío:
  - “Definí la fecha de entrega para calcular el cronograma”.
- No mostrar Gantt, tabla CPM, ruta crítica ni disponibilidad.
- No inventar fechas en el cliente.

---

## 3. `ResultadoDisponibilidad` v2

Se mantienen todos los campos anteriores y se agregan cuatro:

```json
{
  "disponible": true,
  "provisional": false,
  "inicioEvaluado": "2026-11-02T09:00:00",
  "finEvaluado": "2026-11-02T18:00:00",
  "inicioSeleccionado": "2026-11-02T15:00:00",
  "finSeleccionado": "2026-11-02T16:00:00",
  "motivo": null,
  "miembros": [],
  "resultadoPreferencia": "EXACTO",
  "desvioMinutos": 0,
  "inicioPreferido": "2026-11-02T15:00:00",
  "miembrosBloqueantes": []
}
```

### 3.1. `resultadoPreferencia`

| Valor | Significado | UI sugerida |
|---|---|---|
| `SIN_PREFERENCIA` | La actividad no tiene `horarioPreferenciaInicio`. | Mostrar el hueco normalmente. |
| `EXACTO` | El inicio seleccionado coincide con la preferencia. | Badge “Horario preferido”. |
| `DESVIO` | Se adelantó o atrasó dentro de la tolerancia configurada. | Badge “Con ajuste de X min”. |
| `SIN_LUGAR` | No hay hueco dentro de la tolerancia. | Estado “Sin disponibilidad”. |

### 3.2. `desvioMinutos`

- `null` si no hay preferencia o si `resultadoPreferencia = SIN_LUGAR`.
- `0` si es exacto.
- **Negativo** si el inicio elegido es anterior al preferido (se adelanta).
- **Positivo** si es posterior (se atrasa).

Ejemplos:

```text
preferido 15:00, elegido 14:30 → -30
preferido 15:00, elegido 15:30 → +30
preferido 15:00, elegido 15:00 → 0
```

### 3.3. `inicioPreferido`

- Fecha/hora absoluta construida a partir de `horarioPreferenciaInicio`.
- Su fecha de referencia es la del primer hueco común (`ES_DISPONIBLE`).
- `null` si la actividad no tiene preferencia.

### 3.4. `miembrosBloqueantes`

- Lista de `idUsuario` que impiden la intersección común.
- Se calcula solo cuando hay más de un miembro y la intersección es vacía.
- Puede venir vacía aunque no haya hueco común: significa que **no se pudo aislar a un único culpable**.
- No usar esta lista para bloquear por sí sola; el bloqueo ya lo expresa `disponible = false`.

### 3.5. `motivo`

Se mantienen los motivos anteriores y se agrega:

| Motivo | Cuándo |
|---|---|
| `FUERA_TOLERANCIA_PREFERENCIA` | Hay hueco, pero ninguno dentro de la tolerancia de la preferencia. |

Orden de prioridad actual:

1. `SIN_MIEMBROS`
2. `SIN_HUECO_COMUN` si hay varios miembros y no hay intersección.
3. `FUERA_TOLERANCIA_PREFERENCIA` si hay intersección pero la preferencia no entra en tolerancia.
4. Motivo individual (`SIN_JORNADA`, `SIN_HUECO`, `DURACION_EXCEDE_VENTANA`).

---

## 4. Preferencia horaria: reglas de negocio

1. Sin preferencia: se mantiene el primer hueco cronológico.
2. Con preferencia:
   - si existe un hueco que empieza exactamente en la hora preferida → `EXACTO`;
   - si no, se busca el hueco más cercano dentro de `DISPONIBILIDAD_TOLERANCIA_PREFERENCIA_MIN` (seed: `120`) → `DESVIO`;
   - si no hay ninguno → `SIN_LUGAR`.
3. Solo se relaja la preferencia. **Nunca** se relajan:
   - fecha de entrega;
   - jornada;
   - bloqueos manuales;
   - asignaciones;
   - precedencias;
   - margen.
4. La preferencia no reserva ni persiste horarios.
5. Si se acepta una postulación con `DESVIO`, se notifica a todos los directores.
6. La notificación no se dispara desde el `GET` de disponibilidad, sino al concretar la aceptación.

---

## 5. Aceptación de postulación: contrato actualizado

### 5.1. Endpoint

```http
POST /proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/aceptar
```

### 5.2. Respuesta

```json
{
  "idPostulacion": 456,
  "tipo": "ACTIVIDAD",
  "estado": "ACEPTADA",
  "idProyecto": 100,
  "idRequerimiento": 123,
  "idPerfil": 50,
  "idMiembro": 900,
  "idAsignacionActividad": 1200,
  "resultadoPreferencia": "DESVIO",
  "desvioMinutos": 30,
  "advertencias": [
    {
      "idActividad": 77,
      "nombreActividad": "Prueba de vestuario",
      "idProyecto": 101,
      "nombreProyecto": "Campaña Otoño",
      "inicioPotencial": "2026-11-02T14:00:00",
      "finPotencial": "2026-11-02T17:00:00"
    }
  ]
}
```

### 5.3. Reglas

- La aceptación evalúa al postulante **y a todos los profesionales ya asignados a esa actividad**.
- Si no existe hueco común, responde `409` y la postulación sigue `PENDIENTE`.
- Si el postulante ya está asignado a la actividad, `409`.
- Si el postulante es director, `409`.
- Si el cupo está satisfecho, `409`.
- La creación de la asignación y el cambio a `ACEPTADA` siguen siendo transaccionales.

### 5.4. `desvioMinutos` en la aceptación

- `null` si no hay preferencia.
- `0` si fue exacto.
- Con signo si hubo desvío.
- La UI debe mostrar el ajuste al director, por ejemplo:
  - “Se asignó 30 min más tarde de lo preferido”.

### 5.5. `advertencias`

- No bloquean la aceptación.
- Se calculan contra otras actividades aceptadas del mismo profesional, en **otros proyectos**, sin horario persistido y con ventana CPM superpuesta.
- Se notifican a todos los directores.
- La UI debe mostrarlas como advertencia, no como error.

---

## 6. Consulta de disponibilidad multi-perfil

```http
GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad
GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad?idPerfil=10
GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad?idPerfil=10&idPerfil=11
```

Reglas sin cambios:

- sin `idPerfil`, evalúa el perfil activo del solicitante;
- con varios `idPerfil`, exige intersección común;
- consultar perfiles de terceros requiere ser miembro activo del proyecto;
- la consulta es de solo lectura;
- la actividad cancelada responde `409`;
- el proyecto debe ser visible.

Cambios:

- Si el proyecto no tiene `fechaEntrega` → `400`.
- La respuesta incluye los cuatro campos nuevos.
- Si hay varios perfiles y no hay intersección, `miembrosBloqueantes` intenta identificar a los responsables.
- El detalle `miembros[]` sigue igual: `idUsuario`, `inicioEvaluado`, `finEvaluado`, `huecos`, `disponible`, `motivo`.

---

## 7. Notificaciones nuevas

Se agregaron dos tipos:

| Tipo | Cuándo | Destinatario |
|---|---|---|
| `AJUSTE_HORARIO` | Se acepta una postulación con `desvioMinutos != 0`. | Todos los directores. |
| `SOBRECOMPROMISO` | Se acepta una postulación y hay ventanas CPM superpuestas con otras actividades sin horario. | Todos los directores. |

El frontend debe:

- renderizar ambos tipos en el centro de notificaciones;
- no tratarlos como errores bloqueantes;
- mostrar el proyecto y la actividad asociados;
- para `SOBRECOMPROMISO`, mostrar el nombre de la actividad potencialmente superpuesta.

---

## 8. Cambios en actividades y cronograma

### 8.1. `ActividadResponse`

No cambian los nombres de campos. Cambia el valor en proyectos sin entrega:

```json
{
  "estadoCronograma": "SIN_FECHA_ENTREGA",
  "inicioProyectado": null,
  "finProyectado": null,
  "critica": null,
  "holguraMakespan": null,
  "holguraEntrega": null,
  "rangoInicio": null,
  "rangoEjecucion": null,
  "conflicto": null,
  "motivoConflicto": null,
  "parcial": null
}
```

### 8.2. `GET /proyectos/{idProyecto}/cronograma`

- Sin `fechaEntrega`: `400`, no devuelve `resumen` ni `actividades`.
- Con `fechaEntrega`: mismo contrato que la ficha v1.
- `resumen.rutaCritica` sigue usando `enRutaCritica`.
- `finConHolgura` sigue siendo `finProyectado + holguraMakespan`.

---

## 9. Reglas que el frontend no debe duplicar

1. No recalcular CPM, `ES`, `EF`, `LS`, `LF`, holguras ni ruta crítica.
2. No recalcular `desvioMinutos`.
3. No sumar ni restar la tolerancia.
4. No convertir `SIN_LUGAR` en “disponible” por existir otro hueco fuera de tolerancia.
5. No persistir `inicioSeleccionado` ni `finSeleccionado`.
6. No crear asignaciones desde el frontend.
7. No usar `miembrosBloqueantes` como único criterio de bloqueo: usar `disponible`.
8. No tratar `advertencias` como errores.
9. No mostrar cronograma ni disponibilidad si el backend responde `SIN_FECHA_ENTREGA`.
10. No enviar `fechaEntrega` desde el cronograma; se gestiona en el proyecto/planificación.

---

## 10. Matriz de estados visuales sugerida

| Condición | Estado visual |
|---|---|
| `disponible = true && resultadoPreferencia = EXACTO` | Disponible en horario preferido. |
| `disponible = true && resultadoPreferencia = DESVIO` | Disponible con ajuste de `desvioMinutos`. |
| `disponible = false && resultadoPreferencia = SIN_LUGAR` | Sin disponibilidad dentro de la tolerancia. |
| `disponible = false && motivo = SIN_HUECO_COMUN` | Sin hueco común; mostrar `miembrosBloqueantes` si existe. |
| `disponible = false && motivo = SIN_JORNADA` | El profesional no tiene jornada en el rango. |
| `disponible = false && motivo = DURACION_EXCEDE_VENTANA` | La jornada no alcanza para la duración. |
| `advertencias.length > 0` | Aceptación exitosa con advertencia de sobrecompromiso. |
| `estadoCronograma = SIN_FECHA_ENTREGA` | Estado vacío “Falta fecha de entrega”. |

---

## 11. Compatibilidad

- Los campos anteriores de `ResultadoDisponibilidad` y `PostulacionGestionadaResponse` se mantienen.
- Los campos nuevos pueden venir `null` o `[]` en respuestas de rechazo o flujos generales.
- Un cliente viejo que ignore los campos nuevos sigue funcionando.
- Un cliente nuevo debe priorizar `disponible`, `resultadoPreferencia` y `motivo` para la UI.

---

## 12. Pendientes fuera de esta ficha

- **UC-60 / confirmación de proyecto:** no implementado. No hay persistencia de `fecha_hora_inicio`.
- **Postulación masiva:** no implementada.
- **Triggers SQL:** están definidos en `docs/3_diseño/ModaLinkBD.sql`; deben aplicarse en pgAdmin y validarse contra PostgreSQL.
- **Tests de integración:** requieren Docker/PostgreSQL.
