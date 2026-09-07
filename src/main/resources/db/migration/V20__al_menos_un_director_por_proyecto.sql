-- =====================================================================
-- ModaLink - Restricción: Cada proyecto debe tener 1 o más directores
-- Migración Flyway: V20__al_menos_un_director_por_proyecto.sql
--
-- Reglas:
--  1. En cada proyecto debe existir al menos un director (rol 'Director'
--     con estado_participacion = 'Activo').
--  2. No se permite eliminar un miembro director ni cambiarle el rol o
--     su estado a inactivo si es el último director activo del proyecto.
--  3. Un proyecto nuevo o existente no puede quedar sin directores
--     al finalizar la transacción (constraint diferida / trigger deferred).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Función para validar que un miembro director no sea eliminado ni
--    desactivado/degradado si es el único director activo del proyecto.
-- ---------------------------------------------------------------------
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
        -- Si no era director activo, no afecta la cuenta de directores
        IF OLD.id_rol_proyecto <> v_id_rol_director OR OLD.estado_participacion <> 'Activo' THEN
            RETURN OLD;
        END IF;
        v_id_proyecto := OLD.id_proyecto;
    ELSE -- UPDATE
        -- Solo nos interesa si dejó de ser director o dejó de estar activo
        IF OLD.id_rol_proyecto = v_id_rol_director AND OLD.estado_participacion = 'Activo' THEN
            IF NEW.id_rol_proyecto = v_id_rol_director AND NEW.estado_participacion = 'Activo' THEN
                RETURN NEW; -- Sigue siendo director activo
            END IF;
        ELSE
            -- No era director activo previamente, no reduce el conteo
            RETURN NEW;
        END IF;
        v_id_proyecto := OLD.id_proyecto;
    END IF;

    -- Contamos cuántos otros directores activos quedan para el proyecto
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

-- ---------------------------------------------------------------------
-- 2. Constraint trigger diferido al final de la transacción (COMMIT)
--    para asegurar que al crear o modificar un proyecto, este tenga
--    al menos un director activo asignado.
-- ---------------------------------------------------------------------
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
