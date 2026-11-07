# Diseño de auditoría — MVP UC-67

> **Fecha:** Noviembre 2026
> **Alcance:** trabajo final, Analista Universitario en Sistemas.
> **Estado:** implementado y testeado.

---

## 1. Resumen

La auditoría registra eventos críticos en la tabla `auditoria`, con detalle
campo a campo en `detalle_auditoria`. Los eventos se generan por triggers de
PostgreSQL, y el operador real se propaga desde la aplicación a la sesión de BD.

El MVP cubre:

- Catálogo cerrado de 12 eventos.
- Usuarios: alta, baja, deshabilitación, habilitación y cambio de rol.
- Estado de proyecto.
- Estado y baja física de postulaciones e invitaciones.
- Configuración del sistema.
- Reportes de perfil.
- Inmutabilidad del log.
- Consulta paginada con filtros para el administrador (UC-67).

---

## 2. Arquitectura

```text
Request HTTP
   │
   ├─ JwtAuthenticationFilter → SecurityContext (idUsuario)
   │
   ├─ @AuditarOperacion en el servicio
   │      └─ AuditoriaOperadorAspect
   │             ├─ abre/une una transacción
   │             ├─ SELECT set_config('app.id_usuario', '<id>', true)
   │             └─ ejecuta el método de negocio
   │
   ├─ Trigger de auditoría
   │      ├─ lee current_setting('app.id_usuario', true)
   │      ├─ inserta en auditoria
   │      └─ inserta detalle en detalle_auditoria
   │
   └─ Commit / rollback
```

El valor de `app.id_usuario` es **local a la transacción** (`set_config(..., true)`),
por lo que no se filtra entre requests del pool de conexiones.

---

## 3. Modelo

### 3.1. Catálogo

```sql
CREATE TABLE tipo_evento_auditoria (
    codigo          varchar(60)  PRIMARY KEY,
    nombre          varchar(120) NOT NULL,
    descripcion     varchar(255),
    modulo          varchar(40)  NOT NULL,
    activo          boolean      NOT NULL DEFAULT true
);
```

### 3.2. Evento

```sql
ALTER TABLE auditoria
    ADD COLUMN entidad_clave varchar(100),
    ADD COLUMN tipo_evento   varchar(60) REFERENCES tipo_evento_auditoria(codigo);
```

`id_usuario` pasa a significar **operador real**. La entidad afectada se
identifica con `entidad_afectada` + `id_afectado` (o `entidad_clave`).

### 3.3. Detalle

```sql
ALTER TABLE detalle_auditoria
    ALTER COLUMN valor_anterior TYPE text,
    ALTER COLUMN valor_nuevo    TYPE text,
    ALTER COLUMN valor_anterior DROP NOT NULL,
    ALTER COLUMN valor_nuevo    DROP NOT NULL;
```

---

## 4. Catálogo de eventos

| Código | Disparador | Detalle principal |
|---|---|---|
| `USUARIO_ALTA` | `usuario` INSERT | `estado`, `id_rol_global` |
| `USUARIO_BAJA` | `usuario` UPDATE estado `BAJA`/`PENDIENTE_BAJA` o DELETE | `estado` / `registro` |
| `USUARIO_DESHABILITADO` | `usuario` UPDATE estado `DESHABILITADO` | `estado`, `motivo_deshabilitacion`, `fecha_hasta_deshabilitacion` |
| `USUARIO_HABILITADO` | `usuario` UPDATE estado `ACTIVO` | `estado` |
| `USUARIO_ROL_CAMBIADO` | `usuario` UPDATE `id_rol_global` | `id_rol_global` |
| `PROYECTO_ESTADO_CAMBIADO` | `proyecto` UPDATE estado/privacidad o DELETE | `estado`, `privacidad`, `motivo_cancelacion` |
| `POSTULACION_ESTADO_CAMBIADO` | `postulacion_gral` / `postulacion_actividad` UPDATE estado | `estado` |
| `POSTULACION_ELIMINADA` | idem DELETE | `registro` |
| `INVITACION_ESTADO_CAMBIADO` | `invitacion_gral` / `invitacion_actividad` UPDATE estado | `estado` |
| `INVITACION_ELIMINADA` | idem DELETE | `registro` |
| `CONFIGURACION_ACTUALIZADA` | `configuracion_sistema` INSERT/UPDATE/DELETE | `valor`, `descripcion` |
| `REPORTE_ESTADO_CAMBIADO` | `reporte` INSERT/UPDATE | `estado`, `motivo`, `id_admin_revisor` |

---

## 5. Operador real

### 5.1. Anotación

Los métodos de servicio que modifican entidades auditadas se anotan con
`@AuditarOperacion`.

### 5.2. Aspecto

`AuditoriaOperadorAspect`:

1. abre una transacción con `TransactionTemplate` o se une a la existente;
2. llama a `AuditoriaSesionService.marcarOperadorActual()`;
3. ejecuta el método de negocio.

`AuditoriaSesionService` lee el principal de `SecurityContextHolder` y ejecuta:

```sql
SELECT set_config('app.id_usuario', '<id>', true);
```

### 5.3. Fallbacks

- `USUARIO_ALTA`: si no hay contexto, se atribuye al usuario creado.
- Reactivación de cuenta `PENDIENTE_BAJA → ACTIVO`: se atribuye al propio usuario.
- Scheduler automático: `id_usuario = NULL`.
- Acciones iniciadas por un administrador desde `/ejecutar-ahora`: el actor es el administrador.

---

## 6. UC-67 — Contrato

### 6.1. Listado

```http
GET /admin/auditoria?fechaDesde=2026-11-01&fechaHasta=2026-11-07&tipos=USUARIO_ALTA,USUARIO_BAJA&page=0&size=20
```

**Seguridad:** solo `ADMINISTRADOR`.

**Parámetros:**

| Parámetro | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `fechaDesde` | `date` ISO | Sí | Inicio inclusive |
| `fechaHasta` | `date` ISO | Sí | Fin inclusive |
| `tipos` | `string` repetible o separado por comas | No | Si falta, todos los activos |
| `page` | `int` | No | `>= 0`, default 0 |
| `size` | `int` | No | `1..100`, default 20 |

**Respuesta `200`:**

```json
{
  "contenido": [
    {
      "idAuditoria": 10,
      "fechaHora": "2026-11-07T10:15:30",
      "tipoEvento": "USUARIO_DESHABILITADO",
      "tipoEventoNombre": "Deshabilitación de usuario",
      "entidadAfectada": "usuario",
      "entidadClave": null,
      "idAfectado": 45,
      "idUsuario": 1,
      "usuarioNombre": "Admin ModaLink",
      "usuarioBd": "modalink_api",
      "ipUsuario": "203.0.113.10",
      "detalle": [
        {
          "campo": "estado",
          "valorAnterior": "ACTIVO",
          "valorNuevo": "DESHABILITADO"
        }
      ]
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 1,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

### 6.2. Catálogo de tipos

```http
GET /admin/auditoria/tipos
```

Devuelve los tipos activos con `codigo`, `nombre`, `descripcion` y `modulo`.

### 6.3. Tabla de decisión

| # | fechaDesde | fechaHasta | tipos | Resultado | HTTP |
|---|---|---|---|---|---|
| 1 | `null` | `null` | cualquiera | `FECHAS_OBLIGATORIAS` | 400 |
| 2 | válida | `null` | cualquiera | `FECHAS_OBLIGATORIAS` | 400 |
| 3 | `null` | válida | cualquiera | `FECHAS_OBLIGATORIAS` | 400 |
| 4 | desde > hasta | — | — | `RANGO_FECHAS_INVALIDO` | 400 |
| 5 | rango > máximo | — | — | `RANGO_FECHAS_EXCEDIDO` | 400 |
| 6 | válida | válida | vacío | Todos los tipos activos | 200 |
| 7 | válida | válida | 1 tipo válido | Solo ese tipo | 200 |
| 8 | válida | válida | varios tipos válidos | Unión sin duplicados | 200 |
| 9 | válida | válida | tipo inexistente | `TIPO_EVENTO_INVALIDO` | 400 |
| 10 | válida | válida | duplicados | Se deduplican | 200 |
| 11 | válida | válida | válidos, página fuera de rango | Página vacía | 200 |
| 12 | válida | válida | válidos, `size > 100` | `PAGINACION_INVALIDA` | 400 |
| 13 | válida | válida | válidos, `size <= 0` | `PAGINACION_INVALIDA` | 400 |

Los códigos de error se devuelven en el campo `codigo` del cuerpo estándar de
`GlobalExceptionHandler`.

---

## 7. Inmutabilidad

- Trigger `trg_auditoria_no_update_delete` en `auditoria`.
- Trigger `trg_detalle_auditoria_no_update_delete` en `detalle_auditoria`.
- Rol `modalink_api` con solo `INSERT`/`SELECT` sobre ambas tablas.
- `TRUNCATE` revocado al rol de la API.

---

## 8. Índices y rendimiento

Índices:

- `(fecha_hora DESC, tipo_evento)`
- `(entidad_afectada, id_afectado)`
- `(id_usuario, fecha_hora DESC)`
- `(id_auditoria)` en `detalle_auditoria`

Prueba de carga (`AuditoriaPerformanceIT`):

- 100.000 eventos en 180 días.
- Consulta de 30 días con 3 tipos.
- Resultado obtenido: **p50 = 47 ms, p95 = 57 ms**.
- Cota exigida: p95 < 2.000 ms.

---

## 9. Implementación

### 9.1. Base de datos

- `docs/3_diseño/ModaLinkBD.sql`
- `src/test/resources/ModaLinkBD.sql`
- `docs/3_diseño/migraciones/2026-11-06_uc67_auditoria_mvp.sql`

### 9.2. Backend

| Clase | Responsabilidad |
|---|---|
| `Auditoria` | Entidad de lectura del evento |
| `DetalleAuditoria` | Entidad de lectura del detalle |
| `TipoEventoAuditoria` | Catálogo |
| `AuditoriaRepository` | Consulta con Specification |
| `ListarAuditoriaService` | Validaciones, filtros y paginación |
| `AdminAuditoriaController` | `GET /admin/auditoria` y `/tipos` |
| `AuditarOperacion` | Anotación de métodos auditados |
| `AuditoriaSesionService` | Setea `app.id_usuario` |
| `AuditoriaOperadorAspect` | Abre/une transacción y propaga el operador |
| `AuditoriaFiltroInvalidoException` | Errores de UC-67 |

### 9.3. Servicios anotados

- `AdminUsuarioService.habilitar/deshabilitar`
- `SolicitudBajaService.solicitarBaja`
- `ReactivarCuentaService.reactivarCuenta`
- `PublicarProyectoService.publicar`
- `ConfirmarProyectoService.confirmar`
- `FinalizarProyectoService.finalizar`
- `CancelarProyectoService.cancelar`
- `AceptarPostulacionService` y `RechazarPostulacionService`
- `DarDeBajaPostulacionService`
- `AceptarInvitacionService`, `RechazarInvitacionService`, `EliminarInvitacionService`
- `ConfiguracionSistemaService` (actualizaciones y ejecuciones manuales)
- `AdminReportePerfilService.actualizarEstado`

---

## 10. Pruebas

| Tipo | Clase | Resultado |
|---|---|---|
| Unitario | `ListarAuditoriaServiceTest` | 14 tests OK |
| MVC | `AdminAuditoriaControllerTest` | 3 tests OK |
| Integración | `AuditoriaTriggerIntegrationTest` | 10 tests OK |
| Integración | `ReportePerfilIntegrationTest` | 5 tests OK (regresión) |
| Integración | `PublicarProyectoIntegrationTest` | 8 tests OK (regresión) |
| Carga | `AuditoriaPerformanceIT` | p95 = 57 ms |

---

## 11. Limitaciones

- No se auditan lecturas, login/logout ni intentos fallidos.
- La IP es la de la conexión a PostgreSQL.
- Los procesos automáticos no tienen actor.
- No hay retención ni hash chain.
- Las filas históricas no tienen `tipo_evento`.
