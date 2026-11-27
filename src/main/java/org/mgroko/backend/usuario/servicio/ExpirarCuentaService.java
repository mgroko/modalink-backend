package org.mgroko.backend.usuario.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Expira las cuentas cuyo plazo de baja (UC-07) ha vencido.
 *
 * <p>La logica de baja definitiva vive en {@link BajaDefinitivaService}; este
 * servicio conserva el punto de entrada usado por el scheduler y el panel de
 * administracion.</p>
 */
@Service
public class ExpirarCuentaService {

    private final BajaDefinitivaService bajaDefinitivaService;

    public ExpirarCuentaService(BajaDefinitivaService bajaDefinitivaService) {
        this.bajaDefinitivaService = bajaDefinitivaService;
    }

    @Transactional
    public int expirarVencidos(int diasBaja) {
        return expirarVencidosConResumen(diasBaja).cuentas();
    }

    @Transactional
    public ResumenBajaDefinitiva expirarVencidosConResumen(int diasBaja) {
        return bajaDefinitivaService.expirarCuentasVencidas(diasBaja);
    }
}
