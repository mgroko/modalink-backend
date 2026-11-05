package org.mgroko.backend.invitaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionActividadBatchRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralBatchRequest;
import org.mgroko.backend.invitaciones.dto.InvitacionBatchResponse;
import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrearInvitacionBatchServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL = 10L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_REQUERIMIENTO = 30L;

    @Mock
    private CrearInvitacionService crearInvitacionService;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    private CrearInvitacionBatchService servicio;

    @BeforeEach
    void setUp() {
        servicio = new CrearInvitacionBatchService(
                crearInvitacionService,
                perfilRepository,
                proyectoRepository,
                requerimientoActividadRepository,
                proyectoSecurityService);

        prepararContextoGeneral();
    }

    @Test
    void general_variosPerfiles_creaTodas() {
        when(crearInvitacionService.invitarAProyecto(any(), any(), any(), any()))
                .thenReturn(respuestaGeneral(100L), respuestaGeneral(101L));

        InvitacionBatchResponse respuesta = servicio.invitarGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO,
                new CrearInvitacionGralBatchRequest(List.of(20L, 21L), "Hola"));

        assertThat(respuesta.creadas()).hasSize(2);
        assertThat(respuesta.errores()).isEmpty();
        verify(crearInvitacionService, org.mockito.Mockito.times(2))
                .invitarAProyecto(any(), any(), any(), any());
    }

    @Test
    void general_unPerfilConError_creaElRestoYReportaError() {
        when(crearInvitacionService.invitarAProyecto(any(), any(), any(), any()))
                .thenReturn(respuestaGeneral(100L))
                .thenThrow(new InvitacionNoPermitidaException(
                        "El perfil ya tiene una invitación activa para este proyecto.",
                        CrearInvitacionService.CODIGO_INVITACION_DUPLICADA))
                .thenReturn(respuestaGeneral(101L));

        InvitacionBatchResponse respuesta = servicio.invitarGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO,
                new CrearInvitacionGralBatchRequest(List.of(20L, 21L, 22L), null));

        assertThat(respuesta.creadas()).hasSize(2);
        assertThat(respuesta.errores()).hasSize(1);
        assertThat(respuesta.errores().getFirst().idPerfilDestinatario()).isEqualTo(21L);
        assertThat(respuesta.errores().getFirst().codigo())
                .isEqualTo(CrearInvitacionService.CODIGO_INVITACION_DUPLICADA);
    }

    @Test
    void general_idsDuplicados_procesaUnaSolaVez() {
        when(crearInvitacionService.invitarAProyecto(any(), any(), any(), any()))
                .thenReturn(respuestaGeneral(100L));

        InvitacionBatchResponse respuesta = servicio.invitarGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO,
                new CrearInvitacionGralBatchRequest(List.of(20L, 20L, 20L), null));

        assertThat(respuesta.creadas()).hasSize(1);
        verify(crearInvitacionService, org.mockito.Mockito.times(1))
                .invitarAProyecto(any(), any(), any(), any());
    }

    @Test
    void general_proyectoInexistente_propagaErrorComun() {
        when(proyectoRepository.findById(ID_PROYECTO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.invitarGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO,
                new CrearInvitacionGralBatchRequest(List.of(20L), null)))
                .isInstanceOf(ProyectoNoEncontradoException.class);

        verify(crearInvitacionService, never())
                .invitarAProyecto(any(), any(), any(), any());
    }

    @Test
    void actividad_variosPerfiles_creaTodas() {
        prepararContextoActividad();
        when(crearInvitacionService.invitarAProyectoActividad(any(), any(), any(), any()))
                .thenReturn(respuestaActividad(100L));

        InvitacionBatchResponse respuesta = servicio.invitarActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO,
                new CrearInvitacionActividadBatchRequest(
                        ID_REQUERIMIENTO, List.of(20L), null));

        assertThat(respuesta.creadas()).hasSize(1);
        assertThat(respuesta.errores()).isEmpty();
    }

    private void prepararContextoGeneral() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfilActivo()));
        when(proyectoRepository.findById(ID_PROYECTO))
                .thenReturn(Optional.of(proyecto()));
    }

    private void prepararContextoActividad() {
        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO)
                .cantidad(2)
                .descripcion("Requerimiento UC31 batch")
                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                .actividad(Actividad.builder()
                        .idActividad(40L)
                        .nombre("Actividad UC31 batch")
                        .estado(EstadoActividad.PENDIENTE)
                        .planificacion(Planificacion.builder()
                                .idPlanificacion(50L)
                                .proyecto(proyecto())
                                .build())
                        .build())
                .build();

        when(requerimientoActividadRepository
                .findByIdConActividadYProyecto(ID_REQUERIMIENTO))
                .thenReturn(Optional.of(requerimiento));
    }

    private Perfil perfilActivo() {
        return Perfil.builder()
                .idPerfil(ID_PERFIL)
                .nombreArtistico("Directora")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder()
                        .idUsuario(ID_USUARIO)
                        .estado(EstadoUsuario.ACTIVO)
                        .build())
                .build();
    }

    private Proyecto proyecto() {
        return Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Proyecto batch")
                .descripcion("Descripción")
                .estado(EstadoProyecto.PUBLICADO)
                .build();
    }

    private InvitacionResponse respuestaGeneral(Long id) {
        return new InvitacionResponse(
                id, TipoInvitacion.GENERAL, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 5, 12, 0),
                ID_PROYECTO, null, null, ID_PERFIL, 20L, null);
    }

    private InvitacionResponse respuestaActividad(Long id) {
        return new InvitacionResponse(
                id, TipoInvitacion.ACTIVIDAD, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 5, 12, 0),
                ID_PROYECTO, null, ID_REQUERIMIENTO, ID_PERFIL, 20L, null);
    }
}
