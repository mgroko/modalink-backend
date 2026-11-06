# Ficha técnica — Desasignar miembro de una actividad (extensión UC-70)

**Proyecto:** ModaLink
**Backend:** Spring Boot
**Frontend:** Vue 3 + Vuestic
**Fecha:** 2026-11-02
**Acceso:** director activo del proyecto con permiso `ASIGNAR_ACTIVIDAD`.

---

## 1. Objetivo

Permitir que el director quite físicamente a un miembro activo de una actividad,
sin eliminar su membresía del proyecto y sin exigir disponibilidad ni fecha de
entrega.

La operación no reprograma el cronograma. Si la asignación cubría un
requerimiento, el cupo se libera automáticamente porque el cupo se deriva de las
asignaciones activas.

---

## 2. Endpoint

```http
DELETE /proyectos/{idProyecto}/actividades/{idActividad}/asignaciones/{idAsignacionActividad}
```

- No lleva body ni motivo.
- `idAsignacionActividad` es el identificador que devuelve `responsables[]` en
  `GET /proyectos/{idProyecto}/actividades`.
- No requiere `fechaEntrega`.

### Respuesta `200 OK`

```json
{
  "idAsignacionActividad": 1200,
  "idProyecto": 100,
  "idActividad": 77,
  "idMiembro": 900,
  "idPerfil": 50,
  "idUsuario": 2,
  "idRequerimientoActividad": 123,
  "requerimientoSatisfecho": false,
  "cupo": {
    "cantidad": 1,
    "cubiertos": 0,
    "faltantes": 1,
    "satisfecho": false
  },
  "advertencias": [
    "El requerimiento quedó con 1 vacante(s)."
  ]
}
```

Si la asignación no estaba vinculada a un requerimiento:

```json
{
  "idAsignacionActividad": 1201,
  "idProyecto": 100,
  "idActividad": 77,
  "idMiembro": 901,
  "idPerfil": 51,
  "idUsuario": 3,
  "idRequerimientoActividad": null,
  "requerimientoSatisfecho": false,
  "cupo": null,
  "advertencias": []
}
```

### Campos

| Campo | Interpretación |
|---|---|
| `idRequerimientoActividad` | Requerimiento que cubría la asignación; `null` si no estaba vinculada. |
| `requerimientoSatisfecho` | Estado del requerimiento después de borrar la asignación. |
| `cupo` | Cupo recalculado; `null` si no había requerimiento. |
| `advertencias` | Avisos no bloqueantes. Puede incluir requerimiento insatisfecho o proyecto confirmado. |

---

## 3. Reglas de negocio

1. Sólo director activo con `ASIGNAR_ACTIVIDAD`.
2. Proyecto en `BORRADOR`, `PUBLICADO` o `CONFIRMADO`.
3. Actividad en `PENDIENTE` o `EN_CURSO`.
4. No se exige fecha de entrega.
5. No se verifica disponibilidad.
6. La asignación debe pertenecer a la actividad y proyecto del path.
7. El miembro debe seguir `ACTIVO` en el proyecto; si no, la asignación es
   histórica y no se puede desasignar por este flujo.
8. El borrado es **físico**.
9. No se borra la membresía del proyecto.
10. Si había requerimiento, se recalcula el cupo.
11. Las postulaciones rechazadas previamente **no se reabren automáticamente**.
12. En proyecto `CONFIRMADO` se permite la operación con advertencia; el
    cronograma no se reprograma.
13. Se notifica al miembro desasignado con el tipo `ACTIVIDAD_DESASIGNADA`.

---

## 4. Errores

| Código | Caso |
|---|---|
| `401` | Sin sesión |
| `403` | No es director activo o no tiene `ASIGNAR_ACTIVIDAD` |
| `404` | Proyecto, actividad o asignación inexistente |
| `404` | La asignación no pertenece a la actividad/proyecto |
| `409` | Proyecto `FINALIZADO` o `CANCELADO` |
| `409` | Actividad `FINALIZADA` o `CANCELADA` |
| `409` | El miembro ya no está activo en el proyecto |

---

## 5. Flujo de frontend

1. En la columna **Responsables** de la tabla de actividades, cada chip activo
   muestra un botón `person_remove`.
2. El botón sólo se muestra si el usuario es director con
   `ASIGNAR_ACTIVIDAD`, el proyecto/actividad lo permiten y la asignación está
   activa.
3. Al hacer click se abre `ModalDesasignarActividad`.
4. El modal muestra actividad, miembro, duración, estado y advertencias.
5. Al confirmar, se ejecuta el `DELETE`.
6. Si responde `200`:
   - cerrar el modal;
   - mostrar toast;
   - refrescar planificación y disponibilidad.
7. Si responde `404` o `409`, cerrar el modal, avisar y refrescar.

### Texto sugerido del modal

> “Se quitará a **Luna** de la actividad **Montaje**. Si cubría un
> requerimiento, el cupo volverá a quedar disponible. Las postulaciones
> rechazadas previamente no se reabren automáticamente.”

Si el proyecto está confirmado, agregar:

> “El proyecto está confirmado. La desasignación no reprograma el cronograma ni
> modifica los horarios persistidos.”

---

## 6. Notificación

Tipo nuevo:

```text
ACTIVIDAD_DESASIGNADA
```

- Categoría: `ACTIVIDAD`.
- Prioridad: `MEDIA`.
- Destinatario: usuario/perfil del miembro desasignado.
- Entidad: `ACTIVIDAD`.
- `entidadId`: `idActividad`.
- `idProyecto`: proyecto de la actividad.

La notificación se crea desde `DesasignarActividadService`, no desde un trigger,
para no notificar cuando se elimina una actividad completa y se borran todas sus
asignaciones.

---

## 7. Reglas que el frontend no debe duplicar

- No calcular cupos.
- No reabrir postulaciones.
- No inferir si el requerimiento queda satisfecho: usar `cupo` y
  `requerimientoSatisfecho`.
- No tratar las `advertencias` como errores.
- No exigir fecha de entrega para desasignar.
- No borrar la membresía del proyecto desde este flujo.
- No usar un borrado local: siempre llamar al endpoint.
