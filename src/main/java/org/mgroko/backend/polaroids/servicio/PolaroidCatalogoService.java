package org.mgroko.backend.polaroids.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mgroko.backend.modelo.GrupoPolaroid;
import org.mgroko.backend.modelo.TipoPolaroid;
import org.mgroko.backend.polaroids.dto.GrupoPolaroidResponse;
import org.mgroko.backend.polaroids.dto.TipoPolaroidResponse;
import org.mgroko.backend.polaroids.exception.TipoPolaroidNoEncontradoException;
import org.mgroko.backend.repositorio.GrupoPolaroidRepository;
import org.mgroko.backend.repositorio.TipoPolaroidRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura del catálogo fijo de grupos y tipos de polaroid.
 */
@Service
public class PolaroidCatalogoService {

    private final GrupoPolaroidRepository grupoPolaroidRepository;
    private final TipoPolaroidRepository tipoPolaroidRepository;

    public PolaroidCatalogoService(
            GrupoPolaroidRepository grupoPolaroidRepository,
            TipoPolaroidRepository tipoPolaroidRepository) {
        this.grupoPolaroidRepository = grupoPolaroidRepository;
        this.tipoPolaroidRepository = tipoPolaroidRepository;
    }

    @Transactional(readOnly = true)
    public List<GrupoPolaroidResponse> listar() {
        List<GrupoPolaroid> grupos = grupoPolaroidRepository.findAllByOrderByIdGrupoPolaroidAsc();
        List<TipoPolaroid> tipos = tipoPolaroidRepository.findAllByOrderByIdTipoPolaroidAsc();

        Map<Long, List<TipoPolaroid>> tiposPorGrupo = tipos.stream()
                .collect(Collectors.groupingBy(
                        tipo -> tipo.getGrupo().getIdGrupoPolaroid(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return grupos.stream()
                .map(grupo -> new GrupoPolaroidResponse(
                        grupo.getIdGrupoPolaroid(),
                        grupo.getCodigo(),
                        grupo.getNombre(),
                        tiposPorGrupo.getOrDefault(grupo.getIdGrupoPolaroid(), List.of()).stream()
                                .map(this::toResponse)
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoPolaroid obtenerTipo(Long idTipoPolaroid) {
        if (idTipoPolaroid == null) {
            throw new TipoPolaroidNoEncontradoException(null);
        }
        return tipoPolaroidRepository.findById(idTipoPolaroid)
                .orElseThrow(() -> new TipoPolaroidNoEncontradoException(idTipoPolaroid));
    }

    public TipoPolaroidResponse toResponse(TipoPolaroid tipo) {
        return new TipoPolaroidResponse(
                tipo.getIdTipoPolaroid(),
                tipo.getCodigo(),
                tipo.getNombre(),
                nombreCompleto(tipo));
    }

    private String nombreCompleto(TipoPolaroid tipo) {
        if (tipo.getGrupo() == null || tipo.getGrupo().getNombre() == null) {
            return tipo.getNombre();
        }
        String grupo = tipo.getGrupo().getNombre();
        String nombre = tipo.getNombre();
        if (nombre == null || grupo.equalsIgnoreCase(nombre)) {
            return grupo;
        }
        return grupo + " · " + nombre;
    }
}
