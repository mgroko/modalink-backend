package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.perfiles.servicio.HabilidadService;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HabilidadController.class)
@AutoConfigureMockMvc(addFilters = false)
class HabilidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HabilidadService habilidadService;

    private HabilidadResponse habilidad() {
        return new HabilidadResponse(1L, "FOTO_ESTUDIO",
                "Fotografía de Estudio", "Iluminación y sets.");
    }

    @Test
    void buscar_conNombre_devuelve200() throws Exception {
        when(habilidadService.buscar("foto")).thenReturn(List.of(habilidad()));

        mockMvc.perform(get("/habilidades").param("nombre", "foto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idHabilidad").value(1))
                .andExpect(jsonPath("$[0].codigo").value("FOTO_ESTUDIO"))
                .andExpect(jsonPath("$[0].nombre").value("Fotografía de Estudio"));
    }

    @Test
    void buscar_sinNombre_devuelveTodas() throws Exception {
        when(habilidadService.buscar(null)).thenReturn(List.of());

        mockMvc.perform(get("/habilidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(habilidadService).buscar(null);
    }
}
