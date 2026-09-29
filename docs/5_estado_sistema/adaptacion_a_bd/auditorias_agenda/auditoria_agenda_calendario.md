# Auditoría de Gestión de Agenda y Calendario

**Fecha:** 2026-09-29
**Revisión:** 2026-09-29 (rev. 2 — tras commit `189d36d` "Actualización de la BD: checks a tabla")
**Proyecto:** modalink-backend
**Alcance:** Entidades `Agenda`, `BloqueoAgenda`, `JornadaAgenda` y módulo `calendario` (controlador, servicio, mapper, DTOs, repositorios)
**Fuente de verdad:** `docs/3_diseño/ModaLinkBD.sql`

> [!NOTE]
> El código está parcialmente adaptado a la BD (actividad con `duracion_minutos` + fin derivado,
> estados `PUBLICADO`/`CONFIRMADO`, filtro de participación `ACTIVO`), pero persisten
> discrepancias críticas de nulabilidad, defaults y constraints faltantes en la BD.
> Con `spring.jpa.hibernate.ddl-auto=validate` (`application.properties:7`), los desajustes
> de nulabilidad/unicidad pueden impedir el arranque o producir errores 500 en runtime.
>
> **Rev. 2:** la BD exportó los CHECKs a nivel de tabla. El hallazgo **H-04 queda RESUELTO**
> (los `chk_jornada_*` ya existen y su semántica coincide con `validarHorarios`).
> Se agrega **H-12** (nuevo `chk_bloqueo_rango`, consistente, sin acción).
> El código no cambió (`git status` limpio salvo esta auditoría), por lo que
> H-01, H-02, H-03, H-05–H-11 siguen VIGENTES.

---

## 1. Estado de la BD (fuente de verdad)

| Tabla | Columnas clave |
|-------|----------------|
| `agenda` | `id_agenda` PK, `margen_actividad_min` DEFAULT 30 NOT NULL CHECK (>= 0), `id_usuario` NOT NULL FK → usuario (**sin UNIQUE**) |
| `bloqueo_agenda` | `id_bloqueo` PK, `fecha_hora_inicio` NOT NULL, `fecha_hora_fin` NOT NULL, **`motivo` varchar(200) NOT NULL**, `id_agenda` FK |
| `jornada_agenda` | `id_jornada` PK (constraint legacy **`PK_jornada_laboral`**), `dia_semana` CHECK 1–7, `hora_inicio_manana` NOT NULL, `hora_fin_manana` NULL, `hora_inicio_tarde` NULL, `hora_fin_tarde` NOT NULL, `id_agenda` FK (**sin UNIQUE `(id_agenda, dia_semana)`**, **sin checks de corrida/partida**) |

Triggers relevantes: `trg_bloqueo_no_solape` (no solapar bloqueos manuales),
`trg_bloqueo_no_liberar_actividad` (no borrar bloqueo cubierto por actividad de proyecto
`PUBLICADO`/`CONFIRMADO`, fin calculado como `fecha_hora_inicio + duracion_minutos + margen`),
`trg_agenda_crear` (`fn_crear_agenda`: crea agenda con **margen 30** + jornadas Lun–Vie 09:00–18:00
de corrido al insertar usuario).

---

## 2. Tabla de hallazgos

| ID | Severidad | Tipo | Descripción | Ubicación | Impacto |
|----|-----------|------|-------------|-----------|---------|
| H-01 | **CRÍTICO** | Nulabilidad invertida | `motivo` es opcional en código pero `NOT NULL` en BD | `modelo/BloqueoAgenda.java:36-38`, `calendario/dto/MarcarNoDisponibleRequest.java:15`, `calendario/servicio/CalendarioService.java:191-197` | Todo bloqueo sin motivo falla con violación de constraint (500). El comentario "Opcional según UC-18" contradice la BD |
| H-02 | **CRÍTICO** | Default divergente | `MARGEN_ACTIVIDAD_MINUTOS_DEFECTO = 60` vs BD `DEFAULT 30` + trigger con `30` | `modelo/Agenda.java:28,37`, `calendario/servicio/CalendarioService.java:85-87,111-113` | Agendas creadas por trigger tienen 30; objetos construidos en código/tests, 60. El cálculo de bloqueos (margen a cada lado) diverge según el origen del dato. Tests que esperan 60 (`CalendarioControllerTest:78,100`) están desactualizados respecto a la BD |
| H-03 | **ALTO** | Constraint faltante en BD | `UNIQUE (id_agenda, dia_semana)` existe solo en JPA | `modelo/JornadaAgenda.java:34-35`, `calendario/servicio/CalendarioService.java:122-131` | El servicio implementa sync por diff para no violar una unique que la BD no tiene: la BD admite días duplicados. Riesgo de fallo en `validate` y de datos duplicados por inserts directos |
| H-04 | **ALTO** | ~~Checks inexistentes en BD~~ **RESUELTO (rev. 2)** | ~~Comentarios refieren a `chk_jornada_*` de "migración V19" que no existen en el SQL~~ La BD ahora define `chk_jornada_mediodia_completo`, `chk_jornada_orden_bloques`, `chk_jornada_rango_maniana`, `chk_jornada_rango_tarde`, `chk_jornada_rango_total`, `chk_jornada_dia_semana` (`ModaLinkBD.sql:746-751`), con semántica idéntica a `validarHorarios`. Sin acción | `modelo/JornadaAgenda.java:30-31`, `calendario/servicio/CalendarioService.java:251-253` | Ninguno. Defensa en profundidad BD + Java ahora efectiva |
| H-05 | **ALTO** | Unicidad asumida, no garantizada | `@JoinColumn unique = true` en `id_usuario` sin `UNIQUE` en BD | `modelo/Agenda.java:39-41`, `repositorio/AgendaRepository.java:17`, `calendario/servicio/CalendarioService.java:295-299` | Código y trigger asumen 1 agenda por usuario (`findByUsuario` → `Optional`); la BD permite N agendas → `NonUniqueResultException` / agendas huérfanas. Falta `UNIQUE(id_usuario)` en BD |
| H-06 | **MEDIO** | Validación faltante | `margenActividadMinutos` solo `@NotNull`, sin `@Min(0)`; el servicio no lo valida | `calendario/dto/ConfigJornadaRequest.java:16`, `calendario/servicio/CalendarioService.java:163-164` | Un margen negativo pasa la validación 400 y revienta contra el `CHECK (>= 0)` (500); además corrompe la matemática de solape |
| H-07 | **MEDIO** | NPE potencial / inconsistencia | `marcarNoDisponible` y `marcarDisponible` pasan `Integer` a parámetro `int` sin fallback | `calendario/servicio/CalendarioService.java:182,216,228` | `obtener`/`obtenerPublico` sí aplican fallback al default (líneas 85-87, 111-113); estas dos rutas harían NPE (unboxing) si el margen fuera null. Hoy la BD lo impide (`NOT NULL`), pero el código es inconsistente y frágil |
| H-08 | **MEDIO** | Vocabulario divergente | Propiedades `horario*` vs columnas BD `hora_*` (`hora_inicio_manana` con una "n") | `modelo/JornadaAgenda.java:47-57`, DTOs `JornadaDiaRequest/Response` | El `@Column` explícito lo salva en JPA, pero cualquier query nativa/Criteria con el nombre equivocado falla. Confusión en mantenimiento |
| H-09 | **BAJO** | Nombre legacy | PK de `jornada_agenda` se llama `PK_jornada_laboral` | `ModaLinkBD.sql:696` (lado BD, informativo) | Cosmético; evidencia renombre de tabla no propagado al nombre del constraint |
| H-10 | **BAJO** | Sin guardas null | `toBloqueoActividadResponse` opera sobre `getFechaHoraInicio()/getFechaHoraFin()` sin null-check | `calendario/mapper/CalendarioMapper.java:58-63` | La BD garantiza `NOT NULL`, pero objetos transient/tests con inicio null producen NPE en `minusMinutes/plusMinutes` |
| H-11 | **BAJO** | JPQL frágil | `per.usuario.id` en vez de `per.usuario.idUsuario` | `repositorio/ActividadRepository.java:30` | Funciona por el shortcut `.id` de Hibernate al identificador, pero acopla a un atajo implícito; preferir la propiedad real `idUsuario` |
| H-12 | INFO | Nuevo check consistente (rev. 2) | La BD agregó `chk_bloqueo_rango CHECK (fecha_hora_fin > fecha_hora_inicio)` (`ModaLinkBD.sql:207`) | `calendario/servicio/CalendarioService.java:289-293` (`validarRango`) | Ninguno. El servicio ya valida lo mismo antes de persistir; defensa en profundidad correcta. Sin acción |

---

## 3. Detalle de hallazgos críticos y altos

### H-01. `motivo` opcional en código vs `NOT NULL` en BD
- `BloqueoAgenda.motivo` no declara `nullable = false`; `MarcarNoDisponibleRequest.motivo` admite null; el servicio lo persiste tal cual.
- La BD exige `motivo varchar(200) NOT NULL`. Marcar "No disponible" sin motivo → `DataIntegrityViolationException` (500) en vez del 201 esperado.
- Decisión requerida: o bien la BD relaja a NULL (coherente con UC-18 "motivo opcional" y con `toBloqueoResponseAnonimizado` que ya contempla motivo null), o bien el código exige `@NotBlank motivo`.

### H-02. Default del margen: 60 (código) vs 30 (BD + trigger)
- `Agenda.java:28` fija 60; `agenda.margen_actividad_min DEFAULT 30` y `fn_crear_agenda ... VALUES (30, ...)`.
- Consecuencia: el ancho de los bloqueos calculados (`±margen`) depende de cómo se creó la agenda.
- Acción: unificar en 30 (valor de la BD como fuente de verdad), actualizar `MARGEN_ACTIVIDAD_MINUTOS_DEFECTO`, el fallback del servicio y los tests que esperan 60.

### H-03. Unique ausente en BD (VIGENTE rev. 2)
- Verificado rev. 2: `jornada_agenda` no declara UNIQUE y ningún índice `UQ_*` la cubre; la `@UniqueConstraint uq_jornada_agenda_dia` solo existe en JPA.
- Acción: agregar en BD `UNIQUE (id_agenda, dia_semana)` y checks (`hora_fin_manana IS NULL = hora_inicio_tarde IS NULL`, orden `inicio_maniana < fin_maniana < inicio_tarde < fin_tarde`, `inicio_maniana < fin_tarde`); o bien documentar unicidad solo aplicativa. **Rev. 2:** H-03 vigente (sin UNIQUE ni índice en BD); H-04 resuelto (`ModaLinkBD.sql:746-751` replica `validarHorarios`).

### H-05. Una agenda por usuario: asumido, no impuesto
- `AgendaRepository.findByUsuario_IdUsuario` + `agendaDe()` + trigger `trg_agenda_crear` asumen 1:1, pero `agenda.id_usuario` no es UNIQUE.
- Acción: agregar `UNIQUE (id_usuario)` en BD (recomendado, coherente con el trigger), o cambiar el repositorio a `List` + política de desempate en código.

---

## 4. Aspectos verificados como CONSISTENTES (sin acción)

1. **Fin de actividad derivado:** `Actividad.getFechaHoraFin() = inicio + duracion_minutos` coincide con el cálculo del trigger `fn_validar_bloqueo_no_actividad` (`inicio + duracion*interval + margen`). Bien adaptado al cambio de modelo (sin columna `fecha_hora_fin`).
2. **Estados activos:** `CalendarioService.ESTADOS_ACTIVOS = [Publicado, Confirmado]` coincide con `p.estado IN ('PUBLICADO','CONFIRMADO')` del trigger.
3. **Participación activa:** el `@Query` filtra `estadoParticipacion = ACTIVO`, igual que el trigger (`mp.estado_participacion = 'ACTIVO'`).
4. **No-solape manual:** el chequeo `existsBy...FechaHoraInicioLessThan...GreaterThan` replica la condición del trigger (`inicio < fin_existente AND fin > inicio_existente`). Defensa en profundidad correcta.
5. **`dia_semana` 1–7:** CHECK en BD + validación en `validarJornada`. Consistente (notar H-07: `dia.diaSemana()` hace unboxing; un null saltaría la validación `@NotNull` solo si el servicio se invoca fuera del controlador con `@Valid`).
6. **Jornada default del trigger** (Lun–Vie 09:00–18:00, solo `hora_inicio_manana`/`hora_fin_tarde`) es una jornada "de corrido" válida según el modelo Java (`esPartida() == false`).
7. **Longitudes:** `motivo` 200 = `varchar(200)`; `nombre` actividad 20 = `varchar(20)`; `dia_semana`/`margen` tipos compatibles.

---

## 5. Recomendaciones (plan de adaptación)

1. **Decidir nulabilidad de `motivo`** (H-01) y adaptar el lado contrario (BD o Bean Validation). Prioridad máxima: hoy el endpoint `POST /calendario/bloqueos` sin motivo rompe.
2. **Unificar default del margen a 30** (H-02): constante, fallback del servicio y tests (`CalendarioControllerTest:78,100`).
3. **Completar constraints en BD** (H-03/H-05): `UNIQUE(id_usuario)` en `agenda`, `UNIQUE(id_agenda, dia_semana)` en `jornada_agenda`; renombrar `PK_jornada_laboral` (H-09).
4. **Agregar `@Min(0)`** a `ConfigJornadaRequest.margenActividadMinutos` + validación explícita en `configurarJornada` (H-06) para devolver 400 en vez de 500.
5. **Unificar fallback de margen** en `marcarNoDisponible`/`marcarDisponible` con el usado en `obtener` (extraer `margenDe(agenda)` helper) (H-07).
6. **Limpieza menor:** renombrar `horario*` → `hora*` o documentar la equivalencia (H-08); usar `per.usuario.idUsuario` en el `@Query` (H-11); guardas null en el mapper (H-10).
7. **Revalidar arranque** con `ddl-auto=validate` tras los cambios y agregar test de integración que inserte bloqueo sin motivo (según la decisión de H-01) y jornada duplicada (según H-03).

---

## 6. Resumen de archivos auditados

| Categoría | Archivos |
|-----------|----------|
| Entidades | `modelo/Agenda.java`, `modelo/BloqueoAgenda.java`, `modelo/JornadaAgenda.java` (ref: `modelo/Actividad.java`, `modelo/AsignacionActividad.java`, `modelo/Usuario.java`, `modelo/Perfil.java`) |
| Servicio | `calendario/servicio/CalendarioService.java` |
| Controlador | `calendario/controlador/CalendarioController.java` |
| Mapper/DTOs | `calendario/mapper/CalendarioMapper.java`, `CalendarioResponse.java`, `ConfigJornadaRequest/Response.java`, `JornadaDiaRequest/Response.java`, `MarcarNoDisponibleRequest.java`, `BloqueoResponse.java`, `BloqueoActividadResponse.java` |
| Excepciones | `AgendaNoEncontradaException.java`, `BloqueoNoEncontradoException.java`, `BloqueoSolapadoException.java`, `HorarioComprometidoException.java`, `JornadaInvalidaException.java`, `RangoInvalidoException.java` |
| Repositorios | `repositorio/AgendaRepository.java`, `repositorio/BloqueoAgendaRepository.java`, `repositorio/JornadaAgendaRepository.java`, `repositorio/ActividadRepository.java` |
| SQL schema | `docs/3_diseño/ModaLinkBD.sql` (tablas `agenda`, `bloqueo_agenda`, `jornada_agenda`; triggers `trg_bloqueo_no_solape`, `trg_bloqueo_no_liberar_actividad`, `trg_agenda_crear`) |
