package org.mgroko.backend.postulaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.postulaciones.dto.PostulacionGestionadaResponse;
import org.mgroko.backend.postulaciones.exception.PostulacionNoEncontradaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RechazarPostulacionServiceTest {

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private PostulacionGralRepository postulacionGralRepository;
    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;
    @Mock
    private NotificacionService notificacionService;

    private RechazarPostulacionService service;

    private Perfil director;
    private Perfil postulante;
    private Proyecto proyecto;
    private RequerimientoGralProyecto requerimientoGral;
    private PostulacionGral postulacionGral;

    @BeforeEach
    void setUp() {
        service = new RechazarPostulacionService(
                perfilRepository,
                proyectoRepository,
                postulacionGralRepository,
                postulacionActividadRepository,
                proyectoSecurityService,
                notificacionService);

        director = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Directora")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder().idUsuario(1L).build())
                .build();

        postulante = Perfil.builder()
                .idPerfil(12L)
                .nombreArtistico("Postulante")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder().idUsuario(2L).build())
                .build();

        proyecto = Proyecto.builder()
                .idProyecto(5L)
                .nombre("Desfile")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        requerimientoGral = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(20L)
                .proyecto(proyecto)
                .cantidad(1)
                .build();

        postulacionGral = PostulacionGral.builder()
                .idPostulacionGral(77L)
                .perfil(postulante)
                .requerimientoGral(requerimientoGral)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaPostulacion(LocalDateTime.of(2026, 10, 26, 10, 0))
                .build();
    }

    @Test
    void rechazarGeneral_cambiaEstadoYNotifica() {
        prepararProyecto();
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(director));
        when(postulacionGralRepository.findByIdParaGestion(77L))
                .thenReturn(Optional.of(postulacionGral));
        when(postulacionGralRepository.saveAndFlush(any(PostulacionGral.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificacionService.crear(
                any(Usuario.class), any(Perfil.class),
                eq(NotificacionService.TIPO_POSTULACION_RECHAZADA),
                any(String.class), eq("POSTULACION_GRAL"), eq(77L), any(Long.class)))
                .thenReturn(new Notificacion());

        PostulacionGestionadaResponse respuesta =
                service.rechazarGeneral(1L, 10L, 5L, 77L);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(respuesta.idMiembro()).isNull();
        assertThat(postulacionGral.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        verify(notificacionService).registrarTodas(any());
    }

    @Test
    void rechazarActividad_cambiaEstadoYNotifica() {
        Actividad actividad = Actividad.builder()
                .idActividad(40L)
                .nombre("Prueba de vestuario")
                .estado(EstadoActividad.PENDIENTE)
                .duracionMinutos(120)
                .planificacion(Planificacion.builder()
                        .idPlanificacion(30L)
                        .proyecto(proyecto)
                        .build())
                .build();
        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(50L)
                .actividad(actividad)
                .cantidad(1)
                .build();
        PostulacionActividad postulacion = PostulacionActividad.builder()
                .idPostulacion(88L)
                .perfil(postulante)
                .requerimiento(requerimiento)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaPostulacion(LocalDateTime.of(2026, 10, 26, 10, 0))
                .build();

        prepararProyecto();
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(director));
        when(postulacionActividadRepository.findByIdParaGestion(88L))
                .thenReturn(Optional.of(postulacion));
        when(postulacionActividadRepository.saveAndFlush(any(PostulacionActividad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificacionService.crear(
                any(Usuario.class), any(Perfil.class),
                eq(NotificacionService.TIPO_POSTULACION_RECHAZADA),
                any(String.class), eq("POSTULACION_ACTIVIDAD"), eq(88L), any(Long.class)))
                .thenReturn(new Notificacion());

        PostulacionGestionadaResponse respuesta =
                service.rechazarActividad(1L, 10L, 5L, 88L);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(respuesta.idAsignacionActividad()).isNull();
        assertThat(postulacion.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
    }

    @Test
    void rechazarGeneral_yaGestionada_lanzaExcepcion() {
        prepararProyecto();
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(director));
        when(postulacionGralRepository.findByIdParaGestion(77L))
                .thenReturn(Optional.of(postulacionGral));
        postulacionGral.setEstado(EstadoSolicitud.ACEPTADA);

        assertThatThrownBy(() -> service.rechazarGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionYaGestionadaException.class);
    }

    @Test
    void rechazarGeneral_postulacionDeOtroProyecto_lanzaNoEncontrada() {
        prepararProyecto();
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(director));
        when(postulacionGralRepository.findByIdParaGestion(77L))
                .thenReturn(Optional.of(postulacionGral));
        postulacionGral.getRequerimientoGral().setProyecto(
                Proyecto.builder().idProyecto(99L).build());

        assertThatThrownBy(() -> service.rechazarGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionNoEncontradaException.class);
    }

    @Test
    void rechazarGeneral_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.rechazarGeneral(1L, null, 5L, 77L))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void rechazarGeneral_sinPermiso_lanzaExcepcion() {
        prepararProyecto();
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(director));
        doThrow(new AccesoDenegadoProyectoException("Sin permiso"))
                .when(proyectoSecurityService)
                .validarPermiso(5L, 10L, "GESTIONAR_POSTULACIONES");

        assertThatThrownBy(() -> service.rechazarGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    private void prepararProyecto() {
        when(proyectoRepository.findByIdParaGestion(5L))
                .thenReturn(Optional.of(proyecto));
    }
}
