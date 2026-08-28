package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.perfiles.dto.ProfesionResponse;
import org.mgroko.backend.perfiles.servicio.ProfesionService;
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

@WebMvcTest(controllers = ProfesionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfesionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfesionService profesionService;

    @Test
    void buscar_conNombre_devuelve200() throws Exception {
        when(profesionService.buscar("mo"))
                .thenReturn(List.of(new ProfesionResponse(2L, "modelo", "Profesional que posa.")));

        mockMvc.perform(get("/profesiones").param("nombre", "mo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("modelo"))
                .andExpect(jsonPath("$[0].idProfesion").value(2));
    }

    @Test
    void buscar_sinFiltros_devuelve200() throws Exception {
        when(profesionService.buscar(null)).thenReturn(List.of());

        mockMvc.perform(get("/profesiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(profesionService).buscar(null);
    }

    @Test
    void buscar_sinCoincidencias_devuelveListaVacia() throws Exception {
        when(profesionService.buscar("inexistente")).thenReturn(List.of());

        mockMvc.perform(get("/profesiones").param("nombre", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}