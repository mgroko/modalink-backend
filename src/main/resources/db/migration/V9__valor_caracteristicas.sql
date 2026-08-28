-- =====================================================================
-- ModaLink - Catálogo de valores para características técnicas enumerables
-- Migración Flyway: V9__valor_caracteristica.sql
--
-- 1) tipo_dato en caracteristica_tecnica: distingue características de
--    dominio cerrado (ENUMERADO) de las de texto libre o numéricas
-- 2) Tabla valor_caracteristica: catálogo de valores válidos para
--    características de tipo ENUMERADO
-- 3) caracteristica_perfil: agrega id_valor (FK a valor_caracteristica),
--    que reemplaza a valor para las características ENUMERADO
-- 4) Trigger de consistencia: exige valor XOR id_valor según tipo_dato,
--    y que id_valor pertenezca a la misma característica técnica
-- =====================================================================
 
-- ---------------------------------------------------------------------
-- 1) tipo_dato en caracteristica_tecnica
-- ---------------------------------------------------------------------
ALTER TABLE caracteristica_tecnica
    ADD COLUMN tipo_dato VARCHAR(20) NOT NULL DEFAULT 'TEXTO';
 
ALTER TABLE caracteristica_tecnica
    ADD CONSTRAINT chk_caracteristica_tecnica_tipo_dato
        CHECK (tipo_dato IN ('ENUMERADO', 'TEXTO', 'NUMERICO'));
 
-- ---------------------------------------------------------------------
-- 2) Tabla valor_caracteristica
--    color_hex es opcional: solo tiene sentido para valores que
--    representan un color (ojos, cabello, piel); para otras
--    características ENUMERADO futuras queda en NULL.
-- ---------------------------------------------------------------------
CREATE TABLE valor_caracteristica(
    id_valor             BIGSERIAL PRIMARY KEY,
    id_caracteristica    BIGINT NOT NULL REFERENCES caracteristica_tecnica(id_caracteristica),
    codigo                VARCHAR(50) NOT NULL,
    color_hex             VARCHAR(7),
    CONSTRAINT uq_valor_caracteristica_codigo UNIQUE (id_caracteristica, codigo),
    CONSTRAINT chk_valor_caracteristica_color_hex
        CHECK (color_hex IS NULL OR color_hex ~ '^#[0-9A-Fa-f]{6}$')
);
 
-- ---------------------------------------------------------------------
-- 3) caracteristica_perfil: agrega id_valor
-- ---------------------------------------------------------------------
ALTER TABLE caracteristica_perfil
    ADD COLUMN id_valor BIGINT REFERENCES valor_caracteristica(id_valor);
 
-- ---------------------------------------------------------------------
-- 4) Trigger de consistencia valor / id_valor según tipo_dato
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION chk_caracteristica_perfil_valor() RETURNS trigger AS $$
DECLARE
    v_tipo_dato VARCHAR(20);
BEGIN
    SELECT tipo_dato INTO v_tipo_dato
    FROM caracteristica_tecnica
    WHERE id_caracteristica = NEW.id_caracteristica;
 
    IF v_tipo_dato = 'ENUMERADO' THEN
        IF NEW.id_valor IS NULL OR NEW.valor IS NOT NULL THEN
            RAISE EXCEPTION 'Para características de tipo ENUMERADO debe cargarse id_valor y no valor.';
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM valor_caracteristica vc
            WHERE vc.id_valor = NEW.id_valor
              AND vc.id_caracteristica = NEW.id_caracteristica
        ) THEN
            RAISE EXCEPTION 'id_valor no pertenece a la característica técnica indicada.';
        END IF;
    ELSE
        IF NEW.valor IS NULL OR NEW.id_valor IS NOT NULL THEN
            RAISE EXCEPTION 'Para características de tipo TEXTO/NUMERICO debe cargarse valor y no id_valor.';
        END IF;
    END IF;
 
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
 
CREATE TRIGGER trg_caracteristica_perfil_valor
BEFORE INSERT OR UPDATE ON caracteristica_perfil
FOR EACH ROW EXECUTE FUNCTION chk_caracteristica_perfil_valor();