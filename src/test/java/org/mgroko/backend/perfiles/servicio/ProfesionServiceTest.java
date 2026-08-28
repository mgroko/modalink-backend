package org.mgroko.backend.perfiles.servicio;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.perfiles.dto.ProfesionResponse;
import org.mgroko.backend.repositorio.ProfesionRepository;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfesionServiceTest {

    @Mock
    private ProfesionRepository profesionRepository;

    @InjectMocks
    private ProfesionService profesionService;

    private Profesion profesionModelo() {
        return Profesion.builder()
                .idProfesion(2L)
                .nombre("modelo")
                .descripcion("Profesional que posa para producciones.")
                .build();
    }

    @Test
    void buscar_conNombre_devuelveCoincidenciasMapeadas() {
        when(profesionRepository.buscar("%mo%")).thenReturn(List.of(profesionModelo()));

        List<ProfesionResponse> response = profesionService.buscar("mo");

        assertEquals(1, response.size());
        assertEquals("modelo", response.get(0).nombre());
        assertEquals(2L, response.get(0).idProfesion());
        verify(profesionRepository).buscar("%mo%");
    }

    @Test
    void buscar_mayusculas_normalizaPatronMinusculas() {
        when(profesionRepository.buscar("%modelo%")).thenReturn(List.of(profesionModelo()));

        List<ProfesionResponse> response = profesionService.buscar("MODELO");

        assertEquals(1, response.size());
        verify(profesionRepository).buscar("%modelo%");
    }

    @Test
    void buscar_nombreEnBlanco_consultaTodo() {
        when(profesionRepository.buscar("%")).thenReturn(List.of());

        assertTrue(profesionService.buscar("   ").isEmpty());

        verify(profesionRepository).buscar("%");
    }

    @Test
    void buscar_sinCoincidencias_devuelveListaVacia() {
        when(profesionRepository.buscar(anyString())).thenReturn(List.of());

        assertTrue(profesionService.buscar("profesion_inexistente").isEmpty());
    }
}