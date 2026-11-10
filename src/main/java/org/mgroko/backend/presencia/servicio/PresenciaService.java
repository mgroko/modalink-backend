package org.mgroko.backend.presencia.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.presencia.config.PresenciaProperties;
import org.mgroko.backend.presencia.dto.PresenciaPerfilResponse;
import org.mgroko.backend.presencia.dto.PresenciaPerfilRow;
import org.mgroko.backend.presencia.evento.PresenciaActividadRegistrada;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Presencia por perfil: última actividad, cálculo de {@code enLinea} y
 * snapshot para consultas puntuales.
 */
@Service
public class PresenciaService {

    private final PerfilActivoService perfilActivoService;
    private final PerfilRepository perfilRepository;
    private final PresenciaProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock reloj;

    public PresenciaService(
            PerfilActivoService perfilActivoService,
            PerfilRepository perfilRepository,
            PresenciaProperties properties,
            ApplicationEventPublisher eventPublisher,
            Clock reloj) {
        this.perfilActivoService = perfilActivoService;
        this.perfilRepository = perfilRepository;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.reloj = reloj;
    }

    @Transactional
    public void registrarActividad(Long idUsuario, Long idPerfilActivo) {
        Perfil perfil = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        LocalDateTime ahora = ahora();
        LocalDateTime limite = limite(ahora);

        perfilRepository.actualizarUltimaActividad(
                perfil.getIdPerfil(), idUsuario, ahora, limite);

        eventPublisher.publishEvent(new PresenciaActividadRegistrada(
                perfil.getIdPerfil(), ahora, limite));
    }

    /**
     * Calcula presencia con el estado del perfil y el de su usuario. Un perfil
     * activo cuyo usuario está en baja o deshabilitado no está en línea.
     */
    public boolean estaEnLinea(Perfil perfil, LocalDateTime ahora) {
        if (perfil == null) {
            return false;
        }

        return estaEnLinea(
                perfil.getEstado(),
                perfil.getUsuario() != null ? perfil.getUsuario().getEstado() : null,
                perfil.getUltimaActividad(),
                ahora);
    }

    public boolean estaEnLinea(
            EstadoPerfil estadoPerfil,
            EstadoUsuario estadoUsuario,
            LocalDateTime ultimaActividad,
            LocalDateTime ahora) {

        return estadoPerfil == EstadoPerfil.ACTIVO
                && estadoUsuario == EstadoUsuario.ACTIVO
                && ultimaActividad != null
                && !ultimaActividad.isBefore(limite(ahora));
    }

    @Transactional(readOnly = true)
    public List<PresenciaPerfilResponse> snapshot(Collection<Long> idsPerfil) {
        if (idsPerfil == null || idsPerfil.isEmpty()) {
            return List.of();
        }

        List<Long> ids = idsPerfil.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(properties.getMaxIds())
                .toList();

        Map<Long, PresenciaPerfilRow> porId = perfilRepository
                .buscarPresenciaPorIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        PresenciaPerfilRow::idPerfil,
                        fila -> fila));

        LocalDateTime ahora = ahora();

        return ids.stream()
                .map(idPerfil -> {
                    PresenciaPerfilRow fila = porId.get(idPerfil);
                    boolean enLinea = fila != null && estaEnLinea(
                            fila.estado(),
                            fila.estadoUsuario(),
                            fila.ultimaActividad(),
                            ahora);
                    return new PresenciaPerfilResponse(idPerfil, enLinea);
                })
                .toList();
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    public LocalDateTime limite(LocalDateTime ahora) {
        return ahora.minus(properties.getUmbralEnLinea());
    }
}
