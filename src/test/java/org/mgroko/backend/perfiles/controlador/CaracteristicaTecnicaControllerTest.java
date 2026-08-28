package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.servicio.CaracteristicaTecnicaService;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

@WebMvcTest(controllers = CaracteristicaTecnicaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CaracteristicaTecnicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaracteristicaTecnicaService caracteristicaTecnicaService;

    @Test
    void buscar_conFiltros_devuelve200() throws Exception {
        when(caracteristicaTecnicaService.buscar(2L, "alt", null))
                .thenReturn(List.of(new CaracteristicaTecnicaResponse(11L, "altura", "cm", 2L, "modelo")));

        mockMvc.perform(get("/profesiones/2/caracteristicas-tecnicas")
                        .param("codigo", "alt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("altura"))
                .andExpect(jsonPath("$[0].unidad").value("cm"))
                .andExpect(jsonPath("$[0].profesion").value("modelo"));
    }

    @Test
    void buscar_sinFiltros_devuelve200() throws Exception {
        when(caracteristicaTecnicaService.buscar(eq(2L), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/profesiones/2/caracteristicas-tecnicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(caracteristicaTecnicaService).buscar(2L, null, null);
    }

    @Test
    void buscar_profesionInexistente_devuelve400() throws Exception {
        when(caracteristicaTecnicaService.buscar(eq(999L), isNull(), isNull()))
                .thenThrow(new ProfesionNoEncontradaException("Profesión no encontrada: 999"));

        mockMvc.perform(get("/profesiones/999/caracteristicas-tecnicas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Profesión no encontrada: 999"));
    }
}