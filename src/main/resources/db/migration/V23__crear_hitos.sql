-- =====================================================================
-- ModaLink - Módulo de Hitos de Proyecto y Notificaciones
-- Migración Flyway: V23__crear_hitos.sql
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Tabla genérica de notificaciones
-- (Se crea una estructura mínima ya que no existía tabla previa en el esquema;
--  queda sujeta a ampliación para el módulo específico de notificaciones)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notificacion (
    id_notificacion    BIGSERIAL PRIMARY KEY,
    id_usuario_destino BIGINT NOT NULL REFERENCES usuario(id_usuario),
    tipo               VARCHAR(50) NOT NULL,   -- 'HitoCumplido', 'HitoVencido', 'PostulacionRecibida', etc.
    mensaje            TEXT,
    entidad_tipo       VARCHAR(50),            -- 'hito', 'postulacion', 'proyecto', etc.
    entidad_id         BIGINT,
    leida              BOOLEAN NOT NULL DEFAULT false,
    fecha_generada     TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 2. Asegurar columna estado en tabla actividad
-- ---------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'actividad'
          AND column_name = 'estado'
    ) THEN
        ALTER TABLE actividad
            ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
            ADD CONSTRAINT chk_actividad_estado CHECK (estado IN ('Pendiente', 'EnCurso', 'Completada', 'Cancelada'));
    END IF;
END $$;

-- ---------------------------------------------------------------------
-- 3. Tabla hito
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hito (
    id_hito             BIGSERIAL PRIMARY KEY,
    id_proyecto         BIGINT NOT NULL REFERENCES proyecto(id_proyecto),
    nombre              VARCHAR(150) NOT NULL,
    descripcion         TEXT,
    fecha_limite        DATE NULL,        -- NULL = solo indicador de progreso, sin efecto en planificación
    fecha_cumplimiento  DATE NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'Pendiente'
                        CHECK (estado IN ('Pendiente', 'Cumplido', 'Retrasado')),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 4. Tabla asociativa hito_actividad
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hito_actividad (
    id_hito      BIGINT NOT NULL REFERENCES hito(id_hito),
    id_actividad BIGINT NOT NULL REFERENCES actividad(id_actividad),
    PRIMARY KEY (id_hito, id_actividad)
);

-- ---------------------------------------------------------------------
-- 5. Validaciones e integridad para hito
-- a) Trigger BEFORE INSERT OR UPDATE ON hito:
--    Si fecha_limite IS NOT NULL, verificar que no supere la fecha_entrega
--    de la planificación asociada al proyecto.
-- ---------------------------------------------------------------------
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

-- b) Trigger BEFORE DELETE ON hito:
--    No debe poder borrarse un hito mientras tenga filas asociadas en hito_actividad.
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 6. Derivación de estado y notificación al cumplirse
-- Trigger AFTER UPDATE OF estado ON actividad:
-- - Cuando una actividad pasa a estado 'Completada', buscar hitos asociados
--   vía hito_actividad y verificar si todas sus actividades asociadas están
--   en 'Completada'.
-- - Si es así: UPDATE hito SET estado='Cumplido', fecha_cumplimiento=CURRENT_DATE.
-- - Notificar al o a los Directores activos del proyecto.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_derivar_cumplimiento_hito() RETURNS trigger AS $$
DECLARE
    r_hito RECORD;
    r_director RECORD;
    v_pendientes INT;
    v_id_rol_director BIGINT;
BEGIN
    -- Solo actuar cuando la actividad pasa a estado 'Completada'
    IF NEW.estado = 'Completada' AND (OLD.estado IS NULL OR OLD.estado <> 'Completada') THEN
        
        -- Obtener el ID del rol 'Director'
        SELECT id_rol_proyecto INTO v_id_rol_director
        FROM rol_proyecto
        WHERE nombre = 'Director';

        -- Iterar sobre los hitos asociados a esta actividad que aún no estén cumplidos
        FOR r_hito IN
            SELECT h.id_hito, h.id_proyecto, h.nombre AS nombre_hito
            FROM hito h
            JOIN hito_actividad ha ON h.id_hito = ha.id_hito
            WHERE ha.id_actividad = NEW.id_actividad
              AND h.estado <> 'Cumplido'
        LOOP
            -- Contar cuántas actividades del hito NO están completadas
            SELECT COUNT(*) INTO v_pendientes
            FROM hito_actividad ha_sub
            JOIN actividad a ON ha_sub.id_actividad = a.id_actividad
            WHERE ha_sub.id_hito = r_hito.id_hito
              AND a.estado <> 'Completada';

            -- Si no quedan actividades pendientes, el hito pasa a Cumplido
            IF v_pendientes = 0 THEN
                UPDATE hito
                SET estado = 'Cumplido',
                    fecha_cumplimiento = CURRENT_DATE
                WHERE id_hito = r_hito.id_hito;

                -- Notificar a todos los directores activos del proyecto
                FOR r_director IN
                    SELECT per.id_usuario
                    FROM miembros_proyecto mp
                    JOIN perfil per ON mp.id_perfil = per.id_perfil
                    WHERE mp.id_proyecto = r_hito.id_proyecto
                      AND mp.id_rol_proyecto = v_id_rol_director
                      AND mp.estado_participacion = 'Activo'
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

-- ---------------------------------------------------------------------
-- 7. Detección de vencimiento
-- Índice optimizado para el job/scheduler de backend que detecta hitos vencidos:
-- SELECT * FROM hito WHERE estado = 'Pendiente' AND fecha_limite < CURRENT_DATE;
-- Nota: La ejecución periódica se resuelve mediante un proceso programado
-- (Scheduled job) en la capa de aplicación backend.
-- ---------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_hito_fecha_limite_pendiente
    ON hito(fecha_limite)
    WHERE estado = 'Pendiente';
