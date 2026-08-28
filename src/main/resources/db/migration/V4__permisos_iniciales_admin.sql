INSERT INTO permiso_global (nombre) VALUES
    ('VER_USUARIOS'),
    ('HABILITAR_USUARIO'),
    ('DESHABILITAR_USUARIO');

INSERT INTO rol_global_permiso (id_rol_global, id_permiso_global)
SELECT rg.id_rol_global, pg.id_permiso_global
FROM rol_global rg, permiso_global pg
WHERE rg.nombre = 'Administrador';