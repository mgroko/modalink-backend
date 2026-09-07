# Guion de Presentación y Defensa del Sistema — ModaLink

> **Fase 6: Presentaciones & Defensa (Proceso Unificado)**  
> **Trabajo Final (ASC) | Proyecto Software (LSI) — FCEQyN UNaM**  
> **Autora**: Roko María Guillermina

---

## 1. Estructura de Tiempos de la Exposición (Total: 25 min)

| Bloque | Tiempo | Foco Principal |
| :--- | :---: | :--- |
| **1. Introducción y Problema** | 3 min | Problemática de la moda en Misiones, informalidad en redes genéricas y propuesta de valor de ModaLink. |
| **2. Metodología Encadenada** | 5 min | Aplicación del Proceso Unificado (UP): desde la Fase 0 hasta el Diseño, teniendo como ancla canónica el modelo SQL. |
| **3. Arquitectura y Reglas** | 5 min | Doble capa de validación (BD + backend), separación de roles (global vs proyecto), no persistencia de derivados. |
| **4. Demostración en Vivo** | 8 min | Ejecución de consultas en base de datos (`demo_consultas_profesor.sql`) y recorrido por los módulos implementados. |
| **5. Testing y Conclusiones** | 4 min | Cobertura JaCoCo $\ge 80\%$, trazabilidad 100% y líneas de trabajo futuro. |

---

## 2. Guion Discursivo Paso a Paso

### Bloque 1: Planteo del Problema y Justificación
> *"Buenos días profesor y miembros del tribunal evaluador. El proyecto que presento hoy se denomina **ModaLink**, una plataforma de networking y gestión integral para las Industrias Creativas y de la Moda.*  
> *Actualmente en nuestra provincia y región, las producciones se coordinan a través de redes genéricas como Instagram o WhatsApp. Esto genera retrasos críticos: no hay modo de filtrar modelos por características técnicas reales, no se conoce la disponibilidad de agenda en tiempo real y no existe formalización de acuerdos."*

### Bloque 2: Solidez Metodológica (Proceso Unificado)
> *"Para este desarrollo seguimos rigurosamente el Proceso Unificado de forma encadenada. Como establece la guía metodológica, el modelo relacional físico en PostgreSQL actúa como la **Fuente de Verdad Central**.  
> Cada objetivo general y específico planteado en la Fase 0 se descompone en requerimientos formales, los cuales dan origen a los 69 Casos de Uso Extendidos. En la Fase de Análisis, cada caso de uso se traduce a Diagramas de Secuencia del Sistema y Contratos de Operación formalizados. En la Fase de Diseño, cada contrato se realiza internamente mediante Diagramas de Secuencia de Diseño respetando patrones GRASP."*

### Bloque 3: Arquitectura y Decisiones Técnicas
> *"En el backend utilizamos Java 25 con Spring Boot 4.1.0 y Spring Security con JWT vía cookies seguras httpOnly y protección CSRF.  
> Un pilar central exigido por la cátedra es la **doble capa de validación**: ninguna regla de negocio se delega al frontend. Si se restringe la solapación de horarios en la agenda, o el borrado lógico de un perfil con actividades pendientes, la regla se asegura tanto con restricciones y triggers en PostgreSQL como con excepciones semánticas de dominio en Java."*

### Bloque 4: Defensa Técnica del Modelo de Datos (Consultas SQL)
> *(Demostración práctica proyectando `demo_consultas_profesor.sql`)*:
> 1. Mostrar cómo se filtran los talentos cruzando `perfil`, `profesion` y `caracteristica_perfil`.
> 2. Mostrar la detección de solapamiento en `bloqueo_agenda` y el soporte de jornada partida (`hora_fin_manana`, `hora_inicio_tarde`).
> 3. Mostrar la trazabilidad del estado y motivo en usuarios deshabilitados administrativamente.

---

## 3. Preguntas Frecuentes de la Cátedra y Argumentación

- **P: ¿Por qué la base de datos se toma como punto canónico y no el diagrama de clases?**  
  **R**: En el Proceso Unificado adaptado, el esquema relacional normalizado fija de manera inequívoca los tipos de datos, la nulabilidad y las restricciones de integridad. Esto previene divergencias conceptuales entre lo documentado y lo persistido.
- **P: ¿Cómo manejan los bloqueos temporales por actividades de proyectos?**  
  **R**: Siguiendo el principio de arquitectura de **no almacenar atributos derivados**, los compromisos de proyectos no se duplican en la tabla `bloqueo_agenda`. Se calculan dinámicamente uniendo `actividad`, `asignacion_actividad` y el margen configurado en `agenda`.
