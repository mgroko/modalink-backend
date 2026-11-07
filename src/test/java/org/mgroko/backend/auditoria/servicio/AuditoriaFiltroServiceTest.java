package org.mgroko.backend.auditoria.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auditoria.exception.AuditoriaFiltroInvalidoException;
import org.mgroko.backend.modelo.TipoEventoAuditoria;
import org.mgroko.backend.repositorio.TipoEventoAuditoriaRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditoriaFiltroServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 7);

    private static final TipoEventoAuditoria USUARIO_ALTA = new TipoEventoAuditoria(
            "USUARIO_ALTA", "Alta de usuario", "Registro", "USUARIOS", true);

    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    @Mock
    private TipoEventoAuditoriaRepository tipoEventoAuditoriaRepository;

    private AuditoriaFiltroService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaFiltroService(
                configuracionSistemaService,
                tipoEventoAuditoriaRepository);

        lenient().when(configuracionSistemaService.obtenerRangoMaximoAuditoriaDias())
                .thenReturn(90);
        lenient().when(configuracionSistemaService.obtenerPaginaMaximaAuditoria())
                .thenReturn(100);
    }

    @Test
    void sinTiposDevuelveNullParaNoFiltrar() {
        when(tipoEventoAuditoriaRepository.findByActivoTrueOrderByModuloAscNombreAsc())
                .thenReturn(List.of(USUARIO_ALTA));

        var filtro = service.resolver(DESDE, HASTA, null);

        assertThat(filtro.tipos()).isNull();
        assertThat(filtro.tieneFiltroDeTipos()).isFalse();
    }

    @Test
    void conTiposValidosNormalizaYDeduplica() {
        when(tipoEventoAuditoriaRepository.findByActivoTrueOrderByModuloAscNombreAsc())
                .thenReturn(List.of(USUARIO_ALTA));

        var filtro = service.resolver(
                DESDE, HASTA, List.of(" usuario_alta ", "USUARIO_ALTA"));

        assertThat(filtro.tipos()).containsExactly("USUARIO_ALTA");
    }

    @Test
    void catalogoVacioSinTiposNoFiltra() {
        when(tipoEventoAuditoriaRepository.findByActivoTrueOrderByModuloAscNombreAsc())
                .thenReturn(List.of());

        var filtro = service.resolver(DESDE, HASTA, null);

        assertThat(filtro.tipos()).isNull();
    }

    @Test
    void catalogoVacioConTiposDevuelveCatalogoVacio() {
        when(tipoEventoAuditoriaRepository.findByActivoTrueOrderByModuloAscNombreAsc())
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.resolver(DESDE, HASTA, List.of("USUARIO_ALTA")))
                .isInstanceOfSatisfying(
                        AuditoriaFiltroInvalidoException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("CATALOGO_VACIO"));
    }

    @Test
    void tipoDesconocidoDevuelveError() {
        when(tipoEventoAuditoriaRepository.findByActivoTrueOrderByModuloAscNombreAsc())
                .thenReturn(List.of(USUARIO_ALTA));

        assertThatThrownBy(() -> service.resolver(DESDE, HASTA, List.of("NO_EXISTE")))
                .isInstanceOfSatisfying(
                        AuditoriaFiltroInvalidoException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("TIPO_EVENTO_INVALIDO"));
    }

    @Test
    void paginaMayorAlMaximoConfiguradoDevuelveError() {
        assertThatThrownBy(() -> service.validarPaginacion(0, 101))
                .isInstanceOfSatisfying(
                        AuditoriaFiltroInvalidoException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("PAGINACION_INVALIDA"));
    }
}
