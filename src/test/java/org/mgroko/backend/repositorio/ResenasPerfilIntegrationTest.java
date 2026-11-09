package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.ResenasPerfilResponse;
import org.mgroko.backend.perfiles.exception.ResenaNoEncontradaException;
import org.mgroko.backend.perfiles.servicio.ConsultarResenasPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integración de las reseñas de perfil contra PostgreSQL real: consulta
 * paginada, resumen agregado, detalle y reglas de visibilidad.
 */
@SpringBootTest
@Transactional
class ResenasPerfilIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ConsultarResenasPerfilService consultarResenasPerfilService;

    @Autowired
    private CalificacionPerfilRepository calificacionPerfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void listar_devuelvePaginaResumenYDistribucion() {
        Escenario escenario = crearEscenario();

        ResenasPerfilResponse response = consultarResenasPerfilService.listar(
                escenario.perfilReceptor().getIdPerfil(),
                escenario.usuarioReceptor().getIdUsuario(),
                0,
                2);

        assertThat(response.idPerfil())
                .isEqualTo(escenario.perfilReceptor().getIdPerfil());
        assertThat(response.resumen().total()).isEqualTo(3);
        assertThat(response.resumen().promedio()).isEqualTo(4.33);
        assertThat(response.resumen().distribucion())
                .extracting(d -> d.valoracion() + ":" + d.cantidad())
                .containsExactly("5:2", "4:0", "3:1", "2:0", "1:0");

        assertThat(response.calificaciones().contenido()).hasSize(2);
        assertThat(response.calificaciones().totalElementos()).isEqualTo(3);
        assertThat(response.calificaciones().totalPaginas()).isEqualTo(2);
        assertThat(response.calificaciones().primera()).isTrue();
        assertThat(response.calificaciones().ultima()).isFalse();

        var primera = response.calificaciones().contenido().get(0);
        assertThat(primera.emisor().nombreArtistico()).isNotBlank();
        assertThat(primera.emisor().profesion()).isNotBlank();
        assertThat(primera.proyecto().nombre()).isEqualTo("Proyecto reseñas");
    }

    @Test
    void listar_segundaPagina_devuelveElResto() {
        Escenario escenario = crearEscenario();

        ResenasPerfilResponse response = consultarResenasPerfilService.listar(
                escenario.perfilReceptor().getIdPerfil(),
                escenario.usuarioReceptor().getIdUsuario(),
                1,
                2);

        assertThat(response.calificaciones().contenido()).hasSize(1);
        assertThat(response.calificaciones().paginaActual()).isEqualTo(1);
        assertThat(response.calificaciones().ultima()).isTrue();
    }

    @Test
    void obtener_devuelveElDetalleDeLaResenaDelPerfil() {
        Escenario escenario = crearEscenario();
        Long idCalificacion = escenario.calificaciones().get(0).getIdCalificacion();

        var response = consultarResenasPerfilService.obtener(
                escenario.perfilReceptor().getIdPerfil(),
                idCalificacion,
                escenario.usuarioReceptor().getIdUsuario());

        assertThat(response.idCalificacion()).isEqualTo(idCalificacion);
        assertThat(response.emisor().idPerfil())
                .isEqualTo(escenario.perfilEmisor().getIdPerfil());
        assertThat(response.proyecto().idProyecto())
                .isEqualTo(escenario.proyecto().getIdProyecto());
    }

    @Test
    void obtener_resenaDeOtroPerfil_lanzaNoEncontrada() {
        Escenario escenario = crearEscenario();
        Long idCalificacion = escenario.calificaciones().get(0).getIdCalificacion();

        assertThatThrownBy(() -> consultarResenasPerfilService.obtener(
                escenario.perfilEmisor().getIdPerfil(),
                idCalificacion,
                escenario.usuarioReceptor().getIdUsuario()))
                .isInstanceOf(ResenaNoEncontradaException.class);
    }

    @Test
    void listar_perfilAjenoNoActivo_lanzaPerfilNoEncontrado() {
        Escenario escenario = crearEscenario();
        Perfil perfil = em.find(Perfil.class, escenario.perfilReceptor().getIdPerfil());
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        em.flush();

        assertThatThrownBy(() -> consultarResenasPerfilService.listar(
                escenario.perfilReceptor().getIdPerfil(),
                escenario.usuarioEmisor().getIdUsuario(),
                0,
                10))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void listar_perfilPropioEnPendienteBaja_esPermitido() {
        Escenario escenario = crearEscenario();
        Perfil perfil = em.find(Perfil.class, escenario.perfilReceptor().getIdPerfil());
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        em.flush();

        ResenasPerfilResponse response = consultarResenasPerfilService.listar(
                escenario.perfilReceptor().getIdPerfil(),
                escenario.usuarioReceptor().getIdUsuario(),
                0,
                10);

        assertThat(response.resumen().total()).isEqualTo(3);
    }

    private Escenario crearEscenario() {
        Profesion profesion = Profesion.builder()
                .codigo("PROF_RESENA_" + System.nanoTime())
                .nombre("Profesional reseña " + System.nanoTime())
                .build();
        em.persist(profesion);

        Usuario usuarioReceptor = guardarUsuario("resena.receptor." + System.nanoTime() + "@example.com",
                "88992001");
        Usuario usuarioEmisor = guardarUsuario("resena.emisor." + System.nanoTime() + "@example.com",
                "88992002");
        Usuario usuarioEmisor2 = guardarUsuario("resena.emisor2." + System.nanoTime() + "@example.com",
                "88992003");
        Usuario usuarioEmisor3 = guardarUsuario("resena.emisor3." + System.nanoTime() + "@example.com",
                "88992004");

        Perfil perfilReceptor = guardarPerfil(
                "Doja Cat", usuarioReceptor, profesion);
        Perfil perfilEmisor = guardarPerfil(
                "Lucía Benítez", usuarioEmisor, profesion);
        Perfil perfilEmisor2 = guardarPerfil(
                "Martín Gauto", usuarioEmisor2, profesion);
        Perfil perfilEmisor3 = guardarPerfil(
                "Camila Rojas", usuarioEmisor3, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto reseñas")
                .descripcion("Proyecto de integración de reseñas")
                .estado(EstadoProyecto.FINALIZADO)
                .privacidad(Privacidad.PUBLICO)
                .build();
        em.persist(proyecto);

        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilReceptor)
                .rolProyecto(rol("DIRECTOR"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());

        CalificacionPerfil primera = guardarCalificacion(
                proyecto, perfilEmisor, perfilReceptor, 5,
                "Excelente predisposición.", LocalDateTime.now().minusDays(3));
        CalificacionPerfil segunda = guardarCalificacion(
                proyecto, perfilEmisor2, perfilReceptor, 5,
                "Muy buena comunicación.", LocalDateTime.now().minusDays(2));
        CalificacionPerfil tercera = guardarCalificacion(
                proyecto, perfilEmisor3, perfilReceptor, 3,
                "Cumplió con lo acordado.", LocalDateTime.now().minusDays(1));

        em.flush();
        em.clear();

        return new Escenario(
                usuarioReceptor,
                usuarioEmisor,
                perfilReceptor,
                perfilEmisor,
                proyecto,
                List.of(primera, segunda, tercera));
    }

    private Usuario guardarUsuario(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana")
                .apellido("Gomez")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(22))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }

    private Perfil guardarPerfil(
            String nombreArtistico,
            Usuario usuario,
            Profesion profesion) {

        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de integración de reseñas")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private CalificacionPerfil guardarCalificacion(
            Proyecto proyecto,
            Perfil emisor,
            Perfil receptor,
            int valoracion,
            String resenia,
            LocalDateTime fecha) {

        CalificacionPerfil calificacion = CalificacionPerfil.builder()
                .proyecto(proyecto)
                .perfilEmisor(emisor)
                .perfilReceptor(receptor)
                .valoracion(valoracion)
                .resenia(resenia)
                .fechaRegistro(fecha)
                .build();
        return calificacionPerfilRepository.saveAndFlush(calificacion);
    }

    private RolProyecto rol(String codigo) {
        return em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = :codigo",
                RolProyecto.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
    }

    private record Escenario(
            Usuario usuarioReceptor,
            Usuario usuarioEmisor,
            Perfil perfilReceptor,
            Perfil perfilEmisor,
            Proyecto proyecto,
            List<CalificacionPerfil> calificaciones) {
    }
}
