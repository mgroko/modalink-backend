
## Contrato de acceso por rol global

| Rol | Área permitida |
|---|---|
| `Administrador` | `/admin/**`, `/auth/me`, `/auth/logout`, `/notificaciones/**`, `GET /profesiones`, `GET /ubicaciones/**` |
| `Usuario` | `/home/**`, `/perfiles/**`, `/proyectos/**`, `/postulaciones/**`, `/invitaciones/**`, `/calendario/**`, `/usuario/**`, `/habilidades/**`, `/auth/me`, `/auth/logout`, `/notificaciones/**`, `GET /profesiones`, `GET /ubicaciones/**` |

Errores:

| HTTP | Código | Situación |
|---|---|---|
| `401` | `NO_AUTENTICADO` | Sin sesión o token inválido |
| `403` | `ROL_NO_PERMITIDO` | El rol no corresponde al área solicitada |
| `403` | `CSRF_INVALIDO` | Token CSRF ausente o inválido |
| `409` | `SESION_ACTIVA` | Registro con una sesión ya autenticada |

El administrador no posee perfil creativo ni proyectos propios. El login de un
administrador no incluye `idPerfilActivo` ni `nombreArtisticoActivo`.

| Nombre | crearActividad(unProyecto, nombre, descripción, duracion) |
| :---- | :---- |
| **Responsabilidades** | Crear una actividad a partir de los datos ingresados por el usuario |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-49 |
| **Notas** | \- |
| **Excepciones** | Si el usuario deja campos obligatorios vacíos, el nombre es igual al de otra actividad de la planificación del proyecto o la duración es menor a cero. |
| **Salida** | unaActividad |
| **Precondición** | El actor tiene permisos de director de proyecto y el proyecto debe estar en un estado válido (borrador, publicado, confirmado) |
| **Postcondición** | Se creó una instancia unaActividad de Actividad con estado “Pendiente”. |

| Nombre | agregarPredecesora(unaActividad, unaPredecesora) |
| :---- | :---- |
| **Responsabilidades** | Crear una relación de precedencia entre unaActividad y unaPredecesora. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-49 |
| **Notas** | \- |
| **Excepciones** | \- |
| **Salida** | predecesoras\[\] |
| **Precondición** | Se debe seleccionar una actividad predecesora válida. |
| **Postcondición** | Se agregó una actividad predecesora a la colección de predecesoras de unaActividad |

| Nombre | buscarUbicacion(idUbicacion) |
| :---- | :---- |
| **Responsabilidades** | Buscar una ubicación en base al criterio de búsqueda |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-49 |
| **Notas** | \- |
| **Excepciones** | Si el idUbicacion es inválido, el sistema lanza una excepción |
| **Salida** | unaUbicacion |
| **Precondición** | \- |
| **Postcondición** | \- |

| Nombre | agregarUbicacion(unaActividad, unaUbicacion) |
| :---- | :---- |
| **Responsabilidades** | Asociar una ubicación a una actividad |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-49 |
| **Notas** | \- |
| **Excepciones** | \- |
| **Salida** | \- |
| **Precondición** | unaActividad y unaUbicacion deben existir |
| **Postcondición** | Se asoció unaUbicacion a unaActividad |

| Nombre | agregarRequerimiento(unaActividad, unaProfesion, caracteristicas\[\], habilidades\[\]) |
| :---- | :---- |
| **Responsabilidades** | Crear una instancia de requerimiento asociado a una actividad. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-49 |
| **Notas** | La colección de características y habilidades puede estar vacía. |
| **Excepciones** | \- |
| **Salida** | \- |
| **Precondición** | La actividad y profesión deben existir.  |
| **Postcondición** | Se creó una instancia de requerimientoActividad Se asoció un requerimientoActividad con unaActividad |

| Nombre | eliminarActividad(unaActividad, motivo) |
| :---- | :---- |
| **Responsabilidades** | Esta operación se encarga de eliminar una actividad de la planificación de un proyecto |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-51 |
| **Notas** | \- |
| **Excepciones** | No se puede eliminar una actividad que se encuentre en estado “Finalizada”. |
| **Salida** | \- |
| **Precondición** | La actividad debe existir y estar en estado “Pendiente” o “En curso”. |
| **Postcondición** | Se eliminó unaActividad Se actualizó unaPlanificacion de unProyecto Se generó unaNotificación por cada miembro del proyecto informando el motivo. |

| Nombre | guardarMiembro(unPerfil, unaActividad) |
| :---- | :---- |
| **Responsabilidades** | Esta operación se encarga de quitar la relación entre el miembro del proyecto y la actividad por eliminar, manteniéndolo dentro del proyecto pero sin esa actividad asignada.  |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-51 |
| **Notas** | Si el miembro tiene otras actividades asignadas, las mismas permanecerán asignadas al mismo. Sólo se rompe la relación con unaActividad.  |
| **Excepciones** | \- |
| **Salida** | \- |
| **Precondición** | El sistema conoce unaActividad. El sistema conoce unPerfil. |
| **Postcondición** | Se eliminó la asignación de unPerfil a unaActividad. |

| Nombre | modificarActividad(unaActividad, nombre, descripcion, duracionEstimada,  requerimiento\[\], predecesoras\[\], ubicacion, estado) |
| :---- | :---- |
| **Responsabilidades** | Modificar la información de una actividad. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-50 |
| **Notas** | \- |
| **Excepciones** | Existen campos vacíos o la duración es menor a cero. |
| **Salida** | unaActividad |
| **Precondición** | unaActividad debe existir |
| **Postcondición** |  |

| Nombre | crearProyecto(nombre, descripcion, privacidad,  ubicacion, requerimientosGral\[\], moodboard, objetivos\[\], fechaEntregaEstipulada) |
| :---- | :---- |
| **Responsabilidades** | Esta operación cumple la función de crear un proyecto profesional en el sistema con estado Borrador. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-24 |
| **Notas** | \- |
| **Excepciones** | Si hay campos obligatorios vacíos. Si el usuario tiene un proyecto con el mismo nombre en su perfil Si la fecha de entrega estipulada es inválida (fecha de entrega \< fecha actual) |
| **Salida** | unProyecto |
| **Precondición** | \- |
| **Postcondición** | Se creó una instancia de Proyecto unProyecto Se asignó a unProyecto.estado el valor “Borrador” Se asoció unProyecto a unPerfil Se creó una instancia de Planificación unaPlanificación Se asoció unaPlanificacion a unProyecto |

| Nombre | publicarProyecto(unProyecto) |
| :---- | :---- |
| **Responsabilidades** | Cambiar el estado de un proyecto de “Borrador” a “Publicado”, haciéndolo visible según su privacidad y aceptando postulaciones. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-25 |
| **Notas** | \- |
| **Excepciones** | El proyecto tiene información básica incompleta |
| **Salida** | unProyecto |
| **Precondición** | unProyecto debe existir |
| **Postcondición** | Se asignó a unProyecto.estado el valor “Publicado” |

| Nombre | darDeAltaPostulación(unPerfil, unRequerimientoGeneral) |
| :---- | :---- |
| **Responsabilidades** | Esta operación da de alta una postulación para un requerimiento general de proyecto |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-26 |
| **Notas** | El proyecto debe estar en estado “Publicado” (lo cierra UC-60) y aceptar postulaciones generales. Un perfil no puede postularse dos veces al mismo requerimiento. Se notifica a todos los directores activos del proyecto. Si el requerimiento ya alcanzó su cantidad de postulaciones aceptadas, no admite nuevas. La elegibilidad de lectura por requerimiento se expone como `puedePostularse` en la respuesta del proyecto; es sólo una ayuda de UI y no reemplaza estas validaciones de escritura. |
| **Excepciones** | Si el proyecto no está “Publicado”, no acepta postulaciones generales o el requerimiento ya está satisfecho. Si el perfil ya es miembro activo del proyecto, no tiene la profesión solicitada o ya se postuló a ese requerimiento. |
| **Salida** | unaPostulacion |
| **Precondición** | El perfil del usuario debe tener al menos la misma profesión que se solicita en el requerimiento. |
| **Postcondición** | Se creó una instancia de Postulacion unaPostulacion Se asignó a unaPostulacion.estadoPostulacion el valor “Pendiente” Se asoció unaPostulacion a unPerfil Se asoció unaPostulacion a unRequerimientoGeneral |

| Nombre | darDeAltaPostulación(unPerfil, unRequerimientoActividad) |
| :---- | :---- |
| **Responsabilidades** | Esta operación da de alta una postulación para un requerimiento de una actividad de un proyecto |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-26 |
| **Notas** | El proyecto debe estar en estado “Publicado” (lo cierra UC-60). Sólo se admite postular a actividades “Pendiente”. Un perfil no puede postularse dos veces al mismo requerimiento. Se notifica a todos los directores activos del proyecto. La disponibilidad se verifica de forma **provisional**: mientras el proyecto no tenga fecha de inicio confirmada, el rango evaluado es [ahora, fecha de entrega]; al confirmarse el proyecto (UC-60) y al asignarse la actividad (UC-70) debe repetirse la verificación contra el rango factible [ES, LF] calculado por CPM. La elegibilidad de lectura por requerimiento se expone como `puedePostularse` en la búsqueda de actividades; no incluye duplicados ni disponibilidad y no reemplaza estas validaciones de escritura. |
| **Excepciones** | Si el usuario no tiene disponibilidad frente al rango factible de la actividad, la actividad no está “Pendiente”, el proyecto no está “Publicado”, el perfil no tiene la profesión solicitada, ya es miembro activo o ya se postuló a ese requerimiento. |
| **Salida** | unaPostulacion |
| **Precondición** | El perfil del usuario debe tener al menos la misma profesión que se solicita en el requerimiento. |
| **Postcondición** | Se creó una instancia de Postulacion unaPostulacion Se asignó a unaPostulacion.estadoPostulacion el valor “Pendiente” Se asoció unaPostulacion a unPerfil Se asoció unaPostulacion a unRequerimientoActividad |

| Nombre | buscarUsuario(unPerfil) |
| :---- | :---- |
| **Responsabilidades** | Esta operación tiene la responsabilidad de buscar el usuario asociado a un perfil |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-26, UC-70, UC-28 |
| **Notas** | \- |
| **Excepciones** | \- |
| **Salida** | unUsuario |
| **Precondición** | El sistema debe conocer unPerfil |
| **Postcondición** | \- |

| Nombre | verificarDisponibilidad(unUsuario, unaActividad) |
| :---- | :---- |
| **Responsabilidades** | Esta operación tiene la función de verificar la disponibilidad de un usuario frente al rango factible de la actividad  |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-26, UC-70, UC-28 |
| **Notas** | El rango factible de la actividad es calculado con el método de la ruta crítica (cpm). La regla es que exista un hueco libre continuo de al menos la duración de la actividad dentro del rango, descontando la jornada laboral, los bloqueos manuales y las actividades ya asignadas (con su margen). El resultado indica si la verificación es **provisional**: si el proyecto todavía no tiene fecha de inicio, el rango evaluado es [ahora, fecha de entrega] y debe repetirse al confirmar el proyecto (UC-60) y al asignar la actividad (UC-70) contra el rango [ES, LF]. |
| **Excepciones** | Si el usuario no existe o no está activo; si la actividad no tiene planificación asociada. |
| **Salida** | unResultadoDisponibilidad (disponible, provisional, inicio y fin del rango evaluado) |
| **Precondición** | unUsuario y unaActividad existen. |
| **Postcondición** | \- |

| Nombre | asignarActividad(unPerfil, unaActividad) |
| :---- | :---- |
| **Responsabilidades** | Esta operación tiene la función de asignar una actividad a un miembro del proyecto. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-70 |
| **Notas** | \- |
| **Excepciones** | Si el usuario no tiene disponibilidad frente al rango factible de la actividad. |
| **Salida** | unaAsignacionActividad  |
| **Precondición** | unPerfil y unaActividad existen |
| **Postcondición** | Se asignó unPerfil a unaActividad |

| Nombre | cancelarProyecto(unProyecto, motivoCancelacion)  |
| :---- | :---- |
| **Responsabilidades** | Esta operación se encarga de cambiar el estado de un proyecto a “Cancelado”.  |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-35 |
| **Notas** | \- |
| **Excepciones** | \- |
| **Salida** | \- |
| **Precondición** | El proyecto existe en el sistema y se encuentra en estado “Publicado” o “Confirmado” |
| **Postcondición** | Se actualizó unProyecto.estado a “Cancelado” Por cada instancia unaActividad asociada a unProyecto, se actualiza unaActividad.estado a “Cancelada” |

| Nombre | finalizarProyecto(unProyecto) |
| :---- | :---- |
| **Responsabilidades** | Esta operación se encarga de dar por finalizado un proyecto en estado “confirmado” |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-37 |
| **Notas** | \- |
| **Excepciones** | El director no calificó al equipo completo. |
| **Salida** | unProyecto |
| **Precondición** | Todas las actividades de la planificación de unProyecto deben estar en estado “finalizada”. El proyecto debe estar en estado confirmado. |
| **Postcondición** | Se actualizó unProyecto.estado a “Finalizado”. Se indexó unProyecto a HistorialProyecto de cada participante. |

| Nombre | calificarMiembro(unPerfil, calificacion, resenia) |
| :---- | :---- |
| **Responsabilidades** | Calificar a un miembro del proyecto con una calificación y una reseña. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-37 |
| **Notas** | \- |
| **Excepciones** | La calificación está vacía. |
| **Salida** | unaResenia |
| **Precondición** | unPerfil es conocido por el sistema y participó del proyecto. |
| **Postcondición** | Se creó una instancia unaResenia de Resenia. Se asoció unaResenia a unPerfil Se asignó a unaResenia.calificacion el valor calificacion Se asignó a unaResenia.descripcion el valor resenia |

| Nombre | confirmarProyecto(unProyecto) |
| :---- | :---- |
| **Responsabilidades** | Esta operación se encarga de actualizar el estado de un proyecto a “Confirmado” |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-60 |
| **Notas** | Requerimiento completo hace referencia a que el personal requerido esté completo para el desarrollo de actividades (o requerimiento general) del proyecto |
| **Excepciones** | El proyecto no tiene requerimiento completo. |
| **Salida** | unProyecto |
| **Precondición** | Todos los requerimientos están completos. El proyecto está en estado “Publicado”. |
| **Postcondición** | Se actualizó unProyecto.estado a “Confirmado” Cada instancia de unRequerimientoGeneral asociado a unProyecto deja de recibir postulaciones Cada instancia de unRequerimientoActividad asociado a unProyecto deja de recibir postulaciones |

| Nombre | buscarInvitaciones(idProyecto, nombreProyecto, fechaDesde, fechaHasta, perfil, direccion, estado, tipo) |
| :---- | :---- |
| **Responsabilidades** | Buscar las invitaciones recibidas o enviadas por el perfil activo, unificando invitaciones generales y de actividad. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-30 |
| **Notas** | Todos los filtros son opcionales. `direccion` puede ser `RECIBIDA` o `ENVIADA`; si se omite, se incluyen ambas. El filtro `perfil` se aplica a la contraparte; sin dirección, excluye el propio perfil activo. El alcance está limitado al perfil activo. |
| **Excepciones** | Perfil activo no seleccionado o rango de fechas inválido. |
| **Salida** | invitaciones[] paginadas |
| **Precondición** | El usuario inició sesión y tiene un perfil activo. |
| **Postcondición** | No se modifican datos. |

| Nombre | darDeBajaInvitacion(unaInvitacion) |
| :---- | :---- |
| **Responsabilidades** | Eliminar físicamente una invitación a proyecto que todavía está pendiente. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-32 |
| **Notas** | Sólo puede ejecutarla el Director que envió la invitación, con permiso `ELIMINAR_INVITACION` y rol Director activo en el proyecto. |
| **Excepciones** | La invitación no existe, pertenece a otro proyecto, no está en estado `PENDIENTE` o el actor no es el remitente. |
| **Salida** | \- |
| **Precondición** | La invitación está en estado `PENDIENTE`, pertenece al proyecto indicado y el perfil activo es su remitente. |
| **Postcondición** | Se eliminó la instancia de Invitacion. No se crea una notificación. |

| Nombre | invitarAProyecto(unProyecto, unPerfil, unRequerimientoGralOpcional, mensaje) |
| :---- | :---- |
| **Responsabilidades** | Se encarga de emitir una invitación de incorporación a un proyecto dirigida a un perfil registrado en el sistema. Puede asociarse opcionalmente a un requerimiento general. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-31 |
| **Notas** | Si se informa un requerimiento general, se valida pertenencia al proyecto, cupo y profesión del perfil. Si no se informa, la invitación es general al proyecto. |
| **Excepciones** | El perfil ya tiene una invitación al mismo proyecto; requerimiento satisfecho o profesión incompatible si se asocia un requerimiento general. |
| **Salida** | unaInvitacion |
| **Precondición** | El sistema conoce unPerfil y unProyecto |
| **Postcondición** | Se creó una instancia unaInvitacion de Invitacion  Se asignó a unaInvitacion.mensaje el valor mensaje Se asignó a unaInvitacion.estado el valor “Pendiente” Se asoció unaInvitacion a unPerfil Se asoció unProyecto a unaInvitacion Si corresponde, se asoció unRequerimientoGral a unaInvitacion |

| Nombre | buscarRequerimientosActividad(idProyecto, profesion, caracteristicas[], unaActividad) |
| :---- | :---- |
| **Responsabilidades** | Buscar los requerimientos de actividad de un proyecto que coincidan con los filtros indicados. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-73 |
| **Notas** | El proyecto es el criterio mínimo obligatorio. Los demás filtros son opcionales. |
| **Excepciones** | Perfil activo inexistente, proyecto inexistente o sin visibilidad. |
| **Salida** | requerimientosActividad[] |
| **Precondición** | El sistema conoce el proyecto y el actor tiene un perfil activo. |
| **Postcondición** | No se modifican datos. |

| Nombre | invitarAProyectoActividad(unProyecto, unRequerimientoAct, unPerfil, mensaje) |
| :---- | :---- |
| **Responsabilidades** | Se encarga de emitir una invitación de incorporación a un proyecto asociada a una actividad del mismo. La invitación está dirigida a un perfil registrado en el sistema.  |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-31 |
| **Notas** | \- |
| **Excepciones** | El perfil ya tiene una invitación al mismo proyecto |
| **Salida** | unaInvitacion |
| **Precondición** | El sistema conoce unPerfil y unProyecto. El perfil tiene la profesión requerida en el requerimiento de actividad. La actividad existe en el sistema y tiene un estado válido. |
| **Postcondición** | Se creó una instancia unaInvitacion de Invitacion  Se asignó a unaInvitacion.mensaje el valor mensaje Se asignó a unaInvitacion.estado el valor “Pendiente” Se asoció unPerfil a unaInvitacion  Se asoció unProyecto a unaInvitacion Se asoció unRequerimientoAct a unaInvitacion |

| Nombre | invitarAProyectoMultiple(unProyecto, unRequerimientoGralOpcional, perfiles[], mensaje) |
| :---- | :---- |
| **Responsabilidades** | Emitir una invitación general por cada perfil seleccionado, permitiendo éxito parcial. Puede asociarse opcionalmente a un requerimiento general. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-31 |
| **Notas** | Los IDs repetidos se deduplican. Cada perfil se procesa en una transacción independiente. |
| **Excepciones** | Errores por perfil: invitación duplicada, perfil inactivo, miembro activo, autoinvitación o perfil inexistente. Los errores comunes de proyecto o permisos se propagan. |
| **Salida** | creadas[] y errores[] |
| **Precondición** | El Director activo administra el proyecto y el proyecto está Publicado o Confirmado. |
| **Postcondición** | Se creó una instancia de Invitacion por cada perfil válido. Se notificó a cada destinatario. Los perfiles con error no generaron invitación. |

| Nombre | invitarAProyectoActividadMultiple(unProyecto, unRequerimientoAct, perfiles[], mensaje) |
| :---- | :---- |
| **Responsabilidades** | Emitir una invitación por actividad por cada perfil seleccionado, permitiendo éxito parcial. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-31 |
| **Notas** | Los IDs repetidos se deduplican. Cada perfil se procesa en una transacción independiente. |
| **Excepciones** | Errores por perfil: invitación duplicada, perfil inactivo, profesión incompatible, miembro activo, autoinvitación o perfil inexistente. |
| **Salida** | creadas[] y errores[] |
| **Precondición** | El Director activo administra el proyecto, el requerimiento pertenece al proyecto y la actividad está Pendiente o En curso. |
| **Postcondición** | Se creó una instancia de Invitacion por cada perfil válido. Se notificó a cada destinatario. Los perfiles con error no generaron invitación. |

> **Nota de implementación (UC-31):** la invitación general se asocia
> directamente al proyecto mediante `invitacion_gral.id_proyecto`, sin exigir
> un requerimiento general. `id_requerimiento_gral` queda opcional para
> registros históricos o invitaciones asociadas a un requerimiento. La
> invitación por actividad conserva la asociación al requerimiento y deriva el
> proyecto desde la actividad. Ambas nacen en estado `PENDIENTE` y generan una
> notificación `INVITACION_RECIBIDA` para el perfil destinatario.

| Nombre | aceptarIncorporacion(unaInvitacion, unProyecto) |
| :---- | :---- |
| **Responsabilidades** | Se encarga de aceptar la invitación general de incorporación a un proyecto. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-28 |
| **Notas** | Estados válidos de unProyecto: `PUBLICADO` o `CONFIRMADO`. Si la invitación tiene requerimiento general, se valida cupo y profesión. Si no, la membresía queda sin requerimiento. |
| **Excepciones** | El usuario ya es participante de ese proyecto; invitación no pendiente; proyecto cancelado/finalizado; requerimiento satisfecho; profesión incompatible. |
| **Salida** | \- |
| **Precondición** | unaInvitación está vigente, dirigida al perfil activo, y unProyecto en un estado válido.  |
| **Postcondición** | Se creó una instancia unMiembroDeProyecto de MiembroDeProyecto Se asignó a unMiembroDeProyecto.estadoParticipacion el valor “Activo” Se asoció unPerfil a unMiembroDeProyecto  Se asoció unMiembroDeProyecto a unProyecto Se actualizó unaInvitacion.estado a “Aceptada” Si la invitación tenía requerimiento general, se asoció el requerimiento a la membresía y, si el cupo quedó cubierto, se eliminaron las invitaciones pendientes del mismo requerimiento. |

| Nombre | aceptarIncorporacionAct(unaInvitacion, unProyecto, unRequerimientoAct) |
| :---- | :---- |
| **Responsabilidades** | Se encarga de aceptar la invitación por actividad de incorporación a un proyecto. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-28 |
| **Notas** | Estados válidos de unProyecto: `PUBLICADO` o `CONFIRMADO`. Estados válidos de unaActividad: `PENDIENTE` o `EN_CURSO`. Si el perfil ya es miembro activo, se reutiliza la membresía. |
| **Excepciones** | El usuario es Director del proyecto; el usuario no tiene disponibilidad suficiente frente al rango factible de la actividad; requerimiento satisfecho; actividad inválida; invitación no pendiente. |
| **Salida** | \- |
| **Precondición** | unaInvitación está vigente y dirigida al perfil activo; unProyecto en un estado válido; la actividad asociada está en un estado válido.  |
| **Postcondición** | Se creó o reutilizó unMiembroDeProyecto con estado “Activo” Se asoció unPerfil a unMiembroDeProyecto Se asoció unMiembroDeProyecto a unProyecto Se actualizó unaInvitacion.estado a “Aceptada” Se asignó unaActividad a unMiembroDeProyecto Si el cupo quedó cubierto, se eliminaron las invitaciones pendientes del mismo requerimiento. |

| Nombre | rechazarInvitacion(unaInvitacion) |
| :---- | :---- |
| **Responsabilidades** | Rechazar una invitación general o por actividad dirigida al perfil activo. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-28 |
| **Notas** | No crea membresía ni asignación. |
| **Excepciones** | La invitación no existe, no pertenece al perfil activo, no está pendiente o el proyecto no está Publicado/Confirmado. |
| **Salida** | \- |
| **Precondición** | La invitación está vigente y dirigida al perfil activo. |
| **Postcondición** | Se actualizó unaInvitacion.estado a “Rechazada”. Se notificó a los directores del proyecto. No se creó membresía ni asignación. |

| Nombre | aceptarPostulacion(unaPostulacion) |
| :---- | :---- |
| **Responsabilidades** | Esta operación tiene la responsabilidad de aceptar la postulación de un perfil en el sistema e incorporarlo al proyecto. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-34 |
| **Notas** | “Si corresponde” en la postcondición hace referencia a postulaciones que se realicen en un requerimiento de actividad. Pueden existir dos tipos de postulaciones: generales y actividades. |
| **Excepciones** | El usuario es participante del proyecto |
| **Salida** | \- |
| **Precondición** | La postulación está en estado “Pendiente” |
| **Postcondición** | Se creó una instancia unMiembroDeProyecto de MiembroDeProyecto Se asignó a unMiembroDeProyecto.estadoParticipacion el valor “Activo” Se asoció unPerfil a unMiembroDeProyecto  Se asoció unMiembroDeProyecto a unProyecto Se actualizó unaPostulacion.estado a “Aceptada” *\*Si corresponde:*  Se asignó unaActividad a unMiembroDeProyecto |

| Nombre | rechazarPostulacion(unaPostulacion) |
| :---- | :---- |
| **Responsabilidades** | Esta operación tiene la responsabilidad de rechazar la postulación de un perfil en el sistema. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-34 |
| **Notas** | \- |
| **Excepciones** | El usuario es participante del proyecto |
| **Salida** | \- |
| **Precondición** | La postulación está en estado "Pendiente" |
| **Postcondición** | Se actualizó unaPostulacion.estado a "Rechazada" |

> **Nota de implementación (UC-34):** el rechazo es lógico: la postulación pasa
> a `RECHAZADA` y no se borra físicamente. Un perfil rechazado no puede volver a
> postularse al mismo requerimiento; la reincorporación futura se realizará por
> invitación (UC-31). La aceptación general crea una membresía activa vinculada
> al requerimiento; la de actividad revalida disponibilidad y crea una
> `AsignacionActividad`. El cupo se mide por miembros/asignaciones activos, no
> por postulaciones aceptadas. El auto-rechazo de las demás pendientes al
> completar el cupo lo realiza la base de datos.

| Nombre | darseDeBaja(unProyecto, motivo) |
| :---- | :---- |
| **Responsabilidades** | Esta operación se encarga de la baja voluntaria de un participante de un proyecto. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-40 |
| **Notas** | \*Si el usuario que se dió de baja tenía actividades asignadas. ‘actividades\[\]’ hace referencia a la colección de actividades. |
| **Excepciones** | Si el motivo está vacío. |
| **Salida** | \- |
| **Precondición** | El perfil es miembro activo del proyecto. |
| **Postcondición** | Se actualizó unMiembroDeProyecto.estadoParticipacion a “BajaVoluntaria”. Se asignó unMiembroDeProyecto.motivoBaja el valor motivo *\*Si corresponde:*  Se desvinculó unMiembroDeProyecto de actividades\[\] |

> **Nota de implementación (UC-40):** la baja es lógica, igual que UC-38. La
> membresía pasa a `BAJA_VOLUNTARIA` y conserva `motivoBaja` y `fechaBaja`. Las
> asignaciones a actividades no se borran físicamente: se conservan como
> historial y dejan de contar como recursos activos. La vacante se reabre por
> cálculo de cupo. El motivo es obligatorio y tiene un máximo de 100 caracteres.
> Un Director puede darse de baja solo si queda otro Director activo. Se
> notifica a los directores activos restantes con el tipo
> `INTEGRANTE_BAJA_VOLUNTARIA`.

| Nombre | eliminarIntegrante(unProyecto, unPerfil, motivo) |
| :---- | :---- |
| **Responsabilidades** | Esta operación se encarga de dar de baja a un miembro de un proyecto. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-38 |
| **Notas** | \*Si el usuario que se eliminó tenía actividades asignadas. ‘actividades\[\]’ hace referencia a la colección de actividades. Un estado de participación inválido es: ‘Eliminado’, ‘BajaVoluntaria’. |
| **Excepciones** | Si el motivo está vacío o si el perfil tiene un estado de participación inválido. |
| **Salida** | \- |
| **Precondición** | El perfil es miembro activo del proyecto. |
| **Postcondición** | Se actualizó unMiembroDeProyecto.estadoParticipacion a “Eliminado”. Se asignó unMiembroDeProyecto.motivoBaja el valor motivo *\*Si corresponde:*  Se desvinculó unMiembroDeProyecto de actividades\[\] |

| Nombre | darDeBajaPostulación(unaPostulacion) |
| :---- | :---- |
| **Responsabilidades** | Esta operación da de baja una postulación emitida. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-27 |
| **Notas** | \- |
| **Excepciones** | Si la postulación no está en estado ‘Pendiente’. |
| **Salida** | \- |
| **Precondición** | La postulación debe estar en estado ‘Pendiente’ |
| **Postcondición** | Se eliminó una instancia de Postulacion unaPostulacion |

| Nombre | configurarTerminosCondiciones(unPerfil, texto) |
| :---- | :---- |
| **Responsabilidades** | Registrar una nueva versión de los términos y condiciones del perfil. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-23 |
| **Notas** | El texto debe tener entre 1 y 5000 caracteres. Si el texto es igual a la versión vigente, no se crea una versión nueva. |
| **Excepciones** | Perfil inexistente o ajeno, perfil no activo, texto vacío o mayor a 5000 caracteres. |
| **Salida** | TerminosCondicionesResponse |
| **Precondición** | El usuario está autenticado, el perfil existe y está en estado Activo. |
| **Postcondición** | Se cerró la versión vigente anterior y se creó una nueva versión vigente asociada al perfil. |

| Nombre | aceptarTerminosDePerfil(unPerfil, idTyc, aceptaTerminos) |
| :---- | :---- |
| **Responsabilidades** | Validar que el proyecto acepte la versión vigente de los términos y condiciones del perfil que se incorpora. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-23, UC-28, UC-31, UC-34, UC-38 bis |
| **Notas** | Si el perfil no tiene T&C configurados, no se exige aceptación y el id queda nulo. |
| **Excepciones** | Falta la aceptación, la versión enviada no es la vigente o el perfil ya no tiene versión vigente. |
| **Salida** | idTycAceptado |
| **Precondición** | El perfil destinatario existe y está activo. |
| **Postcondición** | Se registró la versión aceptada en la invitación o en la membresía del proyecto. |

---

## UC-67 — Generar informe de auditoría

| Nombre | listarAuditoria(fechaDesde, fechaHasta, tipos, page, size) |
| :---- | :---- |
| **Responsabilidades** | Listar los eventos críticos de auditoría en un rango de fechas, con filtros opcionales por tipo de evento. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-67 |
| **Notas** | Solo rol Administrador. Las fechas son obligatorias e inclusivas. Si no se envían tipos, se devuelven todos los tipos activos. El rango máximo se configura en `AUDITORIA_RANGO_MAX_DIAS` (default 90). Tamaño de página máximo: 100. Orden: `fechaHora DESC, idAuditoria DESC`. |
| **Excepciones** | Fechas ausentes (`FECHAS_OBLIGATORIAS`), rango invertido (`RANGO_FECHAS_INVALIDO`), rango excedido (`RANGO_FECHAS_EXCEDIDO`), tipo inexistente (`TIPO_EVENTO_INVALIDO`), paginación inválida (`PAGINACION_INVALIDA`). |
| **Salida** | `PaginaResponse<AuditoriaEventoResponse>` con `idAuditoria`, `fechaHora`, `tipoEvento`, `tipoEventoNombre`, `entidadAfectada`, `entidadClave`, `idAfectado`, `idUsuario`, `usuarioNombre`, `usuarioBd`, `ipUsuario` y `detalle[]`. |
| **Precondición** | El usuario está autenticado con rol Administrador. |
| **Postcondición** | No modifica datos. Devuelve la página solicitada o una página vacía si no hay coincidencias. |

| Nombre | listarTiposEventoAuditoria() |
| :---- | :---- |
| **Responsabilidades** | Devolver el catálogo de tipos de evento activos para poblar el filtro multi-selección. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-67 |
| **Notas** | Solo rol Administrador. |
| **Excepciones** | Ninguna esperable. |
| **Salida** | Lista de `TipoEventoAuditoriaResponse` (`codigo`, `nombre`, `descripcion`, `modulo`). |
| **Precondición** | El usuario está autenticado con rol Administrador. |
| **Postcondición** | No modifica datos. |
