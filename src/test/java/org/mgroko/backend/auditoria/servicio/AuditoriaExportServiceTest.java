package org.mgroko.backend.auditoria.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auditoria.dto.AuditoriaFiltroResuelto;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenResponse;
import org.mgroko.backend.auditoria.mapper.AuditoriaMapper;
import org.mgroko.backend.modelo.Auditoria;
import org.mgroko.backend.modelo.DetalleAuditoria;
import org.mgroko.backend.repositorio.AuditoriaRepository;
import org.mgroko.backend.repositorio.DetalleAuditoriaRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AuditoriaExportServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private DetalleAuditoriaRepository detalleAuditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditoriaFiltroService auditoriaFiltroService;
    @Mock
    private AuditoriaResumenService auditoriaResumenService;
    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    private AuditoriaExportService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaExportService(
                auditoriaRepository,
                detalleAuditoriaRepository,
                usuarioRepository,
                auditoriaFiltroService,
                auditoriaResumenService,
                new AuditoriaMapper(),
                configuracionSistemaService,
                Clock.systemUTC());

        lenient().when(configuracionSistemaService.obtenerMaximoExportacionAuditoria())
                .thenReturn(10_000);
        lenient().when(configuracionSistemaService.obtenerMaxCambiosPdfAuditoria())
                .thenReturn(5);
    }

    @Test
    void csvEscapaSeparadoresComillasYFormulas() {
        Auditoria auditoria = Auditoria.builder()
                .idAuditoria(1L)
                .fechaHora(LocalDateTime.of(2026, 11, 7, 10, 0))
                .tipoEvento("USUARIO_ALTA")
                .tipoEventoOriginal("USUARIO_ALTA")
                .entidadAfectada("=SUM(A1)")
                .entidadClave("a;b")
                .idAfectado(7L)
                .usuarioBd("x\"y")
                .ipUsuario("127.0.0.1")
                .build();

        DetalleAuditoria detalle = DetalleAuditoria.builder()
                .idDetalleAuditoria(1L)
                .idAuditoria(1L)
                .campo("estado")
                .valorAnterior("ACTIVO")
                .valorNuevo("DESHABILITADO")
                .build();

        when(auditoriaRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(auditoria));
        when(detalleAuditoriaRepository
                .findByIdAuditoriaInOrderByIdDetalleAuditoriaAsc(any()))
                .thenReturn(List.of(detalle));

        AuditoriaFiltroResuelto filtro = new AuditoriaFiltroResuelto(
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 7, 23, 59, 59),
                null,
                Map.of());

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        service.exportarCsv(filtro, salida);

        String csv = salida.toString(StandardCharsets.UTF_8);
        assertThat(csv)
                .contains("id_auditoria;fecha_hora;tipo_evento")
                .contains("'=SUM(A1)")
                .contains("\"a;b\"")
                .contains("\"x\"\"y\"");
    }

    @Test
    void pdfSoportaAcentosYCaracteresNoLatinos() throws Exception {
        Auditoria auditoria = Auditoria.builder()
                .idAuditoria(1L)
                .fechaHora(LocalDateTime.of(2026, 11, 7, 10, 0))
                .tipoEvento("USUARIO_ALTA")
                .tipoEventoOriginal("USUARIO_ALTA")
                .entidadAfectada("ñandú áéíóú ü 中文 😀")
                .idAfectado(7L)
                .usuarioBd("postgres")
                .build();

        DetalleAuditoria valor = DetalleAuditoria.builder()
                .idDetalleAuditoria(1L)
                .idAuditoria(1L)
                .campo("valor")
                .valorAnterior("90")
                .valorNuevo("10000")
                .build();
        DetalleAuditoria descripcion = DetalleAuditoria.builder()
                .idDetalleAuditoria(2L)
                .idAuditoria(1L)
                .campo("descripcion")
                .valorAnterior(null)
                .valorNuevo("Máximo de registros exportables")
                .build();

        when(auditoriaRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(auditoria));
        when(detalleAuditoriaRepository
                .findByIdAuditoriaInOrderByIdDetalleAuditoriaAsc(any()))
                .thenReturn(List.of(valor, descripcion));
        when(auditoriaResumenService.obtenerResumen(any(), any(), any()))
                .thenReturn(new AuditoriaResumenResponse(
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 7),
                        List.of(),
                        1L,
                        List.of(),
                        List.of()));

        AuditoriaFiltroResuelto filtro = new AuditoriaFiltroResuelto(
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 7, 23, 59, 59),
                null,
                Map.of());

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        service.exportarPdf(filtro, 1L, salida);

        assertThat(salida.toByteArray())
                .startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    }
}
