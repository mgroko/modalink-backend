# Casos de Uso Reales — ModaLink

> **Fase 3: Diseño (Proceso Unificado)**  
> **Fuente Canónica de Datos**: [bd.sql](file:///C:/Users/HP/Desktop/modalink-backend/docs/0_nota_presentacion/bd.sql)  
> **Propósito**: Vincular los Casos de Uso Extendidos (Fase 1) con la experiencia de usuario (UI/UX), widgets de interfaz, esquemas de navegación y mapeo directo a campos de base de datos.

---

## 1. Esquema de Navegación Global del Sistema

```
[Visitante]
  ├── /auth/login                 (Inicio de sesión)
  ├── /auth/registro              (Alta de cuenta)
  └── /explorar                   (Catálogo público de proyectos y portfolios)

[Usuario Autenticado]
  ├── /dashboard                  (Resumen de actividad y notificaciones)
  ├── /perfiles
  │     ├── /crear                (Alta de perfil técnico)
  │     ├── /{idPerfil}           (Visualización de portfolio y características)
  │     └── /{idPerfil}/editar    (Edición de biografía y habilidades)
  ├── /calendario
  │     ├── /jornada              (Configuración de jornada corrida / partida)
  │     └── /bloqueos             (Gestión de indisponibilidad)
  ├── /proyectos
  │     ├── /nuevo                (Creación de proyecto)
  │     ├── /{idProyecto}         (Detalle del proyecto)
  │     └── /{idProyecto}/planificacion (Cronograma, actividades y asignaciones)
  └── /publicaciones
        └── /nueva                (Carga de fotos de portfolio y etiquetado de colaboradores)

[Administrador Global]
  └── /admin
        ├── /usuarios             (Listado, habilitación, deshabilitación con motivo y plazo)
        └── /caracteristicas      (Gestión de catálogo técnico por profesión)
```

---

## 2. Realización de Casos de Uso Reales

### CUR-01: Iniciar Sesión (UC-01)
- **Ruta UI**: `/auth/login`
- **Actor**: Usuario / Administrador
- **Elementos de Interfaz**:
  - `[inputEmail]`: Mapea a `usuario.correo` (tipo email, validación formato).
  - `[inputPassword]`: Mapea a `usuario.password_hash` (tipo password).
  - `[btnIngresar]`: Despacha la acción de autenticación.
  - `[bannerAlerta]`: Despliega mensaje de cuenta deshabilitada mostrando `usuario.motivo_deshabilitacion` y `usuario.fecha_hasta_deshabilitacion` si aplica.
- **Flujo en Pantalla**:
  1. El usuario navega a `/auth/login`.
  2. Completa los campos `[inputEmail]` e `[inputPassword]` y hace clic en `[btnIngresar]`.
  3. El frontend envía la petición `POST /auth/login`.
  4. Si la respuesta es exitosa, se almacena la cookie segura httpOnly `jwt` y se redirige a `/dashboard` (o `/admin/usuarios` si el rol es Administrador).

---

### CUR-03: Registrarse en el Sistema (UC-03)
- **Ruta UI**: `/auth/registro`
- **Actor**: Usuario Visitante
- **Elementos de Interfaz**:
  - `[inputNombre]`: Mapea a `usuario.nombre`.
  - `[inputApellido]`: Mapea a `usuario.apellido`.
  - `[inputDni]`: Mapea a `usuario."DNI"` (solo dígitos numéricos).
  - `[inputFechaNacimiento]`: Mapea a `usuario.fecha_nacimiento` (date picker, control $\ge 18$ años).
  - `[selectGenero]`: Carga opciones desde tabla `genero` (`id_genero`, `codigo`).
  - `[selectProvincia]`, `[selectLocalidad]`: Mapea a `usuario.id_ubicacion` $\rightarrow$ `ubicacion`.
  - `[inputCorreo]`: Mapea a `usuario.correo`.
  - `[inputPassword]`: Contraseña en texto claro para hash BCrypt.
  - `[btnRegistrar]`: Botón de envío.
- **Flujo en Pantalla**:
  1. El usuario completa el formulario multi-paso.
  2. Presiona `[btnRegistrar]`.
  3. El sistema valida las restricciones en BD y crea automáticamente la agenda inicial con `margen_actividad_min = '30'`.
  4. Redirige a `/auth/login` con toast de éxito.

---

### CUR-04: Deshabilitar Usuario (UC-04)
- **Ruta UI**: `/admin/usuarios`
- **Actor**: Administrador Global
- **Elementos de Interfaz**:
  - `[tablaUsuarios]`: Grilla con columnas (Nombre, Correo, DNI, Rol, Estado).
  - `[btnDeshabilitar]`: Dispara modal de sanción administrativa.
  - `[modalDeshabilitar]`:
    - `[inputMotivo]`: Mapea a `usuario.motivo_deshabilitacion` (obligatorio, text area).
    - `[inputDiasDuracion]`: Campo numérico opcional. Calcula `usuario.fecha_hasta_deshabilitacion`.
    - `[btnConfirmarDeshabilitacion]`: Botón de ejecución.
- **Flujo en Pantalla**:
  1. El administrador ubica al usuario infractor en `[tablaUsuarios]` y pulsa `[btnDeshabilitar]`.
  2. En `[modalDeshabilitar]` ingresa obligatoriamente el motivo y opcionalmente los días.
  3. Al pulsar `[btnConfirmarDeshabilitacion]`, el sistema envía `PATCH /admin/usuarios/{id}/deshabilitar`, cambia el badge en la grilla a `DESHABILITADO` y cierra la sesión activa del usuario.

---

### CUR-10: Crear Perfil Creativo (UC-10)
- **Ruta UI**: `/perfiles/crear`
- **Actor**: Usuario Autenticado
- **Elementos de Interfaz**:
  - `[selectProfesion]`: Selector alimentado desde la tabla `profesion` (`id_profesion`, `nombre`).
  - `[inputNombreArtistico]`: Mapea a `perfil.nombre_artistico`.
  - `[textAreaBiografia]`: Mapea a `perfil.biografia`.
  - `[uploaderFotoPerfil]`: Carga imagen a `imagen` (`url`, `nombre_archivo`) y vincula `perfil.foto_perfil`.
  - `[seccionCaracteristicasDinamicas]`: Se renderiza automáticamente según la profesión seleccionada consultando `caracteristica_tecnica` (inputs numéricos para medidas, selects para color de ojos/pelo cargados desde `valor_caracteristica`).
  - `[multiSelectHabilidades]`: Chips de selección de la tabla `habilidad`.
  - `[btnGuardarPerfil]`: Guarda el perfil.

---

### CUR-17 / CUR-18: Gestión de Agenda y Bloqueos (UC-17, UC-18)
- **Ruta UI**: `/calendario`
- **Actor**: Usuario Autenticado
- **Elementos de Interfaz**:
  - `[vistaCalendarioSemanal]`: Visualizador interactivo de bloques horarios.
  - `[switchJornada]`: Selector entre "Jornada corrida" y "Jornada partida" persistida en `jornada_agenda` (`hora_fin_manana`, `hora_inicio_tarde`).
  - `[inputMargen]`: Configura `agenda.margen_actividad_min` (ej. 30 min).
  - `[btnNuevoBloqueo]`: Abre modal de bloqueo personal.
  - `[modalBloqueo]`:
    - `[inputInicio]`, `[inputFin]`: Mapean a `bloqueo_agenda.fecha_hora_inicio` y `fecha_hora_fin`.
    - `[inputMotivo]`: Mapea a `bloqueo_agenda.motivo`.
    - `[btnGuardarBloqueo]`: Guarda el bloqueo en BD tras validar ausencia de solapamientos.

---

### CUR-24: Crear Proyecto y Planificación (UC-24)
- **Ruta UI**: `/proyectos/nuevo`
- **Actor**: Usuario Autenticado
- **Elementos de Interfaz**:
  - `[inputNombreProyecto]`: Mapea a `proyecto.nombre`.
  - `[textAreaDescripcion]`: Mapea a `proyecto.descripcion`.
  - `[inputFechaInicio]`: Mapea a `proyecto.fecha_inicio`.
  - `[selectPrivacidad]`: Radio buttons (`PUBLICO`, `PRIVADO`, `OCULTO`).
  - `[switchAceptaPostulacion]`: Flag para `proyecto.acepta_postulacion_gral`.
  - `[selectUbicacion]`: Selector geográfico vinculado a `ubicacion`.
  - `[btnCrearProyecto]`: Da de alta el proyecto en estado `BORRADOR`, inicializa `planificacion` y asigna al usuario como Director en `miembros_proyecto`.
