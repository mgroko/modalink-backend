package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.publicaciones.dto.ComentarioResponse;
import org.mgroko.backend.publicaciones.dto.CrearComentarioRequest;
import org.mgroko.backend.publicaciones.dto.MeGustaResponse;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.ComentarioInvalidoException;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.servicio.CrearComentarioService;
import org.mgroko.backend.publicaciones.servicio.GestionarMeGustaPublicacionService;
import org.mgroko.backend.publicaciones.servicio.ListarComentariosService;
import org.mgroko.backend.publicaciones.servicio.ObtenerPublicacionDetalleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class InteractuarPublicacionIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GestionarMeGustaPublicacionService gestionarMeGustaService;

    @Autowired
    private CrearComentarioService crearComentarioService;

    @Autowired
    private ListarComentariosService listarComentariosService;

    @Autowired
    private ObtenerPublicacionDetalleService obtenerPublicacionDetalleService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void darMeGusta_creaRegistroNotificaYEsIdempotente() {
        Escenario escenario = crearEscenario("01");
        Publicacion publicacion = crearPublicacion(escenario.duenio(), "Editorial Otoño");
        agregarColaborador(publicacion, escenario.colaborador());
        em.flush();
        em.clear();

        MeGustaResponse primera = gestionarMeGustaService.dar(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion());

        assertThat(primera.meGusta()).isTrue();
        assertThat(primera.cantidadMeGusta()).isEqualTo(1);
        assertThat(contarMeGusta(publicacion.getIdPublicacion())).isEqualTo(1);
        assertThat(contarNotificaciones(
                NotificacionService.TIPO_PUBLICACION_MEGUSTA,
                publicacion.getIdPublicacion())).isEqualTo(2);

        MeGustaResponse segunda = gestionarMeGustaService.dar(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion());

        assertThat(segunda.cantidadMeGusta()).isEqualTo(1);
        assertThat(contarMeGusta(publicacion.getIdPublicacion())).isEqualTo(1);
        assertThat(contarNotificaciones(
                NotificacionService.TIPO_PUBLICACION_MEGUSTA,
                publicacion.getIdPublicacion())).isEqualTo(2);
    }

    @Test
    void quitarMeGusta_eliminaElRegistroSinNotificar() {
        Escenario escenario = crearEscenario("02");
        Publicacion publicacion = crearPublicacion(escenario.duenio(), "Editorial");
        em.flush();
        em.clear();

        gestionarMeGustaService.dar(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion());

        MeGustaResponse response = gestionarMeGustaService.quitar(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion());

        assertThat(response.meGusta()).isFalse();
        assertThat(response.cantidadMeGusta()).isZero();
        assertThat(contarMeGusta(publicacion.getIdPublicacion())).isZero();
        assertThat(contarNotificaciones(
                NotificacionService.TIPO_PUBLICACION_MEGUSTA,
                publicacion.getIdPublicacion())).isEqualTo(1);
    }

    @Test
    void comentar_registraTextoYNotificaAlDuenioYColaborador() {
        Escenario escenario = crearEscenario("03");
        Publicacion publicacion = crearPublicacion(escenario.duenio(), "Editorial");
        agregarColaborador(publicacion, escenario.colaborador());
        em.flush();
        em.clear();

        ComentarioResponse response = crearComentarioService.crear(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion(),
                new CrearComentarioRequest("  Muy buena publicación  "));

        assertThat(response.idComentario()).isNotNull();
        assertThat(response.texto()).isEqualTo("Muy buena publicación");
        assertThat(response.autor().idPerfil()).isEqualTo(escenario.idPerfilActor());
        assertThat(contarComentarios(publicacion.getIdPublicacion())).isEqualTo(1);
        assertThat(contarNotificaciones(
                NotificacionService.TIPO_PUBLICACION_COMENTARIO,
                publicacion.getIdPublicacion())).isEqualTo(2);

        PaginaResponse<ComentarioResponse> listado = listarComentariosService.listar(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion(),
                0,
                10);

        assertThat(listado.contenido()).singleElement()
                .satisfies(comentario -> assertThat(comentario.texto())
                        .isEqualTo("Muy buena publicación"));
    }

    @Test
    void detallePublicacion_exponeContadoresYEstadoDeMeGusta() {
        Escenario escenario = crearEscenario("06");
        Publicacion publicacion = crearPublicacion(escenario.duenio(), "Editorial");
        em.flush();
        em.clear();

        gestionarMeGustaService.dar(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion());
        crearComentarioService.crear(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion(),
                new CrearComentarioRequest("Comentario"));

        PublicacionResponse detalle = obtenerPublicacionDetalleService.obtener(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion());

        assertThat(detalle.cantidadMeGusta()).isEqualTo(1);
        assertThat(detalle.cantidadComentarios()).isEqualTo(1);
        assertThat(detalle.meGusta()).isTrue();
    }

    @Test
    void comentar_textoVacio_lanzaErrorYNoPersiste() {
        Escenario escenario = crearEscenario("04");
        Publicacion publicacion = crearPublicacion(escenario.duenio(), "Editorial");
        em.flush();
        em.clear();

        assertThatThrownBy(() -> crearComentarioService.crear(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion(),
                new CrearComentarioRequest("   ")))
                .isInstanceOf(ComentarioInvalidoException.class);

        assertThat(contarComentarios(publicacion.getIdPublicacion())).isZero();
        assertThat(contarNotificaciones(
                NotificacionService.TIPO_PUBLICACION_COMENTARIO,
                publicacion.getIdPublicacion())).isZero();
    }

    @Test
    void comentar_publicacionOculta_lanzaNoEncontrada() {
        Escenario escenario = crearEscenario("05");
        Publicacion publicacion = crearPublicacion(escenario.duenio(), "Editorial");
        publicacion.setEstado(EstadoPublicacion.OCULTA);
        em.flush();
        em.clear();

        assertThatThrownBy(() -> crearComentarioService.crear(
                escenario.idUsuarioActor(),
                escenario.idPerfilActor(),
                publicacion.getIdPublicacion(),
                new CrearComentarioRequest("Comentario")))
                .isInstanceOf(PublicacionNoEncontradaException.class);
    }

    private Escenario crearEscenario(String sufijo) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuarioDuenio = crearUsuario("uc66." + sufijo + ".duenio@example.com",
                "6610" + sufijo, "Luna", "Dueña", rol, genero);
        Usuario usuarioActor = crearUsuario("uc66." + sufijo + ".actor@example.com",
                "6620" + sufijo, "Sol", "Actriz", rol, genero);
        Usuario usuarioColaborador = crearUsuario("uc66." + sufijo + ".colab@example.com",
                "6630" + sufijo, "Eva", "Colaboradora", rol, genero);

        Perfil duenio = crearPerfil(usuarioDuenio, "Duenia UC66 " + sufijo, "PROF_UC66_D_" + sufijo);
        Perfil actor = crearPerfil(usuarioActor, "Actriz UC66 " + sufijo, "PROF_UC66_A_" + sufijo);
        Perfil colaborador = crearPerfil(
                usuarioColaborador, "Colaboradora UC66 " + sufijo, "PROF_UC66_C_" + sufijo);

        em.flush();

        return new Escenario(usuarioDuenio, usuarioActor, duenio, actor, colaborador);
    }

    private Usuario crearUsuario(
            String correo,
            String dni,
            String nombre,
            String apellido,
            RolGlobal rol,
            Genero genero) {

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }

    private Perfil crearPerfil(Usuario usuario, String nombreArtistico, String codigoProfesion) {
        Profesion profesion = Profesion.builder()
                .codigo(codigoProfesion)
                .nombre("Profesión " + codigoProfesion)
                .build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil UC66")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private Publicacion crearPublicacion(Perfil perfil, String titulo) {
        Publicacion publicacion = Publicacion.builder()
                .titulo(titulo)
                .descripcion("Descripción de la publicación")
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .build();
        em.persist(publicacion);
        return publicacion;
    }

    private void agregarColaborador(Publicacion publicacion, Perfil colaborador) {
        em.createNativeQuery("""
                INSERT INTO colaborador_publicacion (id_publicacion, id_perfil)
                VALUES (:idPublicacion, :idPerfil)
                """)
                .setParameter("idPublicacion", publicacion.getIdPublicacion())
                .setParameter("idPerfil", colaborador.getIdPerfil())
                .executeUpdate();
    }

    private Long contarMeGusta(Long idPublicacion) {
        return ((Number) em.createNativeQuery("""
                SELECT COUNT(*) FROM me_gusta
                WHERE id_publicacion = :idPublicacion
                """)
                .setParameter("idPublicacion", idPublicacion)
                .getSingleResult()).longValue();
    }

    private Long contarComentarios(Long idPublicacion) {
        return ((Number) em.createNativeQuery("""
                SELECT COUNT(*) FROM comentario
                WHERE id_publicacion = :idPublicacion
                """)
                .setParameter("idPublicacion", idPublicacion)
                .getSingleResult()).longValue();
    }

    private Long contarNotificaciones(String tipo, Long idPublicacion) {
        return em.createQuery("""
                SELECT COUNT(n)
                FROM Notificacion n
                WHERE n.tipo = :tipo
                  AND n.entidadTipo = :entidadTipo
                  AND n.entidadId = :entidadId
                """, Long.class)
                .setParameter("tipo", tipo)
                .setParameter("entidadTipo", NotificacionService.ENTIDAD_PUBLICACION)
                .setParameter("entidadId", idPublicacion)
                .getSingleResult();
    }

    private record Escenario(
            Usuario duenioUsuario,
            Usuario actorUsuario,
            Perfil duenio,
            Perfil actor,
            Perfil colaborador) {

        Long idUsuarioActor() {
            return actorUsuario.getIdUsuario();
        }

        Long idPerfilActor() {
            return actor.getIdPerfil();
        }
    }
}
