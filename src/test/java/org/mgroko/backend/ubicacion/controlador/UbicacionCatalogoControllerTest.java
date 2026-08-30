package org.mgroko.backend.ubicacion.controlador;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.ubicacion.dto.LocalidadResponse;
import org.mgroko.backend.ubicacion.dto.ProvinciaResponse;
import org.mgroko.backend.ubicacion.servicio.GeorefCatalogoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UbicacionCatalogoController.class)
@AutoConfigureMockMvc(addFilters = false)
class UbicacionCatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeorefCatalogoService catalogoGeoref;

    @Test
    void listarProvincias_devuelve200ConLista() throws Exception {
        when(catalogoGeoref.listarProvincias()).thenReturn(List.of(
                new ProvinciaResponse("02", "Ciudad Autónoma de Buenos Aires"),
                new ProvinciaResponse("06", "Buenos Aires")));

        mockMvc.perform(get("/ubicaciones/provincias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("02"))
                .andExpect(jsonPath("$[0].nombre").value("Ciudad Autónoma de Buenos Aires"));
    }

    @Test
    void buscarLocalidades_sinFiltros_devuelve200() throws Exception {
        when(catalogoGeoref.buscarLocalidades(isNull(), isNull())).thenReturn(List.of(
                new LocalidadResponse("0208401002", "Saavedra", "02",
                        "Ciudad Autónoma de Buenos Aires",
                        new BigDecimal("-34.5548978526608"), new BigDecimal("-58.4863271154338"))));

        mockMvc.perform(get("/ubicaciones/localidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("0208401002"))
                .andExpect(jsonPath("$[0].provinciaNombre").value("Ciudad Autónoma de Buenos Aires"));
    }

    @Test
    void buscarLocalidades_conFiltros_pasaParametros() throws Exception {
        when(catalogoGeoref.buscarLocalidades("02", "saavedra")).thenReturn(List.of(
                new LocalidadResponse("0208401002", "Saavedra", "02",
                        "Ciudad Autónoma de Buenos Aires",
                        new BigDecimal("-34.5548978526608"), new BigDecimal("-58.4863271154338"))));

        mockMvc.perform(get("/ubicaciones/localidades")
                        .param("provinciaId", "02")
                        .param("nombre", "saavedra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Saavedra"));
    }
}