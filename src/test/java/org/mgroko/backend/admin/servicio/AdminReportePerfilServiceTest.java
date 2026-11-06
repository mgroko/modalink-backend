package org.mgroko.backend.admin.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.ActualizarEstadoReporteRequest;
import org.mgroko.backend.admin.dto.AdminReportePerfilDetalleResponse;
import org.mgroko.backend.admin.dto.AdminReportePerfilResumenResponse;
import org.mgroko.backend.admin.dto.BuscarReportesPerfilFiltro;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.reportes.exception.EstadoReporteInvalidoException;
import org.mgroko.backend.reportes.exception.RangoFechasReporteInvalidoException;
import org.mgroko.backend.reportes.exception.ReporteNoEncontradoException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.repositorio.ReporteRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminReportePerfilServiceTest {

    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private AdminReportePerfilService service;
    private Reporte reporte;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-10-29T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        service = new AdminReportePerfilService(reporteRepository, usuarioRepository, reloj);

        admin = usuario(99L, "Admin", "Sistema", "ADMINISTRADOR", EstadoUsuario.ACTIVO);
        Usuario autor = usuario(1L, "Luna", "Pérez", "USUARIO", EstadoUsuario.ACTIVO);
        Usuario usuarioReportado = usuario(2L, "Ana", "Gómez", "USUARIO", EstadoUsuario.ACTIVO);

        Perfil perfilAutor = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(autor)
                .build();

        Perfil perfilReportado = Perfil.builder()
                .idPerfil(20L)
                .nombreArtistico("Perfil Reportado")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuarioReportado)
                .profesion(Profesion.builder().idProfesion(3L).nombre("Modelo").build())
                .build();

        reporte = Reporte.builder()
                .idReporte(1L)
                .entidadTipo(EntidadReporte.PERFIL)
                .entidadId(20L)
                .usuarioAutor(autor)
                .perfilAutor(perfilAutor)
                .perfilReportado(perfilReportado)
                .motivo(MotivoReporte.SPAM)
                .descripcion("Spam insistente")
                .estado(EstadoReporte.PENDIENTE)
                .fechaReporte(LocalDateTime.of(2026, 10, 29, 10, 0))
                .build();
    }

    @Test
    void buscar_devuelvePaginaConDatosDelReporte() {
        when(reporteRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(reporte), PageRequest.of(0, 20), 1));

        PaginaResponse<AdminReportePerfilResumenResponse> pagina =
                service.buscar(new BuscarReportesPerfilFiltro(null, null, null, null), 0, 20, false);

        assertThat(pagina.totalElementos()).isEqualTo(1);
        AdminReportePerfilResumenResponse fila = pagina.contenido().get(0);
        assertThat(fila.idReporte()).isEqualTo(1L);
        assertThat(fila.motivo()).isEqualTo(MotivoReporte.SPAM);
        assertThat(fila.motivoDescripcion()).isEqualTo("Spam o publicidad engañosa");
        assertThat(fila.nombrePerfilReportado()).isEqualTo("Perfil Reportado");
        assertThat(fila.nombreUsuarioAutor()).isEqualTo("Luna Pérez");
        assertThat(fila.nombrePerfilAutor()).isEqualTo("Luna Diseños");
    }

    @Test
    void buscar_rangoDeFechasInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> service.buscar(
                new BuscarReportesPerfilFiltro(
                        null, null, LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 1)),
                0, 20, false))
                .isInstanceOf(RangoFechasReporteInvalidoException.class);

        verify(reporteRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void obtenerDetalle_incluyePerfilUsuarioYAutor() {
        when(reporteRepository.findDetalleById(1L)).thenReturn(Optional.of(reporte));

        AdminReportePerfilDetalleResponse detalle = service.obtenerDetalle(1L);

        assertThat(detalle.idReporte()).isEqualTo(1L);
        assertThat(detalle.entidadTipo()).isEqualTo(EntidadReporte.PERFIL);
        assertThat(detalle.motivoDescripcion()).isEqualTo("Spam o publicidad engañosa");
        assertThat(detalle.perfilReportado().idPerfil()).isEqualTo(20L);
        assertThat(detalle.perfilReportado().profesion()).isEqualTo("Modelo");
        assertThat(detalle.usuarioReportado().correo()).isEqualTo("user2@example.com");
        assertThat(detalle.usuarioReportado().estado()).isEqualTo("Activo");
        assertThat(detalle.autor().idPerfil()).isEqualTo(10L);
        assertThat(detalle.autor().nombreArtistico()).isEqualTo("Luna Diseños");
        assertThat(detalle.idAdminRevisor()).isNull();
    }

    @Test
    void actualizarEstado_pendienteAResuelto_guardaAdminYFecha() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(reporteRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(reporte));
        when(reporteRepository.findDetalleById(1L)).thenReturn(Optional.of(reporte));
        when(reporteRepository.saveAndFlush(any(Reporte.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        AdminReportePerfilDetalleResponse detalle = service.actualizarEstado(
                1L, 99L, new ActualizarEstadoReporteRequest(EstadoReporte.RESUELTO));

        assertThat(reporte.getEstado()).isEqualTo(EstadoReporte.RESUELTO);
        assertThat(reporte.getAdminRevisor()).isSameAs(admin);
        assertThat(reporte.getFechaResolucion())
                .isEqualTo(LocalDateTime.of(2026, 10, 29, 12, 0));
        assertThat(detalle.estado()).isEqualTo(EstadoReporte.RESUELTO);
        assertThat(detalle.idAdminRevisor()).isEqualTo(99L);
        assertThat(detalle.nombreAdminRevisor()).isEqualTo("Admin Sistema");
    }

    @Test
    void actualizarEstado_yaGestionado_lanzaExcepcion() {
        reporte.setEstado(EstadoReporte.RESUELTO);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(reporteRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(reporte));

        assertThatThrownBy(() -> service.actualizarEstado(
                1L, 99L, new ActualizarEstadoReporteRequest(EstadoReporte.DESCARTADO)))
                .isInstanceOf(EstadoReporteInvalidoException.class);

        verify(reporteRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizarEstado_aPendiente_lanzaExcepcion() {
        assertThatThrownBy(() -> service.actualizarEstado(
                1L, 99L, new ActualizarEstadoReporteRequest(EstadoReporte.PENDIENTE)))
                .isInstanceOf(EstadoReporteInvalidoException.class);
    }

    @Test
    void actualizarEstado_usuarioNoAdmin_lanzaNoPermitido() {
        Usuario noAdmin = usuario(98L, "Luna", "Pérez", "USUARIO", EstadoUsuario.ACTIVO);
        when(usuarioRepository.findById(98L)).thenReturn(Optional.of(noAdmin));

        assertThatThrownBy(() -> service.actualizarEstado(
                1L, 98L, new ActualizarEstadoReporteRequest(EstadoReporte.RESUELTO)))
                .isInstanceOf(ReporteNoPermitidoException.class);
    }

    @Test
    void obtenerDetalle_reporteInexistente_lanzaNoEncontrado() {
        when(reporteRepository.findDetalleById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerDetalle(1L))
                .isInstanceOf(ReporteNoEncontradoException.class);
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
                .rolGlobal(RolGlobal.builder()
                        .codigo(codigoRol)
                        .nombre("ADMINISTRADOR".equals(codigoRol) ? "Administrador" : "Usuario")
                        .build())
                .build();
    }
}
