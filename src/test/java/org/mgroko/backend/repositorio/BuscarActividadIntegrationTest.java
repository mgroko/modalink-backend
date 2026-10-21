package org.mgroko.backend.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadBusquedaResponse;
import org.mgroko.backend.actividades.dto.BuscarActividadesFiltro;
import org.mgroko.backend.actividades.servicio.BuscarActividadService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.PerfilTycId;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * UC-52 contra PostgreSQL real: la Specification, el enriquecimiento batch y
 * la regla de visibilidad compartida con el detalle de proyecto.
 */
@SpringBootTest
@Transactional
class BuscarActividadIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private BuscarActividadService buscarActividadService;

    private Perfil crearPerfil(String sufijo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Busqueda").apellido("Actividad").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("busqact" + sufijo + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Profesion profesion = Profesion.builder()
                .codigo("PROF_BA" + sufijo)
                .nombre("Profesion Actividad " + sufijo)
                .build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Perfil Actividad " + sufijo)
                .biografia("Perfil de prueba")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);

        em.persist(PerfilTyc.builder()
                .id(new PerfilTycId(perfil.getIdPerfil(), 1L))
                .perfil(perfil)
                .descripcion("T&C Base")
                .fechaInicio(LocalDateTime.now())
                .build());

        return perfil;
    }

    private Proyecto crearProyecto(String nombre, EstadoProyecto estado, Privacidad privacidad) {
        Proyecto proyecto = Proyecto.builder()
                .nombre(nombre)
                .descripcion("Descripcion de test")
                .fechaInicio(LocalDate.now())
                .estado(estado)
                .privacidad(privacidad)
                .build();
        em.persist(proyecto);
        return proyecto;
    }

    private Planificacion crearPlanificacion(Proyecto proyecto) {
        Planificacion planificacion = Planificacion.builder().proyecto(proyecto).build();
        em.persist(planificacion);
        return planificacion;
    }

    private Actividad crearActividad(
            Planificacion planificacion,
            String nombre,
            String descripcion,
            int duracionMinutos,
            LocalDateTime inicio,
            EstadoActividad estado) {

        Actividad actividad = Actividad.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .duracionMinutos(duracionMinutos)
                .fechaHoraInicio(inicio)
                .estado(estado)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);
        return actividad;
    }

    private void crearMiembro(Proyecto proyecto, Perfil perfil, EstadoParticipacion estado) {
        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolMiembro())
                .idTycAceptado(1L)
                .estadoParticipacion(estado)
                .build());
    }

    private RolProyecto rolMiembro() {
        return em.createQuery("""
                SELECT r FROM RolProyecto r
                WHERE r.codigo = 'MIEMBRO' OR LOWER(r.nombre) LIKE 'miembro%'
                """, RolProyecto.class)
                .setMaxResults(1)
                .getSingleResult();
    }

    private RequerimientoActividad crearRequerimiento(
            Actividad actividad,
            String sufijo,
            String profesionNombre,
            String descripcion,
            String habilidadNombre,
            String caracteristicaNombre) {

        Profesion profesion = Profesion.builder()
                .codigo("REQ_PROF_" + sufijo)
                .nombre(profesionNombre + " " + sufijo)
                .build();
        em.persist(profesion);

        Habilidad habilidad = Habilidad.builder()
                .codigo("HAB_" + sufijo)
                .nombre(habilidadNombre + " " + sufijo)
                .build();
        em.persist(habilidad);

        CaracteristicaTecnica caracteristica = CaracteristicaTecnica.builder()
                .codigo("CAR_" + sufijo)
                .nombre(caracteristicaNombre + " " + sufijo)
                .tipoDato(CaracteristicaTecnica.TIPO_NUMERICO)
                .profesion(profesion)
                .build();
        em.persist(caracteristica);

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .cantidad(1)
                .descripcion(descripcion)
                .profesion(profesion)
                .actividad(actividad)
                .habilidades(Set.of(habilidad))
                .build();

        requerimiento.setCaracteristicas(List.of(RequerimientoActCaract.builder()
                .requerimientoActividad(requerimiento)
                .caracteristica(caracteristica)
                .valorMin(BigDecimal.ZERO)
                .valorMax(BigDecimal.TEN)
                .build()));

        em.persist(requerimiento);
        return requerimiento;
    }

    private List<String> nombres(PaginaResponse<ActividadBusquedaResponse> pagina) {
        return pagina.contenido().stream()
                .map(ActividadBusquedaResponse::nombre)
                .toList();
    }

    @Test
    void miembro_buscaYFiltraDentroDeSuProyecto() {
        Perfil perfil = crearPerfil("A", "99300001");
        Proyecto proyecto = crearProyecto("Proyecto Propio A", EstadoProyecto.BORRADOR, Privacidad.PUBLICO);
        Planificacion planificacion = crearPlanificacion(proyecto);
        crearMiembro(proyecto, perfil, EstadoParticipacion.ACTIVO);

        Actividad montaje = crearActividad(planificacion, "Montaje", "Montar escenario",
                90, LocalDateTime.of(2026, 11, 1, 9, 0), EstadoActividad.PENDIENTE);
        Actividad casting = crearActividad(planificacion, "Casting", "Seleccionar modelos",
                30, LocalDateTime.of(2026, 11, 2, 10, 0), EstadoActividad.FINALIZADA);
        Actividad vestuario = crearActividad(planificacion, "Vestuario", "Preparar vestuario",
                60, LocalDateTime.of(2026, 11, 3, 11, 0), EstadoActividad.EN_CURSO);

        crearRequerimiento(montaje, "A1", "Modelo", "Dos modelos",
                "Costura", "Altura");
        crearRequerimiento(casting, "A2", "Estilista", "Un estilista",
                "Maquillaje", "Experiencia");

        em.flush();

        PaginaResponse<ActividadBusquedaResponse> todas = buscarActividadService.buscar(
                perfil.getIdPerfil(), proyecto.getIdProyecto(), null, 0, 20, false);
        assertThat(nombres(todas)).containsExactly("Montaje", "Casting", "Vestuario");

        PaginaResponse<ActividadBusquedaResponse> porNombre = buscarActividadService.buscar(
                perfil.getIdPerfil(), proyecto.getIdProyecto(),
                new BuscarActividadesFiltro("mont", null, null, null, null, null),
                0, 20, false);
        assertThat(nombres(porNombre)).containsExactly("Montaje");

        PaginaResponse<ActividadBusquedaResponse> porDuracion = buscarActividadService.buscar(
                perfil.getIdPerfil(), proyecto.getIdProyecto(),
                new BuscarActividadesFiltro(null, null, null, 60, 100, null),
                0, 20, false);
        assertThat(nombres(porDuracion)).containsExactly("Montaje", "Vestuario");

        PaginaResponse<ActividadBusquedaResponse> porEstado = buscarActividadService.buscar(
                perfil.getIdPerfil(), proyecto.getIdProyecto(),
                new BuscarActividadesFiltro(null, null, null, null, null,
                        List.of(EstadoActividad.FINALIZADA)),
                0, 20, false);
        assertThat(nombres(porEstado)).containsExactly("Casting");

        PaginaResponse<ActividadBusquedaResponse> porProfesion = buscarActividadService.buscar(
                perfil.getIdPerfil(), proyecto.getIdProyecto(),
                new BuscarActividadesFiltro(null, null, "modelo", null, null, null),
                0, 20, false);
        assertThat(nombres(porProfesion)).containsExactly("Montaje");

        PaginaResponse<ActividadBusquedaResponse> porDescripcionRequerimiento = buscarActividadService.buscar(
                perfil.getIdPerfil(), proyecto.getIdProyecto(),
                new BuscarActividadesFiltro(null, null, "Dos modelos", null, null, null),
                0, 20, false);
        assertThat(nombres(porDescripcionRequerimiento)).containsExactly("Montaje");

        PaginaResponse<ActividadBusquedaResponse> porHabilidad = buscarActividadService.buscar(
                perfil.getIdPerfil(), proyecto.getIdProyecto(),
                new BuscarActividadesFiltro(null, null, "costura", null, null, null),
                0, 20, false);
        assertThat(nombres(porHabilidad)).containsExactly("Montaje");

        PaginaResponse<ActividadBusquedaResponse> porCaracteristica = buscarActividadService.buscar(
                perfil.getIdPerfil(), proyecto.getIdProyecto(),
                new BuscarActividadesFiltro(null, null, "Altura", null, null, null),
                0, 20, false);
        assertThat(nombres(porCaracteristica)).containsExactly("Montaje");
    }

    @Test
    void noMiembro_soloPuedeBuscarEnProyectosPublicosVisibles() {
        Perfil perfil = crearPerfil("B", "99300002");

        Proyecto publico = crearProyecto("Publico B", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO);
        Planificacion planPublico = crearPlanificacion(publico);
        crearActividad(planPublico, "Montaje", "Escenario", 90,
                LocalDateTime.of(2026, 11, 1, 9, 0), EstadoActividad.PENDIENTE);

        Proyecto privado = crearProyecto("Privado B", EstadoProyecto.PUBLICADO, Privacidad.PRIVADO);
        Planificacion planPrivado = crearPlanificacion(privado);
        crearActividad(planPrivado, "Casting", "Modelos", 30,
                LocalDateTime.of(2026, 11, 2, 9, 0), EstadoActividad.PENDIENTE);

        Proyecto borrador = crearProyecto("Borrador B", EstadoProyecto.BORRADOR, Privacidad.PUBLICO);
        Planificacion planBorrador = crearPlanificacion(borrador);
        crearActividad(planBorrador, "Vestuario", "Ropa", 60,
                LocalDateTime.of(2026, 11, 3, 9, 0), EstadoActividad.PENDIENTE);

        em.flush();

        PaginaResponse<ActividadBusquedaResponse> permitida = buscarActividadService.buscar(
                perfil.getIdPerfil(), publico.getIdProyecto(), null, 0, 20, false);
        assertThat(nombres(permitida)).containsExactly("Montaje");

        assertThatThrownBy(() -> buscarActividadService.buscar(
                perfil.getIdPerfil(), privado.getIdProyecto(), null, 0, 20, false))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        assertThatThrownBy(() -> buscarActividadService.buscar(
                perfil.getIdPerfil(), borrador.getIdProyecto(), null, 0, 20, false))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void exMiembro_puedeBuscarEnProyectoCancelado() {
        Perfil perfil = crearPerfil("C", "99300003");
        Proyecto cancelado = crearProyecto(
                "Cancelado C", EstadoProyecto.CANCELADO, Privacidad.PRIVADO);
        Planificacion planificacion = crearPlanificacion(cancelado);
        crearActividad(planificacion, "Montaje", "Escenario", 90,
                LocalDateTime.of(2026, 11, 1, 9, 0), EstadoActividad.CANCELADA);
        crearMiembro(cancelado, perfil, EstadoParticipacion.BAJA_VOLUNTARIA);

        em.flush();

        PaginaResponse<ActividadBusquedaResponse> pagina = buscarActividadService.buscar(
                perfil.getIdPerfil(), cancelado.getIdProyecto(), null, 0, 20, false);

        assertThat(nombres(pagina)).containsExactly("Montaje");
    }

    @Test
    void miembro_sinCoincidencias_devuelvePaginaVacia() {
        Perfil perfil = crearPerfil("D", "99300004");
        Proyecto proyecto = crearProyecto("Proyecto D", EstadoProyecto.BORRADOR, Privacidad.PUBLICO);
        Planificacion planificacion = crearPlanificacion(proyecto);
        crearMiembro(proyecto, perfil, EstadoParticipacion.ACTIVO);
        crearActividad(planificacion, "Montaje", "Escenario", 90,
                LocalDateTime.of(2026, 11, 1, 9, 0), EstadoActividad.PENDIENTE);
        em.flush();

        PaginaResponse<ActividadBusquedaResponse> pagina = buscarActividadService.buscar(
                perfil.getIdPerfil(), proyecto.getIdProyecto(),
                new BuscarActividadesFiltro("NoExiste", null, null, null, null, null),
                0, 20, false);

        assertThat(pagina.contenido()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
    }
}
