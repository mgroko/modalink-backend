package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.perfiles.dto.CaracteristicaPerfilRequest;
import org.mgroko.backend.perfiles.dto.CaracteristicaResponse;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.exception.PerfilDuplicadoException;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.perfiles.servicio.UsuarioPerfilService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PerfilController.class)
@AutoConfigureMockMvc(addFilters = false)
class PerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CrearPerfilService crearPerfilService;

    @MockitoBean
    private UsuarioPerfilService usuarioPerfilService;

    private UsernamePasswordAuthenticationToken autenticacion() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private CrearPerfilRequest requestValido() {
        return new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(11L, "175")));
    }

    @Test
    void crear_datosValidos_devuelve201() throws Exception {
        var authentication = autenticacion();

        PerfilResponse response = new PerfilResponse(
                10L, "Luna", "Modelo profesional.", "Activo", "modelo", null,
                List.of(new CaracteristicaResponse(11L, "altura", "175")));

        when(crearPerfilService.crear(anyLong(), any(CrearPerfilRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/perfiles")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPerfil").value(10))
                .andExpect(jsonPath("$.nombreArtistico").value("Luna"))
                .andExpect(jsonPath("$.profesion").value("modelo"))
                .andExpect(jsonPath("$.caracteristicas[0].codigo").value("altura"))
                .andExpect(jsonPath("$.caracteristicas[0].valor").value("175"));
    }

    @Test
    void crear_nombreArtisticoVacio_devuelve400() throws Exception {
        var authentication = autenticacion();

        CrearPerfilRequest request = new CrearPerfilRequest(
                "", 2L, "Modelo profesional.", List.of());

        mockMvc.perform(post("/perfiles")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(crearPerfilService, never()).crear(anyLong(), any());
    }

    @Test
    void crear_nombreArtisticoCorto_devuelve400() throws Exception {
        var authentication = autenticacion();

        CrearPerfilRequest request = new CrearPerfilRequest(
                "L", 2L, "Modelo profesional.", List.of());

        mockMvc.perform(post("/perfiles")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(crearPerfilService, never()).crear(anyLong(), any());
    }

    @Test
    void crear_profesionNula_devuelve400() throws Exception {
        var authentication = autenticacion();

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", null, "Modelo profesional.", List.of());

        mockMvc.perform(post("/perfiles")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(crearPerfilService, never()).crear(anyLong(), any());
    }

    @Test
    void crear_biografiaVacia_devuelve400() throws Exception {
        var authentication = autenticacion();

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "", List.of());

        mockMvc.perform(post("/perfiles")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(crearPerfilService, never()).crear(anyLong(), any());
    }

    @Test
    void crear_caracteristicaSinId_devuelve400() throws Exception {
        var authentication = autenticacion();

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(null, "175")));

        mockMvc.perform(post("/perfiles")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(crearPerfilService, never()).crear(anyLong(), any());
    }

    @Test
    void crear_perfilDuplicado_devuelve409() throws Exception {
        var authentication = autenticacion();

        when(crearPerfilService.crear(anyLong(), any(CrearPerfilRequest.class)))
                .thenThrow(new PerfilDuplicadoException("El usuario ya posee un perfil para la profesión indicada."));

        mockMvc.perform(post("/perfiles")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El usuario ya posee un perfil para la profesión indicada."));
    }

    @Test
    void crear_profesionNoEncontrada_devuelve400() throws Exception {
        var authentication = autenticacion();

        when(crearPerfilService.crear(anyLong(), any(CrearPerfilRequest.class)))
                .thenThrow(new ProfesionNoEncontradaException("Profesión no encontrada: 999"));

        mockMvc.perform(post("/perfiles")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Profesión no encontrada: 999"));
    }

    @Test
    void listarPerfilesPropios_datosValidos_devuelve200() throws Exception {
        var authentication = autenticacion();

        PerfilResponse perfil = new PerfilResponse(
                10L, "Luna", "Modelo profesional.", "Activo", "modelo", null,
                List.of(new CaracteristicaResponse(11L, "altura", "175")));

        when(usuarioPerfilService.listarPerfilesPropios(1L)).thenReturn(List.of(perfil));

        mockMvc.perform(get("/usuarios/me/perfiles")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombreArtistico").value("Luna"))
                .andExpect(jsonPath("$[0].profesion").value("modelo"))
                .andExpect(jsonPath("$[0].caracteristicas[0].codigo").value("altura"));
    }

    @Test
    void listarPerfilesPropios_sinPerfiles_devuelveListaVacia() throws Exception {
        var authentication = autenticacion();

        when(usuarioPerfilService.listarPerfilesPropios(1L)).thenReturn(List.of());

        mockMvc.perform(get("/usuarios/me/perfiles")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listarPerfilesPropios_usuarioNoExiste_devuelve401() throws Exception {
        var authentication = autenticacion();

        when(usuarioPerfilService.listarPerfilesPropios(1L))
                .thenThrow(new UsuarioNoEncontradoException("Usuario no encontrado."));

        mockMvc.perform(get("/usuarios/me/perfiles")
                        .principal(authentication))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado."));
    }
}