package org.mgroko.backend.polaroids.servicio;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.polaroids.dto.PolaroidResponse;
import org.mgroko.backend.polaroids.dto.PolaroidsPerfilResponse;
import org.mgroko.backend.polaroids.dto.ReordenarPolaroidsRequest;
import org.mgroko.backend.polaroids.exception.ReordenamientoPolaroidsInvalidoException;
import org.mgroko.backend.polaroids.mapper.PolaroidMapper;
import org.mgroko.backend.repositorio.PolaroidPerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReordenarPolaroidsPerfilService {

    private final PolaroidDatosService polaroidDatosService;
    private final PolaroidCatalogoService polaroidCatalogoService;
    private final PolaroidPerfilRepository polaroidPerfilRepository;
    private final PolaroidOrdenService polaroidOrdenService;

    public ReordenarPolaroidsPerfilService(
            PolaroidDatosService polaroidDatosService,
            PolaroidCatalogoService polaroidCatalogoService,
            PolaroidPerfilRepository polaroidPerfilRepository,
            PolaroidOrdenService polaroidOrdenService) {
        this.polaroidDatosService = polaroidDatosService;
        this.polaroidCatalogoService = polaroidCatalogoService;
        this.polaroidPerfilRepository = polaroidPerfilRepository;
        this.polaroidOrdenService = polaroidOrdenService;
    }

    @Transactional
    public PolaroidsPerfilResponse reordenar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPerfil,
            ReordenarPolaroidsRequest request) {

        polaroidDatosService.bloquearPerfilPropioModelo(idUsuario, idPerfil, idPerfilActivo);

        List<Long> ids = request.idsPolaroid();
        if (ids == null || ids.isEmpty()) {
            throw new ReordenamientoPolaroidsInvalidoException(
                    "Debés indicar al menos una polaroid para reordenar.");
        }

        List<PolaroidPerfil> polaroids = polaroidPerfilRepository
                .findByPerfilIdPerfilOrderByOrdenAsc(idPerfil);

        Set<Long> idsRecibidos = new HashSet<>(ids);
        if (idsRecibidos.size() != ids.size()) {
            throw new ReordenamientoPolaroidsInvalidoException(
                    "El reordenamiento no puede contener polaroids repetidas.");
        }

        Set<Long> idsActuales = polaroids.stream()
                .map(PolaroidPerfil::getIdPolaroid)
                .collect(Collectors.toSet());

        if (!idsActuales.equals(idsRecibidos)) {
            throw new ReordenamientoPolaroidsInvalidoException(
                    "El reordenamiento debe incluir exactamente todas las polaroids del perfil.");
        }

        polaroidOrdenService.reordenar(polaroids, ids);

        List<PolaroidResponse> response = polaroids.stream()
                .sorted((a, b) -> a.getOrden().compareTo(b.getOrden()))
                .map(polaroid -> PolaroidMapper.toResponse(polaroid, polaroidCatalogoService))
                .toList();

        return new PolaroidsPerfilResponse(
                idPerfil,
                polaroidDatosService.maximoPorPerfil(),
                response.size(),
                response);
    }
}
