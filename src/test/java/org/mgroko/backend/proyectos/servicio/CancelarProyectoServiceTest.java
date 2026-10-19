package org.mgroko.backend.proyectos.servicio;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.proyectos.dto.CancelarProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.EstadoProyectoNoCancelableException;
import org.mgroko.backend.proyectos.exception.MotivoCancelacionObligatorioException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelarProyectoServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private PostulacionGralRepository postulacionGralRepository;

    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;

    @Mock
    private InvitacionGralRepository invitacionGralRepository;

    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @Mock
    private ObtenerProyectoService obtenerProyectoService;

    @InjectMocks
    private CancelarProyectoService cancelarProyectoService;

    private Perfil perfilDirector;
    private Proyecto proyecto;
    private ProyectoResponse respuesta;

    @BeforeEach
    void setUp() {
        Usuario usuarioDirector = Usuario.builder().idUsuario(1L).build();
        perfilDirector = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioDirector)
                .build();

        proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile 2026")
                .descripcion("Pasarela invierno")
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .aceptaPostulacionGral(true)
                .build();

        respuesta = new ProyectoResponse(
                100L, "Desfile 2026", "Pasarela invierno", null, null,
                "Cancelado", "PUBLICO", true, null, 10L, "Luna Diseños",
                List.of(), List.of(), null,
                org.mgroko.backend.modelo.enums.RolProyectoUsuario.DIRECTOR,
                true, List.of(), false, "Falta de presupuesto");
    }

    private void prepararCaminoFeliz() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfilDirector));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));
        when(proyectoRepository.save(any(Proyecto.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(obtenerProyectoService.obtener(1L, 10L, 100L)).thenReturn(respuesta);

        Usuario usuarioEquipo = Usuario.builder().idUsuario(2L).build();
        Perfil perfilEquipo = Perfil.builder().idPerfil(20L).usuario(usuarioEquipo).build();
        MiembroProyecto miembroEquipo = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilEquipo)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        when(miembroProyectoRepository.buscarMiembrosActivosDeProyecto(100L))
                .thenReturn(List.of(miembroEquipo));

        PostulacionGral postulacionGral = PostulacionGral.builder()
                .idPostulacionGral(300L)
                .perfil(perfilEquipo)
                .requerimientoGral(null)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
        when(postulacionGralRepository.findPendientesDeProyectoConPerfil(100L))
                .thenReturn(List.of(postulacionGral));
        when(postulacionGralRepository.saveAll(anyList()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        PostulacionActividad postulacionActividad = PostulacionActividad.builder()
                .idPostulacion(301L)
                .perfil(perfilEquipo)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
        when(postulacionActividadRepository.findPendientesDeProyectoConPerfil(100L))
                .thenReturn(List.of(postulacionActividad));
        when(postulacionActividadRepository.saveAll(anyList()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        InvitacionGral invitacionGral = InvitacionGral.builder()
                .idInvitacionGral(400L)
                .perfilDestinatario(perfilEquipo)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
        when(invitacionGralRepository.findPendientesDeProyectoConDestinatario(100L))
                .thenReturn(List.of(invitacionGral));
        when(invitacionGralRepository.saveAll(anyList()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        InvitacionActividad invitacionActividad = InvitacionActividad.builder()
                .idInvitacion(401L)
                .perfilDestinatario(perfilEquipo)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
        when(invitacionActividadRepository.findPendientesDeProyectoConDestinatario(100L))
                .thenReturn(List.of(invitacionActividad));
        when(invitacionActividadRepository.saveAll(anyList()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        when(notificacionService.crear(
                any(Usuario.class), anyString(), anyString(), anyString(), anyLong()))
                .thenAnswer(invocacion -> Notificacion.builder()
                        .usuarioDestino(invocacion.getArgument(0))
                        .tipo(invocacion.getArgument(1))
                        .mensaje(invocacion.getArgument(2))
                        .entidadTipo(invocacion.getArgument(3))
                        .entidadId(invocacion.getArgument(4))
                        .build());
    }

    @Test
    void cancelar_caminoFeliz_actualizaEstadoYNotifica() {
        prepararCaminoFeliz();

        ProyectoResponse resultado = cancelarProyectoService.cancelar(
                1L, 10L, 100L, new CancelarProyectoRequest("Falta de presupuesto"));

        assertThat(resultado).isEqualTo(respuesta);
        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.CANCELADO);
        assertThat(proyecto.getMotivoCancelacion()).isEqualTo("Falta de presupuesto");

        verify(actividadRepository).cancelarPendientesDeProyecto(100L);
        verify(proyectoSecurityService).validarPermiso(100L, 10L, "CANCELAR_PROYECTO");
        verify(proyectoSecurityService).validarDirector(100L, 10L);
        verify(obtenerProyectoService).obtener(1L, 10L, 100L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificacionService).registrarTodas(captor.capture());

        assertThat(captor.getValue()).hasSize(5);
        assertThat(captor.getValue())
                .extracting(Notificacion::getTipo)
                .containsExactlyInAnyOrder(
                        NotificacionService.TIPO_PROYECTO_CANCELADO,
                        NotificacionService.TIPO_POSTULACION_RECHAZADA,
                        NotificacionService.TIPO_POSTULACION_RECHAZADA,
                        NotificacionService.TIPO_INVITACION_RECHAZADA,
                        NotificacionService.TIPO_INVITACION_RECHAZADA);
        assertThat(captor.getValue())
                .allSatisfy(notificacion -> assertThat(notificacion.getMensaje())
                        .contains("Falta de presupuesto"));
    }

    @Test
    void cancelar_proyectoEnBorrador_lanzaExcepcion() {
        proyecto.setEstado(EstadoProyecto.BORRADOR);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfilDirector));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));

        assertThatThrownBy(() -> cancelarProyectoService.cancelar(
                1L, 10L, 100L, new CancelarProyectoRequest("Motivo válido")))
                .isInstanceOf(EstadoProyectoNoCancelableException.class);

        verify(proyectoRepository, never()).save(any(Proyecto.class));
        verify(notificacionService, never()).registrarTodas(anyList());
    }

    @Test
    void cancelar_perfilInactivo_lanzaExcepcion() {
        perfilDirector.setEstado(EstadoPerfil.Baja);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfilDirector));

        assertThatThrownBy(() -> cancelarProyectoService.cancelar(
                1L, 10L, 100L, new CancelarProyectoRequest("Motivo válido")))
                .isInstanceOf(PerfilEnBajaException.class);

        verify(proyectoRepository, never()).findById(anyLong());
    }

    @Test
    void cancelar_motivoBlanco_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfilDirector));
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));

        assertThatThrownBy(() -> cancelarProyectoService.cancelar(
                1L, 10L, 100L, new CancelarProyectoRequest("   ")))
                .isInstanceOf(MotivoCancelacionObligatorioException.class);

        verify(proyectoRepository, never()).save(any(Proyecto.class));
    }
}
