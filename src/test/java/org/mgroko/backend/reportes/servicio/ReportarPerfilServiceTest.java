package org.mgroko.backend.reportes.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.reportes.dto.CrearReportePerfilRequest;
import org.mgroko.backend.reportes.evento.ReportePerfilCreadoEvent;
import org.mgroko.backend.reportes.exception.AutoreporteException;
import org.mgroko.backend.reportes.exception.DescripcionReporteInvalidaException;
import org.mgroko.backend.reportes.exception.DescripcionReporteObligatoriaException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.reportes.exception.ReportePendienteExistenteException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ReporteRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ReportarPerfilServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTIVO = 10L;
    private static final Long ID_PERFIL_REPORTADO = 20L;

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReportarPerfilService service;
    private Perfil autor;
    private Perfil reportado;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-10-29T12:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        service = new ReportarPerfilService(
                perfilRepository, reporteRepository, eventPublisher, reloj);

        autor = perfil(10L, "Luna Diseños", usuario(1L, "Luna", "Pérez", "USUARIO", EstadoUsuario.ACTIVO));
        reportado = perfil(20L, "Perfil Reportado", usuario(2L, "Ana", "Gómez", "USUARIO", EstadoUsuario.ACTIVO));
    }

    @Test
    void reportar_valido_guardaYPublicaEvento() {
        prepararAutorYTarget();
        when(reporteRepository.existsPendiente(
                ID_PERFIL_ACTIVO, EntidadReporte.PERFIL, ID_PERFIL_REPORTADO)).thenReturn(false);
        when(reporteRepository.saveAndFlush(any(Reporte.class))).thenAnswer(invocacion -> {
            Reporte reporte = invocacion.getArgument(0);
            reporte.setIdReporte(99L);
            return reporte;
        });

        service.reportar(
                ID_USUARIO,
                ID_PERFIL_ACTIVO,
                ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, "  Spam insistente  "));

        ArgumentCaptor<Reporte> reporteCaptor = ArgumentCaptor.forClass(Reporte.class);
        verify(reporteRepository).saveAndFlush(reporteCaptor.capture());
        Reporte guardado = reporteCaptor.getValue();
        assertThat(guardado.getEntidadTipo()).isEqualTo(EntidadReporte.PERFIL);
        assertThat(guardado.getEntidadId()).isEqualTo(ID_PERFIL_REPORTADO);
        assertThat(guardado.getUsuarioAutor()).isSameAs(autor.getUsuario());
        assertThat(guardado.getPerfilAutor()).isSameAs(autor);
        assertThat(guardado.getPerfilReportado()).isSameAs(reportado);
        assertThat(guardado.getMotivo()).isEqualTo(MotivoReporte.SPAM);
        assertThat(guardado.getDescripcion()).isEqualTo("Spam insistente");
        assertThat(guardado.getEstado()).isEqualTo(EstadoReporte.PENDIENTE);
        assertThat(guardado.getFechaReporte())
                .isEqualTo(LocalDateTime.of(2026, 10, 29, 9, 0));

        ArgumentCaptor<ReportePerfilCreadoEvent> eventoCaptor =
                ArgumentCaptor.forClass(ReportePerfilCreadoEvent.class);
        verify(eventPublisher).publishEvent(eventoCaptor.capture());
        ReportePerfilCreadoEvent evento = eventoCaptor.getValue();
        assertThat(evento.idReporte()).isEqualTo(99L);
        assertThat(evento.idPerfilReportado()).isEqualTo(ID_PERFIL_REPORTADO);
        assertThat(evento.nombrePerfilReportado()).isEqualTo("Perfil Reportado");
        assertThat(evento.idUsuarioAutor()).isEqualTo(ID_USUARIO);
        assertThat(evento.nombreUsuarioAutor()).isEqualTo("Luna Pérez");
        assertThat(evento.motivo()).isEqualTo(MotivoReporte.SPAM);
        assertThat(evento.descripcion()).isEqualTo("Spam insistente");
    }

    @Test
    void reportar_otroSinDescripcion_lanzaExcepcion() {
        prepararAutorYTarget();

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.OTRO, "   ")))
                .isInstanceOf(DescripcionReporteObligatoriaException.class);

        verify(reporteRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportar_descripcionMayorA200_lanzaExcepcion() {
        prepararAutorYTarget();

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, "x".repeat(201))))
                .isInstanceOf(DescripcionReporteInvalidaException.class);

        verify(reporteRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, null, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void reportar_perfilActivoDeOtroUsuario_lanzaNoEncontrado() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void reportar_perfilActivoNoActivo_lanzaExcepcion() {
        autor.setEstado(EstadoPerfil.BAJA);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void reportar_admin_lanzaNoPermitido() {
        autor.getUsuario().setRolGlobal(rol("ADMINISTRADOR", "Administrador"));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(ReporteNoPermitidoException.class);
    }

    @Test
    void reportar_targetInexistente_lanzaNoEncontrado() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));
        when(perfilRepository.findById(ID_PERFIL_REPORTADO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void reportar_targetInactivo_lanzaNoEncontrado() {
        reportado.setEstado(EstadoPerfil.BAJA);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));
        when(perfilRepository.findById(ID_PERFIL_REPORTADO)).thenReturn(Optional.of(reportado));

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void reportar_targetConUsuarioInactivo_lanzaNoEncontrado() {
        reportado.getUsuario().setEstado(EstadoUsuario.DESHABILITADO);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));
        when(perfilRepository.findById(ID_PERFIL_REPORTADO)).thenReturn(Optional.of(reportado));

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void reportar_autorreporte_lanzaExcepcion() {
        reportado.setUsuario(usuario(1L, "Luna", "Pérez", "USUARIO", EstadoUsuario.ACTIVO));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));
        when(perfilRepository.findById(ID_PERFIL_REPORTADO)).thenReturn(Optional.of(reportado));

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(AutoreporteException.class);
    }

    @Test
    void reportar_duplicadoPendiente_lanzaExcepcion() {
        prepararAutorYTarget();
        when(reporteRepository.existsPendiente(
                ID_PERFIL_ACTIVO, EntidadReporte.PERFIL, ID_PERFIL_REPORTADO)).thenReturn(true);

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(ReportePendienteExistenteException.class);

        verify(reporteRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportar_carreraConIndiceUnico_lanzaDuplicado() {
        prepararAutorYTarget();
        when(reporteRepository.existsPendiente(
                ID_PERFIL_ACTIVO, EntidadReporte.PERFIL, ID_PERFIL_REPORTADO)).thenReturn(false);
        when(reporteRepository.saveAndFlush(any(Reporte.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThatThrownBy(() -> service.reportar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_REPORTADO,
                new CrearReportePerfilRequest(MotivoReporte.SPAM, null)))
                .isInstanceOf(ReportePendienteExistenteException.class);
    }

    private void prepararAutorYTarget() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(autor));
        when(perfilRepository.findById(ID_PERFIL_REPORTADO)).thenReturn(Optional.of(reportado));
    }

    private Perfil perfil(Long id, String nombre, Usuario usuario) {
        return Perfil.builder()
                .idPerfil(id)
                .nombreArtistico(nombre)
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .build();
    }

    private Usuario usuario(
            Long id,
            String nombre,
            String apellido,
            String codigoRol,
            EstadoUsuario estado) {
        return Usuario.builder()
                .idUsuario(id)
                .nombre(nombre)
                .apellido(apellido)
                .correo("user" + id + "@example.com")
                .dni("3000000" + id)
                .estado(estado)
                .rolGlobal(rol(codigoRol, "Usuario"))
                .build();
    }

    private RolGlobal rol(String codigo, String nombre) {
        return RolGlobal.builder().codigo(codigo).nombre(nombre).build();
    }
}
