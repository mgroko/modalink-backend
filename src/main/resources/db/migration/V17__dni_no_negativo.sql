-- =====================================================================
-- ModaLink - Prohibir DNI con valor negativo o no numérico
-- Migración Flyway: V17__dni_no_negativo.sql
--
-- El DNI de usuario se guarda como VARCHAR. Este trigger garantiza a
-- nivel de base de datos que:
--   1) El valor sea un número válido (dígitos únicamente).
--   2) El valor no sea negativo.
-- Se complementa con validaciones a nivel de API (@Pattern) y de servicio.
-- =====================================================================

CREATE OR REPLACE FUNCTION chk_usuario_dni_no_negativo() RETURNS trigger AS $$
DECLARE
    v_numero BIGINT;
BEGIN
    IF NEW.dni IS NULL THEN
        RAISE EXCEPTION 'El DNI no puede ser nulo.';
    END IF;

    IF NOT NEW.dni ~ '^[0-9]+$' THEN
        RAISE EXCEPTION 'El DNI debe contener únicamente dígitos numéricos.';
    END IF;

    BEGIN
        v_numero := NEW.dni::bigint;
    EXCEPTION WHEN invalid_text_representation OR numeric_value_out_of_range THEN
        RAISE EXCEPTION 'El DNI debe ser un número válido.';
    END;

    IF v_numero < 0 THEN
        RAISE EXCEPTION 'El DNI no puede ser un número negativo.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuario_dni_no_negativo
BEFORE INSERT OR UPDATE ON usuario
FOR EACH ROW EXECUTE FUNCTION chk_usuario_dni_no_negativo();