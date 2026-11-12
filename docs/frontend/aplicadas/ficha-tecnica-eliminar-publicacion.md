# Ficha técnica — Baja publicación (UC-20)

> **Estado:** backend implementado.
> **Casos de uso relacionados:** UC-19 (alta), UC-22 (búsqueda) y UC-61/62/63
> (solicitudes de colaboración).

## 1. Objetivo

Permitir que el perfil dueño elimine definitivamente una publicación y todos sus
archivos multimedia. Permitir que un colaborador quite la publicación de su
propio perfil sin eliminarla para el dueño ni para los demás colaboradores.

Los colaboradores son etiquetas “soft”: aparecen en la publicación y esta se
muestra en su portfolio, pero no son dueños, no editan, no aprueban y no
invitan.

## 2. Endpoint

```http
DELETE /publicaciones/{idPublicacion}
```

Sin body.

**Respuesta `204 No Content`**

## 3. Reglas de negocio

### 3.1 El perfil activo es el dueño

- Se elimina la publicación de forma **física**.
- Se borran todas sus imágenes y archivos.
- Se borran las etiquetas de `colaborador_publicacion`.
- Se borran las solicitudes de colaboración de la publicación.
- Se borran las notificaciones de colaboración asociadas.
- La publicación desaparece del perfil del dueño, de los colaboradores y del
  feed/comunidad.

### 3.2 El perfil activo es colaborador

- Solo se elimina su fila de `colaborador_publicacion`.
- La publicación sigue existiendo.
- Las imágenes, archivos, solicitudes y notificaciones no se tocan.
- La publicación sigue visible para el dueño y los demás colaboradores.
- La publicación sigue en el feed/comunidad.
- El colaborador desaparece de la lista `colaboradores` y deja de recibir
  notificaciones de UC-66.
- No hay notificación para el colaborador que se quita la etiqueta.

### 3.3 Reglas generales

- Si el perfil no es dueño ni colaborador: `404`.
- Si el colaborador ya se quitó la etiqueta: `404`.
- Si la publicación no existe: `404`.
- Si el perfil activo está en baja: `409`.
- Las publicaciones `OCULTA` siguen las mismas reglas.
- No se audita.
- No hay restauración.
- No se agrega un flag `puedeEliminar`; el frontend puede inferir la acción
  comparando `autor.idPerfil` y `colaboradores` con el perfil activo.

## 4. Errores

| HTTP | Caso |
|---|---|
| `404` | Sin perfil activo, publicación inexistente, perfil ajeno o colaborador ya desetiquetado |
| `409` | Perfil activo en baja |
| `403` | Sin rol `USUARIO` |

## 5. Frontend

- En el portfolio propio:
  - si el perfil activo es el dueño, mostrar “Eliminar publicación”;
  - si el perfil activo está en `colaboradores`, mostrar “Eliminar de mi perfil”.
- Pedir confirmación antes de ejecutar el `DELETE`.
- Si responde `204`:
  - el dueño debe sacar la publicación de todas las vistas;
  - el colaborador solo debe sacarla de su propio portfolio.
- Las imágenes y archivos no se eliminan cuando la baja es de un colaborador.
- No mostrar acciones de edición ni invitación a colaboradores.

## 6. Consideraciones

- El dueño puede eliminar una publicación con colaboradores; en ese caso la
  publicación desaparece para todos.
- Un colaborador no puede eliminar la publicación para los demás.
- Las solicitudes pendientes de una publicación colaborativa permanecen si un
  colaborador se quita la etiqueta.
- Si el dueño elimina la publicación, las solicitudes se eliminan con ella.
- No se modifica el estado de la publicación cuando se quita una etiqueta.
