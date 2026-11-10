package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.conexiones.dto.EnviarSolicitudConexionRequest;
import org.mgroko.backend.conexiones.dto.SolicitudConexionGestionadaResponse;
import org.mgroko.backend.conexiones.dto.SolicitudConexionResponse;
import org.mgroko.backend.conexiones.exception.ConexionYaExistenteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPendienteException;
import org.mgroko.backend.conexiones.servicio.EnviarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.GestionarSolicitudConexionService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * UC-45: dos aceptaciones simultáneas de la misma solicitud no deben crear
 * dos conexiones ni duplicar notificaciones.
 */
@SpringBootTest
class GestionarSolicitudConexionConcurrencyIntegrationTest
        extends AbstractPostgresIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolGlobalRepository rolGlobalRepository;
    @Autowired
    private GeneroRepository generoRepository;
    @Autowired
    private ProfesionRepository profesionRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private EnviarSolicitudConexionService enviarSolicitudConexionService;
    @Autowired
    private GestionarSolicitudConexionService gestionarSolicitudConexionService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PerfilCreado perfilA;
    private PerfilCreado perfilB;
    private Long idSolicitud;

    @BeforeEach
    void setUp() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        perfilA = crearPerfil("A", rol, genero);
        perfilB = crearPerfil("B", rol, genero);

        SolicitudConexionResponse solicitud = enviarSolicitudConexionService.enviar(
                perfilA.idUsuario(),
                perfilA.idPerfil(),
                new EnviarSolicitudConexionRequest(perfilB.idPerfil()));
        idSolicitud = solicitud.idSolicitudConexion();
    }

    @AfterEach
    void tearDown() {
        if (idSolicitud == null || perfilA == null || perfilB == null) {
            return;
        }

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("SET session_replication_role = replica");
                try {
                    statement.executeUpdate(
                            "DELETE FROM notificacion WHERE id_usuario_destino IN ("
                                    + perfilA.idUsuario() + "," + perfilB.idUsuario() + ")");
                    statement.executeUpdate(
                            "DELETE FROM detalle_auditoria WHERE id_auditoria IN ("
                                    + "SELECT id_auditoria FROM auditoria "
                                    + "WHERE entidad_afectada = 'conexion' AND id_afectado IN ("
                                    + "SELECT id_conexion FROM conexion "
                                    + "WHERE id_solicitud_conexion = " + idSolicitud + "))");
                    statement.executeUpdate(
                            "DELETE FROM auditoria WHERE entidad_afectada = 'conexion' "
                                    + "AND id_afectado IN (SELECT id_conexion FROM conexion "
                                    + "WHERE id_solicitud_conexion = " + idSolicitud + ")");
                    statement.executeUpdate(
                            "DELETE FROM conexion WHERE id_solicitud_conexion = " + idSolicitud);
                    statement.executeUpdate(
                            "DELETE FROM solicitud_conexion WHERE id_solicitud_conexion = " + idSolicitud);
                    statement.executeUpdate(
                            "DELETE FROM perfil WHERE id_perfil IN ("
                                    + perfilA.idPerfil() + "," + perfilB.idPerfil() + ")");
                    statement.executeUpdate(
                            "DELETE FROM profesion WHERE id_profesion IN ("
                                    + perfilA.idProfesion() + "," + perfilB.idProfesion() + ")");
                    statement.executeUpdate(
                            "DELETE FROM usuario WHERE id_usuario IN ("
                                    + perfilA.idUsuario() + "," + perfilB.idUsuario() + ")");
                } finally {
                    statement.execute("SET session_replication_role = origin");
                }
            }
            return null;
        });
    }

    @Test
    void dosAceptacionesSimultaneas_soloUnaCreaConexion() throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> primera = pool.submit(() -> aceptarCon(inicio));
            Future<Object> segunda = pool.submit(() -> aceptarCon(inicio));

            inicio.countDown();

            Object resultado1 = primera.get(60, TimeUnit.SECONDS);
            Object resultado2 = segunda.get(60, TimeUnit.SECONDS);

            long exitos = List.of(resultado1, resultado2).stream()
                    .filter(resultado -> resultado instanceof SolicitudConexionGestionadaResponse)
                    .count();
            long conflictos = List.of(resultado1, resultado2).stream()
                    .filter(resultado -> resultado instanceof SolicitudConexionNoPendienteException
                            || resultado instanceof ConexionYaExistenteException)
                    .count();

            assertThat(exitos).isEqualTo(1);
            assertThat(conflictos).isEqualTo(1);

            Long conexiones = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM conexion
                    WHERE id_solicitud_conexion = ? AND estado = 'ACTIVA'
                    """, Long.class, idSolicitud);
            assertThat(conexiones).isEqualTo(1L);

            String estado = jdbcTemplate.queryForObject("""
                    SELECT estado FROM solicitud_conexion
                    WHERE id_solicitud_conexion = ?
                    """, String.class, idSolicitud);
            assertThat(estado).isEqualTo("ACEPTADA");

            Long notificaciones = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM notificacion
                    WHERE tipo = 'SOLICITUD_CONEXION_ACEPTADA'
                      AND entidad_tipo = 'CONEXION'
                    """, Long.class);
            assertThat(notificaciones).isEqualTo(2L);

            Long idConexion = jdbcTemplate.queryForObject("""
                    SELECT id_conexion FROM conexion
                    WHERE id_solicitud_conexion = ?
                    """, Long.class, idSolicitud);
            Long auditorias = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM auditoria
                    WHERE tipo_evento = 'CONEXION_CREADA'
                      AND id_afectado = ?
                    """, Long.class, idConexion);
            assertThat(auditorias).isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }

    private Object aceptarCon(CountDownLatch inicio) {
        try {
            inicio.await();
            return gestionarSolicitudConexionService.aceptar(
                    perfilB.idUsuario(), perfilB.idPerfil(), idSolicitud);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ex;
        } catch (RuntimeException ex) {
            return ex;
        }
    }

    private PerfilCreado crearPerfil(
            String etiqueta,
            RolGlobal rol,
            Genero genero) {

        int n = SECUENCIA.incrementAndGet();
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC45 " + etiqueta)
                .apellido("Concurrencia")
                .dni(String.format("%08d", 91_000_000 + n))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("uc45.conc." + etiqueta + n + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = profesionRepository.saveAndFlush(Profesion.builder()
                .codigo("UC45_CONC_" + etiqueta + n)
                .nombre("Profesion UC45 " + etiqueta + n)
                .build());

        Perfil perfil = perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico("Perfil UC45 " + etiqueta)
                .biografia("Perfil de concurrencia UC-45")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(profesion)
                .build());

        return new PerfilCreado(
                usuario.getIdUsuario(),
                perfil.getIdPerfil(),
                profesion.getIdProfesion());
    }

    private record PerfilCreado(Long idUsuario, Long idPerfil, Long idProfesion) {
    }
}
