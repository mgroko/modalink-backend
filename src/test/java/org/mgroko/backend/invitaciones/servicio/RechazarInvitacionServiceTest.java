package org.mgroko.backend.invitaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.invitaciones.dto.InvitacionGestionadaResponse;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RechazarInvitacionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL = 10L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_INVITACION = 100L;

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private InvitacionGralRepository invitacionGralRepository;
    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;
    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private NotificacionService notificacionService;

    private RechazarInvitacionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new RechazarInvitacionService(
                perfilRepository,
                invitacionGralRepository,
                invitacionActividadRepository,
                requerimientoGralProyectoRepository,
                requerimientoActividadRepository,
                miembroProyectoRepository,
                notificacionService);
    }

    @Test
    void general_pendiente_propia_marcaRechazadaYNotifica() {
        prepararPerfilActivo();
        InvitacionGral invitacion = invitacion(EstadoSolicitud.PENDIENTE, ID_PERFIL);
        when(invitacionGralRepository.findById(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));
        when(invitacionGralRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));
        prepararNotificacion();

        InvitacionGestionadaResponse respuesta = servicio.rechazarGeneral(
                ID_USUARIO, ID_PERFIL, ID_INVITACION);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(respuesta.idMiembro()).isNull();
        assertThat(invitacion.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        verify(invitacionGralRepository).saveAndFlush(invitacion);
        verify(notificacionService).registrarTodas(any());
    }

    @Test
    void general_noPendiente_lanzaConflicto() {
        prepararPerfilActivo();
        InvitacionGral invitacion = invitacion(EstadoSolicitud.ACEPTADA, ID_PERFIL);
        when(invitacionGralRepository.findById(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));
        when(invitacionGralRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));

        assertThatThrownBy(() -> servicio.rechazarGeneral(
                ID_USUARIO, ID_PERFIL, ID_INVITACION))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("pendientes");
    }

    @Test
    void general_deOtroPerfil_lanzaAccesoDenegado() {
        prepararPerfilActivo();
        InvitacionGral invitacion = invitacion(EstadoSolicitud.PENDIENTE, 99L);
        when(invitacionGralRepository.findById(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));
        when(invitacionGralRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));

        assertThatThrownBy(() -> servicio.rechazarGeneral(
                ID_USUARIO, ID_PERFIL, ID_INVITACION))
                .isInstanceOf(org.mgroko.backend.proyectos.exception
                        .AccesoDenegadoProyectoException.class);
    }

    private void prepararPerfilActivo() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(ID_PERFIL)));
    }

    private Perfil perfil(Long id) {
        return Perfil.builder()
                .idPerfil(id)
                .nombreArtistico("Perfil " + id)
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder()
                        .idUsuario(id + 1000)
                        .estado(EstadoUsuario.ACTIVO)
                        .build())
                .build();
    }

    private InvitacionGral invitacion(EstadoSolicitud estado, Long idDestinatario) {
        return InvitacionGral.builder()
                .idInvitacionGral(ID_INVITACION)
                .proyecto(Proyecto.builder()
                        .idProyecto(ID_PROYECTO)
                        .nombre("Proyecto UC28")
                        .descripcion("Descripción")
                        .estado(EstadoProyecto.PUBLICADO)
                        .build())
                .perfilRemitente(perfil(20L))
                .perfilDestinatario(perfil(idDestinatario))
                .estado(estado)
                .build();
    }

    private void prepararNotificacion() {
        when(notificacionService.crear(
                any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocacion -> Notificacion.builder()
                        .usuarioDestino(invocacion.getArgument(0))
                        .perfilContexto(invocacion.getArgument(1))
                        .tipo(invocacion.getArgument(2))
                        .mensaje(invocacion.getArgument(3))
                        .entidadTipo(invocacion.getArgument(4))
                        .entidadId(invocacion.getArgument(5))
                        .idProyecto(invocacion.getArgument(6))
                        .build());
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(ID_PROYECTO)))
                .thenReturn(List.of(MiembroProyecto.builder()
                        .idMiembro(88L)
                        .perfil(perfil(20L))
                        .build()));
        when(notificacionService.registrarTodas(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }
}
