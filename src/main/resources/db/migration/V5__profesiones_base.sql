-- =====================================================================
-- ModaLink - Inserción de profesiones base
-- Migración Flyway: V5__profesiones_base.sql
-- =====================================================================

INSERT INTO profesion (nombre, descripcion) VALUES
    ('Fotógrafo', 'Profesional dedicado a la captura de imágenes fotográficas.'),
    ('Modelo', 'Profesional que posa para producciones fotográficas, audiovisuales o desfiles.'),
    ('Maquillador', 'Especialista en técnicas de maquillaje.'),
    ('Diseñador de Moda', 'Profesional dedicado a la creación y confección de indumentaria.'),
    ('Productor de Moda', 'Encargado de coordinar y organizar los recursos para producciones visuales.'),
    ('Estilista de Imagen', 'Asesor responsable de definir la estética y vestimenta general.'),
    ('Estilista de Cabello', 'Especialista en el peinado y tratamiento capilar para producciones.');