# Ficha técnica frontend: actualización de actividades, requerimientos y reprogramación

- **Fecha:** 2026-11-03
- **Estado:** Contrato vigente para el frontend
- **Alcance:** actividades, requerimientos de actividad, cambio de estado y reprogramación de proyectos confirmados
- **Tipo:** Ficha complementaria. **No reemplaza** las fichas existentes; las actualiza donde entran en conflicto.

> Esta ficha no modifica las fichas técnicas existentes. Mientras no se actualicen, este documento tiene prioridad para los flujos descriptos acá.

---

## 1. Resumen de cambios

| Cambio | Impacto en frontend |
|---|---|
| Nuevo `PATCH /proyectos/{idProyecto}/actividades/{idActividad}/estado` | Los botones de Iniciar, Finalizar y Cancelar ya no deben usar el `PUT` completo |
| `GET` de actividad devuelve `requerimientos[].protegido` | Permite deshabilitar requerimientos sin esperar un `409` |
| En `CONFIRMADO` los requerimientos ya no son inmutables en bloque | Se pueden editar/agregar/quitar los que no estén protegidos |
| Un requerimiento `PENDIENTE` bloquea cambios en `CONFIRMADO` | Antes solo bloqueaba `ACEPTADA`/asignaciones |
| Un requerimiento sin cambios ya no responde `409` | Se puede guardar la actividad sin tocar requerimientos |
| Un requerimiento protegido omitido se conserva | Omitirlo ya no lo elimina |
| Se puede crear actividad en `CONFIRMADO` | La actividad se agenda automáticamente y puede no tener asignados |
| La reprogramación es global en proyectos confirmados | Crear, asignar, desasignar, eliminar o cancelar puede mover otras actividades `PENDIENTE` |
| El error `409` de requerimientos incluye código e ID | El frontend puede mostrar una acción concreta |

---

## 2. Cambio de estado de actividad

### 2.1. Endpoint

```http
PATCH /proyectos/{idProyecto}/actividades/{idActividad}/estado
Content-Type: application/json
```

Body:

```json
{
  "estado": "EN_CURSO"
}
```

Respuesta exitosa: `200 OK` con el `ActividadResponse` completo.

### 2.2. Cuándo usarlo

Usarlo para:

- iniciar una actividad (`PENDIENTE → EN_CURSO`);
- finalizar (`EN_CURSO → FINALIZADA`);
- cancelar (`PENDIENTE` o `EN_CURSO → CANCELADA`).

**No usarlo** para editar nombre, descripción, duración, predecesoras ni requerimientos. Para eso sigue existiendo el `PUT` completo.

### 2.3. Matriz de estados

| Estado actual | Destinos permitidos |
|---|---|
| `PENDIENTE` | `PENDIENTE`, `EN_CURSO`, `CANCELADA` |
| `EN_CURSO` | `EN_CURSO`, `FINALIZADA`, `CANCELADA` |
| `FINALIZADA` | Ninguno |
| `CANCELADA` | Ninguno |

### 2.4. Errores

| HTTP | Situación | Acción UI |
|---|---|---|
| `400` | `estado` ausente o inválido | Validar antes de enviar |
| `403` | Sin permiso `MODIFICAR_ACTIVIDAD` | Ocultar acciones |
| `404` | Proyecto o actividad inexistente | Refrescar |
| `409` | Transición inválida | Mostrar mensaje y refrescar |
| `409` | Actividad `FINALIZADA` o `CANCELADA` | Refrescar; no permitir edición |
| `409` | Actividad sin `fechaHoraInicio` para pasar a `EN_CURSO`/`FINALIZADA` | Mostrar que primero debe agendarse |
| `409` | Proyecto `FINALIZADO` o `CANCELADO` | Refrescar |

> En proyectos confirmados, cancelar una actividad dispara una reprogramación global. Refrescar planificación y Gantt después del `200`.

---

## 3. Requerimientos protegidos

### 3.1. Nuevo campo en el `GET`

Cada requerimiento incluye:

```json
{
  "idRequerimiento": 28,
  "cantidad": 2,
  "descripcion": "Modelos para la prueba",
  "idProfesion": 2,
  "nombreProfesion": "Modelo",
  "caracteristicas": [],
  "habilidades": [],
  "cubiertos": 0,
  "faltantes": 2,
  "satisfecho": false,
  "puedePostularse": true,
  "protegido": true
}
```

`protegido` indica si el requerimiento no se puede modificar ni eliminar en el estado actual del proyecto.

### 3.2. Regla de protección

| Vínculo del requerimiento | Proyecto NO confirmado | Proyecto `CONFIRMADO` |
|---|---|---|
| Sin vínculos | Editable / eliminable | Editable / eliminable |
| Solo `RECHAZADA` | Editable / eliminable | Editable / eliminable |
| `PENDIENTE` | Editable / eliminable | **Inmutable** |
| `ACEPTADA` | **Inmutable** | **Inmutable** |
| Asignación de profesional | **Inmutable** | **Inmutable** |

> En `CONFIRMADO`, un requerimiento con postulación pendiente ya no se puede modificar. Esto protege a los candidatos que todavía están en evaluación.

### 3.3. Comportamiento del `PUT`

El `PUT` sigue siendo de reemplazo total, pero ahora:

1. Un requerimiento enviado **sin cambios**:
   - no dispara `409`;
   - no se reescribe.

2. Un requerimiento enviado **con cambios**:
   - si está protegido → `409`;
   - si no está protegido → se actualiza.

3. Un requerimiento **omitido**:
   - si está protegido → se conserva;
   - si no está protegido → se elimina.

4. Un requerimiento **nuevo**:
   - se crea;
   - en `CONFIRMADO` se permite si no tiene vínculos (siempre es nuevo, así que no los tiene).

5. Un requerimiento no protegido eliminado:
   - se eliminan físicamente sus postulaciones e invitaciones `PENDIENTE`/`RECHAZADA`.
   - No se notifica a esos candidatos en esta iteración.

### 3.4. Error `409` de requerimiento

```json
{
  "message": "No se puede modificar ni eliminar el requerimiento 28 porque tiene postulaciones o invitaciones aceptadas, o asignaciones asociadas.",
  "codigo": "REQUERIMIENTO_CON_VINCULOS",
  "idRequerimiento": 28,
  "httpStatus": 409,
  "timestamp": 1793729665628
}
```

UI recomendada:

- leer `codigo`;
- resaltar el requerimiento `idRequerimiento`;
- ofrecer “resolver postulaciones/asignaciones” o “quitar el cambio”;
- no reintentar automáticamente.

---

## 4. Modificación de actividad en `CONFIRMADO`

### 4.1. Campos inmutables

En un proyecto `CONFIRMADO` no se pueden cambiar:

- `nombre`;
- `duracion` / `unidadDuracion`;
- `predecesoras`.

Enviar un valor distinto responde `409`.

### 4.2. Campos editables

Sí se pueden cambiar:

- `descripcion`;
- `ubicacion`;
- `horarioPreferenciaInicio`;
- `estado` (preferentemente por `PATCH /estado`).

### 4.3. Requerimientos

Se pueden:

- modificar, agregar o quitar los que **no** estén protegidos;
- dejar intactos los protegidos;
- omitir un protegido (se conserva);
- enviar un protegido sin cambios (no dispara error).

No se pueden:

- modificar un requerimiento con postulación/invitación `PENDIENTE` o `ACEPTADA`;
- modificar un requerimiento con asignación;
- eliminar un requerimiento con cualquiera de esos vínculos.

### 4.4. UI sugerida para `CONFIRMADO`

- Deshabilitar nombre, duración y predecesoras.
- Mantener habilitados descripción, ubicación y preferencia horaria.
- Para cada requerimiento:
  - si `protegido === true`: solo lectura, sin botón de eliminar;
  - si `protegido === false`: edición y eliminación habilitadas.
- Permitir “Agregar requerimiento”.
- No bloquear todo el formulario por un requerimiento protegido.

---

## 5. Creación de actividad en `CONFIRMADO`

### 5.1. Endpoint

```http
POST /proyectos/{idProyecto}/actividades
```

Sin cambios en el payload:

```json
{
  "nombre": "Montaje final",
  "descripcion": "Montaje y prueba final.",
  "duracion": 2,
  "unidadDuracion": "HORAS",
  "horarioPreferenciaInicio": "18:00",
  "ubicacion": {
    "localidadId": "06049010000",
    "provinciaId": "06"
  },
  "predecesoras": [201],
  "requerimientos": []
}
```

### 5.2. Comportamiento en confirmado

- La actividad se crea en estado `PENDIENTE`.
- El backend ejecuta CPM + planificador.
- La actividad recibe `fechaHoraInicio` y `origenInicio = CONFIRMADA`.
- Se permite crearla **sin asignaciones**.
- Si no tiene asignados, la fecha es estructural: respeta dependencias, entrega y “no antes de ahora”, pero no valida calendarios de personas.
- La asignación posterior puede moverla.
- La reprogramación es **global**: otras actividades `PENDIENTE` pueden cambiar de fecha.
- La respuesta es el `ActividadResponse` de la actividad nueva. Para ver el resto del cronograma, refrescar:
  - `GET /proyectos/{idProyecto}/actividades`
  - `GET /proyectos/{idProyecto}/cronograma`

### 5.3. Errores

| HTTP | Situación |
|---|---|
| `400` | Payload inválido, duración, ubicación, características, etc. |
| `403` | Sin permiso `CREAR_ACTIVIDAD` |
| `404` | Proyecto/perfil inexistente |
| `409` | Proyecto `FINALIZADO` o `CANCELADO` |
| `409` | Nombre duplicado |
| `409` | No hay plan viable antes de la entrega |
| `409` | Predecesora inválida o ciclo |

---

## 6. Reprogramación automática

### 6.1. Cuándo se dispara

| Evento | ¿Reprograma? |
|---|---|
| Cambio de jornada o margen | Sí, ya existía |
| Crear actividad en `CONFIRMADO` | Sí |
| Asignar un miembro | Sí, si la actividad está `PENDIENTE` en un proyecto confirmado |
| Desasignar un miembro | Sí, en proyecto confirmado |
| Eliminar una actividad | Sí, en proyecto confirmado |
| Cancelar una actividad | Sí, en proyecto confirmado |
| Bloqueo manual de calendario | No en esta iteración |

### 6.2. Alcance

- Se mueven las actividades `PENDIENTE`.
- `EN_CURSO` y `FINALIZADA` conservan su inicio.
- `CANCELADA` no se toca.
- La reprogramación es global, no solo la actividad afectada.
- Si no hay plan viable, la operación falla y no se persiste ningún cambio parcial.

### 6.3. Refrescos obligatorios para el frontend

Después de:

- crear actividad en confirmado;
- asignar miembro;
- desasignar miembro;
- eliminar actividad;
- cancelar actividad;
- cambiar jornada/margen;

refrescar:

1. planificación de actividades;
2. cronograma/Gantt;
3. calendario del usuario si corresponde;
4. notificaciones.

### 6.4. Notificaciones

- Se mantienen las notificaciones propias de cada operación:
  - `ACTIVIDAD_ASIGNADA`
  - `ACTIVIDAD_DESASIGNADA`
  - `ACTIVIDAD_ELIMINADA`
  - `ACTIVIDAD_MODIFICADA`
  - `AJUSTE_HORARIO` y `SOBRECOMPROMISO` para jornada/margen.
- En esta iteración, las actividades movidas por reprogramación en asignación/desasignación/eliminación/cancelación **no generan una notificación individual adicional**. No prometer al usuario que recibirá aviso por cada actividad movida.

---

## 7. Contrato de respuesta

### 7.1. `ActividadResponse`

No se eliminaron campos. Se mantiene:

- `fechaHoraInicio` / `fechaHoraFin` persistidas;
- `inicioProyectado` / `finProyectado`;
- `estadoCronograma`;
- `critica`, `holguraMakespan`, `holguraEntrega`;
- `ventanaInicio`, `ventanaEjecucion`;
- `predecesoras`;
- `requerimientos`;
- `profesionalesAsignados`.

### 7.2. `requerimientos[].protegido`

Nuevo campo booleano:

- `true`: no se puede modificar ni eliminar;
- `false`: se puede modificar o eliminar.

Puede venir `null` en respuestas de búsqueda que no usen el detalle de actividad. En el detalle de actividad siempre se calcula.

---

## 8. Checklist de implementación frontend

- [ ] Reemplazar los botones de estado por `PATCH /estado`.
- [ ] Mantener el `PUT` solo para edición completa.
- [ ] Leer `requerimientos[].protegido` en el detalle.
- [ ] Deshabilitar edición/eliminación de requerimientos protegidos.
- [ ] Permitir agregar requerimientos en confirmado.
- [ ] No bloquear el formulario completo por requerimientos protegidos.
- [ ] No asumir que omitir un requerimiento lo elimina: si está protegido, se conserva.
- [ ] No reescribir requerimientos sin cambios.
- [ ] Manejar `409 REQUERIMIENTO_CON_VINCULOS` mostrando el `idRequerimiento`.
- [ ] En confirmado, deshabilitar nombre, duración y predecesoras.
- [ ] Habilitar descripción, ubicación, preferencia y estado en confirmado.
- [ ] Mostrar la fecha asignada al crear una actividad en confirmado.
- [ ] Avisar que crear una actividad en confirmado puede reprogramar otras actividades.
- [ ] Refrescar planificación, Gantt y calendario después de crear/asignar/desasignar/eliminar/cancelar.
- [ ] No prometer notificación individual por cada actividad movida.
- [ ] Si la creación o asignación falla por falta de plan viable, mostrar los `bloqueos` del `409`.

---

## 9. Ejemplos rápidos

### 9.1. Iniciar actividad

```http
PATCH /proyectos/105/actividades/301/estado
```

```json
{ "estado": "EN_CURSO" }
```

### 9.2. Crear en confirmado

```http
POST /proyectos/105/actividades
```

```json
{
  "nombre": "Prueba de vestuario",
  "descripcion": "Prueba final con modelos.",
  "duracion": 90,
  "unidadDuracion": "MINUTOS",
  "predecesoras": [301],
  "requerimientos": []
}
```

Respuesta esperada: `201 Created` con `fechaHoraInicio` no nula y `estado: "PENDIENTE"`.

### 9.3. Requerimiento protegido en el `GET`

```json
{
  "idRequerimiento": 28,
  "cantidad": 2,
  "idProfesion": 2,
  "protegido": true
}
```

La UI debe mostrarlo como solo lectura.

---

## 10. Relación con las fichas existentes

Este documento es un **addendum**. Las siguientes fichas contienen secciones que pueden estar desactualizadas y no fueron modificadas:

- `ficha-tecnica-crear-actividad.md`
  - precondiciones de `CONFIRMADO`;
  - creación sin asignación;
  - agendado y reprogramación.
- `ficha-tecnica-modificar-actividad.md`
  - regla de requerimientos;
  - endpoint `PATCH /estado`;
  - inmutabilidad por requerimiento en confirmado.
- `ficha-tecnica-asignar-actividad.md`
  - reprogramación antes de asignar;
  - comportamiento sin advertencia.
- `ficha-tecnica-desasignar-actividad.md`
  - reprogramación en confirmado;
  - eliminación de la advertencia “no reprograma”.
- `ficha-tecnica-eliminar-actividad.md`
  - reprogramación posterior a la eliminación.
- `ficha-tecnica-cpm-margen-gantt-reprogramacion.md`
  - nuevos disparadores de reprogramación además de jornada/margen.

Mientras esas fichas no se actualicen, usar esta como contrato vigente para los cambios descriptos.
