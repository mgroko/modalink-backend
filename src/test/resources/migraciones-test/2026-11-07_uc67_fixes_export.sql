-- ============================================================
-- UC-67: correcciones de filtros, legado y exportación
-- Migración idempotente para bases existentes.
-- Ejecutar con la aplicación detenida.
-- ============================================================

BEGIN;

-- 1. Columna para preservar el código original del evento.
ALTER TABLE auditoria ADD COLUMN IF NOT EXISTS tipo_evento_original varchar(60);

-- 2. Catálogo: evento no clasificado.
INSERT INTO tipo_evento_auditoria (codigo, nombre, descripcion, modulo, activo) VALUES
    ('OTRO', 'Evento no clasificado', 'Código de evento no presente en el catálogo', 'SISTEMA', true)
ON CONFLICT (codigo) DO UPDATE SET activo = true;

-- 3. Configuraciones del sistema (sin hardcodeos en Java).
INSERT INTO configuracion_sistema (clave, valor, descripcion) VALUES
    ('AUDITORIA_EXPORT_MAX_REGISTROS', '10000', 'Máximo de registros exportables en UC-67'),
    ('AUDITORIA_PAGINA_MAX', '100', 'Tamaño máximo de página permitido en el listado de auditoría (UC-67)'),
    ('AUDITORIA_PDF_MAX_CAMBIOS_POR_FILA', '5', 'Máximo de cambios mostrados por fila en el PDF de auditoría (UC-67)')
ON CONFLICT (clave) DO NOTHING;

-- 4. Función de resolución de eventos.
-- Resuelve el cÃ³digo de evento contra el catÃ¡logo. Si el cÃ³digo no estÃ¡
-- catalogado, se guarda como OTRO sin romper la operaciÃ³n de negocio.
CREATE OR REPLACE FUNCTION fn_evento_auditoria(p_codigo varchar)
RETURNS varchar AS $$
BEGIN
    IF p_codigo IS NULL THEN
        RETURN NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM tipo_evento_auditoria WHERE codigo = p_codigo
    ) THEN
        RETURN p_codigo;
    END IF;

    RAISE WARNING 'Evento de auditorÃ­a no catalogado: %', p_codigo;

    IF EXISTS (
        SELECT 1 FROM tipo_evento_auditoria WHERE codigo = 'OTRO'
    ) THEN
        RETURN 'OTRO';
    END IF;

    RETURN NULL;
END $$ LANGUAGE plpgsql STABLE;

-- 5. Triggers de auditoría (misma lógica que el esquema canónico).
CREATE OR REPLACE FUNCTION fn_auditar_cambio_proyecto() RETURNS trigger AS $$
DECLARE
    v_id_tipo_auditoria BIGINT;
    v_id_auditoria BIGINT;
    v_id_usuario BIGINT;
BEGIN
    -- Obtener id_tipo_auditoria segÃºn la acciÃ³n (TG_OP en PostgreSQL es siempre 'INSERT', 'UPDATE' o 'DELETE' en mayÃºsculas)
    SELECT id_tipo_auditoria INTO v_id_tipo_auditoria
    FROM tipo_accion_auditoria 
    WHERE nombre = TG_OP;

    IF v_id_tipo_auditoria IS NULL THEN
        RAISE EXCEPTION 'No se encontrÃ³ el tipo de acciÃ³n de auditorÃ­a para la operaciÃ³n %', TG_OP;
    END IF;

    IF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND (OLD.estado <> NEW.estado OR OLD.privacidad <> NEW.privacidad)) THEN

        v_id_usuario := fn_auditoria_usuario_actual();

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
            'proyecto',
            CASE WHEN TG_OP = 'DELETE' THEN OLD.id_proyecto ELSE NEW.id_proyecto END,
            clock_timestamp(),
            inet_client_addr()::varchar,
            v_id_tipo_auditoria,
            v_id_usuario,
            fn_evento_auditoria('PROYECTO_ESTADO_CAMBIADO'),
            'PROYECTO_ESTADO_CAMBIADO'
        ) RETURNING id_auditoria INTO v_id_auditoria;

        IF TG_OP = 'UPDATE' AND OLD.estado <> NEW.estado THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('estado', OLD.estado, NEW.estado, v_id_auditoria);
        END IF;

        IF TG_OP = 'UPDATE' AND OLD.privacidad <> NEW.privacidad THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('privacidad', OLD.privacidad, NEW.privacidad, v_id_auditoria);
        END IF;

        IF TG_OP = 'UPDATE' AND NEW.estado = 'CANCELADO'
                AND OLD.motivo_cancelacion IS DISTINCT FROM NEW.motivo_cancelacion THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('motivo_cancelacion', OLD.motivo_cancelacion, NEW.motivo_cancelacion, v_id_auditoria);
        END IF;

        IF TG_OP = 'DELETE' THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('registro', OLD.nombre, 'ELIMINADO', v_id_auditoria);
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_proyecto ON proyecto;
CREATE TRIGGER trg_auditar_cambio_proyecto
    AFTER UPDATE OF estado, privacidad OR DELETE ON proyecto
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_proyecto();

CREATE OR REPLACE FUNCTION fn_auditar_cambio_usuario() RETURNS trigger AS $$
DECLARE
    v_id_tipo_auditoria BIGINT;
    v_id_auditoria BIGINT;
    v_id_usuario BIGINT;
    v_id_afectado BIGINT;
    v_tipo_evento VARCHAR(60);
BEGIN
    -- Obtener id_tipo_auditoria segÃºn la acciÃ³n (TG_OP en PostgreSQL es siempre 'INSERT', 'UPDATE' o 'DELETE' en mayÃºsculas)
    SELECT id_tipo_auditoria INTO v_id_tipo_auditoria
    FROM tipo_accion_auditoria 
    WHERE nombre = TG_OP;

    IF v_id_tipo_auditoria IS NULL THEN
        RAISE EXCEPTION 'No se encontrÃ³ el tipo de acciÃ³n de auditorÃ­a para la operaciÃ³n %', TG_OP;
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
    ELSE
        IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
    END IF;

    v_id_afectado := CASE WHEN TG_OP = 'DELETE' THEN OLD.id_usuario ELSE NEW.id_usuario END;

    -- Operador real: usuario autenticado de la app. Si no hay contexto,
    -- el alta y la autorreactivaciÃ³n se atribuyen al propio usuario.
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

        IF TG_OP = 'DELETE' THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('registro', OLD.correo, 'ELIMINADO', v_id_auditoria);
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_cambio_usuario ON usuario;
CREATE TRIGGER trg_auditar_cambio_usuario
    AFTER INSERT OR UPDATE OF estado, id_rol_global OR DELETE ON usuario
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_cambio_usuario();

-- Auditoria de reportes: alta y cambios de estado/resolucion.
CREATE OR REPLACE FUNCTION fn_auditar_reporte() RETURNS trigger AS $$
DECLARE
    v_id_tipo_auditoria BIGINT;
    v_id_auditoria BIGINT;
    v_id_usuario BIGINT;
BEGIN
    SELECT id_tipo_auditoria INTO v_id_tipo_auditoria
    FROM tipo_accion_auditoria
    WHERE nombre = TG_OP;

    IF v_id_tipo_auditoria IS NULL THEN
        RAISE EXCEPTION 'No se encontro el tipo de accion de auditoria para la operacion %', TG_OP;
    END IF;

    v_id_usuario := COALESCE(
        fn_auditoria_usuario_actual(),
        CASE
            WHEN TG_OP = 'INSERT' THEN NEW.id_usuario_autor
            ELSE NEW.id_admin_revisor
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
        'reporte',
        NEW.id_reporte,
        clock_timestamp(),
        inet_client_addr()::varchar,
        v_id_tipo_auditoria,
        v_id_usuario,
        fn_evento_auditoria('REPORTE_ESTADO_CAMBIADO'),
        'REPORTE_ESTADO_CAMBIADO'
    ) RETURNING id_auditoria INTO v_id_auditoria;

    IF TG_OP = 'INSERT' THEN
        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria) VALUES
            ('entidad_tipo', '', NEW.entidad_tipo, v_id_auditoria),
            ('entidad_id', '', NEW.entidad_id::varchar, v_id_auditoria),
            ('motivo', '', NEW.motivo, v_id_auditoria),
            ('estado', '', NEW.estado, v_id_auditoria);
    ELSE
        IF OLD.estado IS DISTINCT FROM NEW.estado THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('estado', OLD.estado, NEW.estado, v_id_auditoria);
        END IF;

        IF OLD.id_admin_revisor IS DISTINCT FROM NEW.id_admin_revisor THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES (
                'id_admin_revisor',
                COALESCE(OLD.id_admin_revisor::varchar, ''),
                COALESCE(NEW.id_admin_revisor::varchar, ''),
                v_id_auditoria
            );
        END IF;

        IF OLD.fecha_resolucion IS DISTINCT FROM NEW.fecha_resolucion THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES (
                'fecha_resolucion',
                COALESCE(OLD.fecha_resolucion::varchar, ''),
                COALESCE(NEW.fecha_resolucion::varchar, ''),
                v_id_auditoria
            );
        END IF;
    END IF;

    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_reporte ON reporte;
CREATE TRIGGER trg_auditar_reporte
    AFTER INSERT OR UPDATE OF estado, id_admin_revisor, fecha_resolucion ON reporte
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_reporte();

-- Auditoria de estado y baja fisica de postulaciones e invitaciones.
-- TG_ARGV[0] = columna PK, TG_ARGV[1] = evento de cambio de estado,
-- TG_ARGV[2] = evento de eliminacion.
CREATE OR REPLACE FUNCTION fn_auditar_estado_solicitud() RETURNS trigger AS $$
DECLARE
    v_id_tipo_auditoria BIGINT;
    v_id_auditoria BIGINT;
    v_id_afectado BIGINT;
    v_tipo_evento VARCHAR(60);
    v_estado_anterior TEXT;
    v_estado_nuevo TEXT;
    v_registro TEXT;
BEGIN
    SELECT id_tipo_auditoria INTO v_id_tipo_auditoria
    FROM tipo_accion_auditoria
    WHERE nombre = TG_OP;

    IF v_id_tipo_auditoria IS NULL THEN
        RAISE EXCEPTION 'No se encontrÃ³ el tipo de acciÃ³n de auditorÃ­a para la operaciÃ³n %', TG_OP;
    END IF;

    IF TG_OP = 'DELETE' THEN
        v_tipo_evento := TG_ARGV[2];
        v_id_afectado := (to_jsonb(OLD) ->> TG_ARGV[0])::bigint;
        v_registro := OLD.estado;
    ELSIF NEW.estado IS DISTINCT FROM OLD.estado THEN
        v_tipo_evento := TG_ARGV[1];
        v_id_afectado := (to_jsonb(NEW) ->> TG_ARGV[0])::bigint;
        v_estado_anterior := OLD.estado;
        v_estado_nuevo := NEW.estado;
    ELSE
        RETURN NEW;
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
        TG_TABLE_NAME,
        v_id_afectado,
        clock_timestamp(),
        inet_client_addr()::varchar,
        v_id_tipo_auditoria,
        fn_auditoria_usuario_actual(),
        fn_evento_auditoria(v_tipo_evento),
        v_tipo_evento
    ) RETURNING id_auditoria INTO v_id_auditoria;

    IF TG_OP = 'DELETE' THEN
        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
        VALUES ('registro', v_registro, 'ELIMINADO', v_id_auditoria);
    ELSE
        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
        VALUES ('estado', v_estado_anterior, v_estado_nuevo, v_id_auditoria);
    END IF;

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_estado_postulacion_gral ON postulacion_gral;
CREATE TRIGGER trg_auditar_estado_postulacion_gral
    AFTER UPDATE OF estado OR DELETE ON postulacion_gral
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_estado_solicitud(
        'id_postulacion_gral', 'POSTULACION_ESTADO_CAMBIADO', 'POSTULACION_ELIMINADA');

DROP TRIGGER IF EXISTS trg_auditar_estado_postulacion_actividad ON postulacion_actividad;
CREATE TRIGGER trg_auditar_estado_postulacion_actividad
    AFTER UPDATE OF estado OR DELETE ON postulacion_actividad
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_estado_solicitud(
        'id_postulacion', 'POSTULACION_ESTADO_CAMBIADO', 'POSTULACION_ELIMINADA');

DROP TRIGGER IF EXISTS trg_auditar_estado_invitacion_gral ON invitacion_gral;
CREATE TRIGGER trg_auditar_estado_invitacion_gral
    AFTER UPDATE OF estado OR DELETE ON invitacion_gral
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_estado_solicitud(
        'id_invitacion_gral', 'INVITACION_ESTADO_CAMBIADO', 'INVITACION_ELIMINADA');

DROP TRIGGER IF EXISTS trg_auditar_estado_invitacion_actividad ON invitacion_actividad;
CREATE TRIGGER trg_auditar_estado_invitacion_actividad
    AFTER UPDATE OF estado OR DELETE ON invitacion_actividad
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_estado_solicitud(
        'id_invitacion', 'INVITACION_ESTADO_CAMBIADO', 'INVITACION_ELIMINADA');

-- Auditoria de configuracion_sistema. Los textos largos se resumen para no
-- duplicar contenido sensible (por ejemplo, la plantilla de T&C).
CREATE OR REPLACE FUNCTION fn_auditoria_valor_config(p_clave varchar, p_valor text)
RETURNS text AS $$
BEGIN
    IF p_valor IS NULL THEN
        RETURN NULL;
    END IF;
    IF p_clave = 'PERFIL_TYC_PLANTILLA' THEN
        RETURN '[omitido: ' || char_length(p_valor) || ' caracteres, md5=' || md5(p_valor) || ']';
    END IF;
    IF char_length(p_valor) > 500 THEN
        RETURN left(p_valor, 500) || '...';
    END IF;
    RETURN p_valor;
END $$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION fn_auditar_configuracion() RETURNS trigger AS $$
DECLARE
    v_id_tipo_auditoria BIGINT;
    v_id_auditoria BIGINT;
    v_clave VARCHAR(100);
    v_valor_anterior TEXT;
    v_valor_nuevo TEXT;
    v_desc_anterior TEXT;
    v_desc_nuevo TEXT;
BEGIN
    IF TG_OP = 'UPDATE'
            AND NEW.valor IS NOT DISTINCT FROM OLD.valor
            AND NEW.descripcion IS NOT DISTINCT FROM OLD.descripcion THEN
        RETURN NEW;
    END IF;

    SELECT id_tipo_auditoria INTO v_id_tipo_auditoria
    FROM tipo_accion_auditoria
    WHERE nombre = TG_OP;

    IF v_id_tipo_auditoria IS NULL THEN
        RAISE EXCEPTION 'No se encontrÃ³ el tipo de acciÃ³n de auditorÃ­a para la operaciÃ³n %', TG_OP;
    END IF;

    IF TG_OP = 'DELETE' THEN
        v_clave := OLD.clave;
        v_valor_anterior := fn_auditoria_valor_config(OLD.clave, OLD.valor);
        v_valor_nuevo := 'ELIMINADO';
        v_desc_anterior := OLD.descripcion;
    ELSIF TG_OP = 'INSERT' THEN
        v_clave := NEW.clave;
        v_valor_nuevo := fn_auditoria_valor_config(NEW.clave, NEW.valor);
        v_desc_nuevo := NEW.descripcion;
    ELSE
        v_clave := NEW.clave;
        v_valor_anterior := fn_auditoria_valor_config(OLD.clave, OLD.valor);
        v_valor_nuevo := fn_auditoria_valor_config(NEW.clave, NEW.valor);
        v_desc_anterior := OLD.descripcion;
        v_desc_nuevo := NEW.descripcion;
    END IF;

    INSERT INTO auditoria (
        usuario_bd,
        entidad_afectada,
        entidad_clave,
        id_afectado,
        fecha_hora,
        ip_usuario,
        id_tipo_auditoria,
        id_usuario,
        tipo_evento,
        tipo_evento_original
    ) VALUES (
        CURRENT_USER,
        'configuracion_sistema',
        v_clave,
        0,
        clock_timestamp(),
        inet_client_addr()::varchar,
        v_id_tipo_auditoria,
        fn_auditoria_usuario_actual(),
        fn_evento_auditoria('CONFIGURACION_ACTUALIZADA'),
        'CONFIGURACION_ACTUALIZADA'
    ) RETURNING id_auditoria INTO v_id_auditoria;

    INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
    VALUES ('valor', v_valor_anterior, v_valor_nuevo, v_id_auditoria);

    IF TG_OP <> 'DELETE' AND v_desc_anterior IS DISTINCT FROM v_desc_nuevo THEN
        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
        VALUES ('descripcion', v_desc_anterior, v_desc_nuevo, v_id_auditoria);
    END IF;

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_configuracion ON configuracion_sistema;
CREATE TRIGGER trg_auditar_configuracion
    AFTER INSERT OR UPDATE OR DELETE ON configuracion_sistema
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_configuracion();

-- 6. Inmutabilidad y bloqueo de TRUNCATE.
-- Inmutabilidad del log: el rol de la API solo tiene INSERT/SELECT, y este
-- trigger agrega una segunda barrera para cualquier otro rol.
CREATE OR REPLACE FUNCTION fn_auditoria_inmutable() RETURNS trigger AS $$
BEGIN
    -- Bypass explÃ­cito y transaccional para tareas de mantenimiento.
    IF current_setting('app.auditoria_admin', true) = 'true' THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        ELSIF TG_OP = 'UPDATE' THEN
            RETURN NEW;
        ELSE
            RETURN NULL;
        END IF;
    END IF;

    RAISE EXCEPTION 'La tabla % es inmutable: no se permite %', TG_TABLE_NAME, TG_OP;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditoria_no_update_delete ON auditoria;
CREATE TRIGGER trg_auditoria_no_update_delete
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria_inmutable();

DROP TRIGGER IF EXISTS trg_detalle_auditoria_no_update_delete ON detalle_auditoria;
CREATE TRIGGER trg_detalle_auditoria_no_update_delete
    BEFORE UPDATE OR DELETE ON detalle_auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_no_truncate ON auditoria;
CREATE TRIGGER trg_auditoria_no_truncate
    BEFORE TRUNCATE ON auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION fn_auditoria_inmutable();

DROP TRIGGER IF EXISTS trg_detalle_auditoria_no_truncate ON detalle_auditoria;
CREATE TRIGGER trg_detalle_auditoria_no_truncate
    BEFORE TRUNCATE ON detalle_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION fn_auditoria_inmutable();

-- 7. Índice para el resumen por entidad.
CREATE INDEX IF NOT EXISTS idx_auditoria_entidad_fecha ON auditoria (entidad_afectada, fecha_hora DESC);

-- 8. Permisos explícitos del rol de la API (si existe).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'modalink_api') THEN
        GRANT SELECT, INSERT ON auditoria, detalle_auditoria TO modalink_api;
        REVOKE UPDATE, DELETE, TRUNCATE ON auditoria, detalle_auditoria FROM modalink_api;
    END IF;
END $$;

COMMIT;
