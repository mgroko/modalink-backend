package org.mgroko.backend.conexiones.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.conexiones.dto.BuscarConexionesFiltro;
import org.mgroko.backend.conexiones.dto.ConexionResponse;
import org.mgroko.backend.conexiones.dto.EstadoPresenciaConexion;
import org.mgroko.backend.conexiones.dto.PerfilConexionResponse;
import org.mgroko.backend.conexiones.dto.ProfesionConexionResponse;
import org.mgroko.backend.conexiones.dto.ResumenConexionesResponse;
import org.mgroko.backend.conexiones.exception.ConexionNoActivaException;
import org.mgroko.backend.conexiones.exception.ConexionNoEncontradaException;
import org.mgroko.backend.conexiones.servicio.BuscarConexionService;
import org.mgroko.backend.conexiones.servicio.EliminarConexionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class,
        controllers = ConexionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConexionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuscarConexionService buscarConexionService;

    @MockitoBean
    private EliminarConexionService eliminarConexionService;

    @Test
    void buscar_devuelve200ConPagina() throws Exception {
        when(buscarConexionService.buscar(
                eq(1L), eq(10L), any(BuscarConexionesFiltro.class), eq(0), eq(20)))
                .thenReturn(pagina());

        mockMvc.perform(get("/conexiones")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].idConexion").value(55))
                .andExpect(jsonPath("$.contenido[0].perfil.idPerfil").value(42))
                .andExpect(jsonPath("$.contenido[0].enLinea").value(false))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void buscar_conFiltroDePresencia_loPasaAlServicio() throws Exception {
        when(buscarConexionService.buscar(
                eq(1L), eq(10L), any(BuscarConexionesFiltro.class), eq(0), eq(20)))
                .thenReturn(pagina());

        mockMvc.perform(get("/conexiones")
                        .param("estadoPresencia", "EN_LINEA")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk());

        verify(buscarConexionService).buscar(
                eq(1L),
                eq(10L),
                argThat(filtro -> filtro.estadoPresencia()
                        == EstadoPresenciaConexion.EN_LINEA),
                eq(0),
                eq(20));
    }

    @Test
    void resumen_devuelveTotalesYProfesiones() throws Exception {
        when(buscarConexionService.resumen(1L, 10L))
                .thenReturn(new ResumenConexionesResponse(
                        6,
                        4,
                        List.of(new ProfesionConexionResponse(2L, "Fotógrafo", 3))));

        mockMvc.perform(get("/conexiones/resumen")
                        .principal(autenticacion(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConexiones").value(6))
                .andExpect(jsonPath("$.totalEnLinea").value(4))
                .andExpect(jsonPath("$.profesiones[0].nombre").value("Fotógrafo"))
                .andExpect(jsonPath("$.profesiones[0].cantidad").value(3));
    }

    @Test
    void buscar_sinPerfilActivo_devuelve404() throws Exception {
        mockMvc.perform(get("/conexiones")
                        .principal(autenticacion(null)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(buscarConexionService);
    }

    @Test
    void eliminar_conexionPropia_devuelve204() throws Exception {
        doNothing().when(eliminarConexionService).eliminar(1L, 10L, 55L);

        mockMvc.perform(delete("/conexiones/55")
                        .principal(autenticacion(10L)))
                .andExpect(status().isNoContent());

        verify(eliminarConexionService).eliminar(1L, 10L, 55L);
    }

    @Test
    void eliminar_conexionInexistente_devuelve404() throws Exception {
        doThrow(new ConexionNoEncontradaException())
                .when(eliminarConexionService).eliminar(1L, 10L, 55L);

        mockMvc.perform(delete("/conexiones/55")
                        .principal(autenticacion(10L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_conexionYaEliminada_devuelve409() throws Exception {
        doThrow(new ConexionNoActivaException("La conexión ya fue eliminada."))
                .when(eliminarConexionService).eliminar(1L, 10L, 55L);

        mockMvc.perform(delete("/conexiones/55")
                        .principal(autenticacion(10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONEXION_NO_ACTIVA"));
    }

    private UsernamePasswordAuthenticationToken autenticacion(Long idPerfilActivo) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "1", null, java.util.List.of());
        authentication.setDetails(
                new ContextoAutenticacion(1L, idPerfilActivo, "Ana Moda"));
        return authentication;
    }

    private PaginaResponse<ConexionResponse> pagina() {
        var item = new ConexionResponse(
                55L,
                LocalDateTime.of(2026, 11, 1, 10, 0),
                7L,
                42L,
                new PerfilConexionResponse(
                        42L, "Bruno Estilo", "Bruno", "Gómez",
                        2L, "Fotógrafo", null, null));

        return new PaginaResponse<>(List.of(item), 0, 20, 1, 1, true, true);
    }
}
