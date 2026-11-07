# Controles de Base de Datos — Auditoría (UC-67)

> **Alcance:** MVP de auditoría del trabajo final.
> **Esquema canónico:** `docs/3_diseño/ModaLinkBD.sql`.
> **Migraciones para bases existentes:** `docs/3_diseño/migraciones/2026-11-06_uc67_auditoria_mvp.sql` y `docs/3_diseño/migraciones/2026-11-07_uc67_fixes_export.sql`.
> **Caso de uso:** UC-67 — Generar informe de auditoría.

---

## 1. Objetivo

Registrar de forma inmutable los eventos críticos ya implementados, identificando
al **operador real** de la aplicación, y exponer un listado paginado con filtros
por fechas y tipos de evento para el administrador.

### 1.1. Dentro del alcance

- Alta, baja, deshabilitación, habilitación y cambio de rol de usuario.
- Cambios de estado de proyecto.
- Cambios de estado y bajas físicas de postulaciones e invitaciones.
- Cambios de configuración del sistema.
- Alta y resolución de reportes de perfil (trigger preexistente, ahora con tipo de evento).
- Fallback `OTRO` para códigos de evento no catalogados.
- Inmutabilidad del log (`UPDATE`, `DELETE` y `TRUNCATE`) y rol de BD restringido.
- UC-67: listado paginado, resumen agregado y exportación CSV/PDF con topes configurables.

### 1.2. Fuera del alcance

- Login/logout y accesos denegados.
- Auditoría de lecturas.
- Perfiles, T&C, actividades, calendario, reemplazos, catálogos e imágenes.
- Hash chain, particionado y retención/purga.
- Frontend de UC-67.

---

## 2. Tablas de auditoría

### 2.1. `tipo_evento_auditoria`

Catálogo cerrado de eventos críticos.

| Columna | Tipo | Regla |
|---|---|---|
| `codigo` | `varchar(60)` | PK, código estable en mayúsculas |
| `nombre` | `varchar(120)` | Nombre legible |
| `descripcion` | `varchar(255)` | Descripción funcional |
| `modulo` | `varchar(40)` | Agrupación (`USUARIOS`, `PROYECTOS`, etc.) |
| `activo` | `boolean` | `true` para exponerlo en UC-67 |

### 2.2. `auditoria`

| Columna | Tipo | Regla |
|---|---|---|
| `id_auditoria` | `int8` | PK identity |
| `usuario_bd` | `varchar(200)` | `CURRENT_USER` (rol de BD) |
| `entidad_afectada` | `varchar(100)` | Nombre de tabla o entidad lógica |
| `entidad_clave` | `varchar(100)` | Clave textual para entidades sin ID numérico |
| `id_afectado` | `int8` | ID de la entidad afectada; `0` para configuración |
| `fecha_hora` | `timestamp` | `clock_timestamp()` |
| `ip_usuario` | `varchar(45)` | IP de conexión a BD (`inet_client_addr()`) |
| `id_tipo_auditoria` | `int8` | FK a `tipo_accion_auditoria` (`INSERT`/`UPDATE`/`DELETE`) |
| `id_usuario` | `int8` | **Operador real** de la aplicación (no el afectado) |
| `tipo_evento` | `varchar(60)` | FK a `tipo_evento_auditoria`; puede ser `OTRO` |
| `tipo_evento_original` | `varchar(60)` | Código original del trigger, aun cuando se resuelva como `OTRO` |

### 2.3. `detalle_auditoria`

| Columna | Tipo | Regla |
|---|---|---|
| `id_detalle_auditoria` | `int8` | PK identity |
| `campo` | `varchar(100)` | Nombre del campo |
| `valor_anterior` | `text` | Permite `NULL` y textos largos |
| `valor_nuevo` | `text` | Permite `NULL` y textos largos |
| `id_auditoria` | `int8` | FK a `auditoria` |

---

## 3. Catálogo de eventos

| Código | Evento | Módulo |
|---|---|---|
| `USUARIO_ALTA` | Alta de usuario | USUARIOS |
| `USUARIO_BAJA` | Baja o solicitud de baja | USUARIOS |
| `USUARIO_DESHABILITADO` | Deshabilitación con motivo/duración | USUARIOS |
| `USUARIO_HABILITADO` | Habilitación o reactivación | USUARIOS |
| `USUARIO_ROL_CAMBIADO` | Cambio de rol global | USUARIOS |
| `PROYECTO_ESTADO_CAMBIADO` | Transición de estado de proyecto | PROYECTOS |
| `POSTULACION_ESTADO_CAMBIADO` | Transición de estado de postulación | POSTULACIONES |
| `POSTULACION_ELIMINADA` | Baja física de postulación | POSTULACIONES |
| `INVITACION_ESTADO_CAMBIADO` | Transición de estado de invitación | INVITACIONES |
| `INVITACION_ELIMINADA` | Baja física de invitación | INVITACIONES |
| `CONFIGURACION_ACTUALIZADA` | Alta/edición/baja de configuración | ADMINISTRACION |
| `REPORTE_ESTADO_CAMBIADO` | Alta o resolución de reporte | REPORTES |
| `OTRO` | Código de evento no presente en el catálogo | SISTEMA |

---

## 4. Triggers

| Trigger | Tabla | Disparador | Evento |
|---|---|---|---|
| `trg_auditar_cambio_usuario` | `usuario` | `AFTER INSERT OR UPDATE OF estado, id_rol_global OR DELETE` | Según operación y estado |
| `trg_auditar_cambio_proyecto` | `proyecto` | `AFTER UPDATE OF estado, privacidad OR DELETE` | `PROYECTO_ESTADO_CAMBIADO` |
| `trg_auditar_reporte` | `reporte` | `AFTER INSERT OR UPDATE OF estado, id_admin_revisor, fecha_resolucion` | `REPORTE_ESTADO_CAMBIADO` |
| `trg_auditar_estado_postulacion_gral` | `postulacion_gral` | `AFTER UPDATE OF estado OR DELETE` | `POSTULACION_ESTADO_CAMBIADO` / `POSTULACION_ELIMINADA` |
| `trg_auditar_estado_postulacion_actividad` | `postulacion_actividad` | `AFTER UPDATE OF estado OR DELETE` | idem |
| `trg_auditar_estado_invitacion_gral` | `invitacion_gral` | `AFTER UPDATE OF estado OR DELETE` | `INVITACION_ESTADO_CAMBIADO` / `INVITACION_ELIMINADA` |
| `trg_auditar_estado_invitacion_actividad` | `invitacion_actividad` | `AFTER UPDATE OF estado OR DELETE` | idem |
| `trg_auditar_configuracion` | `configuracion_sistema` | `AFTER INSERT OR UPDATE OR DELETE` | `CONFIGURACION_ACTUALIZADA` |

Todos los triggers resuelven el código con `fn_evento_auditoria(...)`. Si el
código no está en `tipo_evento_auditoria`, se guarda como `OTRO` y se conserva
el código original en `auditoria.tipo_evento_original`; la operación de negocio
no falla.

### 4.1. Reglas de detalle

- Usuario: `estado`, `id_rol_global`, `motivo_deshabilitacion`,
  `fecha_hasta_deshabilitacion`; en `DELETE`, `registro = correo`.
- Proyecto: `estado`, `privacidad`, `motivo_cancelacion`; en `DELETE`,
  `registro = nombre`.
- Postulación/invitación: `estado`; en `DELETE`, `registro = estado anterior`.
- Configuración: `valor` y `descripcion`. Para `PERFIL_TYC_PLANTILLA` se guarda
  un resumen con longitud y `md5`, no el texto completo.

---

## 5. Operador real

La aplicación setea el usuario autenticado al inicio de cada método auditado:

```sql
SELECT set_config('app.id_usuario', '<idUsuario>', true);
```

- El tercer parámetro `true` hace que el valor sea **local a la transacción**, evitando fugas por el pool de conexiones.
- Los triggers leen `current_setting('app.id_usuario', true)` mediante `fn_auditoria_usuario_actual()`.
- Si no hay usuario autenticado (scheduler), el operador queda `NULL`.
- En alta de usuario y autorreactivación, el trigger atribuye el evento al propio usuario.
- La implementación Java está en `AuditoriaSesionService` y `AuditoriaOperadorAspect`; los métodos auditados se anotan con `@AuditarOperacion`.

---

## 6. Inmutabilidad y roles

### 6.1. Trigger de inmutabilidad

`fn_auditoria_inmutable()` bloquea `UPDATE`, `DELETE` y `TRUNCATE` sobre
`auditoria` y `detalle_auditoria` con una excepción de PL/pgSQL. Existe un
bypass explícito y transaccional para mantenimiento:
`set_config('app.auditoria_admin', 'true', true)`.

### 6.2. Rol de la API

La migración crea el rol `modalink_api` con:

- `SELECT`, `INSERT`, `UPDATE`, `DELETE` sobre tablas de negocio.
- `INSERT` y `SELECT` sobre `auditoria` y `detalle_auditoria`.
- `REVOKE UPDATE, DELETE, TRUNCATE` sobre las tablas de auditoría.
- `REVOKE CREATE` sobre el esquema `public`.

En producción se configura:

```properties
spring.datasource.username=modalink_api
spring.datasource.password=${DB_API_PASSWORD}
```

En tests se mantiene `postgres` para poder limpiar datos con `TRUNCATE`.

---

## 7. Índices

| Índice | Columnas |
|---|---|
| `idx_auditoria_fecha_tipo` | `(fecha_hora DESC, tipo_evento)` |
| `idx_auditoria_entidad` | `(entidad_afectada, id_afectado)` |
| `idx_auditoria_entidad_fecha` | `(entidad_afectada, fecha_hora DESC)` |
| `idx_auditoria_usuario` | `(id_usuario, fecha_hora DESC)` |
| `idx_detalle_auditoria` | `(id_auditoria)` |

La prueba de carga con 100.000 eventos arrojó **p95 = 57 ms** para una consulta
de 30 días con 3 tipos, por debajo de la cota de 2 segundos.

---

## 8. UC-67 — Consulta

```http
GET /admin/auditoria?fechaDesde=2026-11-01&fechaHasta=2026-11-07&tipos=USUARIO_ALTA,USUARIO_BAJA&page=0&size=20
```

- Solo rol `ADMINISTRADOR`.
- `fechaDesde` y `fechaHasta` obligatorias.
- `tipos` opcional; si no se envía, **no se filtra por tipo** y se incluyen históricos y eventos fuera del catálogo.
- Rango máximo configurable en `AUDITORIA_RANGO_MAX_DIAS` (default 90).
- Tamaño máximo de página configurable en `AUDITORIA_PAGINA_MAX` (default 100).
- Orden: `fecha_hora DESC, id_auditoria DESC`.

### 8.1. Resumen

```http
GET /admin/auditoria/resumen?fechaDesde=2026-11-01&fechaHasta=2026-11-07&tipos=USUARIO_ALTA
```

Devuelve `totalRegistros`, `porTipoEvento[]` y `porEntidad[]`. Sin filtro de
tipos, las filas históricas se agrupan como `SIN_TIPO`.

### 8.2. Exportación

```http
GET /admin/auditoria/export?formato=CSV&fechaDesde=2026-11-01&fechaHasta=2026-11-07
GET /admin/auditoria/export?formato=PDF&fechaDesde=2026-11-01&fechaHasta=2026-11-07
```

- `formato` obligatorio (`CSV` o `PDF`).
- Tope configurable en `AUDITORIA_EXPORT_MAX_REGISTROS` (default 10.000).
- El PDF incluye encabezado, resumen y tabla de eventos.
- Las filas históricas muestran “No disponible (histórico)” como operador.
- El CSV usa UTF-8 con BOM, separador `;` y escapado RFC 4180.

La tabla de decisión completa y el contrato de respuesta están en
`docs/calidad/auditoria_mvp.md` y en la ficha de frontend
`docs/frontend/ficha-tecnica-auditoria-admin.md`.

---

## 9. Matriz de pruebas

| Nivel | Prueba | Qué cubre |
|---|---|---|
| Unitario | `AuditoriaFiltroServiceTest` | Fechas, tipos, catálogo vacío y paginación |
| Unitario | `ListarAuditoriaServiceTest` | Listado, filtros y catálogo |
| Unitario | `AuditoriaExportServiceTest` | CSV, escapado y sanitización de fórmulas |
| MVC | `AdminAuditoriaControllerTest` | Listado, resumen, exportación y errores |
| Integración | `AuditoriaTriggerIntegrationTest` | Triggers de usuario, proyecto, postulación, invitación, configuración, `OTRO` e inmutabilidad (`UPDATE`, `DELETE`, `TRUNCATE` y bypass) |
| Integración | `AuditoriaConsultaIntegrationTest` | Históricos, resumen, CSV y PDF |
| Integración | `AuditoriaMigracionIdempotenciaTest` | La migración de UC-67 se puede aplicar dos veces sin errores |
| Integración | `ReportePerfilIntegrationTest` | Trigger de reporte y regresión |
| Integración | `PublicarProyectoIntegrationTest` | Trigger de proyecto y regresión |
| Carga | `AuditoriaPerformanceIT` | 100.000 eventos, p95 < 2 s |

Comando de la prueba de carga:

```bash
mvn -Dtest=AuditoriaPerformanceIT test
```

---

## 10. Limitaciones conocidas

- La IP registrada es la de la conexión a PostgreSQL, no la IP real del navegador.
- No se auditan lecturas ni intentos fallidos.
- No hay retención, particionado ni hash chain en este MVP.
- Las filas históricas previas a la migración no tienen `tipo_evento` ni `tipo_evento_original`; se muestran sin filtro y se agrupan como `SIN_TIPO` en el resumen.
- No se backfillean los históricos: `id_usuario` puede no representar al operador real en esas filas. La UI/CSV/PDF muestran “No disponible (histórico)”.
- Los schedulers automáticos nuevos registran `id_usuario = NULL` y se muestran como “Sistema / automático”.
