-- UC-67 - Operador snapshot, busqueda de texto y auditoria de mensajes
-- Migracion para bases existentes. Ejecutar con la aplicacion detenida.
-- No backfillea filas historicas.

BEGIN;

-- ============================================================================
-- UC-67 - Operador snapshot, búsqueda de texto y auditoría de mensajes
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE OR REPLACE FUNCTION f_unaccent(text) RETURNS text AS $$
    SELECT public.unaccent('public.unaccent', $1)
$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT;

ALTER TABLE auditoria
    ADD COLUMN IF NOT EXISTS operador_nombre varchar(255);

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
    v_id_usuario bigint;
    v_operador_nombre varchar(255);
BEGIN
    SELECT id_tipo_auditoria INTO v_id_tipo_auditoria
    FROM tipo_accion_auditoria
    WHERE upper(nombre) = upper(p_accion);

    IF v_id_tipo_auditoria IS NULL THEN
        RAISE EXCEPTION 'No se encontro el tipo de accion de auditoria %', p_accion;
    END IF;

    v_id_usuario := fn_auditoria_usuario_actual();

    IF v_id_usuario IS NOT NULL THEN
        SELECT btrim(COALESCE(nombre, '') || ' ' || COALESCE(apellido, ''))
          INTO v_operador_nombre
          FROM usuario
         WHERE id_usuario = v_id_usuario;

        IF v_operador_nombre = '' THEN
            v_operador_nombre := NULL;
        END IF;
    END IF;

    INSERT INTO auditoria (
        usuario_bd,
        entidad_afectada,
        id_afectado,
        fecha_hora,
        ip_usuario,
        id_tipo_auditoria,
        id_usuario,
        operador_nombre,
        tipo_evento,
        tipo_evento_original
    ) VALUES (
        CURRENT_USER,
        p_entidad,
        p_id,
        clock_timestamp(),
        inet_client_addr()::varchar,
        v_id_tipo_auditoria,
        v_id_usuario,
        v_operador_nombre,
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

CREATE OR REPLACE FUNCTION fn_auditoria_completar_operador() RETURNS trigger AS $$
BEGIN
    IF NEW.operador_nombre IS NULL AND NEW.id_usuario IS NOT NULL THEN
        SELECT btrim(COALESCE(nombre, '') || ' ' || COALESCE(apellido, ''))
          INTO NEW.operador_nombre
          FROM usuario
         WHERE id_usuario = NEW.id_usuario;

        IF NEW.operador_nombre = '' THEN
            NEW.operador_nombre := NULL;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditoria_completar_operador ON auditoria;
CREATE TRIGGER trg_auditoria_completar_operador
    BEFORE INSERT ON auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria_completar_operador();

INSERT INTO tipo_evento_auditoria (codigo, nombre, descripcion, modulo) VALUES
    ('MENSAJE_PRIVADO_ENVIADO', 'Envío de mensaje privado', 'Alta de un mensaje en una conversación privada', 'MENSAJERIA'),
    ('MENSAJE_PROYECTO_ENVIADO', 'Envío de mensaje de proyecto', 'Alta de un mensaje en el chat grupal de un proyecto', 'MENSAJERIA')
ON CONFLICT (codigo) DO UPDATE
    SET nombre = EXCLUDED.nombre,
        descripcion = EXCLUDED.descripcion,
        modulo = EXCLUDED.modulo,
        activo = true;

CREATE OR REPLACE FUNCTION fn_auditar_mensaje_privado() RETURNS trigger AS $$
DECLARE
    v_detalle jsonb;
BEGIN
    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', 'idConversacion', 'nuevo', NEW.id_conversacion::text),
        jsonb_build_object('campo', 'idPerfilEmisor', 'nuevo', NEW.id_perfil_emisor::text),
        jsonb_build_object('campo', 'contenido', 'nuevo', NEW.contenido),
        jsonb_build_object('campo', 'fechaEnvio', 'nuevo', NEW.fecha_envio::text)
    );
    PERFORM fn_auditoria_registrar(
        'mensaje', NEW.id_mensaje, 'MENSAJE_PRIVADO_ENVIADO', 'INSERT', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_mensaje_privado ON mensaje;
CREATE TRIGGER trg_auditar_mensaje_privado
    AFTER INSERT ON mensaje
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_mensaje_privado();

CREATE OR REPLACE FUNCTION fn_auditar_mensaje_proyecto() RETURNS trigger AS $$
DECLARE
    v_detalle jsonb;
BEGIN
    v_detalle := jsonb_build_array(
        jsonb_build_object('campo', 'idProyecto', 'nuevo', NEW.id_proyecto::text),
        jsonb_build_object('campo', 'idPerfilEmisor', 'nuevo', NEW.id_perfil_emisor::text),
        jsonb_build_object('campo', 'contenido', 'nuevo', NEW.contenido),
        jsonb_build_object('campo', 'fechaEnvio', 'nuevo', NEW.fecha_envio::text)
    );
    PERFORM fn_auditoria_registrar(
        'mensaje_proyecto', NEW.id_mensaje_proyecto, 'MENSAJE_PROYECTO_ENVIADO', 'INSERT', v_detalle);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_mensaje_proyecto ON mensaje_proyecto;
CREATE TRIGGER trg_auditar_mensaje_proyecto
    AFTER INSERT ON mensaje_proyecto
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_mensaje_proyecto();

CREATE INDEX IF NOT EXISTS idx_auditoria_operador_nombre
    ON auditoria USING gin (lower(f_unaccent(operador_nombre)) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_auditoria_busqueda_metadata
    ON auditoria USING gin (
        lower(f_unaccent(
            coalesce(entidad_afectada, '') || ' ' ||
            coalesce(entidad_clave, '') || ' ' ||
            coalesce(tipo_evento, '') || ' ' ||
            coalesce(tipo_evento_original, '')
        )) gin_trgm_ops
    );

CREATE INDEX IF NOT EXISTS idx_detalle_auditoria_busqueda
    ON detalle_auditoria USING gin (
        lower(f_unaccent(
            coalesce(campo, '') || ' ' ||
            coalesce(valor_anterior, '') || ' ' ||
            coalesce(valor_nuevo, '')
        )) gin_trgm_ops
    );

COMMIT;
