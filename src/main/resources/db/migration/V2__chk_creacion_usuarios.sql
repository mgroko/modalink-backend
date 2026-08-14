ALTER TABLE usuario ADD CONSTRAINT chk_usuario_nombre_len CHECK (char_length(trim(nombre)) >= 2);
ALTER TABLE usuario ADD CONSTRAINT chk_usuario_apellido_len CHECK (char_length(trim(apellido)) >= 2);
ALTER TABLE usuario ADD CONSTRAINT chk_usuario_edad_minima CHECK (fecha_nacimiento <= CURRENT_DATE - INTERVAL '18 years');