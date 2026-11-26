package org.mgroko.backend.normas.dto;

import java.util.List;

/**
 * Respuesta pública de una versión de las normas de convivencia.
 *
 * @param version      versión solicitada o vigente
 * @param vigenteDesde fecha ISO de inicio de vigencia
 * @param vigenteHasta fecha ISO de fin de vigencia; {@code null} si sigue vigente
 * @param regla        texto principal de la regla de convivencia
 * @param secciones    contenido explicativo de la versión
 * @param historial    todas las versiones publicadas
 */
public record NormasConvivenciaResponse(
        String titulo,
        String version,
        String vigenteDesde,
        String vigenteHasta,
        String regla,
        List<NormasConvivenciaSeccion> secciones,
        List<NormasConvivenciaVersionResumen> historial) {
}
