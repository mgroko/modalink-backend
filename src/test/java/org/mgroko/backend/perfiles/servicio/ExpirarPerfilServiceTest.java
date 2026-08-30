package org.mgroko.backend.perfiles.servicio;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpirarPerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @InjectMocks
    private ExpirarPerfilService expirarPerfilService;

    @Test
    void expirarVencidos_cambiaEstadoABaja() {
        Perfil perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.PendienteBaja)
                .fechaSolicitudBaja(LocalDateTime.now().minusDays(31))
                .build();
        when(perfilRepository.findByEstadoAndFechaSolicitudBajaBefore(eq(EstadoPerfil.PendienteBaja), any()))
                .thenReturn(List.of(perfil));

        int cantidad = expirarPerfilService.expirarVencidos();

        assertEquals(1, cantidad);
        ArgumentCaptor<List<Perfil>> captor = ArgumentCaptor.forClass(List.class);
        verify(perfilRepository).saveAll(captor.capture());
        assertEquals(EstadoPerfil.Baja, captor.getValue().get(0).getEstado());
    }

    @Test
    void expirarVencidos_sinVencidos_noGuarda() {
        when(perfilRepository.findByEstadoAndFechaSolicitudBajaBefore(eq(EstadoPerfil.PendienteBaja), any()))
                .thenReturn(List.of());

        int cantidad = expirarPerfilService.expirarVencidos();

        assertEquals(0, cantidad);
        verify(perfilRepository, never()).saveAll(any());
    }
}