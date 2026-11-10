-- UC-19 - Alta de publicacion
-- Migracion para bases existentes. Ejecutar con la aplicacion detenida.
--
-- Ajusta la tabla publicacion al caso de uso:
--   * descripcion opcional;
--   * proyecto asociado opcional;
--   * ubicacion opcional;
--   * estado ACTIVA/OCULTA;
--   * orden de las imagenes de la publicacion.
--
-- Es idempotente: usa IF NOT EXISTS y bloques DO para las constraints.

BEGIN;

ALTER TABLE publicacion ALTER COLUMN descripcion DROP NOT NULL;
ALTER TABLE publicacion ALTER COLUMN id_proyecto DROP NOT NULL;

ALTER TABLE publicacion ADD COLUMN IF NOT EXISTS id_ubicacion int8;
ALTER TABLE publicacion
    ADD COLUMN IF NOT EXISTS estado varchar(20) NOT NULL DEFAULT 'ACTIVA';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_publicacion_ubicacion'
    ) THEN
        ALTER TABLE publicacion
            ADD CONSTRAINT fk_publicacion_ubicacion
            FOREIGN KEY (id_ubicacion) REFERENCES ubicacion(id_ubicacion);
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_publicacion_estado'
    ) THEN
        ALTER TABLE publicacion
            ADD CONSTRAINT chk_publicacion_estado
            CHECK (estado IN ('ACTIVA', 'OCULTA'));
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_publicacion_titulo'
    ) THEN
        ALTER TABLE publicacion
            ADD CONSTRAINT chk_publicacion_titulo
            CHECK (char_length(trim(titulo)) > 0);
    END IF;
END
$$;

ALTER TABLE imagen_publicacion
    ADD COLUMN IF NOT EXISTS orden int4 NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS "IDX_publicacion_perfil_fecha"
    ON publicacion (id_perfil, fecha_publicacion DESC);

CREATE INDEX IF NOT EXISTS "IDX_imagen_publicacion_publicacion"
    ON imagen_publicacion (id_publicacion, orden);

INSERT INTO configuracion_sistema (clave, valor, descripcion)
VALUES (
    'IMAGEN_PUBLICACION_LADO_SALIDA_PX',
    '1600',
    'Lado maximo de salida de las imagenes de publicaciones en pixeles'
)
ON CONFLICT (clave) DO UPDATE
    SET valor = EXCLUDED.valor,
        descripcion = EXCLUDED.descripcion;

COMMIT;
