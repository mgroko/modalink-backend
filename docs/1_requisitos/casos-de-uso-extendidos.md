# ModaLink — Casos de Uso Extendidos

> Fuente: TF - 2026 - Requerimientos - UC Extendidos.pdf (Iteración 01, Roko María Guillermina, Versión 01.00)
> Total: 69 casos de uso (UC-01 a UC-69).
> Formato pensado para ser parseado por agentes: cada UC sigue el mismo esquema de campos.

## Índice

| UC | Nombre | Actor(es) |
|----|--------|-----------|
| UC-01 | Iniciar sesión | Usuario / Administrador |
| UC-02 | Cerrar sesión | Usuario / Administrador |
| UC-03 | Registrarse | Usuario |
| UC-04 | Deshabilitar usuario | Administrador |
| UC-05 | Habilitar usuario | Administrador |
| UC-06 | Buscar usuario | Administrador |
| UC-07 | Solicitar baja en el sistema | Usuario |
| UC-08 | Modificar datos personales | Usuario |
| UC-09 | Autenticar mediante Google OAuth | Usuario, Google OAuth |
| UC-10 | Crear perfil | Usuario |
| UC-11 | Editar perfil | Usuario |
| UC-12 | Eliminar perfil | Usuario |
| UC-13 | Cambiar perfil activo | Usuario |
| UC-14 | Ver perfil | Usuario |
| UC-15 | Reportar perfil | Usuario |
| UC-16 | Buscar perfil | Usuario / Director de proyecto |
| UC-17 | Asignar en calendario día disponible | Usuario |
| UC-18 | Asignar en calendario día no disponible | Usuario |
| UC-19 | Alta publicación | Usuario |
| UC-20 | Baja publicación | Usuario |
| UC-21 | Modificar publicación | Usuario |
| UC-22 | Buscar publicación | Usuario |
| UC-23 | Configurar términos y condiciones | Usuario |
| UC-24 | Crear proyecto | Usuario |
| UC-25 | Publicar proyecto | Director de proyecto |
| UC-26 | Dar de alta postulación a proyecto | Usuario |
| UC-27 | Dar de baja postulación a proyecto | Usuario |
| UC-28 | Aceptar solicitud de incorporación | Usuario |
| UC-29 | Buscar proyecto | Usuario / Director de proyecto / Miembro de proyecto |
| UC-30 | Buscar invitaciones | Usuario / Director de proyecto |
| UC-31 | Invitar a proyecto | Director de proyecto |
| UC-32 | Eliminar invitación a proyecto | Director de proyecto |
| UC-33 | Visualizar cronograma de proyecto | Director de proyecto / Miembro de proyecto |
| UC-34 | Gestionar postulaciones | Director de proyecto |
| UC-35 | Cancelar proyecto | Director de proyecto |
| UC-36 | Modificar proyecto | Director de proyecto |
| UC-37 | Finalizar proyecto | Director de proyecto |
| UC-38 | Eliminar integrante | Director de proyecto |
| UC-39 | Generar contrato | Director de proyecto |
| UC-40 | Darse de baja de proyecto | Miembro de proyecto |
| UC-41 | Enviar mensaje a chat grupal | Director de proyecto / Miembro de proyecto |
| UC-42 | Enviar mensaje privado | Usuario |
| UC-43 | Enviar solicitud de conexión | Usuario |
| UC-44 | Eliminar conexión | Usuario |
| UC-45 | Gestionar solicitudes de conexión | Usuario |
| UC-46 | Buscar solicitudes de conexión | Usuario |
| UC-47 | Gestionar habilidades | Usuario |
| UC-48 | Buscar habilidades | Usuario |
| UC-49 | Crear actividad a planificación | Director de proyecto |
| UC-50 | Modificar actividad de planificación | Director de proyecto |
| UC-51 | Eliminar actividad de planificación | Director de proyecto |
| UC-52 | Buscar actividades | Director de proyecto / Miembro de proyecto |
| UC-53 | Modificar contrato | Director de proyecto |
| UC-54 | Eliminar contrato | Director de proyecto |
| UC-55 | Firmar contrato | Miembro de proyecto |
| UC-56 | Gestionar habilidades del sistema | Administrador |
| UC-57 | Gestionar características técnicas por profesión | Administrador |
| UC-58 | Buscar características técnicas | Administrador / Usuario |
| UC-59 | Buscar profesiones | Administrador / Usuario |
| UC-60 | Confirmar proyecto | Director de proyecto |
| UC-61 | Enviar solicitud de colaboración | Usuario |
| UC-62 | Aceptar solicitud de colaboración | Usuario |
| UC-63 | Rechazar solicitud de colaboración | Usuario |
| UC-64 | Eliminar solicitud de colaboración | Usuario |
| UC-65 | Buscar solicitudes de colaboración | Usuario |
| UC-66 | Interactuar con publicación | Usuario |
| UC-67 | Generar informe de auditoría | Administrador |
| UC-68 | Buscar contrato | Director de proyecto / Miembro de proyecto |
| UC-69 | Reportar publicación | Usuario |

---

## UC-01 — Iniciar sesión

- **Actor:** Usuario / Administrador
- **Objetivos asociados:** OBJ-01, OBJ-02, OBJ-03, OBJ-04, OBJ-05, OBJ-06
- **Requisitos asociados:** IRQ-01
- **Descripción:** El caso de uso inicia cuando el usuario quiere iniciar sesión en el sistema.
- **Precondición:** El usuario debe estar registrado en el sistema y su cuenta debe estar en estado "Habilitado".

**Secuencia normal**
1. El sistema solicita el correo electrónico y la contraseña.
2. El usuario ingresa sus credenciales de inicio.
3. El sistema valida que el correo electrónico exista y que la contraseña coincida.
4. El sistema valida el estado de la cuenta.
5. El sistema otorga el acceso al sistema.

- **Postcondición:** Un usuario registrado obtuvo acceso al sistema con sus credenciales.

**Flujo alternativo**
- 2.1 Si el usuario decide iniciar sesión con Google, véase el UC-09 Autenticar mediante Google OAuth.

**Excepciones**
- Paso 3: Si el nombre de usuario no existe en la base de datos o la contraseña no coincide, vuelve al paso 1 de la secuencia normal o finaliza el caso de uso.
- Paso 4: Si el usuario está deshabilitado, el sistema informa al actor y el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos
- Paso 4: 1 segundo

- **Frecuencia:** 50 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-02 — Cerrar sesión

- **Actor:** Usuario / Administrador
- **Objetivos asociados:** OBJ-01, OBJ-02, OBJ-03, OBJ-04, OBJ-05, OBJ-06
- **Requisitos asociados:** IRQ-01
- **Descripción:** El caso de uso inicia cuando el usuario quiere cerrar sesión en el sistema.
- **Precondición:** El usuario ha iniciado sesión previamente en el sistema.

**Secuencia normal**
1. El actor selecciona la opción "Cerrar sesión".
2. El sistema solicita confirmación.
3. El actor confirma el cierre de sesión.
4. El sistema invalida el token de autenticación y elimina los datos de sesión activa del navegador.

- **Postcondición:** Se cerró la sesión activa del actor.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 50 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-03 — Registrarse

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01, OBJ-02, OBJ-03, OBJ-04, OBJ-05, OBJ-06
- **Requisitos asociados:** IRQ-01
- **Descripción:** Este caso de uso inicia cuando un usuario desea registrarse en el sistema.
- **Precondición:** -

**Secuencia normal**
1. El sistema solicita los siguientes datos: Nombre, Apellido, Correo electrónico, Contraseña, Fecha de nacimiento.
2. El usuario ingresa los datos solicitados.
3. El sistema verifica que no haya otro usuario con el mismo correo electrónico.
4. El sistema da de alta al usuario.

- **Postcondición:** Se dio de alta a un nuevo usuario en el sistema.

**Flujo alternativo**
- 2.1 Si el usuario desea registrarse con Google, véase el UC-09 Autenticar mediante Google OAuth.
- 3.1 Si el correo electrónico devuelto por Google ya existe en la base de datos, el sistema cancela el proceso de registro y notifica que el correo ya está registrado en el sistema. El caso de uso finaliza.

**Excepciones**
- Paso 3: Si ya existe otro usuario con el mismo correo, el sistema informa al actor y vuelve al paso 1 de la secuencia normal o finaliza el caso de uso.
- Paso 2: Si la fecha de nacimiento ingresada por el usuario da menor a 18 años a la fecha actual del sistema, el sistema notifica la excepción y el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 50 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-04 — Deshabilitar usuario

- **Actor:** Administrador
- **Objetivos asociados:** OBJ-05
- **Requisitos asociados:** IRQ-01
- **Descripción:** El caso de uso inicia cuando se va a deshabilitar un usuario del sistema.
- **Precondición:** El usuario a deshabilitar existe en la base de datos y está en estado "Activo". El administrador está registrado en el sistema.

**Secuencia normal**
1. El administrador busca y selecciona al usuario a deshabilitar (vea UC-06 Buscar usuario).
2. El sistema solicita el motivo y la duración.
3. El administrador ingresa los datos solicitados.
4. El sistema solicita confirmación.
5. El administrador confirma la acción.
6. El sistema actualiza el estado del usuario a "Deshabilitado".

- **Postcondición:** El sistema actualiza el estado del usuario de "Activo" a "Deshabilitado". El sistema emite una notificación al correo del usuario avisando el motivo y tiempo de deshabilitación. El sistema bloquea el acceso al usuario y su perfil deja de ser visible para la comunidad.

**Rendimiento**
- Paso 6: 2 segundos

- **Frecuencia:** 2 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-05 — Habilitar usuario

- **Actor:** Administrador
- **Objetivos asociados:** OBJ-05
- **Requisitos asociados:** IRQ-01
- **Descripción:** El caso de uso inicia cuando se va a habilitar nuevamente a un usuario del sistema.
- **Precondición:** El usuario a habilitar existe en la base de datos y está en estado "Deshabilitado". El administrador está registrado en el sistema.

**Secuencia normal**
1. El administrador busca y selecciona al usuario a habilitar (vea UC-06 Buscar usuario).
2. El sistema solicita confirmación.
3. El administrador confirma la acción.
4. El sistema actualiza el estado del usuario a "Activo".

- **Postcondición:** El sistema actualiza el estado del usuario de "Deshabilitado" a "Activo". El sistema emite una notificación al correo del usuario avisando que su cuenta ha sido habilitada nuevamente. El sistema permite el acceso al usuario y su perfil vuelve a ser visible para la comunidad.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-06 — Buscar usuario

- **Actor:** Administrador
- **Objetivos asociados:** OBJ-01
- **Requisitos asociados:** IRQ-01
- **Descripción:** El caso de uso inicia cuando se va a buscar un usuario registrado en el sistema.
- **Precondición:** El actor debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema solicita el ingreso de los criterios de filtrado (Nombre, apellido, correo electrónico, estado).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de usuarios y filtra aquellos que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con los usuarios que cumplen el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 4 segundos

- **Frecuencia:** 60 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-07 — Solicitar baja en el sistema

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01
- **Requisitos asociados:** IRQ-01
- **Descripción:** El caso de uso inicia cuando un usuario solicita la baja de su cuenta del sistema.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El usuario selecciona la opción "Solicitar baja".
2. El sistema solicita confirmación para la baja del sistema.
3. El usuario confirma la baja.
4. El sistema registra una cuenta regresiva de 30 días para realizar la eliminación definitiva de la cuenta.

- **Postcondición:** El sistema cierra la sesión actual. Los perfiles del usuario dejan de ser visibles para la comunidad. El sistema oculta todas las publicaciones independientes* de sus perfiles asociados. El sistema registra una cuenta regresiva de 30 días. El sistema notifica al usuario que tiene 30 días para recuperar sus datos.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** *Publicaciones independientes: se refiere a publicaciones sin colaboradores. Las publicaciones con colaboradores se eliminan únicamente del perfil dado de baja, pero no del perfil del otro profesional. Si el usuario no inicia sesión al sistema tras pasados los 30 días, el sistema da de baja la cuenta definitivamente, eliminando todas las publicaciones independientes de sus perfiles asociados.

---

## UC-08 — Modificar datos personales

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01, OBJ-04, OBJ-05
- **Requisitos asociados:** IRQ-01
- **Descripción:** El caso de uso inicia cuando el usuario desea actualizar su información personal básica en el sistema.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El usuario selecciona la opción de editar sus datos personales.
2. El sistema muestra los datos asociados a su cuenta: Nombre, Apellido, Fecha de nacimiento (día/mes/año), Ubicación (Localidad y provincia).
3. El usuario modifica los campos deseados con la nueva información.
4. El usuario confirma la modificación.
5. El sistema modifica los datos.

- **Postcondición:** Se modificaron los atributos del usuario.

**Excepciones**
- Paso 3: Si el usuario deja campos obligatorios vacíos o ingresa una fecha inválida, el sistema notifica la excepción y vuelve al paso 2 o el caso de uso finaliza.

**Rendimiento**
- Paso 5: 2 segundos

- **Frecuencia:** 15 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-09 — Autenticar mediante Google OAuth

- **Actor:** Usuario, Google OAuth
- **Objetivos asociados:** OBJ-01
- **Requisitos asociados:** IRQ-01
- **Descripción:** El caso de uso comienza cuando el sistema requiere interactuar con el servicio externo de Google OAuth para validar credenciales y permitir el acceso seguro al sistema.
- **Precondición:** -

**Secuencia normal**
1. El sistema redirige la petición de autenticación al servidor de Google OAuth.
2. El sistema externo Google OAuth solicita permisos al usuario.
3. El usuario autoriza el acceso a sus datos básicos.
4. Google OAuth valida las credenciales y envía al sistema un token de autorización junto con los datos del usuario.
5. El sistema recibe el token y los datos del usuario.

- **Postcondición:** Se validó la identidad del usuario a través de Google. Se obtuvo el correo electrónico, nombre y apellido del usuario.

**Excepciones**
- Paso 4: Si el servicio de Google no responde o el token es inválido, el sistema informa el fallo técnico y el caso de uso finaliza.

**Rendimiento**
- Paso 4: 3 segundos

- **Frecuencia:** 70 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-10 — Crear perfil

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01, OBJ-04, OBJ-05, OBJ-06
- **Requisitos asociados:** IRQ-01, IRQ-02
- **Descripción:** El caso de uso inicia cuando el usuario requiere crear un perfil profesional específico.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema y no debe tener un perfil activo de la misma profesión.

**Secuencia normal**
1. El usuario selecciona la opción "Crear nuevo perfil".
2. El sistema solicita datos generales: Nombre artístico, Profesión (ver UC-59 Buscar profesiones).
3. El usuario ingresa los datos generales.
4. El sistema, basándose en la profesión elegida, despliega el formulario de características técnicas correspondientes (ver UC-58 Buscar características técnicas). Luego solicita una biografía.
5. El usuario completa el formulario.
6. El sistema da de alta el nuevo perfil.

- **Postcondición:** Se creó un perfil profesional y se vinculó a la cuenta del usuario.

**Excepciones**
- Paso 5: Si el usuario deja campos obligatorios vacíos, el sistema notifica la excepción y vuelve al paso 3 o el caso de uso finaliza.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 25 veces/día
- **Estabilidad:** media
- **Comentarios:** En esta primera versión del sistema, las profesiones disponibles son: fotógrafo, modelo, diseñador de moda, productor de moda, maquillador, estilista de cabello y estilista de imagen.

---

## UC-11 — Editar perfil

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01, OBJ-04, OBJ-05, OBJ-06
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando un usuario quiere editar los datos de su perfil profesional.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema y poseer al menos un perfil profesional creado previamente.

**Secuencia normal**
1. El usuario selecciona el perfil que desea editar (ver UC-15... nota: en el original se referencia "UC-15 Buscar perfil", ver comentario de discrepancia en UC-16).
2. El sistema recupera y muestra los datos actuales del perfil: Nombre artístico, Biografía profesional, Características técnicas de la profesión, Foto de perfil.
3. El usuario modifica los campos deseados con la nueva información.
4. El usuario confirma los cambios.
5. El sistema registra los cambios sobre el perfil.

- **Postcondición:** Los atributos del perfil fueron actualizados.

**Excepciones**
- Paso 4: Si el usuario deja campos obligatorios vacíos, el sistema informa el error y vuelve al paso 2.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-12 — Eliminar perfil

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01, OBJ-04, OBJ-05, OBJ-06
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando un usuario quiere dar de baja un perfil asociado a su cuenta.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El usuario selecciona el perfil que desea eliminar (ver UC-15... referencia a "Buscar perfil").
2. El sistema solicita confirmación para la eliminación del perfil.
3. El usuario confirma la acción.
4. El sistema registra una cuenta regresiva de 30 días para realizar la eliminación definitiva del perfil.

- **Postcondición:** El perfil del usuario deja de ser visible para la comunidad. El sistema oculta todas las publicaciones independientes del perfil. El sistema registra una cuenta regresiva de 30 días. El sistema notifica al usuario que tiene 30 días para activar el perfil.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** Si el usuario no activa el perfil pasados los 30 días, el sistema da de baja el perfil definitivamente, eliminando todas sus publicaciones independientes.

---

## UC-13 — Cambiar perfil activo

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando el usuario quiere alternar entre los perfiles vinculados a su cuenta.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema y tener más de un perfil.

**Secuencia normal**
1. El sistema recupera y lista todos los perfiles asociados a la cuenta del usuario (ver UC-15/16 Buscar perfil).
2. El usuario selecciona el perfil que desea activar.
3. El sistema actualiza el contexto de la sesión con los datos del nuevo perfil.

- **Postcondición:** El sistema actualizó el perfil activo del usuario.

**Rendimiento**
- Paso 1: 2 segundos

- **Frecuencia:** 10 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-14 — Ver perfil

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01, OBJ-04, OBJ-05, OBJ-06
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando el actor quiere ver un perfil.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El usuario localiza el perfil que desea ver (ver UC-15/16 Buscar perfil).
2. El usuario selecciona el perfil para ver el detalle.
3. El sistema recupera del repositorio toda la información del perfil seleccionado.
4. El sistema muestra el perfil completo.

- **Salida:** El sistema muestra el perfil completo del perfil seleccionado.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-15 — Reportar perfil

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-05
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso empieza cuando un usuario quiere reportar el perfil de otro usuario.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El usuario localiza el perfil que desea reportar (ver UC-16 Buscar perfil).
2. El sistema despliega un formulario solicitando: Motivo del reporte (categorías predefinidas: spam, contenido inapropiado, identidad falsa, otro), Descripción detallada (opcional).
3. El usuario selecciona el motivo, completa descripción y envía el reporte.
4. El sistema registra el reporte asociado al perfil y genera una alerta de revisión para el administrador.

- **Postcondición:** Se registró un reporte asociado a un perfil, con fecha, hora y autor del reporte. El reporte registrado queda en estado "Pendiente de revisión" por el administrador.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 2 veces/día
- **Estabilidad:** media
- **Comentarios:** -

> **Nota de consistencia:** el documento original referencia indistintamente "UC-15" y "UC-16" para "Buscar perfil" en varios casos de uso (UC-11, UC-12, UC-13, UC-14, UC-21). El UC formalmente numerado como "Buscar perfil" es **UC-16**; posiblemente sea un error de numeración en el documento fuente que conviene validar con el equipo antes de generar código o trazabilidad automática.

---

## UC-16 — Buscar perfil

- **Actor:** Usuario / Director de proyecto
- **Objetivos asociados:** OBJ-01, OBJ-04, OBJ-05, OBJ-06
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando se va a buscar un perfil de usuario registrado en el sistema.
- **Precondición:** El actor debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema solicita el ingreso de los criterios de filtrado (Nombre, profesión (ver UC-59 Buscar profesiones), apellido, nombre artístico, características técnicas, habilidades).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de perfiles y filtra aquellos que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con los perfiles que cumplen el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 50 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-17 — Asignar en calendario día disponible

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01, OBJ-03, OBJ-04
- **Requisitos asociados:** IRQ-05
- **Descripción:** El caso de uso inicia cuando el usuario quiere asignar en su calendario un día u horario disponible.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El usuario selecciona en el calendario un bloque de tiempo (rango de fechas, día u horario) que se encuentra actualmente en estado "No disponible".
2. El usuario selecciona la opción "Marcar como disponible".
3. El sistema elimina el bloqueo y actualiza el bloque de tiempo al estado "Disponible".

- **Postcondición:** Se actualizó el calendario del usuario, dejando disponible el bloque de tiempo (fecha u horarios).

**Excepciones**
- Paso 2: Si el usuario seleccionó alguna fecha o rango horario donde tiene asignada una actividad de un proyecto activo, el sistema notifica al usuario que no puede marcar como disponible un horario comprometido con una actividad y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 10 veces/día
- **Estabilidad:** media
- **Comentarios:** Cuando se habla de "Proyecto activo" se trata sobre proyectos gestionados dentro de la plataforma.

---

## UC-18 — Asignar en calendario día no disponible

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01, OBJ-03, OBJ-04
- **Requisitos asociados:** IRQ-05
- **Descripción:** El caso de uso inicia cuando el usuario quiere asignar en su calendario un día u horario no disponible.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El usuario selecciona en el calendario un bloque de tiempo (rango de fechas, día u horario) que se encuentra actualmente en estado "Disponible".
2. El usuario selecciona la opción "Marcar como no disponible".
3. El sistema solicita de forma opcional el motivo/etiqueta del día no disponible (Vacaciones, compromiso personal, etc.).
4. El usuario ingresa los datos solicitados y confirma la asignación.
5. El sistema registra los datos y actualiza el calendario.

- **Postcondición:** Se registraron días no disponibles en el calendario del usuario, bloqueando esos días para la recepción de ofertas de proyectos.

**Excepciones**
- Paso 2: Si el bloque de tiempo seleccionado por el usuario cuenta con una actividad planificada en un proyecto activo, el sistema informa al usuario que el periodo ya se encuentra bloqueado automáticamente por dicha actividad, omitiendo la creación de un registro duplicado. Se vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 5: 2 segundos

- **Frecuencia:** 10 veces/día
- **Estabilidad:** media
- **Comentarios:** En ModaLink, los días están disponibles por defecto.

---

## UC-19 — Alta publicación

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-05
- **Requisitos asociados:** IRQ-04
- **Descripción:** El caso de uso inicia cuando un usuario quiere realizar una publicación en su perfil.
- **Precondición:** El usuario inició sesión en el sistema y tiene al menos un perfil.

**Secuencia normal**
1. El sistema despliega el formulario de carga solicitando de manera obligatoria: Archivos multimedia, Título de la publicación. Y de forma opcional: Descripción de la publicación, Ubicación, Proyecto asociado (ver UC-29 Buscar proyecto), Invitar colaboradores (ver UC-61 Enviar solicitud de colaboración).
2. El usuario carga el archivo, el título de la publicación y la información opcional. Luego confirma la publicación.
3. El sistema registra la publicación y la hace visible en el portfolio del perfil.

- **Postcondición:** Se registró una nueva publicación en el perfil del usuario. Se vinculó una publicación a un proyecto (si corresponde).

**Excepciones**
- Paso 2: Si el usuario intenta cargar un archivo no válido o deja el título vacío, el sistema notifica del error y vuelve al paso 1, manteniendo los datos previamente cargados.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 30 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-20 — Baja publicación

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-05
- **Requisitos asociados:** IRQ-04
- **Descripción:** El caso de uso inicia cuando el usuario quiere eliminar de forma permanente una publicación de su perfil.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El usuario selecciona la publicación que desea eliminar (ver UC-22 Buscar publicación).
2. El sistema solicita confirmación para la eliminación de la publicación.
3. El usuario confirma la acción.
4. El sistema elimina la publicación del perfil.

- **Postcondición:** El sistema elimina la publicación y los archivos multimedia asociados, dejando de estar disponibles para la comunidad.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** Si la publicación es colaborativa, se elimina del perfil del usuario pero permanece activa en el perfil de los colaboradores.

---

## UC-21 — Modificar publicación

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-05
- **Requisitos asociados:** IRQ-04
- **Descripción:** El caso de uso inicia cuando el usuario desea modificar los datos de una publicación de su perfil.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema y tener al menos una publicación en su perfil.

**Secuencia normal**
1. El usuario selecciona la publicación que desea modificar (referencia en el original a "UC-15 Buscar publicación", ver nota de consistencia — el UC correcto es UC-22 Buscar publicación).
2. El sistema recupera y muestra la información de la publicación: Título, Descripción, Colaboradores (si corresponde), Proyecto asociado (si corresponde). El sistema permite modificar: Título, Descripción, Proyecto asociado (si corresponde, ver UC-29 Buscar proyecto), Invitar un colaborador (si corresponde, ver UC-61 Enviar solicitud de colaboración).
3. El usuario modifica los campos deseados con la nueva información.
4. El usuario confirma los cambios.
5. El sistema registra los cambios sobre el perfil.

- **Postcondición:** Los atributos de la publicación fueron actualizados.

**Excepciones**
- Paso 4: Si el usuario deja campos obligatorios vacíos, el sistema informa el error y vuelve al paso 2.

**Rendimiento**
- Paso 2: 2 segundos
- Paso 5: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-22 — Buscar publicación

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-05
- **Requisitos asociados:** IRQ-04
- **Descripción:** El caso de uso inicia cuando se va a buscar una publicación de un perfil.
- **Precondición:** El actor debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema solicita el ingreso de los criterios de filtrado (fecha de publicación, proyecto asociado (si corresponde), título y descripción).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de publicaciones del perfil y filtra aquellas que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado de las publicaciones que cumplen con el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 25 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-23 — Configurar términos y condiciones

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-05
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando el usuario quiere configurar los términos y condiciones de su perfil.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema recupera y muestra el texto actual de los términos y condiciones (si ya existen) o una plantilla base del sistema.
2. El usuario redacta o modifica el texto en el editor provisto.
3. El usuario guarda los cambios.
4. El sistema asocia los términos y condiciones al perfil del usuario.

- **Postcondición:** Se asociaron los términos y condiciones al perfil.

**Excepciones**
- Paso 3: Si el texto ingresado está vacío o supera el límite de caracteres permitido, el sistema notifica al usuario y vuelve al paso 2 (permitiendo al usuario corregir el contenido sin perder los cambios previos).

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 3 veces/día
- **Estabilidad:** media
- **Comentarios:** El sistema ofrece una plantilla con términos y condiciones genéricos como base en cada perfil.

---

## UC-24 — Crear proyecto

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03
- **Descripción:** El caso de uso inicia cuando el usuario desea crear un proyecto profesional.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema despliega un formulario solicitando información básica del proyecto. Campos obligatorios: Nombre del proyecto, Descripción del proyecto, Privacidad del proyecto (público o privado), Fecha de inicio. Campos opcionales: Ubicación, Requerimientos de personal (cantidad, tipo de profesionales y características técnicas), Material de inspiración / Moodboard, Objetivos del proyecto, Fecha de entrega final estipulada.
2. El usuario ingresa la información solicitada.
3. El sistema crea el proyecto en estado "Borrador" y asocia al perfil de usuario como "Director de proyecto".

- **Postcondición:** Se dio de alta un proyecto con estado "Borrador". El perfil de usuario adquiere los permisos y el rol de "Director de proyecto" sobre el proyecto creado.

**Excepciones**
- Paso 2: Si el usuario deja campos obligatorios vacíos, el sistema notifica la excepción y vuelve al paso 1 o el caso de uso finaliza. Si el nombre ingresado es igual al nombre de otro proyecto asociado al perfil, el sistema notifica la excepción y vuelve al paso 1 o el caso de uso finaliza. Si la fecha de fin es menor a la fecha de inicio, el sistema notifica la excepción y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 100 veces/día
- **Estabilidad:** media
- **Comentarios:** Al crear un proyecto, los únicos campos obligatorios son el nombre del proyecto, su descripción, privacidad de proyecto y fecha de inicio estipulada, dado que permanece como borrador. Véase que en UC-25 Publicar proyecto todos los campos se vuelven obligatorios.

---

## UC-25 — Publicar proyecto

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03
- **Descripción:** El caso de uso inicia cuando el director de proyecto quiere publicar un proyecto en estado "Borrador".
- **Precondición:** El usuario inició sesión en el sistema. El proyecto tiene actividades en la planificación. El director de proyecto cuenta con disponibilidad en su calendario personal para los bloques de tiempo y fechas estipulados en las actividades planificadas del proyecto.

**Secuencia normal**
1. El actor selecciona el proyecto que desea publicar (ver UC-29 Buscar proyecto).
2. El sistema verifica que toda información básica esté completada y solicita confirmación.
3. El actor confirma la acción.
4. El sistema actualiza el estado del proyecto a "Publicado" y lo hace visible para la comunidad.

- **Postcondición:** El estado del proyecto se actualizó de "Borrador" a "Publicado", haciéndolo visible para la comunidad.

**Excepciones**
- Paso 2: Si el proyecto tiene incompleta la información básica, el sistema informa al actor y el caso de uso finaliza.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 40 veces/día
- **Estabilidad:** media
- **Comentarios:** La visibilidad dependerá de la privacidad del proyecto: 1. Público: visible para cualquier usuario de la plataforma. 2. Privado: visible únicamente para la red de contactos del usuario.

---

## UC-26 — Dar de alta postulación a proyecto

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-02
- **Requisitos asociados:** IRQ-02, IRQ-03
- **Descripción:** El caso de uso inicia cuando un usuario desea postularse a un proyecto.
- **Precondición:** El usuario inició sesión en el sistema.

**Secuencia normal**
1. El usuario selecciona el proyecto del que quiere dar de alta la postulación (ver UC-29 Buscar proyecto).
2. El sistema verifica que el cronograma del proyecto y el calendario del usuario sean compatibles y solicita confirmación.
3. El usuario confirma la acción.
4. El sistema registra un alta de postulación a proyecto.

- **Postcondición:** Se registró un alta de postulación a proyecto. El sistema crea una solicitud de notificación al director de proyecto para que evalúe la postulación.

**Excepciones**
- Paso 2: Si el usuario tiene en su calendario días no disponibles u ocupados, el sistema notifica al usuario que no puede haber solapamiento de actividades y el caso de uso finaliza.

**Rendimiento**
- Paso 2: 2 segundos

- **Frecuencia:** 85 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-27 — Dar de baja postulación a proyecto

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-02
- **Requisitos asociados:** IRQ-02, IRQ-03
- **Descripción:** El caso de uso inicia cuando un usuario quiere dar de baja una postulación de un proyecto.
- **Precondición:** El usuario inició sesión en el sistema.

**Secuencia normal**
1. El usuario selecciona el proyecto del que quiere dar de baja la postulación (ver UC-29 Buscar proyecto).
2. El sistema solicita confirmación.
3. El usuario confirma la acción.
4. El sistema registra una baja de postulación a proyecto.

- **Postcondición:** Se registró la baja de postulación.

- **Frecuencia:** 25 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-28 — Aceptar solicitud de incorporación

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03, IRQ-07
- **Descripción:** El caso de uso inicia cuando el usuario quiere aceptar una invitación a proyecto recibida.
- **Precondición:** El usuario inició sesión en el sistema y posee al menos una invitación a proyecto activa.

**Secuencia normal**
1. El usuario selecciona la invitación a proyecto que quiere aceptar (ver UC-30 Buscar invitaciones).
2. El sistema recupera la información del proyecto asociado a la invitación (ver UC-29 Buscar proyecto). Luego despliega la información de la invitación junto con la información del proyecto. Finalmente solicita confirmación de incorporación.
3. El usuario confirma la acción.
4. El sistema registra una incorporación al proyecto asociado con la invitación.

- **Postcondición:** Se registró la incorporación del usuario como miembro del proyecto. Se actualizaron los requerimientos de personal asociados al proyecto. El sistema crea una solicitud de notificación al director de proyecto informando sobre la incorporación.

**Excepciones**
- Paso 2: Si la invitación no es válida (requerimiento de personal ya satisfecho, fue eliminada o el proyecto cancelado), el sistema notifica al usuario y el caso de uso finaliza.

**Rendimiento**
- Paso 2: 2 segundos

- **Frecuencia:** 35 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-29 — Buscar proyecto

- **Actor:** Usuario / Director de proyecto / Miembro de proyecto
- **Objetivos asociados:** OBJ-02
- **Requisitos asociados:** IRQ-03
- **Descripción:** El caso de uso inicia cuando se va a buscar un proyecto.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema solicita el ingreso de al menos un criterio de filtrado (Nombre, estado de proyecto, director de proyecto).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de proyectos y filtra aquellos que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con los proyectos que cumplen con el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 100 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-30 — Buscar invitaciones

- **Actor:** Usuario / Director de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03, IRQ-07
- **Descripción:** El caso de uso inicia cuando se van a buscar solicitudes de unión a proyectos recibidas o enviadas por un perfil de usuario.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema. El usuario debe haber enviado o recibido al menos una invitación.

**Secuencia normal**
1. El sistema solicita el ingreso de los criterios de filtrado (Nombre de proyecto, fecha de envío o datos del perfil remitente o destinatario (si aplica)).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de invitaciones y filtra aquellas que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con las invitaciones que cumplen con el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 70 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-31 — Invitar a proyecto

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-02, IRQ-03, IRQ-07
- **Descripción:** El caso de uso inicia cuando el director de proyecto quiere invitar a un usuario a su proyecto.
- **Precondición:** El usuario inició sesión en el sistema y tiene al menos un proyecto con estado "Publicado".

**Secuencia normal**
1. El actor selecciona el proyecto del cual quiere generar una invitación (ver UC-29 Buscar proyecto).
2. El sistema despliega la información básica del proyecto.
3. El actor selecciona el perfil destinatario de la invitación (ver UC-16 Buscar perfil).
4. El sistema despliega la información del perfil y de forma opcional solicita un mensaje personalizado junto con la invitación.
5. El actor introduce el mensaje (si aplica) y confirma la acción.
6. El sistema registra la nueva invitación y notifica a los involucrados.

- **Postcondición:** Se registró una nueva invitación al proyecto. El sistema crea una solicitud de notificación al perfil destinatario sobre la invitación al proyecto.

**Excepciones**
- Paso 5: Si el perfil seleccionado ya tiene una invitación activa para el mismo proyecto, el sistema notifica al actor y vuelve al paso 3 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 50 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-32 — Eliminar invitación a proyecto

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-02, IRQ-03, IRQ-07
- **Descripción:** El caso de uso inicia cuando el director de proyecto quiere eliminar una invitación de su proyecto.
- **Precondición:** El usuario inició sesión en el sistema, tiene al menos un proyecto y este cuenta con al menos una invitación enviada que aún permanece en estado "Pendiente".

**Secuencia normal**
1. El actor selecciona la invitación que quiere eliminar (ver UC-30 Buscar invitaciones).
2. El sistema despliega la información de la invitación y solicita confirmación.
3. El usuario confirma la acción.
4. El sistema elimina el registro de la invitación.

- **Postcondición:** Se eliminó una invitación al proyecto.

**Excepciones**
- Paso 3: Si la invitación no está en estado "Pendiente", el sistema notifica al actor y el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 20 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-33 — Visualizar cronograma de proyecto

- **Actor:** Director de proyecto / Miembro de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03, IRQ-08
- **Descripción:** El caso de uso inicia cuando el actor quiere visualizar el cronograma del proyecto calculado en base a las actividades de la planificación. El director del proyecto accede a esta vista para evaluar la viabilidad de los plazos y detectar desbordamientos o conflictos de recursos en el proyecto.
- **Precondición:** El usuario ha iniciado sesión en el sistema y tiene o es miembro de al menos un proyecto en estado "Borrador", "Publicado" o "Confirmado".

**Secuencia normal**
1. El actor selecciona el proyecto del cual quiere ver el cronograma (ver UC-29 Buscar proyecto). El sistema recupera las actividades asociadas al mismo (ver UC-52 Buscar actividades).
2. El sistema despliega el cronograma del proyecto mostrando información importante para la toma de decisiones.

- **Salida:** El sistema despliega el cronograma del proyecto seleccionado.

**Rendimiento**
- Paso 2: 2 segundos

- **Frecuencia:** 80 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-34 — Gestionar postulaciones

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02
- **Requisitos asociados:** IRQ-03
- **Descripción:** El caso de uso inicia cuando el director de proyecto quiere gestionar las postulaciones de un proyecto. El sistema le permitirá aceptar una postulación, incorporando al proyecto a un perfil, o rechazar la postulación.
- **Precondición:** El usuario ha iniciado sesión en el sistema, tiene al menos un proyecto en estado "Publicado".

**Secuencia normal**
1. El actor selecciona el proyecto del cual quiere gestionar las postulaciones (ver UC-29 Buscar proyecto).
2. El sistema despliega las postulaciones del proyecto y junto a cada una muestra dos opciones: "Aceptar postulación", "Rechazar postulación".
3. El actor selecciona "Aceptar postulación".
4. El sistema solicita confirmación para la incorporación al proyecto.
5. El actor confirma la acción.

- **Postcondición:** Se incorporó un miembro al proyecto. Se actualizó el requerimiento de personal del proyecto. El sistema crea una solicitud de notificación al perfil informando sobre la incorporación.

**Flujo alternativo**
- 3.1 Si el actor selecciona "Rechazar postulación": 3.1.1 El sistema solicita confirmación. 3.1.2 El actor confirma la acción. 3.1.3 El sistema elimina la postulación y notifica al perfil que su postulación fue rechazada.

- **Frecuencia:** 75 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-35 — Cancelar proyecto

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03, IRQ-08
- **Descripción:** El sistema permite al actor finalizar el proyecto de forma prematura, notificando a los participantes involucrados y restringiendo nuevas acciones sobre el mismo.
- **Precondición:** El actor inició sesión en el sistema y posee un proyecto en estado "Publicado" o "Confirmado".

**Secuencia normal**
1. El actor selecciona el proyecto que quiere cancelar (ver UC-29 Buscar proyecto).
2. El sistema despliega la información básica del proyecto y solicita de manera obligatoria el ingreso de un motivo de cancelación.
3. El actor ingresa el motivo de cancelación y confirma la acción.
4. El sistema actualiza el estado del proyecto a "Cancelado", interrumpe todas las actividades pendientes de la planificación.

- **Postcondición:** El estado del proyecto se actualizó a "Cancelado". Los bloqueos de agenda por la planificación del proyecto se eliminaron para cada uno de los participantes. El sistema generó una notificación para cada integrante del equipo informando la cancelación y el motivo.

**Excepciones**
- Paso 3: Si el actor no ingresa el motivo de cancelación, el sistema informa al actor y vuelve al paso 2 o el caso de uso finaliza.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-36 — Modificar proyecto

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02
- **Requisitos asociados:** IRQ-03
- **Descripción:** El caso de uso inicia cuando el director de proyecto quiere modificar los datos de un proyecto. El sistema le permite actualizar la información descriptiva, los requerimientos, privacidad, ubicación, objetivos y moodboard de un proyecto ya creado para mantener los datos vigentes.
- **Precondición:** El actor inició sesión en el sistema y tiene al menos un proyecto en estado "Borrador", "Publicado" o "Confirmado".

**Secuencia normal**
1. El actor selecciona el proyecto que quiere modificar (ver UC-29 Buscar proyecto).
2. El sistema despliega un formulario cargando los datos actuales del proyecto. Datos que el actor NO puede modificar: Nombre del proyecto, cualquier requerimiento de personal ya satisfecho. Datos que SÍ puede modificar: Descripción, Privacidad (público, privado u oculto), Ubicación, Requerimientos de personal (Profesión, aptitudes y cantidad por profesión), Moodboard, Objetivos, Fecha de entrega final estipulada.
3. El actor realiza las modificaciones deseadas y confirma los cambios.
4. El sistema actualiza el proyecto con los nuevos datos ingresados.

- **Postcondición:** Se actualizaron los atributos de un proyecto. El sistema notifica a todos los participantes sobre los cambios (si corresponde).

**Excepciones**
- Paso 3: Si el usuario deja campos obligatorios vacíos, el sistema informa el error y vuelve al paso 2 o el caso de uso finaliza.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 30 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-37 — Finalizar proyecto

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02
- **Requisitos asociados:** IRQ-03
- **Descripción:** El caso de uso inicia cuando se va a finalizar un proyecto con estado Confirmado. El sistema deberá cambiar el estado del proyecto a Finalizado, notificando a todos los participantes y solicitándoles que califiquen al equipo.
- **Precondición:** El proyecto está en estado "Confirmado". La fecha de finalización estipulada en el cronograma del proyecto es menor o igual a la fecha actual del sistema.

**Secuencia normal**
1. El actor selecciona el proyecto que quiere finalizar (ver UC-29 Buscar proyecto).
2. El sistema solicita al director de proyecto que califique al equipo.
3. El director de proyecto califica el desempeño de cada uno de los participantes.
4. El sistema ofrece la opción de subir imágenes del resultado del proyecto.
5. El director de proyecto confirma la finalización del proyecto.
6. El sistema actualiza el estado del proyecto a Finalizado.

- **Postcondición:** El sistema, por cada participante, crea una solicitud para notificar que califiquen al resto del equipo. El sistema cambia el estado del proyecto de "Confirmado" a "Finalizado" con la fecha actual del sistema. El proyecto finalizado se indexa automáticamente en el historial de proyectos de los perfiles de todos los integrantes.

**Flujo alternativo**
- 3.1 Si el director de proyecto decide subir imágenes del resultado del proyecto, véase UC-19 Alta publicación. Luego el caso de uso continúa desde el paso 5.

**Excepciones**
- Paso 1: Si el director de proyecto no califica al equipo completo, el sistema informa que es obligatorio para continuar.

**Rendimiento**
- Paso 6: 2 segundos

- **Frecuencia:** 55 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-38 — Eliminar integrante

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03, IRQ-08
- **Descripción:** El caso de uso inicia cuando el director de proyecto desea eliminar un integrante del proyecto.
- **Precondición:** El actor inició sesión en el sistema, el proyecto se encuentra en estado "Publicado" o "Confirmado" y posee al menos un miembro de proyecto.

**Secuencia normal**
1. El actor selecciona el proyecto del que quiere eliminar un integrante (ver UC-29 Buscar proyecto).
2. El sistema despliega información básica del proyecto.
3. El actor selecciona el perfil del integrante que quiere eliminar (ver UC-16 Buscar perfil).
4. El sistema despliega la información del perfil y de forma obligatoria solicita un motivo de eliminación.
5. El actor ingresa un motivo y confirma la acción.
6. El sistema registra la baja y actualiza el proyecto.

- **Postcondición:** Se actualizó el registro de miembros del proyecto. Se eliminó la asignación del profesional como recurso en las actividades que correspondan, reabriendo la vacante correspondiente en los requerimientos de la actividad del proyecto. El sistema generó una notificación al perfil del profesional eliminado para informar sobre su desvinculación del proyecto.

**Rendimiento**
- Paso 6: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-39 — Generar contrato

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-03, OBJ-05
- **Requisitos asociados:** IRQ-09
- **Descripción:** El caso de uso inicia cuando el director de proyecto quiere generar un contrato para su proyecto.
- **Precondición:** El actor inició sesión en el sistema, el proyecto se encuentra en estado "Publicado" o "Confirmado" y posee al menos un miembro de proyecto.

**Secuencia normal**
1. El actor selecciona el proyecto del que quiere generar un contrato (ver UC-68 Buscar contrato).
2. El sistema despliega información básica del proyecto.
3. El sistema solicita los siguientes datos para el contrato: Tipo de acuerdo (cesión de derechos de imagen, préstamo de indumentaria/materiales, entre otros), Miembros del proyecto destinatarios.
4. El actor selecciona el tipo de acuerdo, selecciona los integrantes con los que quiere generar el contrato y confirma la acción.
5. El sistema recupera los datos específicos del proyecto, junto con los datos personales de todas las partes y según el tipo de acuerdo consolida un borrador del texto legal. El sistema ofrece al actor añadir cláusulas adicionales de forma opcional.
6. El actor añade cláusulas adicionales y confirma la emisión del contrato.
7. El sistema valida el texto y registra el contrato asociado al proyecto en estado "Pendiente de firma".

- **Postcondición:** Se registró un contrato con estado "Pendiente de firma" asociado al proyecto. El sistema, por cada participante asociado, crea una solicitud para notificar que se emitió un contrato y que está pendiente de firma.

**Excepciones**
- Paso 7: Si algún participante seleccionado ya posee un contrato emitido y activo del mismo tipo para este proyecto, el sistema notifica la excepción y vuelve al paso 3 o el caso de uso finaliza.

**Rendimiento**
- Paso 7: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-40 — Darse de baja de proyecto

- **Actor:** Miembro de proyecto
- **Objetivos asociados:** OBJ-03
- **Requisitos asociados:** IRQ-03, IRQ-08
- **Descripción:** El caso de uso inicia cuando un miembro de proyecto decide darse de baja de un proyecto en estado "Publicado" o "Confirmado".
- **Precondición:** El actor inició sesión en el sistema y es participante de un proyecto en estado "Publicado" o "Confirmado".

**Secuencia normal**
1. El actor selecciona el proyecto del que se quiere dar de baja (ver UC-29 Buscar proyecto).
2. El sistema despliega la información del proyecto y solicita de manera obligatoria el motivo de la baja.
3. El actor introduce el motivo y confirma la acción.
4. El sistema registra la desvinculación.

- **Postcondición:** Se registró una baja voluntaria y se actualizó el registro de miembros del proyecto. Se actualizó el estado de participación de "Activo" a "Baja voluntaria". Se reabrió la vacante correspondiente en los requerimientos del proyecto. Se eliminó la asignación del profesional como recurso en las actividades (si corresponde). El sistema generó una notificación al perfil del director de proyecto para informar sobre su desvinculación del proyecto.

**Excepciones**
- Paso 3: Si el actor no introduce un motivo, el sistema notifica el error y vuelve al paso 2 o el caso de uso finaliza.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 10 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-41 — Enviar mensaje a chat grupal

- **Actor:** Director de proyecto / Miembro de proyecto
- **Objetivos asociados:** OBJ-03
- **Requisitos asociados:** IRQ-03
- **Descripción:** El caso de uso inicia cuando algún participante del proyecto quiere enviar un mensaje al chat grupal del proyecto.
- **Precondición:** El actor inició sesión en el sistema y es participante o director de un proyecto en estado "Publicado" o "Confirmado".

**Secuencia normal**
1. El actor selecciona el proyecto en el cual quiere mandar un mensaje (ver UC-29 Buscar proyecto).
2. El sistema despliega la interfaz de chat grupal, listando los participantes y mensajes anteriores (si corresponde).
3. El actor redacta un mensaje de texto y lo envía.
4. El sistema valida el mensaje y lo registra.

- **Postcondición:** Se envió un mensaje al chat grupal del proyecto. El sistema, por cada participante del proyecto, envía una notificación avisando del nuevo mensaje.

**Excepciones**
- Paso 4: Si el texto ingresado supera el límite de caracteres permitido, el sistema notifica al usuario y vuelve al paso 2 (permitiendo al usuario corregir el contenido sin perder los cambios previos).

**Rendimiento**
- Paso 4: 1 segundo

- **Frecuencia:** 150 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-42 — Enviar mensaje privado

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-05, OBJ-06
- **Requisitos asociados:** IRQ-06
- **Descripción:** El caso de uso inicia cuando un usuario quiere enviar un mensaje privado a otro usuario registrado en el sistema.
- **Precondición:** El actor inició sesión en el sistema.

**Secuencia normal**
1. El actor localiza al usuario con el que desea comunicarse (ver UC-16 Buscar perfil).
2. El sistema despliega información básica del perfil, la interfaz de chat privado y mensajes anteriores (si corresponde).
3. El actor redacta un mensaje de texto y lo envía.
4. El sistema valida el mensaje y lo registra.

- **Postcondición:** Se envió un mensaje al chat privado entre perfiles. El sistema envía al usuario receptor una notificación avisando del nuevo mensaje.

**Excepciones**
- Paso 4: Si el texto ingresado supera el límite de caracteres permitido, el sistema notifica al usuario y vuelve al paso 2 (permitiendo al usuario corregir el contenido sin perder los cambios previos).

**Rendimiento**
- Paso 4: 1 segundo

- **Frecuencia:** 100 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-43 — Enviar solicitud de conexión

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-06
- **Requisitos asociados:** IRQ-06
- **Descripción:** El caso de uso inicia cuando el usuario desea enviar una solicitud de conexión a un perfil registrado en el sistema para ampliar su red de contactos.
- **Precondición:** El actor inició sesión en el sistema y no tiene una solicitud enviada al mismo perfil.

**Secuencia normal**
1. El actor localiza el perfil del usuario con el que desea conectar (ver UC-16 Buscar perfil).
2. El sistema despliega el perfil del usuario y muestra la opción de conectar con el perfil.
3. El actor confirma la acción.
4. El sistema registra la solicitud de conexión.

- **Postcondición:** Se registró una solicitud de conexión en estado "Pendiente". El sistema crea una notificación al receptor para notificar de la nueva solicitud.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 50 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-44 — Eliminar conexión

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-06
- **Requisitos asociados:** IRQ-06
- **Descripción:** El caso de uso inicia cuando el usuario desea eliminar una conexión de su red de contactos.
- **Precondición:** El actor inició sesión en el sistema y posee al menos una conexión en su red de contactos.

**Secuencia normal**
1. El actor, en su red de contactos, localiza el perfil del usuario que desea eliminar (ver UC-16 Buscar perfil).
2. El sistema despliega información del perfil y solicita confirmación.
3. El usuario confirma la acción.
4. El sistema registra la eliminación y actualiza la red de contactos.

- **Postcondición:** Se registró una desvinculación entre perfiles. Ambos perfiles dejan de visualizarse en la red de contactos de cada uno.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 10 veces/día
- **Estabilidad:** media
- **Comentarios:** Al momento de una desvinculación entre profesionales, el sistema deja de mostrar en el inicio las publicaciones del otro.

---

## UC-45 — Gestionar solicitudes de conexión

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-06
- **Requisitos asociados:** IRQ-06
- **Descripción:** El caso de uso inicia cuando el usuario quiere gestionar sus solicitudes de conexión. El sistema permite al actor procesar las solicitudes de conexión recibidas, brindando la posibilidad de aceptar o rechazar cada solicitud para consolidar su red de contactos profesional.
- **Precondición:** El usuario inició sesión en el sistema y cuenta con al menos una solicitud de conexión en estado "Pendiente" dirigida a su perfil.

**Secuencia normal**
1. El actor localiza la solicitud de conexión que desea gestionar (ver UC-46 Buscar solicitudes de conexión).
2. El sistema despliega la información sobre la solicitud de conexión y permite dos acciones: "Aceptar", "Rechazar".
3. El actor selecciona "Aceptar" y confirma la acción.
4. El sistema registra la nueva conexión entre perfiles.

- **Postcondición:** Se actualizó el estado de la solicitud a "Aceptada". Se registró una nueva relación de conexión con fecha de inicio de conexión igual a la fecha actual del sistema. El sistema envía a ambas partes una notificación informando sobre la nueva conexión.

**Flujo alternativo**
- 3.1 Si el actor selecciona "Rechazar": 3.1.1 El actor confirma la acción. 3.1.2 El sistema registra el rechazo, cambia el estado de la solicitud a "Rechazada" y el caso de uso finaliza.

**Rendimiento**
- Paso 4: 2 segundos
- Paso 3.1.2: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-46 — Buscar solicitudes de conexión

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-06
- **Requisitos asociados:** IRQ-06
- **Descripción:** El caso de uso inicia cuando se van a buscar las solicitudes de conexión asociadas a un perfil de usuario registrado en el sistema.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema solicita el ingreso de los criterios de filtrado (Nombre, apellido, nombre artístico, profesión, fecha de solicitud).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de solicitudes y filtra aquellas que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con las solicitudes de conexión que cumplen el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 25 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-47 — Gestionar habilidades

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01, OBJ-04
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando un usuario desde su perfil quiere gestionar sus habilidades profesionales.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema muestra dos opciones para gestionar las habilidades profesionales: "Agregar habilidad", "Eliminar habilidad".
2. El usuario selecciona "Agregar habilidad".
3. El actor selecciona la habilidad profesional que desea incorporar a su perfil (ver UC-48 Buscar habilidades).
4. El sistema solicita confirmación.
5. El usuario confirma la acción.
6. El sistema asocia la habilidad a su perfil.

- **Postcondición:** Se agregó una nueva habilidad al perfil de usuario.

**Flujo alternativo**
- 2.1 Si el usuario selecciona "Eliminar habilidad": 2.1.1 El actor, dentro de su catálogo de habilidades del perfil, selecciona la habilidad profesional que desea eliminar (ver UC-48 Buscar habilidades). 2.1.2 El sistema solicita confirmación. 2.1.3 El usuario confirma la acción. 2.1.4 El sistema elimina la habilidad de su perfil y el caso de uso finaliza.

**Excepciones**
- Paso 5: Si el usuario ya cuenta con esa habilidad en su perfil, el sistema notifica la excepción (el original no especifica el paso de retorno).

**Rendimiento**
- Paso 6: 2 segundos
- Paso 2.1.4: 2 segundos

- **Frecuencia:** 30 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-48 — Buscar habilidades

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-01, OBJ-04
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando se va a buscar un servicio dentro del catálogo de servicios del perfil.
- **Precondición:** El actor debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema solicita el ingreso de los criterios de filtrado (Nombre, profesión).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de habilidades y filtra aquellos que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con las habilidades que cumplen el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 30 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-49 — Crear actividad a planificación

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03, IRQ-08
- **Descripción:** El caso de uso inicia cuando el director de proyecto desea agregar una nueva actividad a la planificación del proyecto.
- **Precondición:** El actor inició sesión en el sistema y tiene un proyecto en estado "Borrador" o "Publicado".

**Secuencia normal**
1. El actor selecciona el proyecto al cual desea agregar la actividad (ver UC-29 Buscar proyecto).
2. El sistema despliega un formulario de carga solicitando los atributos obligatorios de la actividad: Nombre de la actividad, Descripción, Duración estimada en días/horas. Y de forma opcional: Actividades predecesoras asociadas (ver UC-52 Buscar actividades), Ubicación.
3. El actor completa los campos solicitados y confirma la acción.
4. Mientras exista requerimiento profesional por cargar, se ejecutan los siguientes pasos:
   - 4.a El sistema despliega el formulario para la carga de requerimiento profesional: Profesión (ver UC-59 Buscar profesiones), Habilidades (si corresponde, ver UC-48 Buscar habilidades), Características técnicas específicas (si corresponde, ver UC-58 Buscar características técnicas). Si corresponde, el sistema despliega debajo de este formulario un listado de los miembros del proyecto, permitiendo al actor la asignación directa.
   - 4.b El actor ingresa los datos solicitados y confirma la acción.
   - 4.c El sistema registra el nuevo requerimiento.
5. El actor confirma la creación de la actividad.
6. El sistema registra la nueva actividad con estado "Pendiente".

- **Postcondición:** Se registró una nueva actividad con estado "Pendiente". Se actualizó el requerimiento del proyecto (si corresponde). Se actualizó el cronograma del proyecto con la nueva actividad. El sistema, por cada miembro designado, emite una notificación para informar sobre la nueva actividad. El sistema, por cada miembro designado, asigna en su calendario como no disponible el bloque de tiempo que dura la actividad.

**Excepciones**
- Paso 3: Si el usuario deja campos obligatorios vacíos, el nombre es igual al de otra actividad en la planificación o ingresa una duración menor o igual a cero, el sistema informa el error y vuelve al paso 2 o el caso de uso finaliza.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 60 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-50 — Modificar actividad de planificación

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03, IRQ-08
- **Descripción:** El caso de uso inicia cuando el director de proyecto desea modificar una actividad de la planificación del proyecto.
- **Precondición:** El actor inició sesión en el sistema y tiene un proyecto en estado "Borrador", "Publicado" o "Confirmado" que cuenta con al menos una actividad en la planificación.

**Secuencia normal**
1. El actor selecciona la tarea que desea modificar (ver UC-52 Buscar actividades).
2. El sistema despliega un formulario con la información de la actividad: Nombre, Descripción, Duración estimada en días/horas, Requerimiento profesional necesario (Profesión, Habilidades, Características técnicas específicas), Profesionales asignados (si corresponde), Actividades predecesoras asociadas (si corresponde), Ubicación, Estado.
3. El actor modifica los campos deseados y confirma la acción.
4. El sistema registra los cambios de la actividad.

- **Postcondición:** Se actualizó una actividad. Se actualizó el cronograma del proyecto en base a los cambios de la actividad. El sistema, por cada miembro asignado a la actividad, emite una notificación para informar sobre los cambios. El sistema, por cada miembro designado, asigna en su calendario como no disponible el bloque de tiempo que dura la actividad.

**Flujo alternativo**
- 3.1 Si la modificación del actor genera un retraso que supere la fecha de entrega del proyecto, el sistema registra los cambios, actualiza el estado del cronograma a "Alerta por desborde" y notifica al actor para que se tomen medidas.
- 3.2 Si la modificación del actor genera conflicto con el calendario del profesional asignado como recurso, el sistema registra los cambios, actualiza el estado del cronograma a "Conflicto de recursos" y notifica al actor para que se tomen medidas.

**Excepciones**
- Paso 3: Si el actor deja campos obligatorios vacíos o ingresa una duración menor o igual a cero, el sistema informa el error y vuelve al paso 2 o el caso de uso finaliza.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 30 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-51 — Eliminar actividad de planificación

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03, IRQ-08
- **Descripción:** El caso de uso inicia cuando el director de proyecto desea eliminar una actividad de la planificación del proyecto.
- **Precondición:** El actor inició sesión en el sistema y tiene un proyecto en estado "Borrador", "Publicado" o "Confirmado" que cuenta con al menos una actividad en estado "Pendiente" o "En curso" en la planificación.

**Secuencia normal**
1. El actor selecciona la tarea que desea eliminar (ver UC-52 Buscar actividades).
2. El sistema despliega información básica de la actividad y solicita confirmación.
3. El actor ingresa la razón de la eliminación y confirma la acción.
4. El sistema registra la baja y reordena el cronograma.

- **Postcondición:** Se eliminó una actividad. Se actualizó el cronograma del proyecto. El sistema, por cada miembro designado a la actividad, emite una notificación para informar sobre la baja. Se actualizó la planificación, conectando las actividades predecesoras con las sucesoras de la actividad eliminada. Si la actividad tenía postulaciones activas, el sistema las elimina notificando la razón.

**Flujo alternativo**
- 4.1 Si la actividad tenía miembros asignados en solo esa actividad, el sistema ofrece dos opciones: "Eliminar integrante del proyecto", "Guardar integrante de forma temporal".
- 4.2 El actor selecciona "Eliminar integrante del proyecto": 4.2.1 El sistema solicita un motivo y la confirmación por cada integrante asociado a la actividad. 4.2.2 El actor confirma la acción. Postcondición 4.2: El sistema, por cada miembro designado a la actividad, emite una notificación para informar sobre la baja de la actividad y su eliminación del proyecto.
- 4.3 El actor selecciona "Guardar integrante de forma temporal": 4.3.1 El sistema solicita la confirmación. 4.3.2 El actor confirma la acción. Postcondición 4.3: El sistema, por cada miembro designado a la actividad, emite una notificación para informar sobre la baja de la actividad y su continuidad en el proyecto.

**Excepciones**
- Paso 3: Si la actividad está en estado "Finalizada", el sistema informa al actor que no se puede eliminar una actividad completada y el caso de uso finaliza.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 15 veces/día
- **Estabilidad:** media
- **Comentarios:** Al eliminar una actividad que posee dependencias, el sistema preserva la continuidad del cronograma conectando de forma directa las actividades predecesoras del nodo eliminado con sus sucesoras. En el flujo alternativo 4.1 se hace referencia a aquellos miembros que sólo participaban de esa actividad; si existen miembros que tienen otras actividades designadas en el mismo proyecto, no ocurre la opción de eliminarlos o guardarlos.

---

## UC-52 — Buscar actividades

- **Actor:** Director de proyecto / Miembro de proyecto
- **Objetivos asociados:** OBJ-02
- **Requisitos asociados:** IRQ-03, IRQ-08
- **Descripción:** El caso de uso inicia cuando se va a buscar una actividad dentro de la planificación de un proyecto.
- **Precondición:** El actor debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema solicita el ingreso de los criterios de filtrado (Nombre de la actividad, descripción, requerimiento, duración, estado).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de actividades de la planificación y filtra aquellas que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con las actividades que cumplen el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 40 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-53 — Modificar contrato

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-03, OBJ-05
- **Requisitos asociados:** IRQ-09
- **Descripción:** El caso de uso inicia cuando el director de proyecto quiere modificar un contrato asociado a un proyecto en estado "Pendiente de firma". El sistema permite la modificación del tipo de acuerdo, los miembros destinatarios y luego, en base a las primeras modificaciones, vuelve a generar un borrador en un editor de texto donde el director puede modificar o agregar nuevas cláusulas.
- **Precondición:** El usuario inició sesión en el sistema y el contrato a modificar está en estado "Pendiente de firma".

**Secuencia normal**
1. El actor selecciona el proyecto del que quiere modificar un contrato (ver UC-68 Buscar contrato).
2. El sistema despliega los contratos con estado "Pendiente de firma" asociados al proyecto.
3. El usuario selecciona el contrato a modificar.
4. El sistema recupera los datos específicos del contrato: Tipo de acuerdo, Miembros del proyecto destinatarios.
5. El usuario modifica los datos requeridos y confirma la acción.
6. El sistema recupera los datos específicos del proyecto, junto con los datos personales de todas las partes y según el tipo de acuerdo consolida un borrador del texto legal. El sistema ofrece al actor modificar o añadir cláusulas adicionales de forma opcional.
7. El actor modifica o añade cláusulas adicionales y confirma la modificación del contrato.
8. El sistema valida el texto y registra la modificación del contrato asociado al proyecto.

- **Postcondición:** Se registró la modificación del contrato asociado al proyecto. El sistema, por cada participante asociado, crea una solicitud para notificar que se modificó un contrato y que está pendiente de firma.

**Excepciones**
- Paso 7: Si algún participante seleccionado ya posee un contrato emitido y activo del mismo tipo para este proyecto, el sistema notifica la excepción y vuelve al paso 4 o el caso de uso finaliza.

**Rendimiento**
- Paso 7: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-54 — Eliminar contrato

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-03, OBJ-05
- **Requisitos asociados:** IRQ-09
- **Descripción:** El caso de uso inicia cuando el director de proyecto quiere eliminar un contrato asociado a un proyecto en estado "Pendiente de firma".
- **Precondición:** El usuario inició sesión en el sistema y el contrato a eliminar está en estado "Pendiente de firma".

**Secuencia normal**
1. El actor selecciona el proyecto del que quiere eliminar un contrato (ver UC-68 Buscar contrato).
2. El sistema despliega los contratos con estado "Pendiente de firma" asociados al proyecto.
3. El usuario selecciona el contrato a eliminar y confirma la acción.
4. El sistema registra la baja y finaliza el caso de uso.

- **Postcondición:** Se registró una baja de un contrato asociado al proyecto. El sistema notifica a las partes que el contrato fue eliminado.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-55 — Firmar contrato

- **Actor:** Miembro de proyecto
- **Objetivos asociados:** OBJ-03, OBJ-05
- **Requisitos asociados:** IRQ-09
- **Descripción:** El caso de uso inicia cuando un miembro de proyecto quiere firmar un contrato asociado con su perfil que está en estado "Pendiente de firma".
- **Precondición:** El usuario inició sesión en el sistema y el contrato está en estado "Pendiente de firma".

**Secuencia normal**
1. El actor selecciona el proyecto en el cual tiene asociado el contrato a firmar (ver UC-68 Buscar contrato).
2. El sistema despliega los contratos con estado "Pendiente de firma" asociados al perfil del miembro.
3. El usuario selecciona el contrato a firmar y confirma la acción.
4. El sistema registra la firma y finaliza el caso de uso.

- **Postcondición:** Se registró una firma en el contrato. El estado del contrato pasa de "Pendiente de firma" a "Activo" (si corresponde). El sistema notifica a las partes que el contrato fue firmado con éxito.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 15 veces/día
- **Estabilidad:** media
- **Comentarios:** Se coloca "Si corresponde" en el cambio de estado del contrato debido a que podrían existir contratos con más de una parte, lo que conlleva a que el estado del contrato cambie en el momento en el que todas las partes asociadas firmen.

---

## UC-56 — Gestionar habilidades del sistema

- **Actor:** Administrador
- **Objetivos asociados:** OBJ-01, OBJ-04
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando el administrador del sistema quiere gestionar las habilidades profesionales del sistema. El sistema le permite agregar un nuevo tipo de habilidad, modificar o eliminar una ya existente.
- **Precondición:** El usuario inició sesión en el sistema.

**Secuencia normal**
1. El sistema recupera y despliega todas las habilidades registradas en el sistema (ver UC-48 Buscar habilidades) y expone las siguientes opciones: "Agregar nueva habilidad", "Modificar habilidad", "Eliminar habilidad".
2. El actor selecciona "Agregar nueva habilidad".
3. El sistema despliega un formulario de carga solicitando de forma obligatoria: Nombre de la habilidad. Y opcionalmente: Descripción de la habilidad, Profesión asociada (ver UC-59 Buscar profesiones).
4. El actor completa los campos y confirma la acción.
5. El sistema registra la nueva habilidad al sistema.

- **Postcondición:** Se creó una nueva habilidad en el sistema.

**Flujo alternativo**
- 2.1 Si el actor selecciona "Modificar habilidad": 2.1.1 El actor selecciona la habilidad que quiere modificar (ver UC-48 Buscar habilidades). 2.1.2 El sistema despliega la información de la habilidad: Nombre, Descripción, Profesión asociada. 2.1.3 El actor modifica los campos deseados y confirma la acción. 2.1.4 El sistema registra los cambios. Postcondición 2.1: Se modificó una habilidad del sistema. El sistema, por cada perfil de usuario que tenía la habilidad asociada a su perfil, emite una notificación informando de la modificación.
- 2.2 Si el actor selecciona "Eliminar habilidad": 2.2.1 El actor selecciona la habilidad que quiere eliminar (ver UC-48 Buscar habilidades). 2.2.2 El sistema despliega los datos de la habilidad y solicita confirmación. 2.2.3 El actor confirma la acción. 2.2.4 El sistema registra la baja. Postcondición 2.2: Se dio de baja una habilidad en el sistema. El sistema, por cada perfil de usuario que tenía la habilidad asociada a su perfil, emite una notificación informando de la baja.

**Excepciones**
- Paso 4: Si el actor deja campos obligatorios vacíos o introduce un nombre de habilidad igual al de otra ya existente en el sistema, el sistema notifica la excepción y vuelve al paso 3 o el caso de uso finaliza.
- Paso 2.1.3: Si el actor deja campos obligatorios vacíos o introduce un nombre de habilidad igual al de otra ya existente en el sistema, el sistema notifica la excepción y vuelve al paso 2.1.2 o el caso de uso finaliza.

**Rendimiento**
- Paso 5: 2 segundos
- Paso 2.1.4: 2 segundos
- Paso 2.2.4: 2 segundos

- **Frecuencia:** 10 veces/día
- **Estabilidad:** media
- **Comentarios:** En la profesión asociada se listan las profesiones base: fotógrafo, modelo, diseñador de moda, productor de moda, maquillador, estilista de cabello o estilista de imagen.

---

## UC-57 — Gestionar características técnicas por profesión

- **Actor:** Administrador
- **Objetivos asociados:** OBJ-01, OBJ-04
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando el administrador del sistema quiere gestionar las características técnicas por profesión del sistema. El sistema le permite agregar un nuevo tipo de característica técnica, modificar o eliminar una ya existente.
- **Precondición:** El usuario inició sesión en el sistema.

**Secuencia normal**
1. El sistema recupera y despliega todas las características registradas en el sistema (ver UC-58 Buscar características técnicas) y expone las siguientes opciones: "Agregar nueva característica", "Modificar característica", "Eliminar característica".
2. El actor selecciona "Agregar nueva característica".
3. El sistema recupera y despliega todas las profesiones registradas en el sistema (ver UC-59 Buscar profesiones) y solicita que se seleccione una o más profesiones para agregar la característica técnica.
4. El actor selecciona la profesión deseada.
5. El sistema despliega un formulario de carga solicitando de forma obligatoria: Nombre de la característica, Tipo de datos solicitados. Y opcionalmente: Descripción de la característica.
6. El actor completa los campos y confirma la acción.
7. El sistema registra la nueva característica al sistema.

- **Postcondición:** Se creó una nueva característica en el sistema. Se asoció una característica a una profesión en el sistema.

**Flujo alternativo**
- 2.1 Si el actor selecciona "Modificar característica": 2.1.1 El actor selecciona la característica que quiere modificar (ver UC-58 Buscar características técnicas). 2.1.2 El sistema despliega la información: Nombre de la característica, Tipo de datos solicitados, Profesión asociada, Descripción de la característica. 2.1.3 El actor modifica los campos deseados y confirma la acción. 2.1.4 El sistema registra los cambios. Postcondición 2.1: Se modificó una característica del sistema. El sistema, por cada perfil de usuario que tenía la característica técnica asociada a su perfil, emite una notificación informando de la modificación.
- 2.2 Si el actor selecciona "Eliminar característica": 2.2.1 El actor selecciona la característica que quiere eliminar (ver UC-58 Buscar características técnicas). 2.2.2 El sistema despliega los datos de la característica y solicita confirmación. 2.2.3 El actor confirma la acción. 2.2.4 El sistema registra la baja. Postcondición 2.2: Se dio de baja una característica en el sistema. El sistema, por cada perfil de usuario que tenía la característica asociada a su perfil, emite una notificación informando de la baja.

**Excepciones**
- Paso 4: Si el actor deja campos obligatorios vacíos o introduce un nombre de característica igual al de otra ya existente en el sistema, el sistema notifica la excepción y vuelve al paso 3 o el caso de uso finaliza.
- Paso 2.1.3: Si el actor deja campos obligatorios vacíos o introduce un nombre de característica igual al de otra ya existente en el sistema, el sistema notifica la excepción y vuelve al paso 2.1.2 o el caso de uso finaliza.

**Rendimiento**
- Paso 5: 2 segundos
- Paso 2.1.4: 2 segundos
- Paso 2.2.4: 2 segundos

- **Frecuencia:** 10 veces/día
- **Estabilidad:** media
- **Comentarios:** En "tipo de datos solicitados" se refiere a datos numéricos (ej. medidas), datos de texto (ej. equipo técnico) u opciones de selección (ej. dispone de estudio propio: sí/no).

---

## UC-58 — Buscar características técnicas

- **Actor:** Administrador / Usuario
- **Objetivos asociados:** OBJ-01, OBJ-04
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando se va a buscar una característica técnica registrada en el sistema.
- **Precondición:** El actor debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema solicita el ingreso de los criterios de filtrado (Nombre de la característica, descripción, profesión asociada (ver UC-59 Buscar profesiones) (si corresponde)).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de características técnicas y filtra aquellas que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con las características técnicas que cumplen el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 40 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-59 — Buscar profesiones

- **Actor:** Administrador / Usuario
- **Objetivos asociados:** OBJ-01, OBJ-04
- **Requisitos asociados:** IRQ-02
- **Descripción:** El caso de uso inicia cuando se va a buscar una profesión registrada en el sistema.
- **Precondición:** El actor debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El sistema solicita el ingreso de los criterios de filtrado (Nombre de la profesión).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de profesiones y filtra aquellas que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con las profesiones que cumplen el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 40 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-60 — Confirmar proyecto

- **Actor:** Director de proyecto
- **Objetivos asociados:** OBJ-02, OBJ-03
- **Requisitos asociados:** IRQ-03
- **Descripción:** El caso de uso inicia cuando el director de proyecto quiere confirmar un proyecto en estado "Publicado".
- **Precondición:** El usuario inició sesión en el sistema. Existen actividades en la planificación. Los requerimientos de las actividades en la planificación están completos. El director de proyecto cuenta con disponibilidad en su calendario personal para los bloques de tiempo y fechas estipulados en las actividades planificadas del proyecto.

**Secuencia normal**
1. El actor selecciona el proyecto que desea confirmar (ver UC-29 Buscar proyecto).
2. El sistema verifica que el requerimiento de personal de las actividades de la planificación esté completo. Luego solicita confirmación.
3. El actor confirma la acción.
4. El sistema actualiza el estado del proyecto a "Confirmado".

- **Postcondición:** El estado del proyecto se actualizó de "Publicado" a "Confirmado". El proyecto deja de estar abierto a nuevas postulaciones. El sistema, por cada participante del proyecto, emite una notificación informando sobre el cambio de estado del proyecto.

**Excepciones**
- Paso 2: Si el proyecto no tiene actividades en la planificación, el sistema informa al actor y el caso de uso finaliza. Si el proyecto no tiene el requerimiento completo, el sistema informa al actor y el caso de uso finaliza.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 40 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-61 — Enviar solicitud de colaboración

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-06
- **Requisitos asociados:** IRQ-02, IRQ-04
- **Descripción:** El caso de uso inicia cuando el usuario, desde su perfil, quiere invitar a colaborar en su publicación a otro perfil registrado en el sistema y que pertenece a su red de contactos.
- **Precondición:** El usuario inició sesión en el sistema y el perfil seleccionado pertenece a su red de contactos.

**Secuencia normal**
1. El actor selecciona el perfil al que desea enviar la solicitud (ver UC-16 Buscar perfil).
2. El sistema despliega la información del perfil y solicita confirmación.
3. El actor confirma la acción.
4. El sistema envía la solicitud al perfil destinatario.

- **Postcondición:** Se creó una solicitud de colaboración en estado "Pendiente". El sistema envía una notificación al destinatario informando sobre la solicitud.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 20 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-62 — Aceptar solicitud de colaboración

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-06
- **Requisitos asociados:** IRQ-02, IRQ-04
- **Descripción:** El caso de uso inicia cuando el usuario, desde su perfil, quiere aceptar una solicitud de colaboración.
- **Precondición:** El usuario inició sesión en el sistema.

**Secuencia normal**
1. El actor selecciona la solicitud que desea aceptar (ver UC-65 Buscar solicitudes de colaboración).
2. El sistema despliega la información de la solicitud y solicita confirmación.
3. El actor confirma la acción.
4. El sistema vincula la publicación al perfil de usuario.

- **Postcondición:** Se agregó al usuario como "Colaborador" de la publicación y se registró la publicación al perfil del usuario. Se actualizaron los colaboradores de la publicación asociada a la solicitud. Se actualizó el estado de la solicitud de colaboración a "Aceptada". El sistema envía una notificación al emisor informando la aceptación.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 40 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-63 — Rechazar solicitud de colaboración

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-06
- **Requisitos asociados:** IRQ-02, IRQ-04
- **Descripción:** El caso de uso inicia cuando el usuario, desde su perfil, quiere rechazar una solicitud de colaboración.
- **Precondición:** El usuario inició sesión en el sistema.

**Secuencia normal**
1. El actor selecciona la solicitud que desea rechazar (ver UC-65 Buscar solicitudes de colaboración).
2. El sistema despliega la información de la solicitud y solicita confirmación.
3. El actor confirma la acción.
4. El sistema actualiza el estado de la solicitud y notifica al emisor.

- **Postcondición:** Se actualizó el estado de la solicitud de colaboración a "Rechazada". El sistema envía una notificación al emisor informando el rechazo.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 15 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-64 — Eliminar solicitud de colaboración

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-06
- **Requisitos asociados:** IRQ-02, IRQ-04
- **Descripción:** El caso de uso inicia cuando el usuario, desde su perfil, quiere eliminar una solicitud de colaboración enviada a otro perfil registrado en el sistema.
- **Precondición:** El usuario inició sesión en el sistema y tiene al menos una solicitud de colaboración enviada en estado "Pendiente".

**Secuencia normal**
1. El actor selecciona la solicitud de colaboración que desea eliminar (ver UC-65 Buscar solicitudes de colaboración).
2. El sistema despliega la información de la solicitud y solicita confirmación.
3. El actor confirma la acción.
4. El sistema cancela el envío de la solicitud del perfil destinatario.

- **Postcondición:** Se eliminó una solicitud de colaboración. La solicitud de colaboración dejó de ser visible para el perfil destinatario.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 40 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-65 — Buscar solicitudes de colaboración

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-06
- **Requisitos asociados:** IRQ-02, IRQ-04
- **Descripción:** El caso de uso inicia cuando se va a buscar una solicitud de colaboración de una publicación de un perfil.
- **Precondición:** El usuario inició sesión en el sistema.

**Secuencia normal**
1. El sistema solicita el ingreso de los criterios de filtrado (perfil emisor (si corresponde), perfil receptor (si corresponde), fecha de solicitud, estado).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de solicitudes de colaboración del perfil y filtra aquellas que coinciden con los criterios.
4. El sistema recupera las publicaciones asociadas a cada solicitud de colaboración (ver UC-22 Buscar publicación) y lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con las solicitudes de colaboración y sus publicaciones que cumplen el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 40 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-66 — Interactuar con publicación

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-06
- **Requisitos asociados:** IRQ-02, IRQ-04
- **Descripción:** El caso de uso inicia cuando el usuario quiere interactuar con una publicación publicada en el sistema.
- **Precondición:** El usuario inició sesión en el sistema.

**Secuencia normal**
1. El actor selecciona la publicación con la que desea interactuar (ver UC-22 Buscar publicación).
2. El sistema despliega toda la información de la publicación y ofrece dos opciones: "Me gusta", "Comentar".
3. El actor selecciona "Comentar".
4. El sistema despliega un campo y solicita al actor el ingreso del comentario.
5. El usuario ingresa lo solicitado.
6. El sistema registra el nuevo comentario y notifica al dueño de la publicación.

- **Postcondición:** Se registró un nuevo comentario asociado a una publicación. El sistema, por cada perfil asociado a la publicación, genera una notificación informando del nuevo comentario.

**Flujo alternativo**
- 3.1 Si el actor selecciona "Me gusta". 3.2 El sistema registra la interacción y notifica al dueño de la publicación. Postcondición 3.1: Se registró un nuevo "me gusta" asociado a una publicación. Se sumó en 1 la cantidad de "me gusta" de la publicación. El sistema, por cada perfil asociado a la publicación, genera una notificación informando de la nueva interacción.

**Excepciones**
- Paso 5: Si el usuario ingresa texto vacío o supera el límite de caracteres, el sistema informa el error y vuelve al paso 4 o el caso de uso finaliza.

**Rendimiento**
- Paso 6: 2 segundos
- Paso 3.2: 2 segundos

- **Frecuencia:** 40 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-67 — Generar informe de auditoría

- **Actor:** Administrador
- **Objetivos asociados:** OBJ-05
- **Requisitos asociados:** NFR-01
- **Descripción:** El caso de uso inicia cuando el Administrador requiere compilar, visualizar y analizar el registro inmutable de transacciones y cambios críticos de estado ocurridos en la plataforma dentro de un período de tiempo determinado.
- **Precondición:** El usuario inició sesión en el sistema con las credenciales de administrador. Deben existir registros históricos previos almacenados en el módulo de auditoría.

**Secuencia normal**
1. El sistema despliega un formulario solicitando los criterios de filtrado: Fecha de inicio, Fecha de fin. Y de forma opcional: Tipo de evento crítico.
2. El administrador ingresa los parámetros de búsqueda y confirma la acción.
3. El sistema consulta al repositorio de datos de auditoría y filtra las transacciones que coinciden con los criterios ingresados.
4. El sistema recupera y lista los resultados obtenidos.

- **Salida:** El sistema lista los registros de auditoría que coinciden con los filtros aplicados.

**Excepciones**
- Paso 2: Si el rango de fechas es inválido, el sistema notifica el error y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 10 veces/día
- **Estabilidad:** media
- **Comentarios:** En los criterios de filtrado, en "tipo de evento crítico" se pueden ingresar más de un tipo de evento crítico. Si no se selecciona ninguno, por defecto despliega todos los eventos críticos.

---

## UC-68 — Buscar contrato

- **Actor:** Director de proyecto / Miembro de proyecto
- **Objetivos asociados:** OBJ-03, OBJ-05
- **Requisitos asociados:** IRQ-09
- **Descripción:** El caso de uso inicia cuando se va a buscar un contrato asociado a un proyecto.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema y tener al menos un contrato emitido o recibido en un proyecto.

**Secuencia normal**
1. El sistema solicita el ingreso de al menos un criterio de filtrado (partes del contrato, estado del contrato, fecha de emisión, fecha de firma (si corresponde), proyecto asociado (ver UC-29 Buscar proyecto)).
2. El actor ingresa los parámetros de búsqueda.
3. El sistema consulta el repositorio de contratos y filtra aquellos que coinciden con los criterios.
4. El sistema lista los resultados obtenidos.

- **Salida:** El sistema devuelve un listado con los contratos que cumplen con el criterio de búsqueda.

**Excepciones**
- Paso 3: Si no existen registros que coincidan con los filtros, el sistema notifica al actor y vuelve al paso 1 o el caso de uso finaliza.

**Rendimiento**
- Paso 3: 2 segundos

- **Frecuencia:** 15 veces/día
- **Estabilidad:** media
- **Comentarios:** -

---

## UC-69 — Reportar publicación

- **Actor:** Usuario
- **Objetivos asociados:** OBJ-04, OBJ-06
- **Requisitos asociados:** IRQ-02, IRQ-04
- **Descripción:** El caso de uso empieza cuando un usuario quiere reportar la publicación de un perfil de usuario.
- **Precondición:** El usuario debe haber iniciado sesión en el sistema.

**Secuencia normal**
1. El usuario localiza la publicación que desea reportar (referencia en el original a "UC-22 Buscar publicación").
2. El sistema despliega un formulario solicitando: Motivo del reporte (categorías predefinidas: spam, contenido inapropiado, identidad falsa, otro), Descripción detallada (opcional).
3. El usuario selecciona el motivo, completa descripción y envía el reporte.
4. El sistema registra el reporte asociado a la publicación y genera una alerta de revisión para el administrador.

- **Postcondición:** Se registró un reporte asociado a una publicación, con fecha, hora y autor del reporte. El reporte registrado queda en estado "Pendiente de revisión" por el administrador.

**Rendimiento**
- Paso 4: 2 segundos

- **Frecuencia:** 5 veces/día
- **Estabilidad:** media
- **Comentarios:** -
