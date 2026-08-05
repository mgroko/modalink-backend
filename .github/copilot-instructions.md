# ModaLink — Contexto para Copilot

## Qué es este proyecto
ModaLink es una plataforma de networking profesional para la industria creativa/moda —
conceptualmente "un LinkedIn para producciones de moda". Proyecto académico final de la
materia de Ingeniería de Software, desarrollado con metodología UP (Proceso Unificado).

Funcionalidades core: gestión de perfiles múltiples, creación de proyectos, reclutamiento
de equipos, y agenda/scheduling entre actividades.

## Stack tecnológico
- **Backend:** Java + Spring Boot + Spring Security
- **Base de datos:** PostgreSQL
- **Frontend:** proyecto separado (SPA), consume el backend vía API REST
- Arquitectura: backend expone API REST, frontend es un cliente HTTP independiente
  (no server-side rendering, no Thymeleaf, no vistas del lado del backend)

## Estado actual
Fase de implementación. Ya existen: documento de requerimientos, diagrama de casos de uso,
diagramas de secuencia (DSS) para varios casos de uso, y modelo de datos relacional inicial
(prioriza tablas necesarias para el MVP — puede faltar algún atributo secundario).

## Reglas de dominio (fuente de verdad — no las reinterpretes ni las "mejores" sin preguntar)

- **Roles:** separados en `rol_global` (Administrador, Usuario) y `rol_proyecto`
  (Director, Miembro), cada uno con su propia tabla de permisos. No usar una tabla `rol`
  única con atributo de scope.
- Un Administrador global **no puede** tener perfil creativo ni crear proyectos.
- Un usuario puede ser Director de un proyecto y Miembro de otro simultáneamente.
- Un proyecto permite **un solo Director** (sin co-dirección) en esta etapa.
- `bloqueo_agenda` es **calculado/derivado**, no se persiste — un atraso en una actividad
  predecesora obliga a recalcular el bloque según días disponibles, nunca guardar un valor
  desactualizado.
- `requerimiento` = tipo y cantidad de profesionales que necesita una actividad
  (profesión + características técnicas + habilidades + cantidad).
- Un perfil puede tener 0..N postulaciones; varias postulaciones pueden apuntar al mismo
  requerimiento. `postulacion` tiene `fecha` y `estado`.
- Existe el rol de "colaborador general": miembro de proyecto sin actividad asignada
  (ej. asesor de imagen/estilo). Es igual en jerarquía a los demás miembros, pero queda
  fuera del grafo de dependencias entre actividades (ese grafo solo involucra a quienes
  tienen actividad asignada).
- Modelos conceptuales UML: sin flechas de navegabilidad, solo triángulo de dirección
  de lectura.

Si una tarea nueva contradice alguna de estas reglas, **avisame antes de implementar** —
no asumas que es un error mío para "corregir" silenciosamente.

## Cómo quiero trabajar con vos

- **Alcance acotado:** implementá exactamente lo que se pide en el prompt. Si para
  completar la tarea creés que hace falta tocar otro archivo/módulo no mencionado,
  explicámelo primero y esperá confirmación — no lo hagas de una.
- **No refactorices** código fuera del alcance de la tarea actual, aunque veas algo
  mejorable. Si querés sugerirlo, hacelo como comentario aparte, no como cambio aplicado.
- **No agregues dependencias/librerías nuevas** sin preguntar antes.
- **No generes archivos de configuración, tests, o documentación adicional** que no pedí
  explícitamente, salvo que la tarea lo requiera para funcionar.
- Para tareas grandes o poco claras, explicame tu plan antes de escribir código
  (usá plan mode si está disponible).
- Preferí explicaciones breves de *qué* hiciste y *por qué*, no un resumen extenso.
- Estoy aprendiendo el patrón API REST + SPA por primera vez (vengo de MVC con vistas
  HTML puras) — si el enfoque que estás usando es distinto a lo que yo conocía, marcámelo
  explícitamente en vez de asumir que ya lo entiendo.

## Convenciones del proyecto

- Nombres de tablas y columnas en español, snake_case (ej. `bloqueo_agenda`,
  `miembros_proyecto`).
- Entidades JPA en inglés/PascalCase estándar de Java cuando corresponda al código,
  pero respetando el nombre de tabla en español vía `@Table(name = "...")`.
- Separar claramente capa de controlador (REST endpoints), servicio (lógica de negocio)
  y repositorio (acceso a datos) — no lógica de negocio en el controlador.

## Comandos del proyecto
<!-- Completar cuando estén definidos -->
- Build backend: `mvn clean install`
- Correr backend: `mvn spring-boot:run`
- Tests backend: `mvn test`
- Build frontend: (a definir)
- Correr frontend en dev: (a definir)

## Qué NO hacer nunca
- No hardcodear credenciales ni strings de conexión — usar `application.properties` /
  variables de entorno.
- No hacer `git push` ni cambios de configuración de CI sin que yo lo pida explícitamente.
- No borrar ni modificar diagramas/documentación de la cátedra ya entregada, salvo pedido
  explícito.
