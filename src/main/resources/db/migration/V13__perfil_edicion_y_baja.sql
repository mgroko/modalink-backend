-- =====================================================================
-- ModaLink - UC-11 Editar perfil / UC-12 Eliminar perfil
-- Migración Flyway: V13__perfil_edicion_y_baja.sql
--
-- 1) La profesión de un perfil no puede modificarse (UC-11).
-- 2) fecha_solicitud_baja se mantiene sincronizada con el estado:
--    - al pasar a 'PendienteBaja' se setea al momento actual si está nula
--    - al pasar a 'Activo' se limpia (reactivación dentro de los 30 días)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Trigger: prohibir cambiar la profesión de un perfil (UC-11)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION chk_perfil_no_cambiar_profesion() RETURNS trigger AS $$
BEGIN
    IF NEW.id_profesion IS DISTINCT FROM OLD.id_profesion THEN
        RAISE EXCEPTION 'La profesión de un perfil no puede modificarse.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_perfil_no_cambiar_profesion
BEFORE UPDATE ON perfil
FOR EACH ROW EXECUTE FUNCTION chk_perfil_no_cambiar_profesion();

-- ---------------------------------------------------------------------
-- 2) Trigger: sincronizar fecha_solicitud_baja con el estado (UC-12)
-- ---------------------------------------------------------------------
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

CREATE TRIGGER trg_perfil_fecha_solicitud_baja
BEFORE UPDATE ON perfil
FOR EACH ROW EXECUTE FUNCTION chk_perfil_fecha_solicitud_baja();