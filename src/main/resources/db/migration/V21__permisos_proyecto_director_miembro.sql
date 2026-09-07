-- =====================================================================
-- ModaLink - Permisos para roles de proyecto (Director y Miembro)
-- Migración Flyway: V21__permisos_proyecto_director_miembro.sql
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Inserción de permisos de proyecto en permiso_proyecto
-- ---------------------------------------------------------------------
INSERT INTO permiso_proyecto (nombre) VALUES
    ('VER_PROYECTO'),
    ('MODIFICAR_PROYECTO'),
    ('PUBLICAR_PROYECTO'),
    ('CONFIRMAR_PROYECTO'),
    ('FINALIZAR_PROYECTO'),
    ('CANCELAR_PROYECTO'),
    ('CREAR_ACTIVIDAD'),
    ('MODIFICAR_ACTIVIDAD'),
    ('ELIMINAR_ACTIVIDAD'),
    ('ASIGNAR_ACTIVIDAD'),
    ('GESTIONAR_POSTULACIONES'),
    ('INVITAR_MIEMBRO'),
    ('ELIMINAR_INVITACION'),
    ('ELIMINAR_INTEGRANTE')
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- 2. Asignación de todos los permisos de proyecto al rol 'Director'
-- ---------------------------------------------------------------------
INSERT INTO rol_proyecto_permiso (id_rol_proyecto, id_permiso_proyecto)
SELECT rp.id_rol_proyecto, pp.id_permiso_proyecto
FROM rol_proyecto rp
CROSS JOIN permiso_proyecto pp
WHERE rp.nombre = 'Director'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- 3. Asignación de permisos colaborativos básicos al rol 'Miembro'
-- ---------------------------------------------------------------------
INSERT INTO rol_proyecto_permiso (id_rol_proyecto, id_permiso_proyecto)
SELECT rp.id_rol_proyecto, pp.id_permiso_proyecto
FROM rol_proyecto rp
JOIN permiso_proyecto pp ON pp.nombre IN ('VER_PROYECTO')
WHERE rp.nombre = 'Miembro'
ON CONFLICT DO NOTHING;
