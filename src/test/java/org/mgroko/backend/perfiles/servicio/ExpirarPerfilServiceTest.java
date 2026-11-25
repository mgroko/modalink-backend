package org.mgroko.backend.perfiles.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.usuario.servicio.BajaDefinitivaService;
import org.mgroko.backend.usuario.servicio.ResumenBajaDefinitiva;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpirarPerfilServiceTest {

    @Mock
    private BajaDefinitivaService bajaDefinitivaService;

    @InjectMocks
    private ExpirarPerfilService expirarPerfilService;

    @Test
    void expirarVencidos_delegaEnBajaDefinitivaYDevuelvePerfiles() {
        ResumenBajaDefinitiva resumen = new ResumenBajaDefinitiva(
                0, 1, 2, 3, 4, 5);
        when(bajaDefinitivaService.expirarPerfilesVencidos(30))
                .thenReturn(resumen);

        int cantidad = expirarPerfilService.expirarVencidos(30);

        assertEquals(1, cantidad);
        verify(bajaDefinitivaService).expirarPerfilesVencidos(30);
    }

    @Test
    void expirarVencidosConResumen_devuelveElResumenCompleto() {
        ResumenBajaDefinitiva resumen = new ResumenBajaDefinitiva(
                0, 2, 1, 3, 4, 5);
        when(bajaDefinitivaService.expirarPerfilesVencidos(45))
                .thenReturn(resumen);

        ResumenBajaDefinitiva resultado =
                expirarPerfilService.expirarVencidosConResumen(45);

        assertEquals(resumen, resultado);
    }
}
