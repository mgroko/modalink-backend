# Ficha técnica: asignación única por usuario y actividad

**Estado:** propuesta para aplicar.  
**Alcance:** UC-26 (postulación a actividad), UC-34 (aceptación de postulación), UC-70 (asignación directa) y restricción de integridad en base de datos.

---

## 1. Regla de negocio

Un usuario puede tener varios perfiles activos en el mismo proyecto y puede participar en distintas actividades con ellos.

Lo que no puede ocurrir es que **el mismo usuario tenga más de una asignación activa a la misma actividad**, aunque las asignaciones provengan de perfiles distintos.

En otras palabras:

- Mismo usuario + mismo proyecto + **actividades distintas** → permitido.
- Usuarios distintos + misma actividad → permitido.
- Mismo usuario + misma actividad + **perfiles distintos** → prohibido.
- Perfil histórico/inactivo → no bloquea una nueva asignación.

La disponibilidad horaria sigue siendo **por usuario**, no por perfil. Esta regla es independiente de la disponibilidad: es una condición de elegibilidad de la asignación.

---

## 2. Endpoints afectados

### 2.1 Asignación directa

```http
POST /proyectos/{idProyecto}/actividades/{idActividad}/asignaciones
```

Si el usuario del perfil destino ya tiene una asignación activa a esa actividad, responde:

```http
409 Conflict
```

```json
{
  "message": "El usuario ya está asignado a esta actividad con otro perfil.",
  "httpStatus": 409,
  "timestamp": 1730000000000
}
```

El mensaje del mismo perfil ya asignado se mantiene:

```json
{
  "message": "El miembro ya está asignado a esta actividad.",
  "httpStatus": 409
}
```

### 2.2 Creación de postulación a actividad

```http
POST /proyectos/{idProyecto}/postulaciones/actividad
```

Si el usuario ya está asignado a esa actividad, la creación de la postulación se rechaza con:

```http
409 Conflict
```

```json
{
  "message": "Ya estás asignado a esta actividad con otro perfil.",
  "httpStatus": 409
}
```

Esto evita generar postulaciones que luego no podrían aceptarse.

### 2.3 Aceptación de postulación a actividad

```http
POST /proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/aceptar
```

Si el usuario de la postulación ya está asignado a esa actividad con otro perfil, responde:

```http
409 Conflict
```

```json
{
  "message": "El usuario ya está asignado a esta actividad con otro perfil.",
  "httpStatus": 409
}
```

La validación ocurre antes de crear la membresía, reprogramar o insertar la asignación.

---

## 3. Qué significa `disponible` en el preview

El endpoint de disponibilidad sigue respondiendo únicamente por **disponibilidad horaria**:

```http
GET /proyectos/{idProyecto}/actividades/{idActividad}/disponibilidad
```

Por eso puede devolver:

```json
{
  "disponible": true,
  "miembrosBloqueantes": []
}
```

aunque el usuario ya esté asignado a la actividad con otro perfil. Eso no es un error: el preview responde si existe un hueco común, no si el usuario es elegible para una nueva asignación.

La restricción se aplica al **crear la postulación, aceptarla o asignar directamente**, no al consultar disponibilidad.

Si el frontend necesita deshabilitar el botón antes de llamar al endpoint, debe usar un campo de elegibilidad separado. No se debe reutilizar `disponible` para mezclar disponibilidad horaria con duplicidad de usuario.

---

## 4. Casos válidos que deben seguir funcionando

1. Dos usuarios distintos asignados a la misma actividad.
2. Un usuario con dos perfiles en el mismo proyecto asignado a dos actividades distintas.
3. Un usuario con dos perfiles en proyectos distintos.
4. Un usuario cuyo perfil anterior quedó inactivo y se reincorpora con otro perfil.
5. Asignación de un director sin consumir cupo de requerimiento.
6. Desasignación y posterior reasignación con otro perfil.

---

## 5. Implementación en backend

### 5.1 Validación en servicios

Se agregó una consulta por usuario:

```java
existsAsignacionActivaDeUsuarioEnActividad(idActividad, idUsuario)
```

La consulta considera únicamente membresías con:

```text
estado_participacion = 'ACTIVO'
```

Se aplica en:

- `AsignarActividadService`
- `CrearPostulacionService`
- `AceptarPostulacionService`

La validación se ejecuta antes de:

- evaluar disponibilidad,
- reprogramar la actividad,
- crear una membresía,
- insertar la asignación.

### 5.2 Restricción en base de datos

Se agregó:

- función `fn_validar_asignacion_unica_usuario()`;
- trigger `trg_asignacion_unica_usuario`;
- índice `IDX_asignacion_actividad`.

El trigger se dispara antes de insertar o actualizar `id_miembro`/`id_actividad` y rechaza la operación con SQLState `23505` y el marcador:

```text
ASIGNACION_DUPLICADA_USUARIO
```

El backend traduce ese marcador a `409 Conflict` con el mensaje:

```text
El usuario ya está asignado a esta actividad con otro perfil.
```

### 5.3 Membresías históricas

El trigger y la consulta solo consideran membresías activas. Una asignación de una membresía con estado `BAJA_VOLUNTARIA` o `ELIMINADO` no impide que el usuario vuelva a ser asignado con otro perfil.

---

## 6. Migración y datos existentes

La migración manual está en:

```text
docs/3_diseño/migraciones/2026-11-04_asignacion_unica_por_usuario.sql
```

El diagnóstico de duplicados existentes está en:

```text
docs/3_diseño/migraciones/2026-11-04_diagnostico_asignaciones_duplicadas_usuario.sql
```

La migración **no elimina datos automáticamente**. Los duplicados existentes deben revisarse y limpiarse manualmente porque cada asignación puede:

- consumir un cupo de requerimiento;
- pertenecer a una profesión distinta;
- haber generado notificaciones;
- estar referenciada en postulaciones o invitaciones.

---

## 7. Verificación esperada

### Tests unitarios

- Asignación directa rechaza mismo usuario con otro perfil.
- Creación de postulación rechaza mismo usuario ya asignado.
- Aceptación de postulación rechaza mismo usuario ya asignado.
- No se guarda asignación ni se reprograma cuando se rechaza.

### Tests de integración

- Mismo usuario con dos perfiles activos en el mismo proyecto y misma actividad → `409`.
- Mismo usuario con dos perfiles y actividades distintas → permitido.
- Dos usuarios distintos en la misma actividad → permitido.
- Perfil anterior inactivo → permite reincorporación con otro perfil.
- Trigger de base de datos rechaza un `INSERT` directo duplicado.
- Handler traduce el error del trigger a `409`.

---

## 8. Checklist para frontend

- [ ] Mostrar el mensaje de `409` cuando el backend rechace por usuario ya asignado.
- [ ] No interpretar `disponible: true` como “puede asignarse”.
- [ ] Si se quiere deshabilitar el botón antes de llamar al endpoint, solicitar un campo de elegibilidad separado.
- [ ] Refrescar la lista de asignados después de un `409`.
- [ ] Permitir seleccionar otro perfil del mismo usuario para **otra actividad**, pero no para la misma.
