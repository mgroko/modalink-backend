package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
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
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.servicio.EliminarPublicacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class EliminarPublicacionIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private EliminarPublicacionService eliminarPublicacionService;

    @Autowired
    private PublicacionRepository publicacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Test
    void eliminar_comoDuenio_borraPublicacionImagenesColaboradoresSolicitudesYNotificaciones() {
        Escenario escenario = crearEscenario("01");
        Publicacion publicacion = crearPublicacion(escenario.duenio(), 2);
        Perfil colaborador = crearPerfil("011", "Colaborador UC20");
        Perfil destinatario = crearPerfil("012", "Destinatario UC20");
        em.flush();

        insertarColaborador(publicacion.getIdPublicacion(), colaborador.getIdPerfil());
        crearSolicitud(publicacion, escenario.duenio(), destinatario);
        crearNotificacion(destinatario, publicacion);
        em.flush();

        Long idPublicacion = publicacion.getIdPublicacion();
        Long idImagenUno = publicacion.getImagenes().get(0).getIdImagen();
        Long idImagenDos = publicacion.getImagenes().get(1).getIdImagen();
        em.flush();
        em.clear();

        eliminarPublicacionService.eliminar(
                escenario.idUsuario(), escenario.idPerfil(), idPublicacion);
        em.flush();
        em.clear();

        assertThat(em.find(Publicacion.class, idPublicacion)).isNull();
        assertThat(em.find(Imagen.class, idImagenUno)).isNull();
        assertThat(em.find(Imagen.class, idImagenDos)).isNull();
        assertThat(contar("imagen_publicacion", idPublicacion)).isZero();
        assertThat(contar("colaborador_publicacion", idPublicacion)).isZero();
        assertThat(contarSolicitudes(idPublicacion)).isZero();
        assertThat(contarNotificaciones(idPublicacion)).isZero();
    }

    @Test
    void eliminar_comoColaborador_soloQuitaLaEtiquetaYConservaLaPublicacion() {
        Escenario escenario = crearEscenario("02");
        Publicacion publicacion = crearPublicacion(escenario.duenio(), 2);
        Perfil colaboradorUno = crearPerfil("021", "Colaborador uno UC20");
        Perfil colaboradorDos = crearPerfil("022", "Colaborador dos UC20");
        Perfil destinatario = crearPerfil("023", "Destinatario UC20");
        em.flush();

        insertarColaborador(publicacion.getIdPublicacion(), colaboradorUno.getIdPerfil());
        insertarColaborador(publicacion.getIdPublicacion(), colaboradorDos.getIdPerfil());
        crearSolicitud(publicacion, escenario.duenio(), destinatario);
        crearNotificacion(destinatario, publicacion);
        em.flush();

        Long idPublicacion = publicacion.getIdPublicacion();
        Long idImagen = publicacion.getImagenes().get(0).getIdImagen();
        Long idUsuarioColaboradorUno = colaboradorUno.getUsuario().getIdUsuario();
        em.flush();
        em.clear();

        eliminarPublicacionService.eliminar(
                idUsuarioColaboradorUno,
                colaboradorUno.getIdPerfil(),
                idPublicacion);
        em.flush();
        em.clear();

        assertThat(em.find(Publicacion.class, idPublicacion)).isNotNull();
        assertThat(em.find(Imagen.class, idImagen)).isNotNull();
        assertThat(contar("colaborador_publicacion", idPublicacion)).isEqualTo(1);
        assertThat(contarSolicitudes(idPublicacion)).isEqualTo(1);
        assertThat(contarNotificaciones(idPublicacion)).isEqualTo(1);

        assertThat(publicacionesDe(escenario.idPerfil()))
                .extracting(Publicacion::getIdPublicacion)
                .contains(idPublicacion);
        assertThat(publicacionesDe(colaboradorDos.getIdPerfil()))
                .extracting(Publicacion::getIdPublicacion)
                .contains(idPublicacion);
        assertThat(publicacionesDe(colaboradorUno.getIdPerfil()))
                .extracting(Publicacion::getIdPublicacion)
                .doesNotContain(idPublicacion);
    }

    @Test
    void eliminar_comoDuenioSinColaboradores_borraLaPublicacion() {
        Escenario escenario = crearEscenario("03");
        Publicacion publicacion = crearPublicacion(escenario.duenio(), 1);
        Perfil destinatario = crearPerfil("031", "Destinatario UC20");
        em.flush();

        crearSolicitud(publicacion, escenario.duenio(), destinatario);
        crearNotificacion(destinatario, publicacion);
        em.flush();

        Long idPublicacion = publicacion.getIdPublicacion();
        em.flush();
        em.clear();

        eliminarPublicacionService.eliminar(
                escenario.idUsuario(), escenario.idPerfil(), idPublicacion);
        em.flush();
        em.clear();

        assertThat(em.find(Publicacion.class, idPublicacion)).isNull();
        assertThat(contarSolicitudes(idPublicacion)).isZero();
        assertThat(contarNotificaciones(idPublicacion)).isZero();
    }

    @Test
    void eliminar_perfilAjeno_lanzaNoEncontrada() {
        Escenario escenario = crearEscenario("04");
        Publicacion publicacion = crearPublicacion(escenario.duenio(), 1);
        Perfil otro = crearPerfil("041", "Otro UC20");
        em.flush();

        assertThatThrownBy(() -> eliminarPublicacionService.eliminar(
                otro.getUsuario().getIdUsuario(),
                otro.getIdPerfil(),
                publicacion.getIdPublicacion()))
                .isInstanceOf(PublicacionNoEncontradaException.class);

        em.flush();
        em.clear();

        assertThat(em.find(Publicacion.class, publicacion.getIdPublicacion())).isNotNull();
    }

    private Page<Publicacion> publicacionesDe(Long idPerfil) {
        return publicacionRepository.findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
                idPerfil,
                EstadoPublicacion.ACTIVA,
                PageRequest.of(0, 20));
    }

    private long contar(String tabla, Long idPublicacion) {
        Number cantidad = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM " + tabla + " WHERE id_publicacion = :idPublicacion")
                .setParameter("idPublicacion", idPublicacion)
                .getSingleResult();
        return cantidad.longValue();
    }

    private long contarSolicitudes(Long idPublicacion) {
        return em.createQuery("""
                SELECT COUNT(s)
                FROM SolicitudColaboracion s
                WHERE s.publicacion.idPublicacion = :idPublicacion
                """, Long.class)
                .setParameter("idPublicacion", idPublicacion)
                .getSingleResult();
    }

    private long contarNotificaciones(Long idPublicacion) {
        return em.createQuery("""
                SELECT COUNT(n)
                FROM Notificacion n
                WHERE n.entidadTipo = :entidadTipo
                  AND n.entidadId = :entidadId
                """, Long.class)
                .setParameter("entidadTipo", NotificacionService.ENTIDAD_PUBLICACION)
                .setParameter("entidadId", idPublicacion)
                .getSingleResult();
    }

    private void insertarColaborador(Long idPublicacion, Long idPerfil) {
        em.createNativeQuery("""
                INSERT INTO colaborador_publicacion (id_publicacion, id_perfil)
                VALUES (:idPublicacion, :idPerfil)
                """)
                .setParameter("idPublicacion", idPublicacion)
                .setParameter("idPerfil", idPerfil)
                .executeUpdate();
    }

    private void crearSolicitud(
            Publicacion publicacion,
            Perfil remitente,
            Perfil destinatario) {

        em.persist(SolicitudColaboracion.builder()
                .publicacion(publicacion)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.now())
                .build());
    }

    private void crearNotificacion(Perfil destinatario, Publicacion publicacion) {
        List<Notificacion> notificaciones = List.of(notificacionService.crear(
                destinatario.getUsuario(),
                destinatario,
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                "Te invitaron a colaborar.",
                NotificacionService.ENTIDAD_PUBLICACION,
                publicacion.getIdPublicacion()));
        notificacionService.registrarTodas(notificaciones);
    }

    private Publicacion crearPublicacion(Perfil perfil, int cantidadImagenes) {
        Publicacion publicacion = Publicacion.builder()
                .titulo("Editorial UC20")
                .descripcion("Descripción UC20")
                .fechaPublicacion(LocalDateTime.now())
                .estado(EstadoPublicacion.ACTIVA)
                .perfil(perfil)
                .imagenes(new ArrayList<>())
                .colaboradores(new LinkedHashSet<>())
                .build();
        em.persist(publicacion);

        for (int i = 0; i < cantidadImagenes; i++) {
            Imagen imagen = Imagen.builder()
                    .nombreArchivo("uc20_" + i + "_" + System.nanoTime() + ".webp")
                    .url("/uploads/publicaciones/uc20_" + i + ".webp")
                    .tipoImagen("image/webp")
                    .tamanoBytes(100)
                    .build();
            em.persist(imagen);
            publicacion.getImagenes().add(imagen);
        }

        return publicacion;
    }

    private Escenario crearEscenario(String sufijo) {
        Perfil duenio = crearPerfil(sufijo + "1", "Dueño UC20 " + sufijo);
        return new Escenario(duenio);
    }

    private Perfil crearPerfil(String sufijo, String nombreArtistico) {
        int correlativo = SECUENCIA.incrementAndGet();
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("UC20")
                .apellido("Test")
                .dni("79" + String.format("%08d", correlativo))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("uc20." + sufijo + "." + correlativo + "@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = Profesion.builder()
                .codigo("PROF_UC20_" + sufijo + "_" + correlativo)
                .nombre("Profesión UC20 " + correlativo)
                .build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil UC20")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        em.flush();
        return perfil;
    }

    private record Escenario(Perfil duenio) {

        Long idUsuario() {
            return duenio.getUsuario().getIdUsuario();
        }

        Long idPerfil() {
            return duenio.getIdPerfil();
        }
    }
}
