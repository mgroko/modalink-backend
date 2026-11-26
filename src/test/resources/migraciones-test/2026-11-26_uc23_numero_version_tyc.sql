-- UC-23 - Número de versión ordinal en perfil_tyc.
-- Migración para bases existentes. Ejecutar con la aplicación detenida.
--
-- Agrega `numero_version` por perfil (1..N), backfill por fecha de inicio,
-- índice único, trigger de respaldo y auditoría con el número de versión.
-- Es idempotente.

BEGIN;

ALTER TABLE perfil_tyc
    ADD COLUMN IF NOT EXISTS numero_version int;

WITH numeradas AS (
    SELECT
        id_perfil,
        id_tyc,
        ROW_NUMBER() OVER (
            PARTITION BY id_perfil
            ORDER BY fecha_inicio ASC, id_tyc ASC
        ) AS numero
      FROM perfil_tyc
     WHERE numero_version IS NULL
)
UPDATE perfil_tyc pt
   SET numero_version = n.numero
  FROM numeradas n
 WHERE pt.id_perfil = n.id_perfil
   AND pt.id_tyc = n.id_tyc;

ALTER TABLE perfil_tyc
    ALTER COLUMN numero_version SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_tyc_perfil_numero_version_positiva'
    ) THEN
        ALTER TABLE perfil_tyc
            ADD CONSTRAINT ck_tyc_perfil_numero_version_positiva
            CHECK (numero_version > 0);
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS "UQ_perfil_tyc_numero_version"
    ON perfil_tyc(id_perfil, numero_version);

CREATE OR REPLACE FUNCTION fn_asignar_numero_version_tyc()
RETURNS trigger AS $$
BEGIN
    IF NEW.numero_version IS NULL THEN
        SELECT COALESCE(MAX(numero_version), 0) + 1
          INTO NEW.numero_version
          FROM perfil_tyc
         WHERE id_perfil = NEW.id_perfil;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_asignar_numero_version_tyc ON perfil_tyc;
CREATE TRIGGER trg_asignar_numero_version_tyc
    BEFORE INSERT ON perfil_tyc
    FOR EACH ROW EXECUTE FUNCTION fn_asignar_numero_version_tyc();

CREATE OR REPLACE FUNCTION fn_auditar_cambio_tyc() RETURNS trigger AS $$
DECLARE
    v_evento varchar(60);
    v_detalle jsonb;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_evento := 'TERMINOS_VERSION_CREADA';
        v_detalle := jsonb_build_array(
            jsonb_build_object('campo', 'idTyc', 'nuevo', NEW.id_tyc::text),
            jsonb_build_object('campo', 'numeroVersion', 'nuevo', NEW.numero_version::text),
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
            jsonb_build_object('campo', 'numeroVersion', 'nuevo', NEW.numero_version::text),
            jsonb_build_object('campo', 'idPerfil', 'nuevo', NEW.id_perfil::text),
            jsonb_build_object('campo', 'fechaFin', 'anterior', OLD.fecha_fin::text, 'nuevo', NEW.fecha_fin::text)
        );
        PERFORM fn_auditoria_registrar('perfil', NEW.id_perfil, v_evento, 'UPDATE', v_detalle);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMIT;
