package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.conexiones.dto.EnviarSolicitudConexionRequest;
import org.mgroko.backend.conexiones.servicio.EnviarSolicitudConexionService;
import org.mgroko.backend.conexiones.servicio.GestionarSolicitudConexionService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.publicaciones.dto.EnviarSolicitudColaboracionRequest;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionDuplicadaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionInvalidaException;
import org.mgroko.backend.publicaciones.servicio.EnviarSolicitudColaboracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class EnviarSolicitudColaboracionIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private EnviarSolicitudColaboracionService enviarSolicitudColaboracionService;

    @Autowired
    private EnviarSolicitudConexionService enviarSolicitudConexionService;

    @Autowired
    private GestionarSolicitudConexionService gestionarSolicitudConexionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void enviar_creaSolicitudPendienteYNotificacion() {
        Escenario escenario = crearEscenario("1");
        conectar(escenario);
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.ACTIVA, "Editorial Otoño");
        em.flush();

        SolicitudColaboracionResponse response = enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario()));

        assertThat(response.idSolicitudColaboracion()).isNotNull();
        assertThat(response.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(response.idPublicacion()).isEqualTo(publicacion.getIdPublicacion());
        assertThat(response.tituloPublicacion()).isEqualTo("Editorial Otoño");
        assertThat(response.remitente().idPerfil()).isEqualTo(escenario.idPerfilRemitente());
        assertThat(response.destinatario().idPerfil()).isEqualTo(escenario.idPerfilDestinatario());

        em.flush();
        em.clear();

        SolicitudColaboracion guardada = em.find(
                SolicitudColaboracion.class, response.idSolicitudColaboracion());
        assertThat(guardada).isNotNull();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(guardada.getFechaEnvio()).isNotNull();
        assertThat(guardada.getPublicacion().getIdPublicacion())
                .isEqualTo(publicacion.getIdPublicacion());
        assertThat(guardada.getPerfilRemitente().getIdPerfil())
                .isEqualTo(escenario.idPerfilRemitente());
        assertThat(guardada.getPerfilDestinatario().getIdPerfil())
                .isEqualTo(escenario.idPerfilDestinatario());

        List<Notificacion> notificaciones = em.createQuery("""
                SELECT n FROM Notificacion n
                WHERE n.tipo = :tipo
                  AND n.entidadTipo = :entidadTipo
                  AND n.entidadId = :entidadId
                """, Notificacion.class)
                .setParameter("tipo", NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA)
                .setParameter("entidadTipo", NotificacionService.ENTIDAD_PUBLICACION)
                .setParameter("entidadId", publicacion.getIdPublicacion())
                .getResultList();

        assertThat(notificaciones).singleElement().satisfies(notificacion -> {
            assertThat(notificacion.getUsuarioDestino().getIdUsuario())
                    .isEqualTo(escenario.idUsuarioDestinatario());
            assertThat(notificacion.getPerfilContexto().getIdPerfil())
                    .isEqualTo(escenario.idPerfilDestinatario());
            assertThat(notificacion.getMensaje())
                    .contains("Luna UC61 1")
                    .contains("Editorial Otoño");
        });
    }

    @Test
    void enviar_solicitudPendienteDuplicada_lanzaDuplicada() {
        Escenario escenario = crearEscenario("2");
        conectar(escenario);
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.ACTIVA, "Editorial");
        em.flush();

        enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario()));

        assertThatThrownBy(() -> enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario())))
                .isInstanceOf(SolicitudColaboracionDuplicadaException.class);

        em.flush();
        Long cantidad = em.createQuery("""
                SELECT COUNT(s) FROM SolicitudColaboracion s
                WHERE s.publicacion.idPublicacion = :idPublicacion
                  AND s.perfilDestinatario.idPerfil = :idPerfil
                  AND s.estado = :estado
                """, Long.class)
                .setParameter("idPublicacion", publicacion.getIdPublicacion())
                .setParameter("idPerfil", escenario.idPerfilDestinatario())
                .setParameter("estado", EstadoSolicitud.PENDIENTE)
                .getSingleResult();
        assertThat(cantidad).isEqualTo(1);
    }

    @Test
    void enviar_trasRechazo_permiteNuevaSolicitudPendiente() {
        Escenario escenario = crearEscenario("3");
        conectar(escenario);
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.ACTIVA, "Editorial");
        em.flush();

        SolicitudColaboracionResponse primera = enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario()));

        SolicitudColaboracion rechazada = em.find(
                SolicitudColaboracion.class, primera.idSolicitudColaboracion());
        rechazada.setEstado(EstadoSolicitud.RECHAZADA);
        em.flush();

        SolicitudColaboracionResponse segunda = enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario()));

        assertThat(segunda.idSolicitudColaboracion())
                .isNotEqualTo(primera.idSolicitudColaboracion());
        assertThat(segunda.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void enviar_destinatarioFueraDeLaRed_lanzaInvalida() {
        Escenario escenario = crearEscenario("4");
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.ACTIVA, "Editorial");
        em.flush();

        assertThatThrownBy(() -> enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario())))
                .isInstanceOfSatisfying(SolicitudColaboracionInvalidaException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("DESTINATARIO_FUERA_DE_RED"));
    }

    @Test
    void enviar_destinatarioYaEsColaborador_lanzaInvalida() {
        Escenario escenario = crearEscenario("5");
        conectar(escenario);
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.ACTIVA, "Editorial");
        em.flush();

        em.createNativeQuery("""
                INSERT INTO colaborador_publicacion (id_publicacion, id_perfil)
                VALUES (:idPublicacion, :idPerfil)
                """)
                .setParameter("idPublicacion", publicacion.getIdPublicacion())
                .setParameter("idPerfil", escenario.idPerfilDestinatario())
                .executeUpdate();
        em.flush();

        assertThatThrownBy(() -> enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario())))
                .isInstanceOfSatisfying(SolicitudColaboracionInvalidaException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("DESTINATARIO_YA_ES_COLABORADOR"));
    }

    @Test
    void enviar_publicacionOculta_lanzaNoDisponible() {
        Escenario escenario = crearEscenario("6");
        conectar(escenario);
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.OCULTA, "Editorial");
        em.flush();

        assertThatThrownBy(() -> enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario())))
                .isInstanceOfSatisfying(SolicitudColaboracionInvalidaException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("PUBLICACION_NO_DISPONIBLE"));
    }

    @Test
    void enviar_publicacionDeOtroPerfil_lanzaNoEncontrada() {
        Escenario remitente = crearEscenario("7");
        Escenario otro = crearEscenario("8");
        Publicacion publicacionAjena = crearPublicacion(
                otro.remitente(), EstadoPublicacion.ACTIVA, "Ajena");
        em.flush();

        assertThatThrownBy(() -> enviarSolicitudColaboracionService.enviar(
                remitente.idUsuarioRemitente(),
                remitente.idPerfilRemitente(),
                publicacionAjena.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(remitente.idPerfilDestinatario())))
                .isInstanceOf(PublicacionNoEncontradaException.class);
    }

    private void conectar(Escenario escenario) {
        var solicitud = enviarSolicitudConexionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                new EnviarSolicitudConexionRequest(escenario.idPerfilDestinatario()));

        gestionarSolicitudConexionService.aceptar(
                escenario.idUsuarioDestinatario(),
                escenario.idPerfilDestinatario(),
                solicitud.idSolicitudConexion());

        em.flush();
    }

    private Escenario crearEscenario(String sufijo) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuarioRemitente = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Luna")
                .apellido("Remitente")
                .dni("6100" + sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("uc61." + sufijo + ".remitente@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Usuario usuarioDestinatario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Sol")
                .apellido("Destinatario")
                .dni("6200" + sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(24))
                .correo("uc61." + sufijo + ".destinatario@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesionRemitente = Profesion.builder()
                .codigo("PROF_UC61_R_" + sufijo)
                .nombre("Diseñadora UC61 " + sufijo)
                .build();
        em.persist(profesionRemitente);

        Profesion profesionDestinatario = Profesion.builder()
                .codigo("PROF_UC61_D_" + sufijo)
                .nombre("Fotógrafa UC61 " + sufijo)
                .build();
        em.persist(profesionDestinatario);

        Perfil remitente = Perfil.builder()
                .nombreArtistico("Luna UC61 " + sufijo)
                .biografia("Perfil remitente UC61")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioRemitente)
                .profesion(profesionRemitente)
                .build();
        em.persist(remitente);

        Perfil destinatario = Perfil.builder()
                .nombreArtistico("Sol UC61 " + sufijo)
                .biografia("Perfil destinatario UC61")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioDestinatario)
                .profesion(profesionDestinatario)
                .build();
        em.persist(destinatario);

        em.flush();

        return new Escenario(
                usuarioRemitente,
                usuarioDestinatario,
                remitente,
                destinatario);
    }

    private Publicacion crearPublicacion(
            Perfil perfil, EstadoPublicacion estado, String titulo) {
        Publicacion publicacion = Publicacion.builder()
                .titulo(titulo)
                .descripcion("Descripción de la publicación")
                .estado(estado)
                .perfil(perfil)
                .build();
        em.persist(publicacion);
        return publicacion;
    }

    private record Escenario(
            Usuario usuarioRemitente,
            Usuario usuarioDestinatario,
            Perfil remitente,
            Perfil destinatario) {

        Long idUsuarioRemitente() {
            return usuarioRemitente.getIdUsuario();
        }

        Long idUsuarioDestinatario() {
            return usuarioDestinatario.getIdUsuario();
        }

        Long idPerfilRemitente() {
            return remitente.getIdPerfil();
        }

        Long idPerfilDestinatario() {
            return destinatario.getIdPerfil();
        }
    }
}
