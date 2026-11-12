# Plan de Pruebas — ModaLink

> **Metodología:** Proceso Unificado (UP)  
> **Fase actual:** Construcción cerrada / transición a aceptación  
> **Versión:** 1.0  
> **Fecha de corte:** 11/11/2026  
> **Alcance:** backend ModaLink (API REST + PostgreSQL)  
> **Base:** suite de pruebas existente en `src/test/java`

---

## 1. Introducción

### 1.1 Objetivo

Definir la estrategia, los niveles, los criterios y las métricas de prueba del sistema **ModaLink**, tomando como línea base las pruebas automatizadas ya existentes en el repositorio y organizándolas según las fases del Proceso Unificado.

El plan busca:

- Verificar funcionalmente los **76 casos de uso** del seguimiento.
- Validar reglas de negocio, permisos, integridad de datos y concurrencia.
- Asegurar que la suite sea repetible en local y en integración continua.
- Establecer criterios objetivos de entrada, salida y cierre.
- Detectar brechas de cobertura y priorizar su tratamiento.

### 1.2 Alcance

Incluye:

- Pruebas unitarias de DTO, mappers y servicios.
- Pruebas de controlador con `MockMvc` y seguridad.
- Pruebas de integración contra PostgreSQL real con Testcontainers.
- Pruebas de concurrencia, seguridad, storage, SSE y rendimiento puntual.
- Pruebas de aceptación funcional por flujo de caso de uso.
- Trazabilidad entre casos de uso, pruebas y cobertura.

No incluye, por ahora:

- Pruebas E2E de interfaz con navegador real.
- Pruebas de carga sostenida o estrés masivo.
- Pruebas de accesibilidad y usabilidad formal.
- Pruebas de compatibilidad multi-navegador.
- Pruebas de penetración externas.

### 1.3 Referencias

- `docs/1_requisitos/casos_de_uso.md`
- `docs/5_estado_sistema/estado_uc.md`
- `docs/2_analisis/contratos.md`
- `docs/2_analisis/dss.md`
- `docs/3_diseño/ModaLinkBD.sql`
- `docs/calidad/auditoria_mvp.md`
- `docs/calidad/controles_bd.md`
- `docs/calidad/controles_conexiones.md`
- `docs/calidad/controles_mensajeria.md`
- `docs/calidad/controles_chat_grupal.md`
- `docs/calidad/controles_presencia.md`
- `src/test/java`
- `src/test/resources/ModaLinkBD.sql`

---

## 2. Estrategia de pruebas según UP

| Fase UP | Objetivo de prueba | Tipos principales | Criterio de salida |
|---|---|---|---|
| **Inicio (Inception)** | Validar viabilidad técnica y criterios de aceptación iniciales | Smoke tests, pruebas de arquitectura, validación de alcance | Arquitectura viable y alcance acordado |
| **Elaboración (Elaboration)** | Validar arquitectura, modelo de datos y casos de uso críticos | Unitarias, integración con PostgreSQL, seguridad base | Línea base de pruebas verde y riesgos técnicos identificados |
| **Construcción (Construction)** | Validar cada incremento y cada caso de uso implementado | Unitarias, slice/API, integración, regresión, concurrencia | Cada UC cumple su Definition of Done |
| **Transición (Transition)** | Validar el sistema completo antes de la entrega | Regresión completa, aceptación, seguridad, rendimiento, smoke de despliegue | Release candidate aprobado |

### 2.1 Lectura del estado actual

- Los **76 UC del seguimiento** están implementados y testeados.
- La suite actual tiene **354 clases de test**, **2.763 pruebas ejecutadas**, **0 fallos y 0 errores**.
- Cobertura global: **85,61 % de líneas**, **84,48 % de instrucciones**, **65,78 % de ramas**.
- La etapa actual es de **transición**: consolidar regresión, aceptación y brechas de cobertura.

---

## 3. Niveles de prueba

| Nivel | Qué valida | Base existente | Herramientas |
|---|---|---|---|
| **Unitario** | Reglas de negocio, validaciones, mappers, cálculos | Clases `*ServiceTest`, `*MapperTest`, `*ValidationTest` | JUnit 5, Mockito, AssertJ |
| **Slice / API** | Contrato HTTP, status, JSON, validación de entrada, seguridad del endpoint | Clases `*ControllerTest` con `@WebMvcTest` y `MockMvc` | Spring Boot Test, MockMvc, Spring Security Test |
| **Integración** | Servicio + repositorio + PostgreSQL real, triggers, constraints, transacciones | Clases `*IntegrationTest` que extienden `AbstractPostgresIntegrationTest` | Spring Boot Test, Testcontainers, PostgreSQL 17 |
| **Sistema / Aceptación** | Flujo completo de un UC o escenario de negocio | Pruebas de integración de flujo + pruebas manuales/Postman | API real, datos de prueba, guion de aceptación |
| **No funcional** | Concurrencia, seguridad, storage, SSE, rendimiento puntual | `*ConcurrencyIntegrationTest`, `AuditoriaPerformanceIT`, tests de storage y SSE | Testcontainers, JUnit, JaCoCo |

### 3.1 Unitarias

Se aplican sobre:

- DTO de request y validaciones Jakarta Bean Validation.
- Mappers entidad ↔ DTO.
- Servicios con repositorios mockeados.
- Cálculos de CPM, disponibilidad, cupos, reemplazos y texto de contratos.

Ventaja: rápidas, aisladas y de diagnóstico simple.  
Limitación: no validan triggers, constraints ni SQL real.

### 3.2 Slice / API

Se aplican con `@WebMvcTest` y `@AutoConfigureMockMvc`:

- Códigos HTTP esperados.
- Serialización/deserialización JSON.
- Validación de payloads.
- Principal, cookies, CSRF y filtros de seguridad cuando corresponde.
- Contratos de error del `GlobalExceptionHandler`.

### 3.3 Integración

Se aplican con `@SpringBootTest` y `AbstractPostgresIntegrationTest`:

- Contenedor PostgreSQL 17-alpine singleton por JVM.
- Esquema cargado desde `src/test/resources/ModaLinkBD.sql`.
- Rollback por test con `@Transactional` cuando corresponde.
- Validación de triggers, constraints, auditoría, visibilidad y consultas reales.

Esta capa es obligatoria para:

- Reglas de cupo y membresías.
- Triggers de auditoría.
- Historial de miembros y snapshots.
- Contratos y firmas.
- Programación y reprogramación de cronogramas.
- Concurrencia con locks pesimistas.

### 3.4 Sistema / Aceptación

Se ejecuta sobre la API real con datos preparados:

- Flujo completo de alta de publicación → colaboración → interacción → reporte.
- Flujo de proyecto → postulación → aceptación → confirmación → asignación → finalización.
- Flujo de contratos → generación → firma/rechazo → búsqueda → historial.
- Flujo de conexiones → solicitud → aceptación/rechazo → mensajería.
- Flujo de administración → usuarios, catálogos, auditoría y exportación.

---

## 4. Tipos de prueba

| Tipo | Objetivo | Dónde se cubre |
|---|---|---|
| **Funcional** | Verificar cada UC y sus flujos alternativos | Unitarias + API + integración |
| **Regresión** | Evitar roturas al cambiar código | Suite completa Maven |
| **Smoke** | Verificar arranque y flujos mínimos | Tests de contexto, auth, home, health |
| **Sanity** | Validar una corrección puntual | Test específico del defecto |
| **Seguridad** | JWT, cookies, CSRF, roles y accesos | Tests de `security`, `AdminAreaAccessSecurityTest`, `LogoutSecurityIntegrationTest` |
| **Concurrencia** | Evitar duplicados y carreras | Tests de postulaciones, conexiones, publicaciones y contratos |
| **Integridad de datos** | Triggers, constraints, FK y auditoría | Tests de integración con PostgreSQL |
| **Rendimiento puntual** | Consultas de auditoría y exportación | `AuditoriaPerformanceIT` |
| **Storage** | Validación, conversión y limpieza de imágenes | `LocalStorageServiceTest`, `ProcesadorImagenTest`, `LimpiezaImagenesSchedulerTest` |
| **Tiempo real** | SSE de mensajería y presencia | Tests de `mensajeria`, `presencia` e integración |
| **Compatibilidad** | No cubierta formalmente | Pendiente para frontend |
| **Usabilidad** | No cubierta formalmente | Validación manual con usuarios |

---

## 5. Arquitectura y organización de las pruebas

### 5.1 Organización

`src/test/java` espeja la estructura de producción:

- `.../controlador`: pruebas de API con MockMvc.
- `.../servicio`: pruebas unitarias de reglas de negocio.
- `.../dto`: validaciones de request.
- `.../mapper`: conversiones.
- `.../repositorio`: integración con PostgreSQL real.
- `.../security`: JWT, filtros, cookies y accesos.
- `.../storage`: imágenes y archivos.
- `.../notificaciones`: listeners y resolución de destinos.
- `.../auditoria`: consulta, filtros, exportación y performance.

### 5.2 Convenciones

- `*Test`: unitaria, slice o validación.
- `*IntegrationTest`: integración con Testcontainers.
- `*ConcurrencyIntegrationTest`: pruebas de carrera.
- `*PerformanceIT`: prueba de rendimiento puntual.
- `AbstractPostgresIntegrationTest`: base compartida de integración.
- `AbstractFirmaContratoIntegrationTest`: base específica de contratos.

### 5.3 Infraestructura de integración

- Contenedor PostgreSQL 17-alpine estático y compartido por JVM.
- Esquema de prueba: `src/test/resources/ModaLinkBD.sql`.
- Inyección de datasource con `@DynamicPropertySource`.
- Rollback por test con `@Transactional`.
- Requiere Docker disponible.

### 5.4 Datos de prueba

- Seeds de catálogos y configuración del sistema.
- Factories y builders en las clases de test.
- `@TempDir` para storage.
- `MockMultipartFile` para archivos.
- `JdbcTemplate` para verificar efectos directos en BD.

### 5.5 Herramientas

- **JUnit 5** — motor de pruebas.
- **Mockito** — mocks y verificación de interacciones.
- **AssertJ** — aserciones fluidas.
- **Spring Boot Test** — contexto y slices.
- **MockMvc** — API.
- **Spring Security Test** — autenticación y autorización.
- **Testcontainers** — PostgreSQL real.
- **JaCoCo** — cobertura.
- **Maven Surefire** — ejecución.
- **GitHub Actions** — integración continua.

### 5.6 Comandos de ejecución

```bash
# Suite completa
mvn clean test

# Compilación de tests sin ejecutar
mvn clean test-compile

# Reporte de cobertura
mvn jacoco:report

# Verificación completa (si el proyecto lo configura)
mvn verify
```

Reportes:

- `target/surefire-reports/`
- `target/site/jacoco/index.html`
- `target/site/jacoco/jacoco.csv`

---

## 6. Trazabilidad UC ↔ pruebas

| Módulo | UC | Pruebas representativas | Cobertura de líneas |
|---|---:|---|---:|
| Usuarios y administración | 16 | `AuthControllerTest`, `AuthServiceTest`, `GoogleOAuthFlowIntegrationTest`, `AdminUsuarioControllerTest`, `AdminUsuarioServiceTest`, `AdminAuditoriaControllerTest` | 81,6 % auth / 89,6 % admin / 90,7 % auditoría |
| Perfiles y disponibilidad | 13 | `PerfilControllerTest`, `CrearPerfilServiceTest`, `VerPerfilServiceTest`, `CalendarioControllerTest`, `CalendarioRepositoryIntegrationTest`, `GestionTerminosCondicionesIntegrationTest` | 88,8 % perfiles / 90,1 % calendario |
| Publicaciones | 11 | `PublicacionControllerTest`, `CrearPublicacionServiceTest`, `ModificarPublicacionServiceTest`, `InteractuarPublicacionIntegrationTest`, `GestionarSolicitudColaboracionIntegrationTest` | 94,0 % |
| Proyectos | 26 | `ProyectoControllerTest`, `CrearProyectoServiceTest`, `ConfirmarProyectoServiceTest`, `FinalizarProyectoServiceTest`, `CpmMotorTest`, `PublicarProyectoIntegrationTest`, `FinalizarProyectoIntegrationTest` | 84,3 % |
| Contratos | 5 | `ContratoControllerTest`, `GenerarContratoServiceTest`, `GenerarContratoIntegrationTest`, `FirmarContratoIntegrationTest`, `RechazarContratoIntegrationTest` | 93,3 % |
| Conexiones y mensajería | 5 | `ConexionControllerTest`, `SolicitudConexionControllerTest`, `ChatGrupalControllerTest`, `MensajeriaIntegrationTest`, `EliminarConexionConcurrencyIntegrationTest` | 96,9 % conexiones / 92,5 % mensajería |
| Actividades | — | `ActividadControllerTest`, `CpmMotorTest`, `PlanificadorRecursosServiceTest`, `ModificarActividadIntegrationTest`, `EliminarActividadIntegrationTest` | 90,1 % |
| Postulaciones e invitaciones | — | `PostulacionControllerTest`, `AceptarPostulacionServiceTest`, `InvitacionControllerTest`, `GestionarPostulacionConcurrencyIntegrationTest` | 93,3 % postulaciones / 84,2 % invitaciones |
| Notificaciones | — | `NotificacionControllerTest`, listeners de actividad, habilidad, proyecto y reportes | 91,6 % |
| Auditoría | — | `AdminAuditoriaControllerTest`, `ListarAuditoriaServiceTest`, `AuditoriaTriggerIntegrationTest`, `AuditoriaPerformanceIT` | 90,7 % |
| Storage | — | `LocalStorageServiceTest`, `ProcesadorImagenTest`, `BorradoArchivosDiferidoTest`, `LimpiezaImagenesSchedulerTest` | 88,3 % |
| Seguridad | — | `JwtAuthenticationFilterTest`, `JwtServiceTest`, `SecurityConfigTest`, `AdminAreaAccessSecurityTest`, `LogoutSecurityIntegrationTest` | 92,9 % |
| Reemplazos automáticos | derivado | `EvaluadorConcordanciaTest`, `ReemplazoQueryServiceTest`, `GenerarReemplazosIntegrationTest` | 72,2 % |
| Polaroids | derivado | `PolaroidPerfilControllerTest`, `CrearPolaroidPerfilServiceTest`, `PolaroidPerfilIntegrationTest` | 66,9 % |
| Presencia | derivado | `PresenciaControllerTest`, `PresenciaServiceTest`, `PresenciaIntegrationTest` | 89,9 % |

> La cobertura indicada es de líneas y corresponde al reporte JaCoCo del 11/11/2026.

---

## 7. Cobertura actual y brechas

### 7.1 Línea base

| Métrica | Valor |
|---|---:|
| Clases de test | **354** |
| Pruebas ejecutadas | **2.763** |
| Fallos | **0** |
| Errores | **0** |
| Cobertura de líneas | **85,61 %** |
| Cobertura de instrucciones | **84,48 %** |
| Cobertura de ramas | **65,78 %** |

### 7.2 Módulos por debajo del 85 % de líneas

| Módulo | Cobertura | Acción propuesta |
|---|---:|---|
| `common` | 17,1 % | Revisar si incluye DTOs/handlers generados; agregar tests de excepciones y utilidades reales |
| `polaroids` | 66,9 % | Cubrir CRUD, permisos, visibilidad y casos de error |
| `usuario` | 68,5 % | Reforzar datos personales, baja, reactivación y schedulers |
| `reemplazos` | 72,2 % | Cubrir evaluación de candidatos, cancelación y regeneración |
| `modelo` | 74,9 % | No priorizar getters/setters; cubrir lógica de entidades si existe |
| `requerimientos` | 79,4 % | Cubrir búsquedas globales, filtros y elegibilidad |
| `auth` | 81,6 % | Cubrir OAuth, CSRF, cookies y errores del callback |
| `invitaciones` | 84,2 % | Cubrir batch, rechazo, concurrencia y cupos |
| `proyectos` | 84,3 % | Reforzar finalización, reemplazos, reprogramación y snapshots |
| `home` | 84,8 % | Cubrir resumen de oportunidades y estados vacíos |

### 7.3 Objetivos de cobertura

- **Global:** ≥ 85 % de líneas (hoy 85,61 %).
- **Código nuevo o modificado:** ≥ 80 %.
- **Módulos críticos** (proyectos, contratos, postulaciones, conexiones, mensajería, seguridad): ≥ 90 %.
- **Ramas:** subir gradualmente desde 65,78 %; priorizar reglas de negocio y validaciones.

---

## 8. Criterios de entrada y salida

### 8.1 Criterios de entrada

- Código compila y `mvn test-compile` finaliza correctamente.
- Esquema `ModaLinkBD.sql` actualizado y versionado.
- Docker/Testcontainers disponible en el entorno de ejecución.
- Datos de prueba y seeds disponibles.
- Caso de uso y contrato funcional estables.
- Defectos bloqueantes de la iteración anteriores resueltos.

### 8.2 Criterios de salida

- 100 % de las pruebas planificadas ejecutadas.
- 0 fallos y 0 errores en la suite completa.
- 0 defectos críticos o bloqueantes abiertos.
- Cobertura global ≥ 85 % de líneas.
- Cada UC con al menos una prueba unitaria y una de integración, o justificación documentada.
- Regresión completa en verde.
- Reporte de JaCoCo y Surefire adjuntos.
- Documentación de UC y fichas técnicas actualizadas.

### 8.3 Definition of Done de un UC

Un UC se considera cerrado cuando:

1. Tiene implementación completa y validaciones de negocio.
2. Tiene tests unitarios de servicio.
3. Tiene tests de controlador para contratos HTTP.
4. Tiene test de integración si toca BD, triggers, transacciones o concurrencia.
5. Tiene pruebas de seguridad si aplica permisos o roles.
6. Tiene prueba de concurrencia si modifica cupos, firmas, conexiones o estados.
7. Está actualizado en `estado_uc.md`.
8. Tiene ficha técnica para frontend si expone API.

---

## 9. Métricas y reportes

| Métrica | Frecuencia | Fuente |
|---|---|---|
| Pruebas ejecutadas / pasadas / fallidas | Cada ejecución | Surefire |
| Cobertura de líneas, instrucciones y ramas | Cada ejecución | JaCoCo |
| Cobertura por módulo | Cada cierre de iteración | JaCoCo CSV |
| UC cubiertos por tests | Cada iteración | Matriz de trazabilidad |
| Defectos por severidad | Semanal | Gestor de incidencias |
| Defectos reabiertos | Semanal | Gestor de incidencias |
| Tiempo de ejecución de la suite | Cada ejecución | Surefire/CI |
| Pruebas inestables (flaky) | Semanal | Historial CI |

### 9.1 Baseline actual

- 2.763 pruebas, 0 fallos, 0 errores.
- 85,61 % líneas / 84,48 % instrucciones / 65,78 % ramas.
- Módulos con mejor cobertura: conexiones 96,9 %, reportes 95,6 %, ubicación 94,9 %, publicaciones 94,0 %.
- Módulos a reforzar: `common`, `polaroids`, `usuario`, `reemplazos`, `requerimientos`, `auth`, `invitaciones`, `proyectos`.

---

## 10. Gestión de defectos

### 10.1 Severidades

| Severidad | Descripción | Ejemplo |
|---|---|---|
| **S1 — Bloqueante** | Impide operar o corrompe datos | Error 500 en login, trigger que bloquea altas válidas |
| **S2 — Alta** | Rompe una regla de negocio principal | Cupo duplicado, firma de contrato incorrecta |
| **S3 — Media** | Falla secundaria o caso borde | Filtro que no devuelve un resultado esperado |
| **S4 — Baja** | Detalle cosmético o de documentación | Mensaje de error poco claro |

### 10.2 Ciclo de vida

1. Detección y registro con evidencia.
2. Clasificación por severidad y prioridad.
3. Asignación al responsable.
4. Corrección y test de regresión.
5. Verificación y cierre.
6. Seguimiento de reaperturas.

### 10.3 Regla de regresión

Todo defecto corregido debe incorporar o actualizar una prueba automatizada que lo reproduzca. Sin test de regresión, el defecto no se considera cerrado.

---

## 11. Riesgos y mitigación

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Docker/Testcontainers no disponible | No corren las pruebas de integración | Ejecutar en CI o máquina con Docker; mantener suite unitaria como gate mínimo |
| Base compartida entre tests | Contaminación de datos y fallos intermitentes | `@Transactional` y rollback; datos únicos por test |
| Dependencia de Google OAuth | Flujo externo difícil de automatizar | Mocks del proveedor y pruebas de integración con callback simulado |
| SSE y mensajería | Tests inestables por tiempos | Esperas acotadas, buses en memoria y tests deterministas |
| Esquema `ModaLinkBD.sql` desactualizado | Integración con BD irreal | Versionar el script y ejecutarlo en cada corrida |
| `estado_uc.md` desactualizado | Métricas de avance incorrectas | Actualizar en el mismo commit del UC |
| Cobertura baja en módulos secundarios | Regresiones no detectadas | Plan de cobertura por módulo y tests de casos borde |
| Suite de integración lenta | Feedback tardío | Ejecutar unitarias en cada cambio; integración en pre-merge/CI |
| Falta de E2E de frontend | Contratos rotos no detectados | Pruebas de contrato y futura suite E2E |

---

## 12. Roles y responsabilidades

| Rol | Responsabilidad |
|---|---|
| **QA / Líder de pruebas** | Mantener el plan, la matriz de trazabilidad y los reportes |
| **Desarrolladores** | Escribir y mantener tests unitarios, API e integración |
| **DBA / responsable de datos** | Mantener `ModaLinkBD.sql`, triggers y datos de prueba |
| **DevOps** | Configurar Docker, CI, reportes y artefactos |
| **Product Owner / docente** | Validar criterios de aceptación y priorizar brechas |
| **Seguridad** | Revisar autenticación, permisos, CSRF y exposición de datos |

---

## 13. Calendario de transición

| Semana | Actividad | Entregable |
|---|---|---|
| **Semana 1** | Regresión completa y revisión de cobertura por módulo | Reporte Surefire + JaCoCo |
| **Semana 2** | Pruebas de aceptación por flujo de UC y seguridad | Acta de aceptación funcional |
| **Semana 3** | Pruebas no funcionales, corrección de brechas y estabilización | Release candidate |
| **Semana 4** | Regresión final, cierre de defectos y informe de cierre | Informe final de pruebas |

Si se abre una nueva iteración, repetir el ciclo:

1. Pruebas unitarias y de API del incremento.
2. Integración de las reglas nuevas.
3. Regresión de los módulos afectados.
4. Actualización de cobertura y trazabilidad.

---

## 14. Entregables

- Este plan de pruebas.
- Matriz de trazabilidad UC ↔ pruebas.
- Suite automatizada en `src/test/java`.
- Reportes de Surefire.
- Reporte de cobertura JaCoCo.
- Registro de defectos.
- Acta de aceptación funcional.
- Informe final de pruebas y release candidate.

---

## 15. Anexos

### 15.1 Estructura de la suite

```text
src/test/java/org/mgroko/backend/
├── admin/              # controladores, servicios, DTO, mappers y auditoría admin
├── auth/               # login, logout, registro, OAuth y seguridad
├── calendario/         # agenda, jornadas y disponibilidad
├── common/             # excepciones y utilidades
├── conexiones/         # solicitudes, conexiones y red de contactos
├── contratos/          # generación, modificación, firma y búsqueda
├── invitaciones/       # invitaciones generales y por actividad
├── mensajeria/         # chat privado, SSE y notificaciones
├── notificaciones/     # listeners, resolución de destinos y mapper
├── perfiles/           # perfiles, habilidades, T&C, reseñas y polaroids
├── postulaciones/      # alta, baja, aceptación y búsqueda
├── presencia/          # heartbeat y SSE de presencia
├── proyectos/          # CRUD, confirmación, finalización, CPM y reemplazos
├── publicaciones/      # CRUD, colaboración, interacción y reportes
├── reemplazos/         # evaluación de candidatos y consultas
├── reportes/           # reportes de perfil y publicación
├── repositorio/        # pruebas de integración con PostgreSQL
├── requerimientos/     # búsquedas y elegibilidad
├── security/           # JWT, cookies, CSRF y accesos
├── storage/            # imágenes, archivos y limpieza
├── ubicacion/          # catálogos y ubicación
└── usuario/            # datos personales, baja y schedulers
```

### 15.2 Comandos útiles

```bash
# Verificar Docker antes de integración
docker info

# Ejecutar todo
mvn clean test

# Ejecutar solo una clase
mvn -Dtest=PublicarProyectoServiceTest test

# Ejecutar solo integración
mvn -Dtest='*IntegrationTest' test

# Generar reporte de cobertura
mvn jacoco:report
```

### 15.3 Plantilla de defecto

```text
ID:
Título:
Severidad:
Prioridad:
Caso de uso / módulo:
Entorno:
Pasos para reproducir:
Resultado esperado:
Resultado obtenido:
Evidencia (log, request, response, captura):
Test de regresión asociado:
Estado:
```

---

**Fin del plan de pruebas.**
