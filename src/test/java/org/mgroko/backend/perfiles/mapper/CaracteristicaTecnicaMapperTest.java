package org.mgroko.backend.perfiles.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;

class CaracteristicaTecnicaMapperTest {

    @Test
    void toResponse_profesionPresente_remapeaCamposConProfesion() {
        Profesion profesion = Profesion.builder().idProfesion(2L).nombre("modelo").build();

        CaracteristicaTecnica caracteristica = CaracteristicaTecnica.builder()
                .idCaracteristica(11L)
                .codigo("altura")
                .unidad("cm")
                .profesion(profesion)
                .build();

        CaracteristicaTecnicaResponse response = CaracteristicaTecnicaMapper.toResponse(caracteristica);

        assertEquals(11L, response.idCaracteristica());
        assertEquals("altura", response.codigo());
        assertEquals("cm", response.unidad());
        assertEquals(2L, response.idProfesion());
        assertEquals("modelo", response.profesion());
    }

    @Test
    void toResponse_sinProfesion_devuelveProfesionNula() {
        CaracteristicaTecnica caracteristica = CaracteristicaTecnica.builder()
                .idCaracteristica(20L)
                .codigo("libre")
                .unidad(null)
                .build();

        CaracteristicaTecnicaResponse response = CaracteristicaTecnicaMapper.toResponse(caracteristica);

        assertEquals("libre", response.codigo());
        assertNull(response.idProfesion());
        assertNull(response.profesion());
    }
}