-- =====================================================================
-- ModaLink - Jornada partida (bloques de mañana y tarde)
-- Migración Flyway: V19__jornada_partida.sql
--
-- Cambios:
--  1. jornada_agenda: el horario se normaliza en 4 columnas:
--     horario_inicio_maniana, horario_fin_maniana,
--     horario_inicio_tarde, horario_fin_tarde.
--     - Jornada "de corrido": se usan inicio_maniana y fin_tarde, con el
--       par del mediodía en NULL. (Los datos existentes quedan corridos.)
--     - Jornada "partida": las cuatro horas, en orden estricto
--       inicio_maniana < fin_maniana < inicio_tarde < fin_tarde.
--  2. Checks de integridad para los rangos y el par del mediodía.
--  3. fn_crear_agenda actualizada a los nuevos nombres de columna (la
--     jornada por defecto sigue siendo corrida, L-V 09:00-18:00).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Columnas normalizadas (rename + altas; sin migración de datos)
-- ---------------------------------------------------------------------
ALTER TABLE jornada_agenda RENAME COLUMN hora_inicio TO horario_inicio_maniana;
ALTER TABLE jornada_agenda RENAME COLUMN hora_fin TO horario_fin_tarde;

ALTER TABLE jornada_agenda ADD COLUMN horario_fin_maniana TIME;
ALTER TABLE jornada_agenda ADD COLUMN horario_inicio_tarde TIME;

-- ---------------------------------------------------------------------
-- 2. Checks de integridad
-- ---------------------------------------------------------------------
ALTER TABLE jornada_agenda DROP CONSTRAINT chk_jornada_rango;

-- El par del mediodía va completo (partida) o no va (corrido).
ALTER TABLE jornada_agenda
    ADD CONSTRAINT chk_jornada_mediodia_completo CHECK (
        (horario_fin_maniana IS NULL AND horario_inicio_tarde IS NULL)
        OR (horario_fin_maniana IS NOT NULL AND horario_inicio_tarde IS NOT NULL)
    );

-- Bloque de la mañana: el fin es posterior al inicio (si el bloque existe).
ALTER TABLE jornada_agenda
    ADD CONSTRAINT chk_jornada_rango_maniana CHECK (
        horario_fin_maniana IS NULL OR horario_fin_maniana > horario_inicio_maniana
    );

-- Jornada partida: el bloque de la tarde empieza después del de la mañana.
ALTER TABLE jornada_agenda
    ADD CONSTRAINT chk_jornada_orden_bloques CHECK (
        horario_inicio_tarde IS NULL OR horario_inicio_tarde > horario_fin_maniana
    );

-- Bloque de la tarde: el fin es posterior al inicio (si el bloque existe).
ALTER TABLE jornada_agenda
    ADD CONSTRAINT chk_jornada_rango_tarde CHECK (
        horario_inicio_tarde IS NULL OR horario_fin_tarde > horario_inicio_tarde
    );

-- La jornada siempre termina después de que empieza (corrida o partida).
ALTER TABLE jornada_agenda
    ADD CONSTRAINT chk_jornada_rango_total CHECK (
        horario_fin_tarde > horario_inicio_maniana
    );

-- ---------------------------------------------------------------------
-- 3. Trigger: agenda + jornada por defecto (corrida) al crear un usuario
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_crear_agenda() RETURNS trigger AS $$
DECLARE
    v_id_agenda BIGINT;
BEGIN
    INSERT INTO agenda (margen_actividad_minutos, id_usuario)
    VALUES (60, NEW.id_usuario)
    RETURNING id_agenda INTO v_id_agenda;

    INSERT INTO jornada_agenda (id_agenda, dia_semana, horario_inicio_maniana, horario_fin_tarde)
    SELECT v_id_agenda, d, '09:00'::TIME, '18:00'::TIME
    FROM (VALUES (1), (2), (3), (4), (5)) AS v(d);

    RETURN NEW;
END $$ LANGUAGE plpgsql;
