-- =====================================================================
-- ModaLink - Modificacion nombres de profesiones
-- Migración Flyway: V6__fix_profesiones.sql
-- =====================================================================

UPDATE profesion 
SET nombre = 'fotografo' 
WHERE nombre = 'Fotógrafo';

UPDATE profesion 
SET nombre = 'modelo' 
WHERE nombre = 'Modelo';

UPDATE profesion 
SET nombre = 'maquillador', 
    descripcion = 'Especialista en técnicas de maquillaje.' 
WHERE nombre = 'Maquillador';

UPDATE profesion 
SET nombre = 'diseniador_moda' 
WHERE nombre = 'Diseñador de Moda';

UPDATE profesion 
SET nombre = 'productor_moda' 
WHERE nombre = 'Productor de Moda';

UPDATE profesion 
SET nombre = 'estilista_imagen' 
WHERE nombre = 'Estilista de Imagen';

UPDATE profesion 
SET nombre = 'estilista_cabello' 
WHERE nombre = 'Estilista de Cabello';