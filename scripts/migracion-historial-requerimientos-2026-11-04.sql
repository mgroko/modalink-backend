-- Historial inmutable de requerimientos y autor de baja.
-- Migración aditiva: no elimina ni modifica datos existentes.

ALTER TABLE requerimiento_actividad
    ADD COLUMN IF NOT EXISTS fecha_archivado timestamp,
    ADD COLUMN IF NOT EXISTS id_requerimiento_origen int8;

ALTER TABLE requerimiento_gral_proyecto
    ADD COLUMN IF NOT EXISTS fecha_archivado timestamp,
    ADD COLUMN IF NOT EXISTS id_requerimiento_origen int8;

ALTER TABLE miembros_proyecto
    ADD COLUMN IF NOT EXISTS id_perfil_autor_baja int8;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'Refperfil_autor_baja_miembro'
    ) THEN
        ALTER TABLE miembros_proyecto
            ADD CONSTRAINT "Refperfil_autor_baja_miembro"
            FOREIGN KEY (id_perfil_autor_baja)
            REFERENCES perfil(id_perfil);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS "IDX_req_act_origen"
    ON requerimiento_actividad(id_requerimiento_origen);

CREATE INDEX IF NOT EXISTS "IDX_req_gral_origen"
    ON requerimiento_gral_proyecto(id_requerimiento_origen);

CREATE INDEX IF NOT EXISTS "IDX_req_act_activo"
    ON requerimiento_actividad(id_actividad)
    WHERE fecha_archivado IS NULL;

CREATE INDEX IF NOT EXISTS "IDX_req_gral_activo"
    ON requerimiento_gral_proyecto(id_proyecto)
    WHERE fecha_archivado IS NULL;
