-- =====================================================================
-- V12: agrega id_georef a la tabla ubicacion.
-- Guarda el id de la localidad del catálogo de Georef para poder
-- prellenar el selector provincia/localidad en la edición de datos
-- personales sin depender del texto (localidad/provincia).
-- =====================================================================

ALTER TABLE ubicacion
    ADD COLUMN id_georef VARCHAR(20);