### **UC-01: Iniciar sesión**
* **Actores:** Usuario, Google OAuth
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Bloque Alternativo (`alt`)**:
     * `[Inicio de sesión con credenciales locales]`: El actor Usuario envía el mensaje `ingresarCredenciales(correo, contrasenia)`.
     * `[Inicio de sesión con Google]`:
       * El actor Usuario envía el mensaje `seleccionarIrConGoogle()`.
       * **Referencia/Inclusión:** Comienza el DSS de `UC-09 Autenticar mediante GoogleOAuth`.
  2. **Bloque Alternativo (`alt`)**:
     * `[Excepción: Credenciales inválidas]`: El sistema retorna `mostrarErrorCredenciales()`.
     * `[Excepción: Credenciales válidas pero cuenta deshabilitada]`: El sistema retorna `informarCuentaDeshabilitada()`.
     * `[Credenciales válidas]`: El sistema retorna `otorgarAcceso()`.

---

### **UC-03: Registrarse**
* **Actores:** Usuario, Google OAuth
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Bloque Alternativo (`alt`)**:
     * `[Registro con correo y contraseña]`: El actor Usuario envía el mensaje `ingresarDatosRegistro(correo, contrasenia, nombre, apellido, fechaNac, genero)`.
     * `[Registro con Google]`:
       * El actor Usuario envía el mensaje `seleccionarIrConGoogle()`.
       * **Referencia/Inclusión:** Comienza el DSS de `UC-09 Autenticar mediante GoogleOAuth`.
       * **Bloque Opcional (`opt`)** `[Si el correo ya existe]`: El sistema retorna `iniciarSesiónAutomaticamente()`.
  2. **Bloque Alternativo (`alt`)**:
     * `[Excepción: Correo inválido]`: El sistema retorna `mostrarErrorCorreo()`.
     * `[Excepción: Edad inválida]`: El sistema retorna `informarErrorEdad()`.
     * `[Credenciales válidas]`: El sistema retorna `registroExitoso()`.

---

### **UC-04: Deshabilitar usuario**
* **Actor:** Administrador
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-06 Buscar usuario`.
  2. El actor envía el mensaje `unUsuario = obtenerUsuario(idUsuario)`.
  3. El actor envía el mensaje `deshabilitarUsuario(unUsuario, motivo, duracion)`.
  4. El sistema retorna `cambiarEstadoDeshabilitado(unUsuario)`.
  5. **Bloque Alternativo (`alt`)** `[duración != null]`:
     * El sistema retorna `cuentaRegresivaIniciada(unUsuario, duracion)`.
  6. El sistema retorna `ocultarPerfiles(unUsuario)`.
  7. El sistema retorna `notificarDeshabilitación(unUsuario, motivo, duracion)`.

---

### **UC-05: Habilitar usuario**
* **Actor:** Administrador
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-06 Buscar usuario`.
  2. El actor envía el mensaje `unUsuario = obtenerUsuario(idUsuario)`.
  3. El actor envía el mensaje `habilitarUsuario(unUsuario)`.
  4. El sistema retorna `cambiarEstadoActivo(unUsuario)`.
  5. El sistema retorna `mostrarPerfiles(unUsuario)`.
  6. El sistema retorna `notificarHabilitación(unUsuario)`.

---

### **UC-07: Solicitar baja en el sistema**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor envía el mensaje `solicitarBaja(idUsuario)`.
  2. El sistema retorna `solicitarConfirmacion()`.
  3. **Bloque Alternativo (`alt`)** `[confirma]`:
     * El actor envía el mensaje `unUsuario = buscarUsuario(idUsuario)`.
     * **Bloque Alternativo anidado (`alt`)**:
       * `[usuario noEncontrado]`: El sistema retorna `excepción ("usuario no encontrado")`.
       * `[usuario encontrado]`:
         * El sistema retorna `cambiarEstadoPendienteBaja(unUsuario)`.
         * **Referencia/Inclusión:** Comienza el DSS de `UC-15 Buscar perfil`.
         * El sistema retorna `ocultarPerfiles(unUsuario)`.
         * El sistema retorna `cuentaRegresivaIniciada(unUsuario, 30)`.
         * El sistema retorna `notificarBaja(unUsuario)`.
         * El sistema retorna `cerrarSesion()`.

---

### **UC-08: Modificar datos personales**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor envía el mensaje `obtenerDatosPersonales()`.
  2. El sistema retorna `datosPersonales(nombre, apellido, genero, fechaNac, idUbicacion)`.
  3. El actor envía el mensaje `modificarDatosPersonales(nombre, apellido, genero, fechaNac, idUbicacion)`.
  4. **Bloque Alternativo (`alt`)**:
     * `[campos inválidos o vacíos]`: El sistema retorna `excepcion()`.
     * `[datos válidos]`: El sistema retorna `operacionExitosa()`.

---

### **UC-09: Autenticar mediante Google OAuth**
* **Actores:** Usuario, Google OAuth
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El sistema envía el mensaje `redirigirAutenticación()` al actor Google OAuth.
  2. El actor Google OAuth envía el mensaje `solicitarPermisoAcceso()` al actor Usuario.
  3. El actor Usuario envía el mensaje `autorizarAcceso()` al actor Google OAuth.
  4. **Bloque Alternativo (`alt`)**:
     * `[Excepción: Error de comunicación o token inválido]`:
       * El actor Google OAuth envía el mensaje `notificarFalloAutenticación()` al sistema.
       * El sistema retorna `mensajeFallo()` al actor Usuario.
     * `[Credenciales válidas]`:
       * El actor Google OAuth envía el mensaje `datosAutenticación()` al sistema.

---

### **UC-10: Crear perfil**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-59 Buscar profesiones`.
  2. El actor envía el mensaje `unPerfil = crearNuevoPerfil(nombreArtistico, idProfesion)`.
  3. **Bloque Alternativo (`alt`)**:
     * `[si el usuario ya tiene un perfil con la profesión seleccionada]`: El sistema retorna `excepción()`.
     * `[si el usuario no tiene un perfil con la profesión seleccionada]`:
       * **Referencia/Inclusión:** Comienza el DSS de `UC-58 Buscar características técnicas`.
       * **Bloque Bucle (`loop`)** `[mientras existan características por cargar]`:
         * El actor envía el mensaje `unaCaracteristica = registrarCaracterísticaPerfil(valor, idCaracteristica)`.
       * El actor envía el mensaje `agregarBiografía(unaBiografia)`.
  4. **Bloque Alternativo (`alt`)**:
     * `[si el usuario dejó campos obligatorios vacíos]`: El sistema retorna `excepción()`.
     * `[campos completados]`: El sistema retorna `registrarPerfil(unPerfil)`.

---

### **UC-11: Modificar perfil**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-16 Buscar perfil`.
  2. El actor envía el mensaje `editarPerfil(unPerfil, nombreArtistico, biografia, caracteristicas[], fotoPerfil)`.
  3. El sistema retorna `unPerfil`.

---

### **UC-12: Eliminar perfil**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-16 Buscar perfil`.
  2. El actor solicita la baja enviando el mensaje `solicitarBajaPerfil(unPerfil)`.
  3. El sistema retorna `solicitarConfirmacion()`.
  4. **Bloque Opcional (`opt`)** `[el usuario confirma la baja]`:
     * El actor envía el mensaje `confirmarBaja(unPerfil)`.
     * El sistema retorna `notificarCuentaRegresiva(diasEstablecidos)`.

---

### **UC-13: Cambiar perfil activo**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-16 Buscar perfil`.
  2. El actor envía el mensaje `activarPerfil(unPerfil)`.
  3. El sistema retorna `perfilActivado()`.

---

### **UC-15: Reportar perfil**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-16 Buscar perfil`.
  2. El actor envía el mensaje `reportarPerfil(unPerfil, motivo, descripcionDetallada)`.
  3. El sistema retorna `unReporte`.

---

### **UC-17: Asignar en calendario dia disponible**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor envía el mensaje `marcarComoDisponible(fechaHoraInicio, fechaHoraFin)`.
  2. **Bloque Alternativo (`alt`)**:
     * `[Bloque coincide con actividad]`: El sistema retorna `excepción(conflicto con actividad)`.
     * `[Bloque liberado]`: El sistema retorna `bloqueDisponible(unBloque)`.

---

### **UC-18: Asignar en calendario dia no disponible**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor envía el mensaje `marcarComoNoDisponible(fechaHoraInicio, fechaHoraFin, motivo)`.
  2. **Bloque Alternativo (`alt`)**:
     * `[Bloque coincide con actividad]`: El sistema retorna `excepción(registro duplicado)`.
     * `[Bloqueo registrado]`: El sistema retorna `bloqueoRegistrado(idBloqueo)`.

---

### **UC-24: Crear proyecto**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor solicita crear un proyecto enviando el mensaje `crearProyecto(nombre, descripcion, privacidad, fechaInicioEstipulada, ubicacion, requerimientosGral[], moodboard, objetivos[], fechaFinEstipulada)`.
  2. **Bloque Alternativo (`alt`)**:
     * `[Campos obligatorios vacíos || nombre igual a otro proyecto del perfil || fecha de fin menor a la fecha de inicio]`: El sistema retorna `excepcion()`.
     * `[campos correctos]`: El sistema retorna la entidad `unProyecto`.

---

### **UC-25: Publicar proyecto**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  2. El actor solicita publicar el proyecto enviando el mensaje `publicarProyecto(unProyecto)`.
  3. **Bloque Alternativo (`alt`)**:
     * `[proyecto con información básica incompleta]`: El sistema retorna `excepcion()`.
     * `[información completa]`: El sistema retorna `unProyecto`.

---

### **UC-26: Dar de alta postulación a proyecto**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Bloque Alternativo (`alt`)**:
     * `[postulacion para un requerimiento general de proyecto]`:
       * **Referencia/Inclusión:** Comienza el DSS de `UC-72 Buscar requerimiento general`.
       * El actor envía el mensaje `darDeAltaPostulación(unPerfil, unRequerimientoGeneral)`.
     * `[postulacion para un requerimiento de actividad]`:
       * **Referencia/Inclusión:** Comienza el DSS de `UC-73 Buscar requerimiento actividad`.
       * El actor envía el mensaje `unUsuario = buscarUsuario(unPerfil)`.
       * El actor envía el mensaje `verificarDisponibilidad(unUsuario, unaActividad)`.
       * **Bloque Alternativo anidado (`alt`)**:
         * `[si el usuario no tiene disponibilidad]`: El sistema retorna `excepcion()`.
         * `[el usuario tiene disponibilidad frente al rango factible de la actividad]`: El sistema retorna `unaPostulacion`.

---

### **UC-49: Crear actividad a planificación**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  2. El actor solicita crear la actividad enviando `unaActividad = crearActividad(unProyecto, nombre, descripcion, duracion)`.
  3. **Bloque Opcional (`opt`)** `[si la actividad tiene predecesoras]`:
     * **Referencia/Inclusión:** Comienza el DSS de `UC-52 Buscar actividades`.
     * **Bloque Bucle (`loop`)** `[mientras existan actividades predecesoras]`:
       * El actor envía `predecesoras[] = agregarPredecesora(unaActividad, unaPredecesora)`.
  4. **Bloque Opcional (`opt`)** `[si la actividad tiene ubicacion]`:
     * El actor envía `unaUbicacion = buscarUbicacion(idUbicacion)`.
     * El actor envía `agregarUbicacion(unaActividad, unaUbicacion)`.
  5. **Bloque Alternativo (`alt`)**:
     * `[si el actor deja campos obligatorios vacíos]`: El sistema retorna `excepcion()`.
     * `[si el actor ingresó los datos correctos]`:
       * **Bloque Bucle (`loop`)** `[mientras exista requerimiento profesional por cargar]`:
         * **Referencia/Inclusión:** Comienza el DSS de `UC-59 Buscar profesiones`.
         * **Referencia/Inclusión:** Comienza el DSS de `UC-58 Buscar características técnicas`.
         * **Referencia/Inclusión:** Comienza el DSS de `UC-48 Buscar habilidades`.
         * El actor envía `agregarRequerimiento(unaActividad, unaProfesion, caracteristicas[], habilidades[])`.
       * El sistema retorna `unaActividad`.

---

### **UC-50: Modificar actividad de planificación**
* **Actor:** Director de Proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-52 Buscar actividades`.
  2. El actor envía el mensaje `modificarActividad(unaActividad, nombre, descripcion, duracionEstimada, requerimiento[], predecesoras[], ubicacion, estado)`.
  3. El sistema retorna `unaActividad`.

---

### **UC-51: Eliminar actividad de planificación**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-52 Buscar actividades`.
  2. El actor solicita eliminar la actividad enviando `eliminarActividad(unaActividad, motivo)`.
  3. **Bloque Alternativo (`alt`)**:
     * `[Excepción: estado inválido]`: El sistema retorna `excepcion()`.
     * `[Estado válido]`:
       * **Bloque Alternativo anidado (`alt`)** `[Si la actividad tenía miembros asignados]`:
         * **Bloque Bucle (`loop`)** `[mientras existan miembros asignados a la actividad]`:
           * **Bloque Opcional (`opt`)** `[El director decide eliminar al integrante]`:
             * **Referencia/Inclusión:** Comienza el DSS de `UC-38 Eliminar integrante`.
           * **Bloque Opcional (`opt`)** `[El director decide guardar de forma temporal al integrante]`:
             * El actor envía el mensaje `guardarMiembro(unPerfil)`.
       * El sistema retorna `operacion exitosa`.

---

### **UC-70: Asignar actividad a miembro**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-52 Buscar actividades`.
  2. **Referencia/Inclusión:** Comienza el DSS de `UC-16 Buscar perfil`.
  3. El actor envía el mensaje `unUsuario = buscarUsuario(unPerfil)`.
  4. El actor envía el mensaje `verificarDisponibilidad(unUsuario, unaActividad)`.
  5. **Bloque Alternativo (`alt`)**:
     * `[si el miembro no tiene disponibilidad]`: El sistema retorna `excepcion()`.
     * `[el miembro tiene disponibilidad frente al rango factible de la actividad]`:
       * El actor envía el mensaje `unaAsignacionActividad = asignarActividad(unPerfil, unaActividad)`.
       * El sistema retorna `asignacionExitosa(unaAsignacionActividad)`.

---

### **UC-28: Aceptar solicitud de incorporación**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-30 Buscar invitaciones`.
  2. El actor envía el mensaje `unaInvitacion = obtenerInvitacion(idInvitacion)`.
  3. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  4. El actor envía el mensaje `unProyecto = obtenerProyecto(unaInvitacion)`.
  5. **Bloque Alternativo (`alt`)**:
     * `[invitación no válida / requerimiento satisfecho / proyecto cancelado]`: El sistema retorna `excepcion()`.
     * `[invitación válida]`:
       * El sistema retorna `solicitarConfirmacion()`.
       * **Bloque Opcional (`opt`)** `[el usuario confirma la incorporación]`:
         * El actor envía el mensaje `confirmarIncorporacion(unaInvitacion)`.
         * El sistema retorna `incorporacionRegistrada(unProyecto)`.
         * El sistema retorna `actualizarRequerimientosPersonal(unProyecto)`.
         * El sistema retorna `notificarDirector(unProyecto)`.

---

### **UC-29: Buscar proyecto**
* **Actor:** Usuario / Director de proyecto / Miembro de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor envía el mensaje `buscarProyectos(nombre, estado, director)`.
  2. **Bloque Alternativo (`alt`)**:
     * `[no existen coincidencias]`: El sistema retorna `sinResultados()`.
     * `[existen proyectos]`: El sistema retorna `proyectos[]`.

---

### **UC-30: Buscar invitaciones**
* **Actor:** Usuario / Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor envía el mensaje `buscarInvitaciones(nombreProyecto, fechaEnvio, perfil)`.
  2. **Bloque Alternativo (`alt`)**:
     * `[no existen coincidencias]`: El sistema retorna `sinResultados()`.
     * `[existen invitaciones]`: El sistema retorna `invitaciones[]`.

---

### **UC-31: Invitar a proyecto**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  2. El actor envía el mensaje `unProyecto = obtenerProyecto(idProyecto)`.
  3. **Referencia/Inclusión:** Comienza el DSS de `UC-16 Buscar perfil`.
  4. El actor envía el mensaje `unPerfil = obtenerPerfil(idPerfil)`.
  5. El actor envía el mensaje `enviarInvitacion(unProyecto, unPerfil, mensajePersonalizado)`.
  6. **Bloque Alternativo (`alt`)**:
     * `[el perfil ya posee una invitación activa al proyecto]`: El sistema retorna `excepcion()`.
     * `[invitación válida]`:
       * El sistema retorna `invitacionRegistrada(unaInvitacion)`.
       * El sistema retorna `notificarPerfil(unPerfil, unaInvitacion)`.

---

### **UC-32: Eliminar invitación a proyecto**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-30 Buscar invitaciones`.
  2. El actor envía el mensaje `unaInvitacion = obtenerInvitacion(idInvitacion)`.
  3. El actor solicita la baja enviando `solicitarBajaInvitacion(unaInvitacion)`.
  4. El sistema retorna `solicitarConfirmacion()`.
  5. **Bloque Opcional (`opt`)** `[el actor confirma la eliminación]`:
     * El actor envía el mensaje `confirmarBajaInvitacion(unaInvitacion)`.
     * **Bloque Alternativo (`alt`)**:
       * `[la invitación no está en estado 'Pendiente']`: El sistema retorna `excepcion()`.
       * `[estado válido]`: El sistema retorna `invitacionEliminada()`.

---

### **UC-33: Visualizar cronograma de proyecto**
* **Actor:** Director de proyecto / Miembro de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  2. El actor envía el mensaje `unProyecto = obtenerProyecto(idProyecto)`.
  3. **Referencia/Inclusión:** Comienza el DSS de `UC-52 Buscar actividades`.
  4. El actor envía el mensaje `actividades[] = obtenerActividades(unProyecto)`.
  5. El actor envía el mensaje `solicitarCronograma(unProyecto)`.
  6. El sistema retorna `desplegarCronograma(unProyecto, actividades[])`.

---

### **UC-34: Gestionar postulaciones**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  2. El actor envía el mensaje `unProyecto = obtenerProyecto(idProyecto)`.
  3. El actor envía el mensaje `postulaciones[] = obtenerPostulaciones(unProyecto)`.
  4. **Bloque Alternativo (`alt`)**:
     * `[Aceptar postulación]`:
       * El actor envía el mensaje `aceptarPostulacion(unaPostulacion)`.
       * El sistema retorna `solicitarConfirmacion()`.
       * **Bloque Opcional (`opt`)** `[el actor confirma la acción]`:
         * El actor envía el mensaje `confirmarAceptacion(unaPostulacion)`.
         * El sistema retorna `incorporarMiembro(unProyecto, unaPostulacion)`.
         * El sistema retorna `actualizarRequerimientosPersonal(unProyecto)`.
         * El sistema retorna `notificarPostulante(unaPostulacion)`.
     * `[Rechazar postulación]`:
       * El actor envía el mensaje `rechazarPostulacion(unaPostulacion)`.
       * El sistema retorna `solicitarConfirmacion()`.
       * **Bloque Opcional (`opt`)** `[el actor confirma la acción]`:
         * El actor envía el mensaje `confirmarRechazo(unaPostulacion)`.
         * El sistema retorna `postulacionEliminada(unaPostulacion)`.
         * El sistema retorna `notificarPostulanteRechazado(unaPostulacion)`.

---

### **UC-35: Cancelar proyecto**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  2. El actor envía el mensaje `unProyecto = obtenerProyecto(idProyecto)`.
  3. El actor solicita cancelar enviando `cancelarProyecto(unProyecto, motivo)`.
  4. **Bloque Alternativo (`alt`)**:
     * `[motivo vacío]`: El sistema retorna `excepcion()`.
     * `[motivo ingresado]`:
       * El sistema retorna `cambiarEstadoCancelado(unProyecto)`.
       * El sistema retorna `interrumpirActividades(unProyecto)`.
       * El sistema retorna `liberarBloqueosAgendaParticipantes(unProyecto)`.
       * El sistema retorna `notificarCancelacionIntegrantes(unProyecto, motivo)`.

---

### **UC-36: Modificar proyecto**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  2. El actor envía el mensaje `unProyecto = obtenerProyecto(idProyecto)`.
  3. El actor envía el mensaje `modificarProyecto(unProyecto, descripcion, privacidad, ubicacion, requerimientosPersonal[], moodboard, objetivos, fechaFinEstipulada)`.
  4. **Bloque Alternativo (`alt`)**:
     * `[campos obligatorios vacíos o inconsistentes]`: El sistema retorna `excepcion()`.
     * `[datos válidos]`:
       * El sistema retorna `proyectoActualizado(unProyecto)`.
       * El sistema retorna `notificarParticipantes(unProyecto)`.

---

### **UC-37: Finalizar proyecto**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  2. El actor envía el mensaje `unProyecto = obtenerProyecto(idProyecto)`.
  3. El sistema retorna `solicitarCalificaciones(unProyecto)`.
  4. El actor envía el mensaje `calificarEquipo(unProyecto, calificaciones[])`.
  5. **Bloque Opcional (`opt`)** `[el director decide subir imágenes del resultado]`:
     * **Referencia/Inclusión:** Comienza el DSS de `UC-19 Alta publicación`.
  6. **Bloque Alternativo (`alt`)**:
     * `[calificación del equipo incompleta]`: El sistema retorna `excepcion()`.
     * `[calificaciones completas]`:
       * El actor envía el mensaje `confirmarFinalizacion(unProyecto)`.
       * El sistema retorna `cambiarEstadoFinalizado(unProyecto)`.
       * El sistema retorna `solicitarNotificacionesCalificacionEquipo(unProyecto)`.
       * El sistema retorna `indexarHistorialPerfiles(unProyecto)`.

---

### **UC-38: Eliminar integrante**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  2. El actor envía el mensaje `unProyecto = obtenerProyecto(idProyecto)`.
  3. **Referencia/Inclusión:** Comienza el DSS de `UC-16 Buscar perfil`.
  4. El actor envía el mensaje `unPerfil = obtenerPerfil(idPerfil)`.
  5. El actor solicita la eliminación enviando `eliminarIntegrante(unProyecto, unPerfil, motivo)`.
  6. **Bloque Alternativo (`alt`)**:
     * `[motivo vacío]`: El sistema retorna `excepcion()`.
     * `[motivo completado]`:
       * El sistema retorna `bajaIntegranteRegistrada(unProyecto, unPerfil)`.
       * El sistema retorna `reabrirVacantesActividades(unProyecto, unPerfil)`.
       * El sistema retorna `notificarIntegranteEliminado(unPerfil, motivo)`.

---

### **UC-52: Buscar actividades**
* **Actor:** Director de proyecto / Miembro de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor envía el mensaje `buscarActividades(nombre, descripcion, requerimiento, duracion, estado)`.
  2. **Bloque Alternativo (`alt`)**:
     * `[no existen actividades que coincidan]`: El sistema retorna `sinResultados()`.
     * `[existen actividades]`: El sistema retorna `actividades[]`.

---

### **UC-60: Confirmar proyecto**
* **Actor:** Director de proyecto
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. **Referencia/Inclusión:** Comienza el DSS de `UC-29 Buscar proyecto`.
  2. El actor envía el mensaje `unProyecto = obtenerProyecto(idProyecto)`.
  3. El actor solicita confirmar enviando `solicitarConfirmacionProyecto(unProyecto)`.
  4. **Bloque Alternativo (`alt`)**:
     * `[el proyecto no tiene actividades en la planificación || requerimientos incompletos || falta de disponibilidad]`: El sistema retorna `excepcion()`.
     * `[proyecto con actividades y requerimientos completos]`:
       * El sistema retorna `solicitarConfirmacion()`.
       * **Bloque Opcional (`opt`)** `[el actor confirma la acción]`:
         * El actor envía el mensaje `confirmarProyecto(unProyecto)`.
         * El sistema retorna `cambiarEstadoConfirmado(unProyecto)`.
         * El sistema retorna `cerrarPostulaciones(unProyecto)`.
         * El sistema retorna `notificarParticipantes(unProyecto)`.

---

### **UC-71: Configurar jornada laboral**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor envía el mensaje `obtenerJornadaLaboral()`.
  2. El sistema retorna `jornadaPredeterminada()`.
  3. **Bloque Bucle (`loop`)** `[mientras existan días por configurar]`:
     * **Bloque Alternativo (`alt`)**:
       * `[Jornada completa]`:
         * El actor envía el mensaje `configurarDiaCompleto(diaSemana, horaInicio, horaFin)`.
       * `[Jornada partida]`:
         * El actor envía el mensaje `configurarDiaPartida(diaSemana, horaInicioManana, horaFinManana, horaInicioTarde, horaFinTarde)`.
  4. El actor envía el mensaje `confirmarJornadaLaboral()`.
  5. **Bloque Alternativo (`alt`)**:
     * `[horarios inválidos || conflicto con actividades ya asignadas]`: El sistema retorna `excepcion()`.
     * `[jornada válida]`:
       * El sistema retorna `jornadaLaboralRegistrada()`.
       * El sistema retorna `vincularCalendarioUsuario()`.
       * El sistema retorna `notificarCambioJornada()`.

---

### **UC-72: Buscar requerimiento general**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor envía el mensaje `buscarRequerimientosGenerales(profesion, caracteristicas[], unProyecto)`.
  2. **Bloque Alternativo (`alt`)**:
     * `[no existen coincidencias]`: El sistema retorna `sinResultados()`.
     * `[existen requerimientos generales]`: El sistema retorna `requerimientosGenerales[]`.

---

### **UC-73: Buscar requerimiento actividad**
* **Actor:** Usuario
* **Sistema:** `:ModaLink`
* **Flujo de interacción:**
  1. El actor envía el mensaje `buscarRequerimientosActividad(profesion, caracteristicas[], unProyecto, unaActividad)`.
  2. **Bloque Alternativo (`alt`)**:
     * `[no existen coincidencias]`: El sistema retorna `sinResultados()`.
     * `[existen requerimientos de actividad]`: El sistema retorna `requerimientosActividad[]`.