package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoEncontradaException;
import org.mgroko.backend.publicaciones.servicio.EliminarPublicacionService;
import org.mgroko.backend.publicaciones.servicio.GestionarSolicitudColaboracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * UC-20: la baja del dueño y la aceptación de una colaboración deben competir
 * sobre la publicación sin dejar filas huérfanas ni responder 500.
 */
@SpringBootTest
class EliminarPublicacionConcurrencyIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private EliminarPublicacionService eliminarPublicacionService;

    @Autowired
    private GestionarSolicitudColaboracionService gestionarSolicitudColaboracionService;

    @Autowired
    private PublicacionRepository publicacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long idUsuarioDuenio;
    private Long idPerfilDuenio;
    private Long idUsuarioDestinatario;
    private Long idPerfilDestinatario;
    private Long idPublicacion;
    private Long idSolicitud;
    private Long idImagen;
    private Long idProfesionDuenio;
    private Long idProfesionDestinatario;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            String marca = String.valueOf(System.nanoTime());
            RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
            Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

            Usuario usuarioDuenio = usuarioRepository.saveAndFlush(Usuario.builder()
                    .nombre("UC20")
                    .apellido("Duenio")
                    .dni("79" + marca.substring(marca.length() - 8))
                    .fechaNacimiento(LocalDate.now().minusYears(25))
                    .correo("uc20.concurrencia.duenio." + marca + "@example.com")
                    .rolGlobal(rol)
                    .genero(genero)
                    .build());

            Usuario usuarioDestinatario = usuarioRepository.saveAndFlush(Usuario.builder()
                    .nombre("UC20")
                    .apellido("Destinatario")
                    .dni("78" + marca.substring(marca.length() - 8))
                    .fechaNacimiento(LocalDate.now().minusYears(24))
                    .correo("uc20.concurrencia.destinatario." + marca + "@example.com")
                    .rolGlobal(rol)
                    .genero(genero)
                    .build());

            String marcaCorta = marca.substring(marca.length() - 8);
            Profesion profesionDuenio = Profesion.builder()
                    .codigo("PROF_UC20_CONC_D_" + marca)
                    .nombre("Prof UC20 D " + marcaCorta)
                    .build();
            Profesion profesionDestinatario = Profesion.builder()
                    .codigo("PROF_UC20_CONC_R_" + marca)
                    .nombre("Prof UC20 R " + marcaCorta)
                    .build();
            em.persist(profesionDuenio);
            em.persist(profesionDestinatario);

            Perfil duenio = Perfil.builder()
                    .nombreArtistico("Dueño concurrencia UC20")
                    .biografia("Perfil UC20")
                    .estado(EstadoPerfil.Activo)
                    .usuario(usuarioDuenio)
                    .profesion(profesionDuenio)
                    .build();
            Perfil destinatario = Perfil.builder()
                    .nombreArtistico("Destinatario concurrencia UC20")
                    .biografia("Perfil UC20")
                    .estado(EstadoPerfil.Activo)
                    .usuario(usuarioDestinatario)
                    .profesion(profesionDestinatario)
                    .build();
            em.persist(duenio);
            em.persist(destinatario);

            Publicacion publicacion = Publicacion.builder()
                    .titulo("Editorial concurrencia UC20")
                    .descripcion("Descripción UC20")
                    .fechaPublicacion(LocalDateTime.now())
                    .estado(EstadoPublicacion.ACTIVA)
                    .perfil(duenio)
                    .imagenes(new ArrayList<>())
                    .colaboradores(new LinkedHashSet<>())
                    .build();
            em.persist(publicacion);

            Imagen imagen = Imagen.builder()
                    .nombreArchivo("uc20_concurrencia_" + marca + ".webp")
                    .url("/uploads/publicaciones/uc20_concurrencia_" + marca + ".webp")
                    .tipoImagen("image/webp")
                    .tamanoBytes(100)
                    .build();
            em.persist(imagen);
            publicacion.getImagenes().add(imagen);

            em.flush();

            SolicitudColaboracion solicitud = SolicitudColaboracion.builder()
                    .publicacion(publicacion)
                    .perfilRemitente(duenio)
                    .perfilDestinatario(destinatario)
                    .estado(EstadoSolicitud.PENDIENTE)
                    .fechaEnvio(LocalDateTime.now())
                    .build();
            em.persist(solicitud);
            em.flush();

            idUsuarioDuenio = usuarioDuenio.getIdUsuario();
            idPerfilDuenio = duenio.getIdPerfil();
            idUsuarioDestinatario = usuarioDestinatario.getIdUsuario();
            idPerfilDestinatario = destinatario.getIdPerfil();
            idPublicacion = publicacion.getIdPublicacion();
            idSolicitud = solicitud.getIdSolicitudColaboracion();
            idImagen = imagen.getIdImagen();
            idProfesionDuenio = profesionDuenio.getIdProfesion();
            idProfesionDestinatario = profesionDestinatario.getIdProfesion();
        });
    }

    @AfterEach
    void tearDown() {
        if (idPublicacion == null) {
            return;
        }

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("SET session_replication_role = replica");
                try {
                    statement.executeUpdate(
                            "DELETE FROM notificacion WHERE entidad_tipo = 'PUBLICACION' "
                                    + "AND entidad_id = " + idPublicacion);
                    statement.executeUpdate(
                            "DELETE FROM notificacion WHERE id_usuario_destino IN ("
                                    + idUsuarioDuenio + "," + idUsuarioDestinatario + ")");
                    statement.executeUpdate(
                            "DELETE FROM solicitud_colaboracion WHERE id_publicacion = "
                                    + idPublicacion);
                    statement.executeUpdate(
                            "DELETE FROM colaborador_publicacion WHERE id_publicacion = "
                                    + idPublicacion);
                    statement.executeUpdate(
                            "DELETE FROM imagen_publicacion WHERE id_publicacion = "
                                    + idPublicacion);
                    statement.executeUpdate(
                            "DELETE FROM imagen WHERE id_imagen = " + idImagen);
                    statement.executeUpdate(
                            "DELETE FROM publicacion WHERE id_publicacion = " + idPublicacion);
                    statement.executeUpdate(
                            "DELETE FROM perfil WHERE id_perfil IN ("
                                    + idPerfilDuenio + "," + idPerfilDestinatario + ")");
                    statement.executeUpdate(
                            "DELETE FROM profesion WHERE id_profesion IN ("
                                    + idProfesionDuenio + "," + idProfesionDestinatario + ")");
                    statement.executeUpdate(
                            "DELETE FROM usuario WHERE id_usuario IN ("
                                    + idUsuarioDuenio + "," + idUsuarioDestinatario + ")");
                } finally {
                    statement.execute("SET session_replication_role = origin");
                }
            }
            return null;
        });
    }

    @Test
    void bajaYAceptacionSimultaneas_dejanEstadoConsistente() throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> baja = pool.submit(() -> ejecutar(inicio, () -> {
                eliminarPublicacionService.eliminar(
                        idUsuarioDuenio, idPerfilDuenio, idPublicacion);
                return "BAJA";
            }));

            Future<Object> aceptacion = pool.submit(() -> ejecutar(inicio, () -> {
                gestionarSolicitudColaboracionService.aceptar(
                        idUsuarioDestinatario, idPerfilDestinatario, idSolicitud);
                return "ACEPTADA";
            }));

            inicio.countDown();

            Object resultadoBaja = baja.get(60, TimeUnit.SECONDS);
            Object resultadoAceptacion = aceptacion.get(60, TimeUnit.SECONDS);

            assertThat(resultadoBaja).isEqualTo("BAJA");
            assertThat(resultadoAceptacion)
                    .isIn("ACEPTADA", "NO_ENCONTRADA");
        } finally {
            pool.shutdownNow();
        }

        assertThat(publicacionRepository.findById(idPublicacion)).isEmpty();
        assertThat(contar("solicitud_colaboracion")).isZero();
        assertThat(contar("colaborador_publicacion")).isZero();
        assertThat(contar("imagen_publicacion")).isZero();
    }

    private Object ejecutar(CountDownLatch inicio, Callable<Object> accion)
            throws Exception {

        inicio.await();
        try {
            return accion.call();
        } catch (SolicitudColaboracionNoEncontradaException ex) {
            return "NO_ENCONTRADA";
        }
    }

    private long contar(String tabla) {
        Number cantidad = (Number) jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tabla + " WHERE id_publicacion = ?",
                Number.class,
                idPublicacion);
        return cantidad.longValue();
    }
}
