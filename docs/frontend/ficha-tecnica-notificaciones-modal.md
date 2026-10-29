# Ficha Técnica — Modal de notificaciones

Contrato para el modal de notificaciones: título y cuerpo separados, IDs para
construir el destino, perfil a activar antes de navegar y reglas de permisos.

> Esta ficha reemplaza a `ficha-tecnica-notificaciones-navegacion.md`.

---

## 1. Resumen

| Aspecto | Valor |
| :--- | :--- |
| Endpoint de listado | `GET /notificaciones` |
| Endpoint de resumen | `GET /notificaciones/resumen` |
| Marcado individual | `PATCH /notificaciones/{idNotificacion}/leida` |
| Marcado masivo | `PATCH /notificaciones/leidas` |
| Campos nuevos | `titulo`, `idActividad`, `idHito`, `idInvitacion` |
| `idProyecto` | Ya existía; ahora se captura al generar la notificación |
| Backend devuelve URLs/HTML/íconos/colores | **No** |

---

## 2. Contrato de `NotificacionResponse`

```json
{
  "idNotificacion": 30,
  "tipo": "PROYECTO_CANCELADO",
  "titulo": "Proyecto cancelado",
  "mensaje": "El proyecto 'Desfile Primavera' fue cancelado. Motivo: ...",
  "entidadTipo": "PROYECTO",
  "entidadId": 105,
  "leida": false,
  "fechaLeida": null,
  "origen": "SERVICIO",
  "fechaGenerada": "2026-10-27T12:00:00",
  "idPerfilContexto": 20,
  "idProyecto": 105,
  "idActividad": null,
  "idHito": null,
  "idInvitacion": null
}
```

### 2.1. Campos

| Campo | Tipo | Nullable | Descripción |
| :--- | :--- | :--- | :--- |
| `idNotificacion` | `Long` | No | Identificador de la notificación |
| `tipo` | `String` | No | Tipo funcional (`PROYECTO_CANCELADO`, etc.) |
| `titulo` | `String` | Sí | Título legible derivado de `tipo` |
| `mensaje` | `String` | No | Cuerpo de la notificación |
| `entidadTipo` | `String` | Sí | Tipo de entidad referenciada |
| `entidadId` | `Long` | Sí | ID de la entidad referenciada |
| `leida` | `Boolean` | No | Estado de lectura |
| `fechaLeida` | `LocalDateTime` | Sí | Fecha de lectura |
| `origen` | `String` | No | `SERVICIO`, `TRIGGER` o `SCHEDULER` |
| `fechaGenerada` | `LocalDateTime` | No | Fecha de generación |
| `idPerfilContexto` | `Long` | Sí | Perfil que debe activarse antes de navegar |
| `idProyecto` | `Long` | Sí | Proyecto asociado, capturado o derivado |
| `idActividad` | `Long` | Sí | Actividad asociada, si corresponde |
| `idHito` | `Long` | Sí | Hito asociado, si corresponde |
| `idInvitacion` | `Long` | Sí | Invitación asociada, si corresponde |

### 2.2. Reglas de los IDs específicos

| Campo | Se completa cuando |
| :--- | :--- |
| `idActividad` | `entidadTipo = "ACTIVIDAD"` |
| `idHito` | `entidadTipo = "HITO"` |
| `idInvitacion` | `entidadTipo = "INVITACION_GRAL"` o `"INVITACION_ACTIVIDAD"` |

En esos casos, `entidadId` contiene el mismo valor. El frontend debe preferir
los campos explícitos.

`entidadId` **no** es el proyecto en postulaciones:

- `POSTULACION_GRAL` → ID de la postulación general.
- `POSTULACION_ACTIVIDAD` → ID de la postulación de actividad.

Para navegar usar siempre `idProyecto`.

### 2.3. `titulo`

- Se deriva del `tipo`.
- Para un tipo desconocido viene `null`; usar el fallback que ya tenga el frontend.
- Ejemplos: `Proyecto cancelado`, `Postulación recibida`, `Actividad eliminada`,
  `Ajuste horario`, `Hito cumplido`.

---

## 3. Endpoints

### 3.1. Listar

```http
GET /notificaciones?idPerfilContexto=20&page=0&size=20
```

| Parámetro | Tipo | Descripción |
| :--- | :--- | :--- |
| `idPerfilContexto` | `long` | Opcional. Filtra por el perfil activo |
| `page` | `int` | Página, desde `0` |
| `size` | `int` | Tamaño, default `20`, máximo `100` |

- Orden garantizado: `fechaGenerada DESC, idNotificacion DESC`.
- Respuesta: `PaginaResponse<NotificacionResponse>`.

### 3.2. Resumen

```http
GET /notificaciones/resumen?idPerfilContexto=20
```

```json
{ "total": 5, "noLeidas": 2 }
```

### 3.3. Marcado

```http
PATCH /notificaciones/30/leida
PATCH /notificaciones/leidas?idPerfilContexto=20
```

No cambian su semántica. El marcado individual devuelve el
`NotificacionResponse` actualizado.

---

## 4. `idPerfilContexto`

- Es el perfil con el que se generó la notificación.
- El frontend debe activarlo antes de navegar.
- Si viene `null`:
  - no activar ningún perfil;
  - no mostrar link;
  - redirigir al dashboard de usuario si la navegación es obligatoria.
- No se infiere ni se completa desde backend para notificaciones antiguas o
  globales.

---

## 5. Matriz de destinos

| `tipo` | Datos mínimos | Destino |
| :--- | :--- | :--- |
| `PROYECTO_CANCELADO` | `idProyecto` | `/proyectos/{idProyecto}` |
| `POSTULACION_RECIBIDA` | `idProyecto` | `/proyectos/{idProyecto}/postulaciones` |
| `POSTULACION_ACEPTADA` | `idProyecto` | `/proyectos/{idProyecto}` |
| `POSTULACION_RECHAZADA` | — | Sin link |
| `ACTIVIDAD_MODIFICADA` | `idProyecto` + `idActividad` | `/proyectos/{idProyecto}/actividades/{idActividad}` |
| `ACTIVIDAD_ELIMINADA` | `idProyecto` + `idActividad` | Solo `/proyectos/{idProyecto}`; **no** linkear a la actividad |
| `AJUSTE_HORARIO` | `idProyecto` + `idActividad` | `/proyectos/{idProyecto}/actividades/{idActividad}` |
| `SOBRECOMPROMISO` | `idProyecto` + `idActividad` | `/proyectos/{idProyecto}/actividades/{idActividad}` |
| `INVITACION_RECHAZADA` | `idProyecto` + `idInvitacion` | Sin link por defecto |
| `HITO_CUMPLIDO` | `idProyecto` + `idHito` | `/proyectos/{idProyecto}` |
| Otros | — | Sin link |

### 5.1. Reglas de implementación

```ts
export interface DestinoNotificacion {
  etiqueta: string;
  ruta: string;
}

export function destinoNotificacion(n: Notificacion): DestinoNotificacion | null {
  switch (n.tipo) {
    case 'PROYECTO_CANCELADO':
      return n.idProyecto != null
        ? { etiqueta: 'Ver proyecto', ruta: `/proyectos/${n.idProyecto}` }
        : null;

    case 'POSTULACION_RECIBIDA':
      return n.idProyecto != null
        ? { etiqueta: 'Ver postulaciones', ruta: `/proyectos/${n.idProyecto}/postulaciones` }
        : null;

    case 'POSTULACION_ACEPTADA':
      return n.idProyecto != null
        ? { etiqueta: 'Ver proyecto', ruta: `/proyectos/${n.idProyecto}` }
        : null;

    case 'ACTIVIDAD_MODIFICADA':
    case 'AJUSTE_HORARIO':
    case 'SOBRECOMPROMISO':
      return n.idProyecto != null && n.idActividad != null
        ? {
            etiqueta: 'Ver actividad',
            ruta: `/proyectos/${n.idProyecto}/actividades/${n.idActividad}`,
          }
        : null;

    case 'ACTIVIDAD_ELIMINADA':
      return n.idProyecto != null
        ? { etiqueta: 'Ver proyecto', ruta: `/proyectos/${n.idProyecto}` }
        : null;

    case 'HITO_CUMPLIDO':
      return n.idProyecto != null
        ? { etiqueta: 'Ver proyecto', ruta: `/proyectos/${n.idProyecto}` }
        : null;

    // POSTULACION_RECHAZADA, INVITACION_RECHAZADA y el resto: sin link.
    default:
      return null;
  }
}
```

> Para `INVITACION_RECHAZADA` no existe todavía una pantalla de invitaciones.
> Cuando se implemente, el destino puede pasar a usar `idInvitacion`.

---

## 6. Permisos y rutas

| Ruta / caso | Regla |
| :--- | :--- |
| `GET /proyectos/{id}` | Miembro activo; exmiembro si el proyecto está `CANCELADO`; no miembro solo si es público y no cancelado |
| `/proyectos/{id}/postulaciones` | Requiere director activo y `GESTIONAR_POSTULACIONES` |
| `/proyectos/{id}/actividades/{idActividad}` | Requiere membresía activa y `VER_PROYECTO`; si la actividad no existe, `404` |
| `POSTULACION_RECHAZADA` | Sin link. El perfil rechazado puede no tener acceso al proyecto |
| “Mis postulaciones” | No existe endpoint/pantalla; no linkear por ahora |
| `INVITACION_RECHAZADA` | Sin link; no existe pantalla de invitaciones |
| `HITO_CUMPLIDO` | Sin pantalla de hitos; linkear al proyecto y conservar `idHito` |

---

## 7. Casos borde

| Caso | Comportamiento |
| :--- | :--- |
| `idProyecto = null` | No mostrar link; la notificación se muestra igual |
| `idPerfilContexto = null` | No activar perfil; no navegar o ir al dashboard de usuario |
| `ACTIVIDAD_ELIMINADA` | La actividad ya no existe: no linkear a la actividad |
| `POSTULACION_RECHAZADA` | Aunque `idProyecto` no sea `null`, no linkear a gestión |
| Notificación histórica sin proyecto | `idProyecto = null`; no mostrar link |
| Tipo desconocido | `titulo = null`; usar fallback del frontend; sin link |

### 7.1. Limitación histórica

Las notificaciones nuevas capturan `idProyecto` al generarse. Las históricas se
resuelven en lectura mientras la entidad referenciada exista.

No se pueden resolver de forma determinística:

- `ACTIVIDAD_ELIMINADA` cuya actividad ya fue borrada.
- `POSTULACION_RECHAZADA` / `INVITACION_RECHAZADA` generadas por eliminación de
  actividad, cuyas postulaciones o invitaciones también fueron borradas.

En esos casos `idProyecto` queda `null` y el frontend no debe mostrar link.

---

## 8. TypeScript

```ts
export interface Notificacion {
  idNotificacion: number;
  tipo: string;
  titulo: string | null;
  mensaje: string;
  entidadTipo: string | null;
  entidadId: number | null;
  leida: boolean;
  fechaLeida: string | null;
  origen: string;
  fechaGenerada: string;
  idPerfilContexto: number | null;
  idProyecto: number | null;
  idActividad: number | null;
  idHito: number | null;
  idInvitacion: number | null;
}
```

---

## 9. Qué no hacer

- No usar `entidadId` como `idProyecto` en postulaciones.
- No linkear `POSTULACION_RECHAZADA` a la gestión del proyecto.
- No linkear `ACTIVIDAD_ELIMINADA` al detalle de la actividad.
- No asumir que `idPerfilContexto` siempre viene.
- No esperar URLs, íconos ni colores desde el backend.
