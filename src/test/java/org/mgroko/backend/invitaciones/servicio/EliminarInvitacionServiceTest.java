package org.mgroko.backend.invitaciones.servicio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.invitaciones.exception.InvitacionNoEncontradaException;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EliminarInvitacionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL = 10L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_INVITACION = 100L;
    private static final Long ID_REQUERIMIENTO = 30L;

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private InvitacionGralRepository invitacionGralRepository;
    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    private EliminarInvitacionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new EliminarInvitacionService(
                perfilRepository,
                invitacionGralRepository,
                invitacionActividadRepository,
                proyectoSecurityService);
    }

    @Test
    void general_pendienteDelRemitente_elimina() {
        prepararPerfilActivo();
        InvitacionGral invitacion = general(ID_PROYECTO, EstadoSolicitud.PENDIENTE);
        when(invitacionGralRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));

        servicio.eliminarInvitacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION);

        verify(invitacionGralRepository).delete(invitacion);
        verify(invitacionGralRepository).flush();
    }

    @Test
    void actividad_pendienteDelRemitente_elimina() {
        prepararPerfilActivo();
        InvitacionActividad invitacion = actividad(ID_PROYECTO, EstadoSolicitud.PENDIENTE);
        when(invitacionActividadRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));

        servicio.eliminarInvitacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION);

        verify(invitacionActividadRepository).delete(invitacion);
        verify(invitacionActividadRepository).flush();
    }

    @Test
    void general_inexistente_lanzaNoEncontrada() {
        prepararPerfilActivo();
        when(invitacionGralRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminarInvitacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION))
                .isInstanceOf(InvitacionNoEncontradaException.class);
    }

    @Test
    void actividad_inexistente_lanzaNoEncontrada() {
        prepararPerfilActivo();
        when(invitacionActividadRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminarInvitacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION))
                .isInstanceOf(InvitacionNoEncontradaException.class);
    }

    @Test
    void general_deOtroProyecto_lanzaNoEncontrada() {
        prepararPerfilActivo();
        when(invitacionGralRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(general(99L, EstadoSolicitud.PENDIENTE)));

        assertThatThrownBy(() -> servicio.eliminarInvitacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION))
                .isInstanceOf(InvitacionNoEncontradaException.class);

        verify(invitacionGralRepository, never()).delete(any());
    }

    @Test
    void actividad_deOtroProyecto_lanzaNoEncontrada() {
        prepararPerfilActivo();
        when(invitacionActividadRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(actividad(99L, EstadoSolicitud.PENDIENTE)));

        assertThatThrownBy(() -> servicio.eliminarInvitacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION))
                .isInstanceOf(InvitacionNoEncontradaException.class);

        verify(invitacionActividadRepository, never()).delete(any());
    }

    @Test
    void general_sinPermiso_lanzaAccesoDenegado() {
        prepararPerfilActivo();
        when(invitacionGralRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(general(ID_PROYECTO, EstadoSolicitud.PENDIENTE)));
        doThrow(new AccesoDenegadoProyectoException("Sin permiso"))
                .when(proyectoSecurityService)
                .validarPermiso(ID_PROYECTO, ID_PERFIL,
                        ProyectoSecurityService.PERMISO_ELIMINAR_INVITACION);

        assertThatThrownBy(() -> servicio.eliminarInvitacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void general_enviadaPorOtroPerfil_lanzaAccesoDenegado() {
        prepararPerfilActivo();
        InvitacionGral invitacion = general(ID_PROYECTO, EstadoSolicitud.PENDIENTE);
        invitacion.setPerfilRemitente(perfil(88L, EstadoPerfil.ACTIVO, EstadoUsuario.ACTIVO));
        when(invitacionGralRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(invitacion));

        assertThatThrownBy(() -> servicio.eliminarInvitacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION))
                .isInstanceOf(AccesoDenegadoProyectoException.class);

        verify(invitacionGralRepository, never()).delete(any());
    }

    @Test
    void general_aceptada_lanzaNoPermitida() {
        prepararPerfilActivo();
        when(invitacionGralRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(general(ID_PROYECTO, EstadoSolicitud.ACEPTADA)));

        assertThatThrownBy(() -> servicio.eliminarInvitacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("pendientes");

        verify(invitacionGralRepository, never()).delete(any());
    }

    @Test
    void actividad_rechazada_lanzaNoPermitida() {
        prepararPerfilActivo();
        when(invitacionActividadRepository.findByIdForUpdate(ID_INVITACION))
                .thenReturn(Optional.of(actividad(ID_PROYECTO, EstadoSolicitud.RECHAZADA)));

        assertThatThrownBy(() -> servicio.eliminarInvitacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION))
                .isInstanceOf(InvitacionNoPermitidaException.class)
                .hasMessageContaining("pendientes");
    }

    @Test
    void sinPerfilActivo_lanzaNoSeleccionado() {
        assertThatThrownBy(() -> servicio.eliminarInvitacionGeneral(
                ID_USUARIO, null, ID_PROYECTO, ID_INVITACION))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void perfilInactivo_lanzaEnBaja() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(ID_PERFIL, EstadoPerfil.BAJA,
                        EstadoUsuario.ACTIVO)));

        assertThatThrownBy(() -> servicio.eliminarInvitacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_INVITACION))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    private void prepararPerfilActivo() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(ID_PERFIL, EstadoPerfil.ACTIVO,
                        EstadoUsuario.ACTIVO)));
    }

    private Perfil perfil(Long id, EstadoPerfil estado, EstadoUsuario estadoUsuario) {
        return Perfil.builder()
                .idPerfil(id)
                .nombreArtistico("Perfil " + id)
                .estado(estado)
                .usuario(Usuario.builder()
                        .idUsuario(id + 1000)
                        .estado(estadoUsuario)
                        .build())
                .build();
    }

    private Proyecto proyecto(Long id) {
        return Proyecto.builder()
                .idProyecto(id)
                .nombre("Proyecto UC32")
                .descripcion("Descripción")
                .build();
    }

    private InvitacionGral general(Long idProyecto, EstadoSolicitud estado) {
        return InvitacionGral.builder()
                .idInvitacionGral(ID_INVITACION)
                .proyecto(proyecto(idProyecto))
                .perfilRemitente(perfil(ID_PERFIL, EstadoPerfil.ACTIVO, EstadoUsuario.ACTIVO))
                .perfilDestinatario(perfil(20L, EstadoPerfil.ACTIVO, EstadoUsuario.ACTIVO))
                .estado(estado)
                .build();
    }

    private InvitacionActividad actividad(Long idProyecto, EstadoSolicitud estado) {
        Actividad actividad = Actividad.builder()
                .idActividad(40L)
                .nombre("Actividad UC32")
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(Planificacion.builder()
                        .idPlanificacion(50L)
                        .proyecto(proyecto(idProyecto))
                        .build())
                .build();

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO)
                .cantidad(1)
                .descripcion("Requerimiento UC32")
                .profesion(Profesion.builder()
                        .idProfesion(2L)
                        .nombre("Modelo")
                        .build())
                .actividad(actividad)
                .build();

        return InvitacionActividad.builder()
                .idInvitacion(ID_INVITACION)
                .perfilRemitente(perfil(ID_PERFIL, EstadoPerfil.ACTIVO, EstadoUsuario.ACTIVO))
                .perfilDestinatario(perfil(20L, EstadoPerfil.ACTIVO, EstadoUsuario.ACTIVO))
                .requerimiento(requerimiento)
                .estado(estado)
                .build();
    }
}
