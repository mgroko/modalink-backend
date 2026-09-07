# Contratos de Operación del Sistema — ModaLink

> **Fase 2: Análisis (Proceso Unificado)**  
> **Fuente Canónica de Datos**: [bd.sql](file:///C:/Users/HP/Desktop/modalink-backend/docs/0_nota_presentacion/bd.sql)  
> **Regla de Redacción**: Las postcondiciones deben detallar rigurosamente la creación de instancias, modificación de atributos y asociación de claves foráneas sobre el modelo relacional.

---

## 1. Módulo de Gestión de Usuarios

### Contrato: `iniciarSesion(correo, contrasena)`
- **Responsabilidades**: Autentica al usuario verificando la correspondencia del hash de la contraseña y valida que la cuenta se encuentre habilitada para emitir el token de sesión.
- **Tipo**: Sistema.
- **Referencias cruzadas**: [CU-01: Iniciar sesión](file:///C:/Users/HP/Desktop/modalink-backend/docs/1_requisitos/casos-de-uso-extendidos.md#L83).
- **Notas**: Si el usuario está deshabilitado temporalmente pero `CURRENT_DATE >= usuario.fecha_hasta_deshabilitacion`, el sistema actualiza el estado a `'HABILITADO'`.
- **Excepciones**:
  - `CredencialesInvalidasException`: Correo no registrado o contraseña incorrecta.
  - `CuentaDeshabilitadaException`: El usuario tiene `estado = 'DESHABILITADO'`.
- **Salida**: `TokenRespuesta` (JWT, `id_usuario`, `rol_global`).
- **Precondiciones**:
  - Existe un registro en la tabla `usuario` cuyo atributo `correo` coincide con el parámetro ingresado.
- **Postcondiciones**:
  - Ninguna tupla es modificada en caso de solo lectura, o se actualizó `usuario.estado = 'HABILITADO'` si venció la fecha de deshabilitación.

---

### Contrato: `registrarUsuario(nombre, apellido, dni, fechaNacimiento, correo, contrasena, idGenero, idUbicacion)`
- **Responsabilidades**: Crea una nueva cuenta de usuario en el sistema con su correspondiente agenda personal y asigna el rol global de Usuario.
- **Tipo**: Sistema.
- **Referencias cruzadas**: [CU-03: Registrarse](file:///C:/Users/HP/Desktop/modalink-backend/docs/1_requisitos/casos-de-uso-extendidos.md#L125).
- **Excepciones**:
  - `CorreoDuplicadoException`: Ya existe una tupla en `usuario` con el mismo `correo`.
  - `DniDuplicadoException`: Ya existe una tupla en `usuario` con el mismo `DNI`.
  - `EdadInvalidaException`: La diferencia en años entre `CURRENT_DATE` y `fechaNacimiento` es menor a 18 años.
- **Salida**: Instancia de `Usuario` registrado.
- **Precondiciones**:
  - Existen registros en `genero` con clave `idGenero` y en `ubicacion` con clave `idUbicacion`.
  - Existe el registro de rol global `'USUARIO'` en `rol_global`.
- **Postcondiciones**:
  - Se creó una tupla $u$ en la tabla `usuario` con:
    - `id_usuario = nuevoId`
    - `nombre = nombre`, `apellido = apellido`, `"DNI" = dni`
    - `fecha_nacimiento = fechaNacimiento`, `correo = correo`
    - `password_hash = hash(contrasena)`
    - `estado = 'HABILITADO'`, `proveedor_auth = 'LOCAL'`
    - `motivo_deshabilitacion = ''`, `id_externo = ''`
    - `id_genero = idGenero`, `id_ubicacion = idUbicacion`
    - `rol_global_id_rol = idRolUsuario`
  - Se creó una tupla $a$ en la tabla `agenda` con:
    - `id_agenda = nuevoIdAgenda`
    - `id_usuario = u.id_usuario`
    - `margen_actividad_min = '30'`
  - Se asoció la agenda $a$ al usuario $u$ mediante `agenda.id_usuario`.

---

### Contrato: `deshabilitarUsuario(idUsuario, motivoDeshabilitacion, diasDuracion)`
- **Responsabilidades**: Suspende temporal o indeterminadamente la cuenta de un usuario por parte de un Administrador.
- **Tipo**: Sistema.
- **Referencias cruzadas**: [CU-04: Deshabilitar usuario](file:///C:/Users/HP/Desktop/modalink-backend/docs/1_requisitos/casos-de-uso-extendidos.md#L156).
- **Excepciones**:
  - `UsuarioNoEncontradoException`: No existe el registro con `idUsuario`.
  - `OperacionNoPermitidaException`: El usuario ya se encuentra en estado `'DESHABILITADO'`.
- **Salida**: `Usuario` actualizado.
- **Precondiciones**:
  - El usuario actor posee el rol global de `'ADMINISTRADOR'`.
  - Existe una tupla $u$ en `usuario` con `id_usuario = idUsuario`.
- **Postcondiciones**:
  - Se actualizó el registro $u$ en la tabla `usuario`:
    - `estado` se estableció en `'DESHABILITADO'`.
    - `motivo_deshabilitacion` se estableció con el texto `motivoDeshabilitacion`.
    - `fecha_hasta_deshabilitacion` se estableció en `CURRENT_DATE + diasDuracion` (o `NULL` si no se especificaron días).

---

## 2. Módulo de Gestión de Perfiles

### Contrato: `crearPerfil(idProfesion, nombreArtistico, biografia, idFotoPerfil, caracteristicas, habilidades)`
- **Responsabilidades**: Da de alta un perfil profesional asociado al usuario, registrando sus medidas técnicas y habilidades.
- **Tipo**: Sistema.
- **Referencias cruzadas**: [CU-10: Crear perfil](file:///C:/Users/HP/Desktop/modalink-backend/docs/1_requisitos/casos-de-uso-extendidos.md#L305).
- **Excepciones**:
  - `PerfilAdminNoPermitidoException`: El usuario posee rol global de Administrador.
  - `PerfilDuplicadoException`: El usuario ya tiene un perfil activo para `idProfesion`.
- **Salida**: Instancia del `Perfil` creado.
- **Precondiciones**:
  - El usuario está autenticado y su estado es `'HABILITADO'`.
  - Existen registros válidos en `profesion` (`idProfesion`) e `imagen` (`idFotoPerfil`).
- **Postcondiciones**:
  - Se creó una tupla $p$ en `perfil` con:
    - `id_perfil = nuevoIdPerfil`, `nombre_artistico = nombreArtistico`
    - `biografia = biografia`, `id_profesion = idProfesion`
    - `foto_perfil = idFotoPerfil`, `estado = 'ACTIVO'`
    - `fecha_baja_perfil = NULL`
  - Por cada elemento en `caracteristicas`:
    - Se creó una tupla en `caracteristica_perfil` asociada a $p.id\_perfil$.
  - Por cada elemento en `habilidades`:
    - Se creó una tupla en `habilidad_perfil` asociando `id_habilidad` con $p.id\_perfil$.

---

### Contrato: `darDeBajaPerfil(idPerfil)`
- **Responsabilidades**: Ejecuta el borrado lógico del perfil creativo indicado.
- **Tipo**: Sistema.
- **Referencias cruzadas**: [CU-12: Eliminar perfil](file:///C:/Users/HP/Desktop/modalink-backend/docs/1_requisitos/casos-de-uso-extendidos.md#L368).
- **Excepciones**:
  - `PerfilNoEncontradoException`: No existe el registro con `idPerfil`.
  - `PerfilConActividadesPendientesException`: El perfil está asignado a actividades en proyectos en estado `'PUBLICADO'` o `'CONFIRMADO'`.
- **Salida**: Booleano de confirmación.
- **Precondiciones**:
  - Existe la tupla $p$ en `perfil` con `id_perfil = idPerfil` y `estado = 'ACTIVO'`.
- **Postcondiciones**:
  - Se modificó la tupla $p$ en `perfil`:
    - `estado` se estableció en `'DADO_DE_BAJA'`.
    - `fecha_baja_perfil` se estableció con `CURRENT_TIMESTAMP`.

---

## 3. Módulo de Gestión de Disponibilidad

### Contrato: `crearBloqueoAgenda(idAgenda, fechaHoraInicio, fechaHoraFin, motivo)`
- **Responsabilidades**: Asienta un bloqueo manual de indisponibilidad en la agenda personal del usuario sin permitir solapamientos.
- **Tipo**: Sistema.
- **Referencias cruzadas**: [CU-18: Asignar en calendario día no disponible](file:///C:/Users/HP/Desktop/modalink-backend/docs/1_requisitos/casos-de-uso-extendidos.md#L552).
- **Excepciones**:
  - `RangoFechasInvalidoException`: `fechaHoraFin <= fechaHoraInicio`.
  - `SolapamientoHorarioException`: Existe un bloqueo en `bloqueo_agenda` que se superpone con el intervalo solicitado.
- **Salida**: Instancia de `BloqueoAgenda`.
- **Precondiciones**:
  - Existe la tupla $a$ en `agenda` con `id_agenda = idAgenda` perteneciente al usuario en sesión.
- **Postcondiciones**:
  - Se creó una tupla $b$ en la tabla `bloqueo_agenda`:
    - `id_bloqueo = nuevoIdBloqueo`
    - `fecha_hora_inicio = fechaHoraInicio`
    - `fecha_hora_fin = fechaHoraFin`
    - `motivo = motivo`
    - `id_agenda = idAgenda`
  - Se asoció la tupla $b$ con la agenda $a$ mediante `bloqueo_agenda.id_agenda`.

---

## 4. Módulo de Gestión de Proyectos

### Contrato: `crearProyecto(nombre, descripcion, fechaInicio, privacidad, aceptaPostulacionGral, idUbicacion)`
- **Responsabilidades**: Da de alta un nuevo proyecto profesional en estado borrador, crea su planificación e incorpora al creador con rol de Director de Proyecto.
- **Tipo**: Sistema.
- **Referencias cruzadas**: [CU-24: Crear proyecto](file:///C:/Users/HP/Desktop/modalink-backend/docs/1_requisitos/casos-de-uso-extendidos.md#L734).
- **Salida**: Instancia de `Proyecto` creado.
- **Precondiciones**:
  - El usuario autenticado posee al menos un perfil activo en `perfil`.
  - Existe la tupla en `ubicacion` con `id_ubicacion = idUbicacion`.
  - Existe la tupla en `rol_proyecto` con nombre `'DIRECTOR'`.
- **Postcondiciones**:
  - Se creó una tupla $py$ en `proyecto`:
    - `id_proyecto = nuevoIdProyecto`, `nombre = nombre`, `descripcion = descripcion`
    - `fecha_inicio = fechaInicio`, `estado = 'BORRADOR'`, `privacidad = privacidad`
    - `acepta_postulacion_gral = aceptaPostulacionGral`, `id_ubicacion = idUbicacion`
  - Se creó una tupla $pl$ en `planificacion`:
    - `id_planificacion = nuevoIdPlanificacion`, `id_proyecto = py.id_proyecto`, `fecha_entrega = NULL`
  - Se creó una tupla $m$ en `miembros_proyecto`:
    - `id_miembro = nuevoIdMiembro`, `id_proyecto = py.id_proyecto`
    - `id_perfil = perfilUsuario`, `id_rol = idRolDirector`
    - `estado_participacion = 'ACTIVO'`
  - Se asoció $m$ y $pl$ al proyecto $py$.

---

### Contrato: `crearActividadPlanificacion(idPlanificacion, nombre, duracion, fechaHoraInicio, descripcion, idUbicacion, requerimientos)`
- **Responsabilidades**: Añade una actividad al cronograma del proyecto y registra los perfiles técnicos necesarios para su realización.
- **Tipo**: Sistema.
- **Referencias cruzadas**: [CU-49: Crear actividad en planificación](file:///C:/Users/HP/Desktop/modalink-backend/docs/1_requisitos/casos-de-uso-extendidos.md#L1490).
- **Excepciones**:
  - `FechaInvalidaException`: La `fechaHoraInicio` es previa a la fecha de inicio del proyecto.
- **Salida**: Instancia de `Actividad`.
- **Precondiciones**:
  - Existe la tupla `planificacion` con `id_planificacion = idPlanificacion`.
  - El usuario logueado es Director del proyecto asociado a dicha planificación.
- **Postcondiciones**:
  - Se creó una tupla $act$ en `actividad`:
    - `id_actividad = nuevoIdActividad`, `nombre = nombre`, `duracion = duracion`
    - `fecha_hora_inicio = fechaHoraInicio`, `descripcion = descripcion`
    - `id_planificacion = idPlanificacion`, `id_ubicacion = idUbicacion`
  - Por cada requerimiento de disciplina:
    - Se creó una tupla en `requerimiento_actividad` asociada a $act.id\_actividad$.
