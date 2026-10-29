package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.dto.ModificarActividadRequest;
import org.mgroko.backend.actividades.dto.ModificarRequerimientoActividadRequest;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.evento.ActividadModificadaEvent;
import org.mgroko.backend.actividades.exception.ActividadNoModificableException;
import org.mgroko.backend.actividades.exception.CicloActividadException;
import org.mgroko.backend.actividades.exception.NombreActividadDuplicadoException;
import org.mgroko.backend.actividades.exception.RequerimientoConVinculosException;
import org.mgroko.backend.actividades.exception.TransicionEstadoActividadInvalidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModificarActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @Mock
    private UbicacionService ubicacionService;

    @Mock
    private RequerimientoActividadService requerimientoActividadService;

    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;

    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;

    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;

    @Mock
    private CronogramaService cronogramaService;

    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ModificarActividadService modificarActividadService;

    private Perfil perfil;
    private Planificacion planificacion;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().idUsuario(1L).build();
        perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                .build();

        Proyecto proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile Primavera")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2026, 11, 30))
                .build();

        actividad = Actividad.builder()
                .idActividad(55L)
                .nombre("Montaje")
                .descripcion("Vieja")
                .duracionMinutos(60)
                .fechaHoraInicio(LocalDateTime.of(2026, 11, 1, 22, 59))
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findByIdActividadAndPlanificacionIdPlanificacion(55L, 50L))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.existeNombreEnPlanificacionExcluyendo(50L, "Montaje final", 55L))
                .thenReturn(false);
        when(actividadRepository.saveAndFlush(any(Actividad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(actividad));
        when(requerimientoActividadService.listarPorActividad(55L))
                .thenReturn(List.of());
        when(asignacionActividadRepository.findActivasByActividadId(55L))
                .thenReturn(List.of());
        // Fase 2: la respuesta se arma con el cálculo CPM, no con evaluar()
        when(cronogramaService.calcularProyeccion(any(), any()))
                .thenReturn(new CalculoCpm(Map.of(), 0L, 600L, true, List.of()));
        when(cronogramaService.estadoDe(any())).thenAnswer(invocation -> {
            CalculoCpm calculo = invocation.getArgument(0);
            if (calculo.viable() == null) {
                return org.mgroko.backend.modelo.enums.EstadoCronograma.SIN_FECHA_ENTREGA;
            }
            return Boolean.TRUE.equals(calculo.viable())
                    ? org.mgroko.backend.modelo.enums.EstadoCronograma.NORMAL
                    : org.mgroko.backend.modelo.enums.EstadoCronograma.ALERTA_DESBORDE;
        });
    }

    @Test
    void modificar_camposValidos_actualizaActividadYPublicaEvento() {
        AsignacionActividad asignacion = AsignacionActividad.builder()
                .miembro(MiembroProyecto.builder().perfil(perfil).build())
                .actividad(actividad)
                .build();
        when(asignacionActividadRepository.findActivasByActividadId(55L))
                .thenReturn(List.of(asignacion));

        ActividadResponse response = modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("  Montaje final  ", "Nueva descripcion", 2, EstadoActividad.EN_CURSO, null, null));

        assertThat(response.nombre()).isEqualTo("Montaje final");
        assertThat(response.descripcion()).isEqualTo("Nueva descripcion");
        assertThat(response.duracionMinutos()).isEqualTo(120);
        assertThat(response.estado()).isEqualTo("EN_CURSO");
        assertThat(response.estadoCronograma()).isEqualTo("NORMAL");
        assertThat(response.profesionalesAsignados()).singleElement().satisfies(profesional -> {
            assertThat(profesional.idPerfil()).isEqualTo(10L);
            assertThat(profesional.nombreArtistico()).isEqualTo("Luna");
            assertThat(profesional.nombreProfesion()).isEqualTo("Modelo");
        });
        verify(cronogramaService).calcularProyeccion(eq(planificacion), any());

        ArgumentCaptor<ActividadModificadaEvent> evento =
                ArgumentCaptor.forClass(ActividadModificadaEvent.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertThat(evento.getValue().idActividad()).isEqualTo(55L);
        assertThat(evento.getValue().nombreActividad()).isEqualTo("Montaje final");
        assertThat(evento.getValue().nombreProyecto()).isEqualTo("Desfile Primavera");
        assertThat(evento.getValue().idsPerfilesDestinatarios()).containsExactly(10L);
    }

    @Test
    void modificar_conDesborde_devuelveActividadPersistidaYAlerta() {
        when(cronogramaService.calcularProyeccion(planificacion, List.of(actividad)))
                .thenReturn(new CalculoCpm(Map.of(), 600L, 300L, false, List.of()));

        ActividadResponse response = modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null, null));

        assertThat(response.estadoCronograma()).isEqualTo("ALERTA_DESBORDE");
        verify(actividadRepository).saveAndFlush(actividad);
        verify(cronogramaService).calcularProyeccion(eq(planificacion), any());
    }

    @Test
    void modificar_actividadFinalizada_lanzaExcepcion() {
        actividad.setEstado(EstadoActividad.FINALIZADA);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.EN_CURSO, null, null)))
                .isInstanceOf(ActividadNoModificableException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_actividadCancelada_lanzaExcepcion() {
        actividad.setEstado(EstadoActividad.CANCELADA);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null, null)))
                .isInstanceOf(ActividadNoModificableException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_predecesoraQueGeneraCiclo_lanzaExcepcion() {
        Actividad sucesora = Actividad.builder()
                .idActividad(56L)
                .nombre("Sucesora")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
        sucesora.setPredecesoras(Set.of(actividad));

        when(actividadRepository.findByIdsAndPlanificacion(any(), eq(50L)))
                .thenReturn(List.of(sucesora));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(actividad, sucesora));

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE,
                        List.of(56L), null)))
                .isInstanceOf(CicloActividadException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_siTriggerDetectaCiclo_lanzaExcepcionDeCiclo() {
        when(actividadRepository.saveAndFlush(any(Actividad.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Error de integridad",
                        new RuntimeException(
                                "Dependencia invalida: la actividad 55 genera un ciclo")));

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null, null)))
                .isInstanceOf(CicloActividadException.class);

        verify(cronogramaService, never()).recalcular(any(), any());
    }

    @Test
    void modificar_sinPermisoEnProyectoNoModificable_priorizaAccesoDenegado() {
        planificacion.getProyecto().setEstado(EstadoProyecto.FINALIZADO);
        doThrow(new AccesoDenegadoProyectoException("No tienes permiso."))
                .when(proyectoSecurityService)
                .validarPermiso(
                        100L,
                        10L,
                        ModificarActividadService.PERMISO_MODIFICAR_ACTIVIDAD);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null, null)))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        verify(actividadRepository, never())
                .findByIdActividadAndPlanificacionIdPlanificacion(any(), any());
    }

    @Test
    void modificar_nombreDuplicado_lanzaExcepcion() {
        when(actividadRepository.existeNombreEnPlanificacionExcluyendo(
                50L, "Montaje final", 55L)).thenReturn(true);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null, null)))
                .isInstanceOf(NombreActividadDuplicadoException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_requerimientoConPostulacion_lanzaExcepcion() {
        RequerimientoActividad requerimiento = crearRequerimiento();
        when(requerimientoActividadRepository.findByActividad_IdActividad(55L))
                .thenReturn(List.of(requerimiento));
        when(postulacionActividadRepository
                .existsByRequerimiento_IdRequerimiento(7L))
                .thenReturn(true);

        ModificarRequerimientoActividadRequest requerimientoRequest =
                new ModificarRequerimientoActividadRequest(
                        7L, 1, 2L, null, List.of(), List.of());

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null,
                        List.of(requerimientoRequest))))
                .isInstanceOf(RequerimientoConVinculosException.class)
                .hasMessageContaining("postulaciones o invitaciones");

        verify(requerimientoActividadService, never()).actualizar(any(), any());
        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_requerimientoConInvitacion_lanzaExcepcion() {
        RequerimientoActividad requerimiento = crearRequerimiento();
        when(requerimientoActividadRepository.findByActividad_IdActividad(55L))
                .thenReturn(List.of(requerimiento));
        when(postulacionActividadRepository
                .existsByRequerimiento_IdRequerimiento(7L))
                .thenReturn(false);
        when(invitacionActividadRepository
                .existsByRequerimiento_IdRequerimiento(7L))
                .thenReturn(true);

        ModificarRequerimientoActividadRequest requerimientoRequest =
                new ModificarRequerimientoActividadRequest(
                        7L, 1, 2L, null, List.of(), List.of());

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null,
                        List.of(requerimientoRequest))))
                .isInstanceOf(RequerimientoConVinculosException.class);

        verify(requerimientoActividadService, never()).actualizar(any(), any());
        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_predecesorasNull_lanzaExcepcion() {
        ModificarActividadRequest request = new ModificarActividadRequest(
                "Montaje final", null, 1, UnidadDuracion.HORAS, null,
                null, List.of(), EstadoActividad.PENDIENTE);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L, request))
                .isInstanceOf(org.mgroko.backend.actividades.exception.PredecesoraInvalidaException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_requerimientosNull_lanzaExcepcion() {
        ModificarActividadRequest request = new ModificarActividadRequest(
                "Montaje final", null, 1, UnidadDuracion.HORAS, null,
                List.of(), null, EstadoActividad.PENDIENTE);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L, request))
                .isInstanceOf(org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_ubicacionNull_limpiaUbicacion() {
        actividad.setUbicacion(Ubicacion.builder().build());

        ActividadResponse response = modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null, null));

        assertThat(actividad.getUbicacion()).isNull();
        assertThat(response.ubicacion()).isNull();
    }

    @Test
    void modificar_desdePendienteAFinalizada_lanzaExcepcionDeTransicion() {
        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.FINALIZADA, null, null)))
                .isInstanceOf(TransicionEstadoActividadInvalidaException.class)
                .hasMessageContaining("PENDIENTE")
                .hasMessageContaining("FINALIZADA");

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_desdeEnCursoAPendiente_lanzaExcepcionDeTransicion() {
        actividad.setEstado(EstadoActividad.EN_CURSO);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null, null)))
                .isInstanceOf(TransicionEstadoActividadInvalidaException.class)
                .hasMessageContaining("EN_CURSO")
                .hasMessageContaining("PENDIENTE");

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_desdePendienteACancelada_permiteCambioDeEstado() {
        ActividadResponse response = modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.CANCELADA, null, null));

        assertThat(response.estado()).isEqualTo("CANCELADA");
        verify(actividadRepository).saveAndFlush(actividad);
    }

    @Test
    void modificar_alCancelar_reconectaSucesorasAPredecesoras() {
        Actividad predecesora = Actividad.builder()
                .idActividad(1L)
                .nombre("Bocetos")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();

        Actividad sucesora = Actividad.builder()
                .idActividad(60L)
                .nombre("Fitting")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .predecesoras(new HashSet<>(Set.of(actividad)))
                .build();

        when(actividadRepository.findByIdsAndPlanificacion(any(), eq(50L)))
                .thenReturn(List.of(predecesora));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(predecesora, actividad, sucesora));

        modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.CANCELADA,
                        List.of(1L), null));

        assertThat(sucesora.getPredecesoras())
                .extracting(Actividad::getIdActividad)
                .containsExactly(1L);
        verify(actividadRepository).flush();
    }

    @Test
    void modificar_proyectoConfirmadoConCambioDeDuracion_lanzaExcepcion() {
        planificacion.getProyecto().setEstado(EstadoProyecto.CONFIRMADO);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 2, EstadoActividad.PENDIENTE, null, null)))
                .isInstanceOf(org.mgroko.backend.actividades.exception
                        .EstadoProyectoNoPermiteModificarActividadesException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_desdeEnCursoAFinalizada_permiteCambioDeEstado() {
        actividad.setEstado(EstadoActividad.EN_CURSO);

        ActividadResponse response = modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.FINALIZADA, null, null));

        assertThat(response.estado()).isEqualTo("FINALIZADA");
        verify(actividadRepository).saveAndFlush(actividad);
    }

    @Test
    void modificar_requerimientoConVinculosOmitido_lanzaExcepcion() {
        RequerimientoActividad requerimiento = crearRequerimiento();
        when(requerimientoActividadRepository.findByActividad_IdActividad(55L))
                .thenReturn(List.of(requerimiento));
        when(postulacionActividadRepository
                .existsByRequerimiento_IdRequerimiento(7L))
                .thenReturn(true);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null,
                        List.of())))
                .isInstanceOf(RequerimientoConVinculosException.class);

        verify(requerimientoActividadService, never()).eliminar(requerimiento);
        verify(requerimientoActividadService, never()).actualizar(any(), any());
        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_requerimientoSinVinculosOmitido_loElimina() {
        RequerimientoActividad requerimiento = crearRequerimiento();
        when(requerimientoActividadRepository.findByActividad_IdActividad(55L))
                .thenReturn(List.of(requerimiento));
        when(postulacionActividadRepository
                .existsByRequerimiento_IdRequerimiento(7L))
                .thenReturn(false);
        when(invitacionActividadRepository
                .existsByRequerimiento_IdRequerimiento(7L))
                .thenReturn(false);

        modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null,
                        List.of()));

        verify(requerimientoActividadService).eliminar(requerimiento);
        verify(requerimientoActividadService, never()).actualizar(any(), any());
    }

    @Test
    void modificar_requerimientoNuevo_loPersiste() {
        ModificarRequerimientoActividadRequest requerimientoRequest =
                new ModificarRequerimientoActividadRequest(
                        null, 2, 2L, "Dos modelos", List.of(), List.of());

        modificarActividadService.modificar(
                1L, 10L, 100L, 55L,
                request("Montaje final", null, 1, EstadoActividad.PENDIENTE, null,
                        List.of(requerimientoRequest)));

        verify(requerimientoActividadService).persistir(
                eq(actividad), any(CrearRequerimientoActividadRequest.class));
    }

    private RequerimientoActividad crearRequerimiento() {
        return RequerimientoActividad.builder()
                .idRequerimiento(7L)
                .actividad(actividad)
                .cantidad(1)
                .build();
    }

    private ModificarActividadRequest request(
            String nombre,
            String descripcion,
            int duracion,
            EstadoActividad estado,
            List<Long> predecesoras,
            List<ModificarRequerimientoActividadRequest> requerimientos) {

        return new ModificarActividadRequest(
                nombre,
                descripcion,
                duracion,
                UnidadDuracion.HORAS,
                null,
                predecesoras != null ? predecesoras : List.of(),
                requerimientos != null ? requerimientos : List.of(),
                estado);
    }
}
