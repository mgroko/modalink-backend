package org.mgroko.backend.contratos.servicio;

import java.util.List;

import org.mgroko.backend.contratos.dto.ContratoEventoResponse;
import org.mgroko.backend.modelo.ContratoEvento;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.repositorio.ContratoEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historial de eventos de un contrato.
 *
 * <p>La visibilidad se delega en {@link ObtenerContratoService}: director
 * activo del proyecto o parte firmante, incluido un exmiembro que conserva su
 * firma. Un usuario ajeno recibe {@code ACCESO_DENEGADO_PROYECTO}.</p>
 */
@Service
public class ListarEventosContratoService {

    private final ObtenerContratoService obtenerContratoService;
    private final ContratoEventoRepository contratoEventoRepository;

    public ListarEventosContratoService(
            ObtenerContratoService obtenerContratoService,
            ContratoEventoRepository contratoEventoRepository) {
        this.obtenerContratoService = obtenerContratoService;
        this.contratoEventoRepository = contratoEventoRepository;
    }

    @Transactional(readOnly = true)
    public List<ContratoEventoResponse> listar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato) {

        // Valida perfil activo, proyecto y visibilidad del contrato.
        obtenerContratoService.obtener(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                idContrato);

        return contratoEventoRepository
                .findByContratoConDetalle(idContrato)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ContratoEventoResponse toResponse(ContratoEvento evento) {
        Perfil perfilParte = evento.getMiembro() != null
                ? evento.getMiembro().getPerfil()
                : null;

        return new ContratoEventoResponse(
                evento.getIdEvento(),
                evento.getTipoEvento() != null
                        ? evento.getTipoEvento().name()
                        : null,
                evento.getFechaHora(),
                evento.getMiembro() != null
                        ? evento.getMiembro().getIdMiembro()
                        : null,
                perfilParte != null ? perfilParte.getIdPerfil() : null,
                nombrePerfil(perfilParte),
                evento.getUsuario() != null
                        ? evento.getUsuario().getIdUsuario()
                        : null,
                nombreUsuario(evento.getUsuario()),
                evento.getContratoVersion() != null
                        ? evento.getContratoVersion().getVersion()
                        : null,
                evento.getMotivo());
    }

    private String nombrePerfil(Perfil perfil) {
        if (perfil == null) {
            return null;
        }
        if (perfil.getNombreArtistico() != null
                && !perfil.getNombreArtistico().isBlank()) {
            return perfil.getNombreArtistico();
        }
        return nombreUsuario(perfil.getUsuario());
    }

    private String nombreUsuario(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        String nombre = (usuario.getNombre() != null ? usuario.getNombre() : "")
                + " "
                + (usuario.getApellido() != null ? usuario.getApellido() : "");
        String resultado = nombre.trim();
        return resultado.isEmpty() ? null : resultado;
    }
}
