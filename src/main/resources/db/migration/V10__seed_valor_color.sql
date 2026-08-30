-- =====================================================================
-- ModaLink - Marca color_ojos / color_cabello / color_piel como
-- ENUMERADO y carga el catálogo de valores en valor_caracteristica
-- Migración Flyway: V10__seed_valor_color.sql
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Marcar las características de color como ENUMERADO
-- ---------------------------------------------------------------------
UPDATE caracteristica_tecnica
SET tipo_dato = 'ENUMERADO'
WHERE codigo IN ('color_ojos', 'color_cabello', 'color_piel');
 
-- ---------------------------------------------------------------------
-- 2) Seed: valores de color_ojos (con hex para el cuadrito de color)
-- ---------------------------------------------------------------------
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
 
-- ---------------------------------------------------------------------
-- 3) Seed: valores de color_cabello (con hex para el cuadrito de color)
--    'otro' no representa un color puntual, queda sin hex (NULL)
-- ---------------------------------------------------------------------
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
 
-- ---------------------------------------------------------------------
-- 4) Seed: valores de color_piel (con hex para el cuadrito de color)
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 5) Seed: valores de tipo_cabello
-- ---------------------------------------------------------------------

INSERT INTO caracteristica_tecnica (codigo, unidad, id_profesion, tipo_dato)
SELECT 'tipo_cabello', NULL, p.id_profesion, 'ENUMERADO'
FROM profesion p
WHERE p.nombre = 'modelo';

INSERT INTO valor_caracteristica (id_caracteristica, codigo)
SELECT ct.id_caracteristica, v.codigo
FROM (VALUES
    ('lacio'),
    ('ondulado'),
    ('rizado'),
    ('afro')
) AS v(codigo)
JOIN caracteristica_tecnica ct ON ct.codigo = 'tipo_cabello';

-- ---------------------------------------------------------------------
-- 6) Renombre y reclasificación de las características numéricas
--    ya cargadas en V8 (altura/pecho/cintura/cadera)
-- ---------------------------------------------------------------------

UPDATE caracteristica_tecnica SET codigo = 'medida_pecho'   WHERE codigo = 'pecho';
UPDATE caracteristica_tecnica SET codigo = 'medida_cintura' WHERE codigo = 'cintura';
UPDATE caracteristica_tecnica SET codigo = 'medida_cadera'  WHERE codigo = 'cadera';

UPDATE caracteristica_tecnica
SET tipo_dato = 'NUMERICO'
WHERE codigo IN ('altura', 'medida_pecho', 'medida_cintura', 'medida_cadera');

