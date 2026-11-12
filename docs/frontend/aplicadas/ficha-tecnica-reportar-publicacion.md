# Ficha técnica — Reportar publicación (UC-69)

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-14 (ver perfil), UC-15 (reportar perfil), UC-20 (baja de publicación), UC-22 (buscar publicación), UC-66 (interactuar con publicación) y UC-67 (auditoría).

## 1. Objetivo

Permitir que un usuario autenticado, con perfil activo, reporte una publicación
de otro usuario indicando un motivo y, opcionalmente, una descripción. El
reporte queda en estado `PENDIENTE` y genera una alerta para los administradores.

El administrador puede consultar los reportes de publicación, ver el detalle,
resolverlos o descartarlos y, de forma independiente, ocultar o reactivar la
publicación. Resolver un reporte **no oculta ni elimina** la publicación
automáticamente: solo cambia el estado del reporte. La moderación de la
publicación es una acción separada.

El dueño de una publicación oculta recibe una notificación sin link. Puede
volver a ver únicamente el título y la descripción desde la búsqueda específica
de publicaciones ocultas de su perfil, donde la única acción disponible es
eliminarla.

## 2. Alcance

Incluye:

- Acción “Reportar publicación” en el área de usuario/perfiles.
- Formulario de reporte con motivo y descripción opcional.
- Bandeja administrativa de reportes de publicación.
- Detalle del reporte para el administrador.
- Acciones administrativas `Resolver` y `Descartar`.
- Acciones administrativas `Ocultar` y `Reactivar` publicación.
- Búsqueda específica de publicaciones ocultas para su dueño.
- Alerta in-app para administradores.

No incluye:

- Listado de “mis reportes” para el usuario.
- Moderación automática de la publicación.
- Eliminación física de la publicación desde el panel admin.
- Reactivación por parte del dueño.
- Edición o eliminación de reportes por parte del administrador.
- Reapertura de reportes resueltos o descartados.
- Adjuntos, imágenes o evidencia adicional en el reporte.

## 3. Permisos y precondiciones

| Área | Rol requerido | Perfil activo | Observaciones |
|---|---|---|---|
| Usuario/perfiles | `USUARIO` | Sí | El backend toma el perfil activo de la sesión |
| Panel admin | `ADMINISTRADOR` | No | El administrador no opera con perfil activo |

Reglas principales:

- El usuario debe estar autenticado.
- El usuario debe tener un perfil activo.
- El administrador no puede reportar publicaciones.
- Solo se pueden reportar publicaciones `ACTIVA`.
- El perfil dueño y el usuario dueño de la publicación deben estar activos.
- El dueño de la publicación no puede reportarla.
- Un colaborador sí puede reportar una publicación colaborativa.
- Un perfil autor no puede tener dos reportes `PENDIENTE` sobre la misma publicación.

## 4. Flujo de usuario

### 4.1 Dónde mostrar la acción

Agregar la acción **“Reportar publicación”** en:

- El menú de opciones (`...`) de cada tarjeta de publicación del feed.
- El menú de opciones de las publicaciones del portfolio de un perfil.
- El menú o barra de acciones del detalle de publicación.
- El detalle abierto desde una notificación de publicación.

Para decidir si mostrar la acción, usar los datos que ya expone
`PublicacionResponse`: `estado` y `autor.idPerfil`.

No mostrar la acción:

- En publicaciones propias del perfil activo.
- En publicaciones que no estén `ACTIVA`.
- A usuarios sin rol `USUARIO`.

> El backend bloquea el autorreporte por **usuario**, no solo por perfil. Si un
> usuario tiene varios perfiles y uno de ellos es dueño de la publicación, el
> backend puede responder `409`. El front debe manejar ese caso aunque no haya
> ocultado la acción.

### 4.2 Apertura del formulario

Al seleccionar “Reportar publicación”, abrir un modal o página con:

- Título: “Reportar publicación”.
- Resumen de la publicación: título, nombre artístico del autor y, si está disponible, miniatura.
- Selector de motivo.
- Campo de descripción.
- Botones “Cancelar” y “Enviar reporte”.

No pedir fecha, estado, IDs de usuario ni IDs de perfil: los completa el backend.

### 4.3 Motivos

| Código | Etiqueta para el usuario | Descripción | ¿Exige descripción? |
|---|---|---|---|
| `SPAM` | Spam o publicidad engañosa | Publicidad repetida, engañosa o no solicitada | No |
| `CONTENIDO_INAPROPIADO` | Contenido inapropiado | Contenido ofensivo, violento o inapropiado | No |
| `IDENTIDAD_FALSA` | Identidad falsa | Suplantación o identidad engañosa | No |
| `OTRO` | Otro | Cualquier otro motivo | **Sí** |

El selector debe iniciar sin valor. No enviar un motivo por defecto.

### 4.4 Descripción

- Opcional para `SPAM`, `CONTENIDO_INAPROPRIADO` e `IDENTIDAD_FALSA`.
- Obligatoria para `OTRO`.
- Máximo **200 caracteres**.
- Mostrar contador `0/200`.
- Aplicar `trim()` antes de enviar.
- Si después del `trim()` queda vacía:
  - para `OTRO`: mostrar error y no enviar;
  - para los demás motivos: enviar `null` o no enviar el campo.

### 4.5 Envío

```http
POST /publicaciones/{idPublicacion}/reportes
Content-Type: application/json
```

Body:

```json
{
  "motivo": "CONTENIDO_INAPROPIADO",
  "descripcion": "El texto de la publicación es ofensivo."
}
```

Para los motivos que no exigen descripción:

```json
{
  "motivo": "SPAM",
  "descripcion": null
}
```

Respuesta exitosa:

```http
204 No Content
```

La respuesta **no tiene body**. No intentar parsear JSON.

### 4.6 Después de enviar

- Cerrar el modal.
- Mostrar toast o mensaje: “Reporte enviado. Será revisado por un administrador.”
- No ocultar la publicación.
- No marcar visualmente la publicación como reportada.
- No recargar toda la página necesariamente; alcanza con actualizar el estado local del modal.

No existe un endpoint para consultar si el usuario ya reportó esa publicación.
El front no debe intentar deducirlo antes de enviar: si ya existe un reporte
pendiente, el backend responde `409`.

### 4.7 Errores del alta

| HTTP | Caso | Mensaje esperado |
|---|---|---|
| `400` | Falta `motivo` | `El motivo del reporte es obligatorio.` en `errores.motivo` |
| `400` | Motivo inválido | `Solicitud inválida: revisá el formato de los datos enviados.` |
| `400` | `OTRO` sin descripción | `La descripción es obligatoria cuando el motivo es OTRO.` |
| `400` | Descripción mayor a 200 | `La descripción no puede superar los 200 caracteres.` |
| `403` | Sin rol `USUARIO` (incluye al administrador) | `No tenés permiso para acceder a este recurso.` |
| `404` | Sin perfil activo | `No hay un perfil activo seleccionado en la sesión.` |
| `404` | Publicación inexistente, oculta o dueño inactivo | `No se encontró la publicación indicada.` |
| `409` | Autorreporte | `No podés reportar una publicación propia.` |
| `409` | Reporte pendiente duplicado | `Ya existe un reporte pendiente de este perfil contra la misma entidad.` |
| `409` | Perfil activo en baja | `El perfil no se encuentra activo.` |

Recomendaciones de UX:

- `409` por autorreporte: cerrar el modal y mostrar “No podés reportar tu propia publicación.”
- `409` por duplicado: mostrar “Ya reportaste esta publicación y el reporte sigue pendiente.”
- `404` por publicación inexistente/oculta: mostrar “La publicación ya no está disponible.” y refrescar el listado o detalle.
- `404` por perfil activo: redirigir al selector de perfil activo o mostrar el flujo existente de UC-13.
- Errores `400`: mostrar el mensaje del backend y conservar los datos del formulario.

### 4.8 Estados de UI del formulario

- Botón “Enviar reporte” deshabilitado mientras:
  - no hay motivo seleccionado;
  - el motivo es `OTRO` y la descripción está vacía;
  - la descripción supera 200 caracteres;
  - hay una petición en curso.
- Mostrar spinner o texto “Enviando…” durante la petición.
- Evitar doble submit.
- Mantener el foco dentro del modal.
- Cerrar con `Esc` o “Cancelar” solo si no hay una petición en curso.

## 5. Integración en el área de usuario/perfiles

### 5.1 Perfil activo

El backend obtiene el perfil activo desde la sesión. Si no hay perfil activo:

- El endpoint responde `404`.
- El front debe ofrecer el flujo existente para seleccionar o crear un perfil.

No enviar `idPerfil` ni `idUsuario` en el body.

### 5.2 Publicación propia

Si `publicacion.autor.idPerfil === perfilActivo.idPerfil`:

- Ocultar o deshabilitar “Reportar publicación”.
- Mostrar, si hace falta, un tooltip: “No podés reportar tu propia publicación.”

Si el usuario tiene varios perfiles y la publicación pertenece a otro perfil del
mismo usuario, el front puede no detectarlo. En ese caso, manejar el `409` del
backend.

### 5.3 Publicación no disponible

Si la publicación deja de estar `ACTIVA` entre que se abre el formulario y se
envía, el backend responde `404`. Mostrar el mensaje y refrescar la vista.

## 6. Flujo administrativo

### 6.1 Entradas al panel

Agregar en el panel de administración:

- Ítem de menú: **“Reportes de publicaciones”**.
- Ruta sugerida: `/admin/reportes-publicacion`.
- Ruta de detalle sugerida: `/admin/reportes-publicacion/{idReporte}`.
- Badge opcional con cantidad de pendientes.

Para el badge se puede consultar:

```http
GET /admin/reportes-publicacion?estado=PENDIENTE&page=0&size=1
```

y usar `totalElementos`. No existe un endpoint de conteo separado.

### 6.2 Listado

Mostrar una tabla o lista con:

| Columna | Campo |
|---|---|
| Fecha del reporte | `fechaReporte` |
| Publicación | `tituloPublicacion` |
| Perfil dueño de la publicación | `nombrePerfilPublicacion` |
| Autor del reporte | `nombreUsuarioAutor` / `nombrePerfilAutor` |
| Motivo | `motivoDescripcion` |
| Estado | `estado` |
| Acciones | “Revisar” |

Estados vacíos:

- Sin reportes: “No hay reportes de publicaciones.”
- Sin resultados con filtros: “No hay reportes que coincidan con los filtros.”

### 6.3 Detalle

Mostrar:

- Datos del reporte:
  - `idReporte`
  - `fechaReporte`
  - `estado`
- Motivo:
  - `motivo`
  - `motivoDescripcion`
  - `descripcion`
- Autor del reporte:
  - `autor.nombre`
  - `autor.apellido`
  - `autor.correo`
  - `autor.nombreArtistico`
- Publicación reportada:
  - `publicacion.titulo`
  - `publicacion.descripcion`
  - `publicacion.fechaPublicacion`
  - `publicacion.estado`
  - `publicacion.nombreArtistico`
  - `publicacion.nombreUsuario`
- Resolución:
  - `idAdminRevisor`
  - `nombreAdminRevisor`
  - `fechaResolucion`

> Importante: el administrador **no debe abrir** `GET /publicaciones/{id}`
> ni `GET /publicaciones/{id}/detalle`. Esos endpoints son del área de usuario
> y requieren rol `USUARIO`. Para el detalle del reporte, usar únicamente los
> datos embebidos en `GET /admin/reportes-publicacion/{idReporte}`.

### 6.4 Acciones

Para un reporte `PENDIENTE`:

- **Resolver**: `PATCH` con `{ "estado": "RESUELTO" }`.
- **Descartar**: `PATCH` con `{ "estado": "DESCARTADO" }`.

Mostrar un diálogo de confirmación antes de enviar, porque la transición es
irreversible.

Para un reporte `RESUELTO` o `DESCARTADO`:

- Ocultar o deshabilitar ambas acciones.
- Mostrar quién lo resolvió y cuándo.

Después de un `PATCH` exitoso:

- Usar la respuesta para actualizar el detalle.
- Refrescar el listado o actualizar la fila local.
- Si el listado estaba filtrado por `PENDIENTE`, quitar la fila o refrescar.
- Mostrar toast: “Reporte resuelto.” o “Reporte descartado.”

### 6.5 Moderación de la publicación

La moderación es independiente del estado del reporte. Para una publicación:

- Si `publicacion.estado = ACTIVA`, mostrar **“Ocultar publicación”**.
- Si `publicacion.estado = OCULTA`, mostrar **“Reactivar publicación”**.
- Si `publicacionEliminada = true`, no mostrar ninguna de las dos.

Al ocultar:

- Pedir un motivo obligatorio, máximo 200 caracteres.
- Mostrar confirmación.
- Enviar `PATCH /admin/publicaciones/{idPublicacion}/estado` con
  `{ "estado": "OCULTA", "motivo": "..." }`.
- Al confirmar, la publicación deja de ser visible para todos los usuarios.
- El dueño recibe una notificación sin link.

Al reactivar:

- Enviar `PATCH /admin/publicaciones/{idPublicacion}/estado` con
  `{ "estado": "ACTIVA", "motivo": "..." }`.
- El motivo es opcional.
- El dueño recibe una notificación con link a `/publicaciones/{idPublicacion}`.

En ambos casos, actualizar el detalle con la respuesta y refrescar el listado.

### 6.6 Publicación eliminada

Si `publicacionEliminada = true`:

- Mostrar una advertencia: “La publicación fue eliminada. El reporte se conserva como evidencia.”
- No mostrar enlace a la publicación.
- Mostrar los datos que aún existan en el reporte.
- Permitir resolver o descartar el reporte normalmente.

Si la publicación sigue existiendo pero su estado es `OCULTA`:

- Mostrarla con el badge “Oculta”.
- No tratarla como eliminada.
- No habilitar un enlace público si el flujo de usuario no permite verla.

### 6.7 Notificaciones de admin

Cuando llega una notificación:

```json
{
  "tipo": "PUBLICACION_REPORTADA",
  "titulo": "Publicación reportada",
  "entidadTipo": "REPORTE_PUBLICACION",
  "entidadId": 12,
  "link": null
}
```

Navegar a:

```text
/admin/reportes-publicacion/12
```

El backend no devuelve `link` para este tipo actualmente. Usar
`entidadTipo === "REPORTE_PUBLICACION"` y `entidadId` para construir la ruta.
Si en el futuro `link` viene informado, priorizarlo.

Marcar la notificación como leída con el endpoint existente:

```http
PATCH /notificaciones/{idNotificacion}/leida
```

## 7. Contrato API — Usuario

### 7.1 Crear reporte

```http
POST /publicaciones/{idPublicacion}/reportes
Content-Type: application/json
```

Path params:

| Parámetro | Tipo | Obligatorio |
|---|---|---|
| `idPublicacion` | number | Sí |

Body:

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `motivo` | string | Sí | `SPAM`, `CONTENIDO_INAPROPIADO`, `IDENTIDAD_FALSA`, `OTRO` |
| `descripcion` | string \| null | No | Máximo 200; obligatoria si `motivo = OTRO` |

Respuesta:

- `204 No Content`.

Errores: ver sección 4.7.

## 8. Contrato API — Administrador

### 8.1 Listar reportes de publicación

```http
GET /admin/reportes-publicacion
```

Query params:

| Parámetro | Tipo | Default | Descripción |
|---|---|---|---|
| `estado` | string | todos | `PENDIENTE`, `RESUELTO` o `DESCARTADO` |
| `motivo` | string | todos | `SPAM`, `CONTENIDO_INAPROPIADO`, `IDENTIDAD_FALSA` u `OTRO` |
| `fechaDesde` | `YYYY-MM-DD` | sin límite | Primer día incluido |
| `fechaHasta` | `YYYY-MM-DD` | sin límite | Último día incluido |
| `page` | number | `0` | Página, base 0 |
| `size` | number | `20` | Tamaño de página |
| `todos` | boolean | `false` | Si es `true`, ignora la paginación |

Recomendación: usar `page`/`size`, no `todos`, salvo exportaciones o vistas
acotadas.

Respuesta `200 OK`:

```json
{
  "contenido": [
    {
      "idReporte": 12,
      "motivo": "CONTENIDO_INAPROPIADO",
      "motivoDescripcion": "Contenido inapropiado",
      "descripcion": "El texto de la publicación es ofensivo.",
      "estado": "PENDIENTE",
      "fechaReporte": "2026-11-11T10:30:00",
      "publicacionEliminada": false,
      "idPublicacion": 45,
      "tituloPublicacion": "Editorial Otoño",
      "estadoPublicacion": "ACTIVA",
      "idPerfilPublicacion": 7,
      "nombrePerfilPublicacion": "Luna Diseños",
      "idUsuarioAutor": 3,
      "nombreUsuarioAutor": "Carla Pérez",
      "idPerfilAutor": 10,
      "nombrePerfilAutor": "Carla Studio"
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

Si la publicación fue eliminada:

- `publicacionEliminada`: `true`
- `tituloPublicacion`: `null`
- `estadoPublicacion`: `null`
- `idPerfilPublicacion`: `null`
- `nombrePerfilPublicacion`: `null`
- `idPublicacion` se conserva con el ID reportado.

Orden por defecto: `fechaReporte` descendente y luego `idReporte` descendente.

Error de rango de fechas:

- `400` con `La fecha desde no puede ser posterior a la fecha hasta.`

### 8.2 Obtener detalle

```http
GET /admin/reportes-publicacion/{idReporte}
```

Respuesta `200 OK`:

```json
{
  "idReporte": 12,
  "entidadTipo": "PUBLICACION",
  "entidadId": 45,
  "motivo": "CONTENIDO_INAPROPIADO",
  "motivoDescripcion": "Contenido inapropiado",
  "motivoPersistido": "CONTENIDO_OFENSIVO",
  "descripcion": "El texto de la publicación es ofensivo.",
  "estado": "PENDIENTE",
  "fechaReporte": "2026-11-11T10:30:00",
  "publicacionEliminada": false,
  "publicacion": {
    "idPublicacion": 45,
    "titulo": "Editorial Otoño",
    "descripcion": "Producción realizada en Buenos Aires.",
    "fechaPublicacion": "2026-11-10T14:30:00",
    "estado": "ACTIVA",
    "idPerfil": 7,
    "nombreArtistico": "Luna Diseños",
    "idUsuario": 2,
    "nombreUsuario": "Ana Gómez"
  },
  "autor": {
    "idUsuario": 3,
    "nombre": "Carla",
    "apellido": "Pérez",
    "correo": "carla@example.com",
    "idPerfil": 10,
    "nombreArtistico": "Carla Studio"
  },
  "idAdminRevisor": null,
  "nombreAdminRevisor": null,
  "fechaResolucion": null
}
```

Si la publicación fue eliminada:

```json
{
  "publicacionEliminada": true,
  "publicacion": null,
  "entidadTipo": "PUBLICACION",
  "entidadId": 45
}
```

El campo `motivoPersistido` es interno y puede ser útil para soporte. Para la
UI usar `motivo` y `motivoDescripcion`.

### 8.3 Resolver o descartar

```http
PATCH /admin/reportes-publicacion/{idReporte}/estado
Content-Type: application/json
```

Body:

```json
{
  "estado": "RESUELTO"
}
```

Valores permitidos:

- `RESUELTO`
- `DESCARTADO`

No se permite `PENDIENTE`.

Respuesta `200 OK`: el detalle completo actualizado.

Ejemplo:

```json
{
  "idReporte": 12,
  "estado": "RESUELTO",
  "idAdminRevisor": 99,
  "nombreAdminRevisor": "Admin Sistema",
  "fechaResolucion": "2026-11-11T12:00:00"
}
```

Errores:

| HTTP | Caso | Mensaje esperado |
|---|---|---|
| `400` | Falta `estado` | `El estado es obligatorio.` en `errores.estado` |
| `403` | Sin rol `ADMINISTRADOR` | `No tenés permiso para acceder a este recurso.` |
| `404` | Reporte inexistente o no es de publicación | `El reporte no existe.` |
| `409` | Se intenta volver a `PENDIENTE` | `Solo se puede pasar de PENDIENTE a RESUELTO o DESCARTADO.` |
| `409` | El reporte ya fue gestionado | `El reporte ya fue gestionado y no admite nuevas transiciones.` |

### 8.4 Ocultar o reactivar publicación

```http
PATCH /admin/publicaciones/{idPublicacion}/estado
Content-Type: application/json
```

Body para ocultar:

```json
{
  "estado": "OCULTA",
  "motivo": "Contenido inapropiado confirmado"
}
```

Body para reactivar:

```json
{
  "estado": "ACTIVA",
  "motivo": "Apelación aceptada"
}
```

Reglas:

- `estado` es obligatorio y solo admite `ACTIVA` u `OCULTA`.
- `motivo` es obligatorio al ocultar y opcional al reactivar.
- `motivo` máximo 200 caracteres.
- El motivo se registra únicamente en auditoría.
- No se puede ocultar una publicación ya oculta ni reactivar una ya activa.

Respuesta `200 OK`: la publicación administrativa actualizada. Incluye
`estado`, `puedeOcultar`, `puedeReactivar`, `cantidadReportes` y
`cantidadReportesPendientes`.

Errores:

| HTTP | Caso |
|---|---|
| `400` | Falta `estado` o falta `motivo` al ocultar |
| `403` | Sin rol `ADMINISTRADOR` |
| `404` | Publicación inexistente |
| `409` | La publicación ya está en el estado solicitado |

### 8.5 Búsqueda de publicaciones ocultas del dueño

```http
GET /publicaciones/ocultas?texto=editorial&page=0&size=10
```

Solo devuelve publicaciones `OCULTA` del perfil activo. La respuesta expone
únicamente `idPublicacion`, `titulo`, `descripcion`, `fechaPublicacion` y
`estado`. No incluye imágenes, proyecto, ubicación ni interacciones.

El dueño solo puede eliminar cada publicación con el endpoint existente:

```http
DELETE /publicaciones/{idPublicacion}
```

## 9. Tipos TypeScript sugeridos

```ts
export type MotivoReportePublicacion =
  | 'SPAM'
  | 'CONTENIDO_INAPROPIADO'
  | 'IDENTIDAD_FALSA'
  | 'OTRO';

export type EstadoReporte = 'PENDIENTE' | 'RESUELTO' | 'DESCARTADO';

export type EstadoPublicacion = 'ACTIVA' | 'OCULTA';

export interface CrearReportePublicacionRequest {
  motivo: MotivoReportePublicacion;
  descripcion?: string | null;
}

export interface PaginaResponse<T> {
  contenido: T[];
  paginaActual: number;
  tamanoPagina: number;
  totalElementos: number;
  totalPaginas: number;
  primera: boolean;
  ultima: boolean;
}

export interface AdminReportePublicacionResumen {
  idReporte: number;
  motivo: MotivoReportePublicacion;
  motivoDescripcion: string;
  descripcion: string | null;
  estado: EstadoReporte;
  fechaReporte: string;
  publicacionEliminada: boolean;
  idPublicacion: number;
  tituloPublicacion: string | null;
  estadoPublicacion: EstadoPublicacion | null;
  idPerfilPublicacion: number | null;
  nombrePerfilPublicacion: string | null;
  idUsuarioAutor: number;
  nombreUsuarioAutor: string;
  idPerfilAutor: number;
  nombrePerfilAutor: string;
}

export interface AdminReportePublicacionDetalle {
  idReporte: number;
  entidadTipo: 'PUBLICACION';
  entidadId: number;
  motivo: MotivoReportePublicacion;
  motivoDescripcion: string;
  motivoPersistido:
    | 'SPAM'
    | 'CONTENIDO_OFENSIVO'
    | 'SUPLANTACION'
    | 'PERFIL_FALSO'
    | 'OTRO';
  descripcion: string | null;
  estado: EstadoReporte;
  fechaReporte: string;
  publicacionEliminada: boolean;
  publicacion: {
    idPublicacion: number;
    titulo: string;
    descripcion: string | null;
    fechaPublicacion: string;
    estado: EstadoPublicacion;
    idPerfil: number;
    nombreArtistico: string;
    idUsuario: number;
    nombreUsuario: string;
    imagenes: { idImagen: number; url: string; orden: number }[];
    proyecto: { idProyecto: number; nombre: string; puedeVerProyecto: boolean } | null;
    ubicacion: unknown | null;
    colaboradores: {
      idPerfil: number;
      nombreArtistico: string;
      profesion: string | null;
      fotoUrl: string | null;
    }[];
    cantidadMeGusta: number;
    cantidadComentarios: number;
    cantidadReportes: number;
    cantidadReportesPendientes: number;
    puedeOcultar: boolean;
    puedeReactivar: boolean;
  } | null;
  autor: {
    idUsuario: number;
    nombre: string;
    apellido: string;
    correo: string;
    idPerfil: number;
    nombreArtistico: string;
  };
  idAdminRevisor: number | null;
  nombreAdminRevisor: string | null;
  fechaResolucion: string | null;
}

export interface ActualizarEstadoReporteRequest {
  estado: 'RESUELTO' | 'DESCARTADO';
}
```

## 10. Reglas de negocio que el front debe respetar

1. La publicación debe ser `ACTIVA` para poder reportarse.
2. El dueño de la publicación no puede reportarla.
3. El administrador no puede reportar publicaciones.
4. Solo existe un reporte `PENDIENTE` por perfil autor y publicación.
5. Un reporte resuelto o descartado no bloquea un nuevo reporte posterior.
6. `OTRO` exige descripción.
7. La descripción máxima es 200 caracteres.
8. El reporte nace siempre en `PENDIENTE`; el front no envía estado.
9. El front no envía fecha, autor, perfil, entidad ni tipo de entidad.
10. Resolver o descartar no modifica la publicación.
11. No se puede reabrir un reporte gestionado.
12. Si la publicación se elimina, el reporte se conserva.
13. El reporte de publicación no aparece en `/admin/reportes-perfil`.
14. El reporte de perfil no aparece en `/admin/reportes-publicacion`.
15. `fechaReporte`, `fechaResolucion` y `fechaPublicacion` son `LocalDateTime`
    sin zona horaria. Parsearlas como hora local.
16. Los filtros `fechaDesde` y `fechaHasta` usan el formato `YYYY-MM-DD` y son
    inclusivos.

## 11. Estados y etiquetas sugeridas

| Estado | Etiqueta | Color sugerido |
|---|---|---|
| `PENDIENTE` | Pendiente de revisión | Ámbar |
| `RESUELTO` | Resuelto | Verde |
| `DESCARTADO` | Descartado | Gris |

| Motivo | Etiqueta | Color sugerido |
|---|---|---|
| `SPAM` | Spam | Naranja |
| `CONTENIDO_INAPROPIADO` | Contenido inapropiado | Rojo |
| `IDENTIDAD_FALSA` | Identidad falsa | Violeta |
| `OTRO` | Otro | Gris |

## 12. Rutas y componentes sugeridos

| Ruta | Área | Contenido |
|---|---|---|
| `/publicaciones/{idPublicacion}` | Usuario | Detalle de publicación; acción “Reportar publicación” |
| `/perfiles/{idPerfil}` | Usuario | Portfolio; acción en cada tarjeta |
| `/admin/reportes-publicacion` | Admin | Listado y filtros |
| `/admin/reportes-publicacion/{idReporte}` | Admin | Detalle y acciones |

Componentes sugeridos:

- `ReportarPublicacionModal`
- `ReportarPublicacionForm`
- `AdminReportesPublicacionPage`
- `AdminReportePublicacionDetailPage`
- `ReportePublicacionEstadoBadge`
- `ReportePublicacionMotivoBadge`
- `ConfirmarResolucionReporteDialog`

## 13. Ejemplos de integración

### 13.1 Crear reporte

```ts
async function reportarPublicacion(
  idPublicacion: number,
  body: CrearReportePublicacionRequest
): Promise<void> {
  const response = await fetch(`/publicaciones/${idPublicacion}/reportes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(response.status, error);
  }
}
```

### 13.2 Listar reportes

```ts
async function listarReportesPublicacion(params: {
  estado?: EstadoReporte;
  motivo?: MotivoReportePublicacion;
  fechaDesde?: string;
  fechaHasta?: string;
  page?: number;
  size?: number;
}): Promise<PaginaResponse<AdminReportePublicacionResumen>> {
  const query = new URLSearchParams();
  if (params.estado) query.set('estado', params.estado);
  if (params.motivo) query.set('motivo', params.motivo);
  if (params.fechaDesde) query.set('fechaDesde', params.fechaDesde);
  if (params.fechaHasta) query.set('fechaHasta', params.fechaHasta);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  const response = await fetch(`/admin/reportes-publicacion?${query}`, {
    credentials: 'include',
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(response.status, error);
  }

  return response.json();
}
```

### 13.3 Resolver reporte

```ts
async function actualizarEstadoReporte(
  idReporte: number,
  estado: 'RESUELTO' | 'DESCARTADO'
): Promise<AdminReportePublicacionDetalle> {
  const response = await fetch(
    `/admin/reportes-publicacion/${idReporte}/estado`,
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ estado }),
    }
  );

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(response.status, error);
  }

  return response.json();
}
```

## 14. Checklist de implementación

### Usuario

- [ ] Agregar “Reportar publicación” en tarjetas y detalle.
- [ ] Ocultar la acción en publicaciones propias.
- [ ] Ocultar la acción si el perfil activo no es `USUARIO`.
- [ ] Implementar modal con selector de motivo.
- [ ] Implementar descripción opcional con contador 0/200.
- [ ] Exigir descripción para `OTRO`.
- [ ] Deshabilitar submit sin motivo o durante el envío.
- [ ] Enviar `POST /publicaciones/{id}/reportes`.
- [ ] Manejar `204` sin parsear body.
- [ ] Manejar `400`, `403`, `404` y `409`.
- [ ] Mostrar éxito sin ocultar la publicación.
- [ ] Redirigir al selector de perfil si no hay perfil activo.
- [ ] Implementar búsqueda específica de publicaciones ocultas.
- [ ] Mostrar solo título y descripción, con ícono de error y leyenda.
- [ ] Ofrecer únicamente la acción “Eliminar”.

### Admin

- [ ] Agregar ítem “Reportes de publicaciones” al menú.
- [ ] Implementar ruta `/admin/reportes-publicacion`.
- [ ] Implementar filtros por estado, motivo y fechas.
- [ ] Implementar paginación con `page`/`size`.
- [ ] Mostrar badges de estado y motivo.
- [ ] Implementar detalle con datos de publicación y autor.
- [ ] Mostrar advertencia si `publicacionEliminada`.
- [ ] Implementar acciones “Resolver” y “Descartar” con confirmación.
- [ ] Deshabilitar acciones si el reporte no está `PENDIENTE`.
- [ ] Actualizar la UI con la respuesta del `PATCH`.
- [ ] Implementar acciones “Ocultar” y “Reactivar” publicación.
- [ ] Pedir motivo obligatorio al ocultar.
- [ ] Mostrar imágenes, proyecto, ubicación y contadores de la publicación.
- [ ] Mostrar total y pendientes de reportes de la publicación.
- [ ] Manejar `400`, `403`, `404` y `409`.
- [ ] Navegar desde la notificación `PUBLICACION_REPORTADA` a `/admin/reportes-publicacion/{entidadId}`.
- [ ] Marcar la notificación como leída al abrirla.
- [ ] Mostrar badge de pendientes usando `totalElementos`.

## 15. Fuera de alcance / notas

- No hay endpoint para que el usuario consulte sus reportes.
- No se notifica al usuario que reporta.
- El dueño recibe notificación de ocultamiento (sin link) y de reactivación (con link).
- No hay endpoint para eliminar un reporte.
- No hay reapertura de reportes.
- No se adjuntan imágenes ni archivos.
- La descripción no admite HTML enriquecido.
- El backend registra auditoría del alta, de la resolución y de la moderación; el front no debe enviar datos de auditoría.
- Ocultar y resolver el reporte son acciones independientes. El front no debe asumir que una dispara la otra.
- La publicación oculta no se elimina: se conserva para auditoría y para una posible reactivación.
