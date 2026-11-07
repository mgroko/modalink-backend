# Controles de Base de Datos — Auditoría (UC-67)

> **Alcance:** MVP de auditoría del trabajo final.
> **Esquema canónico:** `docs/3_diseño/ModaLinkBD.sql`.
> **Migración para bases existentes:** `docs/3_diseño/migraciones/2026-11-06_uc67_auditoria_mvp.sql`.
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
- Inmutabilidad del log y rol de BD restringido.
- UC-67: listado paginado, filtros y rango máximo configurable.

### 1.2. Fuera del alcance

- Login/logout y accesos denegados.
- Auditoría de lecturas.
- Perfiles, T&C, actividades, calendario, reemplazos, catálogos e imágenes.
- Hash chain, particionado, retención/purga y exportación.
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
| `tipo_evento` | `varchar(60)` | FK a `tipo_evento_auditoria` |

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

`fn_auditoria_inmutable()` bloquea `UPDATE` y `DELETE` sobre `auditoria` y
`detalle_auditoria` con una excepción de PL/pgSQL.

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
- `tipos` opcional; si no se envía, devuelve todos los activos.
- Rango máximo configurable en `AUDITORIA_RANGO_MAX_DIAS` (default 90).
- Orden: `fecha_hora DESC, id_auditoria DESC`.
- Tamaño máximo de página: 100.

La tabla de decisión completa y el contrato de respuesta están en
`docs/3_diseño/auditoria_mvp.md` y en la ficha de frontend
`docs/frontend/aplicadas/ficha-tecnica-auditoria-admin.md`.

---

## 9. Matriz de pruebas

| Nivel | Prueba | Qué cubre |
|---|---|---|
| Unitario | `ListarAuditoriaServiceTest` | Validaciones, tabla de decisión, catálogo |
| MVC | `AdminAuditoriaControllerTest` | Contrato de listado, filtros y errores |
| Integración | `AuditoriaTriggerIntegrationTest` | Triggers de usuario, proyecto, postulación, invitación, configuración e inmutabilidad |
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
- Los schedulers automáticos registran `id_usuario = NULL`.
- No se auditan lecturas ni intentos fallidos.
- No hay retención, particionado ni hash chain en este MVP.
- Las filas históricas previas a la migración no tienen `tipo_evento`; UC-67 las excluye cuando se filtra por tipo.
