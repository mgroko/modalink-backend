-- =====================================================================
-- ModaLink - Permisos para administrar características técnicas y sus
-- valores de catálogo, asignados al rol Administrador
-- Migración Flyway: V11__permisos_caracteristicas_admin.sql
-- =====================================================================

INSERT INTO permiso_global (nombre) VALUES
    ('VER_CARACTERISTICAS'),
    ('CREAR_CARACTERISTICA'),
    ('MODIFICAR_CARACTERISTICA'),
    ('ELIMINAR_CARACTERISTICA');

INSERT INTO rol_global_permiso (id_rol_global, id_permiso_global)
SELECT rg.id_rol_global, pg.id_permiso_global
FROM rol_global rg
JOIN permiso_global pg
     ON pg.nombre IN ('VER_CARACTERISTICAS', 'CREAR_CARACTERISTICA',
                      'MODIFICAR_CARACTERISTICA', 'ELIMINAR_CARACTERISTICA')
WHERE rg.nombre = 'Administrador';