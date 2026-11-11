package org.mgroko.backend.publicaciones.servicio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.publicaciones.dto.ResumenInteraccionPublicacion;
import org.mgroko.backend.repositorio.ComentarioRepository;
import org.mgroko.backend.repositorio.MeGustaRepository;
import org.mgroko.backend.repositorio.proyeccion.ConteoPorPublicacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-66: resuelve en lote los contadores y el estado de Me gusta de una
 * colección de publicaciones.
 */
@Service
public class PublicacionInteraccionResumenService {

    private final MeGustaRepository meGustaRepository;
    private final ComentarioRepository comentarioRepository;

    public PublicacionInteraccionResumenService(
            MeGustaRepository meGustaRepository,
            ComentarioRepository comentarioRepository) {
        this.meGustaRepository = meGustaRepository;
        this.comentarioRepository = comentarioRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, ResumenInteraccionPublicacion> resumir(
            Collection<Long> idsPublicaciones,
            Long idPerfilActivo) {

        if (idsPublicaciones == null || idsPublicaciones.isEmpty()) {
            return Map.of();
        }

        Set<Long> ids = idsPublicaciones.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> cantidadMeGusta =
                aMapa(meGustaRepository.contarPorPublicaciones(ids));
        Map<Long, Integer> cantidadComentarios =
                aMapa(comentarioRepository.contarPorPublicaciones(ids));

        Set<Long> publicacionesConMeGusta = idPerfilActivo == null
                ? Set.of()
                : Set.copyOf(meGustaRepository.findIdsPublicacionesConMeGusta(
                        ids, idPerfilActivo));

        Map<Long, ResumenInteraccionPublicacion> resumenes = new LinkedHashMap<>();
        for (Long idPublicacion : ids) {
            resumenes.put(
                    idPublicacion,
                    new ResumenInteraccionPublicacion(
                            cantidadMeGusta.getOrDefault(idPublicacion, 0),
                            cantidadComentarios.getOrDefault(idPublicacion, 0),
                            publicacionesConMeGusta.contains(idPublicacion)));
        }

        return resumenes;
    }

    @Transactional(readOnly = true)
    public ResumenInteraccionPublicacion resumirUna(
            Long idPublicacion,
            Long idPerfilActivo) {

        if (idPublicacion == null) {
            return ResumenInteraccionPublicacion.vacio();
        }

        return resumir(List.of(idPublicacion), idPerfilActivo)
                .getOrDefault(idPublicacion, ResumenInteraccionPublicacion.vacio());
    }

    private Map<Long, Integer> aMapa(List<ConteoPorPublicacion> filas) {
        Map<Long, Integer> mapa = new LinkedHashMap<>();
        if (filas == null) {
            return mapa;
        }
        for (ConteoPorPublicacion fila : filas) {
            if (fila.getIdPublicacion() != null && fila.getTotal() != null) {
                mapa.put(fila.getIdPublicacion(), fila.getTotal().intValue());
            }
        }
        return mapa;
    }
}
