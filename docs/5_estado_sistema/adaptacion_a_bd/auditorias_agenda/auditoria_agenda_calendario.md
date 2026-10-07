# Auditoría de Gestión de Agenda y Calendario

**Fecha:** 2026-09-29
**Revisión:** 2026-09-29 (rev. 3 — UQs exportados de ER/Studio: `UQ_agenda_usuario`, `UQ_jornada_agenda_dia_semana`)
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
>
> **Rev. 3:** la BD exportó los índices que faltaban de ER/Studio.
> **H-03 queda RESUELTO** (`UQ_jornada_agenda_dia_semana`, `ModaLinkBD.sql:1875-1878`) y
> **H-05 queda RESUELTO** (`UQ_agenda_usuario`, `ModaLinkBD.sql:1871`).
> Siguen VIGENTES: H-01, H-02, H-06–H-11 (todos del lado del código).
>
> **Rev. 4:** **H-01 queda RESUELTO** en código (`motivo` obligatorio en entidad + `@NotBlank` en request; tests ajustados).
>
> **Rev. 5:** **H-02 y H-07 quedan RESUELTOS**. Margen configurable vía `configuracion_sistema`
> (clave `AGENDA_MARGEN_ACTIVIDAD_MIN`, seed 30, trigger `fn_crear_agenda` la lee con fallback 30);
> Java: `ConfiguracionSistemaService.obtenerMargenActividadDefecto()`, `Agenda` default 30,
> `CalendarioService.margenDe()` centraliza el fallback (cubre las 4 rutas). Tests adaptados a 30 + 5 nuevos.
>
> **Rev. 6:** **H-06 queda RESUELTO** (`@Min(0)` en `ConfigJornadaRequest`; 400 vía `@Valid` existente).
>
> **Rev. 7:** **H-08 queda RESUELTO** (renombre `horario*` → `hora*` en entidad, DTOs, servicio, mapper y tests).
>
> **Rev. 8:** **H-09, H-10 y H-11 quedan RESUELTOS**. H-09: constraint renombrado en BD por el usuario.
> H-10: `requireNonNull` en `CalendarioMapper.toBloqueoActividadResponse` + filtro null-safe en
> `solapaActividad`. H-11: `@Query` usa `per.usuario.idUsuario` (propiedad real).

---

## 1. Estado de la BD (fuente de verdad)

| Tabla | Columnas clave |
|-------|----------------|
| `agenda` | `id_agenda` PK, `margen_actividad_min` DEFAULT 30 NOT NULL CHECK (>= 0), `id_usuario` NOT NULL FK → usuario, **`UQ_agenda_usuario` (rev. 3)** |
| `bloqueo_agenda` | `id_bloqueo` PK, `fecha_hora_inicio` NOT NULL, `fecha_hora_fin` NOT NULL, **`motivo` varchar(200) NOT NULL**, `id_agenda` FK |
| `jornada_agenda` | `id_jornada` PK (constraint legacy **`PK_jornada_laboral`**), `dia_semana` CHECK 1–7, `hora_inicio_manana` NOT NULL, `hora_fin_manana` NULL, `hora_inicio_tarde` NULL, `hora_fin_tarde` NOT NULL, `id_agenda` FK, 6 checks `chk_jornada_*`, **`UQ_jornada_agenda_dia_semana (id_agenda, dia_semana)` (rev. 3)** |

Triggers relevantes: `trg_bloqueo_no_solape` (no solapar bloqueos manuales),
`trg_bloqueo_no_liberar_actividad` (no borrar bloqueo cubierto por actividad de proyecto
`PUBLICADO`/`CONFIRMADO`, fin calculado como `fecha_hora_inicio + duracion_minutos + margen`),
`trg_agenda_crear` (`fn_crear_agenda`: crea agenda con **margen 30** + jornadas Lun–Vie 09:00–18:00
de corrido al insertar usuario).

---

## 2. Tabla de hallazgos

| ID | Severidad | Tipo | Descripción | Ubicación | Impacto |
|----|-----------|------|-------------|-----------|---------|
| H-01 | **CRÍTICO** | ~~Nulabilidad invertida~~ **RESUELTO (rev. 4)** | `motivo` ahora obligatorio en ambas capas: `@Column nullable = false` (`BloqueoAgenda.java:37`) + `@NotBlank @Size(max = 200)` (`MarcarNoDisponibleRequest.java:15-17`), validado con `@Valid` en `POST /calendario/bloqueos` (400 sin motivo). Helpers de integración con motivo; test nuevo `marcarNoDisponible_motivoNulo_devuelve400` | `modelo/BloqueoAgenda.java:36-38`, `calendario/dto/MarcarNoDisponibleRequest.java` | Ninguno. Nota: `BloqueoResponse.motivo` siempre viene informado en ambas rutas (la anonimización `toBloqueoResponseAnonimizado` fue eliminada; el motivo es público en `GET /calendario/perfil/{id}`) |
| H-02 | **CRÍTICO** | ~~Default divergente~~ **RESUELTO (rev. 5)** | Margen configurable: seed `AGENDA_MARGEN_ACTIVIDAD_MIN=30` + trigger lee config con fallback 30; Java: `obtenerMargenActividadDefecto()` (ausente/inválido/negativo → 30), `Agenda` default 30, `margenDe()` en las 4 rutas. Tests a 30 (incluye los 3 fallos preexistentes) + 5 nuevos | `admin/servicio/ConfiguracionSistemaService.java`, `modelo/Agenda.java:28`, `calendario/servicio/CalendarioService.java` | Ninguno. Endpoint admin de modificación queda a futuro |
| H-03 | **ALTO** | ~~Constraint faltante en BD~~ **RESUELTO (rev. 3)** | ~~`UNIQUE (id_agenda, dia_semana)` existe solo en JPA~~ La BD ahora declara `CREATE UNIQUE INDEX "UQ_jornada_agenda_dia_semana" ON jornada_agenda(id_agenda, dia_semana)` (`ModaLinkBD.sql:1875-1878`). Cubre la `@UniqueConstraint uq_jornada_agenda_dia` de JPA y respalda el sync por diff del servicio. Sin acción | `modelo/JornadaAgenda.java:34-35`, `calendario/servicio/CalendarioService.java:122-131` | Ninguno |
| H-04 | **ALTO** | ~~Checks inexistentes en BD~~ **RESUELTO (rev. 2)** | ~~Comentarios refieren a `chk_jornada_*` de "migración V19" que no existen en el SQL~~ La BD ahora define `chk_jornada_mediodia_completo`, `chk_jornada_orden_bloques`, `chk_jornada_rango_maniana`, `chk_jornada_rango_tarde`, `chk_jornada_rango_total`, `chk_jornada_dia_semana` (`ModaLinkBD.sql:746-751`), con semántica idéntica a `validarHorarios`. Sin acción | `modelo/JornadaAgenda.java:30-31`, `calendario/servicio/CalendarioService.java:251-253` | Ninguno. Defensa en profundidad BD + Java ahora efectiva |
| H-05 | **ALTO** | ~~Unicidad asumida, no garantizada~~ **RESUELTO (rev. 3)** | ~~`@JoinColumn unique = true` en `id_usuario` sin `UNIQUE` en BD~~ La BD ahora declara `CREATE UNIQUE INDEX "UQ_agenda_usuario" ON agenda(id_usuario)` (`ModaLinkBD.sql:1871`). La relación 1:1 queda impuesta en ambas capas. Sin acción | `modelo/Agenda.java:39-41`, `repositorio/AgendaRepository.java:17`, `calendario/servicio/CalendarioService.java:295-299` | Ninguno |
| H-06 | **MEDIO** | ~~Validación faltante~~ **RESUELTO (rev. 6)** | `@NotNull` + `@Min(0)` en `margenActividadMinutos`; el `@Valid` ya existente en `PUT /calendario/jornada` devuelve 400 ante negativo (antes 500 contra el CHECK). Tests: `margenNegativo_generaViolacion`, `margenCero_esValido`, `configurarJornada_margenNegativo_devuelve400` | `calendario/dto/ConfigJornadaRequest.java:16-18` | Ninguno |
| H-07 | **MEDIO** | ~~NPE potencial / inconsistencia~~ **RESUELTO (rev. 5)** | Helper `margenDe(agenda)` centraliza el fallback (valor propio o config, nunca null) en las 4 rutas: `obtener`, `obtenerPublico`, `marcarNoDisponible`, `marcarDisponible` | `calendario/servicio/CalendarioService.java` (`margenDe`) | Ninguno |
| H-08 | **MEDIO** | ~~Vocabulario divergente~~ **RESUELTO (rev. 7)** | Propiedades renombradas a `horaInicioManana`, `horaFinManana`, `horaInicioTarde`, `horaFinTarde` en entidad, `JornadaDiaRequest/Response`, servicio, mapper y tests. El `@Column` ya apuntaba bien; ahora también el nombre Java. Nota: cambian las claves JSON del API (`horaInicioManana`, etc.) — actualizar clientes | Entidad + DTOs + servicio + mapper + tests de calendario | Ninguno |
| H-09 | **BAJO** | ~~Nombre legacy~~ **RESUELTO (rev. 8)** | Constraint renombrado en BD por el usuario. Sin cambios Java (JPA nunca referencia nombres de constraints) | `ModaLinkBD.sql` | Ninguno |
| H-10 | **BAJO** | ~~Sin guardas null~~ **RESUELTO (rev. 8)** | `toBloqueoActividadResponse` exige inicio/fin con `requireNonNull` (falla rápido con mensaje en vez de NPE críptico); `solapaActividad` ignora actividades sin rango | `calendario/mapper/CalendarioMapper.java`, `calendario/servicio/CalendarioService.java` (`solapaActividad`) | Ninguno. Tests: `toBloqueoActividadResponse_sinInicio/sinDuracion_lanzaExcepcion` |
| H-11 | **BAJO** | ~~JPQL frágil~~ **RESUELTO (rev. 8)** | `@Query` usa `per.usuario.idUsuario` (propiedad real de `Usuario`) en vez del atajo `.id` | `repositorio/ActividadRepository.java:30` | Ninguno |
| H-12 | INFO | Nuevo check consistente (rev. 2) | La BD agregó `chk_bloqueo_rango CHECK (fecha_hora_fin > fecha_hora_inicio)` (`ModaLinkBD.sql:207`) | `calendario/servicio/CalendarioService.java:289-293` (`validarRango`) | Ninguno. El servicio ya valida lo mismo antes de persistir; defensa en profundidad correcta. Sin acción |

---

## 3. Detalle de hallazgos críticos y altos

### H-01. `motivo` — RESUELTO (rev. 4)
- `BloqueoAgenda.motivo` no declara `nullable = false`; `MarcarNoDisponibleRequest.motivo` admite null; el servicio lo persiste tal cual.
- La BD exige `motivo varchar(200) NOT NULL`. Marcar "No disponible" sin motivo → `DataIntegrityViolationException` (500) en vez del 201 esperado.
- Resuelto rev. 4: entidad con `nullable = false`, request con `@NotBlank` (400 vía `@Valid`), comentarios corregidos. Sin acción pendiente.

### H-02. Margen configurable — RESUELTO (rev. 5)
- `Agenda.java:28` fija 60; `agenda.margen_actividad_min DEFAULT 30` y `fn_crear_agenda ... VALUES (30, ...)`.
- Consecuencia: el ancho de los bloqueos calculados (`±margen`) depende de cómo se creó la agenda.
- Acción: unificar en 30 (valor de la BD como fuente de verdad), actualizar `MARGEN_ACTIVIDAD_MINUTOS_DEFECTO`, el fallback del servicio y los tests que esperan 60.
- **Rev. 5:** resuelto con margen configurable (seed + trigger + `obtenerMargenActividadDefecto()` + default 30 + `margenDe()`). Sin acción pendiente.

### H-03. Unique `(id_agenda, dia_semana)` — RESUELTO (rev. 3)
- Verificado rev. 3: la BD declara el índice (exportado de ER/Studio). Cubre la `@UniqueConstraint` de JPA y el sync por diff. Sin acción pendiente.
- H-04 también resuelto en rev. 2. Histórico de lo pedido: `hora_fin_manana IS NULL = hora_inicio_tarde IS NULL`, orden `inicio_maniana < fin_maniana < inicio_tarde < fin_tarde`, `inicio_maniana < fin_tarde` — ambos cumplidos en rev. 2/3).

### H-05. Una agenda por usuario — RESUELTO (rev. 3)
- `AgendaRepository.findByUsuario_IdUsuario` + `agendaDe()` + trigger `trg_agenda_crear` asumen 1:1, y la BD ahora lo impone con `UQ_agenda_usuario`. Sin acción pendiente.
- ~~Acción: agregar `UNIQUE (id_usuario)` en BD~~ Cumplido rev. 3 con `UQ_agenda_usuario`; el repositorio no requiere cambios.

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

1. ~~**Decidir nulabilidad de `motivo`** (H-01)~~ **Hecho rev. 4** (H-01 resuelto: `@NotBlank` + `nullable = false`).
2. ~~**Unificar default del margen a 30** (H-02)~~ **Hecho rev. 5** (margen configurable, default 30).
3. ~~**Completar constraints en BD** (H-03/H-05)~~ **Hecho en BD rev. 3** (H-03/H-05 resueltos). ~~Resta solo renombrar `PK_jornada_laboral` (H-09, cosmético).~~ **Hecho por el usuario rev. 8** (H-09 resuelto).
4. ~~**Agregar `@Min(0)`**~~ **Hecho rev. 6** (H-06 resuelto: `@Min(0)` + 400 vía `@Valid`).
5. ~~**Unificar fallback de margen**~~ **Hecho rev. 5** vía `margenDe()` (H-07 resuelto).
6. ~~**Limpieza menor:** renombrar `horario*` → `hora*`~~ **Hecho rev. 7** (H-08 resuelto). ~~Resta: `per.usuario.idUsuario` en el `@Query` (H-11); guardas null en el mapper (H-10).~~ **Hechos rev. 8** (H-10/H-11 resueltos).
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
