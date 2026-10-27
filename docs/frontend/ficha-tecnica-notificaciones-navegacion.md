# Ficha Técnica Frontend — Navegación de notificaciones

Reglas para decidir cuándo una notificación debe mostrar una **label
clickeable** y a qué pantalla debe llevar. El caso principal es la
notificación `POSTULACION_RECIBIDA` que recibe un director cuando alguien se
postula a su proyecto.

---

## 1. Resumen rápido

| Aspecto | Valor |
| :--- | :--- |
| Cambio de contrato | Nuevo campo `idProyecto` (nullable) en `NotificacionResponse` |
| Origen del dato | Derivado en lectura de `entidadTipo + entidadId`; **no** se persiste |
| Endpoints afectados | `GET /notificaciones` y `PATCH /notificaciones/{id}/leida` |
| Caso principal | `POSTULACION_RECIBIDA` → `/proyectos/{idProyecto}/postulaciones` |
| Tipos sin link | `ACTIVIDAD_MODIFICADA`, `ACTIVIDAD_ELIMINADA`, `INVITACION_RECHAZADA`, `HITO_CUMPLIDO`, etc. |
| Regla de oro | Mostrar la label solo si `idProyecto != null` **y** el tipo tiene destino permitido |

---

## 2. Contrato de `NotificacionResponse`

El campo nuevo se agrega al final del objeto. Es nullable.

```json
{
  "idNotificacion": 30,
  "tipo": "POSTULACION_RECIBIDA",
  "mensaje": "Nueva postulación de Luna al proyecto 'Desfile Primavera'.",
  "entidadTipo": "POSTULACION_GRAL",
  "entidadId": 77,
  "leida": false,
  "fechaLeida": null,
  "origen": "SERVICIO",
  "fechaGenerada": "2026-10-27T12:00:00",
  "idPerfilContexto": 20,
  "idProyecto": 105
}
```

### Significado de los campos

| Campo | Descripción |
| :--- | :--- |
| `tipo` | Tipo funcional de la notificación (`POSTULACION_RECIBIDA`, `PROYECTO_CANCELADO`, etc.) |
| `entidadTipo` | Tipo de la entidad referenciada (`POSTULACION_GRAL`, `POSTULACION_ACTIVIDAD`, `PROYECTO`, `ACTIVIDAD`, `HITO`, …) |
| `entidadId` | Id de esa entidad. En postulaciones es el id de la postulación, **no** el del proyecto |
| `idPerfilContexto` | Perfil con el que se generó la notificación. Sirve para filtrar por perfil activo |
| `idProyecto` | Proyecto asociado, derivado. Puede ser `null` |

> `idProyecto` **no** es una columna nueva de la base. El backend lo calcula a
> partir de la postulación referenciada. Por eso también funciona para
> notificaciones antiguas.

### Cuándo viene `idProyecto`

| `entidadTipo` | `idProyecto` |
| :--- | :--- |
| `PROYECTO` | Igual a `entidadId` |
| `POSTULACION_GRAL` | Proyecto del requerimiento general de la postulación |
| `POSTULACION_ACTIVIDAD` | Proyecto de la actividad de la postulación |
| `ACTIVIDAD`, `HITO`, `INVITACION_*` | `null` (fuera del alcance actual) |

Esto aplica a **todos** los tipos de notificación que usen esas entidades, no
solo a `POSTULACION_RECIBIDA`. Por ejemplo, `POSTULACION_ACEPTADA` y
`POSTULACION_RECHAZADA` también traen `idProyecto`, aunque las reglas de
navegación son distintas (ver sección 5).

---

## 3. Endpoints

| Método | Ruta | Acción |
| :--- | :--- | :--- |
| `GET` | `/notificaciones` | Listar notificaciones paginadas |
| `GET` | `/notificaciones/resumen` | Totales y no leídas |
| `PATCH` | `/notificaciones/{idNotificacion}/leida` | Marcar una como leída |
| `PATCH` | `/notificaciones/leidas` | Marcar todas como leídas |

### 3.1. Listar

```http
GET /notificaciones?idPerfilContexto=20&page=0&size=20
```

| Parámetro | Tipo | Descripción |
| :--- | :--- | :--- |
| `idPerfilContexto` | `long` | Opcional. Filtra por el perfil activo |
| `page` | `int` | Página, desde `0` (default `0`) |
| `size` | `int` | Tamaño de página (default `20`, máximo `100`) |

Respuesta: `PaginaResponse<NotificacionResponse>`.

```json
{
  "contenido": [ /* notificaciones */ ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 1,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

**Recomendación:** enviar siempre `idPerfilContexto` con el perfil activo. Si
se omite, el backend devuelve las notificaciones de todos los perfiles del
usuario.

### 3.2. Marcar una como leída

```http
PATCH /notificaciones/30/leida
```

Devuelve el mismo `NotificacionResponse`, incluido `idProyecto`. Se puede usar
para actualizar el ítem en pantalla sin recargar toda la lista.

### 3.3. Marcar todas como leídas

```http
PATCH /notificaciones/leidas?idPerfilContexto=20
```

Respuesta:

```json
{ "actualizadas": 3 }
```

---

## 4. Regla principal: `POSTULACION_RECIBIDA`

Es el caso que motiva esta ficha.

```text
Si tipo == "POSTULACION_RECIBIDA" y idProyecto != null:
    mostrar label clickeable "Ver postulaciones"
    navegar a /proyectos/{idProyecto}/postulaciones
```

- Aplica tanto a `entidadTipo = "POSTULACION_GRAL"` como a
  `entidadTipo = "POSTULACION_ACTIVIDAD"`.
- La pantalla `/proyectos/{idProyecto}/postulaciones` ya lista ambos tipos.
- El destinatario de esta notificación es un **director activo** del proyecto,
  por lo que tiene permiso `GESTIONAR_POSTULACIONES` y el backend no
  responderá `403` en esa pantalla.
- No usar `entidadId` para armar la ruta: `entidadId` es el id de la
  postulación, no el del proyecto.

---

## 5. Reglas por tipo de notificación

| `tipo` | ¿Label? | Destino | Observaciones |
| :--- | :--- | :--- | :--- |
| `POSTULACION_RECIBIDA` | Sí, si `idProyecto != null` | `/proyectos/{idProyecto}/postulaciones` | Caso principal. El destinatario es director |
| `PROYECTO_CANCELADO` | Sí | `/proyectos/{idProyecto ?? entidadId}` | El destinatario es miembro o exmiembro; el backend permite ver proyectos cancelados |
| `POSTULACION_ACEPTADA` | Opcional | `/proyectos/{idProyecto}` | El perfil pasa a ser miembro activo; el detalle del proyecto es accesible |
| `POSTULACION_RECHAZADA` | **No** | — | El perfil no es miembro. Si el proyecto es privado o fue cancelado, `/proyectos/{id}` responde `403` |
| `ACTIVIDAD_MODIFICADA` | No | — | `idProyecto` viene `null` en el alcance actual |
| `ACTIVIDAD_ELIMINADA` | No | — | `idProyecto` viene `null` en el alcance actual |
| `INVITACION_RECHAZADA` | No | — | `idProyecto` viene `null` en el alcance actual |
| `HITO_CUMPLIDO` / otros | No | — | Sin destino definido |

> Para `POSTULACION_RECHAZADA` no mostrar una label que lleve a la gestión del
> proyecto. Si más adelante se quiere ofrecer algo, el destino correcto es una
> pantalla de **“Mis postulaciones”**, no `/proyectos/{id}`.

---

## 6. Implementación sugerida

### 6.1. Tipo TypeScript

```ts
export interface Notificacion {
  idNotificacion: number;
  tipo: string;
  mensaje: string;
  entidadTipo: string | null;
  entidadId: number | null;
  leida: boolean;
  fechaLeida: string | null;
  origen: string;
  fechaGenerada: string;
  idPerfilContexto: number | null;
  idProyecto: number | null;
}
```

### 6.2. Helper de destino

```ts
export interface DestinoNotificacion {
  etiqueta: string;
  ruta: string;
}

export function destinoNotificacion(
  notificacion: Notificacion
): DestinoNotificacion | null {
  switch (notificacion.tipo) {
    case 'POSTULACION_RECIBIDA':
      return notificacion.idProyecto != null
        ? {
            etiqueta: 'Ver postulaciones',
            ruta: `/proyectos/${notificacion.idProyecto}/postulaciones`,
          }
        : null;

    case 'PROYECTO_CANCELADO': {
      const idProyecto = notificacion.idProyecto ?? notificacion.entidadId;
      return idProyecto != null
        ? {
            etiqueta: 'Ver proyecto',
            ruta: `/proyectos/${idProyecto}`,
          }
        : null;
    }

    case 'POSTULACION_ACEPTADA':
      return notificacion.idProyecto != null
        ? {
            etiqueta: 'Ver proyecto',
            ruta: `/proyectos/${notificacion.idProyecto}`,
          }
        : null;

    // POSTULACION_RECHAZADA y el resto no tienen destino permitido.
    default:
      return null;
  }
}
```

### 6.3. Uso en el componente

```tsx
const destino = destinoNotificacion(notificacion);

return (
  <article>
    <p>{notificacion.mensaje}</p>

    {destino && (
      <a href={destino.ruta} onClick={() => marcarComoLeida(notificacion.idNotificacion)}>
        {destino.etiqueta}
      </a>
    )}
  </article>
);
```

Sugerencia de UX:

- Marcar como leída al hacer click en la label (o dejar que la pantalla destino
  lo haga).
- Si `idProyecto` es `null`, no renderizar el link; la notificación se muestra
  igual como texto.
- No inferir el destino desde `entidadTipo`/`entidadId` en el front: la regla
  vive en `destinoNotificacion` y el backend ya entrega el id correcto.

---

## 7. Casos borde

| Caso | Comportamiento |
| :--- | :--- |
| `idProyecto` es `null` | No mostrar label. La notificación sigue siendo visible |
| La postulación fue eliminada | `idProyecto` puede ser `null`; no mostrar label |
| `POSTULACION_RECHAZADA` | Aunque `idProyecto` no sea `null`, no mostrar link a gestión |
| Proyecto privado y perfil no miembro | El backend responde `403` en `/proyectos/{id}`; por eso no se linkea el rechazo |
| Notificación de otro perfil | Filtrar por `idPerfilContexto` para no mezclar perfiles del mismo usuario |
| `idProyecto = 0` o valores inválidos | Tratar como ausente (el backend no debería enviarlos) |

---

## 8. Seguridad y autorización

- El backend toma `idUsuario` e `idPerfilActivo` de la sesión (cookie `jwt`).
- El frontend **no** envía `idUsuario` ni `idPerfil`.
- Las rutas de gestión de postulaciones exigen:
  - ser **director activo** del proyecto;
  - tener el permiso `GESTIONAR_POSTULACIONES`.
- El detalle `/proyectos/{id}` permite:
  - miembros activos;
  - exmiembros de proyectos cancelados;
  - no miembros solo si el proyecto es `PUBLICO` y no está `BORRADOR` ni `CANCELADO`.
- Los errores esperables son `401`, `403`, `404`.

---

## 9. Qué NO hacer

- No agregar ni asumir una columna `id_proyecto` en la base: el dato es
  derivado y puede cambiar si cambia la entidad referenciada.
- No usar `entidadId` como `idProyecto` en postulaciones.
- No mostrar la label de postulaciones a un postulante rechazado.
- No mostrar links para tipos sin destino definido (`ACTIVIDAD_*`,
  `INVITACION_*`, `HITO_*`).
- No hardcodear `POSTULACION_GRAL` o `POSTULACION_ACTIVIDAD` para decidir la
  ruta: ambos van a la misma pantalla, usando `idProyecto`.

---

## 10. Referencias

- `src/main/java/org/mgroko/backend/notificaciones/dto/NotificacionResponse.java`
- `src/main/java/org/mgroko/backend/notificaciones/servicio/NotificacionService.java`
- `src/main/java/org/mgroko/backend/notificaciones/servicio/NotificacionDestinoResolver.java`
- [`ficha-tecnica-buscar-postulaciones.md`](./ficha-tecnica-buscar-postulaciones.md)
- [`ficha-tecnica-gestionar-postulaciones.md`](./ficha-tecnica-gestionar-postulaciones.md)
