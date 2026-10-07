package org.mgroko.backend.ubicacion.servicio;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.repositorio.CiudadRepository;
import org.mgroko.backend.repositorio.ProvinciaRepository;
import org.mgroko.backend.repositorio.UbicacionRepository;
import org.mgroko.backend.ubicacion.catalogo.FuenteCatalogo;
import org.mgroko.backend.ubicacion.catalogo.LocalidadCatalogo;
import org.mgroko.backend.ubicacion.exception.LocalidadNoEncontradaException;
import org.mgroko.backend.ubicacion.exception.LocalidadSinProvinciaException;
import org.mgroko.backend.ubicacion.exception.PaisNoConfiguradoException;
import org.mgroko.backend.ubicacion.exception.ProvinciaSinLocalidadException;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests del alta de ubicaciones. Las aserciones recorren el grafo de entidades
 * (Pais -> Provincia -> Ciudad -> Ubicacion) en lugar de los getters planos que
 * la arquitectura anterior exponia sobre la entidad.
 */
@ExtendWith(MockitoExtension.class)
class UbicacionServiceTest {

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private CiudadRepository ciudadRepository;

    @Mock
    private ProvinciaRepository provinciaRepository;

    @Mock
    private CatalogoGeograficoService catalogoGeografico;

    @Mock
    private PaisCatalogoService paisCatalogoService;

    @InjectMocks
    private UbicacionService ubicacionService;

    private static final String FUENTE = "GEOREF";
    private static final String CLAVE_LOCALIDAD = "0208401002";
    private static final String ID_PROVINCIA = "02";
    private static final String NOMBRE_PROVINCIA = "Ciudad Autónoma de Buenos Aires";

    private static final Pais ARGENTINA = Pais.builder()
            .idPais(1L)
            .codigoIso("AR")
            .nombre("Argentina")
            .activo(true)
            .build();

    private static final LocalidadCatalogo LOCALIDAD_SAAVEDRA = new LocalidadCatalogo(
            CLAVE_LOCALIDAD,
            "Saavedra",
            ID_PROVINCIA,
            NOMBRE_PROVINCIA,
            FuenteCatalogo.GEOREF,
            new BigDecimal("-34.5548978526608"),
            new BigDecimal("-58.4863271154338"));

    @Test
    void obtenerOCrear_localidadNueva_creaUbicacionConIdExternoDeCiudad() {
        prepararAltaCompleta();

        Ubicacion result = ubicacionService.obtenerOCrear(CLAVE_LOCALIDAD);

        ArgumentCaptor<Ubicacion> capUbicacion = ArgumentCaptor.forClass(Ubicacion.class);
        ArgumentCaptor<Provincia> capProvincia = ArgumentCaptor.forClass(Provincia.class);
        verify(ubicacionRepository).save(capUbicacion.capture());
        verify(provinciaRepository).save(capProvincia.capture());

        Ubicacion guardada = capUbicacion.getValue();
        assertEquals(CLAVE_LOCALIDAD, guardada.getCiudad().getIdExterno());
        assertEquals("Saavedra", guardada.getCiudad().getNombre());
        assertEquals(NOMBRE_PROVINCIA, guardada.getCiudad().getProvincia().getNombre());
        assertEquals("Argentina", guardada.getCiudad().getProvincia().getPais().getNombre());
        assertEquals(result, guardada);
    }

    @Test
    void obtenerOCrear_localidadNueva_laProvinciaQuedaAsociadaAlPaisResueltoPorConfiguracion() {
        prepararAltaCompleta();

        ubicacionService.obtenerOCrear(CLAVE_LOCALIDAD);

        ArgumentCaptor<Provincia> capProvincia = ArgumentCaptor.forClass(Provincia.class);
        verify(provinciaRepository).save(capProvincia.capture());

        // El pais no lo inventa el servicio: lo recibe de PaisCatalogoService.
        assertSame(ARGENTINA, capProvincia.getValue().getPais());
        assertSame(FUENTE, capProvincia.getValue().getFuenteApi());
        assertEquals(ID_PROVINCIA, capProvincia.getValue().getIdExterno());
        verify(paisCatalogoService).resolver(FuenteCatalogo.GEOREF);
    }

    @Test
    void obtenerOCrear_paisNoConfigurable_fallaSinDejarProvinciaHuerfana() {
        when(catalogoGeografico.obtenerLocalidad(CLAVE_LOCALIDAD)).thenReturn(LOCALIDAD_SAAVEDRA);
        when(ciudadRepository.findByFuenteApiAndIdExterno(FUENTE, CLAVE_LOCALIDAD))
                .thenReturn(Optional.empty());
        when(provinciaRepository.findByFuenteApiAndIdExterno(FUENTE, ID_PROVINCIA))
                .thenReturn(Optional.empty());
        when(paisCatalogoService.resolver(FuenteCatalogo.GEOREF))
                .thenThrow(new PaisNoConfiguradoException(
                        "No se pudo determinar el pais de la fuente GEOREF: falta 'CATALOGO_GEOREF_PAIS_ISO'."));

        assertThrows(PaisNoConfiguradoException.class,
                () -> ubicacionService.obtenerOCrear(CLAVE_LOCALIDAD));

        verify(provinciaRepository, never()).save(any());
        verify(ciudadRepository, never()).save(any());
        verify(ubicacionRepository, never()).save(any());
    }

    @Test
    void obtenerOCrear_siLaCiudadYaExiste_noConsultaElPais() {
        Ciudad ciudadExistente = ciudadExistente(7L);
        when(catalogoGeografico.obtenerLocalidad(CLAVE_LOCALIDAD)).thenReturn(LOCALIDAD_SAAVEDRA);
        when(ciudadRepository.findByFuenteApiAndIdExterno(FUENTE, CLAVE_LOCALIDAD))
                .thenReturn(Optional.of(ciudadExistente));
        when(ubicacionRepository.findByCiudad_IdCiudad(7L)).thenReturn(Optional.empty());
        when(ubicacionRepository.save(any(Ubicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ubicacionService.obtenerOCrear(CLAVE_LOCALIDAD);

        // El pais solo se resuelve al dar de alta una provincia, nunca al buscar.
        verifyNoInteractions(paisCatalogoService);
        verify(provinciaRepository, never()).save(any());
        verify(ciudadRepository, never()).save(any());
    }

    @Test
    void obtenerOCrear_siLaCiudadYaTieneUbicacion_laReutiliza() {
        Ciudad ciudadExistente = ciudadExistente(7L);
        Ubicacion ubicacionExistente = Ubicacion.builder()
                .idUbicacion(55L)
                .ciudad(ciudadExistente)
                .build();
        when(catalogoGeografico.obtenerLocalidad(CLAVE_LOCALIDAD)).thenReturn(LOCALIDAD_SAAVEDRA);
        when(ciudadRepository.findByFuenteApiAndIdExterno(FUENTE, CLAVE_LOCALIDAD))
                .thenReturn(Optional.of(ciudadExistente));
        when(ubicacionRepository.findByCiudad_IdCiudad(7L)).thenReturn(Optional.of(ubicacionExistente));

        Ubicacion result = ubicacionService.obtenerOCrear(CLAVE_LOCALIDAD);

        assertEquals(55L, result.getIdUbicacion());
        verify(ubicacionRepository, never()).save(any());
    }

    @Test
    void obtenerOCrear_localidadSinProvincia_lanzaExcepcion() {
        LocalidadCatalogo sinProvincia = new LocalidadCatalogo(
                "9999999999", "Localidad Sin Provincia", null, null,
                FuenteCatalogo.GEOREF,
                new BigDecimal("-34.0"), new BigDecimal("-58.0"));
        when(catalogoGeografico.obtenerLocalidad("9999999999")).thenReturn(sinProvincia);

        assertThrows(LocalidadSinProvinciaException.class,
                () -> ubicacionService.obtenerOCrear("9999999999"));
    }

    /**
     * El reuso se decide por la clave natural de la ciudad, nunca por su nombre.
     * Dos ciudades de paises o catalogos distintos pueden llamarse igual; la
     * clave natural ({@code id_externo} + {@code fuente_api}) las distingue.
     */
    @Test
    void obtenerOCrear_noConsultaPorNombre_paraDecidirElReuso() {
        when(catalogoGeografico.obtenerLocalidad(CLAVE_LOCALIDAD)).thenReturn(LOCALIDAD_SAAVEDRA);
        when(ciudadRepository.findByFuenteApiAndIdExterno(FUENTE, CLAVE_LOCALIDAD))
                .thenReturn(Optional.of(ciudadExistente(7L)));
        when(ubicacionRepository.findByCiudad_IdCiudad(7L)).thenReturn(Optional.empty());
        when(ubicacionRepository.save(any(Ubicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ubicacionService.obtenerOCrear(CLAVE_LOCALIDAD);

        verify(ciudadRepository).findByFuenteApiAndIdExterno(FUENTE, CLAVE_LOCALIDAD);
        verify(ubicacionRepository).findByCiudad_IdCiudad(7L);
    }

    @Test
    void obtenerOCrear_localidadInexistente_lanzaExcepcion() {
        when(catalogoGeografico.obtenerLocalidad("9999999999"))
                .thenThrow(new LocalidadNoEncontradaException("Localidad no encontrada."));

        assertThrows(LocalidadNoEncontradaException.class,
                () -> ubicacionService.obtenerOCrear("9999999999"));
    }

    @Test
    void obtenerOCrear_provinciaSinLocalidad_lanzaExcepcion() {
        assertThrows(ProvinciaSinLocalidadException.class,
                () -> ubicacionService.obtenerOCrear(null, ID_PROVINCIA));
    }

    @Test
    void obtenerOCrear_provinciaSinLocalidadEnBlanco_lanzaExcepcion() {
        assertThrows(ProvinciaSinLocalidadException.class,
                () -> ubicacionService.obtenerOCrear("   ", ID_PROVINCIA));
    }

    @Test
    void obtenerOCrear_provinciaConLocalidad_creaUbicacion() {
        prepararAltaCompleta();

        Ubicacion result = ubicacionService.obtenerOCrear(CLAVE_LOCALIDAD, ID_PROVINCIA);

        assertEquals(CLAVE_LOCALIDAD, result.getCiudad().getIdExterno());
        verify(ubicacionRepository).save(any(Ubicacion.class));
    }

    /**
     * Configura el escenario de alta completa: ni la ciudad ni la provincia
     * existen todavia, y el pais se resuelve desde la configuracion.
     */
    private void prepararAltaCompleta() {
        when(catalogoGeografico.obtenerLocalidad(CLAVE_LOCALIDAD)).thenReturn(LOCALIDAD_SAAVEDRA);
        when(ciudadRepository.findByFuenteApiAndIdExterno(FUENTE, CLAVE_LOCALIDAD))
                .thenReturn(Optional.empty());
        when(provinciaRepository.findByFuenteApiAndIdExterno(FUENTE, ID_PROVINCIA))
                .thenReturn(Optional.empty());
        when(paisCatalogoService.resolver(FuenteCatalogo.GEOREF)).thenReturn(ARGENTINA);
        when(provinciaRepository.save(any(Provincia.class)))
                .thenAnswer(invocation -> {
                    Provincia provincia = invocation.getArgument(0);
                    provincia.setIdProvincia(10L);
                    return provincia;
                });
        when(ciudadRepository.save(any(Ciudad.class)))
                .thenAnswer(invocation -> {
                    Ciudad ciudad = invocation.getArgument(0);
                    ciudad.setIdCiudad(20L);
                    return ciudad;
                });
        when(ubicacionRepository.findByCiudad_IdCiudad(20L)).thenReturn(Optional.empty());
        when(ubicacionRepository.save(any(Ubicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Ciudad ciudadExistente(Long idCiudad) {
        return Ciudad.builder()
                .idCiudad(idCiudad)
                .nombre("Saavedra")
                .idExterno(CLAVE_LOCALIDAD)
                .fuenteApi(FUENTE)
                .provincia(Provincia.builder()
                        .idProvincia(10L)
                        .nombre(NOMBRE_PROVINCIA)
                        .idExterno(ID_PROVINCIA)
                        .fuenteApi(FUENTE)
                        .pais(ARGENTINA)
                        .build())
                .build();
    }
}
