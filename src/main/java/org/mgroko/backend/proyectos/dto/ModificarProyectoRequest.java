package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModificarProyectoRequest(
        @NotBlank(message = "La descripción del proyecto es obligatoria.")
        @Size(max = 200, message = "La descripción del proyecto no puede superar los 200 caracteres.")
        String descripcion,

        @NotNull(message = "La privacidad del proyecto es obligatoria.")
        Privacidad privacidad,

        Boolean aceptaPostulacionGral,

        LocalDate fechaEntregaEstipulada,

        @Valid
        UbicacionRequest ubicacion,

        List<@Valid CrearObjetivoRequest> objetivos,

        List<@Valid ModificarRequerimientoGralRequest> requerimientosGral,

        @Valid
        CrearMoodboardRequest moodboard,

        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres.")
        String motivoCambioFechaEntrega
) {

    /**
     * Constructor histórico para consumidores que todavía no envían motivo.
     */
    public ModificarProyectoRequest(
            String descripcion,
            Privacidad privacidad,
            Boolean aceptaPostulacionGral,
            LocalDate fechaEntregaEstipulada,
            UbicacionRequest ubicacion,
            List<@Valid CrearObjetivoRequest> objetivos,
            List<ModificarRequerimientoGralRequest> requerimientosGral,
            CrearMoodboardRequest moodboard) {
        this(
                descripcion,
                privacidad,
                aceptaPostulacionGral,
                fechaEntregaEstipulada,
                ubicacion,
                objetivos,
                requerimientosGral,
                moodboard,
                null);
    }
}
