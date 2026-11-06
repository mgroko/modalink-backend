package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.CalificacionPerfilRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContextoFinalizacionProyectoServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTOR = 10L;
    private static final Long ID_PERFIL_MIEMBRO = 20L;
    private static final Long ID_PERFIL_OTRO_DIRECTOR = 30L;
    private static final Long ID_PROYECTO = 100L;
    private static final Long ID_PLANIFICACION = 200L;
    private static final Long ID_ACTIVIDAD_FINALIZADA = 55L;
    private static final Long ID_ACTIVIDAD_CANCELADA = 56L;

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private CalificacionPerfilRepository calificacionPerfilRepository;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    private ContextoFinalizacionProyectoService service;

    private Perfil perfilActor;
    private Proyecto proyecto;
    private Planificacion planificacion;
    private Actividad actividadFinalizada;
    private Actividad actividadCancelada;
    private MiembroProyecto miembroActor;
    private MiembroProyecto miembroMiembro;
    private MiembroProyecto miembroOtroDirector;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-01T12:00:00Z"),
                ZoneOffset.UTC);
        service = new ContextoFinalizacionProyectoService(
                perfilRepository,
                proyectoRepository,
                planificacionRepository,
                actividadRepository,
                miembroProyectoRepository,
                calificacionPerfilRepository,
                proyectoSecurityService,
                reloj);

        perfilActor = perfil(ID_PERFIL_ACTOR, ID_USUARIO, "Dirección");
        Perfil perfilMiembro = perfil(ID_PERFIL_MIEMBRO, 2L, "Miembro");
        Perfil perfilOtroDirector = perfil(ID_PERFIL_OTRO_DIRECTOR, 3L, "Otro director");

        proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Desfile")
                .estado(EstadoProyecto.CONFIRMADO)
                .build();

        planificacion = Planificacion.builder()
                .idPlanificacion(ID_PLANIFICACION)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2026, 11, 1))
                .build();

        actividadFinalizada = actividad(
                ID_ACTIVIDAD_FINALIZADA, "Montaje", EstadoActividad.FINALIZADA);
        actividadCancelada = actividad(
                ID_ACTIVIDAD_CANCELADA, "Decoración", EstadoActividad.CANCELADA);

        miembroActor = miembro(perfilActor, RolProyectoUsuario.DIRECTOR);
        miembroMiembro = miembro(perfilMiembro, RolProyectoUsuario.MIEMBRO);
        miembroOtroDirector = miembro(
                perfilOtroDirector, RolProyectoUsuario.DIRECTOR);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(
                ID_PERFIL_ACTOR, ID_USUARIO)).thenReturn(Optional.of(perfilActor));
        when(proyectoRepository.findById(ID_PROYECTO))
                .thenReturn(Optional.of(proyecto));
        when(proyectoRepository.findByIdParaFinalizar(ID_PROYECTO))
                .thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyecto(ID_PROYECTO))
                .thenReturn(Optional.of(planificacion));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(ID_PROYECTO))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findByPlanificacionProyectoIdProyecto(ID_PROYECTO))
                .thenReturn(List.of(actividadFinalizada, actividadCancelada));
        when(miembroProyectoRepository.buscarActivosDeProyectoConPerfilProfesionYRol(
                ID_PROYECTO)).thenReturn(
                        List.of(miembroActor, miembroMiembro, miembroOtroDirector));
        when(calificacionPerfilRepository
                .findByProyectoIdProyectoAndPerfilEmisorIdPerfil(
                        ID_PROYECTO, ID_PERFIL_ACTOR))
                .thenReturn(List.of());
    }

    @Test
    void cargar_confirmadoConFechaAlcanzadaYActividadesFinalizadasOCanceladas_sinBloqueos() {
        ContextoFinalizacionProyectoService.Contexto contexto = cargarSinLock();

        assertThat(contexto.bloqueos()).isEmpty();
        assertThat(contexto.advertencias()).isEmpty();
        assertThat(contexto.requiereConfirmacionPrematura()).isFalse();
        assertThat(contexto.diasAnticipacion()).isZero();
        assertThat(contexto.proyecto()).isSameAs(proyecto);
        assertThat(contexto.planificacion()).isSameAs(planificacion);
        assertThat(contexto.ahora())
                .isEqualTo(LocalDateTime.of(2026, 11, 1, 12, 0));
    }

    @Test
    void cargar_proyectoPublicado_agregaBloqueoDeEstado() {
        proyecto.setEstado(EstadoProyecto.PUBLICADO);

        ContextoFinalizacionProyectoService.Contexto contexto = cargarSinLock();

        assertThat(contexto.bloqueos())
                .extracting(bloqueo -> bloqueo.codigo())
                .contains("ESTADO_PROYECTO_INVALIDO");
    }

    @Test
    void cargar_proyectoFinalizado_agregaBloqueoDeEstado() {
        proyecto.setEstado(EstadoProyecto.FINALIZADO);

        ContextoFinalizacionProyectoService.Contexto contexto = cargarSinLock();

        assertThat(contexto.bloqueos())
                .extracting(bloqueo -> bloqueo.codigo())
                .contains("PROYECTO_YA_FINALIZADO");
    }

    @Test
    void cargar_fechaEntregaFutura_agregaAdvertenciaYRequiereConfirmacion() {
        planificacion.setFechaEntrega(LocalDate.of(2026, 11, 2));

        ContextoFinalizacionProyectoService.Contexto contexto = cargarSinLock();

        assertThat(contexto.bloqueos()).isEmpty();
        assertThat(contexto.advertencias())
                .extracting(advertencia -> advertencia.codigo())
                .contains("FECHA_ENTREGA_FUTURA");
        assertThat(contexto.requiereConfirmacionPrematura()).isTrue();
        assertThat(contexto.diasAnticipacion()).isEqualTo(1);
    }

    @Test
    void cargar_fechaEntregaNula_agregaBloqueo() {
        planificacion.setFechaEntrega(null);

        ContextoFinalizacionProyectoService.Contexto contexto = cargarSinLock();

        assertThat(contexto.bloqueos())
                .extracting(bloqueo -> bloqueo.codigo())
                .contains("FECHA_ENTREGA_NO_DEFINIDA");
    }

    @Test
    void cargar_actividadPendiente_agregaBloqueoConActividad() {
        actividadFinalizada.setEstado(EstadoActividad.PENDIENTE);

        ContextoFinalizacionProyectoService.Contexto contexto = cargarSinLock();

        assertThat(contexto.bloqueos())
                .extracting(bloqueo -> bloqueo.codigo())
                .contains("ACTIVIDAD_NO_FINALIZADA");
        assertThat(contexto.bloqueos())
                .filteredOn(bloqueo -> "ACTIVIDAD_NO_FINALIZADA".equals(
                        bloqueo.codigo()))
                .extracting(bloqueo -> bloqueo.entidadId())
                .contains(ID_ACTIVIDAD_FINALIZADA);
    }

    @Test
    void cargar_actividadEnCurso_agregaBloqueo() {
        actividadFinalizada.setEstado(EstadoActividad.EN_CURSO);

        ContextoFinalizacionProyectoService.Contexto contexto = cargarSinLock();

        assertThat(contexto.bloqueos())
                .extracting(bloqueo -> bloqueo.codigo())
                .contains("ACTIVIDAD_NO_FINALIZADA");
    }

    @Test
    void cargar_sinActividades_agregaBloqueo() {
        when(actividadRepository.findByPlanificacionProyectoIdProyecto(ID_PROYECTO))
                .thenReturn(List.of());

        ContextoFinalizacionProyectoService.Contexto contexto = cargarSinLock();

        assertThat(contexto.bloqueos())
                .extracting(bloqueo -> bloqueo.codigo())
                .contains("SIN_ACTIVIDADES");
    }

    @Test
    void cargar_sinPlanificacion_agregaBloqueo() {
        when(planificacionRepository.findByProyectoIdProyecto(ID_PROYECTO))
                .thenReturn(Optional.empty());

        ContextoFinalizacionProyectoService.Contexto contexto = cargarSinLock();

        assertThat(contexto.bloqueos())
                .extracting(bloqueo -> bloqueo.codigo())
                .contains("PLANIFICACION_NO_ENCONTRADA");
    }

    @Test
    void cargar_listaMiembrosExcluyeAlActorEIndicaCalificacionesExistentes() {
        CalificacionPerfil existente = CalificacionPerfil.builder()
                .proyecto(proyecto)
                .perfilEmisor(perfilActor)
                .perfilReceptor(miembroMiembro.getPerfil())
                .valoracion(5)
                .resenia("Excelente")
                .build();
        when(calificacionPerfilRepository
                .findByProyectoIdProyectoAndPerfilEmisorIdPerfil(
                        ID_PROYECTO, ID_PERFIL_ACTOR))
                .thenReturn(List.of(existente));

        ContextoFinalizacionProyectoService.Contexto contexto = cargarSinLock();

        assertThat(contexto.miembrosACalificar())
                .extracting(miembro -> miembro.idPerfil())
                .containsExactly(ID_PERFIL_MIEMBRO, ID_PERFIL_OTRO_DIRECTOR);
        assertThat(contexto.miembrosACalificar())
                .filteredOn(miembro -> miembro.idPerfil().equals(ID_PERFIL_MIEMBRO))
                .extracting(miembro -> miembro.yaCalificado())
                .containsExactly(true);
        assertThat(contexto.miembrosACalificar())
                .filteredOn(miembro -> miembro.idPerfil().equals(ID_PERFIL_OTRO_DIRECTOR))
                .extracting(miembro -> miembro.yaCalificado())
                .containsExactly(false);
    }

    @Test
    void cargar_perfilInactivo_lanzaPerfilEnBaja() {
        perfilActor.setEstado(EstadoPerfil.Baja);

        assertThatThrownBy(this::cargarSinLock)
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void cargar_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        assertThatThrownBy(() -> service.cargar(
                ID_USUARIO, null, ID_PROYECTO, false))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    private ContextoFinalizacionProyectoService.Contexto cargarSinLock() {
        return service.cargar(ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, false);
    }

    private Perfil perfil(Long idPerfil, Long idUsuario, String nombreArtistico) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombreArtistico)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder()
                        .idUsuario(idUsuario)
                        .nombre("Nombre " + idUsuario)
                        .apellido("Apellido " + idUsuario)
                        .build())
                .build();
    }

    private Actividad actividad(Long id, String nombre, EstadoActividad estado) {
        return Actividad.builder()
                .idActividad(id)
                .nombre(nombre)
                .estado(estado)
                .duracionMinutos(60)
                .planificacion(planificacion)
                .build();
    }

    private MiembroProyecto miembro(
            Perfil perfil,
            RolProyectoUsuario rol) {
        return MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(RolProyecto.builder()
                        .codigo(rol.name())
                        .nombre(rol == RolProyectoUsuario.DIRECTOR
                                ? "Director de proyecto"
                                : "Miembro de proyecto")
                        .build())
                .estadoParticipacion(
                        org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO)
                .build();
    }
}
