package org.mgroko.backend.repositorio;

import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo real del UC-24 (moodboard): tras crear el proyecto con POST /proyectos,
 * el front sube las imágenes retenidas a POST /proyectos/{id}/moodboard/imagenes.
 * Valida contra PostgreSQL real que se crea el moodboard, que los archivos quedan
 * en disco como WebP y que el dashboard devuelve las imágenes asociadas.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
class MoodboardSubidaIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Path DIRECTORIO_SUBIDAS = Path.of("uploads", "perfiles");

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
    private CrearPerfilService crearPerfilService;

    @Autowired
    private CrearProyectoService crearProyectoService;

    @Autowired
    private MoodboardRepository moodboardRepository;

    @Autowired
    private ImagenRepository imagenRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProfesionRepository profesionRepository;

    private final List<Path> archivosCreados = new ArrayList<>();

    @Test
    void subirImagenes_creaMoodboardConArchivosWebpEnDisco() throws Exception {
        Usuario usuario = guardarUsuario("mood.subida1@example.com", "77880001");
        Long idPerfil = crearPerfil(usuario);
        Long idProyecto = crearProyecto(usuario.getIdUsuario(), idPerfil, "Proyecto Mood Subida 1");

        String cuerpo = mockMvc.perform(multipart("/proyectos/{id}/moodboard/imagenes", idProyecto)
                        .file(jpeg("uno.jpg"))
                        .file(jpeg("dos.jpg"))
                        .principal(auth(usuario, idPerfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMoodboard").isNumber())
                .andExpect(jsonPath("$.imagenes.length()").value(2))
                .andExpect(jsonPath("$.imagenes[0].url").value(
                        matchesRegex("/uploads/perfiles/moodboard_[0-9a-f\\-]{36}\\.webp")))
                .andReturn().getResponse().getContentAsString();

        JsonNode respuesta = objectMapper.readTree(cuerpo);
        for (JsonNode imagen : respuesta.get("imagenes")) {
            archivosCreados.add(DIRECTORIO_SUBIDAS.resolve(nombreDe(imagen.get("url").asText())));
        }

        for (Path archivo : archivosCreados) {
            assertTrue(Files.exists(archivo), "Debe existir el archivo convertido en disco: " + archivo);
            byte[] bytes = Files.readAllBytes(archivo);
            assertEquals("RIFF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
            assertEquals("WEBP", new String(bytes, 8, 4, StandardCharsets.US_ASCII));
        }

        Moodboard moodboard = moodboardRepository.findByProyectoIdProyecto(idProyecto).orElseThrow();
        assertEquals(2, moodboard.getImagenes().size(), "El moodboard debe quedar con las 2 imágenes");

        mockMvc.perform(get("/proyectos/{id}", idProyecto).principal(auth(usuario, idPerfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moodboard.idMoodboard").value(respuesta.get("idMoodboard").asLong()))
                .andExpect(jsonPath("$.moodboard.imagenes.length()").value(2));
    }

    @Test
    void subirImagenes_excedeLimiteDeDiez_devuelve400() throws Exception {
        Usuario usuario = guardarUsuario("mood.limite@example.com", "77880002");
        Long idPerfil = crearPerfil(usuario);
        Long idProyecto = crearProyecto(usuario.getIdUsuario(), idPerfil, "Proyecto Mood Limite");

        sembrarMoodboardConImagenes(idProyecto, 10);

        MockMultipartFile archivo = jpeg("extra.jpg");
        mockMvc.perform(multipart("/proyectos/{id}/moodboard/imagenes", idProyecto)
                        .file(archivo)
                        .principal(auth(usuario, idPerfil)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El moodboard no puede contener más de 10 imágenes."));
    }

    @Test
    void subirImagenes_perfilSinMembresia_devuelve403() throws Exception {
        Usuario dueno = guardarUsuario("mood.dueno@example.com", "77880003");
        Long idPerfilDueno = crearPerfil(dueno);
        Long idProyecto = crearProyecto(dueno.getIdUsuario(), idPerfilDueno, "Proyecto Mood Ajeno");

        Usuario ajeno = guardarUsuario("mood.ajeno@example.com", "77880004");
        Long idPerfilAjeno = crearPerfil(ajeno);

        MockMultipartFile archivo = jpeg("intruso.jpg");
        mockMvc.perform(multipart("/proyectos/{id}/moodboard/imagenes", idProyecto)
                        .file(archivo)
                        .principal(auth(ajeno, idPerfilAjeno)))
                .andExpect(status().isForbidden());
    }

    @AfterEach
    void limpiarArchivosSubidos() throws Exception {
        for (Path archivo : archivosCreados) {
            Files.deleteIfExists(archivo);
        }
        archivosCreados.clear();
        borrarSiEstaVacio(DIRECTORIO_SUBIDAS);
        borrarSiEstaVacio(DIRECTORIO_SUBIDAS.getParent());
    }

    private static void borrarSiEstaVacio(Path directorio) throws IOException {
        if (directorio != null && Files.isDirectory(directorio)
                && Files.list(directorio).findAny().isEmpty()) {
            Files.delete(directorio);
        }
    }

    private void sembrarMoodboardConImagenes(Long idProyecto, int cantidad) {
        var proyecto = proyectoRepository.findById(idProyecto).orElseThrow();
        Moodboard moodboard = Moodboard.builder().proyecto(proyecto).build();

        Set<Imagen> imagenes = new HashSet<>();
        for (int i = 0; i < cantidad; i++) {
            imagenes.add(imagenRepository.saveAndFlush(Imagen.builder()
                    .url("/uploads/perfiles/moodboard_seed_" + i + ".webp")
                    .nombreArchivo("moodboard_seed_" + i + ".webp")
                    .tipoImagen("image/webp")
                    .tamanoBytes(100)
                    .estado("Activa")
                    .build()));
        }
        moodboard.setImagenes(imagenes);
        moodboardRepository.saveAndFlush(moodboard);
    }

    private static UsernamePasswordAuthenticationToken auth(Usuario usuario, Long idPerfil) {
        var token = new UsernamePasswordAuthenticationToken(
                String.valueOf(usuario.getIdUsuario()), null, List.of());
        token.setDetails(new ContextoAutenticacion(usuario.getIdUsuario(), idPerfil, "Luna Mood"));
        return token;
    }

    private static String nombreDe(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private Usuario guardarUsuario(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Mood")
                .apellido("Board")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }

    private Long crearPerfil(Usuario usuario) {
        Long idProfesionModelo = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
        Long idPerfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest("Luna Mood", idProfesionModelo, "Modelo profesional.", List.of()))
                .idPerfil();
        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, id_tyc, descripcion, fecha_inicio) VALUES (?, 1, 'TyC Mood', NOW())",
                idPerfil);
        return idPerfil;
    }

    private Long crearProyecto(Long idUsuario, Long idPerfil, String nombre) {
        return crearProyectoService.crear(idUsuario, idPerfil, new CrearProyectoRequest(
                nombre,
                "Descripción de test",
                Privacidad.Publico,
                LocalDate.now().plusDays(30),
                true,
                null,
                null,
                null,
                null)).idProyecto();
    }

    private static MockMultipartFile jpeg(String nombre) throws Exception {
        BufferedImage imagen = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, 400, 300);
        g.dispose();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(imagen, "jpg", salida), "No hay escritor JPEG disponible");
        return new MockMultipartFile("archivos", nombre, MediaType.IMAGE_JPEG_VALUE, salida.toByteArray());
    }
}
