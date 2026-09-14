-- =====================================================================
-- ModaLink - Consolidación de Funciones y Triggers del Sistema
-- Archivo: funciones_y_triggers.sql
-- Ubicación: src/main/resources/db/triggers y seeds/
--
-- Incluye todas las funciones y triggers vigentes de las migraciones Flyway
-- (omitiendo versiones obsoletas, como la versión previa de fn_crear_agenda).
-- =====================================================================

-- =====================================================================
-- 1. USUARIOS: Validación de DNI No Negativo ni Alfanumérico (Origen: V17)
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

DROP TRIGGER IF EXISTS trg_usuario_dni_no_negativo ON usuario;
CREATE TRIGGER trg_usuario_dni_no_negativo
    BEFORE INSERT OR UPDATE ON usuario
    FOR EACH ROW EXECUTE FUNCTION chk_usuario_dni_no_negativo();


-- =====================================================================
-- 2. AGENDA Y CALENDARIO (Origen: V14 y V19)
-- =====================================================================

-- 2.1. Creación automática de agenda y jornada por defecto al crear usuario (V19)
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

DROP TRIGGER IF EXISTS trg_agenda_crear ON usuario;
CREATE TRIGGER trg_agenda_crear
    AFTER INSERT ON usuario
    FOR EACH ROW EXECUTE FUNCTION fn_crear_agenda();


-- 2.2. Impedir solapamiento entre bloqueos manuales de la misma agenda (V14)
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

DROP TRIGGER IF EXISTS trg_bloqueo_no_solape ON bloqueo_agenda;
CREATE TRIGGER trg_bloqueo_no_solape
    BEFORE INSERT OR UPDATE ON bloqueo_agenda
    FOR EACH ROW EXECUTE FUNCTION fn_validar_solape_bloqueo();


-- 2.3. Impedir liberar un bloqueo cubierto por una actividad activa (V14)
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

DROP TRIGGER IF EXISTS trg_bloqueo_no_liberar_actividad ON bloqueo_agenda;
CREATE TRIGGER trg_bloqueo_no_liberar_actividad
    BEFORE DELETE ON bloqueo_agenda
    FOR EACH ROW EXECUTE FUNCTION fn_validar_bloqueo_no_actividad();


-- =====================================================================
-- 3. PERFIL Y PROFESIÓN (Origen: V8 y V13)
-- =====================================================================

-- 3.1. Unicidad de perfil activo/pendiente de baja por profesión y usuario (V8)
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

DROP TRIGGER IF EXISTS trg_perfil_unico_por_profesion ON perfil;
CREATE TRIGGER trg_perfil_unico_por_profesion
    BEFORE INSERT ON perfil
    FOR EACH ROW EXECUTE FUNCTION chk_perfil_unico_por_profesion();


-- 3.2. Prohibir modificar la profesión de un perfil existente (V13)
CREATE OR REPLACE FUNCTION chk_perfil_no_cambiar_profesion() RETURNS trigger AS $$
BEGIN
    IF NEW.id_profesion IS DISTINCT FROM OLD.id_profesion THEN
        RAISE EXCEPTION 'La profesión de un perfil no puede modificarse.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_perfil_no_cambiar_profesion ON perfil;
CREATE TRIGGER trg_perfil_no_cambiar_profesion
    BEFORE UPDATE ON perfil
    FOR EACH ROW EXECUTE FUNCTION chk_perfil_no_cambiar_profesion();


-- 3.3. Sincronizar fecha_solicitud_baja según el estado del perfil (V13)
CREATE OR REPLACE FUNCTION chk_perfil_fecha_solicitud_baja() RETURNS trigger AS $$
BEGIN
    IF NEW.estado = 'PendienteBaja' AND OLD.estado <> 'PendienteBaja' THEN
        IF NEW.fecha_solicitud_baja IS NULL THEN
            NEW.fecha_solicitud_baja := now();
        END IF;
    ELSIF NEW.estado = 'Activo' THEN
        NEW.fecha_solicitud_baja := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_perfil_fecha_solicitud_baja ON perfil;
CREATE TRIGGER trg_perfil_fecha_solicitud_baja
    BEFORE UPDATE ON perfil
    FOR EACH ROW EXECUTE FUNCTION chk_perfil_fecha_solicitud_baja();


-- =====================================================================
-- 4. CARACTERÍSTICAS TÉCNICAS DE PERFIL (Origen: V8, V9, V15)
-- =====================================================================

-- 4.1. Validar que la característica técnica pertenezca a la profesión del perfil (V8)
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

DROP TRIGGER IF EXISTS trg_caracteristica_tecnica_de_profesion ON caracteristica_perfil;
CREATE TRIGGER trg_caracteristica_tecnica_de_profesion
    BEFORE INSERT OR UPDATE ON caracteristica_perfil
    FOR EACH ROW EXECUTE FUNCTION chk_caracteristica_tecnica_de_profesion();


-- 4.2. Consistencia: valor XOR id_valor según el tipo_dato de la característica (V9)
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

DROP TRIGGER IF EXISTS trg_caracteristica_perfil_valor ON caracteristica_perfil;
CREATE TRIGGER trg_caracteristica_perfil_valor
    BEFORE INSERT OR UPDATE ON caracteristica_perfil
    FOR EACH ROW EXECUTE FUNCTION chk_caracteristica_perfil_valor();


-- 4.3. Validación de valor no negativo para características numéricas (V15)
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

DROP TRIGGER IF EXISTS trg_caracteristica_valor_no_negativo ON caracteristica_perfil;
CREATE TRIGGER trg_caracteristica_valor_no_negativo
    BEFORE INSERT OR UPDATE ON caracteristica_perfil
    FOR EACH ROW EXECUTE FUNCTION chk_caracteristica_valor_no_negativo();


-- =====================================================================
-- 5. PROYECTOS Y MIEMBROS: Regla de Director Obligatorio (Origen: V20)
-- =====================================================================

-- 5.1. Validar que no se desactive o elimine al único director activo del proyecto
CREATE OR REPLACE FUNCTION fn_validar_minimo_un_director_miembro() RETURNS trigger AS $$
DECLARE
    v_id_rol_director BIGINT;
    v_id_proyecto BIGINT;
    v_cantidad_directores INT;
BEGIN
    SELECT id_rol_proyecto INTO v_id_rol_director
    FROM rol_proyecto
    WHERE nombre = 'Director';

    IF TG_OP = 'DELETE' THEN
        IF OLD.id_rol_proyecto <> v_id_rol_director OR OLD.estado_participacion <> 'Activo' THEN
            RETURN OLD;
        END IF;
        v_id_proyecto := OLD.id_proyecto;
    ELSE -- UPDATE
        IF OLD.id_rol_proyecto = v_id_rol_director AND OLD.estado_participacion = 'Activo' THEN
            IF NEW.id_rol_proyecto = v_id_rol_director AND NEW.estado_participacion = 'Activo' THEN
                RETURN NEW;
            END IF;
        ELSE
            RETURN NEW;
        END IF;
        v_id_proyecto := OLD.id_proyecto;
    END IF;

    SELECT COUNT(*) INTO v_cantidad_directores
    FROM miembros_proyecto
    WHERE id_proyecto = v_id_proyecto
      AND id_rol_proyecto = v_id_rol_director
      AND estado_participacion = 'Activo'
      AND id_miembro <> OLD.id_miembro;

    IF v_cantidad_directores < 1 THEN
        RAISE EXCEPTION 'El proyecto (ID %) debe tener al menos un director activo. No es posible remover o desactivar al único director.', v_id_proyecto;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_minimo_un_director_miembro ON miembros_proyecto;
CREATE TRIGGER trg_validar_minimo_un_director_miembro
    BEFORE UPDATE OR DELETE ON miembros_proyecto
    FOR EACH ROW EXECUTE FUNCTION fn_validar_minimo_un_director_miembro();


-- 5.2. Constraint trigger diferido: asegurar que el proyecto termine la transacción con al menos un director
CREATE OR REPLACE FUNCTION fn_validar_proyecto_tiene_director() RETURNS trigger AS $$
DECLARE
    v_id_rol_director BIGINT;
    v_cantidad_directores INT;
BEGIN
    SELECT id_rol_proyecto INTO v_id_rol_director
    FROM rol_proyecto
    WHERE nombre = 'Director';

    SELECT COUNT(*) INTO v_cantidad_directores
    FROM miembros_proyecto
    WHERE id_proyecto = NEW.id_proyecto
      AND id_rol_proyecto = v_id_rol_director
      AND estado_participacion = 'Activo';

    IF v_cantidad_directores < 1 THEN
        RAISE EXCEPTION 'El proyecto "%" (ID %) no cuenta con directores activos. Cada proyecto debe tener al menos uno.', NEW.nombre, NEW.id_proyecto;
    END IF;

    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_proyecto_requiere_director ON proyecto;
CREATE CONSTRAINT TRIGGER trg_proyecto_requiere_director
    AFTER INSERT OR UPDATE ON proyecto
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_validar_proyecto_tiene_director();


-- =====================================================================
-- 6. HITOS Y ACTIVIDADES (Origen: V23)
-- =====================================================================

-- 6.1. Validar que la fecha límite del hito no supere la fecha de entrega de la planificación
CREATE OR REPLACE FUNCTION fn_validar_fecha_limite_hito() RETURNS trigger AS $$
DECLARE
    v_fecha_entrega DATE;
BEGIN
    IF NEW.fecha_limite IS NOT NULL THEN
        SELECT p.fecha_entrega INTO v_fecha_entrega
        FROM planificacion p
        WHERE p.id_proyecto = NEW.id_proyecto;

        IF v_fecha_entrega IS NOT NULL AND NEW.fecha_limite > v_fecha_entrega THEN
            RAISE EXCEPTION 'La fecha límite del hito (%) no puede superar la fecha de entrega de la planificación del proyecto (%)',
                NEW.fecha_limite, v_fecha_entrega;
        END IF;
    END IF;

    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_fecha_limite_hito ON hito;
CREATE TRIGGER trg_validar_fecha_limite_hito
    BEFORE INSERT OR UPDATE ON hito
    FOR EACH ROW EXECUTE FUNCTION fn_validar_fecha_limite_hito();


-- 6.2. Impedir eliminar un hito con actividades asociadas
CREATE OR REPLACE FUNCTION fn_validar_borrado_hito() RETURNS trigger AS $$
DECLARE
    v_actividades_asociadas INT;
BEGIN
    SELECT COUNT(*) INTO v_actividades_asociadas
    FROM hito_actividad
    WHERE id_hito = OLD.id_hito;

    IF v_actividades_asociadas > 0 THEN
        RAISE EXCEPTION 'No se puede eliminar el hito (ID %) porque tiene % actividad(es) asociada(s). Debe desvincularlas primero.',
            OLD.id_hito, v_actividades_asociadas;
    END IF;

    RETURN OLD;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_borrado_hito ON hito;
CREATE TRIGGER trg_validar_borrado_hito
    BEFORE DELETE ON hito
    FOR EACH ROW EXECUTE FUNCTION fn_validar_borrado_hito();


-- 6.3. Derivación automática de cumplimiento de hitos y notificación al o a los Directores
CREATE OR REPLACE FUNCTION fn_derivar_cumplimiento_hito() RETURNS trigger AS $$
DECLARE
    r_hito RECORD;
    r_director RECORD;
    v_pendientes INT;
    v_id_rol_director BIGINT;
BEGIN
    IF NEW.estado = 'Completada' AND (OLD.estado IS NULL OR OLD.estado <> 'Completada') THEN
        
        SELECT id_rol_proyecto INTO v_id_rol_director
        FROM rol_proyecto
        WHERE nombre = 'Director';

        FOR r_hito IN
            SELECT h.id_hito, h.id_proyecto, h.nombre AS nombre_hito
            FROM hito h
            JOIN hito_actividad ha ON h.id_hito = ha.id_hito
            WHERE ha.id_actividad = NEW.id_actividad
              AND h.estado <> 'Cumplido'
        LOOP
            SELECT COUNT(*) INTO v_pendientes
            FROM hito_actividad ha_sub
            JOIN actividad a ON ha_sub.id_actividad = a.id_actividad
            WHERE ha_sub.id_hito = r_hito.id_hito
              AND a.estado <> 'Completada';

            IF v_pendientes = 0 THEN
                UPDATE hito
                SET estado = 'Cumplido',
                    fecha_cumplimiento = CURRENT_DATE
                WHERE id_hito = r_hito.id_hito;

                FOR r_director IN
                    SELECT per.id_usuario
                    FROM miembros_proyecto mp
                    JOIN perfil per ON mp.id_perfil = per.id_perfil
                    WHERE mp.id_proyecto = r_hito.id_proyecto
                      AND mp.estado_participacion = 'Activo'
                      AND mp.id_rol_proyecto = v_id_rol_director
                LOOP
                    INSERT INTO notificacion (
                        id_usuario_destino,
                        tipo,
                        mensaje,
                        entidad_tipo,
                        entidad_id,
                        leida,
                        fecha_generada
                    ) VALUES (
                        r_director.id_usuario,
                        'HitoCumplido',
                        'El hito "' || r_hito.nombre_hito || '" ha sido completado.',
                        'hito',
                        r_hito.id_hito,
                        false,
                        now()
                    );
                END LOOP;
            END IF;
        END LOOP;
    END IF;

    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_derivar_cumplimiento_hito ON actividad;
CREATE TRIGGER trg_derivar_cumplimiento_hito
    AFTER UPDATE OF estado ON actividad
    FOR EACH ROW EXECUTE FUNCTION fn_derivar_cumplimiento_hito();
