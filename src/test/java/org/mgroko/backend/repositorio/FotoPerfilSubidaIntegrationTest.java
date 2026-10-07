package org.mgroko.backend.repositorio;

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
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.storage.servicio.ProcesadorImagen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo real de subida de foto: POST /perfiles/{id}/foto con un JPEG debe dejar en
 * disco un archivo .webp que después se sirve por GET /uploads/... con
 * Content-Type: image/webp.
 *
 * Usa PostgreSQL real (Testcontainers) y el directorio de subida por defecto de la
 * aplicación (uploads/perfiles), que es el que publica {@code StorageWebConfig};
 * los archivos que crea cada test se borran al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Rollback
class FotoPerfilSubidaIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private ProfesionRepository profesionRepository;

    @Autowired
    private ImagenRepository imagenRepository;

    @Autowired
    private CrearPerfilService crearPerfilService;

    private final List<Path> archivosCreados = new ArrayList<>();

    @Test
    void subirJpeg_guardaUnWebpSeQueSirveComoImageWebp() throws Exception {
        Usuario usuario = guardarUsuario();
        Long idPerfil = crearPerfil(usuario);

        JsonNode respuesta = subirFoto(usuario, idPerfil, jpeg());

        String fotoUrl = respuesta.get("fotoUrl").asText();
        assertTrue(fotoUrl.matches("/uploads/perfiles/perfil_[0-9a-f\\-]{36}\\.webp"),
                "La URL debe apuntar a un .webp con nombre perfil_<UUID>: " + fotoUrl);
        assertTrue(respuesta.get("idImagen").isNumber());

        String nombreArchivo = fotoUrl.substring(fotoUrl.lastIndexOf('/') + 1);
        Path archivoEnDisco = DIRECTORIO_SUBIDAS.resolve(nombreArchivo);
        assertTrue(Files.exists(archivoEnDisco), "Debe existir el archivo convertido en disco");

        byte[] bytesEnDisco = Files.readAllBytes(archivoEnDisco);
        assertEquals("RIFF", new String(bytesEnDisco, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("WEBP", new String(bytesEnDisco, 8, 4, StandardCharsets.US_ASCII));

        Imagen imagen = imagenRepository.findById(respuesta.get("idImagen").asLong()).orElseThrow();
        assertEquals(ProcesadorImagen.MIME_SALIDA, imagen.getTipoImagen());
        assertEquals((int) Files.size(archivoEnDisco), imagen.getTamanoBytes(),
                "tamanoBytes debe reflejar el archivo ya convertido");

        mockMvc.perform(get(fotoUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType(ProcesadorImagen.MIME_SALIDA)));
    }

    // El reemplazo y el borrado de archivos se validan en FotoPerfilAtomicidadIntegrationTest:
    // desde la Fase 3 los borrados ocurren después del commit, fuera del @Transactional del test.

    @AfterEach
    void limpiarArchivosSubidos() throws Exception {
        for (Path archivo : archivosCreados) {
            Files.deleteIfExists(archivo);
        }
        archivosCreados.clear();
        // Solo se retiran los directorios si quedaron vacíos (no se borra nada de terceros)
        borrarSiEstaVacio(DIRECTORIO_SUBIDAS);
        borrarSiEstaVacio(DIRECTORIO_SUBIDAS.getParent());
    }

    private static void borrarSiEstaVacio(Path directorio) throws IOException {
        if (directorio != null && Files.isDirectory(directorio)
                && Files.list(directorio).findAny().isEmpty()) {
            Files.delete(directorio);
        }
    }

    private JsonNode subirFoto(Usuario usuario, Long idPerfil, byte[] jpeg) throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, jpeg);

        String cuerpo = mockMvc.perform(multipart("/perfiles/{idPerfil}/foto", idPerfil)
                        .file(archivo)
                        .principal(principal(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPerfil").value(idPerfil))
                .andExpect(jsonPath("$.idImagen").isNumber())
                .andReturn().getResponse().getContentAsString();

        JsonNode respuesta = objectMapper.readTree(cuerpo);
        archivosCreados.add(DIRECTORIO_SUBIDAS.resolve(nombreDe(respuesta.get("fotoUrl").asText())));
        return respuesta;
    }

    private static UsernamePasswordAuthenticationToken principal(Usuario usuario) {
        return new UsernamePasswordAuthenticationToken(
                String.valueOf(usuario.getIdUsuario()), null, List.of());
    }

    private static String nombreDe(String fotoUrl) {
        return fotoUrl.substring(fotoUrl.lastIndexOf('/') + 1);
    }

    private Usuario guardarUsuario() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Foto")
                .apellido("Perfil")
                .dni("55550099")
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("foto.perfil@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }

    private Long crearPerfil(Usuario usuario) {
        Long idProfesion = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
        return crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest("Luna", idProfesion, "Modelo profesional.", List.of())).idPerfil();
    }

    private static byte[] jpeg() throws Exception {
        BufferedImage imagen = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 1200, 800);
        g.dispose();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(imagen, "jpg", salida), "No hay escritor JPEG disponible");
        return salida.toByteArray();
    }
}
