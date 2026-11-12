package org.mgroko.backend.polaroids.servicio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.repositorio.PolaroidPerfilRepository;
import org.springframework.stereotype.Service;

/**
 * Aplica un orden consecutivo 1..N sin chocar con el índice único
 * {@code (orden, id_perfil)}. Primero desplaza todas las filas a un rango
 * temporal y luego asigna el orden final.
 */
@Service
public class PolaroidOrdenService {

    private final PolaroidPerfilRepository polaroidPerfilRepository;

    public PolaroidOrdenService(PolaroidPerfilRepository polaroidPerfilRepository) {
        this.polaroidPerfilRepository = polaroidPerfilRepository;
    }

    public void reordenar(List<PolaroidPerfil> polaroids, List<Long> idsEnOrden) {
        if (polaroids.isEmpty()) {
            return;
        }

        Map<Long, PolaroidPerfil> porId = new HashMap<>();
        int maximo = 0;
        for (PolaroidPerfil polaroid : polaroids) {
            porId.put(polaroid.getIdPolaroid(), polaroid);
            maximo = Math.max(maximo, polaroid.getOrden() != null ? polaroid.getOrden() : 0);
        }

        int desplazamiento = maximo + 1;
        for (PolaroidPerfil polaroid : polaroids) {
            polaroid.setOrden(polaroid.getOrden() + desplazamiento);
        }
        polaroidPerfilRepository.saveAllAndFlush(polaroids);

        for (int i = 0; i < idsEnOrden.size(); i++) {
            PolaroidPerfil polaroid = porId.get(idsEnOrden.get(i));
            if (polaroid != null) {
                polaroid.setOrden(i + 1);
            }
        }
        polaroidPerfilRepository.saveAllAndFlush(polaroids);
    }
}
