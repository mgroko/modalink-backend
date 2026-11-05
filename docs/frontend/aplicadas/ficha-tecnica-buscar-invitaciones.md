# Ficha Técnica Frontend: Buscar Invitaciones (UC-30)

Especificación de la API, filtros, respuesta y manejo de errores para que un
usuario consulte las invitaciones que recibió o envió desde su perfil activo.

No modifica las fichas de postulaciones existentes.

---

## 1. Endpoint y autenticación

- **Método:** `GET`
- **URL:** `/invitaciones/buscar`
- **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header
  `X-XSRF-TOKEN`).
- **Perfil activo:** obligatorio en la sesión. El frontend no envía
  `idPerfilActivo`.
- **Alcance:** sólo invitaciones donde el perfil activo es remitente o
  destinatario. No se exige membresía ni visibilidad del proyecto.
- **Respuesta exitosa:** `200 OK` con `PaginaResponse<InvitacionBusquedaResponse>`.

---

## 2. Parámetros de búsqueda

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `idProyecto` | `Long` | No | Filtra por proyecto exacto. |
| `nombreProyecto` | `String` | No | Búsqueda parcial e insensible a mayúsculas. |
| `fechaDesde` | `YYYY-MM-DD` | No | Fecha mínima de envío, inclusive. |
| `fechaHasta` | `YYYY-MM-DD` | No | Fecha máxima de envío, inclusive. |
| `nombrePerfil` | `String` | No | Nombre artístico, nombre o apellido del remitente/destinatario. |
| `direccion` | `RECIBIDA` / `ENVIADA` | No | Si se omite, devuelve ambas. |
| `estado` | `PENDIENTE` / `ACEPTADA` / `RECHAZADA` | No | Filtro por estado. |
| `tipo` | `GENERAL` / `ACTIVIDAD` | No | Filtro por tipo. |
| `page` | `int` | No | Por defecto `0`. |
| `size` | `int` | No | Por defecto `20`. |
| `todos` | `boolean` | No | Si es `true`, no pagina. |

### Regla de `nombrePerfil`

| Dirección | Perfil sobre el que filtra |
|---|---|
| `RECIBIDA` | Remitente |
| `ENVIADA` | Destinatario |
| Omitida | Remitente o destinatario, excluyendo el propio perfil activo |

### Regla de fechas

- `fechaDesde` y `fechaHasta` son inclusivas.
- Si se envían ambas y `fechaDesde > fechaHasta`, el backend responde `400`.

---

## 3. Flujo de pantalla

### 3.1 Entrada

Desde el menú de invitaciones del perfil activo:

1. Cargar la pantalla sin filtros para mostrar todas las invitaciones.
2. Mostrar un selector de dirección: **Todas**, **Recibidas**, **Enviadas**.
3. Mostrar filtros opcionales por proyecto, fechas, perfil, estado y tipo.

### 3.2 Búsqueda

1. El usuario completa o modifica los filtros.
2. El frontend envía `GET /invitaciones/buscar` con los parámetros elegidos.
3. Se muestran los resultados paginados.
4. Si no hay resultados, mostrar un estado vacío con el mensaje
   “No tenés invitaciones que coincidan con los filtros”.

### 3.3 Acciones por ítem

- **Invitación recibida pendiente:** ofrecer el flujo de aceptar o rechazar
  (UC-28), no eliminar.
- **Invitación enviada pendiente:** si `puedeEliminar = true`, ofrecer la acción
  de eliminar cuando se implemente UC-32.
- **Invitación aceptada o rechazada:** mostrar como historial, sin acciones de
  gestión.
- En todos los casos, mostrar:
  - Proyecto.
  - Tipo: general o por actividad.
  - Dirección: recibida o enviada.
  - Remitente y destinatario.
  - Fecha de envío.
  - Estado.
  - Mensaje, si existe.
  - Actividad y requerimiento, si es una invitación por actividad.

---

## 4. Response payload

```json
{
  "contenido": [
    {
      "idInvitacion": 123,
      "tipo": "ACTIVIDAD",
      "direccion": "RECIBIDA",
      "estado": "PENDIENTE",
      "fechaEnvio": "2026-11-04T12:00:00",
      "idProyecto": 5,
      "nombreProyecto": "Desfile 2026",
      "idRequerimientoGral": null,
      "idRequerimientoActividad": 30,
      "idActividad": 40,
      "nombreActividad": "Prueba de vestuario",
      "remitente": {
        "idPerfil": 11,
        "nombreArtistico": "Luna Diseños",
        "nombre": "Luna",
        "apellido": "Pérez",
        "idProfesion": 4,
        "nombreProfesion": "Diseñador de Moda"
      },
      "destinatario": {
        "idPerfil": 10,
        "nombreArtistico": "Sol Modelo",
        "nombre": "Sol",
        "apellido": "Gómez",
        "idProfesion": 2,
        "nombreProfesion": "Modelo"
      },
      "mensaje": "Te invito a cubrir este requerimiento.",
      "puedeEliminar": false
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 1,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

Para una invitación general:

- `tipo`: `GENERAL`.
- `idRequerimientoActividad`, `idActividad` y `nombreActividad`: `null`.
- `idRequerimientoGral`: puede ser `null` si la invitación es general al
  proyecto.

---

## 5. Errores

| Caso | HTTP | Acción en frontend |
|---|---|---|
| Sin perfil activo | `404` | Redirigir a la selección de perfil. |
| `fechaDesde` posterior a `fechaHasta` | `400` | Marcar el rango y no reintentar. |
| Sin resultados | `200` | Mostrar estado vacío. |
| Error inesperado | `5xx` | Mostrar error genérico y permitir reintentar. |

---

## 6. Checklist de aceptación

- [ ] La pantalla carga sin filtros y muestra las invitaciones del perfil activo.
- [ ] Se puede alternar entre todas, recibidas y enviadas.
- [ ] Se puede filtrar por proyecto, fechas, perfil, estado y tipo.
- [ ] Se distingue visualmente una invitación general de una de actividad.
- [ ] Se muestra el proyecto, la contraparte, la fecha y el estado.
- [ ] Las invitaciones de actividad muestran actividad y requerimiento.
- [ ] El estado vacío se muestra cuando no hay coincidencias.
- [ ] Un rango de fechas inválido no se envía o se informa correctamente.
- [ ] `puedeEliminar` se usa para habilitar la acción de UC-32.
- [ ] Las invitaciones recibidas pendientes no ofrecen “Eliminar”.
- [ ] No se consultan invitaciones de otros perfiles.
