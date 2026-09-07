package org.mgroko.backend.security;

public record ContextoAutenticacion(
        Long idUsuario,
        Long idPerfilActivo,
        String nombreArtisticoActivo
) {}