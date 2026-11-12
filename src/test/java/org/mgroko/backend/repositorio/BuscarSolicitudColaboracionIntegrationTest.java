package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
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
import org.mgroko.backend.publicaciones.dto.BuscarSolicitudesColaboracionFiltro;
import org.mgroko.backend.publicaciones.dto.DireccionSolicitudColaboracion;
import org.mgroko.backend.publicaciones.dto.PublicacionSolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionBusquedaResponse;
import org.mgroko.backend.publicaciones.exception.FiltroSolicitudColaboracionInvalidoException;
import org.mgroko.backend.publicaciones.servicio.BuscarSolicitudColaboracionService;
import org.mgroko.backend.publicaciones.servicio.ListarPublicacionesSolicitudColaboracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class BuscarSolicitudColaboracionIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private BuscarSolicitudColaboracionService buscarSolicitudColaboracionService;

    @Autowired
    private ListarPublicacionesSolicitudColaboracionService
            listarPublicacionesSolicitudColaboracionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    private Escenario escenario;
    private Long pubAId;
    private Long pubBId;
    private Long pubCId;
    private Long idR1;
    private Long idR2;
    private Long idR3;
    private Long idR4;
    private Long idR5;
    private Long idR6;
    private Long idR7;
    private Long idR8;

    @BeforeEach
    void setUp() {
        escenario = crearEscenario();

        Publicacion pubA = crearPublicacion(
                escenario.a(), EstadoPublicacion.ACTIVA, "Publicación A", "a.webp");
        Publicacion pubAOculta = crearPublicacion(
                escenario.a(), EstadoPublicacion.OCULTA, "Publicación A oculta", "a-oculta.webp");
        Publicacion pubB = crearPublicacion(
                escenario.b(), EstadoPublicacion.ACTIVA, "Publicación B", "b.webp");
        Publicacion pubC = crearPublicacion(
                escenario.c(), EstadoPublicacion.ACTIVA, "Publicación C", "c.webp");

        idR1 = crearSolicitud(
                pubA, escenario.a(), escenario.b(), EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 10, 10, 0)).getIdSolicitudColaboracion();
        idR2 = crearSolicitud(
                pubB, escenario.b(), escenario.a(), EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 10, 11, 0)).getIdSolicitudColaboracion();
        idR3 = crearSolicitud(
                pubA, escenario.a(), escenario.c(), EstadoSolicitud.ACEPTADA,
                LocalDateTime.of(2026, 11, 9, 10, 0)).getIdSolicitudColaboracion();
        idR4 = crearSolicitud(
                pubC, escenario.c(), escenario.a(), EstadoSolicitud.RECHAZADA,
                LocalDateTime.of(2026, 11, 8, 10, 0)).getIdSolicitudColaboracion();
        idR5 = crearSolicitud(
                pubA, escenario.a(), escenario.d(), EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 10, 12, 0)).getIdSolicitudColaboracion();
        idR6 = crearSolicitud(
                pubAOculta, escenario.a(), escenario.b(), EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 10, 13, 0)).getIdSolicitudColaboracion();
        idR7 = crearSolicitud(
                pubC, escenario.c(), escenario.d(), EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 10, 14, 0)).getIdSolicitudColaboracion();
        idR8 = crearSolicitud(
                pubA, escenario.a(), escenario.e(), EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 10, 9, 0)).getIdSolicitudColaboracion();

        em.flush();
        pubAId = pubA.getIdPublicacion();
        pubBId = pubB.getIdPublicacion();
        pubCId = pubC.getIdPublicacion();
        em.clear();
    }

    @Test
    void buscar_sinFiltros_devuelveSoloPendientesVisiblesDelPerfil() {
        PaginaResponse<SolicitudColaboracionBusquedaResponse> pagina = buscar(null, 0, 20);

        assertThat(pagina.totalElementos()).isEqualTo(3);
        assertThat(ids(pagina)).containsExactly(idR2, idR1, idR8);
        assertThat(pagina.contenido()).allSatisfy(item ->
                assertThat(item.estado()).isEqualTo(EstadoSolicitud.PENDIENTE));

        SolicitudColaboracionBusquedaResponse recibida = porId(pagina, idR2);
        assertThat(recibida.direccion()).isEqualTo(DireccionSolicitudColaboracion.RECIBIDA);
        assertThat(recibida.puedeAceptar()).isTrue();
        assertThat(recibida.puedeRechazar()).isTrue();
        assertThat(recibida.puedeEliminar()).isFalse();

        SolicitudColaboracionBusquedaResponse enviada = porId(pagina, idR1);
        assertThat(enviada.direccion()).isEqualTo(DireccionSolicitudColaboracion.ENVIADA);
        assertThat(enviada.puedeAceptar()).isFalse();
        assertThat(enviada.puedeRechazar()).isFalse();
        assertThat(enviada.puedeEliminar()).isTrue();
    }

    @Test
    void buscar_todosLosEstados_devuelveTodosLosVisibles() {
        PaginaResponse<SolicitudColaboracionBusquedaResponse> pagina = buscar(
                new BuscarSolicitudesColaboracionFiltro(
                        null, null, null, null, true, null, null, null),
                0, 20);

        assertThat(pagina.totalElementos()).isEqualTo(5);
        assertThat(ids(pagina)).containsExactly(idR2, idR1, idR8, idR3, idR4);
        assertThat(ids(pagina)).doesNotContain(idR5, idR6, idR7);
    }

    @Test
    void buscar_direccionRecibidaYEnviada() {
        PaginaResponse<SolicitudColaboracionBusquedaResponse> recibidas = buscar(
                new BuscarSolicitudesColaboracionFiltro(
                        DireccionSolicitudColaboracion.RECIBIDA,
                        null, null, null, true, null, null, null),
                0, 20);
        assertThat(ids(recibidas)).containsExactly(idR2, idR4);

        PaginaResponse<SolicitudColaboracionBusquedaResponse> enviadas = buscar(
                new BuscarSolicitudesColaboracionFiltro(
                        DireccionSolicitudColaboracion.ENVIADA,
                        null, null, null, true, null, null, null),
                0, 20);
        assertThat(ids(enviadas)).containsExactly(idR1, idR8, idR3);
    }

    @Test
    void buscar_porPerfilEmisorYReceptor() {
        PaginaResponse<SolicitudColaboracionBusquedaResponse> porEmisor = buscar(
                new BuscarSolicitudesColaboracionFiltro(
                        null, escenario.idPerfilB(), null, null, true, null, null, null),
                0, 20);
        assertThat(ids(porEmisor)).containsExactly(idR2);

        PaginaResponse<SolicitudColaboracionBusquedaResponse> porReceptor = buscar(
                new BuscarSolicitudesColaboracionFiltro(
                        null, null, escenario.idPerfilB(), null, true, null, null, null),
                0, 20);
        assertThat(ids(porReceptor)).containsExactly(idR1);
    }

    @Test
    void buscar_porPublicacion() {
        PaginaResponse<SolicitudColaboracionBusquedaResponse> pagina = buscar(
                new BuscarSolicitudesColaboracionFiltro(
                        null, null, null, null, true, null, null,
                        pubAId),
                0, 20);

        assertThat(ids(pagina)).containsExactly(idR1, idR8, idR3);
        assertThat(pagina.contenido()).allSatisfy(item ->
                assertThat(item.publicacion().idPublicacion())
                        .isEqualTo(pubAId));
    }

    @Test
    void listarPublicacionesFiltro_recibidasDevuelvePublicacionesDeRemitentesVisibles() {
        List<PublicacionSolicitudColaboracionResponse> publicaciones =
                listarPublicacionesSolicitudColaboracionService.listar(
                        escenario.idPerfilA(),
                        DireccionSolicitudColaboracion.RECIBIDA);

        assertThat(publicaciones)
                .extracting(PublicacionSolicitudColaboracionResponse::idPublicacion)
                .containsExactly(pubBId, pubCId);
        assertThat(publicaciones).allSatisfy(publicacion -> {
            assertThat(publicacion.estado()).isEqualTo("ACTIVA");
            assertThat(publicacion.portadaUrl()).isNotBlank();
        });
    }

    @Test
    void listarPublicacionesFiltro_enviadasDevuelveSoloPublicacionesPropias() {
        List<PublicacionSolicitudColaboracionResponse> publicaciones =
                listarPublicacionesSolicitudColaboracionService.listar(
                        escenario.idPerfilA(),
                        DireccionSolicitudColaboracion.ENVIADA);

        assertThat(publicaciones)
                .extracting(PublicacionSolicitudColaboracionResponse::idPublicacion)
                .containsExactly(pubAId);
    }

    @Test
    void listarPublicacionesFiltro_sinDireccionUneRecibidasYEnviadas() {
        List<PublicacionSolicitudColaboracionResponse> publicaciones =
                listarPublicacionesSolicitudColaboracionService.listar(
                        escenario.idPerfilA(), null);

        assertThat(publicaciones)
                .extracting(PublicacionSolicitudColaboracionResponse::idPublicacion)
                .containsExactly(pubAId, pubBId, pubCId);
    }

    @Test
    void buscar_porRangoDeFechas() {
        PaginaResponse<SolicitudColaboracionBusquedaResponse> pagina = buscar(
                new BuscarSolicitudesColaboracionFiltro(
                        null, null, null, null, false,
                        LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10),
                        null),
                0, 20);

        assertThat(ids(pagina)).containsExactly(idR2, idR1, idR8);
    }

    @Test
    void buscar_paginaYOrdenaPorFechaDescendente() {
        PaginaResponse<SolicitudColaboracionBusquedaResponse> primera = buscar(null, 0, 2);
        assertThat(ids(primera)).containsExactly(idR2, idR1);
        assertThat(primera.totalElementos()).isEqualTo(3);
        assertThat(primera.ultima()).isFalse();

        PaginaResponse<SolicitudColaboracionBusquedaResponse> segunda = buscar(null, 1, 2);
        assertThat(ids(segunda)).containsExactly(idR8);
        assertThat(segunda.ultima()).isTrue();
    }

    @Test
    void buscar_incluyePublicacionYPortada() {
        PaginaResponse<SolicitudColaboracionBusquedaResponse> pagina = buscar(null, 0, 20);
        SolicitudColaboracionBusquedaResponse item = porId(pagina, idR1);

        assertThat(item.publicacion()).isNotNull();
        assertThat(item.publicacion().idPublicacion()).isEqualTo(pubAId);
        assertThat(item.publicacion().titulo()).isEqualTo("Publicación A");
        assertThat(item.publicacion().portadaUrl())
                .isEqualTo("/uploads/publicaciones/a.webp");
        assertThat(item.publicacion().estado()).isEqualTo("ACTIVA");
    }

    @Test
    void buscar_todosLosEstadosConEstado_lanzaFiltroInvalido() {
        assertThatThrownBy(() -> buscar(
                new BuscarSolicitudesColaboracionFiltro(
                        null, null, null, EstadoSolicitud.PENDIENTE, true,
                        null, null, null),
                0, 20))
                .isInstanceOf(FiltroSolicitudColaboracionInvalidoException.class);
    }

    private PaginaResponse<SolicitudColaboracionBusquedaResponse> buscar(
            BuscarSolicitudesColaboracionFiltro filtro, int page, int size) {
        return buscarSolicitudColaboracionService.buscar(
                escenario.idPerfilA(), filtro, page, size);
    }

    private List<Long> ids(
            PaginaResponse<SolicitudColaboracionBusquedaResponse> pagina) {
        return pagina.contenido().stream()
                .map(SolicitudColaboracionBusquedaResponse::idSolicitudColaboracion)
                .toList();
    }

    private SolicitudColaboracionBusquedaResponse porId(
            PaginaResponse<SolicitudColaboracionBusquedaResponse> pagina,
            Long id) {
        return pagina.contenido().stream()
                .filter(item -> id.equals(item.idSolicitudColaboracion()))
                .findFirst()
                .orElseThrow();
    }

    private Escenario crearEscenario() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Perfil a = crearPerfil("A", "uc65.a@example.com", "65001", rol, genero,
                EstadoPerfil.Activo);
        Perfil b = crearPerfil("B", "uc65.b@example.com", "65002", rol, genero,
                EstadoPerfil.Activo);
        Perfil c = crearPerfil("C", "uc65.c@example.com", "65003", rol, genero,
                EstadoPerfil.Activo);
        Perfil d = crearPerfil("D", "uc65.d@example.com", "65004", rol, genero,
                EstadoPerfil.Baja);
        Perfil e = crearPerfil("E", "uc65.e@example.com", "65005", rol, genero,
                EstadoPerfil.Activo);

        em.flush();

        return new Escenario(a, b, c, d, e);
    }

    private Perfil crearPerfil(
            String sufijo,
            String correo,
            String dni,
            RolGlobal rol,
            Genero genero,
            EstadoPerfil estado) {

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Nombre " + sufijo)
                .apellido("Apellido " + sufijo)
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = Profesion.builder()
                .codigo("PROF_UC65_" + sufijo)
                .nombre("Profesión UC65 " + sufijo)
                .build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Perfil UC65 " + sufijo)
                .biografia("Biografía UC65 " + sufijo)
                .estado(estado)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private Publicacion crearPublicacion(
            Perfil perfil,
            EstadoPublicacion estado,
            String titulo,
            String archivoPortada) {

        Publicacion publicacion = Publicacion.builder()
                .titulo(titulo)
                .descripcion("Descripción " + titulo)
                .estado(estado)
                .perfil(perfil)
                .build();
        em.persist(publicacion);

        Imagen imagen = Imagen.builder()
                .nombreArchivo(archivoPortada)
                .url("/uploads/publicaciones/" + archivoPortada)
                .tipoImagen("image/webp")
                .tamanoBytes(100)
                .build();
        em.persist(imagen);
        publicacion.getImagenes().add(imagen);
        return publicacion;
    }

    private SolicitudColaboracion crearSolicitud(
            Publicacion publicacion,
            Perfil remitente,
            Perfil destinatario,
            EstadoSolicitud estado,
            LocalDateTime fechaEnvio) {

        SolicitudColaboracion solicitud = SolicitudColaboracion.builder()
                .publicacion(publicacion)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .estado(estado)
                .fechaEnvio(fechaEnvio)
                .fechaRespuesta(estado == EstadoSolicitud.PENDIENTE
                        ? null
                        : fechaEnvio.plusHours(1))
                .build();
        em.persist(solicitud);
        return solicitud;
    }

    private record Escenario(
            Perfil a,
            Perfil b,
            Perfil c,
            Perfil d,
            Perfil e) {

        Long idPerfilA() {
            return a.getIdPerfil();
        }

        Long idPerfilB() {
            return b.getIdPerfil();
        }
    }
}
