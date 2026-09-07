package org.mgroko.backend.ubicacion.servicio;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.repositorio.UbicacionRepository;
import org.mgroko.backend.ubicacion.exception.LocalidadNoEncontradaException;
import org.mgroko.backend.ubicacion.exception.ProvinciaSinLocalidadException;
import org.mgroko.backend.ubicacion.georef.CentroideGeoref;
import org.mgroko.backend.ubicacion.georef.LocalidadGeoref;
import org.mgroko.backend.ubicacion.georef.ProvinciaGeorefRef;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UbicacionServiceTest {

    @Mock
    private UbicacionRepository ubicacionRepository;

    @Mock
    private GeorefCatalogoService catalogoGeoref;

    @InjectMocks
    private UbicacionService ubicacionService;

    private static final LocalidadGeoref LOCALIDAD_SAAVEDRA = new LocalidadGeoref(
            "0208401002",
            "Saavedra",
            new CentroideGeoref(-34.5548978526608, -58.4863271154338),
            new ProvinciaGeorefRef("02", "Ciudad Autónoma de Buenos Aires"));

    @Test
    void obtenerOCrear_localidadNueva_creaUbicacionConIdGeoref() {
        when(catalogoGeoref.obtenerLocalidad("0208401002")).thenReturn(LOCALIDAD_SAAVEDRA);
        when(ubicacionRepository.findByLocalidadAndProvincia("Saavedra", "Ciudad Autónoma de Buenos Aires"))
                .thenReturn(Optional.empty());
        when(ubicacionRepository.save(any(Ubicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ubicacion result = ubicacionService.obtenerOCrear("0208401002");

        ArgumentCaptor<Ubicacion> captor = ArgumentCaptor.forClass(Ubicacion.class);
        verify(ubicacionRepository).save(captor.capture());
        assertEquals("0208401002", captor.getValue().getIdGeoref());
        assertEquals("Saavedra", captor.getValue().getLocalidad());
        assertEquals("Ciudad Autónoma de Buenos Aires", captor.getValue().getProvincia());
        assertEquals("Argentina", captor.getValue().getPais());
        assertEquals(result, captor.getValue());
    }

    @Test
    void obtenerOCrear_localidadExistente_reutilizaSinGuardar() {
        Ubicacion existente = Ubicacion.builder()
                .idUbicacion(3L)
                .idGeoref("0208401002")
                .localidad("Saavedra")
                .provincia("Ciudad Autónoma de Buenos Aires")
                .build();

        when(catalogoGeoref.obtenerLocalidad("0208401002")).thenReturn(LOCALIDAD_SAAVEDRA);
        when(ubicacionRepository.findByLocalidadAndProvincia("Saavedra", "Ciudad Autónoma de Buenos Aires"))
                .thenReturn(Optional.of(existente));

        Ubicacion result = ubicacionService.obtenerOCrear("0208401002");

        assertEquals(3L, result.getIdUbicacion());
        verify(ubicacionRepository, never()).save(any());
    }

    @Test
    void obtenerOCrear_localidadInexistente_lanzaExcepcion() {
        when(catalogoGeoref.obtenerLocalidad("9999999999"))
                .thenThrow(new LocalidadNoEncontradaException("Localidad no encontrada."));

        assertThrows(LocalidadNoEncontradaException.class,
                () -> ubicacionService.obtenerOCrear("9999999999"));
    }

    @Test
    void obtenerOCrear_provinciaSinLocalidad_lanzaExcepcion() {
        assertThrows(ProvinciaSinLocalidadException.class,
                () -> ubicacionService.obtenerOCrear(null, "02"));
    }

    @Test
    void obtenerOCrear_provinciaSinLocalidadEnBlanco_lanzaExcepcion() {
        assertThrows(ProvinciaSinLocalidadException.class,
                () -> ubicacionService.obtenerOCrear("   ", "02"));
    }

    @Test
    void obtenerOCrear_provinciaConLocalidad_creaUbicacion() {
        when(catalogoGeoref.obtenerLocalidad("0208401002")).thenReturn(LOCALIDAD_SAAVEDRA);
        when(ubicacionRepository.findByLocalidadAndProvincia("Saavedra", "Ciudad Autónoma de Buenos Aires"))
                .thenReturn(Optional.empty());
        when(ubicacionRepository.save(any(Ubicacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ubicacion result = ubicacionService.obtenerOCrear("0208401002", "02");

        assertEquals("0208401002", result.getIdGeoref());
        verify(ubicacionRepository).save(any(Ubicacion.class));
    }
}