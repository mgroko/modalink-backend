# Ficha Técnica — UC-74 Gestionar Unidades de Medida

> **Actor:** Administrador · **Objetivos:** OBJ-01, OBJ-04 · **Requisito:** IRQ-02
> **Backend:** `AdminUnidadMedidaService` + `AdminUnidadMedidaController`
> **Base path:** `/admin/unidades-medida`
> **Auth:** `Authorization: Bearer <token>` (JWT)

---

## 1. Modelo de datos

**Request (`AdminUnidadMedidaRequest`)** — usado en crear y actualizar:

| Campo | Tipo | Obligatorio | Máx | Restricciones |
|---|---|---|---|---|
| `nombre` | string | Sí | 50 | Único (case-insensitive) |
| `simbolo` | string | Sí | 50 | Único (case-insensitive) |
| `tipoDatoPermitido` | string | Sí | 50 | Solo `"NUMERICO"` o `"TEXTO"` (se normaliza a mayúsculas) |

> ⚠️ **Exclusivo del backend:** `ENUMERADO` no es un valor válido (check `chk_caract_tipo_dato_enum_id_unidad`). El selector de tipo debe ofrecer solo NUMERICO / TEXTO.

**Response (`UnidadMedidaResponse`):**

```json
{ "idUnidad": 1, "nombre": "Kilogramo", "simbolo": "kg", "tipoDatoPermitido": "NUMERICO" }
```

---

## 2. Endpoints

| # | Método | Ruta | Permiso (`hasAuthority`) | Éxito | Cuerpo |
|---|---|---|---|---|---|
| 1 | `GET` | `/admin/unidades-medida?tipoDato=` | `VER_CARACTERISTICAS` | 200 | `UnidadMedidaResponse[]` |
| 2 | `GET` | `/admin/unidades-medida/{id}` | `VER_CARACTERISTICAS` | 200 | `UnidadMedidaResponse` |
| 3 | `POST` | `/admin/unidades-medida` | `CREAR_CARACTERISTICA` | **201** | `UnidadMedidaResponse` |
| 4 | `PUT` | `/admin/unidades-medida/{id}` | `MODIFICAR_CARACTERISTICA` | 200 | `UnidadMedidaResponse` |
| 5 | `DELETE` | `/admin/unidades-medida/{id}` | `ELIMINAR_CARACTERISTICA` | **204** | vacío |

**Listado (paso 1 del UC):** `GET /admin/unidades-medida` devuelve todo ordenado alfabéticamente (A→Z, case-insensitive). Filtro opcional `?tipoDato=NUMERICO|TEXTO`.

**Detalle (paso 2.1.2):** `GET /admin/unidades-medida/{id}` precarga el formulario de modificación.

### Ejemplos

```http
GET /admin/unidades-medida HTTP/1.1
Authorization: Bearer <token>
```

```http
POST /admin/unidades-medida HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json

{ "nombre": "Kilogramo", "simbolo": "kg", "tipoDatoPermitido": "NUMERICO" }
```

```http
PUT /admin/unidades-medida/1 HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json

{ "nombre": "Kilogramo", "simbolo": "kg", "tipoDatoPermitido": "NUMERICO" }
```

```http
DELETE /admin/unidades-medida/1 HTTP/1.1
Authorization: Bearer <token>
```

---

## 3. Respuesta de error (estándar)

```json
{ "message": "<texto legible>", "httpStatus": 409, "timestamp": 1730000000000 }
```

Errores de validación `@Valid` (400):

```json
{
  "message": "Validación fallida",
  "errores": {
    "nombre": "must not be blank",
    "simbolo": "must not be blank",
    "tipoDatoPermitido": "must not be blank"
  },
  "httpStatus": 400,
  "timestamp": 1730000000000
}
```

| Caso UC | HTTP | `message` típico | UI sugerida |
|---|---|---|---|
| Campos vacíos / >50 chars (exc. paso 4, 2.1.3) | **400** | `Validación fallida` + `errores` por campo | Resaltar campos en rojo, mantener modal abierto |
| `tipoDatoPermitido` inválido | **400** | `tipoDatoPermitido debe ser NUMERICO o TEXTO.` | Toast de error |
| Nombre/símbolo duplicado (exc. 4, 2.1.3) | **409** | `Ya existe una unidad de medida con el nombre X.` / `... con el símbolo X.` | Toast + foco en el campo |
| Unidad en uso por característica (exc. 2.2.3) | **409** | `La unidad de medida está asociada a una característica técnica y no puede eliminarse.` | Modal de error, cerrar confirmación |
| Cambio de tipo incompatible con características asociadas | **409** | `La unidad de medida está en uso por características de tipo [..] y no puede cambiarse a Y. Características afectadas: [..].` | Toast con detalle |
| Id inexistente | **404** | `Unidad de medida no encontrada: {id}` | Recargar listado |
| Sin permiso | **403** | — | Ocultar botones según permisos |

---

## 4. Mapeo de flujos → UI

### Paso 1 — Listado + acciones

- Tabla con columnas: **Nombre**, **Símbolo**, **Tipo de dato** (badge `NUMERICO`/`TEXTO`), **acciones**.
- Tres botones: *Agregar nueva unidad de medida*, *Modificar unidad de medida*, *Eliminar unidad de medida* (habilitar según permiso del usuario).

### Pasos 2–5 — Crear

- Modal/form con 3 campos obligatorios: `nombre`, `simbolo`, `tipoDatoPermitido` (select: NUMERICO | TEXTO).
- `POST` → **201**: agregar a la tabla + toast de éxito.
- **400/409**: mostrar `message`, no cerrar el modal, volver al paso 3.

### Flujo alternativo 2.1 — Modificar

- Click en fila → `GET /{id}` → abrir modal precargado (Nombre, Símbolo, Tipo).
- `PUT /{id}` → **200**: actualizar fila + toast.
- **404/409/400**: toast, mantener modal (volver al paso 2.1.2).

### Flujo alternativo 2.2 — Eliminar

- Click en fila → modal de confirmación que muestra los datos de la unidad (paso 2.2.2).
- `DELETE /{id}` → **204**: quitar de la tabla + toast.
- **409**: mostrar el mensaje de "asociada a característica técnica" y cerrar/abortar (caso de uso finaliza).

### Rendimiento

Operaciones ≤ 2 s → usar spinner/loading state en el botón de confirmación.

---

## 5. Diferencias / notas para el frontend

1. **Notificación post-modificación (Postcondición 2.1):** el backend **no** emite notificaciones a perfiles con características asociadas — es un pendiente fuera de este service; el FE no debe esperar ningún dato de respuesta adicional.
2. **Validación extra no mencionada en el UC:** al modificar, si se cambia `tipoDatoPermitido`, el backend verifica compatibilidad con las características técnicas que usan la unidad (409 con detalle).
3. **Eliminar no muestra baja lógica:** es un borrado físico (`delete`), 204 sin cuerpo.
4. **`simbolo` también es único:** el UC solo lo menciona en la excepción 2.1.3, pero aplica también al crear.
5. **Filtro de listado** `?tipoDato=` no está en el UC: opcional, útil para pestañas/filtro en la tabla.
