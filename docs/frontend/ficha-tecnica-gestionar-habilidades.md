# Ficha Técnica: Gestión de Habilidades (UC-56, UC-47, UC-48)

## 1. Alcance

Módulo de habilidades profesionales del sistema:

- UC-56: gestión administrativa del catálogo.
- UC-47: alta y baja de habilidades desde el perfil del usuario.
- UC-48: búsqueda de habilidades.

**Decisión de alcance:** las habilidades son globales. No tienen profesión asociada ni existe filtro por profesión. El catálogo se busca por nombre.

---

## 2. Endpoints

### 2.1 Búsqueda pública (UC-48)

| Método | Endpoint | Autorización |
|---|---|---|
| GET | `/habilidades?nombre=` | Usuario autenticado |

- `nombre` es opcional. Sin valor devuelve todas.
- Búsqueda parcial y case-insensitive.
- Orden alfabético por `nombre`.
- No hay paginación.

**Respuesta 200:**

```json
[
  {
    "idHabilidad": 1,
    "codigo": "FOTO_ESTUDIO",
    "nombre": "Fotografía de Estudio",
    "descripcion": "Manejo experto de iluminación y sets fotográficos en interiores."
  }
]
```

### 2.2 Administración (UC-56)

| Método | Endpoint | Permiso | Éxito |
|---|---|---|---|
| GET | `/admin/habilidades?nombre=` | `VER_HABILIDADES` | `200` |
| GET | `/admin/habilidades/{id}` | `VER_HABILIDADES` | `200` |
| POST | `/admin/habilidades` | `CREAR_HABILIDAD` | `201` |
| PUT | `/admin/habilidades/{id}` | `MODIFICAR_HABILIDAD` | `200` |
| DELETE | `/admin/habilidades/{id}` | `ELIMINAR_HABILIDAD` | `204` |

**Request POST/PUT:**

```json
{
  "nombre": "Fotografía de Estudio",
  "descripcion": "Manejo experto de iluminación y sets fotográficos."
}
```

- `nombre`: obligatorio, máximo 255.
- `descripcion`: opcional, máximo 255. Si viene vacía se guarda `null`.
- `codigo` no se envía: lo genera el backend.

**Response:**

```json
{
  "idHabilidad": 1,
  "codigo": "FOTO_ESTUDIO",
  "nombre": "Fotografía de Estudio",
  "descripcion": "Manejo experto de iluminación y sets fotográficos."
}
```

### 2.3 Gestión desde el perfil (UC-47)

| Método | Endpoint | Autorización | Éxito |
|---|---|---|---|
| GET | `/perfiles/{idPerfil}/habilidades` | Dueño del perfil | `200` |
| POST | `/perfiles/{idPerfil}/habilidades` | Dueño del perfil | `201` |
| DELETE | `/perfiles/{idPerfil}/habilidades/{idHabilidad}` | Dueño del perfil | `204` |

**Request POST:**

```json
{
  "idHabilidad": 3
}
```

**Response POST:**

```json
{
  "idHabilidad": 3,
  "codigo": "EDICION_DIGITAL",
  "nombre": "Retoque y Edición Digital",
  "descripcion": "Postproducción digital y corrección cromática avanzada."
}
```

Reglas:

- El perfil debe pertenecer al usuario autenticado.
- El perfil no debe estar en estado `Baja`.
- La habilidad debe existir.
- No se puede agregar dos veces la misma habilidad al mismo perfil.
- No se puede quitar una habilidad que el perfil no tiene.
- No se emiten notificaciones por altas/bajas desde el perfil.

---

## 3. Reglas de negocio

### 3.1 Código automático

El código se deriva del nombre:

- Mayúsculas.
- Sin acentos.
- Caracteres no alfanuméricos reemplazados por `_`.
- Máximo 50 caracteres.
- Si el código ya existe, se agrega `_2`, `_3`, etc.
- Se genera una sola vez al crear la habilidad y no cambia al modificar el nombre.

Ejemplo: `"Fotografía de Estudio"` → `FOTOGRAFIA_DE_ESTUDIO`.

### 3.2 Unicidad

- El nombre no puede repetirse, comparando sin distinguir mayúsculas y recortando espacios.
- En edición se excluye la propia habilidad.

### 3.3 Baja administrativa

- Si la habilidad está usada por un requerimiento general o de actividad, la baja se rechaza con `409`.
- Si no está en requerimientos, se eliminan sus asociaciones con perfiles y luego se elimina la habilidad.
- Se notifica a cada perfil que tenía la habilidad.
- La baja es física.

### 3.4 Modificación administrativa

- Se actualizan nombre y descripción.
- Si no hubo cambios reales, no se notifica.
- Si hubo cambios, se notifica a cada perfil que tiene la habilidad.
- Las asociaciones con perfiles no se alteran.

### 3.5 Notificaciones

Tipos nuevos:

| Tipo | Título | Categoría |
|---|---|---|
| `HABILIDAD_MODIFICADA` | Habilidad modificada | `ADMINISTRACION` |
| `HABILIDAD_ELIMINADA` | Habilidad eliminada | `ADMINISTRACION` |

- Se crea una notificación por perfil afectado.
- `entidadTipo`: `HABILIDAD`.
- `entidadId`: ID de la habilidad.
- `perfilContexto`: perfil afectado.
- Se generan después del commit.
- Un fallo de notificación no revierte la operación administrativa.

---

## 4. Errores

| Caso | HTTP |
|---|---|
| Nombre o descripción inválidos | `400` |
| Habilidad inexistente | `404` |
| Perfil inexistente o de otro usuario | `404` |
| Usuario sin acceso | `404` |
| Nombre duplicado | `409` |
| Habilidad ya asociada al perfil | `409` |
| Habilidad no asociada al perfil | `409` |
| Habilidad en uso por requerimientos | `409` |
| Perfil en estado `Baja` | `409` |
| Sin permiso administrativo | `403` |
| Sin autenticación | `401` |

El formato de error es el estándar del backend (`message`, `httpStatus`, `timestamp`; `errores` en validaciones).

---

## 5. Consideraciones para frontend

### 5.1 Administración

- Tabla con nombre, descripción y código.
- Buscador por nombre.
- Formulario de alta/edición con nombre y descripción; el código se muestra como dato de solo lectura.
- Confirmación previa a la baja.
- Si la baja responde `409`, informar que la habilidad está en uso por proyectos.

### 5.2 Perfil

- Catálogo con búsqueda por nombre.
- Multi-select o agregado individual.
- El alta devuelve la habilidad asociada.
- La baja no devuelve body.
- Refrescar el listado después de cada operación.
- No mostrar selector de profesión para habilidades: son globales.

### 5.3 Permisos

Los administradores con sesión abierta deben volver a iniciar sesión después de aplicar los permisos nuevos para que el JWT los incluya.

---

## 6. Integración con otros módulos

### 6.1 Crear/editar actividad y proyecto

- Los formularios de requerimientos aceptan `habilidades` como `array<long>`.
- El catálogo para el multi-select se obtiene con `GET /habilidades?nombre=`.
- Las habilidades son globales: no se filtran por profesión.
- Enviar únicamente IDs existentes. Un ID inexistente responde `404`.
- No reutilizar nombres como IDs.

### 6.2 Perfil

- `PerfilDetalleResponse` y `PerfilBusquedaResponse` siguen devolviendo los nombres de las habilidades.
- Para gestionar altas/bajas y obtener IDs, usar los endpoints de `/perfiles/{idPerfil}/habilidades`.
- La ficha de perfiles existente no se modificó; este documento concentra el contrato nuevo.

### 6.3 Administración

- Los endpoints administrativos viven bajo `/admin/habilidades`.
- La ficha del módulo admin existente no se modificó; este documento concentra el contrato nuevo.
- La baja y la modificación notifican a los perfiles asociados.

### 6.4 Permisos y migración

- Permisos: `VER_HABILIDADES`, `CREAR_HABILIDAD`, `MODIFICAR_HABILIDAD`, `ELIMINAR_HABILIDAD`.
- Los administradores deben reloguearse después de aplicar los permisos para que el JWT los incluya.
- La migración local está en `docs/3_diseño/migraciones/2026-11-03_uc56_uc47_uc48_habilidades.sql`.
- No hay cambios estructurales en `habilidad` ni en `habilidad_perfil`.

## 7. Fuera de alcance

- Profesión asociada a la habilidad.
- Filtro de búsqueda por profesión.
- Paginación del catálogo.
- Notificaciones por altas/bajas realizadas por el usuario desde su perfil.
