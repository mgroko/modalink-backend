package org.mgroko.backend.admin.servicio;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.dto.AdminUnidadMedidaRequest;
import org.mgroko.backend.admin.dto.UnidadMedidaResponse;
import org.mgroko.backend.admin.exception.TipoDatoInvalidoException;
import org.mgroko.backend.admin.exception.UnidadMedidaDuplicadaException;
import org.mgroko.backend.admin.exception.UnidadMedidaEnUsoException;
import org.mgroko.backend.admin.exception.UnidadMedidaNoEncontradaException;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.UnidadMedida;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.UnidadMedidaRepository;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUnidadMedidaServiceTest {

    @Mock
    private UnidadMedidaRepository unidadMedidaRepository;

    @Mock
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @InjectMocks
    private AdminUnidadMedidaService service;

    private UnidadMedida unidadCm;

    @BeforeEach
    void setUp() {
        unidadCm = UnidadMedida.builder()
                .idUnidad(1L)
                .nombre("Centímetro")
                .simbolo("cm")
                .tipoDatoPermitido("NUMERICO")
                .build();
    }

    private AdminUnidadMedidaRequest request(String nombre, String simbolo, String tipo) {
        return new AdminUnidadMedidaRequest(nombre, simbolo, tipo);
    }

    // ------------------------------------------------------------- listar

    @Test
    void listar_sinFiltro_retornaTodasOrdenadasPorNombre() {
        UnidadMedida kilogramo = UnidadMedida.builder()
                .idUnidad(2L).nombre("Kilogramo").simbolo("kg").tipoDatoPermitido("NUMERICO").build();
        UnidadMedida palabras = UnidadMedida.builder()
                .idUnidad(3L).nombre("Palabras").simbolo("pal").tipoDatoPermitido("TEXTO").build();
        when(unidadMedidaRepository.findAll()).thenReturn(List.of(kilogramo, unidadCm, palabras));

        List<UnidadMedidaResponse> respuesta = service.listar(null);

        assertEquals(3, respuesta.size());
        assertEquals("Centímetro", respuesta.get(0).nombre());
        assertEquals("Kilogramo", respuesta.get(1).nombre());
        assertEquals("Palabras", respuesta.get(2).nombre());
    }

    @Test
    void listar_conFiltroTipoDato_retornaSoloEsasUnidades() {
        UnidadMedida palabras = UnidadMedida.builder()
                .idUnidad(3L).nombre("Palabras").simbolo("pal").tipoDatoPermitido("TEXTO").build();
        when(unidadMedidaRepository.findAll()).thenReturn(List.of(unidadCm, palabras));

        List<UnidadMedidaResponse> respuesta = service.listar("numerico");

        assertEquals(1, respuesta.size());
        assertEquals("cm", respuesta.get(0).simbolo());
    }

    @Test
    void listar_conFiltroTipoDatoInvalido_lanzaTipoDatoInvalido() {
        assertThrows(TipoDatoInvalidoException.class, () -> service.listar("ENUMERADO"));
    }

    @Test
    void listar_conFiltroEnBlanco_retornaTodas() {
        when(unidadMedidaRepository.findAll()).thenReturn(List.of(unidadCm));

        assertEquals(1, service.listar("   ").size());
    }

    // ------------------------------------------------------------- obtener

    @Test
    void obtener_existente_retornaResponse() {
        when(unidadMedidaRepository.findById(1L)).thenReturn(Optional.of(unidadCm));

        UnidadMedidaResponse respuesta = service.obtener(1L);

        assertEquals(1L, respuesta.idUnidad());
        assertEquals("cm", respuesta.simbolo());
        assertEquals("NUMERICO", respuesta.tipoDatoPermitido());
    }

    @Test
    void obtener_inexistente_lanzaNoEncontrada() {
        when(unidadMedidaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UnidadMedidaNoEncontradaException.class, () -> service.obtener(99L));
    }

    // -------------------------------------------------------------- crear

    @Test
    void crear_datosValidos_guardaNuevaUnidad() {
        when(unidadMedidaRepository.existsByNombreIgnoreCase("Centímetro")).thenReturn(false);
        when(unidadMedidaRepository.existsBySimboloIgnoreCase("cm")).thenReturn(false);
        when(unidadMedidaRepository.save(any(UnidadMedida.class))).thenAnswer(invocation -> {
            UnidadMedida guardada = invocation.getArgument(0);
            guardada.setIdUnidad(9L);
            return guardada;
        });

        UnidadMedidaResponse respuesta = service.crear(request("  Centímetro ", " cm ", "numerico"));

        assertEquals(9L, respuesta.idUnidad());
        assertEquals("Centímetro", respuesta.nombre());
        assertEquals("cm", respuesta.simbolo());
        assertEquals("NUMERICO", respuesta.tipoDatoPermitido());
    }

    @Test
    void crear_nombreYaExistente_lanzaDuplicada() {
        when(unidadMedidaRepository.existsByNombreIgnoreCase("centimetro")).thenReturn(true);

        UnidadMedidaDuplicadaException excepcion = assertThrows(UnidadMedidaDuplicadaException.class,
                () -> service.crear(request("centimetro", "cm", "NUMERICO")));

        assertEquals("Ya existe una unidad de medida con el nombre centimetro.", excepcion.getMessage());
        verify(unidadMedidaRepository, never()).save(any(UnidadMedida.class));
    }

    @Test
    void crear_simboloYaExistente_lanzaDuplicada() {
        when(unidadMedidaRepository.existsByNombreIgnoreCase("Centímetro")).thenReturn(false);
        when(unidadMedidaRepository.existsBySimboloIgnoreCase("CM")).thenReturn(true);

        assertThrows(UnidadMedidaDuplicadaException.class,
                () -> service.crear(request("Centímetro", "CM", "NUMERICO")));
        verify(unidadMedidaRepository, never()).save(any(UnidadMedida.class));
    }

    @Test
    void crear_tipoDatoPermitidoInvalido_lanzaTipoDatoInvalido() {
        assertThrows(TipoDatoInvalidoException.class,
                () -> service.crear(request("Color", "color", "ENUMERADO")));
        verify(unidadMedidaRepository, never()).save(any(UnidadMedida.class));
    }

    // ---------------------------------------------------------- actualizar

    @Test
    void actualizar_datosValidosRegistraCambios() {
        when(unidadMedidaRepository.findById(1L)).thenReturn(Optional.of(unidadCm));
        when(unidadMedidaRepository.existsByNombreIgnoreCaseAndIdUnidadNot("Centímetro", 1L)).thenReturn(false);
        when(unidadMedidaRepository.existsBySimboloIgnoreCaseAndIdUnidadNot("cm", 1L)).thenReturn(false);
        when(unidadMedidaRepository.save(any(UnidadMedida.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UnidadMedidaResponse respuesta = service.actualizar(1L, request("Centímetro", "cm", "NUMERICO"));

        assertEquals("Centímetro", respuesta.nombre());
        assertEquals("NUMERICO", respuesta.tipoDatoPermitido());
        verify(caracteristicaTecnicaRepository, never()).findAllByUnidadMedidaIdUnidad(1L);
    }

    @Test
    void actualizar_inexistente_lanzaNoEncontrada() {
        when(unidadMedidaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UnidadMedidaNoEncontradaException.class,
                () -> service.actualizar(99L, request("Otra", "ot", "NUMERICO")));
    }

    @Test
    void actualizar_nombreDuplicadoEnOtraUnidad_lanzaDuplicada() {
        when(unidadMedidaRepository.findById(1L)).thenReturn(Optional.of(unidadCm));
        when(unidadMedidaRepository.existsByNombreIgnoreCaseAndIdUnidadNot("Metro", 1L)).thenReturn(true);

        assertThrows(UnidadMedidaDuplicadaException.class,
                () -> service.actualizar(1L, request("Metro", "m", "NUMERICO")));
        verify(unidadMedidaRepository, never()).save(any(UnidadMedida.class));
    }

    @Test
    void actualizar_simboloDuplicadoEnOtraUnidad_lanzaDuplicada() {
        when(unidadMedidaRepository.findById(1L)).thenReturn(Optional.of(unidadCm));
        when(unidadMedidaRepository.existsByNombreIgnoreCaseAndIdUnidadNot("Centímetro", 1L)).thenReturn(false);
        when(unidadMedidaRepository.existsBySimboloIgnoreCaseAndIdUnidadNot("m", 1L)).thenReturn(true);

        assertThrows(UnidadMedidaDuplicadaException.class,
                () -> service.actualizar(1L, request("Centímetro", "m", "NUMERICO")));
        verify(unidadMedidaRepository, never()).save(any(UnidadMedida.class));
    }

    @Test
    void actualizar_tipoIncompatibleConCaracteristicasEnUso_lanzaEnUso() {
        when(unidadMedidaRepository.findById(1L)).thenReturn(Optional.of(unidadCm));
        when(unidadMedidaRepository.existsByNombreIgnoreCaseAndIdUnidadNot("Centímetro", 1L)).thenReturn(false);
        when(unidadMedidaRepository.existsBySimboloIgnoreCaseAndIdUnidadNot("cm", 1L)).thenReturn(false);
        when(caracteristicaTecnicaRepository.findAllByUnidadMedidaIdUnidad(1L)).thenReturn(List.of(
                CaracteristicaTecnica.builder().codigo("ALTURA").tipoDato("NUMERICO").build()));

        assertThrows(UnidadMedidaEnUsoException.class,
                () -> service.actualizar(1L, request("Centímetro", "cm", "TEXTO")));
        verify(unidadMedidaRepository, never()).save(any(UnidadMedida.class));
    }

    @Test
    void actualizar_tipoCompatibleConCaracteristicasEnUso_registraCambios() {
        when(unidadMedidaRepository.findById(1L)).thenReturn(Optional.of(unidadCm));
        when(unidadMedidaRepository.existsByNombreIgnoreCaseAndIdUnidadNot("Centímetros", 1L)).thenReturn(false);
        when(unidadMedidaRepository.existsBySimboloIgnoreCaseAndIdUnidadNot("cm", 1L)).thenReturn(false);
        when(caracteristicaTecnicaRepository.findAllByUnidadMedidaIdUnidad(1L)).thenReturn(List.of(
                CaracteristicaTecnica.builder().codigo("EQUIPO").tipoDato("TEXTO").build()));
        when(unidadMedidaRepository.save(any(UnidadMedida.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UnidadMedidaResponse respuesta = service.actualizar(1L, request("Centímetros", "cm", "texto"));

        assertEquals("Centímetros", respuesta.nombre());
        assertEquals("TEXTO", respuesta.tipoDatoPermitido());
    }

    // ----------------------------------------------------------- eliminar

    @Test
    void eliminar_sinCaracteristicasAsociadas_daDeBaja() {
        when(unidadMedidaRepository.findById(1L)).thenReturn(Optional.of(unidadCm));
        when(caracteristicaTecnicaRepository.existsByUnidadMedidaIdUnidad(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(unidadMedidaRepository).delete(unidadCm);
    }

    @Test
    void eliminar_asociadaACaracteristica_lanzaEnUso() {
        when(unidadMedidaRepository.findById(1L)).thenReturn(Optional.of(unidadCm));
        when(caracteristicaTecnicaRepository.existsByUnidadMedidaIdUnidad(1L)).thenReturn(true);

        assertThrows(UnidadMedidaEnUsoException.class, () -> service.eliminar(1L));
        verify(unidadMedidaRepository, never()).delete(any(UnidadMedida.class));
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrada() {
        when(unidadMedidaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UnidadMedidaNoEncontradaException.class, () -> service.eliminar(99L));
    }
}
