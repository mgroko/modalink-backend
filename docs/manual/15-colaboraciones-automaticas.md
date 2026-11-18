# 15. Colaboraciones automáticas

Las colaboraciones automáticas son propuestas que el sistema genera cuando detecta que dos publicaciones comparten una imagen. La colaboración solo se activa si ambos dueños confirman.

## 15.1. Consultar las propuestas

**Objetivo:** ver las propuestas de colaboración detectadas para tus publicaciones.

**Quién puede hacerlo:** usuario con perfil activo.

**Pasos:**

1. Abrí **Solicitudes** y elegí la pestaña **Automáticas**. También podés entrar desde **Colaboraciones automáticas**.
2. Revisá las tarjetas de propuestas.
3. Para ver el historial completo, activá **Ver historial completo**.
4. Filtrá por **perfil de la contraparte**, **mi publicación**, **Fecha desde**, **Fecha hasta** y **Estado**.
5. Usá **Limpiar** para restablecer los filtros.

**Resultado esperado:** ves las propuestas con las dos publicaciones, el estado y las acciones disponibles.

**Reglas y restricciones:**

- Solo se muestran propuestas que involucran a tu perfil activo.
- Por defecto se muestran las propuestas **Pendientes**.
- **Ver historial completo** incluye **Confirmada**, **Rechazada**, **Cancelada** y **Disuelta**.
- La fecha **Desde** no puede ser posterior a **Hasta**.
- Si ambas publicaciones son tuyas, la propuesta se marca como autocolaboración.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “Todavía no hay colaboraciones automáticas registradas.” | No hay propuestas. | — |
| “No tenés propuestas de colaboración automática pendientes.” | No hay pendientes. | Activá **Ver historial completo**. |
| “No hay propuestas con el estado seleccionado.” | El filtro no devuelve resultados. | Cambiá el estado. |
| “La fecha desde no puede ser posterior a la fecha hasta.” | Rango invertido. | Corregí las fechas. |
| “No se pudieron cargar las colaboraciones automáticas.” | Problema de conexión. | Reintentá. |

[CAPTURA: colaboraciones-automaticas - listado de propuestas y filtros]

## 15.2. Consultar el detalle de una propuesta

**Objetivo:** ver las publicaciones, la imagen en común, el estado de cada parte y las fechas.

**Quién puede hacerlo:** perfil que participa de la propuesta.

**Pasos:**

1. En la lista, hacé clic en **Ver detalle**.
2. Revisá:
   - **Tu publicación** y la **Publicación de la contraparte**.
   - **Perfil de la contraparte** o **Tu otro perfil**.
   - **Imagen en común**.
   - **Estado de la propuesta**: estado general, tu estado y el estado de la contraparte.
   - **Fechas**: detección, confirmación y disolución.

**Resultado esperado:** ves toda la evidencia y el estado de la propuesta.

**Reglas y restricciones:**

- Si una publicación fue eliminada, se conserva el registro de la propuesta.
- Si la imagen de evidencia ya no está disponible, se informa.
- Las dos publicaciones deben pertenecer a perfiles activos.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “No se encontró la propuesta.” | La propuesta no existe. | Volvé al listado. |
| “El perfil activo no participa de esta propuesta.” | La propuesta es de otro perfil. | Cambiá de perfil. |

[CAPTURA: detalle-colaboracion-automatica - publicaciones, imagen y estados]

## 15.3. Confirmar o rechazar una propuesta

**Objetivo:** aceptar o rechazar la colaboración detectada.

**Quién puede hacerlo:** perfil que participa de la propuesta y todavía no respondió.

**Pasos:**

1. En la lista o en el detalle, hacé clic en **Confirmar colaboración** o **Rechazar propuesta**.
2. Leé el aviso.
3. Confirmá con **Confirmar colaboración** o **Rechazar propuesta**.

**Resultado esperado:**

- Al confirmar: la colaboración se activa cuando ambos perfiles confirmaron. Si ambas publicaciones son tuyas, una sola confirmación alcanza para las dos.
- Al rechazar: la propuesta queda **Rechazada** y no se agregan colaboradores.

**Reglas y restricciones:**

- Solo se puede responder una propuesta **Pendiente**.
- Cada perfil puede responder una sola vez.
- La colaboración se activa solo si ambos perfiles confirman.
- Si ambas publicaciones pertenecen a tu cuenta, la confirmación o el rechazo se aplica a ambos lados.
- Si una publicación o la imagen deja de estar disponible, la propuesta se **Cancela**.
- Si un perfil se quita como colaborador, la colaboración se **Disuelve**.
- El sistema no confirma ni rechaza propuestas automáticamente.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “La colaboración automática ya fue respondida por este perfil.” | Ya respondiste. | Actualizá el listado. |
| “La colaboración automática ya no está pendiente.” | La propuesta cambió de estado. | Actualizá el listado. |
| “La colaboración automática no pertenece al perfil activo.” | La propuesta es de otro perfil. | Cambiá de perfil. |
| “Una de las publicaciones ya no está disponible.” | La publicación se eliminó u ocultó. | La propuesta se cancela. |
| “La propuesta no existe o ya no está disponible.” | La propuesta fue eliminada. | Volvé al listado. |

[CAPTURA: confirmar-colaboracion-automatica - modal con publicaciones y aviso]

## 15.4. Entender los estados de una colaboración automática

**Objetivo:** saber qué significa cada estado.

| Estado | Significado |
|---|---|
| **Pendiente** | La propuesta fue detectada y espera la respuesta de uno o ambos perfiles. |
| **Confirmada** | Ambos perfiles confirmaron (o una sola confirmación si las publicaciones son de la misma cuenta). La colaboración está activa en ambas publicaciones. |
| **Rechazada** | Uno de los perfiles rechazó la propuesta. No se agregaron colaboradores. |
| **Cancelada** | Una publicación o la imagen dejó de estar disponible. |
| **Disuelta** | La colaboración se disolvió porque un perfil se quitó como colaborador. |

**Reglas y restricciones:**

- El estado general resume la situación de las dos partes.
- Cada parte tiene su propio estado: **Pendiente**, **Confirmada** o **Rechazada**.
- Una propuesta **Confirmada** agrega a cada perfil como colaborador de la publicación del otro.
- Las propuestas cerradas se conservan en el historial.

[CAPTURA: estados-colaboracion-automatica - badges de estado]
