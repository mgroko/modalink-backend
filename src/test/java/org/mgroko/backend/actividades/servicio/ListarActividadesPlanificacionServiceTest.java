package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.ActividadPlanificacionResponse;
import org.mgroko.backend.actividades.dto.BuscarActividadesFiltro;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListarActividadesPlanificacionServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;

    @InjectMocks
    private ListarActividadesPlanificacionService service;

    private Perfil perfil;
    private Planificacion planificacion;
    private Actividad primera;
    private Actividad segunda;

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
                .build();

        planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .build();

        primera = Actividad.builder()
                .idActividad(1L)
                .nombre("Bocetos")
                .descripcion("Bocetos")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();

        segunda = Actividad.builder()
                .idActividad(2L)
                .nombre("Fitting")
                .descripcion("Fitting")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .predecesoras(new HashSet<>(Set.of(primera)))
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findConPredecesorasByPlanificacion(50L))
                .thenReturn(List.of(primera, segunda));
        when(actividadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(segunda, primera)));
        when(actividadRepository.findConPredecesoras(anyCollection()))
                .thenReturn(List.of(segunda));
        when(asignacionActividadRepository.findActivasByActividadIds(anyCollection()))
                .thenReturn(List.of(asignacion(segunda)));
    }

    @Test
    void listar_devuelveFilasConResponsablesYPredecesoras() {
        PaginaResponse<ActividadPlanificacionResponse> pagina = service.listar(
                1L, 10L, 100L, new BuscarActividadesFiltro(null, null, null, null, null, null),
                0, 20, true);

        assertThat(pagina.contenido()).hasSize(2);
        assertThat(pagina.contenido().get(0).idActividad()).isEqualTo(1L);
        assertThat(pagina.contenido().get(0).responsables()).isEmpty();
        assertThat(pagina.contenido().get(1).idActividad()).isEqualTo(2L);
        assertThat(pagina.contenido().get(1).predecesoras())
                .singleElement()
                .satisfies(predecesora -> assertThat(predecesora.idActividad()).isEqualTo(1L));
        assertThat(pagina.contenido().get(1).responsables())
                .singleElement()
                .satisfies(responsable -> {
                    assertThat(responsable.idPerfil()).isEqualTo(10L);
                    assertThat(responsable.nombreArtistico()).isEqualTo("Luna");
                });
    }

    @Test
    void listar_paginaEnMemoriaManteniendoElTotal() {
        PaginaResponse<ActividadPlanificacionResponse> pagina = service.listar(
                1L, 10L, 100L, null, 0, 1, false);

        assertThat(pagina.contenido()).hasSize(1);
        assertThat(pagina.totalElementos()).isEqualTo(2);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
    }

    @Test
    void listar_incluyeActividadesCanceladas() {
        primera.setEstado(EstadoActividad.CANCELADA);

        PaginaResponse<ActividadPlanificacionResponse> pagina = service.listar(
                1L, 10L, 100L, null, 0, 20, true);

        assertThat(pagina.contenido())
                .extracting(ActividadPlanificacionResponse::estado)
                .containsExactly("CANCELADA", "PENDIENTE");
    }

    @Test
    void listar_validaPermisoDeLectura() {
        service.listar(1L, 10L, 100L, null, 0, 20, true);

        verify(proyectoSecurityService).validarPermiso(
                100L, 10L, ProyectoSecurityService.PERMISO_VER_PROYECTO);
    }

    @Test
    void listar_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        assertThatThrownBy(() -> service.listar(1L, null, 100L, null, 0, 20, true))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    private AsignacionActividad asignacion(Actividad actividad) {
        MiembroProyecto miembro = MiembroProyecto.builder()
                .idMiembro(88L)
                .perfil(perfil)
                .build();

        return AsignacionActividad.builder()
                .idAsignacionAct(77L)
                .miembro(miembro)
                .actividad(actividad)
                .build();
    }
}
