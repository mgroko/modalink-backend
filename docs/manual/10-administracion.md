# 10. Panel de Administración

El Panel de Administración está reservado a usuarios con rol **Administrador**. Se accede iniciando sesión con una cuenta administrativa. El menú lateral muestra las secciones habilitadas para el rol.

## 10.1. Interpretar el Dashboard

**Objetivo:** ver un resumen de la actividad de la plataforma.

**Quién puede hacerlo:** Administrador con permiso para ver usuarios.

**Pasos:**

1. Abrí **Dashboard** en el menú lateral.
2. Revisá las tarjetas y secciones:
   - **Nuevos usuarios** (total de usuarios registrados).
   - **Proyectos confirmados** (total).
   - **Porcentaje confirmado** (sobre proyectos no borrador).
   - **Usuarios**, con **Gestionar usuarios →** y un acceso **Ver** por fila.
   - **Actividad por provincia** y **Top provincias**.
   - **Provincia con más actividad**.
   - **Profesión más demandada**.
   - **Nuevos proyectos**, con **Ver todos →**.

**Resultado esperado:** ves los indicadores y las listas con los datos actuales. Si una sección no tiene datos, muestra un mensaje de vacío.

**Reglas y restricciones:**

- El dashboard no tiene filtros propios.
- Los datos se cargan en paralelo; si falla una sección, el resto sigue visible.
- **Nuevos usuarios** muestra el total de usuarios registrados.
- **Porcentaje confirmado** se calcula sobre proyectos que no están en borrador.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “No se pudo cargar el resumen del dashboard.” | Problema de conexión. | Usá **Reintentar**. |
| “No se pudieron cargar los usuarios.” | Problema de conexión. | Usá **Reintentar**. |
| “No hay usuarios para mostrar.” | No hay usuarios. | — |
| “No hay actividad provincial.” / “No hay datos disponibles.” | No hay datos suficientes. | — |

[CAPTURA: dashboard-admin - indicadores, usuarios, provincias y proyectos]

## 10.2. Gestionar usuarios

**Objetivo:** buscar cuentas, ver su detalle y habilitarlas o deshabilitarlas.

**Quién puede hacerlo:** Administrador con permiso para ver usuarios; habilitar y deshabilitar requieren sus permisos específicos.

**Pasos:**

1. Abrí **Usuarios** en el menú lateral.
2. Buscá por nombre, apellido o correo en **Buscar por nombre, apellido o correo**.
3. Filtrá por **Estado**: **Activo**, **Deshabilitado**, **Pendiente de baja** o **Baja**.
4. Ajustá los resultados con **20 por página**, **50 por página** o **Ver todos**.
5. Para ver el detalle, hacé clic en **Ver perfil**.
6. Para deshabilitar, hacé clic en **Deshabilitar**, completá **Motivo** y elegí **Duración**: **Indefinida** o **Días**.
7. Para habilitar, hacé clic en **Habilitar**.

**Resultado esperado:**

- Al habilitar: “La cuenta de {correo} fue habilitada.”
- Al deshabilitar: “La cuenta de {correo} fue deshabilitada.”

**Reglas y restricciones:**

- El motivo de la deshabilitación es obligatorio y admite hasta 200 caracteres.
- La duración puede ser indefinida o un número de días mayor a 0.
- No podés deshabilitar tu propia cuenta.
- Los usuarios con solicitud de baja no se pueden habilitar ni deshabilitar desde el panel.
- Al deshabilitar, el usuario no puede iniciar sesión y sus perfiles dejan de ser visibles.
- Al habilitar, el usuario recupera el acceso.
- La tabla muestra ID, nombre, apellido, DNI, correo, rol, estado, género, deshabilitación, solicitud de baja y acciones.
- El detalle incluye los perfiles asociados y el detalle de cada perfil.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El motivo es obligatorio.” | No completaste el motivo. | Escribí un motivo. |
| “El motivo no puede superar los 200 caracteres.” | El motivo es demasiado largo. | Acortalo. |
| “Ingresá una duración mayor a 0 días.” | La duración en días es 0 o negativa. | Indicá al menos 1 día. |
| “No podés deshabilitar tu propia cuenta.” | Intentaste deshabilitarte. | Elegí otro usuario. |
| “El usuario solicitó la baja de su cuenta; no puede habilitarse ni deshabilitarse desde el panel administrativo.” | La cuenta está pendiente de baja o de baja. | Gestioná la baja desde el flujo correspondiente. |
| “No se pudo cargar el detalle del usuario.” | Problema de conexión. | Volvé a intentar. |
| “No se pudo habilitar el usuario.” / “No se pudo deshabilitar el usuario.” | Problema de conexión o de permisos. | Volvé a intentar. |

[CAPTURA: admin-usuarios - tabla con filtros y acciones]
[CAPTURA: deshabilitar-usuario - modal con motivo y duración]

## 10.3. Administrar características técnicas

**Objetivo:** mantener el catálogo de características técnicas por profesión.

**Quién puede hacerlo:** Administrador con permisos de ver, crear, modificar o eliminar características.

**Pasos:**

1. Abrí **Características técnicas**.
2. Buscá por nombre, código o profesión.
3. Para crear, hacé clic en **Nueva característica** y completá:
   - **Código** (obligatorio, máximo 50; se guarda en mayúsculas).
   - **Nombre** (máximo 100).
   - **Unidad de medida (opcional)**.
   - **Profesión** (obligatoria).
   - **Tipo de dato**: **ENUMERADO**, **TEXTO** o **NUMERICO**.
4. Si el tipo es **ENUMERADO**, agregá los valores de catálogo con **Agregar valor**, indicando **Código del valor** y, si querés, un color **#RRGGBB**.
5. Hacé clic en **Crear característica**.
6. Para editar, usá **Editar**. Para gestionar valores, usá **Valores**.
7. Para eliminar, usá **Eliminar**.

**Resultado esperado:** la característica aparece en la tabla con su unidad, profesión, tipo, cantidad de valores y estado **En uso** cuando corresponde.

**Reglas y restricciones:**

- El código es obligatorio, único y se guarda en mayúsculas.
- El nombre admite hasta 100 caracteres.
- La profesión es obligatoria.
- Solo las características **ENUMERADO** admiten valores de catálogo.
- Solo las características **NUMERICO** pueden tener unidad de medida.
- Si la característica está **En uso** por perfiles o requerimientos, solo se puede modificar el nombre y no se puede eliminar.
- No se puede cambiar de **ENUMERADO** a otro tipo si tiene valores cargados.
- El color de un valor debe tener formato **#RRGGBB**.
- No se puede eliminar un valor en uso por perfiles.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “Este campo es requerido” | Falta un dato obligatorio. | Completalo. |
| “Máximo 50 caracteres” / “Máximo 100 caracteres” | Un campo supera el límite. | Acortalo. |
| “Ya existe una característica técnica con el código X.” | El código está repetido. | Usá otro código. |
| “La característica posee valores de catálogo; elimínelos antes de cambiar su tipo.” | Intentaste cambiar el tipo con valores cargados. | Eliminá los valores primero. |
| “La característica técnica está en uso por perfiles o requerimientos y no puede eliminarse.” | Está siendo usada. | No la elimines. |
| “La característica técnica está en uso por perfiles o requerimientos; sólo puede modificarse el nombre.” | Intentaste cambiar otro campo. | Solo cambiá el nombre. |
| “El valor está en uso por perfiles y no puede eliminarse.” | El valor está asignado a perfiles. | No lo elimines. |
| “Formato inválido (#RRGGBB)” | El color no respeta el formato. | Corregilo. |

[CAPTURA: admin-caracteristicas - tabla y modal de alta]
[CAPTURA: admin-caracteristica-valores - modal de valores de catálogo]

## 10.4. Administrar habilidades

**Objetivo:** mantener el catálogo global de habilidades profesionales.

**Quién puede hacerlo:** Administrador con permisos de ver, crear, modificar o eliminar habilidades.

**Pasos:**

1. Abrí **Habilidades**.
2. Buscá por nombre, código o descripción, u ordená por columna.
3. Para crear, hacé clic en **Nueva habilidad**, completá **Nombre** y **Descripción** y confirmá con **Crear habilidad**.
4. Para editar, usá **Editar**. El **Código** se muestra solo lectura: se genera automáticamente y no cambia.
5. Para eliminar, usá **Eliminar** y confirmá.

**Resultado esperado:** la habilidad aparece en el catálogo y queda disponible para los perfiles.

**Reglas y restricciones:**

- El nombre es obligatorio y admite hasta 255 caracteres.
- La descripción es opcional y admite hasta 255 caracteres.
- El código se genera automáticamente y no se modifica.
- No se puede repetir el nombre.
- No se puede eliminar una habilidad en uso por requerimientos de proyectos o actividades.
- Al modificar o eliminar una habilidad, los perfiles asociados reciben una notificación.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El nombre es obligatorio.” | Falta el nombre. | Completalo. |
| “El nombre no puede superar los 255 caracteres.” | El nombre es demasiado largo. | Acortalo. |
| “Ya existe una habilidad con ese nombre.” | El nombre está repetido. | Usá otro. |
| “La habilidad está en uso por requerimientos de proyectos o actividades.” | Está siendo usada. | No la elimines. |
| “No tenés permiso para eliminar habilidades.” | Tu rol no lo permite. | Pedile la acción a otro administrador. |

[CAPTURA: admin-habilidades - catálogo y modal de alta/edición]

## 10.5. Administrar unidades de medida

**Objetivo:** mantener las unidades que usan las características técnicas numéricas.

**Quién puede hacerlo:** Administrador con permisos de características.

**Pasos:**

1. Abrí **Unidad de medidas** en el menú lateral.
2. Buscá por nombre o símbolo y filtrá por **Tipo de dato**.
3. Para crear, hacé clic en **Nueva unidad** y completá **Nombre**, **Símbolo** y **Tipo de dato permitido** (**NUMERICO** o **TEXTO**).
4. Para modificar, usá **Modificar**.
5. Para eliminar, usá **Eliminar** y confirmá.

**Resultado esperado:** la unidad aparece en el catálogo y queda disponible para las características.

**Reglas y restricciones:**

- El nombre y el símbolo son obligatorios y admiten hasta 50 caracteres.
- El tipo de dato permitido debe ser **NUMERICO** o **TEXTO**.
- No se puede repetir el nombre ni el símbolo.
- No se puede eliminar una unidad asociada a una característica técnica.
- No se puede eliminar una unidad en uso por características de un tipo incompatible.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “Este campo es requerido” | Falta un dato. | Completalo. |
| “Ya existe una unidad de medida con el nombre X.” | El nombre está repetido. | Usá otro. |
| “Ya existe una unidad de medida con el símbolo X.” | El símbolo está repetido. | Usá otro. |
| “La unidad de medida está asociada a una característica técnica y no puede eliminarse.” | Está en uso. | No la elimines. |
| “tipoDatoPermitido debe ser NUMERICO o TEXTO.” | El tipo no es válido. | Elegí uno de los dos. |

[CAPTURA: admin-unidades - catálogo y modal]

## 10.6. Administrar tipos de contrato

**Objetivo:** mantener las plantillas disponibles para generar contratos.

**Quién puede hacerlo:** Administrador con permisos de tipos de contrato.

**Pasos:**

1. Abrí **Tipos de contrato**.
2. Buscá por nombre, código o descripción.
3. Para crear, hacé clic en **Nuevo tipo** y completá:
   - **Nombre** (obligatorio, máximo 100).
   - **Descripción** (opcional, máximo 200).
   - **Plantilla legal** (obligatoria, máximo 100.000 caracteres).
4. Insertá los placeholders con los botones **{{proyecto}}**, **{{director}}**, **{{partes}}**, **{{clausulas_adicionales}}** y **{{tipo}}**.
5. Usá **Validar plantilla** para comprobar el formato.
6. Confirmá con **Crear tipo**.
7. Para editar, usá **Editar**. Para retirarlo del catálogo, usá **Desactivar**; para volver a habilitarlo, **Activar**.
8. Para eliminarlo, usá **Eliminar**.

**Resultado esperado:** el tipo queda disponible en el catálogo de generación de contratos según su estado.

**Reglas y restricciones:**

- El código se genera automáticamente y no cambia.
- El nombre es obligatorio y único.
- La plantilla es obligatoria y admite hasta 100.000 caracteres.
- La plantilla solo admite los placeholders indicados.
- Si el tipo tiene contratos pendientes o activos, solo se puede modificar la descripción.
- No se puede desactivar un tipo con contratos pendientes de firma.
- No se puede desactivar el último tipo activo.
- No se puede eliminar un tipo con contratos asociados; en ese caso, hay que desactivarlo.
- No se puede eliminar el último tipo activo.
- La validación de plantilla informa placeholders desconocidos o con formato inválido.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El nombre es obligatorio.” | Falta el nombre. | Completalo. |
| “La plantilla es obligatoria.” | Falta la plantilla. | Escribila. |
| “Ya existe un tipo de contrato con el nombre X.” | El nombre está repetido. | Usá otro. |
| “No se puede modificar el nombre ni la plantilla porque el tipo tiene contratos pendientes o activos asociados.” | El tipo está en uso. | Solo cambiá la descripción. |
| “No se puede desactivar el tipo porque tiene contratos pendientes de firma.” | Hay contratos pendientes. | Esperá a que se firmen o rechacen. |
| “No se puede desactivar el último tipo de contrato activo.” | Es el único activo. | Creá o activá otro antes. |
| “El tipo de contrato tiene contratos asociados y no puede eliminarse.” | Tiene contratos. | Desactivalo. |
| “La plantilla del contrato no es válida.” | Hay placeholders inválidos. | Corregí la plantilla. |

[CAPTURA: admin-tipos-contrato - catálogo y editor de plantilla]
[CAPTURA: validar-plantilla - resultado con placeholders]

## 10.7. Configuración del sistema

**Objetivo:** acceder a las tareas automáticas y de mantenimiento.

**Quién puede hacerlo:** Administrador con permiso de administrar configuración.

**Pasos:**

1. Abrí **Configuración del sistema**.
2. Elegí una tarjeta:
   - **Scheduler de deshabilitación**.
   - **Scheduler de bajas**.
   - **Limpieza de imágenes**.
   - **Reparación de cronogramas**.
   - **Colaboraciones automáticas históricas**.

**Resultado esperado:** entrás a la pantalla de configuración de la tarea elegida.

**Reglas y restricciones:** cada tarjeta abre una pantalla independiente. Los cambios se aplican al guardar en cada pantalla.

[CAPTURA: admin-configuracion - tarjetas de configuración]

## 10.8. Configurar el scheduler de deshabilitación

**Objetivo:** definir a qué hora el sistema reactiva las cuentas cuya suspensión temporal venció.

**Quién puede hacerlo:** Administrador con permiso de administrar configuración.

**Pasos:**

1. Abrí **Scheduler de deshabilitación**.
2. Indicá **Hora (0 - 23)** y **Minuto (0 - 59)**.
3. Hacé clic en **Guardar Horario**.
4. Para ejecutar la revisión en ese momento, usá **Ejecutar Ahora** y confirmá.

**Resultado esperado:** ves la **Expresión Cron** y la **Próxima ejecución**. Al guardar, “Horario actualizado con éxito.”; al ejecutar, el resultado de la tarea.

**Reglas y restricciones:**

- La hora debe estar entre 0 y 23.
- El minuto debe estar entre 0 y 59.
- La ejecución manual pide confirmación.
- La próxima ejecución se calcula automáticamente.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “La hora debe estar entre 0 y 23.” | Hora fuera de rango. | Corregila. |
| “El minuto debe estar entre 0 y 59.” | Minuto fuera de rango. | Corregilo. |
| “No se pudo cargar la configuración del scheduler.” | Problema de conexión. | Reintentá. |
| “No se pudieron guardar los cambios.” | Problema de conexión. | Reintentá. |
| “No se pudo ejecutar la tarea manual.” | Problema de conexión. | Reintentá. |

[CAPTURA: scheduler-deshabilitacion - hora, minuto y próxima ejecución]

## 10.9. Configurar el scheduler de bajas

**Objetivo:** definir cuándo y con qué plazo se procesan las cuentas y perfiles pendientes de baja.

**Quién puede hacerlo:** Administrador con permiso de administrar configuración.

**Pasos:**

1. Abrí **Scheduler de bajas**.
2. Indicá **Hora (0 - 23)**, **Minuto (0 - 59)** y **Días de plazo de baja (1 - 365)**.
3. Hacé clic en **Guardar Configuración**.
4. Para ejecutar la expiración en ese momento, usá **Ejecutar Ahora** y confirmá.

**Resultado esperado:** ves la **Expresión Cron**, la **Próxima ejecución** y el **Plazo de baja**. Al ejecutar, se informa cuántos registros se afectaron.

**Reglas y restricciones:**

- La hora debe estar entre 0 y 23.
- El minuto debe estar entre 0 y 59.
- El plazo debe estar entre 1 y 365 días.
- Las cuentas y perfiles **Pendiente de baja** con plazo vencido pasan a **Baja**.
- La ejecución manual pide confirmación.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El plazo debe estar entre 1 y 365 días.” | Plazo fuera de rango. | Corregilo. |
| “La hora debe estar entre 0 y 23.” / “El minuto debe estar entre 0 y 59.” | Valor fuera de rango. | Corregilo. |
| “No se pudo cargar la configuración del scheduler de bajas.” | Problema de conexión. | Reintentá. |
| “No se pudieron guardar los cambios.” | Problema de conexión. | Reintentá. |

[CAPTURA: scheduler-bajas - hora, minuto, plazo y próxima ejecución]

## 10.10. Configurar la limpieza de imágenes

**Objetivo:** definir cada cuántas horas se eliminan archivos e imágenes huérfanas.

**Quién puede hacerlo:** Administrador con permiso de administrar configuración.

**Pasos:**

1. Abrí **Limpieza de imágenes**.
2. Indicá el **Intervalo de limpieza (horas)** entre 1 y 168.
3. Hacé clic en **Guardar cambios**.
4. Para ejecutar la limpieza en ese momento, usá **Ejecutar ahora** y confirmá con **Ejecutar limpieza**.

**Resultado esperado:** ves la **Gracia actual**, la **Próxima ejecución** y la **Última ejecución**. Al ejecutar, se muestra **Resultado de la limpieza** con los conteos.

**Reglas y restricciones:**

- El intervalo debe ser un número entero entre 1 y 168 horas.
- La limpieza manual elimina archivos huérfanos de forma definitiva.
- No se puede ejecutar una limpieza si ya hay otra en curso.
- La gracia actual es solo informativa y no se edita desde esta pantalla.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El intervalo debe ser un número entero entre 1 y 168 horas.” | Valor fuera de rango. | Corregilo. |
| “Ya hay una limpieza de imágenes en curso. Esperá a que termine e intentá nuevamente.” | Hay un proceso activo. | Esperá y reintentá. |
| “No se pudo cargar la configuración del scheduler de limpieza de imágenes.” | Problema de conexión. | Reintentá. |
| “No se pudo ejecutar la limpieza de imágenes.” | Problema de conexión. | Reintentá. |

[CAPTURA: limpieza-imagenes - intervalo, próxima ejecución y resultado]

## 10.11. Reparar cronogramas

**Objetivo:** detectar y corregir actividades con datos inconsistentes.

**Quién puede hacerlo:** Administrador con permiso de administrar configuración.

**Pasos:**

1. Abrí **Reparación de cronogramas**.
2. En **Filtros**, indicá **ID de proyecto**, **ID de actividad** y **Límite de proyectos** (opcional).
3. Hacé clic en **Detectar inconsistencias**.
4. Revisá la tabla de inconsistencias.
5. Hacé clic en **Simular reparación** para ver el resultado sin aplicar cambios.
6. Si la simulación es correcta, hacé clic en **Aplicar reparación** y confirmá.

**Resultado esperado:** ves cuántas inconsistencias se detectaron, cuántas se repararían o se repararon y cuántas quedan pendientes.

**Reglas y restricciones:**

- Solo se mueven actividades **Pendientes**.
- No se mueven actividades **En curso** ni **Finalizadas**.
- La simulación no modifica datos.
- Los cronogramas de los proyectos se actualizan al volver a abrirlos.
- El límite de proyectos no limita la cantidad de filas.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “Los IDs deben ser números mayores a 0 y el límite debe ser al menos 1.” | Filtros inválidos. | Corregilos. |
| “No se detectaron inconsistencias con los filtros actuales.” | Todo está consistente. | — |
| “La solicitud de reparación no es válida. Revisá los filtros.” | Filtros inválidos. | Corregilos. |
| “No se pudo aplicar la reparación.” | Problema de conexión. | Reintentá. |

[CAPTURA: reparacion-cronogramas - filtros, tabla y simulación]

## 10.12. Procesar colaboraciones automáticas históricas

**Objetivo:** procesar publicaciones existentes para completar huellas y detectar coincidencias.

**Quién puede hacerlo:** Administrador con permiso de administrar configuración.

**Pasos:**

1. Abrí **Configuración del sistema** → **Colaboraciones automáticas históricas**.
2. En **Nuevo procesamiento**, elegí:
   - **Modo**: **Huellas + detección (recomendado)**, **Solo completar huellas** o **Solo detectar coincidencias**.
   - **Perfil** y **Publicación** (opcionales).
   - **Desde** y **Hasta**.
   - **Límite de publicaciones** y **Tamaño de lote**.
   - **Enviar notificaciones** (opcional).
3. Si querés, hacé clic en **Previsualizar impacto**.
4. Hacé clic en **Procesar publicaciones** y confirmá.
5. Seguí el progreso en **Procesamientos**.

**Resultado esperado:** se crea un procesamiento en segundo plano. La previsualización muestra publicaciones, imágenes, faltantes e ilegibles, y pares candidatos estimados.

**Reglas y restricciones:**

- El procesamiento no confirma ni rechaza propuestas automáticamente.
- La previsualización no modifica datos.
- El tamaño de lote debe estar entre 1 y 200.
- El límite de publicaciones debe ser mayor a 0.
- **Enviar notificaciones** está desactivado por defecto para evitar avisos masivos.
- No se puede iniciar un procesamiento si ya hay uno en curso.
- Un procesamiento en curso se puede cancelar desde **Procesamientos**.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “Ya hay un procesamiento histórico en curso.” | Hay un job activo. | Esperá o cancelalo. |
| “No hay publicaciones que cumplan los filtros.” | El alcance no devuelve resultados. | Ampliá los filtros. |
| “El tamaño de lote debe estar entre 1 y 200.” | Lote fuera de rango. | Corregilo. |
| “El límite de publicaciones debe ser mayor a 0.” | Límite inválido. | Corregilo. |
| “La fecha desde no puede ser posterior a la fecha hasta.” | Rango invertido. | Corregí las fechas. |

[CAPTURA: colaboraciones-historicas - formulario, impacto estimado e historial]

## 10.13. Consultar el detalle de un procesamiento histórico

**Objetivo:** seguir el avance, revisar datos y errores de un procesamiento.

**Quién puede hacerlo:** Administrador con permiso de administrar configuración.

**Pasos:**

1. Abrí **Colaboraciones automáticas históricas**.
2. En **Procesamientos**, abrí un job.
3. Revisá **Progreso**, **Datos del procesamiento** y **Errores del procesamiento**.
4. Si corresponde, usá **Cancelar procesamiento** y confirmá.

**Resultado esperado:** ves el estado, los contadores y los errores del job.

**Reglas y restricciones:**

- Solo se pueden cancelar procesamientos que aún se puedan cancelar.
- Los errores se listan con su detalle.
- Los contadores se actualizan mientras el job está activo.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El procesamiento no existe o ya no está disponible.” | El job no existe. | Volvé al historial. |
| “El procesamiento ya no se puede cancelar.” | Ya terminó o fue cancelado. | — |

[CAPTURA: procesamiento-historico-detalle - progreso, datos y errores]

## 10.14. Gestionar reportes de perfil

**Objetivo:** revisar los reportes de perfiles y resolverlos o descartarlos.

**Quién puede hacerlo:** Administrador.

**Pasos:**

1. Abrí **Reportes de perfil**.
2. Filtrá por **Estado**, **Motivo**, **Desde** y **Hasta**.
3. Hacé clic en **Ver detalle**.
4. Revisá el motivo, la descripción, el perfil reportado, el usuario reportado y el autor.
5. Usá **Resolver** o **Descartar** y confirmá.

**Resultado esperado:** el reporte pasa a **Resuelto** o **Descartado** y deja de estar pendiente.

**Reglas y restricciones:**

- Solo se puede pasar de **Pendiente** a **Resuelto** o **Descartado**.
- Un reporte ya gestionado no admite nuevos cambios.
- La acción no elimina el perfil ni la publicación.
- El reporte conserva el historial.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El reporte ya fue gestionado y no admite nuevas transiciones.” | Otro administrador lo resolvió. | Actualizá la lista. |
| “Solo se puede pasar de PENDIENTE a RESUELTO o DESCARTADO.” | Estado inválido. | Actualizá el detalle. |
| “Solo un administrador puede gestionar reportes.” | Tu rol no lo permite. | — |
| “No se pudieron cargar los reportes.” | Problema de conexión. | Reintentá. |

[CAPTURA: admin-reportes-perfil - listado con filtros]
[CAPTURA: reporte-perfil-detalle - datos y acciones Resolver y Descartar]

## 10.15. Gestionar reportes de publicaciones

**Objetivo:** revisar reportes de publicaciones y moderar el contenido.

**Quién puede hacerlo:** Administrador.

**Pasos:**

1. Abrí **Reportes de publicaciones**.
2. Filtrá por **Estado**, **Motivo**, **Desde** y **Hasta**.
3. Hacé clic en **Ver detalle**.
4. Revisá el motivo, la publicación, las imágenes y el autor.
5. Para moderar, usá **Ocultar publicación** o **Reactivar publicación**, indicá el **Motivo** de la moderación y confirmá.
6. Para cerrar el reporte, usá **Resolver** o **Descartar**.

**Resultado esperado:** la publicación se oculta o se reactiva, y el reporte queda **Resuelto** o **Descartado**.

**Reglas y restricciones:**

- Ocultar una publicación exige un motivo.
- El motivo admite hasta 200 caracteres.
- La moderación es independiente del estado del reporte.
- Al ocultar, la publicación deja de ser visible para todos, salvo para el dueño en **Publicaciones ocultas**.
- Al reactivar, la publicación vuelve a ser visible.
- El dueño recibe una notificación.
- Un reporte ya gestionado no admite nuevos cambios.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “El motivo es obligatorio al ocultar una publicación.” | No indicaste motivo. | Completalo. |
| “El motivo no puede superar los 200 caracteres.” | El motivo es demasiado largo. | Acortalo. |
| “La publicación ya se encuentra oculta.” | Ya estaba oculta. | Reactivala si corresponde. |
| “La publicación ya se encuentra activa.” | Ya estaba activa. | No hace falta reactivarla. |
| “El reporte ya fue gestionado y no admite nuevas transiciones.” | Otro administrador lo resolvió. | Actualizá la lista. |

[CAPTURA: admin-reportes-publicacion - listado y detalle con moderación]

## 10.16. Consultar el informe de auditoría

**Objetivo:** revisar los eventos críticos del sistema y exportarlos.

**Quién puede hacerlo:** Administrador.

**Pasos:**

1. Abrí **Auditoría**. La pantalla se titula **Informe de auditoría**.
2. En **Filtros**, indicá **Desde** y **Hasta** (por defecto, últimos 7 días).
3. Elegí **Tipos de evento**. Si no elegís ninguno, se incluyen todos.
4. Completá **Operador** (nombre o apellido) y **Texto** (detalle o metadatos) si querés acotar.
5. Hacé clic en **Consultar**.
6. Revisá el resumen: **Eventos encontrados**, **Tipos de evento**, **Rango consultado**, **Por tipo de evento** y **Por entidad**.
7. Revisá **Eventos**, ordenados del más reciente al más antiguo.
8. Para exportar, usá **Exportar CSV** o **Exportar PDF**.

**Resultado esperado:** ves la lista de eventos con **Fecha y hora**, **Evento**, **Entidad**, **Operador**, **IP** y **Detalle**, y podés descargar el informe.

**Reglas y restricciones:**

- El rango máximo de consulta es el configurado en el sistema.
- El rango por defecto es de 7 días.
- Los filtros de operador y texto son opcionales.
- Los tipos de evento se pueden seleccionar por grupo.
- La exportación respeta los filtros aplicados y tiene un tope configurable.
- El registro de auditoría es inmutable.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “No tenés permiso” | Tu rol no permite ver la auditoría. | — |
| “No se pudieron cargar los eventos de auditoría.” | Problema de conexión. | Usá **Reintentar**. |
| “No se pudo cargar el resumen.” | Problema de conexión. | El listado sigue disponible. |
| “No se pudieron cargar los tipos de evento. Podés consultar sin filtro de tipos.” | Falló el catálogo. | Consultá sin tipos. |
| “El rango máximo permitido es de N días. Elegiste N días. Acortá el rango y volvé a consultar.” | Elegiste un rango demasiado amplio. | Acortá el rango. |

[CAPTURA: auditoria - filtros, resumen y tabla de eventos]
[CAPTURA: auditoria-exportar - botones Exportar CSV y Exportar PDF]

## 10.17. Consultar proyectos (administración)

**Objetivo:** ver el listado general de proyectos de la plataforma.

**Quién puede hacerlo:** Administrador con permiso para ver usuarios.

**Pasos:**

1. Desde el Dashboard, hacé clic en **Ver todos →** en la tarjeta **Nuevos proyectos**.
2. Filtrá por estado con **Filtrar por estado**.
3. Usá **Actualizar** para recargar la lista.
4. Navegá con **Anterior** y **Siguiente**.

**Resultado esperado:** ves la tabla con **Proyecto**, **Fecha de inicio**, **Estado** y **Privacidad**.

**Reglas y restricciones:**

- Por defecto se muestran todos los proyectos excepto los borradores.
- Se puede filtrar por **Borrador**, **Publicado**, **Confirmado**, **Finalizado** o **Cancelado**.
- La pantalla es de solo consulta.

**Errores frecuentes:**

| Mensaje | Causa | Solución |
|---|---|---|
| “No hay proyectos para mostrar.” | No hay resultados. | Cambiá el filtro. |
| “No se pudieron cargar los proyectos.” | Problema de conexión. | Usá **Actualizar**. |

[CAPTURA: admin-proyectos - tabla con filtro por estado]
