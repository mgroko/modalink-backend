-- =====================================================================
-- ModaLink - Configuración del sistema y permiso de administración
-- Migración Flyway: V22__configuracion_sistema_y_permiso.sql
-- =====================================================================

-- 1. Permiso global ADMINISTRAR_CONFIGURACION
INSERT INTO permiso_global (nombre) VALUES
    ('ADMINISTRAR_CONFIGURACION')
ON CONFLICT (nombre) DO NOTHING;

-- 2. Asignar ADMINISTRAR_CONFIGURACION al rol 'Administrador'
INSERT INTO rol_global_permiso (id_rol_global, id_permiso_global)
SELECT rg.id_rol_global, pg.id_permiso_global
FROM rol_global rg
JOIN permiso_global pg ON pg.nombre = 'ADMINISTRAR_CONFIGURACION'
WHERE rg.nombre = 'Administrador'
ON CONFLICT DO NOTHING;

-- 3. Tabla de configuración del sistema (clave-valor)
CREATE TABLE IF NOT EXISTS configuracion_sistema (
    clave VARCHAR(100) PRIMARY KEY,
    valor VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255)
);

-- 4. Valores iniciales para el scheduler de deshabilitaciones
INSERT INTO configuracion_sistema (clave, valor, descripcion) VALUES
    ('SCHEDULER_DESHABILITACION_HORA', '2', 'Hora del día (0-23) en la que se ejecuta el scheduler de deshabilitación'),
    ('SCHEDULER_DESHABILITACION_MINUTO', '0', 'Minuto (0-59) en el que se ejecuta el scheduler de deshabilitación'),
    ('SCHEDULER_DESHABILITACION_CRON', '0 0 2 * * *', 'Expresión cron para el scheduler de deshabilitación de usuarios')
ON CONFLICT (clave) DO NOTHING;
