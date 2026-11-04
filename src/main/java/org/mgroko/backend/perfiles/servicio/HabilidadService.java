package org.mgroko.backend.perfiles.servicio;

import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.perfiles.mapper.HabilidadMapper;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-48 — Buscar habilidades.
 */
@Service
public class HabilidadService {

    private final HabilidadRepository habilidadRepository;

    public HabilidadService(HabilidadRepository habilidadRepository) {
        this.habilidadRepository = habilidadRepository;
    }

    /**
     * Busca habilidades por nombre (contiene, sin distinguir mayúsculas).
     * Sin nombre devuelve todas, ordenadas alfabéticamente.
     *
     * @param nombre fragmento de nombre a buscar; {@code null} o vacío no filtra
     * @return habilidades que coinciden con el criterio
     */
    @Transactional(readOnly = true)
    public List<HabilidadResponse> buscar(String nombre) {
        String valor = (nombre == null) ? "" : nombre.trim();
        return habilidadRepository.buscar(LikePatrones.contiene(valor)).stream()
                .map(HabilidadMapper::toResponse)
                .toList();
    }
}
