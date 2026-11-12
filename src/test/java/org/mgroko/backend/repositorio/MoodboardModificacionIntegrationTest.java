package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.ModificarProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.ImagenMoodboardNoEncontradaException;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.ModificarProyectoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-36: modificación de las imágenes del moodboard contra PostgreSQL real.
 * Verifica que la reconciliación quite primero la asociación y después la fila
 * de imagen (sin violar la FK de imagen_moodboard).
 */
@SpringBootTest
@Transactional
class MoodboardModificacionIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    @Autowired
    private CrearPerfilService crearPerfilService;

    @Autowired
    private CrearProyectoService crearProyectoService;

    @Autowired
    private ModificarProyectoService modificarProyectoService;

    @Autowired
    private MoodboardRepository moodboardRepository;

    @Autowired
    private ImagenRepository imagenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record PerfilCreado(Long idUsuario, Long idPerfil) {}

    private PerfilCreado crearPerfil(String correo, String dni, String nombreArtistico) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Moodboard").apellido("Test").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(26))
                .correo(correo)
                .estado(org.mgroko.backend.modelo.enums.EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesion = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesion, "Perfil de prueba moodboard.", List.of()));

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private ProyectoResponse crearProyectoConMoodboard(PerfilCreado perfil, String nombre) {
        return crearProyectoService.crear(
                perfil.idUsuario(), perfil.idPerfil(),
                new CrearProyectoRequest(
                        nombre,
                        "Descripción del proyecto",
                        Privacidad.PUBLICO,
                        LocalDate.now().plusDays(30),
                        true,
                        null,
                        null,
                        null,
                        new CrearMoodboardRequest("Paleta inicial")));
    }

    private Imagen guardarImagen(String nombreArchivo) {
        return imagenRepository.saveAndFlush(Imagen.builder()
                .nombreArchivo(nombreArchivo)
                .url("/uploads/moodboard/" + nombreArchivo)
                .tipoImagen("image/webp")
                .tamanoBytes(100)
                .estado("Activa")
                .build());
    }

    private ModificarProyectoRequest requestMoodboard(String descripcion, List<Long> conservadas) {
        return new ModificarProyectoRequest(
                "Descripción actualizada",
                Privacidad.PUBLICO,
                true,
                LocalDate.now().plusDays(30),
                null,
                null,
                null,
                new CrearMoodboardRequest(descripcion, conservadas));
    }

    @Test
    void modificar_conservaIndicadasYEliminaElResto() {
        PerfilCreado perfil = crearPerfil("moodboard.mod1@example.com", "70000101", "Moodboard Uno");
        ProyectoResponse proyecto = crearProyectoConMoodboard(perfil, "Proyecto moodboard uno");

        Moodboard moodboard = moodboardRepository
                .findByProyectoIdProyecto(proyecto.idProyecto()).orElseThrow();
        Imagen conservada = guardarImagen("moodboard_conservada.webp");
        Imagen eliminada = guardarImagen("moodboard_eliminada.webp");
        moodboard.getImagenes().add(conservada);
        moodboard.getImagenes().add(eliminada);
        moodboardRepository.saveAndFlush(moodboard);

        modificarProyectoService.modificar(
                perfil.idUsuario(), perfil.idPerfil(), proyecto.idProyecto(),
                requestMoodboard("Paleta actualizada", List.of(conservada.getIdImagen())));

        Moodboard actualizado = moodboardRepository
                .findByProyectoIdProyecto(proyecto.idProyecto()).orElseThrow();
        assertThat(actualizado.getDescripcion()).isEqualTo("Paleta actualizada");
        assertThat(actualizado.getImagenes())
                .extracting(Imagen::getIdImagen)
                .containsExactly(conservada.getIdImagen());
        assertThat(contarImagenesPorId(eliminada.getIdImagen())).isZero();
        assertThat(contarAsociacionesDeImagen(eliminada.getIdImagen())).isZero();
        assertThat(contarImagenesPorId(conservada.getIdImagen())).isOne();
    }

    @Test
    void modificar_conservarImagenAjena_lanzaNoEncontrada() {
        PerfilCreado perfil = crearPerfil("moodboard.mod2@example.com", "70000102", "Moodboard Dos");
        ProyectoResponse proyecto = crearProyectoConMoodboard(perfil, "Proyecto moodboard dos");

        assertThatThrownBy(() -> modificarProyectoService.modificar(
                perfil.idUsuario(), perfil.idPerfil(), proyecto.idProyecto(),
                requestMoodboard("Paleta actualizada", List.of(999999L))))
                .isInstanceOf(ImagenMoodboardNoEncontradaException.class)
                .hasMessageContaining("999999");
    }

    private int contarImagenesPorId(Long idImagen) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM imagen WHERE id_imagen = ?", Integer.class, idImagen);
        return total != null ? total : 0;
    }

    private int contarAsociacionesDeImagen(Long idImagen) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM imagen_moodboard WHERE id_imagen = ?", Integer.class, idImagen);
        return total != null ? total : 0;
    }
}
