package org.mgroko.backend.perfiles.servicio;

import java.util.List;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.perfiles.dto.ProfesionResponse;
import org.mgroko.backend.perfiles.mapper.ProfesionMapper;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfesionService {

    private final ProfesionRepository profesionRepository;

    public ProfesionService(ProfesionRepository profesionRepository) {
        this.profesionRepository = profesionRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfesionResponse> buscar(String nombre) {
        String valor = (nombre == null) ? "" : nombre.trim();
        return profesionRepository.buscar(LikePatrones.contiene(valor), valor).stream()
                .map(ProfesionMapper::toResponse)
                .toList();
    }
}