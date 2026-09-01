-- =====================================================================
-- ModaLink - Jornada de calendario personalizable + reglas de bloqueo
-- Migración Flyway: V14__jornada_calendario.sql
--
-- Cambios:
--  1. agenda: se agrega el margen por actividad (buffer, default 1h).
--  2. agenda: se elimina la jornada rígida (dias_laborales y horas) y
--     se reemplaza por la tabla normalizada jornada_agenda (un día por
--     fila, con su propio horario). Un día ausente = no laborable.
--  3. Índice para acelerar consultas de solapamiento de bloqueos.
--  4. Triggers de integridad:
--     a. Crear agenda (+ jornada por defecto) al crear un usuario.
--     b. Impedir que un bloqueo manual se superponga con otro.
--     c. Impedir liberar un bloqueo cubierto por una actividad de un
--        proyecto activo (Publicado/Confirmado), considerando el margen.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Margen por actividad en la agenda (default 60 minutos = 1 hora)
-- ---------------------------------------------------------------------
ALTER TABLE agenda
    ADD COLUMN margen_actividad_minutos INT NOT NULL DEFAULT 60;

-- ---------------------------------------------------------------------
-- 2. Tabla normalizada de jornada laboral (un día por fila)
-- ---------------------------------------------------------------------
CREATE TABLE jornada_agenda(
    id_jornada    BIGSERIAL PRIMARY KEY,
    dia_semana    INT     NOT NULL,
    hora_inicio   TIME    NOT NULL,
    hora_fin      TIME    NOT NULL,
    id_agenda     BIGINT  NOT NULL REFERENCES agenda(id_agenda),
    CONSTRAINT uq_jornada_agenda_dia UNIQUE (id_agenda, dia_semana),
    CONSTRAINT chk_jornada_dia_semana CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_jornada_rango CHECK (hora_fin > hora_inicio)
);

-- Migración de datos: para las agendas existentes se crea una jornada
-- por defecto (Lunes a Viernes 09:00-18:00), editable por el usuario.
INSERT INTO jornada_agenda (id_agenda, dia_semana, hora_inicio, hora_fin)
SELECT a.id_agenda, v.dia, '09:00'::TIME, '18:00'::TIME
FROM agenda a
CROSS JOIN (VALUES (1), (2), (3), (4), (5)) AS v(dia)
ON CONFLICT DO NOTHING;

-- Se eliminan las columnas de jornada rígida ahora que se normalizó.
ALTER TABLE agenda DROP COLUMN dias_laborales;
ALTER TABLE agenda DROP COLUMN hora_inicio_jornada;
ALTER TABLE agenda DROP COLUMN hora_fin_jornada;

-- ---------------------------------------------------------------------
-- 3. Índice de solapamiento de bloqueos manuales
-- ---------------------------------------------------------------------
CREATE INDEX idx_bloqueo_agenda_rango
    ON bloqueo_agenda (fecha_hora_inicio, fecha_hora_fin);

-- ---------------------------------------------------------------------
-- 4a. Crear agenda (+ jornada por defecto) al crear un usuario
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_crear_agenda() RETURNS trigger AS $$
DECLARE
    v_id_agenda BIGINT;
BEGIN
    INSERT INTO agenda (margen_actividad_minutos, id_usuario)
    VALUES (60, NEW.id_usuario)
    RETURNING id_agenda INTO v_id_agenda;

    INSERT INTO jornada_agenda (id_agenda, dia_semana, hora_inicio, hora_fin)
    SELECT v_id_agenda, d, '09:00'::TIME, '18:00'::TIME
    FROM (VALUES (1), (2), (3), (4), (5)) AS v(d);

    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_agenda_crear
    AFTER INSERT ON usuario
    FOR EACH ROW EXECUTE FUNCTION fn_crear_agenda();

-- ---------------------------------------------------------------------
-- 4b. Impedir solapamiento entre bloqueos manuales de la misma agenda
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_solape_bloqueo() RETURNS trigger AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM bloqueo_agenda b
        WHERE b.id_agenda = NEW.id_agenda
          AND b.id_bloqueo IS DISTINCT FROM NEW.id_bloqueo
          AND NEW.fecha_hora_inicio < b.fecha_hora_fin
          AND NEW.fecha_hora_fin > b.fecha_hora_inicio
    ) THEN
        RAISE EXCEPTION 'El bloqueo se superpone con otro bloqueo existente de la agenda';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bloqueo_no_solape
    BEFORE INSERT OR UPDATE ON bloqueo_agenda
    FOR EACH ROW EXECUTE FUNCTION fn_validar_solape_bloqueo();

-- ---------------------------------------------------------------------
-- 4c. Impedir liberar un bloqueo cubierto por una actividad activa.
--     Los bloqueos por actividad son calculados (no se persisten); una
--     agenda no puede liberar un horario comprometido por una actividad
--     de un proyecto activo, considerando el margen configurado.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_bloqueo_no_actividad() RETURNS trigger AS $$
DECLARE
    v_margen INT;
    v_id_usuario BIGINT;
BEGIN
    SELECT margen_actividad_minutos INTO v_margen
    FROM agenda WHERE id_agenda = OLD.id_agenda;

    v_margen := COALESCE(v_margen, 60);

    SELECT a.id_usuario INTO v_id_usuario
    FROM agenda a WHERE a.id_agenda = OLD.id_agenda;

    IF EXISTS (
        SELECT 1
        FROM asignacion_actividad aa
        JOIN actividad act ON act.id_actividad = aa.id_actividad
        JOIN planificacion pl ON pl.id_planificacion = act.id_planificacion
        JOIN proyecto p ON p.id_proyecto = pl.id_proyecto
        JOIN miembros_proyecto mp ON mp.id_miembro = aa.id_miembro
        JOIN perfil pf ON pf.id_perfil = mp.id_perfil
        WHERE p.estado IN ('Publicado', 'Confirmado')
          AND mp.estado_participacion = 'Activo'
          AND pf.id_usuario = v_id_usuario
          AND OLD.fecha_hora_inicio < act.fecha_hora_fin + (v_margen * INTERVAL '1 minute')
          AND OLD.fecha_hora_fin > act.fecha_hora_inicio - (v_margen * INTERVAL '1 minute')
    ) THEN
        RAISE EXCEPTION 'No se puede liberar un horario comprometido por una actividad';
    END IF;
    RETURN OLD;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bloqueo_no_liberar_actividad
    BEFORE DELETE ON bloqueo_agenda
    FOR EACH ROW EXECUTE FUNCTION fn_validar_bloqueo_no_actividad();