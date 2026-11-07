package org.mgroko.backend.invitaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.invitaciones.dto.BuscarInvitacionesFiltro;
import org.mgroko.backend.invitaciones.dto.DireccionInvitacion;
import org.mgroko.backend.invitaciones.dto.InvitacionBusquedaResponse;
import org.mgroko.backend.invitaciones.dto.TipoInvitacion;
import org.mgroko.backend.invitaciones.exception.RangoFechasInvitacionInvalidoException;
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
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuscarInvitacionServiceTest {

    private static final Long ID_PERFIL_ACTIVO = 10L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_REQUERIMIENTO = 30L;
    private static final Long ID_ACTIVIDAD = 40L;

    @Mock
    private InvitacionGralRepository invitacionGralRepository;
    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;

    private BuscarInvitacionService servicio;

    private Perfil activo;
    private Perfil otro;
    private Perfil tercero;
    private Proyecto proyecto;
    private Proyecto otroProyecto;

    @BeforeEach
    void setUp() {
        servicio = new BuscarInvitacionService(
                invitacionGralRepository, invitacionActividadRepository);

        activo = perfil(ID_PERFIL_ACTIVO, "Perfil Activo", "Ana", "Activa", 1L);
        otro = perfil(20L, "Luna Diseños", "Luna", "Pérez", 4L);
        tercero = perfil(30L, "Sol Modelo", "Sol", "Gómez", 2L);
        proyecto = proyecto(ID_PROYECTO, "Desfile 2026");
        otroProyecto = proyecto(6L, "Editorial 2026");
    }

    @Test
    void buscar_sinFiltros_devuelveRecibidasYEnviadasOrdenadas() {
        InvitacionGral recibida = general(
                otro, activo, proyecto, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 3, 10, 0));
        InvitacionActividad enviada = actividad(
                activo, tercero, proyecto, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 4, 10, 0));

        when(invitacionGralRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of(recibida));
        when(invitacionActividadRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of(enviada));

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO, null, 0, 20, false);

        assertThat(respuesta.contenido()).hasSize(2);
        assertThat(respuesta.contenido().get(0).tipo()).isEqualTo(TipoInvitacion.ACTIVIDAD);
        assertThat(respuesta.contenido().get(0).direccion())
                .isEqualTo(DireccionInvitacion.ENVIADA);
        assertThat(respuesta.contenido().get(0).puedeEliminar()).isTrue();
        assertThat(respuesta.contenido().get(1).tipo()).isEqualTo(TipoInvitacion.GENERAL);
        assertThat(respuesta.contenido().get(1).direccion())
                .isEqualTo(DireccionInvitacion.RECIBIDA);
        assertThat(respuesta.contenido().get(1).puedeEliminar()).isFalse();
    }

    @Test
    void buscar_porDireccionRecibida_soloDevuelveRecibidas() {
        prepararDosInvitaciones();

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO,
                        new BuscarInvitacionesFiltro(
                                null, null, null, null, null,
                                DireccionInvitacion.RECIBIDA, null, null),
                        0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().getFirst().direccion())
                .isEqualTo(DireccionInvitacion.RECIBIDA);
    }

    @Test
    void buscar_porTipoGeneral_soloDevuelveGenerales() {
        prepararDosInvitaciones();

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO,
                        new BuscarInvitacionesFiltro(
                                null, null, null, null, null, null, null,
                                TipoInvitacion.GENERAL),
                        0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().getFirst().tipo()).isEqualTo(TipoInvitacion.GENERAL);
    }

    @Test
    void buscar_porEstado_filtraCorrectamente() {
        InvitacionGral rechazada = general(
                otro, activo, proyecto, EstadoSolicitud.RECHAZADA,
                LocalDateTime.of(2026, 11, 3, 10, 0));
        InvitacionActividad pendiente = actividad(
                activo, tercero, proyecto, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 4, 10, 0));
        when(invitacionGralRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of(rechazada));
        when(invitacionActividadRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of(pendiente));

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO,
                        new BuscarInvitacionesFiltro(
                                null, null, null, null, null, null,
                                EstadoSolicitud.PENDIENTE, null),
                        0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().getFirst().estado())
                .isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void buscar_invitacionRechazada_exponeFechaDeRechazo() {
        LocalDateTime fechaRechazo = LocalDateTime.of(2026, 11, 6, 18, 30);
        InvitacionGral rechazada = general(
                otro, activo, proyecto, EstadoSolicitud.RECHAZADA,
                LocalDateTime.of(2026, 11, 3, 10, 0));
        rechazada.setFechaRechazo(fechaRechazo);
        when(invitacionGralRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of(rechazada));

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO, null, 0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().getFirst().fechaRechazo())
                .isEqualTo(fechaRechazo);
    }

    @Test
    void buscar_porNombreProyecto_esParcialEIgnoraMayusculas() {
        prepararDosInvitaciones();

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO,
                        new BuscarInvitacionesFiltro(
                                null, "desfile", null, null, null, null, null, null),
                        0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().getFirst().nombreProyecto())
                .isEqualTo("Desfile 2026");
    }

    @Test
    void buscar_porIdProyecto_filtraExactamente() {
        InvitacionGral enUno = general(
                otro, activo, proyecto, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 3, 10, 0));
        InvitacionGral enOtro = general(
                otro, activo, otroProyecto, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 4, 10, 0));
        when(invitacionGralRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of(enUno, enOtro));

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO,
                        new BuscarInvitacionesFiltro(
                                ID_PROYECTO, null, null, null, null, null, null, null),
                        0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().getFirst().idProyecto()).isEqualTo(ID_PROYECTO);
    }

    @Test
    void buscar_porRangoDeFechas_esInclusivo() {
        InvitacionGral incluida = general(
                otro, activo, proyecto, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 1, 23, 59));
        InvitacionGral fuera = general(
                otro, activo, proyecto, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 2, 0, 0));
        when(invitacionGralRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of(incluida, fuera));

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO,
                        new BuscarInvitacionesFiltro(
                                null, null, LocalDate.of(2026, 11, 1),
                                LocalDate.of(2026, 11, 1), null, null, null, null),
                        0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().getFirst().fechaEnvio())
                .isEqualTo(LocalDateTime.of(2026, 11, 1, 23, 59));
    }

    @Test
    void buscar_porNombrePerfilEnRecibidas_filtraAlRemitente() {
        prepararDosInvitaciones();

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO,
                        new BuscarInvitacionesFiltro(
                                null, null, null, null, "luna",
                                DireccionInvitacion.RECIBIDA, null, null),
                        0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().getFirst().remitente().nombreArtistico())
                .isEqualTo("Luna Diseños");
    }

    @Test
    void buscar_porNombrePerfilEnEnviadas_filtraAlDestinatario() {
        prepararDosInvitaciones();

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO,
                        new BuscarInvitacionesFiltro(
                                null, null, null, null, "sol",
                                DireccionInvitacion.ENVIADA, null, null),
                        0, 20, false);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.contenido().getFirst().destinatario().nombreArtistico())
                .isEqualTo("Sol Modelo");
    }

    @Test
    void buscar_porNombreDelPropioPerfilSinDireccion_noDevuelveTodo() {
        prepararDosInvitaciones();

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO,
                        new BuscarInvitacionesFiltro(
                                null, null, null, null, "Perfil Activo",
                                null, null, null),
                        0, 20, false);

        assertThat(respuesta.contenido()).isEmpty();
    }

    @Test
    void buscar_paginacion_devuelveLaPaginaSolicitada() {
        prepararDosInvitaciones();

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO, null, 1, 1, false);

        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.totalElementos()).isEqualTo(2);
        assertThat(respuesta.totalPaginas()).isEqualTo(2);
        assertThat(respuesta.paginaActual()).isEqualTo(1);
        assertThat(respuesta.contenido().getFirst().tipo())
                .isEqualTo(TipoInvitacion.GENERAL);
    }

    @Test
    void buscar_conRangoInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> servicio.buscar(
                ID_PERFIL_ACTIVO,
                new BuscarInvitacionesFiltro(
                        null, null, LocalDate.of(2026, 11, 30),
                        LocalDate.of(2026, 11, 1), null, null, null, null),
                0, 20, false))
                .isInstanceOf(RangoFechasInvitacionInvalidoException.class);
    }

    @Test
    void buscar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> servicio.buscar(null, null, 0, 20, false))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void buscar_sinResultados_devuelvePaginaVacia() {
        when(invitacionGralRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of());
        when(invitacionActividadRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of());

        PaginaResponse<InvitacionBusquedaResponse> respuesta =
                servicio.buscar(ID_PERFIL_ACTIVO, null, 0, 20, false);

        assertThat(respuesta.contenido()).isEmpty();
        assertThat(respuesta.totalElementos()).isZero();
    }

    private void prepararDosInvitaciones() {
        InvitacionGral recibida = general(
                otro, activo, proyecto, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 3, 10, 0));
        InvitacionActividad enviada = actividad(
                activo, tercero, otroProyecto, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 11, 4, 10, 0));

        when(invitacionGralRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of(recibida));
        when(invitacionActividadRepository.buscarPorPerfil(ID_PERFIL_ACTIVO))
                .thenReturn(List.of(enviada));
    }

    private Perfil perfil(
            Long id,
            String nombreArtistico,
            String nombre,
            String apellido,
            Long idProfesion) {

        return Perfil.builder()
                .idPerfil(id)
                .nombreArtistico(nombreArtistico)
                .usuario(Usuario.builder()
                        .idUsuario(id + 1000)
                        .nombre(nombre)
                        .apellido(apellido)
                        .build())
                .profesion(Profesion.builder()
                        .idProfesion(idProfesion)
                        .nombre("Profesión " + idProfesion)
                        .build())
                .build();
    }

    private Proyecto proyecto(Long id, String nombre) {
        return Proyecto.builder()
                .idProyecto(id)
                .nombre(nombre)
                .descripcion("Descripción")
                .build();
    }

    private InvitacionGral general(
            Perfil remitente,
            Perfil destinatario,
            Proyecto proyecto,
            EstadoSolicitud estado,
            LocalDateTime fecha) {

        return InvitacionGral.builder()
                .idInvitacionGral(100L)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .proyecto(proyecto)
                .estado(estado)
                .fechaEnvio(fecha)
                .mensaje("Mensaje general")
                .build();
    }

    private InvitacionActividad actividad(
            Perfil remitente,
            Perfil destinatario,
            Proyecto proyecto,
            EstadoSolicitud estado,
            LocalDateTime fecha) {

        Actividad actividad = Actividad.builder()
                .idActividad(ID_ACTIVIDAD)
                .nombre("Actividad UC30")
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(Planificacion.builder()
                        .idPlanificacion(50L)
                        .proyecto(proyecto)
                        .build())
                .build();

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO)
                .cantidad(1)
                .descripcion("Requerimiento UC30")
                .profesion(Profesion.builder()
                        .idProfesion(2L)
                        .nombre("Modelo")
                        .build())
                .actividad(actividad)
                .build();

        return InvitacionActividad.builder()
                .idInvitacion(200L)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .requerimiento(requerimiento)
                .estado(estado)
                .fechaEnvio(fecha)
                .mensaje("Mensaje actividad")
                .build();
    }
}
