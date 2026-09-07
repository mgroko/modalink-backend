--
-- ER/Studio 8.0 SQL Code Generation
-- Company :      HP Inc.
-- Project :      Version-00.02.DM1
-- Author :       notebooks24@hotmail.com
--
-- Date Created : Friday, September 04, 2026 11:46:59
-- Target DBMS : PostgreSQL 8.0
--

DROP TABLE actividad
;
DROP TABLE agenda
;
DROP TABLE asignacion_actividad
;
DROP TABLE bloqueo_agenda
;
DROP TABLE caracteristica_perfil
;
DROP TABLE caracteristica_tecnica
;
DROP TABLE colaborador_publicacion
;
DROP TABLE comentario
;
DROP TABLE dependencia_actividades
;
DROP TABLE genero
;
DROP TABLE habilidad
;
DROP TABLE habilidad_perfil
;
DROP TABLE imagen
;
DROP TABLE imagen_moodboard
;
DROP TABLE imagen_publicacion
;
DROP TABLE invitacion_actividad
;
DROP TABLE invitacion_gral
;
DROP TABLE jornada_agenda
;
DROP TABLE me_gusta
;
DROP TABLE miembros_proyecto
;
DROP TABLE moodboard
;
DROP TABLE objetivo
;
DROP TABLE perfil
;
DROP TABLE permiso_global
;
DROP TABLE permiso_proyecto
;
DROP TABLE planificacion
;
DROP TABLE postulacion_actividad
;
DROP TABLE postulacion_gral
;
DROP TABLE profesion
;
DROP TABLE proyecto
;
DROP TABLE publicacion
;
DROP TABLE requerimiento_act_caract
;
DROP TABLE requerimiento_act_habilidad
;
DROP TABLE requerimiento_actividad
;
DROP TABLE requerimiento_gral_caract
;
DROP TABLE requerimiento_gral_habilidad
;
DROP TABLE requerimiento_gral_proyecto
;
DROP TABLE rol_global
;
DROP TABLE rol_global_permiso
;
DROP TABLE rol_proyecto
;
DROP TABLE rol_proyecto_permiso
;
DROP TABLE solicitud_colaboracion
;
DROP TABLE ubicacion
;
DROP TABLE usuario
;
DROP TABLE valor_caracteristica
;
-- 
-- TABLE: actividad 
--

CREATE TABLE actividad(
    id_actividad         varchar(20)     NOT NULL,
    nombre               varchar(20)     NOT NULL,
    duracion             int4            NOT NULL,
    fecha_hora_inicio    timestamp       NOT NULL,
    descripcion          varchar(200),
    id_planificacion     char(10)        NOT NULL,
    id_ubicacion         char(20)        NOT NULL
)
;



-- 
-- TABLE: agenda 
--

CREATE TABLE agenda(
    id_agenda               char(10)       NOT NULL,
    margen_actividad_min    varchar(20)    NOT NULL,
    id_usuario              char(10)       NOT NULL
)
;



-- 
-- TABLE: asignacion_actividad 
--

CREATE TABLE asignacion_actividad(
    id_asignacion_act    char(10)       NOT NULL,
    id_miembro           char(10)       NOT NULL,
    id_actividad         varchar(20)    NOT NULL
)
;



-- 
-- TABLE: bloqueo_agenda 
--

CREATE TABLE bloqueo_agenda(
    id_bloqueo           char(10)        NOT NULL,
    fecha_hora_inicio    timestamp       NOT NULL,
    fecha_hora_fin       timestamp       NOT NULL,
    motivo               varchar(200)    NOT NULL,
    id_agenda            char(10)        NOT NULL
)
;



-- 
-- TABLE: caracteristica_perfil 
--

CREATE TABLE caracteristica_perfil(
    id_caracteristica_perfil    char(10)       NOT NULL,
    id_caracteristica           char(10),
    valor                       varchar(50)    NOT NULL,
    fecha_registro              date           NOT NULL,
    id_perfil                   char(10)
)
;



-- 
-- TABLE: caracteristica_tecnica 
--

CREATE TABLE caracteristica_tecnica(
    id_caracteristica    char(10)       NOT NULL,
    unidad               varchar(50),
    codigo               varchar(10),
    tipo_dato            varchar(50)    NOT NULL,
    id_profesion         char(10)
)
;



-- 
-- TABLE: colaborador_publicacion 
--

CREATE TABLE colaborador_publicacion(
    id_perfil         char(10),
    id_publicacion    char(10)
)
;



-- 
-- TABLE: comentario 
--

CREATE TABLE comentario(
    id_comentario       char(10)    NOT NULL,
    texto               char(10),
    fecha_comentario    char(10),
    id_publicacion      char(10)    NOT NULL,
    id_perfil           char(10)    NOT NULL
)
;



-- 
-- TABLE: dependencia_actividades 
--

CREATE TABLE dependencia_actividades(
    id_actividad_predecesora    varchar(20),
    id_actividad_sucesora       varchar(20)
)
;



-- 
-- TABLE: genero 
--

CREATE TABLE genero(
    id_genero    char(10)       NOT NULL,
    codigo       varchar(50)
)
;



-- 
-- TABLE: habilidad 
--

CREATE TABLE habilidad(
    id_habilidad    char(10)        NOT NULL,
    nombre          varchar(20)     NOT NULL,
    descripción     varchar(200)
)
;



-- 
-- TABLE: habilidad_perfil 
--

CREATE TABLE habilidad_perfil(
    id_habilidad    char(10),
    id_perfil       char(10)
)
;



-- 
-- TABLE: imagen 
--

CREATE TABLE imagen(
    id_imagen         char(10)         NOT NULL,
    url               varchar(1024)    NOT NULL,
    estado            varchar(10),
    nombre_archivo    varchar(50)      NOT NULL,
    tipo_imagen       varchar(20)      NOT NULL,
    tamano_bytes      int4             NOT NULL,
    fecha_subida      timestamp        NOT NULL
)
;



-- 
-- TABLE: imagen_moodboard 
--

CREATE TABLE imagen_moodboard(
    id_moodboard    char(10),
    id_imagen       char(10)
)
;



-- 
-- TABLE: imagen_publicacion 
--

CREATE TABLE imagen_publicacion(
    id_imagen         char(10),
    id_publicacion    char(10)
)
;



-- 
-- TABLE: invitacion_actividad 
--

CREATE TABLE invitacion_actividad(
    id_invitacion             char(10)        NOT NULL,
    mensaje                   varchar(100),
    fecha_envio               char(10),
    estado                    char(10),
    id_perfil_remitente       char(10)        NOT NULL,
    id_requerimiento          char(10)        NOT NULL,
    id_perfil_destinatario    char(10)        NOT NULL
)
;



-- 
-- TABLE: invitacion_gral 
--

CREATE TABLE invitacion_gral(
    id_invitacion_gral        char(10)       NOT NULL,
    mensaje                   varchar(50),
    fecha_envio               timestamp      NOT NULL,
    estado                    varchar(20)    NOT NULL,
    id_perfil_destinatario    char(10)       NOT NULL,
    id_perfil_remitente       char(10)       NOT NULL,
    id_requerimiento          char(10)       NOT NULL
)
;



-- 
-- TABLE: jornada_agenda 
--

CREATE TABLE jornada_agenda(
    id_jornada           char(10)    NOT NULL,
    dia_semana           char(10)    NOT NULL,
    hora_inicio          time        NOT NULL,
    hora_fin_manana      time,
    hora_inicio_tarde    time,
    hora_fin             time        NOT NULL,
    id_agenda            char(10)    NOT NULL
)
;



-- 
-- TABLE: me_gusta 
--

CREATE TABLE me_gusta(
    id_publicacion    char(10),
    id_perfil         char(10),
    fecha             date        NOT NULL
)
;



-- 
-- TABLE: miembros_proyecto 
--

CREATE TABLE miembros_proyecto(
    id_miembro              char(10)       NOT NULL,
    id_rol                  char(10)       NOT NULL,
    estado_participacion    varchar(30)    NOT NULL,
    id_proyecto             char(10)       NOT NULL,
    id_perfil               char(10)       NOT NULL
)
;



-- 
-- TABLE: moodboard 
--

CREATE TABLE moodboard(
    id_moodboard      char(10)        NOT NULL,
    fecha_creacion    timestamp       NOT NULL,
    descripcion       varchar(200),
    id_proyecto       char(10)        NOT NULL
)
;



-- 
-- TABLE: objetivo 
--

CREATE TABLE objetivo(
    id_objetivo    char(10)    NOT NULL,
    nombre         char(10),
    descripcion    char(10),
    id_proyecto    char(10)    NOT NULL
)
;



-- 
-- TABLE: perfil 
--

CREATE TABLE perfil(
    id_perfil            char(10)        NOT NULL,
    fecha_baja_perfil    timestamp,
    estado               varchar(20),
    nombre_artistico     varchar(50),
    biografia            varchar(200),
    id_profesion         char(10)        NOT NULL,
    foto_perfil          char(10)        NOT NULL
)
;



-- 
-- TABLE: permiso_global 
--

CREATE TABLE permiso_global(
    id_permiso    char(10)       NOT NULL,
    nombre        varchar(50)    NOT NULL
)
;



-- 
-- TABLE: permiso_proyecto 
--

CREATE TABLE permiso_proyecto(
    id_permiso    char(10)       NOT NULL,
    nombre        varchar(50)    NOT NULL
)
;



-- 
-- TABLE: planificacion 
--

CREATE TABLE planificacion(
    id_planificacion    char(10)    NOT NULL,
    fecha_entrega       date,
    id_proyecto         char(10)    NOT NULL
)
;



-- 
-- TABLE: postulacion_actividad 
--

CREATE TABLE postulacion_actividad(
    id_postulacion       char(10)       NOT NULL,
    fecha_postulacion    date           NOT NULL,
    estado               varchar(20)    NOT NULL,
    id_perfil            char(10)       NOT NULL,
    id_requerimiento     char(10)       NOT NULL
)
;



-- 
-- TABLE: postulacion_gral 
--

CREATE TABLE postulacion_gral(
    id_postulacion_gral    char(10)       NOT NULL,
    fecha_postulacion      timestamp      NOT NULL,
    estado                 varchar(20)    NOT NULL,
    id_perfil              char(10)       NOT NULL,
    id_requerimiento       char(10)       NOT NULL
)
;



-- 
-- TABLE: profesion 
--

CREATE TABLE profesion(
    id_profesion    char(10)        NOT NULL,
    nombre          varchar(50)     NOT NULL,
    descripcion     varchar(200)
)
;



-- 
-- TABLE: proyecto 
--

CREATE TABLE proyecto(
    id_proyecto                char(10)        NOT NULL,
    fecha_inicio               timestamp       NOT NULL,
    acepta_postulacion_gral    char(10)        NOT NULL,
    nombre                     varchar(50)     NOT NULL,
    descripcion                varchar(200)    NOT NULL,
    estado                     varchar(20)     NOT NULL,
    privacidad                 varchar(20)     NOT NULL,
    id_ubicacion               char(20)        NOT NULL
)
;



-- 
-- TABLE: publicacion 
--

CREATE TABLE publicacion(
    id_publicacion       char(10)        NOT NULL,
    titulo               varchar(150)    NOT NULL,
    descripcion          char(150)       NOT NULL,
    fecha_publicacion    timestamp       NOT NULL,
    id_perfil            char(10)        NOT NULL,
    id_proyecto          char(10)
)
;



-- 
-- TABLE: requerimiento_act_caract 
--

CREATE TABLE requerimiento_act_caract(
    id_requerimiento     char(10),
    id_caracteristica    char(10)
)
;



-- 
-- TABLE: requerimiento_act_habilidad 
--

CREATE TABLE requerimiento_act_habilidad(
    id_requerimiento    char(10),
    id_habilidad        char(10)
)
;



-- 
-- TABLE: requerimiento_actividad 
--

CREATE TABLE requerimiento_actividad(
    id_requerimiento    char(10)        NOT NULL,
    cantidad            int4            NOT NULL,
    descripcion         varchar(200),
    id_profesion        char(10)        NOT NULL,
    id_actividad        varchar(20)     NOT NULL
)
;



-- 
-- TABLE: requerimiento_gral_caract 
--

CREATE TABLE requerimiento_gral_caract(
    id_requerimiento     char(10),
    id_caracteristica    char(10)
)
;



-- 
-- TABLE: requerimiento_gral_habilidad 
--

CREATE TABLE requerimiento_gral_habilidad(
    id_requerimiento    char(10),
    id_habilidad        char(10)
)
;



-- 
-- TABLE: requerimiento_gral_proyecto 
--

CREATE TABLE requerimiento_gral_proyecto(
    id_requerimiento    char(10)        NOT NULL,
    cantidad            int4            NOT NULL,
    descripcion         varchar(200),
    id_proyecto         char(10)        NOT NULL,
    id_profesion        char(10)        NOT NULL
)
;



-- 
-- TABLE: rol_global 
--

CREATE TABLE rol_global(
    id_rol    char(10)       NOT NULL,
    nombre    varchar(20)    NOT NULL
)
;



-- 
-- TABLE: rol_global_permiso 
--

CREATE TABLE rol_global_permiso(
    id_rol_permiso    char(10)    NOT NULL,
    id_rol            char(10)    NOT NULL,
    id_permiso        char(10)    NOT NULL
)
;



-- 
-- TABLE: rol_proyecto 
--

CREATE TABLE rol_proyecto(
    id_rol    char(10)       NOT NULL,
    nombre    varchar(20)    NOT NULL
)
;



-- 
-- TABLE: rol_proyecto_permiso 
--

CREATE TABLE rol_proyecto_permiso(
    id_rol_permiso    char(10)    NOT NULL,
    id_rol            char(10)    NOT NULL,
    id_permiso        char(10)    NOT NULL
)
;



-- 
-- TABLE: solicitud_colaboracion 
--

CREATE TABLE solicitud_colaboracion(
    id_solicitud              char(10)       NOT NULL,
    estado                    varchar(20)    NOT NULL,
    fecha_envio               timestamp      NOT NULL,
    id_publicacion            char(10)       NOT NULL,
    id_perfil_remitente       char(10)       NOT NULL,
    id_perfil_destinatario    char(10)       NOT NULL
)
;



-- 
-- TABLE: ubicacion 
--

CREATE TABLE ubicacion(
    id_ubicacion     char(20)          NOT NULL,
    localidad        varchar(20)       NOT NULL,
    pais             varchar(50),
    provincia        varchar(20)       NOT NULL,
    codigo_postal    varchar(10),
    latitud          decimal(10, 8),
    longitud         decimal(11, 8),
    id_georef        varchar(20)
)
;



-- 
-- TABLE: usuario 
--

CREATE TABLE usuario(
    id_usuario                     char(10)        NOT NULL,
    motivo_deshabilitacion         varchar(100)    NOT NULL,
    fecha_hasta_deshabilitacion    date,
    id_externo                     varchar(20)     NOT NULL,
    proveedor_auth                 varchar(20),
    password_hash                  varchar(20),
    fecha_solicitud_baja           timestamp,
    estado                         varchar(20)     NOT NULL,
    nombre                         varchar(50)     NOT NULL,
    "DNI"                          varchar(15),
    fecha_nacimiento               date            NOT NULL,
    apellido                       varchar(50)     NOT NULL,
    correo                         varchar(255)    NOT NULL,
    id_ubicacion                   char(20)        NOT NULL,
    id_genero                      char(10)        NOT NULL,
    rol_global_id_rol              char(10)        NOT NULL
)
;



-- 
-- TABLE: valor_caracteristica 
--

CREATE TABLE valor_caracteristica(
    id_valor                    char(10)    NOT NULL,
    codigo                      char(10),
    color_hex                   char(10),
    id_caracteristica           char(10)    NOT NULL,
    id_caracteristica_perfil    char(10)    NOT NULL
)
;



-- 
-- INDEX: "Ref1399" 
--

CREATE INDEX "Ref1399" ON actividad(id_ubicacion)
;
-- 
-- INDEX: "Ref1817" 
--

CREATE INDEX "Ref1817" ON actividad(id_planificacion)
;
-- 
-- INDEX: "Ref57146" 
--

CREATE INDEX "Ref57146" ON agenda(id_usuario)
;
-- 
-- INDEX: "Ref896" 
--

CREATE INDEX "Ref896" ON asignacion_actividad(id_miembro)
;
-- 
-- INDEX: "Ref1597" 
--

CREATE INDEX "Ref1597" ON asignacion_actividad(id_actividad)
;
-- 
-- INDEX: "Ref60144" 
--

CREATE INDEX "Ref60144" ON bloqueo_agenda(id_agenda)
;
-- 
-- INDEX: "Ref59" 
--

CREATE INDEX "Ref59" ON caracteristica_perfil(id_perfil)
;
-- 
-- INDEX: "Ref912" 
--

CREATE INDEX "Ref912" ON caracteristica_perfil(id_caracteristica)
;
-- 
-- INDEX: "Ref613" 
--

CREATE INDEX "Ref613" ON caracteristica_tecnica(id_profesion)
;
-- 
-- INDEX: "Ref3493" 
--

CREATE INDEX "Ref3493" ON colaborador_publicacion(id_publicacion)
;
-- 
-- INDEX: "Ref594" 
--

CREATE INDEX "Ref594" ON colaborador_publicacion(id_perfil)
;
-- 
-- INDEX: "Ref3472" 
--

CREATE INDEX "Ref3472" ON comentario(id_publicacion)
;
-- 
-- INDEX: "Ref583" 
--

CREATE INDEX "Ref583" ON comentario(id_perfil)
;
-- 
-- INDEX: "Ref1520" 
--

CREATE INDEX "Ref1520" ON dependencia_actividades(id_actividad_predecesora)
;
-- 
-- INDEX: "Ref1522" 
--

CREATE INDEX "Ref1522" ON dependencia_actividades(id_actividad_sucesora)
;
-- 
-- INDEX: "Ref510" 
--

CREATE INDEX "Ref510" ON habilidad_perfil(id_perfil)
;
-- 
-- INDEX: "Ref1011" 
--

CREATE INDEX "Ref1011" ON habilidad_perfil(id_habilidad)
;
-- 
-- INDEX: "Ref3379" 
--

CREATE INDEX "Ref3379" ON imagen_moodboard(id_imagen)
;
-- 
-- INDEX: "Ref3180" 
--

CREATE INDEX "Ref3180" ON imagen_moodboard(id_moodboard)
;
-- 
-- INDEX: "Ref3377" 
--

CREATE INDEX "Ref3377" ON imagen_publicacion(id_imagen)
;
-- 
-- INDEX: "Ref3478" 
--

CREATE INDEX "Ref3478" ON imagen_publicacion(id_publicacion)
;
-- 
-- INDEX: "Ref5101" 
--

CREATE INDEX "Ref5101" ON invitacion_actividad(id_perfil_remitente)
;
-- 
-- INDEX: "Ref16102" 
--

CREATE INDEX "Ref16102" ON invitacion_actividad(id_requerimiento)
;
-- 
-- INDEX: "Ref5103" 
--

CREATE INDEX "Ref5103" ON invitacion_actividad(id_perfil_destinatario)
;
-- 
-- INDEX: "Ref5125" 
--

CREATE INDEX "Ref5125" ON invitacion_gral(id_perfil_destinatario)
;
-- 
-- INDEX: "Ref5126" 
--

CREATE INDEX "Ref5126" ON invitacion_gral(id_perfil_remitente)
;
-- 
-- INDEX: "Ref47127" 
--

CREATE INDEX "Ref47127" ON invitacion_gral(id_requerimiento)
;
-- 
-- INDEX: "Ref60145" 
--

CREATE INDEX "Ref60145" ON jornada_agenda(id_agenda)
;
-- 
-- INDEX: "Ref582" 
--

CREATE INDEX "Ref582" ON me_gusta(id_perfil)
;
-- 
-- INDEX: "Ref3486" 
--

CREATE INDEX "Ref3486" ON me_gusta(id_publicacion)
;
-- 
-- INDEX: "Ref558" 
--

CREATE INDEX "Ref558" ON miembros_proyecto(id_perfil)
;
-- 
-- INDEX: "Ref44109" 
--

CREATE INDEX "Ref44109" ON miembros_proyecto(id_rol)
;
-- 
-- INDEX: "Ref75" 
--

CREATE INDEX "Ref75" ON miembros_proyecto(id_proyecto)
;
-- 
-- INDEX: "Ref773" 
--

CREATE INDEX "Ref773" ON moodboard(id_proyecto)
;
-- 
-- INDEX: "Ref765" 
--

CREATE INDEX "Ref765" ON objetivo(id_proyecto)
;
-- 
-- INDEX: "Ref695" 
--

CREATE INDEX "Ref695" ON perfil(id_profesion)
;
-- 
-- INDEX: "Ref33129" 
--

CREATE INDEX "Ref33129" ON perfil(foto_perfil)
;
-- 
-- INDEX: "Ref716" 
--

CREATE INDEX "Ref716" ON planificacion(id_proyecto)
;
-- 
-- INDEX: "Ref560" 
--

CREATE INDEX "Ref560" ON postulacion_actividad(id_perfil)
;
-- 
-- INDEX: "Ref1664" 
--

CREATE INDEX "Ref1664" ON postulacion_actividad(id_requerimiento)
;
-- 
-- INDEX: "Ref5123" 
--

CREATE INDEX "Ref5123" ON postulacion_gral(id_perfil)
;
-- 
-- INDEX: "Ref47124" 
--

CREATE INDEX "Ref47124" ON postulacion_gral(id_requerimiento)
;
-- 
-- INDEX: "Ref13130" 
--

CREATE INDEX "Ref13130" ON proyecto(id_ubicacion)
;
-- 
-- INDEX: "Ref584" 
--

CREATE INDEX "Ref584" ON publicacion(id_perfil)
;
-- 
-- INDEX: "Ref785" 
--

CREATE INDEX "Ref785" ON publicacion(id_proyecto)
;
-- 
-- INDEX: "Ref1629" 
--

CREATE INDEX "Ref1629" ON requerimiento_act_caract(id_requerimiento)
;
-- 
-- INDEX: "Ref931" 
--

CREATE INDEX "Ref931" ON requerimiento_act_caract(id_caracteristica)
;
-- 
-- INDEX: "Ref1630" 
--

CREATE INDEX "Ref1630" ON requerimiento_act_habilidad(id_requerimiento)
;
-- 
-- INDEX: "Ref1033" 
--

CREATE INDEX "Ref1033" ON requerimiento_act_habilidad(id_habilidad)
;
-- 
-- INDEX: "Ref666" 
--

CREATE INDEX "Ref666" ON requerimiento_actividad(id_profesion)
;
-- 
-- INDEX: "Ref1568" 
--

CREATE INDEX "Ref1568" ON requerimiento_actividad(id_actividad)
;
-- 
-- INDEX: "Ref9115" 
--

CREATE INDEX "Ref9115" ON requerimiento_gral_caract(id_caracteristica)
;
-- 
-- INDEX: "Ref47118" 
--

CREATE INDEX "Ref47118" ON requerimiento_gral_caract(id_requerimiento)
;
-- 
-- INDEX: "Ref47117" 
--

CREATE INDEX "Ref47117" ON requerimiento_gral_habilidad(id_requerimiento)
;
-- 
-- INDEX: "Ref10119" 
--

CREATE INDEX "Ref10119" ON requerimiento_gral_habilidad(id_habilidad)
;
-- 
-- INDEX: "Ref7113" 
--

CREATE INDEX "Ref7113" ON requerimiento_gral_proyecto(id_proyecto)
;
-- 
-- INDEX: "Ref6114" 
--

CREATE INDEX "Ref6114" ON requerimiento_gral_proyecto(id_profesion)
;
-- 
-- INDEX: "Ref22" 
--

CREATE INDEX "Ref22" ON rol_global_permiso(id_rol)
;
-- 
-- INDEX: "Ref43" 
--

CREATE INDEX "Ref43" ON rol_global_permiso(id_permiso)
;
-- 
-- INDEX: "Ref44106" 
--

CREATE INDEX "Ref44106" ON rol_proyecto_permiso(id_rol)
;
-- 
-- INDEX: "Ref45108" 
--

CREATE INDEX "Ref45108" ON rol_proyecto_permiso(id_permiso)
;
-- 
-- INDEX: "Ref3487" 
--

CREATE INDEX "Ref3487" ON solicitud_colaboracion(id_publicacion)
;
-- 
-- INDEX: "Ref588" 
--

CREATE INDEX "Ref588" ON solicitud_colaboracion(id_perfil_remitente)
;
-- 
-- INDEX: "Ref590" 
--

CREATE INDEX "Ref590" ON solicitud_colaboracion(id_perfil_destinatario)
;
-- 
-- INDEX: "Ref58140" 
--

CREATE INDEX "Ref58140" ON usuario(id_genero)
;
-- 
-- INDEX: "Ref2141" 
--

CREATE INDEX "Ref2141" ON usuario(rol_global_id_rol)
;
-- 
-- INDEX: "Ref13143" 
--

CREATE INDEX "Ref13143" ON usuario(id_ubicacion)
;
-- 
-- INDEX: "Ref12147" 
--

CREATE INDEX "Ref12147" ON valor_caracteristica(id_caracteristica_perfil)
;
-- 
-- INDEX: "Ref9148" 
--

CREATE INDEX "Ref9148" ON valor_caracteristica(id_caracteristica)
;
-- 
-- TABLE: actividad 
--

ALTER TABLE actividad ADD 
    CONSTRAINT "PK15" PRIMARY KEY (id_actividad)
;

-- 
-- TABLE: agenda 
--

ALTER TABLE agenda ADD 
    CONSTRAINT "PK28_1" PRIMARY KEY (id_agenda)
;

-- 
-- TABLE: asignacion_actividad 
--

ALTER TABLE asignacion_actividad ADD 
    CONSTRAINT "PK42" PRIMARY KEY (id_asignacion_act)
;

-- 
-- TABLE: bloqueo_agenda 
--

ALTER TABLE bloqueo_agenda ADD 
    CONSTRAINT "PK29_1" PRIMARY KEY (id_bloqueo)
;

-- 
-- TABLE: caracteristica_perfil 
--

ALTER TABLE caracteristica_perfil ADD 
    CONSTRAINT "PK12" PRIMARY KEY (id_caracteristica_perfil)
;

-- 
-- TABLE: caracteristica_tecnica 
--

ALTER TABLE caracteristica_tecnica ADD 
    CONSTRAINT "PK9" PRIMARY KEY (id_caracteristica)
;

-- 
-- TABLE: comentario 
--

ALTER TABLE comentario ADD 
    CONSTRAINT "PK35" PRIMARY KEY (id_comentario)
;

-- 
-- TABLE: genero 
--

ALTER TABLE genero ADD 
    CONSTRAINT "PK56" PRIMARY KEY (id_genero)
;

-- 
-- TABLE: habilidad 
--

ALTER TABLE habilidad ADD 
    CONSTRAINT "PK10" PRIMARY KEY (id_habilidad)
;

-- 
-- TABLE: imagen 
--

ALTER TABLE imagen ADD 
    CONSTRAINT "PK33" PRIMARY KEY (id_imagen)
;

-- 
-- TABLE: invitacion_actividad 
--

ALTER TABLE invitacion_actividad ADD 
    CONSTRAINT "PK43" PRIMARY KEY (id_invitacion)
;

-- 
-- TABLE: invitacion_gral 
--

ALTER TABLE invitacion_gral ADD 
    CONSTRAINT "PK53" PRIMARY KEY (id_invitacion_gral)
;

-- 
-- TABLE: jornada_agenda 
--

ALTER TABLE jornada_agenda ADD 
    CONSTRAINT "PK59" PRIMARY KEY (id_jornada)
;

-- 
-- TABLE: miembros_proyecto 
--

ALTER TABLE miembros_proyecto ADD 
    CONSTRAINT "PK8" PRIMARY KEY (id_miembro)
;

-- 
-- TABLE: moodboard 
--

ALTER TABLE moodboard ADD 
    CONSTRAINT "PK31" PRIMARY KEY (id_moodboard)
;

-- 
-- TABLE: objetivo 
--

ALTER TABLE objetivo ADD 
    CONSTRAINT "PK30" PRIMARY KEY (id_objetivo)
;

-- 
-- TABLE: perfil 
--

ALTER TABLE perfil ADD 
    CONSTRAINT "PK5" PRIMARY KEY (id_perfil)
;

-- 
-- TABLE: permiso_global 
--

ALTER TABLE permiso_global ADD 
    CONSTRAINT "PK4" PRIMARY KEY (id_permiso)
;

-- 
-- TABLE: permiso_proyecto 
--

ALTER TABLE permiso_proyecto ADD 
    CONSTRAINT "PK4_1" PRIMARY KEY (id_permiso)
;

-- 
-- TABLE: planificacion 
--

ALTER TABLE planificacion ADD 
    CONSTRAINT "PK18" PRIMARY KEY (id_planificacion)
;

-- 
-- TABLE: postulacion_actividad 
--

ALTER TABLE postulacion_actividad ADD 
    CONSTRAINT "PK27" PRIMARY KEY (id_postulacion)
;

-- 
-- TABLE: postulacion_gral 
--

ALTER TABLE postulacion_gral ADD 
    CONSTRAINT "PK52" PRIMARY KEY (id_postulacion_gral)
;

-- 
-- TABLE: profesion 
--

ALTER TABLE profesion ADD 
    CONSTRAINT "PK6" PRIMARY KEY (id_profesion)
;

-- 
-- TABLE: proyecto 
--

ALTER TABLE proyecto ADD 
    CONSTRAINT "PK7" PRIMARY KEY (id_proyecto)
;

-- 
-- TABLE: publicacion 
--

ALTER TABLE publicacion ADD 
    CONSTRAINT "PK34" PRIMARY KEY (id_publicacion)
;

-- 
-- TABLE: requerimiento_actividad 
--

ALTER TABLE requerimiento_actividad ADD 
    CONSTRAINT "PK16" PRIMARY KEY (id_requerimiento)
;

-- 
-- TABLE: requerimiento_gral_proyecto 
--

ALTER TABLE requerimiento_gral_proyecto ADD 
    CONSTRAINT "PK16_1" PRIMARY KEY (id_requerimiento)
;

-- 
-- TABLE: rol_global 
--

ALTER TABLE rol_global ADD 
    CONSTRAINT "PK2" PRIMARY KEY (id_rol)
;

-- 
-- TABLE: rol_global_permiso 
--

ALTER TABLE rol_global_permiso ADD 
    CONSTRAINT "PK3" PRIMARY KEY (id_rol_permiso)
;

-- 
-- TABLE: rol_proyecto 
--

ALTER TABLE rol_proyecto ADD 
    CONSTRAINT "PK2_2" PRIMARY KEY (id_rol)
;

-- 
-- TABLE: rol_proyecto_permiso 
--

ALTER TABLE rol_proyecto_permiso ADD 
    CONSTRAINT "PK3_1" PRIMARY KEY (id_rol_permiso)
;

-- 
-- TABLE: solicitud_colaboracion 
--

ALTER TABLE solicitud_colaboracion ADD 
    CONSTRAINT "PK37" PRIMARY KEY (id_solicitud)
;

-- 
-- TABLE: ubicacion 
--

ALTER TABLE ubicacion ADD 
    CONSTRAINT "PK13" PRIMARY KEY (id_ubicacion)
;

-- 
-- TABLE: usuario 
--

ALTER TABLE usuario ADD 
    CONSTRAINT "PK1_1" PRIMARY KEY (id_usuario)
;

-- 
-- TABLE: valor_caracteristica 
--

ALTER TABLE valor_caracteristica ADD 
    CONSTRAINT "PK54" PRIMARY KEY (id_valor)
;

-- 
-- TABLE: actividad 
--

ALTER TABLE actividad ADD CONSTRAINT "Refubicacion991" 
    FOREIGN KEY (id_ubicacion)
    REFERENCES ubicacion(id_ubicacion)
;

ALTER TABLE actividad ADD CONSTRAINT "Refplanificacion171" 
    FOREIGN KEY (id_planificacion)
    REFERENCES planificacion(id_planificacion)
;


-- 
-- TABLE: agenda 
--

ALTER TABLE agenda ADD CONSTRAINT "Refusuario1461" 
    FOREIGN KEY (id_usuario)
    REFERENCES usuario(id_usuario)
;


-- 
-- TABLE: asignacion_actividad 
--

ALTER TABLE asignacion_actividad ADD CONSTRAINT "Refmiembros_proyecto961" 
    FOREIGN KEY (id_miembro)
    REFERENCES miembros_proyecto(id_miembro)
;

ALTER TABLE asignacion_actividad ADD CONSTRAINT "Refactividad971" 
    FOREIGN KEY (id_actividad)
    REFERENCES actividad(id_actividad)
;


-- 
-- TABLE: bloqueo_agenda 
--

ALTER TABLE bloqueo_agenda ADD CONSTRAINT "Refagenda1441" 
    FOREIGN KEY (id_agenda)
    REFERENCES agenda(id_agenda)
;


-- 
-- TABLE: caracteristica_perfil 
--

ALTER TABLE caracteristica_perfil ADD CONSTRAINT "Refperfil91" 
    FOREIGN KEY (id_perfil)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE caracteristica_perfil ADD CONSTRAINT "Refcaracteristica_tecnica121" 
    FOREIGN KEY (id_caracteristica)
    REFERENCES caracteristica_tecnica(id_caracteristica)
;


-- 
-- TABLE: caracteristica_tecnica 
--

ALTER TABLE caracteristica_tecnica ADD CONSTRAINT "Refprofesion131" 
    FOREIGN KEY (id_profesion)
    REFERENCES profesion(id_profesion)
;


-- 
-- TABLE: colaborador_publicacion 
--

ALTER TABLE colaborador_publicacion ADD CONSTRAINT "Refpublicacion931" 
    FOREIGN KEY (id_publicacion)
    REFERENCES publicacion(id_publicacion)
;

ALTER TABLE colaborador_publicacion ADD CONSTRAINT "Refperfil941" 
    FOREIGN KEY (id_perfil)
    REFERENCES perfil(id_perfil)
;


-- 
-- TABLE: comentario 
--

ALTER TABLE comentario ADD CONSTRAINT "Refpublicacion721" 
    FOREIGN KEY (id_publicacion)
    REFERENCES publicacion(id_publicacion)
;

ALTER TABLE comentario ADD CONSTRAINT "Refperfil831" 
    FOREIGN KEY (id_perfil)
    REFERENCES perfil(id_perfil)
;


-- 
-- TABLE: dependencia_actividades 
--

ALTER TABLE dependencia_actividades ADD CONSTRAINT "Refactividad201" 
    FOREIGN KEY (id_actividad_predecesora)
    REFERENCES actividad(id_actividad)
;

ALTER TABLE dependencia_actividades ADD CONSTRAINT "Refactividad221" 
    FOREIGN KEY (id_actividad_sucesora)
    REFERENCES actividad(id_actividad)
;


-- 
-- TABLE: habilidad_perfil 
--

ALTER TABLE habilidad_perfil ADD CONSTRAINT "Refperfil102" 
    FOREIGN KEY (id_perfil)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE habilidad_perfil ADD CONSTRAINT "Refhabilidad111" 
    FOREIGN KEY (id_habilidad)
    REFERENCES habilidad(id_habilidad)
;


-- 
-- TABLE: imagen_moodboard 
--

ALTER TABLE imagen_moodboard ADD CONSTRAINT "Refimagen791" 
    FOREIGN KEY (id_imagen)
    REFERENCES imagen(id_imagen)
;

ALTER TABLE imagen_moodboard ADD CONSTRAINT "Refmoodboard801" 
    FOREIGN KEY (id_moodboard)
    REFERENCES moodboard(id_moodboard)
;


-- 
-- TABLE: imagen_publicacion 
--

ALTER TABLE imagen_publicacion ADD CONSTRAINT "Refimagen771" 
    FOREIGN KEY (id_imagen)
    REFERENCES imagen(id_imagen)
;

ALTER TABLE imagen_publicacion ADD CONSTRAINT "Refpublicacion781" 
    FOREIGN KEY (id_publicacion)
    REFERENCES publicacion(id_publicacion)
;


-- 
-- TABLE: invitacion_actividad 
--

ALTER TABLE invitacion_actividad ADD CONSTRAINT "Refperfil1011" 
    FOREIGN KEY (id_perfil_remitente)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE invitacion_actividad ADD CONSTRAINT "Refrequerimiento_actividad1021" 
    FOREIGN KEY (id_requerimiento)
    REFERENCES requerimiento_actividad(id_requerimiento)
;

ALTER TABLE invitacion_actividad ADD CONSTRAINT "Refperfil1031" 
    FOREIGN KEY (id_perfil_destinatario)
    REFERENCES perfil(id_perfil)
;


-- 
-- TABLE: invitacion_gral 
--

ALTER TABLE invitacion_gral ADD CONSTRAINT "Refperfil1251" 
    FOREIGN KEY (id_perfil_destinatario)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE invitacion_gral ADD CONSTRAINT "Refperfil1261" 
    FOREIGN KEY (id_perfil_remitente)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE invitacion_gral ADD CONSTRAINT "Refrequerimiento_gral_proyecto1271" 
    FOREIGN KEY (id_requerimiento)
    REFERENCES requerimiento_gral_proyecto(id_requerimiento)
;


-- 
-- TABLE: jornada_agenda 
--

ALTER TABLE jornada_agenda ADD CONSTRAINT "Refagenda1451" 
    FOREIGN KEY (id_agenda)
    REFERENCES agenda(id_agenda)
;


-- 
-- TABLE: me_gusta 
--

ALTER TABLE me_gusta ADD CONSTRAINT "Refperfil821" 
    FOREIGN KEY (id_perfil)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE me_gusta ADD CONSTRAINT "Refpublicacion861" 
    FOREIGN KEY (id_publicacion)
    REFERENCES publicacion(id_publicacion)
;


-- 
-- TABLE: miembros_proyecto 
--

ALTER TABLE miembros_proyecto ADD CONSTRAINT "Refperfil581" 
    FOREIGN KEY (id_perfil)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE miembros_proyecto ADD CONSTRAINT "Refrol_proyecto1091" 
    FOREIGN KEY (id_rol)
    REFERENCES rol_proyecto(id_rol)
;

ALTER TABLE miembros_proyecto ADD CONSTRAINT "Refproyecto51" 
    FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto)
;


-- 
-- TABLE: moodboard 
--

ALTER TABLE moodboard ADD CONSTRAINT "Refproyecto731" 
    FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto)
;


-- 
-- TABLE: objetivo 
--

ALTER TABLE objetivo ADD CONSTRAINT "Refproyecto651" 
    FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto)
;


-- 
-- TABLE: perfil 
--

ALTER TABLE perfil ADD CONSTRAINT "Refprofesion951" 
    FOREIGN KEY (id_profesion)
    REFERENCES profesion(id_profesion)
;

ALTER TABLE perfil ADD CONSTRAINT "Refimagen1291" 
    FOREIGN KEY (foto_perfil)
    REFERENCES imagen(id_imagen)
;


-- 
-- TABLE: planificacion 
--

ALTER TABLE planificacion ADD CONSTRAINT "Refproyecto161" 
    FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto)
;


-- 
-- TABLE: postulacion_actividad 
--

ALTER TABLE postulacion_actividad ADD CONSTRAINT "Refperfil601" 
    FOREIGN KEY (id_perfil)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE postulacion_actividad ADD CONSTRAINT "Refrequerimiento_actividad641" 
    FOREIGN KEY (id_requerimiento)
    REFERENCES requerimiento_actividad(id_requerimiento)
;


-- 
-- TABLE: postulacion_gral 
--

ALTER TABLE postulacion_gral ADD CONSTRAINT "Refperfil1231" 
    FOREIGN KEY (id_perfil)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE postulacion_gral ADD CONSTRAINT "Refrequerimiento_gral_proyecto1241" 
    FOREIGN KEY (id_requerimiento)
    REFERENCES requerimiento_gral_proyecto(id_requerimiento)
;


-- 
-- TABLE: proyecto 
--

ALTER TABLE proyecto ADD CONSTRAINT "Refubicacion1301" 
    FOREIGN KEY (id_ubicacion)
    REFERENCES ubicacion(id_ubicacion)
;


-- 
-- TABLE: publicacion 
--

ALTER TABLE publicacion ADD CONSTRAINT "Refperfil841" 
    FOREIGN KEY (id_perfil)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE publicacion ADD CONSTRAINT "Refproyecto851" 
    FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto)
;


-- 
-- TABLE: requerimiento_act_caract 
--

ALTER TABLE requerimiento_act_caract ADD CONSTRAINT "Refrequerimiento_actividad291" 
    FOREIGN KEY (id_requerimiento)
    REFERENCES requerimiento_actividad(id_requerimiento)
;

ALTER TABLE requerimiento_act_caract ADD CONSTRAINT "Refcaracteristica_tecnica311" 
    FOREIGN KEY (id_caracteristica)
    REFERENCES caracteristica_tecnica(id_caracteristica)
;


-- 
-- TABLE: requerimiento_act_habilidad 
--

ALTER TABLE requerimiento_act_habilidad ADD CONSTRAINT "Refrequerimiento_actividad301" 
    FOREIGN KEY (id_requerimiento)
    REFERENCES requerimiento_actividad(id_requerimiento)
;

ALTER TABLE requerimiento_act_habilidad ADD CONSTRAINT "Refhabilidad331" 
    FOREIGN KEY (id_habilidad)
    REFERENCES habilidad(id_habilidad)
;


-- 
-- TABLE: requerimiento_actividad 
--

ALTER TABLE requerimiento_actividad ADD CONSTRAINT "Refprofesion661" 
    FOREIGN KEY (id_profesion)
    REFERENCES profesion(id_profesion)
;

ALTER TABLE requerimiento_actividad ADD CONSTRAINT "Refactividad681" 
    FOREIGN KEY (id_actividad)
    REFERENCES actividad(id_actividad)
;


-- 
-- TABLE: requerimiento_gral_caract 
--

ALTER TABLE requerimiento_gral_caract ADD CONSTRAINT "Refcaracteristica_tecnica1151" 
    FOREIGN KEY (id_caracteristica)
    REFERENCES caracteristica_tecnica(id_caracteristica)
;

ALTER TABLE requerimiento_gral_caract ADD CONSTRAINT "Refrequerimiento_gral_proyecto1181" 
    FOREIGN KEY (id_requerimiento)
    REFERENCES requerimiento_gral_proyecto(id_requerimiento)
;


-- 
-- TABLE: requerimiento_gral_habilidad 
--

ALTER TABLE requerimiento_gral_habilidad ADD CONSTRAINT "Refrequerimiento_gral_proyecto1171" 
    FOREIGN KEY (id_requerimiento)
    REFERENCES requerimiento_gral_proyecto(id_requerimiento)
;

ALTER TABLE requerimiento_gral_habilidad ADD CONSTRAINT "Refhabilidad1191" 
    FOREIGN KEY (id_habilidad)
    REFERENCES habilidad(id_habilidad)
;


-- 
-- TABLE: requerimiento_gral_proyecto 
--

ALTER TABLE requerimiento_gral_proyecto ADD CONSTRAINT "Refproyecto1131" 
    FOREIGN KEY (id_proyecto)
    REFERENCES proyecto(id_proyecto)
;

ALTER TABLE requerimiento_gral_proyecto ADD CONSTRAINT "Refprofesion1141" 
    FOREIGN KEY (id_profesion)
    REFERENCES profesion(id_profesion)
;


-- 
-- TABLE: rol_global_permiso 
--

ALTER TABLE rol_global_permiso ADD CONSTRAINT "Refrol_global21" 
    FOREIGN KEY (id_rol)
    REFERENCES rol_global(id_rol)
;

ALTER TABLE rol_global_permiso ADD CONSTRAINT "Refpermiso_global31" 
    FOREIGN KEY (id_permiso)
    REFERENCES permiso_global(id_permiso)
;


-- 
-- TABLE: rol_proyecto_permiso 
--

ALTER TABLE rol_proyecto_permiso ADD CONSTRAINT "Refrol_proyecto1061" 
    FOREIGN KEY (id_rol)
    REFERENCES rol_proyecto(id_rol)
;

ALTER TABLE rol_proyecto_permiso ADD CONSTRAINT "Refpermiso_proyecto1081" 
    FOREIGN KEY (id_permiso)
    REFERENCES permiso_proyecto(id_permiso)
;


-- 
-- TABLE: solicitud_colaboracion 
--

ALTER TABLE solicitud_colaboracion ADD CONSTRAINT "Refpublicacion871" 
    FOREIGN KEY (id_publicacion)
    REFERENCES publicacion(id_publicacion)
;

ALTER TABLE solicitud_colaboracion ADD CONSTRAINT "Refperfil881" 
    FOREIGN KEY (id_perfil_remitente)
    REFERENCES perfil(id_perfil)
;

ALTER TABLE solicitud_colaboracion ADD CONSTRAINT "Refperfil901" 
    FOREIGN KEY (id_perfil_destinatario)
    REFERENCES perfil(id_perfil)
;


-- 
-- TABLE: usuario 
--

ALTER TABLE usuario ADD CONSTRAINT "Refgenero1401" 
    FOREIGN KEY (id_genero)
    REFERENCES genero(id_genero)
;

ALTER TABLE usuario ADD CONSTRAINT "Refrol_global1411" 
    FOREIGN KEY (rol_global_id_rol)
    REFERENCES rol_global(id_rol)
;

ALTER TABLE usuario ADD CONSTRAINT "Refubicacion1431" 
    FOREIGN KEY (id_ubicacion)
    REFERENCES ubicacion(id_ubicacion)
;


-- 
-- TABLE: valor_caracteristica 
--

ALTER TABLE valor_caracteristica ADD CONSTRAINT "Refcaracteristica_perfil1471" 
    FOREIGN KEY (id_caracteristica_perfil)
    REFERENCES caracteristica_perfil(id_caracteristica_perfil)
;

ALTER TABLE valor_caracteristica ADD CONSTRAINT "Refcaracteristica_tecnica1481" 
    FOREIGN KEY (id_caracteristica)
    REFERENCES caracteristica_tecnica(id_caracteristica)
;

