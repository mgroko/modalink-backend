-- ============================================================================
-- UC-67 Auditoria ampliada - Fase 0 y Fase 1
-- ============================================================================
-- Fase 0: helper generico de auditoria y catalogo de eventos.
-- Fase 1: evidencia legal (contratos, terminos y condiciones, perfiles y
-- datos personales).
--
-- La migracion es idempotente: usa CREATE OR REPLACE, DROP TRIGGER IF EXISTS
-- e INSERT ... ON CONFLICT.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Catalogo de eventos
-- ----------------------------------------------------------------------------

INSERT INTO tipo_evento_auditoria (codigo, nombre, descripcion, modulo) VALUES
    ('CONTRATO_CREADO',              'Alta de contrato',                 'Creacion de un contrato en estado borrador', 'CONTRATOS'),
    ('CONTRATO_EMITIDO',             'Emision de contrato',              'Contrato emitido y pendiente de firma', 'CONTRATOS'),
    ('CONTRATO_MODIFICADO',          'Modificacion de contrato',         'Cambio de datos editables de un contrato pendiente de firma', 'CONTRATOS'),
    ('CONTRATO_FIRMADO',             'Firma de contrato',                'Firma registrada por una de las partes', 'CONTRATOS'),
    ('CONTRATO_RECHAZADO',           'Rechazo de contrato',              'Rechazo registrado por una de las partes', 'CONTRATOS'),
    ('CONTRATO_ACTIVADO',            'Activacion de contrato',           'Contrato activo al completarse todas las firmas', 'CONTRATOS'),
    ('CONTRATO_CANCELADO',           'Cancelacion de contrato',          'Contrato cancelado', 'CONTRATOS'),
    ('CONTRATO_ELIMINADO',           'Eliminacion de contrato',          'Contrato dado de baja logica', 'CONTRATOS'),
    ('CONTRATO_FIRMA_CANCELADA',     'Cancelacion de firma',             'Firma pendiente cancelada', 'CONTRATOS'),
    ('CONTRATO_FIRMA_INVALIDADA',    'Invalidacion de firma',            'Firma invalidada por baja del firmante', 'CONTRATOS'),
    ('CONTRATO_MIEMBRO_BAJA',        'Baja de miembro en contrato',      'Baja o cambio de participacion de un firmante', 'CONTRATOS'),
    ('TERMINOS_VERSION_CREADA',      'Alta de version de terminos',      'Nueva version de terminos y condiciones de un perfil', 'LEGAL'),
    ('TERMINOS_VERSION_CERRADA',     'Cierre de version de terminos',    'Version de terminos y condiciones cerrada', 'LEGAL'),
    ('TERMINOS_ACEPTADOS',           'Aceptacion de terminos',           'Aceptacion de terminos y condiciones por un perfil', 'LEGAL'),
    ('USUARIO_DATOS_MODIFICADOS',    'Modificacion de datos personales', 'Cambio de datos personales de un usuario', 'USUARIOS'),
    ('PERFIL_CREADO',                'Alta de perfil',                   'Creacion de un perfil de usuario', 'PERFILES'),
    ('PERFIL_MODIFICADO',            'Modificacion de perfil',           'Cambio de datos editables de un perfil', 'PERFILES'),
    ('PERFIL_BAJA_SOLICITADA',       'Solicitud de baja de perfil',      'Perfil en tramite de baja', 'PERFILES'),
    ('PERFIL_DESHABILITADO',         'Deshabilitacion de perfil',        'Perfil deshabilitado', 'PERFILES'),
    ('PERFIL_ELIMINADO',             'Baja de perfil',                   'Perfil dado de baja definitiva', 'PERFILES'),
    ('PERFIL_REACTIVADO',            'Reactivacion de perfil',           'Perfil reactivado', 'PERFILES')
ON CONFLICT (codigo) DO UPDATE
    SET nombre      = EXCLUDED.nombre,
        descripcion = EXCLUDED.descripcion,
        modulo      = EXCLUDED.modulo,
        activo      = EXCLUDED.activo;

-- ----------------------------------------------------------------------------
-- 2. Helper generico
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditoria_registrar(
    p_entidad varchar,
    p_id bigint,
    p_evento varchar,
    p_accion varchar,
    p_detalle jsonb DEFAULT '[]'::jsonb
) RETURNS bigint AS $$
DECLARE
    v_id_tipo_auditoria bigint;
    v_id_auditoria bigint;
    v_elemento jsonb;
BEGIN
    SELECT id_tipo_auditoria INTO v_id_tipo_auditoria
    FROM tipo_accion_auditoria
    WHERE upper(nombre) = upper(p_accion);

    IF v_id_tipo_auditoria IS NULL THEN
        RAISE EXCEPTION 'No se encontro el tipo de accion de auditoria %', p_accion;
    END IF;

    INSERT INTO auditoria (
        usuario_bd,
        entidad_afectada,
        id_afectado,
        fecha_hora,
        ip_usuario,
        id_tipo_auditoria,
        id_usuario,
        tipo_evento,
        tipo_evento_original
    ) VALUES (
        CURRENT_USER,
        p_entidad,
        p_id,
        clock_timestamp(),
        inet_client_addr()::varchar,
        v_id_tipo_auditoria,
        fn_auditoria_usuario_actual(),
        fn_evento_auditoria(p_evento),
        p_evento
    ) RETURNING id_auditoria INTO v_id_auditoria;

    IF p_detalle IS NOT NULL THEN
        FOR v_elemento IN SELECT * FROM jsonb_array_elements(p_detalle)
        LOOP
            IF v_elemento ? 'campo' THEN
                INSERT INTO detalle_auditoria (
                    campo,
                    valor_anterior,
                    valor_nuevo,
                    id_auditoria
                ) VALUES (
                    v_elemento->>'campo',
                    CASE WHEN v_elemento ? 'anterior' THEN v_elemento->>'anterior' ELSE NULL END,
                    CASE WHEN v_elemento ? 'nuevo' THEN v_elemento->>'nuevo' ELSE NULL END,
                    v_id_auditoria
                );
            END IF;
        END LOOP;
    END IF;

    RETURN v_id_auditoria;
END;
$$ LANGUAGE plpgsql;

-- ----------------------------------------------------------------------------
-- 3. Contratos: espejo de contrato_evento hacia auditoria
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_registrar_evento_contrato(
    p_id_contrato BIGINT,
    p_id_miembro BIGINT,
    p_id_usuario BIGINT,
    p_tipo_evento VARCHAR,
    p_id_contrato_version BIGINT,
    p_motivo VARCHAR
) RETURNS void AS $$
DECLARE
    v_evento varchar(60);
    v_accion varchar(20);
    v_id_proyecto bigint;
    v_estado varchar(50);
    v_nombre varchar(255);
    v_id_tipo_acuerdo bigint;
    v_detalle jsonb := '[]'::jsonb;
BEGIN
    INSERT INTO contrato_evento (
        id_contrato,
        id_miembro,
        id_usuario,
        tipo_evento,
        id_contrato_version,
        motivo
    ) VALUES (
        p_id_contrato,
        p_id_miembro,
        p_id_usuario,
        p_tipo_evento,
        p_id_contrato_version,
        p_motivo
    );

    v_evento := 'CONTRATO_' || p_tipo_evento;
    v_accion := CASE
        WHEN p_tipo_evento IN ('CREADO', 'EMITIDO') THEN 'INSERT'
        ELSE 'UPDATE'
    END;

    SELECT c.id_proyecto, c.estado, c.nombre, c.id_tipo_acuerdo
      INTO v_id_proyecto, v_estado, v_nombre, v_id_tipo_acuerdo
      FROM contrato c
     WHERE c.id_contrato = p_id_contrato;

    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', 'tipoEventoContrato', 'nuevo', p_tipo_evento),
        jsonb_build_object('campo', 'idProyecto', 'nuevo', v_id_proyecto::text),
        jsonb_build_object('campo', 'estadoContrato', 'nuevo', v_estado),
        jsonb_build_object('campo', 'nombreContrato', 'nuevo', v_nombre),
        jsonb_build_object('campo', 'idTipoAcuerdo', 'nuevo', v_id_tipo_acuerdo::text)
    );

    IF p_id_miembro IS NOT NULL THEN
        v_detalle := v_detalle || jsonb_build_array(
            jsonb_build_object('campo', 'idMiembro', 'nuevo', p_id_miembro::text)
        );
    END IF;

    IF p_id_contrato_version IS NOT NULL THEN
        v_detalle := v_detalle || jsonb_build_array(
            jsonb_build_object('campo', 'idContratoVersion', 'nuevo', p_id_contrato_version::text)
        );
    END IF;

    IF p_motivo IS NOT NULL THEN
        v_detalle := v_detalle || jsonb_build_array(
            jsonb_build_object('campo', 'motivo', 'nuevo', p_motivo)
        );
    END IF;

    PERFORM fn_auditoria_registrar(
        'contrato',
        p_id_contrato,
        v_evento,
        v_accion,
        v_detalle
    );
END;
$$ LANGUAGE plpgsql;

-- ----------------------------------------------------------------------------
-- 4. Usuarios: datos personales con valores anteriores y nuevos
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_usuario() RETURNS trigger AS $$
DECLARE
    v_id_tipo_auditoria BIGINT;
    v_id_auditoria BIGINT;
    v_id_usuario BIGINT;
    v_id_afectado BIGINT;
    v_tipo_evento VARCHAR(60);
    v_cambio_datos BOOLEAN;
BEGIN
    v_cambio_datos := TG_OP = 'UPDATE' AND (
        NEW.nombre IS DISTINCT FROM OLD.nombre
        OR NEW.apellido IS DISTINCT FROM OLD.apellido
        OR NEW.dni IS DISTINCT FROM OLD.dni
        OR NEW.fecha_nacimiento IS DISTINCT FROM OLD.fecha_nacimiento
        OR NEW.correo IS DISTINCT FROM OLD.correo
        OR NEW.id_ubicacion IS DISTINCT FROM OLD.id_ubicacion
        OR NEW.id_genero IS DISTINCT FROM OLD.id_genero
    );

    SELECT id_tipo_auditoria INTO v_id_tipo_auditoria
    FROM tipo_accion_auditoria
    WHERE nombre = TG_OP;

    IF v_id_tipo_auditoria IS NULL THEN
        RAISE EXCEPTION 'No se encontro el tipo de accion de auditoria para la operacion %', TG_OP;
    END IF;

    IF TG_OP = 'INSERT' THEN
        v_tipo_evento := 'USUARIO_ALTA';
    ELSIF TG_OP = 'DELETE' THEN
        v_tipo_evento := 'USUARIO_BAJA';
    ELSIF NEW.id_rol_global IS DISTINCT FROM OLD.id_rol_global THEN
        v_tipo_evento := 'USUARIO_ROL_CAMBIADO';
    ELSIF NEW.estado IS DISTINCT FROM OLD.estado THEN
        v_tipo_evento := CASE NEW.estado
            WHEN 'BAJA' THEN 'USUARIO_BAJA'
            WHEN 'PENDIENTE_BAJA' THEN 'USUARIO_BAJA'
            WHEN 'DESHABILITADO' THEN 'USUARIO_DESHABILITADO'
            WHEN 'ACTIVO' THEN 'USUARIO_HABILITADO'
            ELSE 'USUARIO_HABILITADO'
        END;
    ELSIF v_cambio_datos THEN
        v_tipo_evento := 'USUARIO_DATOS_MODIFICADOS';
    ELSE
        IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
    END IF;

    v_id_afectado := CASE WHEN TG_OP = 'DELETE' THEN OLD.id_usuario ELSE NEW.id_usuario END;

    v_id_usuario := COALESCE(
        fn_auditoria_usuario_actual(),
        CASE
            WHEN TG_OP = 'INSERT' THEN NEW.id_usuario
            WHEN TG_OP = 'UPDATE'
                 AND OLD.estado = 'PENDIENTE_BAJA'
                 AND NEW.estado = 'ACTIVO' THEN NEW.id_usuario
            ELSE NULL
        END
    );

    INSERT INTO auditoria (
        usuario_bd,
        entidad_afectada,
        id_afectado,
        fecha_hora,
        ip_usuario,
        id_tipo_auditoria,
        id_usuario,
        tipo_evento,
        tipo_evento_original
    ) VALUES (
        CURRENT_USER,
        'usuario',
        v_id_afectado,
        clock_timestamp(),
        inet_client_addr()::varchar,
        v_id_tipo_auditoria,
        v_id_usuario,
        fn_evento_auditoria(v_tipo_evento),
        v_tipo_evento
    ) RETURNING id_auditoria INTO v_id_auditoria;

    IF TG_OP = 'INSERT' THEN
        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria) VALUES
            ('estado', NULL, NEW.estado, v_id_auditoria),
            ('id_rol_global', NULL, NEW.id_rol_global::varchar, v_id_auditoria);
    ELSE
        IF TG_OP = 'UPDATE' AND NEW.estado IS DISTINCT FROM OLD.estado THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('estado', OLD.estado, NEW.estado, v_id_auditoria);
        END IF;

        IF TG_OP = 'UPDATE' AND NEW.id_rol_global IS DISTINCT FROM OLD.id_rol_global THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('id_rol_global', OLD.id_rol_global::varchar, NEW.id_rol_global::varchar, v_id_auditoria);
        END IF;

        IF TG_OP = 'UPDATE' AND NEW.motivo_deshabilitacion IS DISTINCT FROM OLD.motivo_deshabilitacion THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('motivo_deshabilitacion', OLD.motivo_deshabilitacion, NEW.motivo_deshabilitacion, v_id_auditoria);
        END IF;

        IF TG_OP = 'UPDATE' AND NEW.fecha_hasta_deshabilitacion IS DISTINCT FROM OLD.fecha_hasta_deshabilitacion THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('fecha_hasta_deshabilitacion', OLD.fecha_hasta_deshabilitacion::varchar, NEW.fecha_hasta_deshabilitacion::varchar, v_id_auditoria);
        END IF;

        IF v_cambio_datos THEN
            IF NEW.nombre IS DISTINCT FROM OLD.nombre THEN
                INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
                VALUES ('nombre', OLD.nombre, NEW.nombre, v_id_auditoria);
            END IF;
            IF NEW.apellido IS DISTINCT FROM OLD.apellido THEN
                INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
                VALUES ('apellido', OLD.apellido, NEW.apellido, v_id_auditoria);
            END IF;
            IF NEW.dni IS DISTINCT FROM OLD.dni THEN
                INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
                VALUES ('dni', OLD.dni, NEW.dni, v_id_auditoria);
            END IF;
            IF NEW.fecha_nacimiento IS DISTINCT FROM OLD.fecha_nacimiento THEN
                INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
                VALUES ('fecha_nacimiento', OLD.fecha_nacimiento::varchar, NEW.fecha_nacimiento::varchar, v_id_auditoria);
            END IF;
            IF NEW.correo IS DISTINCT FROM OLD.correo THEN
                INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
                VALUES ('correo', OLD.correo, NEW.correo, v_id_auditoria);
            END IF;
            IF NEW.id_ubicacion IS DISTINCT FROM OLD.id_ubicacion THEN
                INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
                VALUES ('id_ubicacion', OLD.id_ubicacion::varchar, NEW.id_ubicacion::varchar, v_id_auditoria);
            END IF;
            IF NEW.id_genero IS DISTINCT FROM OLD.id_genero THEN
                INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
                VALUES ('id_genero', OLD.id_genero::varchar, NEW.id_genero::varchar, v_id_auditoria);
            END IF;
        END IF;

        IF TG_OP = 'DELETE' THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('registro', OLD.correo, 'ELIMINADO', v_id_auditoria);
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_usuario ON usuario;
CREATE TRIGGER trg_auditar_cambio_usuario
    AFTER INSERT OR UPDATE OF estado, id_rol_global, nombre, apellido, dni,
        fecha_nacimiento, correo, id_ubicacion, id_genero OR DELETE ON usuario
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_usuario();

-- ----------------------------------------------------------------------------
-- 5. Perfiles
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_perfil() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_detalle jsonb := '[]'::jsonb;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_evento := 'PERFIL_CREADO';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idUsuario', 'nuevo', NEW.id_usuario::text),
            jsonb_build_object('campo', 'idProfesion', 'nuevo', NEW.id_profesion::text),
            jsonb_build_object('campo', 'estado', 'nuevo', NEW.estado),
            jsonb_build_object('campo', 'nombreArtistico', 'nuevo', NEW.nombre_artistico)
        );
        PERFORM fn_auditoria_registrar('perfil', NEW.id_perfil, v_evento, 'INSERT', v_detalle);
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'estado', 'anterior', OLD.estado, 'nuevo', 'ELIMINADO')
        );
        PERFORM fn_auditoria_registrar('perfil', OLD.id_perfil, 'PERFIL_ELIMINADO', 'DELETE', v_detalle);
        RETURN OLD;
    END IF;

    IF NEW.estado IS DISTINCT FROM OLD.estado THEN
        v_evento := CASE NEW.estado
            WHEN 'PENDIENTE_BAJA' THEN 'PERFIL_BAJA_SOLICITADA'
            WHEN 'BAJA' THEN 'PERFIL_ELIMINADO'
            WHEN 'DESHABILITADO' THEN 'PERFIL_DESHABILITADO'
            WHEN 'ACTIVO' THEN 'PERFIL_REACTIVADO'
            ELSE 'PERFIL_MODIFICADO'
        END;
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'estado', 'anterior', OLD.estado, 'nuevo', NEW.estado)
        );
    ELSIF NEW.nombre_artistico IS DISTINCT FROM OLD.nombre_artistico
       OR NEW.biografia IS DISTINCT FROM OLD.biografia
       OR NEW.foto_perfil IS DISTINCT FROM OLD.foto_perfil
       OR NEW.id_profesion IS DISTINCT FROM OLD.id_profesion THEN
        v_evento := 'PERFIL_MODIFICADO';

        IF NEW.nombre_artistico IS DISTINCT FROM OLD.nombre_artistico THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'nombreArtistico', 'anterior', OLD.nombre_artistico, 'nuevo', NEW.nombre_artistico)
            );
        END IF;
        IF NEW.biografia IS DISTINCT FROM OLD.biografia THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'biografia', 'anterior', OLD.biografia, 'nuevo', NEW.biografia)
            );
        END IF;
        IF NEW.foto_perfil IS DISTINCT FROM OLD.foto_perfil THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'fotoPerfil', 'anterior', OLD.foto_perfil::text, 'nuevo', NEW.foto_perfil::text)
            );
        END IF;
        IF NEW.id_profesion IS DISTINCT FROM OLD.id_profesion THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'idProfesion', 'anterior', OLD.id_profesion::text, 'nuevo', NEW.id_profesion::text)
            );
        END IF;
    ELSE
        RETURN NEW;
    END IF;

    PERFORM fn_auditoria_registrar('perfil', NEW.id_perfil, v_evento, 'UPDATE', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_perfil ON perfil;
CREATE TRIGGER trg_auditar_cambio_perfil
    AFTER INSERT OR UPDATE OF estado, nombre_artistico, biografia, foto_perfil, id_profesion
        OR DELETE ON perfil
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_perfil();

-- ----------------------------------------------------------------------------
-- 6. Terminos y condiciones: versiones y aceptaciones
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_tyc() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_detalle jsonb;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_evento := 'TERMINOS_VERSION_CREADA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idTyc', 'nuevo', NEW.id_tyc::text),
            jsonb_build_object('campo', 'idPerfil', 'nuevo', NEW.id_perfil::text),
            jsonb_build_object('campo', 'fechaInicio', 'nuevo', NEW.fecha_inicio::text),
            jsonb_build_object('campo', 'vigente', 'nuevo', CASE WHEN NEW.fecha_fin IS NULL THEN 'true' ELSE 'false' END)
        );
        PERFORM fn_auditoria_registrar('perfil', NEW.id_perfil, v_evento, 'INSERT', v_detalle);
        RETURN NEW;
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.fecha_fin IS DISTINCT FROM NEW.fecha_fin THEN
        v_evento := 'TERMINOS_VERSION_CERRADA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idTyc', 'nuevo', NEW.id_tyc::text),
            jsonb_build_object('campo', 'idPerfil', 'nuevo', NEW.id_perfil::text),
            jsonb_build_object('campo', 'fechaFin', 'anterior', OLD.fecha_fin::text, 'nuevo', NEW.fecha_fin::text)
        );
        PERFORM fn_auditoria_registrar('perfil', NEW.id_perfil, v_evento, 'UPDATE', v_detalle);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_tyc ON perfil_tyc;
CREATE TRIGGER trg_auditar_cambio_tyc
    AFTER INSERT OR UPDATE OF fecha_fin ON perfil_tyc
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_tyc();

CREATE OR REPLACE FUNCTION fn_auditar_aceptacion_tyc() RETURNS trigger AS $$
DECLARE
    v_entidad varchar(100);
    v_id bigint;
    v_id_perfil bigint;
    v_id_proyecto bigint;
    v_detalle jsonb;
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.id_tyc_aceptado IS NOT DISTINCT FROM NEW.id_tyc_aceptado THEN
        RETURN NEW;
    END IF;

    IF NEW.id_tyc_aceptado IS NULL THEN
        RETURN NEW;
    END IF;

    IF TG_TABLE_NAME = 'miembros_proyecto' THEN
        v_entidad := 'miembro_proyecto';
        v_id := NEW.id_miembro;
        v_id_perfil := NEW.id_perfil;
        v_id_proyecto := NEW.id_proyecto;
    ELSIF TG_TABLE_NAME = 'invitacion_gral' THEN
        v_entidad := 'invitacion_gral';
        v_id := NEW.id_invitacion_gral;
        v_id_perfil := NEW.id_perfil_destinatario;
        v_id_proyecto := NEW.id_proyecto;
    ELSIF TG_TABLE_NAME = 'invitacion_actividad' THEN
        v_entidad := 'invitacion_actividad';
        v_id := NEW.id_invitacion;
        v_id_perfil := NEW.id_perfil_destinatario;
        v_id_proyecto := NULL;
    ELSE
        RETURN NEW;
    END IF;

    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', 'idTyc', 'nuevo', NEW.id_tyc_aceptado::text),
        jsonb_build_object('campo', 'idPerfil', 'nuevo', v_id_perfil::text),
        jsonb_build_object('campo', 'idProyecto', 'nuevo', v_id_proyecto::text)
    );

    PERFORM fn_auditoria_registrar(v_entidad, v_id, 'TERMINOS_ACEPTADOS', 'INSERT', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_aceptacion_tyc_miembro ON miembros_proyecto;
CREATE TRIGGER trg_auditar_aceptacion_tyc_miembro
    AFTER INSERT OR UPDATE OF id_tyc_aceptado ON miembros_proyecto
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_aceptacion_tyc();

DROP TRIGGER IF EXISTS trg_auditar_aceptacion_tyc_invitacion_gral ON invitacion_gral;
CREATE TRIGGER trg_auditar_aceptacion_tyc_invitacion_gral
    AFTER INSERT OR UPDATE OF id_tyc_aceptado ON invitacion_gral
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_aceptacion_tyc();

DROP TRIGGER IF EXISTS trg_auditar_aceptacion_tyc_invitacion_act ON invitacion_actividad;
CREATE TRIGGER trg_auditar_aceptacion_tyc_invitacion_act
    AFTER INSERT OR UPDATE OF id_tyc_aceptado ON invitacion_actividad
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_aceptacion_tyc();

-- ----------------------------------------------------------------------------
-- 7. Inmutabilidad de la bitacora de contratos
-- ----------------------------------------------------------------------------

DROP TRIGGER IF EXISTS trg_contrato_evento_no_update_delete ON contrato_evento;
CREATE TRIGGER trg_contrato_evento_no_update_delete
    BEFORE UPDATE OR DELETE ON contrato_evento
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria_inmutable();

DROP TRIGGER IF EXISTS trg_contrato_evento_no_truncate ON contrato_evento;
CREATE TRIGGER trg_contrato_evento_no_truncate
    BEFORE TRUNCATE ON contrato_evento
    FOR EACH STATEMENT EXECUTE FUNCTION fn_auditoria_inmutable();
