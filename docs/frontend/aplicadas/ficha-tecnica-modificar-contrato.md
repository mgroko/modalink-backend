# Ficha técnica — UC-53 Modificar contrato

**Módulo:** Contratos  
**Actor:** Director de proyecto  
**Estado backend:** implementado y testeado con tests unitarios, MVC e integración con PostgreSQL (Testcontainers).

---

## 1. Resumen

El director activo modifica un contrato en estado `PENDIENTE_FIRMA` de un proyecto `PUBLICADO` o `CONFIRMADO`. Puede cambiar:

- tipo de acuerdo;
- miembros destinatarios;
- nombre y descripción;
- texto final editado y/o cláusulas adicionales.

El backend reconcilia las firmas, actualiza el contrato y deja que los triggers registren la nueva versión, el evento `MODIFICADO` y las notificaciones `CONTRATO_MODIFICADO` a las partes actuales.

---

## 2. Precondiciones y reglas

- Usuario autenticado con perfil activo.
- Actor director activo del proyecto.
- Contrato `PENDIENTE_FIRMA`.
- Proyecto `PUBLICADO` o `CONFIRMADO`.
- Ninguna firma en `FIRMADO`, `RECHAZADO` o `INVALIDADA`.
- Tipo de acuerdo activo.
- Al menos un destinatario activo del proyecto.
- Ningún destinatario con otro contrato `ACTIVO` del mismo tipo en el proyecto.
- Texto no vacío y dentro del máximo permitido.

Las firmas `CANCELADA` no bloquean: se resetean si el miembro sigue seleccionado o se eliminan si se lo remueve.

---

## 3. Endpoints

### 3.1. Detalle para edición

```text
GET /proyectos/{idProyecto}/contratos/{idContrato}
```

Devuelve tipo, destinatarios actuales, texto, versión, partes, firmas y `puedeModificarse`.

### 3.2. Previsualización

```text
POST /proyectos/{idProyecto}/contratos/{idContrato}/previsualizar
```

Genera el borrador con el nuevo tipo y las nuevas partes sin persistir. Devuelve `texto`, `puedeModificarse` y `conflictos[]`.

Request:

```json
{
  "idTipoAcuerdo": 1,
  "idsMiembro": [8, 11],
  "texto": null,
  "clausulasAdicionales": [
    "La parte cedente conserva el derecho de aprobar usos adicionales."
  ],
  "nombre": null,
  "descripcion": null
}
```

### 3.3. Modificación

```text
PUT /proyectos/{idProyecto}/contratos/{idContrato}
```

Mismo request que la previsualización. Respuesta `200` con el detalle actualizado.

Reglas de texto:

- Si `texto` viene no vacío, se usa como texto final.
- Si `texto` es `null`, se regenera desde la plantilla del tipo y las cláusulas adicionales.
- Si `nombre` no viene y cambió el tipo, se regenera como `"{tipo} - {proyecto}"`.
- Si `nombre` no viene y el tipo no cambió, se conserva.
- `descripcion` se conserva si no viene.

---

## 4. Flujo transaccional

1. Validar perfil, proyecto y director.
2. Cargar el contrato con lock pesimista.
3. Validar pertenencia al proyecto, estado y firmas registradas.
4. Validar tipo y destinatarios.
5. Detectar duplicados activos.
6. Generar o validar el texto final.
7. Reconciliar firmas:
   - director: `DIRECTOR` o `DIRECTOR_DESTINATARIO`;
   - destinatarios: `PENDIENTE`/`DESTINATARIO`;
   - nuevos: insertar;
   - removidos: eliminar solo si están `PENDIENTE`/`CANCELADA`;
   - `CANCELADA` seleccionada: resetear.
8. Actualizar tipo, nombre, descripción, texto y `fecha_modificacion`.
9. Flush + clear.
10. Devolver el detalle actualizado.

El orden es importante: primero se reconcilian las firmas y después se actualiza el contrato, para que el trigger de notificación vea las partes nuevas.

---

## 5. Cambios de base de datos

- `fn_validar_contrato_editable`:
  - actualiza `fecha_modificacion` ante cualquier cambio;
  - bloquea `FIRMADO`/`RECHAZADO`/`INVALIDADA`;
  - ya no registra `MODIFICADO`.
- `fn_post_modificacion_contrato` + `trg_contrato_modificacion`:
  - registra `MODIFICADO` con la versión actual;
  - notifica `CONTRATO_MODIFICADO` a las partes actuales.
- `fn_notificar_partes_contrato_modificado`:
  - no deduplica entre modificaciones;
  - guarda `id_proyecto`.
- La migración está en:

```text
docs/3_diseño/migraciones/2026-11-07_uc53_uc68_buscar_modificar_contrato.sql
```

---

## 6. Errores

| HTTP | Código | Situación |
|---|---|---|
| `400` | `TIPO_ACUERDO_INVALIDO` | Tipo inexistente o inactivo |
| `409` | `CONTRATO_TEXTO_INVALIDO` | Texto vacío o excedido |
| `403` | `ACCESO_DENEGADO_PROYECTO` | No es director activo |
| `404` | `CONTRATO_NO_ENCONTRADO` | Contrato inexistente o de otro proyecto |
| `409` | `CONTRATO_NO_PENDIENTE_FIRMA` | Estado distinto de `PENDIENTE_FIRMA` |
| `409` | `PROYECTO_NO_APTO_PARA_CONTRATO` | Proyecto no `PUBLICADO`/`CONFIRMADO` |
| `409` | `CONTRATO_CON_FIRMAS_REGISTRADAS` | Alguna firma ya registrada |
| `409` | `DESTINATARIO_INVALIDO` | Miembro inactivo o ajeno |
| `409` | `CONTRATO_DUPLICADO_ACTIVO` | Destinatario con contrato ACTIVO del mismo tipo |
| `409` | `CONTRATO_SIN_DESTINATARIOS` | No se seleccionó ninguno |

---

## 7. Pruebas

- `ContratoModificacionTriggerIntegrationTest`: texto, versión, evento, notificación sin deduplicar, nombre sin texto y bloqueo por firma.
- `ModificarContratoIntegrationTest`: texto final, solo partes, director seleccionado/deseleccionado, preview sin persistencia, firma registrada, duplicado activo y no director.
- `ContratoControllerTest`: detalle, previsualización y `PUT`.
- Regresión de UC-39, triggers de contrato y notificaciones.

---

## 8. Pendiente

- UC-54 Eliminar contrato.
- UC-55 Firmar contrato.

La base, el modelo y los triggers ya quedaron preparados para esos casos.
