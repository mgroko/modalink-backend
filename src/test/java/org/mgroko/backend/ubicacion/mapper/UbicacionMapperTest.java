package org.mgroko.backend.ubicacion.mapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

class UbicacionMapperTest {

    @Test
    void toResponse_conTodosLosCampos_mapea() {
        Ubicacion ubicacion = Ubicacion.builder()
                .idUbicacion(7L)
                .idGeoref("0208401001")
                .localidad("Recoleta")
                .provincia("Ciudad Autónoma de Buenos Aires")
                .pais("Argentina")
                .codigoPostal("C1024")
                .latitud(new BigDecimal("-34.588043854884"))
                .longitud(new BigDecimal("-58.3971817497302"))
                .build();

        UbicacionResponse response = UbicacionMapper.toResponse(ubicacion);

        assertEquals(7L, response.idUbicacion());
        assertEquals("0208401001", response.localidadId());
        assertEquals("Recoleta", response.localidad());
        assertEquals("Ciudad Autónoma de Buenos Aires", response.provincia());
        assertEquals("Argentina", response.pais());
        assertEquals("C1024", response.codigoPostal());
        assertEquals(new BigDecimal("-34.588043854884"), response.latitud());
        assertEquals(new BigDecimal("-58.3971817497302"), response.longitud());
    }

    @Test
    void toResponse_conCamposOpcionalesNulos_devuelveNull() {
        Ubicacion ubicacion = Ubicacion.builder()
                .idUbicacion(8L)
                .localidad("Saavedra")
                .provincia("Ciudad Autónoma de Buenos Aires")
                .build();

        UbicacionResponse response = UbicacionMapper.toResponse(ubicacion);

        assertEquals("Saavedra", response.localidad());
        assertNull(response.localidadId());
        assertNull(response.pais());
        assertNull(response.codigoPostal());
        assertNull(response.latitud());
        assertNull(response.longitud());
    }
}