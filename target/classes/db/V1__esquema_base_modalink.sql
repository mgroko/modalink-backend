-- =====================================================================
-- ModaLink - Esquema base MVP (Primera iteración: perfiles + proyectos)
-- Target: PostgreSQL 16+
-- Migración Flyway: V1__esquema_base_modalink.sql
--
-- Alcance: gestión de usuarios, perfiles, roles/permisos, agenda,
-- proyectos, planificación, actividades, requerimientos, postulaciones
-- e invitaciones. Quedan fuera de esta migración (segunda iteración):
-- publicacion, comentario, me_gusta, colaborador_publicacion,
-- solicitud_colaboracion, imagen_publicacion.
-- =====================================================================


-- =====================================================================
-- MÓDULO: Ubicación (reutilizada por usuario, proyecto y actividad)
-- =====================================================================

CREATE TABLE ubicacion(
    id_ubicacion     BIGSERIAL PRIMARY KEY,
    localidad        VARCHAR(100)     NOT NULL,
    pais             VARCHAR(50),
    provincia        VARCHAR(100)     NOT NULL,
    codigo_postal    VARCHAR(10),
    latitud          DECIMAL(10, 8),
    longitud         DECIMAL(11, 8)
);


-- =====================================================================
-- MÓDULO: Roles y permisos
-- =====================================================================

CREATE TABLE rol_global(
    id_rol_global    BIGSERIAL PRIMARY KEY,
    nombre           VARCHAR(20)    NOT NULL,
    CONSTRAINT uq_rol_global_nombre UNIQUE (nombre)
);

CREATE TABLE rol_proyecto(
    id_rol_proyecto    BIGSERIAL PRIMARY KEY,
    nombre             VARCHAR(20)    NOT NULL,
    CONSTRAINT uq_rol_proyecto_nombre UNIQUE (nombre)
);

CREATE TABLE permiso_global(
    id_permiso_global    BIGSERIAL PRIMARY KEY,
    nombre               VARCHAR(50)    NOT NULL,
    CONSTRAINT uq_permiso_global_nombre UNIQUE (nombre)
);

CREATE TABLE permiso_proyecto(
    id_permiso_proyecto    BIGSERIAL PRIMARY KEY,
    nombre                  VARCHAR(50)    NOT NULL,
    CONSTRAINT uq_permiso_proyecto_nombre UNIQUE (nombre)
);


-- =====================================================================
-- MÓDULO: Usuario
-- =====================================================================

CREATE TABLE usuario(
    id_usuario              BIGSERIAL PRIMARY KEY,
    nombre                  VARCHAR(50)     NOT NULL,
    apellido                VARCHAR(50)     NOT NULL,
    dni                     VARCHAR(15)     NOT NULL,
    fecha_nacimiento        DATE            NOT NULL,
    correo                  VARCHAR(255)    NOT NULL,
    password_hash           VARCHAR(255),                      
    proveedor_auth          VARCHAR(20)     NOT NULL DEFAULT 'LOCAL',
    id_externo              VARCHAR(100),                     
    estado                  VARCHAR(20)     NOT NULL DEFAULT 'Activo',
    fecha_solicitud_baja    TIMESTAMP,
    id_ubicacion            BIGINT REFERENCES ubicacion(id_ubicacion),
    id_rol_global           BIGINT          NOT NULL REFERENCES rol_global(id_rol_global),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT uq_usuario_dni UNIQUE (dni),
    CONSTRAINT chk_usuario_estado CHECK (estado IN ('Activo', 'Deshabilitado', 'Inactivo', 'Baja')),
    CONSTRAINT chk_usuario_proveedor CHECK (proveedor_auth IN ('LOCAL', 'GOOGLE'))
);

CREATE TABLE rol_global_permiso(
    id_rol_global        BIGINT NOT NULL REFERENCES rol_global(id_rol_global),
    id_permiso_global    BIGINT NOT NULL REFERENCES permiso_global(id_permiso_global),
    PRIMARY KEY (id_rol_global, id_permiso_global)
);

CREATE TABLE rol_proyecto_permiso(
    id_rol_proyecto        BIGINT NOT NULL REFERENCES rol_proyecto(id_rol_proyecto),
    id_permiso_proyecto    BIGINT NOT NULL REFERENCES permiso_proyecto(id_permiso_proyecto),
    PRIMARY KEY (id_rol_proyecto, id_permiso_proyecto)
);


-- =====================================================================
-- MÓDULO: Agenda y bloqueos
-- =====================================================================

CREATE TABLE agenda(
    id_agenda             BIGSERIAL PRIMARY KEY,
    dias_laborales        VARCHAR(100)    NOT NULL,
    hora_inicio_jornada   TIME            NOT NULL,
    hora_fin_jornada      TIME            NOT NULL,
    id_usuario            BIGINT          NOT NULL REFERENCES usuario(id_usuario),
    CONSTRAINT uq_agenda_usuario UNIQUE (id_usuario)
);

CREATE TABLE bloqueo_agenda(
    id_bloqueo           BIGSERIAL PRIMARY KEY,
    fecha_hora_inicio    TIMESTAMP    NOT NULL,
    fecha_hora_fin       TIMESTAMP    NOT NULL,
    motivo               VARCHAR(200),
    id_agenda            BIGINT       NOT NULL REFERENCES agenda(id_agenda),
    CONSTRAINT chk_bloqueo_rango CHECK (fecha_hora_fin > fecha_hora_inicio)
);


-- =====================================================================
-- MÓDULO: Perfil profesional
-- =====================================================================

CREATE TABLE profesion(
    id_profesion    BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(50)     NOT NULL,
    descripcion     VARCHAR(200),
    CONSTRAINT uq_profesion_nombre UNIQUE (nombre)
);

CREATE TABLE caracteristica_tecnica(
    id_caracteristica    BIGSERIAL PRIMARY KEY,
    nombre               VARCHAR(100)    NOT NULL,
    descripcion          VARCHAR(200),
    id_profesion         BIGINT REFERENCES profesion(id_profesion)
);

CREATE TABLE habilidad(
    id_habilidad    BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(50)     NOT NULL,
    descripcion     VARCHAR(200),
    CONSTRAINT uq_habilidad_nombre UNIQUE (nombre)
);

CREATE TABLE imagen(
    id_imagen         BIGSERIAL PRIMARY KEY,
    url               VARCHAR(1024)    NOT NULL,
    estado            VARCHAR(20),
    nombre_archivo    VARCHAR(150)     NOT NULL,
    tipo_imagen       VARCHAR(20)      NOT NULL,
    tamano_bytes      INT              NOT NULL,
    fecha_subida      TIMESTAMP        NOT NULL DEFAULT now()
);

CREATE TABLE perfil(
    id_perfil               BIGSERIAL PRIMARY KEY,
    nombre_artistico        VARCHAR(50)     NOT NULL,
    biografia               VARCHAR(500)    NOT NULL,
    estado                  VARCHAR(20)     NOT NULL DEFAULT 'Activo',
    fecha_solicitud_baja    TIMESTAMP,
    id_usuario              BIGINT          NOT NULL REFERENCES usuario(id_usuario),
    id_profesion            BIGINT          NOT NULL REFERENCES profesion(id_profesion),
    id_imagen               BIGINT REFERENCES imagen(id_imagen),   -- foto de perfil, opcional
    CONSTRAINT chk_perfil_estado CHECK (estado IN ('Activo', 'PendienteBaja', 'Baja'))
);

CREATE TABLE caracteristica_perfil(
    id_perfil            BIGINT NOT NULL REFERENCES perfil(id_perfil),
    id_caracteristica    BIGINT NOT NULL REFERENCES caracteristica_tecnica(id_caracteristica),
    PRIMARY KEY (id_perfil, id_caracteristica)
);

CREATE TABLE habilidad_perfil(
    id_perfil       BIGINT NOT NULL REFERENCES perfil(id_perfil),
    id_habilidad    BIGINT NOT NULL REFERENCES habilidad(id_habilidad),
    PRIMARY KEY (id_perfil, id_habilidad)
);


-- =====================================================================
-- MÓDULO: Proyecto
-- =====================================================================

CREATE TABLE proyecto(
    id_proyecto                   BIGSERIAL PRIMARY KEY,
    nombre                        VARCHAR(50)     NOT NULL,
    descripcion                   VARCHAR(200)    NOT NULL,
    fecha_inicio                  DATE            NOT NULL,
    estado                        VARCHAR(20)     NOT NULL DEFAULT 'Borrador',
    privacidad                    VARCHAR(20)     NOT NULL,
    acepta_postulacion_gral       BOOLEAN         NOT NULL DEFAULT false,
    id_ubicacion                  BIGINT REFERENCES ubicacion(id_ubicacion),
    CONSTRAINT chk_proyecto_estado CHECK (estado IN ('Borrador', 'Publicado', 'Confirmado', 'Finalizado', 'Cancelado')),
    CONSTRAINT chk_proyecto_privacidad CHECK (privacidad IN ('Publico', 'Privado', 'Oculto'))
);

CREATE TABLE objetivo(
    id_objetivo    BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(100),
    descripcion    VARCHAR(300),
    id_proyecto    BIGINT NOT NULL REFERENCES proyecto(id_proyecto)
);

CREATE TABLE moodboard(
    id_moodboard      BIGSERIAL PRIMARY KEY,
    fecha_creacion    TIMESTAMP       NOT NULL DEFAULT now(),
    descripcion       VARCHAR(300),
    id_proyecto       BIGINT          NOT NULL REFERENCES proyecto(id_proyecto),
    CONSTRAINT uq_moodboard_proyecto UNIQUE (id_proyecto)  
);

CREATE TABLE imagen_moodboard(
    id_imagen       BIGINT NOT NULL REFERENCES imagen(id_imagen),
    id_moodboard    BIGINT NOT NULL REFERENCES moodboard(id_moodboard),
    PRIMARY KEY (id_imagen, id_moodboard)
);


-- =====================================================================
-- MÓDULO: Miembros de proyecto
-- =====================================================================

CREATE TABLE miembros_proyecto(
    id_miembro              BIGSERIAL PRIMARY KEY,
    estado_participacion    VARCHAR(30)    NOT NULL DEFAULT 'Activo',
    id_proyecto             BIGINT         NOT NULL REFERENCES proyecto(id_proyecto),
    id_perfil               BIGINT         NOT NULL REFERENCES perfil(id_perfil),
    id_rol_proyecto         BIGINT         NOT NULL REFERENCES rol_proyecto(id_rol_proyecto),
    CONSTRAINT uq_miembro_proyecto_perfil UNIQUE (id_proyecto, id_perfil),
    CONSTRAINT chk_estado_participacion CHECK (estado_participacion IN ('Activo', 'BajaVoluntaria', 'Eliminado'))
);


-- =====================================================================
-- MÓDULO: Planificación y actividades
-- =====================================================================

CREATE TABLE planificacion(
    id_planificacion    BIGSERIAL PRIMARY KEY,
    fecha_entrega        DATE,
    id_proyecto          BIGINT NOT NULL REFERENCES proyecto(id_proyecto),
    CONSTRAINT uq_planificacion_proyecto UNIQUE (id_proyecto)
);

CREATE TABLE actividad(
    id_actividad         BIGSERIAL PRIMARY KEY,
    nombre               VARCHAR(100)    NOT NULL,
    descripcion          VARCHAR(200),
    duracion_minutos     INT             NOT NULL,
    fecha_hora_inicio    TIMESTAMP       NOT NULL,
    fecha_hora_fin       TIMESTAMP GENERATED ALWAYS AS
                             (fecha_hora_inicio + (duracion_minutos * INTERVAL '1 minute')) STORED,
    id_planificacion     BIGINT          NOT NULL REFERENCES planificacion(id_planificacion),
    id_ubicacion         BIGINT REFERENCES ubicacion(id_ubicacion),
    CONSTRAINT chk_actividad_duracion CHECK (duracion_minutos > 0)
);

CREATE TABLE dependencia_actividades(
    id_actividad_predecesora    BIGINT NOT NULL REFERENCES actividad(id_actividad),
    id_actividad_sucesora       BIGINT NOT NULL REFERENCES actividad(id_actividad),
    PRIMARY KEY (id_actividad_predecesora, id_actividad_sucesora),
    CONSTRAINT chk_dependencia_no_autoreferencia CHECK (id_actividad_predecesora <> id_actividad_sucesora)
);

CREATE TABLE asignacion_actividad(
    id_asignacion_act    BIGSERIAL PRIMARY KEY,
    id_miembro           BIGINT NOT NULL REFERENCES miembros_proyecto(id_miembro),
    id_actividad         BIGINT NOT NULL REFERENCES actividad(id_actividad),
    CONSTRAINT uq_asignacion_miembro_actividad UNIQUE (id_miembro, id_actividad)
);


-- =====================================================================
-- MÓDULO: Requerimientos (de actividad y generales de proyecto)
-- Con diseño de tablas separadas (actividades y generales)
-- =====================================================================

CREATE TABLE requerimiento_actividad(
    id_requerimiento    BIGSERIAL PRIMARY KEY,
    cantidad             INT             NOT NULL,
    descripcion          VARCHAR(200),
    id_profesion         BIGINT          NOT NULL REFERENCES profesion(id_profesion),
    id_actividad         BIGINT          NOT NULL REFERENCES actividad(id_actividad),
    CONSTRAINT chk_requerimiento_act_cantidad CHECK (cantidad > 0)
);

CREATE TABLE requerimiento_act_habilidad(
    id_requerimiento    BIGINT NOT NULL REFERENCES requerimiento_actividad(id_requerimiento),
    id_habilidad         BIGINT NOT NULL REFERENCES habilidad(id_habilidad),
    PRIMARY KEY (id_requerimiento, id_habilidad)
);

CREATE TABLE requerimiento_act_caract(
    id_requerimiento     BIGINT NOT NULL REFERENCES requerimiento_actividad(id_requerimiento),
    id_caracteristica    BIGINT NOT NULL REFERENCES caracteristica_tecnica(id_caracteristica),
    PRIMARY KEY (id_requerimiento, id_caracteristica)
);

CREATE TABLE requerimiento_gral_proyecto(
    id_requerimiento_gral    BIGSERIAL PRIMARY KEY,
    cantidad                  INT             NOT NULL,
    descripcion               VARCHAR(200),
    id_proyecto               BIGINT          NOT NULL REFERENCES proyecto(id_proyecto),
    id_profesion              BIGINT          NOT NULL REFERENCES profesion(id_profesion),
    CONSTRAINT chk_requerimiento_gral_cantidad CHECK (cantidad > 0)
);

CREATE TABLE requerimiento_gral_habilidad(
    id_requerimiento_gral    BIGINT NOT NULL REFERENCES requerimiento_gral_proyecto(id_requerimiento_gral),
    id_habilidad              BIGINT NOT NULL REFERENCES habilidad(id_habilidad),
    PRIMARY KEY (id_requerimiento_gral, id_habilidad)
);

CREATE TABLE requerimiento_gral_caract(
    id_requerimiento_gral    BIGINT NOT NULL REFERENCES requerimiento_gral_proyecto(id_requerimiento_gral),
    id_caracteristica         BIGINT NOT NULL REFERENCES caracteristica_tecnica(id_caracteristica),
    PRIMARY KEY (id_requerimiento_gral, id_caracteristica)
);


-- =====================================================================
-- MÓDULO: Postulaciones e invitaciones
-- =====================================================================

CREATE TABLE postulacion_actividad(
    id_postulacion       BIGSERIAL PRIMARY KEY,
    fecha_postulacion    TIMESTAMP      NOT NULL DEFAULT now(),
    estado               VARCHAR(20)    NOT NULL DEFAULT 'Pendiente',
    id_perfil            BIGINT         NOT NULL REFERENCES perfil(id_perfil),
    id_requerimiento     BIGINT         NOT NULL REFERENCES requerimiento_actividad(id_requerimiento),
    CONSTRAINT uq_postulacion_act_perfil_req UNIQUE (id_perfil, id_requerimiento),
    CONSTRAINT chk_postulacion_act_estado CHECK (estado IN ('Pendiente', 'Aceptada', 'Rechazada'))
);

CREATE TABLE postulacion_gral(
    id_postulacion_gral    BIGSERIAL PRIMARY KEY,
    fecha_postulacion       TIMESTAMP      NOT NULL DEFAULT now(),
    estado                  VARCHAR(20)    NOT NULL DEFAULT 'Pendiente',
    id_perfil                BIGINT         NOT NULL REFERENCES perfil(id_perfil),
    id_requerimiento_gral    BIGINT         NOT NULL REFERENCES requerimiento_gral_proyecto(id_requerimiento_gral),
    CONSTRAINT uq_postulacion_gral_perfil_req UNIQUE (id_perfil, id_requerimiento_gral),
    CONSTRAINT chk_postulacion_gral_estado CHECK (estado IN ('Pendiente', 'Aceptada', 'Rechazada'))
);

CREATE TABLE invitacion_actividad(
    id_invitacion             BIGSERIAL PRIMARY KEY,
    mensaje                    VARCHAR(200),
    fecha_envio                TIMESTAMP      NOT NULL DEFAULT now(),
    estado                     VARCHAR(20)    NOT NULL DEFAULT 'Pendiente',
    id_perfil_remitente        BIGINT         NOT NULL REFERENCES perfil(id_perfil),
    id_perfil_destinatario     BIGINT         NOT NULL REFERENCES perfil(id_perfil),
    id_requerimiento           BIGINT         NOT NULL REFERENCES requerimiento_actividad(id_requerimiento),
    CONSTRAINT chk_invitacion_act_estado CHECK (estado IN ('Pendiente', 'Aceptada', 'Rechazada')),
    CONSTRAINT chk_invitacion_act_no_autoinvitacion CHECK (id_perfil_remitente <> id_perfil_destinatario)
);

CREATE UNIQUE INDEX uq_invitacion_act_pendiente
    ON invitacion_actividad (id_perfil_destinatario, id_requerimiento)
    WHERE estado = 'Pendiente';

CREATE TABLE invitacion_gral(
    id_invitacion_gral        BIGSERIAL PRIMARY KEY,
    mensaje                    VARCHAR(200),
    fecha_envio                TIMESTAMP      NOT NULL DEFAULT now(),
    estado                     VARCHAR(20)    NOT NULL DEFAULT 'Pendiente',
    id_perfil_remitente        BIGINT         NOT NULL REFERENCES perfil(id_perfil),
    id_perfil_destinatario     BIGINT         NOT NULL REFERENCES perfil(id_perfil),
    id_requerimiento_gral      BIGINT         NOT NULL REFERENCES requerimiento_gral_proyecto(id_requerimiento_gral),
    CONSTRAINT chk_invitacion_gral_estado CHECK (estado IN ('Pendiente', 'Aceptada', 'Rechazada')),
    CONSTRAINT chk_invitacion_gral_no_autoinvitacion CHECK (id_perfil_remitente <> id_perfil_destinatario)
);

CREATE UNIQUE INDEX uq_invitacion_gral_pendiente
    ON invitacion_gral (id_perfil_destinatario, id_requerimiento_gral)
    WHERE estado = 'Pendiente';


-- =====================================================================
-- Datos base necesarios para poder operar el sistema desde el primer arranque
-- =====================================================================

INSERT INTO rol_global (nombre) VALUES ('Administrador'), ('Usuario');
INSERT INTO rol_proyecto (nombre) VALUES ('Director'), ('Miembro');
