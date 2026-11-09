package org.mgroko.backend.conexiones.dto;

import java.util.List;

/**
 * Resumen de la red para el hero y los chips de profesión del mockup.
 */
public record ResumenConexionesResponse(
        long totalConexiones,
        long totalEnLinea,
        List<ProfesionConexionResponse> profesiones
) {
}
