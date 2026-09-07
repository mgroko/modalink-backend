Apóstoles (Misiones), 20 de Abril de 2026

Al Prof. Adjunto de las cátedras  
Trabajo Final (ASC) | Proyecto Software (LSI)  
FCEQyN - UNaM  
Lic. Sergio Daniel Caballero  
S/D

De mi mayor consideración:

Me dirijo a Ud. a efectos de presentar la propuesta para el desarrollo de un producto software como tema para las cátedras “Proyecto Software” de la carrera Licenciatura en Sistemas de Información (plan de estudios 2013) y “Trabajo Final” de la carrera Analista en Sistemas de Computación (plan de estudios 2010).

El producto software a desarrollar se denomina: **ModaLink**

Sin otro particular, y quedando a la espera de la evaluación de la propuesta, me despido atte.

______________________________  
Roko María Guillermina  
Analista en Sistemas de Computación  
Licenciatura en Sistemas de Información  
45390594  

*Reservado para el equipo de cátedra*

## Evaluación de la propuesta

| Nombre del proyecto | ModaLink |
| :---- | :---- |
| Alumno/a | Roko María Guillermina |
| Fecha de la evaluación | |

| Aspecto | Evaluación |
| ----- | ----- |
| Presentación en general | |
| Objetivos | |
| Requisitos funcionales | |
| Requisitos no funcionales | |
| Procesos automáticos | 1. 2. 3. |
| Planificación | |
| Estrategia de validación / verificación | |
| Metodología | |

| Resultado final | APROBADO - DEBE CORREGIR |
| :---- | :---: |

*Firma por el equipo de cátedra:*

---

## Contenidos

1. [Planteo del problema](#planteo-del-problema)
2. [Introducción y Objetivos](#introducción-y-objetivos)
   - [Objetivos Generales](#objetivos-generales)
   - [Objetivos Específicos](#objetivos-específicos)
3. [Requisitos de Información (RI)](#requisitos-de-información-ri)
4. [Alcance y Limitaciones](#alcance-y-limitaciones)
5. [Especificación de Módulos](#especificación-de-módulos)
   - [Módulos Funcionales (MOD-F)](#módulos-funcionales-mod-f)
   - [Módulos No Funcionales (MOD-NF)](#módulos-no-funcionales-mod-nf)
6. [Actores y Matriz Preliminar de Alcance](#actores-y-matriz-preliminar-de-alcance)
7. [Procesos Automatizados](#procesos-automatizados)
8. [Estimación de Tamaño por Módulo](#estimación-de-tamaño-por-módulo)
9. [Entorno Tecnológico y Metodológico](#entorno-tecnológico-y-metodológico)
10. [Planificación de Actividades (UP)](#planificación-de-actividades)

---

## Planteo del problema

Actualmente, la gestión de producciones de moda se realiza de forma informal y descentralizada, la búsqueda de talento y la coordinación del equipo se realiza por medio de redes sociales genéricas. Esta modalidad genera retrasos en producciones y problemas en la coordinación.

Por otro lado, estas plataformas presentan limitaciones críticas al momento de exponer portfolios técnicos. La ausencia de tecnicismo resulta en búsquedas fragmentadas de colaboradores, invisibilidad de talento cualificado y frustración entre profesionales.

---

## Introducción y objetivos

En Misiones hay cada vez más profesionales dedicados a las Industrias Creativas y de la Moda. Esta creciente demanda hace que la centralización de profesionales, la gestión integral de producciones de moda y la posibilidad de un portfolio técnico dinámico sean cada vez más importantes para una experiencia profesional. Bajo este contexto, muchos profesionales aún utilizan plataformas genéricas para su trabajo, generando tareas repetitivas, procesos lentos y frustración.

El presente proyecto propone el desarrollo de una Plataforma de Networking para las Industrias Creativas y de la Moda (**ModaLink**). Esta plataforma permitirá a los usuarios un portfolio con características técnicas específicas, un calendario de disponibilidad, la posibilidad de conectar con otros profesionales y la gestión integral de proyectos profesionales.

### Objetivos Generales

- **OBJ-01**: Optimizar la gestión integral y planificación de proyectos profesionales en la industria creativa y de la moda.
- **OBJ-02**: Reducir los tiempos de búsqueda y selección de talento especializado mediante filtrado técnico y agendas centralizadas.
- **OBJ-03**: Facilitar la comunicación y vinculación formal entre profesionales del sector.
- **OBJ-04**: Potenciar la visibilidad, reputación y posicionamiento de los integrantes del sector mediante portfolios técnicos y colaboraciones verificadas.
- **OBJ-05**: Mejorar la experiencia del usuario y la formalización documental en el entorno de trabajo colaborativo.

### Objetivos Específicos

- **OBJ-ESP-01**: Implementar un sistema de autenticación seguro con control de roles globales y de proyecto.
- **OBJ-ESP-02**: Digitalizar currículums profesionales basados en características técnicas parametrizadas por profesión.
- **OBJ-ESP-03**: Proporcionar un calendario sincronizado con cálculo automático de márgenes y soporte de jornada corrida y partida.
- **OBJ-ESP-04**: Facilitar la creación y seguimiento de proyectos con gestión de actividades, cronograma y dependencias.
- **OBJ-ESP-05**: Automatizar el reemplazo de profesionales ante bajas y la detección de publicaciones en colaboración.
- **OBJ-ESP-06**: Establecer una base documental para la emisión, seguimiento y firma de contratos de cesión de imagen y préstamo de prendas.

---

## Requisitos de Información (RI)

- **RI-01 (Datos de Usuario y Cuenta)**: Almacena credenciales, estado (Habilitado, Deshabilitado, Pendiente de baja), motivo y duración de deshabilitación, proveedor de autenticación (`LOCAL`, `GOOGLE`), datos personales (DNI, nombre, apellido, fecha de nacimiento), género y ubicación geográfica (`usuario`, `genero`, `ubicacion`).
- **RI-02 (Perfiles Profesionales)**: Registra los perfiles artísticos de los usuarios, biografía, foto, estado y fecha de baja lógica (`perfil`).
- **RI-03 (Catálogo de Profesiones y Especialidades)**: Catálogo oficial de profesiones y descripción de roles en el rubro creativo (`profesion`).
- **RI-04 (Características Técnicas y Habilidades)**: Especificaciones físicas y técnicas por profesión (altura, talles, medidas, color de ojos, etc.) con sus tipos de datos y unidades, así como habilidades declaradas (`caracteristica_tecnica`, `valor_caracteristica`, `caracteristica_perfil`, `habilidad`, `habilidad_perfil`).
- **RI-05 (Disponibilidad y Calendario)**: Configuración de la jornada laboral de agenda (mañana/tarde, margen entre actividades) y bloqueos manuales temporales (`agenda`, `jornada_agenda`, `bloqueo_agenda`).
- **RI-06 (Proyectos y Planificación)**: Datos de proyectos (nombre, descripción, estado, privacidad, fechas, ubicación), objetivos y su planificación asociada (`proyecto`, `objetivo`, `planificacion`).
- **RI-07 (Actividades y Cronograma)**: Actividades del proyecto, duración en minutos, fecha/hora inicio, descripción, ubicación y relaciones de precedencia (`actividad`, `dependencia_actividades`).
- **RI-08 (Requerimientos de Personal)**: Necesidades de talento tanto a nivel global del proyecto como por actividad específica, con filtros por profesión, habilidades y características requeridas (`requerimiento_gral_proyecto`, `requerimiento_actividad`, `requerimiento_gral_caract`, `requerimiento_act_caract`, `requerimiento_gral_habilidad`, `requerimiento_act_habilidad`).
- **RI-09 (Miembros y Asignaciones)**: Asignación de integrantes al equipo del proyecto con sus respectivos roles y asignaciones directas a actividades planificadas (`miembros_proyecto`, `asignacion_actividad`).
- **RI-10 (Postulaciones e Invitaciones)**: Trazabilidad de solicitudes de incorporación y de postulaciones directas a actividades o generales (`postulacion_gral`, `postulacion_actividad`, `invitacion_gral`, `invitacion_actividad`).
- **RI-11 (Portfolios y Publicaciones)**: Publicaciones con contenido multimedia vinculado a un proyecto o perfil, créditos de colaboración y solicitudes de coautoría (`publicacion`, `imagen`, `imagen_publicacion`, `colaborador_publicacion`, `solicitud_colaboracion`).
- **RI-12 (Interacciones Sociales)**: Registro de interacciones en publicaciones (`me_gusta`, `comentario`).
- **RI-13 (Moodboards y Referencias Visuales)**: Colección de imágenes conceptuales asociadas a los proyectos (`moodboard`, `imagen_moodboard`).
- **RI-14 (Seguridad y Permisos)**: Catálogo de permisos globales y permisos específicos de proyecto asignados a cada rol (`rol_global`, `permiso_global`, `rol_global_permiso`, `rol_proyecto`, `permiso_proyecto`, `rol_proyecto_permiso`).

---

## Alcance y limitaciones

### Alcance
- **Registro y autenticación de usuarios:** Creación de cuenta con nombre, apellido, correo electrónico, DNI, fecha de nacimiento, ubicación y contraseña cifrada.
- **Gestión de perfiles:** Personalización de perfil por profesión, carga de atributos técnicos y habilidades específicas, y selección de foto principal.
- **Visualización y búsqueda de perfiles:** Búsqueda técnica filtrando por profesión, características y disponibilidad.
- **Gestión de disponibilidad:** Configuración de jornada corrida o partida con márgenes y agenda de bloqueos sin solapamiento.
- **Planificación de proyectos:** Creación de proyectos (públicos/privados), moodboards, cronograma con dependencias entre actividades y gestión del equipo.
- **Gestión de postulaciones e invitaciones:** Flujo de convocatoria para el armado de producciones.
- **Publicaciones y portfolio dinámico:** Publicación de trabajos realizados asociados o no a proyectos.
- **Colaboraciones automáticas:** Detección de imágenes compartidas y solicitud de validación de coautoría.
- **Autenticación con Google:** Soporte de inicio federado.

### Limitaciones
- **Transacciones financieras:** No se procesarán cobros ni pagos dentro de la plataforma en esta fase.
- **Validez legal de firma digital:** No reemplaza firmas digitales con certificado de autoridad (PKI); se trata de firma electrónica interna de aceptación.
- **Plataforma:** Aplicación web responsiva (sin cliente nativo móvil).
- **Material audiovisual:** Soporte enfocado en imágenes (JPG, PNG, WebP). Sin transcodificación de video/audio en el servidor.
- **Moderación:** No se aplican filtros predictivos automáticos de IA sobre contenidos de texto o imágenes.

---

## Especificación de módulos

### Módulos Funcionales (MOD-F)

- **MOD-F-01: Módulo de Gestión de Usuarios**: Administra el registro, autenticación, habilitación/deshabilitación administrativa, solicitud y reactivación de baja, y control de roles globales (`Administrador`, `Usuario`).
- **MOD-F-02: Módulo de Gestión de Perfiles**: Administra perfiles profesionales, profesiones, características técnicas y habilidades específicas por disciplina.
- **MOD-F-03: Módulo de Gestión de Publicaciones**: Administra publicaciones de portfolio, imágenes y detección de coautorías.
- **MOD-F-04: Módulo de Interacción**: Gestiona solicitudes de colaboración, comentarios, me gusta y mensajería/red profesional.
- **MOD-F-05: Módulo de Gestión de Disponibilidad**: Gestiona la agenda de cada usuario, jornadas laborales de corrido o partida, margen entre actividades y bloqueos manuales.
- **MOD-F-06: Módulo de Gestión de Proyectos**: Coordina la creación de proyectos, planificación temporal, grafo de dependencias entre actividades, moodboards, requerimientos y asignación de equipo con roles de proyecto (`Director`, `Miembro`).
- **MOD-F-07: Módulo de Gestión de Convocatorias y Postulaciones**: Gestiona postulaciones e invitaciones generales y específicas a actividades.
- **MOD-F-08: Módulo de Gestión de Contratos**: Emisión documental y flujo de estados de contratos de uso de imagen y préstamo de prendas.
- **MOD-F-09: Módulo de Feedback y Reputación**: Valoraciones posteriores al cierre de proyectos y colaboraciones.

### Módulos No Funcionales (MOD-NF)

- **MOD-NF-01: Seguridad**: Cifrado BCrypt, tokens JWT vía cookies httpOnly, prevención CSRF y control RBAC en dos niveles (global y por proyecto).
- **MOD-NF-02: Integridad y Persistencia (SQL Canónico)**: Doble capa de validación (constraints/triggers en PostgreSQL + excepciones de dominio tipificadas en Spring Boot).
- **MOD-NF-03: Auditoría**: Trazabilidad temporal de altas, modificaciones, solicitudes de baja y deshabilitaciones administrativas.
- **MOD-NF-04: Diseño Responsivo**: Adaptabilidad completa a interfaces de escritorio y dispositivos móviles vía web.
- **MOD-NF-05: Rendimiento**: Tiempos de respuesta inferiores a 200ms en endpoints transaccionales y de consulta de disponibilidad.
- **MOD-NF-06: Mantenibilidad y Calidad**: Arquitectura desacoplada basada en el Proceso Unificado, cobertura JaCoCo superior al 80% y suite de pruebas JUnit 5 de caja negra y blanca.
- **MOD-NF-07: Usabilidad**: Navegación consistente, feedback claro de validaciones y experiencia fluida para usuarios creativos.

---

## Actores y Matriz Preliminar de Alcance

| Actor | Descripción | Alcance en el Sistema |
| :--- | :--- | :--- |
| **Usuario No Autenticado** | Visitante de la plataforma web. | Puede registrarse, autenticarse y consultar publicaciones/proyectos públicos. |
| **Usuario Autenticado** | Profesional registrado en el sistema. | Gestiona su perfil, publicaciones, agenda, se postula a proyectos y recibe invitaciones. |
| **Director de Proyecto** | Miembro con rol de dirección en un proyecto específico. | Planifica actividades, define requerimientos, aprueba postulaciones, envía invitaciones y cancela/finaliza el proyecto. |
| **Miembro de Proyecto** | Profesional incorporado formalmente al equipo de un proyecto. | Visualiza el cronograma, sus actividades asignadas y puede solicitar su desvinculación. |
| **Administrador** | Administrador global del sistema. | Gestiona usuarios (habilitar/deshabilitar/auditoría), catálogo de profesiones y características técnicas. No posee perfil creativo ni crea proyectos propios. |

---

## Procesos automatizados

| Denominación | Módulos que intervienen | Descripción del proceso |
| ----- | ----- | ----- |
| **Reemplazo automático** | MOD-F-02, MOD-F-05, MOD-F-06, MOD-F-07 | Se activa cuando un miembro asignado se da de baja de un proyecto confirmado. El sistema busca perfiles que cumplan los requerimientos de la actividad y posean disponibilidad en su agenda, proponiendo candidatos al Director de Proyecto. |
| **Colaboraciones automáticas** | MOD-F-02, MOD-F-03, MOD-F-04 | Se activa al registrar una imagen existente en el repositorio. El sistema identifica coincidencias y envía una solicitud de confirmación de coautoría a los perfiles involucrados. |
| **Reactivación automática por vencimiento** | MOD-F-01, MOD-NF-03 | Al vencer la fecha establecida en una deshabilitación temporal (`fecha_hasta_deshabilitacion`), el sistema restituye automáticamente el estado habilitado del usuario. |

---

## Estimación de tamaño por módulo

| Módulo | Porcentaje de participación / Producto |
| ----- | ----- |
| MOD-F-01: Gestión de Usuarios | 10% |
| MOD-F-02: Gestión de Perfiles | 20% |
| MOD-F-03 y MOD-F-04: Publicaciones e Interacción | 15% |
| MOD-F-05: Gestión de Disponibilidad | 10% |
| MOD-F-06 y MOD-F-07: Proyectos, Planificación y Convocatorias | 30% |
| MOD-F-08: Gestión de Contratos | 10% |
| MOD-F-09: Feedback y Reputación | 5% |
| **Total** | **100%** |

---

## Entorno tecnológico y metodológico

| Componente | Especificación |
| :---- | :---- |
| **Lenguaje Backend:** | Java 25 |
| **Framework Backend:** | Spring Boot 4.1.0, Spring Security |
| **Persistencia & Migraciones:** | PostgreSQL, Flyway (`V1` a `V19+`) |
| **Seguridad:** | JWT (JJWT 0.13.0, HttpOnly Cookie), CSRF token repository, BCrypt |
| **Testing & Cobertura:** | JUnit 5, AssertJ, Mockito, JaCoCo (mínimo 80%) |
| **Arquitectura:** | Cliente - Servidor RESTful por capas |
| **Metodología seleccionada:** | Proceso Unificado (UP) con enfoque iterativo e incremental |

---

## Planificación de actividades

| Actividad | Fecha de inicio | Fecha de finalización |
| ----- | ----- | ----- |
| **FASE DE INICIO** | | |
| Relevamiento de datos y refinamiento de requerimientos | 20/04/2026 | 28/04/2026 |
| **FASE DE ELABORACIÓN** | | |
| Elaboración de Diagramas de Casos de Uso | 29/04/2026 | 07/05/2026 |
| Definición de Casos de Uso Extendidos | 08/05/2026 | 18/05/2026 |
| Diagramas de Secuencia de Sistema (DSS) | 19/05/2026 | 05/06/2026 |
| Contratos de Operación | 08/06/2026 | 25/06/2026 |
| Diagramas de Secuencia de Diseño (DSD) | 26/06/2026 | 15/07/2026 |
| Diseño de Base de Datos y Mapeo Físico | 16/07/2026 | 04/08/2026 |
| Diseño de Interfaces de Usuario (UI/UX Mockups) | 05/08/2026 | 24/08/2026 |
| **FASE DE CONSTRUCCIÓN** | | |
| Desarrollo e implementación incremental guiada por pruebas | 25/08/2026 | 10/11/2026 |
| **FASE DE TRANSICIÓN** | | |
| Diseño y ejecución de Casos de Prueba (Caja Negra / Blanca) | 13/10/2026 | 21/10/2026 |
| Diseño de Manuales de Usuario y Auditoría Final | 13/10/2026 | 21/10/2026 |
| Ejecución de pruebas integrales y corrección de errores | 22/10/2026 | 02/11/2026 |
| **ENTREGA FINAL Y DEFENSA** | **15/11/2026** | |
