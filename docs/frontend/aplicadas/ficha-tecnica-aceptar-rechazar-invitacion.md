# Ficha Técnica Frontend: Aceptar y Rechazar Invitación (UC-28)

Especificación de la API, flujo de pantalla y manejo de errores para que el
perfil destinatario acepte o rechace una invitación a proyecto.

No modifica fichas técnicas existentes.

---

## 1. Endpoints y autenticación

### Aceptar

```http
POST /invitaciones/general/{idInvitacion}/aceptar
POST /invitaciones/actividad/{idInvitacion}/aceptar
```

### Rechazar

```http
POST /invitaciones/general/{idInvitacion}/rechazar
POST /invitaciones/actividad/{idInvitacion}/rechazar
```

### Común

- **Método:** `POST`.
- **Body:** ninguno.
- **Autenticación:** JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`.
- **Perfil activo:** obligatorio en la sesión.
- **Destinatario:** el perfil activo debe ser el destinatario de la invitación.
- **Respuesta exitosa:** `200 OK` con `InvitacionGestionadaResponse`.

El frontend usa `tipo` e `idInvitacion` devueltos por UC-30 para elegir el
endpoint correcto.

---

## 2. Flujo de pantalla

### 2.1 Entrada

Desde UC-30, el usuario selecciona una invitación recibida en estado
`PENDIENTE`.

Mostrar:

- Proyecto.
- Tipo de invitación: general o por actividad.
- Remitente.
- Fecha de envío.
- Mensaje, si existe.
- Actividad y requerimiento, si corresponde.
- Estado del cupo, si está disponible.

### 2.2 Aceptar

1. El usuario selecciona **Aceptar invitación**.
2. Mostrar un modal de confirmación.
3. En invitaciones por actividad, informar que se verificará la disponibilidad.
4. El usuario confirma.
5. Enviar el `POST` correspondiente.
6. Con `200`, mostrar éxito y refrescar UC-30 y el proyecto.

### 2.3 Rechazar

1. El usuario selecciona **Rechazar invitación**.
2. Mostrar un modal de confirmación.
3. El usuario confirma.
4. Enviar el `POST` correspondiente.
5. Con `200`, mostrar que la invitación fue rechazada y refrescar UC-30.

No se elimina la invitación: queda en estado `RECHAZADA`.

### 2.4 Después de aceptar

- Si era general, el usuario queda incorporado al proyecto.
- Si era por actividad, además queda asignado a la actividad.
- La invitación ya no aparece como pendiente.
- Si el cupo del requerimiento se cubrió, otras invitaciones pendientes del
  mismo requerimiento pueden desaparecer del listado.

---

## 3. Response payload

```json
{
  "idInvitacion": 123,
  "tipo": "ACTIVIDAD",
  "estado": "ACEPTADA",
  "idProyecto": 5,
  "idPerfil": 10,
  "idMiembro": 88,
  "idAsignacionActividad": 99,
  "disponibilidad": {
    "disponible": true,
    "provisional": false,
    "inicioEvaluado": "2026-11-10T09:00:00",
    "finEvaluado": "2026-11-10T17:00:00"
  },
  "advertencias": []
}
```

Para rechazo:

```json
{
  "idInvitacion": 123,
  "tipo": "GENERAL",
  "estado": "RECHAZADA",
  "idProyecto": 5,
  "idPerfil": 10,
  "idMiembro": null,
  "idAsignacionActividad": null,
  "disponibilidad": null,
  "advertencias": []
}
```

---

## 4. Errores

| Caso | HTTP | Acción en frontend |
|---|---|---|
| Invitación inexistente | `404` | Cerrar el modal y refrescar UC-30. |
| La invitación no pertenece al perfil activo | `403` | Ocultar la acción y refrescar. |
| Invitación ya aceptada o rechazada | `409` | Refrescar el estado. |
| Proyecto cancelado o finalizado | `409` | Informar y refrescar. |
| Requerimiento satisfecho | `409` | Informar que el cupo ya fue cubierto. |
| Profesión incompatible | `409` | Informar y refrescar. |
| Sin disponibilidad | `409` | Informar el motivo y no reintentar automáticamente. |
| El perfil ya es miembro general | `409` | Informar y refrescar. |
| Actividad inválida | `409` | Refrescar la invitación. |

Los `409` de aceptación por actividad pueden incluir `codigoMotivo` de
disponibilidad.

---

## 5. Checklist de aceptación

- [ ] Se muestran los datos de la invitación antes de confirmar.
- [ ] Se distingue invitación general de invitación por actividad.
- [ ] Se pide confirmación explícita.
- [ ] Se usa el endpoint correcto según `tipo`.
- [ ] El botón se deshabilita durante la petición.
- [ ] Con `200` se refresca UC-30.
- [ ] La invitación aceptada deja de estar pendiente.
- [ ] La invitación rechazada queda en estado `RECHAZADA`.
- [ ] Se informa si no hay disponibilidad.
- [ ] Se informa si el cupo ya fue cubierto.
- [ ] No se reintenta automáticamente un `403` o `409`.
- [ ] No se elimina la invitación al rechazarla.
