# Ficha técnica frontend: modificar proyecto confirmado

- **Fecha:** 2026-11-03
- **Estado:** Contrato nuevo para implementar en el frontend
- **Alcance:** `PUT /proyectos/{idProyecto}` en proyectos `CONFIRMADO`, con foco en requerimientos generales
- **Tipo:** Ficha complementaria. **No reemplaza** `docs/frontend/aplicadas/ficha-tecnica-modificar-proyecto.md`; actualiza los puntos que entran en conflicto.

> Esta ficha existe porque las fichas dentro de `docs/frontend/aplicadas/` ya fueron implementadas en el frontend. Los cambios de abajo todavía no están reflejados allí y requieren una adaptación nueva.

---

## 1. Resumen de cambios

| Cambio | Impacto en frontend |
|---|---|
| En `CONFIRMADO` se pueden agregar, editar y eliminar requerimientos generales no protegidos | Ya no hay que ocultar toda la sección de requerimientos en confirmados |
| La protección es por requerimiento, no por proyecto | La UI debe deshabilitar solo los requerimientos protegidos |
| `requerimientosGral[].protegido` es nuevo | Usarlo para bloquear edición/eliminación sin esperar un `409` |
| Un requerimiento protegido omitido se conserva | Omitirlo ya no lo elimina |
| Un requerimiento enviado sin cambios no lanza `409` | Se puede guardar el formulario completo sin tocar requerimientos |
| Un requerimiento no protegido omitido se elimina | Si se quiere conservar, debe viajar en el payload |
| Los requerimientos existentes se actualizan en el lugar | El `idRequerimientoGral` se mantiene estable |
| El error `409` incluye código e ID | Permite resaltar el requerimiento conflictivo |

---

## 2. Endpoint y contexto

```http
PUT /proyectos/{idProyecto}
Content-Type: application/json
```

- Requiere JWT en cookie HTTP-only `jwt` y header `X-XSRF-TOKEN`.
- Requiere perfil activo (`idPerfilActivo`).
- Requiere membresía activa y permiso `MODIFICAR_PROYECTO`.
- Respuesta exitosa: `200 OK` con el `ProyectoResponse` completo.
- El nombre del proyecto sigue siendo inmutable.

### Estados permitidos

| Estado | ¿Se puede modificar? |
|---|---|
| `Borrador` | Sí |
| `Publicado` | Sí |
| `Confirmado` | Sí |
| `Finalizado` | No → `409` |
| `Cancelado` | No → `409` |

---

## 3. Campos editables en `CONFIRMADO`

| Campo | ¿Editable? | Observaciones |
|---|---|---|
| `nombre` | No | Inmutable; no viaja en el request |
| `descripcion` | Sí | Obligatoria, máx. 200 |
| `privacidad` | Sí | Obligatoria |
| `aceptaPostulacionGral` | Sí | Si se omite o es `null`, no cambia |
| `fechaEntregaEstipulada` | Sí | Dispara reprogramación y notificación si cambia |
| `ubicacion` | Sí, pero obligatoria | En `CONFIRMADO` no se puede quitar |
| `objetivos` | Sí | Si se envía, reemplaza todos |
| `moodboard.descripcion` | Sí | Si no existía moodboard, se crea |
| `requerimientosGral` | Sí, con protección | Ver sección 4 |

---

## 4. Regla de protección de requerimientos generales

Un requerimiento está **protegido** y no se puede modificar ni eliminar cuando:

1. El cupo está completo (`satisfecho = true`).
2. Tiene cualquier miembro vinculado, **activo o histórico** (baja voluntaria o eliminado).
3. Tiene una postulación o invitación `ACEPTADA`.
4. En un proyecto `CONFIRMADO`, tiene una postulación o invitación `PENDIENTE`.

Las postulaciones e invitaciones `RECHAZADA` **no** bloquean.

### Matriz por estado del proyecto

| Situación del requerimiento | `BORRADOR` / `PUBLICADO` | `CONFIRMADO` |
|---|---|---|
| Sin vínculos | Editable / eliminable | Editable / eliminable |
| Solo `RECHAZADA` | Editable / eliminable | Editable / eliminable |
| `PENDIENTE` | Editable / eliminable | **Inmutable** |
| `ACEPTADA` | **Inmutable** | **Inmutable** |
| Cupo completo | **Inmutable** | **Inmutable** |
| Cualquier miembro vinculado | **Inmutable** | **Inmutable** |

> Consecuencia de negocio: un requerimiento con cupo parcial (`1/2`) también queda inmutable si ya tiene un miembro. No se puede aumentar la cantidad desde este endpoint una vez que hay alguien vinculado.

---

## 5. Campo `protegido`

Cada elemento de `requerimientosGral[]` incluye:

```json
{
  "idRequerimientoGral": 28,
  "cantidad": 2,
  "descripcion": "Modelos para la prueba",
  "idProfesion": 2,
  "nombreProfesion": "Modelo",
  "caracteristicas": [],
  "habilidades": [],
  "satisfecho": false,
  "cubiertos": 1,
  "faltantes": 1,
  "puedePostularse": true,
  "protegido": true
}
```

- `protegido === true`: deshabilitar edición y eliminación.
- `protegido === false`: permitir edición y eliminación.
- El campo está presente en `GET /proyectos/{idProyecto}`, en la respuesta del `PUT` y en la publicación.
- En requerimientos recién creados puede venir `false` hasta que se refresque el proyecto.

---

## 6. Semántica del payload de requerimientos

`requerimientosGral` sigue siendo un reemplazo del conjunto, pero ahora con protección por requerimiento.

| Payload | Comportamiento |
|---|---|
| `requerimientosGral: null` o ausente | No se toca ningún requerimiento |
| `requerimientosGral: []` | Se eliminan los no protegidos; los protegidos se conservan |
| Requerimiento existente enviado sin cambios | No se valida protección y no se reescribe |
| Requerimiento existente no protegido enviado con cambios | Se actualiza en el lugar, conservando su ID |
| Requerimiento existente protegido enviado con cambios | `409` |
| Requerimiento existente no protegido omitido | Se elimina y se limpian sus postulaciones/invitaciones no aceptadas |
| Requerimiento existente protegido omitido | Se conserva |
| Requerimiento nuevo (`idRequerimientoGral: null`) | Se crea |

### Recomendación para el frontend

Para evitar `409` innecesarios:

1. **Omitir del payload los requerimientos con `protegido === true`.**
2. Enviar completos todos los requerimientos no protegidos que se quieran conservar.
3. No enviar un requerimiento protegido “parcialmente”: si se envía, debe ser idéntico al del `GET`.

### Ejemplo de actualización en confirmado

```json
{
  "descripcion": "Colección actualizada",
  "privacidad": "PUBLICO",
  "fechaEntregaEstipulada": "2026-12-20",
  "ubicacion": {
    "localidadId": "06049010000",
    "provinciaId": "06"
  },
  "objetivos": [
    { "nombre": "Nuevo objetivo", "descripcion": "Detalle" }
  ],
  "requerimientosGral": [
    {
      "idRequerimientoGral": 11,
      "cantidad": 3,
      "idProfesion": 3,
      "descripcion": "Estilista actualizado",
      "caracteristicas": [],
      "habilidades": []
    },
    {
      "cantidad": 1,
      "idProfesion": 2,
      "descripcion": "Modelo nuevo",
      "caracteristicas": [],
      "habilidades": []
    }
  ],
  "moodboard": {
    "descripcion": "Paleta actualizada"
  }
}
```

El requerimiento `10`, si existe y no está protegido, se elimina porque no viaja. Si está protegido, se conserva.

---

## 7. Error `409` de requerimiento protegido

```json
{
  "message": "No se puede modificar ni eliminar el requerimiento 28 porque tiene cupo cubierto, miembros vinculados o postulaciones/invitaciones pendientes o aceptadas.",
  "codigo": "REQUERIMIENTO_CON_VINCULOS",
  "idRequerimientoGral": 28,
  "httpStatus": 409,
  "timestamp": 1760000000000
}
```

UI recomendada:

- leer `codigo`;
- resaltar el requerimiento `idRequerimientoGral`;
- mostrar que tiene cupo o postulaciones activas;
- ofrecer “quitar el cambio” o “gestionar postulaciones”;
- no reintentar automáticamente.

### Otros errores relevantes

| HTTP | Situación |
|---|---|
| `400` | Payload inválido, descripción vacía, cantidad ≤ 0, característica inválida |
| `400` | Ubicación quitada en `Publicado`/`Confirmado` |
| `400` | Fecha de entrega anterior a hoy |
| `400` | Información básica incompleta |
| `403` | Sin permiso `MODIFICAR_PROYECTO` |
| `404` | Proyecto, perfil o requerimiento inexistente |
| `409` | Proyecto `Finalizado` o `Cancelado` |
| `409` | Requerimiento general protegido |
| `409` | Perfil en baja |

---

## 8. UX sugerida para `CONFIRMADO`

- Mantener habilitados:
  - descripción;
  - fecha de entrega;
  - ubicación;
  - objetivos;
  - moodboard;
  - requerimientos no protegidos.
- Para cada requerimiento:
  - `protegido === true`: solo lectura, sin botón de eliminar;
  - `protegido === false`: editar y eliminar habilitados.
- Permitir “Agregar requerimiento”.
- No bloquear todo el formulario por un requerimiento protegido.
- Mostrar badge “Protegido” o “Con personal vinculado”.
- Si se cambia la fecha de entrega, avisar que se reprogramará el cronograma.
- Después de guardar, usar la respuesta del `PUT` para actualizar el estado local; no hace falta un `GET` adicional.

---

## 9. Checklist de implementación frontend

- [ ] Leer `requerimientosGral[].protegido` en el detalle del proyecto.
- [ ] Deshabilitar edición y eliminación de requerimientos protegidos.
- [ ] Omitir los requerimientos protegidos del payload del `PUT`.
- [ ] Enviar completos los requerimientos no protegidos que deban conservarse.
- [ ] Permitir agregar requerimientos en `CONFIRMADO`.
- [ ] No asumir que omitir un requerimiento siempre lo elimina.
- [ ] No asumir que los requerimientos se recrean: el ID ahora se conserva.
- [ ] Manejar `409 REQUERIMIENTO_CON_VINCULOS` mostrando el `idRequerimientoGral`.
- [ ] Habilitar descripción, fecha, objetivos y moodboard en `CONFIRMADO`.
- [ ] Mantener la ubicación obligatoria en `CONFIRMADO`.
- [ ] Refrescar el cronograma/Gantt si cambia la fecha de entrega.
- [ ] Mostrar un mensaje claro cuando un requerimiento tenga cupo parcial con miembro vinculado.

---

## 10. Relación con la ficha aplicada existente

Esta ficha actualiza estos puntos de `docs/frontend/aplicadas/ficha-tecnica-modificar-proyecto.md`:

- la regla “los requerimientos no satisfechos se eliminan y recrean”;
- la regla “no se pueden enviar requerimientos satisfechos”;
- la semántica de `requerimientosGral` en `CONFIRMADO`;
- la ausencia de `protegido`;
- el tratamiento del `409`.

Mientras el frontend no incorpore estos cambios, debe seguir usando la ficha aplicada existente.
