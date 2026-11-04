package org.mgroko.backend.perfiles.servicio;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.repositorio.HabilidadRepository;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HabilidadServiceTest {

    @Mock
    private HabilidadRepository habilidadRepository;

    @InjectMocks
    private HabilidadService habilidadService;

    private Habilidad habilidad() {
        return Habilidad.builder()
                .idHabilidad(1L)
                .codigo("FOTO_ESTUDIO")
                .nombre("Fotografía de Estudio")
                .descripcion("Iluminación y sets.")
                .build();
    }

    @Test
    void buscar_sinNombre_usaPatronSinFiltro() {
        when(habilidadRepository.buscar("%")).thenReturn(List.of(habilidad()));

        List<HabilidadResponse> resultado = habilidadService.buscar(null);

        assertEquals(1, resultado.size());
        assertEquals("FOTO_ESTUDIO", resultado.get(0).codigo());
        verify(habilidadRepository).buscar("%");
    }

    @Test
    void buscar_conNombre_usaPatronEnMinusculas() {
        when(habilidadRepository.buscar("%foto%")).thenReturn(List.of(habilidad()));

        habilidadService.buscar(" Foto ");

        verify(habilidadRepository).buscar("%foto%");
    }

    @Test
    void buscar_escapaWildcardsDelUsuario() {
        when(habilidadRepository.buscar("%100\\%%")).thenReturn(List.of());

        habilidadService.buscar("100%");

        verify(habilidadRepository).buscar("%100\\%%");
    }

    @Test
    void buscar_mapeaTodosLosCampos() {
        when(habilidadRepository.buscar("%")).thenReturn(List.of(habilidad()));

        HabilidadResponse response = habilidadService.buscar(null).get(0);

        assertEquals(1L, response.idHabilidad());
        assertEquals("FOTO_ESTUDIO", response.codigo());
        assertEquals("Fotografía de Estudio", response.nombre());
        assertEquals("Iluminación y sets.", response.descripcion());
    }
}
