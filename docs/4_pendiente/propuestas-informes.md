# Propuestas de informes — ModaLink

> **Estado:** documento de análisis y propuesta. No describe funcionalidad implementada.
>
> **Fecha:** 2026-11-25
>
> **Actualización 2026-11-26:** la Propuesta 1 reemplaza el índice numérico por estado por área derivado de alertas y agrega riesgo de entrega, antigüedad de decisiones y metadatos de encabezado/pie.
>
> **Base analizada:** backend de ModaLink (Java 25 + Spring Boot + PostgreSQL), esquema `docs/3_diseño/ModaLinkBD.sql`, módulos de proyectos, actividades, contratos, administración, auditoría, reportes, perfiles, publicaciones, conexiones y mensajería.

---

## 1. Contexto

ModaLink ya cuenta con los datos necesarios para generar informes de valor sin depender de planillas externas. Los módulos principales ya implementados son:

- proyectos, requerimientos, postulaciones, invitaciones, equipo, bajas y reemplazos;
- moodboard, objetivos, ubicación y publicaciones asociadas;
- actividades, asignaciones, cronograma CPM, agenda y reprogramación;
- contratos, versiones, firmas, rechazos y eventos;
- perfiles, habilidades, características técnicas, polaroids, T&C y reseñas;
- conexiones, publicaciones, interacciones, colaboraciones automáticas y mensajería;
- reportes de perfil/publicación, moderación, notificaciones, auditoría y panel administrativo.

También existe una base técnica reutilizable para PDF:

- `AuditoriaPdfRenderer` y `AuditoriaExportService` generan PDF/CSV con OpenPDF.
- `ModaLinkPdfEstilos` y `ModaLinkPdfPageEvent` concentran la identidad visual y el pie de página.
- `AuditoriaPdfEstilos` demuestra el patrón de estilos por informe.

### Limitaciones actuales que condicionan las propuestas

| Limitación | Impacto |
|---|---|
| No hay presupuesto, costos ni honorarios en el modelo | Los informes no deben prometer métricas monetarias |
| No hay `usuario.fecha_alta` ni `perfil.fecha_alta` | Las cohortes y la retención histórica requieren proxies o instrumentación |
| No hay snapshots diarios de actividad ni de estados | Las tendencias históricas necesitan una tabla de snapshots |
| No hay visitas a perfiles ni impresiones de publicaciones | No se puede medir “alcance” real sin instrumentación |
| `Hito` está mapeado de forma mínima | Los hitos con fecha límite y cumplimiento requieren completar la API |
| El moodboard no tiene epígrafe por imagen | El informe puede mostrar descripción general y miniaturas, no pies individuales |
| Las imágenes procesadas se guardan en WebP | OpenPDF no lo soporta de forma nativa; hay que convertir a PNG/JPEG para incrustarlas |
| La mensajería se audita al enviar, no al leer | No se puede medir “tasa de lectura” de mensajes sin instrumentación adicional |

### Principios para todos los informes

1. **Solo lectura:** no modifican datos de negocio.
2. **Métricas explicables:** toda fórmula debe poder rastrearse a tablas/campos existentes.
3. **Privacidad por rol:** cada rol ve solo lo que ya puede ver en la aplicación.
4. **Datos agregados para benchmarks:** nunca exponer información individual de terceros.
5. **Formato reutilizable:** JSON para la UI y PDF para descarga/compartir.
6. **Estados vacíos explícitos:** “sin datos” no debe confundirse con “todo bien”.
7. **Sin métricas de vanidad:** priorizar decisiones, no totales históricos.

---

## 2. Mapa de propuestas

| # | Informe | Audiencia | Decisión que habilita | Esfuerzo |
|---|---|---|---|---|
| 1 | Estado y salud del proyecto | Director del proyecto | Qué corregir antes de la entrega | M |
| 2 | Salud, crecimiento y liquidez de la plataforma | Administrador | Dónde invertir y qué profesiones/provincias impulsar | L |
| 3 | Confianza, moderación y cumplimiento | Administrador | Qué riesgos atender y cómo priorizar la moderación | S/M |
| 4 | Reputación y posicionamiento profesional | Usuario con perfil | Cómo mejorar su marca y su perfil | M |
| 5 | Radar de oportunidades y preparación competitiva | Usuario con perfil | A qué postularse y qué le falta | M/L |

**Escala de esfuerzo:** S = 1–2 semanas; M = 2–3 semanas; L = 3–5 semanas, con un desarrollador backend familiarizado con el proyecto.

---

## 3. Propuesta 1 — Informe ejecutivo de estado y salud del proyecto

**Afinación de:** “informe sobre el estado de un proyecto”.

### 3.1. Audiencia y pregunta

- **Audiencia principal:** director activo del proyecto.
- **Audiencia secundaria:** administrador, en modo solo lectura, reutilizando el mismo modelo de datos.
- **Pregunta:** “¿Este proyecto va a llegar bien a la entrega? ¿Quién está, quién falta, qué contratos están trabados y qué decisiones tengo que tomar esta semana?”

### 3.2. Valor

El director hoy puede consultar secciones separadas: detalle del proyecto, miembros, cronograma, contratos, historial y calificaciones. El informe integra todo en una sola vista ejecutiva y agrega una capa de interpretación:

- estado general derivado del área más grave;
- estado por área (cronograma, equipo, contratos, brief) explicado con alertas visibles;
- alertas priorizadas y decisiones con antigüedad;
- riesgo para la fecha de entrega;
- moodboard y equipo en el mismo documento;
- brecha entre plan y entrega.

### 3.3. Contenido propuesto

1. **Portada y ficha ejecutiva**
   - Nombre, estado, privacidad, ubicación, director, fecha de inicio y fecha de entrega.
   - Fecha de generación, corte, versión del informe y responsable de la generación.
   - Estado general del proyecto.

2. **Resumen de una página**
   - Indicadores principales.
   - “Decisiones que requieren atención”.
   - Próximas actividades de los próximos 7/14 días.

3. **Moodboard**
   - Descripción conceptual.
   - Grilla de imágenes con miniatura, orden y fecha.
   - Cantidad de imágenes y límite configurado (`IMAGEN_MAX_POR_MOODBOARD`).
   - Advertencia si está vacío o incompleto.
   - Nota técnica: las imágenes WebP se convierten temporalmente a PNG/JPEG para el PDF.

4. **Equipo y cobertura de requerimientos**
   - Directores y miembros activos.
   - Rol, profesión, estado, fecha de alta/baja y motivo.
   - Requerimientos: cantidad pedida, cubierta y faltante.
   - Miembros históricos, bajas y reemplazos.
   - T&C aceptados por cada miembro.
   - Postulaciones e invitaciones pendientes.

5. **Planificación y cronograma**
   - Actividades por estado.
   - Avance simple y ponderado por duración.
   - Resumen CPM: inicio, fin proyectado, makespan, margen, viabilidad y ruta crítica.
   - Actividades críticas, conflictos y vencidas.
   - Comparación entre fecha de entrega y fin proyectado.

6. **Contratos y firmas**
   - Contratos por estado.
   - Firmas obtenidas / total.
   - Partes pendientes y días desde la emisión.
   - Rechazos, cancelaciones y eventos relevantes.

7. **Estado por área, riesgos y alertas**
   - Estado de cronograma, equipo, contratos y brief.
   - El color de cada área se deriva de sus alertas, no de un puntaje.
   - Riesgo para la fecha de entrega.
   - Alertas priorizadas, antigüedad y acción sugerida.

8. **Historial y cierre**
   - Altas y bajas de miembros.
   - Cambios relevantes del proyecto.
   - Calificaciones del equipo, solo con reglas de privacidad y proyecto finalizado.
   - Anexo con detalle de actividades y miembros.

### 3.4. Métricas de valor para el director

| Métrica | Fórmula propuesta | Decisión |
|---|---|---|
| Avance del plan | Actividades `FINALIZADA` / total no canceladas | Saber si el ritmo alcanza |
| Avance ponderado | Suma de duración de finalizadas / suma de duración de no canceladas | Evitar que actividades cortas distorsionen el avance |
| Margen de entrega | `fechaEntrega - fechaFinProyectada` del CPM | Anticipar faltantes de tiempo |
| Viabilidad | `viable`, `margenMinutos`, conflictos y ruta crítica | Replanificar o reprogramar |
| Cobertura de cupos | Miembros activos / cantidad requerida, global y por requerimiento | Reclutar o reasignar |
| Estabilidad del equipo | 1 − (bajas / total de miembros históricos) | Dato informativo de fricción o sobrecarga |
| Preparación contractual | Firmas obtenidas / firmas totales de contratos emitidos | Destrabar firmas antes de producir |
| Completitud del brief | Ítems completos / 6: descripción, objetivos, requerimientos, moodboard, ubicación, fecha de entrega | Evitar confirmar con faltantes |
| Estado por área | La gravedad de las alertas del área: `ROJO` si hay `ALTA`; `AMARILLO` si hay `MEDIA`; `VERDE` si no hay alertas y hay datos; `SIN_DATOS` si no hay datos | Saber dónde intervenir sin promedios |
| Riesgo para la entrega | Fin proyectado, entrega, margen, críticas sin iniciar, vencidas, conflictos y avance | Anticipar faltantes de tiempo |
| Actividades vencidas | Actividades pendientes/en curso cuyo fin calculado ya pasó | Priorizar reprogramación |
| Postulaciones pendientes | Pendientes y antigüedad de la más vieja | Responder a tiempo |
| Contratos trabados | Contratos pendientes de firma con más de N días | Escalar firmas |

### 3.5. Estado por área propuesto

Se elimina el índice numérico. Cada área se evalúa por separado y su color es consecuencia directa de sus alertas.

| Estado del área | Condición |
|---|---|
| `ROJO` | Tiene al menos una alerta `ALTA` |
| `AMARILLO` | Tiene alertas `MEDIA` y ninguna `ALTA` |
| `VERDE` | Sin alertas, o solo `BAJA`/`INFO`, y con datos para evaluarla |
| `SIN_DATOS` | Sin alertas y sin datos suficientes |
| `NO_APLICA` | El modo del informe no evalúa esa área |

Áreas y alertas asociadas:

| Área | Alertas |
|---|---|
| Cronograma | `SIN_FECHA_ENTREGA`, `SIN_ACTIVIDADES`, `CRONOGRAMA_INVIABLE`, `MARGEN_NEGATIVO`, `MARGEN_AJUSTADO`, `CONFLICTOS_CRONOGRAMA`, `ACTIVIDADES_VENCIDAS`, `ACTIVIDADES_CRITICAS_SIN_INICIAR` |
| Equipo | `SIN_REQUERIMIENTOS`, `CUPOS_SIN_CUBRIR`, `MIEMBROS_SIN_TYC`, `POSTULACIONES_PENDIENTES`, `INVITACIONES_PENDIENTES` |
| Contratos | `CONTRATOS_PENDIENTES_FIRMA` |
| Brief | `SIN_MOODBOARD`, `MOODBOARD_INCOMPLETO`, `BRIEF_INCOMPLETO` |

Reglas:

- **Estado general = el área más grave**, con el orden `ROJO` > `AMARILLO` > `VERDE`.
- `SIN_DATOS` y `NO_APLICA` no cuentan para el estado general.
- Si todas las áreas son `SIN_DATOS` o `NO_APLICA`, el estado general es `SIN_DATOS`.
- Un área sin datos no se muestra en verde.
- Cada área incluye un motivo legible generado desde sus alertas, con un máximo de dos frases.
- Las decisiones son las primeras alertas `ALTA` y luego `MEDIA`, ordenadas por severidad y antigüedad.
- Cada alerta operativa y decisión debe declarar su área.
- La estabilidad del equipo sigue siendo un dato informativo, pero no define el color del área.
- Para proyectos `FINALIZADO` o `CANCELADO`, el informe pasa a modo cierre y solo admite alertas `INFO` o `BAJA`.

El **riesgo para la entrega** se muestra como bloque aparte con:

- fecha fin proyectada;
- fecha de entrega;
- margen en días;
- actividades críticas sin iniciar;
- actividades vencidas;
- conflictos;
- avance de actividades.

Si no hay fecha de entrega o no hay actividades, el bloque se muestra como “Sin datos”.

### 3.6. Datos disponibles

- `GET /proyectos/{idProyecto}`, `/miembros`, `/historial`, `/cronograma`, `/actividades`, `/contratos`, `/calificaciones`.
- `AdminProyectoDetalleResponse` ya reúne moodboard, miembros, requerimientos y contratos.
- `ResumenCronogramaResponse` expone margen, viabilidad, ruta crítica y conflictos.
- `CupoRequerimientoService` es la fuente única de verdad para cobertura.
- `ContratoRepository` y `FirmaContratoRepository` permiten calcular el estado de firmas.
- `MoodboardRepository.findByProyectoIdProyecto` ya carga imágenes con `@EntityGraph`.

### 3.7. Brechas y fases posteriores

- No hay snapshots históricos: el MVP será una foto actual.
- No hay presupuesto ni costos.
- No hay hitos completos: el MVP usa actividades próximas; los hitos quedan para fase 2.
- No hay epígrafe por imagen.
- La velocidad real por semana requiere `fecha_finalizacion` de actividad o auditoría; el MVP puede usar un promedio desde el inicio.
- La comparación “contra el informe anterior” requiere una tabla de snapshots.

### 3.8. Formato

- **JSON:** `GET /proyectos/{idProyecto}/informe` para la UI.
- **PDF:** `GET /proyectos/{idProyecto}/informe/pdf` para descargar o compartir.
- **Encabezado y pie del PDF:** proyecto, corte, generado por, sistema, versión, fecha de impresión, “Hoja X / Y” y aviso de que es una foto del momento que no reemplaza la auditoría.
- **Referencia visual:** mockups adjuntos de 4 páginas (portada/estado por área, moodboard/equipo, cronograma/contratos, alertas/cierre).
- **Fase 2:** envío periódico por email con link al PDF.
- **Fase 2:** variante administrativa.

### 3.9. Criterio de éxito

Un director puede abrir el informe y responder en menos de dos minutos:

1. ¿El proyecto está en riesgo?
2. ¿Qué tres decisiones debe tomar?
3. ¿Quiénes están y quiénes faltan?
4. ¿Cómo está el moodboard y el brief?
5. ¿Qué contratos están trabados?

---

## 4. Propuesta 2 — Informe de salud, crecimiento y liquidez de la plataforma

### 4.1. Audiencia y pregunta

- **Audiencia:** administrador del sistema.
- **Pregunta:** “¿La plataforma está creciendo de verdad y está logrando que las personas colaboren, o solo se están registrando?”

### 4.2. Métrica estrella

**Colaboraciones efectivas por mes:** membresías activas incorporadas a proyectos que alcanzaron `CONFIRMADO` o `FINALIZADO`, excluyendo al director, más colaboraciones automáticas confirmadas.

Es mejor que “usuarios registrados” o “proyectos creados” porque mide el resultado real de la plataforma: que alguien encontró con quién trabajar.

### 4.3. Métricas de valor para el administrador

| Métrica | Fórmula / fuente | Decisión |
|---|---|---|
| Activación | % de cuentas nuevas con perfil activo, T&C y primera conexión/postulación en 7/30 días | Mejorar onboarding |
| Cobertura de vacantes | Requerimientos cubiertos / publicados a 7/14/30 días | Saber si falta oferta o demanda |
| Tiempo a confirmación | Días desde publicación hasta `CONFIRMADO` | Detectar fricción |
| Conversión de proyectos | Publicados → confirmados → finalizados; cancelados por motivo | Medir calidad de la demanda |
| Liquidez por profesión/provincia | Vacantes / perfiles activos; fill rate y tiempo de cobertura | Orientar adquisición |
| Calidad del match | Postulaciones por vacante, aceptación, invitaciones aceptadas, colaboraciones automáticas aceptadas | Ajustar oportunidades |
| Retención | Perfiles activos últimos 30 días / cohorte; bajas solicitadas y definitivas | Medir valor sostenido |
| Engagement | WAU/MAU, publicaciones, conexiones y mensajes por perfil activo | Priorizar producto |
| Salud operativa | Entregas de email, notificaciones pendientes, schedulers, limpieza de imágenes | Evitar degradación |

### 4.4. Lo que conviene evitar

- Total de usuarios registrados como métrica de crecimiento. Hoy la tarjeta “Nuevos usuarios” muestra el total, no los nuevos del período.
- Total de proyectos sin distinguir estados.
- Likes o publicaciones totales sin normalizar por usuario activo.

### 4.5. Datos disponibles

- `AdminDashboardService` y `AdminDashboardResumen`.
- `Perfil.ultimaActividad` y `PresenciaService` para actividad reciente.
- `ProyectoRepository`, `MiembroProyectoRepository`, `PostulacionGralRepository`, `PostulacionActividadRepository`, `ConexionRepository`, `MeGustaRepository`, `ComentarioRepository`.
- Auditoría: `PROYECTO_CREADO`, `PERFIL_CREADO`, `POSTULACION_CREADA`, `INVITACION_CREADA`, `CONEXION_CREADA`.

### 4.6. Brechas críticas

- No existe `usuario.fecha_alta` ni `perfil.fecha_alta`.
- No hay snapshots diarios de actividad ni de estados.
- No hay visitas a perfiles ni impresiones.
- La auditoría es confiable desde su implementación, no para períodos anteriores.

### 4.7. Formato y esfuerzo

- Dashboard con filtros por período, profesión y provincia.
- PDF ejecutivo mensual.
- CSV para análisis.
- **Esfuerzo:** L, por consultas agregadas e instrumentación histórica.

---

## 5. Propuesta 3 — Informe de confianza, moderación y cumplimiento

### 5.1. Audiencia y pregunta

- **Audiencia:** administrador del sistema, especialmente con más de un moderador.
- **Pregunta:** “¿Qué tan rápido y bien estamos resolviendo reportes, qué contenido es riesgoso y dónde se concentran los problemas?”

### 5.2. Contenido

1. Backlog de reportes y antigüedad.
2. Reportes por tipo, motivo y entidad.
3. Tiempos de atención y resolución.
4. Reincidencia de perfiles o publicaciones.
5. Moderación de contenido: ocultas/reactivadas y tiempo hasta ocultar.
6. Contratos y confianza: firmas pendientes, rechazos, firmas invalidadas por bajas.
7. Cuentas: deshabilitadas, bajas solicitadas, bajas definitivas y reactivaciones.
8. Auditoría: eventos críticos por módulo y acciones administrativas.

### 5.3. Métricas de valor

| Métrica | Fórmula / fuente | Decisión |
|---|---|---|
| Backlog pendiente | Reportes `PENDIENTE` y antigüedad P50/P90 | Asignar moderación |
| Cumplimiento de SLA | % resueltos en menos de 24/72 horas | Definir o ajustar SLA |
| Tiempo a primera acción | `fecha_reporte` → primera acción | Detectar cuellos de botella |
| Mezcla de resolución | Resueltos vs. descartados por motivo | Estimar falsos positivos |
| Reincidencia | Entidades con 2+ reportes en 90 días | Priorizar investigación |
| Reportes por 1.000 activos | Reportes / perfiles o publicaciones activas | Comparar períodos |
| Contratos trabados | `PENDIENTE_FIRMA` con más de 7 días; tasa de rechazo | Detectar fricción contractual |
| Riesgo de cuenta | Deshabilitados por motivo; bajas pendientes y definitivas | Medir abuso y churn |

### 5.4. Datos disponibles

- `reporte`: estado, motivo, fechas, autor, perfil reportado y admin revisor.
- `publicacion.estado` para ocultas/reactivadas.
- `contrato_evento` y `firma_contrato`.
- Auditoría: `REPORTE_ESTADO_CAMBIADO`, `CONTRATO_*` y eventos administrativos.
- Renderer de PDF de auditoría reutilizable.

### 5.5. Brechas

- No hay asignación formal de reportes ni SLA configurable.
- No hay apelaciones.
- Para medir “tiempo a primera acción” hay que registrar esa acción o inferirla de auditoría.

### 5.6. Formato y esfuerzo

- PDF mensual, vista operativa de bandeja y CSV.
- **Esfuerzo:** S/M. Es el informe más rápido de implementar.

---

## 6. Propuesta 4 — Informe de reputación y posicionamiento profesional

### 6.1. Audiencia y pregunta

- **Audiencia:** usuario con rol `USUARIO` y perfil activo.
- **Pregunta:** “¿Cómo me ven los demás, qué tan visible soy y qué debería mejorar para conseguir mejores proyectos?”

### 6.2. Contenido

1. Portada de marca personal: foto, nombre artístico, profesión, ubicación y estado.
2. Reputación: promedio, cantidad de reseñas, distribución 5→1, evolución y últimas reseñas.
3. Experiencia: proyectos como director o miembro, roles, finalizados/cancelados, tiempo de participación y colaboradores recurrentes.
4. Red: conexiones activas, solicitudes, tasa de aceptación y composición por profesión/provincia.
5. Contenido: publicaciones, likes y comentarios recibidos, engagement promedio y mejor publicación.
6. Colaboraciones: propuestas automáticas y manuales, aceptadas/rechazadas y conversión.
7. Completitud del perfil: foto, biografía, habilidades, características técnicas, polaroids y T&C.
8. Plan de acción con tres recomendaciones.
9. Benchmark opcional contra perfiles de la misma profesión y provincia, solo con cohortes mínimas.

### 6.3. Métricas de valor

| Métrica | Fórmula | Para qué sirve |
|---|---|---|
| Reputación ponderada | `(C * m + suma) / (C + n)`, con `C = 5` y `m` promedio de la profesión | Evitar que 1 reseña de 5 estrellas supere a 20 de 4,8 |
| Tasa de respuesta | (Conexiones aceptadas + invitaciones aceptadas) / recibidas | Detectar oportunidades desatendidas |
| Conversión de postulaciones | Aceptadas / enviadas | Medir eficacia del perfil |
| Engagement | (Likes + comentarios) / publicaciones | Saber si el portfolio genera interés |
| Conversión de colaboraciones | Confirmadas / propuestas recibidas | Medir confianza de la red |
| Completitud del perfil | Score ponderado de foto, bio, skills, características, polaroids y T&C | Detectar mejoras rápidas |
| Colaboradores recurrentes | % de proyectos con alguien con quien ya trabajó | Medir reputación real |
| Percentil | Comparación agregada con cohorte mínima de 20 perfiles | Contextualizar su posición |

### 6.4. Datos disponibles

- `calificacion_perfil` y `CalificacionPerfilRepository`.
- `miembros_proyecto`, `Proyecto`, `ConexionRepository`, `SolicitudConexionRepository`.
- `Publicacion`, `MeGustaRepository`, `ComentarioRepository`.
- `HabilidadPerfil`, `CaracteristicaPerfil`, `PolaroidPerfil`, `PerfilTyc`.
- `ColaboracionAutomaticaRepository`.

### 6.5. Brechas

- No hay visitas a perfiles ni impresiones.
- La calificación es de una sola dimensión.
- El benchmark necesita anonimización y umbral mínimo.
- El informe debe ser solo del propio perfil.

### 6.6. Formato y esfuerzo

- Sección “Mi reputación” en el dashboard.
- PDF “Mi perfil profesional” descargable.
- **Esfuerzo:** M.

---

## 7. Propuesta 5 — Radar de oportunidades y preparación competitiva

### 7.1. Audiencia y pregunta

- **Audiencia:** usuario con rol `USUARIO` que busca proyectos.
- **Pregunta:** “¿A qué me conviene postularme ahora y qué me falta para quedar?”

### 7.2. Contenido

1. Oportunidades recomendadas, ordenadas por compatibilidad y con explicación.
2. Compatibilidad desglosada: profesión, habilidades, características técnicas, ubicación, disponibilidad y requisitos obligatorios.
3. Brechas: habilidades o características que más se repiten y que el perfil no tiene.
4. Estado del perfil: T&C, foto, bio, skills y disponibilidad; impacto estimado.
5. Pipeline personal: enviadas, pendientes, aceptadas y rechazadas; tasa de respuesta.
6. Agenda: conflictos con actividades y carga próxima.
7. Alertas: nuevas oportunidades de alta compatibilidad, cierres próximos y perfil incompleto.

### 7.3. Métricas de valor

| Métrica | Fórmula | Para qué sirve |
|---|---|---|
| Match score | 60 % requisitos obligatorios + 20 % deseables + 10 % disponibilidad + 10 % ubicación | Priorizar postulaciones |
| Elegibilidad | Oportunidades elegibles / relevantes | Evitar postularse en vano |
| Brecha de habilidades | Frecuencia de skills/características faltantes | Saber qué capacitarse |
| Conversión | Aceptadas / enviadas | Medir efectividad |
| Tiempo de respuesta | Días desde postulación hasta respuesta | Detectar demoras |
| Frescura | Días desde publicación y hasta inicio/entrega | Postular a tiempo |
| Competencia | Postulaciones por vacante, agregada y con mínimo de casos | Estimar dificultad sin exponer datos |

### 7.4. Datos disponibles

- Búsqueda global de requerimientos y `RequerimientoOportunidadResponse` con `faltantes`.
- `ElegibilidadActividad`, `PlanificadorRecursosService`, `DisponibilidadModificadaListener` y servicios de agenda.
- `PostulacionGralRepository` y `PostulacionActividadRepository`.
- Perfil, habilidades, características, T&C y ubicación.

### 7.5. Brechas

- No hay “guardados” ni búsquedas favoritas.
- No hay fecha de cierre explícita para requerimientos generales.
- La competencia por vacante puede ser sensible.
- El match score debe ser transparente y configurable.

### 7.6. Formato y esfuerzo

- Resumen semanal por email o notificación in-app.
- PDF “Mis oportunidades”.
- **Esfuerzo:** M/L.

---

## 8. Prioridad recomendada

1. **Informe de estado del proyecto** — mayor valor inmediato y reutiliza casi todo lo existente.
2. **Informe de confianza y moderación** — alto valor operativo, bajo esfuerzo, reutiliza auditoría y PDF.
3. **Informe de reputación del usuario** — valor directo para el usuario y la retención.
4. **Radar de oportunidades** — muy accionable, pero requiere cuidado en matching y privacidad.
5. **Informe de salud de la plataforma** — estratégico, pero conviene después de agregar snapshots y fechas de alta.

---

## 9. Brechas de datos para una segunda fase

| Brecha | Cambio sugerido | Informes que habilita |
|---|---|---|
| Sin fecha de alta de usuario/perfil | Agregar `fecha_alta` o tabla de eventos de ciclo de vida | Activación, cohortes, retención |
| Sin snapshots diarios | Tabla `metricas_plataforma_diaria` o job de snapshots | Tendencias WAU/MAU, estados, vacantes |
| Sin snapshots de proyecto | Tabla `informe_proyecto_snapshot` | Comparación contra informe anterior |
| Sin primera acción en reportes | Campo `fecha_primera_accion` o auditoría específica | SLA real de moderación |
| Sin visitas/impresiones | Tabla de eventos de vista/impresión con privacidad | Alcance y conversión de contenido |
| `Hito` incompleto | Completar entidad, API y fechas | Hitos y cumplimiento real |
| Sin epígrafe de moodboard | Agregar descripción por imagen | Informe visual más rico |
| Sin fecha de finalización de actividad | Agregar campo o usar auditoría | Velocidad real y cycle time |

---

## 10. Recomendación

Empezar por la **Propuesta 1** porque:

- el director ya tiene los datos dispersos y necesita una vista integrada;
- el backend ya calcula CPM, cupos, contratos y moodboard;
- se puede implementar sin cambios de esquema;
- reutiliza el pipeline de PDF existente;
- genera un entregable visible y demostrable para la defensa del proyecto.

Las propuestas 2 y 3 son las siguientes candidatas naturales: la 3 por bajo esfuerzo y la 2 por valor estratégico, aunque esta última requiere instrumentación para tendencias confiables.

---

## 11. Nota de alcance

Este documento no modifica el sistema. Es una base para priorizar y para convertir cada propuesta en una especificación funcional/técnica. El plan detallado de la Propuesta 1 está en:

`docs/4_pendiente/plan-implementacion-informe-proyecto.md`
