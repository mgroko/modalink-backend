package org.mgroko.backend.admin.controlador;

import java.util.List;

import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.UnidadMedidaResponse;
import org.mgroko.backend.modelo.UnidadMedida;
import org.mgroko.backend.repositorio.UnidadMedidaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUnidadMedidaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUnidadMedidaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnidadMedidaRepository unidadMedidaRepository;

    private UsernamePasswordAuthenticationToken autenticacionAdmin() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    @Test
    void listar_sinFiltro_retornaTodas() throws Exception {
        UnidadMedida u1 = UnidadMedida.builder().idUnidad(1L).nombre("Centímetro").simbolo("cm").tipoDatoPermitido("NUMERICO").build();
        UnidadMedida u2 = UnidadMedida.builder().idUnidad(2L).nombre("Kilogramo").simbolo("kg").tipoDatoPermitido("NUMERICO").build();

        when(unidadMedidaRepository.findAll()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/admin/unidades-medida")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].simbolo").value("cm"))
                .andExpect(jsonPath("$[1].simbolo").value("kg"));
    }

    @Test
    void listar_conFiltroTipoDato_retornaFiltradas() throws Exception {
        UnidadMedida u1 = UnidadMedida.builder().idUnidad(1L).nombre("Centímetro").simbolo("cm").tipoDatoPermitido("NUMERICO").build();
        UnidadMedida u2 = UnidadMedida.builder().idUnidad(3L).nombre("Palabras").simbolo("pal").tipoDatoPermitido("TEXTO").build();

        when(unidadMedidaRepository.findAll()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/admin/unidades-medida")
                        .param("tipoDato", "NUMERICO")
                        .principal(autenticacionAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].simbolo").value("cm"));
    }
}
