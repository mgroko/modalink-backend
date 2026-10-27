# Plan de acción v3 — CPM, disponibilidad y confirmación de proyecto

**Fecha:** 2026-10-26  
**Reemplaza:** plan de acción v2 (no versionado en el repositorio). Este documento conserva la referencia a su punto **P2.8** y lo corrige de forma explícita.  
**Alcance:** UC-33 (visualizar cronograma), UC-60 (confirmar proyecto), UC-70 (asignar actividad), y los efectos sobre UC-26, UC-49, UC-50 y UC-51.  
**Estado:** propuesta correctiva. En esta entrega **no se modifica código**; el entregable es este plan.

---

## 0. Resumen ejecutivo

El plan v2 proponía, en su punto **P2.8**, calcular el fin tardío absoluto como:

```text
LF_abs = t0 + finTardio + margen
```

Esa fórmula **no es correcta en general**. El margen contra la entrega no se suma al fin tardío de cada actividad: se incorpora **anclando la segunda pasada del CPM al tiempo disponible hasta la entrega** y propagando hacia atrás el mínimo entre sucesoras, con las actividades fijas inamovibles.

La corrección central de este plan es separar dos cálculos:

1. **Pasada de makespan:** calcula la ruta crítica y las holguras propias del grafo, sin fecha de entrega.
2. **Pasada de entrega:** calcula los inicios tardíos reales respecto de la entrega (`T`), con las actividades fijas ancladas en `LS = ES` y `LF = EF`.

El margen global (`margen = T - makespan`) se informa como indicador, pero **nunca se suma a un `LF`**.

Además de P2.8, este plan corrige otros problemas detectados en el código actual:

- El `t0` se mueve a “hoy” cuando la fecha de inicio ya pasó.
- El recalculo persiste proyecciones en actividades `PENDIENTE`, pisando inicios confirmados.
- Las actividades fijas no están ancladas en la pasada hacia atrás.
- El rango factible `[ES, LF]` es ambiguo y no distingue “ventana de inicio” de “ventana de ejecución”.
- La disponibilidad se verifica para un solo usuario, sin intersección de varios miembros.
- El margen de la actividad nueva no se aplica; el de las actividades existentes sí.
- No existe servicio ni endpoint de confirmación de proyecto (UC-60).
- No existe flujo de aceptación de postulaciones que cree `AsignacionActividad` (UC-34/UC-70), por lo que la confirmación no tiene asignaciones que validar.
- `CrearActividadService`, `ModificarActividadService` y `EliminarActividadService` permiten operar sobre proyectos `CONFIRMADO` sin reprogramar ni validar.
- Los triggers de BD no cubren todos los invariantes y uno de ellos usa el margen hardcodeado `30`.

---

## 1. Corrección de P2.8: por qué `+ margen` está mal

### 1.1. El error conceptual

El margen contra la entrega es una propiedad **global** del proyecto:

```text
margen = T - makespan
```

donde `T` son los minutos entre `t0` y el fin de la fecha de entrega.

Si se suma `margen` al `LF` de una actividad que tiene una sucesora fija, se le está dando a esa actividad un tiempo que en realidad está consumido por la sucesora. El resultado es que la actividad puede empezar después de lo que permite su sucesora.

### 1.2. Contraejemplo con una sucesora fija

Sea:

- `t0 = 2026-11-01 09:00`
- entrega: `2026-11-01 18:00` → `T = 540` minutos
- actividad `A`: duración `240` minutos, libre
- actividad `B`: duración `120` minutos, **fija** porque ya tiene un inicio confirmado, con inicio `2026-11-01 13:00` → `S_B = 240`
- dependencia `A → B`

Pasada hacia adelante:

| Actividad | ES | EF |
|---|---:|---:|
| A | 0 | 240 |
| B (fija) | 240 | 360 |

`makespan = 360`, `margen = 540 - 360 = 180`.

Pasada de makespan hacia atrás, con B anclada:

| Actividad | ES | EF | LS | LF | Holgura |
|---|---:|---:|---:|---:|---:|
| A | 0 | 240 | 0 | 240 | 0 |
| B (fija) | 240 | 360 | 240 | 360 | 0 |

Pasada de entrega hacia atrás, con B anclada:

| Actividad | ES | EF | LS entrega | LF entrega | Holgura entrega |
|---|---:|---:|---:|---:|---:|
| A | 0 | 240 | 0 | 240 | 0 |
| B (fija) | 240 | 360 | 240 | 360 | 0 |

La respuesta correcta es que **A no tiene holgura**: debe empezar en 0 y terminar en 240 para que B pueda empezar a las 13:00.

La fórmula de P2.8 daría:

```text
LF_abs(A) = t0 + 240 + 180 = t0 + 420  → 16:00
LS_abs(A) = 16:00 - 4 h = 12:00
```

Eso permitiría programar A de 12:00 a 16:00, pero B empieza a las 13:00. **Se viola la dependencia A → B.** Además, si se aplicara también a B:

```text
LF_abs(B) = t0 + 360 + 180 = t0 + 540  → 18:00
LS_abs(B) = 16:00
```

se estaría **moviendo una actividad fija** de las 13:00 a las 16:00, algo que el negocio prohíbe.

### 1.3. Cuándo “sumar margen” parece funcionar y por qué no alcanza

En un grafo **sin actividades fijas**, sumar el mismo `margen` a todos los `LF` de la pasada de makespan es equivalente a anclar los terminales en `T`, porque todo el grafo se desplaza en bloque:

```text
LF_entrega(i) = LF_makespan(i) + (T - makespan)
```

Pero esa equivalencia se rompe en cuanto hay una actividad fija, porque su `LF` no puede desplazarse: debe quedar en `EF`. También se rompe si el cálculo se encadena sumando margen en cada nodo en lugar de propagar el mínimo (se duplica el margen), y si `finTardio` ya viene de una pasada anclada a la entrega (se vuelve a sumar).

La formulación correcta no depende de que esa equivalencia se cumpla: **ancla los terminales libres en `T`, ancla las fijas en `EF` y propaga el mínimo**.

### 1.4. El margen positivo no garantiza viabilidad

Otro error posible es usar `margen >= 0` como única condición de viabilidad. Contraejemplo:

- `A` duración `300`, dependencia `A → B`
- `B` fija, inicio `240`, duración `120`
- `T = 540`

`makespan = 360`, `margen = 180` (positivo). Pero `A` termina en `300`, después del inicio fijo de `B` en `240`. El cronograma **no es viable** aunque el margen sea positivo.

Por eso la viabilidad debe verificar, como mínimo:

```text
makespan <= T
y no hay conflicto con actividades fijas
y toda holgura de entrega es >= 0
```

---

## 2. Diagnóstico del código actual

La tabla siguiente resume los hallazgos verificados. Las referencias son al estado del repositorio al 2026-10-26.

| # | Hallazgo | Evidencia | Impacto | Corrección propuesta |
|---|---|---|---|---|
| D-01 | La pasada hacia atrás se inicializa en `makespan`, no en la entrega. | `CronogramaService.java:221-229` | `LS/LF` no representan el plazo real; la holgura es solo respecto del makespan. | Segunda pasada anclada en `T`. |
| D-02 | Las actividades fijas solo se fijan en la pasada hacia adelante; la pasada hacia atrás las mueve. | `CronogramaService.java:180-191` y `217-245` | Una actividad `EN_CURSO` o `FINALIZADA` puede recibir `LS/LF` distintos de su inicio/fin real. | En ambas pasadas: `LS = ES`, `LF = EF` para fijas. |
| D-03 | Hay una sola holgura y `critica = holgura <= 0`. | `CronogramaService.java:232-245`; `NodoCpm.java:20-29` | Se mezcla ruta crítica con holgura de entrega; una holgura negativa se marca como “crítica” en lugar de “conflicto”. | Separar `holguraMakespan`, `holguraEntrega`, `critica` y `conflicto`. |
| D-04 | El rango factible se devuelve como `[ES, LF]` y se usa tanto para iniciar como para ejecutar. | `CronogramaService.java:292-315`; `RangoFactible.java:9-15` | El consumidor no sabe si `LF` es el último inicio o el último fin; se pierde el límite real de inicio (`LS`). | Exponer `inicioMinimo`, `inicioMaximo`, `finMinimo`, `finMaximo` y `ventanaEjecucion`. |
| D-05 | `inicioEfectivo` reemplaza una fecha de inicio pasada por el inicio del día actual. | `CronogramaService.java:323-333` | `t0` deja de ser inmutable; se desplazan todas las actividades fijas y se “replanifica desde hoy” un proyecto ya confirmado. | `t0` inmutable; usar un `release time` separado para no programar en el pasado. |
| D-06 | `recalcular` escribe `fechaHoraInicio` en todas las actividades `PENDIENTE`. | `CronogramaService.java:344-371` | Se persisten proyecciones, se pisan inicios confirmados y se generan bloqueos fantasma en el calendario. | No persistir proyecciones; persistir solo en confirmación/reprogramación explícita. |
| D-07 | `iniciosFijos` solo considera no `PENDIENTE`; una `PENDIENTE` con inicio confirmado no se fija. | `CronogramaService.java:378-395` | En un proyecto confirmado, una actividad pendiente con inicio confirmado se puede reprogramar. | Fijar también `PENDIENTE` con inicio confirmado (con origen confiable). |
| D-08 | Una actividad no `PENDIENTE` sin fecha se fija silenciosamente en `t0`. | `CronogramaService.java:386-393` | Datos inconsistentes se ocultan y se produce un cronograma falso. | Error de datos explícito. |
| D-09 | `evaluar` devuelve `NORMAL` si no hay fecha de entrega. | `CronogramaService.java:95-106` | Un cronograma sin plazo se reporta como sano. | Estado `SIN_FECHA_ENTREGA` (sin lanzar excepción en GET). |
| D-10 | La hora de fin de entrega está hardcodeada en `23:59`. | `CronogramaService.java:64`, `397-402` | No se puede configurar y se desalinea con la configuración del sistema. | Clave `CRONOGRAMA_HORA_FIN_ENTREGA` con default `23:59`. |
| D-11 | La consulta de ocupación no filtra `fechaHoraInicio IS NOT NULL`. | `ActividadRepository.java:26-40` | Actividades sin inicio pueden entrar a la lógica de ocupación según el consumidor. | Agregar el filtro y excluir `CANCELADA`. |
| D-12 | La disponibilidad se verifica para un solo usuario y devuelve solo un booleano. | `CalendarioService.java:162-187`; `DisponibilidadCalculator.java:51-108`; `ResultadoDisponibilidad.java:18-22` | No se puede validar una actividad con varios miembros ni explicar por qué no hay cupo. | Intersección de huecos por miembro, motivo y hueco seleccionado. |
| D-13 | Se extienden las actividades existentes por su margen, pero no se aplica el margen de la actividad nueva. | `CalendarioService.java:370-403` | La separación entre actividades queda subestimada; se aceptan solapamientos con el margen. | Aplicar margen a ambos lados de la actividad nueva y de las existentes. |
| D-14 | `DisponibilidadCalculator` solo responde “hay hueco” y trabaja ventana por ventana. | `DisponibilidadCalculator.java:51-108` | No hay intersección entre miembros ni primer hueco concreto. | Devolver huecos libres e intersectarlos. |
| D-15 | El CPM incluye actividades `CANCELADA`. | `CronogramaService.java:141-207`; `ActividadRepository.java:125-131` | Una actividad cancelada puede alargar el makespan o bloquear sucesoras. | Excluir `CANCELADA` del grafo y de la ocupación. |
| D-16 | Se permite crear actividades en proyectos `CONFIRMADO` sin reprogramar. | `CrearActividadService.java:89-116` | Un proyecto confirmado puede quedar con una actividad sin inicio y fuera del plan. | Rechazar o exigir reprogramación explícita. |
| D-17 | `ModificarActividadService` solo recalcula si `fechaHoraInicio != null` y permite `CONFIRMADO`. | `ModificarActividadService.java:155-157`, `176-183` | Cambios de duración/dependencias en un proyecto confirmado no se validan. | Validar siempre; política de cambios post-confirmación. |
| D-18 | `EliminarActividadService` permite `CONFIRMADO` y recalcula persistiendo proyecciones. | `EliminarActividadService.java:158-165`, `273-287` | Se puede alterar un plan confirmado y pisar inicios. | Rechazar o reprogramar atómicamente. |
| D-19 | No existe servicio/endpoint de confirmación de proyecto. | `ProyectoController.java:62-214` | UC-60 no está implementado; `fechaInicio` no se setea en producción. | Implementar `ConfirmarProyectoService` + endpoint. |
| D-20 | No existe flujo de aceptación que cree asignaciones. | `CrearPostulacionService.java:154-219`; `AsignacionActividadRepository.java` | La confirmación no tiene asignaciones para validar disponibilidad. | Implementar UC-34/UC-70 como prerrequisito. |
| D-21 | `AsignacionActividad` no vincula la asignación con el requerimiento ni tiene unicidad. | `AsignacionActividad.java:9-23`; `ModaLinkBD.sql:132-138` | No se puede saber qué requerimiento satisface cada asignación; se permiten duplicados. | Agregar vínculo con requerimiento y `UNIQUE`. |
| D-22 | El trigger de coherencia de fechas solo escucha cambios en `planificacion`. | `ModaLinkBD.sql:1194-1213` | Cambiar `proyecto.fecha_inicio` después de la entrega no se valida en BD. | Trigger también sobre `proyecto.fecha_inicio`. |
| D-23 | El trigger de bloqueo usa `30` hardcodeado, no filtra estados ni inicios nulos y solo corre en `DELETE`. | `ModaLinkBD.sql:261-297` | Defensa de BD incompleta y desalineada con la configuración. | Corregir trigger y cubrir `INSERT/UPDATE` de bloqueos. |
| D-24 | `EstadoCronograma` solo tiene `NORMAL` y `ALERTA_DESBORDE`; el mapper usa `NORMAL` por defecto. | `EstadoCronograma.java:3-6`; `ActividadMapper.java:63-109` | No se representan datos faltantes, conflictos ni inconsistencias. | Ampliar estados o usar un resultado con detalle. |
| D-25 | Los tests actuales afirman el comportamiento incorrecto. | `CronogramaServiceTest.java:104-115`, `193-222`; `CalendarioDisponibilidadTest.java` | La suite protege el bug (replanificar desde hoy, holgura solo de makespan). | Reescribir los tests con los valores exactos corregidos. |

---

## 3. Modelo formal corregido

### 3.1. Definiciones

Sea:

- `t0`: inicio del proyecto. Es `proyecto.fechaInicio` cuando el proyecto está `CONFIRMADO`. Es inmutable.
- `T`: minutos entre `t0` y el fin de la entrega:
  ```text
  finEntrega = planificacion.fechaEntrega.atTime(horaFinEntrega)
  T = Duration.between(t0, finEntrega).toMinutes()
  ```
- `d_i > 0`: duración de la actividad `i`.
- `Pred(i)`: predecesoras directas de `i`.
- `Succ(i)`: sucesoras directas de `i`.
- `F`: conjunto de actividades fijas.
- `S_i`: inicio fijo de la actividad `i` en minutos desde `t0`.

### 3.2. Qué actividades entran al CPM

| Estado / situación | ¿Entra? | ¿Es fija? | Inicio usado |
|---|---:|---:|---|
| `CANCELADA` | No | — | — |
| `PENDIENTE` en proyecto no confirmado | Sí | No | Proyección calculada; no se persiste |
| `PENDIENTE` en proyecto confirmado con inicio confirmado | Sí | Sí | `fechaHoraInicio` |
| `PENDIENTE` en proyecto confirmado sin inicio | Sí | — | Dato inconsistente: error |
| `EN_CURSO` | Sí | Sí | `fechaHoraInicio` real |
| `FINALIZADA` | Sí | Sí | `fechaHoraInicio` real |

Reglas:

- `CANCELADA` se excluye del grafo y de la ocupación. Si una cancelada era puente entre dos actividades, se debe decidir si se reconectan sus sucesoras con sus predecesoras (como hace hoy la eliminación) o si simplemente se elimina la restricción. Este plan recomienda **reconectar**, para no perder dependencias transitivas.
- Una actividad `PENDIENTE` con inicio persistido en un proyecto **no confirmado** es dato legado de una proyección vieja. No se considera fija: se ignora para el cálculo y se limpia en la migración.
- Una actividad `EN_CURSO` o `FINALIZADA` sin `fechaHoraInicio` es un dato inconsistente: no se puede calcular CPM. Se debe reportar, no inventar `t0`.

La regla de la `PENDIENTE` sin inicio en proyecto confirmado se aplica **fuera** de la transacción de confirmación; dentro de ella se difiere al commit (ver D9). La calificación de fija para una `PENDIENTE` con inicio confirmado aplica a la lectura normal del cronograma; la reprogramación explícita (D12) las trata como movibles.

**Consecuencia:** una vez confirmado el proyecto, todas las `PENDIENTE` tienen inicio y son fijas, por lo que `H^M = H^E = 0`. La holgura solo vuelve a ser informativa en una reprogramación explícita o en una vista tentativa previa a la confirmación. Para la lectura normal, `enRutaCritica` sigue distinguiendo la cadena que determina el makespan.

### 3.3. Pasada hacia adelante

Para cada actividad en orden topológico:

```text
si i ∈ F:
    ES_i = S_i
si i ∉ F:
    ES_i = max(R, max_{p ∈ Pred(i)} EF_p)
EF_i = ES_i + d_i
```

`makespan = max_i EF_i`.

Notas:

- Para una actividad fija, **no se recalcula** el inicio a partir de sus predecesoras. Si una predecesora termina después de `S_i`, hay un conflicto que se reporta (ver 3.6).
- Con `R = 0`, las raíces libres empiezan en `0`. Para no programar trabajo en el pasado se pasa `R >= 0` como cota inferior de las actividades libres, sin cambiar `t0`:
  ```text
  ES_i = max(R, max_{p ∈ Pred(i)} EF_p)
  ```
  `R` se calcula fuera del CPM puro, por ejemplo `R = max(0, minutos(ahora - t0))`. No se debe modificar `t0` para lograr esto.

### 3.4. Pasada hacia atrás de makespan (ruta crítica)

Recorriendo en orden topológico inverso:

```text
si i ∈ F:
    LS_i^M = ES_i
    LF_i^M = EF_i
si i ∉ F:
    LF_i^M = min( { makespan } ∪ { LS_j^M | j ∈ Succ(i) } )
    LS_i^M = LF_i^M - d_i

H_i^M = LS_i^M - ES_i
critica_i = (H_i^M == 0)
```

Esta pasada responde: **¿cuánto puede desplazarse una actividad sin alargar el proyecto, independientemente de la fecha de entrega?** Es la que define la ruta crítica.

#### 3.4.1. Holgura cero en actividades fijas

Por construcción, **toda actividad fija tiene `H^M = 0`**, aunque no determine el makespan:

- `A`: 300 minutos, libre;
- `B`: 60 minutos, fija en `S = 0`, sin dependencias;
- `T = 540`.

`M = 300` lo fija `A`, pero `B` también tiene `H^M = 0` y quedaría marcada como crítica. Es un **falso positivo**: `B` no está en la cadena que determina el makespan, solo es inamovible.

Por eso:

- se mantiene `critica = (H^M == 0)` como indicador de **holgura nula**;
- toda actividad fija se expone con `fijo = true` (no se agrega un campo `inamovible` redundante);
- si la UI necesita la **cadena que conduce al makespan**, se calcula una marca adicional:
  ```text
  enRutaCritica_i = (EF_i == M)
                    o existe j ∈ Succ(i) con enRutaCritica_j y ES_j == EF_i
  ```
  propagada desde los terminales con `EF = M` hacia atrás;
- no se debe usar `critica` de una fija para afirmar que “determina la fecha de entrega”; para eso está `enRutaCritica`.

Esto no cambia las fórmulas de 3.4 ni 3.5; solo agrega un campo de lectura para evitar interpretaciones erróneas.

### 3.5. Pasada hacia atrás de entrega (ventanas reales)

Recorriendo en orden topológico inverso:

```text
si i ∈ F:
    LS_i^E = ES_i
    LF_i^E = EF_i
si i ∉ F:
    LF_i^E = min( { T } ∪ { LS_j^E | j ∈ Succ(i) } )
    LS_i^E = LF_i^E - d_i

H_i^E = LS_i^E - ES_i
```

Esta pasada responde: **¿cuál es el último inicio y el último fin que todavía permiten entregar en fecha?** Es la que se usa para disponibilidad, confirmación y programación.

### 3.6. Viabilidad

El cronograma es viable si y solo si:

```text
makespan <= T
y para toda actividad i: H_i^M >= 0
y para toda actividad i: H_i^E >= 0
y para toda actividad fija i: S_i >= 0 y max_{p ∈ Pred(i)} EF_p <= S_i
```

`detectarConflictosFijos` reporta, como mínimo:

- una fija con `S_i < 0` (`FUERA_DE_PROYECTO`);
- una predecesora con `EF_p > S_i` (`CONFLICTO_FIJO`);
- una fija sin `fechaHoraInicio` (`DATO_INCONSISTENTE`).

El solapamiento de dos fijas del mismo miembro no es un conflicto de precedencias: lo valida el planificador de recursos (D10), que sí recibe las asignaciones.

El **margen global** es:

```text
margen = T - makespan
```

pero `margen >= 0` **no es suficiente** (ver 1.4). `margen` se informa como indicador; no se usa para calcular `LF`.

### 3.7. Ventanas y rangos

Para cada actividad:

```text
inicioMinimo   = ES_i
inicioMaximo   = LS_i^E        (para libres)
inicioMaximo   = S_i           (para fijas)
finMinimo      = EF_i
finMaximo      = LF_i^E        (para libres)
finMaximo      = EF_i          (para fijas)
```

- **Ventana de inicio:** `[inicioMinimo, inicioMaximo]`.
- **Ventana de ejecución:** `[inicioMinimo, finMaximo]`.
- Para una actividad libre, `inicioMaximo + d_i = finMaximo` y `H_i^E = inicioMaximo - inicioMinimo`.
- Para una actividad fija, ambas ventanas son exactas: `[S_i, S_i]` y `[S_i, EF_i]`.

Nunca se debe llamar “rango de inicio” a `[ES, LF]`. Si la API necesita la ventana de ejecución, debe llamarla `ventanaEjecucion`.

### 3.8. Fechas absolutas

```text
fechaInicioMinima = t0 + ES_i
fechaInicioMaxima = t0 + LS_i^E
fechaFinMinima    = t0 + EF_i
fechaFinMaxima    = t0 + LF_i^E
```

Para fijas:

```text
fechaInicioMinima = fechaInicioMaxima = t0 + S_i
fechaFinMaxima    = t0 + EF_i
```

`t0` no se reemplaza por “hoy”. Si el proyecto está confirmado con `t0` en el pasado, las fechas absolutas siguen siendo relativas a ese `t0`; para la **programación** se aplica una cota `no antes de ahora` separada (ver D4).

### 3.9. Seudocódigo del motor

```text
calcularCPM(actividades, fijas, T, R):
    orden = ordenTopologico(actividades)          // falla si hay ciclo
    para i en orden:
        si i es fija:
            ES[i] = S[i]
        sino:
            ES[i] = max(R, max(EF[p] para p en pred(i)))
        EF[i] = ES[i] + d[i]
    M = max(EF[i]) o 0 si no hay actividades

    para i en reversa(orden):
        si i es fija:
            LS_M[i] = ES[i]; LF_M[i] = EF[i]
            LS_E[i] = ES[i]; LF_E[i] = EF[i]
        sino:
            LF_M[i] = min({ M } ∪ { LS_M[j] para j en succ(i) })
            LS_M[i] = LF_M[i] - d[i]
            LF_E[i] = min({ T } ∪ { LS_E[j] para j en succ(i) })
            LS_E[i] = LF_E[i] - d[i]
        H_M[i] = LS_M[i] - ES[i]
        H_E[i] = LS_E[i] - ES[i]

    conflictos = detectarConflictosFijos(actividades, fijas)
    viable = null si T es null
             sino (M <= T) and sinConflictos(conflictos)
                  and todas(H_M >= 0) and todas(H_E >= 0)
    enRutaCritica = marcarCadenaCritica(orden, ES, EF, M)   // ver 3.4.1

    retornar { ES, EF, LS_M, LF_M, H_M,
               LS_E, LF_E, H_E, M, T,
               margen: T - M, viable, conflictos, enRutaCritica }
```

La función es pura: no lee el reloj, no lee la BD y no modifica entidades. El `release time` y el `t0` se pasan como parámetros.

### 3.10. Entrega ausente (`T` nulo)

Un proyecto puede no tener fecha de entrega. En ese caso:

- no se ejecuta la pasada de entrega;
- `T`, `margen`, `LS^E`, `LF^E` y `H^E` son `null` (tipo `Long`, no `long`);
- `viable` es `null`: no se puede afirmar viabilidad sin plazo;
- el estado es `SIN_FECHA_ENTREGA`;
- para lectura se puede usar la ventana de makespan `[ES, LS^M]` / `[ES, LF^M]`, marcada explícitamente como `parcial = true`;
- la confirmación rechaza con `409` hasta que exista entrega.

`calcular` **no debe lanzar excepción** por `T` nulo: devuelve el resultado parcial. La excepción se reserva para ciclos y datos inconsistentes.

---

## 4. Actividades fijas: reglas definitivas

| Situación | ¿Fija? | Regla |
|---|---:|---|
| `EN_CURSO` | Sí | `ES = S`, `EF = S + d`, `LS = ES`, `LF = EF` en ambas pasadas. |
| `FINALIZADA` | Sí | Igual que `EN_CURSO`. Si su fin excede `T`, se reporta `ALERTA_DESBORDE`; la confirmación previa sigue exigiendo `M <= T`, pero una actividad ya ejecutada no se rechaza ni se mueve. |
| `PENDIENTE` en proyecto `CONFIRMADO` con inicio confirmado | Sí | Igual que `EN_CURSO`, pero el inicio es un compromiso de plan, no un hecho real. |
| `PENDIENTE` en proyecto no confirmado con inicio persistido | No | Es una proyección legada; se ignora y se limpia. |
| `PENDIENTE` en proyecto `CONFIRMADO` sin inicio | — | Dato inconsistente: no se puede confirmar ni calcular. |
| `CANCELADA` | No entra | Se excluye del grafo y de la ocupación. |

### 4.1. Problema de procedencia del inicio

`fechaHoraInicio` no alcanza para distinguir:

- un inicio **confirmado** de una actividad pendiente;
- una **proyección** vieja escrita por el recalculo anterior;
- un inicio **real** de una actividad en curso/finalizada.

La solución recomendada es agregar a `actividad` una columna de procedencia, por ejemplo:

```sql
origen_inicio varchar(20) CHECK (origen_inicio IN ('PROYECTADA', 'CONFIRMADA', 'REAL'))
```

o, mínimamente, una columna booleana `inicio_confirmado`. Mientras no exista, la migración debe auditar los datos legados antes de tratar todo inicio no nulo de una actividad `PENDIENTE` como confirmado.

**Decisión de este plan:** solo la confirmación (o una reprogramación explícita) escribe `fechaHoraInicio` en actividades `PENDIENTE`. Las proyecciones viven en DTOs de respuesta.

---

## 5. Decisiones corregidas

### D1. Dos pasadas, no una pasada con margen

- **Pasada de makespan:** ruta crítica y holgura propia del grafo.
- **Pasada de entrega:** ventanas reales contra `T`.
- `critica` se calcula solo con `H^M == 0`.
- `H^E` se usa para disponibilidad, prioridad y confirmación.

### D2. Actividades fijas inamovibles

En ambas pasadas:

```text
LS = ES
LF = EF
```

No se les suma margen. Sus predecesoras reciben el tope de `LS` de la fija.

### D3. El margen no se suma a `LF`

```text
margen = T - makespan
```

Se expone como indicador global. No se usa para calcular `LF`, `LS`, rangos ni fechas.

### D4. `t0` inmutable

- Si el proyecto está `CONFIRMADO`, `t0 = proyecto.fechaInicio`.
- No se reemplaza por hoy aunque esté en el pasado.
- Para no programar trabajo en el pasado se usa un `release time` separado:
  ```text
  R = max(0, minutos(ahora - t0))
  ```
  que solo aplica a actividades libres y a la búsqueda de disponibilidad.
- Si el proyecto no está confirmado, se puede usar un `t0` estimado para mostrar proyecciones, marcado como `tentativo`, pero nunca se persiste. El estimado es `proyecto.fechaInicio` si existe; si no, `ahora` (o la fecha de publicación si el negocio la define). En ambos casos se aplica el mismo `R` sobre las actividades libres.

### D5. Ventana de inicio vs. ventana de ejecución

- Inicio: `[ES, LS^E]`.
- Ejecución: `[ES, LF^E]`.
- La API debe nombrarlas explícitamente. No usar `[ES, LF]` como si fuera un rango de inicio.

### D6. No persistir proyecciones

- `recalcular` deja de escribir `fechaHoraInicio` en `PENDIENTE`.
- `calcular`, `proyectar` y `evaluar` son de solo lectura.
- Solo se persiste:
  - `proyecto.fechaInicio` y `proyecto.estado = CONFIRMADO` en la confirmación;
  - `actividad.fechaHoraInicio` de las actividades colocadas en la confirmación o en una reprogramación explícita.
- Las respuestas incluyen `inicioProyectado`, `finProyectado`, `tentativo`, `rangoInicio` y `rangoEjecucion`.

### D7. Excluir `CANCELADA`

- No entra al CPM.
- No bloquea calendario.
- Si era puente de dependencias, se reconectan sus sucesoras con sus predecesoras al cancelar (misma política que la eliminación).

### D8. Disponibilidad multi-miembro y margen simétrico

- Una actividad con varios miembros requiere un **hueco común** para todos.
- Cada miembro tiene su propia agenda y su propio margen.
- La actividad nueva reserva `[s - m, s + d + m]`.
- Las actividades existentes reservan `[e - m, e + d + m]`.
- La separación resultante entre dos actividades es `2m`, coherente con “margen a cada lado”.
- Los bloqueos manuales no llevan margen, pero la reserva de la actividad nueva no puede solaparlos.

### D9. Confirmación atómica

Toda la confirmación ocurre en **una transacción**:

1. bloquear proyecto/planificación;
2. validar estado y datos;
3. setear `estado = CONFIRMADO` y `fechaInicio`, hacer flush;
4. calcular CPM y colocar actividades;
5. persistir inicios;
6. si algo falla, rollback completo (el proyecto vuelve a `PUBLICADO`).

El orden “estado primero” permite que los triggers y las validaciones de disponibilidad vean el proyecto ya confirmado.

**Precaución:** dentro de esa transacción, después de setear `CONFIRMADO`, las actividades `PENDIENTE` todavía no tienen inicio. La regla “`PENDIENTE` en confirmado sin inicio es inconsistente” no debe aplicarse hasta terminar la colocación. Para eso:

- se toma una **foto** del grafo y de las actividades fijas antes de cambiar el estado;
- el cálculo y la colocación usan esa foto;
- la exigencia de que todas las pendientes tengan inicio se difiere al commit mediante un constraint trigger diferido (o una validación explícita al final de la transacción).

### D10. Programación con calendario

El CPM es una cota de precedencias en minutos. No garantiza que la actividad quepa en la jornada laboral. La confirmación debe ejecutar un **planificador de recursos**:

- recorrer en orden topológico y, entre las actividades listas (predecesoras ya colocadas), elegir primero la de menor `H^E`;
- para cada actividad libre, buscar el primer hueco común `>= max(t0 + ES_i, fin real de predecesoras, ahora)`;
- respetar `inicioMaximo` de entrega;
- para fijas, usar su inicio real y validar que no se solapen entre sí para un mismo miembro (el CPM solo modela precedencias, no recursos);
- si no hay hueco, fallar con detalle y rollback.

El planificador es greedy con prioridad por menor `H^E` (menos holgura de entrega). No se promete optimalidad; se promete detectar infactibilidad y no persistir un plan inválido.

El `margen` de D8 tampoco está modelado en el CPM. Por eso `viable` es una **cota inferior**: si el planificador no encuentra hueco por márgenes, jornada o recursos, la confirmación falla aunque `M <= T`. No se debe presentar `margen` como garantía de que el plan cabe. Si el negocio quiere que el CPM refleje los márgenes, se reemplaza la duración `d_i` por `d_i + 2m` en las aristas (o se agrega un desfase), decisión que se documenta aparte.

### D11. Estados de cronograma explícitos

`EstadoCronograma` debe poder representar:

- `NORMAL`;
- `ALERTA_DESBORDE`;
- `SIN_FECHA_ENTREGA`;
- `DATOS_INCONSISTENTES`;
- `CONFLICTO_FIJO`;
- `NO_VIABLE`.

`evaluar` no debe lanzar excepción en un GET por falta de entrega: devuelve `SIN_FECHA_ENTREGA`. La confirmación sí debe rechazar con `409` y detalle.

### D12. Política post-confirmación

En un proyecto `CONFIRMADO`:

- crear una actividad: rechazar con `409` (`PROYECTO_CONFIRMADO`);
- eliminar una actividad: rechazar con `409`;
- modificar duración, dependencias o `fechaHoraInicio`: rechazar con `409`;
- permitir las transiciones de ejecución `PENDIENTE → EN_CURSO → FINALIZADA`: son el avance real del trabajo. Al pasar a `EN_CURSO` se registra el inicio real y la actividad pasa a fija; si el avance real desborda la entrega, se recalcula y se emite `ALERTA_DESBORDE`, **no** se rechaza ni se revierte lo ya ejecutado;
- modificar solo campos descriptivos: permitir;
- si el negocio necesita cambios estructurales, ofrecer un endpoint explícito de **reprogramación** que reejecute CPM + planificador, preserve fijas, actualice pendientes en una transacción y notifique. En esa reprogramación, las `PENDIENTE` con inicio confirmado se tratan como movibles (son compromisos de plan, no hechos); las `EN_CURSO`/`FINALIZADA` siguen fijas.

Una actividad `EN_CURSO` o `FINALIZADA` no puede cambiar de duración, dependencias ni inicio.

### D13. Aceptación/asignación como prerrequisito

UC-60 no se puede implementar correctamente sin resolver antes:

- UC-34: aceptar una postulación debe incorporar al miembro y, si es de actividad, crear la `AsignacionActividad`;
- UC-70: asignar una actividad a un miembro debe validar disponibilidad y crear la asignación.

Además, `AsignacionActividad` debería vincularse al requerimiento que satisface (o existir una tabla equivalente) y tener unicidad `(id_miembro, id_actividad)`.

Sin esto, la confirmación no sabe qué miembros validar ni si los requerimientos están completos.

### D14. Reglas de BD

- `CHECK (estado <> 'CONFIRMADO' OR fecha_inicio IS NOT NULL)` en `proyecto` (agregar como `NOT VALID` y validar después de la migración).
- Trigger sobre `proyecto.fecha_inicio` para comparar con `planificacion.fecha_entrega`.
- Trigger diferido al confirmar: toda actividad `PENDIENTE` de un proyecto `CONFIRMADO` debe tener `fecha_hora_inicio` no nula al commit.
- Corregir `fn_validar_bloqueo_no_actividad`:
  - margen desde `configuracion_sistema` si la agenda no tiene valor;
  - filtrar `act.estado IN ('PENDIENTE','EN_CURSO')`;
  - filtrar `act.fecha_hora_inicio IS NOT NULL`;
  - considerar solo actividades de proyectos `CONFIRMADO`, para no bloquear por proyecciones;
  - aplicar en `INSERT` y `UPDATE` de `bloqueo_agenda`, no solo en `DELETE`.
- `UNIQUE (id_miembro, id_actividad)` en `asignacion_actividad`.

### D15. Configuración de hora de entrega

Agregar la clave:

```text
CRONOGRAMA_HORA_FIN_ENTREGA = 23:59
```

Usarla en `CronogramaService` y en la proyección absoluta. Si se prefiere evitar el minuto perdido, usar `fechaEntrega.plusDays(1).atStartOfDay()` como fin exclusivo y documentarlo; no mezclar ambas convenciones.

---

## 6. Diseño de API y DTOs

### 6.1. `NodoCpm`

Reemplazar el registro actual por uno con dos holguras y conflicto explícito:

| Campo | Tipo | Significado |
|---|---|---|
| `idActividad` | `Long` | ID |
| `duracionMinutos` | `int` | Duración |
| `fijo` | `boolean` | Actividad fija |
| `inicioFijo` | `Long` | `S_i` si es fija |
| `inicioTemprano` | `long` | `ES` |
| `finTemprano` | `long` | `EF` |
| `inicioTardioMakespan` | `long` | `LS^M` |
| `finTardioMakespan` | `long` | `LF^M` |
| `holguraMakespan` | `long` | `LS^M - ES` |
| `critica` | `boolean` | `holguraMakespan == 0` |
| `inicioTardioEntrega` | `Long` | `LS^E` (null sin entrega) |
| `finTardioEntrega` | `Long` | `LF^E` (null sin entrega) |
| `holguraEntrega` | `Long` | `LS^E - ES` (null sin entrega) |
| `conflicto` | `boolean` | Holgura negativa o choque con fija |
| `motivoConflicto` | `String` | Detalle |
| `enRutaCritica` | `boolean` | Pertenece a la cadena que determina el makespan (3.4.1) |

Métodos:

- `ventanaInicio()` → `[ES, LS^E]` (o `[S, S]` si es fija).
- `ventanaEjecucion()` → `[ES, LF^E]` (o `[S, EF]` si es fija).
- Si no hay entrega, ambos métodos usan la pasada de makespan (`LS^M`, `LF^M`) y la respuesta se marca `parcial = true`.
- `rangoFactible()` queda deprecado o se redefine para devolver la ventana de inicio.

### 6.2. `CalculoCpm`

| Campo | Tipo | Significado |
|---|---|---|
| `nodos` | `Map<Long, NodoCpm>` | Resultado |
| `makespan` | `long` | `M` |
| `minutosHastaEntrega` | `Long` | `T` (nullable si no hay entrega) |
| `margen` | `Long` | `T - M` (nullable) |
| `viable` | `Boolean` | Resultado de 3.6; `null` si no hay entrega |
| `parcial` | `boolean` | `true` si `T` es null y solo se calculó makespan |
| `conflictos` | `List<ConflictoCpm>` | Detalle |

`ConflictoCpm` contiene `idActividad`, `tipo` (`FUERA_DE_PROYECTO`, `CONFLICTO_FIJO`, `DATO_INCONSISTENTE`) y `detalle`.

`viable` no debe depender solo de `esViable(T)`.

### 6.3. `RangoFactible` y `RangoFactibleAbsoluto`

| Campo | Significado |
|---|---|
| `inicioMinimo` / `fechaInicioMinima` | `ES` |
| `inicioMaximo` / `fechaInicioMaxima` | `LS^E` |
| `finMinimo` / `fechaFinMinima` | `EF` |
| `finMaximo` / `fechaFinMaxima` | `LF^E` |
| `fijo` | Si la actividad ya está agendada |
| `tentativo` | Si `t0` es estimado |
| `parcial` | Si se usó la pasada de makespan por falta de entrega |
| `motivoNoFactible` | Si no hay ventana |

No usar `fin` para referirse a `LF` sin aclarar que es fin de ejecución. Sin entrega, `inicioMaximo` y `finMaximo` usan la pasada de makespan y la respuesta se marca `parcial = true`.

### 6.4. `ResultadoDisponibilidad`

| Campo | Significado |
|---|---|
| `disponible` | Hay hueco común |
| `provisional` | `t0` estimado o proyecto no confirmado |
| `inicioEvaluado`, `finEvaluado` | Ventana de ejecución evaluada |
| `inicioSeleccionado`, `finSeleccionado` | Primer hueco común, si existe |
| `motivo` | `SIN_JORNADA`, `SIN_HUECO`, `SIN_HUECO_COMUN`, `DURACION_EXCEDE_VENTANA`, `SIN_MIEMBROS`, `CONFLICTO_FIJO`, etc. |
| `miembros` | Detalle por miembro: ventana, huecos, motivo |

### 6.5. Endpoints

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/proyectos/{id}/cronograma` | UC-33: nodos con ambas holguras, rangos, `margen`, estado. |
| `POST` | `/proyectos/{id}/confirmar` | UC-60: sin body de fecha; el backend registra el instante de confirmación mediante su `Clock`, convertido a `LocalDateTime` con la zona configurada, en una transacción atómica. |
| `POST` | `/proyectos/{id}/reprogramar` | Cambios post-confirmación, si se habilita. |
| `GET` | `/proyectos/{id}/actividades/{idActividad}/disponibilidad` | Consulta de disponibilidad con detalle. |
| `POST` | `/proyectos/{id}/actividades/{idActividad}/asignaciones` | UC-70. |

`POST /confirmar` responde `200` con el proyecto y el resumen del cronograma, o `409` con:

```json
{
  "codigo": "CRONOGRAMA_NO_VIABLE",
  "mensaje": "La actividad A no puede comenzar antes de ...",
  "conflictos": [
    { "idActividad": 1, "tipo": "CONFLICTO_FIJO", "detalle": "..." }
  ]
}
```

Los conflictos de planificación (`SOLAPAMIENTO_FIJO`, `SIN_HUECO_COMUN`, etc.) usan el mismo sobre de error, pero no forman parte de `ConflictoCpm`.

---

## 7. Fases de implementación

> Ninguna fase de este plan se ejecuta en esta entrega. El orden es por dependencia y cada fase debe dejar el proyecto compilable y con tests verdes.

### Fase 0 — Prerrequisitos de negocio y datos

**Objetivo:** resolver las dependencias que hoy impiden confirmar.

Tareas:

1. Definir el contrato de UC-60:
   - el endpoint no recibe `fechaInicio`;
   - `fechaInicio` se obtiene exclusivamente del `Clock` del backend en el instante de confirmación;
   - el valor se fija una sola vez y queda inmutable;
   - ¿Qué significa “requerimiento completo” para requerimientos generales y de actividad?
2. Definir el vínculo asignación–requerimiento:
   - agregar `id_requerimiento_act` a `asignacion_actividad`, o
   - aceptar que la completitud se valida por postulaciones aceptadas y que la asignación es solo miembro-actividad.
3. Implementar UC-34 (aceptación) y UC-70 (asignación) o, como mínimo, el servicio que cree asignaciones desde postulaciones aceptadas.
4. Auditar datos legados:
   - proyectos `CONFIRMADO` sin `fechaInicio`;
   - actividades `PENDIENTE` con inicio en proyectos no confirmados;
   - actividades `PENDIENTE` sin inicio en proyectos confirmados;
   - asignaciones duplicadas.
5. Agregar clave `CRONOGRAMA_HORA_FIN_ENTREGA`.

**Criterio de salida:** existe una decisión escrita para cada punto y un script de auditoría que reporta los casos legados.

### Fase 1 — Motor CPM puro de dos pasadas

**Archivos principales:**

- `CronogramaService.java`
- `NodoCpm.java`
- `CalculoCpm.java`
- `RangoFactible.java`
- nuevo `ConflictoCpm.java`

Tareas:

1. Implementar la pasada de makespan y la de entrega con las fórmulas de la sección 3.
2. Anclar fijas en ambas pasadas.
3. Separar `critica` de `conflicto`.
4. Excluir `CANCELADA`.
5. Recibir `t0`, `T`, `fijas` y `releaseTime` como parámetros; no leer reloj ni BD.
6. Reportar conflictos con actividad y motivo.
7. Mantener `calcular(List)` como fachada para tests existentes, pero con `T` opcional.

**Tests:** ver sección 8.1.

**Criterio de salida:** todos los valores exactos de `ES/EF/LS/LF` y ambas holguras están cubiertos; el contraejemplo de P2.8 falla si se reintroduce `+margen`.

### Fase 2 — Proyecciones, rangos y lectura sin persistencia

**Archivos principales:**

- `CronogramaService.java`
- `ProyeccionCpm.java`
- `RangoFactibleAbsoluto.java`
- `ActividadResponse.java`
- `ActividadMapper.java`
- `ObtenerActividadService.java`
- `ModificarActividadService.java`
- `EliminarActividadService.java`
- `CrearActividadService.java`

Tareas:

1. Eliminar la escritura de `PENDIENTE` en `recalcular`; reemplazar por cálculo de solo lectura.
2. `inicioEfectivo`:
   - confirmado: `fechaInicio` tal cual;
   - no confirmado: `t0` estimado, marcado `tentativo`;
   - confirmado sin `fechaInicio`: error de datos.
3. Exponer en la respuesta:
   - `fechaHoraInicio` persistida (puede ser null);
   - `inicioProyectado`, `finProyectado`;
   - `rangoInicio`, `rangoEjecucion`;
   - `tentativo`, `critica`, `holguraMakespan`, `holguraEntrega`.
4. `evaluar` devuelve `SIN_FECHA_ENTREGA` si falta plazo; no lanza.
5. `ModificarActividadService` evalúa siempre, no solo si hay inicio.
6. `EliminarActividadService` no persiste proyecciones.
7. `CrearActividadService` devuelve proyección y estado, sin persistir inicio.

**Tests:** ver sección 8.2.

**Criterio de salida:** ninguna operación de creación/modificación/eliminación escribe `fechaHoraInicio` de una actividad `PENDIENTE` fuera de confirmación/reprogramación.

### Fase 3 — Disponibilidad con huecos, márgenes e intersección

**Archivos principales:**

- `CalendarioService.java`
- `DisponibilidadCalculator.java`
- `ResultadoDisponibilidad.java`
- `ActividadRepository.java`
- `AsignacionActividadRepository.java`
- `BloqueoAgendaRepository.java`

Tareas:

1. Agregar `fechaHoraInicio IS NOT NULL` a la consulta de ocupación.
2. Excluir `CANCELADA` y `FINALIZADA` de la ocupación futura.
3. Implementar `huecosLibres(ventanas, bloqueos, margen)` y `primerHueco`.
4. Implementar intersección de huecos para varios miembros.
5. Aplicar margen a la actividad nueva y a las existentes.
6. Devolver motivo y hueco seleccionado.
7. Para disponibilidad provisional, usar `[max(ahora, t0 + ES), t0 + LF^E]` y marcar `provisional`; si no hay entrega, usar `t0 + LF^M` y marcar además `parcial`.
8. No usar `[ahora, entrega]` como sustituto del CPM cuando el proyecto ya está confirmado.

**Tests:** ver sección 8.3.

**Criterio de salida:** una actividad con dos miembros solo se considera disponible si existe un hueco común; el margen de la actividad nueva se respeta; los motivos permiten explicar el rechazo.

### Fase 4 — Confirmación de proyecto (UC-60)

**Archivos principales:**

- nuevo `ConfirmarProyectoService.java`
- `ProyectoController.java`
- `ProyectoRepository.java` / `PlanificacionRepository.java` (lock)
- `CronogramaService.java`
- `CalendarioService.java`
- `AsignacionActividadRepository.java`

Tareas:

1. Endpoint `POST /proyectos/{id}/confirmar`.
2. Lock pesimista de proyecto/planificación.
3. Validaciones:
   - proyecto `PUBLICADO`;
   - `fechaEntrega` presente y coherente con el instante capturado por el `Clock`;
   - al menos una actividad;
   - requerimientos completos (según Fase 0);
   - asignaciones existentes para cada actividad;
   - actividades `PENDIENTE` del proyecto con `fechaHoraInicio = null` antes de la colocación (las proyecciones legadas se limpian o se excluyen de la ocupación);
   - actividades ya iniciadas (`EN_CURSO`/`FINALIZADA`) con `fechaHoraInicio` anterior al instante de confirmación: rechazar con `409` (o habilitar explícitamente una `fechaInicio` retroactiva);
   - sin ciclos;
   - CPM viable;
   - fijas sin solapamiento para un mismo miembro;
   - disponibilidad común para todas las asignaciones.
4. Capturar `fechaInicio` del `Clock` dentro del servicio (convertido a `LocalDateTime` con la zona configurada), tomar una foto del grafo/fijas y setear `estado = CONFIRMADO` y `fechaInicio`, flush.
5. Ejecutar el planificador de recursos sobre la foto y persistir inicios. La validación de “pendiente sin inicio” se difiere al commit.
6. Verificar que el fin real colocado (no solo el `M` del CPM) no supere el fin de entrega.
7. Rollback ante cualquier fallo.
8. Notificaciones after-commit.
9. Idempotencia: si ya está `CONFIRMADO`, `409` (o `200` solo si se define explícitamente).

**Tests:** ver sección 8.4.

**Criterio de salida:** un fallo en cualquier actividad deja el proyecto en `PUBLICADO`, sin inicios nuevos; un éxito persiste `fechaInicio`, `estado` y todos los inicios en una sola transacción.

### Fase 5 — Ciclo de vida post-confirmación y BD

**Archivos principales:**

- `CrearActividadService.java`
- `ModificarActividadService.java`
- `EliminarActividadService.java`
- `CancelarProyectoService.java`
- `docs/3_diseño/ModaLinkBD.sql`
- migración nueva

Tareas:

1. Aplicar D12: rechazar cambios estructurales en `CONFIRMADO`.
2. Implementar `reprogramar` si el negocio lo requiere.
3. Al cancelar, reconectar sucesoras o documentar la semántica.
4. Triggers y constraints de D14.
5. Migración de datos legados.
6. Actualizar `EstadoCronograma` y el mapper.

**Tests:** ver sección 8.5.

**Criterio de salida:** no se puede crear/eliminar/modificar estructuralmente un proyecto confirmado sin pasar por reprogramación; la BD defiende los mismos invariantes.

### Fase 6 — UC-33, documentación y cierre

Tareas:

1. Endpoint `GET /proyectos/{id}/cronograma`.
2. Documentar el contrato de respuesta (ambas holguras, rangos, tentativo).
3. Actualizar `docs/2_analisis/contratos.md`, `dss.md`, `docs/5_estado_sistema/estado_uc.md`.
4. Actualizar fichas de frontend que hoy muestran `[ES, LF]`.
5. Ejecutar la suite completa y la matriz de aceptación.

---

## 8. Plan de pruebas

### 8.1. CPM puro

Valores en minutos, `t0 = 0`.

| Caso | Grafo | `T` | Resultado esperado |
|---|---|---:|---|
| Cadena simple | A(60) → B(120) → C(60) | 300 | `M=240`; `ES/EF`: A 0/60, B 60/180, C 180/240; `LS^M`: A 0, B 60, C 180; `H^M` todos 0; `LS^E`: A 60, B 120, C 240; `H^E` todos 60; `margen=60`. |
| Paralelas con unión | A(60), B(120) → C(60) | 300 | `M=180`; A `H^M=60`, B y C `H^M=0`; `H^E`: A 180, B 120, C 120; `critica`: B y C. |
| Sucesora fija | A(240) → B(120, fija `S=240`) | 540 | `M=360`; A `LS^M=0`, `LF^M=240`, `H^M=0`; B `LS=240`, `LF=360`; A `H^E=0`; `margen=180`; **no** `LS(A)=180`. |
| Fija en el medio | A(60) → B(120, fija `S=120`) → C(60) | 360 | `M=300`; A `H^M=60`; B fija; C `H^M=0`; A `H^E=60`; C `H^E=60`. |
| Conflicto con fija | A(300) → B(120, fija `S=240`) | 540 | `M=360`, `margen=180`, pero `A.H^M=-60`, `A.H^E=-60`, `conflicto=true`, `viable=false`. |
| Entrega insuficiente | A(300) | 240 | `M=300`, `margen=-60`, `A.H^E=-60`, `viable=false`. |
| Fija paralela | A(240) libre, B(120, fija `S=240`) | 540 | `M=360`; A `H^M=120`, `H^E=300`; B `H=0`; B no se mueve. |
| Fija no vinculante | A(300) libre, B(60, fija `S=0`) | 540 | `M=300`; A y B `H^M=0`, ambos `critica=true`; `enRutaCritica` solo A; B `fijo=true`. |
| Fija después de la entrega | A(60, fija `S=600`) | 540 | `M=660`, `margen=-120`, `viable=false`. |
| Sin entrega | A(60) → B(120) | `null` | `M=180`; `LS^E`/`LF^E`/`H^E` null; `viable=null`; `parcial=true`. |
| Fija fuera del proyecto | A(120, fija `S=-60`) | 540 | `conflicto=true`, `viable=false`. |
| Todas fijas | A(60, `S=0`), B(120, `S=60`) | 300 | `M=180`; `H=0`; `margen=120`; no hay nodos libres. |
| Vacío | — | 300 | `M=0`, `margen=300`, `viable=true`, sin nodos. |
| Ciclo | A → B → A | 300 | `CronogramaInviableException`. |
| Cancelada excluida | A cancelada(600) → B(60) | 300 | El grafo solo tiene B; `M=60`; A no influye. |

Además:

- Test explícito de regresión de P2.8: con el caso “sucesora fija”, afirmar que `LF^E(A) == 240` y que **no** es `240 + 180`.
- Test de que `critica` usa solo `H^M`: con `T > M`, una actividad crítica de makespan tiene `H^M=0` y `H^E=margen > 0`; debe seguir siendo `critica=true`.
- Test de que `enRutaCritica` distingue una fija no vinculante (caso “fija no vinculante”).
- Test de entrega nula: `T=null` no lanza, devuelve resultado parcial y `viable=null`.
- Test de que `margen` no se usa para calcular `LF`: cambiar `T` no debe alterar `H^M` ni `critica`; solo `H^E` y `margen`.
- Test de pureza: `calcular` no modifica entidades y no depende del `Clock`.
- Test de `releaseTime`: con `R>0`, las raíces libres empiezan en `R`, pero `t0` no cambia.

### 8.2. Proyecciones y persistencia

| Caso | Acción | Esperado |
|---|---|---|
| Proyecto confirmado con `t0` pasado | `evaluar`/`proyectar` | Usa `t0` original; no lo mueve a hoy. |
| Proyecto no confirmado | `proyectar` | `tentativo=true`; no escribe `fechaHoraInicio`. |
| `recalcular` sobre pendientes | Llamada | No modifica `fechaHoraInicio`. |
| `evaluar` sin entrega | Llamada | `SIN_FECHA_ENTREGA`, sin excepción; `viable=null`. |
| Proyecto confirmado sin `fechaInicio` | `evaluar` | `DATOS_INCONSISTENTES` o error controlado, no “hoy”. |
| Crear actividad en `PUBLICADO` | `POST` | Devuelve proyección; `fechaHoraInicio` persistida sigue null. |
| Modificar actividad en `PUBLICADO` | `PUT` | No persiste proyecciones; estado recalculado. |
| Eliminar actividad en `PUBLICADO` | `DELETE` | No persiste proyecciones de las restantes. |
| Crear/modificar/eliminar en `CONFIRMADO` | `POST/PUT/DELETE` | `409` según D12. |

### 8.3. Disponibilidad

| Caso | Configuración | Esperado |
|---|---|---|
| Hueco exacto | Jornada 09-18, actividad 120, rango 09-11 | Disponible; hueco 09-11. |
| Bloqueo manual parte la ventana | Bloqueo 09-10, actividad 120 | No disponible si no hay otro hueco. |
| Jornada partida | 09-12 y 14-18, actividad 120 | Disponible en 14-16 si el rango lo permite. |
| Actividad mayor que la ventana | Jornada 09-12, actividad 240 | No disponible (`DURACION_EXCEDE_VENTANA`). |
| Sin jornada | Sin filas | No disponible (`SIN_JORNADA`). |
| Margen de actividad existente | Existente 10:00-10:30, margen 30, nueva 60, jornada 09-12 | No disponible: la existente reserva 09:30-11:00 y cualquier inicio de 60 min choca con esa reserva al aplicar también el margen de la nueva. |
| Margen simétrico insuficiente | Existente 10:00-10:30, nueva 11:00-11:30, margen 30, jornada 09-13 | No disponible: la separación es 30 min, pero cada actividad reserva 30 min por lado (se requieren 60 min entre fines e inicios). |
| Margen simétrico suficiente | Existente 10:00-10:30, nueva 11:30-12:30, margen 30, jornada 09-13 | Disponible: separación de 60 min = `2m`. |
| Dos miembros con hueco común | Miembro 1 09-12, miembro 2 10-13, actividad 60 | Disponible en 10-11. |
| Dos miembros sin intersección | Miembro 1 09-12, miembro 2 14-17, actividad 60 | No disponible (`SIN_HUECO_COMUN`). |
| Dos miembros, uno sin jornada | Miembro 2 sin filas | No disponible; motivo del miembro 2. |
| Actividad cancelada | Cancelada con inicio | No bloquea. |
| Actividad sin inicio | `fechaHoraInicio=null` | No bloquea. |
| Rango provisional | Proyecto no confirmado | `provisional=true`; usa `[max(ahora, ...), entrega]`; no persiste. |
| Proyecto confirmado | `t0` en pasado | No usa `[ahora, entrega]`; usa `[ES, LF^E]` con `releaseTime`. |

> La tabla de “margen simétrico” debe fijarse con la definición exacta de reserva adoptada. Si cada actividad reserva `m` a cada lado, la separación mínima entre dos actividades es `2m`. Si el negocio define `m` como separación total, ajustar la fórmula y el nombre de la configuración.

### 8.4. Confirmación (integración con PostgreSQL)

| Caso | Esperado |
|---|---|
| Camino feliz | `200`; proyecto `CONFIRMADO`; `fechaInicio` = instante de confirmación; todas las `PENDIENTE` con inicio; una sola transacción. |
| Sin fecha de entrega | `409`; proyecto sigue `PUBLICADO`; sin inicios. |
| Sin actividades | `409`; rollback. |
| Requerimiento incompleto | `409`; rollback. |
| Sin asignaciones | `409`; rollback. |
| Actividad ya iniciada antes del instante de confirmación | `409`; rollback (o requiere fecha retroactiva explícita). |
| Conflicto con fija | `409` con `CONFLICTO_FIJO`; rollback. |
| Fijas del mismo miembro solapadas | `409` con `SOLAPAMIENTO_FIJO`; rollback. |
| `T < M` | `409` con `margen` negativo; rollback. |
| Sin hueco común | `409` con miembro y motivo; rollback. |
| Falla al persistir una actividad | Rollback total; ninguna actividad queda con inicio nuevo. |
| Confirmación concurrente | Una transacción gana; la otra recibe `409`/espera y ve `CONFIRMADO`; sin doble plan. |
| Trigger de BD | Al persistir inicios, el proyecto ya está `CONFIRMADO`; los triggers ven el estado correcto. |
| Notificación falla | El plan confirmado no se revierte; la notificación se reintenta o se registra. |

### 8.5. Post-confirmación y BD

- Crear actividad en `CONFIRMADO` → `409`.
- Eliminar actividad en `CONFIRMADO` → `409`.
- Modificar duración/dependencias en `CONFIRMADO` → `409`.
- Modificar descripción en `CONFIRMADO` → `200`.
- `PENDIENTE → EN_CURSO` en `CONFIRMADO` → `200`; registra inicio real y la actividad pasa a fija.
- `EN_CURSO → FINALIZADA` en `CONFIRMADO` → `200`; no revierte el plan.
- Avance real que desborda `T` → `200` con `ALERTA_DESBORDE`, no `409`.
- `EN_CURSO`/`FINALIZADA` con cambio de duración → `409`.
- Trigger de `proyecto.fecha_inicio` contra entrega → error de BD.
- `CHECK` de proyecto confirmado sin fecha → error de BD.
- Trigger diferido de actividades pendientes sin inicio → error al commit.
- `UNIQUE` de asignación duplicada → error de BD.
- Trigger de bloqueo con actividad cancelada → permite.
- Trigger de bloqueo con actividad con inicio null → permite.
- Trigger de bloqueo con proyección de proyecto no confirmado → permite.

### 8.6. Invariantes automáticos

Agregar tests de propiedad (o aserciones parametrizadas) para todo grafo generado:

```text
EF_i = ES_i + d_i
LF_i^M >= EF_i  (salvo conflicto)
LF_i^E >= EF_i  (salvo conflicto)
H_i^M = LS_i^M - ES_i
H_i^E = LS_i^E - ES_i
si i es fija: LS_i^M = ES_i y LF_i^M = EF_i
si i es fija: LS_i^E = ES_i y LF_i^E = EF_i
si i es libre y terminal: LF_i^E = T
si i es libre: LF_i^E = min(T, min(LS_j^E))
margen = T - M
critica_i = (H_i^M == 0)
enRutaCritica_i => H_i^M == 0
si T es null: H_i^E = null y viable = null
```

---

## 9. Migración y datos legados

### 9.1. Consultas de auditoría

```sql
-- Proyectos confirmados sin fecha de inicio
SELECT id_proyecto, nombre
FROM proyecto
WHERE estado = 'CONFIRMADO' AND fecha_inicio IS NULL;

-- Actividades pendientes con inicio en proyectos no confirmados
SELECT a.id_actividad, a.nombre, p.id_proyecto, p.estado
FROM actividad a
JOIN planificacion pl ON pl.id_planificacion = a.id_planificacion
JOIN proyecto p ON p.id_proyecto = pl.id_proyecto
WHERE a.estado = 'PENDIENTE'
  AND a.fecha_hora_inicio IS NOT NULL
  AND p.estado <> 'CONFIRMADO';

-- Actividades pendientes sin inicio en proyectos confirmados
SELECT a.id_actividad, a.nombre, p.id_proyecto
FROM actividad a
JOIN planificacion pl ON pl.id_planificacion = a.id_planificacion
JOIN proyecto p ON p.id_proyecto = pl.id_proyecto
WHERE a.estado = 'PENDIENTE'
  AND a.fecha_hora_inicio IS NULL
  AND p.estado = 'CONFIRMADO';

-- Asignaciones duplicadas
SELECT id_miembro, id_actividad, COUNT(*)
FROM asignacion_actividad
GROUP BY id_miembro, id_actividad
HAVING COUNT(*) > 1;
```

### 9.2. Estrategia

1. **Proyectos confirmados sin `fechaInicio`:** no inventar. Reportar y requerir corrección manual o un script de regularización con aprobación.
2. **Pendientes con inicio en proyectos no confirmados:** respaldar y setear `fecha_hora_inicio = NULL`; son proyecciones.
3. **Pendientes sin inicio en proyectos confirmados:** recalcular y persistir un plan regularizado, o marcar el proyecto como inconsistente. No confirmar de nuevo automáticamente.
4. **Asignaciones duplicadas:** consolidar antes de agregar el `UNIQUE`.
5. **Origen del inicio:** si se agrega `origen_inicio`, backfill:
   - `EN_CURSO`/`FINALIZADA` → `REAL`;
   - `PENDIENTE` en confirmado → `CONFIRMADA` solo si se validó el plan;
   - `PENDIENTE` en no confirmado → `NULL` y limpiar.
6. **Constraints:** agregar como `NOT VALID`, ejecutar limpieza y luego `VALIDATE CONSTRAINT`.

---

## 10. Riesgos y preguntas abiertas

1. **Fecha de inicio en UC-60:** se decidió que `fechaInicio` sea el instante de confirmación capturado por el `Clock` del backend (Fase 0). Queda abierto si el negocio necesita una fecha retroactiva o elegida por el director; si es así, se agrega un parámetro explícito y una validación adicional.
2. **Inicio en el pasado:** con la decisión anterior, la confirmación no puede fijar un `t0` pasado. Si se habilita una `fechaInicio` retroactiva (o una reprogramación posterior), ¿cómo se tratan las actividades pendientes que quedaron atrás? La propuesta es permitirlo solo con `releaseTime` y validación de que las fijas sean coherentes.
3. **Vínculo asignación–requerimiento:** sin él, “requerimiento completo” es ambiguo. Es el bloqueante más importante para UC-60.
4. **Duración efectiva vs. duración de calendario:** `duracion_minutos` hoy parece ser tiempo transcurrido. Si una actividad de 8 h empieza a las 16:00, el CPM dice que termina a las 00:00, pero la jornada termina antes. El planificador de recursos corrige la colocación, pero la ruta crítica sigue siendo optimista. ¿Se acepta esa aproximación o se implementa CPM con calendario?
5. **Almuerzo y jornadas partidas:** la regla actual exige un hueco continuo dentro de una ventana; una actividad no puede cruzar el almuerzo. ¿Es la regla de negocio correcta?
6. **Múltiples miembros:** se asume que todos deben estar presentes en el mismo intervalo. Si algunos roles pueden trabajar en paralelo o en momentos distintos, el modelo de intersección cambia.
7. **Planificador greedy:** no garantiza optimalidad. ¿Se acepta un primer hueco determinista o se requiere backtracking?
8. **Prioridad:** se propone ordenar por `H^E` ascendente (menos holgura de entrega primero). ¿El negocio prefiere priorizar por ruta crítica de makespan, por prioridad manual o por fecha de entrega?
9. **Reprogramación post-confirmación:** ¿se permite? ¿Quién la autoriza? ¿Notifica a todos?
10. **Cancelación:** ¿se reconectan dependencias de una actividad cancelada? ¿O el sucesor queda libre?
11. **Zona horaria/DST:** se usa `LocalDateTime`; no hay zona. Si el sistema opera en una zona con horario de verano, la aritmética de minutos puede desalinearse. Definir si se trabaja con hora local naive.
12. **Actividades `FINALIZADA`:** el modelo no guarda fin real, solo inicio + duración. Si el fin real difiere, el CPM usa una ventana ficticia. ¿Se agrega `fecha_hora_fin_real`?
13. **Notificaciones:** deben ser after-commit para no revertir la confirmación por un fallo de notificación.
14. **Compatibilidad de API:** agregar campos y estados nuevos puede romper el frontend. Se requiere versionado o actualización coordinada.

---

## 11. Criterios de aceptación

- [ ] No existe ninguna fórmula que sume `margen` a `LF`.
- [ ] `NodoCpm` expone `holguraMakespan` y `holguraEntrega` por separado.
- [ ] `critica` se calcula solo con `holguraMakespan == 0`; la holgura negativa se reporta como conflicto.
- [ ] Las actividades `EN_CURSO`, `FINALIZADA` y `PENDIENTE` con inicio confirmado tienen `LS = ES` y `LF = EF` en ambas pasadas.
- [ ] Las actividades fijas se exponen con `fijo=true`; `enRutaCritica` distingue las que determinan el makespan de las que solo tienen holgura nula por ser fijas.
- [ ] El contraejemplo de la sucesora fija está cubierto por un test que falla con `+margen`.
- [ ] `t0` no se mueve a hoy cuando está en el pasado.
- [ ] Las proyecciones no se persisten en actividades `PENDIENTE`.
- [ ] `evaluar` sin fecha de entrega devuelve `SIN_FECHA_ENTREGA`, no `NORMAL` ni una excepción de GET.
- [ ] Sin entrega, `T`, `margen` y las holguras de entrega son `null`, `viable` es `null` y el cálculo no lanza.
- [ ] Las transiciones `PENDIENTE → EN_CURSO → FINALIZADA` se permiten en un proyecto confirmado y no se revierten; un desborde real genera `ALERTA_DESBORDE`.
- [ ] La disponibilidad devuelve motivo y hueco seleccionado.
- [ ] Una actividad con varios miembros exige un hueco común.
- [ ] El margen de la actividad nueva se aplica y la separación entre actividades es la definida por negocio.
- [ ] `CANCELADA` no entra al CPM ni a la ocupación.
- [ ] La confirmación es atómica y hace rollback ante cualquier fallo.
- [ ] La confirmación setea `estado` y `fechaInicio` antes de persistir inicios, para que los triggers vean el estado correcto.
- [ ] No se puede crear/eliminar/modificar estructuralmente un proyecto `CONFIRMADO` sin reprogramación.
- [ ] Los triggers y constraints de la sección D14 están aplicados y probados.
- [ ] Los datos legados fueron auditados y migrados.
- [ ] La suite completa pasa, incluidos los tests nuevos de integración con PostgreSQL.

---

## 12. Trazabilidad con el plan v2

| Punto v2 | Corrección v3 |
|---|---|
| **P2.8:** `LF_abs = t0 + finTardio + margen` | Reemplazado por dos pasadas (sección 3). El margen global no se suma a `LF`; los terminales libres se anclan en `T` y las fijas en `EF`. |
| Pasada única hacia atrás | Pasada de makespan + pasada de entrega. |
| Holgura única | `holguraMakespan` + `holguraEntrega`. |
| Rango `[ES, LF]` | Ventana de inicio `[ES, LS^E]` y ventana de ejecución `[ES, LF^E]`. |
| Recalcular persistiendo | Cálculo de solo lectura; persistencia solo en confirmación/reprogramación. |
| `t0` ajustado a hoy | `t0` inmutable; `releaseTime` separado. |
| Disponibilidad booleana de un usuario | Intersección multi-miembro con motivo y hueco. |
| Sin mención de aceptación/asignación | Fase 0: UC-34/UC-70 como prerrequisito. |
| Sin política post-confirmación | D12 y Fase 5. |

---

## 13. Anexo: ejemplo numérico completo

Proyecto:

- `t0 = 2026-11-01 09:00`
- entrega: `2026-11-01 18:00` → `T = 540`
- `A`: 240 min, libre
- `B`: 120 min, fija, inicio `2026-11-01 13:00` → `S_B = 240`
- `C`: 60 min, libre, predecesoras `A` y `B`
- `D`: 120 min, libre, predecesora `C`

Grafo:

```text
A ─┐
   ├──> C ──> D
B ─┘
```

Pasada hacia adelante:

| Actividad | ES | EF |
|---|---:|---:|
| A | 0 | 240 |
| B (fija) | 240 | 360 |
| C | 360 | 420 |
| D | 420 | 540 |

`M = 540`, `margen = 0`.

Pasada de makespan:

| Actividad | ES | EF | LS | LF | H^M | ¿Crítica? |
|---|---:|---:|---:|---:|---:|---:|
| A | 0 | 240 | 120 | 360 | 120 | No |
| B | 240 | 360 | 240 | 360 | 0 | Sí |
| C | 360 | 420 | 360 | 420 | 0 | Sí |
| D | 420 | 540 | 420 | 540 | 0 | Sí |

La ruta crítica es `B → C → D`. `A` tiene 120 minutos de holgura porque puede empezar a las 11:00 y terminar a las 15:00, justo cuando empieza `C` (que espera a `B`, que termina a las 15:00).

Pasada de entrega:

| Actividad | ES | EF | LS^E | LF^E | H^E |
|---|---:|---:|---:|---:|---:|
| A | 0 | 240 | 120 | 360 | 120 |
| B | 240 | 360 | 240 | 360 | 0 |
| C | 360 | 420 | 360 | 420 | 0 |
| D | 420 | 540 | 420 | 540 | 0 |

Como `T = M`, ambas pasadas coinciden. Si la entrega fuera a las 20:00 (`T = 660`, `margen = 120`), la pasada de makespan no cambiaría (`A` mantiene `H^M = 120`; `B`, `C`, `D` siguen con `H^M = 0`). La pasada de entrega daría:

| Actividad | LS^E | LF^E | H^E |
|---|---:|---:|---:|
| A | 240 | 480 | 240 |
| B (fija) | 240 | 360 | 0 |
| C | 480 | 540 | 120 |
| D | 540 | 660 | 120 |

`A` gana 240 minutos de holgura de entrega porque puede empezar a las 13:00 y terminar a las 17:00, mientras `C` empieza a las 17:00. `B` no se mueve. Eso es exactamente lo que P2.8 no puede expresar sin mover `B`.

---

**Fin del documento.**
