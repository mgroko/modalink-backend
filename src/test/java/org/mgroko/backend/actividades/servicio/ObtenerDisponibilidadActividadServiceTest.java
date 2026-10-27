package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.exception.ActividadCanceladaException;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.calendario.dto.DetalleMiembroDisponibilidad;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObtenerDisponibilidadActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    @Mock
    private CalendarioService calendarioService;

    @InjectMocks
    private ObtenerDisponibilidadActividadService service;

    private Perfil perfilActivo;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().idUsuario(1L).build();
        perfilActivo = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .build();

        Proyecto proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile Primavera")
                .build();

        Planificacion planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2026, 11, 10))
                .build();

        actividad = Actividad.builder()
                .idActividad(55L)
                .nombre("Montaje")
                .duracionMinutos(60)
                .planificacion(planificacion)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfilActivo));
        when(planificacionRepository.findByProyectoIdProyecto(100L))
                .thenReturn(Optional.of(planificacion));
        when(actividadRepository.findByIdActividadAndPlanificacionIdPlanificacion(55L, 50L))
                .thenReturn(Optional.of(actividad));
    }

    @Test
    void obtener_sinPerfiles_evaluaPerfilActivo() {
        ResultadoDisponibilidad esperado = resultado(true);
        when(calendarioService.verificarDisponibilidadMiembros(List.of(1L), actividad))
                .thenReturn(esperado);

        ResultadoDisponibilidad resultado = service.obtener(1L, 10L, 100L, 55L, null);

        assertThat(resultado).isSameAs(esperado);
        verify(calendarioService).verificarDisponibilidadMiembros(List.of(1L), actividad);
    }

    @Test
    void obtener_conPerfiles_evaluaTodosYDeduplicaPorUsuario() {
        Perfil otroPerfilMismoUsuario = Perfil.builder()
                .idPerfil(11L)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder().idUsuario(1L).build())
                .build();
        Perfil otroUsuario = Perfil.builder()
                .idPerfil(12L)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder().idUsuario(2L).build())
                .build();

        when(perfilRepository.findById(11L)).thenReturn(Optional.of(otroPerfilMismoUsuario));
        when(perfilRepository.findById(12L)).thenReturn(Optional.of(otroUsuario));
        when(perfilRepository.findById(10L)).thenReturn(Optional.of(perfilActivo));
        when(proyectoVisibilidadService.esMiembroActivo(100L, 10L)).thenReturn(true);
        when(calendarioService.verificarDisponibilidadMiembros(List.of(1L, 2L), actividad))
                .thenReturn(resultado(true));

        service.obtener(1L, 10L, 100L, 55L, List.of(10L, 11L, 12L));

        verify(calendarioService).verificarDisponibilidadMiembros(List.of(1L, 2L), actividad);
    }

    @Test
    void obtener_consultaDeTercerosSinSerMiembro_lanzaAccesoDenegado() {
        Perfil otroUsuario = Perfil.builder()
                .idPerfil(12L)
                .estado(EstadoPerfil.Activo)
                .usuario(Usuario.builder().idUsuario(2L).build())
                .build();

        when(perfilRepository.findById(12L)).thenReturn(Optional.of(otroUsuario));
        when(proyectoVisibilidadService.esMiembroActivo(100L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.obtener(1L, 10L, 100L, 55L, List.of(12L)))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        verify(calendarioService, never()).verificarDisponibilidadMiembros(any(), any());
    }

    @Test
    void obtener_perfilSolicitadoEnBaja_lanzaPerfilEnBaja() {
        Perfil enBaja = Perfil.builder()
                .idPerfil(12L)
                .estado(EstadoPerfil.Baja)
                .usuario(Usuario.builder().idUsuario(2L).build())
                .build();
        when(perfilRepository.findById(12L)).thenReturn(Optional.of(enBaja));

        assertThatThrownBy(() -> service.obtener(1L, 10L, 100L, 55L, List.of(12L)))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void obtener_perfilSolicitadoInexistente_lanzaPerfilNoEncontrado() {
        when(perfilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(1L, 10L, 100L, 55L, List.of(99L)))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void obtener_actividadDeOtroProyecto_lanzaActividadNoEncontrada() {
        when(actividadRepository.findByIdActividadAndPlanificacionIdPlanificacion(55L, 50L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(1L, 10L, 100L, 55L, null))
                .isInstanceOf(ActividadNoEncontradaException.class);
    }

    @Test
    void obtener_actividadCancelada_lanzaActividadCancelada() {
        actividad.setEstado(org.mgroko.backend.modelo.enums.EstadoActividad.CANCELADA);

        assertThatThrownBy(() -> service.obtener(1L, 10L, 100L, 55L, null))
                .isInstanceOf(ActividadCanceladaException.class);
    }

    @Test
    void obtener_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        assertThatThrownBy(() -> service.obtener(1L, null, 100L, 55L, null))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    private ResultadoDisponibilidad resultado(boolean disponible) {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 1, 10, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 11, 1, 11, 0);
        return new ResultadoDisponibilidad(
                disponible,
                false,
                LocalDateTime.of(2026, 11, 1, 9, 0),
                LocalDateTime.of(2026, 11, 1, 18, 0),
                disponible ? inicio : null,
                disponible ? fin : null,
                disponible ? null : "SIN_HUECO",
                List.of(new DetalleMiembroDisponibilidad(
                        1L,
                        LocalDateTime.of(2026, 11, 1, 9, 0),
                        LocalDateTime.of(2026, 11, 1, 18, 0),
                        List.of(),
                        disponible,
                        disponible ? null : "SIN_HUECO")));
    }
}
