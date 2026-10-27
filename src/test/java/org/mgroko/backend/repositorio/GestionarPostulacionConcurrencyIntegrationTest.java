package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.postulaciones.dto.PostulacionGestionadaResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionResponse;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.postulaciones.servicio.AceptarPostulacionService;
import org.mgroko.backend.postulaciones.servicio.CrearPostulacionService;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.mgroko.backend.proyectos.servicio.PublicarProyectoService;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica que dos aceptaciones simultáneas de la misma postulación no creen
 * dos membresías. El lock del proyecto serializa las operaciones y la segunda
 * transacción debe encontrar la postulación ya gestionada.
 */
@SpringBootTest
class GestionarPostulacionConcurrencyIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private PublicarProyectoService publicarProyectoService;
    @Autowired
    private CrearPostulacionService crearPostulacionService;
    @Autowired
    private AceptarPostulacionService aceptarPostulacionService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long idUsuarioDirector;
    private Long idPerfilDirector;
    private Long idUsuarioPostulante;
    private Long idPerfilPostulante;
    private Long idProyecto;
    private Long idRequerimiento;
    private Long idPostulacion;

    @BeforeEach
    void setUp() {
        PerfilCreado director = crearPerfil(
                "uc34.conc.dir@example.com", "88834901", "Directora Concurrencia UC34");
        PerfilCreado postulante = crearPerfil(
                "uc34.conc.pos@example.com", "88834902", "Postulante Concurrencia UC34");

        idUsuarioDirector = director.idUsuario();
        idPerfilDirector = director.idPerfil();
        idUsuarioPostulante = postulante.idUsuario();
        idPerfilPostulante = postulante.idPerfil();

        ProyectoResponse borrador = crearProyectoService.crear(
                idUsuarioDirector, idPerfilDirector,
                new CrearProyectoRequest(
                        "Desfile UC34 Concurrencia",
                        "Proyecto de concurrencia UC34",
                        Privacidad.PUBLICO,
                        LocalDate.now().plusDays(30),
                        true,
                        new UbicacionRequest("0602801002"),
                        null,
                        List.of(new CrearRequerimientoGralRequest(
                                1, idProfesion("modelo"), "Una modelo", null, null)),
                        new CrearMoodboardRequest("Paleta neutra")));

        ProyectoResponse publicado = publicarProyectoService.publicar(
                idUsuarioDirector, idPerfilDirector, borrador.idProyecto());
        idProyecto = publicado.idProyecto();
        idRequerimiento = publicado.requerimientosGral().get(0).idRequerimientoGral();

        PostulacionResponse postulacion = crearPostulacionService.darDeAltaPostulacionGeneral(
                idUsuarioPostulante, idPerfilPostulante, idProyecto, idRequerimiento);
        idPostulacion = postulacion.idPostulacion();
    }

    @AfterEach
    void tearDown() {
        if (idProyecto == null) {
            return;
        }

        // El trigger de "al menos un director" impide borrar las membresías en
        // orden normal. En el entorno de test se desactivan los triggers de la
        // sesión solo para limpiar los datos creados por esta clase.
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("SET session_replication_role = replica");
                try {
                    if (idPostulacion != null) {
                        statement.executeUpdate(
                                "DELETE FROM notificacion WHERE entidad_tipo = 'POSTULACION_GRAL'"
                                        + " AND entidad_id = " + idPostulacion);
                    }
                    if (idRequerimiento != null) {
                        statement.executeUpdate(
                                "DELETE FROM postulacion_gral WHERE id_requerimiento_gral = "
                                        + idRequerimiento);
                    }
                    statement.executeUpdate(
                            "DELETE FROM miembros_proyecto WHERE id_proyecto = " + idProyecto);
                    statement.executeUpdate(
                            "DELETE FROM requerimiento_gral_proyecto WHERE id_proyecto = " + idProyecto);
                    statement.executeUpdate(
                            "DELETE FROM moodboard WHERE id_proyecto = " + idProyecto);
                    statement.executeUpdate(
                            "DELETE FROM planificacion WHERE id_proyecto = " + idProyecto);
                    statement.executeUpdate(
                            "DELETE FROM proyecto WHERE id_proyecto = " + idProyecto);
                } finally {
                    statement.execute("SET session_replication_role = origin");
                }
            }
            return null;
        });
    }

    @Test
    void dosAceptacionesSimultaneas_soloUnaGana() throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> primera = pool.submit(() -> aceptarCon(inicio));
            Future<Object> segunda = pool.submit(() -> aceptarCon(inicio));

            inicio.countDown();

            Object resultado1 = primera.get(60, TimeUnit.SECONDS);
            Object resultado2 = segunda.get(60, TimeUnit.SECONDS);

            long exitos = List.of(resultado1, resultado2).stream()
                    .filter(resultado -> resultado instanceof PostulacionGestionadaResponse)
                    .count();
            long conflictos = List.of(resultado1, resultado2).stream()
                    .filter(resultado -> resultado instanceof PostulacionYaGestionadaException
                            || resultado instanceof PostulacionNoPermitidaException)
                    .count();

            assertThat(exitos).isEqualTo(1);
            assertThat(conflictos).isEqualTo(1);

            Long miembros = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM miembros_proyecto
                    WHERE id_proyecto = ? AND id_perfil = ? AND estado_participacion = 'ACTIVO'
                    """, Long.class, idProyecto, idPerfilPostulante);
            assertThat(miembros).isEqualTo(1L);

            String estado = jdbcTemplate.queryForObject("""
                    SELECT estado FROM postulacion_gral WHERE id_postulacion_gral = ?
                    """, String.class, idPostulacion);
            assertThat(estado).isEqualTo("ACEPTADA");

            Long notificaciones = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM notificacion
                    WHERE id_usuario_destino = ? AND tipo = 'POSTULACION_ACEPTADA'
                      AND entidad_tipo = 'POSTULACION_GRAL' AND entidad_id = ?
                    """, Long.class, idUsuarioPostulante, idPostulacion);
            assertThat(notificaciones).isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }

    private Object aceptarCon(CountDownLatch inicio) {
        try {
            inicio.await();
            return aceptarPostulacionService.aceptarGeneral(
                    idUsuarioDirector, idPerfilDirector, idProyecto, idPostulacion);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ex;
        } catch (RuntimeException ex) {
            return ex;
        }
    }

    private PerfilCreado crearPerfil(String correo, String dni, String nombreArtistico) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC34").apellido("Concurrencia").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(26))
                .correo(correo)
                .estado(org.mgroko.backend.modelo.enums.EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Long idProfesion = idProfesion("modelo");
        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest(nombreArtistico, idProfesion,
                        "Perfil de prueba UC-34.", List.of()));

        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, id_tyc, descripcion, fecha_inicio) VALUES (?, 1, 'TyC UC-34', NOW())",
                perfil.idPerfil());

        return new PerfilCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private Long idProfesion(String codigo) {
        return profesionRepository.findByCodigoIgnoreCase(codigo)
                .map(Profesion::getIdProfesion)
                .orElseGet(() -> profesionRepository.buscar("%" + codigo + "%", codigo)
                        .get(0).getIdProfesion());
    }

    private record PerfilCreado(Long idUsuario, Long idPerfil) {}
}
