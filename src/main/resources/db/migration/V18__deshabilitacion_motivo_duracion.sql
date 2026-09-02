-- =====================================================================
-- ModaLink - Motivo y duración de la deshabilitación (UC-04)
-- Migración Flyway: V18__deshabilitacion_motivo_duracion.sql
--
-- Agrega a usuario la información que el administrador registra al
-- deshabilitar una cuenta:
--   1) motivo_deshabilitacion: texto con el motivo de la deshabilitación.
--   2) fecha_hasta_deshabilitacion: fin de la deshabilitación. NULL = la
--      deshabilitación es indefinida (solo se revierte con /habilitar).
--      Cuando no es NULL, un proceso programado la revierte a "Activo"
--      automáticamente al vencer (UC-04, duración).
-- =====================================================================

ALTER TABLE usuario
    ADD COLUMN motivo_deshabilitacion         VARCHAR(200),
    ADD COLUMN fecha_hasta_deshabilitacion    TIMESTAMP;

-- Índice para acelerar la consulta de deshabilitaciones vencidas del
-- proceso programado de reactivación automática.
CREATE INDEX idx_usuario_deshabilitacion_vencida
    ON usuario (fecha_hasta_deshabilitacion)
    WHERE estado = 'Deshabilitado';