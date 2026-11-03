package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.ActividadReprogramadaResponse;
import org.mgroko.backend.proyectos.dto.ReprogramacionProyectoResponse;
import org.mgroko.backend.proyectos.dto.ResumenReprogramacionResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JornadaReprogramacionServiceTest {

    private static final LocalDateTime ANTERIOR =
            LocalDateTime.of(2026, 11, 5, 9, 0);
    private static final LocalDateTime NUEVO =
            LocalDateTime.of(2026, 11, 5, 10, 0);

    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private ReprogramarProyectoService reprogramarProyectoService;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    private JornadaReprogramacionService service;

    @BeforeEach
    void setUp() {
        service = new JornadaReprogramacionService(
                asignacionActividadRepository,
                reprogramarProyectoService,
                proyectoRepository,
                miembroProyectoRepository,
                notificacionService,
                perfilRepository,
                proyectoSecurityService,
                true);
        lenient().when(notificacionService.crear(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Notificacion.builder().build());
    }

    @Test
    void reprogramarAfectados_unExitoYUnFallo_devuelveResumenYNotifica() {
        when(asignacionActividadRepository.findProyectosConfirmadosDeUsuario(1L))
                .thenReturn(List.of(100L, 101L));
        when(proyectoRepository.findAllById(List.of(100L, 101L)))
                .thenReturn(List.of(
                        proyecto(100L, "Desfile Primavera"),
                        proyecto(101L, "Campaña Otoño")));

        ReprogramacionProyectoResponse exito = resultadoExitoso(100L, "Desfile Primavera");
        when(reprogramarProyectoService.reprogramar(
                100L, MotivoReprogramacion.CAMBIO_JORNADA, null))
                .thenReturn(exito);
        when(reprogramarProyectoService.reprogramar(
                101L, MotivoReprogramacion.CAMBIO_JORNADA, null))
                .thenThrow(new ProyectoNoConfirmableException(
                        "SIN_DISPONIBILIDAD",
                        "Sin hueco común."));

        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(100L)))
                .thenReturn(List.of(director(10L)));
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(101L)))
                .thenReturn(List.of(director(10L)));

        ResumenReprogramacionResponse resumen = service.reprogramarAfectados(1L);

        assertThat(resumen.totalProyectos()).isEqualTo(2);
        assertThat(resumen.exitosos()).hasSize(1);
        assertThat(resumen.exitosos().get(0).idProyecto()).isEqualTo(100L);
        assertThat(resumen.fallidos()).hasSize(1);
        assertThat(resumen.fallidos().get(0).idProyecto()).isEqualTo(101L);
        assertThat(resumen.fallidos().get(0).codigo()).isEqualTo("SIN_DISPONIBILIDAD");
        verify(notificacionService, atLeast(2)).registrarTodas(any());
    }

    @Test
    void reprogramarAfectados_deshabilitado_noConsultaProyectos() {
        JornadaReprogramacionService deshabilitado = new JornadaReprogramacionService(
                asignacionActividadRepository,
                reprogramarProyectoService,
                proyectoRepository,
                miembroProyectoRepository,
                notificacionService,
                perfilRepository,
                proyectoSecurityService,
                false);

        ResumenReprogramacionResponse resumen = deshabilitado.reprogramarAfectados(1L);

        assertThat(resumen.totalProyectos()).isZero();
        verifyNoInteractions(asignacionActividadRepository, reprogramarProyectoService);
    }

    @Test
    void reintentar_director_llamaAlServicioYDevuelveResultado() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil(10L, 1L)));
        when(proyectoRepository.findById(100L))
                .thenReturn(Optional.of(proyecto(100L, "Desfile Primavera")));

        ReprogramacionProyectoResponse esperado =
                resultadoExitoso(100L, "Desfile Primavera");
        when(reprogramarProyectoService.reprogramar(
                100L, MotivoReprogramacion.REINTENTO, null))
                .thenReturn(esperado);
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(100L)))
                .thenReturn(List.of(director(10L)));

        ReprogramacionProyectoResponse resultado = service.reintentar(1L, 10L, 100L);

        assertThat(resultado).isSameAs(esperado);
        verify(proyectoSecurityService).validarDirector(100L, 10L);
    }

    @Test
    void reintentar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.reintentar(1L, null, 100L))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void reintentar_sinPermisoDeDirector_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfil(10L, 1L)));
        when(proyectoRepository.findById(100L))
                .thenReturn(Optional.of(proyecto(100L, "Desfile Primavera")));
        doThrow(new AccesoDenegadoProyectoException("No es director"))
                .when(proyectoSecurityService)
                .validarDirector(100L, 10L);

        assertThatThrownBy(() -> service.reintentar(1L, 10L, 100L))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    private ReprogramacionProyectoResponse resultadoExitoso(
            Long idProyecto,
            String nombre) {

        return new ReprogramacionProyectoResponse(
                idProyecto,
                nombre,
                MotivoReprogramacion.CAMBIO_JORNADA.name(),
                LocalDate.of(2026, 12, 15),
                List.of(new ActividadReprogramadaResponse(
                        55L, "Montaje", "PENDIENTE", ANTERIOR, NUEVO)),
                List.of());
    }

    private Proyecto proyecto(Long id, String nombre) {
        return Proyecto.builder()
                .idProyecto(id)
                .nombre(nombre)
                .build();
    }

    private Perfil perfil(Long idPerfil, Long idUsuario) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .usuario(Usuario.builder().idUsuario(idUsuario).build())
                .estado(EstadoPerfil.Activo)
                .build();
    }

    private MiembroProyecto director(Long idPerfil) {
        return MiembroProyecto.builder()
                .idMiembro(idPerfil + 100)
                .perfil(perfil(idPerfil, idPerfil))
                .build();
    }
}
