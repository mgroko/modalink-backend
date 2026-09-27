

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

| Nombre | crearProyecto(nombre, descripcion, privacidad, fechaInicioEstipulada,  ubicacion, requerimientosGral\[\], moodboard, objetivos\[\], fechaFinEstipulada) |
| :---- | :---- |
| **Responsabilidades** | Esta operación cumple la función de crear un proyecto profesional en el sistema con estado Borrador. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-24 |
| **Notas** | \- |
| **Excepciones** | Si hay campos obligatorios vacíos. Si el usuario tiene un proyecto con el mismo nombre en su perfil Si las fechas estipuladas son inválidas (fecha de fin \< fecha de inicio) |
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
| **Notas** | \- |
| **Excepciones** | \- |
| **Salida** | unaPostulacion |
| **Precondición** | El perfil del usuario debe tener al menos la misma profesión que se solicita en el requerimiento. |
| **Postcondición** | Se creó una instancia de Postulacion unaPostulacion Se asignó a unaPostulacion.estadoPostulacion el valor “Pendiente” Se asoció unaPostulacion a unPerfil Se asoció unaPostulacion a unRequerimientoGeneral |

| Nombre | darDeAltaPostulación(unPerfil, unRequerimientoActividad) |
| :---- | :---- |
| **Responsabilidades** | Esta operación da de alta una postulación para un requerimiento de una actividad de un proyecto |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-26 |
| **Notas** | \- |
| **Excepciones** | Si el usuario no tiene disponibilidad frente al rango factible de la actividad. |
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
| **Notas** | El rango factible de la actividad es calculado con el método de la ruta crítica (cpm) |
| **Excepciones** | \- |
| **Salida** | \- |
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

| Nombre | invitarAProyecto(unProyecto, unPerfil, mensaje) |
| :---- | :---- |
| **Responsabilidades** | Se encarga de emitir una invitación de incorporación a un proyecto dirigida a un perfil registrado en el sistema.  |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-31 |
| **Notas** | \- |
| **Excepciones** | El perfil ya tiene una invitación al mismo proyecto |
| **Salida** | unaInvitacion |
| **Precondición** | El sistema conoce unPerfil y unProyecto |
| **Postcondición** | Se creó una instancia unaInvitacion de Invitacion  Se asignó a unaInvitacion.mensaje el valor mensaje Se asignó a unaInvitacion.estado el valor “Pendiente” Se asoció unaInvitacion a unPerfil Se asoció unProyecto a unaInvitacion |

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

| Nombre | aceptarIncorporacion(unaInvitacion, unProyecto) |
| :---- | :---- |
| **Responsabilidades** | Se encarga de aceptar la invitación general de incorporación a un proyecto. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-28 |
| **Notas** | Estados válidos de unProyecto: ‘borrador’, ‘publicado’, ‘confirmado’ |
| **Excepciones** | El usuario ya es participante de ese proyecto |
| **Salida** | \- |
| **Precondición** | unaInvitación está vigente y unProyecto en un estado válido.  |
| **Postcondición** | Se creó una instancia unMiembroDeProyecto de MiembroDeProyecto Se asignó a unMiembroDeProyecto.estadoParticipacion el valor “Activo” Se asoció unPerfil a unMiembroDeProyecto  Se asoció unMiembroDeProyecto a unProyecto Se actualizó unaInvitacion.estado a “Aceptada” |

| Nombre | aceptarIncorporacionAct(unaInvitacion, unProyecto, unRequerimientoAct) |
| :---- | :---- |
| **Responsabilidades** | Se encarga de aceptar la invitación por actividad de incorporación a un proyecto. |
| **Tipo** | Sistema |
| **Referencias cruzadas** | UC-28 |
| **Notas** | Estados válidos de unProyecto: ‘borrador’, ‘publicado’, ‘confirmado’. Estados válidos de unaActividad: ‘pendiente’, ‘en curso’. |
| **Excepciones** | El usuario ya es participante de ese proyecto. El usuario  no tiene disponibilidad suficiente frente al rango factible de la actividad |
| **Salida** | \- |
| **Precondición** | unaInvitación está vigente y unProyecto en un estado válido. La actividad asociada está en un estado válido.  |
| **Postcondición** | Se creó una instancia unMiembroDeProyecto de MiembroDeProyecto Se asignó a unMiembroDeProyecto.estadoParticipacion el valor “Activo” Se asoció unPerfil a unMiembroDeProyecto  Se asoció unMiembroDeProyecto a unProyecto Se actualizó unaInvitacion.estado a “Aceptada” Se asignó unaActividad a unMiembroDeProyecto |

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
| **Precondición** | La postulación está en estado “Pendiente” |
| **Postcondición** | Se actualizó unaPostulacion.estado a “Rechazada” |

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

