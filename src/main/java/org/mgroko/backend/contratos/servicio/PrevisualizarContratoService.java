package org.mgroko.backend.contratos.servicio;

import org.mgroko.backend.contratos.dto.GenerarContratoRequest;
import org.mgroko.backend.contratos.dto.PrevisualizacionContratoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera el borrador del contrato sin persistirlo. Devuelve el texto y los
 * conflictos detectados para que el director pueda volver al paso 3 de UC-39.
 */
@Service
public class PrevisualizarContratoService {

    private final ContratoContextoService contextoService;
    private final GeneradorTextoContratoService generadorTextoService;

    public PrevisualizarContratoService(
            ContratoContextoService contextoService,
            GeneradorTextoContratoService generadorTextoService) {
        this.contextoService = contextoService;
        this.generadorTextoService = generadorTextoService;
    }

    @Transactional(readOnly = true)
    public PrevisualizacionContratoResponse previsualizar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            GenerarContratoRequest request) {

        ContratoContextoService.ContextoContrato contexto = contextoService.cargar(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                request.idTipoAcuerdo(),
                request.idsMiembro());

        String texto = generadorTextoService.generar(
                contexto,
                request.clausulasAdicionales());

        return new PrevisualizacionContratoResponse(
                texto,
                contexto.conflictos().isEmpty(),
                contexto.conflictos());
    }
}
