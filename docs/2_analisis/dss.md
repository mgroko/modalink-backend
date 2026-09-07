# Diagramas de Secuencia del Sistema (DSS) — ModaLink

> **Fase 2: Análisis (Proceso Unificado)**  
> **Fuente de Verdad de Datos**: [bd.sql](file:///C:/Users/HP/Desktop/modalink-backend/docs/0_nota_presentacion/bd.sql)  
> **Regla Metodológica**: El sistema es tratado como una caja negra unificada (`:Sistema`). Las operaciones tienen firmas exactas `nombreOperacion(params)` que se trasladan idénticas a los Contratos de Operación y a los DSD.

---

## 1. Módulo de Gestión de Usuarios (MOD-F-01)

### DSS-01 | Iniciar Sesión (UC-01)
```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Actor: Usuario / Administrador
    participant Sistema as :Sistema

    Usuario ->> Sistema: iniciarSesion(correo, contrasena)
    activate Sistema
    alt Credenciales válidas y usuario en estado HABILITADO
        Sistema -->> Usuario: sesionIniciada(idUsuario, rolGlobal, tokenJwt)
    else Credenciales incorrectas
        Sistema -->> Usuario: errorCredencialesInvalidas()
    else Cuenta en estado DESHABILITADO
        Sistema -->> Usuario: errorCuentaDeshabilitada(motivoDeshabilitacion, fechaHasta)
    end
    deactivate Sistema
```

---

### DSS-03 | Registrarse en el Sistema (UC-03)
```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Actor: Usuario
    participant Sistema as :Sistema

    Usuario ->> Sistema: registrarUsuario(nombre, apellido, dni, fechaNacimiento, correo, contrasena, idGenero, idUbicacion)
    activate Sistema
    alt Correo o DNI ya existente
        Sistema -->> Usuario: errorDatosDuplicados()
    else Edad menor a 18 años
        Sistema -->> Usuario: errorEdadInvalida()
    else Datos válidos
        Sistema -->> Usuario: usuarioRegistrado(idUsuario, estado)
    end
    deactivate Sistema
```

---

### DSS-04 | Deshabilitar Usuario (UC-04)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as Actor: Administrador
    participant Sistema as :Sistema

    Admin ->> Sistema: deshabilitarUsuario(idUsuario, motivoDeshabilitacion, diasDuracion)
    activate Sistema
    alt Usuario no existe o ya está deshabilitado
        Sistema -->> Admin: errorOperacionNoPermitida()
    else Solicitud válida
        Sistema -->> Admin: usuarioDeshabilitado(idUsuario, fechaHastaDeshabilitacion)
    end
    deactivate Sistema
```

---

### DSS-07 | Solicitar Baja en el Sistema (UC-07)
```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Actor: Usuario
    participant Sistema as :Sistema

    Usuario ->> Sistema: solicitarBajaCuenta(idUsuario)
    activate Sistema
    alt Usuario con compromisos activos en proyectos confirmados
        Sistema -->> Usuario: errorBajaImpedidaPorCompromisos()
    else Baja admitida
        Sistema -->> Usuario: bajaSolicitadaConfirmada(fechaSolicitudBaja)
    end
    deactivate Sistema
```

---

## 2. Módulo de Gestión de Perfiles (MOD-F-02)

### DSS-10 | Crear Perfil Creativo (UC-10)
```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Actor: Usuario
    participant Sistema as :Sistema

    Usuario ->> Sistema: solicitarFormularioPerfil()
    activate Sistema
    Sistema -->> Usuario: presentarCatalogo(profesiones, caracteristicasPorProfesion, habilidades)
    deactivate Sistema

    Usuario ->> Sistema: crearPerfil(idProfesion, nombreArtistico, biografia, idFotoPerfil, caracteristicas, habilidades)
    activate Sistema
    alt Usuario posee rol de Administrador Global
        Sistema -->> Usuario: errorAdminNoPuedeTenerPerfil()
    else Perfil ya existente para la profesión seleccionada
        Sistema -->> Usuario: errorPerfilDuplicado()
    else Creación exitosa
        Sistema -->> Usuario: perfilCreado(idPerfil, estado)
    end
    deactivate Sistema
```

---

### DSS-11 | Editar Perfil (UC-11)
```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Actor: Usuario
    participant Sistema as :Sistema

    Usuario ->> Sistema: actualizarPerfil(idPerfil, nombreArtistico, biografia, idFotoPerfil, caracteristicas, habilidades)
    activate Sistema
    alt Perfil no pertenece al usuario autenticado
        Sistema -->> Usuario: errorAccesoDenegado()
    else Datos válidos
        Sistema -->> Usuario: perfilActualizado(idPerfil)
    end
    deactivate Sistema
```

---

### DSS-12 | Eliminar Perfil (Borrado Lógico) (UC-12)
```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Actor: Usuario
    participant Sistema as :Sistema

    Usuario ->> Sistema: darDeBajaPerfil(idPerfil)
    activate Sistema
    alt Perfil con actividades asignadas en proyectos activos
        Sistema -->> Usuario: errorPerfilConActividadesPendientes()
    else Baja lógica exitosa
        Sistema -->> Usuario: perfilDadoDeBaja(idPerfil, fechaBajaPerfil)
    end
    deactivate Sistema
```

---

## 3. Módulo de Gestión de Disponibilidad (MOD-F-05)

### DSS-17/18 | Asignar Bloqueo de Agenda / Modificar Jornada (UC-17, UC-18)
```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Actor: Usuario
    participant Sistema as :Sistema

    Usuario ->> Sistema: configurarJornadaLaboral(idAgenda, diasJornada)
    activate Sistema
    Sistema -->> Usuario: jornadaActualizada(idAgenda)
    deactivate Sistema

    Usuario ->> Sistema: crearBloqueoAgenda(idAgenda, fechaHoraInicio, fechaHoraFin, motivo)
    activate Sistema
    alt Rango horario invertido (inicio >= fin)
        Sistema -->> Usuario: errorRangoFechasInvalido()
    else Solapamiento con otro bloqueo o actividad confirmada
        Sistema -->> Usuario: errorSolapamientoHorario()
    else Bloqueo registrado
        Sistema -->> Usuario: bloqueoRegistrado(idBloqueo)
    end
    deactivate Sistema
```

---

## 4. Módulo de Gestión de Proyectos y Planificación (MOD-F-06, MOD-F-07)

### DSS-24 | Crear Proyecto (UC-24)
```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Actor: Usuario
    participant Sistema as :Sistema

    Usuario ->> Sistema: crearProyecto(nombre, descripcion, fechaInicio, privacidad, aceptaPostulacionGral, idUbicacion)
    activate Sistema
    Sistema -->> Usuario: proyectoCreado(idProyecto, idMiembroDirector, estadoBorrador)
    deactivate Sistema
```

---

### DSS-49 | Crear Actividad en Planificación (UC-49)
```mermaid
sequenceDiagram
    autonumber
    actor Director as Actor: Director de Proyecto
    participant Sistema as :Sistema

    Director ->> Sistema: crearActividadPlanificacion(idPlanificacion, nombre, duracion, fechaHoraInicio, descripcion, idUbicacion, requerimientos)
    activate Sistema
    alt Fecha de actividad anterior a la fecha de inicio del proyecto
        Sistema -->> Director: errorFechaFueraDeRango()
    else Creación exitosa
        Sistema -->> Director: actividadCreada(idActividad)
    end
    deactivate Sistema
```

---

### DSS-60 | Confirmar Proyecto (UC-60)
```mermaid
sequenceDiagram
    autonumber
    actor Director as Actor: Director de Proyecto
    participant Sistema as :Sistema

    Director ->> Sistema: confirmarProyecto(idProyecto)
    activate Sistema
    alt Existen actividades sin miembros requeridos asignados
        Sistema -->> Director: errorRequerimientosIncompletos()
    else Estado válido y miembros confirmados
        Sistema -->> Director: proyectoConfirmado(idProyecto, estadoConfirmado)
    end
    deactivate Sistema
```

---

## 5. Módulo de Publicaciones e Interacción (MOD-F-03, MOD-F-04)

### DSS-19 | Alta de Publicación con Detección de Coautoría (UC-19)
```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Actor: Usuario
    participant Sistema as :Sistema

    Usuario ->> Sistema: crearPublicacion(idPerfil, titulo, descripcion, idProyecto, idsImagenes, idsColaboradores)
    activate Sistema
    opt Imagen ya existente en el repositorio de otro usuario
        Sistema -->> Sistema: generarSolicitudColaboracionAutomatica()
    end
    Sistema -->> Usuario: publicacionCreada(idPublicacion, fechaPublicacion)
    deactivate Sistema
```
