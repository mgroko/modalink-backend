package org.mgroko.backend.admin.dto;

import java.time.LocalDateTime;

public record AdminPerfilResponse(
    Long idPerfil,
    String nombreArtistico,
    String biografia,
    String estado,
    String profesion,
    LocalDateTime fechaSolicitudBaja
) {}
