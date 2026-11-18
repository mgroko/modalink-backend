# 12. Contratos

Los contratos permiten acordar por escrito las condiciones de un proyecto y dejar registrada la firma de cada parte.

## 12.1. Buscar contratos

**Objetivo:** encontrar los contratos de tus proyectos y revisar su estado.

**Quién puede hacerlo:** usuario con perfil activo. El Director ve todos los contratos del proyecto; cada parte ve los suyos.

**Pasos:**

1. Abrí **Contratos** en la barra superior.
2. Completá los filtros:
   - **Contrato**: nombre del contrato.
   - **Proyecto**: **Todos mis proyectos** o uno en particular.
   - **Parte**: nombre o apellido.
   - **Resultados**: 20 o 50 resultados.
   - Chips de estado: **Pendiente de firma**, **Activo**, **Rechazado por las partes**, **Cancelado**, **Eliminado**.
   - **Solo pendientes de mi firma**.
3. Para más filtros, usá **Más filtros**: **Emisión** y **Firma** con **Desde** y **Hasta**, y el estado **Borrador**.
4. Hacé clic en **Buscar**.

**Resultado esperado:** ves la tabla con **Contrato**, **Tipo**, **Estado**, **Emisión**, **Firmas**, **Mi firma** y **Acciones**.

**Reglas y restricciones:**

- Por defecto se muestran los estados **Pendiente de firma**, **Activo**, **Rechazado por las partes**, **Cancelado** y **Eliminado**.
- El estado **Borrador** solo se ve si lo seleccionás en **Estados avanzados**.
- Los contratos eliminados se incluyen por defecto y se marcan como **Eliminado**.
- Si no seleccionás estados, se aplican los predeterminados.
- La fecha **Desde** no puede ser posterior a **Hasta**.
- **Limpiar filtros** restablece la búsqueda.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “La fecha desde no puede ser posterior a la fecha hasta.” | Rango invertido. | Corregí las fechas. |
| “No tenés permiso para consultar estos contratos.” | Tu perfil no tiene visibilidad. | Pedí acceso al Director. |
| “El perfil no se encuentra activo.” | Tu perfil está dado de baja. | Elegí otro perfil. |
| “No se encontraron contratos.” | No hay resultados. | Ampliá los filtros. |
| “Ocurrió un error al buscar contratos. Inténtalo nuevamente.” | Problema de conexión. | Usá **Reintentar**. |

[CAPTURA: contratos - filtros, chips de estado y tabla]

## 12.2. Consultar los contratos pendientes de mi firma

**Objetivo:** ver solo los contratos donde tu firma sigue pendiente.

**Quién puede hacerlo:** usuario con perfil activo.

**Pasos:**

1. Abrí **Contratos** y hacé clic en **Pendientes de mi firma**.
2. Revisá la lista.
3. Para firmar o rechazar, abrí el detalle del contrato.

**Resultado esperado:** ves únicamente los contratos con tu firma pendiente.

**Reglas y restricciones:**

- El filtro **Solo pendientes de mi firma** queda aplicado de forma fija.
- La firma y el rechazo se realizan desde el detalle.
- Si el contrato deja de estar pendiente, desaparece de esta bandeja.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “No tenés contratos pendientes de firma.” | No hay contratos para firmar. | — |
| “No encontramos contratos pendientes con los filtros seleccionados.” | Los filtros no devuelven resultados. | Limpiá los filtros. |

[CAPTURA: contratos-pendientes - bandeja de pendientes de mi firma]

## 12.3. Consultar el detalle de un contrato

**Objetivo:** leer el texto, revisar las partes y gestionar tu firma.

**Quién puede hacerlo:** Director del proyecto o parte firmante.

**Pasos:**

1. En **Contratos**, hacé clic en el nombre del contrato o en **Ver**.
2. Revisá **Datos del contrato**, **Texto del contrato** y **Partes y firmas**.
3. Si te corresponde firmar, marcá **Leí el contrato completo y estoy de acuerdo con su contenido.**
4. Hacé clic en **Firmar contrato**.
5. Si querés rechazarlo, hacé clic en **Rechazar contrato**, escribí el **Motivo del rechazo** y confirmá.

**Resultado esperado:**

- Al firmar: “Tu firma quedó registrada y el contrato pasó a Activo.” o “Tu firma quedó registrada.”
- Al rechazar: “El rechazo quedó registrado y el contrato fue notificado.”

**Reglas y restricciones:**

- Solo pueden firmar los miembros activos con firma **Pendiente**.
- El contrato debe estar **Pendiente de firma** y el proyecto **Publicado** o **Confirmado**.
- Para firmar hay que aceptar el texto de la versión vigente.
- El contrato pasa a **Activo** cuando todas las partes activas firmaron.
- El rechazo exige un motivo de hasta 200 caracteres.
- Al rechazar, el contrato pasa a **Rechazado por las partes** y las firmas pendientes se cancelan.
- Un contrato **Eliminado** o de un proyecto **Finalizado** o **Cancelado** se conserva como registro histórico y no admite acciones.
- Podés ver el detalle de cada parte, su rol y el estado de su firma.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El contrato ya no está pendiente de firma.” | Otra parte lo firmó o rechazó. | Actualizá el detalle. |
| “Tu firma ya no está pendiente para este contrato.” | Ya firmaste. | — |
| “El contrato ya está eliminado.” | El contrato fue eliminado. | — |
| “Indicá un motivo para rechazar el contrato.” | No escribiste el motivo. | Completalo. |
| “El motivo no puede superar los 200 caracteres.” | El motivo es demasiado largo. | Acortalo. |
| “No tenés permiso para ver este contrato.” | No sos parte ni Director. | Pedí acceso. |

[CAPTURA: detalle-contrato - datos, texto, partes y bloque Tu firma]

## 12.4. Generar un contrato

**Objetivo:** crear un contrato a partir de una plantilla y enviarlo a las partes.

**Quién puede hacerlo:** Director activo del proyecto, en proyectos **Publicados** o **Confirmados** con al menos un miembro activo.

**Pasos:**

1. Abrí el proyecto y entrá a **Contratos**.
2. Hacé clic en **Nuevo contrato**.
3. En el paso **Datos**, elegí el **Tipo de acuerdo**.
4. En **Destinatarios (miembros activos)**, seleccioná una o varias personas. Podés usar **Seleccionar todos** o **Deseleccionar todos**.
5. Hacé clic en **Continuar al borrador**.
6. En el paso **Borrador**, revisá el texto generado.
7. Si querés, agregá **Cláusulas adicionales (opcional)** con **Agregar cláusula**.
8. Hacé clic en **Emitir contrato**.

**Resultado esperado:** ves **Contrato emitido** con el tipo, el estado, la versión, la emisión y las partes. Cada parte recibe una notificación.

**Reglas y restricciones:**

- El proyecto debe tener al menos un miembro activo.
- El tipo de acuerdo debe estar activo.
- Los destinatarios deben ser miembros activos del proyecto.
- No se puede generar un contrato para alguien que ya tiene un contrato **Activo** del mismo tipo en el proyecto.
- La previsualización no guarda el contrato.
- El contrato se emite en estado **Pendiente de firma**.
- El Director queda registrado como firmante al emitir.
- Cada cláusula adicional admite hasta 1.000 caracteres.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El proyecto no tiene miembros activos.” | No hay destinatarios posibles. | Sumá miembros al proyecto. |
| “Alguno de los destinatarios ya no es un miembro activo del proyecto.” | El miembro fue dado de baja. | Actualizá la selección. |
| “Algún destinatario ya tiene un contrato activo de este tipo en el proyecto.” | Hay un contrato vigente. | Elegí otro tipo o destinatario. |
| “El tipo de acuerdo no existe o no está activo.” | El tipo fue desactivado. | Elegí otro tipo. |
| “Escribí una cláusula antes de agregarla.” | La cláusula está vacía. | Escribila. |
| “La cláusula no puede superar los 1000 caracteres.” | La cláusula es demasiado larga. | Acortala. |
| “No se pudo generar el contrato.” | Problema de conexión. | Volvé a intentar. |

[CAPTURA: generar-contrato-datos - tipo de acuerdo y destinatarios]
[CAPTURA: generar-contrato-borrador - texto y cláusulas adicionales]
[CAPTURA: contrato-emitido - resumen y partes]

## 12.5. Modificar un contrato

**Objetivo:** corregir el tipo, los destinatarios, el nombre, la descripción o el texto antes de que se firme.

**Quién puede hacerlo:** Director activo del proyecto, con el contrato **Pendiente de firma**, el proyecto **Publicado** o **Confirmado** y sin firmas registradas.

**Pasos:**

1. Abrí el contrato y hacé clic en **Modificar**.
2. Cambiá el **Tipo de acuerdo**, los **Destinatarios**, el **Nombre** o la **Descripción**.
3. Elegí el modo de texto:
   - **Editar texto final**: modificá el texto directamente.
   - **Regenerar desde plantilla**: agregá cláusulas y regenerá el texto.
4. Hacé clic en **Previsualizar cambios**.
5. Si no hay conflictos, hacé clic en **Guardar cambios** y confirmá.

**Resultado esperado:** ves “Contrato modificado” y “Los cambios quedaron guardados y las partes fueron notificadas.” Se crea una nueva versión.

**Reglas y restricciones:**

- Solo el Director activo puede modificar.
- El contrato debe estar **Pendiente de firma**.
- No se puede modificar si alguna parte ya firmó o rechazó.
- El proyecto debe estar **Publicado** o **Confirmado**.
- Hay que previsualizar antes de guardar.
- Los destinatarios deben seguir siendo miembros activos y no tener un contrato activo del mismo tipo.
- El nombre admite hasta 50 caracteres; la descripción, hasta 200; el texto final, hasta 100.000.
- En el modo **Editar texto final** no se aplican las cláusulas adicionales.
- Al guardar se reconcilian las firmas y se notifica a las partes actuales.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “Solo el Director activo del proyecto puede modificar este contrato.” | Tu rol no lo permite. | Pedile la acción al Director. |
| “El contrato ya no está pendiente de firma.” | Ya fue firmado o rechazado. | No se puede modificar. |
| “Alguna parte ya firmó o rechazó el contrato.” | Hay una firma registrada. | No se puede modificar. |
| “El proyecto no está Publicado o Confirmado.” | El proyecto cambió de estado. | No se puede modificar. |
| “Estos destinatarios actuales ya no están activos en el proyecto y no se conservarán.” | Hay miembros inactivos. | Revisá la selección. |
| “Estos destinatarios actuales ya tienen un contrato activo de este tipo y no se conservarán.” | Hay conflicto de contrato activo. | Revisá la selección. |
| “El texto final no puede estar vacío.” | El texto quedó vacío. | Escribí el texto. |
| “Seleccioná al menos un destinatario.” | No hay destinatarios. | Elegí uno. |

[CAPTURA: modificar-contrato - datos, texto y previsualización]

## 12.6. Eliminar un contrato

**Objetivo:** dar de baja un contrato conservando su historial.

**Quién puede hacerlo:** Director activo del proyecto, salvo que el proyecto esté **Finalizado** o **Cancelado**.

**Pasos:**

1. Abrí el contrato y hacé clic en **Eliminar**. También podés hacerlo desde el listado.
2. Leé las advertencias.
3. Confirmá con **Eliminar contrato**.

**Resultado esperado:** ves “Contrato eliminado” y el contrato queda en estado **Eliminado**. Sigue visible en la búsqueda.

**Reglas y restricciones:**

- El contrato no se borra físicamente: queda como registro histórico.
- Se conservan el historial, las versiones, las firmas y los eventos.
- Las firmas pendientes pasan a **Cancelada**.
- Las firmas ya registradas se conservan.
- Se notifica a las partes, excepto si el contrato estaba en **Borrador**.
- El contrato deja de aparecer en la búsqueda por defecto solo si se excluye el estado **Eliminado**.
- La acción no se puede deshacer desde la interfaz.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “No tenés permiso para eliminar este contrato.” | Tu rol no lo permite. | Pedile la acción al Director. |
| “El contrato ya está eliminado.” | Ya fue eliminado. | — |
| “No se puede eliminar un contrato de un proyecto finalizado o cancelado.” | El proyecto está finalizado o cancelado. | No se puede eliminar. |

[CAPTURA: eliminar-contrato - advertencias y confirmación]

## 12.7. Entender los estados del contrato

**Objetivo:** saber qué significa cada estado y qué acciones permite.

**Quién puede hacerlo:** cualquier usuario con acceso al contrato.

| Estado | Significado | Acciones habituales |
|---|---|---|
| **Borrador** | El contrato se está preparando y todavía no se emitió. | Visible solo con el filtro avanzado. |
| **Pendiente de firma** | Fue emitido y espera las firmas. | Firmar, rechazar, modificar o eliminar. |
| **Activo** | Todas las partes activas firmaron. | Consultar. |
| **Rechazado por las partes** | Alguien rechazó el contrato. | Consultar el motivo. |
| **Cancelado** | El contrato fue cancelado. | Consultar. |
| **Eliminado** | Se dio de baja y se conserva como historial. | Consultar. |

**Reglas y restricciones:**

- El contrato pasa a **Activo** solo cuando todas las partes activas firmaron.
- Las firmas **Cancelada** no bloquean la activación.
- Un contrato **Eliminado** no se puede volver a firmar.
- Los contratos de proyectos **Finalizados** o **Cancelados** se conservan como registro histórico.

[CAPTURA: contrato-estados - badges de estado en el listado]
