# 13. Mensajería

ModaLink tiene dos canales de mensajería: el **chat privado** entre contactos y el **chat grupal** del proyecto.

## 13.1. Abrir una conversación privada

**Objetivo:** conversar con un contacto de tu red.

**Quién puede hacerlo:** usuario con perfil activo y una conexión activa con el otro perfil.

**Pasos:**

1. Abrí **Mensajes** desde la barra superior.
2. En **Chats recientes**, elegí una conversación existente.
3. Si no tenés conversación, abrí la pestaña **Contactos** y elegí un contacto.
4. También podés iniciar el chat desde **Mi red** con el botón **Mensaje**, o desde un perfil con **Mensaje**.

**Resultado esperado:** se abre el chat con el nombre, la profesión y el estado de presencia del contacto.

**Reglas y restricciones:**

- Solo se puede chatear con perfiles que tengan una conexión activa.
- No podés iniciar una conversación con tu propio perfil.
- No podés chatear entre perfiles de la misma cuenta.
- Si la conexión se elimina, la conversación queda en **Solo lectura**.
- La conversación se crea la primera vez que escribís.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “No podés iniciar una conversación con este perfil. Necesitás una conexión activa.” | No hay conexión. | Enviá una solicitud de conexión. |
| “No podés iniciar una conversación con tu propio perfil.” | Elegiste tu perfil. | Elegí otro contacto. |
| “No podés chatear entre perfiles del mismo usuario.” | Elegiste otro perfil tuyo. | Elegí otro contacto. |
| “El perfil ya no está disponible.” | El perfil se dio de baja. | Elegí otro contacto. |
| “La conversación ya no está disponible.” | La conversación fue eliminada. | Volvé a la lista. |

[CAPTURA: mensajes - lista de chats recientes y panel de conversación]

## 13.2. Buscar conversaciones y contactos

**Objetivo:** encontrar rápido un chat o un contacto.

**Quién puede hacerlo:** usuario con perfil activo.

**Pasos:**

1. En **Mensajes**, usá **Chats recientes** o **Contactos**.
2. En **Chats recientes**, buscá con **Buscar por nombre, profesión o mensaje** y filtrá por **Todos**, **No leídos** o **En línea**.
3. En **Contactos**, buscá con **Buscar por nombre o profesión** y filtrá por **Todos** o **En línea**.
4. Si hay más resultados, usá **Cargar más conversaciones** o **Cargar más contactos**.

**Resultado esperado:** ves solo los chats o contactos que coinciden. Los contactos se agrupan por profesión.

**Reglas y restricciones:**

- La lista de contactos incluye solo conexiones activas.
- Los contactos se agrupan por profesión; los que no tienen profesión aparecen en **Sin profesión**.
- El filtro **No leídos** muestra conversaciones con mensajes sin leer.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “No hay conversaciones que coincidan con la búsqueda.” | La búsqueda no devuelve resultados. | Probá con otro texto. |
| “No hay contactos que coincidan con la búsqueda.” | La búsqueda no devuelve resultados. | Probá con otro texto. |
| “Todavía no tenés contactos. Agregá conexiones desde Mi red.” | No tenés conexiones. | Enviá solicitudes de conexión. |
| “No se pudieron cargar los contactos.” | Problema de conexión. | Reintentá. |

## 13.3. Enviar mensajes en un chat privado

**Objetivo:** escribir y enviar mensajes a un contacto.

**Quién puede hacerlo:** usuario con perfil activo y conexión activa.

**Pasos:**

1. Abrí una conversación.
2. Escribí en **Escribí un mensaje.**.
3. Enviá con el botón **Enviar** o con Enter.
4. Si querés, usá el selector de emojis.

**Resultado esperado:** el mensaje aparece en la conversación y el destinatario recibe una notificación.

**Reglas y restricciones:**

- El mensaje admite hasta 1.000 caracteres.
- No se pueden enviar mensajes vacíos.
- No hay adjuntos, edición ni borrado de mensajes.
- El historial se carga desde el más reciente; podés usar **Cargar mensajes anteriores**.
- Cuando el otro perfil está escribiendo, ves **Escribiendo**.
- El sistema marca la conversación como leída al abrirla y mantenerla visible.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El mensaje no puede estar vacío ni superar los 1000 caracteres.” | El mensaje está vacío o es demasiado largo. | Corregilo. |
| “La conexión ya no está activa. Podés leer el historial, pero no enviar mensajes.” | La conexión se eliminó. | Volvé a conectar. |
| “No se pudo enviar el mensaje.” | Problema de conexión. | Volvé a intentar. |
| “No se pudo cargar el historial.” | Problema de conexión. | Reintentá. |

[CAPTURA: chat-privado - historial, indicador Escribiendo y compositor]

## 13.4. Leer un chat cuando la conexión ya no está activa

**Objetivo:** consultar el historial de una conversación sin poder enviar mensajes.

**Quién puede hacerlo:** participante de la conversación.

**Pasos:**

1. Abrí **Mensajes**.
2. Elegí la conversación.
3. Leé el historial.

**Resultado esperado:** ves el badge **Solo lectura** y el aviso “La conexión ya no está activa. Podés leer el historial, pero no enviar mensajes.” El compositor no está disponible.

**Reglas y restricciones:**

- El historial se conserva.
- No se pueden enviar mensajes nuevos hasta que vuelva a existir una conexión activa.
- La conversación no se elimina.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “La conexión ya no está activa. Podés leer el historial, pero no enviar mensajes.” | La conexión se eliminó. | Volvé a conectar si querés seguir chateando. |

[CAPTURA: chat-solo-lectura - badge Solo lectura y aviso]

## 13.5. Usar el chat grupal del proyecto

**Objetivo:** conversar con todo el equipo de un proyecto.

**Quién puede hacerlo:** miembro activo del proyecto.

**Pasos:**

1. Abrí el proyecto y entrá a la pestaña **Chat grupal**.
2. Revisá la lista de participantes.
3. Escribí en **Escribí un mensaje al equipo.**.
4. Enviá con **Enviar** o Enter.

**Resultado esperado:** el mensaje aparece para todos los participantes y se genera una notificación **Mensaje en el chat del proyecto** para quienes no lo leyeron.

**Reglas y restricciones:**

- Solo los miembros activos pueden acceder al chat.
- Se puede enviar mientras el proyecto esté **Publicado** o **Confirmado**.
- Si el proyecto está **Finalizado** o **Cancelado**, el chat queda en solo lectura.
- El mensaje admite hasta 1.000 caracteres.
- No hay adjuntos, edición, borrado, búsqueda ni confirmación de lectura por mensaje.
- El historial se conserva.
- Los Directores y miembros activos aparecen como participantes.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El chat del proyecto ya no admite mensajes porque el proyecto no está Publicado ni Confirmado.” | El proyecto está finalizado o cancelado. | Solo se puede leer el historial. |
| “El perfil no está activo para usar el chat grupal.” | Tu perfil está dado de baja. | Elegí otro perfil. |
| “No se pudo enviar el mensaje. Verificá que el proyecto siga admitiendo mensajes.” | El proyecto cambió de estado. | Recargá el proyecto. |
| “Todavía no hay mensajes en este proyecto.” | No hay mensajes. | Escribí el primero. |

[CAPTURA: chat-grupal - cabecera con participantes, historial y compositor]

## 13.6. Reglas generales de mensajería

**Objetivo:** conocer los límites y el alcance de los mensajes.

- El emisor siempre es el **perfil activo**.
- El chat privado requiere una conexión activa entre ambos perfiles.
- El chat grupal requiere membresía activa en un proyecto **Publicado** o **Confirmado**.
- Los mensajes admiten hasta 1.000 caracteres.
- El historial de un chat privado queda en solo lectura si se elimina la conexión.
- En el chat grupal se puede leer el historial después de finalizar o cancelar el proyecto, pero no enviar.
- No hay adjuntos, edición, borrado ni búsqueda de mensajes.
