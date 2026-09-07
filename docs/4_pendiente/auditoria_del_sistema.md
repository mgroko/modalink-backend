# Auditoría de Trazabilidad 1 a 1: BD Canónica vs Migraciones Flyway vs Entidades JPA

> **Fase 4: Pendiente & Auditoría (Proceso Unificado)**  
> **Fecha de Auditoría**: Septiembre 2026  
> **Artefactos Analizados**:
> 1. **Modelo Físico Canónico (Single Source of Truth)**: [bd.sql](file:///c:/Users/HP/Desktop/modalink-backend/docs/0_nota_presentacion/bd.sql) (45 sentencias `CREATE TABLE`)
> 2. **Migraciones Versionadas Flyway**: `src/main/resources/db/migration/` (`V1` a `V19`)
> 3. **Entidades del Modelo Backend**: `src/main/java/org/mgroko/backend/modelo/` (30 clases `@Entity` + tablas intermedias JPA)

---

## 1. Resumen Ejecutivo del Diagnóstico

1. **Total de Tablas en `bd.sql`**: **45 tablas**.
2. **Total de Tablas en Migraciones Flyway**: **39 tablas** (`V1` a `V19`).
   - **Tablas postergadas para la Iteración 02**: 6 tablas pertenecientes al Módulo de Publicaciones e Interacción (`publicacion`, `imagen_publicacion`, `colaborador_publicacion`, `solicitud_colaboracion`, `comentario`, `me_gusta`). Están formalmente declaradas en el alcance de `V1__esquema_base_modalink.sql` (líneas 8-11: *"Quedan fuera de esta migración (segunda iteración): publicacion, comentario, me_gusta, colaborador_publicacion, solicitud_colaboracion, imagen_publicacion"*).
3. **Total de Entidades y Mapeos en Java (`org.mgroko.backend.modelo`)**: **39 tablas mapeadas (100% de cobertura sobre las migraciones activas)**.
   - **29 clases `@Entity` directas** anotadas con `@Table(name = "...")` mapeando tablas principales.
   - **10 tablas intermedias / N:M** modeladas mediante anotaciones `@JoinTable` de JPA en las entidades dueñas de la relación.
   - **0 discrepancias no justificadas** en el alcance de la Iteración 01.

---

## 2. Matriz de Trazabilidad 1 a 1 Completa (45 Tablas)

| # | Tabla Canónica (`bd.sql`) | Migración Flyway | Entidad Java (`org.mgroko.backend.modelo`) | Tipo de Mapeo en JPA | Estado de Trazabilidad |
|:---:|:---|:---|:---|:---|:---:|
| 1 | `actividad` | `V1` | `Actividad.java` | `@Entity` `@Table(name = "actividad")` |  **1:1 Conforme** |
| 2 | `agenda` | `V1`, `V14`, `V16` | `Agenda.java` | `@Entity` `@Table(name = "agenda")` |  **1:1 Conforme** |
| 3 | `asignacion_actividad` | `V1` | `AsignacionActividad.java` | `@Entity` `@Table(name = "asignacion_actividad")` |  **1:1 Conforme** |
| 4 | `bloqueo_agenda` | `V1`, `V14` | `BloqueoAgenda.java` | `@Entity` `@Table(name = "bloqueo_agenda")` |  **1:1 Conforme** |
| 5 | `caracteristica_perfil` | `V8` | `CaracteristicaPerfil.java` | `@Entity` `@Table(name = "caracteristica_perfil")` |  **1:1 Conforme** |
| 6 | `caracteristica_tecnica` | `V1`, `V8`, `V11` | `CaracteristicaTecnica.java` | `@Entity` `@Table(name = "caracteristica_tecnica")` |  **1:1 Conforme** |
| 7 | `colaborador_publicacion` | *Diferido (Iteración 02)* | *No mapeado* | *Diferido (Iteración 02)* | ⚠️ **Diferido Iteración 02** |
| 8 | `comentario` | *Diferido (Iteración 02)* | *No mapeado* | *Diferido (Iteración 02)* | ⚠️ **Diferido Iteración 02** |
| 9 | `dependencia_actividades` | `V1` | `Actividad.java` | `@JoinTable(name = "dependencia_actividades")` |  **1:1 Conforme (JPA JoinTable)** |
| 10 | `genero` | `V3` | `Genero.java` | `@Entity` `@Table(name = "genero")` |  **1:1 Conforme** |
| 11 | `habilidad` | `V1` | `Habilidad.java` | `@Entity` `@Table(name = "habilidad")` |  **1:1 Conforme** |
| 12 | `habilidad_perfil` | `V1` | `Perfil.java` | `@JoinTable(name = "habilidad_perfil")` |  **1:1 Conforme (JPA JoinTable)** |
| 13 | `imagen` | `V1` | `Imagen.java` | `@Entity` `@Table(name = "imagen")` |  **1:1 Conforme** |
| 14 | `imagen_moodboard` | `V1` | `Moodboard.java` | `@JoinTable(name = "imagen_moodboard")` |  **1:1 Conforme (JPA JoinTable)** |
| 15 | `imagen_publicacion` | *Diferido (Iteración 02)* | *No mapeado* | *Diferido (Iteración 02)* | ⚠️ **Diferido Iteración 02** |
| 16 | `invitacion_actividad` | `V1` | `InvitacionActividad.java` | `@Entity` `@Table(name = "invitacion_actividad")` |  **1:1 Conforme** |
| 17 | `invitacion_gral` | `V1` | `InvitacionGral.java` | `@Entity` `@Table(name = "invitacion_gral")` |  **1:1 Conforme** |
| 18 | `jornada_agenda` | `V14`, `V19` | `JornadaAgenda.java` | `@Entity` `@Table(name = "jornada_agenda")` |  **1:1 Conforme** |
| 19 | `me_gusta` | *Diferido (Iteración 02)* | *No mapeado* | *Diferido (Iteración 02)* | ⚠️ **Diferido Iteración 02** |
| 20 | `miembros_proyecto` | `V1` | `MiembroProyecto.java` | `@Entity` `@Table(name = "miembros_proyecto")` |  **1:1 Conforme** |
| 21 | `moodboard` | `V1` | `Moodboard.java` | `@Entity` `@Table(name = "moodboard")` |  **1:1 Conforme** |
| 22 | `objetivo` | `V1` | `Objetivo.java` | `@Entity` `@Table(name = "objetivo")` |  **1:1 Conforme** |
| 23 | `perfil` | `V1`, `V8`, `V13` | `Perfil.java` | `@Entity` `@Table(name = "perfil")` |  **1:1 Conforme** |
| 24 | `permiso_global` | `V1`, `V4`, `V11` | `PermisoGlobal.java` | `@Entity` `@Table(name = "permiso_global")` |  **1:1 Conforme** |
| 25 | `permiso_proyecto` | `V1` | `PermisoProyecto.java` | `@Entity` `@Table(name = "permiso_proyecto")` |  **1:1 Conforme** |
| 26 | `planificacion` | `V1` | `Planificacion.java` | `@Entity` `@Table(name = "planificacion")` |  **1:1 Conforme** |
| 27 | `postulacion_actividad` | `V1` | `PostulacionActividad.java` | `@Entity` `@Table(name = "postulacion_actividad")` |  **1:1 Conforme** |
| 28 | `postulacion_gral` | `V1` | `PostulacionGral.java` | `@Entity` `@Table(name = "postulacion_gral")` |  **1:1 Conforme** |
| 29 | `profesion` | `V1`, `V5`, `V6` | `Profesion.java` | `@Entity` `@Table(name = "profesion")` |  **1:1 Conforme** |
| 30 | `proyecto` | `V1` | `Proyecto.java` | `@Entity` `@Table(name = "proyecto")` |  **1:1 Conforme** |
| 31 | `publicacion` | *Diferido (Iteración 02)* | *No mapeado* | *Diferido (Iteración 02)* | ⚠️ **Diferido Iteración 02** |
| 32 | `requerimiento_act_caract` | `V1` | `RequerimientoActividad.java` | `@JoinTable(name = "requerimiento_act_caract")` |  **1:1 Conforme (JPA JoinTable)** |
| 33 | `requerimiento_act_habilidad` | `V1` | `RequerimientoActividad.java` | `@JoinTable(name = "requerimiento_act_habilidad")` |  **1:1 Conforme (JPA JoinTable)** |
| 34 | `requerimiento_actividad` | `V1` | `RequerimientoActividad.java` | `@Entity` `@Table(name = "requerimiento_actividad")` |  **1:1 Conforme** |
| 35 | `requerimiento_gral_caract` | `V1` | `RequerimientoGralProyecto.java` | `@JoinTable(name = "requerimiento_gral_caract")` |  **1:1 Conforme (JPA JoinTable)** |
| 36 | `requerimiento_gral_habilidad` | `V1` | `RequerimientoGralProyecto.java` | `@JoinTable(name = "requerimiento_gral_habilidad")` |  **1:1 Conforme (JPA JoinTable)** |
| 37 | `requerimiento_gral_proyecto` | `V1` | `RequerimientoGralProyecto.java` | `@Entity` `@Table(name = "requerimiento_gral_proyecto")` |  **1:1 Conforme** |
| 38 | `rol_global` | `V1` | `RolGlobal.java` | `@Entity` `@Table(name = "rol_global")` |  **1:1 Conforme** |
| 39 | `rol_global_permiso` | `V1` | `RolGlobal.java` | `@JoinTable(name = "rol_global_permiso")` |  **1:1 Conforme (JPA JoinTable)** |
| 40 | `rol_proyecto` | `V1` | `RolProyecto.java` | `@Entity` `@Table(name = "rol_proyecto")` |  **1:1 Conforme** |
| 41 | `rol_proyecto_permiso` | `V1` | `RolProyecto.java` | `@JoinTable(name = "rol_proyecto_permiso")` |  **1:1 Conforme (JPA JoinTable)** |
| 42 | `solicitud_colaboracion` | *Diferido (Iteración 02)* | *No mapeado* | *Diferido (Iteración 02)* | ⚠️ **Diferido Iteración 02** |
| 43 | `ubicacion` | `V1`, `V12` | `Ubicacion.java` | `@Entity` `@Table(name = "ubicacion")` |  **1:1 Conforme** |
| 44 | `usuario` | `V1`, `V2`, `V3`, `V7`, `V17`, `V18` | `Usuario.java` | `@Entity` `@Table(name = "usuario")` |  **1:1 Conforme** |
| 45 | `valor_caracteristica` | `V9`, `V10`, `V15` | `ValorCaracteristica.java` | `@Entity` `@Table(name = "valor_caracteristica")` |  **1:1 Conforme** |

---

## 3. Análisis Detallado por Capas y Tablas Puente

### 3.1 Tablas Intermedias Relacionales (Many-to-Many en JPA)
En el diseño orientado a objetos con JPA/Hibernate, las tablas puramente asociativas no requieren una clase `@Entity` propia si no contienen atributos adicionales, mapeándose limpiamente con `@JoinTable`:

1. **`dependencia_actividades`**: Mapeada en [Actividad.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/Actividad.java#L44-L58) con colecciones de `predecesoras` y `sucesoras`.
2. **`habilidad_perfil`**: Mapeada en [Perfil.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/Perfil.java#L76-L81) con `@JoinTable` hacia `Habilidad`.
3. **`imagen_moodboard`**: Mapeada en [Moodboard.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/Moodboard.java#L33-L38) con `@JoinTable` hacia `Imagen`.
4. **`requerimiento_act_caract`**: Mapeada en [RequerimientoActividad.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/RequerimientoActividad.java#L43-L48) hacia `CaracteristicaTecnica`.
5. **`requerimiento_act_habilidad`**: Mapeada en [RequerimientoActividad.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/RequerimientoActividad.java#L34-L39) hacia `Habilidad`.
6. **`requerimiento_gral_caract`**: Mapeada en [RequerimientoGralProyecto.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/RequerimientoGralProyecto.java#L43-L48) hacia `CaracteristicaTecnica`.
7. **`requerimiento_gral_habilidad`**: Mapeada en [RequerimientoGralProyecto.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/RequerimientoGralProyecto.java#L34-L39) hacia `Habilidad`.
8. **`rol_global_permiso`**: Mapeada en [RolGlobal.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/RolGlobal.java#L23-L28) hacia `PermisoGlobal`.
9. **`rol_proyecto_permiso`**: Mapeada en [RolProyecto.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/RolProyecto.java#L23-L28) hacia `PermisoProyecto`.
10. **`caracteristica_perfil`**: Posee entidad propia [CaracteristicaPerfil.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/CaracteristicaPerfil.java) y clave compuesta [CaracteristicaPerfilId.java](file:///c:/Users/HP/Desktop/modalink-backend/src/main/java/org/mgroko/backend/modelo/CaracteristicaPerfilId.java), debido a que porta el atributo de negocio `valor`.

---

## 4. Auditoría de Columnas Críticas y Evolución de Migraciones

| Tabla | Columna Auditada | Migración | Mapeo en Entidad Java | Validación de Integridad |
|:---|:---|:---:|:---|:---:|
| `usuario` | `motivo_deshabilitacion` | `V18` | `Usuario.java: String motivoDeshabilitacion` |  Alineado con UC-04 (obligatorio al deshabilitar). |
| `usuario` | `fecha_hasta_deshabilitacion`| `V18` | `Usuario.java: LocalDateTime fechaHastaDeshabilitacion` |  Alineado con UC-04 (reactivación automática). |
| `usuario` | `fecha_solicitud_baja` | `V1`, `V7` | `Usuario.java: LocalDateTime fechaSolicitudBaja` |  Alineado con UC-07 (borrado lógico). |
| `perfil` | `fecha_solicitud_baja` / `baja` | `V13` | `Perfil.java: LocalDateTime fechaSolicitudBaja`, `EstadoPerfil estado` |  Alineado con UC-12 (borrado lógico). |
| `jornada_agenda` | `horario_inicio_maniana` | `V19` | `JornadaAgenda.java: LocalTime horarioInicioManiana` |  Alineado con UC-17 / UC-18. |
| `jornada_agenda` | `horario_fin_maniana` | `V19` | `JornadaAgenda.java: LocalTime horarioFinManiana` |  Alineado con UC-17 / UC-18 (jornada partida). |
| `jornada_agenda` | `horario_inicio_tarde` | `V19` | `JornadaAgenda.java: LocalTime horarioInicioTarde` |  Alineado con UC-17 / UC-18 (jornada partida). |
| `jornada_agenda` | `horario_fin_tarde` | `V19` | `JornadaAgenda.java: LocalTime horarioFinTarde` |  Alineado con UC-17 / UC-18. |
| `agenda` | `margen_actividad_minutos` | `V14`, `V16` | `Agenda.java: Integer margenActividadMinutos` |  Alineado (default 60 min). |

---

## 5. Dictamen y Conclusiones de Trazabilidad

1. **Trazabilidad 1:1 en Iteración 01**: **100% CONFORME**. Las 39 tablas requeridas para operar los módulos de Usuarios, Perfiles, Calendario, Proyectos y Convocatorias están creadas en Flyway y mapeadas exactamente en `org.mgroko.backend.modelo`.
2. **Consistencia con `bd.sql`**: Las 6 tablas restantes en `bd.sql` corresponden exclusivamente al Módulo de Publicaciones e Interacción social (`publicacion`, `colaborador_publicacion`, `solicitud_colaboracion`, `comentario`, `me_gusta`, `imagen_publicacion`), las cuales fueron explícitamente planificadas para la **Segunda Iteración** según la arquitectura del proyecto y la cabecera de `V1__esquema_base_modalink.sql`.
3. **Sin Fugas ni Inconsistencias**: No existen tablas huérfanas en Flyway ni entidades JPA apuntando a tablas inexistentes.
