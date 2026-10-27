# Ficha Técnica Frontend — UC-34 Gestionar postulaciones

Especificación de la API para que un **director de proyecto** acepte o rechace
postulaciones de un proyecto. El listado de postulaciones se documenta en
[`ficha-tecnica-buscar-postulaciones.md`](./ficha-tecnica-buscar-postulaciones.md).

---

## 1. Resumen rápido

| Aspecto | Valor |
| :--- | :--- |
| Actor | Director activo del proyecto |
| Permiso | `GESTIONAR_POSTULACIONES` |
| Autenticación | Cookie `jwt` + header `X-XSRF-TOKEN` |
| Perfil activo | Requerido; lo resuelve el backend desde la sesión |
| Confirmación | Modal en el frontend; el `POST` se envía solo al confirmar |
| Body | No se envía body |
| Éxito | `200 OK` + `PostulacionGestionadaResponse` |
| Errores esperables | `401`, `403`, `404`, `409` |

---

## 2. Autenticación

- El backend toma `idUsuario` e `idPerfilActivo` de la sesión.
- El frontend **no** envía `idPerfil`, `idUsuario` ni `idProyecto` en el body.
- Si no hay perfil activo, el backend responde `404`.

---

## 3. Endpoints

| Método | Ruta | Acción |
| :--- | :--- | :--- |
| `GET` | `/proyectos/{idProyecto}/postulaciones/buscar` | Listar postulaciones (UC-74) |
| `POST` | `/proyectos/{idProyecto}/postulaciones/general/{idPostulacion}/aceptar` | Aceptar general |
| `POST` | `/proyectos/{idProyecto}/postulaciones/general/{idPostulacion}/rechazar` | Rechazar general |
| `POST` | `/proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/aceptar` | Aceptar actividad |
| `POST` | `/proyectos/{idProyecto}/postulaciones/actividad/{idPostulacion}/rechazar` | Rechazar actividad |

`idPostulacion` es el `idPostulacion` devuelto por UC-74. No confundir el id
de una postulación general con el de una de actividad: pertenecen a tablas
distintas y las rutas las distinguen por `general` o `actividad`.

---

## 4. Aceptar postulación general

### 4.1. Flujo UI

1. En el listado, el director pulsa **Aceptar postulación**.
2. Mostrar un modal de confirmación con:
   - nombre del proyecto;
   - nombre artístico del postulante;
   - profesión;
   - requerimiento y cupo (`cubiertos` / `faltantes`).
3. Al confirmar, enviar:

```http
POST /proyectos/105/postulaciones/general/77/aceptar
```

4. Deshabilitar el botón mientras la petición está en curso.
5. En éxito, cerrar el modal, mostrar toast y refrescar el listado.

### 4.2. Efectos en el backend

- Crea un `MiembroProyecto` activo con rol `MIEMBRO`.
- Asocia la membresía al requerimiento general.
- Cambia la postulación a `ACEPTADA`.
- Notifica al postulante con `POSTULACION_ACEPTADA`.
- Si con esta aceptación se completa el cupo, el trigger rechaza las demás
  postulaciones `PENDIENTE` del mismo requerimiento y las notifica.

### 4.3. Reglas

- El proyecto debe estar `PUBLICADO` o `CONFIRMADO`.
- El requerimiento debe estar insatisfecho (`faltantes > 0`).
- La postulación debe estar `PENDIENTE`.
- El postulante debe estar activo.
- El postulante no puede ser ya miembro activo del proyecto.

---

## 5. Aceptar postulación de actividad

### 5.1. Flujo UI

Mismo flujo de confirmación que el general, mostrando además el nombre de la
actividad y su fecha/horario si está definido.

```http
POST /proyectos/105/postulaciones/actividad/88/aceptar
```

### 5.2. Efectos en el backend

- Revalida la disponibilidad del postulante.
- Si el postulante **no** es miembro, crea la membresía activa.
- Si ya es miembro `MIEMBRO`, reutiliza su membresía.
- Crea una `AsignacionActividad` vinculada al requerimiento de actividad.
- Cambia la postulación a `ACEPTADA`.
- Notifica `POSTULACION_ACEPTADA`.
- El trigger de asignación genera además `ACTIVIDAD_ASIGNADA`.

### 5.3. Reglas

- El proyecto debe estar `PUBLICADO` o `CONFIRMADO`.
- La actividad debe estar `PENDIENTE`.
- El requerimiento de actividad debe estar insatisfecho.
- El postulante debe estar activo.
- El postulante puede ser un miembro activo `MIEMBRO` con la profesión
  requerida.
- Un `DIRECTOR` no puede ser aceptado como postulante de actividad.
- No debe existir ya una asignación del miembro a esa actividad.
- Debe existir disponibilidad en el rango factible de la actividad.

---

## 6. Rechazar postulación

### 6.1. Flujo UI

1. El director pulsa **Rechazar postulación**.
2. Mostrar confirmación.
3. Al confirmar, enviar el `POST` correspondiente.
4. En éxito, refrescar el listado.

### 6.2. Efectos

- La postulación pasa a `RECHAZADA`.
- No se crea miembro ni asignación.
- No se libera cupo por sí mismo (el cupo se mide por miembros/asignaciones
  activos).
- Se notifica al postulante con `POSTULACION_RECHAZADA`.

> El rechazo es lógico: la postulación **no se borra**. Un perfil rechazado no
> puede volver a postularse al mismo requerimiento. La reincorporación futura
> se hará por invitación (UC-31).

---

## 7. Respuesta de gestión

```json
{
  "idPostulacion": 77,
  "tipo": "ACTIVIDAD",
  "estado": "ACEPTADA",
  "idProyecto": 105,
  "idRequerimiento": 20,
  "idPerfil": 12,
  "idMiembro": 88,
  "idAsignacionActividad": 45
}
```

En un rechazo:

```json
{
  "idPostulacion": 77,
  "tipo": "GENERAL",
  "estado": "RECHAZADA",
  "idProyecto": 105,
  "idRequerimiento": 30,
  "idPerfil": 12,
  "idMiembro": null,
  "idAsignacionActividad": null
}
```

---

## 8. Errores

| HTTP | Caso | Mensaje / origen | Acción UI |
| :---: | :--- | :--- | :--- |
| `401` | Sin sesión | Seguridad | Redirigir a login |
| `403` | No es director o no tiene `GESTIONAR_POSTULACIONES` | `AccesoDenegadoProyectoException` | Mostrar aviso; no reintentar |
| `404` | Sin perfil activo | `PerfilActivoNoSeleccionadoException` | Abrir selección de perfil |
| `404` | Proyecto inexistente | `ProyectoNoEncontradoException` | Volver al listado |
| `404` | Postulación inexistente o de otro proyecto | `PostulacionNoEncontradaException` | Refrescar listado |
| `409` | Postulación ya gestionada | `PostulacionYaGestionadaException` | Refrescar listado; ocultar acciones |
| `409` | Proyecto no permite gestionar postulaciones | `PostulacionNoPermitidaException` | Refrescar proyecto |
| `409` | Requerimiento satisfecho | `PostulacionNoPermitidaException` | Refrescar proyecto/listado |
| `409` | Postulante ya es miembro | `PostulacionNoPermitidaException` | Refrescar listado |
| `409` | Director intenta postularse a actividad | `PostulacionNoPermitidaException` | No ofrecer la acción |
| `409` | Actividad no pendiente | `PostulacionNoPermitidaException` | Refrescar actividades |
| `409` | Sin disponibilidad | `PostulacionNoPermitidaException` | Mostrar mensaje; ofrecer revisar calendario |
| `409` | Perfil inactivo | `PerfilEnBajaException` | Avisar que el perfil no está activo |

---

## 9. Recomendaciones de UI/UX

- Usar un modal de confirmación; nunca aceptar/rechazar con un solo clic.
- Deshabilitar el botón mientras la petición está en curso para evitar dobles
  envíos.
- Tras un `200`, refrescar el listado y el detalle del proyecto.
- Mostrar `PENDIENTE`, `ACEPTADA` y `RECHAZADA` con etiquetas claras.
- No confundir una postulación `ACEPTADA` cuyo miembro se dio de baja con cupo
  cubierto: usar `aportaCupo` y `satisfecho`.
- En actividades, informar que la aceptación también genera la asignación.
- En rechazos, explicar que la postulación queda rechazada y no se elimina.

---

## 10. Checklist de aceptación

- [ ] Solo se muestran acciones a directores con `GESTIONAR_POSTULACIONES`.
- [ ] Aceptar general usa la ruta `.../general/{id}/aceptar`.
- [ ] Aceptar actividad usa la ruta `.../actividad/{id}/aceptar`.
- [ ] Rechazar usa las rutas `.../rechazar`.
- [ ] Se muestra confirmación antes de cualquier acción.
- [ ] El botón se deshabilita durante la petición.
- [ ] Un `409` refresca el listado/proyecto y oculta o actualiza las acciones.
- [ ] Un `403` no se reintenta.
- [ ] Se muestra el cupo (`cubiertos`/`faltantes`) antes de aceptar.
- [ ] Se contempla que una aceptación puede auto-rechazar otras pendientes.
- [ ] No se navega a una pantalla de invitaciones: UC-31 aún no está implementado.
