package org.mgroko.backend.proyectos.dto;

/**
 * Alerta accionable del informe de proyecto.
 *
 * @param codigo          código estable para UI y tests
 * @param area            área a la que afecta; puede ser null en alertas informativas
 * @param severidad       severidad de la alerta
 * @param mensaje         texto legible para el director
 * @param antiguedadDias  días desde la fecha de referencia; puede ser null
 * @param accionSugerida  acción recomendada
 */
public record AlertaInforme(
        String codigo,
        String area,
        SeveridadInforme severidad,
        String mensaje,
        Long antiguedadDias,
        String accionSugerida) {
}
