package org.mgroko.backend.contratos.servicio;

import org.mgroko.backend.contratos.dto.ModificarContratoRequest;
import org.mgroko.backend.contratos.dto.PrevisualizacionModificacionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-53 paso 6: regenera el borrador con el nuevo tipo y las nuevas partes sin
 * persistir cambios.
 */
@Service
public class PrevisualizarModificacionContratoService {

    private final ContratoModificacionContextoService contextoService;

    public PrevisualizarModificacionContratoService(
            ContratoModificacionContextoService contextoService) {
        this.contextoService = contextoService;
    }

    @Transactional(readOnly = true)
    public PrevisualizacionModificacionResponse previsualizar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato,
            ModificarContratoRequest request) {

        ContratoModificacionContextoService.ContextoModificacion contexto =
                contextoService.cargarParaPreview(
                        idUsuario,
                        idPerfilActivo,
                        idProyecto,
                        idContrato,
                        request);

        return new PrevisualizacionModificacionResponse(
                contexto.texto(),
                contexto.conflictos().isEmpty(),
                contexto.conflictos());
    }
}
