# Estado del Sistema — Versión 0.2

> **Fase 5: Estado del Sistema (Proceso Unificado)**  
> **Período de Análisis**: Commits `25c9e20` (30-Ago-2026) a `5884bbe` (07-Sep-2026)  
> **Versión**: v0.2  
> **Fuente de Verdad de BD**: `base_de_datos.sql` (Migraciones Flyway V1 a V20 consolidadas)

---

## 1. Resumen Ejecutivo de la Versión

La versión **v0.2** representa un salto evolutivo mayor en el backend de **ModaLink**, focalizado en tres pilares esenciales:
1. **Desarrollo completo y refinamiento del módulo de Calendario y Disponibilidad** (jornadas laborales configurables de corrido o partidas, márgenes entre actividades, bloqueos manuales y por actividad).
2. **Contextualización de la sesión por Perfil Activo** (propagación del perfil en el token JWT, activación automática en caso de perfil único, endpoints dedicados y adaptación para el Home del usuario).
3. **Evolución del modelo relacional y reglas de integridad dura en base de datos** (Migraciones Flyway `V14` a `V20`), incorporando triggers de no solapamiento, validaciones numéricas/DNI, auditoría de deshabilitación con scheduler de reactivación automática y garantías estructurales para proyectos (al menos un Director obligatorio).
4. **Reestructuración documental bajo Proceso Unificado (UP)** y generación de especificaciones técnicas y guías de integración para el frontend en Vue 3.

---

## 2. Hitos y Funcionalidades Desarrolladas (Por Módulo)

### 2.1. Gestión de Perfiles y Contexto de Sesión (`MOD-F-02`)
- **UC-13 Cambiar Perfil Activo y Contexto JWT (`25c9e20`, `d123cf7`, `be08dc9`):**
  - Incorporación de `idPerfilActivo` y `nombreArtistico` en el payload y claims del token JWT.
  - Selección automática por defecto de perfil activo si el usuario posee un único perfil al iniciar sesión o consultar el perfil.
  - Creación del servicio `ActivarPerfilService` y endpoint `PATCH /perfiles/{idPerfil}/activar`.
  - Endpoint `GET /perfiles/activo` para devolver los datos completos del perfil en uso en la sesión actual.
- **Preparación de Pantalla Home (`be08dc9`, `22955d1`):**
  - Implementación de `HomeController` y endpoint agregado `GET /home/resumen` con `HomeResumenResponse` para agilizar la carga del frontend.
- **Validación de Características Técnicas (`f86257e`, `6b26b33`):**
  - Prevención de valores negativos en características técnicas numéricas (`ValorNumericoNegativoException` y validación en `CaracteristicaPerfilHelper`).

### 2.2. Módulo de Disponibilidad y Calendario (`MOD-F-05` / `UC-17`, `UC-18`)
- **Gestión Integral de Agenda (`ab67324`, `25a505c`, `75df981`, `d04a524`):**
  - Creación de entidades `Agenda`, `JornadaAgenda` y repositorios `AgendaRepository`, `JornadaAgendaRepository` y `BloqueoAgendaRepository`.
  - Servicio de negocio `CalendarioService` y controlador `CalendarioController`.
  - Endpoints disponibles:
    - `GET /calendario`: Obtiene la agenda con configuración de jornadas, bloqueos manuales y bloqueos por actividades de proyectos.
    - `PUT /calendario/jornada`: Configuración de jornadas laborales por día de la semana y margen entre actividades.
    - `POST /calendario/bloqueos`: Registro de bloqueos manuales de no disponibilidad (`UC-18`).
    - `DELETE /calendario/bloqueos/{idBloqueo}`: Liberación de bloqueo manual (`UC-17`). Prohibición estricta de eliminar bloqueos de tipo actividad.
- **Soporte de Jornada Partida y Corrida (`232f06f`, `1fe3864`):**
  - Ampliación del modelo y DTOs para soportar turno mañana (`horaInicioManana`, `horaFinManana`) y turno tarde (`horaInicioTarde`, `horaFinTarde`).
  - Validación de coherencia de rangos y pausas intermedias obligatorias en jornadas partidas.

### 2.3. Gestión de Usuarios y Autenticación (`MOD-F-01`)
- **UC-04 Deshabilitar Usuario con Motivo y Temporalidad (`0b2086c`, `378f493`):**
  - Modificación del caso de uso: motivo obligatorio de deshabilitación (`motivo_deshabilitacion`) y fecha optativa de fin (`fecha_hasta_deshabilitacion`).
  - Creación del servicio `ExpirarDeshabilitacionService` y tarea programada con Spring Scheduler (`DeshabilitacionScheduler`) para rehabilitación automática al expirar el plazo.
- **Validaciones de Registro e Integridad (`0798974`, `b528a4c`):**
  - Control riguroso de DNI numérico no negativo (`DniInvalidoException`).
  - Validación de localidad obligatoria cuando se selecciona provincia en `UbicacionRequestValidator` (`ProvinciaSinLocalidadException`).
- **Seguridad y Seeding (`1eb3271`, `70128a5`):**
  - Seeding automático de usuario administrador al inicio del sistema mediante `AdminSeeder`.
  - Reorganización y refactorización modular del paquete de autenticación.

---

## 3. Evolución de la Base de Datos (Migraciones Flyway)

Durante este rango de commits se crearon y consolidaron las migraciones desde `V14` hasta `V20`:

| Migración | Archivo / Nombre | Objetivo e Impacto |
| :--- | :--- | :--- |
| **V14** | `V14__jornada_calendario.sql` | Normalización de agenda. Crea tabla `jornada_agenda`, elimina jornada rígida en tabla `agenda`, añade columna `margen_actividad`. Triggers para unicidad de agenda por usuario, impedimento de solapamiento de bloqueos e inmutabilidad de bloqueos por actividad. |
| **V15** | `V15__valor_caracteristica_no_negativo.sql` | Trigger de integridad en `perfil_caracteristica_tecnica` que bloquea valores numéricos negativos. |
| **V16** | `V16__margen_actividad_defecto.sql` | Establece un margen por defecto de 30 minutos entre actividades para la agenda. |
| **V17** | `V17__dni_no_negativo.sql` | Trigger en tabla `usuario` asegurando que el DNI esté compuesto exclusivamente por caracteres numéricos no negativos. |
| **V18** | `V18__deshabilitacion_motivo_duracion.sql` | Incorpora en la tabla `usuario` las columnas `motivo_deshabilitacion` (VARCHAR NOT NULL en deshabilitados) y `fecha_hasta_deshabilitacion` (DATETIME NULL). |
| **V19** | `V19__jornada_partida.sql` | Rediseño de `jornada_agenda` para albergar bloques de mañana (`hora_inicio_manana`, `hora_fin_manana`) y tarde (`hora_inicio_tarde`, `hora_fin_tarde`), con checks de coherencia horaria. |
| **V20** | `V20__al_menos_un_director_por_proyecto.sql` | Triggers y restricciones sobre `rol_proyecto` y `miembro_proyecto` asegurando que todo proyecto posea siempre al menos un miembro con rol 'Director'. |

> **Nota:** La base consolidada se sincronizó y renombró a `base_de_datos.sql` (`46e6fa1`, `4ae0a9f`, `7b39b61`).

---

## 4. Reestructuración Metodológica y Documentación

En los commits `88eecd1`, `a185aab`, `7b9c9ea` y `5884bbe`:
1. **Adopción de estructura UP en carpeta `/docs`:**
   - `0_nota_presentacion/`: Objetivos del sistema y notas de metodología.
   - `1_requisitos/`: Catálogo de casos de uso extendidos (`UC-01` a `UC-69`).
   - `2_analisis/`: Modelo de dominio técnico, DSS (Diagramas de Secuencia del Sistema) y Contratos de operaciones.
   - `3_diseño/`: Casos de uso reales, DSD (Diagramas de Secuencia de Diseño) y script SQL consolidado.
   - `4_pendiente/`: Auditoría de deuda técnica y trazabilidad.
   - `5_estado_sistema/`: Trazabilidad formal por versión y avance de casos de uso (`estado_uc.md`).
   - `6_presentaciones/`: Guiones y minutas para exposiciones y defensas del proyecto.
2. **Guías Técnicas para el Frontend (Vue 3):**
   - `ficha-tecnica-jornada-partida.md`: Reglas de negocio y contratos para configurar jornadas de corrido o partidas.
   - `guia-integracion-home.md`: Consumo del Home y perfil activo.
   - `guia-integracion-perfil-activo.md`: Flujo de autenticación con selección de perfil en JWT.

---

## 5. Calidad y Estrategia de Testing

Se añadieron suites exhaustivas de pruebas unitarias y de integración:
- **Unitarias:** `CalendarioServiceTest` (más de 500 líneas con casos de borde de solapamientos y jornadas), `CalendarioControllerTest`, `ActivarPerfilServiceTest`, `AdminUsuarioServiceTest` y `ExpirarDeshabilitacionServiceTest`.
- **Validaciones Bean Validation:** Tests para DTOs de jornada, bloqueos, deshabilitación y ubicación.
- **Integración con Base de Datos / Flyway:** `CalendarioRepositoryIntegrationTest` y `CalendarioActividadIntegrationTest`, validando la ejecución real de triggers PostgreSQL/MySQL para solapamientos y márgenes.

---

## 6. Historial de Commits Comprendidos en el Período

| Hash | Fecha | Mensaje de Commit |
| :--- | :---: | :--- |
| `25c9e20` | 2026-08-30 | Desarrollo de caso de uso UC-13 Cambiar perfil activo. Contexto JWT + servicio activo. |
| `9b6fba4` | 2026-08-30 | Incorporación de excepciones para los anteriores cambios |
| `eee754a` | 2026-08-30 | Modificación de los tests existentes según las nuevas modificaciones y se agregó el test de ActivarPerfilService |
| `d123cf7` | 2026-08-31 | Cambios mínimos en el token por errores molestos |
| `b528a4c` | 2026-08-31 | Solución de bug que no lanzaba excepción al momento de intentar cargar una provincia sin localidad |
| `61ee4e6` | 2026-08-31 | Actualización de tests en base al fix de ubicación |
| `1eb3271` | 2026-08-31 | Seedeo al administrador al inicio de la app |
| `ab67324` | 2026-09-01 | Migración V14 para el desarrollo del calendario de usuario (jornadas, margen, triggers) |
| `25a505c` | 2026-09-01 | Adición de clases para la gestión de calendarios dentro de la plataforma (repos, services, controllers) |
| `75df981` | 2026-09-01 | DTOs y mappers para la gestión de calendario, bloqueos manuales, por actividad y jornadas laborales |
| `6b26b33` | 2026-09-01 | Migraciones V15, V16 y V17 (característica no negativa, margen 30m, DNI no negativo) |
| `d04a524` | 2026-09-01 | Excepciones para el módulo de agenda de usuario |
| `99329b0` | 2026-09-01 | Tests para el módulo de gestión de calendario (unitarios y de integración) |
| `0798974` | 2026-09-01 | Validación del DNI al registrar un usuario. El DNI no puede ser negativo y debe ser un NÚMERO |
| `76f68ef` | 2026-09-01 | Reversión de los cambios para errores CSRF |
| `f86257e` | 2026-09-01 | Fix de bug que permitía que las características técnicas numéricas sean negativas |
| `f13e6c5` | 2026-09-01 | Tests para las modificaciones en las clases crear perfil y en la nueva clase incorporada |
| `9405fba` | 2026-09-01 | Actualización del estado de los casos de uso |
| `0b2086c` | 2026-09-02 | UC-04 Deshabilitar usuario: motivo OBLIGATORIO y duración OPCIONAL (Migración V18) |
| `f6bac5b` | 2026-09-02 | Agrego un TODO en el service de solicitud baja |
| `378f493` | 2026-09-02 | Actualización de tests en base a los cambios del caso de uso UC-04 |
| `7b39b61` | 2026-09-07 | Incorporación de la base de datos completa (actualizada con todas las migraciones flyway) |
| `70128a5` | 2026-09-07 | Reorganización de las carpetas del módulo 'auth' (refactorización de código) |
| `360e959` | 2026-09-07 | Actualización de los test a partir de la refactorización |
| `4ae0a9f` | 2026-09-07 | Actualizo la BD en base a las migraciones de flyway |
| `88eecd1` | 2026-09-07 | Adaptación de la carpeta /docs para el desarrollo con agentes (instrucciones y contexto UP) |
| `a185aab` | 2026-09-07 | Se agregan documentos para el seguimiento del estado del sistema, auditorías y guiones |
| `232f06f` | 2026-09-07 | V19 de la base de datos: jornadas 'partidas' o 'completas', chks de integridad y triggers |
| `46e6fa1` | 2026-09-07 | Cambio de nombre al archivo de bd.sql a base_de_datos.sql |
| `1fe3864` | 2026-09-07 | Modificación de jornada laboral: jornadas partidas o de corrido (DTOs, services, controllers, mappers) |
| `1ddd942` | 2026-09-07 | Actualización de los tests en base a las modificaciones del calendario |
| `7b9c9ea` | 2026-09-07 | Modificación del estado de los casos de uso e incorporación de una guía para el agente del frontend |
| `000b549` | 2026-09-07 | V20 de la base de datos: checks y triggers para garantizar al menos un director por proyecto |
| `be08dc9` | 2026-09-07 | Adaptación del backend para la adición de una pantalla de 'inicio' al activar un perfil |
| `22955d1` | 2026-09-07 | Incorporación de excepciones para pantalla de inicio y contexto de perfil |
| `8a4d342` | 2026-09-07 | Modificación y creación de tests para los cambios realizados |
| `5884bbe` | 2026-09-07 | Guía para el frontend (integración de Home y Perfil Activo) |

---

## 7. Próximos Pasos Identificados

- **Implementación del Módulo de Gestión de Proyectos (`MOD-F-06`):** Inicio con la creación de proyectos (`UC-24`), roles de proyecto con permisos granulares (migración `V21`), miembros y planificación asociada.
- **Interacción y Postulaciones:** Continuar con los casos de uso `UC-25` (Publicar proyecto) y convocatorias de integrantes.
