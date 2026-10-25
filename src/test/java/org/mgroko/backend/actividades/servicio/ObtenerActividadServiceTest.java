package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoCronograma;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObtenerActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @Mock
    private RequerimientoActividadService requerimientoActividadService;

    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;

    @Mock
    private CronogramaService cronogramaService;

    @InjectMocks
    private ObtenerActividadService obtenerActividadService;

    private Actividad actividad;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().idUsuario(1L).build();
        Perfil perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                .build();

        Proyecto proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile Primavera")
                .build();

        Planificacion planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .build();

        actividad = Actividad.builder()
                .idActividad(55L)
                .nombre("Montaje")
                .descripcion("Montar escenario")
                .duracionMinutos(60)
                .fechaHoraInicio(LocalDateTime.of(2026, 11, 1, 22, 59))
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();

        var asignacion = org.mgroko.backend.modelo.AsignacionActividad.builder()
                .idAsignacionAct(77L)
                .miembro(MiembroProyecto.builder().idMiembro(88L).perfil(perfil).build())
                .actividad(actividad)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findByIdActividadAndPlanificacionIdPlanificacion(55L, 50L))
                .thenReturn(Optional.of(actividad));
        when(requerimientoActividadService.listarPorActividad(55L))
                .thenReturn(List.of());
        when(asignacionActividadRepository.findActivasByActividadId(55L))
                .thenReturn(List.of(asignacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(actividad));
        when(cronogramaService.evaluar(any(), any()))
                .thenReturn(EstadoCronograma.NORMAL);
    }

    @Test
    void obtener_conPermiso_devuelveDetalleConProfesionales() {
        ActividadResponse response = obtenerActividadService.obtener(1L, 10L, 100L, 55L);

        assertThat(response.idActividad()).isEqualTo(55L);
        assertThat(response.estadoCronograma()).isEqualTo("NORMAL");
        assertThat(response.profesionalesAsignados()).singleElement().satisfies(profesional -> {
            assertThat(profesional.idPerfil()).isEqualTo(10L);
            assertThat(profesional.nombreArtistico()).isEqualTo("Luna");
            assertThat(profesional.nombreProfesion()).isEqualTo("Modelo");
        });
        verify(proyectoSecurityService).validarPermiso(
                100L, 10L, ModificarActividadService.PERMISO_MODIFICAR_ACTIVIDAD);
    }
}
