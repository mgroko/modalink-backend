# Diagramas de Secuencia de Diseño (DSD) — ModaLink

> **Fase 3: Diseño (Proceso Unificado)**  
> **Fuente Canónica de Datos**: [bd.sql](file:///C:/Users/HP/Desktop/modalink-backend/docs/0_nota_presentacion/bd.sql)  
> **Patrones Aplicados**: Controlador, Creador, Experto en Información y Repositorio de Persistencia.  
> **Regla Metodológica**: Las firmas de métodos coinciden carácter por carácter con las definidas en los DSS y Contratos de la Fase 2.

---

## 1. DSD: `iniciarSesion(correo, contrasena)` | UC-01
* **Actores**: Usuario / Administrador
* **Objetos / Instancias participantes**:
  * `:AuthController` (Controlador)
  * `:AuthService` (Servicio de Dominio)
  * `usuarioRepo : UsuarioRepository` (Repositorio / Colección SQL)
  * `passwordEncoder : PasswordEncoder` (Servicio criptográfico)
  * `jwtUtil : JwtUtil` (Generador de tokens)
  * `u : Usuario` (Fila de la tabla `usuario`)
* **Flujo de interacción**:
  1. El actor invoca `iniciarSesion(correo, contrasena)` en `:AuthController`.
  2. `:AuthController` delega en `:AuthService.autenticar(correo, contrasena)`.
  3. `:AuthService` invoca `u = usuarioRepo.findByCorreo(correo)`.
  4. **Bloque Alternativo (`alt`)** [u == null]:
     * `:AuthService` lanza `CredencialesInvalidasException`.
  5. `:AuthService` consulta `coincide = passwordEncoder.matches(contrasena, u.password_hash)`.
  6. **Bloque Alternativo (`alt`)** [coincide == false]:
     * `:AuthService` lanza `CredencialesInvalidasException`.
  7. **Bloque Alternativo (`alt`)** [u.estado == 'DESHABILITADO']:
     * **Bloque Opcional (`opt`)** [u.fecha_hasta_deshabilitacion != null && CURRENT_DATE >= u.fecha_hasta_deshabilitacion]:
       * `:AuthService` actualiza `u.estado = 'HABILITADO'`.
       * `:AuthService` invoca `usuarioRepo.save(u)`.
     * **Bloque Alternativo (`alt`)** [u.estado sigue siendo 'DESHABILITADO']:
       * `:AuthService` lanza `CuentaDeshabilitadaException(u.motivo_deshabilitacion, u.fecha_hasta_deshabilitacion)`.
  8. `:AuthService` solicita `token = jwtUtil.generarToken(u.id_usuario, u.rol_global_id_rol)`.
  9. `:AuthController` adjunta el JWT en una cookie httpOnly `jwt` y retorna la respuesta con estado 200 OK.

---

## 2. DSD: `registrarUsuario(nombre, apellido, dni, fechaNacimiento, correo, contrasena, idGenero, idUbicacion)` | UC-03
* **Actores**: Usuario
* **Objetos / Instancias participantes**:
  * `:AuthController` (Controlador)
  * `:RegistroService` (Servicio de Dominio)
  * `usuarioRepo : UsuarioRepository` (Colección `usuario`)
  * `agendaRepo : AgendaRepository` (Colección `agenda`)
  * `generoRepo : GeneroRepository` (Colección `genero`)
  * `ubicacionRepo : UbicacionRepository` (Colección `ubicacion`)
  * `passwordEncoder : PasswordEncoder`
  * `u : Usuario` (Instancia de `usuario`)
  * `a : Agenda` (Instancia de `agenda`)
* **Flujo de interacción**:
  1. El actor invoca `registrarUsuario(...)` en `:AuthController`.
  2. `:AuthController` invoca `:RegistroService.registrar(...)`.
  3. `:RegistroService` valida unicidad:
     * `existeCorreo = usuarioRepo.existsByCorreo(correo)`
     * `existeDni = usuarioRepo.existsByDni(dni)`
     * **Bloque Alternativo (`alt`)** [existeCorreo || existeDni]:
       * Lanza `DatosDuplicadosException`.
  4. `:RegistroService` valida edad:
     * `esMayor = calcularEdad(fechaNacimiento) >= 18`
     * **Bloque Alternativo (`alt`)** [!esMayor]:
       * Lanza `EdadInvalidaException`.
  5. `:RegistroService` invoca `hash = passwordEncoder.encode(contrasena)`.
  6. `:RegistroService` instancia `u = new Usuario(nuevoId(), nombre, apellido, dni, fechaNacimiento, correo, hash, 'HABILITADO', 'LOCAL', idGenero, idUbicacion, 'ROL_USUARIO')`.
  7. `:RegistroService` ejecuta `usuarioRepo.save(u)`.
  8. `:RegistroService` instancia `a = new Agenda(nuevoIdAgenda(), u.id_usuario, '30')`.
  9. `:RegistroService` ejecuta `agendaRepo.save(a)`.
  10. `:AuthController` retorna confirmación de registro.

---

## 3. DSD: `deshabilitarUsuario(idUsuario, motivoDeshabilitacion, diasDuracion)` | UC-04
* **Actores**: Administrador
* **Objetos / Instancias participantes**:
  * `:AdminUsuarioController` (Controlador)
  * `:AdminUsuarioService` (Servicio de Dominio)
  * `usuarioRepo : UsuarioRepository` (Colección `usuario`)
  * `u : Usuario` (Instancia de `usuario`)
* **Flujo de interacción**:
  1. El administrador invoca `deshabilitarUsuario(idUsuario, motivoDeshabilitacion, diasDuracion)` en `:AdminUsuarioController`.
  2. `:AdminUsuarioController` delega en `:AdminUsuarioService.deshabilitar(idUsuario, motivoDeshabilitacion, diasDuracion)`.
  3. `:AdminUsuarioService` invoca `u = usuarioRepo.findById(idUsuario)`.
  4. **Bloque Alternativo (`alt`)** [u == null]:
     * Lanza `UsuarioNoEncontradoException`.
  5. **Bloque Alternativo (`alt`)** [u.estado == 'DESHABILITADO']:
     * Lanza `OperacionNoPermitidaException`.
  6. `:AdminUsuarioService` ejecuta:
     * `u.setEstado('DESHABILITADO')`
     * `u.setMotivoDeshabilitacion(motivoDeshabilitacion)`
     * **Bloque Alternativo (`alt`)** [diasDuracion != null]:
       * `u.setFechaHastaDeshabilitacion(LocalDate.now().plusDays(diasDuracion))`
     * **Bloque Alternativo (`alt`)** [diasDuracion == null]:
       * `u.setFechaHastaDeshabilitacion(null)`
  7. `:AdminUsuarioService` ejecuta `usuarioRepo.save(u)`.
  8. `:AdminUsuarioController` retorna estado 200 OK con los datos actualizados.

---

## 4. DSD: `crearBloqueoAgenda(idAgenda, fechaHoraInicio, fechaHoraFin, motivo)` | UC-18
* **Actores**: Usuario
* **Objetos / Instancias participantes**:
  * `:CalendarioController` (Controlador)
  * `:CalendarioService` (Servicio de Dominio)
  * `agendaRepo : AgendaRepository` (Colección `agenda`)
  * `bloqueoRepo : BloqueoAgendaRepository` (Colección `bloqueo_agenda`)
  * `b : BloqueoAgenda` (Instancia de `bloqueo_agenda`)
* **Flujo de interacción**:
  1. El actor invoca `crearBloqueoAgenda(...)` en `:CalendarioController`.
  2. `:CalendarioController` delega en `:CalendarioService.agregarBloqueo(...)`.
  3. `:CalendarioService` valida consistencia:
     * **Bloque Alternativo (`alt`)** [fechaHoraFin <= fechaHoraInicio]:
       * Lanza `RangoFechasInvalidoException`.
  4. `:CalendarioService` verifica solapamiento en BD:
     * `haySolapamiento = bloqueoRepo.existeSolapamiento(idAgenda, fechaHoraInicio, fechaHoraFin)`
     * **Bloque Alternativo (`alt`)** [haySolapamiento == true]:
       * Lanza `SolapamientoHorarioException`.
  5. `:CalendarioService` instancia `b = new BloqueoAgenda(nuevoId(), fechaHoraInicio, fechaHoraFin, motivo, idAgenda)`.
  6. `:CalendarioService` ejecuta `bloqueoRepo.save(b)`.
  7. `:CalendarioController` retorna el ID y estado del bloqueo creado.
