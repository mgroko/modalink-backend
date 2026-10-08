# Estado del Sistema — Versión 0.3

> **Fase 5: Estado del Sistema (Proceso Unificado)**  
> **Fecha de Corte**: 13 de Septiembre de 2026  
> **Período de Análisis**: Commits `5884bbe` (07-Sep-2026) a `f82f41c` (13-Sep-2026) + cambios de sesión de trabajo  
> **Versión**: v0.3  
> **Fuente de Verdad de BD**: `base_de_datos.sql` / Migraciones Flyway `V1` a `V23`

---

## 1. Resumen Ejecutivo de la Versión

La versión **v0.3** consolida la expansión del sistema **ModaLink** hacia el **Módulo de Gestión de Proyectos (`MOD-F-06`)**, la finalización de capacidades clave en la **Gestión de Perfiles (`MOD-F-02`) y Usuarios (`MOD-F-01`)**, la infraestructura de almacenamiento y procesamiento de imágenes, y la incorporación de parámetros de configuración dinámicos administrados en base de datos.

### Principales Logros Integrados en esta Versión:
1. **Inicio e Infraestructura del Módulo de Proyectos (`MOD-F-06`):**
   - Implementación completa de `UC-24: Crear proyecto` en estado `Borrador`, asignación automática del creador como `Director` y generación de la instancia inicial de `Planificación`.
   - Reglas de integridad relacional: control de nombres duplicados, validación de fechas, y roles de proyecto con permisos granulares (Migración Flyway `V21`).
   - Diseño e introducción del modelo relacional para **Hitos de Proyecto y Notificaciones** (Migración Flyway `V23`), garantizando reglas de no superación de fecha de entrega y estado de hitos/actividades.
2. **Consolidación de la Gestión y Búsqueda de Perfiles y Usuarios:**
   - Implementación y testeo integral de `UC-14: Ver perfil` (consulta pública y privada de perfiles con sus métricas y catálogo).
   - Implementación y testeo de `UC-16: Buscar perfil` (filtros avanzados por profesión, características técnicas, habilidades y ubicación).
   - Implementación y testeo de `UC-06: Buscar usuario` desde el panel de administración con paginación y filtros.
3. **Servicio de Almacenamiento y Optimización Multimedia:**
   - Sistema de guardado y gestión de fotos de perfil (`UC-10` / `UC-11`): redimensionado, compresión y optimización en servidor con validación MIME.
4. **Configuración Dinámica del Sistema (Panel de Administración):**
   - Creación de la tabla `configuracion_sistema` (Migración Flyway `V22`) y permiso `ADMINISTRAR_CONFIGURACION`.
   - Parametrización del Scheduler de reactivación automática por deshabilitación (previamente prefijado a las 02:00 AM, ahora configurable dinámicamente).
5. **Calendario y Disponibilidad Pública:**
   - Endpoint público de calendario (`GET /calendario/perfil/{id}`), permitiendo consultar la disponibilidad de un usuario protegiendo la privacidad de los motivos de bloqueos manuales.
6. **Evolución Documental y Sincronización Frontend (Vue 3):**
   - Actualización de los Casos de Uso Extendidos (incorporación formal de `UC-70`, `UC-71`, `UC-72`, `UC-73`).
   - Formalización de los Diagramas de Secuencia del Sistema (DSS) en Markdown unificado.
   - Publicación de Fichas Técnicas de Integración para el Frontend: creación de proyectos, búsqueda de perfiles (`UC-16`), búsqueda de usuarios (`UC-06`), visualización de perfiles (`UC-14`) y panel de configuración de administrador.

---

## 2. Matriz General de Avance de Casos de Uso

A continuación se detalla el avance metodológico de los casos de uso relevados en el sistema bajo las cuatro fases del Proceso Unificado (UP):
- **Documentado (Extendido)**: Ficha de caso de uso con secuencia normal, alternativa, excepciones y requisitos de información.
- **Analizado (DSS + Contrato)**: Diagrama de secuencia del sistema y contrato formal de operaciones del sistema.
- **Diseñado (Real + DSD)**: Caso de uso real con navegación/pantallas y diagrama de secuencia de diseño con asignación de patrones GRASP.
- **Implementado / En Pruebas**: Código desarrollado en controladores, servicios, repositorios, entidades JPA y suite de tests unitarios/integración.

### 2.1. Gestión de Usuarios y Administración (`MOD-F-01`)

| Caso de Uso | Nombre | Documentado (Extendido) | Analizado (DSS + Contrato) | Diseñado (Real + DSD) | Implementado / En Pruebas |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **UC-01** | Iniciar sesión | Sí | Sí | Sí | Implementado y testeado |
| **UC-02** | Cerrar sesión | Sí | No | No | Implementado y testeado |
| **UC-03** | Registrarse | Sí | Sí | Sí | Implementado y testeado |
| **UC-04** | Deshabilitar usuario | Sí | Sí | Sí | Implementado y testeado |
| **UC-05** | Habilitar usuario | Sí | Sí (DSS) | No | Implementado y testeado |
| **UC-06** | Buscar usuario | Sí | No | No | Implementado y testeado |
| **UC-07** | Solicitar baja en el sistema | Sí | Sí (DSS) | No | Implementado y testeado |
| **UC-08** | Modificar datos personales | Sí | Sí (DSS) | No | Implementado y testeado |
| **UC-09** | Autenticar mediante Google OAuth | Sí | Sí (DSS) | No | No iniciado |
| **UC-56** | Gestionar habilidades del sistema | Sí | No | No | No iniciado |
| **UC-57** | Gestionar características técnicas por profesión | Sí | No | No | Implementado |
| **UC-58** | Buscar características técnicas | Sí | No | No | Implementado y testeado |
| **UC-59** | Buscar profesiones | Sí | No | No | Implementado y testeado |
| **UC-67** | Generar informe de auditoría | Sí | No | No | No iniciado |
| **UC-70\*** | Recuperar contraseña / Configuración Admin | Sí | No | No | Implementado y testeado (Config) |

\* *Nota*: El catálogo extendido asigna a recuperación de contraseña este código, mientras que en la gestión de configuración del sistema se introdujo la administración de variables y schedulers.

### 2.2. Gestión de Perfiles y Portfolio (`MOD-F-02`)

| Caso de Uso | Nombre | Documentado (Extendido) | Analizado (DSS + Contrato) | Diseñado (Real + DSD) | Implementado / En Pruebas |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **UC-10** | Crear perfil | Sí | Sí | Sí | Implementado y testeado |
| **UC-11** | Editar perfil | Sí | Sí (DSS) | No | En progreso (Servicio/DTO/Fotos) |
| **UC-12** | Eliminar perfil | Sí | Sí | No | En progreso (Servicio/DTO) |
| **UC-13** | Cambiar perfil activo | Sí | Sí (DSS) | No | Implementado y testeado |
| **UC-14** | Ver perfil | Sí | No | No | Implementado y testeado |
| **UC-15** | Reportar perfil | Sí | Sí (DSS) | No | No iniciado |
| **UC-16** | Buscar perfil | Sí | Sí (DSS) | No | Implementado y testeado |
| **UC-23** | Configurar términos y condiciones | Sí | No | No | No iniciado |
| **UC-47** | Gestionar habilidades | Sí | No | No | No iniciado |
| **UC-48** | Buscar habilidades | Sí | No | No | No iniciado |

### 2.3. Gestión de Disponibilidad y Agenda (`MOD-F-05`)

| Caso de Uso | Nombre | Documentado (Extendido) | Analizado (DSS + Contrato) | Diseñado (Real + DSD) | Implementado / En Pruebas |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **UC-17** | Asignar en calendario día disponible | Sí | Sí (DSS) | Sí (CUR-17/18) | Implementado y testeado |
| **UC-18** | Asignar en calendario día no disponible | Sí | Sí | Sí (DSD + CUR) | Implementado y testeado |
| **UC-71** | Configurar jornada laboral | Sí | Sí (DSS) | Sí | Implementado y testeado |
| **-** | Obtener calendario público de perfil | Sí | Sí | Sí | Implementado y testeado |

### 2.4. Gestión de Proyectos y Convocatorias (`MOD-F-06` y `MOD-F-07`)

| Caso de Uso | Nombre | Documentado (Extendido) | Analizado (DSS + Contrato) | Diseñado (Real + DSD) | Implementado / En Pruebas |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **UC-24** | Crear proyecto | Sí | Sí | Sí (CUR-24) | Implementado y testeado |
| **UC-25** | Publicar proyecto | Sí | Sí (DSS) | No | No iniciado |
| **UC-26** | Dar de alta postulación a proyecto | Sí | Sí (DSS) | No | No iniciado |
| **UC-27** | Dar de baja postulación a proyecto | Sí | No | No | No iniciado |
| **UC-28** | Aceptar solicitud de incorporación | Sí | Sí (DSS) | No | No iniciado |
| **UC-29** | Buscar proyecto | Sí | Sí (DSS) | No | No iniciado |
| **UC-30** | Buscar invitaciones | Sí | Sí (DSS) | No | No iniciado |
| **UC-31** | Invitar a proyecto | Sí | Sí (DSS) | No | No iniciado |
| **UC-32** | Eliminar invitación a proyecto | Sí | Sí (DSS) | No | No iniciado |
| **UC-33** | Visualizar cronograma de proyecto | Sí | Sí (DSS) | No | No iniciado |
| **UC-34** | Gestionar postulaciones | Sí | Sí (DSS) | No | No iniciado |
| **UC-35** | Cancelar proyecto | Sí | Sí (DSS) | No | No iniciado |
| **UC-36** | Modificar proyecto | Sí | Sí (DSS) | No | No iniciado |
| **UC-37** | Finalizar proyecto | Sí | Sí (DSS) | No | No iniciado |
| **UC-38** | Eliminar integrante | Sí | Sí (DSS) | No | No iniciado |
| **UC-40** | Darse de baja de proyecto | Sí | No | No | No iniciado |
| **UC-49** | Crear actividad en planificación | Sí | Sí | No | No iniciado |
| **UC-50** | Modificar actividad de planificación | Sí | Sí (DSS) | No | No iniciado |
| **UC-51** | Eliminar actividad de planificación | Sí | Sí (DSS) | No | No iniciado |
| **UC-52** | Buscar actividades | Sí | Sí (DSS) | No | No iniciado |
| **UC-60** | Confirmar proyecto | Sí | Sí (DSS) | No | No iniciado |
| **UC-70 (P)** | Asignar actividad a miembro | Sí | Sí (DSS) | No | No iniciado |
| **UC-72** | Buscar requerimiento general | Sí | Sí (DSS) | No | No iniciado |
| **UC-73** | Buscar requerimiento actividad | Sí | Sí (DSS) | No | No iniciado |

### 2.5. Gestión de Publicaciones (`MOD-F-03`), Contratos (`MOD-F-08`) y Conexiones (`MOD-F-04`)

| Módulo | Casos de Uso | Documentado (Extendido) | Analizado (DSS + Contrato) | Diseñado (Real + DSD) | Implementado / En Pruebas |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Publicaciones** | UC-19 a UC-22, UC-61 a UC-66, UC-69 | 100% (11/11) | 0% | 0% | No iniciado (Segunda iteración) |
| **Contratos** | UC-39, UC-53 a UC-55, UC-68 | 100% (5/5) | 0% | 0% | No iniciado (Segunda iteración) |
| **Conexiones y Mensajería** | UC-41 a UC-46 | 100% (6/6) | 0% | 0% | No iniciado (Segunda iteración) |

---

## 3. Correcciones Arquitectónicas Introducidas Respecto a la Versión Anterior (v0.2)

Respecto a la línea base de la versión **v0.2**, se introdujeron las siguientes mejoras y correcciones de arquitectura:

1. **Permisos Granulares de Proyecto a Nivel de Datos (Migración `V21`):**
   - Se desacopló la autorización de proyectos del rol global del usuario.
   - Creación de 14 permisos granulares en `permiso_proyecto` (`VER_PROYECTO`, `MODIFICAR_PROYECTO`, `PUBLICAR_PROYECTO`, `CREAR_ACTIVIDAD`, etc.) asignados selectivamente al `Director` y al `Miembro`.
2. **Parametrización Dinámica de Tareas Programadas (Migración `V22`):**
   - Sustitución de expresiones cron y horas fijas ("hardcodeadas") en el código Java (`@Scheduled(cron = "0 0 2 * * *")`) por una arquitectura basada en `configuracion_sistema`.
   - Se habilitó la lectura dinámica de configuración permitiendo al Administrador modificar la hora/minuto del scheduler sin recompilar ni reiniciar la aplicación.
3. **Modelo de Integridad para Hitos y Notificaciones (Migración `V23`):**
   - Creación de tablas `hito`, `hito_actividad` y la tabla genérica desacoplada `notificacion`.
   - Triggers de integridad referencial dura:
     - `trg_validar_fecha_limite_hito`: impide que un hito tenga fecha límite posterior a la fecha de entrega de la planificación del proyecto.
     - `trg_validar_borrado_hito`: previene la eliminación de hitos que posean actividades enlazadas.
     - `trg_cambio_estado_hito`: deriva automáticamente el estado del hito en función de la finalización de sus actividades asociadas.
4. **Módulo Centralizado de Almacenamiento y Optimización de Archivos (`StorageService`):**
   - Se desacopló la recepción del archivo HTTP multipart del almacenamiento físico.
   - Incorporación de procesamiento automático de imágenes (redimensionamiento a resoluciones estándar y compresión de peso) para resguardar el consumo de ancho de banda y almacenamiento del servidor.
5. **Políticas de Privacidad en la Exposición de Calendario:**
   - Corrección arquitectónica en la capa de presentación de agenda: distinción entre la vista privada del dueño del perfil (`CalendarioController.obtener()`) y la consulta pública de terceros (`CalendarioController.obtenerPorPerfil()`), donde los motivos de los bloqueos personales son enmascarados de forma segura mediante `toBloqueoResponseAnonimizado`.

---

## 4. Historial de Commits Comprendidos en el Período

| Hash | Fecha | Mensaje de Commit / Descripción del Cambio |
| :--- | :---: | :--- |
| `9774d5d` | 2026-09-07 | Merge pull request #10 from mgroko/modulo-perfiles |
| `68ada7c` | 2026-09-08 | V21 de la bd. Inserción de permisos en permiso_proyecto. Asignación de permisos a Director y a Miembro de proyecto |
| `d9d9298` | 2026-09-08 | Implementación del servicio de creación de proyecto, capa de seguridad, controlador para proyecto, dtos y mappers. Repositorios de miembros, roles, planificación y proyecto |
| `0e9279f` | 2026-09-08 | Request de crear proyecto |
| `3a0688e` | 2026-09-08 | Gestión de objetivos de proyecto |
| `cd48f8d` | 2026-09-08 | Excepciones para la creación de proyecto: acceso denegado, nombre duplicado, rango de fechas inválido y rol no encontrado |
| `3f92e46` | 2026-09-08 | Actualización del documento estado_uc.md y adición de lo desarrollado para la guía del frontend |
| `f577954` | 2026-09-08 | Tests para la creación de proyecto (unitarios e integración) |
| `5bba08f` | 2026-09-08 | Merge pull request #11 from mgroko/modulo-perfiles |
| `1e13434` | 2026-09-08 | Merge pull request #12 from mgroko/main |
| `9d80401` | 2026-09-08 | Migración V22 de la base de datos. Permiso ADMINISTRAR_CONFIGURACION y creación de tabla configuracion_sistema |
| `0f8db9c` | 2026-09-08 | Clases para gestión de configuración desde panel de administrador y desacople dinámico del Scheduler |
| `71112df` | 2026-09-08 | Tests para la configuración del sistema |
| `2596b8f` | 2026-09-08 | Actualización de la documentación de requerimientos: casos de uso extendidos |
| `c6b3b06` | 2026-09-08 | Instrucciones para la aplicación de tests y controles en el proyecto |
| `e2f44d5` | 2026-09-08 | Guía para el frontend para la configuración del panel de administrador |
| `52946e6` | 2026-09-08 | Creación de los DSS en formato .md formal |
| `a21649a` | 2026-09-09 | Migración V23 de hitos de proyecto, derivación de estado y tabla genérica de notificaciones |
| `4e32d31` | 2026-09-11 | Implementación de guardado de imágenes en el servidor junto a redimensionado y optimización |
| `b28b280` | 2026-09-11 | Implementación de funcionalidades para la gestión de fotos de perfil |
| `3aba838` | 2026-09-11 | Excepciones necesarias para nuevas implementaciones de perfiles y storage |
| `28ca48e` | 2026-09-11 | Incorporación de tests correspondientes a la subida de fotos de perfil y storage |
| `be099fb` | 2026-09-11 | Ficha técnica para el frontend: Creación de proyectos |
| `01ff3be` | 2026-09-12 | Implementación del caso de uso UC-16 Buscar perfil |
| `a698c3d` | 2026-09-12 | Implementación de tests para el caso de uso UC-16 Buscar perfil |
| `81f05c2` | 2026-09-12 | Actualización de estado de casos de uso, archivo de DSS y ficha técnica frontend |
| `bec9cd6` | 2026-09-13 | Implementación del caso de uso UC-06 Buscar usuario |
| `a415c6d` | 2026-09-13 | Tests correspondientes a la adición del caso de uso UC-06 |
| `8315d91` | 2026-09-13 | Ficha técnica para el frontend para el desarrollo de UC-06 |
| `b9964d4` | 2026-09-13 | Implementación del caso de uso UC-14 Ver perfil |
| `62003b3` | 2026-09-13 | Tests que corresponden a la implementación del caso de uso UC-14 |
| `f82f41c` | 2026-09-13 | Actualización de estado de casos de uso y ficha técnica para el frontend |
| *WIP* | 2026-09-13 | Implementación de endpoint de calendario público por perfil (`GET /calendario/perfil/{id}`) y suite de tests |

---

## 5. Próximos Pasos Identificados

1. **Continuación de la Gestión de Proyectos (`MOD-F-06`):**
   - Implementar `UC-25: Publicar proyecto` y `UC-36: Modificar proyecto`.
   - Implementar `UC-49: Crear actividad en planificación` vinculando requerimientos de profesiones y validación con el cronograma.
2. **Módulo de Convocatorias y Postulaciones (`MOD-F-07`):**
   - Implementar `UC-26: Dar de alta postulación a proyecto` y `UC-34: Gestionar postulaciones`.
3. **Consolidación del Motor de Notificaciones:**
   - Exponer y poblar las notificaciones generadas en la tabla `notificacion` (hitos cumplidos, vencimientos, invitaciones).
