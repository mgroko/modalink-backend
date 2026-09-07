# Aspectos Técnicos del Modelo de Dominio — ModaLink

> **Propósito**: Traducir el esquema relacional canónico (`bd.sql`) a conceptos formales del dominio del negocio según la metodología del Proceso Unificado (Fase 2: Análisis), especificando políticas de persistencia, integridad referencial, borrado lógico y catálogos.

---

## 1. Principios y Criterios Transversales de Diseño

1. **Fuente de Verdad Relacional**: Toda entidad del dominio tiene correspondencia directa 1:1 con las 38 tablas declaradas en el archivo físico [bd.sql](file:///C:/Users/HP/Desktop/modalink-backend/docs/0_nota_presentacion/bd.sql).
2. **Atributos Derivados y Calculables**: No se persisten redundancias en disco salvo requerimiento explícito. Por ejemplo:
   - Los bloqueos automáticos generados por asignación a actividades de proyectos se recalculan dinámicamente consultando la tabla `actividad` a través de `asignacion_actividad` y sumando el margen configurado en `agenda.margen_actividad_min`.
   - La edad del usuario se deriva de `usuario.fecha_nacimiento` respecto a la fecha actual (`CURRENT_DATE`).
3. **Política de Borrado Lógico**:
   - Para entidades con historial crítico no se realiza borrado físico (`DELETE`).
   - En `perfil`, el borrado lógico se registra mediante `fecha_baja_perfil` (tipo `TIMESTAMP`) y el campo `estado` (`'ACTIVO'`, `'DADO_DE_BAJA'`).
   - En `usuario`, la baja solicitada se asienta en `fecha_solicitud_baja` y se transiciona el `estado` a `'PENDIENTE_BAJA'`.
   - La suspensión temporal administrativa utiliza `motivo_deshabilitacion` y `fecha_hasta_deshabilitacion`.
4. **Tablas Catálogo vs Flags**:
   - Se utilizan tablas independientes de catálogo (`genero`, `profesion`, `habilidad`, `caracteristica_tecnica`, `rol_global`, `rol_proyecto`) para asegurar extensibilidad sin modificar el esquema físico.
5. **Separación de Roles Globales y de Proyecto**:
   - `rol_global` (`ADMINISTRADOR`, `USUARIO`): Gobierna permisos a nivel plataforma (`permiso_global`). Un usuario administrador global no puede poseer perfil creativo ni crear proyectos.
   - `rol_proyecto` (`DIRECTOR`, `MIEMBRO`): Gobierna la gobernanza interna de cada proyecto (`permiso_proyecto`). Un usuario puede ser Director en el Proyecto A y Miembro en el Proyecto B. Todo proyecto debe tener al menos un Director.

---

## 2. Diccionario de Entidades y Mapeo Físico (38 Tablas)

### 2.1 Módulo: Usuarios, Autenticación y Seguridad

#### Tabla `usuario`
- **id_usuario** (`char(10)`, PK): Identificador unívoco del usuario.
- **nombre** (`varchar(50)`, NOT NULL) y **apellido** (`varchar(50)`, NOT NULL): Datos de filiación personal.
- **"DNI"** (`varchar(15)`, NULLABLE): Documento de identidad (debe ser numérico positivo).
- **correo** (`varchar(255)`, NOT NULL, UNIQUE): Email corporativo/personal para autenticación y notificaciones.
- **fecha_nacimiento** (`date`, NOT NULL): Fecha de nacimiento (valida mayoría de edad $\ge 18$ años).
- **id_genero** (`char(10)`, NOT NULL, FK $\rightarrow$ `genero`): Identidad de género del usuario.
- **id_ubicacion** (`char(20)`, NOT NULL, FK $\rightarrow$ `ubicacion`): Localización de residencia.
- **rol_global_id_rol** (`char(10)`, NOT NULL, FK $\rightarrow$ `rol_global`): Rol administrativo o regular.
- **estado** (`varchar(20)`, NOT NULL): `'HABILITADO'`, `'DESHABILITADO'`, `'PENDIENTE_BAJA'`.
- **motivo_deshabilitacion** (`varchar(100)`, NOT NULL): Justificación administrativa de la sanción.
- **fecha_hasta_deshabilitacion** (`date`, NULLABLE): Fecha límite de la suspensión (reversión automática).
- **proveedor_auth** (`varchar(20)`, NULLABLE): `'LOCAL'` o `'GOOGLE'`.
- **password_hash** (`varchar(20)` / BCrypt en capa de aplicación): Hash de la clave de acceso.
- **id_externo** (`varchar(20)`, NOT NULL): Identificador provisto por Identity Provider externo.
- **fecha_solicitud_baja** (`timestamp`, NULLABLE): Marca temporal cuando el usuario solicita darse de baja.

#### Tabla `rol_global`, `permiso_global`, `rol_global_permiso`
- `rol_global`: Identificador y nombre del rol de plataforma.
- `permiso_global`: Acciones autorizadas (`ADMIN_USUARIOS`, `GESTION_PROFESIONES`, etc.).
- `rol_global_permiso`: Tabla intermedia de asociación muchos a muchos.

#### Tabla `ubicacion`
- Normaliza `pais`, `provincia`, `localidad`, `codigo_postal`, `latitud` y `longitud` para geolocalización de usuarios y proyectos.

#### Tabla `genero`
- Catálogo de géneros (`MASCULINO`, `FEMENINO`, `NO_BINARIO`, `OTRO`).

---

### 2.2 Módulo: Perfiles Profesionales y Habilidades

#### Tabla `perfil`
- **id_perfil** (`char(10)`, PK): Identificador del perfil creativo.
- **nombre_artistico** (`varchar(50)`): Alias profesional visible en el portfolio.
- **biografia** (`varchar(200)`): Resumen de trayectoria profesional.
- **foto_perfil** (`char(10)`, NOT NULL, FK $\rightarrow$ `imagen`): Foto de portada/perfil.
- **id_profesion** (`char(10)`, NOT NULL, FK $\rightarrow$ `profesion`): Especialidad técnica principal.
- **estado** (`varchar(20)`): Estado operativo del perfil (`ACTIVO`, `INACTIVO`, `DADO_DE_BAJA`).
- **fecha_baja_perfil** (`timestamp`): Registro de eliminación lógica.

#### Tablas `profesion`, `caracteristica_tecnica`, `valor_caracteristica`, `caracteristica_perfil`
- `profesion`: Catálogo (`MODELO`, `FOTOGRAFO`, `DISENADOR_INDUMENTARIA`, `MAQUILLADOR`, `ESTILISTA`).
- `caracteristica_tecnica`: Parámetros medibles por profesión (ej. *Altura*, *Busto*, *Cintura*, *Calzado*, *Color de Ojos*).
- `valor_caracteristica`: Opciones prefijadas para características tipo selección (códigos y colores hex).
- `caracteristica_perfil`: Valor concreto asignado a un perfil (`valor`, `fecha_registro`).

#### Tablas `habilidad`, `habilidad_perfil`
- `habilidad`: Catálogo de habilidades transversales y técnicas (ej. *Pasarela alta costura*, *Iluminación estudio*, *Patronaje industrial*).
- `habilidad_perfil`: Tabla intermedia que asocia perfiles con sus destrezas acreditadas.

---

### 2.3 Módulo: Disponibilidad y Calendario

#### Tabla `agenda`
- **id_agenda** (`char(10)`, PK): Agenda vinculada a un único usuario (`id_usuario`).
- **margen_actividad_min** (`varchar(20)`, NOT NULL): Tiempo de amortiguación (en minutos) requerido entre compromisos para traslados y preparación.

#### Tabla `jornada_agenda`
- Permite configurar la jornada laboral por día de la semana (`dia_semana`).
- Soporta **jornada corrida** (`hora_inicio` a `hora_fin`) y **jornada partida** (`hora_fin_manana` y `hora_inicio_tarde`).

#### Tabla `bloqueo_agenda`
- Bloqueos manuales creados por el usuario (`fecha_hora_inicio`, `fecha_hora_fin`, `motivo`).
- **Regla de integridad**: No se admiten bloqueos con fechas invertidas (`fecha_hora_fin > fecha_hora_inicio`) ni solapamientos temporales sobre la misma agenda.

---

### 2.4 Módulo: Proyectos, Planificación y Cronograma

#### Tabla `proyecto`
- **id_proyecto** (`char(10)`, PK): Identificador único del proyecto.
- **nombre** (`varchar(50)`, NOT NULL) y **descripcion** (`varchar(200)`, NOT NULL).
- **fecha_inicio** (`timestamp`, NOT NULL): Comienzo formal del proyecto.
- **estado** (`varchar(20)`, NOT NULL): `'BORRADOR'`, `'PUBLICADO'`, `'CONFIRMADO'`, `'FINALIZADO'`, `'CANCELADO'`.
- **privacidad** (`varchar(20)`, NOT NULL): `'PUBLICO'`, `'PRIVADO'`, `'OCULTO'`.
- **acepta_postulacion_gral** (`char(10)`, NOT NULL): Flag indicador de postulaciones abiertas.
- **id_ubicacion** (`char(20)`, NOT NULL, FK $\rightarrow$ `ubicacion`): Sede o locación principal de la producción.

#### Tabla `miembros_proyecto`
- Vincula perfiles (`id_perfil`) con el proyecto (`id_proyecto`).
- Asigna un `id_rol` (`rol_proyecto`: Director o Miembro) y un `estado_participacion` (`ACTIVO`, `BAJA_SOLICITADA`, `RETIRADO`).

#### Tablas `planificacion`, `actividad`, `dependencia_actividades`
- `planificacion`: Cronograma marco del proyecto con `fecha_entrega`.
- `actividad`: Bloque operativo concreto con `nombre`, `duracion` (en minutos), `fecha_hora_inicio`, `descripcion`, `id_ubicacion`.
- `dependencia_actividades`: Grafo dirigido que modela precedencias (`id_actividad_predecesora` y `id_actividad_sucesora`). Una actividad no puede iniciar hasta que finalicen sus predecesoras.

#### Tablas de Requerimientos de Personal
- `requerimiento_gral_proyecto`: Necesidad global de profesionales por disciplina (`id_profesion`, `cantidad`).
- `requerimiento_actividad`: Necesidad de un perfil para una actividad puntual (`id_actividad`, `id_profesion`, `cantidad`).
- `requerimiento_gral_caract`, `requerimiento_gral_habilidad`, `requerimiento_act_caract`, `requerimiento_act_habilidad`: Tablas puente que asocian restricciones técnicas exactas a la convocatoria.

#### Tablas de Postulaciones e Invitaciones
- `postulacion_gral` y `postulacion_actividad`: Solicitud iniciada por el profesional (`ESTADO`: `PENDIENTE`, `ACEPTADA`, `RECHAZADA`).
- `invitacion_gral` e `invitacion_actividad`: Invitación emitida por el Director del proyecto hacia un profesional destinatario.

---

### 2.5 Módulo: Publicaciones, Portfolio y Contenido Multimedia

#### Tablas `imagen`, `publicacion`, `imagen_publicacion`
- `imagen`: Almacena metadatos de archivo (`url`, `nombre_archivo`, `tipo_imagen`, `tamano_bytes`, `fecha_subida`, `estado`).
- `publicacion`: Pieza de portfolio asociada a un autor (`id_perfil`) y opcionalmente a un proyecto (`id_proyecto`), con `titulo`, `descripcion` y `fecha_publicacion`.
- `imagen_publicacion`: Galería fotográfica asociada a la publicación.

#### Tablas `colaborador_publicacion`, `solicitud_colaboracion`
- `colaborador_publicacion`: Créditos formales asignados a coautores (fotógrafos, modelos, maquilladores) en una misma producción.
- `solicitud_colaboracion`: Flujo de validación mutua cuando se detecta o propone una colaboración compartida.

#### Tablas `me_gusta`, `comentario`
- Interacción y feedback social sobre publicaciones.

#### Tablas `moodboard`, `imagen_moodboard`, `objetivo`
- Herramientas visuales y conceptuales para que los directores de proyecto alineen la estética de la producción antes del rodaje.

---

## 3. Matriz de Integridad Referencial y Reglas de Negocio

| Entidad Padre | Entidad Hija | Clave Foránea | Regla de Negocio / Comportamiento |
| :--- | :--- | :--- | :--- |
| `usuario` | `agenda` | `id_usuario` | Cada usuario posee exactamente una agenda personal creada al registrarse. |
| `agenda` | `bloqueo_agenda` | `id_agenda` | Los bloqueos manuales deben pertenecer a la agenda del usuario logueado. |
| `profesion` | `caracteristica_tecnica` | `id_profesion` | Las características técnicas están fuertemente tipadas y acotadas a la profesión. |
| `proyecto` | `miembros_proyecto` | `id_proyecto` | Un proyecto no puede quedar con 0 directores activos. |
| `proyecto` | `planificacion` | `id_proyecto` | Toda planificación pertenece a un proyecto y organiza sus actividades. |
| `actividad` | `asignacion_actividad` | `id_actividad` | Solo los miembros confirmados del proyecto pueden ser asignados a sus actividades. |
| `publicacion`| `solicitud_colaboracion`| `id_publicacion` | La solicitud de coautoría debe vincularse a una publicación existente. |
