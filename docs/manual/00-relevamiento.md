# Relevamiento de pantallas — Manual de usuario de ModaLink

**Etapa:** 1 de 4 (relevamiento) — **VALIDADO**
**Fecha:** 16/11/2026
**Decisiones de validación (16/11/2026):**
1. Se incluyen los cuatro capítulos complementarios: Contratos, Mensajería, Calificaciones y reseñas, y Colaboraciones automáticas.
2. Las publicaciones se documentan con el mismo nivel de detalle que los proyectos, dentro del capítulo 8.
3. Solo se documentan las pantallas de administración navegables desde el menú o desde un enlace visible. **Búsqueda de usuarios** queda fuera del cuerpo del manual; **Proyectos** se incluye porque el Dashboard tiene un enlace **Ver todos →**. La antigua pantalla **Reportes** ya no existe.
4. Los defectos visibles de la interfaz no se mencionan en el manual.
**Fuentes consultadas:**
- Frontend Vue 3: `C:\Users\HP\Desktop\modalink-frontend` (router, vistas, componentes y servicios).
- Backend Spring Boot: `C:\Users\HP\Desktop\modalink-backend` (controladores, servicios y reglas).
- Esquema canónico: `docs/3_diseño/ModaLinkBD.sql` y migraciones.
- Requisitos y seguimiento: `docs/1_requisitos/casos_de_uso.md` y `docs/5_estado_sistema/estado_uc.md`.

**Criterio de estado**
- **Completa:** la pantalla existe, es alcanzable desde la interfaz y su flujo principal está implementado de punta a punta (frontend + backend).
- **Parcial:** la pantalla existe, pero hay acciones, secciones o validaciones incompletas, ocultas o señaladas como no disponibles.
- **Solo backend:** la funcionalidad existe en el servidor pero no tiene pantalla ni control visible para el usuario.

> Los números entre corchetes remiten a las observaciones del final del documento.

---

## 1. Autenticación y acceso

| Pantalla | Ruta | Rol que accede | Caso de uso asociado | Estado |
|---|---|---|---|---|
| Te damos la bienvenida (iniciar sesión) | `/login` | Público | UC-01, UC-09; reactivación UC-07 | Completa [1][3] |
| Creá tu cuenta (registro) | `/registro` | Público | UC-03, UC-09 | Completa [4] |
| Conectando con Google (retorno) | `/auth/google/callback` | Público (retorno de Google) | UC-09 | Completa [5] |
| Completá tu registro (onboarding Google) | `/auth/google/completar` | Usuario pendiente de completar datos de Google | UC-09 | Completa |
| Ingresando a ModaLink (enlace mágico por correo) | `/auth/email` | Público (enlace recibido por correo) | Extensión de UC-01 (no catalogada) | Completa [6] |
| Normas de convivencia | `/normas-de-convivencia` | Público | Condición de la plataforma | Completa |
| Ayuda / manual de usuario | `/ayuda` y `/ayuda/:id` | Usuario autenticado | Manual in-app | Completa |
| Cerrar sesión (acción global) | Menú de perfil / barra lateral | Usuario y Administrador | UC-02 | Completa |

---

## 2. Cuenta, perfiles y agenda

| Pantalla | Ruta | Rol que accede | Caso de uso asociado | Estado |
|---|---|---|---|---|
| Dashboard / Mis perfiles | `/dashboard-usuario` | Usuario autenticado (también sin perfil activo) | UC-07, UC-12, UC-13, UC-14 | Completa [7][8] |
| Modificar datos personales | `/dashboard-usuario/modificar-datos` | Usuario autenticado | UC-08 | Completa [10] |
| Calendario | `/dashboard-usuario/calendario` | Usuario con perfil activo | UC-17, UC-18, UC-71 | Completa [11] |
| Notificaciones de la cuenta | `/dashboard-usuario/notificaciones` | Usuario autenticado | Módulo de notificaciones (sin UC numerado) | Completa [12] |
| Notificaciones del perfil | `/notificaciones` | Usuario con perfil activo | Módulo de notificaciones (sin UC numerado) | Completa [12] |
| Crear nuevo perfil | `/dashboard-usuario/crear-perfil` | Usuario autenticado | UC-10, UC-47, UC-14 | Completa [13] |
| Editar perfil (desde el panel) | `/dashboard-usuario/editar-perfil/:id` | Perfil activo dueño | UC-11, UC-47 | Completa [14] |
| Mi perfil | `/inicio-perfil` y `/inicio-perfil/:seccion` | Usuario con perfil activo | UC-14, UC-14 bis, UC-22, UC-23, UC-47, polaroids | Completa [15][16][18] |
| Ver perfil de otro profesional | `/perfiles/:id` y `/perfiles/:id/:seccion` | Usuario con perfil activo | UC-14, UC-14 bis, UC-15, UC-16, UC-43, UC-44, UC-45 | Completa [19] |
| Ajustes de perfil | `/inicio-perfil/ajustes` | Perfil activo dueño | UC-11, UC-12, UC-23 | Completa [20] |
| Editar perfil (desde Mi perfil / Ajustes) | `/inicio-perfil/editar/:id` y `/inicio-perfil/ajustes/editar/:id` | Perfil activo dueño | UC-11, UC-47 | Completa [14] |
| Configurar términos y condiciones | `/inicio-perfil/ajustes/terminos-condiciones` | Perfil activo dueño (perfil en estado Activo) | UC-23 | Completa [21] |
| Detalle de una reseña | `/inicio-perfil/resenas/:idCalificacion` y `/perfiles/:id/resenas/:idCalificacion` | Usuario con perfil activo | UC-14 bis | Completa [22] |
| Detalle de publicación | `/publicaciones/:idPublicacion` | Usuario con perfil activo | UC-22, UC-66, UC-69, UC-61 a UC-64 | Completa [23][24] |

---

## 3. Búsqueda, red, mensajería y contratos

| Pantalla | Ruta | Rol que accede | Caso de uso asociado | Estado |
|---|---|---|---|---|
| Buscar (perfiles, proyectos y oportunidades) | `/buscar` | Usuario con perfil activo | UC-16, UC-29, UC-72, UC-73, UC-43 | Completa [25][26] |
| Mi red | `/mi-red` | Usuario con perfil activo | UC-43, UC-44, UC-45, UC-46 | Completa [27] |
| Mensajes (chat privado) | `/mensajes` | Usuario con perfil activo | UC-42 | Completa [28] |
| Contratos | `/contratos` | Usuario con perfil activo | UC-68, UC-55, UC-53, UC-54 | Completa [29] |
| Pendientes de mi firma | `/contratos/pendientes` | Usuario con perfil activo | UC-55, UC-68 | Completa |
| Detalle del contrato | `/proyectos/:idProyecto/contratos/:idContrato` | Perfil activo con visibilidad del contrato | UC-55, UC-53, UC-54, UC-68 | Completa [30] |
| Modificar contrato | `/proyectos/:idProyecto/contratos/:idContrato/modificar` | Director activo del proyecto | UC-53 | Completa |

---

## 4. Proyectos y actividades

| Pantalla | Ruta | Rol que accede | Caso de uso asociado | Estado |
|---|---|---|---|---|
| Proyectos / Inicio | `/proyectos` | Usuario con perfil activo | UC-29 bis | Completa [31] |
| Proyectos / Crear proyecto (asistente de 4 pasos) | `/crear-proyecto` | Usuario con perfil activo | UC-24 | Completa [32] |
| Proyecto (panel con pestañas) | `/proyectos/:id` | Miembro activo; no miembro con visibilidad según privacidad | UC-29, UC-33 a UC-38, UC-40, UC-41, UC-49 a UC-52, UC-60, UC-70 | Completa [33][34][35] |
| Proyectos / Modificar proyecto | `/proyectos/:id/modificar` | Director activo con permiso de modificación | UC-36 | Completa [36] |
| Invitar a proyecto | `/proyectos/:id/invitar` | Director activo con permiso de invitación | UC-31, UC-23, UC-28 | Completa |
| Nueva actividad | `/proyectos/:id/actividades/crear` | Miembro activo con permiso de creación | UC-49, UC-70 | Completa [37] |
| Editar actividad | `/proyectos/:id/actividades/:idActividad/modificar` | Miembro activo con permiso de modificación | UC-50, UC-51 | Completa [38] |
| Actividad (detalle) | `/proyectos/:id/actividades/:idActividad/detalle` | Perfil con visibilidad del proyecto | UC-52, UC-26 | Completa |
| Calificaciones del equipo | `/proyectos/:id/calificaciones` | Director y miembros habilitados al finalizar | UC-37 | Completa [39] |
| Mis postulaciones | `/proyectos/mis-postulaciones` | Usuario con perfil activo | UC-27 | Completa [40] |
| Solicitudes | `/solicitudes` | Usuario con perfil activo | UC-28, UC-30 a UC-32, UC-34, UC-61 a UC-65, reemplazos | Completa [41] |
| Colaboraciones automáticas | `/colaboraciones-automaticas` | Usuario con perfil activo | Extensión no catalogada | Completa [42] |
| Detalle de colaboración automática | `/colaboraciones-automaticas/:id` | Usuario con perfil activo | Extensión no catalogada | Completa [42] |
| Nuevo contrato | `/proyectos/:id/contratos/nuevo` | Director activo del proyecto | UC-39 | Completa [43] |

---

## 5. Panel de Administración

| Pantalla | Ruta | Rol que accede | Caso de uso asociado | Estado |
|---|---|---|---|---|
| Panel de administración (dashboard) | `/admin/dashboard` | Administrador con permiso de ver usuarios | Dashboard administrativo (sin UC numerado) | Completa [44] |
| Notificaciones (administración) | `/admin/notificaciones` | Administrador | Módulo de notificaciones (sin UC numerado) | Completa |
| Ayuda / manual de administración | `/admin/ayuda` y `/admin/ayuda/:id` | Administrador | Manual in-app | Completa |
| Gestión de usuarios | `/admin/gestion-usuarios` | Administrador | UC-04, UC-05, UC-06 | Completa [45][46] |
| Búsqueda de usuarios | `/admin/busqueda-usuarios` | Administrador con permiso de ver usuarios | UC-06 | Completa [47] |
| Características técnicas | `/admin/gestionar-caracteristicas` | Administrador con permiso de ver características | UC-57, UC-58, UC-59 | Completa |
| Habilidades | `/admin/habilidades` | Administrador con permiso de ver habilidades | UC-48, UC-56 | Completa |
| Unidades de medida | `/admin/unidades-medida` | Administrador con permiso de ver características | UC-75 | Completa [48][49] |
| Tipos de contrato | `/admin/tipos-contrato` | Administrador con permiso de ver tipos de contrato | UC-76 | Completa |
| Configuración del sistema | `/admin/configuracion` | Administrador con permiso de administrar configuración | Configuración (sin UC numerado) | Completa [50] |
| Scheduler de deshabilitación | `/admin/configuracion-scheduler` | Administrador con permiso de administrar configuración | Extensión de UC-04 | Completa |
| Scheduler de bajas | `/admin/configuracion-scheduler/baja` | Administrador con permiso de administrar configuración | Extensión de UC-07 | Completa |
| Limpieza de imágenes | `/admin/configuracion-scheduler/limpieza-imagenes` | Administrador con permiso de administrar configuración | Extensión (sin UC numerado) | Completa |
| Reparación de cronogramas | `/admin/configuracion/cronograma` | Administrador con permiso de administrar configuración | Extensión de UC-33 | Completa |
| Colaboraciones automáticas históricas | `/admin/configuracion/colaboraciones-automaticas-historicas` | Administrador con permiso de administrar configuración | Extensión (sin UC numerado) | Completa |
| Detalle de procesamiento histórico | `/admin/configuracion/colaboraciones-automaticas-historicas/:id` | Administrador con permiso de administrar configuración | Extensión (sin UC numerado) | Completa |
| Reportes de perfil | `/admin/reportes-perfil` | Administrador | UC-15 | Completa |
| Detalle del reporte de perfil | `/admin/reportes-perfil/:id` | Administrador | UC-15 | Completa |
| Reportes de publicaciones | `/admin/reportes-publicacion` | Administrador | UC-69 | Completa |
| Detalle del reporte de publicación | `/admin/reportes-publicacion/:id` | Administrador | UC-69 | Completa |
| Auditoría (informe y exportación) | `/admin/auditoria` | Administrador | UC-67 | Completa [51] |
| Proyectos (administración) | `/admin/proyectos` | Administrador con permiso de ver usuarios | Consulta administrativa (sin UC numerado) | Completa [52] |

---

## 6. Flujos y modales sin ruta propia

Estos flujos son accesibles desde las pantallas anteriores y se documentarán dentro del capítulo correspondiente.

| Flujo / modal | Se abre desde | Rol que accede | Caso de uso asociado | Estado |
|---|---|---|---|---|
| Reactivar cuenta pendiente de baja | Iniciar sesión | Usuario con cuenta pendiente de baja | UC-07 | Completa |
| Solicitar baja de la cuenta | Barra lateral del panel de usuario | Usuario autenticado | UC-07 | Completa |
| Cambiar perfil activo | Menú de perfil / dashboard | Usuario con más de un perfil | UC-13 | Completa |
| Solicitar baja del perfil | Panel de usuario, Ajustes, Mi perfil | Perfil activo dueño | UC-12 | Completa |
| Reactivar perfil | Panel de usuario, Mi perfil | Perfil en baja pendiente | UC-12 | Completa |
| Subir, cambiar o eliminar foto de perfil | Crear / Editar perfil | Perfil activo dueño | UC-10, UC-11 | Completa |
| Gestionar habilidades del perfil | Crear / Editar perfil | Perfil activo dueño | UC-47 | Completa |
| Gestionar polaroids (alta, edición, orden, baja) | Mi perfil | Perfil activo dueño | Extensión de UC-10/11/14 | Completa |
| Configurar jornada laboral y margen | Calendario | Perfil activo | UC-71 | Completa |
| Agregar / eliminar bloqueo de disponibilidad | Calendario | Perfil activo | UC-17, UC-18 | Completa |
| Reportar perfil | Ver perfil ajeno | Usuario con perfil activo (no administrador) | UC-15 | Completa |
| Publicar proyecto | Panel del proyecto | Director activo | UC-25 | Completa |
| Confirmar proyecto | Panel del proyecto | Director activo | UC-60 | Completa |
| Finalizar proyecto (incluye finalización anticipada) | Panel del proyecto | Director activo | UC-37 | Completa |
| Cancelar proyecto | Panel del proyecto | Director activo | UC-35 | Completa |
| Darse de baja del proyecto | Panel del proyecto | Miembro activo | UC-40 | Completa |
| Eliminar integrante | Pestaña Miembros | Director activo | UC-38 | Completa |
| Asignar / desasignar actividad | Planificación / actividad | Director activo | UC-70 | Completa |
| Crear / editar requerimiento de una actividad | Nueva / Editar actividad | Miembro con permiso | UC-49, UC-50 | Completa |
| Eliminar actividad (con decisión sobre integrantes exclusivos) | Planificación / actividad | Miembro con permiso | UC-51 | Completa |
| Cambiar estado de actividad | Editar actividad | Miembro con permiso | UC-50 | Completa |
| Postularse a un requerimiento general o de actividad | Buscar, Home, panel del proyecto | Usuario con perfil activo | UC-26 | Completa |
| Aceptar / rechazar postulación | Pestaña Postulaciones | Director activo | UC-34 | Completa |
| Aceptar / rechazar invitación | Solicitudes | Perfil destinatario | UC-28 | Completa |
| Aviso de normas de convivencia | Postulación, invitación y reemplazo | Usuario | Condición de la plataforma | Completa |
| Eliminar invitación enviada | Solicitudes | Director emisor | UC-32 | Completa |
| Aprobar / rechazar candidato de reemplazo | Pestaña Reemplazos | Director activo | Extensión de UC-38/UC-40 | Completa |
| Confirmar, regenerar o cancelar proceso de reemplazo | Pestaña Reemplazos | Director activo | Extensión de UC-38/UC-40 | Completa |
| Aceptar / rechazar invitación de reemplazo | Solicitudes | Perfil candidato | Extensión de UC-38/UC-40 | Completa |
| Nueva publicación | Home / Mi perfil | Perfil activo dueño | UC-19 | Completa |
| Editar publicación | Home / Mi perfil / detalle | Perfil activo dueño | UC-21 | Completa |
| Eliminar publicación | Home / Mi perfil / detalle | Dueño o colaborador (alcance distinto) | UC-20 | Completa |
| Me gusta y comentarios | Publicación | Usuario con perfil activo | UC-66 | Completa |
| Enviar / aceptar / rechazar / eliminar solicitud de colaboración | Publicación / Solicitudes | Perfil activo | UC-61 a UC-64 | Completa |
| Reportar publicación | Publicación | Usuario con perfil activo (no dueño) | UC-69 | Completa |
| Ocultar / reactivar publicación reportada | Reportes de publicaciones | Administrador | UC-69 | Completa |
| Firmar / rechazar contrato | Detalle del contrato | Miembro activo con firma pendiente | UC-55 | Completa |
| Eliminar contrato | Listado / detalle | Director activo | UC-54 | Completa |
| Chat grupal del proyecto | Pestaña Chat grupal | Miembro activo | UC-41 | Completa |

---

## 7. Solo backend

| Funcionalidad | Dónde existe | Estado | Nota |
|---|---|---|---|
| Configuración de notificaciones por correo | Backend y base | Solo backend | Scheduler, remitente, reintentos y prioridad se configuran por propiedades del sistema; no hay pantalla. [54] |
| Historial de eventos de un contrato | Backend | Solo backend | El detalle del contrato muestra datos y firmas, pero no la línea de eventos/versiones. [30] |
| Auditoría de bajas de un proyecto | Backend | Solo backend | No se encontró pantalla que la consuma. |
| Presencia en línea en tiempo real | Backend y componentes | Completa (invisible) | Se refleja en “En línea” de Mi red, mensajes y perfiles; no tiene pantalla propia. |
| Rechazo automático por cupo completo | Backend | Solo backend | Se refleja en el estado de postulaciones e invitaciones y en notificaciones. |
| Eliminación definitiva de cuentas y perfiles vencidos | Backend (scheduler) | Solo backend | Se configura desde el Panel de Administración; el usuario no ejecuta la acción. |

---

## 8. Mapa de capítulos propuesto

| Archivo | Contenido | Pantallas que cubre |
|---|---|---|
| `01-introduccion.md` | Qué es ModaLink, destinatarios, convenciones | — |
| `02-conceptos.md` | Usuario, perfil, roles, glosario | — |
| `03-primeros-pasos.md` | Registro, Google, enlace por correo, login, recorrido | 1 |
| `04-mi-cuenta.md` | Datos personales, baja de cuenta, calendario/jornada/bloqueos | 2 (cuenta y calendario) |
| `05-perfiles.md` | Crear/editar perfil, foto, polaroids, T&C, baja, ver perfiles, reseñas | 2 |
| `06-proyectos-directores.md` | Asistente, planificación, requerimientos, postulaciones, invitaciones, confirmación, bajas, reemplazos | 4 |
| `07-participar.md` | Buscar, postularse, mis postulaciones, solicitudes, aceptar T&C, baja voluntaria | 3 y 4 |
| `08-conexiones-publicaciones.md` | Mi red, solicitudes de conexión, publicaciones, interacciones, colaboraciones | 2 y 3 |
| `09-notificaciones.md` | Bandeja de cuenta, de perfil y de administración | 2 y 5 |
| `10-administracion.md` | Dashboard, usuarios, catálogos, configuración, reportes, auditoría | 5 |
| `11-ayuda.md` | Preguntas frecuentes y errores frecuentes | — |
| `12-contratos.md` (complementario) | Generar, buscar, modificar, firmar, rechazar y eliminar contratos | 3 y 4 |
| `13-mensajeria.md` (complementario) | Chat privado y chat grupal | 3 y 4 |
| `14-calificaciones.md` (complementario) | Calificación del equipo y reseñas de perfil | 2 y 4 |
| `15-colaboraciones-automaticas.md` (complementario) | Bandeja y detalle de colaboraciones automáticas | 4 |
| `16-normas-de-convivencia.md` | Normas de convivencia, aceptación implícita y T&C de los participantes | 1 (aviso en postulación e invitaciones) |
| `Anexo A` | Permisos por rol | — |
| `Anexo B` | Estados y su significado | — |

---

## 9. Observaciones preliminares (a completar en la revisión final)

[1] El inicio de sesión con Google está integrado en la misma pantalla; no hay una pantalla separada de “elegir método”.
[3] El modal de reactivación de cuenta solo aparece cuando el backend responde con el código de cuenta pendiente de baja.
[4] El formulario de registro pide también DNI y género, que no figuran en el caso de uso UC-03.
[5] La pantalla de retorno de Google es solo de transición: no tiene acciones propias.
[6] El acceso por enlace de correo es una extensión implementada que no está en el catálogo de casos de uso.
[7] La pestaña “Calendario” del dashboard navega a otra ruta; el contenido alternativo de esa pestaña no es alcanzable.
[8] El color de las tarjetas de perfil por profesión está fijado en el código (comentario pendiente en el código).
[10] El archivo de la vista contiene un texto suelto antes de la plantilla (“Modificardatosview · VUE”), defecto técnico que conviene corregir.
[11] La agenda se puede consultar en modo lectura desde el perfil propio y ajeno; las acciones solo aparecen en el perfil propio.
[12] La bandeja no tiene paginación visible (pide 100 registros) ni acciones de borrar o marcar como no leída.
[13] La foto y las habilidades se asocian después de crear el perfil; si falla alguna, el perfil queda creado y se avisa.
[14] La profesión no se puede cambiar una vez creado el perfil.
[15] “Mi perfil” incluye publicaciones, proyectos, reseñas, términos, habilidades, características y polaroids.
[16] El historial de proyectos del perfil muestra solo proyectos finalizados o cancelados.
[18] Las polaroids se gestionan con modales dentro del perfil; no tienen pantalla propia.
[19] En el perfil ajeno el calendario es solo lectura y no se muestran acciones de edición.
[20] La baja del perfil se puede iniciar desde tres lugares distintos (panel, ajustes y perfil) con textos equivalentes.
[21] Los términos y condiciones son versionados: cada guardado crea una nueva versión vigente; el máximo es 5.000 caracteres.
[22] Las reseñas son de solo lectura: no se pueden escribir, responder ni reportar desde la interfaz.
[23] La creación y edición de publicaciones se hace en modales, no en pantallas con ruta propia.
[24] La baja de una publicación por parte de un colaborador solo quita su etiqueta; no elimina la publicación.
[25] La búsqueda unificada se compone en el navegador porque no existe un endpoint global único.
[26] En la pestaña “Perfiles” no hay filtro de ubicación, aunque el buscador sugiere buscar por ubicación.
[27] “Mi red” incluye conexiones y solicitudes; las solicitudes aceptadas/rechazadas se consultan en las mismas pestañas.
[28] El chat privado exige conexión activa; si se elimina la conexión, el historial queda en solo lectura.
[29] El listado de contratos incluye por defecto los contratos eliminados; el estado “Borrador” solo se ve desde los filtros avanzados.
[30] No hay pantalla para el historial de eventos/versiones del contrato, aunque el backend lo expone.
[31] La vista “Proyectos / Inicio” no aplica el orden por estado que sí existe como utilidad interna.
[32] Al entrar al paso “Personal” se agrega una fila de requerimiento vacía; si no se completa ni se quita, bloquea el avance.
[33] La pestaña “Postulaciones” se muestra también en la vista pública del proyecto, con el listado de personal requerido.
[34] La pestaña “Reemplazos” es solo para directores; el badge se calcula en el navegador porque no hay endpoint de resumen.
[35] El chat grupal es una pestaña del proyecto; no tiene ruta independiente (la ruta antigua redirige a la pestaña).
[36] En la modificación de proyecto, los pasos intermedios muestran a la vez “Siguiente” y “Guardar cambios”.
[37] La creación de actividades no permite fijar fechas manualmente: las calcula el sistema.
[38] “Iniciar” y “Finalizar” actividad se aplican sin confirmación; solo “Cancelar actividad” pide confirmación.
[39] La calificación es de 1 a 5 estrellas con reseña obligatoria para todos los integrantes pendientes.
[40] Solo se pueden dar de baja postulaciones en estado Pendiente.
[41] “Solicitudes” agrupa invitaciones a proyectos, solicitudes de colaboración y colaboraciones automáticas en pestañas.
[42] Las colaboraciones automáticas son una extensión implementada que no figura en el catálogo de casos de uso.
[43] La generación de contrato tiene dos pasos (definición y borrador) y termina en una pantalla de contrato emitido.
[44] La tarjeta “Nuevos usuarios” del dashboard muestra el total de usuarios registrados, no los nuevos del período.
[45] El filtro “Pendiente de baja” de Gestión de usuarios envía un valor distinto al que espera el backend; puede devolver error de parámetros.
[46] Las acciones de habilitar/deshabilitar no se ocultan por permiso en la interfaz; el backend las valida.
[47] “Búsqueda de usuarios” no está en el menú lateral: solo se accede por URL directa.
[48] El menú lateral dice “Unidad de medidas” y la pantalla dice “Unidades de medida”.
[49] Al modificar una unidad de medida no se notifica a los perfiles afectados (postcondición pendiente según la documentación de seguimiento).
[50] “Configuración del sistema” es un menú de cinco tarjetas; no edita parámetros generales.
[51] La auditoría permite filtrar, ver el resumen y exportar a CSV o PDF.
[52] “Proyectos” de administración no está en el menú lateral y su encabezado no tiene título de ruta.
[54] Las notificaciones por correo se envían solo para prioridad alta y su configuración no es visible en la interfaz.

### Contradicciones entre documentación y código (preliminar)

1. **UC-69 (reportar publicación):** `README.md` lo marca como pendiente, pero el código, las migraciones y `docs/5_estado_sistema/estado_uc.md` lo dan por implementado y testeado.
2. **UC-74 (buscar postulaciones):** existe en el código y en `estado_uc.md`, pero no figura en `docs/1_requisitos/casos_de_uso.md` (la numeración salta de UC-73 a UC-75).
3. **UC-03 (registro):** el caso de uso pide nombre, apellido, correo, contraseña y fecha de nacimiento; la interfaz pide además DNI y género, y exige mayoría de edad.
4. **UC-07 (baja de cuenta):** el caso de uso fija 30 días; la implementación usa un plazo configurable (30 por defecto) desde el Panel de Administración.
5. **UC-04 (deshabilitar usuario):** el caso de uso indica notificación por correo; la implementación notifica en la aplicación y el envío por correo depende de la prioridad y de la configuración del sistema.
6. **Catálogo de permisos:** el esquema canónico define 12 permisos globales, pero el código y las migraciones usan además permisos de tipos de contrato (`VER_TIPOS_CONTRATO`, `CREAR_TIPO_CONTRATO`, `MODIFICAR_TIPO_CONTRATO`, `ELIMINAR_TIPO_CONTRATO`).
7. **Estructura del manual:** la estructura solicitada no contempla contratos, mensajería, calificaciones/reseñas ni colaboraciones automáticas, que sí están implementados y son accesibles.
8. **Publicaciones:** los casos de uso UC-19 a UC-22 y UC-61 a UC-66 están implementados, pero la estructura solicitada solo menciona “Conexiones y publicaciones (solo la parte implementada)”.
9. **Notificaciones:** no tienen casos de uso numerados en el catálogo, aunque son un módulo completo con tres bandejas.

---

## 10. Validación y decisiones de redacción

1. Se incluyeron capítulos complementarios para **Contratos**, **Mensajería**, **Calificaciones y reseñas** y **Colaboraciones automáticas**.
2. Las **publicaciones** se documentaron con el mismo nivel de detalle que los proyectos dentro del capítulo 8.
3. Se documentaron solo las pantallas de administración navegables. **Búsqueda de usuarios** queda fuera; **Proyectos** se incluye por el enlace del Dashboard.
4. No se incluyeron defectos visibles en los pasos del manual.
5. La antigua pantalla **Reportes** fue eliminada: el Panel de Administración solo tiene **Reportes de perfil** y **Reportes de publicaciones**.
6. La sección **Participaciones** ya no existe.
7. No se documentan funciones no previstas (recuperación de contraseña, cambio de correo, DNI o contraseña).
