package org.mgroko.backend.perfiles.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.UnidadMedida;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;

class CaracteristicaTecnicaMapperTest {

    @Test
    void toResponse_profesionPresente_remapeaCamposConProfesion() {
        Profesion profesion = Profesion.builder().idProfesion(2L).nombre("modelo").build();
        UnidadMedida unidad = UnidadMedida.builder()
                .idUnidad(1L)
                .nombre("Centímetro")
                .simbolo("cm")
                .tipoDatoPermitido("NUMERICO")
                .build();

        CaracteristicaTecnica caracteristica = CaracteristicaTecnica.builder()
                .idCaracteristica(11L)
                .codigo("altura")
                .nombre("Altura")
                .unidadMedida(unidad)
                .profesion(profesion)
                .build();

        CaracteristicaTecnicaResponse response = CaracteristicaTecnicaMapper.toResponse(caracteristica);

        assertEquals(11L, response.idCaracteristica());
        assertEquals("altura", response.codigo());
        assertEquals("Altura", response.nombre());
        assertNotNull(response.unidad());
        assertEquals(1L, response.unidad().idUnidad());
        assertEquals("cm", response.unidad().simbolo());
        assertEquals("Centímetro", response.unidad().nombre());
        assertEquals("NUMERICO", response.unidad().tipoDatoPermitido());
        assertEquals(2L, response.idProfesion());
        assertEquals("modelo", response.profesion());
    }

    @Test
    void toResponse_sinProfesion_devuelveProfesionNula() {
        CaracteristicaTecnica caracteristica = CaracteristicaTecnica.builder()
                .idCaracteristica(20L)
                .codigo("libre")
                .nombre("Libre")
                .unidadMedida(null)
                .build();

        CaracteristicaTecnicaResponse response = CaracteristicaTecnicaMapper.toResponse(caracteristica);

        assertEquals("libre", response.codigo());
        assertEquals("Libre", response.nombre());
        assertNull(response.unidad());
        assertNull(response.idProfesion());
        assertNull(response.profesion());
    }
}