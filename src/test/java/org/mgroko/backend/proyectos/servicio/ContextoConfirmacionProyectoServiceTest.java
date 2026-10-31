package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.BloqueoConfirmacionResponse;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContextoConfirmacionProyectoServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL = 10L;
    private static final Long ID_PROYECTO = 100L;
    private static final Long ID_PLANIFICACION = 200L;
    private static final Long ID_ACTIVIDAD = 55L;
    private static final Long ID_REQUERIMIENTO = 123L;

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private RequerimientoCompletitudService requerimientoCompletitudService;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    private ContextoConfirmacionProyectoService service;

    private Proyecto proyecto;
    private Planificacion planificacion;
    private Actividad actividad;
    private RequerimientoActividad requerimiento;
    private MiembroProyecto miembro;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-01T12:00:00Z"),
                ZoneOffset.UTC);
        service = new ContextoConfirmacionProyectoService(
                perfilRepository,
                proyectoRepository,
                planificacionRepository,
                actividadRepository,
                asignacionActividadRepository,
                requerimientoActividadRepository,
                requerimientoCompletitudService,
                proyectoSecurityService,
                reloj);

        Perfil perfil = Perfil.builder()
                .idPerfil(ID_PERFIL)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder().idUsuario(ID_USUARIO).build())
                .build();

        proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Desfile")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        planificacion = Planificacion.builder()
                .idPlanificacion(ID_PLANIFICACION)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2026, 11, 10))
                .build();

        actividad = Actividad.builder()
                .idActividad(ID_ACTIVIDAD)
                .nombre("Montaje")
                .duracionMinutos(60)
                .planificacion(planificacion)
                .build();

        requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO)
                .actividad(actividad)
                .build();

        miembro = MiembroProyecto.builder()
                .idMiembro(900L)
                .perfil(perfil)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil));
        when(proyectoRepository.findById(ID_PROYECTO))
                .thenReturn(Optional.of(proyecto));
        when(planificacionRepository.findByProyectoIdProyecto(ID_PROYECTO))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(ID_PLANIFICACION))
                .thenReturn(List.of(actividad));
        when(asignacionActividadRepository.findActivasByActividadIds(List.of(ID_ACTIVIDAD)))
                .thenReturn(List.of(asignacion(null)));
        when(requerimientoActividadRepository.findConProfesionByActividadIds(
                List.of(ID_ACTIVIDAD)))
                .thenReturn(List.of(requerimiento));
        when(requerimientoCompletitudService.validar(eq(ID_PROYECTO), any()))
                .thenReturn(new RequerimientoCompletitudService.Resultado(
                        List.of(), List.of(), List.of()));
    }

    @Test
    void asignacionSinRequerimiento_noGeneraDatosInconsistentes() {
        ContextoConfirmacionProyectoService.Contexto contexto =
                service.cargar(ID_USUARIO, ID_PERFIL, ID_PROYECTO, false);

        assertThat(contexto.bloqueos())
                .extracting(BloqueoConfirmacionResponse::codigo)
                .doesNotContain("DATOS_INCONSISTENTES");
        assertThat(contexto.bloqueos()).isEmpty();
    }

    @Test
    void asignacionSinRequerimiento_conRequerimientoIncompleto_mantieneBloqueoDeCupo() {
        when(requerimientoCompletitudService.validar(eq(ID_PROYECTO), any()))
                .thenReturn(new RequerimientoCompletitudService.Resultado(
                        List.of(),
                        List.of(),
                        List.of(new BloqueoConfirmacionResponse(
                                "REQUERIMIENTOS_INCOMPLETOS",
                                "Falta un integrante.",
                                "REQUERIMIENTO_ACTIVIDAD",
                                ID_REQUERIMIENTO))));

        ContextoConfirmacionProyectoService.Contexto contexto =
                service.cargar(ID_USUARIO, ID_PERFIL, ID_PROYECTO, false);

        assertThat(contexto.bloqueos())
                .extracting(BloqueoConfirmacionResponse::codigo)
                .containsExactly("REQUERIMIENTOS_INCOMPLETOS");
    }

    @Test
    void asignacionConRequerimientoDeOtraActividad_sigueSiendoInconsistente() {
        RequerimientoActividad requerimientoAjeno = RequerimientoActividad.builder()
                .idRequerimiento(999L)
                .actividad(Actividad.builder().idActividad(777L).build())
                .build();

        when(asignacionActividadRepository.findActivasByActividadIds(List.of(ID_ACTIVIDAD)))
                .thenReturn(List.of(asignacion(requerimientoAjeno)));

        ContextoConfirmacionProyectoService.Contexto contexto =
                service.cargar(ID_USUARIO, ID_PERFIL, ID_PROYECTO, false);

        assertThat(contexto.bloqueos())
                .extracting(BloqueoConfirmacionResponse::codigo)
                .contains("DATOS_INCONSISTENTES");
    }

    private AsignacionActividad asignacion(RequerimientoActividad requerimientoActividad) {
        return AsignacionActividad.builder()
                .idAsignacionAct(1L)
                .miembro(miembro)
                .actividad(actividad)
                .requerimientoActividad(requerimientoActividad)
                .build();
    }
}
