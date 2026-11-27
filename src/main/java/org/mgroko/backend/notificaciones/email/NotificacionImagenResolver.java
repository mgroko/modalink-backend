package org.mgroko.backend.notificaciones.email;

import java.util.Optional;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.ColaboracionAutomaticaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve la imagen que debe mostrarse en los mails de colaboración
 * automática.
 *
 * <p>La notificación referencia la colaboración por {@code entidadTipo +
 * entidadId}. La imagen es la que disparó la detección
 * ({@code colaboracion.imagen}). La URL almacenada es relativa
 * ({@code /uploads/...}), así que se completa con la base pública configurada
 * para los mails.</p>
 */
@Component
public class NotificacionImagenResolver {

    private static final String BASE_POR_DEFECTO = "http://localhost:8080";

    private final ColaboracionAutomaticaRepository colaboracionRepository;
    private final String baseUrl;

    public NotificacionImagenResolver(
            ColaboracionAutomaticaRepository colaboracionRepository,
            @Value("${app.notificaciones.email.base-url:http://localhost:8080}") String baseUrl) {
        this.colaboracionRepository = colaboracionRepository;
        this.baseUrl = normalizarBase(baseUrl);
    }

    /**
     * Devuelve la URL absoluta de la imagen de la colaboración referenciada por
     * la notificación, o vacío si no aplica o la imagen ya no existe.
     */
    @Transactional(readOnly = true)
    public Optional<String> resolver(Notificacion notificacion) {
        if (notificacion == null
                || notificacion.getTipo() == null
                || notificacion.getEntidadId() == null
                || !NotificacionService.ENTIDAD_COLABORACION_AUTOMATICA
                        .equals(notificacion.getEntidadTipo())
                || !esTipoColaboracion(notificacion.getTipo())) {
            return Optional.empty();
        }

        return colaboracionRepository
                .findImagenUrlById(notificacion.getEntidadId())
                .filter(url -> !url.isBlank())
                .map(this::urlAbsoluta);
    }

    private boolean esTipoColaboracion(String tipo) {
        return switch (tipo) {
            case NotificacionService.TIPO_COLABORACION_AUTOMATICA_DETECTADA,
                 NotificacionService.TIPO_COLABORACION_AUTOMATICA_CONFIRMADA,
                 NotificacionService.TIPO_COLABORACION_AUTOMATICA_RECHAZADA,
                 NotificacionService.TIPO_COLABORACION_AUTOMATICA_CANCELADA,
                 NotificacionService.TIPO_COLABORACION_AUTOMATICA_DISUELTA -> true;
            default -> false;
        };
    }

    private String urlAbsoluta(String url) {
        String limpia = url.trim();
        if (limpia.startsWith("http://") || limpia.startsWith("https://")) {
            return limpia;
        }
        return limpia.startsWith("/") ? baseUrl + limpia : baseUrl + "/" + limpia;
    }

    private static String normalizarBase(String valor) {
        if (valor == null || valor.isBlank()) {
            return BASE_POR_DEFECTO;
        }
        return valor.trim().replaceAll("/+$", "");
    }
}
