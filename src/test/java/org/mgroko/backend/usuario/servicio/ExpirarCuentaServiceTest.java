package org.mgroko.backend.usuario.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpirarCuentaServiceTest {

    @Mock
    private BajaDefinitivaService bajaDefinitivaService;

    @InjectMocks
    private ExpirarCuentaService expirarCuentaService;

    @Test
    void expirarVencidos_delegaEnBajaDefinitivaYDevuelveCuentas() {
        ResumenBajaDefinitiva resumen = new ResumenBajaDefinitiva(
                2, 3, 1, 4, 5, 6);
        when(bajaDefinitivaService.expirarCuentasVencidas(30))
                .thenReturn(resumen);

        int cantidad = expirarCuentaService.expirarVencidos(30);

        assertEquals(2, cantidad);
        verify(bajaDefinitivaService).expirarCuentasVencidas(30);
    }

    @Test
    void expirarVencidosConResumen_devuelveElResumenCompleto() {
        ResumenBajaDefinitiva resumen = new ResumenBajaDefinitiva(
                1, 2, 3, 4, 5, 6);
        when(bajaDefinitivaService.expirarCuentasVencidas(45))
                .thenReturn(resumen);

        ResumenBajaDefinitiva resultado =
                expirarCuentaService.expirarVencidosConResumen(45);

        assertEquals(resumen, resultado);
    }
}
