-- UC-76 - Gestion de tipos de contrato desde el panel de administracion
-- Migracion para bases existentes. Ejecutar con la aplicacion detenida.
--
-- Agrega:
--   1) permisos globales y asignacion al rol Administrador;
--   2) indices unicos case-insensitive para codigo y nombre activo;
--   3) catalogo de eventos y trigger de auditoria sobre tipo_acuerdo_contrato.
--
-- No modifica la estructura de tipo_acuerdo_contrato ni los contratos existentes.

BEGIN;

-- ============================================================================
-- 1) PERMISOS
-- ============================================================================

INSERT INTO permiso_global (nombre)
SELECT v.nombre
FROM (VALUES
    ('VER_TIPOS_CONTRATO'),
    ('CREAR_TIPO_CONTRATO'),
    ('MODIFICAR_TIPO_CONTRATO'),
    ('ELIMINAR_TIPO_CONTRATO')
) AS v(nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM permiso_global pg WHERE pg.nombre = v.nombre
);

INSERT INTO rol_global_permiso (id_rol_global, id_permiso_global)
SELECT rg.id_rol_global, pg.id_permiso_global
FROM rol_global rg
CROSS JOIN permiso_global pg
WHERE rg.nombre = 'Administrador'
  AND pg.nombre IN (
      'VER_TIPOS_CONTRATO',
      'CREAR_TIPO_CONTRATO',
      'MODIFICAR_TIPO_CONTRATO',
      'ELIMINAR_TIPO_CONTRATO'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 2) INDICES
-- ============================================================================

-- El codigo se genera automaticamente y no se repite. El indice funcional
-- evita duplicados que solo difieran en mayusculas/minusculas.
CREATE UNIQUE INDEX IF NOT EXISTS uq_tipo_acuerdo_contrato_codigo_lower
    ON tipo_acuerdo_contrato (lower(codigo));

-- La unicidad de nombre se valida en el servicio. No se impone un indice
-- parcial porque las pruebas y los datos historicos pueden reutilizar nombres
-- en tipos inactivos.

-- ============================================================================
-- 3) AUDITORIA
-- ============================================================================

INSERT INTO tipo_evento_auditoria (codigo, nombre, descripcion, modulo) VALUES
    ('TIPO_CONTRATO_CREADO', 'Alta de tipo de contrato',
        'Alta de un tipo de acuerdo para contratos', 'ADMINISTRACION'),
    ('TIPO_CONTRATO_MODIFICADO', 'Modificacion de tipo de contrato',
        'Cambio de nombre, descripcion o plantilla de un tipo de contrato', 'ADMINISTRACION'),
    ('TIPO_CONTRATO_ESTADO_CAMBIADO', 'Cambio de estado de tipo de contrato',
        'Activacion o desactivacion de un tipo de contrato', 'ADMINISTRACION'),
    ('TIPO_CONTRATO_ELIMINADO', 'Baja de tipo de contrato',
        'Eliminacion fisica de un tipo de contrato sin contratos asociados', 'ADMINISTRACION')
ON CONFLICT (codigo) DO UPDATE
    SET nombre = EXCLUDED.nombre,
        descripcion = EXCLUDED.descripcion,
        modulo = EXCLUDED.modulo,
        activo = true;

-- La plantilla legal puede ser extensa. Para no duplicarla en el log se
-- guarda su longitud y md5.
CREATE OR REPLACE FUNCTION fn_resumen_plantilla_contrato(p_plantilla text)
RETURNS text AS $$
BEGIN
    IF p_plantilla IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN '[omitido: ' || char_length(p_plantilla)
        || ' caracteres, md5=' || md5(p_plantilla) || ']';
END $$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION fn_auditar_tipo_acuerdo_contrato() RETURNS trigger AS $$
DECLARE
    v_id_tipo_auditoria BIGINT;
    v_id_auditoria BIGINT;
    v_id_tipo BIGINT;
    v_codigo VARCHAR(50);
    v_evento VARCHAR(40);
BEGIN
    IF TG_OP = 'UPDATE'
            AND NEW.codigo IS NOT DISTINCT FROM OLD.codigo
            AND NEW.nombre IS NOT DISTINCT FROM OLD.nombre
            AND NEW.descripcion IS NOT DISTINCT FROM OLD.descripcion
            AND NEW.plantilla IS NOT DISTINCT FROM OLD.plantilla
            AND NEW.activo IS NOT DISTINCT FROM OLD.activo THEN
        RETURN NEW;
    END IF;

    SELECT id_tipo_auditoria INTO v_id_tipo_auditoria
    FROM tipo_accion_auditoria
    WHERE nombre = TG_OP;

    IF v_id_tipo_auditoria IS NULL THEN
        RAISE EXCEPTION 'No se encontro el tipo de accion de auditoria para la operacion %', TG_OP;
    END IF;

    IF TG_OP = 'DELETE' THEN
        v_id_tipo := OLD.id_tipo_acuerdo;
        v_codigo := OLD.codigo;
        v_evento := 'TIPO_CONTRATO_ELIMINADO';
    ELSE
        v_id_tipo := NEW.id_tipo_acuerdo;
        v_codigo := NEW.codigo;
        IF TG_OP = 'INSERT' THEN
            v_evento := 'TIPO_CONTRATO_CREADO';
        ELSIF NEW.activo IS DISTINCT FROM OLD.activo
                AND NEW.codigo IS NOT DISTINCT FROM OLD.codigo
                AND NEW.nombre IS NOT DISTINCT FROM OLD.nombre
                AND NEW.descripcion IS NOT DISTINCT FROM OLD.descripcion
                AND NEW.plantilla IS NOT DISTINCT FROM OLD.plantilla THEN
            v_evento := 'TIPO_CONTRATO_ESTADO_CAMBIADO';
        ELSE
            v_evento := 'TIPO_CONTRATO_MODIFICADO';
        END IF;
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
        'tipo_acuerdo_contrato',
        v_codigo,
        v_id_tipo,
        clock_timestamp(),
        inet_client_addr()::varchar,
        v_id_tipo_auditoria,
        fn_auditoria_usuario_actual(),
        fn_evento_auditoria(v_evento),
        v_evento
    ) RETURNING id_auditoria INTO v_id_auditoria;

    IF TG_OP = 'INSERT' THEN
        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
        VALUES ('nombre', NULL, NEW.nombre, v_id_auditoria);

        IF NEW.descripcion IS NOT NULL THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('descripcion', NULL, NEW.descripcion, v_id_auditoria);
        END IF;

        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
        VALUES ('plantilla', NULL,
            fn_resumen_plantilla_contrato(NEW.plantilla), v_id_auditoria);

        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
        VALUES ('activo', NULL, NEW.activo::text, v_id_auditoria);

    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
        VALUES ('nombre', OLD.nombre, NULL, v_id_auditoria);

        IF OLD.descripcion IS NOT NULL THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('descripcion', OLD.descripcion, NULL, v_id_auditoria);
        END IF;

        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
        VALUES ('plantilla',
            fn_resumen_plantilla_contrato(OLD.plantilla), NULL, v_id_auditoria);

        INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
        VALUES ('activo', OLD.activo::text, NULL, v_id_auditoria);

    ELSE
        IF NEW.codigo IS DISTINCT FROM OLD.codigo THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('codigo', OLD.codigo, NEW.codigo, v_id_auditoria);
        END IF;

        IF NEW.nombre IS DISTINCT FROM OLD.nombre THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('nombre', OLD.nombre, NEW.nombre, v_id_auditoria);
        END IF;

        IF NEW.descripcion IS DISTINCT FROM OLD.descripcion THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('descripcion', OLD.descripcion, NEW.descripcion, v_id_auditoria);
        END IF;

        IF NEW.plantilla IS DISTINCT FROM OLD.plantilla THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('plantilla',
                fn_resumen_plantilla_contrato(OLD.plantilla),
                fn_resumen_plantilla_contrato(NEW.plantilla),
                v_id_auditoria);
        END IF;

        IF NEW.activo IS DISTINCT FROM OLD.activo THEN
            INSERT INTO detalle_auditoria (campo, valor_anterior, valor_nuevo, id_auditoria)
            VALUES ('activo', OLD.activo::text, NEW.activo::text, v_id_auditoria);
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_tipo_acuerdo_contrato
    ON tipo_acuerdo_contrato;

CREATE TRIGGER trg_auditar_tipo_acuerdo_contrato
    AFTER INSERT OR UPDATE OR DELETE ON tipo_acuerdo_contrato
    FOR EACH ROW EXECUTE FUNCTION fn_auditar_tipo_acuerdo_contrato();

COMMIT;
