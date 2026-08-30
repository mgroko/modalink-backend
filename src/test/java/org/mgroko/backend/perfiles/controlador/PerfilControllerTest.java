package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.perfiles.dto.CaracteristicaPerfilRequest;
import org.mgroko.backend.perfiles.dto.CaracteristicaResponse;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.EditarPerfilRequest;
import org.mgroko.backend.perfiles.dto.EliminarPerfilResponse;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.exception.PerfilDuplicadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.perfiles.servicio.EditarPerfilService;
import org.mgroko.backend.perfiles.servicio.EliminarPerfilService;
import org.mgroko.backend.perfiles.servicio.ReactivarPerfilService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @MockitoBean
    private EditarPerfilService editarPerfilService;

    @MockitoBean
    private EliminarPerfilService eliminarPerfilService;

    @MockitoBean
    private ReactivarPerfilService reactivarPerfilService;

    private UsernamePasswordAuthenticationToken autenticacion() {
        return new UsernamePasswordAuthenticationToken("1", null, List.of());
    }

    private CrearPerfilRequest requestValido() {
        return new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(11L, "175", null)));
    }

    @Test
    void crear_datosValidos_devuelve201() throws Exception {
        var authentication = autenticacion();

        PerfilResponse response = new PerfilResponse(
                10L, "Luna", "Modelo profesional.", "Activo", "modelo", null,
                List.of(new CaracteristicaResponse(11L, "altura", "175", null, null, null)));

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
                List.of(new CaracteristicaPerfilRequest(null, "175", null)));

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
                List.of(new CaracteristicaResponse(11L, "altura", "175", null, null, null)));

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

    @Test
    void obtener_perfilExistente_devuelve200() throws Exception {
        var authentication = autenticacion();

        PerfilResponse perfil = new PerfilResponse(
                10L, "Luna", "Modelo profesional.", "Activo", "modelo", null, List.of());

        when(usuarioPerfilService.obtenerPerfilPropio(1L, 10L)).thenReturn(perfil);

        mockMvc.perform(get("/perfiles/10")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPerfil").value(10))
                .andExpect(jsonPath("$.nombreArtistico").value("Luna"));
    }

    @Test
    void obtener_perfilInexistente_devuelve404() throws Exception {
        var authentication = autenticacion();

        when(usuarioPerfilService.obtenerPerfilPropio(1L, 999L))
                .thenThrow(new org.mgroko.backend.admin.exception.PerfilNoEncontradoException("Perfil no encontrado."));

        mockMvc.perform(get("/perfiles/999")
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Perfil no encontrado."));
    }

    @Test
    void editar_datosValidos_devuelve200() throws Exception {
        var authentication = autenticacion();

        PerfilResponse perfil = new PerfilResponse(
                10L, "Luna Nova", "Modelo renovada.", "Activo", "modelo", null,
                List.of(new CaracteristicaResponse(11L, "altura", "176", null, null, null)));

        when(editarPerfilService.editar(anyLong(), anyLong(), any(EditarPerfilRequest.class)))
                .thenReturn(perfil);

        EditarPerfilRequest request = new EditarPerfilRequest(
                "Luna Nova", "Modelo renovada.", null,
                List.of(new CaracteristicaPerfilRequest(11L, "176", null)));

        mockMvc.perform(put("/perfiles/10")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreArtistico").value("Luna Nova"))
                .andExpect(jsonPath("$.biografia").value("Modelo renovada."));
    }

    @Test
    void editar_nombreArtisticoVacio_devuelve400() throws Exception {
        var authentication = autenticacion();

        EditarPerfilRequest request = new EditarPerfilRequest(
                "", "Modelo renovada.", null, List.of());

        mockMvc.perform(put("/perfiles/10")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(editarPerfilService, never()).editar(anyLong(), anyLong(), any());
    }

    @Test
    void editar_perfilEnBaja_devuelve409() throws Exception {
        var authentication = autenticacion();

        when(editarPerfilService.editar(anyLong(), anyLong(), any(EditarPerfilRequest.class)))
                .thenThrow(new PerfilEnBajaException("No se puede editar un perfil dado de baja."));

        EditarPerfilRequest request = new EditarPerfilRequest(
                "Luna Nova", "Modelo renovada.", null, List.of());

        mockMvc.perform(put("/perfiles/10")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede editar un perfil dado de baja."));
    }

    @Test
    void eliminar_perfilActivo_devuelve200() throws Exception {
        var authentication = autenticacion();

        EliminarPerfilResponse response = new EliminarPerfilResponse(
                "Solicitud de baja registrada.", java.time.LocalDateTime.now().plusDays(30));
        when(eliminarPerfilService.eliminar(1L, 10L)).thenReturn(response);

        mockMvc.perform(delete("/perfiles/10")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Solicitud de baja registrada."));
    }

    @Test
    void eliminar_perfilPendienteBaja_devuelve409() throws Exception {
        var authentication = autenticacion();

        when(eliminarPerfilService.eliminar(1L, 10L))
                .thenThrow(new PerfilEnBajaException("Ya existe una solicitud de baja activa para este perfil."));

        mockMvc.perform(delete("/perfiles/10")
                        .principal(authentication))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una solicitud de baja activa para este perfil."));
    }

    @Test
    void reactivar_dentroDePlazo_devuelve200() throws Exception {
        var authentication = autenticacion();

        PerfilResponse perfil = new PerfilResponse(
                10L, "Luna", "Modelo profesional.", "Activo", "modelo", null, List.of());
        when(reactivarPerfilService.reactivar(1L, 10L))
                .thenReturn(org.mgroko.backend.modelo.Perfil.builder()
                        .idPerfil(10L)
                        .nombreArtistico("Luna")
                        .biografia("Modelo profesional.")
                        .estado(org.mgroko.backend.modelo.enums.EstadoPerfil.Activo)
                        .profesion(org.mgroko.backend.modelo.Profesion.builder().idProfesion(2L).nombre("modelo").build())
                        .build());

        mockMvc.perform(post("/perfiles/10/reactivar")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Activo"));
    }
}