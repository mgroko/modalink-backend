package org.mgroko.backend.polaroids.servicio;

import java.util.List;

import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.polaroids.dto.PolaroidResponse;
import org.mgroko.backend.polaroids.dto.PolaroidsPerfilResponse;
import org.mgroko.backend.polaroids.mapper.PolaroidMapper;
import org.mgroko.backend.repositorio.PolaroidPerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarPolaroidsPerfilService {

    private final PolaroidDatosService polaroidDatosService;
    private final PolaroidCatalogoService polaroidCatalogoService;
    private final PolaroidPerfilRepository polaroidPerfilRepository;

    public ListarPolaroidsPerfilService(
            PolaroidDatosService polaroidDatosService,
            PolaroidCatalogoService polaroidCatalogoService,
            PolaroidPerfilRepository polaroidPerfilRepository) {
        this.polaroidDatosService = polaroidDatosService;
        this.polaroidCatalogoService = polaroidCatalogoService;
        this.polaroidPerfilRepository = polaroidPerfilRepository;
    }

    @Transactional(readOnly = true)
    public PolaroidsPerfilResponse listar(Long idPerfil, Long idUsuarioAutenticado) {
        polaroidDatosService.validarPerfilVisible(idPerfil, idUsuarioAutenticado);

        List<PolaroidPerfil> polaroids = polaroidPerfilRepository
                .findByPerfilIdPerfilOrderByOrdenAsc(idPerfil);

        List<PolaroidResponse> response = polaroids.stream()
                .map(polaroid -> PolaroidMapper.toResponse(polaroid, polaroidCatalogoService))
                .toList();

        return new PolaroidsPerfilResponse(
                idPerfil,
                polaroidDatosService.maximoPorPerfil(),
                response.size(),
                response);
    }
}
