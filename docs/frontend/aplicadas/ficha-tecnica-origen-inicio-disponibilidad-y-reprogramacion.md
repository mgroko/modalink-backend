# Ficha técnica frontend: origen del inicio, disponibilidad y reprogramación

**Proyecto:** ModaLink
**Backend:** Spring Boot
**Fecha:** 2026-11-04
**Estado:** contrato para el frontend. Es una ficha nueva y no modifica las fichas existentes de `docs/frontend` ni las de `docs/frontend/aplicadas`.

---

## 1. Alcance

Esta ficha documenta el contrato que el frontend debe consumir para:

1. Interpretar el `origenInicio` calculado de una actividad.
2. Saber qué actividades se pueden reprogramar y cuáles no.
3. Leer el preview de disponibilidad con `codigoMotivo` y `reprogramacionPropuesta`.
4. Manejar los `409` de asignación, postulación y aceptación.
5. Entender qué ocurre al asignar, aceptar una postulación y desasignar.
6. Refrescar calendario, cronograma y planificación después de una reprogramación.
7. Configurar jornada, margen y bloqueos manuales sin romper el cronograma.

No cubre snapshots, historial, protección de actividades, auditoría ni bajas con snapshot. Esos flujos pertenecen a otro frente.

Esta ficha complementa a `docs/frontend/aplicadas/ficha-tecnica-asignacion-provisional-y-conflictos.md`. Si hay diferencias, para este alcance manda el contrato descrito acá.

---

## 2. `origenInicio`: valor calculado, no persistido

`origenInicio` **no es una columna ni un dato editable**. El backend lo calcula en cada consulta a partir de:

- el estado de la actividad;
- su `fechaHoraInicio` persistida;
- la existencia de asignaciones activas;
- el estado del proyecto.

Valores posibles:

| `origenInicio` | Cuándo aparece | Lectura de UI |
|---|---|---|
| `PROYECTADA` | Actividad `PENDIENTE` sin inicio persistido, o proyecto no `CONFIRMADO`, o sin asignaciones activas. | Chip **“Provisional”**. |
| `CONFIRMADA` | Actividad `PENDIENTE` con inicio persistido, proyecto `CONFIRMADO` y al menos una asignación activa. | Chip **“Confirmada”**. Hay un inicio persistido y cobertura activa, pero **sigue siendo reprogramable**. |
| `REAL` | Actividad `EN_CURSO` o `FINALIZADA`. | Chip **“Real”**. Inicio real; no se reprograma. |
| `null` | Actividad `CANCELADA`. | Sin chip de origen. |

Reglas de presentación:

- `PROYECTADA` no significa “la actividad no existe”: significa que su inicio es una proyección.
- `CONFIRMADA` **no significa “inmóvil”**. Una actividad `PENDIENTE` con origen `CONFIRMADA` puede moverse si cambia la disponibilidad, la jornada, un bloqueo o la asignación.
- `REAL` es el único origen que coincide con una actividad inmóvil, porque solo `EN_CURSO` y `FINALIZADA` son inmóviles.
- El valor puede cambiar entre dos consultas después de una asignación, aceptación, desasignación o reprogramación. No lo guardes como verdad local: refresca la actividad.
- No uses `origenInicio` para deshabilitar acciones de reprogramación. La decisión de movilidad depende del **estado de la actividad**, no del origen.

Pantallas donde mostrar el chip:

- detalle de actividad, tabla de planificación, Gantt/cronograma y resultados de búsqueda;
- en edición, mostrarlo como dato de solo lectura; nunca como campo editable;
- en creación, recién después de guardar, usando el `origenInicio` que devuelve la respuesta.

### 2.1. `provisional` no es lo mismo que `origenInicio`

En `ResultadoDisponibilidad` y en `AsignacionActividadResponse` existe un booleano `provisional`.

| Campo | Significado |
|---|---|
| `origenInicio` | Origen efectivo del inicio de **una actividad**. |
| `provisional` | La evaluación de disponibilidad se hizo sobre un rango estimado porque **el proyecto no tiene fecha de inicio confirmada**. |

Un `provisional: true` no convierte a la actividad en “provisional” ni cambia su `origenInicio`. Son conceptos distintos y deben mostrarse por separado. En la UI, el badge del preview debe decir **“Evaluación provisional”** para no confundirlo con el chip **“Provisional”** de la actividad.

### 2.2. Dónde viaja `origenInicio`

Se expone en los cuatro DTOs de actividad:

| DTO | Uso principal |
|---|---|
| `ActividadResponse` | Detalle de actividad. |
| `ActividadPlanificacionResponse` | Tabla de Planificación. |
| `ActividadCronogramaResponse` | Gantt y tabla CPM. |
| `ActividadBusquedaResponse` | Búsqueda de actividades. |

Fragmentos de respuesta:

```json
{
  "idActividad": 42,
  "estado": "PENDIENTE",
  "fechaHoraInicio": "2026-11-08T09:15:00",
  "estadoCronograma": "NORMAL",
  "tentativo": false,
  "origenInicio": "CONFIRMADA"
}
```

```json
{
  "idActividad": 42,
  "estado": "PENDIENTE",
  "fechaHoraInicio": "2026-11-08T09:15:00",
  "inicioProyectado": "2026-11-08T09:15:00",
  "inicioPersistido": "2026-11-08T09:15:00",
  "tentativo": false,
  "origenInicio": "PROYECTADA"
}
```

No confundir `tentativo` con `origenInicio`:

- `tentativo` indica que el proyecto no tiene fecha de inicio confirmada;
- `origenInicio` indica el origen del inicio de esa actividad.

Compatibilidad defensiva:

- si `origenInicio` no viene en la respuesta, tratarlo como desconocido y no mostrar chip; no romper la pantalla;
- si `codigoMotivo` trae un valor no reconocido, mostrar `motivo` o un mensaje genérico;
- antes de dar por desplegado el contrato, verificar en el ambiente una respuesta real con `origenInicio`, `codigoMotivo` y `reprogramacionPropuesta`.

---

## 3. Movilidad de actividades

La regla es simple y no depende del origen calculado:

| Estado de la actividad | `origenInicio` posible | ¿Se puede reprogramar? |
|---|---|---|
| `PENDIENTE` | `PROYECTADA` o `CONFIRMADA` | **Sí, siempre**. |
| `EN_CURSO` | `REAL` | No. |
| `FINALIZADA` | `REAL` | No. |
| `CANCELADA` | `null` | No. |

Alcance del comportamiento:

- Aplica a proyectos `PUBLICADO` y `CONFIRMADO`.
- No aplica a `BORRADOR`: un borrador no tiene cronograma comprometido y no dispara reprogramación.
- Un proyecto `FINALIZADO` o `CANCELADO` no se reprograma.
- La `CONFIRMADA` de una actividad `PENDIENTE` no es una restricción dura.

Cuando una reprogramación mueve una actividad `PENDIENTE`:

- también mueven sus sucesoras `PENDIENTE`;
- no se mueven sus predecesoras;
- no se mueven actividades `EN_CURSO` ni `FINALIZADA`;
- los bloqueos manuales y las actividades fijas siguen siendo restricciones duras.

En la UI:

- mostrar el chip de origen, no un candado;
- no impedir reprogramar una `PENDIENTE` por tener `origenInicio = CONFIRMADA`;
- si una actividad `EN_CURSO` o `FINALIZADA` quedó fuera de la nueva disponibilidad, el backend la reporta como conflicto fijo y no la mueve;
- no ofrecer “mover” sobre actividades ejecutadas.

### 3.1. Proyecto en `BORRADOR`

La asignación se permite en `BORRADOR`, pero **no reprograma** el cronograma. El modal debe informar:

> “Este proyecto está en borrador: la asignación se guarda, pero no se reprograma el cronograma. Las fechas se calcularán al publicar o confirmar el proyecto.”

No bloquear la asignación por este motivo. Si el backend la rechaza, mostrar su mensaje.

---

## 4. Preview de disponibilidad

Endpoint:

```http
GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad?idPerfil={idPerfil}
```

`idPerfil` es opcional y se puede repetir para evaluar varios perfiles con hueco común. Sin `idPerfil`, evalúa el perfil activo del solicitante.

La consulta es de solo lectura: no reserva, ni asigna, ni reprograma nada.

### 4.1. Qué calcula el backend

| Caso | Comportamiento |
|---|---|
| Proyecto `PUBLICADO`/`CONFIRMADO` y actividad `PENDIENTE` | Simula la asignación completa: mueve copias de la actividad y sus sucesoras a la primera ventana común. Devuelve `reprogramacionPropuesta`. |
| Proyecto `BORRADOR` o actividad no `PENDIENTE` | Usa la verificación clásica de calendario. `reprogramacionPropuesta` queda vacío. |

La simulación no persiste nada y no debe interpretarse como una reserva. El horario recién se confirma con la asignación o la aceptación real.

### 4.2. Respuesta

```json
{
  "disponible": true,
  "provisional": false,
  "inicioEvaluado": "2026-11-06T00:00:00",
  "finEvaluado": "2026-11-10T23:59:00",
  "inicioSeleccionado": "2026-11-08T09:15:00",
  "finSeleccionado": "2026-11-08T11:15:00",
  "motivo": null,
  "codigoMotivo": null,
  "miembros": [],
  "resultadoPreferencia": "EXACTO",
  "desvioMinutos": 0,
  "inicioPreferido": "2026-11-08T09:15:00",
  "miembrosBloqueantes": [],
  "reprogramacionPropuesta": [
    {
      "idActividad": 42,
      "nombre": "Fitting",
      "estado": "PENDIENTE",
      "inicioAnterior": "2026-11-06T16:44:16",
      "inicioNuevo": "2026-11-08T09:15:00"
    },
    {
      "idActividad": 43,
      "nombre": "Prueba final",
      "estado": "PENDIENTE",
      "inicioAnterior": null,
      "inicioNuevo": "2026-11-08T11:45:00"
    }
  ]
}
```

Campos relevantes:

| Campo | Uso |
|---|---|
| `disponible` | Hay o no hay ventana válida. |
| `provisional` | La ventana se calculó sobre un proyecto sin fecha de inicio confirmada. |
| `inicioEvaluado` / `finEvaluado` | Rango factible evaluado. |
| `inicioSeleccionado` / `finSeleccionado` | Primer hueco propuesto; `null` si no hay. |
| `motivo` | Texto legible del problema. Si viene informado, es el mensaje principal a mostrar. |
| `codigoMotivo` | Código estable para clasificar (icono, color, acción y etiqueta de respaldo). No reemplaza el texto de `motivo`. |
| `miembros` | Detalle individual por miembro (incluye `idPerfil` y `nombreArtistico` cuando se conocen). |
| `resultadoPreferencia` | `SIN_PREFERENCIA`, `EXACTO`, `DESVIO` o `SIN_LUGAR`. |
| `desvioMinutos` | Diferencia con la hora preferida; puede ser negativa. |
| `inicioPreferido` | Hora preferida aplicada al día del hueco elegido. |
| `miembrosBloqueantes` | Usuarios que impiden el hueco común, si se pudieron aislar. |
| `reprogramacionPropuesta` | Actividades que se moverían al confirmar la asignación. |

`reprogramacionPropuesta` incluye la actividad objetivo y sus sucesoras `PENDIENTE` que cambian de inicio. `inicioAnterior` puede ser `null` si la actividad todavía no tenía inicio persistido. Si la simulación no encuentra ventana, la propuesta vuelve vacía y el resultado trae `disponible = false` con `codigoMotivo`.

### 4.3. Códigos de motivo

| Código | Significado |
|---|---|
| `SIN_MIEMBROS` | No se recibieron miembros para evaluar. |
| `SIN_JORNADA` | El miembro no tiene jornada en el rango. |
| `DURACION_EXCEDE_VENTANA` | La actividad dura más que la ventana laboral disponible. |
| `SIN_HUECO` | Hay jornada, pero no hay hueco. |
| `SIN_HUECO_COMUN` | Hay huecos individuales, pero no un hueco común entre todos. |
| `FUERA_TOLERANCIA_PREFERENCIA` | Hay hueco, pero fuera de la tolerancia de la hora preferida. |
| `CONFLICTO_FIJO` | La actividad choca con una restricción fija o con una predecesora/sucesora fija. |
| `DATOS_INCONSISTENTES` | Faltan datos o son incoherentes para calcular. |
| `CRONOGRAMA_NO_VIABLE` | El CPM no puede construir un plan válido. |
| `SIN_DISPONIBILIDAD` | El planificador no encontró ventana para la asignación. |
| `FECHA_ENTREGA_INVALIDA` | La fecha de entrega no permite planificar. |
| `ACTIVIDAD_NO_REPROGRAMABLE` | La actividad no está en un estado reprogramable. |
| `ERROR_REPROGRAMACION` | Error inesperado al reprogramar un proyecto; aparece en el resumen de reprogramación, no en el preview. |

Regla de UI:

- si `disponible = true`, no mostrar el motivo;
- si `disponible = false` y `motivo` trae texto, mostrar ese texto como mensaje principal;
- usar `codigoMotivo` para elegir icono, color, acción y como etiqueta de respaldo cuando `motivo` venga vacío;
- nunca inferir el motivo parseando el texto libre de `motivo`.

---

## 5. Errores `409`

### 5.1. Asignación sin disponibilidad

`POST /proyectos/{idProyecto}/actividades/{idActividad}/asignaciones` puede responder `409` con:

```json
{
  "message": "No hay disponibilidad común suficiente dentro del rango factible de la actividad.",
  "motivo": "SIN_HUECO_COMUN",
  "codigoMotivo": "SIN_HUECO_COMUN",
  "provisional": false,
  "inicioEvaluado": "2026-11-06T00:00:00",
  "finEvaluado": "2026-11-10T23:59:00",
  "miembrosBloqueantes": [10, 22],
  "httpStatus": 409,
  "timestamp": 1762290000000
}
```

Notas:

- `codigoMotivo` puede ser un código de calendario (`SIN_HUECO`, `SIN_HUECO_COMUN`, etc.) o `SIN_DISPONIBILIDAD` cuando lo detecta el planificador.
- Este `409` **no incluye `reprogramacionPropuesta`**. Si el front necesita mostrar qué se movería, debe volver a consultar el endpoint de disponibilidad.
- `miembrosBloqueantes` contiene `idUsuario`, no `idPerfil` ni nombres. Para mostrar nombres, resolverlo contra `miembros` del preview (`idUsuario` → `idPerfil`/`nombreArtistico`) o contra el selector de miembros del proyecto. Si no se puede resolver, mostrar “Usuario N”.
- `miembrosBloqueantes` puede venir vacío aunque no haya hueco: no usarlo como único diagnóstico.

### 5.2. Postulación o aceptación no permitida

`POST /proyectos/{idProyecto}/postulaciones/...` puede responder `409` con:

```json
{
  "message": "No hay disponibilidad común suficiente dentro del rango factible de la actividad.",
  "codigoMotivo": "SIN_HUECO_COMUN",
  "httpStatus": 409,
  "timestamp": 1762290000000
}
```

En este caso no hay detalle de miembros ni rango evaluado. Si el front necesita ese detalle, debe consultar el preview antes de reintentar.

Regla general: en ambos `409`, leer `codigoMotivo`; si falta, usar `message` como respaldo.

---

## 6. Asignación, aceptación y desasignación

### 6.1. Asignación directa

```http
POST /proyectos/{idProyecto}/actividades/{idActividad}/asignaciones
Content-Type: application/json

{ "idPerfil": 123 }
```

Comportamiento:

- revalida disponibilidad en el backend;
- si el proyecto es `PUBLICADO`/`CONFIRMADO` y la actividad es `PENDIENTE`, mueve la actividad y sus sucesoras `PENDIENTE` a una ventana común antes de guardar la asignación;
- si no hay ventana, responde `409` y no crea la asignación;
- al confirmar, publica un evento después del commit para reprogramar **otros** proyectos del mismo usuario, excluyendo el proyecto actual.

Respuesta `201`:

```json
{
  "idAsignacionActividad": 501,
  "idProyecto": 12,
  "idActividad": 42,
  "idMiembro": 88,
  "idPerfil": 123,
  "idUsuario": 10,
  "idRequerimientoActividad": 7,
  "requerimientoSatisfecho": true,
  "provisional": false,
  "inicioSeleccionado": "2026-11-08T09:15:00",
  "finSeleccionado": "2026-11-08T11:15:00",
  "resultadoPreferencia": "EXACTO",
  "desvioMinutos": 0
}
```

Importante:

- `inicioSeleccionado` y `finSeleccionado` son informativos: describen el hueco usado para validar, no una reserva persistida aparte;
- `provisional` se refiere al proyecto, no a la actividad;
- después de asignar, el `origenInicio` puede pasar de `PROYECTADA` a `CONFIRMADA`;
- refrescar detalle, planificación, cronograma y calendario.

### 6.2. Aceptación de postulación

```http
POST /proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/aceptar
```

Comportamiento equivalente a la asignación:

- revalida disponibilidad con el postulante y los miembros ya asignados;
- si no hay hueco común, responde `409` con `codigoMotivo`;
- si hay hueco, reprograma la actividad y sus sucesoras `PENDIENTE`, crea la asignación y publica el evento after-commit;
- devuelve `PostulacionGestionadaResponse` con `resultadoPreferencia`, `desvioMinutos` y `advertencias`.

La aceptación puede devolver advertencias no bloqueantes por sobrecompromiso del profesional en otros proyectos. Mostrarlas, pero no impedir la operación.

### 6.3. Desasignación

```http
DELETE /proyectos/{idProyecto}/actividades/{idActividad}/asignaciones/{idAsignacionActividad}
```

Comportamiento:

- elimina la asignación y libera el cupo del requerimiento;
- reprograma el proyecto actual si está `PUBLICADO`/`CONFIRMADO` y tiene fecha de entrega;
- publica el evento after-commit para reprogramar otros proyectos del usuario, excluyendo el actual;
- no reabre postulaciones rechazadas.

Respuesta `200`:

```json
{
  "idAsignacionActividad": 501,
  "idProyecto": 12,
  "idActividad": 42,
  "idMiembro": 88,
  "idPerfil": 123,
  "idUsuario": 10,
  "idRequerimientoActividad": 7,
  "requerimientoSatisfecho": false,
  "cupo": {
    "cantidad": 2,
    "cubiertos": 1,
    "faltantes": 1,
    "satisfecho": false
  },
  "advertencias": [
    "El requerimiento quedó con 1 vacante(s)."
  ]
}
```

Si `advertencias` no está vacío, mostrarlo como aviso. La desasignación ya se aplicó.

---

## 7. Reprogramación cruzada entre proyectos

La asignación, la aceptación y la desasignación pueden afectar **otros proyectos** del mismo usuario.

Características:

- se ejecuta después del commit de la operación original;
- excluye el proyecto que originó el cambio;
- cada proyecto se reprograma en su propia transacción;
- si un proyecto falla, los demás continúan y el proyecto fallido conserva su plan anterior;
- las notificaciones son best-effort y no revierten la reprogramación.

Consecuencia para el frontend:

- la operación actual reprograma el proyecto actual de forma síncrona: refrescar sus vistas alcanza para ese proyecto;
- los demás proyectos se reprograman after-commit y pueden quedar desactualizados hasta que se abran;
- las actividades `EN_CURSO` y `FINALIZADA` no se mueven;
- una actividad fija que queda fuera de la nueva disponibilidad se informa como conflicto fijo y no se mueve.

Decisión acordada para esta UI:

- refrescar las vistas actuales al terminar la operación;
- las demás vistas se actualizan al abrirse;
- si hay una vista agregada o global (calendario global, tablero con varios proyectos), ofrecer un botón **“Actualizar”** en el toast de éxito;
- no hacer polling ni bloquear la operación principal.

No hay un endpoint de “esperar a que termine la reprogramación”. El evento corre después del commit, por lo que un refresco inmediato puede no reflejar todavía los proyectos secundarios. Por eso las vistas agregadas no deben asumir consistencia inmediata.

En el resumen de reprogramación, `ERROR_REPROGRAMACION` se muestra como fallo técnico genérico, con el nombre del proyecto y el `mensaje` devuelto. No ocultarlo y no mostrar el código crudo. Distinguirlo de los fallos de dominio (`SIN_DISPONIBILIDAD`, `DATOS_INCONSISTENTES`, etc.) usando el campo `codigo` de `ReprogramacionFallidaResponse`.

---

## 8. Jornada, margen y bloqueos

### 8.1. Guardar jornada

```http
PUT /calendario/jornada
Content-Type: application/json

{
  "margenActividadMinutos": 30,
  "dias": [
    {
      "diaSemana": 1,
      "horaInicioManana": "09:00",
      "horaFinManana": "13:00",
      "horaInicioTarde": "14:00",
      "horaFinTarde": "18:00"
    }
  ]
}
```

La operación **reemplaza** la jornada completa:

- los días enviados son los laborables;
- un día ausente deja de ser laborable;
- `diaSemana` usa ISO 8601: 1 = lunes, 7 = domingo;
- `margenActividadMinutos` es obligatorio y no puede ser negativo.

Respuesta:

```json
{
  "margenActividadMinutos": 30,
  "dias": [],
  "reprogramacion": {
    "totalProyectos": 2,
    "exitosos": [],
    "fallidos": []
  }
}
```

`reprogramacion` solo aparece al guardar. Las lecturas de jornada no lo incluyen.

El backend elige el motivo automáticamente:

- `CAMBIO_MARGEN` si cambió el margen (aunque además cambie el horario);
- `CAMBIO_JORNADA` si el margen no cambió y cambió el horario de los días.

Si la reprogramación tiene fallidos, no revierte la jornada guardada. Mostrar el resumen y ofrecer reintento o revisión manual.

### 8.2. Caveat del margen

El margen efectivo del usuario no está expuesto como campo propio. `ConfigJornadaResponse.margenActividadMinutos` devuelve el valor **crudo** guardado en la agenda; si es `null`, el backend usa el default del sistema (`AGENDA_MARGEN_ACTIVIDAD_MIN`, 30 por defecto), pero ese valor efectivo no se devuelve.

Por eso:

- el front **no debe enviar `margenActividadMinutos: 0`** para “conservar” el margen al guardar solo los días: `0` elimina el margen;
- si el valor leído es `null`, **bloquear el guardado de la jornada** con un aviso del tipo “No se puede guardar: falta definir el margen efectivo” y escalar a backend/producto. No enviar `30` ni `0` automáticamente;
- no calcular ni mostrar el margen efectivo en el front como si fuera el valor persistido;
- al cambiar únicamente los días, reenviar el margen actual leído de `GET /calendario` (`jornada.margenActividadMinutos`).

### 8.3. Bloqueos manuales

Crear:

```http
POST /calendario/bloqueos
Content-Type: application/json

{
  "fechaHoraInicio": "2026-11-08T15:00:00",
  "fechaHoraFin": "2026-11-08T17:00:00",
  "motivo": "Trámite personal"
}
```

Eliminar:

```http
DELETE /calendario/bloqueos/{idBloqueo}
```

Reglas:

- el motivo es obligatorio y admite hasta 200 caracteres;
- el bloqueo no puede superponerse con otro bloqueo manual ni con el horario comprometido por una actividad de un proyecto activo;
- un alta o baja exitosa dispara la reprogramación con motivo `CAMBIO_BLOQUEO`;
- los bloqueos manuales son restricciones duras: el planificador no los puede atravesar;
- si el alta falla por superposición, no se reprograma nada.

En la UI:

- validar el rango antes de enviar (`fechaHoraFin` posterior a `fechaHoraInicio`);
- mostrar el error de superposición sin ofrecer “forzar”;
- después de crear o eliminar un bloqueo, refrescar calendario y las vistas actuales.

---

## 9. Cronograma y conflictos

### 9.1. `EstadoCronograma`

Valores:

| Estado | Significado |
|---|---|
| `NORMAL` | Plan viable. |
| `ALERTA_DESBORDE` | El plan no entra en la entrega o tiene holguras negativas. |
| `CONFLICTO` | Hay conflictos estructurales, por ejemplo una actividad fija que empieza antes de que termine su predecesora. |
| `SIN_FECHA_ENTREGA` | No se puede evaluar. |

Regla de UI:

- si el estado es `CONFLICTO`, **no mostrar “alerta por desborde”**;
- mostrar el conflicto real con el detalle de la fila (`conflicto`, `motivoConflicto`) o de `conflictos`;
- `ALERTA_DESBORDE` y `CONFLICTO` son mensajes distintos.

### 9.2. Detalle por fila

`ActividadCronogramaResponse` incluye:

- `inicioProyectado` / `finProyectado`: proyección CPM;
- `inicioPersistido` / `finPersistido`: fechas realmente guardadas;
- `tentativo`: el proyecto no tiene fecha de inicio confirmada;
- `conflicto` / `motivoConflicto`: problema estructural de esa actividad;
- `fijo`: la actividad tiene una restricción fija;
- `origenInicio`: origen calculado.

En un proyecto `CONFIRMADO`, el Gantt debe priorizar `inicioPersistido`/`finPersistido`. En uno no confirmado, `inicioProyectado`/`finProyectado`.

### 9.3. Conflictos del cronograma

`CronogramaResponse.conflictos` es una lista de `ConflictoCpm`:

```json
[
  {
    "idActividad": 42,
    "tipo": "CONFLICTO_FIJO",
    "detalle": "La actividad fija empieza antes de que termine su predecesora."
  }
]
```

Tipos:

- `FUERA_DE_PROYECTO`: la actividad fija empieza antes del inicio del proyecto;
- `CONFLICTO_FIJO`: una predecesora termina después del inicio fijo de su sucesora;
- `DATO_INCONSISTENTE`: falta un dato imprescindible.

`ConflictoCpm` no trae el nombre de la actividad. Resolverlo con `idActividad` contra `actividades` del mismo `CronogramaResponse` y mostrar **nombre + detalle**. Si no se encuentra, mostrar el ID. En los conflictos por fila, usar directamente `nombre` de la fila.

### 9.4. Regla de predecesoras

Una actividad `PENDIENTE` solo puede pasar a `EN_CURSO` si todas sus predecesoras no canceladas están `FINALIZADA`. Lo mismo aplica para finalizar.

- predecesoras `CANCELADA` se excluyen;
- si la regla no se cumple, el backend responde `409`;
- deshabilitar “Iniciar actividad” y “Finalizar” cuando corresponda;
- indicar qué predecesora bloquea.

---

## 10. Matriz de refresco

| Acción | Refrescar |
|---|---|
| Consultar disponibilidad (preview) | Nada. Es solo lectura. |
| Asignar actividad | Vistas actuales: detalle, planificación, cronograma y calendario. Los demás proyectos se actualizan al abrirse; si hay vista global, ofrecer “Actualizar”. |
| Aceptar postulación | Vistas actuales: detalle, postulaciones, planificación, cronograma y calendario. Demás proyectos al abrirse. |
| Desasignar | Vistas actuales: detalle, planificación, cronograma, cupos/requerimientos y calendario. Demás proyectos al abrirse. |
| Guardar jornada | Calendario y vistas actuales; los demás proyectos al abrirse. |
| Crear o eliminar bloqueo | Calendario y vistas actuales; los demás proyectos al abrirse. |
| Reprogramación con fallidos | Mantener el plan anterior del proyecto fallido, mostrar `ERROR_REPROGRAMACION` como fallo técnico y ofrecer revisión/reintento. |

Recomendación: tratar `origenInicio`, `estadoCronograma`, `conflicto` y `tentativo` como datos volátiles. Refrescarlos después de cada mutación en lugar de recalcularlos en el front.

---

## 11. Checklist de implementación

- [ ] Mostrar chip “Provisional” cuando `origenInicio = PROYECTADA`.
- [ ] Mostrar chip “Confirmada” cuando `origenInicio = CONFIRMADA` y “Real” cuando es `REAL`.
- [ ] No mostrar “inmóvil” para `origenInicio = CONFIRMADA`.
- [ ] No mostrar chip de origen cuando `origenInicio = null`.
- [ ] Distinguir `origenInicio` de `provisional` y de `tentativo`; renombrar el badge del preview a “Evaluación provisional”.
- [ ] Tratar toda actividad `PENDIENTE` como reprogramable.
- [ ] No ofrecer reprogramación sobre `EN_CURSO` ni `FINALIZADA`.
- [ ] Usar `codigoMotivo` para clasificar y `motivo` para el texto visible.
- [ ] Mostrar `reprogramacionPropuesta` en el preview de asignación.
- [ ] Reconsultar el preview si el `409` de asignación no trae la propuesta.
- [ ] Resolver `miembrosBloqueantes` a nombres usando el preview o el selector de miembros.
- [ ] Refrescar las vistas actuales después de asignar, aceptar o desasignar.
- [ ] En vistas agregadas, ofrecer un botón “Actualizar” en lugar de polling.
- [ ] Informar en BORRADOR que la asignación no reprograma el cronograma.
- [ ] No enviar `margenActividadMinutos: 0` para conservar el margen.
- [ ] Bloquear el guardado de jornada si el margen viene `null`, sin enviar `30` ni `0`.
- [ ] Mostrar el resumen de `reprogramacion` al guardar jornada, incluido `ERROR_REPROGRAMACION`.
- [ ] Refrescar luego de crear o eliminar un bloqueo manual.
- [ ] Distinguir `CONFLICTO` de `ALERTA_DESBORDE` y mostrar nombre + detalle del conflicto.
- [ ] Respetar la regla de predecesoras para iniciar y finalizar.

---

## 12. Fuera de alcance

No forman parte de esta ficha:

- snapshots de actividad;
- historial de cambios;
- `fecha_archivado` y protección de requerimientos;
- auditoría de bajas;
- eliminación con snapshot;
- reconciliación administrativa masiva.

Esos flujos se documentan y se implementan por separado.
