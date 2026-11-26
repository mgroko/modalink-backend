package org.mgroko.backend.contratos.servicio;

import org.mgroko.backend.contratos.dto.GuardarContratoRequest;
import org.mgroko.backend.contratos.dto.PrevisualizacionContratoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera el texto del contrato sin persistirlo. La previsualizacion es
 * opcional y nunca es requisito para guardar un borrador o emitirlo.
 */
@Service
public class PrevisualizarContratoService {

    private final ContratoContextoService contextoService;
    private final ContratoTextoResolverService textoResolverService;

    public PrevisualizarContratoService(
            ContratoContextoService contextoService,
            ContratoTextoResolverService textoResolverService) {
        this.contextoService = contextoService;
        this.textoResolverService = textoResolverService;
    }

    @Transactional(readOnly = true)
    public PrevisualizacionContratoResponse previsualizar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            GuardarContratoRequest request) {

        ContratoContextoService.ContextoContrato contexto = contextoService.cargar(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                request.idTipoAcuerdo(),
                request.idsMiembro(),
                true);

        String texto = textoResolverService.resolver(
                contexto.proyecto(),
                contexto.tipoAcuerdo(),
                contexto.emisor(),
                contexto.destinatarios(),
                request);

        return new PrevisualizacionContratoResponse(
                texto,
                contexto.conflictos().isEmpty(),
                contexto.conflictos());
    }
}
