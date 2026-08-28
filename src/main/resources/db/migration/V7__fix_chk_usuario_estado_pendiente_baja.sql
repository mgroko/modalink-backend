-- =====================================================================
-- ModaLink - Fix: agregar 'PendienteBaja' al CHECK constraint de usuario y eliminar el 'Inactivo'
-- Migración Flyway: V7__fix_chk_usuario_estado_pendiente_baja.sql
-- =====================================================================

ALTER TABLE usuario DROP CONSTRAINT chk_usuario_estado;

ALTER TABLE usuario
    ADD CONSTRAINT chk_usuario_estado
    CHECK (estado IN ('Activo', 'Deshabilitado', 'PendienteBaja', 'Baja'));
