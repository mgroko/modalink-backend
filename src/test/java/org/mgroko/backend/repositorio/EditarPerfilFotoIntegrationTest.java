package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.EditarPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.perfiles.servicio.EditarPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La edición de un perfil no puede modificar ni desvincular la foto, aunque el
 * request lleve un campo idImagen: el campo ya no existe en
 * {@link EditarPerfilRequest}, así que se recibe como propiedad desconocida y
 * se ignora sin romper la deserialización.
 *
 * Usa PostgreSQL real (Testcontainers) y transacción con rollback por test.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
class EditarPerfilFotoIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    @Autowired
    private ImagenRepository imagenRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private CrearPerfilService crearPerfilService;

    @Autowired
    private EditarPerfilService editarPerfilService;

    private Usuario usuario;
    private Long idPerfilConFoto;
    private Long idPerfilConOtraFoto;
    private Long idImagenPropia;
    private Long idImagenAjena;

    @BeforeEach
    void prepararPerfilesConFoto() {
        usuario = guardarUsuario("Edita", "Foto", "55550077", "editar.foto@example.com");
        Usuario usuarioAjeno = guardarUsuario("Otra", "Persona", "55550078", "otra.persona@example.com");

        idPerfilConFoto = crearPerfil(usuario, "Luna", idProfesionModelo());
        idPerfilConOtraFoto = crearPerfil(usuarioAjeno, "Sol", idProfesionModelo());
        idImagenPropia = guardarImagen("foto-luna");
        idImagenAjena = guardarImagen("foto-sol");
        asociarFoto(idPerfilConFoto, idImagenPropia);
        asociarFoto(idPerfilConOtraFoto, idImagenAjena);
    }

    @Test
    void editar_conIdImagenDeOtroPerfil_ignoraElCampoYSuFotoNoCambia() throws Exception {
        String cuerpo = """
                {"nombreArtistico":"Luna Editada","biografia":"Biografia editada.",
                "idImagen":%d,"caracteristicas":[]}
                """.formatted(idImagenAjena);

        String respuesta = mockMvc.perform(put("/perfiles/{idPerfil}", idPerfilConFoto)
                        .principal(principal(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreArtistico").value("Luna Editada"))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(respuesta);
        assertEquals(idImagenPropia.longValue(), json.get("idImagen").asLong(),
                "El idImagen enviado no debe poder cambiar la foto");
        assertEquals("/uploads/perfiles/foto-luna.webp", json.get("fotoUrl").asText());

        assertEquals(idImagenPropia, fotoDe(idPerfilConFoto), "La foto del perfil editado se conserva");
        assertEquals(idImagenAjena, fotoDe(idPerfilConOtraFoto), "La foto del otro perfil no cambia");
    }

    @Test
    void editar_sinTocarLaFoto_laConserva() {
        PerfilResponse editado = editarPerfilService.editar(usuario.getIdUsuario(), idPerfilConFoto,
                new EditarPerfilRequest("Luna Editada", "Biografia editada.", List.of()));

        assertEquals(idImagenPropia, editado.idImagen(), "La respuesta sigue apuntando a la misma foto");
        assertEquals("/uploads/perfiles/foto-luna.webp", editado.fotoUrl());
        assertEquals(idImagenPropia, fotoDe(idPerfilConFoto), "La foto persistida se conserva");
        assertEquals(idImagenAjena, fotoDe(idPerfilConOtraFoto), "El otro perfil queda intacto");
    }

    @Test
    void editar_perfilSinFoto_noCreaDesvinculacion() {
        Long idPerfilSinFoto = crearPerfil(usuario, "Sin Foto", idProfesionDistintaDeModelo());
        assertNull(fotoDe(idPerfilSinFoto));

        PerfilResponse editado = editarPerfilService.editar(usuario.getIdUsuario(), idPerfilSinFoto,
                new EditarPerfilRequest("Sin Foto Editado", "Biografia editada.", List.of()));

        assertNull(editado.idImagen());
        assertNull(fotoDe(idPerfilSinFoto));
    }

    private Long fotoDe(Long idPerfil) {
        Imagen imagen = perfilRepository.findById(idPerfil).orElseThrow().getImagen();
        return imagen == null ? null : imagen.getIdImagen();
    }

    private static UsernamePasswordAuthenticationToken principal(Usuario usuario) {
        return new UsernamePasswordAuthenticationToken(
                String.valueOf(usuario.getIdUsuario()), null, List.of());
    }

    private Long asociarFoto(Long idPerfil, Long idImagen) {
        Perfil perfil = perfilRepository.findById(idPerfil).orElseThrow();
        perfil.setImagen(imagenRepository.findById(idImagen).orElseThrow());
        return perfilRepository.saveAndFlush(perfil).getIdPerfil();
    }

    private Long guardarImagen(String nombreArchivo) {
        Imagen imagen = imagenRepository.save(Imagen.builder()
                .nombreArchivo(nombreArchivo + ".webp")
                .url("/uploads/perfiles/" + nombreArchivo + ".webp")
                .tipoImagen("image/webp")
                .tamanoBytes(128)
                .build());
        return imagen.getIdImagen();
    }

    private Long crearPerfil(Usuario usuario, String nombreArtistico, Long idProfesion) {
        return crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesion, "Modelo profesional.", List.of()))
                .idPerfil();
    }

    private Long idProfesionModelo() {
        return profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
    }

    private Long idProfesionDistintaDeModelo() {
        return profesionRepository.buscar("%", "").stream()
                .filter(profesion -> !profesion.getCodigo().equalsIgnoreCase("MODELO"))
                .findFirst().orElseThrow().getIdProfesion();
    }

    private Usuario guardarUsuario(String nombre, String apellido, String dni, String correo) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }
}
