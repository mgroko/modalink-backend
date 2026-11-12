package org.mgroko.backend.publicaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.mgroko.backend.modelo.ColaboradorPublicacionOrigen;
import org.mgroko.backend.modelo.enums.OrigenColaboracion;
import org.mgroko.backend.repositorio.ColaboradorPublicacionOrigenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestiona el origen de las etiquetas de colaborador.
 */
@Service
public class ColaboradorPublicacionOrigenService {

    private final ColaboradorPublicacionOrigenRepository repository;
    private final Clock reloj;

    public ColaboradorPublicacionOrigenService(
            ColaboradorPublicacionOrigenRepository repository,
            Clock reloj) {
        this.repository = repository;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Optional<ColaboradorPublicacionOrigen> buscar(
            Long idPublicacion,
            Long idPerfil) {
        return repository.findByIdPublicacionAndIdPerfil(idPublicacion, idPerfil);
    }

    /**
     * Devuelve, por publicación, el origen de cada etiqueta de colaborador.
     */
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, OrigenColaboracion>> obtenerOrigenes(
            Collection<Long> idsPublicacion) {

        if (idsPublicacion == null || idsPublicacion.isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<Long, OrigenColaboracion>> resultado = new HashMap<>();
        for (ColaboradorPublicacionOrigen origen : repository.findByIdPublicacionIn(idsPublicacion)) {
            resultado.computeIfAbsent(origen.getIdPublicacion(), id -> new HashMap<>())
                    .put(origen.getIdPerfil(), origen.getOrigen());
        }
        return resultado;
    }

    @Transactional
    public ColaboradorPublicacionOrigen registrarManual(
            Long idPublicacion,
            Long idPerfil) {

        return repository.findByIdPublicacionAndIdPerfil(idPublicacion, idPerfil)
                .map(actual -> {
                    if (actual.getOrigen() == OrigenColaboracion.AUTOMATICA) {
                        actual.setOrigen(OrigenColaboracion.AMBOS);
                    }
                    return repository.save(actual);
                })
                .orElseGet(() -> repository.save(ColaboradorPublicacionOrigen.builder()
                        .idPublicacion(idPublicacion)
                        .idPerfil(idPerfil)
                        .origen(OrigenColaboracion.MANUAL)
                        .fechaAlta(LocalDateTime.now(reloj))
                        .build()));
    }

    @Transactional
    public ColaboradorPublicacionOrigen registrarAutomatica(
            Long idPublicacion,
            Long idPerfil,
            Long idColaboracionAutomatica) {

        return repository.findByIdPublicacionAndIdPerfil(idPublicacion, idPerfil)
                .map(actual -> {
                    actual.setOrigen(actual.getOrigen() == OrigenColaboracion.MANUAL
                            ? OrigenColaboracion.AMBOS
                            : OrigenColaboracion.AUTOMATICA);
                    actual.setIdColaboracionAutomatica(idColaboracionAutomatica);
                    return repository.save(actual);
                })
                .orElseGet(() -> repository.save(ColaboradorPublicacionOrigen.builder()
                        .idPublicacion(idPublicacion)
                        .idPerfil(idPerfil)
                        .origen(OrigenColaboracion.AUTOMATICA)
                        .idColaboracionAutomatica(idColaboracionAutomatica)
                        .fechaAlta(LocalDateTime.now(reloj))
                        .build()));
    }

    @Transactional
    public void eliminar(Long idPublicacion, Long idPerfil) {
        repository.findByIdPublicacionAndIdPerfil(idPublicacion, idPerfil)
                .ifPresent(repository::delete);
    }
}
