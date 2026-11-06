package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.MiembroExclusivoResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MiembrosActividadServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTOR = 10L;
    private static final Long ID_PROYECTO = 100L;
    private static final Long ID_PLANIFICACION = 50L;
    private static final Long ID_ACTIVIDAD = 55L;

    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private PlanificacionRepository planificacionRepository;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    private MiembrosActividadService service;

    private Perfil perfilActor;
    private Proyecto proyecto;
    private Planificacion planificacion;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        service = new MiembrosActividadService(
                asignacionActividadRepository,
                actividadRepository,
                planificacionRepository,
                perfilRepository,
                miembroProyectoRepository,
                proyectoSecurityService);

        perfilActor = perfil(10L, 1L, "Directora");
        proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Desfile")
                .estado(EstadoProyecto.PUBLICADO)
                .build();
        planificacion = Planificacion.builder()
                .idPlanificacion(ID_PLANIFICACION)
                .proyecto(proyecto)
                .build();
        actividad = Actividad.builder()
                .idActividad(ID_ACTIVIDAD)
                .nombre("Montaje")
                .planificacion(planificacion)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTOR, ID_USUARIO))
                .thenReturn(Optional.of(perfilActor));
        when(planificacionRepository.findByProyectoIdProyecto(ID_PROYECTO))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findByIdActividadAndPlanificacionIdPlanificacion(
                ID_ACTIVIDAD, ID_PLANIFICACION))
                .thenReturn(Optional.of(actividad));
        when(asignacionActividadRepository.findExclusivasByActividadIdAndProyecto(
                ID_ACTIVIDAD, ID_PROYECTO))
                .thenReturn(List.of());
    }

    @Test
    void listarExclusivos_mapeaLosDatosYHabilitaEliminacion() {
        AsignacionActividad asignacion = asignacion(
                perfil(11L, 2L, "Luna"), rol("MIEMBRO"));

        when(asignacionActividadRepository.findExclusivasByActividadIdAndProyecto(
                ID_ACTIVIDAD, ID_PROYECTO))
                .thenReturn(List.of(asignacion));

        List<MiembroExclusivoResponse> respuesta = service.listarExclusivos(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD);

        assertThat(respuesta).singleElement().satisfies(miembro -> {
            assertThat(miembro.idAsignacionActividad()).isEqualTo(30L);
            assertThat(miembro.idMiembro()).isEqualTo(20L);
            assertThat(miembro.idPerfil()).isEqualTo(11L);
            assertThat(miembro.idUsuario()).isEqualTo(2L);
            assertThat(miembro.nombreArtistico()).isEqualTo("Luna");
            assertThat(miembro.idProfesion()).isEqualTo(2L);
            assertThat(miembro.nombreProfesion()).isEqualTo("Modelo");
            assertThat(miembro.puedeEliminar()).isTrue();
            assertThat(miembro.motivoNoEliminable()).isNull();
        });
    }

    @Test
    void listarExclusivos_marcaAutoeliminacion() {
        AsignacionActividad asignacion = asignacion(perfilActor, rol("MIEMBRO"));

        when(asignacionActividadRepository.findExclusivasByActividadIdAndProyecto(
                ID_ACTIVIDAD, ID_PROYECTO))
                .thenReturn(List.of(asignacion));

        List<MiembroExclusivoResponse> respuesta = service.listarExclusivos(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD);

        assertThat(respuesta).singleElement().satisfies(miembro -> {
            assertThat(miembro.puedeEliminar()).isFalse();
            assertThat(miembro.motivoNoEliminable()).isEqualTo("AUTOELIMINACION");
        });
    }

    @Test
    void listarExclusivos_marcaUltimoDirector() {
        AsignacionActividad asignacion = asignacion(
                perfil(11L, 2L, "Luna"), rol("DIRECTOR"));

        when(asignacionActividadRepository.findExclusivasByActividadIdAndProyecto(
                ID_ACTIVIDAD, ID_PROYECTO))
                .thenReturn(List.of(asignacion));
        when(miembroProyectoRepository.countOtrosDirectoresActivos(ID_PROYECTO, 20L))
                .thenReturn(0L);

        List<MiembroExclusivoResponse> respuesta = service.listarExclusivos(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD);

        assertThat(respuesta).singleElement().satisfies(miembro -> {
            assertThat(miembro.puedeEliminar()).isFalse();
            assertThat(miembro.motivoNoEliminable()).isEqualTo("ULTIMO_DIRECTOR");
        });
    }

    @Test
    void listarExclusivos_marcaProyectoBorrador() {
        proyecto.setEstado(EstadoProyecto.BORRADOR);
        AsignacionActividad asignacion = asignacion(
                perfil(11L, 2L, "Luna"), rol("MIEMBRO"));

        when(asignacionActividadRepository.findExclusivasByActividadIdAndProyecto(
                ID_ACTIVIDAD, ID_PROYECTO))
                .thenReturn(List.of(asignacion));

        List<MiembroExclusivoResponse> respuesta = service.listarExclusivos(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD);

        assertThat(respuesta).singleElement().satisfies(miembro -> {
            assertThat(miembro.puedeEliminar()).isFalse();
            assertThat(miembro.motivoNoEliminable()).isEqualTo("PROYECTO_BORRADOR");
        });
    }

    @Test
    void listarExclusivos_sinAsignaciones_devuelveListaVacia() {
        assertThat(service.listarExclusivos(
                ID_USUARIO, ID_PERFIL_ACTOR, ID_PROYECTO, ID_ACTIVIDAD)).isEmpty();
    }

    @Test
    void guardarMiembro_eliminaLaAsignacionDelPerfil() {
        service.guardarMiembro(ID_ACTIVIDAD, 10L);

        verify(asignacionActividadRepository)
                .deleteByActividadIdAndPerfil(ID_ACTIVIDAD, 10L);
    }

    private Perfil perfil(Long idPerfil, Long idUsuario, String nombre) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombre)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder().idUsuario(idUsuario).build())
                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                .build();
    }

    private RolProyecto rol(String codigo) {
        return RolProyecto.builder().codigo(codigo).nombre(codigo).build();
    }

    private AsignacionActividad asignacion(Perfil perfil, RolProyecto rol) {
        return AsignacionActividad.builder()
                .idAsignacionAct(30L)
                .miembro(MiembroProyecto.builder()
                        .idMiembro(20L)
                        .perfil(perfil)
                        .rolProyecto(rol)
                        .build())
                .actividad(actividad)
                .build();
    }
}
