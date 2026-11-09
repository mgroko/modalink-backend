-- ============================================================================
-- UC-67 Auditoria ampliada - Fase 2, Fase 3 y Fase 4
-- ============================================================================
-- Fase 2: identidad, perfiles, habilidades, roles y membresias.
-- Fase 3: estado operativo (proyectos, actividades, asignaciones,
--         postulaciones, invitaciones, reemplazos, calendario y resenas).
-- Fase 4: catalogos administrativos.
--
-- Requiere que la migracion de Fase 0/1 ya haya creado
-- fn_auditoria_registrar.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Catalogo de eventos
-- ----------------------------------------------------------------------------

INSERT INTO tipo_evento_auditoria (codigo, nombre, descripcion, modulo) VALUES
    ('USUARIO_IDENTIDAD_VINCULADA',      'Vinculacion de identidad',          'Alta de una identidad local o de Google para un usuario', 'USUARIOS'),
    ('USUARIO_IDENTIDAD_DESVINCULADA',   'Desvinculacion de identidad',       'Baja de una identidad vinculada a un usuario', 'USUARIOS'),
    ('PERFIL_HABILIDAD_ALTA',            'Alta de habilidad de perfil',       'Habilidad agregada a un perfil', 'PERFILES'),
    ('PERFIL_HABILIDAD_BAJA',            'Baja de habilidad de perfil',       'Habilidad quitada de un perfil', 'PERFILES'),
    ('ROL_GLOBAL_CREADO',                'Alta de rol global',                'Creacion de un rol global', 'ROLES'),
    ('ROL_GLOBAL_MODIFICADO',            'Modificacion de rol global',        'Cambio de datos de un rol global', 'ROLES'),
    ('ROL_GLOBAL_ELIMINADO',             'Baja de rol global',                'Eliminacion de un rol global', 'ROLES'),
    ('ROL_GLOBAL_PERMISO_ASIGNADO',      'Permiso global asignado',           'Permiso agregado a un rol global', 'ROLES'),
    ('ROL_GLOBAL_PERMISO_REVOCADO',      'Permiso global revocado',           'Permiso quitado de un rol global', 'ROLES'),
    ('ROL_PROYECTO_CREADO',              'Alta de rol de proyecto',           'Creacion de un rol de proyecto', 'ROLES'),
    ('ROL_PROYECTO_MODIFICADO',          'Modificacion de rol de proyecto',   'Cambio de datos de un rol de proyecto', 'ROLES'),
    ('ROL_PROYECTO_ELIMINADO',           'Baja de rol de proyecto',           'Eliminacion de un rol de proyecto', 'ROLES'),
    ('ROL_PROYECTO_PERMISO_ASIGNADO',    'Permiso de proyecto asignado',      'Permiso agregado a un rol de proyecto', 'ROLES'),
    ('ROL_PROYECTO_PERMISO_REVOCADO',    'Permiso de proyecto revocado',      'Permiso quitado de un rol de proyecto', 'ROLES'),
    ('MIEMBRO_ALTA',                     'Alta de miembro',                   'Incorporacion de un perfil a un proyecto', 'PROYECTOS'),
    ('MIEMBRO_BAJA_VOLUNTARIA',          'Baja voluntaria de miembro',        'Baja voluntaria de un perfil de un proyecto', 'PROYECTOS'),
    ('MIEMBRO_ELIMINADO',                'Eliminacion de miembro',            'Eliminacion de un perfil de un proyecto', 'PROYECTOS'),
    ('MIEMBRO_ROL_CAMBIADO',             'Cambio de rol de miembro',          'Cambio del rol de proyecto de un miembro', 'PROYECTOS'),
    ('MIEMBRO_REINCORPORADO',            'Reincorporacion de miembro',        'Reincorporacion de un perfil a un proyecto', 'PROYECTOS'),
    ('PROYECTO_CREADO',                  'Alta de proyecto',                  'Creacion de un proyecto', 'PROYECTOS'),
    ('PROYECTO_MODIFICADO',              'Modificacion de proyecto',          'Cambio de datos editables de un proyecto', 'PROYECTOS'),
    ('PROYECTO_REPROGRAMADO',            'Reprogramacion de proyecto',        'Cambio de fechas de un proyecto', 'PROYECTOS'),
    ('ACTIVIDAD_CREADA',                 'Alta de actividad',                 'Creacion de una actividad de planificacion', 'ACTIVIDADES'),
    ('ACTIVIDAD_MODIFICADA',             'Modificacion de actividad',         'Cambio de datos editables de una actividad', 'ACTIVIDADES'),
    ('ACTIVIDAD_ELIMINADA',              'Baja de actividad',                 'Eliminacion de una actividad', 'ACTIVIDADES'),
    ('ACTIVIDAD_ESTADO_CAMBIADO',        'Cambio de estado de actividad',     'Transicion de estado de una actividad', 'ACTIVIDADES'),
    ('ACTIVIDAD_REPROGRAMADA',           'Reprogramacion de actividad',       'Cambio de fecha de inicio de una actividad', 'ACTIVIDADES'),
    ('ASIGNACION_ACTIVIDAD_ALTA',        'Alta de asignacion',                'Asignacion de un miembro a una actividad', 'ACTIVIDADES'),
    ('ASIGNACION_ACTIVIDAD_BAJA',        'Baja de asignacion',                'Desasignacion de un miembro de una actividad', 'ACTIVIDADES'),
    ('POSTULACION_CREADA',               'Alta de postulacion',               'Postulacion a un requerimiento general o de actividad', 'POSTULACIONES'),
    ('INVITACION_CREADA',                'Alta de invitacion',                'Invitacion a un proyecto o actividad', 'INVITACIONES'),
    ('REEMPLAZO_INICIADO',               'Inicio de reemplazo',               'Inicio de un proceso de reemplazo', 'REEMPLAZOS'),
    ('REEMPLAZO_CUBIERTO',               'Reemplazo cubierto',                'Proceso de reemplazo cubierto', 'REEMPLAZOS'),
    ('REEMPLAZO_CANDIDATO_APROBADO',     'Candidato de reemplazo aprobado',   'Aprobacion de un candidato de reemplazo', 'REEMPLAZOS'),
    ('REEMPLAZO_CANDIDATO_RECHAZADO',    'Candidato de reemplazo rechazado',  'Rechazo de un candidato de reemplazo', 'REEMPLAZOS'),
    ('REEMPLAZO_CANDIDATO_CANCELADO',    'Candidato de reemplazo cancelado',  'Cancelacion de un candidato de reemplazo', 'REEMPLAZOS'),
    ('REEMPLAZO_CANCELADO',              'Cancelacion de reemplazo',          'Cancelacion de un proceso de reemplazo', 'REEMPLAZOS'),
    ('CALENDARIO_JORNADA_ACTUALIZADA',   'Actualizacion de jornada',          'Cambio de jornada laboral o margen de agenda', 'CALENDARIO'),
    ('CALENDARIO_BLOQUEO_ALTA',          'Alta de bloqueo de agenda',         'Bloqueo manual de disponibilidad', 'CALENDARIO'),
    ('CALENDARIO_BLOQUEO_BAJA',          'Baja de bloqueo de agenda',         'Eliminacion de un bloqueo manual de disponibilidad', 'CALENDARIO'),
    ('CALIFICACION_REGISTRADA',          'Calificacion registrada',           'Calificacion de un perfil dentro de un proyecto', 'CALIFICACIONES'),
    ('CATALOGO_ALTA',                    'Alta de catalogo',                  'Alta de un registro de catalogo administrativo', 'CATALOGOS'),
    ('CATALOGO_MODIFICACION',            'Modificacion de catalogo',          'Modificacion de un registro de catalogo administrativo', 'CATALOGOS'),
    ('CATALOGO_BAJA',                    'Baja de catalogo',                  'Baja de un registro de catalogo administrativo', 'CATALOGOS'),
    ('CRONOGRAMA_REPARADO',              'Reparacion de cronograma',          'Reparacion manual de inconsistencias de cronograma', 'ADMINISTRACION'),
    ('CRONOGRAMA_RECONCILIADO',          'Reconciliacion de cronograma',      'Reconciliacion automatica de cronogramas', 'ADMINISTRACION'),
    ('SCHEDULER_EJECUTADO',              'Ejecucion de scheduler',            'Ejecucion manual de un scheduler', 'ADMINISTRACION'),
    ('SCHEDULER_FALLIDO',                'Fallo de scheduler',                'Error en la ejecucion manual de un scheduler', 'ADMINISTRACION'),
    ('AUDITORIA_EXPORTADA',              'Exportacion de auditoria',          'Exportacion de un informe de auditoria', 'ADMINISTRACION')
ON CONFLICT (codigo) DO UPDATE
    SET nombre      = EXCLUDED.nombre,
        descripcion = EXCLUDED.descripcion,
        modulo      = EXCLUDED.modulo,
        activo      = EXCLUDED.activo;

-- ----------------------------------------------------------------------------
-- 2. Identidad de usuario
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_identidad() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_detalle jsonb;
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF fn_auditoria_usuario_actual() IS NULL THEN
            PERFORM set_config('app.id_usuario', NEW.id_usuario::text, true);
        END IF;

        v_evento := 'USUARIO_IDENTIDAD_VINCULADA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'proveedor', 'nuevo', NEW.proveedor),
            jsonb_build_object('campo', 'correoVerificado', 'nuevo', NEW.correo_verificado::text),
            jsonb_build_object('campo', 'fechaVinculacion', 'nuevo', NEW.fecha_vinculacion::text)
        );
        PERFORM fn_auditoria_registrar('usuario', NEW.id_usuario, v_evento, 'INSERT', v_detalle);
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        v_evento := 'USUARIO_IDENTIDAD_DESVINCULADA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'proveedor', 'anterior', OLD.proveedor, 'nuevo', NULL),
            jsonb_build_object('campo', 'correoVerificado', 'anterior', OLD.correo_verificado::text, 'nuevo', NULL)
        );
        PERFORM fn_auditoria_registrar('usuario', OLD.id_usuario, v_evento, 'DELETE', v_detalle);
        RETURN OLD;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_identidad ON usuario_identidad;
CREATE TRIGGER trg_auditar_cambio_identidad
    AFTER INSERT OR DELETE ON usuario_identidad
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_identidad();

-- ----------------------------------------------------------------------------
-- 3. Habilidades de perfil
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_habilidad_perfil() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_detalle jsonb;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_evento := 'PERFIL_HABILIDAD_ALTA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idHabilidad', 'nuevo', NEW.id_habilidad::text)
        );
        PERFORM fn_auditoria_registrar('perfil', NEW.id_perfil, v_evento, 'INSERT', v_detalle);
        RETURN NEW;
    END IF;

    v_evento := 'PERFIL_HABILIDAD_BAJA';
    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', 'idHabilidad', 'anterior', OLD.id_habilidad::text, 'nuevo', NULL)
    );
    PERFORM fn_auditoria_registrar('perfil', OLD.id_perfil, v_evento, 'DELETE', v_detalle);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_habilidad_perfil ON habilidad_perfil;
CREATE TRIGGER trg_auditar_cambio_habilidad_perfil
    AFTER INSERT OR DELETE ON habilidad_perfil
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_habilidad_perfil();

-- ----------------------------------------------------------------------------
-- 4. Roles globales y de proyecto
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_rol() RETURNS trigger AS $$
DECLARE
    v_prefijo varchar(40);
    v_columna_id varchar(40);
    v_evento varchar(60);
    v_id bigint;
    v_detalle jsonb;
BEGIN
    v_prefijo := TG_ARGV[0];
    v_columna_id := TG_ARGV[1];
    v_evento := v_prefijo || CASE TG_OP
        WHEN 'INSERT' THEN '_CREADO'
        WHEN 'UPDATE' THEN '_MODIFICADO'
        ELSE '_ELIMINADO'
    END;

    IF TG_OP = 'DELETE' THEN
        v_id := (to_jsonb(OLD)->>v_columna_id)::bigint;
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'codigo', 'anterior', OLD.codigo, 'nuevo', NULL),
            jsonb_build_object('campo', 'nombre', 'anterior', OLD.nombre, 'nuevo', NULL)
        );
    ELSE
        v_id := (to_jsonb(NEW)->>v_columna_id)::bigint;
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'codigo',
                'anterior', CASE WHEN TG_OP = 'UPDATE' THEN OLD.codigo ELSE NULL END,
                'nuevo', NEW.codigo),
            jsonb_build_object('campo', 'nombre',
                'anterior', CASE WHEN TG_OP = 'UPDATE' THEN OLD.nombre ELSE NULL END,
                'nuevo', NEW.nombre)
        );
    END IF;

    PERFORM fn_auditoria_registrar(
        CASE WHEN v_prefijo = 'ROL_GLOBAL' THEN 'rol_global' ELSE 'rol_proyecto' END,
        v_id, v_evento, TG_OP, v_detalle);

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_rol_global ON rol_global;
CREATE TRIGGER trg_auditar_cambio_rol_global
    AFTER INSERT OR UPDATE OR DELETE ON rol_global
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_rol('ROL_GLOBAL', 'id_rol_global');

DROP TRIGGER IF EXISTS trg_auditar_cambio_rol_proyecto ON rol_proyecto;
CREATE TRIGGER trg_auditar_cambio_rol_proyecto
    AFTER INSERT OR UPDATE OR DELETE ON rol_proyecto
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_rol('ROL_PROYECTO', 'id_rol_proyecto');

CREATE OR REPLACE FUNCTION fn_auditar_cambio_rol_permiso() RETURNS trigger AS $$
DECLARE
    v_prefijo varchar(40);
    v_columna_rol varchar(40);
    v_columna_permiso varchar(40);
    v_id_rol bigint;
    v_id_permiso bigint;
    v_evento varchar(60);
    v_detalle jsonb;
BEGIN
    v_prefijo := TG_ARGV[0];
    v_columna_rol := TG_ARGV[1];
    v_columna_permiso := TG_ARGV[2];

    IF TG_OP = 'INSERT' THEN
        v_id_rol := (to_jsonb(NEW)->>v_columna_rol)::bigint;
        v_id_permiso := (to_jsonb(NEW)->>v_columna_permiso)::bigint;
        v_evento := v_prefijo || '_PERMISO_ASIGNADO';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', v_columna_permiso, 'nuevo', v_id_permiso::text)
        );
        PERFORM fn_auditoria_registrar(
            CASE WHEN v_prefijo = 'ROL_GLOBAL' THEN 'rol_global' ELSE 'rol_proyecto' END,
            v_id_rol, v_evento, 'INSERT', v_detalle);
        RETURN NEW;
    END IF;

    v_id_rol := (to_jsonb(OLD)->>v_columna_rol)::bigint;
    v_id_permiso := (to_jsonb(OLD)->>v_columna_permiso)::bigint;
    v_evento := v_prefijo || '_PERMISO_REVOCADO';
    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', v_columna_permiso, 'anterior', v_id_permiso::text, 'nuevo', NULL)
    );
    PERFORM fn_auditoria_registrar(
        CASE WHEN v_prefijo = 'ROL_GLOBAL' THEN 'rol_global' ELSE 'rol_proyecto' END,
        v_id_rol, v_evento, 'DELETE', v_detalle);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_rol_global_permiso ON rol_global_permiso;
CREATE TRIGGER trg_auditar_cambio_rol_global_permiso
    AFTER INSERT OR DELETE ON rol_global_permiso
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_rol_permiso(
        'ROL_GLOBAL', 'id_rol_global', 'id_permiso_global');

DROP TRIGGER IF EXISTS trg_auditar_cambio_rol_proyecto_permiso ON rol_proyecto_permiso;
CREATE TRIGGER trg_auditar_cambio_rol_proyecto_permiso
    AFTER INSERT OR DELETE ON rol_proyecto_permiso
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_rol_permiso(
        'ROL_PROYECTO', 'id_rol_proyecto', 'id_permiso_proyecto');

-- ----------------------------------------------------------------------------
-- 5. Membresias de proyecto
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_miembro() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_detalle jsonb := '[]'::jsonb;
    v_id bigint;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_evento := 'MIEMBRO_ALTA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idProyecto', 'nuevo', NEW.id_proyecto::text),
            jsonb_build_object('campo', 'idPerfil', 'nuevo', NEW.id_perfil::text),
            jsonb_build_object('campo', 'idRolProyecto', 'nuevo', NEW.id_rol_proyecto::text),
            jsonb_build_object('campo', 'idRequerimientoGral', 'nuevo', NEW.id_requerimiento_gral::text),
            jsonb_build_object('campo', 'idTycAceptado', 'nuevo', NEW.id_tyc_aceptado::text),
            jsonb_build_object('campo', 'estado', 'nuevo', NEW.estado_participacion)
        );
        PERFORM fn_auditoria_registrar('miembros_proyecto', NEW.id_miembro, v_evento, 'INSERT', v_detalle);
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        v_evento := 'MIEMBRO_ELIMINADO';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idProyecto', 'nuevo', OLD.id_proyecto::text),
            jsonb_build_object('campo', 'idPerfil', 'nuevo', OLD.id_perfil::text),
            jsonb_build_object('campo', 'estado', 'anterior', OLD.estado_participacion, 'nuevo', 'ELIMINADO')
        );
        PERFORM fn_auditoria_registrar('miembros_proyecto', OLD.id_miembro, v_evento, 'DELETE', v_detalle);
        RETURN OLD;
    END IF;

    IF NEW.estado_participacion IS DISTINCT FROM OLD.estado_participacion THEN
        v_evento := CASE NEW.estado_participacion
            WHEN 'BAJA_VOLUNTARIA' THEN 'MIEMBRO_BAJA_VOLUNTARIA'
            WHEN 'ELIMINADO' THEN 'MIEMBRO_ELIMINADO'
            WHEN 'ACTIVO' THEN 'MIEMBRO_REINCORPORADO'
            ELSE 'MIEMBRO_ALTA'
        END;
        v_detalle := v_detalle || jsonb_build_array(
            jsonb_build_object('campo', 'estado', 'anterior', OLD.estado_participacion, 'nuevo', NEW.estado_participacion),
            jsonb_build_object('campo', 'motivoBaja', 'anterior', OLD."motivoBaja", 'nuevo', NEW."motivoBaja"),
            jsonb_build_object('campo', 'idPerfilAutorBaja', 'anterior', OLD.id_perfil_autor_baja::text, 'nuevo', NEW.id_perfil_autor_baja::text)
        );
    ELSIF NEW.id_rol_proyecto IS DISTINCT FROM OLD.id_rol_proyecto THEN
        v_evento := 'MIEMBRO_ROL_CAMBIADO';
        v_detalle := v_detalle || jsonb_build_array(
            jsonb_build_object('campo', 'idRolProyecto', 'anterior', OLD.id_rol_proyecto::text, 'nuevo', NEW.id_rol_proyecto::text)
        );
    ELSE
        RETURN NEW;
    END IF;

    v_id := NEW.id_miembro;
    PERFORM fn_auditoria_registrar('miembros_proyecto', v_id, v_evento, 'UPDATE', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_miembro ON miembros_proyecto;
CREATE TRIGGER trg_auditar_cambio_miembro
    AFTER INSERT OR UPDATE OF estado_participacion, id_rol_proyecto OR DELETE ON miembros_proyecto
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_miembro();

-- ----------------------------------------------------------------------------
-- 6. Proyectos
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_proyecto() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_accion varchar(20);
    v_id bigint;
    v_detalle jsonb := '[]'::jsonb;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_evento := 'PROYECTO_CREADO';
        v_accion := 'INSERT';
        v_id := NEW.id_proyecto;
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'nombre', 'nuevo', NEW.nombre),
            jsonb_build_object('campo', 'descripcion', 'nuevo', NEW.descripcion),
            jsonb_build_object('campo', 'estado', 'nuevo', NEW.estado),
            jsonb_build_object('campo', 'privacidad', 'nuevo', NEW.privacidad),
            jsonb_build_object('campo', 'fechaInicio', 'nuevo', NEW.fecha_inicio::text),
            jsonb_build_object('campo', 'fechaFinalizacion', 'nuevo', NEW.fecha_finalizacion::text)
        );
        PERFORM fn_auditoria_registrar('proyecto', v_id, v_evento, v_accion, v_detalle);
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'registro', 'anterior', OLD.nombre, 'nuevo', 'ELIMINADO')
        );
        PERFORM fn_auditoria_registrar('proyecto', OLD.id_proyecto, 'PROYECTO_ESTADO_CAMBIADO', 'DELETE', v_detalle);
        RETURN OLD;
    END IF;

    IF NEW.estado IS DISTINCT FROM OLD.estado
       OR NEW.privacidad IS DISTINCT FROM OLD.privacidad THEN
        v_evento := 'PROYECTO_ESTADO_CAMBIADO';
        IF NEW.estado IS DISTINCT FROM OLD.estado THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'estado', 'anterior', OLD.estado, 'nuevo', NEW.estado));
        END IF;
        IF NEW.privacidad IS DISTINCT FROM OLD.privacidad THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'privacidad', 'anterior', OLD.privacidad, 'nuevo', NEW.privacidad));
        END IF;
        IF NEW.motivo_cancelacion IS DISTINCT FROM OLD.motivo_cancelacion THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'motivoCancelacion', 'anterior', OLD.motivo_cancelacion, 'nuevo', NEW.motivo_cancelacion));
        END IF;
    ELSIF NEW.fecha_inicio IS DISTINCT FROM OLD.fecha_inicio
          OR NEW.fecha_finalizacion IS DISTINCT FROM OLD.fecha_finalizacion THEN
        v_evento := 'PROYECTO_REPROGRAMADO';
        IF NEW.fecha_inicio IS DISTINCT FROM OLD.fecha_inicio THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'fechaInicio', 'anterior', OLD.fecha_inicio::text, 'nuevo', NEW.fecha_inicio::text));
        END IF;
        IF NEW.fecha_finalizacion IS DISTINCT FROM OLD.fecha_finalizacion THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'fechaFinalizacion', 'anterior', OLD.fecha_finalizacion::text, 'nuevo', NEW.fecha_finalizacion::text));
        END IF;
    ELSE
        IF NEW.nombre IS DISTINCT FROM OLD.nombre THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'nombre', 'anterior', OLD.nombre, 'nuevo', NEW.nombre));
        END IF;
        IF NEW.descripcion IS DISTINCT FROM OLD.descripcion THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'descripcion', 'anterior', OLD.descripcion, 'nuevo', NEW.descripcion));
        END IF;
        IF NEW.acepta_postulacion_gral IS DISTINCT FROM OLD.acepta_postulacion_gral THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'aceptaPostulacionGral', 'anterior', OLD.acepta_postulacion_gral::text, 'nuevo', NEW.acepta_postulacion_gral::text));
        END IF;
        IF NEW.id_ubicacion IS DISTINCT FROM OLD.id_ubicacion THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'idUbicacion', 'anterior', OLD.id_ubicacion::text, 'nuevo', NEW.id_ubicacion::text));
        END IF;

        IF jsonb_array_length(v_detalle) = 0 THEN
            RETURN NEW;
        END IF;
        v_evento := 'PROYECTO_MODIFICADO';
    END IF;

    PERFORM fn_auditoria_registrar('proyecto', NEW.id_proyecto, v_evento, 'UPDATE', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_proyecto ON proyecto;
CREATE TRIGGER trg_auditar_cambio_proyecto
    AFTER INSERT OR UPDATE OR DELETE ON proyecto
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_proyecto();

-- ----------------------------------------------------------------------------
-- 7. Actividades y asignaciones
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_actividad() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_detalle jsonb := '[]'::jsonb;
    v_id_proyecto bigint;
    v_id_planificacion bigint;
BEGIN
    v_id_planificacion := CASE WHEN TG_OP = 'DELETE' THEN OLD.id_planificacion ELSE NEW.id_planificacion END;

    SELECT p.id_proyecto INTO v_id_proyecto
    FROM planificacion pl
    JOIN proyecto p ON p.id_proyecto = pl.id_proyecto
    WHERE pl.id_planificacion = v_id_planificacion;

    IF TG_OP = 'INSERT' THEN
        v_evento := 'ACTIVIDAD_CREADA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idProyecto', 'nuevo', v_id_proyecto::text),
            jsonb_build_object('campo', 'nombre', 'nuevo', NEW.nombre),
            jsonb_build_object('campo', 'duracionMinutos', 'nuevo', NEW.duracion_minutos::text),
            jsonb_build_object('campo', 'estado', 'nuevo', NEW.estado)
        );
        PERFORM fn_auditoria_registrar('actividad', NEW.id_actividad, v_evento, 'INSERT', v_detalle);
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        v_evento := 'ACTIVIDAD_ELIMINADA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idProyecto', 'nuevo', v_id_proyecto::text),
            jsonb_build_object('campo', 'nombre', 'anterior', OLD.nombre, 'nuevo', NULL)
        );
        PERFORM fn_auditoria_registrar('actividad', OLD.id_actividad, v_evento, 'DELETE', v_detalle);
        RETURN OLD;
    END IF;

    IF NEW.estado IS DISTINCT FROM OLD.estado THEN
        v_evento := 'ACTIVIDAD_ESTADO_CAMBIADO';
        v_detalle := v_detalle || jsonb_build_array(
            jsonb_build_object('campo', 'estado', 'anterior', OLD.estado, 'nuevo', NEW.estado));
    ELSIF NEW.fecha_hora_inicio IS DISTINCT FROM OLD.fecha_hora_inicio THEN
        v_evento := 'ACTIVIDAD_REPROGRAMADA';
        v_detalle := v_detalle || jsonb_build_array(
            jsonb_build_object('campo', 'fechaHoraInicio', 'anterior', OLD.fecha_hora_inicio::text, 'nuevo', NEW.fecha_hora_inicio::text));
    ELSE
        IF NEW.nombre IS DISTINCT FROM OLD.nombre THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'nombre', 'anterior', OLD.nombre, 'nuevo', NEW.nombre));
        END IF;
        IF NEW.duracion_minutos IS DISTINCT FROM OLD.duracion_minutos THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'duracionMinutos', 'anterior', OLD.duracion_minutos::text, 'nuevo', NEW.duracion_minutos::text));
        END IF;
        IF NEW.descripcion IS DISTINCT FROM OLD.descripcion THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'descripcion', 'anterior', OLD.descripcion, 'nuevo', NEW.descripcion));
        END IF;
        IF NEW.horario_preferencia_inicio IS DISTINCT FROM OLD.horario_preferencia_inicio THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'horarioPreferenciaInicio', 'anterior', OLD.horario_preferencia_inicio::text, 'nuevo', NEW.horario_preferencia_inicio::text));
        END IF;
        IF NEW.id_ubicacion IS DISTINCT FROM OLD.id_ubicacion THEN
            v_detalle := v_detalle || jsonb_build_array(
                jsonb_build_object('campo', 'idUbicacion', 'anterior', OLD.id_ubicacion::text, 'nuevo', NEW.id_ubicacion::text));
        END IF;

        IF jsonb_array_length(v_detalle) = 0 THEN
            RETURN NEW;
        END IF;
        v_evento := 'ACTIVIDAD_MODIFICADA';
    END IF;

    PERFORM fn_auditoria_registrar('actividad', NEW.id_actividad, v_evento, 'UPDATE', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_actividad ON actividad;
CREATE TRIGGER trg_auditar_cambio_actividad
    AFTER INSERT OR UPDATE OR DELETE ON actividad
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_actividad();

CREATE OR REPLACE FUNCTION fn_auditar_cambio_asignacion() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_id_proyecto bigint;
    v_detalle jsonb;
BEGIN
    SELECT p.id_proyecto INTO v_id_proyecto
    FROM actividad a
    JOIN planificacion pl ON pl.id_planificacion = a.id_planificacion
    JOIN proyecto p ON p.id_proyecto = pl.id_proyecto
    WHERE a.id_actividad = CASE WHEN TG_OP = 'DELETE' THEN OLD.id_actividad ELSE NEW.id_actividad END;

    IF TG_OP = 'INSERT' THEN
        v_evento := 'ASIGNACION_ACTIVIDAD_ALTA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idProyecto', 'nuevo', v_id_proyecto::text),
            jsonb_build_object('campo', 'idActividad', 'nuevo', NEW.id_actividad::text),
            jsonb_build_object('campo', 'idMiembro', 'nuevo', NEW.id_miembro::text),
            jsonb_build_object('campo', 'idRequerimientoAct', 'nuevo', NEW.id_requerimiento_act::text)
        );
        PERFORM fn_auditoria_registrar('asignacion_actividad', NEW.id_asignacion_act, v_evento, 'INSERT', v_detalle);
        RETURN NEW;
    END IF;

    v_evento := 'ASIGNACION_ACTIVIDAD_BAJA';
    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', 'idProyecto', 'nuevo', v_id_proyecto::text),
        jsonb_build_object('campo', 'idActividad', 'anterior', OLD.id_actividad::text, 'nuevo', NULL),
        jsonb_build_object('campo', 'idMiembro', 'anterior', OLD.id_miembro::text, 'nuevo', NULL)
    );
    PERFORM fn_auditoria_registrar('asignacion_actividad', OLD.id_asignacion_act, v_evento, 'DELETE', v_detalle);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_asignacion ON asignacion_actividad;
CREATE TRIGGER trg_auditar_cambio_asignacion
    AFTER INSERT OR DELETE ON asignacion_actividad
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_asignacion();

-- ----------------------------------------------------------------------------
-- 8. Altas de postulaciones e invitaciones
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_alta_postulacion() RETURNS trigger AS $$
DECLARE
    v_id bigint;
    v_id_requerimiento bigint;
    v_detalle jsonb;
BEGIN
    IF TG_TABLE_NAME = 'postulacion_gral' THEN
        v_id := NEW.id_postulacion_gral;
        v_id_requerimiento := NEW.id_requerimiento_gral;
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idPerfil', 'nuevo', NEW.id_perfil::text),
            jsonb_build_object('campo', 'idRequerimientoGral', 'nuevo', v_id_requerimiento::text),
            jsonb_build_object('campo', 'estado', 'nuevo', NEW.estado),
            jsonb_build_object('campo', 'fechaPostulacion', 'nuevo', NEW.fecha_postulacion::text)
        );
    ELSE
        v_id := NEW.id_postulacion;
        v_id_requerimiento := NEW.id_requerimiento_act;
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idPerfil', 'nuevo', NEW.id_perfil::text),
            jsonb_build_object('campo', 'idRequerimientoAct', 'nuevo', v_id_requerimiento::text),
            jsonb_build_object('campo', 'estado', 'nuevo', NEW.estado),
            jsonb_build_object('campo', 'fechaPostulacion', 'nuevo', NEW.fecha_postulacion::text)
        );
    END IF;

    PERFORM fn_auditoria_registrar(
        TG_TABLE_NAME::varchar, v_id, 'POSTULACION_CREADA', 'INSERT', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_alta_postulacion_gral ON postulacion_gral;
CREATE TRIGGER trg_auditar_alta_postulacion_gral
    AFTER INSERT ON postulacion_gral
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_alta_postulacion();

DROP TRIGGER IF EXISTS trg_auditar_alta_postulacion_act ON postulacion_actividad;
CREATE TRIGGER trg_auditar_alta_postulacion_act
    AFTER INSERT ON postulacion_actividad
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_alta_postulacion();

CREATE OR REPLACE FUNCTION fn_auditar_alta_invitacion() RETURNS trigger AS $$
DECLARE
    v_id bigint;
    v_id_proyecto bigint;
    v_id_requerimiento bigint;
    v_detalle jsonb;
BEGIN
    IF TG_TABLE_NAME = 'invitacion_gral' THEN
        v_id := NEW.id_invitacion_gral;
        v_id_proyecto := NEW.id_proyecto;
        v_id_requerimiento := NEW.id_requerimiento_gral;
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idProyecto', 'nuevo', v_id_proyecto::text),
            jsonb_build_object('campo', 'idPerfilDestinatario', 'nuevo', NEW.id_perfil_destinatario::text),
            jsonb_build_object('campo', 'idPerfilRemitente', 'nuevo', NEW.id_perfil_remitente::text),
            jsonb_build_object('campo', 'idRequerimientoGral', 'nuevo', v_id_requerimiento::text),
            jsonb_build_object('campo', 'estado', 'nuevo', NEW.estado),
            jsonb_build_object('campo', 'idTycAceptado', 'nuevo', NEW.id_tyc_aceptado::text)
        );
    ELSE
        v_id := NEW.id_invitacion;
        v_id_proyecto := NULL;
        v_id_requerimiento := NEW.id_requerimiento_act;
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idPerfilDestinatario', 'nuevo', NEW.id_perfil_destinatario::text),
            jsonb_build_object('campo', 'idPerfilRemitente', 'nuevo', NEW.id_perfil_remitente::text),
            jsonb_build_object('campo', 'idRequerimientoAct', 'nuevo', v_id_requerimiento::text),
            jsonb_build_object('campo', 'estado', 'nuevo', NEW.estado),
            jsonb_build_object('campo', 'idTycAceptado', 'nuevo', NEW.id_tyc_aceptado::text)
        );
    END IF;

    PERFORM fn_auditoria_registrar(
        TG_TABLE_NAME::varchar, v_id, 'INVITACION_CREADA', 'INSERT', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_alta_invitacion_gral ON invitacion_gral;
CREATE TRIGGER trg_auditar_alta_invitacion_gral
    AFTER INSERT ON invitacion_gral
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_alta_invitacion();

DROP TRIGGER IF EXISTS trg_auditar_alta_invitacion_act ON invitacion_actividad;
CREATE TRIGGER trg_auditar_alta_invitacion_act
    AFTER INSERT ON invitacion_actividad
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_alta_invitacion();

-- ----------------------------------------------------------------------------
-- 9. Reemplazos
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_reemplazo() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_detalle jsonb;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_evento := 'REEMPLAZO_INICIADO';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idProyecto', 'nuevo', NEW.id_proyecto::text),
            jsonb_build_object('campo', 'tipo', 'nuevo', NEW.tipo),
            jsonb_build_object('campo', 'origen', 'nuevo', NEW.origen),
            jsonb_build_object('campo', 'estado', 'nuevo', NEW.estado),
            jsonb_build_object('campo', 'idMiembroBaja', 'nuevo', NEW.id_miembro_baja::text),
            jsonb_build_object('campo', 'idPerfilBaja', 'nuevo', NEW.id_perfil_baja::text)
        );
        PERFORM fn_auditoria_registrar('reemplazo', NEW.id_reemplazo, v_evento, 'INSERT', v_detalle);
        RETURN NEW;
    END IF;

    IF NEW.estado IS DISTINCT FROM OLD.estado THEN
        v_evento := CASE NEW.estado
            WHEN 'CANCELADO' THEN 'REEMPLAZO_CANCELADO'
            WHEN 'CUBIERTO' THEN 'REEMPLAZO_CUBIERTO'
            ELSE NULL
        END;
        IF v_evento IS NOT NULL THEN
            v_detalle := jsonb_build_array(
                jsonb_build_object('campo', 'estado', 'anterior', OLD.estado, 'nuevo', NEW.estado),
                jsonb_build_object('campo', 'motivoCierre', 'anterior', OLD.motivo_cierre, 'nuevo', NEW.motivo_cierre)
            );
            PERFORM fn_auditoria_registrar('reemplazo', NEW.id_reemplazo, v_evento, 'UPDATE', v_detalle);
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_reemplazo ON reemplazo;
CREATE TRIGGER trg_auditar_cambio_reemplazo
    AFTER INSERT OR UPDATE OF estado ON reemplazo
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_reemplazo();

CREATE OR REPLACE FUNCTION fn_auditar_cambio_candidato_reemplazo() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_detalle jsonb;
BEGIN
    IF TG_OP = 'INSERT' OR NEW.estado IS NOT DISTINCT FROM OLD.estado THEN
        RETURN NEW;
    END IF;

    v_evento := CASE NEW.estado
        WHEN 'APROBADO' THEN 'REEMPLAZO_CANDIDATO_APROBADO'
        WHEN 'RECHAZADO' THEN 'REEMPLAZO_CANDIDATO_RECHAZADO'
        WHEN 'RECHAZADO_ADMIN' THEN 'REEMPLAZO_CANDIDATO_RECHAZADO'
        WHEN 'CANCELADO' THEN 'REEMPLAZO_CANDIDATO_CANCELADO'
        ELSE NULL
    END;

    IF v_evento IS NULL THEN
        RETURN NEW;
    END IF;

    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', 'idReemplazo', 'nuevo', NEW.id_reemplazo::text),
        jsonb_build_object('campo', 'idProyecto', 'nuevo', NEW.id_proyecto::text),
        jsonb_build_object('campo', 'idPerfil', 'nuevo', NEW.id_perfil::text),
        jsonb_build_object('campo', 'estado', 'anterior', OLD.estado, 'nuevo', NEW.estado),
        jsonb_build_object('campo', 'motivoDecision', 'anterior', OLD.motivo_decision, 'nuevo', NEW.motivo_decision)
    );

    PERFORM fn_auditoria_registrar('reemplazo_candidato', NEW.id_candidato, v_evento, 'UPDATE', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_candidato_reemplazo ON reemplazo_candidato;
CREATE TRIGGER trg_auditar_cambio_candidato_reemplazo
    AFTER UPDATE OF estado ON reemplazo_candidato
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_candidato_reemplazo();

-- ----------------------------------------------------------------------------
-- 10. Calendario
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_cambio_jornada() RETURNS trigger AS $$
DECLARE
    v_detalle jsonb;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idJornada', 'anterior', OLD.id_jornada::text, 'nuevo', NULL),
            jsonb_build_object('campo', 'diaSemana', 'anterior', OLD.dia_semana::text, 'nuevo', NULL)
        );
        PERFORM fn_auditoria_registrar('agenda', OLD.id_agenda, 'CALENDARIO_JORNADA_ACTUALIZADA', 'DELETE', v_detalle);
        RETURN OLD;
    END IF;

    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', 'idJornada', 'nuevo', NEW.id_jornada::text),
        jsonb_build_object('campo', 'diaSemana', 'nuevo', NEW.dia_semana::text),
        jsonb_build_object('campo', 'horaInicioManana', 'nuevo', NEW.hora_inicio_manana::text),
        jsonb_build_object('campo', 'horaFinManana', 'nuevo', NEW.hora_fin_manana::text),
        jsonb_build_object('campo', 'horaInicioTarde', 'nuevo', NEW.hora_inicio_tarde::text),
        jsonb_build_object('campo', 'horaFinTarde', 'nuevo', NEW.hora_fin_tarde::text)
    );
    PERFORM fn_auditoria_registrar('agenda', NEW.id_agenda, 'CALENDARIO_JORNADA_ACTUALIZADA', TG_OP, v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_jornada ON jornada_agenda;
CREATE TRIGGER trg_auditar_cambio_jornada
    AFTER INSERT OR UPDATE OR DELETE ON jornada_agenda
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_jornada();

CREATE OR REPLACE FUNCTION fn_auditar_cambio_bloqueo() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_detalle jsonb;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_evento := 'CALENDARIO_BLOQUEO_ALTA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idAgenda', 'nuevo', NEW.id_agenda::text),
            jsonb_build_object('campo', 'fechaHoraInicio', 'nuevo', NEW.fecha_hora_inicio::text),
            jsonb_build_object('campo', 'fechaHoraFin', 'nuevo', NEW.fecha_hora_fin::text),
            jsonb_build_object('campo', 'motivo', 'nuevo', NEW.motivo)
        );
        PERFORM fn_auditoria_registrar('bloqueo_agenda', NEW.id_bloqueo, v_evento, 'INSERT', v_detalle);
        RETURN NEW;
    END IF;

    v_evento := 'CALENDARIO_BLOQUEO_BAJA';
    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', 'idAgenda', 'nuevo', OLD.id_agenda::text),
        jsonb_build_object('campo', 'fechaHoraInicio', 'anterior', OLD.fecha_hora_inicio::text, 'nuevo', NULL),
        jsonb_build_object('campo', 'fechaHoraFin', 'anterior', OLD.fecha_hora_fin::text, 'nuevo', NULL),
        jsonb_build_object('campo', 'motivo', 'anterior', OLD.motivo, 'nuevo', NULL)
    );
    PERFORM fn_auditoria_registrar('bloqueo_agenda', OLD.id_bloqueo, v_evento, 'DELETE', v_detalle);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_bloqueo ON bloqueo_agenda;
CREATE TRIGGER trg_auditar_cambio_bloqueo
    AFTER INSERT OR DELETE ON bloqueo_agenda
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_bloqueo();

-- ----------------------------------------------------------------------------
-- 11. Calificaciones
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_calificacion() RETURNS trigger AS $$
DECLARE
    v_detalle jsonb;
BEGIN
    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', 'idProyecto', 'nuevo', NEW.id_proyecto::text),
        jsonb_build_object('campo', 'idPerfilEmisor', 'nuevo', NEW.id_perfil_emisor::text),
        jsonb_build_object('campo', 'idPerfilReceptor', 'nuevo', NEW.id_perfil_receptor::text),
        jsonb_build_object('campo', 'valoracion', 'nuevo', NEW.valoracion::text)
    );
    PERFORM fn_auditoria_registrar(
        'calificacion_perfil', NEW.id_calificacion, 'CALIFICACION_REGISTRADA', 'INSERT', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_calificacion ON calificacion_perfil;
CREATE TRIGGER trg_auditar_calificacion
    AFTER INSERT ON calificacion_perfil
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_calificacion();

-- ----------------------------------------------------------------------------
-- 12. Catalogos administrativos
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditar_catalogo() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_id bigint;
    v_detalle jsonb := '[]'::jsonb;
    v_nuevo jsonb;
    v_anterior jsonb;
    v_campo text;
    v_campos text[] := ARRAY[
        'codigo', 'nombre', 'descripcion', 'tipo_dato', 'id_profesion',
        'id_unidad', 'simbolo', 'tipo_dato_permitido', 'etiqueta',
        'color_hex', 'id_caracteristica'
    ];
BEGIN
    v_nuevo := CASE WHEN TG_OP = 'DELETE' THEN NULL ELSE to_jsonb(NEW) END;
    v_anterior := CASE WHEN TG_OP = 'INSERT' THEN NULL ELSE to_jsonb(OLD) END;

    v_evento := CASE TG_OP
        WHEN 'INSERT' THEN 'CATALOGO_ALTA'
        WHEN 'UPDATE' THEN 'CATALOGO_MODIFICACION'
        ELSE 'CATALOGO_BAJA'
    END;

    v_id := COALESCE(
        CASE WHEN v_nuevo IS NOT NULL THEN (v_nuevo->>TG_ARGV[0])::bigint END,
        CASE WHEN v_anterior IS NOT NULL THEN (v_anterior->>TG_ARGV[0])::bigint END
    );

    FOREACH v_campo IN ARRAY v_campos LOOP
        IF (v_nuevo IS NOT NULL AND v_nuevo ? v_campo)
           OR (v_anterior IS NOT NULL AND v_anterior ? v_campo) THEN
            IF v_nuevo IS NULL
               OR v_anterior IS NULL
               OR (v_nuevo->>v_campo) IS DISTINCT FROM (v_anterior->>v_campo) THEN
                v_detalle := v_detalle || jsonb_build_array(
                    jsonb_build_object(
                        'campo', v_campo,
                        'anterior', v_anterior->>v_campo,
                        'nuevo', v_nuevo->>v_campo
                    )
                );
            END IF;
        END IF;
    END LOOP;

    PERFORM fn_auditoria_registrar(TG_TABLE_NAME::varchar, v_id, v_evento, TG_OP, v_detalle);

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_catalogo_profesion ON profesion;
CREATE TRIGGER trg_auditar_catalogo_profesion
    AFTER INSERT OR UPDATE OR DELETE ON profesion
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_catalogo('id_profesion');

DROP TRIGGER IF EXISTS trg_auditar_catalogo_habilidad ON habilidad;
CREATE TRIGGER trg_auditar_catalogo_habilidad
    AFTER INSERT OR UPDATE OR DELETE ON habilidad
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_catalogo('id_habilidad');

DROP TRIGGER IF EXISTS trg_auditar_catalogo_caracteristica ON caracteristica_tecnica;
CREATE TRIGGER trg_auditar_catalogo_caracteristica
    AFTER INSERT OR UPDATE OR DELETE ON caracteristica_tecnica
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_catalogo('id_caracteristica');

DROP TRIGGER IF EXISTS trg_auditar_catalogo_valor ON valor_caracteristica;
CREATE TRIGGER trg_auditar_catalogo_valor
    AFTER INSERT OR UPDATE OR DELETE ON valor_caracteristica
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_catalogo('id_valor');

DROP TRIGGER IF EXISTS trg_auditar_catalogo_unidad ON unidad_medida;
CREATE TRIGGER trg_auditar_catalogo_unidad
    AFTER INSERT OR UPDATE OR DELETE ON unidad_medida
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_catalogo('id_unidad');
