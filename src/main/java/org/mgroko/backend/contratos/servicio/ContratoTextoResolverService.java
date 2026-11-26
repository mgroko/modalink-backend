package org.mgroko.backend.contratos.servicio;

import java.util.List;

import org.mgroko.backend.contratos.dto.GuardarContratoRequest;
import org.mgroko.backend.contratos.dto.ModoTextoContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.springframework.stereotype.Service;

/**
 * Resuelve el texto final de un contrato segun el modo elegido.
 *
 * <p>Centraliza la regla para que crear, editar y previsualizar se comporten
 * igual. En {@link ModoTextoContrato#MANUAL} el texto enviado es obligatorio;
 * en {@link ModoTextoContrato#AUTO} se regenera desde la plantilla.</p>
 */
@Service
public class ContratoTextoResolverService {

    private final GeneradorTextoContratoService generadorTextoService;

    public ContratoTextoResolverService(
            GeneradorTextoContratoService generadorTextoService) {
        this.generadorTextoService = generadorTextoService;
    }

    public String resolver(
            Proyecto proyecto,
            TipoAcuerdoContrato tipoAcuerdo,
            MiembroProyecto emisor,
            List<MiembroProyecto> destinatarios,
            GuardarContratoRequest request) {
        return resolver(proyecto, tipoAcuerdo, emisor, destinatarios, request, false);
    }

    public String resolver(
            Proyecto proyecto,
            TipoAcuerdoContrato tipoAcuerdo,
            MiembroProyecto emisor,
            List<MiembroProyecto> destinatarios,
            GuardarContratoRequest request,
            boolean permitirVacio) {

        ModoTextoContrato modo = request.modoTexto();
        String texto = request.texto();

        boolean manual = modo == ModoTextoContrato.MANUAL
                || (modo == null && texto != null && !texto.isBlank());

        if (manual) {
            String textoManual = texto == null ? null : texto;
            if (permitirVacio && (textoManual == null || textoManual.isBlank())) {
                return textoManual;
            }
            generadorTextoService.validarTexto(textoManual);
            return textoManual;
        }

        return generadorTextoService.generar(
                proyecto,
                tipoAcuerdo,
                emisor,
                destinatarios,
                request.clausulasAdicionales());
    }
}
