package org.mgroko.backend.contratos.servicio;

import java.util.List;

import org.mgroko.backend.contratos.dto.ProyectoFiltroContratoResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-68: opciones del filtro "Proyecto" de la búsqueda de contratos.
 *
 * <p>Devuelve los proyectos donde el perfil activo tiene al menos un contrato
 * visible: como director activo o como parte firmante. La segunda condición
 * incluye proyectos históricos en los que el perfil ya no tiene membresía
 * activa pero conserva un contrato firmado.</p>
 */
@Service
public class ListarProyectosContratoService {

    private final PerfilRepository perfilRepository;
    private final ContratoRepository contratoRepository;

    public ListarProyectosContratoService(
            PerfilRepository perfilRepository,
            ContratoRepository contratoRepository) {
        this.perfilRepository = perfilRepository;
        this.contratoRepository = contratoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProyectoFiltroContratoResponse> listar(
            Long idUsuario,
            Long idPerfilActivo) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        return contratoRepository.listarProyectosVisibles(idPerfilActivo);
    }
}
