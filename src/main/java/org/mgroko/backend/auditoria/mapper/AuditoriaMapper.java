package org.mgroko.backend.auditoria.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.auditoria.dto.AuditoriaDetalleResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.modelo.Auditoria;
import org.mgroko.backend.modelo.DetalleAuditoria;
import org.mgroko.backend.modelo.TipoEventoAuditoria;
import org.mgroko.backend.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Mapea filas de auditoría al contrato de UC-67.
 *
 * <p>La marca {@code legacy} identifica filas anteriores a la migración, que
 * no tienen tipo de evento ni código original. En esas filas el
 * {@code id_usuario} histórico no representa necesariamente al operador real,
 * por eso se oculta el nombre y se muestra un texto explícito.</p>
 */
@Component
public class AuditoriaMapper {

    public static final String OPERADOR_HISTORICO = "No disponible (histórico)";
    public static final String OPERADOR_SISTEMA = "Sistema / automático";

    public AuditoriaEventoResponse toResponse(
            Auditoria auditoria,
            Map<String, TipoEventoAuditoria> catalogo,
            Map<Long, List<DetalleAuditoria>> detallesPorAuditoria,
            Map<Long, Usuario> usuarios) {

        TipoEventoAuditoria tipo = auditoria.getTipoEvento() == null
                ? null
                : catalogo.get(auditoria.getTipoEvento());

        Usuario usuario = auditoria.getIdUsuario() == null
                ? null
                : usuarios.get(auditoria.getIdUsuario());

        boolean legacy = auditoria.getTipoEvento() == null
                && auditoria.getTipoEventoOriginal() == null;

        String operador = resolverOperador(auditoria, usuario, legacy);
        String usuarioNombre = legacy || usuario == null
                ? null
                : nombreCompleto(usuario);

        List<AuditoriaDetalleResponse> detalle = detallesPorAuditoria
                .getOrDefault(auditoria.getIdAuditoria(), Collections.emptyList())
                .stream()
                .map(item -> new AuditoriaDetalleResponse(
                        item.getCampo(),
                        item.getValorAnterior(),
                        item.getValorNuevo()))
                .toList();

        return new AuditoriaEventoResponse(
                auditoria.getIdAuditoria(),
                auditoria.getFechaHora(),
                auditoria.getTipoEvento(),
                tipo != null ? tipo.getNombre() : auditoria.getTipoEvento(),
                auditoria.getEntidadAfectada(),
                auditoria.getEntidadClave(),
                auditoria.getIdAfectado(),
                auditoria.getIdUsuario(),
                usuarioNombre,
                auditoria.getUsuarioBd(),
                auditoria.getIpUsuario(),
                auditoria.getTipoEventoOriginal(),
                legacy,
                operador,
                detalle);
    }

    private String resolverOperador(Auditoria auditoria, Usuario usuario, boolean legacy) {
        if (legacy) {
            return OPERADOR_HISTORICO;
        }
        if (auditoria.getIdUsuario() == null) {
            return OPERADOR_SISTEMA;
        }
        if (usuario == null) {
            return "Usuario #" + auditoria.getIdUsuario();
        }
        String nombre = nombreCompleto(usuario);
        return nombre.isEmpty() ? "Usuario #" + auditoria.getIdUsuario() : nombre;
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
        return (nombre + " " + apellido).trim();
    }
}
