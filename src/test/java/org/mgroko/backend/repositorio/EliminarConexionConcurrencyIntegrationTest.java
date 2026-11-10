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
import org.mgroko.backend.conexiones.exception.ConexionNoActivaException;
import org.mgroko.backend.conexiones.servicio.EliminarConexionService;
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
 * UC-44: dos eliminaciones simultáneas de la misma conexión. Solo una debe
 * aplicar la baja lógica y registrar auditoría.
 */
@SpringBootTest
class EliminarConexionConcurrencyIntegrationTest
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
    private EliminarConexionService eliminarConexionService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PerfilCreado perfilA;
    private PerfilCreado perfilB;
    private Long idSolicitud;
    private Long idConexion;

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

        SolicitudConexionGestionadaResponse aceptada =
                gestionarSolicitudConexionService.aceptar(
                        perfilB.idUsuario(),
                        perfilB.idPerfil(),
                        idSolicitud);
        idConexion = aceptada.idConexion();
    }

    @AfterEach
    void tearDown() {
        if (idConexion == null || perfilA == null || perfilB == null) {
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
                                    + "WHERE entidad_afectada = 'conexion' AND id_afectado = "
                                    + idConexion + ")");
                    statement.executeUpdate(
                            "DELETE FROM auditoria WHERE entidad_afectada = 'conexion' "
                                    + "AND id_afectado = " + idConexion);
                    statement.executeUpdate(
                            "DELETE FROM conexion WHERE id_conexion = " + idConexion);
                    statement.executeUpdate(
                            "DELETE FROM solicitud_conexion WHERE id_solicitud_conexion = "
                                    + idSolicitud);
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
    void dosEliminacionesSimultaneas_soloUnaAplicaLaBaja() throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> primera = pool.submit(() -> eliminarCon(inicio));
            Future<Object> segunda = pool.submit(() -> eliminarCon(inicio));

            inicio.countDown();

            Object resultado1 = primera.get(60, TimeUnit.SECONDS);
            Object resultado2 = segunda.get(60, TimeUnit.SECONDS);

            long exitos = List.of(resultado1, resultado2).stream()
                    .filter(resultado -> "OK".equals(resultado))
                    .count();
            long conflictos = List.of(resultado1, resultado2).stream()
                    .filter(resultado -> resultado instanceof ConexionNoActivaException)
                    .count();

            assertThat(exitos).isEqualTo(1);
            assertThat(conflictos).isEqualTo(1);

            String estado = jdbcTemplate.queryForObject("""
                    SELECT estado FROM conexion WHERE id_conexion = ?
                    """, String.class, idConexion);
            assertThat(estado).isEqualTo("ELIMINADA");

            Long auditorias = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM auditoria
                    WHERE tipo_evento = 'CONEXION_ELIMINADA'
                      AND id_afectado = ?
                    """, Long.class, idConexion);
            assertThat(auditorias).isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }

    private Object eliminarCon(CountDownLatch inicio) {
        try {
            inicio.await();
            eliminarConexionService.eliminar(
                    perfilA.idUsuario(), perfilA.idPerfil(), idConexion);
            return "OK";
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
                .nombre("UC44 " + etiqueta)
                .apellido("Concurrencia")
                .dni(String.format("%08d", 93_000_000 + n))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("uc44.conc." + etiqueta + n + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = profesionRepository.saveAndFlush(Profesion.builder()
                .codigo("UC44_CONC_" + etiqueta + n)
                .nombre("Profesion UC44 " + etiqueta + n)
                .build());

        Perfil perfil = perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico("Perfil UC44 " + etiqueta)
                .biografia("Perfil de concurrencia UC-44")
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
