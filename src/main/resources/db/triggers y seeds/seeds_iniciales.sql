-- =====================================================================
-- ModaLink - Consolidación de Seeds y Datos Iniciales del Sistema
-- Archivo: seeds_iniciales.sql
-- Ubicación: src/main/resources/db/triggers y seeds/
--
-- Consolida los datos maestros iniciales (catálogos, roles, permisos,
-- características técnicas y valores) con sus nombres y formatos definitivos,
-- eliminando pasos intermedios, renames y migraciones temporales.
-- =====================================================================

-- =====================================================================
-- 1. GÉNEROS (Origen: V3)
-- =====================================================================
INSERT INTO genero (codigo) VALUES
    ('mujer'),
    ('hombre'),
    ('no_binario'),
    ('no_decirlo')
ON CONFLICT (codigo) DO NOTHING;


-- =====================================================================
-- 2. ROLES GLOBALES Y PERMISOS GLOBALES (Origen: V1, V4, V11, V22)
-- =====================================================================

-- 2.1. Roles Globales
INSERT INTO rol_global (nombre) VALUES
    ('Administrador'),
    ('Usuario')
ON CONFLICT (nombre) DO NOTHING;

-- 2.2. Permisos Globales
INSERT INTO permiso_global (nombre) VALUES
    ('VER_USUARIOS'),
    ('HABILITAR_USUARIO'),
    ('DESHABILITAR_USUARIO'),
    ('VER_CARACTERISTICAS'),
    ('CREAR_CARACTERISTICA'),
    ('MODIFICAR_CARACTERISTICA'),
    ('ELIMINAR_CARACTERISTICA'),
    ('ADMINISTRAR_CONFIGURACION')
ON CONFLICT (nombre) DO NOTHING;

-- 2.3. Asignación de Permisos al Rol 'Administrador'
INSERT INTO rol_global_permiso (id_rol_global, id_permiso_global)
SELECT rg.id_rol_global, pg.id_permiso_global
FROM rol_global rg
CROSS JOIN permiso_global pg
WHERE rg.nombre = 'Administrador'
ON CONFLICT DO NOTHING;


-- =====================================================================
-- 3. ROLES DE PROYECTO Y PERMISOS DE PROYECTO (Origen: V1, V21)
-- =====================================================================

-- 3.1. Roles de Proyecto
INSERT INTO rol_proyecto (nombre) VALUES
    ('Director'),
    ('Miembro')
ON CONFLICT (nombre) DO NOTHING;

-- 3.2. Permisos de Proyecto
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

-- 3.3. Asignar todos los permisos de proyecto al rol 'Director'
INSERT INTO rol_proyecto_permiso (id_rol_proyecto, id_permiso_proyecto)
SELECT rp.id_rol_proyecto, pp.id_permiso_proyecto
FROM rol_proyecto rp
CROSS JOIN permiso_proyecto pp
WHERE rp.nombre = 'Director'
ON CONFLICT DO NOTHING;

-- 3.4. Asignar permisos iniciales colaborativos al rol 'Miembro'
INSERT INTO rol_proyecto_permiso (id_rol_proyecto, id_permiso_proyecto)
SELECT rp.id_rol_proyecto, pp.id_permiso_proyecto
FROM rol_proyecto rp
JOIN permiso_proyecto pp ON pp.nombre IN ('VER_PROYECTO')
WHERE rp.nombre = 'Miembro'
ON CONFLICT DO NOTHING;


-- =====================================================================
-- 4. PROFESIONES BASE (Origen: V5, normalizadas con V6)
-- =====================================================================
INSERT INTO profesion (nombre, descripcion) VALUES
    ('fotografo',        'Profesional dedicado a la captura de imágenes fotográficas.'),
    ('modelo',           'Profesional que posa para producciones fotográficas, audiovisuales o desfiles.'),
    ('maquillador',      'Especialista en técnicas de maquillaje.'),
    ('diseniador_moda',  'Profesional dedicado a la creación y confección de indumentaria.'),
    ('productor_moda',   'Encargado de coordinar y organizar los recursos para producciones visuales.'),
    ('estilista_imagen', 'Asesor responsable de definir la estética y vestimenta general.'),
    ('estilista_cabello','Especialista en el peinado y tratamiento capilar para producciones.')
ON CONFLICT (nombre) DO NOTHING;


-- =====================================================================
-- 5. CARACTERÍSTICAS TÉCNICAS (Profesión: 'modelo') (Origen: V8, V9, V10)
-- =====================================================================

-- 5.1. Características de la profesión modelo con sus tipos de datos definitivos
INSERT INTO caracteristica_tecnica (codigo, unidad, id_profesion, tipo_dato)
SELECT v.codigo, v.unidad, p.id_profesion, v.tipo_dato
FROM (VALUES
    ('altura',         'cm',    'NUMERICO'),
    ('medida_pecho',   'cm',    'NUMERICO'),
    ('medida_cintura', 'cm',    'NUMERICO'),
    ('medida_cadera',  'cm',    'NUMERICO'),
    ('color_ojos',     'color', 'ENUMERADO'),
    ('color_cabello',  'color', 'ENUMERADO'),
    ('color_piel',     'color', 'ENUMERADO'),
    ('tipo_cabello',   NULL,    'ENUMERADO')
) AS v(codigo, unidad, tipo_dato)
JOIN profesion p ON p.nombre = 'modelo';


-- =====================================================================
-- 6. VALORES DE CATÁLOGO PARA CARACTERÍSTICAS ENUMERADAS (Origen: V9, V10)
-- =====================================================================

-- 6.1. Valores para 'color_ojos'
INSERT INTO valor_caracteristica (id_caracteristica, codigo, color_hex)
SELECT ct.id_caracteristica, v.codigo, v.color_hex
FROM (VALUES
    ('marron',   '#6B4226'),
    ('negro',    '#1C1C1C'),
    ('azul',     '#3D85C6'),
    ('verde',    '#4E9A51'),
    ('avellana', '#A67B5B'),
    ('gris',     '#A9A9A9')
) AS v(codigo, color_hex)
JOIN caracteristica_tecnica ct ON ct.codigo = 'color_ojos';

-- 6.2. Valores para 'color_cabello'
INSERT INTO valor_caracteristica (id_caracteristica, codigo, color_hex)
SELECT ct.id_caracteristica, v.codigo, v.color_hex
FROM (VALUES
    ('negro',     '#1C1C1C'),
    ('castano',   '#4A2E1F'),
    ('rubio',     '#E8C267'),
    ('pelirrojo', '#B23A1E'),
    ('vino',      '#5F0000'),
    ('canoso',    '#C9C9C9'),
    ('verde',     '#519E35'),
    ('azul',      '#107AB3'),
    ('rosa',      '#FF33C2'),
    ('blanco',    '#FFFFFF'),
    ('violeta',   '#560080'),
    ('otro',      NULL)
) AS v(codigo, color_hex)
JOIN caracteristica_tecnica ct ON ct.codigo = 'color_cabello';

-- 6.3. Valores para 'color_piel'
INSERT INTO valor_caracteristica (id_caracteristica, codigo, color_hex)
SELECT ct.id_caracteristica, v.codigo, v.color_hex
FROM (VALUES
    ('muy_clara',  '#F6D8C6'),
    ('clara',      '#EAC1A0'),
    ('media',      '#C68863'),
    ('morena',     '#8D5A3B'),
    ('oscura',     '#5C3A21'),
    ('muy_oscura', '#3B2415')
) AS v(codigo, color_hex)
JOIN caracteristica_tecnica ct ON ct.codigo = 'color_piel';

-- 6.4. Valores para 'tipo_cabello'
INSERT INTO valor_caracteristica (id_caracteristica, codigo)
SELECT ct.id_caracteristica, v.codigo
FROM (VALUES
    ('lacio'),
    ('ondulado'),
    ('rizado'),
    ('afro')
) AS v(codigo)
JOIN caracteristica_tecnica ct ON ct.codigo = 'tipo_cabello';


-- =====================================================================
-- 7. CONFIGURACIÓN DEL SISTEMA (Origen: V22)
-- =====================================================================
INSERT INTO configuracion_sistema (clave, valor, descripcion) VALUES
    ('SCHEDULER_DESHABILITACION_HORA', '2', 'Hora del día (0-23) en la que se ejecuta el scheduler de deshabilitación'),
    ('SCHEDULER_DESHABILITACION_MINUTO', '0', 'Minuto (0-59) en el que se ejecuta el scheduler de deshabilitación'),
    ('SCHEDULER_DESHABILITACION_CRON', '0 0 2 * * *', 'Expresión cron para el scheduler de deshabilitación de usuarios')
ON CONFLICT (clave) DO NOTHING;
