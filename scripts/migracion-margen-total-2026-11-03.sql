-- Migración: margen total entre actividades consecutivas.
--
-- Convierte las funciones de validación de solape de "margen a cada lado" a
-- "margen total": con 30 minutos, la separación entre el fin de una actividad
-- y el inicio de la siguiente es de 30 minutos. Cada actividad reserva la
-- mitad del margen a cada lado.
--
-- Valida que un bloque de horario de una persona no se superponga con otra
-- actividad suya ni con un bloqueo manual. El margen es total entre
-- actividades: se reparte en mitades a cada lado de la actividad nueva.
CREATE OR REPLACE FUNCTION fn_validar_solape_horario_usuario(
    p_id_usuario BIGINT,
    p_id_actividad BIGINT,
    p_inicio TIMESTAMP,
    p_duracion INT,
    p_margen INT
) RETURNS void AS $$
DECLARE
    v_margen_izquierdo INT;
    v_margen_derecho INT;
BEGIN
    IF p_inicio IS NULL OR p_duracion IS NULL THEN
        RETURN;
    END IF;
    IF p_duracion <= 0 THEN
        RAISE EXCEPTION 'La duración de la actividad debe ser mayor que cero.';
    END IF;
    IF p_margen < 0 THEN
        RAISE EXCEPTION 'El margen de la agenda no puede ser negativo: %', p_margen;
    END IF;

    v_margen_izquierdo := p_margen / 2;
    v_margen_derecho := p_margen - v_margen_izquierdo;

    IF EXISTS (
        SELECT 1
        FROM asignacion_actividad aa
        JOIN actividad act ON act.id_actividad = aa.id_actividad
        JOIN planificacion pl ON pl.id_planificacion = act.id_planificacion
        JOIN proyecto p ON p.id_proyecto = pl.id_proyecto
        JOIN miembros_proyecto mp ON mp.id_miembro = aa.id_miembro
        JOIN perfil pf ON pf.id_perfil = mp.id_perfil
        WHERE p.estado IN ('PUBLICADO', 'CONFIRMADO')
          AND mp.estado_participacion = 'ACTIVO'
          AND pf.id_usuario = p_id_usuario
          AND act.id_actividad <> p_id_actividad
          AND act.fecha_hora_inicio IS NOT NULL
          AND act.estado IN ('PENDIENTE', 'EN_CURSO')
          AND (p_inicio - (v_margen_izquierdo * INTERVAL '1 minute'))
              < (act.fecha_hora_inicio + (act.duracion_minutos * INTERVAL '1 minute') + (v_margen_derecho * INTERVAL '1 minute'))
          AND (p_inicio + (p_duracion * INTERVAL '1 minute') + (v_margen_derecho * INTERVAL '1 minute'))
              > (act.fecha_hora_inicio - (v_margen_izquierdo * INTERVAL '1 minute'))
    ) THEN
        RAISE EXCEPTION 'El horario se superpone con otra actividad del mismo profesional.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM bloqueo_agenda b
        JOIN agenda ag ON ag.id_agenda = b.id_agenda
        WHERE ag.id_usuario = p_id_usuario
          AND (p_inicio - (v_margen_izquierdo * INTERVAL '1 minute')) < b.fecha_hora_fin
          AND (p_inicio + (p_duracion * INTERVAL '1 minute') + (v_margen_derecho * INTERVAL '1 minute')) > b.fecha_hora_inicio
    ) THEN
        RAISE EXCEPTION 'El horario se superpone con un bloqueo manual del profesional.';
    END IF;
END $$ LANGUAGE plpgsql;

-- Bloqueo manual: no puede insertarse, modificarse ni eliminarse si deja un
-- bloqueo cubriendo una actividad agendada.
CREATE OR REPLACE FUNCTION fn_validar_bloqueo_no_actividad() RETURNS trigger AS $$
DECLARE
    v_id_agenda BIGINT;
    v_inicio TIMESTAMP;
    v_fin TIMESTAMP;
    v_id_usuario BIGINT;
    v_margen INT;
    v_margen_izquierdo INT;
    v_margen_derecho INT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_id_agenda := OLD.id_agenda;
        v_inicio := OLD.fecha_hora_inicio;
        v_fin := OLD.fecha_hora_fin;
    ELSE
        v_id_agenda := NEW.id_agenda;
        v_inicio := NEW.fecha_hora_inicio;
        v_fin := NEW.fecha_hora_fin;
    END IF;

    SELECT id_usuario INTO v_id_usuario
    FROM agenda WHERE id_agenda = v_id_agenda;

    v_margen := fn_margen_agenda(v_id_agenda);
    v_margen_izquierdo := v_margen / 2;
    v_margen_derecho := v_margen - v_margen_izquierdo;

    IF EXISTS (
        SELECT 1
        FROM asignacion_actividad aa
        JOIN actividad act ON act.id_actividad = aa.id_actividad
        JOIN planificacion pl ON pl.id_planificacion = act.id_planificacion
        JOIN proyecto p ON p.id_proyecto = pl.id_proyecto
        JOIN miembros_proyecto mp ON mp.id_miembro = aa.id_miembro
        JOIN perfil pf ON pf.id_perfil = mp.id_perfil
        WHERE p.estado IN ('PUBLICADO', 'CONFIRMADO')
          AND mp.estado_participacion = 'ACTIVO'
          AND pf.id_usuario = v_id_usuario
          AND act.fecha_hora_inicio IS NOT NULL
          AND act.estado IN ('PENDIENTE', 'EN_CURSO')
          AND v_inicio < (act.fecha_hora_inicio + (act.duracion_minutos * INTERVAL '1 minute') + (v_margen_derecho * INTERVAL '1 minute'))
          AND v_fin > (act.fecha_hora_inicio - (v_margen_izquierdo * INTERVAL '1 minute'))
    ) THEN
        RAISE EXCEPTION 'El bloqueo se superpone con una actividad agendada del profesional.';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

UPDATE configuracion_sistema
SET descripcion = 'Margen total en minutos entre dos actividades consecutivas del calendario. Valor inicial de agendas nuevas y fallback si la agenda no tiene margen'
WHERE clave = 'AGENDA_MARGEN_ACTIVIDAD_MIN';
