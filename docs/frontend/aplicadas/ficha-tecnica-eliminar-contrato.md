# Ficha técnica — UC-54 Eliminar contrato

**Módulo:** Contratos
**Actor:** Director activo del proyecto
**Estado backend:** implementado y testeado con tests unitarios, MVC e integración con PostgreSQL (Testcontainers).

---

## 1. Resumen

El director del proyecto elimina lógicamente un contrato. No se borra la fila: el contrato pasa a `ELIMINADO` y conserva su historial, versiones, firmas y eventos.

La baja se permite desde cualquier estado del contrato, siempre que el proyecto no esté `FINALIZADO` ni `CANCELADO`. Es una operación equivalente a una cancelación administrativa: deja de mostrarse en la búsqueda por defecto y se notifica a las partes.

El backend:

1. valida perfil activo, pertenencia y rol Director;
2. bloquea el contrato para evitar carreras con firma/modificación;
3. valida el estado del proyecto;
4. cambia el contrato a `ELIMINADO`;
5. deja que los triggers registren `fecha_baja`, cancelen firmas pendientes, creen el evento `ELIMINADO` y notifiquen.

---

## 2. Flujo de negocio

### 2.1. Precondiciones

- Sesión con rol `USUARIO` y perfil activo seleccionado.
- Contrato existente en el proyecto indicado por la URL.
- Proyecto en cualquier estado distinto de `FINALIZADO` y `CANCELADO`.
- Perfil con membresía `ACTIVO` y rol Director en el proyecto.

### 2.2. Pasos

1. El director busca contratos del proyecto desde UC-68.
2. Selecciona un contrato y confirma la eliminación.
3. El backend valida y cambia el contrato a `ELIMINADO`.
4. Los triggers completan el historial y las notificaciones.
5. El backend responde `204 No Content`.

### 2.3. Postcondiciones

- `contrato.estado = ELIMINADO`.
- `contrato.fecha_baja` completada por trigger.
- Firmas `PENDIENTE` pasan a `CANCELADA`.
- Las firmas que ya estaban `FIRMADO` se conservan como historial.
- Evento `ELIMINADO` registrado.
- Notificación `CONTRATO_ELIMINADO` a las partes, excepto cuando el contrato estaba en `BORRADOR`.
- El contrato sigue visible en la búsqueda por defecto; el cliente puede excluirlo enviando explícitamente la lista de estados sin `ELIMINADO`.
- Si el contrato estaba `CANCELADO`, sus campos de cancelación se limpian, pero el motivo original permanece en `contrato_evento`.

---

## 3. Endpoint

```http
DELETE /proyectos/{idProyecto}/contratos/{idContrato}
```

- Sin body.
- Requiere sesión, perfil activo y Director activo del proyecto.
- Respuesta `204 No Content`.

La operación es idempotente: si el contrato ya está `ELIMINADO`, responde `204` sin duplicar notificaciones ni eventos.

---

## 4. Reglas de negocio

1. **Proyecto apto.** No se elimina si el proyecto está `FINALIZADO` o `CANCELADO`; en ese caso `409 PROYECTO_NO_APTO_PARA_ELIMINACION`.
2. **Estado del contrato.** No se valida el estado del contrato: se permite desde `BORRADOR`, `PENDIENTE_FIRMA`, `ACTIVO`, `RECHAZADO_POR_PARTES` o `CANCELADO`.
3. **Permiso.** Solo un Director activo del proyecto puede eliminar. Cualquier otro miembro recibe `403`.
4. **Pertenencia.** El contrato debe pertenecer al proyecto de la URL; si no, `404`.
5. **Baja lógica.** La fila nunca se borra físicamente; el trigger `fn_impedir_borrado_contrato` lo impide.
6. **Firmas.** Las firmas pendientes quedan `CANCELADA`; las firmadas se conservan.
7. **Notificaciones.** `CONTRATO_ELIMINADO` se envía a las partes activas del contrato. Un contrato en `BORRADOR` no genera notificación.
8. **Idempotencia.** Repetir la llamada sobre un contrato ya eliminado devuelve `204`.
9. **Concurrencia.** El contrato se carga con lock pesimista, por lo que firma, modificación y eliminación quedan serializadas.

---

## 5. Flags para el frontend

`GET /contratos/buscar` y `GET /proyectos/{idProyecto}/contratos/{idContrato}` agregan:

- `puedeEliminar`: `true` si el perfil es Director activo, el proyecto no está `FINALIZADO`/`CANCELADO` y el contrato no está ya `ELIMINADO`.
- `motivoNoEliminable` (solo detalle): `SIN_PERMISO`, `PROYECTO_NO_APTO_PARA_ELIMINACION` o `CONTRATO_YA_ELIMINADO`.

La UI debe mostrar la acción de eliminar solo cuando `puedeEliminar = true`.

---

## 6. Errores

| HTTP | Código | Situación |
| --- | --- | --- |
| 204 | — | Baja registrada o contrato ya eliminado. |
| 403 | — | El perfil no es Director activo del proyecto. |
| 404 | — | Contrato inexistente, de otro proyecto, o perfil activo no encontrado. |
| 409 | `PROYECTO_NO_APTO_PARA_ELIMINACION` | Proyecto `FINALIZADO` o `CANCELADO`. |

---

## 7. Recomendaciones para el frontend

- Mostrar la acción solo con `puedeEliminar = true`; si está deshabilitada, usar `motivoNoEliminable`.
- Pedir confirmación explícita antes de llamar al endpoint.
- Tras `204`, quitar el contrato del listado y redirigir o refrescar la búsqueda.
- No asumir que “eliminar” implica borrar físicamente: es una baja lógica.
- Los contratos eliminados se incluyen por defecto en la búsqueda y se distinguen con el badge `Eliminado` y la fecha de baja.
- En la pestaña de contratos del proyecto se aplican los estados por defecto de UC-68, que incluyen `ELIMINADO`; el chip permite ocultarlos.

---

## 8. Tests

- `ContratoControllerTest`: `DELETE` devuelve `204` y mapea `409` con código.
- `EliminarContratoIntegrationTest`: 8 casos sobre contrato pendiente, activo, borrador, doble baja, proyecto finalizado, miembro no director, contrato de otro proyecto y flags del detalle.
- `BuscarContratoIntegrationTest`: `puedeEliminar`, `motivoNoEliminable` y exclusión por defecto de eliminados.
- Regresión de UC-39, UC-53, UC-55 y UC-68.
- Migración `2026-11-08_uc54_eliminar_contrato.sql` validada en PostgreSQL 17.

---

## 9. Referencias

- `docs/1_requisitos/casos_de_uso.md` — UC-54.
- `docs/3_diseño/ModaLinkBD.sql` — triggers y estados del contrato.
- `docs/3_diseño/migraciones/2026-11-08_uc54_eliminar_contrato.sql` — migración de `fn_validar_transicion_contrato`.
- `docs/5_estado_sistema/estado_uc.md` — seguimiento del avance.
- `ContratoController`, `EliminarContratoService`, `ContratoRepository`, `ContratoMapper`, `ContratoSpecifications`.
