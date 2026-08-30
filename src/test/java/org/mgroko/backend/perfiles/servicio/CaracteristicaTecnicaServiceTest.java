package org.mgroko.backend.perfiles.servicio;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaracteristicaTecnicaServiceTest {

    @Mock
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Mock
    private ProfesionRepository profesionRepository;

    @InjectMocks
    private CaracteristicaTecnicaService caracteristicaTecnicaService;

    private Profesion profesionModelo() {
        return Profesion.builder().idProfesion(2L).nombre("modelo").build();
    }

    private CaracteristicaTecnica caracteristicaAltura() {
        return CaracteristicaTecnica.builder()
                .idCaracteristica(11L)
                .codigo("altura")
                .unidad("cm")
                .profesion(profesionModelo())
                .build();
    }

    @Test
    void buscar_conFiltros_devuelveCoincidenciasMapeadas() {
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(caracteristicaTecnicaRepository.buscar("%alt%", "%", 2L))
                .thenReturn(List.of(caracteristicaAltura()));

        List<CaracteristicaTecnicaResponse> response =
                caracteristicaTecnicaService.buscar(2L, "alt", null);

        assertEquals(1, response.size());
        assertEquals("altura", response.get(0).codigo());
        assertEquals("cm", response.get(0).unidad());
        assertEquals(2L, response.get(0).idProfesion());
        assertEquals("modelo", response.get(0).profesion());
        verify(caracteristicaTecnicaRepository).buscar("%alt%", "%", 2L);
    }

    @Test
    void buscar_mayusculas_normalizaPatronMinusculas() {
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(caracteristicaTecnicaRepository.buscar("%pecho%", "%", 2L))
                .thenReturn(List.of());

        assertTrue(caracteristicaTecnicaService.buscar(2L, "PECHO", "  ").isEmpty());

        verify(caracteristicaTecnicaRepository).buscar("%pecho%", "%", 2L);
    }

    @Test
    void buscar_sinFiltros_consultaTodo() {
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(caracteristicaTecnicaRepository.buscar(eq("%"), eq("%"), eq(2L)))
                .thenReturn(List.of());

        assertTrue(caracteristicaTecnicaService.buscar(2L, "  ", " ").isEmpty());

        verify(caracteristicaTecnicaRepository).buscar("%", "%", 2L);
    }

    @Test
    void buscar_idProfesionInexistente_lanzaExcepcion() {
        when(profesionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProfesionNoEncontradaException.class,
                () -> caracteristicaTecnicaService.buscar(999L, null, null));

        verify(caracteristicaTecnicaRepository, never()).buscar(any(), any(), any());
    }
}