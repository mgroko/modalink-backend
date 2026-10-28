package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.exception.NombreActividadDuplicadoException;
import org.mgroko.backend.actividades.exception.PredecesoraInvalidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrearActividadServiceTest {

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
    private CronogramaService cronogramaService;

    @InjectMocks
    private CrearActividadService crearActividadService;

    private Perfil perfil;
    private Planificacion planificacion;

    @BeforeEach
    void setUp() {
        perfil = Perfil.builder()
                .idPerfil(10L)
                .estado(EstadoPerfil.Activo)
                .build();

        Proyecto proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile Primavera")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2026, 11, 10))
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
    }

    @Test
    void crear_conDatosValidos_guardaActividadSinHorarioDefinitivo() {
        when(actividadRepository.existeNombreEnPlanificacion(50L, "Montaje"))
                .thenReturn(false);

        when(actividadRepository.saveAndFlush(any(Actividad.class)))
                .thenAnswer(invocation -> {
                    Actividad actividad = invocation.getArgument(0);
                    actividad.setIdActividad(55L);
                    return actividad;
                });
        when(cronogramaService.calcularProyeccion(any(Planificacion.class), any()))
                .thenReturn(new CalculoCpm(Map.of(), 0L, 600L, true, List.of()));

        CrearActividadRequest request = new CrearActividadRequest(
                "  Montaje  ",
                "Montar escenario",
                2,
                UnidadDuracion.HORAS,
                null,
                null,
                null);

        ActividadResponse response = crearActividadService.crear(1L, 10L, 100L, request);

        assertThat(response.idActividad()).isEqualTo(55L);
        assertThat(response.nombre()).isEqualTo("Montaje");
        assertThat(response.duracionMinutos()).isEqualTo(120);
        assertThat(response.fechaHoraInicio()).isNull();
        assertThat(response.fechaHoraFin()).isNull();
        assertThat(response.estado()).isEqualTo("PENDIENTE");
        assertThat(response.horarioPreferenciaInicio()).isNull();

        verify(proyectoSecurityService).validarPermiso(100L, 10L, "CREAR_ACTIVIDAD");
        verify(cronogramaService, never()).recalcular(any(Planificacion.class), any());
    }

    @Test
    void crear_nombreDuplicado_lanzaExcepcion() {
        when(actividadRepository.existeNombreEnPlanificacion(50L, "Montaje"))
                .thenReturn(true);

        CrearActividadRequest request = new CrearActividadRequest(
                "Montaje", null, 1, UnidadDuracion.HORAS, null, null, null);

        assertThatThrownBy(() -> crearActividadService.crear(1L, 10L, 100L, request))
                .isInstanceOf(NombreActividadDuplicadoException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_sinFechaEntrega_permiteHorarioPendiente() {
        planificacion.setFechaEntrega(null);
        when(actividadRepository.existeNombreEnPlanificacion(50L, "Montaje"))
                .thenReturn(false);
        when(actividadRepository.saveAndFlush(any(Actividad.class)))
                .thenAnswer(invocation -> {
                    Actividad actividad = invocation.getArgument(0);
                    actividad.setIdActividad(56L);
                    return actividad;
                });
        when(cronogramaService.calcularProyeccion(any(Planificacion.class), any()))
                .thenReturn(new CalculoCpm(Map.of(), 0L, null, null, List.of()));

        CrearActividadRequest request = new CrearActividadRequest(
                "Montaje", null, 1, UnidadDuracion.HORAS, null, null, null);

        assertThat(crearActividadService.crear(1L, 10L, 100L, request).fechaHoraInicio())
                .isNull();
        verify(actividadRepository).saveAndFlush(any());
    }

    @Test
    void crear_predecesoraDeOtraPlanificacion_lanzaExcepcion() {
        when(actividadRepository.existeNombreEnPlanificacion(50L, "Montaje"))
                .thenReturn(false);
        when(actividadRepository.findByIdsAndPlanificacion(any(), anyLong()))
                .thenReturn(List.of());

        CrearActividadRequest request = new CrearActividadRequest(
                "Montaje", null, 1, UnidadDuracion.HORAS, null,
                List.of(999L), null);

        assertThatThrownBy(() -> crearActividadService.crear(1L, 10L, 100L, request))
                .isInstanceOf(PredecesoraInvalidaException.class);

        verify(actividadRepository, never()).saveAndFlush(any());
    }
}
