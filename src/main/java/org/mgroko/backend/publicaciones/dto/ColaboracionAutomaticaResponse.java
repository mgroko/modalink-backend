package org.mgroko.backend.publicaciones.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoConfirmacionColaboracion;

public record ColaboracionAutomaticaResponse(
        Long idColaboracionAutomatica,
        EstadoColaboracionAutomatica estado,
        EstadoConfirmacionColaboracion miEstado,
        EstadoConfirmacionColaboracion estadoContraparte,
        LocalDateTime fechaDeteccion,
        LocalDateTime fechaConfirmacion,
        LocalDateTime fechaDisolucion,
        PublicacionColaboracionAutomaticaResponse publicacionPropia,
        PublicacionColaboracionAutomaticaResponse publicacionContraparte,
        AutorPublicacionResponse perfilContraparte,
        ImagenColaboracionAutomaticaResponse imagen,
        boolean puedeConfirmar,
        boolean puedeRechazar
) {
}
