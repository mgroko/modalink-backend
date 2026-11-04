# Ficha técnica frontend: asignación provisional, disponibilidad primero y conflictos CPM

**Proyecto:** ModaLink
**Backend:** Spring Boot
**Fecha:** 2026-11-03
**Estado:** instrucciones de contrato para el frontend. No modifica fichas de `docs/frontend/aplicadas`.

---

## 1. Alcance

Esta ficha documenta los cambios de comportamiento y contrato para:

1. Actividades nuevas en proyectos confirmados con inicio provisional.
2. Asignación de miembros con prioridad de disponibilidad.
3. Preview de asignación con dry-run.
4. Conflictos CPM separados de desborde de entrega.
5. Regla de predecesoras para pasar a `EN_CURSO`/`FINALIZADA`.
6. Endpoint administrativo de reparación de cronogramas.

---

## 2. Inicio provisional de actividades nuevas

Cuando se crea una actividad en un proyecto confirmado:

- la actividad queda `PENDIENTE`;
- puede tener un `fechaHoraInicio` provisional;
- su `origenInicio` es `PROYECTADA`, no `CONFIRMADA`;
- ese inicio **no es inmóvil** y no debe presentarse como una reserva definitiva.

Valores de `origenInicio`:

| Valor | Significado |
|---|---|
| `PROYECTADA` | Inicio provisional/proyectado. Se puede reprogramar. |
| `CONFIRMADA` | Inicio persistido por el plan confirmado. Sigue siendo reprogramable mientras la actividad esté `PENDIENTE`. |
| `REAL` | Inicio real de una actividad `EN_CURSO` o `FINALIZADA`. No se mueve. |

Regla de inmovilidad:

- `EN_CURSO` y `FINALIZADA`: nunca se mueven.
- `PENDIENTE`: siempre reprogramable, sin importar si su origen es `PROYECTADA` o `CONFIRMADA`.
- `PENDIENTE` con `origenInicio = REAL`: dato inconsistente; el backend lo reporta/repara.

En la UI:

- mostrar la actividad nueva como “provisional” o “sin confirmar”;
- no mostrar su horario como definitivo;
- permitir que la asignación lo mueva.

---

## 3. Disponibilidad primero

Al asignar un miembro a una actividad `PENDIENTE` de un proyecto confirmado:

1. El backend calcula el rango factible CPM:
   - inicio mínimo: después de las predecesoras;
   - límite: entrega y sucesoras fijas.
2. Dentro de ese rango, busca la **primera ventana común disponible** de los miembros a asignar.
3. La actividad se mueve a esa ventana.
4. Solo si no existe ninguna ventana válida se rechaza la asignación.

No se debe validar al miembro contra un horario fijo ya calculado por el sistema. La disponibilidad de los miembros manda dentro del rango factible.

La actividad objetivo y sus sucesoras transitivas `PENDIENTE` pueden moverse. Las predecesoras **no** se mueven: solo son restricción de inicio mínimo. Las sucesoras `EN_CURSO`/`FINALIZADA` no se mueven y bloquean el movimiento.

---

## 4. Preview de asignación con dry-run

`GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad` ahora:

- para una actividad `PENDIENTE` de un proyecto confirmado, simula la asignación;
- no persiste nada;
- devuelve el hueco propuesto;
- devuelve el detalle de las actividades que se moverían;
- evalúa la agenda de los miembros, no solo el slot actual.

### 4.1. Nuevos campos en `ResultadoDisponibilidad`

```json
{
  "disponible": true,
  "provisional": false,
  "inicioEvaluado": "2026-11-06T16:44:16",
  "finEvaluado": "2026-11-10T23:59:00",
  "inicioSeleccionado": "2026-11-08T09:15:00",
  "finSeleccionado": "2026-11-08T11:15:00",
  "motivo": null,
  "codigoMotivo": null,
  "miembros": [],
  "reprogramacionPropuesta": [
    {
      "idActividad": 26,
      "nombre": "Fitting",
      "estado": "PENDIENTE",
      "inicioAnterior": "2026-11-06T16:44:16",
      "inicioNuevo": "2026-11-08T09:15:00"
    }
  ]
}
```

| Campo | Uso |
|---|---|
| `codigoMotivo` | Código estable del resultado. `null` si hay disponibilidad. |
| `reprogramacionPropuesta` | Actividades que se moverían si se confirma la asignación. |

### 4.2. Códigos de motivo

| Código | Significado |
|---|---|
| `SIN_MIEMBROS` | No se recibieron miembros. |
| `SIN_JORNADA` | El miembro no tiene jornada en el rango. |
| `DURACION_EXCEDE_VENTANA` | La actividad no entra en la ventana laboral. |
| `SIN_HUECO` | Hay jornada pero no hay hueco. |
| `SIN_HUECO_COMUN` | Hay huecos individuales, pero no comunes. |
| `FUERA_TOLERANCIA_PREFERENCIA` | No hay hueco dentro de la tolerancia de la hora preferida. |
| `CONFLICTO_FIJO` | La actividad fija choca con una predecesora/sucesora o con una restricción fija. |
| `DATOS_INCONSISTENTES` | Hay datos inválidos, por ejemplo `PENDIENTE` con `origenInicio = REAL`. |
| `CRONOGRAMA_NO_VIABLE` | El CPM no puede construir un plan válido. |

El frontend debe priorizar `codigoMotivo` sobre el texto de `motivo`.

---

## 5. `EstadoCronograma.CONFLICTO`

Se agrega el estado:

```text
CONFLICTO
```

Uso:

- `NORMAL`: plan viable.
- `ALERTA_DESBORDE`: el plan no entra en la entrega o tiene holguras negativas.
- `CONFLICTO`: hay conflictos estructurales, por ejemplo una actividad fija que empieza antes de que termine su predecesora.
- `SIN_FECHA_ENTREGA`: no se puede evaluar.

El frontend **no debe mostrar “alerta por desborde”** cuando el estado es `CONFLICTO`. Debe mostrar el conflicto real con su `motivo`/`codigoMotivo`.

---

## 6. Regla de estado de actividades

Una actividad `PENDIENTE` solo puede pasar a `EN_CURSO` si todas sus predecesoras no canceladas están `FINALIZADA`.

También aplica para `FINALIZADA`: no se puede finalizar una actividad cuyas predecesoras no estén finalizadas.

- Predecesoras `CANCELADA`: se excluyen, no bloquean.
- Si la regla no se cumple, el backend responde `409` con un mensaje que indica qué predecesora falta finalizar.

En la UI:

- deshabilitar “Iniciar actividad” si alguna predecesora no está `FINALIZADA`;
- mostrar qué predecesora bloquea;
- no ofrecer “Finalizar” en las mismas condiciones.

---

## 7. Endpoint administrativo de reparación

Solo para administradores con `ADMINISTRAR_CONFIGURACION`.

### 7.1. Detección

```http
GET /admin/cronograma/inconsistencias?idProyecto=&idActividad=&limite=
```

Devuelve las actividades inconsistentes:

- `PENDIENTE` con `origenInicio = REAL`;
- `PENDIENTE` con inicio anterior al fin de una predecesora;
- `EN_CURSO`/`FINALIZADA` con predecesoras no `FINALIZADA`.

### 7.2. Reparación

```http
POST /admin/cronograma/reparar
```

Body:

```json
{
  "dryRun": true,
  "idProyecto": null,
  "idActividad": null,
  "limite": 50
}
```

- `dryRun: true` por defecto.
- `dryRun: false` aplica la reparación.
- Solo repara `PENDIENTE`.
- No mueve `EN_CURSO`/`FINALIZADA`.
- No mueve predecesoras.
- Si un proyecto no se puede reparar, lo reporta y no deja cambios parciales.

Respuesta:

```json
{
  "dryRun": false,
  "totalDetectadas": 3,
  "totalReparadas": 2,
  "totalFallidas": 1,
  "inconsistencias": [],
  "errores": [
    "Proyecto 101, actividad 77: requiere intervención manual; no se mueven actividades en curso o finalizadas."
  ]
}
```

La UI administrativa debe:

- ejecutar primero `dryRun`;
- mostrar el detalle;
- pedir confirmación;
- aplicar;
- refrescar los cronogramas afectados.

---

## 8. Resumen de cambios para el frontend

- [ ] Mostrar actividades nuevas como provisionales (`PROYECTADA`).
- [ ] No tratar `CONFIRMADA` como “no se puede mover” para una `PENDIENTE`.
- [ ] Usar `codigoMotivo` para distinguir conflictos reales.
- [ ] Mostrar `CONFLICTO` distinto de `ALERTA_DESBORDE`.
- [ ] Leer `reprogramacionPropuesta` en el preview de asignación.
- [ ] No bloquear la asignación con el slot fijo actual; usar el dry-run.
- [ ] Deshabilitar el inicio/fin de actividad si las predecesoras no están `FINALIZADA`.
- [ ] Agregar la pantalla administrativa de detección/reparación con `dryRun`.
- [ ] Refrescar calendario, cronograma y notificaciones después de reparar.
