# Ficha técnica — UC-55 Firmar contrato

**Módulo:** Contratos
**Actor:** Miembro activo del proyecto con firma pendiente (incluye al director emisor)
**Estado backend:** implementado y testeado con tests unitarios, MVC e integración con PostgreSQL (Testcontainers).

---

## 1. Resumen

Un miembro activo del proyecto firma el contrato desde su perfil activo. La firma queda registrada en `firma_contrato` y, cuando todas las partes activas firmaron, el contrato pasa automáticamente a `ACTIVO`.

La misma ficha cubre la extensión confirmada de **rechazo de contrato**: cualquier parte con firma `PENDIENTE` puede rechazar indicando un motivo obligatorio. El rechazo deja el contrato en `RECHAZADO_POR_PARTES`, cancela las firmas que seguían pendientes y notifica a las partes y al director del proyecto.

El backend:

1. valida el acceso y el estado del proyecto y del contrato;
2. identifica la membresía activa del perfil y su fila de firma;
3. registra la firma o el rechazo;
4. deja que los triggers de BD actualicen versión, fechas, estado y notificaciones;
5. devuelve el detalle actualizado del contrato.

La operación es atómica y bloquea el contrato con lock pesimista para serializar firmas concurrentes.

---

## 2. Flujo de negocio

### 2.1. Precondiciones

- Sesión con rol `USUARIO` y perfil activo seleccionado.
- Contrato existente en el proyecto indicado por la URL.
- Proyecto en estado `PUBLICADO` o `CONFIRMADO`.
- Contrato en estado `PENDIENTE_FIRMA`.
- Perfil activo con membresía `ACTIVO` en el proyecto.
- Fila de firma del miembro en estado `PENDIENTE`, `FIRMADO`, `RECHAZADO`, `CANCELADA` o `INVALIDADA` según corresponda.

### 2.2. Firmar

1. El usuario abre el detalle del contrato o el listado de contratos pendientes.
2. La UI muestra la acción de firmar solo si `puedeFirmar = true`.
3. El usuario confirma la firma.
4. El backend registra `estado_firma = FIRMADO` y `fecha_firma`.
5. Si todavía faltan firmas, el contrato sigue `PENDIENTE_FIRMA`.
6. Si ya firmaron todas las partes activas, el contrato pasa a `ACTIVO` y se notifica `CONTRATO_ACTIVO`.
7. El backend devuelve el detalle actualizado.

### 2.3. Rechazar

1. La UI muestra la acción de rechazar solo si `puedeRechazar = true`.
2. El usuario escribe un motivo (obligatorio, máximo 200 caracteres).
3. El backend registra `estado_firma = RECHAZADO`, `fecha_rechazo` y `motivo_rechazo`.
4. El trigger de estado pasa el contrato a `RECHAZADO_POR_PARTES`.
5. Las firmas que seguían `PENDIENTE` pasan a `CANCELADA`.
6. Se notifica `CONTRATO_RECHAZADO` a las partes y al director del proyecto.
7. El backend devuelve el detalle actualizado.

### 2.4. Postcondiciones

**Firma parcial**

- `firma_contrato.estado_firma = FIRMADO` para el perfil firmante.
- `firma_contrato.fecha_firma` y `id_contrato_version` actualizados por trigger.
- `contrato.estado = PENDIENTE_FIRMA`.
- Sin notificación `CONTRATO_ACTIVO`.

**Última firma**

- Todas las firmas activas en `FIRMADO`.
- `contrato.estado = ACTIVO`.
- Evento `ACTIVADO` registrado.
- Notificación `CONTRATO_ACTIVO` una sola vez por parte (el trigger deduplica por usuario, perfil, tipo y contrato).

**Rechazo**

- `firma_contrato.estado_firma = RECHAZADO` con `fecha_rechazo` y `motivo_rechazo`.
- Firmas pendientes restantes en `CANCELADA`.
- `contrato.estado = RECHAZADO_POR_PARTES`.
- Notificación `CONTRATO_RECHAZADO` a las partes y al director del proyecto (deduplicada).

---

## 3. Endpoints

### 3.1. Firmar contrato

```http
POST /proyectos/{idProyecto}/contratos/{idContrato}/firmar
```

- Sin body.
- Requiere sesión, perfil activo y membresía activa con firma `PENDIENTE`.
- Respuesta `200` con el detalle del contrato actualizado.

### 3.2. Rechazar contrato

```http
POST /proyectos/{idProyecto}/contratos/{idContrato}/rechazar
Content-Type: application/json
```

```json
{
  "motivo": "No estoy de acuerdo con la cláusula de exclusividad."
}
```

- `motivo` obligatorio, no vacío, máximo 200 caracteres.
- Respuesta `200` con el detalle del contrato actualizado.

### 3.3. Listar contratos pendientes de mi firma

```http
GET /contratos/buscar?miFirmaPendiente=true&page=0&size=20
```

El filtro `miFirmaPendiente` es opcional (`false` por defecto) y se puede combinar con los filtros existentes de UC-68 (`idProyecto`, `estado`, `nombre`, etc.). Devuelve solo los contratos donde la membresía activa del perfil activo tiene una firma `PENDIENTE`.

### 3.4. Respuesta común de detalle

```json
{
  "idContrato": 55,
  "idProyecto": 100,
  "nombreProyecto": "Desfile Primavera",
  "estadoProyecto": "PUBLICADO",
  "idTipoAcuerdo": 1,
  "codigoTipoAcuerdo": "CESION_DERECHOS_IMAGEN",
  "nombreTipoAcuerdo": "Cesión de derechos de imagen",
  "nombre": "Cesión - Desfile Primavera",
  "descripcion": "Contrato de cesión",
  "estado": "PENDIENTE_FIRMA",
  "versionActual": 1,
  "fechaCreacion": "2026-11-07T10:00:00",
  "fechaEmision": "2026-11-07T10:00:00",
  "fechaModificacion": null,
  "texto": "CONTRATO DE CESION...",
  "emisor": {
    "idMiembro": 11,
    "idPerfil": 3,
    "nombreArtistico": "Directora Uno",
    "nombreCompleto": "Ana Gómez",
    "dni": "30111222"
  },
  "partes": [
    {
      "idMiembro": 11,
      "idPerfil": 3,
      "nombre": "Directora Uno",
      "rolParte": "DIRECTOR",
      "estadoFirma": "FIRMADO",
      "fechaFirma": "2026-11-07T11:00:00"
    },
    {
      "idMiembro": 12,
      "idPerfil": 8,
      "nombre": "Miembro Uno",
      "rolParte": "DESTINATARIO",
      "estadoFirma": "PENDIENTE",
      "fechaFirma": null
    }
  ],
  "miEstadoFirma": "PENDIENTE",
  "miFechaFirma": null,
  "puedeModificarse": true,
  "motivoNoModificable": null,
  "puedeFirmar": true,
  "puedeRechazar": true,
  "firmados": 1,
  "totalPartes": 2
}
```

Los campos `puedeFirmar`, `puedeRechazar`, `firmados` y `totalPartes` también están disponibles en cada fila de `GET /contratos/buscar`.

---

## 4. Reglas de negocio

1. **Acceso.** El contrato debe existir y pertenecer al `idProyecto` de la URL; si no, `404`. El perfil debe pertenecer al usuario autenticado y estar activo.
2. **Proyecto apto.** Solo se firma o rechaza si el proyecto está `PUBLICADO` o `CONFIRMADO`; si no, `409 PROYECTO_NO_APTO_PARA_CONTRATO`.
3. **Contrato pendiente.** Solo se firma o rechaza un contrato `PENDIENTE_FIRMA`; si no, `409 CONTRATO_NO_PENDIENTE_FIRMA`.
4. **Membresía activa.** El perfil debe tener una membresía `ACTIVO` en el proyecto y una fila de firma en el contrato. Si no tiene membresía o no es parte, `403`.
5. **Firma pendiente.** El perfil solo firma si su firma está `PENDIENTE`. Si ya firmó, `409 CONTRATO_YA_FIRMADO`; si ya rechazó, `409 CONTRATO_YA_RECHAZADO`; si está `CANCELADA` o `INVALIDADA`, `409 FIRMA_NO_PENDIENTE`.
6. **Director emisor.** Puede firmar como cualquier otra parte si su firma está `PENDIENTE`.
7. **Activación.** El contrato pasa a `ACTIVO` cuando todas las partes con firma `PENDIENTE` o `FIRMADO` tienen `FIRMADO`. Las firmas `CANCELADA` no se cuentan y no bloquean la activación.
8. **Notificación de activación.** Solo se notifica `CONTRATO_ACTIVO` al activarse el contrato, no en cada firma individual. El trigger deduplica una notificación por parte.
9. **Rechazo.** El motivo es obligatorio y de máximo 200 caracteres. El rechazo de una parte pasa el contrato a `RECHAZADO_POR_PARTES` y cancela las firmas pendientes.
10. **Concurrencia.** La firma y el rechazo bloquean el contrato con `PESSIMISTIC_WRITE` para serializar operaciones simultáneas.
11. **Auditoría.** Los servicios de firma y rechazo están anotados con `@AuditarOperacion`, por lo que los triggers registran al usuario real como operador.

---

## 5. Modelo de datos

No se crean tablas nuevas. Se reutilizan:

- `contrato`: `estado`, `version_actual`, fechas y `id_miembro_emisor`.
- `firma_contrato`: PK compuesta `(id_miembro, id_contrato)`, `rol_parte`, `estado_firma`, `fecha_firma`, `fecha_rechazo`, `motivo_rechazo` e `id_contrato_version`.
- `contrato_evento`: eventos `CREADO`, `EMITIDO`, `MODIFICADO`, `ACTIVADO`, `CANCELADO`, etc.
- `notificacion`: tipos `CONTRATO_PENDIENTE_FIRMA`, `CONTRATO_ACTIVO`, `CONTRATO_RECHAZADO`, `CONTRATO_MODIFICADO`.

Estados de `firma_contrato`: `PENDIENTE`, `FIRMADO`, `RECHAZADO`, `CANCELADA`, `INVALIDADA`.

La corrección de los triggers `fn_validar_transicion_contrato` y `fn_actualizar_estado_contrato` está en `docs/3_diseño/ModaLinkBD.sql` y en la migración `docs/3_diseño/migraciones/2026-11-07_uc55_firmar_rechazar_contrato.sql`. La migración solo reemplaza ambas funciones y no toca datos existentes.

---

## 6. Errores

| HTTP | Código | Situación |
| ---- | ------ | --------- |
| 400 | Validación | Body de rechazo ausente, `motivo` vacío o mayor a 200 caracteres. |
| 403 | — | El perfil no es miembro activo del proyecto o no es parte del contrato. |
| 404 | — | Contrato inexistente, de otro proyecto, o perfil activo no encontrado. |
| 409 | `PROYECTO_NO_APTO_PARA_CONTRATO` | El proyecto no está `PUBLICADO`/`CONFIRMADO`. |
| 409 | `CONTRATO_NO_PENDIENTE_FIRMA` | El contrato no está `PENDIENTE_FIRMA`. |
| 409 | `CONTRATO_YA_FIRMADO` | El perfil ya firmó este contrato. |
| 409 | `CONTRATO_YA_RECHAZADO` | El perfil ya rechazó este contrato. |
| 409 | `FIRMA_NO_PENDIENTE` | La firma está `CANCELADA` o `INVALIDADA`. |
| 409 | `MOTIVO_RECHAZO_OBLIGATORIO` | El servicio recibió un motivo vacío (defensa adicional al `400` de validación). |
| 409 | — | Perfil en baja (`PerfilEnBajaException`). |

---

## 7. Recomendaciones para el frontend

- Usar `puedeFirmar` y `puedeRechazar` del detalle o del listado para mostrar u ocultar las acciones. No inferir solo por estado del contrato.
- Mostrar el progreso de firma con `firmados` / `totalPartes` y el estado de cada fila de `partes`.
- Para la bandeja "Pendientes de mi firma", llamar `GET /contratos/buscar?miFirmaPendiente=true` y combinar con paginación y filtros de UC-68.
- Enviar la firma con `POST` sin body; no adjuntar un DTO vacío.
- El modal de rechazo debe exigir un motivo no vacío y limitarlo a 200 caracteres en el cliente.
- Tras una firma o rechazo, refrescar el detalle con la respuesta `200` (ya viene actualizada) y volver a consultar el listado si está abierto.
- Mapear los códigos `409` a mensajes claros: ya firmado, contrato no pendiente, proyecto no apto, motivo obligatorio.
- No mostrar la acción de firma cuando `estado` sea `ACTIVO`, `RECHAZADO_POR_PARTES`, `CANCELADO` o `ELIMINADO`.
- Cuando el contrato pase a `ACTIVO`, reflejar el cambio y dejar de mostrar el bloque de firma.

---

## 8. Tests

- `ContratoControllerTest`: 14 tests MVC, incluidos firma, rechazo, validación de motivo y filtro `miFirmaPendiente`.
- `FirmarContratoIntegrationTest`: 7 tests de integración (firma parcial, activación, ya firmado, acceso denegado, proyecto no apto, firma `CANCELADA`, contrato ya activo).
- `RechazarContratoIntegrationTest`: 5 tests de integración (rechazo con motivo, motivo vacío, ya firmado, acceso denegado, contrato ya rechazado).
- `BuscarContratoIntegrationTest`: 11 tests, incluidos `miFirmaPendiente`, flags y conteos de firma.
- Regresión: `GenerarContratoIntegrationTest` (5), `ModificarContratoIntegrationTest` (11) y `ContratoModificacionTriggerIntegrationTest` (3).
- La migración se validó aplicándola sobre PostgreSQL 17 con el esquema canónico y verificando que ambas funciones quedan con el conteo corregido de firmas activas.

---

## 9. Referencias

- `docs/1_requisitos/casos_de_uso.md` — UC-55.
- `docs/3_diseño/ModaLinkBD.sql` — esquema canónico y triggers.
- `docs/3_diseño/migraciones/2026-11-07_uc55_firmar_rechazar_contrato.sql` — migración de funciones.
- `docs/5_estado_sistema/estado_uc.md` — seguimiento del avance.
- `ContratoController`, `FirmarContratoService`, `RechazarContratoService`, `ContratoFirmaContextoService`, `ContratoMapper`, `ContratoSpecifications`.
