package org.mgroko.backend.reportes.dto;

import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de un reporte de publicación (UC-69).
 *
 * <p>La descripción es opcional salvo para el motivo {@code OTRO}, regla que
 * valida el servicio porque depende de otro campo del mismo request.</p>
 */
public record CrearReportePublicacionRequest(
        @NotNull(message = "El motivo del reporte es obligatorio.")
        MotivoReportePublicacion motivo,

        @Size(max = 200, message = "La descripción no puede superar los 200 caracteres.")
        String descripcion
) {}
