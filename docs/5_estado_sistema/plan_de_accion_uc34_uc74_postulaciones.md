# Plan de acción — UC-34 Gestionar postulaciones y UC-74 Buscar postulaciones

**Fecha:** 2026-10-27  
**Estado:** Fases 1–8 completadas (modelo de datos, cupo, UC-74, aceptación/rechazo, ajustes de UC-26, concurrencia y documentación).  
**Alcance:** UC-34 (aceptar/rechazar postulaciones), UC-74 (buscar postulaciones), ajustes necesarios en UC-26 (alta de postulaciones) y preparación para UC-31 (invitaciones), UC-60 (confirmar proyecto) y UC-70 (asignar actividad).  
**Reemplaza:** las definiciones preliminares discutidas para UC-34. El plan de CPM y disponibilidad (`plan_de_accion_cpm_disponibilidad_confirmacion_v3.md`) sigue vigente para UC-60/UC-70.

---

## 0. Resumen ejecutivo

UC-34 se implementa con:

1. **Aceptar postulación general:** crea una membresía activa vinculada al requerimiento general, pasa la postulación a `ACEPTADA` y notifica al postulante.
2. **Aceptar postulación de actividad:** reutiliza la membresía si el postulante ya es miembro; revalida disponibilidad; crea la `AsignacionActividad` vinculada al requerimiento de actividad; pasa la postulación a `ACEPTADA` y notifica.
3. **Rechazar postulación:** pasa la postulación a `RECHAZADA` y notifica. No borra físicamente.
4. **Buscar postulaciones (UC-74):** endpoint por proyecto, con filtros, paginación y orden por estado (`PENDIENTE`, `ACEPTADA`, `RECHAZADA`).
5. **Cupo y satisfacción:** se calculan con miembros/asignaciones **activos** vinculados al requerimiento, no con postulaciones aceptadas.
6. **Auto-rechazo:** cuando el cupo se completa, las demás postulaciones `PENDIENTE` del mismo requerimiento pasan a `RECHAZADA`. Los rechazados no pueden volver a postularse (D9), pero el director podrá invitarlos cuando UC-31 esté implementado.
7. **Historial:** `miembros_proyecto` pasa a ser una tabla de períodos (una fila por alta), con fechas y vínculo al requerimiento. Se agrega la vista `v_historial_miembros`. No se crea una tabla `historial_miembros`.

---

## 1. Decisiones cerradas

| ID | Decisión |
| :--- | :--- |
| D1 | Rechazar no elimina físicamente: la postulación pasa a `RECHAZADA`. |
| D2 | La aceptación genera notificación `POSTULACION_ACEPTADA` al postulante. |
| D3 | El requerimiento se satisface parcialmente: si pide 2 y hay 1, `cubiertos = 1`, `faltantes = 1`, `satisfecho = false`. No se decrementa `cantidad`. |
| D4 | Aceptar una postulación de actividad crea una `AsignacionActividad`. |
| D5 | Se puede aceptar en proyectos `PUBLICADO` y `CONFIRMADO` si el requerimiento está insatisfecho. También se pueden crear nuevas postulaciones en `CONFIRMADO` si falta personal (opción A). |
| D6 | Un miembro activo con rol `MIEMBRO` puede postularse a una actividad si su profesión coincide. No puede postularse a un requerimiento general. Un `DIRECTOR` no puede postularse nunca, aunque pueda ser asignado a actividades por UC-70. |
| D7 | La aceptación de una actividad revalida disponibilidad. |
| D8 | Aceptar/rechazar se serializa con locks pesimistas y revalidación de estado. |
| D9 | Una postulación rechazada no permite volver a postularse al mismo requerimiento. La invitación futura sí puede usarse (opción A). |
| D10 | Se implementa UC-74 Buscar postulaciones, anidado por proyecto, con el mismo patrón que las demás búsquedas. UC-75 es Gestionar unidades de medida. |
| D11 | Se mantiene el auto-rechazo al completar el cupo (opción A), corrigiendo los triggers para contar miembros/asignaciones activos. |
| D12 | Se permiten nuevas postulaciones en proyectos `CONFIRMADO` mientras el requerimiento esté insatisfecho (opción A). |
| D13 | Un postulante rechazado puede ser invitado en el futuro; D9 sólo bloquea la nueva postulación (opción A). |
| D14 | El historial de membresías se guarda como períodos en `miembros_proyecto` (una fila por alta), con índice parcial de membresía activa y una vista de consulta. No se crea tabla `historial_miembros`. |

---

## 2. Estado actual y brechas

### Ya existe

- Entidades `PostulacionGral`, `PostulacionActividad`, `MiembroProyecto` y `AsignacionActividad`.
- Alta de postulaciones (UC-26) con validaciones de proyecto, profesión, cupo, duplicados y disponibilidad.
- Notificación `POSTULACION_RECIBIDA` a directores.
- Permiso `GESTIONAR_POSTULACIONES` asignado al rol `DIRECTOR`.
- `ProyectoSecurityService.validarPermiso` y `validarDirector`.
- Tipo `POSTULACION_ACEPTADA` sembrado en `notificacion_tipo`.
- Triggers de cupo y de asignación, hoy basados en postulaciones `ACEPTADA`.

### Falta

- Endpoints de aceptar/rechazar.
- Servicio que cree miembros y asignaciones desde postulaciones aceptadas.
- Búsqueda de postulaciones por proyecto.
- Vínculo entre membresía/asignación y requerimiento.
- Historial de períodos de membresía.
- Triggers corregidos para contar miembros/asignaciones activos.
- Ajuste de UC-26 para `CONFIRMADO` y para miembros que postulan a actividades.
- Ajuste de `satisfecho` en las respuestas de proyecto.

---

## 3. Modelo de datos final

### 3.1. Nuevas columnas

```sql
ALTER TABLE miembros_proyecto
    ADD COLUMN fecha_alta timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN fecha_baja timestamp,
    ADD COLUMN id_requerimiento_gral int8;

ALTER TABLE miembros_proyecto
    ADD CONSTRAINT fk_miembro_requerimiento_gral
        FOREIGN KEY (id_requerimiento_gral)
        REFERENCES requerimiento_gral_proyecto(id_requerimiento_gral);

ALTER TABLE asignacion_actividad
    ADD COLUMN id_requerimiento_act int8;

ALTER TABLE asignacion_actividad
    ADD CONSTRAINT fk_asignacion_requerimiento_act
        FOREIGN KEY (id_requerimiento_act)
        REFERENCES requerimiento_actividad(id_requerimiento_act);

ALTER TABLE miembros_proyecto
    ADD CONSTRAINT chk_miembro_fechas
    CHECK (fecha_baja IS NULL OR fecha_baja >= fecha_alta);
```

**Semántica:**

- `miembros_proyecto.fecha_alta`: inicio del período de membresía.
- `miembros_proyecto.fecha_baja`: fin del período; `NULL` mientras está activo.
- `miembros_proyecto.id_requerimiento_gral`: requerimiento general que ocupa esa membresía. `NULL` para directores, miembros incorporados por actividad sin requerimiento general, o invitaciones sin requerimiento.
- `asignacion_actividad.id_requerimiento_act`: requerimiento de actividad que satisface esa asignación.

No se agrega columna de auditoría de usuario.

### 3.2. Índices y restricciones

```sql
-- Sólo una membresía activa por perfil y proyecto.
-- Se permiten filas históricas (bajas).
CREATE UNIQUE INDEX uq_miembro_proyecto_activo
    ON miembros_proyecto (id_proyecto, id_perfil)
    WHERE estado_participacion = 'ACTIVO';

-- Ya existe en el esquema base como "UQ_asignacion_miembro_actividad".
-- Se conserva; no hay que crearla de nuevo.
-- CREATE UNIQUE INDEX uq_asignacion_miembro_actividad
--     ON asignacion_actividad (id_miembro, id_actividad);

-- Conteos de cupo.
CREATE INDEX idx_miembro_requerimiento_gral_activo
    ON miembros_proyecto (id_requerimiento_gral)
    WHERE estado_participacion = 'ACTIVO';

CREATE INDEX idx_asignacion_requerimiento_act
    ON asignacion_actividad (id_requerimiento_act);
```

### 3.3. Vista `v_historial_miembros`

```sql
CREATE OR REPLACE VIEW v_historial_miembros AS
SELECT
    mp.id_miembro,
    mp.id_proyecto,
    p.nombre AS nombre_proyecto,
    mp.id_perfil,
    pf.nombre_artistico,
    mp.id_rol_proyecto,
    rp.nombre AS nombre_rol,
    mp.id_requerimiento_gral,
    rg.descripcion AS descripcion_requerimiento,
    rg.id_profesion,
    pr.nombre AS nombre_profesion,
    mp.estado_participacion,
    mp.fecha_alta,
    mp.fecha_baja,
    mp."motivoBaja"
FROM miembros_proyecto mp
JOIN proyecto p
  ON p.id_proyecto = mp.id_proyecto
JOIN perfil pf
  ON pf.id_perfil = mp.id_perfil
JOIN rol_proyecto rp
  ON rp.id_rol_proyecto = mp.id_rol_proyecto
LEFT JOIN requerimiento_gral_proyecto rg
  ON rg.id_requerimiento_gral = mp.id_requerimiento_gral
LEFT JOIN profesion pr
  ON pr.id_profesion = rg.id_profesion
ORDER BY mp.id_proyecto, mp.id_perfil, mp.fecha_alta;
```

**Vista opcional para asignaciones:**

```sql
CREATE OR REPLACE VIEW v_historial_asignaciones_actividad AS
SELECT
    aa.id_asignacion_act,
    aa.id_actividad,
    a.nombre AS nombre_actividad,
    aa.id_requerimiento_act,
    ra.descripcion AS descripcion_requerimiento,
    aa.id_miembro,
    mp.id_proyecto,
    mp.id_perfil,
    mp.estado_participacion,
    mp.fecha_alta AS fecha_alta_miembro,
    mp.fecha_baja AS fecha_baja_miembro
FROM asignacion_actividad aa
JOIN actividad a
  ON a.id_actividad = aa.id_actividad
JOIN requerimiento_actividad ra
  ON ra.id_requerimiento_act = aa.id_requerimiento_act
JOIN miembros_proyecto mp
  ON mp.id_miembro = aa.id_miembro
ORDER BY mp.id_proyecto, mp.id_perfil, aa.id_actividad;
```

La vista es de lectura. No reemplaza la tabla de períodos ni se usa para las validaciones de negocio.

### 3.4. Triggers de cupo corregidos

Los triggers dejan de contar postulaciones `ACEPTADA` y pasan a contar **miembros/asignaciones activos vinculados al requerimiento**.

#### `fn_control_cupo_postulacion_gral`

```sql
DECLARE
    v_cantidad_requerida INT;
    v_cubiertos INT;
BEGIN
    IF NEW.estado = 'ACEPTADA'
       AND (OLD.estado IS NULL OR OLD.estado <> 'ACEPTADA') THEN

        SELECT rgp.cantidad INTO v_cantidad_requerida
        FROM requerimiento_gral_proyecto rgp
        WHERE rgp.id_requerimiento_gral = NEW.id_requerimiento_gral;

        SELECT COUNT(*) INTO v_cubiertos
        FROM miembros_proyecto mp
        WHERE mp.id_requerimiento_gral = NEW.id_requerimiento_gral
          AND mp.estado_participacion = 'ACTIVO';

        IF v_cantidad_requerida IS NOT NULL
           AND v_cubiertos >= v_cantidad_requerida THEN
            -- Mismo bloque de rechazo de las demás PENDIENTE
            -- y de notificación POSTULACION_RECHAZADA que existe hoy.
        END IF;
    END IF;

    RETURN NEW;
END
```

#### `fn_control_cupo_postulacion_actividad`

```sql
SELECT COUNT(*) INTO v_cubiertos
FROM asignacion_actividad aa
JOIN miembros_proyecto mp
  ON mp.id_miembro = aa.id_miembro
WHERE aa.id_requerimiento_act = NEW.id_requerimiento_act
  AND mp.estado_participacion = 'ACTIVO';
```

El resto de la función (rechazo de pendientes y notificación) se conserva.

#### Orden obligatorio en el servicio

```text
1. Lock de proyecto → requerimiento → postulación.
2. Validar estado y cupo con miembros/asignaciones activos.
3. Crear el miembro y/o la asignación.
4. saveAndFlush del miembro/asignación.
5. Pasar la postulación a ACEPTADA y saveAndFlush (dispara el trigger).
6. Registrar POSTULACION_ACEPTADA.
```

Si el miembro o la asignación no se flush-ean antes del cambio de estado, el trigger no los cuenta y el cierre de cupo queda mal.

### 3.5. Entidades impactadas

#### `MiembroProyecto`

Agregar:

```java
@Column(name = "fecha_alta", nullable = false)
private LocalDateTime fechaAlta;

@Column(name = "fecha_baja")
private LocalDateTime fechaBaja;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_requerimiento_gral")
private RequerimientoGralProyecto requerimientoGral;
```

`fechaAlta` se asigna con `@PrePersist` si la entidad no la trae, y la BD mantiene el `DEFAULT` como respaldo. En los servicios nuevos se puede setear explícitamente con el `Clock` inyectado si se necesita determinismo en tests.

#### `AsignacionActividad`

Agregar:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "id_requerimiento_act")
private RequerimientoActividad requerimientoActividad;
```

### 3.6. Repositorios

**`MiembroProyectoRepository`**

- `existsByProyectoIdProyectoAndPerfilIdPerfil(...)` para visibilidad sin asumir una única fila.
- `findFirstByProyectoIdProyectoAndPerfilIdPerfilOrderByFechaAltaDesc(...)` para obtener la membresía más reciente.
- `findHistorial...` por proyecto/perfil ordenado por `fechaAlta`.
- Conteo de activos por requerimiento general.

**`AsignacionActividadRepository`**

- `existsByMiembro_IdMiembroAndActividad_IdActividad(...)`.
- Conteo de asignaciones activas por requerimiento de actividad.

**`PostulacionGralRepository` / `PostulacionActividadRepository`**

- Búsqueda con locks `PESSIMISTIC_WRITE` para aceptar/rechazar.
- Consultas de búsqueda por proyecto con `JOIN FETCH` de perfil, usuario, profesión y requerimiento.
- Filtros por estado, tipo, profesión, nombre artístico y fechas.

**`RequerimientoGralProyectoRepository` / `RequerimientoActividadRepository`**

- `findByIdParaGestion` con lock pesimista.

**`ProyectoRepository`**

- `findByIdParaGestion` con lock pesimista.

### 3.7. Migración

- La base actual tiene datos de prueba, sin postulaciones aceptadas ni miembros reales: **no requiere backfill**.
- Actualizar los dos scripts:
  - `docs/3_diseño/ModaLinkBD.sql`
  - `src/test/resources/ModaLinkBD.sql`
- Script incremental para bases existentes: `docs/3_diseño/migracion_uc34_uc74.sql` (idempotente).
- El script incremental ya se aplicó a la base local `modalink`.
- Antes de aplicar los índices únicos en una base con datos, auditar duplicados:
  - membresías activas duplicadas por `(id_proyecto, id_perfil)`;
  - asignaciones duplicadas por `(id_miembro, id_actividad)`.
- `UQ_asignacion_miembro_actividad` ya existía en el esquema base; no se duplica.

---

## 4. Reglas de negocio

### 4.1. Cupo y satisfacción

**Requerimiento general:**

```text
cubiertos = COUNT(miembros_proyecto
                  WHERE id_requerimiento_gral = req
                    AND estado_participacion = 'ACTIVO')
faltantes = max(0, cantidad - cubiertos)
satisfecho = cubiertos >= cantidad
```

**Requerimiento de actividad:**

```text
cubiertos = COUNT(asignacion_actividad aa
                  JOIN miembros_proyecto mp
                  WHERE aa.id_requerimiento_act = req
                    AND mp.estado_participacion = 'ACTIVO')
faltantes = max(0, cantidad - cubiertos)
satisfecho = cubiertos >= cantidad
```

Una baja o eliminación de la membresía reduce `cubiertos` automáticamente. Una asignación de un miembro inactivo no cuenta.

### 4.2. Aceptar postulación general

Precondiciones:

- postulación `PENDIENTE`;
- proyecto `PUBLICADO` o `CONFIRMADO`;
- requerimiento insatisfecho (`faltantes > 0`);
- perfil postulante activo;
- postulante no es miembro activo del proyecto.

Pasos:

1. Validar perfil activo, director y permiso `GESTIONAR_POSTULACIONES`.
2. Lock de proyecto, requerimiento y postulación.
3. Revalidar estado y cupo.
4. Crear una nueva fila `MiembroProyecto`:
   - `estadoParticipacion = ACTIVO`;
   - rol `MIEMBRO`;
   - `id_requerimiento_gral = requerimiento`;
   - `fechaAlta = ahora`.
5. Flush del miembro.
6. Cambiar la postulación a `ACEPTADA` y flush (dispara el trigger).
7. Registrar `POSTULACION_ACEPTADA`.
8. Devolver `PostulacionGestionadaResponse`.

No se reutiliza una membresía activa: si el postulante ya es miembro activo, se rechaza con `409`. La única excepción es la actividad, que se trata en 4.3.

### 4.3. Aceptar postulación de actividad

Precondiciones:

- postulación `PENDIENTE`;
- proyecto `PUBLICADO` o `CONFIRMADO`;
- actividad `PENDIENTE`;
- requerimiento de actividad insatisfecho;
- perfil postulante activo;
- si ya es miembro activo, debe ser `MIEMBRO` (no `DIRECTOR`) y tener la profesión requerida.

Pasos:

1. Validaciones de seguridad y locks.
2. Revalidar estado, cupo y actividad `PENDIENTE`.
3. Revalidar disponibilidad con `CalendarioService`; si no hay, `409` y no se modifica nada.
4. Si el postulante **no** es miembro activo, crear la membresía como en 4.2, con `id_requerimiento_gral = NULL` (la cobertura se registra en la asignación).
5. Si ya es miembro activo `MIEMBRO`, reutilizar su membresía; no crear otra.
6. Crear `AsignacionActividad(miembro, actividad, id_requerimiento_act)`.
7. Flush de membresía/asignación.
8. Cambiar la postulación a `ACEPTADA` y flush.
9. Registrar `POSTULACION_ACEPTADA`.
10. Devolver respuesta con `idMiembro` e `idAsignacionActividad`.

El trigger `trg_notificar_asignacion_actividad` genera además `ACTIVIDAD_ASIGNADA`. Es esperado: el postulante recibe “postulación aceptada” y “asignado a la actividad”.

### 4.4. Rechazar postulación

1. Validar perfil activo, director y permiso.
2. Lock y validar que la postulación exista, pertenezca al proyecto y esté `PENDIENTE`.
3. Cambiar a `RECHAZADA`.
4. Registrar `POSTULACION_RECHAZADA`.
5. Devolver respuesta sin miembro ni asignación.

No se borra la fila. No se libera cupo (el cupo se mide por activos). El rechazado no puede volver a postularse al mismo requerimiento (D9), pero podrá ser invitado en el futuro.

### 4.5. Auto-rechazo

- Cuando una aceptación completa el cupo (`cubiertos >= cantidad`), el trigger rechaza las demás `PENDIENTE` del mismo requerimiento y notifica.
- El auto-rechazo es definitivo para la postulación: esas personas quedan `RECHAZADA` y no pueden re-postularse.
- UC-74 mostrará las rechazadas; en el futuro UC-31 permitirá invitarlas.
- Si después hay una baja, el requerimiento vuelve a estar insatisfecho. Los rechazados siguen rechazados; se puede incorporar gente nueva por postulación o por invitación.

### 4.6. Estados y UC-26

**Proyecto:**

| Estado | Crear postulación | Aceptar postulación |
| :--- | :---: | :---: |
| `PUBLICADO` | Sí, si el requerimiento está insatisfecho | Sí, si está insatisfecho |
| `CONFIRMADO` | Sí, si el requerimiento está insatisfecho (D12) | Sí, si está insatisfecho |
| `BORRADOR` | No | No |
| `FINALIZADO` | No | No |
| `CANCELADO` | No | No |

**Actividad:** sólo `PENDIENTE` para crear y aceptar. Para `EN_CURSO` se definirá otra regla en UC-70 o en una iteración posterior.

**UC-26 — ajustes:**

- `validarProyecto` acepta `PUBLICADO` y `CONFIRMADO`; sigue rechazando el resto.
- `validarCupoGeneral` y `validarCupoActividad` usan miembros/asignaciones activos.
- `validarPerfilApto`:
  - general: miembro activo → `409`;
  - actividad: miembro activo `MIEMBRO` con profesión coincidente → permitido;
  - actividad: miembro activo `DIRECTOR` → `409`;
  - no miembro → flujo normal.
- `ObtenerProyectoService.elegibleParaPostularse` contempla `PUBLICADO` y `CONFIRMADO`.
- Las respuestas de requerimientos exponen `cubiertos`, `faltantes` y `satisfecho`.

### 4.7. Directores

- Un director no puede postularse, ni a requerimientos generales ni a actividades.
- Un director sí puede ser asignado a una actividad por UC-70 (caso de uso futuro).
- Si existiera una postulación pendiente de un director por datos legados, la aceptación debe rechazarse con `409`.

### 4.8. Reingreso e historial

- Cada alta crea una nueva fila en `miembros_proyecto`.
- Cada baja actualiza `estado_participacion`, `fecha_baja` y `motivoBaja`.
- Sólo puede haber una fila `ACTIVO` por perfil y proyecto.
- El reingreso se hace por invitación (UC-31, futuro), no por nueva postulación si ya existe una postulación rechazada o aceptada para ese requerimiento.
- La vista `v_historial_miembros` permite consultar todos los períodos.

---

## 5. API

### 5.1. UC-74 — Buscar postulaciones

```http
GET /proyectos/{idProyecto}/postulaciones/buscar
```

**Autorización:** perfil activo que sea director activo del proyecto y tenga `GESTIONAR_POSTULACIONES`.

**Filtros opcionales (`BuscarPostulacionesFiltro`):**

| Parámetro | Tipo | Descripción |
| :--- | :--- | :--- |
| `estado` | `List<EstadoSolicitud>` | Repetible; `PENDIENTE`, `ACEPTADA`, `RECHAZADA`. |
| `tipo` | `TipoPostulacion` | `GENERAL` o `ACTIVIDAD`. |
| `nombrePerfil` | `String` | Búsqueda por nombre artístico. |
| `idProfesion` | `Long` | Profesión del postulante o del requerimiento. |
| `fechaDesde` | `LocalDate` | Fecha de postulación desde. |
| `fechaHasta` | `LocalDate` | Fecha de postulación hasta. |
| `page`, `size` | `int` | Paginación. |
| `todos` | `boolean` | Devuelve todo sin paginar. |

**Orden:**

1. `PENDIENTE` (primero), `ACEPTADA`, `RECHAZADA`.
2. Dentro de cada estado, `fechaPostulacion DESC` (más recientes primero).

**Implementación:** `BuscarPostulacionesProyectoService` consulta por separado generales y actividades con `Specification`, une los resultados en memoria, los ordena y pagina. Es una búsqueda acotada al proyecto; si el volumen crece, se puede reemplazar por un `UNION ALL` nativo sin cambiar el contrato.

**Respuesta:** `PaginaResponse<PostulacionBusquedaResponse>`.

```json
{
  "idPostulacion": 77,
  "tipo": "GENERAL",
  "estado": "PENDIENTE",
  "fechaPostulacion": "2026-10-27T10:15:00",
  "idPerfil": 12,
  "nombreArtistico": "Luna",
  "idProfesion": 2,
  "nombreProfesion": "Modelo",
  "idRequerimiento": 30,
  "descripcionRequerimiento": "Vacante para desfile",
  "idActividad": null,
  "nombreActividad": null,
  "cantidadRequerida": 2,
  "cubiertos": 1,
  "faltantes": 1,
  "satisfecho": false,
  "aportaCupo": false,
  "puedeAceptar": true,
  "puedeRechazar": true
}
```

**Campos derivados:**

- `cubiertos`, `faltantes`, `satisfecho`: calculados por el servicio de cupo.
- `aportaCupo`: `true` si la postulación está `ACEPTADA` y su miembro/asignación sigue activa. Permite distinguir una aceptada cuyo miembro se dio de baja.
- `puedeAceptar` / `puedeRechazar`: comodidad para la UI; el backend siempre revalida al ejecutar.

### 5.2. Aceptar y rechazar

| Método | Ruta | Acción |
| :--- | :--- | :--- |
| `POST` | `/proyectos/{idProyecto}/postulaciones/general/{idPostulacion}/aceptar` | Aceptar general |
| `POST` | `/proyectos/{idProyecto}/postulaciones/general/{idPostulacion}/rechazar` | Rechazar general |
| `POST` | `/proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/aceptar` | Aceptar actividad |
| `POST` | `/proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/rechazar` | Rechazar actividad |

Sin body. La confirmación de los pasos 4–5 del UC es una interacción de UI; el `POST` representa la acción ya confirmada.

**Respuesta `200`:**

```json
{
  "idPostulacion": 77,
  "tipo": "ACTIVIDAD",
  "estado": "ACEPTADA",
  "idProyecto": 5,
  "idRequerimiento": 20,
  "idPerfil": 12,
  "idMiembro": 88,
  "idAsignacionActividad": 45
}
```

En rechazo: `estado=RECHAZADA`, `idMiembro=null`, `idAsignacionActividad=null`.

### 5.3. DTOs nuevos

En `postulaciones/dto`:

- `BuscarPostulacionesFiltro`
- `PostulacionBusquedaResponse`
- `PostulacionGestionadaResponse`

En `postulaciones/mapper`:

- `PostulacionMapper` para mapear entidades y campos derivados.

### 5.4. Errores

| HTTP | Caso | Excepción |
| :---: | :--- | :--- |
| `400` | Parámetros inválidos | Validación de filtros |
| `401` | Sin sesión | Seguridad |
| `403` | No es director / sin permiso | `AccesoDenegadoProyectoException` |
| `404` | Sin perfil activo | `PerfilActivoNoSeleccionadoException` |
| `404` | Proyecto inexistente | `ProyectoNoEncontradoException` |
| `404` | Postulación inexistente o de otro proyecto | `PostulacionNoEncontradaException` |
| `409` | Postulación no `PENDIENTE` | `PostulacionYaGestionadaException` |
| `409` | Proyecto en estado no permitido | `PostulacionNoPermitidaException` |
| `409` | Actividad no `PENDIENTE` | `PostulacionNoPermitidaException` |
| `409` | Requerimiento sin cupo | `PostulacionNoPermitidaException` |
| `409` | Perfil ya es miembro activo (general) | `PostulacionNoPermitidaException` |
| `409` | Director intenta postularse | `PostulacionNoPermitidaException` |
| `409` | Perfil inactivo | `PerfilEnBajaException` |
| `409` | Sin disponibilidad | `PostulacionNoPermitidaException` |
| `409` | Asignación o membresía duplicada | `PostulacionNoPermitidaException` / `DataIntegrityViolationException` |

Nuevas excepciones:

- `PostulacionNoEncontradaException` → `404`.
- `PostulacionYaGestionadaException` → `409`.

---

## 6. Cambios por capa

### 6.1. Servicios

```text
postulaciones/servicio/
  AceptarPostulacionService.java
  RechazarPostulacionService.java
  BuscarPostulacionesProyectoService.java
  PostulacionGestionSupport.java
  CupoRequerimientoService.java
```

**`CupoRequerimientoService`** (compartido con UC-26, UC-36 y futuras invitaciones):

- `cubiertosGenerales(idRequerimientoGral)`
- `cubiertosActividad(idRequerimientoAct)`
- `faltantesGenerales(...)`
- `faltantesActividad(...)`
- `satisfechoGeneral(...)`
- `satisfechoActividad(...)`

**`PostulacionGestionSupport`:**

- perfil activo;
- carga y validación del proyecto;
- permiso y director;
- locks y validación de pertenencia;
- construcción de notificaciones.

**`AceptarPostulacionService`:**

- `aceptarGeneral(...)`.
- `aceptarActividad(...)`.

**`RechazarPostulacionService`:**

- `rechazarGeneral(...)`.
- `rechazarActividad(...)`.

**`BuscarPostulacionesProyectoService`:**

- búsqueda paginada, filtros y orden por estado.

### 6.2. Controlador

Extender `PostulacionController` con los cinco endpoints. Reutilizar los helpers de `Authentication` existentes.

### 6.3. Notificaciones

Agregar en `NotificacionService`:

```java
public static final String TIPO_POSTULACION_ACEPTADA = "POSTULACION_ACEPTADA";
```

- Aceptación: `POSTULACION_ACEPTADA`, `entidadTipo = POSTULACION_GRAL` o `POSTULACION_ACTIVIDAD`, destinatario el postulante.
- Rechazo: `POSTULACION_RECHAZADA`.
- En actividad, el trigger agrega `ACTIVIDAD_ASIGNADA`.

### 6.4. Ajustes en UC-26

- `CrearPostulacionService.validarProyecto`.
- `CrearPostulacionService.validarPerfilApto`.
- `CrearPostulacionService.validarCupoGeneral/Actividad`.
- `CrearPostulacionService` usa `CupoRequerimientoService`.

### 6.5. Ajustes en respuestas de proyecto

- `ObtenerProyectoService`, `PublicarProyectoService` y `ModificarProyectoService` dejan de usar `existsBy...ACEPTADA` para `satisfecho` y usan `CupoRequerimientoService`.
- `RequerimientoGralResponse` ahora expone `satisfecho`, `cubiertos` y `faltantes`; el frontend debe consumir los tres campos.
- `ModificarProyectoService` alinea la restricción de “requerimiento satisfecho” con la nueva definición basada en activos.

---

## 7. Pruebas

### 7.1. Unitarias

- `AceptarPostulacionServiceTest`:
  - acepta general: crea miembro activo, vincula requerimiento, cambia estado, notifica;
  - acepta actividad: crea asignación, revalida disponibilidad, reutiliza membresía si ya es miembro;
  - postulación inexistente/otro proyecto/ya gestionada;
  - proyecto no permitido, actividad no pendiente, sin cupo, director, miembro en general, perfil inactivo;
  - rollback si falla la notificación o la asignación.
- `RechazarPostulacionServiceTest`:
  - estado `RECHAZADA`, notificación, sin miembro/asignación;
  - ya gestionada, inexistente, otro proyecto.
- `BuscarPostulacionesProyectoServiceTest`:
  - filtros, orden por estado, paginación, `todos`, campos derivados.
- `CupoRequerimientoServiceTest`:
  - conteo con activos/inactivos, general/actividad, faltantes y satisfecho.

### 7.2. Controlador

`PostulacionControllerTest`:

- aceptar/rechazar → `200`;
- sin perfil activo → `404`;
- errores mapeados (`404`, `409`, `403`);
- búsqueda: parámetros, página y orden.

### 7.3. Integración (PostgreSQL/Testcontainers)

`GestionarPostulacionIntegrationTest`:

1. Aceptar general:
   - crea `miembros_proyecto` activo con `id_requerimiento_gral`;
   - notificación `POSTULACION_ACEPTADA`;
   - `satisfecho` y `cubiertos` correctos.
2. Aceptar general y completar cupo:
   - trigger rechaza pendientes y notifica;
   - no hay duplicados de notificación.
3. Aceptar actividad:
   - crea asignación con `id_requerimiento_act`;
   - trigger `ACTIVIDAD_ASIGNADA`;
   - miembro existente no duplica membresía.
4. Baja de miembro:
   - `satisfecho=false`, `faltantes>0`;
   - se puede aceptar otra postulación;
   - historial conserva el período anterior.
5. Rechazo:
   - no crea miembro/asignación;
   - notificación correcta.
6. Concurrencia:
   - dos aceptaciones simultáneas no superan el cupo.
7. Seguridad:
   - no director → `403`;
   - otro proyecto → `404`.
8. Vista:
   - `v_historial_miembros` devuelve períodos ordenados y datos de proyecto/perfil/requerimiento.

### 7.4. Smoke manual

- Proyecto `PUBLICADO` con dos postulaciones.
- Buscar postulaciones (UC-74) y ver orden.
- Aceptar una, verificar miembro, notificación y cupo.
- Rechazar otra, verificar estado.
- Repetir acción → `409`.
- En `CONFIRMADO` con requerimiento insatisfecho, crear y aceptar una postulación.

---

## 8. Fases de implementación

| Fase | Entregable | Criterio de salida | Estado |
| :--- | :--- | :--- | :--- |
| 0 | Plan y modelo cerrados | Este documento aprobado. | Completada |
| 1 | SQL: columnas, índices, vista, triggers corregidos, entidades y repositorios | Compila; tests existentes verdes. | Completada |
| 2 | `CupoRequerimientoService` y ajustes de `satisfecho` en respuestas de proyecto | Conteos correctos con activos/inactivos. | Completada |
| 3 | UC-74: búsqueda de postulaciones por proyecto | Endpoint, filtros, orden y tests. | Completada |
| 4 | Aceptar general | Servicio, endpoint, trigger y tests. | Completada |
| 5 | Aceptar actividad | Asignación, disponibilidad, membresía existente y tests. | Completada |
| 6 | Rechazar general y actividad | Servicio, endpoint y tests. | Completada |
| 7 | Ajustes UC-26 (`CONFIRMADO`, miembro en actividad, director) | Tests de alta actualizados. | Completada |
| 8 | Concurrencia, seguridad, documentación y smoke | Suite completa y fichas actualizadas. | Completada |

**Estimación orientativa:** 4–5 días de desarrollo y pruebas.

---

## 9. Documentación a actualizar

| Archivo | Cambio |
| :--- | :--- |
| `docs/3_diseño/ModaLinkBD.sql` | Columnas, FKs, índices, vistas y triggers. |
| `src/test/resources/ModaLinkBD.sql` | Mismos cambios para Testcontainers. |
| `docs/5_estado_sistema/estado_uc.md` | UC-34 y UC-74 a “Implementado y testeado” al finalizar. |
| `docs/frontend/ficha-tecnica-gestionar-postulaciones.md` | Nueva ficha de UC-34. |
| `docs/frontend/ficha-tecnica-buscar-postulaciones.md` | Nueva ficha de UC-74. |
| `docs/frontend/ficha-tecnica-postular-a-proyecto.md` | Reglas de `CONFIRMADO`, miembro en actividad y director. |
| `docs/2_analisis/contratos.md` | Nota: rechazo lógico, no borrado físico. |
| `docs/calidad/plan_de_pruebas.md` | Casos de UC-34/UC-74. |
| `docs/5_estado_sistema/plan_de_accion_cpm_disponibilidad_confirmacion_v3.md` | Referencia a este plan como desbloqueo de D-20/D13. |

---

## 10. Criterios de aceptación

- [ ] Aceptar una postulación `PENDIENTE` la pasa a `ACEPTADA`.
- [ ] La aceptación general crea una membresía activa vinculada al requerimiento.
- [ ] La aceptación de actividad crea la asignación vinculada al requerimiento y revalida disponibilidad.
- [ ] Si el postulante ya es miembro `MIEMBRO`, la aceptación de actividad no duplica la membresía.
- [ ] Un director no puede postularse; un miembro no puede postularse a un requerimiento general.
- [ ] `satisfecho` es `false` mientras `cubiertos < cantidad` y `true` al completar el cupo.
- [ ] Una baja reduce `cubiertos` y vuelve a habilitar la aceptación.
- [ ] Al completar el cupo, las demás pendientes pasan a `RECHAZADA` y se notifican.
- [ ] El rechazo no crea miembro/asignación y notifica.
- [ ] Un rechazado no puede volver a postularse al mismo requerimiento.
- [ ] UC-74 devuelve todas las postulaciones del proyecto, ordenadas `PENDIENTE`, `ACEPTADA`, `RECHAZADA`.
- [ ] UC-74 muestra `cubiertos`, `faltantes`, `satisfecho` y `aportaCupo`.
- [ ] Se puede crear y aceptar una postulación en `CONFIRMADO` si el requerimiento está insatisfecho.
- [ ] `v_historial_miembros` devuelve los períodos de membresía con proyecto, perfil, rol, requerimiento, estado y fechas.
- [ ] No se puede operar sobre postulaciones de otro proyecto.
- [ ] Un usuario sin permiso recibe `403`.
- [ ] Dos aceptaciones concurrentes no superan el cupo.
- [ ] Tests unitarios, de controlador e integración en verde.

---

## 11. Riesgos y pendientes

1. **UC-31 no implementado:** el reingreso por invitación no se puede probar de punta a punta todavía. El modelo de períodos y el vínculo con el requerimiento quedan preparados.
2. **Auto-rechazo y D9:** los auto-rechazados no pueden volver a postularse. La única vía de retorno será la invitación futura. Es el comportamiento acordado.
3. **Historial de asignaciones:** `asignacion_actividad` sigue siendo estado actual; si se elimina una asignación, su rastro queda en la postulación aceptada y en la vista de asignaciones. Si se necesita historial completo de asignaciones, habrá que agregar fechas/estado o una tabla de eventos en otra iteración.
4. **UC-36:** la restricción de modificar requerimientos “satisfechos” debe alinearse con la nueva definición basada en activos.
5. **UC-60:** permitir nuevas postulaciones en `CONFIRMADO` cambia la regla de “el proyecto deja de aceptar postulaciones”. Documentarlo en UC-60.
6. **Triggers:** la corrección debe probarse con aceptaciones reales; el orden `crear miembro/asignación → flush → actualizar postulación` es obligatorio.
7. **Datos reales:** si la base objetivo tiene datos productivos, auditar duplicados antes de crear los índices únicos y evaluar backfill de vínculos.
8. **Orden dentro de cada estado:** se definió `fechaPostulacion DESC`; si se prefiere FIFO para pendientes, ajustar la consulta y la ficha de UC-74.

---

## 12. Estado de implementación — Fases 1 a 8

**Completado:**

- `docs/3_diseño/ModaLinkBD.sql` y `src/test/resources/ModaLinkBD.sql`:
  - columnas `fecha_alta`, `fecha_baja`, `id_requerimiento_gral` en `miembros_proyecto`;
  - columna `id_requerimiento_act` en `asignacion_actividad`;
  - FKs, check de fechas, índices parciales y vistas de historial;
  - triggers de cupo corregidos para contar miembros/asignaciones activos.
- `docs/3_diseño/migracion_uc34_uc74.sql`: migración incremental idempotente.
- `MiembroProyecto`: nuevos campos y `@PrePersist` para `fecha_alta`.
- `AsignacionActividad`: vínculo con `RequerimientoActividad`.
- `MiembroProyectoRepository`: `existsBy...`, `findFirst...`, historial, búsqueda con rol y conteo por requerimiento general.
- `AsignacionActividadRepository`: existencia por miembro/actividad y conteo activo por requerimiento de actividad.
- `ProyectoVisibilidadService`: usa `existsBy...` para no asumir una única fila histórica.

**Fase 2:**

- `CupoRequerimiento` (DTO) y `CupoRequerimientoService`: fuente única de verdad para `cantidad`, `cubiertos`, `faltantes` y `satisfecho`.
- `RequerimientoGralResponse` agrega `cubiertos` y `faltantes`.
- `ObtenerProyectoService`, `PublicarProyectoService` y `ModificarProyectoService` usan `CupoRequerimientoService` y dejaron de depender de `PostulacionGralRepository` para `satisfecho`.
- `ModificarProyectoService` alinea la restricción de requerimientos “satisfechos” con el conteo de activos.
- `CupoRequerimientoServiceTest` con casos parcial, completo, exceso y sin requerimiento.

**Validación realizada:**

- `mvn -q -DskipTests compile`: OK.
- `mvn -q -DskipTests test-compile`: OK.
- Tests unitarios seleccionados (`CrearPostulacionServiceTest`, `ProyectoSecurityServiceTest`, `CrearProyectoServiceTest`, `PublicarProyectoServiceTest`, `ModificarProyectoServiceTest`, `ObtenerProyectoServiceTest`, `CupoRequerimientoServiceTest`, `ProyectoControllerTest`): OK.
- Script completo de `docs/3_diseño/ModaLinkBD.sql` aplicado sobre una base temporal PostgreSQL 17: OK; vistas y columnas verificadas.
- Script completo de `src/test/resources/ModaLinkBD.sql` aplicado sobre una base temporal: OK.
- Migración incremental aplicada a la base local `modalink`: OK.
- Suite de integración ejecutada con Testcontainers: 143 tests, 141 OK, 2 fallos preexistentes ajenos a UC-34/UC-74:
  - `CrearPostulacionIntegrationTest.actividad_sinHuecoLibre_lanzaExcepcionYNoPersiste` (dependiente de la hora/día).
  - `EliminarActividadIntegrationTest.eliminar_borraActividadYSusVinculosConservandoMiembrosYNotificando` (reprogramación de actividad).
  - Ambos fallos se reprodujeron también en un worktree limpio sobre `HEAD`, sin los cambios de UC-34.
- Suite unitaria completa (excluyendo integración): 913 tests, 10 errores preexistentes en `CrearActividadServiceTest`, `ModificarActividadServiceTest` y `ObtenerActividadServiceTest` por `CronogramaService.calcular` devolviendo `null` en los mocks. También se reprodujo en un worktree limpio sobre `HEAD`.

**Fase 3:**

- `BuscarPostulacionesFiltro`, `PostulacionBusquedaResponse`.
- `PostulacionSpecifications` para generales y actividades.
- `PostulacionMapper` para el DTO de búsqueda.
- `BuscarPostulacionesProyectoService`: autorización de director, filtros, unión de ambos tipos, orden por estado/fecha y paginación en memoria.
- `GET /proyectos/{idProyecto}/postulaciones/buscar`.
- `RangoFechasPostulacionInvalidoException` + handler `400`.
- `@EntityGraph` y `JpaSpecificationExecutor` en repositorios de postulaciones.
- `AsignacionActividadRepository.findActivasDeProyectoConRequerimiento` para calcular `aportaCupo`.
- Tests: `BuscarPostulacionesProyectoServiceTest`, `PostulacionControllerTest` y dos casos de integración en `CrearPostulacionIntegrationTest`.

**Validación de Fase 3:**

- `mvn -q -DskipTests test-compile`: OK.
- `BuscarPostulacionesProyectoServiceTest` y `PostulacionControllerTest`: OK.
- Integración `buscarPostulaciones_devuelveLasDelProyectoConCupoYFiltros` y `buscarPostulaciones_sinPermisoDeDirector_lanzaExcepcion`: OK.

**Fase 4:**

- `AceptarPostulacionService`: aceptar postulación general con locks, validación de cupo, creación de membresía, cambio de estado y notificación.
- `PostulacionGestionadaResponse`.
- `PostulacionNoEncontradaException` y `PostulacionYaGestionadaException` + handlers.
- `POST /proyectos/{idProyecto}/postulaciones/general/{idPostulacion}/aceptar`.
- `TIPO_POSTULACION_ACEPTADA` en `NotificacionService`.
- Locks `findByIdParaGestion` en proyecto, requerimiento general y postulación general.
- Tests unitarios `AceptarPostulacionServiceTest`, de controlador y de integración:
  - crea miembro activo vinculado al requerimiento;
  - notifica `POSTULACION_ACEPTADA`;
  - el trigger rechaza las demás pendientes y notifica `POSTULACION_RECHAZADA`;
  - repetir la aceptación devuelve `409`;
  - no director devuelve `403`;
  - acepta en proyecto `CONFIRMADO`.

**Validación de Fase 4:**

- `mvn -q -DskipTests test-compile`: OK.
- `AceptarPostulacionServiceTest` y `PostulacionControllerTest`: OK.
- Integración de aceptación general (3 casos): OK.

**Fase 5:**

- `AceptarPostulacionService.aceptarActividad`.
- Locks `findByIdParaGestion` en postulación de actividad y requerimiento de actividad.
- Revalidación de disponibilidad con `CalendarioService`.
- Creación de `AsignacionActividad` vinculada al requerimiento.
- Reutilización de membresía existente `MIEMBRO`; un director no puede postularse.
- `POST /proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/aceptar`.
- Tests de integración:
  - crea asignación, membresía y notificaciones `POSTULACION_ACEPTADA` + `ACTIVIDAD_ASIGNADA`;
  - sin disponibilidad no crea membresía ni asignación;
  - miembro existente reutiliza su membresía;
  - un director no puede ser aceptado como postulante de actividad;
  - al llenar el cupo, el trigger rechaza otra postulación pendiente.

**Validación de Fase 5:**

- `mvn -q -DskipTests test-compile`: OK.
- `AceptarPostulacionServiceTest` y `PostulacionControllerTest`: OK.
- Integración de aceptación de actividad (5 casos): OK.

**Fase 6:**

- `RechazarPostulacionService`: rechazo general y de actividad.
- `POST /proyectos/{idProyecto}/postulaciones/general/{idPostulacion}/rechazar`.
- `POST /proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/rechazar`.
- Rechazo lógico (`RECHAZADA`), sin crear miembro ni asignación.
- Notificación `POSTULACION_RECHAZADA` al postulante.
- Reutiliza `PostulacionNoEncontradaException`, `PostulacionYaGestionadaException` y `PostulacionNoPermitidaException`.
- Tests unitarios `RechazarPostulacionServiceTest`, de controlador e integración:
  - rechazo general cambia estado, no crea miembro y notifica;
  - rechazo de actividad cambia estado, no crea asignación y notifica;
  - repetir el rechazo devuelve `409`.

**Validación de Fase 6:**

- `mvn -q -DskipTests test-compile`: OK.
- `RechazarPostulacionServiceTest` y `PostulacionControllerTest`: OK.
- Integración de rechazo general y de actividad (2 casos): OK.

**Fase 7:**

- UC-26 permite proyectos `PUBLICADO` y `CONFIRMADO` si el requerimiento está insatisfecho.
- `validarCupoGeneral` y `validarCupoActividad` usan `CupoRequerimientoService` (miembros/asignaciones activos).
- Un miembro activo `MIEMBRO` puede postularse a una actividad con la profesión requerida.
- Un miembro activo no puede postularse a un requerimiento general.
- Un director no puede postularse, ni a actividades.
- `ObtenerProyectoService.elegibleParaPostularse` contempla `CONFIRMADO`.
- Tests unitarios actualizados y nuevos casos:
  - postulación general en `CONFIRMADO`;
  - miembro `MIEMBRO` postulándose a actividad;
  - director bloqueado;
  - cupo por activos.
- Tests de integración:
  - miembro activo postula a actividad;
  - director intenta postular a actividad y es rechazado;
  - miembro activo intenta postular a general y es rechazado;
  - postulación general en `CONFIRMADO`.

**Validación de Fase 7:**

- `mvn -q -DskipTests clean test-compile`: OK.
- `CrearPostulacionServiceTest`: OK.
- Integración de ajustes UC-26 (4 casos): OK.

**Fase 8:**

- Prueba de concurrencia `GestionarPostulacionConcurrencyIntegrationTest`: dos
  aceptaciones simultáneas de la misma postulación; una gana, la otra recibe
  conflicto, y solo se crea una membresía y una notificación.
- Prueba de seguridad en controlador para `403`.
- Fichas frontend:
  - `docs/frontend/ficha-tecnica-gestionar-postulaciones.md`;
  - `docs/frontend/ficha-tecnica-buscar-postulaciones.md`.
- Actualización de `ficha-tecnica-postular-a-proyecto.md`.
- Nota de implementación en `docs/2_analisis/contratos.md`.
- `docs/calidad/plan_de_pruebas.md` con los casos de UC-34/UC-74.
- Actualización de `estado_uc.md` y de este plan.

**Validación de Fase 8:**

- `mvn -q -DskipTests clean test-compile`: OK.
- Suite focalizada de UC-34/UC-74: OK.
- Smoke de integración: **16/16** casos de `CrearPostulacionIntegrationTest` + **1/1** de concurrencia.
- Fallos preexistentes de actividades/CPM documentados y ajenos a estos cambios.

**Descubrimiento:** `UQ_asignacion_miembro_actividad` ya existía en el esquema base; se conserva y no se crea de nuevo.

**Estado final:** UC-34 y UC-74 implementados y testeados.
