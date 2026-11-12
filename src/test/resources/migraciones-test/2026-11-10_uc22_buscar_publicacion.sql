-- UC-22 - Buscar publicacion
-- Migracion para bases existentes. Ejecutar con la aplicacion detenida.
--
-- Agrega indices para la busqueda de publicaciones por perfil, estado,
-- fecha y proyecto. Es idempotente.

BEGIN;

CREATE INDEX IF NOT EXISTS "IDX_publicacion_perfil_estado_fecha"
    ON publicacion (id_perfil, estado, fecha_publicacion DESC);

CREATE INDEX IF NOT EXISTS "IDX_publicacion_perfil_estado_proyecto"
    ON publicacion (id_perfil, estado, id_proyecto, fecha_publicacion DESC);

COMMIT;
