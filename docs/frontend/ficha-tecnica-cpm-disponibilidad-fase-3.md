# Ficha técnica para frontend

## CPM, proyecciones y disponibilidad — Fases 1 a 3

**Proyecto:** ModaLink  
**Backend:** Spring Boot  
**Fecha de referencia:** 2026-10-27  
**Estado:** backend CPM/disponibilidad hasta fase 3; **endpoint HTTP de consulta de disponibilidad implementado**; **pestañas Planificación y Cronograma implementadas en backend**; **UC-34 (aceptación de postulaciones) implementado**; UC-60 (confirmación de proyecto) pendiente.

---

## 1. Resumen funcional

El backend calcula el cronograma de actividades mediante CPM y devuelve las
fechas proyectadas sin persistirlas en actividades `PENDIENTE`.

El frontend debe distinguir entre:

- fechas persistidas y confirmadas;
- fechas proyectadas por CPM;
- fechas tentativas;
- actividades críticas;
- actividades con conflicto;
- rangos parciales por falta de fecha de entrega;
- disponibilidad real y disponibilidad provisional.

Las fechas proyectadas son datos de lectura. No deben tratarse como fechas
confirmadas ni enviarse nuevamente al backend como si estuvieran persistidas.

---

## 2. Contrato de `ActividadResponse`

La respuesta de actividad contiene los siguientes grupos de datos:

### Identificación y datos básicos

```json
{
  "idActividad": 55,
  "idProyecto": 100,
  "nombreProyecto": "Desfile Primavera",
  "nombre": "Montaje",
  "descripcion": "Montar escenario",
  "duracionMinutos": 90,
  "estado": "PENDIENTE",
  "estadoCronograma": "NORMAL"
}
```

### Fechas persistidas

```json
{
  "fechaHoraInicio": null,
  "fechaHoraFin": null
}
```

Cuando la actividad es `PENDIENTE`, estos campos pueden ser `null`. Esto no
significa necesariamente que el backend no tenga una proyección.

### Fechas proyectadas

```json
{
  "inicioProyectado": "2026-11-01T09:00:00",
  "finProyectado": "2026-11-01T10:30:00",
  "tentativo": false,
  "parcial": false
}
```

Regla recomendada para mostrar una fecha:

```text
fecha visible = fechaHoraInicio ?? inicioProyectado
```

La misma regla aplica para el fin.

Si `tentativo === true`, la fecha proyectada debe mostrarse visualmente como
estimada o provisional. No debe presentarse como una reserva confirmada.

### Campos CPM

| Campo | Tipo | Uso frontend |
|---|---|---|
| `estadoCronograma` | `string` | Estado global del cálculo: `NORMAL`, `ALERTA_DESBORDE`, `SIN_FECHA_ENTREGA` u otros estados definidos por backend. |
| `critica` | `boolean` / `null` | La actividad tiene holgura de makespan igual a cero. |
| `holguraMakespan` | `number` / `null` | Minutos de holgura respecto del makespan. |
| `holguraEntrega` | `number` / `null` | Minutos de holgura respecto de la entrega. Puede ser `null` sin fecha de entrega. |
| `conflicto` | `boolean` / `null` | Existe conflicto CPM o una holgura negativa. |
| `motivoConflicto` | `string` / `null` | Texto explicativo del conflicto. |
| `parcial` | `boolean` / `null` | El cálculo no pudo usar una fecha de entrega completa. |

`critica` y `conflicto` son conceptos diferentes:

- una actividad puede ser crítica sin estar en conflicto;
- una actividad con conflicto no debe marcarse automáticamente como crítica;
- no usar `holguraEntrega <= 0` como sustituto de `conflicto`.

### Compatibilidad de `tentative`

El DTO conserva un campo histórico llamado `tentative`, de tipo string, y
también expone `tentativo`, de tipo boolean.

Para el frontend se recomienda utilizar:

```text
tentativo
```

y mantener `tentative` únicamente como compatibilidad con respuestas antiguas.

---

## 3. Rangos CPM

Los rangos de actividad se expresan mediante `RangoFactible`:

```json
{
  "inicioMinutos": 0,
  "finMinutos": 120
}
```

Los valores son minutos relativos a `t0`, el origen temporal del proyecto.

### `rangoInicio`

Representa la ventana en la que la actividad puede comenzar:

```text
[ES, LS de entrega]
```

Uso sugerido:

- mostrar el inicio más temprano;
- mostrar el último inicio permitido;
- construir tooltips o información contextual;
- no usarlo como una duración de ejecución.

### `rangoEjecucion`

Representa la ventana de ejecución:

```text
[ES, LF de entrega]
```

Uso sugerido:

- mostrar hasta cuándo puede terminar la actividad;
- representar la ventana completa de ejecución;
- advertir si el rango es parcial o inviable.

### Conversión a fechas absolutas

Para las respuestas HTTP de actividad, el backend ya traduce los rangos CPM a
fechas absolutas cuando corresponde. El frontend no debe recalcular CPM ni
sumar márgenes por su cuenta.

---

## 4. Estados y presentación visual sugerida

### Actividad persistida

Condición:

```text
fechaHoraInicio !== null
```

Mostrar como actividad agendada/confirmada según el estado del proyecto.

### Actividad proyectada

Condición:

```text
fechaHoraInicio === null && inicioProyectado !== null
```

Mostrar con una marca visual de proyección, por ejemplo:

- línea punteada;
- opacidad reducida;
- etiqueta `Proyectada`;
- tooltip indicando que no está persistida.

### Actividad tentativa

Condición:

```text
tentativo === true
```

Mostrar como estimada. Puede cambiar cuando el proyecto confirme su fecha de
inicio.

### Actividad crítica

Condición:

```text
critica === true
```

Mostrar con una indicación de ruta crítica, sin implicar necesariamente un
error.

### Actividad conflictiva

Condición:

```text
conflicto === true
```

Mostrar alerta y, si existe, `motivoConflicto`.

### Actividad parcial

Condición:

```text
parcial === true
```

Mostrar que el cálculo no tiene una ventana de entrega completa. No afirmar
que la actividad está definitivamente confirmada.

---

## 5. Disponibilidad de un perfil para UC-26

En UC-26, cuando un perfil se postula a un requerimiento de actividad, el
backend valida:

1. profesión y aptitud del perfil;
2. estado y cupo del requerimiento;
3. postulación duplicada;
4. rango factible generado por CPM;
5. jornada del perfil;
6. bloqueos manuales;
7. actividades ya asignadas;
8. margen de las actividades existentes;
9. margen de la actividad nueva.

La postulación se crea únicamente si existe un hueco suficiente.

La disponibilidad puede ser provisional cuando el proyecto todavía no tiene
fecha de inicio confirmada. La respuesta de postulación incluye el indicador
`tentativa`.

Importante: una postulación no equivale a una asignación. La aceptación de la
postulación (UC-34) revalida la disponibilidad y crea la `AsignacionActividad`.
Si no hay disponibilidad al aceptar, el backend responde `409` y la postulación
queda `PENDIENTE`. UC-70 volverá a verificar la disponibilidad cuando se asigne
una actividad fuera del flujo de postulación.

### 5.1. Aceptación de postulación de actividad

```http
POST /proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/aceptar
```

Comportamiento del backend:

1. valida que el actor sea director activo con `GESTIONAR_POSTULACIONES`;
2. revalida la disponibilidad del postulante sobre el rango factible;
3. si el postulante ya es miembro `MIEMBRO`, reutiliza su membresía; si no,
   crea una membresía activa;
4. crea la `AsignacionActividad` vinculada al requerimiento;
5. cambia la postulación a `ACEPTADA`;
6. notifica `POSTULACION_ACEPTADA` y, por trigger, `ACTIVIDAD_ASIGNADA`.

Si no hay disponibilidad, el endpoint responde `409` y no crea membresía ni
asignación. Un director no puede ser aceptado como postulante de actividad.

Para el contrato completo de aceptación/rechazo y la UI de gestión, ver
[`ficha-tecnica-gestionar-postulaciones.md`](./aplicadas/ficha-tecnica-gestionar-postulaciones.md).

---

## 6. Contrato de `ResultadoDisponibilidad`

El resultado ampliado tiene esta forma:

```json
{
  "disponible": true,
  "provisional": false,
  "inicioEvaluado": "2026-10-26T09:00:00",
  "finEvaluado": "2026-10-26T18:00:00",
  "inicioSeleccionado": "2026-10-26T11:30:00",
  "finSeleccionado": "2026-10-26T12:30:00",
  "motivo": null,
  "miembros": [
    {
      "idUsuario": 1,
      "inicioEvaluado": "2026-10-26T09:00:00",
      "finEvaluado": "2026-10-26T18:00:00",
      "huecos": [
        {
          "inicio": "2026-10-26T11:30:00",
          "fin": "2026-10-26T12:30:00"
        }
      ],
      "disponible": true,
      "motivo": null
    }
  ]
}
```

### Campos principales

| Campo | Significado |
|---|---|
| `disponible` | Existe un hueco válido para la actividad. |
| `provisional` | La evaluación usa un origen temporal estimado o un proyecto aún no confirmado. |
| `inicioEvaluado`, `finEvaluado` | Rango temporal sobre el que se buscó disponibilidad. |
| `inicioSeleccionado`, `finSeleccionado` | Primer hueco seleccionado. `null` si no existe. |
| `motivo` | Motivo de indisponibilidad. `null` cuando está disponible. |
| `miembros` | Detalle de disponibilidad individual por miembro. |

Los accesores históricos `inicio` y `fin` existen en el backend Java, pero el
frontend debe priorizar los nombres explícitos `inicioEvaluado`,
`finEvaluado`, `inicioSeleccionado` y `finSeleccionado` cuando estén
serializados por la API.

### Motivos previstos

| Motivo | Interpretación |
|---|---|
| `SIN_MIEMBROS` | No se recibió ningún miembro para evaluar. |
| `SIN_JORNADA` | El miembro no tiene jornada configurada dentro del rango. |
| `DURACION_EXCEDE_VENTANA` | La duración supera la ventana laboral disponible. |
| `SIN_HUECO` | El miembro tiene jornada, pero sus bloqueos impiden encontrar un hueco. |
| `SIN_HUECO_COMUN` | Los miembros tienen disponibilidad individual, pero no existe intersección. |
| `CONFLICTO_FIJO` | El CPM detecta conflicto con una actividad fija. |

El frontend debe tratar motivos desconocidos como mensajes de indisponibilidad
genéricos, sin asumir que la lista es cerrada.

---

## 7. Disponibilidad multi-miembro

Para actividades que requieren más de un miembro, la regla es:

```text
disponible = existe un hueco común para todos los miembros
```

No alcanza con que cada miembro tenga disponibilidad en horarios distintos.

El backend aplica el margen configurado a:

- actividades existentes;
- actividad nueva;
- ambos lados del intervalo.

Por eso el frontend no debe agregar márgenes manualmente ni desplazar el
`inicioSeleccionado`.

---

## 8. Fechas y formatos

- `LocalDateTime`: JSON ISO-8601 sin zona, por ejemplo
  `2026-11-01T09:00:00`.
- `LocalTime`: formato `HH:mm`, por ejemplo `09:00`.
- `LocalDate`: formato ISO `yyyy-MM-dd`.
- Los valores `null` deben manejarse explícitamente.
- El frontend no debe interpretar fechas del backend como UTC salvo que el
  contrato de infraestructura agregue una zona explícita.

---

## 9. Superficies actualmente disponibles y pendientes

### Disponible en el backend

- respuestas de actividad con datos CPM;
- proyecciones de inicio y fin;
- rangos CPM;
- consulta interna de disponibilidad individual;
- consulta interna de disponibilidad multi-miembro;
- detalle de huecos y motivos en el servicio;
- postulación UC-26 con validación de disponibilidad;
- aceptación de postulaciones de actividad (UC-34) con revalidación de
  disponibilidad y creación de `AsignacionActividad`;
- notificaciones `POSTULACION_ACEPTADA` y `ACTIVIDAD_ASIGNADA`;
- endpoint HTTP de consulta de disponibilidad:

```http
GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad
GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad?idPerfil=10
GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad?idPerfil=10&idPerfil=11
```

Reglas del endpoint:

- requiere sesión iniciada y un perfil activo seleccionado;
- sin `idPerfil` evalúa el perfil activo del solicitante (previsualización
  de UC-26: el candidato todavía no es miembro del proyecto);
- con `idPerfil` evalúa los perfiles indicados y devuelve `miembros` con el
  detalle de cada uno; con varios perfiles solo hay disponibilidad si existe
  un hueco común;
- consultar la disponibilidad de otros perfiles requiere ser miembro activo
  del proyecto;
- el proyecto debe ser visible para el solicitante (miembro activo, ex
  miembro de proyecto cancelado o proyecto público no borrador/cancelado);
- una actividad cancelada responde `409`;
- es una consulta de solo lectura: no reserva ni asigna la actividad.

La respuesta es el `ResultadoDisponibilidad` descrito en la sección 6. Los
huecos de `miembros[].huecos` se serializan como `{ "inicio": "...", "fin":
"..." }`; no se expone el campo derivado `minutos`.

#### Pestaña Planificación

```http
GET /proyectos/{idProyecto}/actividades
```

- Acceso: `VER_PROYECTO` (miembros activos del proyecto).
- Tabla liviana: datos básicos, fechas persistidas, predecesoras y
  `responsables` (perfiles activos en `asignacion_actividad`).
- No calcula CPM. No incluye `critica`, `holgura`, `inicioProyectado` ni
  `finProyectado`.
- Filtros y paginación en servidor. Orden topológico por dependencias con
  desempate por id de actividad.
- Las actividades canceladas se listan con `estado = "CANCELADA"`.
- La búsqueda de candidatos sigue siendo
  `GET /proyectos/{idProyecto}/actividades/buscar` (visibilidad, sin
  responsables).

Ver [`ficha-tecnica-planificacion-actividades.md`](./aplicadas/ficha-tecnica-planificacion-actividades.md).

#### Pestaña Cronograma

```http
GET /proyectos/{idProyecto}/cronograma
```

- Acceso: `VER_PROYECTO`.
- Devuelve `resumen`, `actividades` para el Gantt y la tabla CPM, y
  `conflictos`.
- Calcula el CPM sobre todas las actividades de la planificación excepto
  `CANCELADA`; las canceladas no aparecen en este endpoint.
- `resumen.rutaCritica` viene ordenado por inicio temprano.
- `resumen.margenMinutos` es la fuente del margen hasta la entrega; no
  recalcularlo desde fechas.
- `actividades[].finConHolgura` es el fin de la barra clara del Gantt
  (`finProyectado + holguraMakespan`). No usar `rangoEjecucion` para esa barra.
- Los tooltips pueden usar `holguraEntrega`, `rangoInicio`, `rangoEjecucion`,
  `conflicto`, `motivoConflicto`, `fijo` y `tentativo`.
- El detalle completo al hacer click en una barra se obtiene con
  `GET /proyectos/{idProyecto}/actividades/{idActividad}` (`VER_PROYECTO`).

Ver [`ficha-tecnica-visualizar-cronograma.md`](./aplicadas/ficha-tecnica-visualizar-cronograma.md).

### Pendiente de exponer o cerrar

- integración del endpoint con el router/servicio frontend;
- revalidación definitiva durante UC-70 para asignaciones fuera del flujo de
  postulación;
- confirmación de proyecto de fase 4 (UC-60).

El frontend no debe duplicar el cálculo de disponibilidad en JavaScript: usa
este endpoint y refresca la consulta después de cambios de proyecto, actividad,
agenda o asignación.

---

## 10. Recomendaciones de implementación frontend

1. Mantener separadas las fechas persistidas y proyectadas.
2. Usar `inicioProyectado`/`finProyectado` solo como fallback visual.
3. Mostrar etiquetas diferentes para `tentativo`, `critica`, `conflicto` y
   `parcial`.
4. Mostrar `motivoConflicto` y `motivo` cuando existan.
5. No calcular CPM, holguras ni márgenes en el frontend.
6. No persistir proyecciones desde la SPA.
7. No convertir una postulación en una asignación localmente: la asignación la
   crea el backend al aceptar la postulación de actividad.
8. Reconsultar disponibilidad después de cualquier cambio de proyecto,
   actividad, agenda o asignación.
9. Considerar que `miembros` puede venir vacío en la consulta individual.
10. Preparar los componentes para que `inicioSeleccionado` y
    `finSeleccionado` sean `null`.
11. Tras aceptar una postulación de actividad, refrescar actividades y
    asignaciones: el backend revalida disponibilidad y puede responder `409`.
12. No ofrecer aceptación de actividad a un director ni a un postulante sin
    disponibilidad; el backend igualmente lo valida.

---
