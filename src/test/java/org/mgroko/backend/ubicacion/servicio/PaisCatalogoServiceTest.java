package org.mgroko.backend.ubicacion.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
import org.mgroko.backend.repositorio.PaisRepository;
import org.mgroko.backend.ubicacion.catalogo.FuenteCatalogo;
import org.mgroko.backend.ubicacion.exception.PaisNoConfiguradoException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaisCatalogoServiceTest {

    @Mock
    private ConfiguracionSistemaRepository configuracionSistemaRepository;

    @Mock
    private PaisRepository paisRepository;

    @InjectMocks
    private PaisCatalogoService paisCatalogoService;

    private static final String CLAVE_GEOREF = "CATALOGO_GEOREF_PAIS_ISO";

    @Test
    void resolver_configuracionValida_devuelveElPaisActivo() {
        Pais argentina = Pais.builder()
                .idPais(1L)
                .codigoIso("AR")
                .nombre("Argentina")
                .activo(true)
                .build();
        when(configuracionSistemaRepository.findByClave(CLAVE_GEOREF))
                .thenReturn(Optional.of(configuracion("AR")));
        when(paisRepository.findByCodigoIsoAndActivoTrue("AR")).thenReturn(Optional.of(argentina));

        Pais resultado = paisCatalogoService.resolver(FuenteCatalogo.GEOREF);

        assertEquals(argentina, resultado);
    }

    @Test
    void resolver_consultaPorCodigoIsoYActivoTrue() {
        when(configuracionSistemaRepository.findByClave(CLAVE_GEOREF))
                .thenReturn(Optional.of(configuracion("AR")));
        when(paisRepository.findByCodigoIsoAndActivoTrue("AR"))
                .thenReturn(Optional.of(Pais.builder().codigoIso("AR").build()));

        paisCatalogoService.resolver(FuenteCatalogo.GEOREF);

        verify(paisRepository).findByCodigoIsoAndActivoTrue("AR");
        verify(paisRepository, never()).findByCodigoIso("AR");
    }

    @Test
    void resolver_codigoIsoConEspaciosYMinusculas_loNormaliza() {
        when(configuracionSistemaRepository.findByClave(CLAVE_GEOREF))
                .thenReturn(Optional.of(configuracion("  ar  ")));
        when(paisRepository.findByCodigoIsoAndActivoTrue("AR"))
                .thenReturn(Optional.of(Pais.builder().codigoIso("AR").build()));

        Pais resultado = paisCatalogoService.resolver(FuenteCatalogo.GEOREF);

        assertEquals("AR", resultado.getCodigoIso());
    }

    @Test
    void resolver_derivaLaClaveDesdeElEnum() {
        when(configuracionSistemaRepository.findByClave("CATALOGO_GOOGLEMAPS_PAIS_ISO"))
                .thenReturn(Optional.of(configuracion("ES")));
        when(paisRepository.findByCodigoIsoAndActivoTrue("ES"))
                .thenReturn(Optional.of(Pais.builder().codigoIso("ES").build()));

        paisCatalogoService.resolver(FuenteCatalogo.GOOGLEMAPS);

        verify(configuracionSistemaRepository).findByClave("CATALOGO_GOOGLEMAPS_PAIS_ISO");
    }

    @Test
    void resolver_claveAusente_lanzaExcepcionIndicandoLaClave() {
        when(configuracionSistemaRepository.findByClave(CLAVE_GEOREF)).thenReturn(Optional.empty());

        PaisNoConfiguradoException ex = assertThrows(PaisNoConfiguradoException.class,
                () -> paisCatalogoService.resolver(FuenteCatalogo.GEOREF));

        assertTrue(ex.getMessage().contains(CLAVE_GEOREF), ex.getMessage());
        verify(paisRepository, never()).findByCodigoIsoAndActivoTrue("AR");
    }

    @Test
    void resolver_valorEnBlanco_lanzaExcepcion() {
        when(configuracionSistemaRepository.findByClave(CLAVE_GEOREF))
                .thenReturn(Optional.of(configuracion("   ")));

        assertThrows(PaisNoConfiguradoException.class,
                () -> paisCatalogoService.resolver(FuenteCatalogo.GEOREF));

        verify(paisRepository, never()).findByCodigoIsoAndActivoTrue("AR");
    }

    @Test
    void resolver_isoQueNoExisteEnPais_lanzaExcepcion() {
        when(configuracionSistemaRepository.findByClave(CLAVE_GEOREF))
                .thenReturn(Optional.of(configuracion("AR")));
        when(paisRepository.findByCodigoIsoAndActivoTrue("AR")).thenReturn(Optional.empty());

        PaisNoConfiguradoException ex = assertThrows(PaisNoConfiguradoException.class,
                () -> paisCatalogoService.resolver(FuenteCatalogo.GEOREF));

        assertTrue(ex.getMessage().contains("AR"), ex.getMessage());
    }

    @Test
    void resolver_paisInactivo_lanzaExcepcion() {
        when(configuracionSistemaRepository.findByClave(CLAVE_GEOREF))
                .thenReturn(Optional.of(configuracion("AR")));
        when(paisRepository.findByCodigoIsoAndActivoTrue("AR")).thenReturn(Optional.empty());

        assertThrows(PaisNoConfiguradoException.class,
                () -> paisCatalogoService.resolver(FuenteCatalogo.GEOREF));
    }

    private ConfiguracionSistema configuracion(String valor) {
        return ConfiguracionSistema.builder()
                .clave(CLAVE_GEOREF)
                .valor(valor)
                .build();
    }
}
