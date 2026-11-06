package org.mgroko.backend.admin.servicio;

import org.mgroko.backend.admin.dto.RepararCronogramaRequest;
import org.mgroko.backend.admin.dto.ReparacionCronogramaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reconciliación periódica de cronogramas publicados o confirmados.
 *
 * <p>Reutiliza la detección y reparación administrativa. Por defecto solo
 * detecta; para que mueva actividades {@code PENDIENTE} hay que habilitar
 * {@code agenda.reconciliacion-cronograma.auto-reparar=true}.</p>
 */
@Service
public class ReconciliarCronogramaService {

    private final RepararCronogramaService repararCronogramaService;
    private final boolean autoReparar;
    private final Integer limite;

    public ReconciliarCronogramaService(
            RepararCronogramaService repararCronogramaService,
            @Value("${agenda.reconciliacion-cronograma.auto-reparar:false}")
            boolean autoReparar,
            @Value("${agenda.reconciliacion-cronograma.limite:50}")
            Integer limite) {
        this.repararCronogramaService = repararCronogramaService;
        this.autoReparar = autoReparar;
        this.limite = limite;
    }

    /**
     * Ejecuta un ciclo de reconciliación.
     *
     * @return detecciones y, si {@code auto-reparar} está habilitado, el
     *         resultado de la reparación
     */
    public ReparacionCronogramaResponse ejecutar() {
        return repararCronogramaService.reparar(
                new RepararCronogramaRequest(
                        !autoReparar,
                        null,
                        null,
                        limite));
    }
}
