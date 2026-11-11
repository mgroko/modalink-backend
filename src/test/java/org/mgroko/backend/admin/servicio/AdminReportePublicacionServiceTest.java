package org.mgroko.backend.admin.servicio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.ActualizarEstadoReporteRequest;
import org.mgroko.backend.admin.dto.BuscarReportesPublicacionFiltro;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Reporte;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.reportes.exception.EstadoReporteInvalidoException;
import org.mgroko.backend.reportes.exception.RangoFechasReporteInvalidoException;
import org.mgroko.backend.reportes.exception.ReporteNoEncontradoException;
import org.mgroko.backend.reportes.exception.ReporteNoPermitidoException;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.ReporteRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminReportePublicacionServiceTest {

    private static final Long ID_REPORTE = 1L;
    private static final Long ID_ADMIN = 99L;

    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private AdminReportePublicacionService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-11T12:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires"));
        service = new AdminReportePublicacionService(
                reporteRepository, publicacionRepository, usuarioRepository, reloj);
    }

    @Test
    void buscar_rangoDeFechasInvalido_lanzaExcepcion() {
        BuscarReportesPublicacionFiltro filtro = new BuscarReportesPublicacionFiltro(
                null, null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1));

        assertThatThrownBy(() -> service.buscar(filtro, 0, 20, false))
                .isInstanceOf(RangoFechasReporteInvalidoException.class);
    }

    @Test
    void obtenerDetalle_reporteDePerfil_lanzaNoEncontrado() {
        when(reporteRepository.findDetalleById(ID_REPORTE))
                .thenReturn(Optional.of(reporte(EntidadReporte.PERFIL)));

        assertThatThrownBy(() -> service.obtenerDetalle(ID_REPORTE))
                .isInstanceOf(ReporteNoEncontradoException.class);
    }

    @Test
    void actualizarEstado_estadoPendiente_lanzaInvalido() {
        assertThatThrownBy(() -> service.actualizarEstado(
                ID_REPORTE,
                ID_ADMIN,
                new ActualizarEstadoReporteRequest(EstadoReporte.PENDIENTE)))
                .isInstanceOf(EstadoReporteInvalidoException.class);
    }

    @Test
    void actualizarEstado_usuarioNoAdministrador_lanzaNoPermitido() {
        when(usuarioRepository.findById(ID_ADMIN))
                .thenReturn(Optional.of(usuario("USUARIO", "Usuario")));

        assertThatThrownBy(() -> service.actualizarEstado(
                ID_REPORTE,
                ID_ADMIN,
                new ActualizarEstadoReporteRequest(EstadoReporte.RESUELTO)))
                .isInstanceOf(ReporteNoPermitidoException.class);
    }

    @Test
    void actualizarEstado_reporteDePerfil_lanzaNoEncontrado() {
        when(usuarioRepository.findById(ID_ADMIN))
                .thenReturn(Optional.of(usuario("ADMINISTRADOR", "Administrador")));
        when(reporteRepository.findByIdParaActualizar(ID_REPORTE))
                .thenReturn(Optional.of(reporte(EntidadReporte.PERFIL)));

        assertThatThrownBy(() -> service.actualizarEstado(
                ID_REPORTE,
                ID_ADMIN,
                new ActualizarEstadoReporteRequest(EstadoReporte.RESUELTO)))
                .isInstanceOf(ReporteNoEncontradoException.class);
    }

    @Test
    void actualizarEstado_reporteYaGestionado_lanzaInvalido() {
        Reporte reporte = reporte(EntidadReporte.PUBLICACION);
        reporte.setEstado(EstadoReporte.RESUELTO);
        when(usuarioRepository.findById(ID_ADMIN))
                .thenReturn(Optional.of(usuario("ADMINISTRADOR", "Administrador")));
        when(reporteRepository.findByIdParaActualizar(ID_REPORTE))
                .thenReturn(Optional.of(reporte));

        assertThatThrownBy(() -> service.actualizarEstado(
                ID_REPORTE,
                ID_ADMIN,
                new ActualizarEstadoReporteRequest(EstadoReporte.DESCARTADO)))
                .isInstanceOf(EstadoReporteInvalidoException.class);
    }

    private Reporte reporte(EntidadReporte entidadTipo) {
        return Reporte.builder()
                .idReporte(ID_REPORTE)
                .entidadTipo(entidadTipo)
                .entidadId(30L)
                .motivo(MotivoReporte.SPAM)
                .estado(EstadoReporte.PENDIENTE)
                .perfilAutor(Perfil.builder().idPerfil(10L).build())
                .usuarioAutor(usuario("USUARIO", "Usuario"))
                .build();
    }

    private Usuario usuario(String codigoRol, String nombreRol) {
        return Usuario.builder()
                .idUsuario(ID_ADMIN)
                .nombre("Admin")
                .apellido("Sistema")
                .rolGlobal(RolGlobal.builder()
                        .codigo(codigoRol)
                        .nombre(nombreRol)
                        .build())
                .build();
    }
}
