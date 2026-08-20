-- =====================================================================
-- V3: Atributos de caracteristica_tecnica / caracteristica_perfil
--     + módulo Género (1 a 1 con Usuario)
-- =====================================================================

-- ---------------------------------------------------------------------
-- caracteristica_tecnica: se reemplazan nombre/descripcion por
-- codigo y unidad, que quedan como únicos atributos de la tabla
-- (además de id_caracteristica e id_profesion)
-- ---------------------------------------------------------------------
ALTER TABLE caracteristica_tecnica DROP COLUMN nombre;
ALTER TABLE caracteristica_tecnica DROP COLUMN descripcion;
ALTER TABLE caracteristica_tecnica ADD COLUMN codigo VARCHAR(50) NOT NULL;
ALTER TABLE caracteristica_tecnica ADD COLUMN unidad VARCHAR(50);

-- ---------------------------------------------------------------------
-- caracteristica_perfil: valor concreto que asume la característica
-- para ese perfil, y fecha en que se registró
-- ---------------------------------------------------------------------
ALTER TABLE caracteristica_perfil ADD COLUMN valor VARCHAR(50);
ALTER TABLE caracteristica_perfil ADD COLUMN fecha_registro DATE;

-- ---------------------------------------------------------------------
-- Tabla genero
-- ---------------------------------------------------------------------
CREATE TABLE genero(
    id_genero    BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(50) NOT NULL,
    CONSTRAINT uq_genero_codigo UNIQUE (codigo)
);

-- ---------------------------------------------------------------------
-- Datos iniciales (puede sufrir modificaciones)
-- ---------------------------------------------------------------------
INSERT INTO genero (codigo) VALUES
    ('mujer'),
    ('hombre'),
    ('no_binario'),
    ('no_decirlo');

-- ---------------------------------------------------------------------
-- Relación 1 a 1 Usuario-Género 
-- ---------------------------------------------------------------------
ALTER TABLE usuario ADD COLUMN id_genero BIGINT NOT NULL REFERENCES genero(id_genero);
ALTER TABLE usuario ADD CONSTRAINT uq_usuario_genero UNIQUE (id_genero);