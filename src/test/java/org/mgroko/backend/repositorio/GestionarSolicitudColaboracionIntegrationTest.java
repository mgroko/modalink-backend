package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.publicaciones.dto.ProyectoPublicacionFiltroResponse;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionGestionadaResponse;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionInvalidaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPendienteException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPermitidaException;
import org.mgroko.backend.publicaciones.servicio.GestionarSolicitudColaboracionService;
import org.mgroko.backend.publicaciones.servicio.ListarProyectosPublicacionesPerfilService;
import org.mgroko.backend.publicaciones.servicio.ListarPublicacionesPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class GestionarSolicitudColaboracionIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GestionarSolicitudColaboracionService gestionarSolicitudColaboracionService;

    @Autowired
    private ListarPublicacionesPerfilService listarPublicacionesPerfilService;

    @Autowired
    private ListarProyectosPublicacionesPerfilService listarProyectosPublicacionesPerfilService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void aceptar_agregaColaboradorYNotificaAlEmisor() {
        Escenario escenario = crearEscenario("1");

        SolicitudColaboracionGestionadaResponse response =
                gestionarSolicitudColaboracionService.aceptar(
                        escenario.idUsuarioDestinatario(),
                        escenario.idPerfilDestinatario(),
                        escenario.idSolicitud());

        assertThat(response.estado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(response.fechaRespuesta()).isNotNull();
        assertThat(response.colaboradorAgregado()).isTrue();

        em.flush();
        em.clear();

        SolicitudColaboracion solicitud = em.find(
                SolicitudColaboracion.class, escenario.idSolicitud());
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(solicitud.getFechaRespuesta()).isNotNull();

        Publicacion publicacion = em.find(
                Publicacion.class, escenario.idPublicacion());
        assertThat(publicacion.getColaboradores())
                .extracting(Perfil::getIdPerfil)
                .containsExactly(escenario.idPerfilDestinatario());

        List<Notificacion> notificaciones = notificaciones(
                NotificacionService.TIPO_SOLICITUD_COLABORACION_ACEPTADA,
                escenario.idPublicacion());
        assertThat(notificaciones).singleElement().satisfies(notificacion -> {
            assertThat(notificacion.getUsuarioDestino().getIdUsuario())
                    .isEqualTo(escenario.idUsuarioRemitente());
            assertThat(notificacion.getPerfilContexto().getIdPerfil())
                    .isEqualTo(escenario.idPerfilRemitente());
            assertThat(notificacion.getMensaje())
                    .contains("Sol")
                    .contains("Editorial Otoño");
        });

        PaginaResponse<PublicacionResponse> portfolio =
                listarPublicacionesPerfilService.listar(
                        escenario.idPerfilDestinatario(),
                        escenario.idUsuarioDestinatario(),
                        escenario.idPerfilDestinatario(),
                        null,
                        0,
                        20);
        assertThat(portfolio.contenido())
                .extracting(PublicacionResponse::idPublicacion)
                .contains(escenario.idPublicacion());
    }

    @Test
    void aceptar_incluyeProyectoEnElSelectorDelColaborador() {
        Escenario escenario = crearEscenario("8");
        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto colaborativo UC62")
                .descripcion("Proyecto de test UC62")
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .build();
        em.persist(proyecto);

        Publicacion publicacion = em.find(
                Publicacion.class, escenario.idPublicacion());
        publicacion.setProyecto(proyecto);
        em.flush();

        gestionarSolicitudColaboracionService.aceptar(
                escenario.idUsuarioDestinatario(),
                escenario.idPerfilDestinatario(),
                escenario.idSolicitud());

        List<ProyectoPublicacionFiltroResponse> proyectos =
                listarProyectosPublicacionesPerfilService.listar(
                        escenario.idPerfilDestinatario(),
                        escenario.idUsuarioDestinatario(),
                        escenario.idPerfilDestinatario());

        assertThat(proyectos)
                .extracting(ProyectoPublicacionFiltroResponse::idProyecto)
                .contains(proyecto.getIdProyecto());
    }

    @Test
    void rechazar_actualizaEstadoYNotificaSinAgregarColaborador() {
        Escenario escenario = crearEscenario("2");

        SolicitudColaboracionGestionadaResponse response =
                gestionarSolicitudColaboracionService.rechazar(
                        escenario.idUsuarioDestinatario(),
                        escenario.idPerfilDestinatario(),
                        escenario.idSolicitud());

        assertThat(response.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(response.fechaRespuesta()).isNotNull();
        assertThat(response.colaboradorAgregado()).isFalse();

        em.flush();
        em.clear();

        SolicitudColaboracion solicitud = em.find(
                SolicitudColaboracion.class, escenario.idSolicitud());
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(solicitud.getFechaRespuesta()).isNotNull();

        Publicacion publicacion = em.find(
                Publicacion.class, escenario.idPublicacion());
        assertThat(publicacion.getColaboradores()).isEmpty();

        assertThat(notificaciones(
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECHAZADA,
                escenario.idPublicacion())).singleElement();
    }

    @Test
    void aceptar_soloPermiteAlDestinatario() {
        Escenario escenario = crearEscenario("3");

        assertThatThrownBy(() -> gestionarSolicitudColaboracionService.aceptar(
                escenario.idUsuarioRemitente(),
                escenario.idPerfilRemitente(),
                escenario.idSolicitud()))
                .isInstanceOf(SolicitudColaboracionNoPermitidaException.class);
    }

    @Test
    void aceptar_solicitudNoPendiente_lanzaNoPendiente() {
        Escenario escenario = crearEscenario("4");
        SolicitudColaboracion solicitud = em.find(
                SolicitudColaboracion.class, escenario.idSolicitud());
        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(LocalDateTime.now());
        em.flush();

        assertThatThrownBy(() -> gestionarSolicitudColaboracionService.aceptar(
                escenario.idUsuarioDestinatario(),
                escenario.idPerfilDestinatario(),
                escenario.idSolicitud()))
                .isInstanceOf(SolicitudColaboracionNoPendienteException.class);
    }

    @Test
    void aceptar_publicacionOculta_lanzaNoDisponible() {
        Escenario escenario = crearEscenario("5");
        Publicacion publicacion = em.find(
                Publicacion.class, escenario.idPublicacion());
        publicacion.setEstado(EstadoPublicacion.OCULTA);
        em.flush();

        assertThatThrownBy(() -> gestionarSolicitudColaboracionService.aceptar(
                escenario.idUsuarioDestinatario(),
                escenario.idPerfilDestinatario(),
                escenario.idSolicitud()))
                .isInstanceOfSatisfying(SolicitudColaboracionInvalidaException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("PUBLICACION_NO_DISPONIBLE"));
    }

    @Test
    void aceptar_yaEsColaborador_lanzaConflicto() {
        Escenario escenario = crearEscenario("6");
        em.createNativeQuery("""
                INSERT INTO colaborador_publicacion (id_publicacion, id_perfil)
                VALUES (:idPublicacion, :idPerfil)
                """)
                .setParameter("idPublicacion", escenario.idPublicacion())
                .setParameter("idPerfil", escenario.idPerfilDestinatario())
                .executeUpdate();
        em.flush();

        assertThatThrownBy(() -> gestionarSolicitudColaboracionService.aceptar(
                escenario.idUsuarioDestinatario(),
                escenario.idPerfilDestinatario(),
                escenario.idSolicitud()))
                .isInstanceOfSatisfying(SolicitudColaboracionInvalidaException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("DESTINATARIO_YA_ES_COLABORADOR"));
    }

    @Test
    void rechazar_publicacionOculta_permiteCerrarLaSolicitud() {
        Escenario escenario = crearEscenario("7");
        Publicacion publicacion = em.find(
                Publicacion.class, escenario.idPublicacion());
        publicacion.setEstado(EstadoPublicacion.OCULTA);
        em.flush();

        SolicitudColaboracionGestionadaResponse response =
                gestionarSolicitudColaboracionService.rechazar(
                        escenario.idUsuarioDestinatario(),
                        escenario.idPerfilDestinatario(),
                        escenario.idSolicitud());

        assertThat(response.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(publicacion.getColaboradores()).isEmpty();
    }

    private List<Notificacion> notificaciones(String tipo, Long idPublicacion) {
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

    private Escenario crearEscenario(String sufijo) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuarioRemitente = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Luna")
                .apellido("Remitente")
                .dni("6300" + sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("uc62." + sufijo + ".remitente@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Usuario usuarioDestinatario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Sol")
                .apellido("Destinatario")
                .dni("6400" + sufijo)
                .fechaNacimiento(LocalDate.now().minusYears(24))
                .correo("uc62." + sufijo + ".destinatario@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesionRemitente = Profesion.builder()
                .codigo("PROF_UC62_R_" + sufijo)
                .nombre("Diseñadora UC62 " + sufijo)
                .build();
        em.persist(profesionRemitente);

        Profesion profesionDestinatario = Profesion.builder()
                .codigo("PROF_UC62_D_" + sufijo)
                .nombre("Fotógrafa UC62 " + sufijo)
                .build();
        em.persist(profesionDestinatario);

        Perfil remitente = Perfil.builder()
                .nombreArtistico("Luna UC62")
                .biografia("Perfil remitente UC62")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioRemitente)
                .profesion(profesionRemitente)
                .build();
        em.persist(remitente);

        Perfil destinatario = Perfil.builder()
                .nombreArtistico("Sol")
                .biografia("Perfil destinatario UC62")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioDestinatario)
                .profesion(profesionDestinatario)
                .build();
        em.persist(destinatario);

        Publicacion publicacion = Publicacion.builder()
                .titulo("Editorial Otoño")
                .descripcion("Descripción")
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(remitente)
                .build();
        em.persist(publicacion);

        Imagen portada = Imagen.builder()
                .nombreArchivo("uc62_" + sufijo + ".webp")
                .url("/uploads/publicaciones/uc62_" + sufijo + ".webp")
                .tipoImagen("image/webp")
                .tamanoBytes(100)
                .build();
        em.persist(portada);
        publicacion.getImagenes().add(portada);

        SolicitudColaboracion solicitud = SolicitudColaboracion.builder()
                .publicacion(publicacion)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.now())
                .build();
        em.persist(solicitud);
        em.flush();

        return new Escenario(
                usuarioRemitente,
                usuarioDestinatario,
                remitente,
                destinatario,
                publicacion,
                solicitud);
    }

    private record Escenario(
            Usuario usuarioRemitente,
            Usuario usuarioDestinatario,
            Perfil remitente,
            Perfil destinatario,
            Publicacion publicacion,
            SolicitudColaboracion solicitud) {

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

        Long idPublicacion() {
            return publicacion.getIdPublicacion();
        }

        Long idSolicitud() {
            return solicitud.getIdSolicitudColaboracion();
        }
    }
}
