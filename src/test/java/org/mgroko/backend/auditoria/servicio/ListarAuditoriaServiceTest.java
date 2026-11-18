package org.mgroko.backend.auditoria.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auditoria.exception.AuditoriaFiltroInvalidoException;
import org.mgroko.backend.auditoria.mapper.AuditoriaMapper;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Auditoria;
import org.mgroko.backend.modelo.TipoEventoAuditoria;
import org.mgroko.backend.repositorio.AuditoriaRepository;
import org.mgroko.backend.repositorio.DetalleAuditoriaRepository;
import org.mgroko.backend.repositorio.TipoEventoAuditoriaRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ListarAuditoriaServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 7);
    private static final TipoEventoAuditoria USUARIO_ALTA = new TipoEventoAuditoria(
            "USUARIO_ALTA", "Alta de usuario", "Registro", "USUARIOS", true);

    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private DetalleAuditoriaRepository detalleAuditoriaRepository;
    @Mock
    private TipoEventoAuditoriaRepository tipoEventoAuditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    private ListarAuditoriaService service;

    @BeforeEach
    void setUp() {
        AuditoriaFiltroService auditoriaFiltroService = new AuditoriaFiltroService(
                configuracionSistemaService,
                tipoEventoAuditoriaRepository);

        service = new ListarAuditoriaService(
                auditoriaRepository,
                detalleAuditoriaRepository,
                tipoEventoAuditoriaRepository,
                usuarioRepository,
                auditoriaFiltroService,
                new AuditoriaMapper());

        lenient().when(configuracionSistemaService.obtenerRangoMaximoAuditoriaDias())
                .thenReturn(90);
        lenient().when(configuracionSistemaService.obtenerPaginaMaximaAuditoria())
                .thenReturn(100);
        lenient().when(tipoEventoAuditoriaRepository
                .findByActivoTrueOrderByModuloAscNombreAsc())
                .thenReturn(List.of(USUARIO_ALTA));
        lenient().when(auditoriaRepository.findAll(
                        any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.<Auditoria>empty());
    }

    @Test
    void listarTipos_devuelveCatalogoActivo() {
        var tipos = service.listarTipos();

        assertThat(tipos).hasSize(1);
        assertThat(tipos.get(0).codigo()).isEqualTo("USUARIO_ALTA");
        assertThat(tipos.get(0).modulo()).isEqualTo("USUARIOS");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosDeFiltros")
    void listar_aplicaLasReglasDeLaTablaDeDecision(
            String nombre,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            List<String> tipos,
            int page,
            int size,
            String codigoError) {

        if (codigoError != null) {
            assertThatThrownBy(() -> service.listar(
                fechaDesde, fechaHasta, tipos, null, null, page, size))
                    .isInstanceOfSatisfying(
                            AuditoriaFiltroInvalidoException.class,
                            ex -> assertThat(ex.getCodigo()).isEqualTo(codigoError));
            return;
        }

        assertThatCode(() -> {
            PaginaResponse<?> pagina = service.listar(
                fechaDesde, fechaHasta, tipos, null, null, page, size);
            assertThat(pagina.contenido()).isEmpty();
        }).doesNotThrowAnyException();
    }

    @Test
    void listar_sinTiposConsultaTodosLosActivos() {
        service.listar(DESDE, HASTA, null, null, null, 0, 20);

        verify(auditoriaRepository).findAll(
                any(Specification.class), any(Pageable.class));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> casosDeFiltros() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "fechas nulas", null, null, null, 0, 20, "FECHAS_OBLIGATORIAS"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "falta fecha hasta", DESDE, null, null, 0, 20, "FECHAS_OBLIGATORIAS"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "falta fecha desde", null, HASTA, null, 0, 20, "FECHAS_OBLIGATORIAS"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "desde posterior a hasta", HASTA, DESDE, null, 0, 20, "RANGO_FECHAS_INVALIDO"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "rango excedido", DESDE, DESDE.plusDays(90), null, 0, 20, "RANGO_FECHAS_EXCEDIDO"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "sin tipos", DESDE, HASTA, null, 0, 20, null),
                org.junit.jupiter.params.provider.Arguments.of(
                        "tipo valido", DESDE, HASTA, List.of("USUARIO_ALTA"), 0, 20, null),
                org.junit.jupiter.params.provider.Arguments.of(
                        "tipo invalido", DESDE, HASTA, List.of("NO_EXISTE"), 0, 20, "TIPO_EVENTO_INVALIDO"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "tipos duplicados", DESDE, HASTA, List.of("USUARIO_ALTA", " usuario_alta "), 0, 20, null),
                org.junit.jupiter.params.provider.Arguments.of(
                        "pagina negativa", DESDE, HASTA, null, -1, 20, "PAGINACION_INVALIDA"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "tamanio cero", DESDE, HASTA, null, 0, 0, "PAGINACION_INVALIDA"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "tamanio excedido", DESDE, HASTA, null, 0, 101, "PAGINACION_INVALIDA"));
    }
}
