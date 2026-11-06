package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro;
import org.mgroko.backend.proyectos.dto.CalificacionRequest;
import org.mgroko.backend.proyectos.dto.CalificacionesProyectoResponse;
import org.mgroko.backend.proyectos.dto.CalificarEquipoRequest;
import org.mgroko.backend.proyectos.dto.FinalizarProyectoRequest;
import org.mgroko.backend.proyectos.dto.FinalizarProyectoResponse;
import org.mgroko.backend.proyectos.dto.PrevisualizacionFinalizacionResponse;
import org.mgroko.backend.proyectos.dto.ProyectoPropioResponse;
import org.mgroko.backend.proyectos.evento.ProyectoFinalizadoEvent;
import org.mgroko.backend.proyectos.exception.CalificacionIncompletaException;
import org.mgroko.backend.proyectos.exception.CalificacionInvalidaException;
import org.mgroko.backend.proyectos.exception.ConfirmacionPrematuraRequeridaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoFinalizableException;
import org.mgroko.backend.proyectos.servicio.BuscarProyectosPropiosService;
import org.mgroko.backend.proyectos.servicio.CalificarEquipoService;
import org.mgroko.backend.proyectos.servicio.FinalizarProyectoService;
import org.mgroko.backend.proyectos.servicio.PrevisualizarFinalizacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integración de UC-37 contra PostgreSQL real: finalización, calificaciones
 * del director, calificaciones entre participantes e historial propio.
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class FinalizarProyectoIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private FinalizarProyectoService finalizarProyectoService;

    @Autowired
    private PrevisualizarFinalizacionService previsualizarFinalizacionService;

    @Autowired
    private CalificarEquipoService calificarEquipoService;

    @Autowired
    private CalificacionPerfilRepository calificacionPerfilRepository;

    @Autowired
    private BuscarProyectosPropiosService buscarProyectosPropiosService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void finalizar_caminoFeliz_persisteEstadoFechaCalificacionesYPublicaEvento() {
        Datos datos = crearProyectoConfirmado();

        PrevisualizacionFinalizacionResponse preview =
                previsualizarFinalizacionService.previsualizar(
                        datos.director().getIdUsuario(),
                        datos.perfilDirector().getIdPerfil(),
                        datos.proyecto().getIdProyecto());

        assertThat(preview.puedeFinalizar()).isTrue();
        assertThat(preview.miembrosACalificar()).hasSize(2);
        assertThat(preview.calificacionesPendientes()).isEqualTo(2);

        FinalizarProyectoResponse response = finalizar(datos);

        em.flush();
        em.clear();

        Proyecto proyecto = em.find(
                Proyecto.class, datos.proyecto().getIdProyecto());

        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.FINALIZADO);
        assertThat(proyecto.getFechaFinalizacion()).isNotNull();
        assertThat(response.proyecto().estado()).isEqualTo("Finalizado");
        assertThat(response.fechaFinalizacion())
                .isEqualTo(proyecto.getFechaFinalizacion());
        assertThat(response.calificacionesRegistradas()).isEqualTo(2);

        List<CalificacionPerfil> calificaciones =
                calificacionPerfilRepository
                        .findByProyectoIdProyectoAndPerfilEmisorIdPerfil(
                                datos.proyecto().getIdProyecto(),
                                datos.perfilDirector().getIdPerfil());

        assertThat(calificaciones).hasSize(2);
        assertThat(calificaciones)
                .extracting(CalificacionPerfil::getValoracion)
                .containsExactlyInAnyOrder(5, 4);
        assertThat(calificaciones)
                .extracting(CalificacionPerfil::getResenia)
                .containsExactlyInAnyOrder("Excelente desempeño", "Muy bien");

        assertThat(events.stream(ProyectoFinalizadoEvent.class).toList())
                .singleElement()
                .satisfies(evento -> {
                    assertThat(evento.idProyecto())
                            .isEqualTo(datos.proyecto().getIdProyecto());
                    assertThat(evento.idPerfilActor())
                            .isEqualTo(datos.perfilDirector().getIdPerfil());
                });

        PaginaResponse<ProyectoPropioResponse> propios =
                buscarProyectosPropiosService.buscar(
                        datos.perfilDirector().getIdPerfil(),
                        new BuscarProyectosPropiosFiltro(
                                null, EstadoProyecto.FINALIZADO, null),
                        0,
                        50,
                        false);

        assertThat(propios.contenido())
                .extracting(ProyectoPropioResponse::nombre)
                .contains(datos.proyecto().getNombre());
    }

    @Test
    void finalizar_fechaFuturaSinConfirmacion_lanzaExcepcionYNoCambiaEstado() {
        Datos datos = crearProyectoConfirmado();
        datos.planificacion().setFechaEntrega(LocalDate.now().plusDays(10));
        em.flush();

        PrevisualizacionFinalizacionResponse preview =
                previsualizarFinalizacionService.previsualizar(
                        datos.director().getIdUsuario(),
                        datos.perfilDirector().getIdPerfil(),
                        datos.proyecto().getIdProyecto());

        assertThat(preview.puedeFinalizar()).isTrue();
        assertThat(preview.requiereConfirmacionPrematura()).isTrue();
        assertThat(preview.diasAnticipacion()).isEqualTo(10);

        assertThatThrownBy(() -> finalizar(datos, false))
                .isInstanceOf(ConfirmacionPrematuraRequeridaException.class);

        em.flush();
        em.clear();

        Proyecto proyecto = em.find(
                Proyecto.class, datos.proyecto().getIdProyecto());

        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.CONFIRMADO);
        assertThat(proyecto.getFinalizacionPrematura()).isFalse();
        assertThat(proyecto.getFechaFinalizacion()).isNull();
        assertThat(calificacionPerfilRepository.findByProyectoIdProyecto(
                datos.proyecto().getIdProyecto())).isEmpty();
    }

    @Test
    void finalizar_fechaFuturaConConfirmacion_marcaFinalizacionPrematura() {
        Datos datos = crearProyectoConfirmado();
        datos.planificacion().setFechaEntrega(LocalDate.now().plusDays(10));
        em.flush();

        FinalizarProyectoResponse response = finalizar(datos, true);

        em.flush();
        em.clear();

        Proyecto proyecto = em.find(
                Proyecto.class, datos.proyecto().getIdProyecto());

        assertThat(response.proyecto().estado()).isEqualTo("Finalizado");
        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.FINALIZADO);
        assertThat(proyecto.getFinalizacionPrematura()).isTrue();

        assertThat(events.stream(ProyectoFinalizadoEvent.class).toList())
                .singleElement()
                .satisfies(evento -> assertThat(
                        evento.finalizacionPrematura()).isTrue());
    }

    @Test
    void finalizar_calificacionesIncompletas_noCambiaEstadoNiPersisteCalificaciones() {
        Datos datos = crearProyectoConfirmado();

        assertThatThrownBy(() -> finalizarProyectoService.finalizar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                new FinalizarProyectoRequest(List.of(
                        new CalificacionRequest(
                                datos.perfilMiembro1().getIdPerfil(),
                                5,
                                "Excelente desempeño")))))
                .isInstanceOf(CalificacionIncompletaException.class);

        em.flush();
        em.clear();

        Proyecto proyecto = em.find(
                Proyecto.class, datos.proyecto().getIdProyecto());

        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.CONFIRMADO);
        assertThat(proyecto.getFechaFinalizacion()).isNull();
        assertThat(calificacionPerfilRepository.findByProyectoIdProyecto(
                datos.proyecto().getIdProyecto())).isEmpty();
    }

    @Test
    void finalizar_actividadPendiente_lanzaNoFinalizableYNoCambiaEstado() {
        Datos datos = crearProyectoConfirmado();
        datos.actividadFinalizada().setEstado(EstadoActividad.PENDIENTE);
        em.flush();

        assertThatThrownBy(() -> finalizar(datos))
                .isInstanceOf(ProyectoNoFinalizableException.class)
                .extracting("codigo")
                .isEqualTo("ACTIVIDAD_NO_FINALIZADA");

        em.flush();
        em.clear();

        Proyecto proyecto = em.find(
                Proyecto.class, datos.proyecto().getIdProyecto());

        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.CONFIRMADO);
        assertThat(calificacionPerfilRepository.findByProyectoIdProyecto(
                datos.proyecto().getIdProyecto())).isEmpty();
    }

    @Test
    void finalizar_todasLasActividadesCanceladas_esValido() {
        Datos datos = crearProyectoConfirmado();
        datos.actividadFinalizada().setEstado(EstadoActividad.CANCELADA);
        em.flush();

        FinalizarProyectoResponse response = finalizar(datos);

        assertThat(response.proyecto().estado()).isEqualTo("Finalizado");
        assertThat(response.calificacionesRegistradas()).isEqualTo(2);
    }

    @Test
    void finalizar_dosVeces_lanzaNoFinalizableYNoDuplicaCalificaciones() {
        Datos datos = crearProyectoConfirmado();

        finalizar(datos);

        assertThatThrownBy(() -> finalizar(datos))
                .isInstanceOf(ProyectoNoFinalizableException.class)
                .extracting("codigo")
                .isEqualTo("PROYECTO_YA_FINALIZADO");

        em.flush();
        em.clear();

        assertThat(calificacionPerfilRepository.findByProyectoIdProyecto(
                datos.proyecto().getIdProyecto())).hasSize(2);
    }

    @Test
    void calificarEntreParticipantes_persisteTodasLasCalificacionesYPendientes() {
        Datos datos = crearProyectoConfirmado();
        finalizar(datos);

        em.flush();
        em.clear();

        CalificacionesProyectoResponse inicialMiembro1 =
                calificarEquipoService.obtener(
                        datos.miembro1().getIdUsuario(),
                        datos.perfilMiembro1().getIdPerfil(),
                        datos.proyecto().getIdProyecto());

        assertThat(inicialMiembro1.calificacionesRequeridas()).isEqualTo(2);
        assertThat(inicialMiembro1.calificacionesRealizadas()).isZero();
        assertThat(inicialMiembro1.calificacionesPendientes()).isEqualTo(2);
        assertThat(inicialMiembro1.calificacionesEmitidas()).isEmpty();

        CalificacionesProyectoResponse miembro1 =
                calificarEquipoService.calificar(
                        datos.miembro1().getIdUsuario(),
                        datos.perfilMiembro1().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        new CalificarEquipoRequest(List.of(
                                new CalificacionRequest(
                                        datos.perfilDirector().getIdPerfil(),
                                        5,
                                        "Excelente dirección"),
                                new CalificacionRequest(
                                        datos.perfilMiembro2().getIdPerfil(),
                                        4,
                                        "Muy buena colaboración"))));

        assertThat(miembro1.calificacionesCompletas()).isTrue();
        assertThat(miembro1.calificacionesRealizadas()).isEqualTo(2);
        assertThat(miembro1.calificacionesPendientes()).isZero();

        CalificacionesProyectoResponse inicialMiembro2 =
                calificarEquipoService.obtener(
                        datos.miembro2().getIdUsuario(),
                        datos.perfilMiembro2().getIdPerfil(),
                        datos.proyecto().getIdProyecto());

        assertThat(inicialMiembro2.calificacionesPendientes()).isEqualTo(2);
        assertThat(inicialMiembro2.calificacionesRealizadas()).isZero();

        CalificacionesProyectoResponse miembro2 =
                calificarEquipoService.calificar(
                        datos.miembro2().getIdUsuario(),
                        datos.perfilMiembro2().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        new CalificarEquipoRequest(List.of(
                                new CalificacionRequest(
                                        datos.perfilDirector().getIdPerfil(),
                                        5,
                                        "Excelente dirección"),
                                new CalificacionRequest(
                                        datos.perfilMiembro1().getIdPerfil(),
                                        4,
                                        "Muy buena colaboración"))));

        assertThat(miembro2.calificacionesCompletas()).isTrue();
        assertThat(miembro2.calificacionesRealizadas()).isEqualTo(2);

        em.flush();
        em.clear();

        assertThat(calificacionPerfilRepository.findByProyectoIdProyecto(
                datos.proyecto().getIdProyecto())).hasSize(6);

        assertThatThrownBy(() -> calificarEquipoService.calificar(
                datos.miembro1().getIdUsuario(),
                datos.perfilMiembro1().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                new CalificarEquipoRequest(List.of(
                        new CalificacionRequest(
                                datos.perfilDirector().getIdPerfil(),
                                5,
                                "Repetida")))))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("ya tiene una calificación");
    }

    @Test
    void calificacionDuplicadaEnBd_violaConstraintUnica() {
        Datos datos = crearProyectoConfirmado();

        CalificacionPerfil primera = CalificacionPerfil.builder()
                .proyecto(datos.proyecto())
                .perfilEmisor(datos.perfilDirector())
                .perfilReceptor(datos.perfilMiembro1())
                .valoracion(5)
                .resenia("Excelente desempeño")
                .fechaRegistro(LocalDateTime.now())
                .build();
        calificacionPerfilRepository.saveAndFlush(primera);

        CalificacionPerfil duplicada = CalificacionPerfil.builder()
                .proyecto(datos.proyecto())
                .perfilEmisor(datos.perfilDirector())
                .perfilReceptor(datos.perfilMiembro1())
                .valoracion(4)
                .resenia("Otra reseña")
                .fechaRegistro(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> calificacionPerfilRepository
                .saveAndFlush(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private FinalizarProyectoResponse finalizar(Datos datos) {
        return finalizar(datos, false);
    }

    private FinalizarProyectoResponse finalizar(
            Datos datos,
            boolean confirmarFinalizacionPrematura) {
        return finalizarProyectoService.finalizar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                new FinalizarProyectoRequest(
                        List.of(
                                new CalificacionRequest(
                                        datos.perfilMiembro1().getIdPerfil(),
                                        5,
                                        "Excelente desempeño"),
                                new CalificacionRequest(
                                        datos.perfilMiembro2().getIdPerfil(),
                                        4,
                                        "Muy bien")),
                        confirmarFinalizacionPrematura));
    }

    private Datos crearProyectoConfirmado() {
        Profesion profesion = Profesion.builder()
                .codigo("PROF_UC37_" + System.nanoTime())
                .nombre("Profesional UC37 " + System.nanoTime())
                .build();
        em.persist(profesion);

        Usuario director = guardarUsuario(
                "uc37.dir." + System.nanoTime() + "@example.com", "88991001");
        Usuario miembro1 = guardarUsuario(
                "uc37.miem1." + System.nanoTime() + "@example.com", "88991002");
        Usuario miembro2 = guardarUsuario(
                "uc37.miem2." + System.nanoTime() + "@example.com", "88991003");

        Perfil perfilDirector = perfil("Directora UC37", director, profesion);
        Perfil perfilMiembro1 = perfil("Miembro Uno UC37", miembro1, profesion);
        Perfil perfilMiembro2 = perfil("Miembro Dos UC37", miembro2, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC37 " + System.nanoTime())
                .descripcion("Proyecto de integración UC-37")
                .estado(EstadoProyecto.CONFIRMADO)
                .privacidad(Privacidad.PUBLICO)
                .fechaInicio(LocalDateTime.now().minusDays(2).withNano(0))
                .build();
        em.persist(proyecto);

        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now())
                .build();
        em.persist(planificacion);

        Actividad actividadFinalizada = Actividad.builder()
                .nombre("Actividad finalizada UC37")
                .duracionMinutos(60)
                .estado(EstadoActividad.FINALIZADA)
                .fechaHoraInicio(LocalDateTime.now().minusDays(1).withNano(0))
                .planificacion(planificacion)
                .build();
        em.persist(actividadFinalizada);

        Actividad actividadCancelada = Actividad.builder()
                .nombre("Actividad cancelada UC37")
                .duracionMinutos(60)
                .estado(EstadoActividad.CANCELADA)
                .planificacion(planificacion)
                .build();
        em.persist(actividadCancelada);

        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilDirector)
                .rolProyecto(rol("DIRECTOR"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());
        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilMiembro1)
                .rolProyecto(rol("MIEMBRO"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());
        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilMiembro2)
                .rolProyecto(rol("MIEMBRO"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());

        em.flush();

        return new Datos(
                proyecto,
                planificacion,
                actividadFinalizada,
                actividadCancelada,
                director,
                perfilDirector,
                miembro1,
                perfilMiembro1,
                miembro2,
                perfilMiembro2);
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

    private Perfil perfil(String nombre, Usuario usuario, Profesion profesion) {
        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombre)
                .biografia("Perfil de integración UC-37")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private RolProyecto rol(String codigo) {
        return em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = :codigo",
                RolProyecto.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
    }

    private record Datos(
            Proyecto proyecto,
            Planificacion planificacion,
            Actividad actividadFinalizada,
            Actividad actividadCancelada,
            Usuario director,
            Perfil perfilDirector,
            Usuario miembro1,
            Perfil perfilMiembro1,
            Usuario miembro2,
            Perfil perfilMiembro2) {
    }
}
