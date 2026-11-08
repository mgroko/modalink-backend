package org.mgroko.backend.admin.servicio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.AdminTipoContratoCreateRequest;
import org.mgroko.backend.admin.dto.AdminTipoContratoEstadoRequest;
import org.mgroko.backend.admin.dto.AdminTipoContratoResponse;
import org.mgroko.backend.admin.dto.AdminTipoContratoResumenResponse;
import org.mgroko.backend.admin.dto.AdminTipoContratoUpdateRequest;
import org.mgroko.backend.admin.dto.ValidacionPlantillaResponse;
import org.mgroko.backend.admin.dto.ValidarPlantillaRequest;
import org.mgroko.backend.admin.exception.PlantillaContratoInvalidaException;
import org.mgroko.backend.admin.exception.TipoContratoDuplicadoException;
import org.mgroko.backend.admin.exception.TipoContratoEnUsoException;
import org.mgroko.backend.admin.exception.TipoContratoNoEncontradoException;
import org.mgroko.backend.admin.exception.TipoContratoNoModificableException;
import org.mgroko.backend.admin.exception.UltimoTipoContratoActivoException;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.TipoAcuerdoContratoRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminTipoContratoServiceTest {

    private static final String PLANTILLA_VALIDA = """
            CONTRATO DE {{tipo}}

            Proyecto: {{proyecto}}
            Director: {{director}}
            Partes:
            {{partes}}

            {{clausulas_adicionales}}
            """;

    @Mock
    private TipoAcuerdoContratoRepository tipoAcuerdoRepository;

    @Mock
    private ContratoRepository contratoRepository;

    @InjectMocks
    private AdminTipoContratoService service;

    private TipoAcuerdoContrato tipo;

    @BeforeEach
    void setUp() {
        tipo = TipoAcuerdoContrato.builder()
                .idTipoAcuerdo(1L)
                .codigo("ACUERDO_GENERAL")
                .nombre("Acuerdo general")
                .descripcion("Acuerdo general entre las partes.")
                .plantilla(PLANTILLA_VALIDA)
                .activo(true)
                .build();
    }

    private void mockUso(long asociados, long vivos, long pendientes) {
        when(contratoRepository.countByTipoAcuerdo_IdTipoAcuerdo(1L))
                .thenReturn(asociados);
        when(contratoRepository.countByTipoAcuerdo_IdTipoAcuerdoAndEstadoIn(
                eq(1L), anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<EstadoContrato> estados = invocation.getArgument(1);
                    return estados.contains(EstadoContrato.ACTIVO) ? vivos : pendientes;
                });
    }

    // ------------------------------------------------------------- listar

    @Test
    void listar_incluyeInactivosYFiltraPorNombre() {
        TipoAcuerdoContrato inactivo = TipoAcuerdoContrato.builder()
                .idTipoAcuerdo(2L)
                .codigo("ACUERDO_VIEJO")
                .nombre("Acuerdo viejo")
                .plantilla(PLANTILLA_VALIDA)
                .activo(false)
                .build();
        when(tipoAcuerdoRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(tipo, inactivo));
        when(contratoRepository.countByTipoAcuerdo_IdTipoAcuerdo(any()))
                .thenReturn(0L);
        when(contratoRepository.countByTipoAcuerdo_IdTipoAcuerdoAndEstadoIn(
                any(), anyCollection())).thenReturn(0L);
        when(tipoAcuerdoRepository.countByActivoTrue()).thenReturn(1L);

        List<AdminTipoContratoResumenResponse> todos =
                service.listar(null, true);
        List<AdminTipoContratoResumenResponse> soloActivos =
                service.listar(null, false);
        List<AdminTipoContratoResumenResponse> filtrados =
                service.listar("general", true);

        assertEquals(2, todos.size());
        assertEquals(1, soloActivos.size());
        assertEquals(1, filtrados.size());
        assertEquals("Acuerdo general", filtrados.get(0).nombre());
    }

    @Test
    void obtener_inexistente_lanza404() {
        when(tipoAcuerdoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TipoContratoNoEncontradoException.class,
                () -> service.obtener(99L));
    }

    // -------------------------------------------------------------- crear

    @Test
    void crear_datosValidos_generaCodigoYGuarda() {
        when(tipoAcuerdoRepository.existsByNombreIgnoreCase("Acuerdo marco"))
                .thenReturn(false);
        when(tipoAcuerdoRepository.existsByCodigoIgnoreCase("ACUERDO_MARCO"))
                .thenReturn(false);
        when(tipoAcuerdoRepository.save(any(TipoAcuerdoContrato.class)))
                .thenAnswer(invocation -> {
                    TipoAcuerdoContrato guardado = invocation.getArgument(0);
                    guardado.setIdTipoAcuerdo(9L);
                    return guardado;
                });
        when(tipoAcuerdoRepository.countByActivoTrue()).thenReturn(2L);
        mockUsoConId(9L, 0, 0, 0);

        AdminTipoContratoResponse response = service.crear(
                new AdminTipoContratoCreateRequest(
                        "  Acuerdo marco  ",
                        "  Marco general.  ",
                        PLANTILLA_VALIDA,
                        null));

        assertEquals(9L, response.idTipoAcuerdo());
        assertEquals("ACUERDO_MARCO", response.codigo());
        assertEquals("Acuerdo marco", response.nombre());
        assertEquals("Marco general.", response.descripcion());
        assertTrue(response.activo());
        assertTrue(response.puedeEditar());
        assertTrue(response.puedeEliminar());
    }

    @Test
    void crear_nombreDuplicado_lanza409() {
        when(tipoAcuerdoRepository.existsByNombreIgnoreCase("Acuerdo general"))
                .thenReturn(true);

        assertThrows(TipoContratoDuplicadoException.class,
                () -> service.crear(new AdminTipoContratoCreateRequest(
                        "Acuerdo general", null, PLANTILLA_VALIDA, true)));
        verify(tipoAcuerdoRepository, never()).save(any(TipoAcuerdoContrato.class));
    }

    @Test
    void crear_codigoExistente_agregaSufijo() {
        when(tipoAcuerdoRepository.existsByNombreIgnoreCase("Acuerdo general"))
                .thenReturn(false);
        when(tipoAcuerdoRepository.existsByCodigoIgnoreCase("ACUERDO_GENERAL"))
                .thenReturn(true);
        when(tipoAcuerdoRepository.existsByCodigoIgnoreCase("ACUERDO_GENERAL_2"))
                .thenReturn(false);
        when(tipoAcuerdoRepository.save(any(TipoAcuerdoContrato.class)))
                .thenAnswer(invocation -> {
                    TipoAcuerdoContrato guardado = invocation.getArgument(0);
                    guardado.setIdTipoAcuerdo(9L);
                    return guardado;
                });
        when(tipoAcuerdoRepository.countByActivoTrue()).thenReturn(1L);
        mockUsoConId(9L, 0, 0, 0);

        AdminTipoContratoResponse response = service.crear(
                new AdminTipoContratoCreateRequest(
                        "Acuerdo general", null, PLANTILLA_VALIDA, true));

        assertEquals("ACUERDO_GENERAL_2", response.codigo());
    }

    @Test
    void crear_plantillaConPlaceholderDesconocido_lanza400() {
        PlantillaContratoInvalidaException ex = assertThrows(
                PlantillaContratoInvalidaException.class,
                () -> service.crear(new AdminTipoContratoCreateRequest(
                        "Acuerdo raro", null,
                        "Proyecto: {{proyecto}} {{desconocido}}", true)));

        assertTrue(ex.getErrores().stream()
                .anyMatch(e -> e.contains("{{desconocido}}")));
        verify(tipoAcuerdoRepository, never()).save(any(TipoAcuerdoContrato.class));
    }

    @Test
    void crear_inactivoSinTiposActivos_lanzaUltimoActivo() {
        when(tipoAcuerdoRepository.existsByNombreIgnoreCase("Acuerdo nuevo"))
                .thenReturn(false);
        when(tipoAcuerdoRepository.countByActivoTrue()).thenReturn(0L);

        assertThrows(UltimoTipoContratoActivoException.class,
                () -> service.crear(new AdminTipoContratoCreateRequest(
                        "Acuerdo nuevo", null, PLANTILLA_VALIDA, false)));
        verify(tipoAcuerdoRepository, never()).save(any(TipoAcuerdoContrato.class));
    }

    // ---------------------------------------------------------- actualizar

    @Test
    void actualizar_conContratosVivos_bloqueaNombreYPlantilla() {
        when(tipoAcuerdoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(tipoAcuerdoRepository.existsByNombreIgnoreCaseAndIdTipoAcuerdoNot(
                "Acuerdo general", 1L)).thenReturn(false);
        mockUso(1, 1, 0);

        assertThrows(TipoContratoNoModificableException.class,
                () -> service.actualizar(1L, new AdminTipoContratoUpdateRequest(
                        "Acuerdo general", "Nueva descripción",
                        PLANTILLA_VALIDA + "\nCláusula extra.")));
        verify(tipoAcuerdoRepository, never()).save(any(TipoAcuerdoContrato.class));
    }

    @Test
    void actualizar_conContratosVivos_permiteCambiarSoloDescripcion() {
        when(tipoAcuerdoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(tipoAcuerdoRepository.existsByNombreIgnoreCaseAndIdTipoAcuerdoNot(
                "Acuerdo general", 1L)).thenReturn(false);
        when(tipoAcuerdoRepository.save(any(TipoAcuerdoContrato.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        mockUso(1, 1, 0);

        AdminTipoContratoResponse response = service.actualizar(
                1L, new AdminTipoContratoUpdateRequest(
                        "Acuerdo general", "Descripción actualizada", PLANTILLA_VALIDA));

        assertEquals("Descripción actualizada", response.descripcion());
    }

    @Test
    void actualizar_conSoloContratosCerrados_permiteNombreYPlantilla() {
        when(tipoAcuerdoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(tipoAcuerdoRepository.existsByNombreIgnoreCaseAndIdTipoAcuerdoNot(
                "Acuerdo marco", 1L)).thenReturn(false);
        when(tipoAcuerdoRepository.save(any(TipoAcuerdoContrato.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        mockUso(1, 0, 0);

        AdminTipoContratoResponse response = service.actualizar(
                1L, new AdminTipoContratoUpdateRequest(
                        "Acuerdo marco", null, PLANTILLA_VALIDA));

        assertEquals("Acuerdo marco", response.nombre());
        assertEquals("ACUERDO_GENERAL", response.codigo());
    }

    // ----------------------------------------------------------- estado

    @Test
    void desactivar_conContratosActivosPeroSinPendientes_permite() {
        when(tipoAcuerdoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(tipoAcuerdoRepository.countByActivoTrue()).thenReturn(2L);
        when(tipoAcuerdoRepository.save(any(TipoAcuerdoContrato.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        mockUso(1, 1, 0);

        AdminTipoContratoResponse response = service.cambiarEstado(
                1L, new AdminTipoContratoEstadoRequest(false));

        assertFalse(response.activo());
    }

    @Test
    void desactivar_conContratosPendientes_lanza409() {
        when(tipoAcuerdoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        mockUso(1, 1, 1);

        assertThrows(TipoContratoNoModificableException.class,
                () -> service.cambiarEstado(
                        1L, new AdminTipoContratoEstadoRequest(false)));
        verify(tipoAcuerdoRepository, never()).save(any(TipoAcuerdoContrato.class));
    }

    @Test
    void desactivar_ultimoActivo_lanza409() {
        when(tipoAcuerdoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(tipoAcuerdoRepository.countByActivoTrue()).thenReturn(1L);
        mockUso(0, 0, 0);

        assertThrows(UltimoTipoContratoActivoException.class,
                () -> service.cambiarEstado(
                        1L, new AdminTipoContratoEstadoRequest(false)));
    }

    @Test
    void activar_conNombreDuplicado_lanza409() {
        TipoAcuerdoContrato inactivo = TipoAcuerdoContrato.builder()
                .idTipoAcuerdo(1L)
                .codigo("ACUERDO_GENERAL")
                .nombre("Acuerdo general")
                .plantilla(PLANTILLA_VALIDA)
                .activo(false)
                .build();
        when(tipoAcuerdoRepository.findById(1L)).thenReturn(Optional.of(inactivo));
        when(tipoAcuerdoRepository
                .existsByNombreIgnoreCaseAndActivoTrueAndIdTipoAcuerdoNot(
                        "Acuerdo general", 1L))
                .thenReturn(true);

        assertThrows(TipoContratoDuplicadoException.class,
                () -> service.cambiarEstado(
                        1L, new AdminTipoContratoEstadoRequest(true)));
    }

    // --------------------------------------------------------- eliminar

    @Test
    void eliminar_conContratos_lanza409() {
        when(tipoAcuerdoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(contratoRepository.existsByTipoAcuerdo_IdTipoAcuerdo(1L))
                .thenReturn(true);

        assertThrows(TipoContratoEnUsoException.class,
                () -> service.eliminar(1L));
        verify(tipoAcuerdoRepository, never()).delete(any(TipoAcuerdoContrato.class));
    }

    @Test
    void eliminar_sinContratosYPendientes_elimina() {
        when(tipoAcuerdoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(contratoRepository.existsByTipoAcuerdo_IdTipoAcuerdo(1L))
                .thenReturn(false);
        when(tipoAcuerdoRepository.countByActivoTrue()).thenReturn(2L);

        service.eliminar(1L);

        verify(tipoAcuerdoRepository).delete(tipo);
    }

    @Test
    void eliminar_ultimoActivoSinContratos_lanza409() {
        when(tipoAcuerdoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(contratoRepository.existsByTipoAcuerdo_IdTipoAcuerdo(1L))
                .thenReturn(false);
        when(tipoAcuerdoRepository.countByActivoTrue()).thenReturn(1L);

        assertThrows(UltimoTipoContratoActivoException.class,
                () -> service.eliminar(1L));
    }

    // --------------------------------------------------- validar plantilla

    @Test
    void validarPlantilla_validaConAdvertencias() {
        ValidacionPlantillaResponse response = service.validarPlantilla(
                new ValidarPlantillaRequest("Texto sin placeholders."));

        assertTrue(response.valido());
        assertEquals(3, response.advertencias().size());
        assertFalse(response.tieneClausulasAdicionales());
    }

    @Test
    void validarPlantilla_detectaPlaceholderConFormatoInvalido() {
        ValidacionPlantillaResponse response = service.validarPlantilla(
                new ValidarPlantillaRequest("{{ proyecto }}"));

        assertFalse(response.valido());
        assertEquals(List.of("{{ proyecto }}"),
                response.placeholdersConFormatoInvalido());
    }

    @Test
    void validarPlantilla_detectaDesconocido() {
        ValidacionPlantillaResponse response = service.validarPlantilla(
                new ValidarPlantillaRequest("{{foo}}"));

        assertFalse(response.valido());
        assertEquals(List.of("{{foo}}"), response.placeholdersDesconocidos());
    }

    private void mockUsoConId(Long id, long asociados, long vivos, long pendientes) {
        when(contratoRepository.countByTipoAcuerdo_IdTipoAcuerdo(id))
                .thenReturn(asociados);
        when(contratoRepository.countByTipoAcuerdo_IdTipoAcuerdoAndEstadoIn(
                eq(id), anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<EstadoContrato> estados = invocation.getArgument(1);
                    return estados.contains(EstadoContrato.ACTIVO) ? vivos : pendientes;
                });
    }
}
