# Ficha Técnica Frontend: Invitar a Varios Perfiles (UC-31)

Especificación para que el Director seleccione varios perfiles y envíe la misma
invitación a todos en una sola operación.

No modifica fichas técnicas existentes.

---

## 1. Endpoints y autenticación

### Invitación general múltiple

- **Método:** `POST`
- **URL:** `/proyectos/{idProyecto}/invitaciones/general/batch`
- **Content-Type:** `application/json`
- **Respuesta:** `200 OK` con `InvitacionBatchResponse`.

### Invitación por actividad múltiple

- **Método:** `POST`
- **URL:** `/proyectos/{idProyecto}/invitaciones/actividad/batch`
- **Content-Type:** `application/json`
- **Respuesta:** `200 OK` con `InvitacionBatchResponse`.

### Común

- **Autenticación:** JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`.
- **Perfil activo:** obligatorio en la sesión.
- **Permiso:** `INVITAR_MIEMBRO`.
- **Rol:** Director activo del proyecto.
- **Proyecto:** `Publicado` o `Confirmado`.
- **Actividad:** `Pendiente` o `En curso`.

---

## 2. Request payload

### Invitación general

```json
{
  "idsPerfilDestinatario": [10, 11, 12],
  "mensaje": "Te invito a participar del proyecto."
}
```

### Invitación por actividad

```json
{
  "idRequerimientoActividad": 30,
  "idsPerfilDestinatario": [10, 11, 12],
  "mensaje": "Te invito a cubrir este requerimiento."
}
```

Reglas:

- La lista debe tener al menos un perfil.
- Los IDs repetidos se deduplican en el backend.
- El mensaje es opcional y tiene máximo 200 caracteres.
- La misma invitación y el mismo mensaje se aplican a todos los perfiles.
- Cada perfil genera una invitación independiente.

---

## 3. Response payload

```json
{
  "creadas": [
    {
      "idInvitacion": 123,
      "tipo": "GENERAL",
      "estado": "PENDIENTE",
      "fechaEnvio": "2026-11-05T10:00:00",
      "idProyecto": 5,
      "idRequerimientoGral": null,
      "idRequerimientoActividad": null,
      "idPerfilRemitente": 11,
      "idPerfilDestinatario": 10,
      "mensaje": "Te invito a participar del proyecto."
    }
  ],
  "errores": [
    {
      "idPerfilDestinatario": 12,
      "mensaje": "El perfil ya tiene una invitación activa para este proyecto.",
      "codigo": "INVITACION_DUPLICADA"
    }
  ]
}
```

### Interpretación

- `creadas` contiene las invitaciones que se registraron correctamente.
- `errores` contiene los perfiles que no se pudieron invitar.
- Puede haber:
  - Todas creadas y `errores` vacío.
  - Algunas creadas y algunos errores.
  - Ninguna creada y todos los perfiles en `errores`.
- Cada invitación creada es independiente para UC-30 y UC-32.

---

## 4. Flujo de pantalla

### 4.1 Selección de perfiles

1. El Director entra a la pantalla de invitación.
2. Busca perfiles con `GET /perfiles/buscar`.
3. Puede seleccionar varios perfiles.
4. Se muestra un contador de perfiles seleccionados.
5. Se permite quitar perfiles de la selección antes de confirmar.

### 4.2 Confirmación

Antes de enviar, mostrar:

- Proyecto.
- Tipo de invitación.
- Cantidad de perfiles seleccionados.
- Lista resumida de perfiles.
- Mensaje, si se ingresó.

El botón de envío se deshabilita mientras la petición está en curso.

### 4.3 Resultado

Con `200`:

- Mostrar cuántas invitaciones se crearon y cuántas fallaron.
- Si `errores` está vacío, mostrar éxito total.
- Si hay errores, mostrar:
  - Perfiles creados.
  - Perfiles con error y el mensaje correspondiente.
- Refrescar UC-30 y el detalle del proyecto.
- No repetir automáticamente los perfiles que fallaron.

---

## 5. Errores comunes de la operación

| Caso | HTTP | Acción |
|---|---|---|
| Lista vacía | `400` | Impedir el envío y pedir al menos un perfil. |
| Mensaje mayor a 200 | `400` | Mostrar el límite. |
| Sin perfil activo | `404` | Redirigir a selección de perfil. |
| Proyecto inexistente | `404` | Volver al listado de proyectos. |
| Requerimiento inexistente | `404` | Refrescar la búsqueda de requerimientos. |
| Sin permiso o no Director | `403` | Ocultar la acción. |
| Proyecto en estado inválido | `409` | Refrescar el proyecto. |
| Actividad en estado inválido | `409` | Refrescar la actividad. |

Los errores por perfil se informan dentro de `errores[]` con `200`, no como
error global.

---

## 6. Consideraciones

- No enviar una petición por perfil desde el frontend salvo como fallback; usar
  los endpoints batch para mantener una sola operación.
- No interpretar `200` como “todos creados”: revisar `creadas` y `errores`.
- No ofrecer reintento automático para perfiles con error.
- Un perfil que ya tiene invitación pendiente aparecerá en `errores` con código
  `INVITACION_DUPLICADA`.
- Un perfil que ya es miembro activo aparecerá en `errores`.
- Un perfil con profesión incompatible en actividad aparecerá en `errores`.

---

## 7. Checklist de aceptación

- [ ] Se pueden seleccionar varios perfiles.
- [ ] Se puede quitar un perfil de la selección.
- [ ] El mensaje es opcional y máximo 200 caracteres.
- [ ] Se muestra confirmación con la cantidad de perfiles.
- [ ] Se usa el endpoint batch correcto.
- [ ] Se interpretan `creadas` y `errores` por separado.
- [ ] El éxito parcial no se muestra como error total.
- [ ] Los perfiles con error muestran su motivo.
- [ ] No se reintenta automáticamente.
- [ ] UC-30 y el proyecto se refrescan después de la operación.
- [ ] Cada invitación creada queda en estado `PENDIENTE`.
