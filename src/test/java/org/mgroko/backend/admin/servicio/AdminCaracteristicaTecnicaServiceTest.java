package org.mgroko.backend.admin.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.AdminCaracteristicaTecnicaRequest;
import org.mgroko.backend.admin.dto.AdminValorCaracteristicaRequest;
import org.mgroko.backend.admin.exception.CaracteristicaCodigoDuplicadoException;
import org.mgroko.backend.admin.exception.CaracteristicaEnUsoException;
import org.mgroko.backend.admin.exception.CaracteristicaTecnicaNoEncontradaException;
import org.mgroko.backend.admin.exception.TipoDatoInvalidoException;
import org.mgroko.backend.admin.exception.ValorCaracteristicaAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.ValorCodigoDuplicadoException;
import org.mgroko.backend.admin.exception.ValorEnUsoException;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.dto.ValorCaracteristicaResponse;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.repositorio.CaracteristicaPerfilRepository;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminCaracteristicaTecnicaServiceTest {

    @Mock
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Mock
    private ValorCaracteristicaRepository valorCaracteristicaRepository;

    @Mock
    private CaracteristicaPerfilRepository caracteristicaPerfilRepository;

    @Mock
    private ProfesionRepository profesionRepository;

    @InjectMocks
    private AdminCaracteristicaTecnicaService service;

    private Profesion profesion;

    @BeforeEach
    void setUp() {
        profesion = Profesion.builder()
                .idProfesion(1L)
                .nombre("modelo")
                .descripcion("Profesión modelo")
                .build();
    }

    // --- LISTAR ---

    @Test
    void listar_retornaListaOrdenada() {
        CaracteristicaTecnica c = CaracteristicaTecnica.builder()
                .idCaracteristica(1L)
                .codigo("altura")
                .unidad("cm")
                .tipoDato("NUMERICO")
                .profesion(profesion)
                .valores(List.of())
                .build();

        when(caracteristicaTecnicaRepository.findAllByOrderByCodigo()).thenReturn(List.of(c));

        List<CaracteristicaTecnicaResponse> resultado = service.listar();

        assertEquals(1, resultado.size());
        assertEquals("altura", resultado.get(0).codigo());
    }

    // --- CREAR ---

    @Test
    void crear_numericoValido_persisteYRetornaResponse() {
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "altura", "cm", 1L, "NUMERICO", null);

        when(caracteristicaTecnicaRepository.existsByCodigo("altura")).thenReturn(false);
        when(profesionRepository.findById(1L)).thenReturn(Optional.of(profesion));
        when(caracteristicaTecnicaRepository.save(any(CaracteristicaTecnica.class)))
                .thenAnswer(inv -> {
                    CaracteristicaTecnica ent = inv.getArgument(0);
                    ent.setIdCaracteristica(10L);
                    return ent;
                });

        CaracteristicaTecnicaResponse response = service.crear(request);

        assertNotNull(response);
        assertEquals("altura", response.codigo());
        assertEquals("NUMERICO", response.tipoDato());
        verify(caracteristicaTecnicaRepository).save(any(CaracteristicaTecnica.class));
    }

    @Test
    void crear_enumeradoConValores_persisteValoresAsociados() {
        AdminValorCaracteristicaRequest v1 = new AdminValorCaracteristicaRequest(null, "AZUL", "#0000FF");
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "ojos", null, 1L, "ENUMERADO", List.of(v1));

        when(caracteristicaTecnicaRepository.existsByCodigo("ojos")).thenReturn(false);
        when(profesionRepository.findById(1L)).thenReturn(Optional.of(profesion));
        when(caracteristicaTecnicaRepository.save(any(CaracteristicaTecnica.class)))
                .thenAnswer(inv -> {
                    CaracteristicaTecnica ent = inv.getArgument(0);
                    ent.setIdCaracteristica(20L);
                    return ent;
                });

        CaracteristicaTecnicaResponse response = service.crear(request);

        assertNotNull(response);
        assertEquals("ojos", response.codigo());
        verify(valorCaracteristicaRepository).save(any(ValorCaracteristica.class));
    }

    @Test
    void crear_tipoDatoInvalido_lanzaTipoDatoInvalidoException() {
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "test", null, 1L, "BOOLEAN", null);

        assertThrows(TipoDatoInvalidoException.class, () -> service.crear(request));
        verify(caracteristicaTecnicaRepository, never()).save(any());
    }

    @Test
    void crear_codigoDuplicado_lanzaCaracteristicaCodigoDuplicadoException() {
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "altura", "cm", 1L, "NUMERICO", null);

        when(caracteristicaTecnicaRepository.existsByCodigo("altura")).thenReturn(true);

        assertThrows(CaracteristicaCodigoDuplicadoException.class, () -> service.crear(request));
        verify(caracteristicaTecnicaRepository, never()).save(any());
    }

    @Test
    void crear_profesionInexistente_lanzaProfesionNoEncontradaException() {
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "altura", "cm", 99L, "NUMERICO", null);

        when(caracteristicaTecnicaRepository.existsByCodigo("altura")).thenReturn(false);
        when(profesionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProfesionNoEncontradaException.class, () -> service.crear(request));
        verify(caracteristicaTecnicaRepository, never()).save(any());
    }

    @Test
    void crear_noEnumeradoConValores_lanzaTipoDatoInvalidoException() {
        AdminValorCaracteristicaRequest v1 = new AdminValorCaracteristicaRequest(null, "180", null);
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "altura", "cm", 1L, "NUMERICO", List.of(v1));

        when(caracteristicaTecnicaRepository.existsByCodigo("altura")).thenReturn(false);
        when(profesionRepository.findById(1L)).thenReturn(Optional.of(profesion));

        assertThrows(TipoDatoInvalidoException.class, () -> service.crear(request));
        verify(caracteristicaTecnicaRepository, never()).save(any());
    }

    @Test
    void crear_enumeradoConValoresDuplicados_lanzaValorCodigoDuplicadoException() {
        AdminValorCaracteristicaRequest v1 = new AdminValorCaracteristicaRequest(null, "AZUL", null);
        AdminValorCaracteristicaRequest v2 = new AdminValorCaracteristicaRequest(null, "AZUL", null);
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "ojos", null, 1L, "ENUMERADO", List.of(v1, v2));

        when(caracteristicaTecnicaRepository.existsByCodigo("ojos")).thenReturn(false);
        when(profesionRepository.findById(1L)).thenReturn(Optional.of(profesion));
        when(caracteristicaTecnicaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(ValorCodigoDuplicadoException.class, () -> service.crear(request));
    }

    // --- ACTUALIZAR ---

    @Test
    void actualizar_valido_modificaYGuarda() {
        CaracteristicaTecnica existente = CaracteristicaTecnica.builder()
                .idCaracteristica(10L)
                .codigo("altura")
                .unidad("cm")
                .tipoDato("NUMERICO")
                .profesion(profesion)
                .valores(new ArrayList<>())
                .build();

        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "altura_cm", "centimetros", 1L, "NUMERICO", null);

        when(caracteristicaTecnicaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(caracteristicaTecnicaRepository.existsByCodigo("altura_cm")).thenReturn(false);
        when(profesionRepository.findById(1L)).thenReturn(Optional.of(profesion));
        when(caracteristicaTecnicaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CaracteristicaTecnicaResponse response = service.actualizar(10L, request);

        assertEquals("altura_cm", response.codigo());
        assertEquals("centimetros", response.unidad());
    }

    @Test
    void actualizar_inexistente_lanzaCaracteristicaTecnicaNoEncontradaException() {
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "altura", "cm", 1L, "NUMERICO", null);

        when(caracteristicaTecnicaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CaracteristicaTecnicaNoEncontradaException.class,
                () -> service.actualizar(99L, request));
    }

    @Test
    void actualizar_pasaANoEnumeradoConValores_lanzaCaracteristicaEnUsoException() {
        ValorCaracteristica val = ValorCaracteristica.builder().idValor(1L).codigo("AZUL").build();
        CaracteristicaTecnica existente = CaracteristicaTecnica.builder()
                .idCaracteristica(10L)
                .codigo("ojos")
                .tipoDato("ENUMERADO")
                .profesion(profesion)
                .valores(List.of(val))
                .build();

        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "ojos", null, 1L, "TEXTO", null);

        when(caracteristicaTecnicaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(profesionRepository.findById(1L)).thenReturn(Optional.of(profesion));

        assertThrows(CaracteristicaEnUsoException.class,
                () -> service.actualizar(10L, request));
    }

    // --- ELIMINAR ---

    @Test
    void eliminar_validoSinValores_eliminaEntidad() {
        CaracteristicaTecnica c = CaracteristicaTecnica.builder()
                .idCaracteristica(5L)
                .valores(List.of())
                .build();

        when(caracteristicaTecnicaRepository.findById(5L)).thenReturn(Optional.of(c));
        when(caracteristicaPerfilRepository.existsByCaracteristicaTecnicaIdCaracteristica(5L)).thenReturn(false);

        service.eliminar(5L);

        verify(caracteristicaTecnicaRepository).delete(c);
    }

    @Test
    void eliminar_conValoresDeCatalogo_eliminaValoresYEntidad() {
        ValorCaracteristica val = ValorCaracteristica.builder().idValor(1L).codigo("AZUL").build();
        CaracteristicaTecnica c = CaracteristicaTecnica.builder()
                .idCaracteristica(5L)
                .valores(List.of(val))
                .build();

        when(caracteristicaTecnicaRepository.findById(5L)).thenReturn(Optional.of(c));
        when(caracteristicaPerfilRepository.existsByCaracteristicaTecnicaIdCaracteristica(5L)).thenReturn(false);

        service.eliminar(5L);

        verify(valorCaracteristicaRepository).deleteAll(c.getValores());
        verify(caracteristicaTecnicaRepository).delete(c);
    }

    @Test
    void eliminar_enUsoPorPerfiles_lanzaCaracteristicaEnUsoException() {
        CaracteristicaTecnica c = CaracteristicaTecnica.builder().idCaracteristica(5L).build();

        when(caracteristicaTecnicaRepository.findById(5L)).thenReturn(Optional.of(c));
        when(caracteristicaPerfilRepository.existsByCaracteristicaTecnicaIdCaracteristica(5L)).thenReturn(true);

        assertThrows(CaracteristicaEnUsoException.class, () -> service.eliminar(5L));
        verify(caracteristicaTecnicaRepository, never()).delete(any());
    }

    // --- AGREGAR VALOR ---

    @Test
    void agregarValor_caracteristicaEnumeradaValida_persisteYRetorna() {
        CaracteristicaTecnica c = CaracteristicaTecnica.builder()
                .idCaracteristica(10L)
                .tipoDato("ENUMERADO")
                .build();
        AdminValorCaracteristicaRequest req = new AdminValorCaracteristicaRequest(null, "VERDE", "#00FF00");

        when(caracteristicaTecnicaRepository.findById(10L)).thenReturn(Optional.of(c));
        when(valorCaracteristicaRepository.existsByCaracteristicaTecnicaIdCaracteristicaAndCodigo(10L, "VERDE"))
                .thenReturn(false);
        when(valorCaracteristicaRepository.save(any())).thenAnswer(inv -> {
            ValorCaracteristica v = inv.getArgument(0);
            v.setIdValor(100L);
            return v;
        });

        ValorCaracteristicaResponse response = service.agregarValor(10L, req);

        assertEquals("VERDE", response.codigo());
        assertEquals("#00FF00", response.colorHex());
    }

    @Test
    void agregarValor_noEnumerado_lanzaTipoDatoInvalidoException() {
        CaracteristicaTecnica c = CaracteristicaTecnica.builder()
                .idCaracteristica(10L)
                .tipoDato("TEXTO")
                .build();
        AdminValorCaracteristicaRequest req = new AdminValorCaracteristicaRequest(null, "VERDE", null);

        when(caracteristicaTecnicaRepository.findById(10L)).thenReturn(Optional.of(c));

        assertThrows(TipoDatoInvalidoException.class, () -> service.agregarValor(10L, req));
        verify(valorCaracteristicaRepository, never()).save(any());
    }

    @Test
    void agregarValor_codigoDuplicado_lanzaValorCodigoDuplicadoException() {
        CaracteristicaTecnica c = CaracteristicaTecnica.builder()
                .idCaracteristica(10L)
                .tipoDato("ENUMERADO")
                .build();
        AdminValorCaracteristicaRequest req = new AdminValorCaracteristicaRequest(null, "VERDE", null);

        when(caracteristicaTecnicaRepository.findById(10L)).thenReturn(Optional.of(c));
        when(valorCaracteristicaRepository.existsByCaracteristicaTecnicaIdCaracteristicaAndCodigo(10L, "VERDE"))
                .thenReturn(true);

        assertThrows(ValorCodigoDuplicadoException.class, () -> service.agregarValor(10L, req));
        verify(valorCaracteristicaRepository, never()).save(any());
    }

    // --- ACTUALIZAR VALOR ---

    @Test
    void actualizarValor_valido_modificaYGuarda() {
        CaracteristicaTecnica c = CaracteristicaTecnica.builder().idCaracteristica(10L).build();
        ValorCaracteristica existente = ValorCaracteristica.builder()
                .idValor(50L)
                .caracteristicaTecnica(c)
                .codigo("VERDE")
                .colorHex("#00FF00")
                .build();

        AdminValorCaracteristicaRequest req = new AdminValorCaracteristicaRequest(null, "VERDE_CLARO", "#80FF80");

        when(valorCaracteristicaRepository.findById(50L)).thenReturn(Optional.of(existente));
        when(valorCaracteristicaRepository.existsByCaracteristicaTecnicaIdCaracteristicaAndCodigo(10L, "VERDE_CLARO"))
                .thenReturn(false);
        when(valorCaracteristicaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ValorCaracteristicaResponse res = service.actualizarValor(50L, req);

        assertEquals("VERDE_CLARO", res.codigo());
        assertEquals("#80FF80", res.colorHex());
    }

    @Test
    void actualizarValor_noEncontrado_lanzaValorCaracteristicaAdminNoEncontradoException() {
        AdminValorCaracteristicaRequest req = new AdminValorCaracteristicaRequest(null, "VERDE", null);

        when(valorCaracteristicaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ValorCaracteristicaAdminNoEncontradoException.class,
                () -> service.actualizarValor(99L, req));
    }

    // --- ELIMINAR VALOR ---

    @Test
    void eliminarValor_validoSinUso_eliminaEntidad() {
        ValorCaracteristica v = ValorCaracteristica.builder().idValor(50L).build();

        when(valorCaracteristicaRepository.findById(50L)).thenReturn(Optional.of(v));
        when(caracteristicaPerfilRepository.existsByValorCaracteristicaIdValor(50L)).thenReturn(false);

        service.eliminarValor(50L);

        verify(valorCaracteristicaRepository).delete(v);
    }

    @Test
    void eliminarValor_enUsoPorPerfiles_lanzaValorEnUsoException() {
        ValorCaracteristica v = ValorCaracteristica.builder().idValor(50L).build();

        when(valorCaracteristicaRepository.findById(50L)).thenReturn(Optional.of(v));
        when(caracteristicaPerfilRepository.existsByValorCaracteristicaIdValor(50L)).thenReturn(true);

        assertThrows(ValorEnUsoException.class, () -> service.eliminarValor(50L));
        verify(valorCaracteristicaRepository, never()).delete(any());
    }
}
