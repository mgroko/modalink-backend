# Diagramas de secuencia de diseño

> **Alcance y criterio de lectura**
>
> Estos diagramas se elaboraron a partir de la estructura actual del backend y de
> `docs/3_diseño/ModaLinkBD.sql`.
>
> - **UC-24** refleja el flujo implementado actualmente.
> - **UC-26, UC-28 y UC-34** reflejan el diseño de los casos de uso a partir de
>   sus DSS, contratos y entidades persistentes. Los nombres de las capas de
>   aplicación se expresan según la responsabilidad que deben cumplir en el
>   diseño, independientemente de su estado de implementación.
> - La disponibilidad se calcula en backend mediante la agenda, bloqueos y
>   actividades existentes; no se trata como un dato persistido estático.

## UC-24 — Crear proyecto

**Implementación actual:** `POST /proyectos`, atendido por
`ProyectoController.crear` y `CrearProyectoService.crear`.

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant API as ProyectoController
    participant Servicio as CrearProyectoService
    participant PerfilRepo as PerfilRepository
    participant RolRepo as RolProyectoRepository
    participant ProyectoRepo as ProyectoRepository
    participant Ubicacion as UbicacionService
    participant Proyecto as Proyecto
    participant MiembroRepo as MiembroProyectoRepository
    participant PlanRepo as PlanificacionRepository
    participant ObjetivoRepo as ObjetivoRepository
    participant ReqGral as RequerimientoGralService
    participant MoodRepo as MoodboardRepository
    participant Mapper as ProyectoMapper

    Usuario->>API: POST /proyectos(request)
    API->>API: Extraer idUsuario e idPerfilActivo de Authentication

    alt No hay perfil activo seleccionado
        API-->>Usuario: Error PerfilActivoNoSeleccionado
    else Perfil activo disponible
        API->>Servicio: crear(idUsuario, idPerfilActivo, request)
        Servicio->>PerfilRepo: findByIdPerfilAndUsuarioIdUsuario(...)
        PerfilRepo-->>Servicio: Perfil

        alt Perfil inexistente o no activo
            Servicio-->>API: PerfilNoEncontrado / PerfilEnBaja
            API-->>Usuario: Error de validación
        else Perfil válido
            Servicio->>Servicio: Validar fecha de entrega
            Servicio->>RolRepo: findByNombre("Director")
            RolRepo-->>Servicio: RolProyecto Director
            Servicio->>ProyectoRepo: existeProyectoConNombreParaPerfil(...)

            alt Nombre duplicado para el perfil
                ProyectoRepo-->>Servicio: true
                Servicio-->>API: NombreProyectoDuplicado
                API-->>Usuario: Error de validación
            else Nombre disponible
                opt Ubicación recibida
                    Servicio->>Ubicacion: obtenerOCrear(localidadId, provinciaId)
                    Ubicacion-->>Servicio: Ubicacion
                end

                Servicio->>Proyecto: Construir proyecto
                Note right of Proyecto: estado = Borrador
                Servicio->>ProyectoRepo: save(proyecto)
                ProyectoRepo-->>Servicio: Proyecto guardado

                Servicio->>MiembroRepo: save(MiembroProyecto)
                Note right of MiembroRepo: perfil activo + rol Director + estado Activo

                Servicio->>PlanRepo: save(Planificacion 1:1)

                opt Objetivos recibidos
                    loop Por cada objetivo
                        Servicio->>ObjetivoRepo: save(Objetivo)
                    end
                end

                opt Requerimientos generales recibidos
                    loop Por cada requerimiento
                        Servicio->>ReqGral: persistir(proyecto, profesión, cantidad, datos)
                        ReqGral-->>Servicio: RequerimientoGralResponse
                    end
                end

                opt Moodboard recibido
                    Servicio->>MoodRepo: save(Moodboard)
                    MoodRepo-->>Servicio: Moodboard guardado
                end

                Servicio->>Mapper: toResponse(proyecto, director, datos, contexto)
                Mapper-->>Servicio: ProyectoResponse
                Servicio-->>API: ProyectoResponse
                API-->>Usuario: 201 Created + ProyectoResponse
            end
        end
    end
```

### Persistencia relevante de UC-24

La transacción crea o actualiza las siguientes relaciones:

1. `proyecto`, con estado `BORRADOR`.
2. `miembros_proyecto`, asociando el perfil activo al rol `DIRECTOR`.
3. `planificacion`, en relación 1:1 con el proyecto.
4. `objetivo`, si se enviaron objetivos.
5. `requerimiento_gral_proyecto` y sus características/habilidades, si se
   enviaron requerimientos.
6. `moodboard`, si se envió material de inspiración.

Los campos obligatorios del request son `nombre`, `descripcion` y `privacidad`.
La validación de Bean Validation ocurre antes de ingresar al servicio.

## UC-26 — Dar de alta postulación a proyecto

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant API as PostulacionController
    participant Servicio as CrearPostulacionService
    participant PerfilRepo as PerfilRepository
    participant ReqActividad as RequerimientoActividadService
    participant ReqGeneral as RequerimientoGralService
    participant Calendario as CalendarioService
    participant PostActRepo as PostulacionActividadRepository
    participant PostGralRepo as PostulacionGralRepository
    participant Notif as NotificacionService
    participant DirectorRepo as MiembroProyectoRepository

    Usuario->>API: Seleccionar requerimiento y solicitar postulación
    API->>Servicio: darDeAltaPostulación(unPerfil, unRequerimiento)
    Servicio->>PerfilRepo: Validar perfil activo y profesión
    PerfilRepo-->>Servicio: Perfil activo

    alt Postulación a requerimiento de actividad
        Servicio->>ReqActividad: buscarRequerimientoActividad(idRequerimiento)
        ReqActividad-->>Servicio: Requerimiento + actividad
        Servicio->>Calendario: verificarDisponibilidad(unUsuario, unaActividad)
        Calendario-->>Servicio: Resultado de disponibilidad

        alt Usuario sin disponibilidad
            Servicio-->>API: excepcion()
            API-->>Usuario: Notificar error y finalizar
        else Usuario disponible frente al rango factible
            Servicio->>PostActRepo: save(PostulacionActividad[PENDIENTE])
            PostActRepo-->>Servicio: unaPostulacion
        end
    else Postulación a requerimiento general
        Servicio->>ReqGeneral: buscarRequerimientoGeneral(idRequerimiento)
        ReqGeneral-->>Servicio: Requerimiento general
        Note right of Servicio: No se evalúa disponibilidad
        Servicio->>PostGralRepo: save(PostulacionGral[PENDIENTE])
        PostGralRepo-->>Servicio: unaPostulacion
    end

    Servicio->>DirectorRepo: Buscar directores activos del proyecto
    DirectorRepo-->>Servicio: Perfiles directores
    Servicio->>Notif: crear(notificación a cada director)
    Notif-->>Servicio: Solicitud de notificación
    Servicio-->>API: unaPostulacion
    API-->>Usuario: Postulación registrada
```

La postulación debe quedar inicialmente en estado `PENDIENTE`, asociada al
perfil y al requerimiento correspondiente. Se notifica a **todos** los
directores activos del proyecto con el tipo `POSTULACION_RECIBIDA`; la
notificación debe referenciar la postulación mediante `entidad_tipo` y
`entidad_id`.

Sólo los proyectos en estado `PUBLICADO` aceptan postulaciones (UC-60 los
cierra), el perfil debe estar activo, tener la profesión solicitada y no ser
miembro activo del proyecto. Un perfil no puede postularse dos veces al mismo
requerimiento (garantizado además por los índices únicos
`UQ_perfil_postulacion_act` / `UQ_perfil_postulacion_gral`). Sólo se admite
postular a actividades `PENDIENTE`.

La verificación de disponibilidad de UC-26 es **provisional**: mientras el
proyecto no tenga fecha de inicio confirmada se evalúa el rango
`[ahora, fecha de entrega]` en lugar del rango estricto `[ES, LF]` del CPM, y
la respuesta lo informa. La comprobación debe repetirse al confirmar el
proyecto (UC-60) y al asignar la actividad (UC-70).

Endpoints:

- `POST /proyectos/{idProyecto}/postulaciones/actividad` con
  `{ "idRequerimientoActividad": ... }`.
- `POST /proyectos/{idProyecto}/postulaciones/general` con
  `{ "idRequerimientoGral": ... }`.

## UC-28 — Aceptar solicitud de incorporación

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant API as InvitacionController
    participant Servicio as AceptarInvitacionService
    participant InvitacionRepo as InvitacionRepository
    participant ProyectoService as ObtenerProyectoService
    participant ReqService as RequerimientoService
    participant Calendario as CalendarioService
    participant MiembroRepo as MiembroProyectoRepository
    participant RolRepo as RolProyectoRepository
    participant AsignacionRepo as AsignacionActividadRepository
    participant Notif as NotificacionService

    Usuario->>API: Seleccionar invitación
    API->>Servicio: obtenerInvitacion(idInvitacion)
    Servicio->>InvitacionRepo: Buscar invitación vigente
    InvitacionRepo-->>Servicio: Invitación

    Servicio->>ProyectoService: obtenerProyecto(unaInvitacion)
    ProyectoService-->>Servicio: Proyecto

    alt Invitación no válida, requerimiento satisfecho o proyecto cancelado
        Servicio-->>API: InvitaciónNoValida
        API-->>Usuario: Notificar error y finalizar
    else Invitación válida
        Note right of Servicio: Proyecto en estado Borrador, Publicado o Confirmado
        Servicio-->>API: solicitarConfirmacion()
        API-->>Usuario: Información de proyecto e invitación

        alt Invitación para actividad
            Servicio->>ReqService: Buscar requerimiento de actividad
            ReqService-->>Servicio: Requerimiento + rango factible
            Servicio->>Calendario: Consultar disponibilidad(idUsuario, rango)
            Calendario-->>Servicio: Disponibilidad calculada

            alt Disponibilidad insuficiente
                Servicio-->>API: Error de disponibilidad
                API-->>Usuario: Notificar error y finalizar
            else Disponibilidad suficiente y requerimiento vigente
                Usuario->>API: confirmarIncorporacion(unaInvitacion)
                API->>Servicio: aceptarIncorporacionAct(unaInvitacion, unProyecto, unRequerimientoAct)
                Servicio->>MiembroRepo: Verificar que el usuario no sea participante

                alt Usuario ya participante
                    Servicio-->>API: excepcion()
                    API-->>Usuario: Notificar error y finalizar
                else Usuario no es participante
                    Servicio->>RolRepo: Buscar rol de miembro
                    RolRepo-->>Servicio: Rol de proyecto
                    Servicio->>MiembroRepo: save(MiembroProyecto[ACTIVO])
                    Servicio->>AsignacionRepo: save(AsignacionActividad)
                    Servicio->>InvitacionRepo: Actualizar invitación a ACEPTADA
                    Servicio->>Notif: notificarDirector(unProyecto)
                    Notif-->>Servicio: Solicitud de notificación
                    Servicio-->>API: Incorporación registrada
                    API-->>Usuario: 200 OK
                end
            end
        else Invitación general
            Usuario->>API: confirmarIncorporacion(unaInvitacion)
            API->>Servicio: aceptarIncorporacion(unaInvitacion, unProyecto)
            Servicio->>MiembroRepo: Verificar que el usuario no sea participante

            alt Usuario ya participante
                Servicio-->>API: excepcion()
                API-->>Usuario: Notificar error y finalizar
            else Usuario no es participante
                Servicio->>RolRepo: Buscar rol de miembro
                RolRepo-->>Servicio: Rol de proyecto
                Servicio->>MiembroRepo: save(MiembroProyecto[ACTIVO])
                Servicio->>InvitacionRepo: Actualizar invitación a ACEPTADA
                Servicio->>Notif: notificarDirector(unProyecto)
                Notif-->>Servicio: Solicitud de notificación
                Servicio-->>API: Incorporación registrada
                API-->>Usuario: 200 OK
            end
        end
    end
```

La rama de actividad agrega la validación de disponibilidad y la asignación de
la actividad al miembro. Si el perfil ya es miembro activo, se reutiliza la
membresía; si es Director, se rechaza. La rama general no consulta
disponibilidad porque no asigna una actividad concreta y rechaza la operación
si el perfil ya es miembro activo.

La invitación general puede estar asociada opcionalmente a un requerimiento
general. En ese caso se valida profesión y cupo, y la membresía se crea vinculada
al requerimiento. Si no tiene requerimiento, la membresía queda sin vínculo y no
consume cupo.

Cuando una aceptación cubre el cupo del requerimiento, se eliminan físicamente
todas las invitaciones pendientes de ese mismo requerimiento, de cualquier
director, sin notificar a los afectados. La operación notifica a los directores
con `INVITACION_ACEPTADA`.

El rechazo de una invitación se expone con los endpoints:

```http
POST /invitaciones/general/{idInvitacion}/rechazar
POST /invitaciones/actividad/{idInvitacion}/rechazar
```

Actualiza la invitación a `RECHAZADA`, no crea membresía ni asignación y notifica
a los directores con `INVITACION_RECHAZADA`. Los endpoints de aceptación son:

```http
POST /invitaciones/general/{idInvitacion}/aceptar
POST /invitaciones/actividad/{idInvitacion}/aceptar
```

## UC-34 — Gestionar postulaciones

```mermaid
sequenceDiagram
    autonumber
    actor Director as Director de proyecto
    participant API as PostulacionController
    participant Servicio as GestionarPostulacionService
    participant ProyectoService as ProyectoSecurityService
    participant PostActRepo as PostulacionActividadRepository
    participant PostGralRepo as PostulacionGralRepository
    participant PostRepo as PostulacionRepository
    participant MiembroRepo as MiembroProyectoRepository
    participant AsignacionRepo as AsignacionActividadRepository
    participant ReqRepo as RequerimientoRepository
    participant Notif as NotificacionService

    Director->>API: Seleccionar proyecto
    API->>Servicio: obtenerProyecto(idProyecto)
    Servicio->>ProyectoService: Validar director y proyecto PUBLICADO
    ProyectoService-->>Servicio: Acceso autorizado
    Director->>API: obtenerPostulaciones(unProyecto)
    API->>Servicio: listarPostulaciones(unProyecto)
    Servicio->>PostActRepo: findPendientesDeProyectoConPerfil(idProyecto)
    PostActRepo-->>Servicio: Postulaciones de actividad
    Servicio->>PostGralRepo: findPendientesDeProyectoConPerfil(idProyecto)
    PostGralRepo-->>Servicio: Postulaciones generales
    Servicio-->>API: Postulaciones + acciones
    API-->>Director: Mostrar Aceptar / Rechazar

    Director->>API: Seleccionar acción(postulación)
    API->>Servicio: gestionar(idPostulacion, acción, confirmación)
    Servicio->>ProyectoService: Revalidar permisos del director
    ProyectoService-->>Servicio: Acceso autorizado

    alt Aceptar postulación
        Servicio-->>API: solicitarConfirmacion()
        API-->>Director: Confirmación
        Director->>API: confirmarAceptacion(unaPostulacion)
        API->>Servicio: aceptarPostulacion(unaPostulacion)
        Servicio->>PostRepo: Validar estado PENDIENTE
        PostRepo-->>Servicio: Postulación válida
        Servicio->>MiembroRepo: Verificar que no sea participante

        alt Perfil ya es participante
            Servicio-->>API: excepcion()
            API-->>Director: Notificar error
        else Perfil no es participante
            Servicio->>MiembroRepo: save(MiembroProyecto[ACTIVO])
            Servicio->>PostRepo: Actualizar estado a ACEPTADA
            opt Postulación asociada a actividad
                Servicio->>AsignacionRepo: save(AsignacionActividad)
            end
            Servicio->>ReqRepo: actualizarRequerimientosPersonal(unProyecto)
            Servicio->>Notif: notificarPostulante(unaPostulacion)
            Notif-->>Servicio: Solicitud de notificación
            Servicio-->>API: Incorporación registrada
            API-->>Director: 200 OK
        end
    else Rechazar postulación
        Servicio-->>API: solicitarConfirmacion()
        API-->>Director: Confirmación
        Director->>API: confirmarRechazo(unaPostulacion)
        API->>Servicio: rechazarPostulacion(unaPostulacion)
        Servicio->>PostRepo: Validar estado PENDIENTE
        PostRepo-->>Servicio: Postulación válida
        Servicio->>PostRepo: Actualizar estado a RECHAZADA
        Servicio->>Notif: notificarPostulanteRechazado(unaPostulacion)
        Notif-->>Servicio: Solicitud de notificación
        Servicio-->>API: Postulación rechazada
        API-->>Director: 200 OK
    end
```

### Reglas de persistencia y consistencia

- La aceptación debe crear un registro `miembros_proyecto` activo y actualizar
  el estado de la postulación en una única transacción.
- El estado de la postulación utiliza los valores del modelo:
  `PENDIENTE`, `ACEPTADA` o `RECHAZADA`.
- La base de datos ya contiene triggers para controlar cupos al aceptar
  postulaciones. El servicio debe propagar los errores de integridad y no
  devolver una aceptación exitosa si la transacción falla.
- El rechazo debe conservar la postulación con estado `RECHAZADA` para que
  pueda auditarse y generar la notificación correspondiente; el contrato de
  `rechazarPostulacion` no indica eliminarla.

## UC-30 — Buscar invitaciones

La búsqueda se limita a las invitaciones recibidas o enviadas por el perfil
activo. Las invitaciones generales y de actividad se consultan por separado y
se unifican, filtran, ordenan y paginan en el servicio.

```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Usuario / Director
    participant API as InvitacionController
    participant Servicio as BuscarInvitacionService
    participant InvGralRepo as InvitacionGralRepository
    participant InvActRepo as InvitacionActividadRepository

    Usuario->>API: GET /invitaciones/buscar?filtros
    API->>Servicio: buscar(idPerfilActivo, filtros, page, size, todos)
    Servicio->>InvGralRepo: buscarPorPerfil(idPerfilActivo)
    InvGralRepo-->>Servicio: invitaciones generales
    Servicio->>InvActRepo: buscarPorPerfil(idPerfilActivo)
    InvActRepo-->>Servicio: invitaciones de actividad

    alt Sin coincidencias
        Servicio-->>API: página vacía
    else Con coincidencias
        Servicio->>Servicio: unificar, filtrar, ordenar y paginar
        Servicio-->>API: invitaciones[]
    end

    API-->>Usuario: 200 OK
```

La respuesta incluye `tipo` para distinguir invitaciones generales de las de
actividad y `direccion` para indicar si el perfil activo es remitente o
destinatario. El filtro `nombrePerfil` se aplica al remitente en las recibidas,
al destinatario en las enviadas y a la contraparte cuando no se informa
dirección.

## UC-31 — Invitar a proyecto

El caso de uso contempla una invitación general al proyecto y una invitación
asociada a un requerimiento de actividad. Ambas se almacenan con estado
`PENDIENTE`. La invitación por actividad además conserva la referencia al
requerimiento seleccionado.

```mermaid
sequenceDiagram
    autonumber
    actor Director as Director de proyecto
    participant API as InvitacionController
    participant Servicio as CrearInvitacionService
    participant ProyectoService as ObtenerProyectoService
    participant PerfilService as ObtenerPerfilService
    participant ReqService as RequerimientoActividadService
    participant InvGralRepo as InvitacionGralRepository
    participant InvActRepo as InvitacionActividadRepository
    participant Notif as NotificacionService

    Director->>API: Seleccionar proyecto
    API->>ProyectoService: obtenerProyecto(idProyecto)
    ProyectoService-->>API: unProyecto
    API-->>Director: Información básica y opciones de invitación

    alt Invitación general
        Director->>API: Seleccionar invitación general
        API-->>Director: Solicitar perfil destinatario
        Director->>API: Seleccionar perfil
        API->>PerfilService: obtenerPerfil(idPerfil)
        PerfilService-->>API: unPerfil
        API-->>Director: Mostrar información del perfil
        Director->>API: Introducir mensaje opcional y confirmar
        API->>Servicio: invitarAProyecto(unProyecto, unPerfil, mensaje)
        Servicio->>InvGralRepo: Buscar invitación activa para proyecto y perfil

        alt El perfil ya tiene invitación activa
            InvGralRepo-->>Servicio: Invitación existente
            Servicio-->>API: excepcion()
            API-->>Director: Notificar duplicación
        else No existe invitación activa
            Servicio->>InvGralRepo: save(InvitacionGral[PENDIENTE])
            InvGralRepo-->>Servicio: unaInvitacion
            Servicio->>Notif: notificarPerfil(unPerfil, unaInvitacion)
            Notif-->>Servicio: Solicitud de notificación
            Servicio-->>API: invitacionRegistrada(unaInvitacion)
            API-->>Director: Invitación registrada
        end
    else Invitación por actividad
        Director->>API: Seleccionar invitación por actividad
        API-->>Director: Solicitar requerimiento de actividad
        Director->>API: Seleccionar requerimiento
        API->>ReqService: buscarRequerimientoActividad(idRequerimientoAct)
        ReqService-->>API: unRequerimientoAct
        API-->>Director: Información del requerimiento
        Director->>API: Seleccionar perfil
        API->>PerfilService: obtenerPerfil(idPerfil)
        PerfilService-->>API: unPerfil
        API-->>Director: Mostrar información del perfil
        Director->>API: Introducir mensaje opcional y confirmar
        API->>Servicio: invitarAProyectoActividad(unProyecto, unRequerimientoAct, unPerfil, mensaje)
        Servicio->>Servicio: Validar profesión del perfil y estado de la actividad
        Servicio->>InvActRepo: Buscar invitación activa para proyecto y perfil

        alt El perfil ya tiene invitación activa
            InvActRepo-->>Servicio: Invitación existente
            Servicio-->>API: excepcion()
            API-->>Director: Notificar duplicación
        else Invitación válida
            Servicio->>InvActRepo: save(InvitacionActividad[PENDIENTE])
            InvActRepo-->>Servicio: unaInvitacion
            Servicio->>Notif: notificarPerfil(unPerfil, unaInvitacion)
            Notif-->>Servicio: Solicitud de notificación
            Servicio-->>API: invitacionRegistrada(unaInvitacion)
            API-->>Director: Invitación registrada
        end
    end
```

La invitación general se asocia directamente al proyecto: `InvitacionGral`
conserva `proyecto` y deja `requerimientoGral` opcional. La invitación por
actividad debe asociar el perfil destinatario, el perfil remitente, el
requerimiento de actividad, el mensaje y el estado `PENDIENTE`. La notificación
se dirige al perfil destinatario.

La búsqueda previa de requerimientos de actividad corresponde a UC-73 y se
expone mediante `GET /proyectos/{idProyecto}/requerimientos-actividad/buscar`,
con filtros opcionales por actividad, profesión, características y habilidades.

Para seleccionar varios perfiles en una sola acción se agregan:

```http
POST /proyectos/{idProyecto}/invitaciones/general/batch
POST /proyectos/{idProyecto}/invitaciones/actividad/batch
```

El servicio valida una vez el contexto común (perfil activo, proyecto,
permisos y, en actividad, requerimiento y estado) y luego procesa cada perfil
en una transacción independiente. La respuesta contiene `creadas[]` y
`errores[]`, permitiendo éxito parcial. Los IDs repetidos se deduplican.

## UC-70 — Asignar actividad a miembro

La asignación relaciona un `MiembroProyecto` activo con una `Actividad`.
Primero se obtiene el perfil y su usuario para verificar la disponibilidad
frente al rango factible de la actividad; solamente después de la confirmación
se persiste `AsignacionActividad`.

```mermaid
sequenceDiagram
    autonumber
    actor Director as Director de proyecto
    participant API as AsignacionActividadController
    participant Servicio as AsignarActividadService
    participant ActividadService as ObtenerActividadService
    participant PerfilService as ObtenerPerfilService
    participant MiembroRepo as MiembroProyectoRepository
    participant Calendario as CalendarioService
    participant AsignacionRepo as AsignacionActividadRepository
    participant ReqService as RequerimientoActividadService
    participant DB as Base de datos
    participant Notificacion as Tabla notificacion

    Director->>API: Seleccionar actividad
    API->>ActividadService: obtenerActividad(idActividad)
    ActividadService-->>API: unaActividad
    Director->>API: Seleccionar miembro/perfil
    API->>PerfilService: obtenerPerfil(idPerfil)
    PerfilService-->>API: unPerfil
    API->>MiembroRepo: Buscar miembro activo del proyecto
    MiembroRepo-->>API: unMiembroProyecto
    API-->>Director: Actividad y perfil seleccionados

    API->>Servicio: buscarUsuario(unPerfil)
    Servicio-->>API: unUsuario
    API->>Calendario: verificarDisponibilidad(unUsuario, unaActividad)
    Calendario-->>API: Resultado de disponibilidad

    alt Miembro sin disponibilidad suficiente
        API-->>Director: excepcion()
        Note right of API: El caso de uso finaliza sin crear asignación
    else Miembro disponible frente al rango factible
        API-->>Director: solicitarConfirmacion()
        Director->>API: Confirmar asignación
        API->>Servicio: asignarActividad(unPerfil, unaActividad)
        Servicio->>MiembroRepo: Validar miembro activo del proyecto
        MiembroRepo-->>Servicio: unMiembroProyecto
        Servicio->>AsignacionRepo: save(AsignacionActividad)
        AsignacionRepo-->>Servicio: unaAsignacionActividad

        opt El miembro satisface un requerimiento de actividad
            Servicio->>ReqService: actualizarEstadoRequerimiento(unRequerimientoAct)
            ReqService-->>Servicio: Requerimiento actualizado/cerrado
        end

        AsignacionRepo->>DB: INSERT asignacion_actividad
        DB->>DB: Ejecutar trg_notificar_asignacion_actividad
        DB->>Notificacion: INSERT ACTIVIDAD_ASIGNADA
        Notificacion-->>DB: Notificación generada
        Servicio-->>API: asignacionExitosa(unaAsignacionActividad)
        API-->>Director: Asignación registrada
    end
```

La operación `asignarActividad` debe ejecutarse dentro de una transacción:
la asignación y la actualización condicional del requerimiento forman parte
del mismo resultado de negocio. El trigger
`trg_notificar_asignacion_actividad` crea la notificación de tipo
`ACTIVIDAD_ASIGNADA` para el usuario y perfil del miembro insertado.

## UC-32 — Eliminar invitación a proyecto

El contrato de `darDeBajaInvitación` establece que la invitación debe estar en
estado `PENDIENTE`. Una vez confirmada la acción, se elimina el registro de la
invitación, no se lo transforma en un estado histórico alternativo.

```mermaid
sequenceDiagram
    autonumber
    actor Director as Director de proyecto
    participant API as InvitacionController
    participant Servicio as EliminarInvitacionService
    participant InvitacionService as BuscarInvitacionesService
    participant InvGralRepo as InvitacionGralRepository
    participant InvActRepo as InvitacionActividadRepository
    participant ProyectoService as ProyectoSecurityService

    Director->>API: Seleccionar proyecto
    API->>ProyectoService: Validar director y proyecto
    ProyectoService-->>API: Acceso autorizado
    Director->>API: Buscar invitaciones del proyecto
    API->>InvitacionService: buscarInvitaciones(filtros)
    InvitacionService-->>API: invitaciones[]
    API-->>Director: Mostrar invitaciones

    Director->>API: Seleccionar unaInvitacion
    API-->>Director: Mostrar información y solicitar confirmación
    Director->>API: confirmarBajaInvitacion(unaInvitacion)
    API->>Servicio: darDeBajaInvitacion(unaInvitacion)
    Servicio->>ProyectoService: Validar que el director administra el proyecto
    ProyectoService-->>Servicio: Acceso autorizado

    alt Invitación no está en estado PENDIENTE
        Servicio-->>API: excepcion()
        API-->>Director: Notificar error y finalizar
    else Invitación PENDIENTE
        alt Invitación general
            Servicio->>InvGralRepo: delete(unaInvitacion)
        else Invitación por actividad
            Servicio->>InvActRepo: delete(unaInvitacion)
        end
        Servicio-->>API: invitacionEliminada()
        API-->>Director: Operación exitosa
    end
```

La eliminación debe ejecutarse dentro de una transacción y debe volver a
validar el estado de la invitación al confirmar la operación, para evitar
eliminar una invitación que haya sido aceptada o rechazada entre la consulta y
la confirmación.

La implementación expone dos endpoints `DELETE`, uno por tipo:

```http
DELETE /proyectos/{idProyecto}/invitaciones/general/{idInvitacion}
DELETE /proyectos/{idProyecto}/invitaciones/actividad/{idInvitacion}
```

Sólo el Director que envió la invitación puede eliminarla. El backend valida
`ELIMINAR_INVITACION`, rol Director activo, pertenencia al proyecto, remitente y
estado `PENDIENTE`. La fila se bloquea con `PESSIMISTIC_WRITE` para evitar
carreras con UC-28. El borrado es físico y no genera notificación. La respuesta
exitosa es `204 No Content`.

## UC-38 — Eliminar integrante

El director elimina a un integrante activo del proyecto. La baja se registra
como estado `ELIMINADO`, se guarda el motivo y la fecha de baja, y se conservan
las asignaciones para auditoría y trazabilidad. El profesional deja de contar
como recurso activo, por lo que cada actividad que pierde el recurso reabre la
vacante correspondiente por cálculo de cupo.

```mermaid
sequenceDiagram
    autonumber
    actor Director as Director de proyecto
    participant API as MiembroProyectoController
    participant Servicio as EliminarIntegranteService
    participant ProyectoService as ProyectoSecurityService
    participant PerfilService as ObtenerPerfilService
    participant MiembroRepo as MiembroProyectoRepository
    participant AsignacionRepo as AsignacionActividadRepository
    participant ReqService as RequerimientoActividadService
    participant Notif as NotificacionService

    Director->>API: Seleccionar proyecto
    API->>ProyectoService: Validar director y proyecto PUBLICADO/CONFIRMADO
    ProyectoService-->>API: Acceso autorizado
    Director->>API: Seleccionar perfil integrante
    API->>PerfilService: obtenerPerfil(idPerfil)
    PerfilService-->>API: unPerfil
    API-->>Director: Mostrar información del perfil y solicitar motivo
    Director->>API: Introducir motivo y confirmar
    API->>Servicio: eliminarIntegrante(unProyecto, unPerfil, motivo)
    Servicio->>MiembroRepo: Buscar miembro activo del proyecto
    MiembroRepo-->>Servicio: unMiembroDeProyecto

    alt Motivo vacío
        Servicio-->>API: excepcion()
        API-->>Director: Notificar error y finalizar
    else Miembro con estado inválido
        Servicio-->>API: excepcion()
        API-->>Director: Notificar error y finalizar
    else Miembro activo y motivo válido
        Servicio->>MiembroRepo: setEstado(ELIMINADO)
        Servicio->>MiembroRepo: setMotivoBaja(motivo)
        Servicio->>MiembroRepo: setFechaBaja(ahora)
        Servicio->>MiembroRepo: save(unMiembroDeProyecto)
        Note over Servicio,ReqService: Las asignaciones se conservan para auditoría.<br/>La vacante se reabre por cálculo de cupo.
        Servicio->>ReqService: recalcularVacantesDeActividades(unProyecto, unPerfil)
        ReqService-->>Servicio: Requerimientos actualizados
        Servicio->>Notif: notificarIntegranteEliminado(unPerfil, motivo)
        Notif-->>Servicio: Solicitud de notificación
        Servicio-->>API: bajaIntegranteRegistrada(unProyecto, unPerfil)
        API-->>Director: Operación exitosa
    end
```

La implementación actual realiza una baja lógica: la membresía pasa a
`ELIMINADO` y conserva `motivoBaja` y `fechaBaja`. No se borran físicamente las
asignaciones; se conservan para auditoría. Las consultas operativas filtran
miembros `ACTIVO`, por lo que el profesional deja de contar como recurso y la
vacante se reabre por cálculo de cupo. El historial de actividad muestra a los
profesionales eliminados cuando la actividad está `EN_CURSO`, `FINALIZADA` o
`CANCELADA`; no los muestra en `PENDIENTE`. La notificación se dirige al perfil
eliminado.

## UC-40 — Darse de baja de proyecto

La baja voluntaria sigue el mismo criterio de baja lógica que UC-38: la
membresía pasa a `BAJA_VOLUNTARIA`, conserva `motivoBaja` y `fechaBaja`, y las
asignaciones a actividades no se borran; quedan como historial. La vacante se
reabre por cálculo de cupo porque las consultas operativas filtran miembros
activos. El actor es cualquier miembro activo, incluido un Director si queda al
menos otro Director activo. La notificación se dirige a los directores activos
restantes.

```mermaid
sequenceDiagram
    autonumber
    actor Miembro as Miembro de proyecto
    participant API as MiembroProyectoController
    participant Servicio as DarseDeBajaProyectoService
    participant MiembroRepo as MiembroProyectoRepository
    participant DirectorRepo as MiembroProyectoRepository
    participant Notif as NotificacionService

    Miembro->>API: Seleccionar proyecto
    API-->>Miembro: Mostrar información y solicitar motivo obligatorio
    Miembro->>API: Introducir motivo y confirmar
    API->>Servicio: darseDeBaja(unProyecto, motivo)
    Servicio->>MiembroRepo: Buscar membresía activa del perfil
    MiembroRepo-->>Servicio: unMiembroDeProyecto

    alt Motivo vacío
        Servicio-->>API: excepcion()
        API-->>Miembro: Notificar error y finalizar
    else Proyecto no Publicado/Confirmado
        Servicio-->>API: excepcion()
        API-->>Miembro: Notificar error y finalizar
    else Membresía inactiva
        Servicio-->>API: excepcion()
        API-->>Miembro: Notificar error y finalizar
    else Director único
        Servicio-->>API: excepcion()
        API-->>Miembro: Notificar error y finalizar
    else Motivo válido y baja permitida
        Servicio->>MiembroRepo: setEstado(BAJA_VOLUNTARIA)
        Servicio->>MiembroRepo: setMotivoBaja(motivo)
        Servicio->>MiembroRepo: setFechaBaja(ahora)
        Servicio->>MiembroRepo: save(unMiembroDeProyecto)
        Note over Servicio,MiembroRepo: Las asignaciones se conservan para auditoría.<br/>La vacante se reabre por cálculo de cupo.
        Servicio->>DirectorRepo: Buscar directores activos restantes
        DirectorRepo-->>Servicio: directores[]
        Servicio->>Notif: notificarDirectores(unProyecto, motivo)
        Notif-->>Servicio: Notificaciones registradas
        Servicio-->>API: Baja voluntaria registrada
        API-->>Miembro: Operación exitosa
    end
```

La operación es transaccional. No se borran asignaciones ni se recalcula el
cronograma. Si el proyecto está `CONFIRMADO`, las actividades conservan sus
fechas. El historial de actividades muestra a los profesionales con
`estadoParticipacion = BAJA_VOLUNTARIA` cuando la actividad está `EN_CURSO`,
`FINALIZADA` o `CANCELADA`; no los muestra en `PENDIENTE`.

## UC-09 — Autenticar mediante Google OAuth

**Implementación:** `GET /auth/google`, `GET /auth/google/pendiente` y
`POST /auth/google/completar`, atendidos por `AuthController` y
`GoogleAuthService`, con `usuario_identidad` como fuente de proveedores.

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant Front as Frontend
    participant API as AuthController
    participant OAuth as Spring OAuth2 Client
    participant Google as Google OAuth
    participant Servicio as GoogleAuthService
    participant IdentidadRepo as UsuarioIdentidadRepository
    participant UsuarioRepo as UsuarioRepository
    participant Auth as AuthService

    Usuario->>Front: Clic "Continuar con Google"
    Front->>API: GET /auth/google
    API-->>Front: 302 /oauth2/authorization/google
    Front->>OAuth: GET /oauth2/authorization/google
    OAuth->>OAuth: Guardar state en cookie firmada
    OAuth-->>Usuario: 302 accounts.google.com
    Usuario->>Google: Autoriza acceso
    Google-->>OAuth: GET /login/oauth2/code/google?code&state
    OAuth->>Google: Canjear code por token
    Google-->>OAuth: userinfo (sub, email, email_verified, name)
    OAuth->>Servicio: autenticar(GoogleUserData)

    alt email_verified = false
        Servicio-->>OAuth: CuentaGoogleNoVerificada
        OAuth-->>Front: 302 /login?googleError=google_auth_failed
    else Identidad GOOGLE existente
        Servicio->>IdentidadRepo: findByProveedorAndIdExterno(GOOGLE, sub)
        IdentidadRepo-->>Servicio: UsuarioIdentidad
        Servicio->>Auth: reactivarSiPendienteBaja(usuario)
        Servicio->>Auth: construirLoginResultado(usuario)
        Auth-->>Servicio: JWT
        Servicio-->>OAuth: LoginResultado
        OAuth-->>Front: 302 /auth/google/callback + Set-Cookie jwt
    else Usuario local con el mismo correo
        Servicio->>UsuarioRepo: findByCorreo(correo)
        UsuarioRepo-->>Servicio: Usuario
        Servicio->>IdentidadRepo: save(UsuarioIdentidad GOOGLE)
        Servicio->>Auth: construirLoginResultado(usuario)
        Servicio-->>OAuth: LoginResultado
        OAuth-->>Front: 302 /auth/google/callback + Set-Cookie jwt
    else Usuario nuevo
        Servicio->>Servicio: crearTicketRegistro(datosGoogle)
        Servicio-->>OAuth: RegistroPendiente(ticket)
        OAuth-->>Front: 302 /auth/google/completar + Set-Cookie google_registro
        Front->>API: GET /auth/google/pendiente
        API-->>Front: nombre, apellido, correo
        Usuario->>Front: Completa DNI, fecha y género
        Front->>API: POST /auth/google/completar
        API->>Servicio: completarRegistro(ticket, request)
        Servicio->>Servicio: Validar DNI, edad y género
        Servicio->>UsuarioRepo: save(Usuario con password_hash NULL)
        Servicio->>IdentidadRepo: save(UsuarioIdentidad GOOGLE)
        Servicio->>Auth: construirLoginResultado(usuario)
        Auth-->>Servicio: JWT
        API-->>Front: AuthResponse + Set-Cookie jwt
    end
```

La cuenta canónica sigue siendo `usuario`; `usuario_identidad` solo resuelve
con qué proveedor se autenticó. El onboarding es obligatorio antes de crear la
cuenta para no relajar las restricciones de DNI, edad y género.

## UC-23 — Configurar términos y condiciones

**Implementación actual:** `GET/PUT /perfiles/{idPerfil}/terminos-condiciones`,
atendido por `PerfilTerminosCondicionesController` y
`GestionTerminosCondicionesService`.

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant API as PerfilTerminosCondicionesController
    participant Servicio as GestionTerminosCondicionesService
    participant PerfilRepo as PerfilRepository
    participant TycRepo as PerfilTycRepository
    participant Config as ConfiguracionSistemaService
    participant DB as PostgreSQL

    Usuario->>API: GET /perfiles/{idPerfil}/terminos-condiciones
    API->>Servicio: obtener(idUsuario, idPerfil)
    Servicio->>PerfilRepo: findById(idPerfil)
    PerfilRepo-->>Servicio: Perfil
    Servicio->>TycRepo: findFirstBy...FechaFinIsNull(idPerfil)
    alt Existe versión vigente
        TycRepo-->>Servicio: PerfilTyc
        Servicio-->>API: TerminosCondicionesResponse (esPlantilla=false)
    else No existe versión
        Servicio->>Config: obtenerPlantillaTerminosCondiciones()
        Config-->>Servicio: texto de plantilla
        Servicio-->>API: TerminosCondicionesResponse (esPlantilla=true)
    end
    API-->>Usuario: 200 OK

    Usuario->>API: PUT /perfiles/{idPerfil}/terminos-condiciones {descripcion}
    API->>Servicio: actualizar(idUsuario, idPerfil, request)
    Servicio->>PerfilRepo: findByIdPerfilAndUsuarioIdUsuarioForUpdate(...)
    PerfilRepo-->>Servicio: Perfil (lock pesimista)
    alt Perfil no Activo
        Servicio-->>API: PerfilEnBajaException (409)
    else Perfil Activo
        Servicio->>TycRepo: buscar versión vigente
        alt Mismo texto
            Servicio-->>API: versión vigente sin cambios
        else Texto distinto
            Servicio->>TycRepo: cerrar versión vigente (fecha_fin)
            Servicio->>TycRepo: nextIdTyc()
            Servicio->>DB: persist nueva versión
            Servicio-->>API: TerminosCondicionesResponse
        end
    end
    API-->>Usuario: 200 OK
```

La versión vigente se incluye en `GET /perfiles/{idPerfil}`. Cuando un proyecto
incorpora al perfil, el director debe aceptar esa versión; el id aceptado se
guarda en `invitacion_gral.id_tyc_aceptado`,
`invitacion_actividad.id_tyc_aceptado` o
`miembros_proyecto.id_tyc_aceptado` según el flujo.