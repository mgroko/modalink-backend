# Diagramas de Secuencia de Diseño (DSD) — ModaLink

> **Fase 3 — Diseño.** Este documento realiza los contratos de
> `docs/2_analisis/contratos.md` y los flujos de `docs/2_analisis/dss.md`
> aplicando los patrones Controlador, Creador y Experto en Información.
>
> **Criterios de lectura**
>
> - Cada DSD corresponde a **una operación** y se identifica con
>   `DSD nombreOperacion(params) | UC-xx: Nombre`.
> - `:ModaLink` es el controlador/sistema. Las colecciones representan la
>   persistencia (tablas del modelo SQL) y las instancias representan filas.
> - Se usan bloques `loop`, `alt` y `opt` para el flujo, y variables locales
>   explícitas.
> - Cuando una operación existe en `contratos.md`, se adopta su firma. Para
>   operaciones que sólo aparecen en el DSS, se usa la firma del DSS.
> - No se modelan endpoints, transacciones, triggers ni detalles de
>   implementación: esos aspectos pertenecen a Fase 2 y Fase 3.5.
> - Los DSD auxiliares de búsqueda/selección (`buscarX`, `seleccionarX`)
>   completan los flujos de los UC que los referencian.
> - `perfilActivo` y `usuarioActivo` representan el contexto de sesión del
>   actor; no se declaran como variables locales.
> - Quedan fuera de este alcance los UC que no tienen DSS ni contrato en la
>   Fase 2 (por ejemplo, UC-54, UC-55 y UC-68) y los UC no iniciados
>   (UC-45 y UC-62).

## Correspondencia con el modelo de datos

| Colección | Tabla canónica |
| :--- | :--- |
| `usuarios` | `usuario` |
| `identidades` | `usuario_identidad` |
| `rolesGlobales` | `rol_global` |
| `perfiles` | `perfil` |
| `perfilesTyc` | `perfil_tyc` |
| `profesiones` | `profesion` |
| `caracteristicasTecnicas` | `caracteristica_tecnica` |
| `valoresCaracteristica` | `valor_caracteristica` |
| `caracteristicasPerfil` | `caracteristica_perfil` |
| `unidadesMedida` | `unidad_medida` |
| `habilidades` | `habilidad` |
| `proyectos` | `proyecto` |
| `tiposAcuerdoContrato` | `tipo_acuerdo_contrato` |
| `contratos` | `contrato` |
| `firmasContrato` | `firma_contrato` |
| `contratoVersiones` | `contrato_version` |
| `eventosContrato` | `contrato_evento` |
| `planificaciones` | `planificacion` |
| `rolesProyecto` | `rol_proyecto` |
| `ubicaciones` | `ubicacion` |
| `objetivos` | `objetivo` |
| `moodboards` | `moodboard` |
| `miembros` | `miembros_proyecto` |
| `invitacionesGenerales` | `invitacion_gral` |
| `invitacionesActividad` | `invitacion_actividad` |
| `postulacionesGenerales` | `postulacion_gral` |
| `postulacionesActividad` | `postulacion_actividad` |
| `requerimientosGenerales` | `requerimiento_gral_proyecto` |
| `requerimientosActividad` | `requerimiento_actividad` |
| `actividades` | `actividad` |
| `dependencias` | `dependencia_actividades` |
| `asignaciones` | `asignacion_actividad` |
| `agendas` | `agenda` |
| `jornadas` | `jornada_agenda` |
| `bloqueos` | `bloqueo_agenda` |
| `calificaciones` | `calificacion_perfil` |
| `reportes` | `reporte` |
| `auditorias` | `auditoria` |
| `tiposEvento` | `tipo_evento_auditoria` |
| `notificaciones` | `notificacion` |

## Mapa de cobertura

| UC | DSD |
| :--- | :--- |
| UC-01 | `ingresarCredenciales` |
| UC-03 | `registrarse` |
| UC-04 | `deshabilitarUsuario` |
| UC-05 | `habilitarUsuario` |
| UC-07 | `solicitarBaja` |
| UC-08 | `modificarDatosPersonales` |
| UC-09 | `autenticarConGoogle`, `completarRegistroGoogle` |
| UC-10 | `crearPerfil` |
| UC-11 | `editarPerfil` |
| UC-12 | `solicitarBajaPerfil`, `confirmarBajaPerfil` |
| UC-13 | `activarPerfil` |
| UC-15 | `reportarPerfil` |
| UC-17 | `marcarComoDisponible` |
| UC-18 | `marcarComoNoDisponible` |
| UC-23 | `configurarTerminosCondiciones`, `aceptarTerminosDePerfil` |
| UC-24 | `crearProyecto` |
| UC-25 | `publicarProyecto` |
| UC-26 | `darDeAltaPostulacionGeneral`, `darDeAltaPostulacionActividad` |
| UC-27 | `darDeBajaPostulacion` |
| UC-28 | `aceptarIncorporacion`, `aceptarIncorporacionAct`, `rechazarInvitacion` |
| UC-29 | `buscarProyectos`, `seleccionarProyecto` |
| UC-30 | `buscarInvitaciones`, `seleccionarInvitacion` |
| UC-31 | `invitarAProyecto`, `invitarAProyectoActividad`, `invitarAProyectoMultiple`, `invitarAProyectoActividadMultiple` |
| UC-32 | `darDeBajaInvitacion` |
| UC-33 | `solicitarCronograma` |
| UC-34 | `aceptarPostulacion`, `rechazarPostulacion` |
| UC-35 | `cancelarProyecto` |
| UC-36 | `modificarProyecto` |
| UC-37 | `finalizarProyecto`, `calificarMiembro` |
| UC-38 | `eliminarIntegrante` |
| UC-39 | `obtenerOpcionesContrato`, `previsualizarContrato`, `generarContrato` |
| UC-40 | `darseDeBaja` |
| UC-49 | `crearActividad`, `agregarPredecesora`, `buscarUbicacion`, `agregarUbicacion`, `agregarRequerimiento` |
| UC-50 | `modificarActividad` |
| UC-51 | `eliminarActividad`, `guardarMiembro` |
| UC-52 | `buscarActividades`, `seleccionarActividad` |
| UC-53 | `previsualizarModificacionContrato`, `modificarContrato` |
| UC-57 | `listarCaracteristicasTecnicas`, `registrarCaracteristicaTecnica`, `modificarCaracteristicaTecnica`, `eliminarCaracteristicaTecnica`, `agregarValorCaracteristica`, `modificarValorCaracteristica`, `eliminarValorCaracteristica` |
| UC-60 | `confirmarProyecto` |
| UC-67 | `listarAuditoria`, `listarTiposEventoAuditoria`, `obtenerResumenAuditoria`, `exportarAuditoria` |
| UC-70 | `buscarUsuario`, `verificarDisponibilidad`, `asignarActividad` |
| UC-71 | `configurarDiaCompleto`, `configurarDiaPartida`, `confirmarJornadaLaboral` |
| UC-72 | `buscarRequerimientosGenerales`, `seleccionarRequerimientoGeneral` |
| UC-73 | `buscarRequerimientosActividad`, `seleccionarRequerimientoActividad` |

**DSD auxiliares referenciados por los flujos**

| UC auxiliar | DSD |
| :--- | :--- |
| UC-06 | `buscarUsuarios`, `seleccionarUsuario` |
| UC-16 | `buscarPerfiles`, `seleccionarPerfil` |
| UC-48 | `buscarHabilidades` |
| UC-58 | `buscarCaracteristicasTecnicas` |
| UC-59 | `buscarProfesiones`, `seleccionarProfesion` |
| UC-74 | `buscarPostulaciones`, `seleccionarPostulacion` |

### Correspondencias con el DSS

Cuando el DSS y los Contratos usan nombres distintos, el DSD adopta la firma
del Contrato y conserva la referencia al UC:

| Mensaje del DSS | Operación del DSD |
| :--- | :--- |
| `enviarInvitacion(...)` | `invitarAProyecto(...)` |
| `crearNuevoPerfil(...)` / `registrarPerfil(...)` | `crearPerfil(...)` |
| `obtenerPostulaciones(...)` | `buscarPostulaciones(...)` |
| `obtenerProyecto(...)` | `seleccionarProyecto(...)` |
| `obtenerPerfil(...)` | `seleccionarPerfil(...)` |
| `obtenerActividades(...)` | `buscarActividades(...)` |
| `darDeAltaPostulación(...)` | `darDeAltaPostulacionGeneral(...)` / `darDeAltaPostulacionActividad(...)` |

---

## 1. Usuarios y administración

### DSD ingresarCredenciales(correo, contrasenia) | UC-01: Iniciar sesión

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `usuarios` (Colección de usuarios)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
- **Variables locales / Notas:**
  - `boolean encontrado = false`
  - `boolean credencialesValidas`
  - `boolean cuentaActiva`
- **Flujo de interacción:**
  1. El actor envía `sesion = ingresarCredenciales(correo, contrasenia)` a `:ModaLink`.
  2. Se inicializan las variables locales `encontrado`, `credencialesValidas` y `cuentaActiva`.
  3. **Bloque Bucle (`loop`)** [Mientras haya usuarios && `encontrado == false`]:
     - `:ModaLink` solicita a `usuarios`: `unUsuario = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unUsuario`: `encontrado = tieneCorreo(correo)`.
  4. **Bloque Alternativo (`alt`)** [!encontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Credenciales inválidas")`.
  5. **Bloque Alternativo (`alt`)** [encontrado]:
     - `:ModaLink` consulta a `unUsuario`: `credencialesValidas = verificarContrasenia(contrasenia)`.
     - **Bloque Alternativo (`alt`)** [!credencialesValidas]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Credenciales inválidas")`.
     - **Bloque Alternativo (`alt`)** [credencialesValidas]:
       - `:ModaLink` consulta a `unUsuario`: `cuentaActiva = estaActivo()`.
       - **Bloque Alternativo (`alt`)** [!cuentaActiva]:
         - `:ModaLink` retorna al actor: `informarCuentaDeshabilitada()`.
       - **Bloque Alternativo (`alt`)** [cuentaActiva]:
         - `:ModaLink` solicita a `unUsuario`: `sesion = iniciarSesion()`.
         - `:ModaLink` retorna `sesion` al actor: `otorgarAcceso(sesion)`.

---

### DSD registrarse(correo, contrasenia, nombre, apellido, fechaNac, genero) | UC-03: Registrarse

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `usuarios` (Colección de usuarios)
  - `rolesGlobales` (Colección de roles globales)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
  - `unRol : RolGlobal` (Instancia de la entidad RolGlobal)
- **Variables locales / Notas:**
  - `boolean correoDuplicado = false`
  - `boolean datosValidos`
  - `boolean edadValida`
- **Flujo de interacción:**
  1. El actor envía `unUsuario = registrarse(correo, contrasenia, nombre, apellido, fechaNac, genero)` a `:ModaLink`.
  2. Se inicializan las variables locales `correoDuplicado`, `datosValidos` y `edadValida`.
  3. **Bloque Bucle (`loop`)** [Mientras haya usuarios && `correoDuplicado == false`]:
     - `:ModaLink` solicita a `usuarios`: `unUsuario = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unUsuario`: `correoDuplicado = tieneCorreo(correo)`.
  4. `:ModaLink` valida los datos: `datosValidos = validarCampos(nombre, apellido, correo, contrasenia)`.
  5. `:ModaLink` valida la edad: `edadValida = esMayorDeEdad(fechaNac)`.
  6. **Bloque Alternativo (`alt`)** [correoDuplicado || !datosValidos || !edadValida]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Correo, datos o edad inválidos")`.
  7. **Bloque Alternativo (`alt`)** [!correoDuplicado && datosValidos && edadValida]:
     - `:ModaLink` solicita a `rolesGlobales`: `unRol = buscarPorCodigo("USUARIO")`.
     - `:ModaLink` crea la instancia: `unUsuario = registrar(correo, contrasenia, nombre, apellido, fechaNac, genero, unRol)`.
     - `:ModaLink` agrega la instancia: `usuarios.agregar(unUsuario)`.
     - `:ModaLink` retorna al actor: `registroExitoso(unUsuario)`.

---

### DSD deshabilitarUsuario(unUsuario, motivo, duracion) | UC-04: Deshabilitar usuario

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `usuarios` (Colección de usuarios)
  - `perfiles` (Colección de perfiles)
  - `notificaciones` (Colección de notificaciones)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
- **Variables locales / Notas:**
  - `boolean estaActivo`
  - `boolean perteneceAlUsuario`
- **Flujo de interacción:**
  1. El actor envía `deshabilitarUsuario(unUsuario, motivo, duracion)` a `:ModaLink`.
  2. Se inicializan las variables locales `estaActivo` y `perteneceAlUsuario`.
  3. `:ModaLink` consulta a `unUsuario`: `estaActivo = estaActivo()`.
  4. **Bloque Alternativo (`alt`)** [!estaActivo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El usuario no se encuentra activo")`.
  5. **Bloque Alternativo (`alt`)** [estaActivo]:
     - `:ModaLink` envía a `unUsuario`: `setEstado(DESHABILITADO)`.
     - `:ModaLink` envía a `unUsuario`: `setMotivoDeshabilitacion(motivo)`.
     - **Bloque Opcional (`opt`)** [duracion != null]:
       - `:ModaLink` envía a `unUsuario`: `setFechaHastaDeshabilitacion(ahora + duracion)`.
     - **Bloque Bucle (`loop`)** [Mientras haya perfiles del usuario]:
       - `:ModaLink` solicita a `perfiles`: `unPerfil = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unPerfil`: `perteneceAlUsuario = perteneceA(unUsuario)`.
       - **Bloque Alternativo (`alt`)** [perteneceAlUsuario]:
         - `:ModaLink` envía a `unPerfil`: `setEstado(DESHABILITADO)`.
         - `:ModaLink` envía a `unPerfil`: `ocultar()`.
     - `:ModaLink` agrega a `notificaciones`: `notificarDeshabilitacion(unUsuario, motivo, duracion)`.
     - `:ModaLink` retorna al actor: `operacionExitosa()`.

---

### DSD habilitarUsuario(unUsuario) | UC-05: Habilitar usuario

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `perfiles` (Colección de perfiles)
  - `notificaciones` (Colección de notificaciones)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
- **Variables locales / Notas:**
  - `boolean estaDeshabilitado`
  - `boolean perteneceAlUsuario`
- **Flujo de interacción:**
  1. El actor envía `habilitarUsuario(unUsuario)` a `:ModaLink`.
  2. Se inicializan las variables locales `estaDeshabilitado` y `perteneceAlUsuario`.
  3. `:ModaLink` consulta a `unUsuario`: `estaDeshabilitado = estaDeshabilitado()`.
  4. **Bloque Alternativo (`alt`)** [!estaDeshabilitado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El usuario no se encuentra deshabilitado")`.
  5. **Bloque Alternativo (`alt`)** [estaDeshabilitado]:
     - `:ModaLink` envía a `unUsuario`: `setEstado(ACTIVO)`.
     - `:ModaLink` envía a `unUsuario`: `limpiarMotivoDeshabilitacion()`.
     - `:ModaLink` envía a `unUsuario`: `limpiarFechaHastaDeshabilitacion()`.
     - **Bloque Bucle (`loop`)** [Mientras haya perfiles del usuario]:
       - `:ModaLink` solicita a `perfiles`: `unPerfil = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unPerfil`: `perteneceAlUsuario = perteneceA(unUsuario)`.
       - **Bloque Alternativo (`alt`)** [perteneceAlUsuario]:
         - `:ModaLink` envía a `unPerfil`: `setEstado(ACTIVO)`.
         - `:ModaLink` envía a `unPerfil`: `mostrar()`.
     - `:ModaLink` agrega a `notificaciones`: `notificarHabilitacion(unUsuario)`.
     - `:ModaLink` retorna al actor: `operacionExitosa()`.

---

### DSD buscarUsuarios(nombre, correo, estado) | UC-06: Buscar usuario

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `usuarios` (Colección de usuarios)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
- **Variables locales / Notas:**
  - `Lista usuariosFiltrados = nueva Lista()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `usuarios = buscarUsuarios(nombre, correo, estado)` a `:ModaLink`.
  2. Se inicializan las variables locales `usuariosFiltrados` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya usuarios]:
     - `:ModaLink` solicita a `usuarios`: `unUsuario = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unUsuario`: `sePuedeAgregar = cumpleFiltros(nombre, correo, estado)`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `usuariosFiltrados.agregar(unUsuario)`.
  4. `:ModaLink` retorna `usuariosFiltrados` al actor.

---

### DSD seleccionarUsuario(idUsuario, usuarios) | UC-06: Buscar usuario

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `usuarios` (Colección de usuarios)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unUsuario = seleccionarUsuario(idUsuario, usuarios)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya usuarios && `retorno == false`]:
     - `:ModaLink` solicita a `usuarios`: `unUsuario = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unUsuario`: `retorno = tieneId(idUsuario)`.
  4. `:ModaLink` retorna la instancia encontrada `unUsuario` al actor.

---

### DSD solicitarBaja(idUsuario) | UC-07: Solicitar baja en el sistema

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `usuarios` (Colección de usuarios)
  - `perfiles` (Colección de perfiles)
  - `notificaciones` (Colección de notificaciones)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
- **Variables locales / Notas:**
  - `boolean encontrado = false`
  - `boolean perteneceAlUsuario`
- **Flujo de interacción:**
  1. El actor envía `solicitarBaja(idUsuario)` a `:ModaLink`.
  2. Se inicializan las variables locales `encontrado` y `perteneceAlUsuario`.
  3. **Bloque Bucle (`loop`)** [Mientras haya usuarios && `encontrado == false`]:
     - `:ModaLink` solicita a `usuarios`: `unUsuario = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unUsuario`: `encontrado = tieneId(idUsuario)`.
  4. **Bloque Alternativo (`alt`)** [!encontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Usuario no encontrado")`.
  5. **Bloque Alternativo (`alt`)** [encontrado]:
     - `:ModaLink` solicita al actor: `solicitarConfirmacion()`.
     - **Bloque Opcional (`opt`)** [el actor confirma]:
       - `:ModaLink` envía a `unUsuario`: `setEstado(PENDIENTE_BAJA)`.
       - `:ModaLink` envía a `unUsuario`: `setFechaSolicitudBaja(ahora)`.
       - **Bloque Bucle (`loop`)** [Mientras haya perfiles del usuario]:
         - `:ModaLink` solicita a `perfiles`: `unPerfil = obtenerSiguiente()`.
         - `:ModaLink` consulta a `unPerfil`: `perteneceAlUsuario = perteneceA(unUsuario)`.
         - **Bloque Alternativo (`alt`)** [perteneceAlUsuario]:
           - `:ModaLink` envía a `unPerfil`: `setEstado(PENDIENTE_BAJA)`.
           - `:ModaLink` envía a `unPerfil`: `setFechaSolicitudBaja(ahora)`.
           - `:ModaLink` envía a `unPerfil`: `ocultar()`.
       - `:ModaLink` agrega a `notificaciones`: `notificarBaja(unUsuario, 30)`.
       - `:ModaLink` retorna al actor: `cuentaRegresivaIniciada(unUsuario, 30)`.
       - `:ModaLink` retorna al actor: `cerrarSesion()`.

---

### DSD modificarDatosPersonales(nombre, apellido, genero, fechaNac, idUbicacion) | UC-08: Modificar datos personales

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `ubicaciones` (Colección de ubicaciones)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
  - `unaUbicacion : Ubicacion` (Instancia de la entidad Ubicacion)
- **Variables locales / Notas:**
  - `boolean datosValidos`
  - `boolean ubicacionEncontrada = false`
- **Flujo de interacción:**
  1. El actor envía `unUsuario = modificarDatosPersonales(nombre, apellido, genero, fechaNac, idUbicacion)` a `:ModaLink`.
  2. Se inicializan las variables locales `datosValidos` y `ubicacionEncontrada`.
  3. `:ModaLink` valida los datos: `datosValidos = validarCampos(nombre, apellido, genero, fechaNac)`.
  4. **Bloque Alternativo (`alt`)** [!datosValidos]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Datos personales inválidos")`.
  5. **Bloque Alternativo (`alt`)** [datosValidos]:
     - `:ModaLink` envía a `unUsuario`: `setNombre(nombre)`.
     - `:ModaLink` envía a `unUsuario`: `setApellido(apellido)`.
     - `:ModaLink` envía a `unUsuario`: `setGenero(genero)`.
     - `:ModaLink` envía a `unUsuario`: `setFechaNacimiento(fechaNac)`.
     - **Bloque Opcional (`opt`)** [idUbicacion != null]:
       - **Bloque Bucle (`loop`)** [Mientras haya ubicaciones && `ubicacionEncontrada == false`]:
         - `:ModaLink` solicita a `ubicaciones`: `unaUbicacion = obtenerSiguiente()`.
         - `:ModaLink` consulta a `unaUbicacion`: `ubicacionEncontrada = tieneId(idUbicacion)`.
       - **Bloque Alternativo (`alt`)** [ubicacionEncontrada]:
         - `:ModaLink` envía a `unUsuario`: `setUbicacion(unaUbicacion)`.
     - `:ModaLink` retorna al actor: `operacionExitosa()`.

---

### DSD autenticarConGoogle(sub, correo, emailVerificado, nombre, apellido) | UC-09: Autenticar mediante Google OAuth

- **Actores:** Usuario, Google OAuth
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `identidades` (Colección de identidades)
  - `usuarios` (Colección de usuarios)
  - `unaIdentidad : UsuarioIdentidad` (Instancia de la entidad UsuarioIdentidad)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
- **Variables locales / Notas:**
  - `boolean existeIdentidad = false`
  - `boolean existeUsuario = false`
  - `String ticket`
- **Flujo de interacción:**
  1. El actor Google OAuth envía `resultado = autenticarConGoogle(sub, correo, emailVerificado, nombre, apellido)` a `:ModaLink`.
  2. Se inicializan las variables locales `existeIdentidad`, `existeUsuario` y `ticket`.
  3. **Bloque Alternativo (`alt`)** [!emailVerificado]:
     - `:ModaLink` retorna al actor: `Excepcion("Cuenta de Google no verificada")`.
  4. **Bloque Bucle (`loop`)** [Mientras haya identidades && `existeIdentidad == false`]:
     - `:ModaLink` solicita a `identidades`: `unaIdentidad = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaIdentidad`: `existeIdentidad = coincideCon(GOOGLE, sub)`.
  5. **Bloque Alternativo (`alt`)** [existeIdentidad]:
     - `:ModaLink` solicita a `unaIdentidad`: `unUsuario = obtenerUsuario()`.
     - `:ModaLink` envía a `unUsuario`: `reactivarSiPendienteBaja()`.
     - `:ModaLink` solicita a `unUsuario`: `resultado = iniciarSesion()`.
  6. **Bloque Alternativo (`alt`)** [!existeIdentidad]:
     - **Bloque Bucle (`loop`)** [Mientras haya usuarios && `existeUsuario == false`]:
       - `:ModaLink` solicita a `usuarios`: `unUsuario = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unUsuario`: `existeUsuario = tieneCorreo(correo)`.
     - **Bloque Alternativo (`alt`)** [existeUsuario]:
       - `:ModaLink` solicita a `unUsuario`: `unaIdentidad = vincularIdentidad(GOOGLE, sub, correo)`.
       - `identidades.agregar(unaIdentidad)`.
       - `:ModaLink` solicita a `unUsuario`: `resultado = iniciarSesion()`.
     - **Bloque Alternativo (`alt`)** [!existeUsuario]:
       - `:ModaLink` crea el ticket: `ticket = crearTicketRegistro(sub, correo, nombre, apellido)`.
       - `:ModaLink` retorna al actor: `RegistroPendiente(ticket)`.
  7. `:ModaLink` retorna `resultado` al actor.

---

### DSD completarRegistroGoogle(ticket, nombre, apellido, dni, fechaNacimiento, genero) | UC-09: Autenticar mediante Google OAuth

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `usuarios` (Colección de usuarios)
  - `identidades` (Colección de identidades)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
  - `unaIdentidad : UsuarioIdentidad` (Instancia de la entidad UsuarioIdentidad)
- **Variables locales / Notas:**
  - `boolean ticketValido`
  - `boolean dniDuplicado = false`
  - `boolean datosValidos`
- **Flujo de interacción:**
  1. El actor envía `resultado = completarRegistroGoogle(ticket, nombre, apellido, dni, fechaNacimiento, genero)` a `:ModaLink`.
  2. Se inicializan las variables locales `ticketValido`, `dniDuplicado` y `datosValidos`.
  3. `:ModaLink` valida el ticket: `ticketValido = validarTicket(ticket)`.
  4. **Bloque Bucle (`loop`)** [Mientras haya usuarios && `dniDuplicado == false`]:
     - `:ModaLink` solicita a `usuarios`: `unUsuario = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unUsuario`: `dniDuplicado = tieneDni(dni)`.
  5. `:ModaLink` valida los datos: `datosValidos = validarCampos(nombre, apellido, dni, fechaNacimiento, genero)`.
  6. **Bloque Alternativo (`alt`)** [!ticketValido || dniDuplicado || !datosValidos]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Ticket, DNI o datos inválidos")`.
  7. **Bloque Alternativo (`alt`)** [ticketValido && !dniDuplicado && datosValidos]:
     - `:ModaLink` crea la instancia: `unUsuario = registrar(nombre, apellido, dni, fechaNacimiento, genero, ticket.correo)`.
     - `usuarios.agregar(unUsuario)`.
     - `:ModaLink` crea la identidad: `unaIdentidad = registrar(GOOGLE, ticket.sub, ticket.correo)`.
     - `identidades.agregar(unaIdentidad)`.
     - `:ModaLink` solicita a `unUsuario`: `resultado = iniciarSesion()`.
     - `:ModaLink` retorna `resultado` al actor.

---

### DSD configurarTerminosCondiciones(unPerfil, texto) | UC-23: Configurar términos y condiciones

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `perfilesTyc` (Colección de versiones de términos y condiciones)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unaVersion : PerfilTyc` (Instancia de la entidad PerfilTyc)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
  - `boolean textoValido`
  - `boolean mismaVersion`
- **Flujo de interacción:**
  1. El actor envía `resultado = configurarTerminosCondiciones(unPerfil, texto)` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilActivo`, `textoValido` y `mismaVersion`.
  3. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  4. `:ModaLink` valida el texto: `textoValido = texto != null && longitud(texto) >= 1 && longitud(texto) <= 5000`.
  5. **Bloque Alternativo (`alt`)** [!perfilActivo || !textoValido]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Perfil inactivo o texto inválido")`.
  6. **Bloque Alternativo (`alt`)** [perfilActivo && textoValido]:
     - `:ModaLink` solicita a `perfilesTyc`: `unaVersion = buscarVersionVigente(unPerfil)`.
     - **Bloque Alternativo (`alt`)** [unaVersion != null]:
       - `:ModaLink` consulta a `unaVersion`: `mismaVersion = tieneTexto(texto)`.
     - **Bloque Alternativo (`alt`)** [unaVersion == null || !mismaVersion]:
       - **Bloque Opcional (`opt`)** [unaVersion != null]:
         - `:ModaLink` envía a `unaVersion`: `cerrar(ahora)`.
       - `:ModaLink` crea la versión: `unaVersion = registrar(unPerfil, texto, ahora, null)`.
       - `perfilesTyc.agregar(unaVersion)`.
     - `:ModaLink` retorna a `unPerfil`: `TerminosCondicionesResponse(unaVersion)`.

---

### DSD aceptarTerminosDePerfil(unPerfil, idTyc, aceptaTerminos) | UC-23: Configurar términos y condiciones

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `perfilesTyc` (Colección de versiones de términos y condiciones)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unaVersion : PerfilTyc` (Instancia de la entidad PerfilTyc)
- **Variables locales / Notas:**
  - `boolean tieneVersionVigente`
  - `boolean versionVigente`
- **Flujo de interacción:**
  1. El actor envía `idTycAceptado = aceptarTerminosDePerfil(unPerfil, idTyc, aceptaTerminos)` a `:ModaLink`.
  2. Se inicializan las variables locales `tieneVersionVigente` y `versionVigente`.
  3. `:ModaLink` solicita a `perfilesTyc`: `unaVersion = buscarVersionVigente(unPerfil)`.
  4. `:ModaLink` evalúa: `tieneVersionVigente = unaVersion != null`.
  5. **Bloque Alternativo (`alt`)** [!tieneVersionVigente]:
     - `:ModaLink` retorna `idTycAceptado = null` al actor.
  6. **Bloque Alternativo (`alt`)** [tieneVersionVigente]:
     - **Bloque Alternativo (`alt`)** [!aceptaTerminos]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Debe aceptar la versión vigente de los términos y condiciones")`.
     - **Bloque Alternativo (`alt`)** [aceptaTerminos]:
       - `:ModaLink` consulta a `unaVersion`: `versionVigente = tieneId(idTyc)`.
       - **Bloque Alternativo (`alt`)** [!versionVigente]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("La versión enviada no es la vigente")`.
       - **Bloque Alternativo (`alt`)** [versionVigente]:
         - `:ModaLink` retorna `idTycAceptado = idTyc` al actor.

---

### DSD listarAuditoria(fechaDesde, fechaHasta, tipos, page, size) | UC-67: Generar informe de auditoría

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `auditorias` (Colección de eventos de auditoría)
  - `tiposEvento` (Colección de tipos de evento)
  - `unEvento : Auditoria` (Instancia de la entidad Auditoria)
  - `unTipo : TipoEventoAuditoria` (Instancia de la entidad TipoEventoAuditoria)
- **Variables locales / Notas:**
  - `Lista eventosFiltrados = nueva Lista()`
  - `boolean fechasValidas`
  - `boolean sePuedeAgregar`
  - `boolean tipoValido = true`
- **Flujo de interacción:**
  1. El actor envía `pagina = listarAuditoria(fechaDesde, fechaHasta, tipos, page, size)` a `:ModaLink`.
  2. Se inicializan las variables locales `eventosFiltrados`, `fechasValidas`, `sePuedeAgregar` y `tipoValido`.
  3. `:ModaLink` valida las fechas: `fechasValidas = fechaDesde != null && fechaHasta != null && fechaDesde <= fechaHasta`.
  4. **Bloque Alternativo (`alt`)** [!fechasValidas]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Fechas obligatorias o rango inválido")`.
  5. **Bloque Opcional (`opt`)** [tipos != null && tipos no vacío]:
     - **Bloque Bucle (`loop`)** [Mientras haya tipos && `tipoValido`]:
       - `:ModaLink` solicita a `tiposEvento`: `unTipo = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unTipo`: `tipoValido = tieneCodigo(tipos)`.
  6. **Bloque Alternativo (`alt`)** [!tipoValido]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Tipo de evento inválido")`.
  7. **Bloque Bucle (`loop`)** [Mientras haya auditorías]:
     - `:ModaLink` solicita a `auditorias`: `unEvento = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unEvento`: `sePuedeAgregar = cumpleFiltros(fechaDesde, fechaHasta, tipos)`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `eventosFiltrados.agregar(unEvento)`.
  8. `:ModaLink` ordena `eventosFiltrados` por `fechaHora DESC, idAuditoria DESC`.
  9. `:ModaLink` pagina `eventosFiltrados` según `page` y `size`.
  10. `:ModaLink` retorna la página al actor.

---

### DSD listarTiposEventoAuditoria() | UC-67: Generar informe de auditoría

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `tiposEvento` (Colección de tipos de evento)
  - `unTipo : TipoEventoAuditoria` (Instancia de la entidad TipoEventoAuditoria)
- **Variables locales / Notas:**
  - `Lista tiposActivos = nueva Lista()`
  - `boolean estaActivo`
- **Flujo de interacción:**
  1. El actor envía `tiposActivos = listarTiposEventoAuditoria()` a `:ModaLink`.
  2. Se inicializan las variables locales `tiposActivos` y `estaActivo`.
  3. **Bloque Bucle (`loop`)** [Mientras haya tipos de evento]:
     - `:ModaLink` solicita a `tiposEvento`: `unTipo = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unTipo`: `estaActivo = estaActivo()`.
     - **Bloque Alternativo (`alt`)** [estaActivo]:
       - `tiposActivos.agregar(unTipo)`.
  4. `:ModaLink` retorna `tiposActivos` al actor.

---

### DSD obtenerResumenAuditoria(fechaDesde, fechaHasta, tipos) | UC-67: Generar informe de auditoría

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `auditorias` (Colección de eventos de auditoría)
  - `unEvento : Auditoria` (Instancia de la entidad Auditoria)
- **Variables locales / Notas:**
  - `int totalRegistros = 0`
  - `Map porTipoEvento = nuevo Map()`
  - `Map porEntidad = nuevo Map()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `resumen = obtenerResumenAuditoria(fechaDesde, fechaHasta, tipos)` a `:ModaLink`.
  2. Se inicializan las variables locales `totalRegistros`, `porTipoEvento` y `porEntidad`.
  3. **Bloque Bucle (`loop`)** [Mientras haya auditorías]:
     - `:ModaLink` solicita a `auditorias`: `unEvento = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unEvento`: `sePuedeAgregar = cumpleFiltros(fechaDesde, fechaHasta, tipos)`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `totalRegistros = totalRegistros + 1`.
       - `porTipoEvento.acumular(unEvento.obtenerTipoEvento())`.
       - `porEntidad.acumular(unEvento.obtenerEntidadAfectada())`.
  4. `:ModaLink` retorna al actor: `AuditoriaResumenResponse(totalRegistros, porTipoEvento, porEntidad)`.

---

### DSD exportarAuditoria(formato, fechaDesde, fechaHasta, tipos) | UC-67: Generar informe de auditoría

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `auditorias` (Colección de eventos de auditoría)
  - `unEvento : Auditoria` (Instancia de la entidad Auditoria)
- **Variables locales / Notas:**
  - `boolean formatoValido`
  - `Lista eventos`
  - `boolean exportacionExcedida`
- **Flujo de interacción:**
  1. El actor envía `archivo = exportarAuditoria(formato, fechaDesde, fechaHasta, tipos)` a `:ModaLink`.
  2. Se inicializan las variables locales `formatoValido` y `eventos`.
  3. `:ModaLink` valida el formato: `formatoValido = formato == "CSV" || formato == "PDF"`.
  4. **Bloque Alternativo (`alt`)** [!formatoValido]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Formato de exportación inválido")`.
  5. **Bloque Alternativo (`alt`)** [formatoValido]:
     - `:ModaLink` invoca la búsqueda: `eventos = buscarEventosAuditoria(fechaDesde, fechaHasta, tipos)`.
     - `:ModaLink` evalúa: `exportacionExcedida = eventos.cantidad() > limiteExportacion`.
     - **Bloque Alternativo (`alt`)** [exportacionExcedida]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("La exportación supera el máximo permitido")`.
     - **Bloque Alternativo (`alt`)** [!exportacionExcedida]:
       - `:ModaLink` genera el archivo: `archivo = generarArchivo(formato, eventos)`.
       - `:ModaLink` retorna `archivo` al actor.

---

### DSD listarCaracteristicasTecnicas() | UC-57: Gestionar características técnicas por profesión

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `caracteristicasTecnicas` (Colección de características técnicas)
  - `caracteristicasPerfil` (Colección de características de perfil)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `unaCaracteristica : CaracteristicaTecnica` (Instancia de la entidad CaracteristicaTecnica)
  - `unUso : CaracteristicaPerfil` (Instancia de la entidad CaracteristicaPerfil)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
- **Variables locales / Notas:**
  - `Lista caracteristicas = nueva Lista()`
  - `boolean enUso`
- **Flujo de interacción:**
  1. El actor envía `caracteristicas = listarCaracteristicasTecnicas()` a `:ModaLink`.
  2. Se inicializan las variables locales `caracteristicas` y `enUso`.
  3. **Bloque Bucle (`loop`)** [Mientras haya características técnicas]:
     - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaCaracteristica`: `enUso = estaEnUso(caracteristicasPerfil, requerimientosActividad, requerimientosGenerales)`.
     - `:ModaLink` agrega a `caracteristicas`: `CaracteristicaTecnicaResponse(unaCaracteristica, enUso)`.
  4. `:ModaLink` ordena `caracteristicas` por código.
  5. `:ModaLink` retorna `caracteristicas` al actor.

---

### DSD registrarCaracteristicaTecnica(codigo, nombre, idUnidad, idProfesion, tipoDato, valores[]) | UC-57: Gestionar características técnicas por profesión

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `caracteristicasTecnicas` (Colección de características técnicas)
  - `profesiones` (Colección de profesiones)
  - `unidadesMedida` (Colección de unidades de medida)
  - `valoresCaracteristica` (Colección de valores de característica)
  - `unaCaracteristica : CaracteristicaTecnica` (Instancia de la entidad CaracteristicaTecnica)
  - `unaProfesion : Profesion` (Instancia de la entidad Profesion)
  - `unaUnidad : UnidadMedida` (Instancia de la entidad UnidadMedida)
  - `unValor : ValorCaracteristica` (Instancia de la entidad ValorCaracteristica)
- **Variables locales / Notas:**
  - `boolean codigoDuplicado = false`
  - `boolean profesionValida = false`
  - `boolean tipoValido`
  - `boolean unidadCompatible = true`
  - `boolean valoresValidos = true`
- **Flujo de interacción:**
  1. El actor envía `unaCaracteristica = registrarCaracteristicaTecnica(codigo, nombre, idUnidad, idProfesion, tipoDato, valores[])` a `:ModaLink`.
  2. Se inicializan las variables locales `codigoDuplicado`, `profesionValida`, `tipoValido`, `unidadCompatible` y `valoresValidos`.
  3. `:ModaLink` valida el tipo de dato: `tipoValido = tipoDato == "ENUMERADO" || tipoDato == "TEXTO" || tipoDato == "NUMERICO"`.
  4. **Bloque Alternativo (`alt`)** [!tipoValido]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("tipoDato debe ser ENUMERADO, TEXTO o NUMERICO")`.
  5. **Bloque Alternativo (`alt`)** [tipoValido]:
     - **Bloque Bucle (`loop`)** [Mientras haya características técnicas && `codigoDuplicado == false`]:
       - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaCaracteristica`: `codigoDuplicado = tieneCodigo(codigo)`.
     - **Bloque Alternativo (`alt`)** [codigoDuplicado]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Ya existe una característica técnica con ese código")`.
     - **Bloque Alternativo (`alt`)** [!codigoDuplicado]:
       - **Bloque Bucle (`loop`)** [Mientras haya profesiones && `profesionValida == false`]:
         - `:ModaLink` solicita a `profesiones`: `unaProfesion = obtenerSiguiente()`.
         - `:ModaLink` consulta a `unaProfesion`: `profesionValida = tieneId(idProfesion)`.
       - **Bloque Alternativo (`alt`)** [!profesionValida]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("La profesión no existe")`.
       - **Bloque Alternativo (`alt`)** [profesionValida]:
         - **Bloque Alternativo (`alt`)** [idUnidad == null]:
           - `unaUnidad = null`.
         - **Bloque Opcional (`opt`)** [idUnidad != null]:
           - `:ModaLink` solicita a `unidadesMedida`: `unaUnidad = buscarPorId(idUnidad)`.
           - **Bloque Alternativo (`alt`)** [unaUnidad == null]:
             - `:ModaLink` retorna una excepción al actor: `Excepcion("La unidad de medida no existe")`.
           - **Bloque Alternativo (`alt`)** [unaUnidad != null]:
             - `:ModaLink` consulta a `unaUnidad`: `unidadCompatible = permiteTipoDato(tipoDato)`.
             - **Bloque Alternativo (`alt`)** [!unidadCompatible]:
               - `:ModaLink` retorna una excepción al actor: `Excepcion("La unidad no admite el tipo de dato indicado")`.
         - **Bloque Alternativo (`alt`)** [tipoDato != "ENUMERADO" && valores != null && !valores.isEmpty()]:
           - `:ModaLink` retorna una excepción al actor: `Excepcion("Solo las características ENUMERADO admiten valores")`.
         - **Bloque Alternativo (`alt`)** [unidadCompatible && (tipoDato == "ENUMERADO" || valores == null || valores.isEmpty())]:
           - `:ModaLink` crea la característica: `unaCaracteristica = registrar(codigo, nombre, unaUnidad, unaProfesion, tipoDato)`.
           - `caracteristicasTecnicas.agregar(unaCaracteristica)`.
           - **Bloque Opcional (`opt`)** [tipoDato == "ENUMERADO" && valores != null]:
             - **Bloque Bucle (`loop`)** [Mientras haya valores por cargar]:
               - `:ModaLink` solicita a `valores`: `unValor = obtenerSiguiente()`.
               - `:ModaLink` consulta a `valoresCaracteristica`: `valoresValidos = !existeEtiqueta(unaCaracteristica, unValor.codigo())`.
               - **Bloque Alternativo (`alt`)** [valoresValidos]:
                 - `:ModaLink` crea el valor: `unValor = registrar(unaCaracteristica, unValor.codigo(), unValor.colorHex())`.
                 - `valoresCaracteristica.agregar(unValor)`.
               - **Bloque Alternativo (`alt`)** [!valoresValidos]:
                 - `:ModaLink` retorna una excepción al actor: `Excepcion("Valor de catálogo duplicado")`.
           - `:ModaLink` retorna al actor: `CaracteristicaTecnicaResponse(unaCaracteristica, false)`.

---

### DSD modificarCaracteristicaTecnica(idCaracteristica, codigo, nombre, idUnidad, idProfesion, tipoDato, valores[]) | UC-57: Gestionar características técnicas por profesión

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `caracteristicasTecnicas` (Colección de características técnicas)
  - `caracteristicasPerfil` (Colección de características de perfil)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `profesiones` (Colección de profesiones)
  - `unidadesMedida` (Colección de unidades de medida)
  - `unaCaracteristica : CaracteristicaTecnica` (Instancia de la entidad CaracteristicaTecnica)
  - `unaProfesion : Profesion` (Instancia de la entidad Profesion)
  - `unaUnidad : UnidadMedida` (Instancia de la entidad UnidadMedida)
- **Variables locales / Notas:**
  - `boolean encontrada = false`
  - `boolean enUso`
  - `boolean soloNombreValido`
  - `boolean codigoDuplicado = false`
  - `boolean profesionValida = false`
  - `boolean cambioDeTipoInvalido`
  - `boolean unidadCompatible = true`
- **Flujo de interacción:**
  1. El actor envía `unaCaracteristica = modificarCaracteristicaTecnica(idCaracteristica, codigo, nombre, idUnidad, idProfesion, tipoDato, valores[])` a `:ModaLink`.
  2. Se inicializan las variables locales `encontrada`, `enUso`, `soloNombreValido`, `codigoDuplicado`, `profesionValida`, `cambioDeTipoInvalido` y `unidadCompatible`.
  3. **Bloque Bucle (`loop`)** [Mientras haya características técnicas && `encontrada == false`]:
     - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaCaracteristica`: `encontrada = tieneId(idCaracteristica)`.
  4. **Bloque Alternativo (`alt`)** [!encontrada]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Característica técnica inexistente")`.
  5. **Bloque Alternativo (`alt`)** [encontrada]:
     - `:ModaLink` consulta a `unaCaracteristica`: `enUso = estaEnUso(caracteristicasPerfil, requerimientosActividad, requerimientosGenerales)`.
     - **Bloque Alternativo (`alt`)** [enUso]:
       - `:ModaLink` consulta a `unaCaracteristica`: `soloNombreValido = soloCambiaNombre(codigo, idUnidad, idProfesion, tipoDato)`.
       - **Bloque Alternativo (`alt`)** [!soloNombreValido]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("La característica está en uso; sólo puede modificarse el nombre")`.
       - **Bloque Alternativo (`alt`)** [soloNombreValido]:
         - `:ModaLink` envía a `unaCaracteristica`: `setNombre(nombre)`.
         - `:ModaLink` retorna al actor: `CaracteristicaTecnicaResponse(unaCaracteristica, enUso)`.
     - **Bloque Alternativo (`alt`)** [!enUso]:
       - **Bloque Bucle (`loop`)** [Mientras haya características técnicas && `codigoDuplicado == false`]:
         - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
         - `:ModaLink` consulta a `unaCaracteristica`: `codigoDuplicado = tieneCodigo(codigo) && !tieneId(idCaracteristica)`.
       - **Bloque Alternativo (`alt`)** [codigoDuplicado]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("Ya existe una característica técnica con ese código")`.
       - **Bloque Alternativo (`alt`)** [!codigoDuplicado]:
         - **Bloque Bucle (`loop`)** [Mientras haya profesiones && `profesionValida == false`]:
           - `:ModaLink` solicita a `profesiones`: `unaProfesion = obtenerSiguiente()`.
           - `:ModaLink` consulta a `unaProfesion`: `profesionValida = tieneId(idProfesion)`.
         - **Bloque Alternativo (`alt`)** [!profesionValida]:
           - `:ModaLink` retorna una excepción al actor: `Excepcion("La profesión no existe")`.
         - **Bloque Alternativo (`alt`)** [profesionValida]:
           - `:ModaLink` consulta a `unaCaracteristica`: `cambioDeTipoInvalido = esEnumerada() && tipoDato != "ENUMERADO" && tieneValores()`.
           - **Bloque Alternativo (`alt`)** [cambioDeTipoInvalido]:
             - `:ModaLink` retorna una excepción al actor: `Excepcion("Elimine los valores antes de cambiar el tipo")`.
           - **Bloque Alternativo (`alt`)** [!cambioDeTipoInvalido]:
             - **Bloque Alternativo (`alt`)** [idUnidad == null]:
               - `unaUnidad = null`.
             - **Bloque Opcional (`opt`)** [idUnidad != null]:
               - `:ModaLink` solicita a `unidadesMedida`: `unaUnidad = buscarPorId(idUnidad)`.
               - **Bloque Alternativo (`alt`)** [unaUnidad == null]:
                 - `:ModaLink` retorna una excepción al actor: `Excepcion("La unidad de medida no existe")`.
               - **Bloque Alternativo (`alt`)** [unaUnidad != null]:
                 - `:ModaLink` consulta a `unaUnidad`: `unidadCompatible = permiteTipoDato(tipoDato)`.
                 - **Bloque Alternativo (`alt`)** [!unidadCompatible]:
                   - `:ModaLink` retorna una excepción al actor: `Excepcion("La unidad no admite el tipo de dato indicado")`.
             - **Bloque Alternativo (`alt`)** [unidadCompatible && tipoDato != "ENUMERADO" && valores != null && !valores.isEmpty()]:
               - `:ModaLink` retorna una excepción al actor: `Excepcion("Solo las características ENUMERADO admiten valores")`.
             - **Bloque Alternativo (`alt`)** [unidadCompatible && (tipoDato == "ENUMERADO" || valores == null || valores.isEmpty())]:
               - `:ModaLink` envía a `unaCaracteristica`: `setCodigo(codigo)`.
               - `:ModaLink` envía a `unaCaracteristica`: `setNombre(nombre)`.
               - `:ModaLink` envía a `unaCaracteristica`: `setUnidad(unaUnidad)`.
               - `:ModaLink` envía a `unaCaracteristica`: `setTipoDato(tipoDato)`.
               - `:ModaLink` envía a `unaCaracteristica`: `setProfesion(unaProfesion)`.
               - `:ModaLink` retorna al actor: `CaracteristicaTecnicaResponse(unaCaracteristica, enUso)`.

---

### DSD eliminarCaracteristicaTecnica(idCaracteristica) | UC-57: Gestionar características técnicas por profesión

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `caracteristicasTecnicas` (Colección de características técnicas)
  - `caracteristicasPerfil` (Colección de características de perfil)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `valoresCaracteristica` (Colección de valores de característica)
  - `unaCaracteristica : CaracteristicaTecnica` (Instancia de la entidad CaracteristicaTecnica)
  - `unValor : ValorCaracteristica` (Instancia de la entidad ValorCaracteristica)
- **Variables locales / Notas:**
  - `boolean encontrada = false`
  - `boolean enUso`
- **Flujo de interacción:**
  1. El actor envía `eliminarCaracteristicaTecnica(idCaracteristica)` a `:ModaLink`.
  2. Se inicializan las variables locales `encontrada` y `enUso`.
  3. **Bloque Bucle (`loop`)** [Mientras haya características técnicas && `encontrada == false`]:
     - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaCaracteristica`: `encontrada = tieneId(idCaracteristica)`.
  4. **Bloque Alternativo (`alt`)** [!encontrada]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Característica técnica inexistente")`.
  5. **Bloque Alternativo (`alt`)** [encontrada]:
     - `:ModaLink` consulta a `unaCaracteristica`: `enUso = estaEnUso(caracteristicasPerfil, requerimientosActividad, requerimientosGenerales)`.
     - **Bloque Alternativo (`alt`)** [enUso]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("La característica está en uso y no puede eliminarse")`.
     - **Bloque Alternativo (`alt`)** [!enUso]:
       - **Bloque Bucle (`loop`)** [Mientras haya valores de la característica]:
         - `:ModaLink` solicita a `valoresCaracteristica`: `unValor = obtenerSiguiente()`.
         - **Bloque Alternativo (`alt`)** [unValor.perteneceA(unaCaracteristica)]:
           - `valoresCaracteristica.eliminar(unValor)`.
       - `:ModaLink` solicita a `caracteristicasTecnicas`: `eliminar(unaCaracteristica)`.
       - `:ModaLink` retorna al actor: `operacionExitosa()`.

---

### DSD agregarValorCaracteristica(idCaracteristica, codigo, colorHex) | UC-57: Gestionar características técnicas por profesión

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `caracteristicasTecnicas` (Colección de características técnicas)
  - `valoresCaracteristica` (Colección de valores de característica)
  - `unaCaracteristica : CaracteristicaTecnica` (Instancia de la entidad CaracteristicaTecnica)
  - `unValor : ValorCaracteristica` (Instancia de la entidad ValorCaracteristica)
- **Variables locales / Notas:**
  - `boolean encontrada = false`
  - `boolean esEnumerada`
  - `boolean valorDuplicado = false`
- **Flujo de interacción:**
  1. El actor envía `unValor = agregarValorCaracteristica(idCaracteristica, codigo, colorHex)` a `:ModaLink`.
  2. Se inicializan las variables locales `encontrada`, `esEnumerada` y `valorDuplicado`.
  3. **Bloque Bucle (`loop`)** [Mientras haya características técnicas && `encontrada == false`]:
     - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaCaracteristica`: `encontrada = tieneId(idCaracteristica)`.
  4. **Bloque Alternativo (`alt`)** [!encontrada]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Característica técnica inexistente")`.
  5. **Bloque Alternativo (`alt`)** [encontrada]:
     - `:ModaLink` consulta a `unaCaracteristica`: `esEnumerada = esEnumerada()`.
     - **Bloque Alternativo (`alt`)** [!esEnumerada]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Solo las características ENUMERADO admiten valores")`.
     - **Bloque Alternativo (`alt`)** [esEnumerada]:
       - **Bloque Bucle (`loop`)** [Mientras haya valores de la característica && `valorDuplicado == false`]:
         - `:ModaLink` solicita a `valoresCaracteristica`: `unValor = obtenerSiguiente()`.
         - `:ModaLink` consulta a `unValor`: `valorDuplicado = perteneceA(unaCaracteristica) && tieneEtiqueta(codigo)`.
       - **Bloque Alternativo (`alt`)** [valorDuplicado]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("La característica ya posee un valor con ese código")`.
       - **Bloque Alternativo (`alt`)** [!valorDuplicado]:
         - `:ModaLink` crea el valor: `unValor = registrar(unaCaracteristica, codigo, colorHex)`.
         - `valoresCaracteristica.agregar(unValor)`.
         - `:ModaLink` retorna `unValor` al actor.

---

### DSD modificarValorCaracteristica(idCaracteristica, idValor, codigo, colorHex) | UC-57: Gestionar características técnicas por profesión

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `caracteristicasTecnicas` (Colección de características técnicas)
  - `valoresCaracteristica` (Colección de valores de característica)
  - `unaCaracteristica : CaracteristicaTecnica` (Instancia de la entidad CaracteristicaTecnica)
  - `unValor : ValorCaracteristica` (Instancia de la entidad ValorCaracteristica)
  - `otroValor : ValorCaracteristica` (Instancia de la entidad ValorCaracteristica)
- **Variables locales / Notas:**
  - `boolean caracteristicaEncontrada = false`
  - `boolean valorEncontrado = false`
  - `boolean valorDuplicado = false`
- **Flujo de interacción:**
  1. El actor envía `unValor = modificarValorCaracteristica(idCaracteristica, idValor, codigo, colorHex)` a `:ModaLink`.
  2. Se inicializan las variables locales `caracteristicaEncontrada`, `valorEncontrado` y `valorDuplicado`.
  3. **Bloque Bucle (`loop`)** [Mientras haya características técnicas && `caracteristicaEncontrada == false`]:
     - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaCaracteristica`: `caracteristicaEncontrada = tieneId(idCaracteristica)`.
  4. **Bloque Alternativo (`alt`)** [!caracteristicaEncontrada]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Característica técnica inexistente")`.
  5. **Bloque Alternativo (`alt`)** [caracteristicaEncontrada]:
     - **Bloque Bucle (`loop`)** [Mientras haya valores de la característica && `valorEncontrado == false`]:
       - `:ModaLink` solicita a `valoresCaracteristica`: `unValor = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unValor`: `valorEncontrado = tieneId(idValor)`.
     - **Bloque Alternativo (`alt`)** [!valorEncontrado]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Valor de característica inexistente")`.
     - **Bloque Alternativo (`alt`)** [valorEncontrado]:
       - **Bloque Bucle (`loop`)** [Mientras haya valores de la característica && `valorDuplicado == false`]:
         - `:ModaLink` solicita a `valoresCaracteristica`: `otroValor = obtenerSiguiente()`.
         - `:ModaLink` consulta a `otroValor`: `valorDuplicado = otroValor.perteneceA(unaCaracteristica) && otroValor.tieneEtiqueta(codigo) && !otroValor.tieneId(idValor)`.
       - **Bloque Alternativo (`alt`)** [valorDuplicado]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("La característica ya posee un valor con ese código")`.
       - **Bloque Alternativo (`alt`)** [!valorDuplicado]:
         - `:ModaLink` envía a `unValor`: `setEtiqueta(codigo)`.
         - `:ModaLink` envía a `unValor`: `setColorHex(colorHex)`.
         - `:ModaLink` retorna `unValor` al actor.

---

### DSD eliminarValorCaracteristica(idCaracteristica, idValor) | UC-57: Gestionar características técnicas por profesión

- **Actores:** Administrador
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `caracteristicasTecnicas` (Colección de características técnicas)
  - `valoresCaracteristica` (Colección de valores de característica)
  - `caracteristicasPerfil` (Colección de características de perfil)
  - `unaCaracteristica : CaracteristicaTecnica` (Instancia de la entidad CaracteristicaTecnica)
  - `unValor : ValorCaracteristica` (Instancia de la entidad ValorCaracteristica)
  - `unUso : CaracteristicaPerfil` (Instancia de la entidad CaracteristicaPerfil)
- **Variables locales / Notas:**
  - `boolean caracteristicaEncontrada = false`
  - `boolean valorEncontrado = false`
  - `boolean valorEnUso = false`
- **Flujo de interacción:**
  1. El actor envía `eliminarValorCaracteristica(idCaracteristica, idValor)` a `:ModaLink`.
  2. Se inicializan las variables locales `caracteristicaEncontrada`, `valorEncontrado` y `valorEnUso`.
  3. **Bloque Bucle (`loop`)** [Mientras haya características técnicas && `caracteristicaEncontrada == false`]:
     - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaCaracteristica`: `caracteristicaEncontrada = tieneId(idCaracteristica)`.
  4. **Bloque Alternativo (`alt`)** [!caracteristicaEncontrada]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Característica técnica inexistente")`.
  5. **Bloque Alternativo (`alt`)** [caracteristicaEncontrada]:
     - **Bloque Bucle (`loop`)** [Mientras haya valores de la característica && `valorEncontrado == false`]:
       - `:ModaLink` solicita a `valoresCaracteristica`: `unValor = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unValor`: `valorEncontrado = tieneId(idValor)`.
     - **Bloque Alternativo (`alt`)** [!valorEncontrado]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Valor de característica inexistente")`.
     - **Bloque Alternativo (`alt`)** [valorEncontrado]:
       - **Bloque Bucle (`loop`)** [Mientras haya características de perfil && `valorEnUso == false`]:
         - `:ModaLink` solicita a `caracteristicasPerfil`: `unUso = obtenerSiguiente()`.
         - `:ModaLink` consulta a `unUso`: `valorEnUso = utilizaValor(unValor)`.
       - **Bloque Alternativo (`alt`)** [valorEnUso]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("El valor está en uso por perfiles y no puede eliminarse")`.
       - **Bloque Alternativo (`alt`)** [!valorEnUso]:
         - `:ModaLink` solicita a `valoresCaracteristica`: `eliminar(unValor)`.
         - `:ModaLink` retorna al actor: `operacionExitosa()`.

---

## 2. Perfiles y disponibilidad

### DSD crearPerfil(nombreArtistico, idProfesion, caracteristicas[], biografia) | UC-10: Crear perfil

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `perfiles` (Colección de perfiles)
  - `profesiones` (Colección de profesiones)
  - `caracteristicasTecnicas` (Colección de características técnicas)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unaProfesion : Profesion` (Instancia de la entidad Profesion)
  - `unaCaracteristica : CaracteristicaTecnica` (Instancia de la entidad CaracteristicaTecnica)
- **Variables locales / Notas:**
  - `boolean perfilDuplicado = false`
  - `boolean profesionValida = false`
  - `boolean datosValidos`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `unPerfil = crearPerfil(nombreArtistico, idProfesion, caracteristicas[], biografia)` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilDuplicado`, `profesionValida`, `datosValidos` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya perfiles && `perfilDuplicado == false`]:
     - `:ModaLink` solicita a `perfiles`: `unPerfil = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unPerfil`: `perfilDuplicado = tieneProfesionParaUsuario(idProfesion, usuarioActivo)`.
  4. **Bloque Bucle (`loop`)** [Mientras haya profesiones && `profesionValida == false`]:
     - `:ModaLink` solicita a `profesiones`: `unaProfesion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaProfesion`: `profesionValida = tieneId(idProfesion)`.
  5. `:ModaLink` valida los datos: `datosValidos = validarCampos(nombreArtistico, biografia)`.
  6. **Bloque Alternativo (`alt`)** [perfilDuplicado || !profesionValida || !datosValidos]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Perfil duplicado, profesión inexistente o datos inválidos")`.
  7. **Bloque Alternativo (`alt`)** [!perfilDuplicado && profesionValida && datosValidos]:
     - `:ModaLink` crea la instancia: `unPerfil = registrar(nombreArtistico, unaProfesion, biografia, ACTIVO)`.
     - `:ModaLink` agrega la instancia: `perfiles.agregar(unPerfil)`.
     - **Bloque Bucle (`loop`)** [Mientras haya características por cargar]:
       - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaCaracteristica`: `sePuedeAgregar = perteneceAProfesion(unaProfesion)`.
       - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
         - `:ModaLink` crea la característica del perfil: `unPerfil.agregarCaracteristica(unaCaracteristica, valor)`.
     - `:ModaLink` retorna `unPerfil` al actor.

---

### DSD editarPerfil(unPerfil, nombreArtistico, biografia, caracteristicas[], fotoPerfil) | UC-11: Modificar perfil

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `caracteristicasTecnicas` (Colección de características técnicas)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unaCaracteristica : CaracteristicaTecnica` (Instancia de la entidad CaracteristicaTecnica)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
  - `boolean datosValidos`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `unPerfil = editarPerfil(unPerfil, nombreArtistico, biografia, caracteristicas[], fotoPerfil)` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilActivo`, `datosValidos` y `sePuedeAgregar`.
  3. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  4. `:ModaLink` valida los datos: `datosValidos = validarCampos(nombreArtistico, biografia)`.
  5. **Bloque Alternativo (`alt`)** [!perfilActivo || !datosValidos]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Perfil inactivo o datos inválidos")`.
  6. **Bloque Alternativo (`alt`)** [perfilActivo && datosValidos]:
     - `:ModaLink` envía a `unPerfil`: `setNombreArtistico(nombreArtistico)`.
     - `:ModaLink` envía a `unPerfil`: `setBiografia(biografia)`.
     - `:ModaLink` envía a `unPerfil`: `setFotoPerfil(fotoPerfil)`.
     - **Bloque Bucle (`loop`)** [Mientras haya características por cargar]:
       - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaCaracteristica`: `sePuedeAgregar = perteneceA(unPerfil.obtenerProfesion())`.
       - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
         - `:ModaLink` envía a `unPerfil`: `actualizarCaracteristica(unaCaracteristica, valor)`.
     - `:ModaLink` retorna `unPerfil` al actor.

---

### DSD solicitarBajaPerfil(unPerfil) | UC-12: Eliminar perfil

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
- **Flujo de interacción:**
  1. El actor envía `solicitarBajaPerfil(unPerfil)` a `:ModaLink`.
  2. Se inicializa la variable local `perfilActivo`.
  3. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  4. **Bloque Alternativo (`alt`)** [!perfilActivo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil no se encuentra activo")`.
  5. **Bloque Alternativo (`alt`)** [perfilActivo]:
     - `:ModaLink` retorna al actor: `solicitarConfirmacion()`.
     - **Bloque Opcional (`opt`)** [el actor confirma]:
       - `:ModaLink` envía a `unPerfil`: `setEstado(PENDIENTE_BAJA)`.
       - `:ModaLink` envía a `unPerfil`: `setFechaSolicitudBaja(ahora)`.
       - `:ModaLink` retorna al actor: `notificarCuentaRegresiva(diasEstablecidos)`.

---

### DSD confirmarBajaPerfil(unPerfil) | UC-12: Eliminar perfil

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `notificaciones` (Colección de notificaciones)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
- **Variables locales / Notas:**
  - `boolean pendienteBaja`
- **Flujo de interacción:**
  1. El actor envía `confirmarBajaPerfil(unPerfil)` a `:ModaLink`.
  2. Se inicializa la variable local `pendienteBaja`.
  3. `:ModaLink` consulta a `unPerfil`: `pendienteBaja = estaPendienteBaja()`.
  4. **Bloque Alternativo (`alt`)** [!pendienteBaja]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil no está pendiente de baja")`.
  5. **Bloque Alternativo (`alt`)** [pendienteBaja]:
     - `:ModaLink` envía a `unPerfil`: `setEstado(BAJA)`.
     - `:ModaLink` envía a `unPerfil`: `setFechaBaja(ahora)`.
     - `:ModaLink` agrega a `notificaciones`: `notificarBajaPerfil(unPerfil)`.
     - `:ModaLink` retorna al actor: `bajaPerfilRegistrada(unPerfil)`.

---

### DSD activarPerfil(unPerfil) | UC-13: Cambiar perfil activo

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
- **Flujo de interacción:**
  1. El actor envía `activarPerfil(unPerfil)` a `:ModaLink`.
  2. Se inicializa la variable local `perfilActivo`.
  3. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  4. **Bloque Alternativo (`alt`)** [!perfilActivo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil no se encuentra activo")`.
  5. **Bloque Alternativo (`alt`)** [perfilActivo]:
     - `:ModaLink` solicita a `unPerfil`: `unUsuario = obtenerUsuario()`.
     - `:ModaLink` envía a `unUsuario`: `setPerfilActivo(unPerfil)`.
     - `:ModaLink` retorna al actor: `perfilActivado(unPerfil)`.

---

### DSD reportarPerfil(unPerfil, motivo, descripcionDetallada) | UC-15: Reportar perfil

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `reportes` (Colección de reportes)
  - `notificaciones` (Colección de notificaciones)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unReporte : Reporte` (Instancia de la entidad Reporte)
- **Variables locales / Notas:**
  - `boolean motivoValido`
  - `boolean reporteDuplicado = false`
  - `boolean descripcionValida`
- **Flujo de interacción:**
  1. El actor envía `unReporte = reportarPerfil(unPerfil, motivo, descripcionDetallada)` a `:ModaLink`.
  2. Se inicializan las variables locales `motivoValido`, `reporteDuplicado` y `descripcionValida`.
  3. `:ModaLink` valida el motivo: `motivoValido = motivo != null`.
  4. `:ModaLink` valida la descripción: `descripcionValida = motivo != "OTRO" || (descripcionDetallada != null && descripcionDetallada != "")`.
  5. **Bloque Bucle (`loop`)** [Mientras haya reportes && `reporteDuplicado == false`]:
     - `:ModaLink` solicita a `reportes`: `unReporte = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unReporte`: `reporteDuplicado = estaPendientePara(perfilActivo, unPerfil)`.
  6. **Bloque Alternativo (`alt`)** [!motivoValido || !descripcionValida || reporteDuplicado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Motivo, descripción o reporte duplicado")`.
  7. **Bloque Alternativo (`alt`)** [motivoValido && descripcionValida && !reporteDuplicado]:
     - `:ModaLink` crea la instancia: `unReporte = registrar(PERFIL, unPerfil, perfilActivo, motivo, descripcionDetallada, PENDIENTE)`.
     - `reportes.agregar(unReporte)`.
     - `:ModaLink` agrega a `notificaciones`: `notificarAdministradores(unReporte)`.
     - `:ModaLink` retorna `unReporte` al actor.

---

### DSD buscarPerfiles(nombreArtistico, profesion, caracteristicas[], habilidades[]) | UC-16: Buscar perfil

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `perfiles` (Colección de perfiles)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
- **Variables locales / Notas:**
  - `Lista perfilesFiltrados = nueva Lista()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `perfiles = buscarPerfiles(nombreArtistico, profesion, caracteristicas[], habilidades[])` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilesFiltrados` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya perfiles activos]:
     - `:ModaLink` solicita a `perfiles`: `unPerfil = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unPerfil`: `sePuedeAgregar = cumpleFiltros(nombreArtistico, profesion, caracteristicas, habilidades)`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `perfilesFiltrados.agregar(unPerfil)`.
  4. `:ModaLink` retorna `perfilesFiltrados` al actor.

---

### DSD seleccionarPerfil(idPerfil, perfiles) | UC-16: Buscar perfil

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `perfiles` (Colección de perfiles)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unPerfil = seleccionarPerfil(idPerfil, perfiles)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya perfiles && `retorno == false`]:
     - `:ModaLink` solicita a `perfiles`: `unPerfil = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unPerfil`: `retorno = tieneId(idPerfil)`.
  4. `:ModaLink` retorna `unPerfil` al actor.

---

### DSD marcarComoDisponible(fechaHoraInicio, fechaHoraFin) | UC-17: Asignar en calendario día disponible

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `agendas` (Colección de agendas)
  - `bloqueos` (Colección de bloqueos de agenda)
  - `unaAgenda : Agenda` (Instancia de la entidad Agenda)
  - `unBloqueo : BloqueoAgenda` (Instancia de la entidad BloqueoAgenda)
- **Variables locales / Notas:**
  - `boolean tieneActividad`
  - `boolean bloqueoEncontrado = false`
  - La disponibilidad se representa por ausencia de bloqueos; la operación elimina el bloqueo que ocupa el rango indicado.
- **Flujo de interacción:**
  1. El actor envía `unBloqueo = marcarComoDisponible(fechaHoraInicio, fechaHoraFin)` a `:ModaLink`.
  2. Se inicializan las variables locales `tieneActividad` y `bloqueoEncontrado`.
  3. `:ModaLink` solicita a `agendas`: `unaAgenda = buscarPorUsuario(usuarioActivo)`.
  4. `:ModaLink` consulta a `unaAgenda`: `tieneActividad = tieneActividadEn(fechaHoraInicio, fechaHoraFin)`.
  5. **Bloque Alternativo (`alt`)** [tieneActividad]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El rango se superpone con una actividad asignada")`.
  6. **Bloque Alternativo (`alt`)** [!tieneActividad]:
     - **Bloque Bucle (`loop`)** [Mientras haya bloqueos de la agenda && `bloqueoEncontrado == false`]:
       - `:ModaLink` solicita a `bloqueos`: `unBloqueo = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unBloqueo`: `bloqueoEncontrado = coincideCon(fechaHoraInicio, fechaHoraFin)`.
     - **Bloque Alternativo (`alt`)** [bloqueoEncontrado]:
       - `:ModaLink` solicita a `bloqueos`: `eliminar(unBloqueo)`.
       - `:ModaLink` retorna al actor: `bloqueDisponible(unBloqueo)`.
     - **Bloque Alternativo (`alt`)** [!bloqueoEncontrado]:
       - `:ModaLink` retorna al actor: `bloqueDisponible(null)`.

---

### DSD marcarComoNoDisponible(fechaHoraInicio, fechaHoraFin, motivo) | UC-18: Asignar en calendario día no disponible

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `agendas` (Colección de agendas)
  - `bloqueos` (Colección de bloqueos de agenda)
  - `unaAgenda : Agenda` (Instancia de la entidad Agenda)
  - `unBloqueo : BloqueoAgenda` (Instancia de la entidad BloqueoAgenda)
- **Variables locales / Notas:**
  - `boolean rangoValido`
  - `boolean seSuperpone = false`
  - `boolean motivoValido`
- **Flujo de interacción:**
  1. El actor envía `unBloqueo = marcarComoNoDisponible(fechaHoraInicio, fechaHoraFin, motivo)` a `:ModaLink`.
  2. Se inicializan las variables locales `rangoValido`, `seSuperpone` y `motivoValido`.
  3. `:ModaLink` valida el rango: `rangoValido = fechaHoraFin > fechaHoraInicio`.
  4. `:ModaLink` valida el motivo: `motivoValido = motivo != null && motivo != ""`.
  5. `:ModaLink` solicita a `agendas`: `unaAgenda = buscarPorUsuario(usuarioActivo)`.
  6. **Bloque Bucle (`loop`)** [Mientras haya bloqueos de la agenda && `seSuperpone == false`]:
     - `:ModaLink` solicita a `bloqueos`: `unBloqueo = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unBloqueo`: `seSuperpone = seSuperponeCon(fechaHoraInicio, fechaHoraFin)`.
  7. **Bloque Alternativo (`alt`)** [!rangoValido || !motivoValido || seSuperpone]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Rango, motivo o superposición inválidos")`.
  8. **Bloque Alternativo (`alt`)** [rangoValido && motivoValido && !seSuperpone]:
     - `:ModaLink` crea la instancia: `unBloqueo = registrar(unaAgenda, fechaHoraInicio, fechaHoraFin, motivo)`.
     - `:ModaLink` agrega la instancia: `bloqueos.agregar(unBloqueo)`.
     - `:ModaLink` retorna al actor: `bloqueoRegistrado(unBloqueo)`.

---

### DSD buscarHabilidades(nombre) | UC-48: Buscar habilidades

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `habilidades` (Colección de habilidades)
  - `unaHabilidad : Habilidad` (Instancia de la entidad Habilidad)
- **Variables locales / Notas:**
  - `Lista habilidadesFiltradas = nueva Lista()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `habilidades = buscarHabilidades(nombre)` a `:ModaLink`.
  2. Se inicializan las variables locales `habilidadesFiltradas` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya habilidades]:
     - `:ModaLink` solicita a `habilidades`: `unaHabilidad = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaHabilidad`: `sePuedeAgregar = cumpleFiltro(nombre)`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `habilidadesFiltradas.agregar(unaHabilidad)`.
  4. `:ModaLink` retorna `habilidadesFiltradas` al actor.

---

### DSD buscarCaracteristicasTecnicas(idProfesion) | UC-58: Buscar características técnicas

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `caracteristicasTecnicas` (Colección de características técnicas)
  - `unaCaracteristica : CaracteristicaTecnica` (Instancia de la entidad CaracteristicaTecnica)
- **Variables locales / Notas:**
  - `Lista caracteristicasFiltradas = nueva Lista()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `caracteristicas = buscarCaracteristicasTecnicas(idProfesion)` a `:ModaLink`.
  2. Se inicializan las variables locales `caracteristicasFiltradas` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya características técnicas]:
     - `:ModaLink` solicita a `caracteristicasTecnicas`: `unaCaracteristica = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaCaracteristica`: `sePuedeAgregar = perteneceAProfesion(idProfesion)`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `caracteristicasFiltradas.agregar(unaCaracteristica)`.
  4. `:ModaLink` retorna `caracteristicasFiltradas` al actor.

---

### DSD buscarProfesiones() | UC-59: Buscar profesiones

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `profesiones` (Colección de profesiones)
  - `unaProfesion : Profesion` (Instancia de la entidad Profesion)
- **Variables locales / Notas:**
  - `Lista profesionesActivas = nueva Lista()`
  - `boolean estaActiva`
- **Flujo de interacción:**
  1. El actor envía `profesiones = buscarProfesiones()` a `:ModaLink`.
  2. Se inicializan las variables locales `profesionesActivas` y `estaActiva`.
  3. **Bloque Bucle (`loop`)** [Mientras haya profesiones]:
     - `:ModaLink` solicita a `profesiones`: `unaProfesion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaProfesion`: `estaActiva = estaActiva()`.
     - **Bloque Alternativo (`alt`)** [estaActiva]:
       - `profesionesActivas.agregar(unaProfesion)`.
  4. `:ModaLink` retorna `profesionesActivas` al actor.

---

### DSD seleccionarProfesion(idProfesion, profesiones) | UC-59: Buscar profesiones

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `profesiones` (Colección de profesiones)
  - `unaProfesion : Profesion` (Instancia de la entidad Profesion)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unaProfesion = seleccionarProfesion(idProfesion, profesiones)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya profesiones && `retorno == false`]:
     - `:ModaLink` solicita a `profesiones`: `unaProfesion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaProfesion`: `retorno = tieneId(idProfesion)`.
  4. `:ModaLink` retorna `unaProfesion` al actor.

---

### DSD configurarDiaCompleto(diaSemana, horaInicio, horaFin) | UC-71: Configurar jornada laboral

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `agendas` (Colección de agendas)
  - `jornadas` (Colección de jornadas de agenda)
  - `unaAgenda : Agenda` (Instancia de la entidad Agenda)
  - `unaJornada : JornadaAgenda` (Instancia de la entidad JornadaAgenda)
- **Variables locales / Notas:**
  - `boolean rangoValido`
  - `boolean existeJornada = false`
- **Flujo de interacción:**
  1. El actor envía `configurarDiaCompleto(diaSemana, horaInicio, horaFin)` a `:ModaLink`.
  2. Se inicializan las variables locales `rangoValido` y `existeJornada`.
  3. `:ModaLink` valida el rango: `rangoValido = horaFin > horaInicio`.
  4. **Bloque Alternativo (`alt`)** [!rangoValido]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Horario inválido")`.
  5. **Bloque Alternativo (`alt`)** [rangoValido]:
     - `:ModaLink` solicita a `agendas`: `unaAgenda = buscarPorUsuario(usuarioActivo)`.
     - **Bloque Bucle (`loop`)** [Mientras haya jornadas de la agenda && `existeJornada == false`]:
       - `:ModaLink` solicita a `jornadas`: `unaJornada = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaJornada`: `existeJornada = correspondeADia(diaSemana)`.
     - **Bloque Alternativo (`alt`)** [existeJornada]:
       - `:ModaLink` envía a `unaJornada`: `configurarCompleto(horaInicio, horaFin)`.
     - **Bloque Alternativo (`alt`)** [!existeJornada]:
       - `:ModaLink` crea la jornada: `unaJornada = registrar(unaAgenda, diaSemana, horaInicio, horaFin)`.
       - `jornadas.agregar(unaJornada)`.
     - `:ModaLink` retorna al actor: `jornadaConfigurada(unaJornada)`.

---

### DSD configurarDiaPartida(diaSemana, horaInicioManana, horaFinManana, horaInicioTarde, horaFinTarde) | UC-71: Configurar jornada laboral

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `agendas` (Colección de agendas)
  - `jornadas` (Colección de jornadas de agenda)
  - `unaAgenda : Agenda` (Instancia de la entidad Agenda)
  - `unaJornada : JornadaAgenda` (Instancia de la entidad JornadaAgenda)
- **Variables locales / Notas:**
  - `boolean rangoValido`
  - `boolean existeJornada = false`
- **Flujo de interacción:**
  1. El actor envía `configurarDiaPartida(diaSemana, horaInicioManana, horaFinManana, horaInicioTarde, horaFinTarde)` a `:ModaLink`.
  2. Se inicializan las variables locales `rangoValido` y `existeJornada`.
  3. `:ModaLink` valida el rango: `rangoValido = horaFinManana > horaInicioManana && horaFinTarde > horaInicioTarde && horaInicioTarde > horaFinManana`.
  4. **Bloque Alternativo (`alt`)** [!rangoValido]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Horario partido inválido")`.
  5. **Bloque Alternativo (`alt`)** [rangoValido]:
     - `:ModaLink` solicita a `agendas`: `unaAgenda = buscarPorUsuario(usuarioActivo)`.
     - **Bloque Bucle (`loop`)** [Mientras haya jornadas de la agenda && `existeJornada == false`]:
       - `:ModaLink` solicita a `jornadas`: `unaJornada = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaJornada`: `existeJornada = correspondeADia(diaSemana)`.
     - **Bloque Alternativo (`alt`)** [existeJornada]:
       - `:ModaLink` envía a `unaJornada`: `configurarPartido(horaInicioManana, horaFinManana, horaInicioTarde, horaFinTarde)`.
     - **Bloque Alternativo (`alt`)** [!existeJornada]:
       - `:ModaLink` crea la jornada: `unaJornada = registrar(unaAgenda, diaSemana, horaInicioManana, horaFinManana, horaInicioTarde, horaFinTarde)`.
       - `jornadas.agregar(unaJornada)`.
     - `:ModaLink` retorna al actor: `jornadaConfigurada(unaJornada)`.

---

### DSD confirmarJornadaLaboral() | UC-71: Configurar jornada laboral

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `agendas` (Colección de agendas)
  - `jornadas` (Colección de jornadas de agenda)
  - `actividades` (Colección de actividades)
  - `notificaciones` (Colección de notificaciones)
  - `unaAgenda : Agenda` (Instancia de la entidad Agenda)
  - `unaJornada : JornadaAgenda` (Instancia de la entidad JornadaAgenda)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
- **Variables locales / Notas:**
  - `boolean existeConflicto = false`
  - `boolean jornadaValida`
- **Flujo de interacción:**
  1. El actor envía `confirmarJornadaLaboral()` a `:ModaLink`.
  2. Se inicializan las variables locales `existeConflicto` y `jornadaValida`.
  3. `:ModaLink` solicita a `agendas`: `unaAgenda = buscarPorUsuario(usuarioActivo)`.
  4. **Bloque Bucle (`loop`)** [Mientras haya actividades asignadas al usuario && `existeConflicto == false`]:
     - `:ModaLink` solicita a `actividades`: `unaActividad = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaActividad`: `existeConflicto = seSuperponeConJornada(unaAgenda)`.
  5. **Bloque Alternativo (`alt`)** [existeConflicto]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("La jornada se superpone con actividades ya asignadas")`.
  6. **Bloque Alternativo (`alt`)** [!existeConflicto]:
     - `:ModaLink` envía a `unaAgenda`: `guardarJornada()`.
     - `:ModaLink` retorna al actor: `jornadaLaboralRegistrada()`.
     - `:ModaLink` retorna al actor: `vincularCalendarioUsuario()`.
     - `:ModaLink` agrega a `notificaciones`: `notificarCambioJornada()`.

---

---

## 3. Proyectos

### DSD crearProyecto(nombre, descripcion, privacidad, ubicacion, requerimientosGral[], moodboard, objetivos[], fechaEntregaEstipulada) | UC-24: Crear proyecto

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `proyectos` (Colección de proyectos)
  - `perfiles` (Colección de perfiles)
  - `rolesProyecto` (Colección de roles de proyecto)
  - `planificaciones` (Colección de planificaciones)
  - `objetivos` (Colección de objetivos)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `moodboards` (Colección de moodboards)
  - `ubicaciones` (Colección de ubicaciones)
  - `miembros` (Colección de miembros de proyecto)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unRol : RolProyecto` (Instancia de la entidad RolProyecto)
  - `unaPlanificacion : Planificacion` (Instancia de la entidad Planificacion)
  - `unObjetivo : Objetivo` (Instancia de la entidad Objetivo)
  - `unRequerimiento : RequerimientoGeneral` (Instancia de la entidad RequerimientoGeneral)
  - `unMoodboard : Moodboard` (Instancia de la entidad Moodboard)
  - `unaUbicacion : Ubicacion` (Instancia de la entidad Ubicacion)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
  - `boolean fechaValida`
  - `boolean nombreDuplicado = false`
  - `boolean rolEncontrado = false`
  - `boolean ubicacionEncontrada = false`
- **Flujo de interacción:**
  1. El actor envía `unProyecto = crearProyecto(nombre, descripcion, privacidad, ubicacion, requerimientosGral[], moodboard, objetivos[], fechaEntregaEstipulada)` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilActivo`, `fechaValida`, `nombreDuplicado`, `rolEncontrado` y `ubicacionEncontrada`.
  3. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  4. `:ModaLink` valida la fecha: `fechaValida = fechaEntregaEstipulada > ahora`.
  5. **Bloque Alternativo (`alt`)** [!perfilActivo || !fechaValida]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Perfil inactivo o fecha de entrega inválida")`.
  6. **Bloque Bucle (`loop`)** [Mientras haya roles de proyecto && `rolEncontrado == false`]:
     - `:ModaLink` solicita a `rolesProyecto`: `unRol = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unRol`: `rolEncontrado = tieneCodigo("DIRECTOR")`.
  7. **Bloque Bucle (`loop`)** [Mientras haya proyectos && `nombreDuplicado == false`]:
     - `:ModaLink` solicita a `proyectos`: `unProyecto = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unProyecto`: `nombreDuplicado = tieneNombreParaPerfil(nombre, unPerfil)`.
  8. **Bloque Alternativo (`alt`)** [nombreDuplicado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil ya tiene un proyecto con ese nombre")`.
  9. **Bloque Alternativo (`alt`)** [!nombreDuplicado]:
     - **Bloque Opcional (`opt`)** [ubicacion != null]:
       - **Bloque Bucle (`loop`)** [Mientras haya ubicaciones && `ubicacionEncontrada == false`]:
         - `:ModaLink` solicita a `ubicaciones`: `unaUbicacion = obtenerSiguiente()`.
         - `:ModaLink` consulta a `unaUbicacion`: `ubicacionEncontrada = coincideCon(ubicacion)`.
       - **Bloque Alternativo (`alt`)** [!ubicacionEncontrada]:
         - `:ModaLink` crea la ubicación: `unaUbicacion = registrar(ubicacion)`.
         - `ubicaciones.agregar(unaUbicacion)`.
     - `:ModaLink` crea el proyecto: `unProyecto = registrar(nombre, descripcion, privacidad, BORRADOR, fechaEntregaEstipulada, unaUbicacion)`.
     - `proyectos.agregar(unProyecto)`.
     - `:ModaLink` crea el miembro director: `unMiembro = registrar(unPerfil, unRol, ACTIVO)`.
     - `miembros.agregar(unMiembro)`.
     - `:ModaLink` crea la planificación: `unaPlanificacion = registrar(unProyecto, fechaEntregaEstipulada)`.
     - `planificaciones.agregar(unaPlanificacion)`.
     - **Bloque Bucle (`loop`)** [Mientras haya objetivos por cargar]:
       - `:ModaLink` crea el objetivo: `unObjetivo = registrar(descripcion, unProyecto)`.
       - `objetivos.agregar(unObjetivo)`.
     - **Bloque Bucle (`loop`)** [Mientras haya requerimientos generales por cargar]:
       - `:ModaLink` crea el requerimiento: `unRequerimiento = registrar(profesion, cantidad, caracteristicas[], habilidades[], unProyecto)`.
       - `requerimientosGenerales.agregar(unRequerimiento)`.
     - **Bloque Opcional (`opt`)** [moodboard != null]:
       - `:ModaLink` crea el moodboard: `unMoodboard = registrar(moodboard, unProyecto)`.
       - `moodboards.agregar(unMoodboard)`.
     - `:ModaLink` retorna `unProyecto` al actor.

---

### DSD publicarProyecto(unProyecto) | UC-25: Publicar proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
- **Variables locales / Notas:**
  - `boolean estaEnBorrador`
  - `boolean informacionCompleta`
- **Flujo de interacción:**
  1. El actor envía `unProyecto = publicarProyecto(unProyecto)` a `:ModaLink`.
  2. Se inicializan las variables locales `estaEnBorrador` e `informacionCompleta`.
  3. `:ModaLink` consulta a `unProyecto`: `estaEnBorrador = estaEnEstado(BORRADOR)`.
  4. **Bloque Alternativo (`alt`)** [!estaEnBorrador]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El proyecto no está en estado Borrador")`.
  5. **Bloque Alternativo (`alt`)** [estaEnBorrador]:
     - `:ModaLink` consulta a `unProyecto`: `informacionCompleta = tieneInformacionBasicaCompleta()`.
     - **Bloque Alternativo (`alt`)** [!informacionCompleta]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("El proyecto tiene información básica incompleta")`.
     - **Bloque Alternativo (`alt`)** [informacionCompleta]:
       - `:ModaLink` envía a `unProyecto`: `setEstado(PUBLICADO)`.
       - `:ModaLink` retorna `unProyecto` al actor.

---

### DSD buscarProyectos(nombre, estado, director) | UC-29: Buscar proyecto

- **Actores:** Usuario / Director de proyecto / Miembro de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `proyectos` (Colección de proyectos)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
- **Variables locales / Notas:**
  - `Lista proyectosFiltrados = nueva Lista()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `proyectos = buscarProyectos(nombre, estado, director)` a `:ModaLink`.
  2. Se inicializan las variables locales `proyectosFiltrados` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya proyectos visibles]:
     - `:ModaLink` solicita a `proyectos`: `unProyecto = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unProyecto`: `sePuedeAgregar = cumpleFiltros(nombre, estado, director)`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `proyectosFiltrados.agregar(unProyecto)`.
  4. `:ModaLink` retorna `proyectosFiltrados` al actor.

---

### DSD seleccionarProyecto(idProyecto, proyectos) | UC-29: Buscar proyecto

- **Actores:** Usuario / Director de proyecto / Miembro de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `proyectos` (Colección de proyectos)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unProyecto = seleccionarProyecto(idProyecto, proyectos)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya proyectos && `retorno == false`]:
     - `:ModaLink` solicita a `proyectos`: `unProyecto = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unProyecto`: `retorno = tieneId(idProyecto)`.
  4. `:ModaLink` retorna `unProyecto` al actor.

---

### DSD solicitarCronograma(unProyecto) | UC-33: Visualizar cronograma de proyecto

- **Actores:** Director de proyecto / Miembro de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `actividades` (Colección de actividades)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
- **Variables locales / Notas:**
  - `Lista actividadesProyecto = nueva Lista()`
  - `boolean perteneceAlProyecto`
  - `Cronograma cronograma`
- **Flujo de interacción:**
  1. El actor envía `cronograma = solicitarCronograma(unProyecto)` a `:ModaLink`.
  2. Se inicializan las variables locales `actividadesProyecto` y `perteneceAlProyecto`.
  3. **Bloque Bucle (`loop`)** [Mientras haya actividades]:
     - `:ModaLink` solicita a `actividades`: `unaActividad = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaActividad`: `perteneceAlProyecto = perteneceA(unProyecto)`.
     - **Bloque Alternativo (`alt`)** [perteneceAlProyecto]:
       - `actividadesProyecto.agregar(unaActividad)`.
  4. `:ModaLink` calcula la ruta crítica: `cronograma = calcularCPM(actividadesProyecto)`.
  5. `:ModaLink` retorna al actor: `desplegarCronograma(unProyecto, cronograma)`.

---

### DSD cancelarProyecto(unProyecto, motivoCancelacion) | UC-35: Cancelar proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `actividades` (Colección de actividades)
  - `postulacionesGenerales` (Colección de postulaciones generales)
  - `postulacionesActividad` (Colección de postulaciones de actividad)
  - `invitacionesGenerales` (Colección de invitaciones generales)
  - `invitacionesActividad` (Colección de invitaciones de actividad)
  - `notificaciones` (Colección de notificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unaPostulacion : PostulacionGeneral` (Instancia de la entidad PostulacionGeneral)
  - `unaPostulacionAct : PostulacionActividad` (Instancia de la entidad PostulacionActividad)
  - `unaInvitacion : InvitacionGeneral` (Instancia de la entidad InvitacionGeneral)
  - `unaInvitacionAct : InvitacionActividad` (Instancia de la entidad InvitacionActividad)
- **Variables locales / Notas:**
  - `boolean estadoValido`
  - `boolean motivoValido`
  - `boolean perteneceAlProyecto`
- **Flujo de interacción:**
  1. El actor envía `cancelarProyecto(unProyecto, motivoCancelacion)` a `:ModaLink`.
  2. Se inicializan las variables locales `estadoValido` y `motivoValido`.
  3. `:ModaLink` consulta a `unProyecto`: `estadoValido = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
  4. `:ModaLink` valida el motivo: `motivoValido = motivoCancelacion != null && motivoCancelacion != ""`.
  5. **Bloque Alternativo (`alt`)** [!estadoValido || !motivoValido]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Estado o motivo de cancelación inválido")`.
  6. **Bloque Alternativo (`alt`)** [estadoValido && motivoValido]:
     - `:ModaLink` envía a `unProyecto`: `setEstado(CANCELADO)`.
     - `:ModaLink` envía a `unProyecto`: `setMotivoCancelacion(motivoCancelacion)`.
     - `:ModaLink` envía a `unProyecto`: `setFechaCancelacion(ahora)`.
     - **Bloque Bucle (`loop`)** [Mientras haya actividades del proyecto]:
       - `:ModaLink` solicita a `actividades`: `unaActividad = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaActividad`: `perteneceAlProyecto = perteneceA(unProyecto)`.
       - **Bloque Alternativo (`alt`)** [perteneceAlProyecto && (unaActividad.estaPendiente() || unaActividad.estaEnCurso())]:
         - `:ModaLink` envía a `unaActividad`: `setEstado(CANCELADA)`.
     - **Bloque Bucle (`loop`)** [Mientras haya postulaciones generales del proyecto]:
       - `:ModaLink` solicita a `postulacionesGenerales`: `unaPostulacion = obtenerSiguiente()`.
       - **Bloque Alternativo (`alt`)** [unaPostulacion.perteneceA(unProyecto) && unaPostulacion.estaPendiente()]:
         - `:ModaLink` envía a `unaPostulacion`: `setEstado(RECHAZADA)`.
     - **Bloque Bucle (`loop`)** [Mientras haya postulaciones de actividad del proyecto]:
       - `:ModaLink` solicita a `postulacionesActividad`: `unaPostulacionAct = obtenerSiguiente()`.
       - **Bloque Alternativo (`alt`)** [unaPostulacionAct.perteneceA(unProyecto) && unaPostulacionAct.estaPendiente()]:
         - `:ModaLink` envía a `unaPostulacionAct`: `setEstado(RECHAZADA)`.
     - **Bloque Bucle (`loop`)** [Mientras haya invitaciones generales del proyecto]:
       - `:ModaLink` solicita a `invitacionesGenerales`: `unaInvitacion = obtenerSiguiente()`.
       - **Bloque Alternativo (`alt`)** [unaInvitacion.perteneceA(unProyecto) && unaInvitacion.estaPendiente()]:
         - `:ModaLink` envía a `unaInvitacion`: `setEstado(RECHAZADA)`.
     - **Bloque Bucle (`loop`)** [Mientras haya invitaciones de actividad del proyecto]:
       - `:ModaLink` solicita a `invitacionesActividad`: `unaInvitacionAct = obtenerSiguiente()`.
       - **Bloque Alternativo (`alt`)** [unaInvitacionAct.perteneceA(unProyecto) && unaInvitacionAct.estaPendiente()]:
         - `:ModaLink` envía a `unaInvitacionAct`: `setEstado(RECHAZADA)`.
     - `:ModaLink` agrega a `notificaciones`: `notificarCancelacionIntegrantes(unProyecto, motivoCancelacion)`.
     - `:ModaLink` retorna al actor: `proyectoCancelado(unProyecto)`.

---

### DSD modificarProyecto(unProyecto, descripcion, privacidad, ubicacion, requerimientosPersonal[], moodboard, objetivos, fechaEntregaEstipulada) | UC-36: Modificar proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `notificaciones` (Colección de notificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unRequerimiento : RequerimientoGeneral` (Instancia de la entidad RequerimientoGeneral)
- **Variables locales / Notas:**
  - `boolean estadoValido`
  - `boolean datosValidos`
  - `boolean requerimientoProtegido`
- **Flujo de interacción:**
  1. El actor envía `unProyecto = modificarProyecto(unProyecto, descripcion, privacidad, ubicacion, requerimientosPersonal[], moodboard, objetivos, fechaEntregaEstipulada)` a `:ModaLink`.
  2. Se inicializan las variables locales `estadoValido`, `datosValidos` y `requerimientoProtegido`.
  3. `:ModaLink` consulta a `unProyecto`: `estadoValido = estaEnEstado(BORRADOR) || estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
  4. `:ModaLink` valida los datos: `datosValidos = validarCampos(descripcion, privacidad, fechaEntregaEstipulada)`.
  5. **Bloque Alternativo (`alt`)** [!estadoValido || !datosValidos]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Estado o datos inválidos")`.
  6. **Bloque Alternativo (`alt`)** [estadoValido && datosValidos]:
     - `:ModaLink` envía a `unProyecto`: `setDescripcion(descripcion)`.
     - `:ModaLink` envía a `unProyecto`: `setPrivacidad(privacidad)`.
     - `:ModaLink` envía a `unProyecto`: `setUbicacion(ubicacion)`.
     - `:ModaLink` envía a `unProyecto`: `setMoodboard(moodboard)`.
     - `:ModaLink` envía a `unProyecto`: `setObjetivos(objetivos)`.
     - `:ModaLink` envía a `unProyecto`: `setFechaEntregaEstipulada(fechaEntregaEstipulada)`.
     - **Bloque Bucle (`loop`)** [Mientras haya requerimientos personales por actualizar]:
       - `:ModaLink` solicita a `requerimientosGenerales`: `unRequerimiento = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unRequerimiento`: `requerimientoProtegido = estaProtegido()`.
       - **Bloque Alternativo (`alt`)** [requerimientoProtegido]:
         - `:ModaLink` conserva `unRequerimiento` sin cambios.
       - **Bloque Alternativo (`alt`)** [!requerimientoProtegido]:
         - `:ModaLink` envía a `unRequerimiento`: `actualizar(datos)`.
     - `:ModaLink` agrega a `notificaciones`: `notificarParticipantes(unProyecto)`.
     - `:ModaLink` retorna `unProyecto` al actor.

---

### DSD finalizarProyecto(unProyecto) | UC-37: Finalizar proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `actividades` (Colección de actividades)
  - `miembros` (Colección de miembros de proyecto)
  - `calificaciones` (Colección de calificaciones)
  - `notificaciones` (Colección de notificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unaCalificacion : CalificacionPerfil` (Instancia de la entidad CalificacionPerfil)
- **Variables locales / Notas:**
  - `boolean estadoValido`
  - `boolean actividadesFinalizadas = true`
  - `boolean equipoCalificado = true`
  - `boolean tieneCalificacion`
- **Flujo de interacción:**
  1. El actor envía `unProyecto = finalizarProyecto(unProyecto)` a `:ModaLink`.
  2. Se inicializan las variables locales `estadoValido`, `actividadesFinalizadas` y `equipoCalificado`.
  3. `:ModaLink` consulta a `unProyecto`: `estadoValido = estaEnEstado(CONFIRMADO)`.
  4. **Bloque Alternativo (`alt`)** [!estadoValido]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El proyecto no está confirmado")`.
  5. **Bloque Alternativo (`alt`)** [estadoValido]:
     - **Bloque Bucle (`loop`)** [Mientras haya actividades del proyecto && `actividadesFinalizadas`]:
       - `:ModaLink` solicita a `actividades`: `unaActividad = obtenerSiguiente()`.
       - **Bloque Alternativo (`alt`)** [unaActividad.perteneceA(unProyecto) && (unaActividad.estaPendiente() || unaActividad.estaEnCurso())]:
         - `actividadesFinalizadas = false`.
     - **Bloque Alternativo (`alt`)** [!actividadesFinalizadas]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Existen actividades pendientes o en curso")`.
     - **Bloque Alternativo (`alt`)** [actividadesFinalizadas]:
       - **Bloque Bucle (`loop`)** [Mientras haya miembros activos && `equipoCalificado`]:
         - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
         - `:ModaLink` consulta a `calificaciones`: `tieneCalificacion = existeCalificacion(unProyecto, unMiembro)`.
         - **Bloque Alternativo (`alt`)** [!tieneCalificacion]:
           - `equipoCalificado = false`.
       - **Bloque Alternativo (`alt`)** [!equipoCalificado]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("El equipo no fue calificado completo")`.
       - **Bloque Alternativo (`alt`)** [equipoCalificado]:
         - `:ModaLink` envía a `unProyecto`: `setEstado(FINALIZADO)`.
         - `:ModaLink` envía a `unProyecto`: `setFechaFinalizacion(ahora)`.
         - `:ModaLink` indexa el proyecto en el historial de cada miembro: `indexarHistorial(unProyecto, miembros)`.
         - `:ModaLink` agrega a `notificaciones`: `notificarCalificacionesEquipo(unProyecto)`.
         - `:ModaLink` retorna `unProyecto` al actor.

---

### DSD calificarMiembro(unPerfil, calificacion, resenia) | UC-37: Finalizar proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `miembros` (Colección de miembros de proyecto)
  - `calificaciones` (Colección de calificaciones)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unaCalificacion : CalificacionPerfil` (Instancia de la entidad CalificacionPerfil)
- **Variables locales / Notas:**
  - `boolean calificacionValida`
  - `boolean participo = false`
  - `boolean yaCalificado = false`
- **Flujo de interacción:**
  1. El actor envía `unaCalificacion = calificarMiembro(unPerfil, calificacion, resenia)` a `:ModaLink`.
  2. Se inicializan las variables locales `calificacionValida`, `participo` y `yaCalificado`.
  3. `:ModaLink` valida la calificación: `calificacionValida = calificacion >= 1 && calificacion <= 5 && resenia != null && resenia != ""`.
  4. **Bloque Bucle (`loop`)** [Mientras haya miembros && `participo == false`]:
     - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unMiembro`: `participo = correspondeAPerfil(unPerfil)`.
  5. **Bloque Bucle (`loop`)** [Mientras haya calificaciones && `yaCalificado == false`]:
     - `:ModaLink` solicita a `calificaciones`: `unaCalificacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaCalificacion`: `yaCalificado = correspondeA(unPerfil, unProyecto)`.
  6. **Bloque Alternativo (`alt`)** [!calificacionValida || !participo || yaCalificado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Calificación, participación o duplicado inválido")`.
  7. **Bloque Alternativo (`alt`)** [calificacionValida && participo && !yaCalificado]:
     - `:ModaLink` crea la calificación: `unaCalificacion = registrar(unPerfil, perfilActivo, calificacion, resenia, unProyecto)`.
     - `calificaciones.agregar(unaCalificacion)`.
     - `:ModaLink` retorna `unaCalificacion` al actor.

---

### DSD eliminarIntegrante(unProyecto, unPerfil, motivo) | UC-38: Eliminar integrante

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `miembros` (Colección de miembros de proyecto)
  - `asignaciones` (Colección de asignaciones de actividad)
  - `notificaciones` (Colección de notificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unaAsignacion : AsignacionActividad` (Instancia de la entidad AsignacionActividad)
- **Variables locales / Notas:**
  - `boolean estadoValido`
  - `boolean motivoValido`
  - `boolean miembroEncontrado = false`
  - `boolean esUltimoDirector`
- **Flujo de interacción:**
  1. El actor envía `eliminarIntegrante(unProyecto, unPerfil, motivo)` a `:ModaLink`.
  2. Se inicializan las variables locales `estadoValido`, `motivoValido`, `miembroEncontrado` y `esUltimoDirector`.
  3. `:ModaLink` consulta a `unProyecto`: `estadoValido = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
  4. `:ModaLink` valida el motivo: `motivoValido = motivo != null && motivo != ""`.
  5. **Bloque Bucle (`loop`)** [Mientras haya miembros && `miembroEncontrado == false`]:
     - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unMiembro`: `miembroEncontrado = correspondeA(unProyecto, unPerfil) && estaActivo()`.
  6. **Bloque Alternativo (`alt`)** [!estadoValido || !motivoValido || !miembroEncontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Estado, motivo o integrante inválido")`.
  7. **Bloque Alternativo (`alt`)** [estadoValido && motivoValido && miembroEncontrado]:
     - `:ModaLink` consulta a `unProyecto`: `esUltimoDirector = unMiembro.esUltimoDirectorActivo(unProyecto)`.
     - **Bloque Alternativo (`alt`)** [esUltimoDirector]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("No se puede eliminar al último director activo")`.
     - **Bloque Alternativo (`alt`)** [!esUltimoDirector]:
       - `:ModaLink` envía a `unMiembro`: `setEstado(ELIMINADO)`.
       - `:ModaLink` envía a `unMiembro`: `setMotivoBaja(motivo)`.
       - `:ModaLink` envía a `unMiembro`: `setFechaBaja(ahora)`.
       - `:ModaLink` envía a `unMiembro`: `setPerfilAutorBaja(perfilActivo)`.
       - **Bloque Bucle (`loop`)** [Mientras haya asignaciones del integrante]:
         - `:ModaLink` solicita a `asignaciones`: `unaAsignacion = obtenerSiguiente()`.
         - **Bloque Alternativo (`alt`)** [unaAsignacion.perteneceA(unMiembro)]:
           - `:ModaLink` conserva `unaAsignacion` para auditoría.
       - `:ModaLink` agrega a `notificaciones`: `notificarIntegranteEliminado(unPerfil, motivo)`.
       - `:ModaLink` retorna al actor: `bajaIntegranteRegistrada(unProyecto, unPerfil)`.

---

### DSD darseDeBaja(unProyecto, motivo) | UC-40: Darse de baja de proyecto

- **Actores:** Miembro de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `miembros` (Colección de miembros de proyecto)
  - `notificaciones` (Colección de notificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
- **Variables locales / Notas:**
  - `boolean estadoValido`
  - `boolean motivoValido`
  - `boolean miembroEncontrado = false`
  - `boolean esUltimoDirector`
- **Flujo de interacción:**
  1. El actor envía `darseDeBaja(unProyecto, motivo)` a `:ModaLink`.
  2. Se inicializan las variables locales `estadoValido`, `motivoValido`, `miembroEncontrado` y `esUltimoDirector`.
  3. `:ModaLink` consulta a `unProyecto`: `estadoValido = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
  4. `:ModaLink` valida el motivo: `motivoValido = motivo != null && motivo != ""`.
  5. **Bloque Bucle (`loop`)** [Mientras haya miembros && `miembroEncontrado == false`]:
     - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unMiembro`: `miembroEncontrado = correspondeA(unProyecto, perfilActivo) && estaActivo()`.
  6. **Bloque Alternativo (`alt`)** [!estadoValido || !motivoValido || !miembroEncontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Estado, motivo o membresía inválida")`.
  7. **Bloque Alternativo (`alt`)** [estadoValido && motivoValido && miembroEncontrado]:
     - `:ModaLink` consulta a `unProyecto`: `esUltimoDirector = unMiembro.esUltimoDirectorActivo(unProyecto)`.
     - **Bloque Alternativo (`alt`)** [esUltimoDirector]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Debe quedar al menos otro director activo")`.
     - **Bloque Alternativo (`alt`)** [!esUltimoDirector]:
       - `:ModaLink` envía a `unMiembro`: `setEstado(BAJA_VOLUNTARIA)`.
       - `:ModaLink` envía a `unMiembro`: `setMotivoBaja(motivo)`.
       - `:ModaLink` envía a `unMiembro`: `setFechaBaja(ahora)`.
       - `:ModaLink` agrega a `notificaciones`: `notificarDirectores(unProyecto, motivo)`.
       - `:ModaLink` retorna al actor: `bajaVoluntariaRegistrada(unProyecto)`.

---

### DSD confirmarProyecto(unProyecto) | UC-60: Confirmar proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `actividades` (Colección de actividades)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `miembros` (Colección de miembros de proyecto)
  - `notificaciones` (Colección de notificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unRequerimiento : RequerimientoGeneral` (Instancia de la entidad RequerimientoGeneral)
  - `unRequerimientoAct : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
- **Variables locales / Notas:**
  - `boolean estaPublicado`
  - `boolean tieneActividades = false`
  - `boolean requerimientosCompletos = true`
  - `boolean disponibilidadValida = true`
- **Flujo de interacción:**
  1. El actor envía `unProyecto = confirmarProyecto(unProyecto)` a `:ModaLink`.
  2. Se inicializan las variables locales `estaPublicado`, `tieneActividades`, `requerimientosCompletos` y `disponibilidadValida`.
  3. `:ModaLink` consulta a `unProyecto`: `estaPublicado = estaEnEstado(PUBLICADO)`.
  4. **Bloque Alternativo (`alt`)** [!estaPublicado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El proyecto no está publicado")`.
  5. **Bloque Alternativo (`alt`)** [estaPublicado]:
     - **Bloque Bucle (`loop`)** [Mientras haya actividades del proyecto]:
       - `:ModaLink` solicita a `actividades`: `unaActividad = obtenerSiguiente()`.
       - **Bloque Alternativo (`alt`)** [unaActividad.perteneceA(unProyecto)]:
         - `tieneActividades = true`.
     - **Bloque Alternativo (`alt`)** [!tieneActividades]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("El proyecto no tiene actividades planificadas")`.
     - **Bloque Alternativo (`alt`)** [tieneActividades]:
       - **Bloque Bucle (`loop`)** [Mientras haya requerimientos generales && `requerimientosCompletos`]:
         - `:ModaLink` solicita a `requerimientosGenerales`: `unRequerimiento = obtenerSiguiente()`.
         - **Bloque Alternativo (`alt`)** [unRequerimiento.perteneceA(unProyecto) && !unRequerimiento.estaSatisfecho()]:
           - `requerimientosCompletos = false`.
       - **Bloque Bucle (`loop`)** [Mientras haya requerimientos de actividad && `requerimientosCompletos`]:
         - `:ModaLink` solicita a `requerimientosActividad`: `unRequerimientoAct = obtenerSiguiente()`.
         - **Bloque Alternativo (`alt`)** [unRequerimientoAct.perteneceA(unProyecto) && !unRequerimientoAct.estaSatisfecho()]:
           - `requerimientosCompletos = false`.
       - **Bloque Bucle (`loop`)** [Mientras haya miembros activos && `disponibilidadValida`]:
         - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
         - `:ModaLink` consulta a `unMiembro`: `disponibilidadValida = verificarDisponibilidadMiembro(unMiembro, unProyecto)`.
       - **Bloque Alternativo (`alt`)** [!requerimientosCompletos || !disponibilidadValida]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("Requerimientos incompletos o indisponibilidad de recursos")`.
       - **Bloque Alternativo (`alt`)** [requerimientosCompletos && disponibilidadValida]:
         - `:ModaLink` envía a `unProyecto`: `setEstado(CONFIRMADO)`.
         - `:ModaLink` envía a `unProyecto`: `setFechaInicio(ahora)`.
         - **Bloque Bucle (`loop`)** [Mientras haya requerimientos generales]:
           - `:ModaLink` solicita a `requerimientosGenerales`: `unRequerimiento = obtenerSiguiente()`.
           - `:ModaLink` envía a `unRequerimiento`: `cerrarPostulaciones()`.
         - `:ModaLink` agrega a `notificaciones`: `notificarParticipantes(unProyecto)`.
         - `:ModaLink` retorna `unProyecto` al actor.

---

---

## 4. Postulaciones

### DSD darDeAltaPostulacionGeneral(unPerfil, unRequerimientoGeneral) | UC-26: Dar de alta postulación a proyecto

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `postulacionesGenerales` (Colección de postulaciones generales)
  - `miembros` (Colección de miembros de proyecto)
  - `notificaciones` (Colección de notificaciones)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unRequerimiento : RequerimientoGeneral` (Instancia de la entidad RequerimientoGeneral)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unaPostulacion : PostulacionGeneral` (Instancia de la entidad PostulacionGeneral)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
  - `boolean requerimientoEncontrado = false`
  - `boolean proyectoAcepta`
  - `boolean profesionCompatible`
  - `boolean esMiembro = false`
  - `boolean yaPostulado = false`
  - `boolean requerimientoSatisfecho`
- **Flujo de interacción:**
  1. El actor envía `unaPostulacion = darDeAltaPostulacionGeneral(unPerfil, unRequerimientoGeneral)` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilActivo`, `requerimientoEncontrado`, `proyectoAcepta`, `profesionCompatible`, `esMiembro`, `yaPostulado` y `requerimientoSatisfecho`.
  3. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  4. **Bloque Alternativo (`alt`)** [!perfilActivo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil no se encuentra activo")`.
  5. **Bloque Bucle (`loop`)** [Mientras haya requerimientos generales && `requerimientoEncontrado == false`]:
     - `:ModaLink` solicita a `requerimientosGenerales`: `unRequerimiento = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unRequerimiento`: `requerimientoEncontrado = tieneId(unRequerimientoGeneral)`.
  6. **Bloque Alternativo (`alt`)** [!requerimientoEncontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Requerimiento general inexistente")`.
  7. **Bloque Alternativo (`alt`)** [requerimientoEncontrado]:
     - `:ModaLink` solicita a `unRequerimiento`: `unProyecto = obtenerProyecto()`.
     - `:ModaLink` consulta a `unProyecto`: `proyectoAcepta = aceptaPostulaciones()`.
     - `:ModaLink` consulta a `unPerfil`: `profesionCompatible = tieneProfesion(unRequerimiento.obtenerProfesion())`.
     - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto && `esMiembro == false`]:
       - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unMiembro`: `esMiembro = correspondeA(unProyecto, unPerfil) && estaActivo()`.
     - **Bloque Bucle (`loop`)** [Mientras haya postulaciones generales && `yaPostulado == false`]:
       - `:ModaLink` solicita a `postulacionesGenerales`: `unaPostulacion = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaPostulacion`: `yaPostulado = correspondeA(unRequerimiento, unPerfil)`.
     - `:ModaLink` consulta a `unRequerimiento`: `requerimientoSatisfecho = estaSatisfecho()`.
     - **Bloque Alternativo (`alt`)** [!proyectoAcepta || !profesionCompatible || esMiembro || yaPostulado || requerimientoSatisfecho]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil no es elegible para postularse")`.
     - **Bloque Alternativo (`alt`)** [proyectoAcepta && profesionCompatible && !esMiembro && !yaPostulado && !requerimientoSatisfecho]:
       - `:ModaLink` crea la postulación: `unaPostulacion = registrar(unPerfil, unRequerimiento, PENDIENTE, ahora)`.
       - `postulacionesGenerales.agregar(unaPostulacion)`.
       - `:ModaLink` agrega a `notificaciones`: `notificarDirectores(unProyecto, unaPostulacion)`.
       - `:ModaLink` retorna `unaPostulacion` al actor.

---

### DSD darDeAltaPostulacionActividad(unPerfil, unRequerimientoActividad) | UC-26: Dar de alta postulación a proyecto

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `postulacionesActividad` (Colección de postulaciones de actividad)
  - `miembros` (Colección de miembros de proyecto)
  - `notificaciones` (Colección de notificaciones)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unaPostulacion : PostulacionActividad` (Instancia de la entidad PostulacionActividad)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
  - `boolean requerimientoEncontrado = false`
  - `boolean proyectoAcepta`
  - `boolean actividadPendiente`
  - `boolean profesionCompatible`
  - `boolean esMiembro = false`
  - `boolean yaPostulado = false`
  - `boolean disponible`
- **Flujo de interacción:**
  1. El actor envía `unaPostulacion = darDeAltaPostulacionActividad(unPerfil, unRequerimientoActividad)` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilActivo`, `requerimientoEncontrado`, `proyectoAcepta`, `actividadPendiente`, `profesionCompatible`, `esMiembro`, `yaPostulado` y `disponible`.
  3. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  4. **Bloque Alternativo (`alt`)** [!perfilActivo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil no se encuentra activo")`.
  5. **Bloque Bucle (`loop`)** [Mientras haya requerimientos de actividad && `requerimientoEncontrado == false`]:
     - `:ModaLink` solicita a `requerimientosActividad`: `unRequerimiento = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unRequerimiento`: `requerimientoEncontrado = tieneId(unRequerimientoActividad)`.
  6. **Bloque Alternativo (`alt`)** [!requerimientoEncontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Requerimiento de actividad inexistente")`.
  7. **Bloque Alternativo (`alt`)** [requerimientoEncontrado]:
     - `:ModaLink` solicita a `unRequerimiento`: `unaActividad = obtenerActividad()`.
     - `:ModaLink` solicita a `unaActividad`: `unProyecto = obtenerProyecto()`.
     - `:ModaLink` consulta a `unProyecto`: `proyectoAcepta = aceptaPostulaciones()`.
     - `:ModaLink` consulta a `unaActividad`: `actividadPendiente = estaPendiente()`.
     - `:ModaLink` consulta a `unPerfil`: `profesionCompatible = tieneProfesion(unRequerimiento.obtenerProfesion())`.
     - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto && `esMiembro == false`]:
       - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unMiembro`: `esMiembro = correspondeA(unProyecto, unPerfil) && estaActivo()`.
     - **Bloque Bucle (`loop`)** [Mientras haya postulaciones de actividad && `yaPostulado == false`]:
       - `:ModaLink` solicita a `postulacionesActividad`: `unaPostulacion = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaPostulacion`: `yaPostulado = correspondeA(unRequerimiento, unPerfil)`.
     - `:ModaLink` solicita a `unPerfil`: `unUsuario = obtenerUsuario()`.
     - `:ModaLink` consulta a `unUsuario`: `disponible = verificarDisponibilidad(unaActividad)`.
     - **Bloque Alternativo (`alt`)** [!proyectoAcepta || !actividadPendiente || !profesionCompatible || esMiembro || yaPostulado || !disponible]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil no es elegible para postularse a la actividad")`.
     - **Bloque Alternativo (`alt`)** [proyectoAcepta && actividadPendiente && profesionCompatible && !esMiembro && !yaPostulado && disponible]:
       - `:ModaLink` crea la postulación: `unaPostulacion = registrar(unPerfil, unRequerimiento, PENDIENTE, ahora)`.
       - `postulacionesActividad.agregar(unaPostulacion)`.
       - `:ModaLink` agrega a `notificaciones`: `notificarDirectores(unProyecto, unaPostulacion)`.
       - `:ModaLink` retorna `unaPostulacion` al actor.

---

### DSD darDeBajaPostulacion(unaPostulacion) | UC-27: Dar de baja postulación a proyecto

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `postulacionesGenerales` (Colección de postulaciones generales)
  - `postulacionesActividad` (Colección de postulaciones de actividad)
  - `unaPostulacion : PostulacionGeneral` (Instancia de la entidad PostulacionGeneral)
- **Variables locales / Notas:**
  - `boolean estaPendiente`
  - `boolean perteneceAlPerfil`
  - `boolean esDeActividad`
- **Flujo de interacción:**
  1. El actor envía `darDeBajaPostulacion(unaPostulacion)` a `:ModaLink`.
  2. Se inicializan las variables locales `estaPendiente`, `perteneceAlPerfil` y `esDeActividad`.
  3. `:ModaLink` consulta a `unaPostulacion`: `estaPendiente = estaEnEstado(PENDIENTE)`.
  4. `:ModaLink` consulta a `unaPostulacion`: `perteneceAlPerfil = correspondeA(perfilActivo)`.
  5. `:ModaLink` consulta a `unaPostulacion`: `esDeActividad = esPostulacionDeActividad()`.
  6. **Bloque Alternativo (`alt`)** [!estaPendiente || !perteneceAlPerfil]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("La postulación no está pendiente o no pertenece al perfil")`.
  7. **Bloque Alternativo (`alt`)** [estaPendiente && perteneceAlPerfil]:
     - **Bloque Alternativo (`alt`)** [esDeActividad]:
       - `:ModaLink` solicita a `postulacionesActividad`: `eliminar(unaPostulacion)`.
     - **Bloque Alternativo (`alt`)** [!esDeActividad]:
       - `:ModaLink` solicita a `postulacionesGenerales`: `eliminar(unaPostulacion)`.
     - `:ModaLink` retorna al actor: `postulacionEliminada()`.

---

### DSD buscarPostulaciones(unProyecto, estado, tipo, nombrePerfil, profesion, fechaDesde, fechaHasta) | UC-74: Buscar postulaciones

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `postulacionesGenerales` (Colección de postulaciones generales)
  - `postulacionesActividad` (Colección de postulaciones de actividad)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unaPostulacion : PostulacionGeneral` (Instancia de la entidad PostulacionGeneral)
- **Variables locales / Notas:**
  - `Lista postulacionesFiltradas = nueva Lista()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `postulaciones = buscarPostulaciones(unProyecto, estado, tipo, nombrePerfil, profesion, fechaDesde, fechaHasta)` a `:ModaLink`.
  2. Se inicializan las variables locales `postulacionesFiltradas` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya postulaciones generales del proyecto]:
     - `:ModaLink` solicita a `postulacionesGenerales`: `unaPostulacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaPostulacion`: `sePuedeAgregar = cumpleFiltros(unProyecto, estado, tipo, nombrePerfil, profesion, fechaDesde, fechaHasta)`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `postulacionesFiltradas.agregar(unaPostulacion)`.
  4. **Bloque Bucle (`loop`)** [Mientras haya postulaciones de actividad del proyecto]:
     - `:ModaLink` solicita a `postulacionesActividad`: `unaPostulacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaPostulacion`: `sePuedeAgregar = cumpleFiltros(unProyecto, estado, tipo, nombrePerfil, profesion, fechaDesde, fechaHasta)`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `postulacionesFiltradas.agregar(unaPostulacion)`.
  5. `:ModaLink` ordena `postulacionesFiltradas` por estado y fecha.
  6. `:ModaLink` retorna `postulacionesFiltradas` al actor.

---

### DSD seleccionarPostulacion(idPostulacion, postulaciones) | UC-74: Buscar postulaciones

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `postulacionesGenerales` (Colección de postulaciones generales)
  - `postulacionesActividad` (Colección de postulaciones de actividad)
  - `unaPostulacion : PostulacionGeneral` (Instancia de la entidad PostulacionGeneral)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unaPostulacion = seleccionarPostulacion(idPostulacion, postulaciones)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya postulaciones generales && `retorno == false`]:
     - `:ModaLink` solicita a `postulacionesGenerales`: `unaPostulacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaPostulacion`: `retorno = tieneId(idPostulacion)`.
  4. **Bloque Bucle (`loop`)** [Mientras haya postulaciones de actividad && `retorno == false`]:
     - `:ModaLink` solicita a `postulacionesActividad`: `unaPostulacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaPostulacion`: `retorno = tieneId(idPostulacion)`.
  5. `:ModaLink` retorna `unaPostulacion` al actor.

---

### DSD aceptarPostulacion(unaPostulacion) | UC-34: Gestionar postulaciones

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `miembros` (Colección de miembros de proyecto)
  - `asignaciones` (Colección de asignaciones de actividad)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `notificaciones` (Colección de notificaciones)
  - `unaPostulacion : PostulacionGeneral` (Instancia de la entidad PostulacionGeneral)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unaAsignacion : AsignacionActividad` (Instancia de la entidad AsignacionActividad)
  - `unRequerimientoGral : RequerimientoGeneral` (Instancia de la entidad RequerimientoGeneral)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
- **Variables locales / Notas:**
  - `boolean estaPendiente`
  - `boolean esMiembro = false`
  - `boolean esDeActividad`
- **Flujo de interacción:**
  1. El actor envía `aceptarPostulacion(unaPostulacion)` a `:ModaLink`.
  2. Se inicializan las variables locales `estaPendiente`, `esMiembro` y `esDeActividad`.
  3. `:ModaLink` consulta a `unaPostulacion`: `estaPendiente = estaEnEstado(PENDIENTE)`.
  4. **Bloque Alternativo (`alt`)** [!estaPendiente]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("La postulación no está pendiente")`.
  5. **Bloque Alternativo (`alt`)** [estaPendiente]:
     - `:ModaLink` solicita a `unaPostulacion`: `unPerfil = obtenerPerfil()`.
     - `:ModaLink` solicita a `unaPostulacion`: `unProyecto = obtenerProyecto()`.
     - `:ModaLink` consulta a `unaPostulacion`: `esDeActividad = esPostulacionDeActividad()`.
     - **Bloque Alternativo (`alt`)** [esDeActividad]:
       - `:ModaLink` solicita a `unaPostulacion`: `unaActividad = obtenerActividad()`.
       - `:ModaLink` solicita a `unaPostulacion`: `unRequerimiento = obtenerRequerimientoActividad()`.
     - **Bloque Alternativo (`alt`)** [!esDeActividad]:
       - `:ModaLink` solicita a `unaPostulacion`: `unRequerimientoGral = obtenerRequerimientoGeneral()`.
     - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto && `esMiembro == false`]:
       - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unMiembro`: `esMiembro = correspondeA(unProyecto, unPerfil) && estaActivo()`.
     - **Bloque Alternativo (`alt`)** [esMiembro]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil ya es participante del proyecto")`.
     - **Bloque Alternativo (`alt`)** [!esMiembro]:
       - `:ModaLink` crea o reutiliza el miembro: `unMiembro = registrar(unProyecto, unPerfil, ACTIVO, unRequerimientoGral)`.
       - `miembros.agregar(unMiembro)`.
       - `:ModaLink` envía a `unaPostulacion`: `setEstado(ACEPTADA)`.
       - **Bloque Opcional (`opt`)** [esDeActividad]:
         - `:ModaLink` crea la asignación: `unaAsignacion = registrar(unMiembro, unaActividad, unRequerimiento)`.
         - `asignaciones.agregar(unaAsignacion)`.
       - `:ModaLink` actualiza el requerimiento: `actualizarEstadoRequerimiento(unProyecto)`.
       - `:ModaLink` agrega a `notificaciones`: `notificarPostulante(unaPostulacion)`.
       - `:ModaLink` retorna al actor: `postulacionAceptada(unaPostulacion)`.

---

### DSD rechazarPostulacion(unaPostulacion) | UC-34: Gestionar postulaciones

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `notificaciones` (Colección de notificaciones)
  - `unaPostulacion : PostulacionGeneral` (Instancia de la entidad PostulacionGeneral)
- **Variables locales / Notas:**
  - `boolean estaPendiente`
- **Flujo de interacción:**
  1. El actor envía `rechazarPostulacion(unaPostulacion)` a `:ModaLink`.
  2. Se inicializa la variable local `estaPendiente`.
  3. `:ModaLink` consulta a `unaPostulacion`: `estaPendiente = estaEnEstado(PENDIENTE)`.
  4. **Bloque Alternativo (`alt`)** [!estaPendiente]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("La postulación no está pendiente")`.
  5. **Bloque Alternativo (`alt`)** [estaPendiente]:
     - `:ModaLink` envía a `unaPostulacion`: `setEstado(RECHAZADA)`.
     - `:ModaLink` agrega a `notificaciones`: `notificarPostulanteRechazado(unaPostulacion)`.
     - `:ModaLink` retorna al actor: `postulacionRechazada(unaPostulacion)`.

---

---

## 5. Invitaciones

### DSD buscarInvitaciones(idProyecto, nombreProyecto, fechaDesde, fechaHasta, perfil, direccion, estado, tipo) | UC-30: Buscar invitaciones

- **Actores:** Usuario / Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `invitacionesGenerales` (Colección de invitaciones generales)
  - `invitacionesActividad` (Colección de invitaciones de actividad)
  - `unaInvitacion : InvitacionGeneral` (Instancia de la entidad InvitacionGeneral)
- **Variables locales / Notas:**
  - `Lista invitacionesFiltradas = nueva Lista()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `invitaciones = buscarInvitaciones(idProyecto, nombreProyecto, fechaDesde, fechaHasta, perfil, direccion, estado, tipo)` a `:ModaLink`.
  2. Se inicializan las variables locales `invitacionesFiltradas` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya invitaciones generales del perfil activo]:
     - `:ModaLink` solicita a `invitacionesGenerales`: `unaInvitacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaInvitacion`: `sePuedeAgregar = cumpleFiltros(perfilActivo, idProyecto, nombreProyecto, fechaDesde, fechaHasta, perfil, direccion, estado, "GENERAL")`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `invitacionesFiltradas.agregar(unaInvitacion)`.
  4. **Bloque Bucle (`loop`)** [Mientras haya invitaciones de actividad del perfil activo]:
     - `:ModaLink` solicita a `invitacionesActividad`: `unaInvitacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaInvitacion`: `sePuedeAgregar = cumpleFiltros(perfilActivo, idProyecto, nombreProyecto, fechaDesde, fechaHasta, perfil, direccion, estado, "ACTIVIDAD")`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `invitacionesFiltradas.agregar(unaInvitacion)`.
  5. `:ModaLink` unifica, ordena y pagina `invitacionesFiltradas`.
  6. `:ModaLink` retorna `invitacionesFiltradas` al actor.

---

### DSD seleccionarInvitacion(idInvitacion, invitaciones) | UC-30: Buscar invitaciones

- **Actores:** Usuario / Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `invitacionesGenerales` (Colección de invitaciones generales)
  - `invitacionesActividad` (Colección de invitaciones de actividad)
  - `unaInvitacion : InvitacionGeneral` (Instancia de la entidad InvitacionGeneral)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unaInvitacion = seleccionarInvitacion(idInvitacion, invitaciones)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya invitaciones generales && `retorno == false`]:
     - `:ModaLink` solicita a `invitacionesGenerales`: `unaInvitacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaInvitacion`: `retorno = tieneId(idInvitacion)`.
  4. **Bloque Bucle (`loop`)** [Mientras haya invitaciones de actividad && `retorno == false`]:
     - `:ModaLink` solicita a `invitacionesActividad`: `unaInvitacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaInvitacion`: `retorno = tieneId(idInvitacion)`.
  5. `:ModaLink` retorna `unaInvitacion` al actor.

---

### DSD invitarAProyecto(unProyecto, unPerfil, unRequerimientoGralOpcional, mensaje) | UC-31: Invitar a proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `miembros` (Colección de miembros de proyecto)
  - `invitacionesGenerales` (Colección de invitaciones generales)
  - `invitacionesActividad` (Colección de invitaciones de actividad)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `notificaciones` (Colección de notificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unaInvitacion : InvitacionGeneral` (Instancia de la entidad InvitacionGeneral)
  - `unRequerimiento : RequerimientoGeneral` (Instancia de la entidad RequerimientoGeneral)
- **Variables locales / Notas:**
  - `boolean proyectoValido`
  - `boolean perfilActivo`
  - `boolean esAutoinvitacion`
  - `boolean esMiembro = false`
  - `boolean invitacionDuplicada = false`
  - `boolean requerimientoEncontrado = false`
  - `boolean requerimientoValido`
  - `boolean profesionCompatible`
  - `boolean cupoDisponible`
- **Flujo de interacción:**
  1. El actor envía `unaInvitacion = invitarAProyecto(unProyecto, unPerfil, unRequerimientoGralOpcional, mensaje)` a `:ModaLink`.
  2. Se inicializan las variables locales `proyectoValido`, `perfilActivo`, `esAutoinvitacion`, `esMiembro`, `invitacionDuplicada`, `requerimientoEncontrado`, `requerimientoValido`, `profesionCompatible` y `cupoDisponible`.
  3. `:ModaLink` consulta a `unProyecto`: `proyectoValido = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
  4. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  5. `:ModaLink` evalúa: `esAutoinvitacion = unPerfil == perfilActivo`.
  6. **Bloque Alternativo (`alt`)** [!proyectoValido || !perfilActivo || esAutoinvitacion]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Proyecto, perfil o autoinvitación inválida")`.
  7. **Bloque Alternativo (`alt`)** [proyectoValido && perfilActivo && !esAutoinvitacion]:
     - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto && `esMiembro == false`]:
       - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unMiembro`: `esMiembro = correspondeA(unProyecto, unPerfil) && estaActivo()`.
     - **Bloque Bucle (`loop`)** [Mientras haya invitaciones generales && `invitacionDuplicada == false`]:
       - `:ModaLink` solicita a `invitacionesGenerales`: `unaInvitacion = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaInvitacion`: `invitacionDuplicada = correspondeA(unProyecto, unPerfil) && estaPendiente()`.
     - **Bloque Bucle (`loop`)** [Mientras haya invitaciones de actividad && `invitacionDuplicada == false`]:
       - `:ModaLink` solicita a `invitacionesActividad`: `unaInvitacion = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaInvitacion`: `invitacionDuplicada = correspondeA(unProyecto, unPerfil) && estaPendiente()`.
     - **Bloque Alternativo (`alt`)** [esMiembro || invitacionDuplicada]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil ya es miembro o tiene una invitación pendiente")`.
     - **Bloque Alternativo (`alt`)** [!esMiembro && !invitacionDuplicada]:
       - **Bloque Opcional (`opt`)** [unRequerimientoGralOpcional != null]:
         - **Bloque Bucle (`loop`)** [Mientras haya requerimientos generales && `requerimientoEncontrado == false`]:
           - `:ModaLink` solicita a `requerimientosGenerales`: `unRequerimiento = obtenerSiguiente()`.
           - `:ModaLink` consulta a `unRequerimiento`: `requerimientoEncontrado = tieneId(unRequerimientoGralOpcional)`.
         - `:ModaLink` consulta a `unRequerimiento`: `requerimientoValido = requerimientoEncontrado && perteneceA(unProyecto) && !estaSatisfecho()`.
         - `:ModaLink` consulta a `unPerfil`: `profesionCompatible = tieneProfesion(unRequerimiento.obtenerProfesion())`.
         - `:ModaLink` consulta a `unRequerimiento`: `cupoDisponible = tieneCupoDisponible()`.
         - **Bloque Alternativo (`alt`)** [!requerimientoValido || !profesionCompatible || !cupoDisponible]:
           - `:ModaLink` retorna una excepción al actor: `Excepcion("Requerimiento, profesión o cupo inválido")`.
       - **Bloque Alternativo (`alt`)** [unRequerimientoGralOpcional == null || (requerimientoValido && profesionCompatible && cupoDisponible)]:
         - `:ModaLink` crea la invitación: `unaInvitacion = registrar(unProyecto, unPerfil, perfilActivo, unRequerimientoGralOpcional, mensaje, PENDIENTE, ahora)`.
         - `invitacionesGenerales.agregar(unaInvitacion)`.
         - `:ModaLink` agrega a `notificaciones`: `notificarPerfil(unPerfil, unaInvitacion)`.
         - `:ModaLink` retorna `unaInvitacion` al actor.

---

### DSD invitarAProyectoActividad(unProyecto, unRequerimientoAct, unPerfil, mensaje) | UC-31: Invitar a proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `miembros` (Colección de miembros de proyecto)
  - `invitacionesGenerales` (Colección de invitaciones generales)
  - `invitacionesActividad` (Colección de invitaciones de actividad)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `notificaciones` (Colección de notificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unaInvitacion : InvitacionActividad` (Instancia de la entidad InvitacionActividad)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
- **Variables locales / Notas:**
  - `boolean proyectoValido`
  - `boolean perfilActivo`
  - `boolean esAutoinvitacion`
  - `boolean esMiembro = false`
  - `boolean invitacionDuplicada = false`
  - `boolean requerimientoEncontrado = false`
  - `boolean requerimientoValido`
  - `boolean actividadValida`
  - `boolean profesionCompatible`
  - `boolean cupoDisponible`
- **Flujo de interacción:**
  1. El actor envía `unaInvitacion = invitarAProyectoActividad(unProyecto, unRequerimientoAct, unPerfil, mensaje)` a `:ModaLink`.
  2. Se inicializan las variables locales `proyectoValido`, `perfilActivo`, `esAutoinvitacion`, `esMiembro`, `invitacionDuplicada`, `requerimientoEncontrado`, `requerimientoValido`, `actividadValida`, `profesionCompatible` y `cupoDisponible`.
  3. `:ModaLink` consulta a `unProyecto`: `proyectoValido = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
  4. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  5. `:ModaLink` evalúa: `esAutoinvitacion = unPerfil == perfilActivo`.
  6. **Bloque Alternativo (`alt`)** [!proyectoValido || !perfilActivo || esAutoinvitacion]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Proyecto, perfil o autoinvitación inválida")`.
  7. **Bloque Alternativo (`alt`)** [proyectoValido && perfilActivo && !esAutoinvitacion]:
     - **Bloque Bucle (`loop`)** [Mientras haya requerimientos de actividad && `requerimientoEncontrado == false`]:
       - `:ModaLink` solicita a `requerimientosActividad`: `unRequerimiento = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unRequerimiento`: `requerimientoEncontrado = tieneId(unRequerimientoAct)`.
     - `:ModaLink` consulta a `unRequerimiento`: `requerimientoValido = requerimientoEncontrado && perteneceA(unProyecto) && !estaSatisfecho()`.
     - `:ModaLink` solicita a `unRequerimiento`: `unaActividad = obtenerActividad()`.
     - `:ModaLink` consulta a `unaActividad`: `actividadValida = estaPendiente() || estaEnCurso()`.
     - `:ModaLink` consulta a `unPerfil`: `profesionCompatible = tieneProfesion(unRequerimiento.obtenerProfesion())`.
     - `:ModaLink` consulta a `unRequerimiento`: `cupoDisponible = tieneCupoDisponible()`.
     - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto && `esMiembro == false`]:
       - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unMiembro`: `esMiembro = correspondeA(unProyecto, unPerfil) && estaActivo()`.
     - **Bloque Bucle (`loop`)** [Mientras haya invitaciones generales && `invitacionDuplicada == false`]:
       - `:ModaLink` solicita a `invitacionesGenerales`: `unaInvitacion = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaInvitacion`: `invitacionDuplicada = correspondeA(unProyecto, unPerfil) && estaPendiente()`.
     - **Bloque Bucle (`loop`)** [Mientras haya invitaciones de actividad && `invitacionDuplicada == false`]:
       - `:ModaLink` solicita a `invitacionesActividad`: `unaInvitacion = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaInvitacion`: `invitacionDuplicada = correspondeA(unProyecto, unPerfil) && estaPendiente()`.
     - **Bloque Alternativo (`alt`)** [!requerimientoValido || !actividadValida || !profesionCompatible || !cupoDisponible || esMiembro || invitacionDuplicada]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Requerimiento, actividad, profesión, cupo o duplicado inválido")`.
     - **Bloque Alternativo (`alt`)** [requerimientoValido && actividadValida && profesionCompatible && cupoDisponible && !esMiembro && !invitacionDuplicada]:
       - `:ModaLink` crea la invitación: `unaInvitacion = registrar(unRequerimiento, unPerfil, perfilActivo, mensaje, PENDIENTE, ahora)`.
       - `invitacionesActividad.agregar(unaInvitacion)`.
       - `:ModaLink` agrega a `notificaciones`: `notificarPerfil(unPerfil, unaInvitacion)`.
       - `:ModaLink` retorna `unaInvitacion` al actor.

---

### DSD invitarAProyectoMultiple(unProyecto, unRequerimientoGralOpcional, perfiles[], mensaje) | UC-31: Invitar a proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `perfiles` (Colección de perfiles)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unaInvitacion : InvitacionGeneral` (Instancia de la entidad InvitacionGeneral)
- **Variables locales / Notas:**
  - `Lista creadas = nueva Lista()`
  - `Lista errores = nueva Lista()`
- **Flujo de interacción:**
  1. El actor envía `resultado = invitarAProyectoMultiple(unProyecto, unRequerimientoGralOpcional, perfiles[], mensaje)` a `:ModaLink`.
  2. Se inicializan las variables locales `creadas` y `errores`.
  3. `:ModaLink` deduplica `perfiles[]`.
  4. **Bloque Bucle (`loop`)** [Mientras haya perfiles por procesar]:
     - `:ModaLink` solicita a `perfiles`: `unPerfil = obtenerSiguiente()`.
     - `:ModaLink` ejecuta de forma independiente: `unaInvitacion = invitarAProyecto(unProyecto, unPerfil, unRequerimientoGralOpcional, mensaje)`.
     - **Bloque Alternativo (`alt`)** [la operación finaliza correctamente]:
       - `creadas.agregar(unaInvitacion)`.
     - **Bloque Alternativo (`alt`)** [la operación lanza una excepción]:
       - `errores.agregar(unPerfil, excepcion)`.
  5. `:ModaLink` retorna al actor: `ResultadoMultiple(creadas, errores)`.

---

### DSD invitarAProyectoActividadMultiple(unProyecto, unRequerimientoAct, perfiles[], mensaje) | UC-31: Invitar a proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `perfiles` (Colección de perfiles)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
  - `unaInvitacion : InvitacionActividad` (Instancia de la entidad InvitacionActividad)
- **Variables locales / Notas:**
  - `Lista creadas = nueva Lista()`
  - `Lista errores = nueva Lista()`
- **Flujo de interacción:**
  1. El actor envía `resultado = invitarAProyectoActividadMultiple(unProyecto, unRequerimientoAct, perfiles[], mensaje)` a `:ModaLink`.
  2. Se inicializan las variables locales `creadas` y `errores`.
  3. `:ModaLink` deduplica `perfiles[]`.
  4. **Bloque Bucle (`loop`)** [Mientras haya perfiles por procesar]:
     - `:ModaLink` solicita a `perfiles`: `unPerfil = obtenerSiguiente()`.
     - `:ModaLink` ejecuta de forma independiente: `unaInvitacion = invitarAProyectoActividad(unProyecto, unRequerimientoAct, unPerfil, mensaje)`.
     - **Bloque Alternativo (`alt`)** [la operación finaliza correctamente]:
       - `creadas.agregar(unaInvitacion)`.
     - **Bloque Alternativo (`alt`)** [la operación lanza una excepción]:
       - `errores.agregar(unPerfil, excepcion)`.
  5. `:ModaLink` retorna al actor: `ResultadoMultiple(creadas, errores)`.

---

### DSD aceptarIncorporacion(unaInvitacion, unProyecto) | UC-28: Aceptar solicitud de incorporación

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `miembros` (Colección de miembros de proyecto)
  - `invitacionesGenerales` (Colección de invitaciones generales)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `notificaciones` (Colección de notificaciones)
  - `unaInvitacion : InvitacionGeneral` (Instancia de la entidad InvitacionGeneral)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unRequerimiento : RequerimientoGeneral` (Instancia de la entidad RequerimientoGeneral)
- **Variables locales / Notas:**
  - `boolean invitacionValida`
  - `boolean proyectoValido`
  - `boolean esMiembro = false`
  - `boolean requerimientoValido`
  - `boolean profesionCompatible`
  - `boolean cupoCubierto`
- **Flujo de interacción:**
  1. El actor envía `aceptarIncorporacion(unaInvitacion, unProyecto)` a `:ModaLink`.
  2. Se inicializan las variables locales `invitacionValida`, `proyectoValido`, `esMiembro`, `requerimientoValido`, `profesionCompatible` y `cupoCubierto`.
  3. `:ModaLink` consulta a `unaInvitacion`: `invitacionValida = estaPendiente() && correspondeA(perfilActivo)`.
  4. `:ModaLink` consulta a `unProyecto`: `proyectoValido = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
  5. **Bloque Alternativo (`alt`)** [!invitacionValida || !proyectoValido]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Invitación o proyecto inválido")`.
  6. **Bloque Alternativo (`alt`)** [invitacionValida && proyectoValido]:
     - `:ModaLink` solicita a `unaInvitacion`: `unPerfil = obtenerPerfil()`.
     - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto && `esMiembro == false`]:
       - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unMiembro`: `esMiembro = correspondeA(unProyecto, unPerfil) && estaActivo()`.
     - **Bloque Alternativo (`alt`)** [esMiembro]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("El usuario ya es participante del proyecto")`.
     - **Bloque Alternativo (`alt`)** [!esMiembro]:
       - **Bloque Opcional (`opt`)** [unaInvitacion.tieneRequerimientoGeneral()]:
         - `:ModaLink` solicita a `unaInvitacion`: `unRequerimiento = obtenerRequerimientoGeneral()`.
         - `:ModaLink` consulta a `unRequerimiento`: `requerimientoValido = !estaSatisfecho()`.
         - `:ModaLink` consulta a `unPerfil`: `profesionCompatible = tieneProfesion(unRequerimiento.obtenerProfesion())`.
         - **Bloque Alternativo (`alt`)** [!requerimientoValido || !profesionCompatible]:
           - `:ModaLink` retorna una excepción al actor: `Excepcion("Requerimiento satisfecho o profesión incompatible")`.
       - **Bloque Alternativo (`alt`)** [!unaInvitacion.tieneRequerimientoGeneral() || (requerimientoValido && profesionCompatible)]:
         - `:ModaLink` crea el miembro: `unMiembro = registrar(unProyecto, unPerfil, ACTIVO, unRequerimiento)`.
         - `miembros.agregar(unMiembro)`.
         - `:ModaLink` envía a `unaInvitacion`: `setEstado(ACEPTADA)`.
         - **Bloque Opcional (`opt`)** [unaInvitacion.tieneRequerimientoGeneral()]:
           - `:ModaLink` consulta a `unRequerimiento`: `cupoCubierto = estaSatisfecho()`.
           - **Bloque Alternativo (`alt`)** [cupoCubierto]:
             - `:ModaLink` solicita a `invitacionesGenerales`: `eliminarPendientesDe(unRequerimiento)`.
         - `:ModaLink` agrega a `notificaciones`: `notificarDirectores(unProyecto)`.
         - `:ModaLink` retorna al actor: `incorporacionRegistrada(unProyecto)`.

---

### DSD aceptarIncorporacionAct(unaInvitacion, unProyecto, unRequerimientoAct) | UC-28: Aceptar solicitud de incorporación

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `miembros` (Colección de miembros de proyecto)
  - `asignaciones` (Colección de asignaciones de actividad)
  - `invitacionesActividad` (Colección de invitaciones de actividad)
  - `notificaciones` (Colección de notificaciones)
  - `unaInvitacion : InvitacionActividad` (Instancia de la entidad InvitacionActividad)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unaAsignacion : AsignacionActividad` (Instancia de la entidad AsignacionActividad)
- **Variables locales / Notas:**
  - `boolean invitacionValida`
  - `boolean proyectoValido`
  - `boolean actividadValida`
  - `boolean esDirector = false`
  - `boolean esMiembro = false`
  - `boolean disponible`
  - `boolean cupoCubierto`
- **Flujo de interacción:**
  1. El actor envía `aceptarIncorporacionAct(unaInvitacion, unProyecto, unRequerimientoAct)` a `:ModaLink`.
  2. Se inicializan las variables locales `invitacionValida`, `proyectoValido`, `actividadValida`, `esDirector`, `esMiembro`, `disponible` y `cupoCubierto`.
  3. `:ModaLink` consulta a `unaInvitacion`: `invitacionValida = estaPendiente() && correspondeA(perfilActivo)`.
  4. `:ModaLink` consulta a `unProyecto`: `proyectoValido = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
  5. `:ModaLink` solicita a `unRequerimiento`: `unaActividad = obtenerActividad()`.
  6. `:ModaLink` consulta a `unaActividad`: `actividadValida = estaPendiente() || estaEnCurso()`.
  7. **Bloque Alternativo (`alt`)** [!invitacionValida || !proyectoValido || !actividadValida]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Invitación, proyecto o actividad inválida")`.
  8. **Bloque Alternativo (`alt`)** [invitacionValida && proyectoValido && actividadValida]:
     - `:ModaLink` solicita a `unaInvitacion`: `unPerfil = obtenerPerfil()`.
     - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto]:
       - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
       - **Bloque Alternativo (`alt`)** [unMiembro.correspondeA(unProyecto, unPerfil) && unMiembro.esDirector()]:
         - `esDirector = true`.
       - **Bloque Alternativo (`alt`)** [unMiembro.correspondeA(unProyecto, unPerfil) && unMiembro.estaActivo()]:
         - `esMiembro = true`.
     - **Bloque Alternativo (`alt`)** [esDirector]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("El director no puede aceptar esta invitación")`.
     - **Bloque Alternativo (`alt`)** [!esDirector]:
       - `:ModaLink` solicita a `unPerfil`: `unUsuario = obtenerUsuario()`.
       - `:ModaLink` consulta a `unUsuario`: `disponible = verificarDisponibilidad(unaActividad)`.
       - **Bloque Alternativo (`alt`)** [!disponible]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("Disponibilidad insuficiente para la actividad")`.
       - **Bloque Alternativo (`alt`)** [disponible]:
         - **Bloque Alternativo (`alt`)** [!esMiembro]:
           - `:ModaLink` crea el miembro: `unMiembro = registrar(unProyecto, unPerfil, ACTIVO, unRequerimiento)`.
           - `miembros.agregar(unMiembro)`.
         - `:ModaLink` crea la asignación: `unaAsignacion = registrar(unMiembro, unaActividad, unRequerimiento)`.
         - `asignaciones.agregar(unaAsignacion)`.
         - `:ModaLink` envía a `unaInvitacion`: `setEstado(ACEPTADA)`.
         - `:ModaLink` consulta a `unRequerimiento`: `cupoCubierto = estaSatisfecho()`.
         - **Bloque Alternativo (`alt`)** [cupoCubierto]:
           - `:ModaLink` solicita a `invitacionesActividad`: `eliminarPendientesDe(unRequerimiento)`.
         - `:ModaLink` agrega a `notificaciones`: `notificarDirectores(unProyecto)`.
         - `:ModaLink` retorna al actor: `incorporacionRegistrada(unProyecto)`.

---

### DSD rechazarInvitacion(unaInvitacion) | UC-28: Aceptar solicitud de incorporación

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `notificaciones` (Colección de notificaciones)
  - `unaInvitacion : InvitacionGeneral` (Instancia de la entidad InvitacionGeneral)
- **Variables locales / Notas:**
  - `boolean invitacionValida`
- **Flujo de interacción:**
  1. El actor envía `rechazarInvitacion(unaInvitacion)` a `:ModaLink`.
  2. Se inicializa la variable local `invitacionValida`.
  3. `:ModaLink` consulta a `unaInvitacion`: `invitacionValida = estaPendiente() && correspondeA(perfilActivo)`.
  4. **Bloque Alternativo (`alt`)** [!invitacionValida]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("La invitación no está pendiente o no pertenece al perfil")`.
  5. **Bloque Alternativo (`alt`)** [invitacionValida]:
     - `:ModaLink` envía a `unaInvitacion`: `setEstado(RECHAZADA)`.
     - `:ModaLink` envía a `unaInvitacion`: `setFechaRechazo(ahora)`.
     - `:ModaLink` agrega a `notificaciones`: `notificarDirectores(unaInvitacion.obtenerProyecto())`.
     - `:ModaLink` retorna al actor: `invitacionRechazada(unaInvitacion)`.

---

### DSD darDeBajaInvitacion(unaInvitacion) | UC-32: Eliminar invitación a proyecto

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `invitacionesGenerales` (Colección de invitaciones generales)
  - `invitacionesActividad` (Colección de invitaciones de actividad)
  - `unaInvitacion : InvitacionGeneral` (Instancia de la entidad InvitacionGeneral)
- **Variables locales / Notas:**
  - `boolean estaPendiente`
  - `boolean esRemitente`
  - `boolean esDeActividad`
- **Flujo de interacción:**
  1. El actor envía `darDeBajaInvitacion(unaInvitacion)` a `:ModaLink`.
  2. Se inicializan las variables locales `estaPendiente`, `esRemitente` y `esDeActividad`.
  3. `:ModaLink` consulta a `unaInvitacion`: `estaPendiente = estaPendiente()`.
  4. `:ModaLink` consulta a `unaInvitacion`: `esRemitente = fueEnviadaPor(perfilActivo)`.
  5. `:ModaLink` consulta a `unaInvitacion`: `esDeActividad = esInvitacionDeActividad()`.
  6. **Bloque Alternativo (`alt`)** [!estaPendiente || !esRemitente]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("La invitación no está pendiente o el actor no es el remitente")`.
  7. **Bloque Alternativo (`alt`)** [estaPendiente && esRemitente]:
     - **Bloque Alternativo (`alt`)** [esDeActividad]:
       - `:ModaLink` solicita a `invitacionesActividad`: `eliminar(unaInvitacion)`.
     - **Bloque Alternativo (`alt`)** [!esDeActividad]:
       - `:ModaLink` solicita a `invitacionesGenerales`: `eliminar(unaInvitacion)`.
     - `:ModaLink` retorna al actor: `invitacionEliminada()`.

---

---

## 6. Requerimientos

### DSD buscarRequerimientosGenerales(profesion, caracteristicas[], unProyecto) | UC-72: Buscar requerimiento general

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unRequerimiento : RequerimientoGeneral` (Instancia de la entidad RequerimientoGeneral)
- **Variables locales / Notas:**
  - `Lista requerimientosFiltrados = nueva Lista()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `requerimientos = buscarRequerimientosGenerales(profesion, caracteristicas[], unProyecto)` a `:ModaLink`.
  2. Se inicializan las variables locales `requerimientosFiltrados` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya requerimientos generales abiertos]:
     - `:ModaLink` solicita a `requerimientosGenerales`: `unRequerimiento = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unRequerimiento`: `sePuedeAgregar = cumpleFiltros(profesion, caracteristicas, unProyecto) && !estaSatisfecho()`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `requerimientosFiltrados.agregar(unRequerimiento)`.
  4. `:ModaLink` retorna `requerimientosFiltrados` al actor.

---

### DSD seleccionarRequerimientoGeneral(idRequerimientoGeneral, requerimientos) | UC-72: Buscar requerimiento general

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `requerimientosGenerales` (Colección de requerimientos generales)
  - `unRequerimiento : RequerimientoGeneral` (Instancia de la entidad RequerimientoGeneral)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unRequerimiento = seleccionarRequerimientoGeneral(idRequerimientoGeneral, requerimientos)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya requerimientos generales && `retorno == false`]:
     - `:ModaLink` solicita a `requerimientosGenerales`: `unRequerimiento = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unRequerimiento`: `retorno = tieneId(idRequerimientoGeneral)`.
  4. `:ModaLink` retorna `unRequerimiento` al actor.

---

### DSD buscarRequerimientosActividad(idProyecto, profesion, caracteristicas[], unaActividad) | UC-73: Buscar requerimiento actividad

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
- **Variables locales / Notas:**
  - `Lista requerimientosFiltrados = nueva Lista()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `requerimientos = buscarRequerimientosActividad(idProyecto, profesion, caracteristicas[], unaActividad)` a `:ModaLink`.
  2. Se inicializan las variables locales `requerimientosFiltrados` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya requerimientos de actividad abiertos]:
     - `:ModaLink` solicita a `requerimientosActividad`: `unRequerimiento = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unRequerimiento`: `sePuedeAgregar = cumpleFiltros(idProyecto, profesion, caracteristicas, unaActividad) && !estaSatisfecho()`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `requerimientosFiltrados.agregar(unRequerimiento)`.
  4. `:ModaLink` retorna `requerimientosFiltrados` al actor.

---

### DSD seleccionarRequerimientoActividad(idRequerimientoActividad, requerimientos) | UC-73: Buscar requerimiento actividad

- **Actores:** Usuario
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unRequerimiento = seleccionarRequerimientoActividad(idRequerimientoActividad, requerimientos)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya requerimientos de actividad && `retorno == false`]:
     - `:ModaLink` solicita a `requerimientosActividad`: `unRequerimiento = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unRequerimiento`: `retorno = tieneId(idRequerimientoActividad)`.
  4. `:ModaLink` retorna `unRequerimiento` al actor.

---

## 7. Actividades

### DSD crearActividad(unProyecto, nombre, descripcion, duracion) | UC-49: Crear actividad a planificación

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `planificaciones` (Colección de planificaciones)
  - `actividades` (Colección de actividades)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unaPlanificacion : Planificacion` (Instancia de la entidad Planificacion)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
- **Variables locales / Notas:**
  - `boolean planificacionEncontrada = false`
  - `boolean nombreDuplicado = false`
  - `boolean datosValidos`
- **Flujo de interacción:**
  1. El actor envía `unaActividad = crearActividad(unProyecto, nombre, descripcion, duracion)` a `:ModaLink`.
  2. Se inicializan las variables locales `planificacionEncontrada`, `nombreDuplicado` y `datosValidos`.
  3. **Bloque Bucle (`loop`)** [Mientras haya planificaciones && `planificacionEncontrada == false`]:
     - `:ModaLink` solicita a `planificaciones`: `unaPlanificacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaPlanificacion`: `planificacionEncontrada = perteneceA(unProyecto)`.
  4. `:ModaLink` valida los datos: `datosValidos = nombre != null && nombre != "" && duracion > 0`.
  5. **Bloque Bucle (`loop`)** [Mientras haya actividades de la planificación && `nombreDuplicado == false`]:
     - `:ModaLink` solicita a `actividades`: `unaActividad = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaActividad`: `nombreDuplicado = perteneceA(unaPlanificacion) && tieneNombre(nombre)`.
  6. **Bloque Alternativo (`alt`)** [!planificacionEncontrada || !datosValidos || nombreDuplicado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Planificación, datos o nombre duplicado inválido")`.
  7. **Bloque Alternativo (`alt`)** [planificacionEncontrada && datosValidos && !nombreDuplicado]:
     - `:ModaLink` crea la actividad: `unaActividad = registrar(unaPlanificacion, nombre, descripcion, duracion, PENDIENTE)`.
     - `actividades.agregar(unaActividad)`.
     - `:ModaLink` retorna `unaActividad` al actor.

---

### DSD agregarPredecesora(unaActividad, unaPredecesora) | UC-49: Crear actividad a planificación

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `dependencias` (Colección de dependencias entre actividades)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unaPredecesora : Actividad` (Instancia de la entidad Actividad)
  - `unaDependencia : DependenciaActividades` (Instancia de la entidad DependenciaActividades)
- **Variables locales / Notas:**
  - `boolean esMismaActividad`
  - `boolean generaCiclo`
- **Flujo de interacción:**
  1. El actor envía `predecesoras = agregarPredecesora(unaActividad, unaPredecesora)` a `:ModaLink`.
  2. Se inicializan las variables locales `esMismaActividad` y `generaCiclo`.
  3. `:ModaLink` evalúa: `esMismaActividad = unaActividad == unaPredecesora`.
  4. `:ModaLink` consulta a `unaActividad`: `generaCiclo = generaCicloCon(unaPredecesora)`.
  5. **Bloque Alternativo (`alt`)** [esMismaActividad || generaCiclo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("La predecesora es inválida o genera un ciclo")`.
  6. **Bloque Alternativo (`alt`)** [!esMismaActividad && !generaCiclo]:
     - `:ModaLink` crea la dependencia: `unaDependencia = registrar(unaActividad, unaPredecesora)`.
     - `dependencias.agregar(unaDependencia)`.
     - `:ModaLink` retorna a `unaActividad`: `predecesoras[]`.

---

### DSD buscarUbicacion(idUbicacion) | UC-49: Crear actividad a planificación

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `ubicaciones` (Colección de ubicaciones)
  - `unaUbicacion : Ubicacion` (Instancia de la entidad Ubicacion)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unaUbicacion = buscarUbicacion(idUbicacion)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya ubicaciones && `retorno == false`]:
     - `:ModaLink` solicita a `ubicaciones`: `unaUbicacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaUbicacion`: `retorno = tieneId(idUbicacion)`.
  4. **Bloque Alternativo (`alt`)** [!retorno]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Ubicación inexistente")`.
  5. **Bloque Alternativo (`alt`)** [retorno]:
     - `:ModaLink` retorna `unaUbicacion` al actor.

---

### DSD agregarUbicacion(unaActividad, unaUbicacion) | UC-49: Crear actividad a planificación

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unaUbicacion : Ubicacion` (Instancia de la entidad Ubicacion)
- **Flujo de interacción:**
  1. El actor envía `agregarUbicacion(unaActividad, unaUbicacion)` a `:ModaLink`.
  2. `:ModaLink` envía a `unaActividad`: `setUbicacion(unaUbicacion)`.
  3. `:ModaLink` retorna al actor: `ubicacionAgregada(unaActividad)`.

---

### DSD agregarRequerimiento(unaActividad, unaProfesion, caracteristicas[], habilidades[]) | UC-49: Crear actividad a planificación

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unaProfesion : Profesion` (Instancia de la entidad Profesion)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
- **Variables locales / Notas:**
  - `boolean profesionValida`
- **Flujo de interacción:**
  1. El actor envía `agregarRequerimiento(unaActividad, unaProfesion, caracteristicas[], habilidades[])` a `:ModaLink`.
  2. Se inicializa la variable local `profesionValida`.
  3. `:ModaLink` consulta a `unaProfesion`: `profesionValida = estaActiva()`.
  4. **Bloque Alternativo (`alt`)** [!profesionValida]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("La profesión no se encuentra activa")`.
  5. **Bloque Alternativo (`alt`)** [profesionValida]:
     - `:ModaLink` crea el requerimiento: `unRequerimiento = registrar(unaActividad, unaProfesion, caracteristicas[], habilidades[])`.
     - `requerimientosActividad.agregar(unRequerimiento)`.
     - `:ModaLink` retorna al actor: `requerimientoAgregado(unRequerimiento)`.

---

### DSD modificarActividad(unaActividad, nombre, descripcion, duracionEstimada, requerimiento[], predecesoras[], ubicacion, estado) | UC-50: Modificar actividad de planificación

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `dependencias` (Colección de dependencias entre actividades)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
- **Variables locales / Notas:**
  - `boolean datosValidos`
  - `boolean requerimientoProtegido`
- **Flujo de interacción:**
  1. El actor envía `unaActividad = modificarActividad(unaActividad, nombre, descripcion, duracionEstimada, requerimiento[], predecesoras[], ubicacion, estado)` a `:ModaLink`.
  2. Se inicializan las variables locales `datosValidos` y `requerimientoProtegido`.
  3. `:ModaLink` valida los datos: `datosValidos = nombre != null && nombre != "" && duracionEstimada > 0`.
  4. **Bloque Alternativo (`alt`)** [!datosValidos]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Datos de actividad inválidos")`.
  5. **Bloque Alternativo (`alt`)** [datosValidos]:
     - `:ModaLink` envía a `unaActividad`: `setNombre(nombre)`.
     - `:ModaLink` envía a `unaActividad`: `setDescripcion(descripcion)`.
     - `:ModaLink` envía a `unaActividad`: `setDuracion(duracionEstimada)`.
     - `:ModaLink` envía a `unaActividad`: `setUbicacion(ubicacion)`.
     - `:ModaLink` envía a `unaActividad`: `setEstado(estado)`.
     - **Bloque Bucle (`loop`)** [Mientras haya requerimientos por actualizar]:
       - `:ModaLink` solicita a `requerimientosActividad`: `unRequerimiento = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unRequerimiento`: `requerimientoProtegido = estaProtegido()`.
       - **Bloque Alternativo (`alt`)** [requerimientoProtegido]:
         - `:ModaLink` conserva `unRequerimiento` sin cambios.
       - **Bloque Alternativo (`alt`)** [!requerimientoProtegido]:
         - `:ModaLink` envía a `unRequerimiento`: `actualizar(datos)`.
     - `:ModaLink` envía a `unaActividad`: `setPredecesoras(predecesoras[])`.
     - `:ModaLink` retorna `unaActividad` al actor.

---

### DSD eliminarActividad(unaActividad, motivo) | UC-51: Eliminar actividad de planificación

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `actividades` (Colección de actividades)
  - `asignaciones` (Colección de asignaciones de actividad)
  - `dependencias` (Colección de dependencias entre actividades)
  - `notificaciones` (Colección de notificaciones)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unaAsignacion : AsignacionActividad` (Instancia de la entidad AsignacionActividad)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
- **Variables locales / Notas:**
  - `boolean estadoValido`
  - `boolean motivoValido`
  - `boolean tieneMiembrosAsignados = false`
  - `String decision`
- **Flujo de interacción:**
  1. El actor envía `eliminarActividad(unaActividad, motivo)` a `:ModaLink`.
  2. Se inicializan las variables locales `estadoValido`, `motivoValido`, `tieneMiembrosAsignados` y `decision`.
  3. `:ModaLink` consulta a `unaActividad`: `estadoValido = !estaEnEstado(FINALIZADA)`.
  4. `:ModaLink` valida el motivo: `motivoValido = motivo != null && motivo != ""`.
  5. **Bloque Alternativo (`alt`)** [!estadoValido || !motivoValido]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Estado o motivo inválido")`.
  6. **Bloque Alternativo (`alt`)** [estadoValido && motivoValido]:
     - **Bloque Bucle (`loop`)** [Mientras haya asignaciones de la actividad]:
       - `:ModaLink` solicita a `asignaciones`: `unaAsignacion = obtenerSiguiente()`.
       - **Bloque Alternativo (`alt`)** [unaAsignacion.perteneceA(unaActividad)]:
         - `tieneMiembrosAsignados = true`.
         - `:ModaLink` solicita al actor: `decision = elegirDecisionIntegrante()`.
         - **Bloque Alternativo (`alt`)** [decision == "ELIMINAR"]:
           - `:ModaLink` ejecuta la baja del integrante: `eliminarIntegrante(unaActividad.obtenerProyecto(), unaAsignacion.obtenerPerfil(), "Eliminación de actividad")`.
         - **Bloque Alternativo (`alt`)** [decision == "GUARDAR"]:
           - `:ModaLink` ejecuta: `guardarMiembro(unaAsignacion.obtenerPerfil(), unaActividad)`.
     - `:ModaLink` reconecta predecesoras y sucesoras: `reconectarDependencias(unaActividad)`.
     - `:ModaLink` solicita a `actividades`: `eliminar(unaActividad)`.
     - `:ModaLink` agrega a `notificaciones`: `notificarActividadEliminada(unaActividad, motivo)`.
     - `:ModaLink` retorna al actor: `operacionExitosa()`.

---

### DSD guardarMiembro(unPerfil, unaActividad) | UC-51: Eliminar actividad de planificación

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `asignaciones` (Colección de asignaciones de actividad)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unaAsignacion : AsignacionActividad` (Instancia de la entidad AsignacionActividad)
- **Variables locales / Notas:**
  - `boolean asignacionEncontrada = false`
- **Flujo de interacción:**
  1. El actor envía `guardarMiembro(unPerfil, unaActividad)` a `:ModaLink`.
  2. Se inicializa la variable local `asignacionEncontrada`.
  3. **Bloque Bucle (`loop`)** [Mientras haya asignaciones && `asignacionEncontrada == false`]:
     - `:ModaLink` solicita a `asignaciones`: `unaAsignacion = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaAsignacion`: `asignacionEncontrada = correspondeA(unPerfil, unaActividad)`.
  4. **Bloque Alternativo (`alt`)** [!asignacionEncontrada]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El miembro no está asignado a la actividad")`.
  5. **Bloque Alternativo (`alt`)** [asignacionEncontrada]:
     - `:ModaLink` solicita a `asignaciones`: `eliminar(unaAsignacion)`.
     - `:ModaLink` retorna al actor: `miembroGuardadoEnProyecto(unPerfil)`.

---

### DSD buscarActividades(nombre, descripcion, requerimiento, duracion, estado) | UC-52: Buscar actividades

- **Actores:** Director de proyecto / Miembro de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `actividades` (Colección de actividades)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
- **Variables locales / Notas:**
  - `Lista actividadesFiltradas = nueva Lista()`
  - `boolean sePuedeAgregar`
- **Flujo de interacción:**
  1. El actor envía `actividades = buscarActividades(nombre, descripcion, requerimiento, duracion, estado)` a `:ModaLink`.
  2. Se inicializan las variables locales `actividadesFiltradas` y `sePuedeAgregar`.
  3. **Bloque Bucle (`loop`)** [Mientras haya actividades]:
     - `:ModaLink` solicita a `actividades`: `unaActividad = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaActividad`: `sePuedeAgregar = cumpleFiltros(nombre, descripcion, requerimiento, duracion, estado)`.
     - **Bloque Alternativo (`alt`)** [sePuedeAgregar]:
       - `actividadesFiltradas.agregar(unaActividad)`.
  4. `:ModaLink` retorna `actividadesFiltradas` al actor.

---

### DSD seleccionarActividad(idActividad, actividades) | UC-52: Buscar actividades

- **Actores:** Director de proyecto / Miembro de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `actividades` (Colección de actividades)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unaActividad = seleccionarActividad(idActividad, actividades)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya actividades && `retorno == false`]:
     - `:ModaLink` solicita a `actividades`: `unaActividad = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unaActividad`: `retorno = tieneId(idActividad)`.
  4. `:ModaLink` retorna `unaActividad` al actor.

---

### DSD buscarUsuario(unPerfil) | UC-70: Asignar actividad a miembro

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `usuarios` (Colección de usuarios)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
- **Variables locales / Notas:**
  - `boolean retorno = false`
- **Flujo de interacción:**
  1. El actor envía `unUsuario = buscarUsuario(unPerfil)` a `:ModaLink`.
  2. Se inicializa la variable local `retorno = false`.
  3. **Bloque Bucle (`loop`)** [Mientras haya usuarios && `retorno == false`]:
     - `:ModaLink` solicita a `usuarios`: `unUsuario = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unUsuario`: `retorno = tienePerfil(unPerfil)`.
  4. `:ModaLink` retorna `unUsuario` al actor.

---

### DSD verificarDisponibilidad(unUsuario, unaActividad) | UC-70: Asignar actividad a miembro

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `agendas` (Colección de agendas)
  - `jornadas` (Colección de jornadas de agenda)
  - `bloqueos` (Colección de bloqueos de agenda)
  - `asignaciones` (Colección de asignaciones de actividad)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unaAgenda : Agenda` (Instancia de la entidad Agenda)
  - `unaJornada : JornadaAgenda` (Instancia de la entidad JornadaAgenda)
  - `unBloqueo : BloqueoAgenda` (Instancia de la entidad BloqueoAgenda)
  - `unaAsignacion : AsignacionActividad` (Instancia de la entidad AsignacionActividad)
- **Variables locales / Notas:**
  - `Rango rangoFactible`
  - `boolean disponible`
  - `boolean provisional`
  - `boolean seSuperpone`
- **Flujo de interacción:**
  1. El actor envía `resultado = verificarDisponibilidad(unUsuario, unaActividad)` a `:ModaLink`.
  2. Se inicializan las variables locales `rangoFactible`, `disponible`, `provisional` y `seSuperpone`.
  3. `:ModaLink` solicita a `agendas`: `unaAgenda = buscarPorUsuario(unUsuario)`.
  4. `:ModaLink` solicita a `unaActividad`: `rangoFactible = obtenerRangoFactible()`.
  5. `:ModaLink` solicita a `unaActividad`: `provisional = !tieneFechaInicioConfirmada()`.
  6. `:ModaLink` solicita a `unaAgenda`: `disponible = tieneHuecoContinuo(rangoFactible, unaActividad.obtenerDuracion())`.
  7. **Bloque Opcional (`opt`)** [disponible]:
     - **Bloque Bucle (`loop`)** [Mientras haya bloqueos de la agenda && `seSuperpone == false`]:
       - `:ModaLink` solicita a `bloqueos`: `unBloqueo = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unBloqueo`: `seSuperpone = seSuperponeCon(rangoFactible)`.
     - **Bloque Bucle (`loop`)** [Mientras haya asignaciones del usuario && `seSuperpone == false`]:
       - `:ModaLink` solicita a `asignaciones`: `unaAsignacion = obtenerSiguiente()`.
       - `:ModaLink` consulta a `unaAsignacion`: `seSuperpone = seSuperponeCon(rangoFactible)`.
     - **Bloque Alternativo (`alt`)** [seSuperpone]:
       - `disponible = false`.
  8. `:ModaLink` retorna al actor: `ResultadoDisponibilidad(disponible, provisional, rangoFactible)`.

---

### DSD asignarActividad(unPerfil, unaActividad) | UC-70: Asignar actividad a miembro

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `miembros` (Colección de miembros de proyecto)
  - `asignaciones` (Colección de asignaciones de actividad)
  - `requerimientosActividad` (Colección de requerimientos de actividad)
  - `notificaciones` (Colección de notificaciones)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unUsuario : Usuario` (Instancia de la entidad Usuario)
  - `unaActividad : Actividad` (Instancia de la entidad Actividad)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unaAsignacion : AsignacionActividad` (Instancia de la entidad AsignacionActividad)
  - `unRequerimiento : RequerimientoActividad` (Instancia de la entidad RequerimientoActividad)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
- **Variables locales / Notas:**
  - `boolean miembroEncontrado = false`
  - `boolean disponible`
  - `boolean yaAsignado = false`
  - `boolean requerimientoEncontrado = false`
- **Flujo de interacción:**
  1. El actor envía `unaAsignacion = asignarActividad(unPerfil, unaActividad)` a `:ModaLink`.
  2. Se inicializan las variables locales `miembroEncontrado`, `disponible`, `yaAsignado` y `requerimientoEncontrado`.
  3. `:ModaLink` solicita a `unaActividad`: `unProyecto = obtenerProyecto()`.
  4. **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto && `miembroEncontrado == false`]:
     - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unMiembro`: `miembroEncontrado = correspondeA(unProyecto, unPerfil) && estaActivo()`.
  5. **Bloque Alternativo (`alt`)** [!miembroEncontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil no es miembro activo del proyecto")`.
  6. **Bloque Alternativo (`alt`)** [miembroEncontrado]:
     - `:ModaLink` solicita a `unPerfil`: `unUsuario = obtenerUsuario()`.
     - `:ModaLink` consulta a `unUsuario`: `disponible = verificarDisponibilidad(unaActividad)`.
     - **Bloque Alternativo (`alt`)** [!disponible]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("El miembro no tiene disponibilidad suficiente")`.
     - **Bloque Alternativo (`alt`)** [disponible]:
       - **Bloque Bucle (`loop`)** [Mientras haya asignaciones de la actividad && `yaAsignado == false`]:
         - `:ModaLink` solicita a `asignaciones`: `unaAsignacion = obtenerSiguiente()`.
         - `:ModaLink` consulta a `unaAsignacion`: `yaAsignado = correspondeA(unMiembro, unaActividad)`.
       - **Bloque Alternativo (`alt`)** [yaAsignado]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("El miembro ya está asignado a la actividad")`.
       - **Bloque Alternativo (`alt`)** [!yaAsignado]:
         - **Bloque Bucle (`loop`)** [Mientras haya requerimientos de la actividad && `requerimientoEncontrado == false`]:
           - `:ModaLink` solicita a `requerimientosActividad`: `unRequerimiento = obtenerSiguiente()`.
           - `:ModaLink` consulta a `unRequerimiento`: `requerimientoEncontrado = correspondeA(unaActividad) && tieneProfesion(unPerfil) && tieneCupoDisponible()`.
         - **Bloque Alternativo (`alt`)** [requerimientoEncontrado]:
           - `:ModaLink` crea la asignación: `unaAsignacion = registrar(unMiembro, unaActividad, unRequerimiento)`.
           - `asignaciones.agregar(unaAsignacion)`.
           - `:ModaLink` actualiza el requerimiento: `unRequerimiento.actualizarEstado()`.
         - **Bloque Alternativo (`alt`)** [!requerimientoEncontrado]:
           - `:ModaLink` crea la asignación sin requerimiento: `unaAsignacion = registrar(unMiembro, unaActividad, null)`.
           - `asignaciones.agregar(unaAsignacion)`.
         - `:ModaLink` agrega a `notificaciones`: `notificarAsignacion(unMiembro, unaActividad)`.
         - `:ModaLink` retorna `unaAsignacion` al actor.

---

## 8. Contratos

### DSD obtenerOpcionesContrato(idProyecto) | UC-39: Generar contrato

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `proyectos` (Colección de proyectos)
  - `perfiles` (Colección de perfiles)
  - `miembros` (Colección de miembros de proyecto)
  - `tiposAcuerdoContrato` (Colección de tipos de acuerdo de contrato)
  - `contratos` (Colección de contratos)
  - `planificaciones` (Colección de planificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unEmisor : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unTipoAcuerdo : TipoAcuerdoContrato` (Instancia de la entidad TipoAcuerdoContrato)
  - `unContrato : Contrato` (Instancia de la entidad Contrato)
  - `unaPlanificacion : Planificacion` (Instancia de la entidad Planificacion)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
  - `boolean proyectoEncontrado = false`
  - `boolean proyectoApto`
  - `boolean esDirector`
  - `boolean tipoActivo`
  - `boolean perteneceAlProyecto`
  - `Lista tiposActivos = nueva Lista()`
  - `Lista miembrosActivos = nueva Lista()`
  - `Lista contratosActivos = nueva Lista()`
  - `LocalDate fechaEntrega`
- **Flujo de interacción:**
  1. El actor envía `opciones = obtenerOpcionesContrato(idProyecto)` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilActivo`, `proyectoEncontrado`, `proyectoApto`, `esDirector`, `tipoActivo`, `perteneceAlProyecto`, `tiposActivos`, `miembrosActivos` y `contratosActivos`.
  3. `:ModaLink` solicita a `perfiles`: `unPerfil = buscarPerfilActivo(perfilActivo)`.
  4. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  5. **Bloque Alternativo (`alt`)** [!perfilActivo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil activo no existe o no está activo")`.
  6. **Bloque Bucle (`loop`)** [Mientras haya proyectos && `proyectoEncontrado == false`]:
     - `:ModaLink` solicita a `proyectos`: `unProyecto = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unProyecto`: `proyectoEncontrado = tieneId(idProyecto)`.
  7. **Bloque Alternativo (`alt`)** [!proyectoEncontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Proyecto inexistente")`.
  8. **Bloque Alternativo (`alt`)** [proyectoEncontrado]:
     - `:ModaLink` consulta a `unProyecto`: `proyectoApto = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
     - **Bloque Alternativo (`alt`)** [!proyectoApto]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Solo se puede generar un contrato para un proyecto PUBLICADO o CONFIRMADO")`.
     - **Bloque Alternativo (`alt`)** [proyectoApto]:
       - `:ModaLink` consulta a `unProyecto`: `esDirector = tieneDirectorActivo(unPerfil)`.
       - **Bloque Alternativo (`alt`)** [!esDirector]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil activo no es director del proyecto")`.
       - **Bloque Alternativo (`alt`)** [esDirector]:
         - `:ModaLink` solicita a `unProyecto`: `unEmisor = obtenerMiembroActivo(unPerfil)`.
         - **Bloque Bucle (`loop`)** [Mientras haya planificaciones]:
           - `:ModaLink` solicita a `planificaciones`: `unaPlanificacion = obtenerSiguiente()`.
           - **Bloque Alternativo (`alt`)** [unaPlanificacion.perteneceA(unProyecto)]:
             - `fechaEntrega = unaPlanificacion.obtenerFechaEntrega()`.
         - **Bloque Bucle (`loop`)** [Mientras haya tipos de acuerdo]:
           - `:ModaLink` solicita a `tiposAcuerdoContrato`: `unTipoAcuerdo = obtenerSiguiente()`.
           - `:ModaLink` consulta a `unTipoAcuerdo`: `tipoActivo = estaActivo()`.
           - **Bloque Alternativo (`alt`)** [tipoActivo]:
             - `tiposActivos.agregar(unTipoAcuerdo)`.
         - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto]:
           - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
           - `:ModaLink` consulta a `unMiembro`: `perteneceAlProyecto = correspondeA(unProyecto) && estaActivo()`.
           - **Bloque Alternativo (`alt`)** [perteneceAlProyecto]:
             - `miembrosActivos.agregar(unMiembro)`.
         - **Bloque Bucle (`loop`)** [Mientras haya contratos del proyecto]:
           - `:ModaLink` solicita a `contratos`: `unContrato = obtenerSiguiente()`.
           - **Bloque Alternativo (`alt`)** [unContrato.perteneceA(unProyecto) && unContrato.estaActivo()]:
             - `contratosActivos.agregar(unContrato)`.
         - `:ModaLink` retorna al actor: `OpcionesGeneracionContratoResponse(unProyecto, fechaEntrega, unEmisor, tiposActivos, miembrosActivos, contratosActivos)`.

---

### DSD previsualizarContrato(idProyecto, idTipoAcuerdo, idsMiembro[], clausulasAdicionales[]) | UC-39: Generar contrato

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `proyectos` (Colección de proyectos)
  - `perfiles` (Colección de perfiles)
  - `miembros` (Colección de miembros de proyecto)
  - `tiposAcuerdoContrato` (Colección de tipos de acuerdo de contrato)
  - `contratos` (Colección de contratos)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unEmisor : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unTipoAcuerdo : TipoAcuerdoContrato` (Instancia de la entidad TipoAcuerdoContrato)
  - `unContrato : Contrato` (Instancia de la entidad Contrato)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
  - `boolean proyectoEncontrado = false`
  - `boolean proyectoApto`
  - `boolean esDirector`
  - `boolean tipoEncontrado = false`
  - `boolean sinDestinatarios`
  - `boolean destinatarioValido`
  - `boolean tieneContratoActivo`
  - `Lista destinatarios = nueva Lista()`
  - `Lista conflictos = nueva Lista()`
  - `String texto`
- **Flujo de interacción:**
  1. El actor envía `previsualizacion = previsualizarContrato(idProyecto, idTipoAcuerdo, idsMiembro[], clausulasAdicionales[])` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilActivo`, `proyectoEncontrado`, `proyectoApto`, `esDirector`, `tipoEncontrado`, `sinDestinatarios`, `destinatarioValido`, `tieneContratoActivo`, `destinatarios` y `conflictos`.
  3. `:ModaLink` solicita a `perfiles`: `unPerfil = buscarPerfilActivo(perfilActivo)`.
  4. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  5. **Bloque Alternativo (`alt`)** [!perfilActivo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil activo no existe o no está activo")`.
  6. **Bloque Bucle (`loop`)** [Mientras haya proyectos && `proyectoEncontrado == false`]:
     - `:ModaLink` solicita a `proyectos`: `unProyecto = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unProyecto`: `proyectoEncontrado = tieneId(idProyecto)`.
  7. **Bloque Alternativo (`alt`)** [!proyectoEncontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Proyecto inexistente")`.
  8. **Bloque Alternativo (`alt`)** [proyectoEncontrado]:
     - `:ModaLink` consulta a `unProyecto`: `proyectoApto = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
     - **Bloque Alternativo (`alt`)** [!proyectoApto]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Solo se puede generar un contrato para un proyecto PUBLICADO o CONFIRMADO")`.
     - **Bloque Alternativo (`alt`)** [proyectoApto]:
       - `:ModaLink` consulta a `unProyecto`: `esDirector = tieneDirectorActivo(unPerfil)`.
       - **Bloque Alternativo (`alt`)** [!esDirector]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil activo no es director del proyecto")`.
       - **Bloque Alternativo (`alt`)** [esDirector]:
         - **Bloque Bucle (`loop`)** [Mientras haya tipos de acuerdo && `tipoEncontrado == false`]:
           - `:ModaLink` solicita a `tiposAcuerdoContrato`: `unTipoAcuerdo = obtenerSiguiente()`.
           - `:ModaLink` consulta a `unTipoAcuerdo`: `tipoEncontrado = tieneId(idTipoAcuerdo) && estaActivo()`.
         - **Bloque Alternativo (`alt`)** [!tipoEncontrado]:
           - `:ModaLink` retorna una excepción al actor: `Excepcion("Tipo de acuerdo inexistente o inactivo")`.
         - **Bloque Alternativo (`alt`)** [tipoEncontrado]:
           - `:ModaLink` evalúa: `sinDestinatarios = idsMiembro == null || idsMiembro.isEmpty()`.
           - **Bloque Alternativo (`alt`)** [sinDestinatarios]:
             - `:ModaLink` retorna una excepción al actor: `Excepcion("Debe seleccionar al menos un miembro destinatario")`.
           - **Bloque Alternativo (`alt`)** [!sinDestinatarios]:
             - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto]:
               - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
               - **Bloque Alternativo (`alt`)** [idsMiembro.contiene(unMiembro.getIdMiembro())]:
                 - `:ModaLink` consulta a `unMiembro`: `destinatarioValido = correspondeA(unProyecto) && estaActivo()`.
                 - **Bloque Alternativo (`alt`)** [destinatarioValido]:
                   - `destinatarios.agregar(unMiembro)`.
                 - **Bloque Alternativo (`alt`)** [!destinatarioValido]:
                   - `conflictos.agregar("DESTINATARIO_INVALIDO", unMiembro.getIdMiembro())`.
             - **Bloque Bucle (`loop`)** [Mientras haya contratos del proyecto]:
               - `:ModaLink` solicita a `contratos`: `unContrato = obtenerSiguiente()`.
               - `:ModaLink` consulta a `unContrato`: `tieneContratoActivo = tieneContratoActivoDe(unProyecto, unTipoAcuerdo, destinatarios)`.
               - **Bloque Alternativo (`alt`)** [tieneContratoActivo]:
                 - `conflictos.agregar("CONTRATO_DUPLICADO_ACTIVO", unContrato.obtenerMiembroDuplicado())`.
             - `:ModaLink` solicita a `unProyecto`: `unEmisor = obtenerMiembroActivo(unPerfil)`.
             - `:ModaLink` solicita a `unTipoAcuerdo`: `texto = generarTexto(unProyecto, unEmisor, destinatarios, clausulasAdicionales)`.
             - `:ModaLink` retorna al actor: `PrevisualizacionContratoResponse(texto, conflictos.isEmpty(), conflictos)`.

---

### DSD generarContrato(idProyecto, idTipoAcuerdo, idsMiembro[], clausulasAdicionales[]) | UC-39: Generar contrato

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `proyectos` (Colección de proyectos)
  - `perfiles` (Colección de perfiles)
  - `miembros` (Colección de miembros de proyecto)
  - `tiposAcuerdoContrato` (Colección de tipos de acuerdo de contrato)
  - `contratos` (Colección de contratos)
  - `firmasContrato` (Colección de firmas de contrato)
  - `eventosContrato` (Colección de eventos de contrato)
  - `notificaciones` (Colección de notificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unEmisor : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unTipoAcuerdo : TipoAcuerdoContrato` (Instancia de la entidad TipoAcuerdoContrato)
  - `unContrato : Contrato` (Instancia de la entidad Contrato)
  - `unaFirma : FirmaContrato` (Instancia de la entidad FirmaContrato)
  - `unDestinatario : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
  - `boolean proyectoEncontrado = false`
  - `boolean proyectoApto`
  - `boolean esDirector`
  - `boolean tipoEncontrado = false`
  - `boolean sinDestinatarios`
  - `boolean destinatarioValido`
  - `boolean tieneContratoActivo`
  - `Lista destinatarios = nueva Lista()`
  - `Lista conflictos = nueva Lista()`
  - `Lista firmas = nueva Lista()`
  - `String texto`
  - `String nombre`
  - `String descripcion`
- **Flujo de interacción:**
  1. El actor envía `unContrato = generarContrato(idProyecto, idTipoAcuerdo, idsMiembro[], clausulasAdicionales[])` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilActivo`, `proyectoEncontrado`, `proyectoApto`, `esDirector`, `tipoEncontrado`, `sinDestinatarios`, `destinatarioValido`, `tieneContratoActivo`, `destinatarios`, `conflictos` y `firmas`.
  3. `:ModaLink` solicita a `perfiles`: `unPerfil = buscarPerfilActivo(perfilActivo)`.
  4. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  5. **Bloque Alternativo (`alt`)** [!perfilActivo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil activo no existe o no está activo")`.
  6. **Bloque Bucle (`loop`)** [Mientras haya proyectos && `proyectoEncontrado == false`]:
     - `:ModaLink` solicita a `proyectos`: `unProyecto = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unProyecto`: `proyectoEncontrado = tieneId(idProyecto)`.
  7. **Bloque Alternativo (`alt`)** [!proyectoEncontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Proyecto inexistente")`.
  8. **Bloque Alternativo (`alt`)** [proyectoEncontrado]:
     - `:ModaLink` consulta a `unProyecto`: `proyectoApto = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
     - **Bloque Alternativo (`alt`)** [!proyectoApto]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Solo se puede generar un contrato para un proyecto PUBLICADO o CONFIRMADO")`.
     - **Bloque Alternativo (`alt`)** [proyectoApto]:
       - `:ModaLink` consulta a `unProyecto`: `esDirector = tieneDirectorActivo(unPerfil)`.
       - **Bloque Alternativo (`alt`)** [!esDirector]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil activo no es director del proyecto")`.
       - **Bloque Alternativo (`alt`)** [esDirector]:
         - **Bloque Bucle (`loop`)** [Mientras haya tipos de acuerdo && `tipoEncontrado == false`]:
           - `:ModaLink` solicita a `tiposAcuerdoContrato`: `unTipoAcuerdo = obtenerSiguiente()`.
           - `:ModaLink` consulta a `unTipoAcuerdo`: `tipoEncontrado = tieneId(idTipoAcuerdo) && estaActivo()`.
         - **Bloque Alternativo (`alt`)** [!tipoEncontrado]:
           - `:ModaLink` retorna una excepción al actor: `Excepcion("Tipo de acuerdo inexistente o inactivo")`.
         - **Bloque Alternativo (`alt`)** [tipoEncontrado]:
           - `:ModaLink` evalúa: `sinDestinatarios = idsMiembro == null || idsMiembro.isEmpty()`.
           - **Bloque Alternativo (`alt`)** [sinDestinatarios]:
             - `:ModaLink` retorna una excepción al actor: `Excepcion("Debe seleccionar al menos un miembro destinatario")`.
           - **Bloque Alternativo (`alt`)** [!sinDestinatarios]:
             - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto]:
               - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
               - **Bloque Alternativo (`alt`)** [idsMiembro.contiene(unMiembro.getIdMiembro())]:
                 - `:ModaLink` consulta a `unMiembro`: `destinatarioValido = correspondeA(unProyecto) && estaActivo()`.
                 - **Bloque Alternativo (`alt`)** [destinatarioValido]:
                   - `destinatarios.agregar(unMiembro)`.
                 - **Bloque Alternativo (`alt`)** [!destinatarioValido]:
                   - `conflictos.agregar("DESTINATARIO_INVALIDO", unMiembro.getIdMiembro())`.
             - **Bloque Bucle (`loop`)** [Mientras haya contratos del proyecto]:
               - `:ModaLink` solicita a `contratos`: `unContrato = obtenerSiguiente()`.
               - `:ModaLink` consulta a `unContrato`: `tieneContratoActivo = tieneContratoActivoDe(unProyecto, unTipoAcuerdo, destinatarios)`.
               - **Bloque Alternativo (`alt`)** [tieneContratoActivo]:
                 - `conflictos.agregar("CONTRATO_DUPLICADO_ACTIVO", unContrato.obtenerMiembroDuplicado())`.
             - **Bloque Alternativo (`alt`)** [!conflictos.isEmpty()]:
               - `:ModaLink` retorna una excepción al actor: `Excepcion("Existen conflictos de destinatarios o contratos activos")`.
             - **Bloque Alternativo (`alt`)** [conflictos.isEmpty()]:
               - `:ModaLink` solicita a `unTipoAcuerdo`: `texto = generarTexto(unProyecto, unPerfil, destinatarios, clausulasAdicionales)`.
               - `:ModaLink` solicita a `unTipoAcuerdo`: `nombre = generarNombre(unProyecto)`.
               - `:ModaLink` solicita a `unProyecto`: `descripcion = generarDescripcion()`.
               - `:ModaLink` solicita a `unProyecto`: `unEmisor = obtenerMiembroActivo(unPerfil)`.
               - `:ModaLink` crea el contrato: `unContrato = registrar(unProyecto, unTipoAcuerdo, unEmisor, nombre, descripcion, texto, BORRADOR, 1)`.
               - `contratos.agregar(unContrato)`.
               - **Bloque Bucle (`loop`)** [Mientras haya destinatarios]:
                 - `:ModaLink` solicita a `destinatarios`: `unDestinatario = obtenerSiguiente()`.
                 - `:ModaLink` crea la firma: `unaFirma = registrar(unContrato, unDestinatario, DESTINATARIO, PENDIENTE)`.
                 - `firmasContrato.agregar(unaFirma)`.
                 - `firmas.agregar(unaFirma)`.
               - `:ModaLink` crea la firma del director: `unaFirma = registrar(unContrato, unEmisor, DIRECTOR, PENDIENTE)`.
               - `firmasContrato.agregar(unaFirma)`.
               - `firmas.agregar(unaFirma)`.
               - `:ModaLink` envía a `unContrato`: `setEstado(PENDIENTE_FIRMA)`.
               - `:ModaLink` envía a `unContrato`: `setFechaEmision(ahora)`.
               - `eventosContrato.agregar(registrar(unContrato, EMITIDO, ahora))`.
               - **Bloque Bucle (`loop`)** [Mientras haya destinatarios]:
                 - `:ModaLink` solicita a `destinatarios`: `unDestinatario = obtenerSiguiente()`.
                 - `notificaciones.agregar(notificarContratoPendienteFirma(unDestinatario, unContrato))`.
               - `:ModaLink` retorna al actor: `ContratoGeneradoResponse(unContrato, firmas)`.

---

### DSD previsualizarModificacionContrato(idProyecto, idContrato, idTipoAcuerdo, idsMiembro[], texto, clausulasAdicionales[], nombre, descripcion) | UC-53: Modificar contrato

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `proyectos` (Colección de proyectos)
  - `perfiles` (Colección de perfiles)
  - `miembros` (Colección de miembros de proyecto)
  - `tiposAcuerdoContrato` (Colección de tipos de acuerdo de contrato)
  - `contratos` (Colección de contratos)
  - `firmasContrato` (Colección de firmas de contrato)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unTipoAcuerdo : TipoAcuerdoContrato` (Instancia de la entidad TipoAcuerdoContrato)
  - `unContrato : Contrato` (Instancia de la entidad Contrato)
  - `otroContrato : Contrato` (Instancia de la entidad Contrato)
  - `unaFirma : FirmaContrato` (Instancia de la entidad FirmaContrato)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
  - `boolean proyectoEncontrado = false`
  - `boolean proyectoApto`
  - `boolean esDirector`
  - `boolean contratoEncontrado = false`
  - `boolean contratoPendiente`
  - `boolean firmaRegistrada = false`
  - `boolean tipoEncontrado = false`
  - `boolean sinDestinatarios`
  - `boolean destinatarioValido`
  - `boolean tieneContratoActivo`
  - `Lista destinatarios = nueva Lista()`
  - `Lista conflictos = nueva Lista()`
  - `String textoFinal`
  - `String nombreFinal`
  - `String descripcionFinal`
- **Flujo de interacción:**
  1. El actor envía `previsualizacion = previsualizarModificacionContrato(idProyecto, idContrato, idTipoAcuerdo, idsMiembro[], texto, clausulasAdicionales[], nombre, descripcion)` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilActivo`, `proyectoEncontrado`, `proyectoApto`, `esDirector`, `contratoEncontrado`, `contratoPendiente`, `firmaRegistrada`, `tipoEncontrado`, `sinDestinatarios`, `destinatarioValido`, `tieneContratoActivo`, `destinatarios` y `conflictos`.
  3. `:ModaLink` solicita a `perfiles`: `unPerfil = buscarPerfilActivo(perfilActivo)`.
  4. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  5. **Bloque Alternativo (`alt`)** [!perfilActivo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil activo no existe o no está activo")`.
  6. **Bloque Bucle (`loop`)** [Mientras haya proyectos && `proyectoEncontrado == false`]:
     - `:ModaLink` solicita a `proyectos`: `unProyecto = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unProyecto`: `proyectoEncontrado = tieneId(idProyecto)`.
  7. **Bloque Alternativo (`alt`)** [!proyectoEncontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Proyecto inexistente")`.
  8. **Bloque Alternativo (`alt`)** [proyectoEncontrado]:
     - `:ModaLink` consulta a `unProyecto`: `proyectoApto = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
     - **Bloque Alternativo (`alt`)** [!proyectoApto]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Solo se puede modificar un contrato de un proyecto PUBLICADO o CONFIRMADO")`.
     - **Bloque Alternativo (`alt`)** [proyectoApto]:
       - `:ModaLink` consulta a `unProyecto`: `esDirector = tieneDirectorActivo(unPerfil)`.
       - **Bloque Alternativo (`alt`)** [!esDirector]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil activo no es director del proyecto")`.
       - **Bloque Alternativo (`alt`)** [esDirector]:
         - **Bloque Bucle (`loop`)** [Mientras haya contratos && `contratoEncontrado == false`]:
           - `:ModaLink` solicita a `contratos`: `unContrato = obtenerSiguiente()`.
           - `:ModaLink` consulta a `unContrato`: `contratoEncontrado = tieneId(idContrato) && perteneceA(unProyecto)`.
         - **Bloque Alternativo (`alt`)** [!contratoEncontrado]:
           - `:ModaLink` retorna una excepción al actor: `Excepcion("Contrato inexistente")`.
         - **Bloque Alternativo (`alt`)** [contratoEncontrado]:
           - `:ModaLink` consulta a `unContrato`: `contratoPendiente = estaEnEstado(PENDIENTE_FIRMA)`.
           - **Bloque Alternativo (`alt`)** [!contratoPendiente]:
             - `:ModaLink` retorna una excepción al actor: `Excepcion("Solo se puede modificar un contrato PENDIENTE_FIRMA")`.
           - **Bloque Alternativo (`alt`)** [contratoPendiente]:
             - **Bloque Bucle (`loop`)** [Mientras haya firmas del contrato && `firmaRegistrada == false`]:
               - `:ModaLink` solicita a `firmasContrato`: `unaFirma = obtenerSiguiente()`.
               - `:ModaLink` consulta a `unaFirma`: `firmaRegistrada = estaFirmada() || estaRechazada() || estaInvalidada()`.
             - **Bloque Alternativo (`alt`)** [firmaRegistrada]:
               - `:ModaLink` retorna una excepción al actor: `Excepcion("No se puede modificar un contrato con firmas registradas")`.
             - **Bloque Alternativo (`alt`)** [!firmaRegistrada]:
               - **Bloque Bucle (`loop`)** [Mientras haya tipos de acuerdo && `tipoEncontrado == false`]:
                 - `:ModaLink` solicita a `tiposAcuerdoContrato`: `unTipoAcuerdo = obtenerSiguiente()`.
                 - `:ModaLink` consulta a `unTipoAcuerdo`: `tipoEncontrado = tieneId(idTipoAcuerdo) && estaActivo()`.
               - **Bloque Alternativo (`alt`)** [!tipoEncontrado]:
                 - `:ModaLink` retorna una excepción al actor: `Excepcion("Tipo de acuerdo inexistente o inactivo")`.
               - **Bloque Alternativo (`alt`)** [tipoEncontrado]:
                 - `:ModaLink` evalúa: `sinDestinatarios = idsMiembro == null || idsMiembro.isEmpty()`.
                 - **Bloque Alternativo (`alt`)** [sinDestinatarios]:
                   - `:ModaLink` retorna una excepción al actor: `Excepcion("Debe seleccionar al menos un miembro destinatario")`.
                 - **Bloque Alternativo (`alt`)** [!sinDestinatarios]:
                   - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto]:
                     - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
                     - **Bloque Alternativo (`alt`)** [idsMiembro.contiene(unMiembro.getIdMiembro())]:
                       - `:ModaLink` consulta a `unMiembro`: `destinatarioValido = correspondeA(unProyecto) && estaActivo()`.
                       - **Bloque Alternativo (`alt`)** [destinatarioValido]:
                         - `destinatarios.agregar(unMiembro)`.
                       - **Bloque Alternativo (`alt`)** [!destinatarioValido]:
                         - `conflictos.agregar("DESTINATARIO_INVALIDO", unMiembro.getIdMiembro())`.
                   - **Bloque Bucle (`loop`)** [Mientras haya contratos del proyecto]:
                     - `:ModaLink` solicita a `contratos`: `otroContrato = obtenerSiguiente()`.
                     - `:ModaLink` consulta a `otroContrato`: `tieneContratoActivo = otroContrato.tieneContratoActivoDe(unProyecto, unTipoAcuerdo, destinatarios)`.
                     - **Bloque Alternativo (`alt`)** [tieneContratoActivo]:
                       - `conflictos.agregar("CONTRATO_DUPLICADO_ACTIVO", otroContrato.obtenerMiembroDuplicado())`.
                   - **Bloque Alternativo (`alt`)** [texto != null && texto != ""]:
                     - `:ModaLink` solicita a `unContrato`: `textoFinal = validarTexto(texto)`.
                   - **Bloque Alternativo (`alt`)** [texto == null || texto == ""]:
                     - `:ModaLink` solicita a `unTipoAcuerdo`: `textoFinal = generarTexto(unProyecto, unContrato.obtenerEmisor(), destinatarios, clausulasAdicionales)`.
                   - **Bloque Alternativo (`alt`)** [nombre != null && nombre != ""]:
                     - `nombreFinal = nombre`.
                   - **Bloque Alternativo (`alt`)** [nombre == null || nombre == ""]:
                     - `:ModaLink` solicita a `unContrato`: `nombreFinal = generarNombre(unTipoAcuerdo, unProyecto)`.
                   - **Bloque Alternativo (`alt`)** [descripcion != null]:
                     - `descripcionFinal = descripcion`.
                   - **Bloque Alternativo (`alt`)** [descripcion == null]:
                     - `:ModaLink` solicita a `unContrato`: `descripcionFinal = obtenerDescripcion()`.
                   - `:ModaLink` retorna al actor: `PrevisualizacionModificacionResponse(textoFinal, nombreFinal, descripcionFinal, conflictos)`.

---

### DSD modificarContrato(idProyecto, idContrato, idTipoAcuerdo, idsMiembro[], texto, clausulasAdicionales[], nombre, descripcion) | UC-53: Modificar contrato

- **Actores:** Director de proyecto
- **Objetos / Instancias participantes:**
  - `:ModaLink` (Controlador / Sistema)
  - `proyectos` (Colección de proyectos)
  - `perfiles` (Colección de perfiles)
  - `miembros` (Colección de miembros de proyecto)
  - `tiposAcuerdoContrato` (Colección de tipos de acuerdo de contrato)
  - `contratos` (Colección de contratos)
  - `firmasContrato` (Colección de firmas de contrato)
  - `eventosContrato` (Colección de eventos de contrato)
  - `notificaciones` (Colección de notificaciones)
  - `unProyecto : Proyecto` (Instancia de la entidad Proyecto)
  - `unPerfil : Perfil` (Instancia de la entidad Perfil)
  - `unMiembro : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
  - `unTipoAcuerdo : TipoAcuerdoContrato` (Instancia de la entidad TipoAcuerdoContrato)
  - `unContrato : Contrato` (Instancia de la entidad Contrato)
  - `otroContrato : Contrato` (Instancia de la entidad Contrato)
  - `unaFirma : FirmaContrato` (Instancia de la entidad FirmaContrato)
  - `unDestinatario : MiembroProyecto` (Instancia de la entidad MiembroProyecto)
- **Variables locales / Notas:**
  - `boolean perfilActivo`
  - `boolean proyectoEncontrado = false`
  - `boolean proyectoApto`
  - `boolean esDirector`
  - `boolean contratoEncontrado = false`
  - `boolean contratoPendiente`
  - `boolean firmaRegistrada = false`
  - `boolean tipoEncontrado = false`
  - `boolean sinDestinatarios`
  - `boolean destinatarioValido`
  - `boolean tieneContratoActivo`
  - `Lista destinatarios = nueva Lista()`
  - `Lista conflictos = nueva Lista()`
  - `Lista firmas = nueva Lista()`
  - `String textoFinal`
  - `String nombreFinal`
  - `String descripcionFinal`
- **Flujo de interacción:**
  1. El actor envía `unContrato = modificarContrato(idProyecto, idContrato, idTipoAcuerdo, idsMiembro[], texto, clausulasAdicionales[], nombre, descripcion)` a `:ModaLink`.
  2. Se inicializan las variables locales `perfilActivo`, `proyectoEncontrado`, `proyectoApto`, `esDirector`, `contratoEncontrado`, `contratoPendiente`, `firmaRegistrada`, `tipoEncontrado`, `sinDestinatarios`, `destinatarioValido`, `tieneContratoActivo`, `destinatarios`, `conflictos` y `firmas`.
  3. `:ModaLink` solicita a `perfiles`: `unPerfil = buscarPerfilActivo(perfilActivo)`.
  4. `:ModaLink` consulta a `unPerfil`: `perfilActivo = estaActivo()`.
  5. **Bloque Alternativo (`alt`)** [!perfilActivo]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil activo no existe o no está activo")`.
  6. **Bloque Bucle (`loop`)** [Mientras haya proyectos && `proyectoEncontrado == false`]:
     - `:ModaLink` solicita a `proyectos`: `unProyecto = obtenerSiguiente()`.
     - `:ModaLink` consulta a `unProyecto`: `proyectoEncontrado = tieneId(idProyecto)`.
  7. **Bloque Alternativo (`alt`)** [!proyectoEncontrado]:
     - `:ModaLink` retorna una excepción al actor: `Excepcion("Proyecto inexistente")`.
  8. **Bloque Alternativo (`alt`)** [proyectoEncontrado]:
     - `:ModaLink` consulta a `unProyecto`: `proyectoApto = estaEnEstado(PUBLICADO) || estaEnEstado(CONFIRMADO)`.
     - **Bloque Alternativo (`alt`)** [!proyectoApto]:
       - `:ModaLink` retorna una excepción al actor: `Excepcion("Solo se puede modificar un contrato de un proyecto PUBLICADO o CONFIRMADO")`.
     - **Bloque Alternativo (`alt`)** [proyectoApto]:
       - `:ModaLink` consulta a `unProyecto`: `esDirector = tieneDirectorActivo(unPerfil)`.
       - **Bloque Alternativo (`alt`)** [!esDirector]:
         - `:ModaLink` retorna una excepción al actor: `Excepcion("El perfil activo no es director del proyecto")`.
       - **Bloque Alternativo (`alt`)** [esDirector]:
         - **Bloque Bucle (`loop`)** [Mientras haya contratos && `contratoEncontrado == false`]:
           - `:ModaLink` solicita a `contratos`: `unContrato = obtenerSiguiente()`.
           - `:ModaLink` consulta a `unContrato`: `contratoEncontrado = tieneId(idContrato) && perteneceA(unProyecto)`.
         - **Bloque Alternativo (`alt`)** [!contratoEncontrado]:
           - `:ModaLink` retorna una excepción al actor: `Excepcion("Contrato inexistente")`.
         - **Bloque Alternativo (`alt`)** [contratoEncontrado]:
           - `:ModaLink` consulta a `unContrato`: `contratoPendiente = estaEnEstado(PENDIENTE_FIRMA)`.
           - **Bloque Alternativo (`alt`)** [!contratoPendiente]:
             - `:ModaLink` retorna una excepción al actor: `Excepcion("Solo se puede modificar un contrato PENDIENTE_FIRMA")`.
           - **Bloque Alternativo (`alt`)** [contratoPendiente]:
             - **Bloque Bucle (`loop`)** [Mientras haya firmas del contrato && `firmaRegistrada == false`]:
               - `:ModaLink` solicita a `firmasContrato`: `unaFirma = obtenerSiguiente()`.
               - `:ModaLink` consulta a `unaFirma`: `firmaRegistrada = estaFirmada() || estaRechazada() || estaInvalidada()`.
             - **Bloque Alternativo (`alt`)** [firmaRegistrada]:
               - `:ModaLink` retorna una excepción al actor: `Excepcion("No se puede modificar un contrato con firmas registradas")`.
             - **Bloque Alternativo (`alt`)** [!firmaRegistrada]:
               - **Bloque Bucle (`loop`)** [Mientras haya tipos de acuerdo && `tipoEncontrado == false`]:
                 - `:ModaLink` solicita a `tiposAcuerdoContrato`: `unTipoAcuerdo = obtenerSiguiente()`.
                 - `:ModaLink` consulta a `unTipoAcuerdo`: `tipoEncontrado = tieneId(idTipoAcuerdo) && estaActivo()`.
               - **Bloque Alternativo (`alt`)** [!tipoEncontrado]:
                 - `:ModaLink` retorna una excepción al actor: `Excepcion("Tipo de acuerdo inexistente o inactivo")`.
               - **Bloque Alternativo (`alt`)** [tipoEncontrado]:
                 - `:ModaLink` evalúa: `sinDestinatarios = idsMiembro == null || idsMiembro.isEmpty()`.
                 - **Bloque Alternativo (`alt`)** [sinDestinatarios]:
                   - `:ModaLink` retorna una excepción al actor: `Excepcion("Debe seleccionar al menos un miembro destinatario")`.
                 - **Bloque Alternativo (`alt`)** [!sinDestinatarios]:
                   - **Bloque Bucle (`loop`)** [Mientras haya miembros del proyecto]:
                     - `:ModaLink` solicita a `miembros`: `unMiembro = obtenerSiguiente()`.
                     - **Bloque Alternativo (`alt`)** [idsMiembro.contiene(unMiembro.getIdMiembro())]:
                       - `:ModaLink` consulta a `unMiembro`: `destinatarioValido = correspondeA(unProyecto) && estaActivo()`.
                       - **Bloque Alternativo (`alt`)** [destinatarioValido]:
                         - `destinatarios.agregar(unMiembro)`.
                       - **Bloque Alternativo (`alt`)** [!destinatarioValido]:
                         - `conflictos.agregar("DESTINATARIO_INVALIDO", unMiembro.getIdMiembro())`.
                   - **Bloque Bucle (`loop`)** [Mientras haya contratos del proyecto]:
                     - `:ModaLink` solicita a `contratos`: `otroContrato = obtenerSiguiente()`.
                     - `:ModaLink` consulta a `otroContrato`: `tieneContratoActivo = otroContrato.tieneContratoActivoDe(unProyecto, unTipoAcuerdo, destinatarios)`.
                     - **Bloque Alternativo (`alt`)** [tieneContratoActivo]:
                       - `conflictos.agregar("CONTRATO_DUPLICADO_ACTIVO", otroContrato.obtenerMiembroDuplicado())`.
                   - **Bloque Alternativo (`alt`)** [!conflictos.isEmpty()]:
                     - `:ModaLink` retorna una excepción al actor: `Excepcion("Existen conflictos de destinatarios o contratos activos")`.
                   - **Bloque Alternativo (`alt`)** [conflictos.isEmpty()]:
                     - **Bloque Bucle (`loop`)** [Mientras haya firmas del contrato]:
                       - `:ModaLink` solicita a `firmasContrato`: `unaFirma = obtenerSiguiente()`.
                       - `firmas.agregar(unaFirma)`.
                       - **Bloque Alternativo (`alt`)** [!destinatarios.contiene(unaFirma.obtenerMiembro())]:
                         - `:ModaLink` envía a `unaFirma`: `cancelar()`.
                     - **Bloque Bucle (`loop`)** [Mientras haya destinatarios]:
                       - `:ModaLink` solicita a `destinatarios`: `unDestinatario = obtenerSiguiente()`.
                       - **Bloque Alternativo (`alt`)** [!firmasContrato.existeFirma(unContrato, unDestinatario)]:
                         - `:ModaLink` crea la firma: `unaFirma = registrar(unContrato, unDestinatario, DESTINATARIO, PENDIENTE)`.
                         - `firmasContrato.agregar(unaFirma)`.
                         - `firmas.agregar(unaFirma)`.
                     - **Bloque Alternativo (`alt`)** [texto != null && texto != ""]:
                       - `:ModaLink` solicita a `unContrato`: `textoFinal = validarTexto(texto)`.
                     - **Bloque Alternativo (`alt`)** [texto == null || texto == ""]:
                       - `:ModaLink` solicita a `unTipoAcuerdo`: `textoFinal = generarTexto(unProyecto, unContrato.obtenerEmisor(), destinatarios, clausulasAdicionales)`.
                     - **Bloque Alternativo (`alt`)** [nombre != null && nombre != ""]:
                       - `nombreFinal = nombre`.
                     - **Bloque Alternativo (`alt`)** [nombre == null || nombre == ""]:
                       - `:ModaLink` solicita a `unContrato`: `nombreFinal = generarNombre(unTipoAcuerdo, unProyecto)`.
                     - **Bloque Alternativo (`alt`)** [descripcion != null]:
                       - `descripcionFinal = descripcion`.
                     - **Bloque Alternativo (`alt`)** [descripcion == null]:
                       - `:ModaLink` solicita a `unContrato`: `descripcionFinal = obtenerDescripcion()`.
                     - `:ModaLink` envía a `unContrato`: `setTipoAcuerdo(unTipoAcuerdo)`.
                     - `:ModaLink` envía a `unContrato`: `setNombre(nombreFinal)`.
                     - `:ModaLink` envía a `unContrato`: `setDescripcion(descripcionFinal)`.
                     - `:ModaLink` envía a `unContrato`: `setTexto(textoFinal)`.
                     - `:ModaLink` envía a `unContrato`: `setFechaModificacion(ahora)`.
                     - `eventosContrato.agregar(registrar(unContrato, MODIFICADO, ahora))`.
                     - **Bloque Bucle (`loop`)** [Mientras haya destinatarios]:
                       - `:ModaLink` solicita a `destinatarios`: `unDestinatario = obtenerSiguiente()`.
                       - `notificaciones.agregar(notificarContratoModificado(unDestinatario, unContrato))`.
                     - `:ModaLink` retorna al actor: `ContratoDetalleResponse(unContrato, firmas)`.

