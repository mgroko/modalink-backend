package org.mgroko.backend.perfiles.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.perfiles.dto.ProfesionResponse;

class ProfesionMapperTest {

    @Test
    void toResponse_remapeaCampos() {
        Profesion profesion = Profesion.builder()
                .idProfesion(2L)
                .nombre("modelo")
                .descripcion("Profesional que posa para producciones.")
                .build();

        ProfesionResponse response = ProfesionMapper.toResponse(profesion);

        assertEquals(2L, response.idProfesion());
        assertEquals("modelo", response.nombre());
        assertEquals("Profesional que posa para producciones.", response.descripcion());
    }

    @Test
    void toResponse_descripcionAusente_devuelveNull() {
        Profesion profesion = Profesion.builder()
                .idProfesion(3L)
                .nombre("fotografo")
                .build();

        ProfesionResponse response = ProfesionMapper.toResponse(profesion);

        assertNull(response.descripcion());
    }
}