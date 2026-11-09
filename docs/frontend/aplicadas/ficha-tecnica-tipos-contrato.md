# Ficha técnica — UC-76 Gestionar tipos de contrato

**Módulo:** Administración / Contratos  
**Actor:** Administrador  
**Estado backend:** implementado con tests unitarios, MVC e integración con PostgreSQL (Testcontainers).  
**Estado frontend:** implementado en el repo `modalink-frontend`; QA manual pendiente a cargo del equipo.

---

## 1. Resumen

El administrador administra el catálogo global `tipo_acuerdo_contrato`, que define los tipos de contrato disponibles al generar o modificar un contrato.

El ABM permite:

- listar tipos activos e inactivos;
- crear un tipo con nombre, descripción y plantilla legal;
- editar nombre, descripción y plantilla;
- activar o desactivar un tipo;
- eliminar físicamente un tipo sin contratos asociados;
- validar la plantilla y sus placeholders.

El código se genera automáticamente desde el nombre y es **inmutable**.

---

## 2. Reglas de negocio

| Regla | Comportamiento |
|---|---|
| Código | Generado desde el nombre. No se puede editar. |
| Nombre | Obligatorio, máximo 100 caracteres. Único entre todos los tipos. |
| Descripción | Opcional, máximo 200 caracteres. Editable siempre. |
| Plantilla | Obligatoria, máximo 100.000 caracteres. |
| Edición de nombre/plantilla | Bloqueada si existen contratos `BORRADOR`, `PENDIENTE_FIRMA` o `ACTIVO`. |
| Edición con contratos cerrados | Permitida si solo hay `RECHAZADO_POR_PARTES`, `CANCELADO` o `ELIMINADO`. |
| Desactivación | Permitida si no hay `PENDIENTE_FIRMA` ni `BORRADOR` y no es el último tipo activo. |
| Contratos `ACTIVO` | No impiden desactivar el tipo. |
| Eliminación física | Solo si no existe ningún contrato asociado. |
| Último tipo activo | No se puede desactivar ni eliminar. |
| Contratos existentes | El texto legal está congelado en `contrato.texto` y `contrato_version`; cambiar la plantilla no los altera. |
| Auditoría | Alta, modificación, cambio de estado y baja. La plantilla se audita como longitud + MD5. |

---

## 3. Endpoints

| Método | Endpoint | Permiso |
|---|---|---|
| `GET` | `/admin/tipos-contrato?nombre=&incluirInactivos=true` | `VER_TIPOS_CONTRATO` |
| `GET` | `/admin/tipos-contrato/{id}` | `VER_TIPOS_CONTRATO` |
| `POST` | `/admin/tipos-contrato` | `CREAR_TIPO_CONTRATO` |
| `PUT` | `/admin/tipos-contrato/{id}` | `MODIFICAR_TIPO_CONTRATO` |
| `PATCH` | `/admin/tipos-contrato/{id}/estado` | `MODIFICAR_TIPO_CONTRATO` |
| `DELETE` | `/admin/tipos-contrato/{id}` | `ELIMINAR_TIPO_CONTRATO` |
| `POST` | `/admin/tipos-contrato/validar-plantilla` | `CREAR_TIPO_CONTRATO` |

### 3.1. Respuesta de listado

```json
[
  {
    "idTipoAcuerdo": 1,
    "codigo": "ACUERDO_GENERAL",
    "nombre": "Acuerdo general",
    "descripcion": "Acuerdo general entre las partes.",
    "activo": true,
    "contratosAsociados": 3,
    "contratosVivos": 2,
    "contratosPendientes": 1,
    "puedeEditar": false,
    "puedeDesactivar": false,
    "puedeEliminar": false,
    "motivoNoEditable": "Tiene contratos pendientes o activos asociados.",
    "motivoNoDesactivable": "Tiene contratos pendientes de firma.",
    "motivoNoEliminable": "Tiene 3 contrato(s) asociado(s)."
  }
]
```

### 3.2. Alta

```json
{
  "nombre": "Acuerdo de confidencialidad",
  "descripcion": "NDA aplicable a los proyectos.",
  "plantilla": "ACUERDO DE CONFIDENCIALIDAD\n\nProyecto: {{proyecto}}\nDirector: {{director}}\nPartes:\n{{partes}}\n\n{{clausulas_adicionales}}",
  "activo": true
}
```

### 3.3. Modificación

```json
{
  "nombre": "Acuerdo de confidencialidad",
  "descripcion": "NDA aplicable a los proyectos.",
  "plantilla": "ACUERDO DE CONFIDENCIALIDAD\n\nProyecto: {{proyecto}}\nPartes:\n{{partes}}"
}
```

### 3.4. Cambio de estado

```json
{
  "activo": false
}
```

### 3.5. Validación de plantilla

```json
{
  "plantilla": "Contrato {{proyecto}} {{foo}} {{ partes }}"
}
```

```json
{
  "valido": false,
  "longitud": 38,
  "placeholdersDesconocidos": ["{{foo}}"],
  "placeholdersConFormatoInvalido": ["{{ partes }}"],
  "advertencias": ["La plantilla no incluye {{director}}."],
  "tieneClausulasAdicionales": false
}
```

---

## 4. Placeholders soportados

| Placeholder | Contenido |
|---|---|
| `{{proyecto}}` | Nombre y descripción del proyecto |
| `{{director}}` | Director que emite el contrato |
| `{{partes}}` | Destinatarios del contrato |
| `{{clausulas_adicionales}}` | Cláusulas adicionales ingresadas por el director |
| `{{tipo}}` | Nombre del tipo de contrato |

Los placeholders deben escribirse exactamente, sin espacios internos. `{{ proyecto }}` se considera formato inválido.

---

## 5. Errores

| HTTP | Código | Situación |
|---|---|---|
| `400` | `TIPO_CONTRATO_PLANTILLA_INVALIDA` | Plantilla vacía, excedida o con placeholders inválidos |
| `404` | `TIPO_CONTRATO_NO_ENCONTRADO` | Tipo inexistente |
| `409` | `TIPO_CONTRATO_DUPLICADO` | Nombre duplicado |
| `409` | `TIPO_CONTRATO_CON_CONTRATOS_VIVOS` | Intento de editar nombre/plantilla con contratos vivos |
| `409` | `TIPO_CONTRATO_CON_CONTRATOS_PENDIENTES` | Intento de desactivar con contratos pendientes |
| `409` | `TIPO_CONTRATO_EN_USO` | Intento de eliminar un tipo con contratos |
| `409` | `ULTIMO_TIPO_CONTRATO_ACTIVO` | Intento de desactivar/eliminar el último activo |

---

## 6. Frontend

- Servicio: `src/services/adminTiposContratoService.js`.
- Vista: `src/views/admin/GestionTiposContratoView.vue`.
- Ruta: `/admin/tipos-contrato` con `requierePermiso: "VER_TIPOS_CONTRATO"`.
- Menú: link “Tipos de contrato” en `AdminLayout.vue`.
- Permisos: agregados a `PERMISOS_ADMIN` en `src/services/authState.js`.

La vista usa los flags `puedeEditar`, `puedeDesactivar` y `puedeEliminar` devueltos por el backend para deshabilitar acciones y mostrar el motivo.

---

## 7. Migración

`docs/3_diseño/migraciones/2026-11-08_uc76_tipos_contrato_admin.sql`

Incluye:

- permisos globales y asignación al rol Administrador;
- índice único case-insensitive para `codigo`;
- eventos de auditoría;
- trigger `trg_auditar_tipo_acuerdo_contrato`.

La unicidad de nombre se valida en el servicio, no con un índice parcial, para
permitir reutilizar nombres en tipos inactivos sin romper datos históricos.

No modifica la estructura de `tipo_acuerdo_contrato`.
