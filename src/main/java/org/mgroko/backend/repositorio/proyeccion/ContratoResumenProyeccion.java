package org.mgroko.backend.repositorio.proyeccion;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoProyecto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Proyeccion de resumen de contrato para UC-68. No incluye el texto legal.
 */
@Getter
@AllArgsConstructor
public class ContratoResumenProyeccion {

    private final Long idContrato;
    private final String nombre;
    private final EstadoContrato estado;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaEmision;
    private final LocalDateTime fechaModificacion;
    private final LocalDateTime fechaBaja;
    private final Integer versionActual;
    private final Long idProyecto;
    private final String nombreProyecto;
    private final EstadoProyecto estadoProyecto;
    private final Long idTipoAcuerdo;
    private final String codigoTipoAcuerdo;
    private final String nombreTipoAcuerdo;
    private final Long idMiembroEmisor;
}
