-- =====================================================================
-- ModaLink - Reglas de negocio del perfil + características de "modelo"
-- Migración Flyway: V8__perfil_reglas_y_caracteristicas.sql
--
-- 1) Longitudes mínimas/máximas de nombre_artistico y biografia
-- 2) Un usuario no puede tener más de un perfil por profesión vigente
--    (los perfiles con estado 'Baja' no bloquean la creación de uno nuevo)
-- 3) Las características técnicas deben pertenecer a la profesión del perfil
-- 4) Seed de características técnicas para la profesión "modelo"
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) CHECK constraints de longitud
-- ---------------------------------------------------------------------
ALTER TABLE perfil ADD CONSTRAINT chk_perfil_nombre_artistico_len
    CHECK (char_length(trim(nombre_artistico)) BETWEEN 2 AND 50);

ALTER TABLE perfil ADD CONSTRAINT chk_perfil_biografia_len
    CHECK (char_length(trim(biografia)) BETWEEN 1 AND 500);

-- ---------------------------------------------------------------------
-- 2) Trigger: un perfil activo/pendiente de baja por profesión y usuario
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION chk_perfil_unico_por_profesion() RETURNS trigger AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM perfil
        WHERE id_usuario = NEW.id_usuario
          AND id_profesion = NEW.id_profesion
          AND estado IN ('Activo', 'PendienteBaja')
    ) THEN
        RAISE EXCEPTION 'El usuario ya posee un perfil para la profesión indicada.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_perfil_unico_por_profesion
BEFORE INSERT ON perfil
FOR EACH ROW EXECUTE FUNCTION chk_perfil_unico_por_profesion();

-- ---------------------------------------------------------------------
-- 3) Trigger: la característica debe corresponder a la profesión del perfil
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION chk_caracteristica_tecnica_de_profesion() RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM caracteristica_tecnica ct
        JOIN perfil p ON p.id_perfil = NEW.id_perfil
        WHERE ct.id_caracteristica = NEW.id_caracteristica
          AND ct.id_profesion = p.id_profesion
    ) THEN
        RAISE EXCEPTION 'La característica técnica no corresponde a la profesión del perfil.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_caracteristica_tecnica_de_profesion
BEFORE INSERT OR UPDATE ON caracteristica_perfil
FOR EACH ROW EXECUTE FUNCTION chk_caracteristica_tecnica_de_profesion();

-- ---------------------------------------------------------------------
-- 4) Seed: características técnicas de la profesión "modelo"
-- ---------------------------------------------------------------------
INSERT INTO caracteristica_tecnica (codigo, unidad, id_profesion)
SELECT v.codigo, v.unidad, p.id_profesion
FROM (VALUES
    ('altura',        'cm'),
    ('pecho',         'cm'),
    ('cintura',       'cm'),
    ('cadera',        'cm'),
    ('color_ojos',    'color'),
    ('color_cabello', 'color'),
    ('color_piel',    'color')
) AS v(codigo, unidad)
JOIN profesion p ON p.nombre = 'modelo';