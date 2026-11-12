package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoEncontradaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPendienteException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPermitidaException;
import org.mgroko.backend.publicaciones.servicio.BuscarSolicitudColaboracionService;
import org.mgroko.backend.publicaciones.servicio.EliminarSolicitudColaboracionService;
import org.mgroko.backend.publicaciones.servicio.EnviarSolicitudColaboracionService;
import org.mgroko.backend.publicaciones.servicio.GestionarSolicitudColaboracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class EliminarSolicitudColaboracionIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private EliminarSolicitudColaboracionService eliminarSolicitudColaboracionService;

    @Autowired
    private EnviarSolicitudColaboracionService enviarSolicitudColaboracionService;

    @Autowired
    private BuscarSolicitudColaboracionService buscarSolicitudColaboracionService;

    @Autowired
    private GestionarSolicitudColaboracionService gestionarSolicitudColaboracionService;

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
    void eliminar_solicitudPendiente_borraFilaYNotificacionYDesapareceDeLaBusqueda() {
        Escenario escenario = crearEscenario("01");
        conectar(
                escenario.usuarioRemitente(),
                escenario.remitente(),
                escenario.usuarioDestinatario(),
                escenario.destinatario());
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.ACTIVA, "Editorial UC64");

        SolicitudColaboracionResponse creada = enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario()));
        em.flush();
        em.clear();

        eliminarSolicitudColaboracionService.eliminar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                creada.idSolicitudColaboracion());
        em.flush();
        em.clear();

        assertThat(em.find(
                SolicitudColaboracion.class, creada.idSolicitudColaboracion())).isNull();

        assertThat(notificacionesDe(
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                publicacion.getIdPublicacion())).isEmpty();

        assertThat(buscarSolicitudColaboracionService.buscar(
                escenario.idPerfilRemitente(), null, 0, 20).contenido()).isEmpty();
        assertThat(buscarSolicitudColaboracionService.buscar(
                escenario.idPerfilDestinatario(), null, 0, 20).contenido()).isEmpty();

        assertThatThrownBy(() -> gestionarSolicitudColaboracionService.rechazar(
                escenario.idUsuarioDestinatario(),
                escenario.idPerfilDestinatario(),
                creada.idSolicitudColaboracion()))
                .isInstanceOf(SolicitudColaboracionNoEncontradaException.class);

        SolicitudColaboracionResponse nueva = enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario()));

        assertThat(nueva.idSolicitudColaboracion())
                .isNotEqualTo(creada.idSolicitudColaboracion());
        assertThat(notificacionesDe(
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                publicacion.getIdPublicacion())).hasSize(1);
    }

    @Test
    void eliminar_solicitudDeUnDestinatario_noBorraNotificacionDeOtro() {
        Escenario escenario = crearEscenario("02");
        conectar(
                escenario.usuarioRemitente(),
                escenario.remitente(),
                escenario.usuarioDestinatario(),
                escenario.destinatario());
        Usuario usuarioOtro = crearUsuario("023", "otro");
        Perfil otro = crearPerfil(usuarioOtro, "023", "Otro UC64");
        conectar(escenario.usuarioRemitente(), escenario.remitente(), usuarioOtro, otro);
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.ACTIVA, "Editorial UC64 dos destinatarios");

        SolicitudColaboracionResponse creada = enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario()));
        enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(otro.getIdPerfil()));
        em.flush();
        em.clear();

        eliminarSolicitudColaboracionService.eliminar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                creada.idSolicitudColaboracion());
        em.flush();
        em.clear();

        assertThat(notificacionesDe(
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                publicacion.getIdPublicacion(),
                escenario.idUsuarioDestinatario(),
                escenario.idPerfilDestinatario())).isEmpty();
        assertThat(notificacionesDe(
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                publicacion.getIdPublicacion(),
                usuarioOtro.getIdUsuario(),
                otro.getIdPerfil())).hasSize(1);
    }

    @Test
    void eliminar_publicacionOculta_eliminaIgual() {
        Escenario escenario = crearEscenario("03");
        conectar(
                escenario.usuarioRemitente(),
                escenario.remitente(),
                escenario.usuarioDestinatario(),
                escenario.destinatario());
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.ACTIVA, "Editorial UC64 oculta");

        SolicitudColaboracionResponse creada = enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario()));
        publicacion.setEstado(EstadoPublicacion.OCULTA);
        em.flush();
        em.clear();

        eliminarSolicitudColaboracionService.eliminar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                creada.idSolicitudColaboracion());
        em.flush();
        em.clear();

        assertThat(em.find(
                SolicitudColaboracion.class, creada.idSolicitudColaboracion())).isNull();
    }

    @Test
    void eliminar_porDestinatario_lanzaNoPermitidaYMantieneLaSolicitud() {
        Escenario escenario = crearEscenario("04");
        conectar(
                escenario.usuarioRemitente(),
                escenario.remitente(),
                escenario.usuarioDestinatario(),
                escenario.destinatario());
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.ACTIVA, "Editorial UC64 ajena");

        SolicitudColaboracionResponse creada = enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario()));
        em.flush();
        em.clear();

        assertThatThrownBy(() -> eliminarSolicitudColaboracionService.eliminar(
                escenario.idUsuarioDestinatario(),
                escenario.idPerfilDestinatario(),
                creada.idSolicitudColaboracion()))
                .isInstanceOf(SolicitudColaboracionNoPermitidaException.class);

        em.flush();
        em.clear();

        assertThat(em.find(
                SolicitudColaboracion.class, creada.idSolicitudColaboracion())).isNotNull();
        assertThat(notificacionesDe(
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                publicacion.getIdPublicacion())).hasSize(1);
    }

    @Test
    void eliminar_solicitudAceptada_lanzaNoPendiente() {
        Escenario escenario = crearEscenario("05");
        conectar(
                escenario.usuarioRemitente(),
                escenario.remitente(),
                escenario.usuarioDestinatario(),
                escenario.destinatario());
        Publicacion publicacion = crearPublicacion(
                escenario.remitente(), EstadoPublicacion.ACTIVA, "Editorial UC64 aceptada");

        SolicitudColaboracionResponse creada = enviarSolicitudColaboracionService.enviar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                publicacion.getIdPublicacion(),
                new EnviarSolicitudColaboracionRequest(escenario.idPerfilDestinatario()));
        em.flush();

        SolicitudColaboracion solicitud = em.find(
                SolicitudColaboracion.class, creada.idSolicitudColaboracion());
        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());
        em.flush();

        assertThatThrownBy(() -> eliminarSolicitudColaboracionService.eliminar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                creada.idSolicitudColaboracion()))
                .isInstanceOf(SolicitudColaboracionNoPendienteException.class);
    }

    @Test
    void eliminar_solicitudInexistente_lanzaNoEncontrada() {
        Escenario escenario = crearEscenario("06");

        assertThatThrownBy(() -> eliminarSolicitudColaboracionService.eliminar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                999999L))
                .isInstanceOf(SolicitudColaboracionNoEncontradaException.class);
    }

    private List<Notificacion> notificacionesDe(String tipo, Long idPublicacion) {
        return em.createQuery("""
                SELECT n FROM Notificacion n
                WHERE n.tipo = :tipo
                  AND n.entidadTipo = :entidadTipo
                  AND n.entidadId = :entidadId
                """, Notificacion.class)
                .setParameter("tipo", tipo)
                .setParameter("entidadTipo", NotificacionService.ENTIDAD_PUBLICACION)
                .setParameter("entidadId", idPublicacion)
                .getResultList();
    }

    private List<Notificacion> notificacionesDe(
            String tipo,
            Long idPublicacion,
            Long idUsuarioDestino,
            Long idPerfilContexto) {

        return em.createQuery("""
                SELECT n FROM Notificacion n
                WHERE n.tipo = :tipo
                  AND n.entidadTipo = :entidadTipo
                  AND n.entidadId = :entidadId
                  AND n.usuarioDestino.idUsuario = :idUsuarioDestino
                  AND n.perfilContexto.idPerfil = :idPerfilContexto
                """, Notificacion.class)
                .setParameter("tipo", tipo)
                .setParameter("entidadTipo", NotificacionService.ENTIDAD_PUBLICACION)
                .setParameter("entidadId", idPublicacion)
                .setParameter("idUsuarioDestino", idUsuarioDestino)
                .setParameter("idPerfilContexto", idPerfilContexto)
                .getResultList();
    }

    private Escenario crearEscenario(String sufijo) {
        Usuario usuarioRemitente = crearUsuario(sufijo + "1", "remitente");
        Usuario usuarioDestinatario = crearUsuario(sufijo + "2", "destinatario");

        Perfil remitente = crearPerfil(
                usuarioRemitente, sufijo + "1", "Luna UC64 " + sufijo);
        Perfil destinatario = crearPerfil(
                usuarioDestinatario, sufijo + "2", "Sol UC64 " + sufijo);

        return new Escenario(
                usuarioRemitente,
                usuarioDestinatario,
                remitente,
                destinatario);
    }

    private Usuario crearUsuario(String sufijo, String nombre) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre(nombre)
                .apellido("UC64")
                .dni("75" + sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("uc64." + sufijo + "." + nombre + "@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }

    private Perfil crearPerfil(Usuario usuario, String sufijo, String nombreArtistico) {
        Profesion profesion = Profesion.builder()
                .codigo("PROF_UC64_" + sufijo)
                .nombre("Profesión UC64 " + sufijo)
                .build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil UC64")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        em.flush();
        return perfil;
    }

    private Publicacion crearPublicacion(
            Perfil perfil, EstadoPublicacion estado, String titulo) {
        Publicacion publicacion = Publicacion.builder()
                .titulo(titulo)
                .descripcion("Descripción UC64")
                .estado(estado)
                .perfil(perfil)
                .build();
        em.persist(publicacion);
        em.flush();
        return publicacion;
    }

    private void conectar(
            Usuario usuarioOrigen,
            Perfil perfilOrigen,
            Usuario usuarioDestino,
            Perfil perfilDestino) {

        var solicitud = enviarSolicitudConexionService.enviar(
                usuarioOrigen.getIdUsuario(),
                perfilOrigen.getIdPerfil(),
                new EnviarSolicitudConexionRequest(perfilDestino.getIdPerfil()));

        gestionarSolicitudConexionService.aceptar(
                usuarioDestino.getIdUsuario(),
                perfilDestino.getIdPerfil(),
                solicitud.idSolicitudConexion());

        em.flush();
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
