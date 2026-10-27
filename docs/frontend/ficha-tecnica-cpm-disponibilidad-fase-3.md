# Ficha técnica para frontend

## CPM, proyecciones y disponibilidad — Fases 1 a 3

**Proyecto:** ModaLink  
**Backend:** Spring Boot  
**Fecha de referencia:** 2026-10-27  
**Estado:** implementación backend hasta fase 3, sin confirmación de proyecto (fase 4)

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

Importante: una postulación no equivale a una asignación. La disponibilidad
debe verificarse nuevamente al aceptar o asignar el perfil.

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
- postulación UC-26 con validación de disponibilidad.

### Pendiente de exponer o cerrar

- endpoint HTTP específico:

```text
GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad
```

- integración del endpoint con el router/servicio frontend;
- aceptación de postulaciones y creación de `AsignacionActividad`;
- revalidación definitiva durante UC-70;
- confirmación de proyecto de fase 4;
- contrato final de serialización HTTP para el detalle de disponibilidad.

Hasta que el endpoint específico esté disponible, el frontend no debe inventar
una ruta ni duplicar el cálculo de disponibilidad en JavaScript.

---

## 10. Recomendaciones de implementación frontend

1. Mantener separadas las fechas persistidas y proyectadas.
2. Usar `inicioProyectado`/`finProyectado` solo como fallback visual.
3. Mostrar etiquetas diferentes para `tentativo`, `critica`, `conflicto` y
   `parcial`.
4. Mostrar `motivoConflicto` y `motivo` cuando existan.
5. No calcular CPM, holguras ni márgenes en el frontend.
6. No persistir proyecciones desde la SPA.
7. No convertir una postulación en una asignación localmente.
8. Reconsultar disponibilidad después de cualquier cambio de proyecto,
   actividad, agenda o asignación.
9. Considerar que `miembros` puede venir vacío en la consulta individual.
10. Preparar los componentes para que `inicioSeleccionado` y
    `finSeleccionado` sean `null`.

---
