-- =====================================================================
-- ModaLink - Prohibir valores numéricos negativos en características técnicas
-- Migración Flyway: V15__valor_caracteristica_no_negativo.sql
--
-- Las características de tipo NUMERICO (altura, pecho, cintura, cadera...)
-- guardan su valor como VARCHAR en caracteristica_perfil.valor. Este trigger
-- garantiza a nivel de base de datos que:
--   1) El valor sea un número válido (parseable como número).
--   2) El valor no sea negativo.
-- Aplica únicamente cuando la característica es de tipo NUMERICO; las de tipo
-- TEXTO no son validadas.
-- =====================================================================

CREATE OR REPLACE FUNCTION chk_caracteristica_valor_no_negativo() RETURNS trigger AS $$
DECLARE
    v_tipo_dato VARCHAR(20);
    v_numero    DOUBLE PRECISION;
BEGIN
    SELECT tipo_dato INTO v_tipo_dato
    FROM caracteristica_tecnica
    WHERE id_caracteristica = NEW.id_caracteristica;

    IF v_tipo_dato = 'NUMERICO' THEN
        IF NEW.valor IS NULL THEN
            RAISE EXCEPTION 'Para la característica técnica debe especificarse un valor numérico.';
        END IF;
        BEGIN
            v_numero := NEW.valor::double precision;
        EXCEPTION WHEN invalid_text_representation THEN
            RAISE EXCEPTION 'La característica técnica debe ser un número válido.';
        END;

        IF v_numero < 0 THEN
            RAISE EXCEPTION 'La característica técnica no admite valores negativos.';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_caracteristica_valor_no_negativo
BEFORE INSERT OR UPDATE ON caracteristica_perfil
FOR EACH ROW EXECUTE FUNCTION chk_caracteristica_valor_no_negativo();
