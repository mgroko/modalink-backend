package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.mgroko.backend.modelo.enums.EstadoProyecto;

public record AdminDashboardResumen(
        Long totalUsuarios,
        Long proyectosConfirmados,
        Double porcentajeConfirmados,
        List<AdminResumenProvincia> actividadProvincia,
        AdminProfesionDemandada profesionMasDemandada,
        List<AdminTopProvincia> topProvincias,
        List<AdminUltimoProyecto> listaNuevosProyectos
) {
    public record AdminResumenProvincia(
            Long idProvincia,
            String nombreProvincia,
            Long totalPerfiles,
            Long totalProyectos,
            Long totalActividad
    ) {}

    public record AdminProfesionDemandada(
            Long idProfesion,
            String nombreProfesion,
            Long totalPerfiles
    ) {}

    public record AdminTopProvincia(
            Long idProvincia,
            String nombreProvincia,
            Long totalActividad
    ) {}

    public record AdminUltimoProyecto(
            Long idProyecto,
            String nombre,
            LocalDateTime fechaInicio,
            EstadoProyecto estado
    ) {}
}