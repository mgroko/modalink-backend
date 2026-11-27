package org.mgroko.backend.perfiles.servicio;

import org.mgroko.backend.usuario.servicio.BajaDefinitivaService;
import org.mgroko.backend.usuario.servicio.ResumenBajaDefinitiva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Expira los perfiles cuyo plazo de baja (UC-12) ha vencido.
 *
 * <p>La logica de baja definitiva vive en {@link BajaDefinitivaService}; este
 * servicio conserva el punto de entrada usado por el scheduler y el panel de
 * administracion.</p>
 */
@Service
public class ExpirarPerfilService {

    private final BajaDefinitivaService bajaDefinitivaService;

    public ExpirarPerfilService(BajaDefinitivaService bajaDefinitivaService) {
        this.bajaDefinitivaService = bajaDefinitivaService;
    }

    @Transactional
    public int expirarVencidos(int diasBaja) {
        return expirarVencidosConResumen(diasBaja).perfiles();
    }

    @Transactional
    public ResumenBajaDefinitiva expirarVencidosConResumen(int diasBaja) {
        return bajaDefinitivaService.expirarPerfilesVencidos(diasBaja);
    }
}
