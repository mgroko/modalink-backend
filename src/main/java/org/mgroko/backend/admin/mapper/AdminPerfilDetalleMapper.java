package org.mgroko.backend.admin.mapper;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.admin.dto.AdminPerfilDetalleResponse;
import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.CaracteristicaResponse;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesResponse;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

/**
 * Arma la vista administrativa completa de un perfil.
 */
public final class AdminPerfilDetalleMapper {

    private AdminPerfilDetalleMapper() {
    }

    public static AdminPerfilDetalleResponse toDetalle(
            Perfil perfil,
            TerminosCondicionesResponse terminosCondiciones,
            Integer diasBaja,
            List<AdminPerfilDetalleResponse.ProyectoRelacionado> proyectos,
            long totalProyectos,
            List<AdminPerfilDetalleResponse.ContratoRelacionado> contratos,
            long totalContratos,
            List<AdminPerfilDetalleResponse.PublicacionRelacionada> publicaciones,
            long totalPublicaciones) {

        Usuario usuario = perfil.getUsuario();

        List<String> habilidades = perfil.getHabilidades() == null
                ? List.of()
                : perfil.getHabilidades().stream()
                        .sorted(Comparator.comparing(org.mgroko.backend.modelo.Habilidad::getNombre))
                        .map(org.mgroko.backend.modelo.Habilidad::getNombre)
                        .toList();

        List<CaracteristicaResponse> caracteristicas = perfil.getCaracteristicas() == null
                ? List.of()
                : perfil.getCaracteristicas().stream()
                        .sorted(Comparator.comparing(
                                c -> c.getCaracteristicaTecnica().getCodigo()))
                        .map(AdminPerfilDetalleMapper::toCaracteristica)
                        .toList();

        Long idImagen = perfil.getImagen() != null ? perfil.getImagen().getIdImagen() : null;
        String fotoUrl = perfil.getImagen() != null ? perfil.getImagen().getUrl() : null;

        return new AdminPerfilDetalleResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getBiografia(),
                perfil.getEstado() != null ? perfil.getEstado().name() : null,
                perfil.getFechaSolicitudBaja(),
                fechaLimiteBaja(perfil, diasBaja),
                perfil.getProfesion() != null ? perfil.getProfesion().getIdProfesion() : null,
                perfil.getProfesion() != null ? perfil.getProfesion().getNombre() : null,
                idImagen,
                fotoUrl,
                toUsuarioAdmin(usuario),
                usuario != null ? UbicacionMapper.toResponse(usuario.getUbicacion()) : null,
                habilidades,
                caracteristicas,
                terminosCondiciones,
                new AdminPerfilDetalleResponse.RelacionadosResumen(
                        totalProyectos, totalContratos, totalPublicaciones),
                proyectos,
                contratos,
                publicaciones);
    }

    private static AdminPerfilDetalleResponse.UsuarioAdmin toUsuarioAdmin(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new AdminPerfilDetalleResponse.UsuarioAdmin(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getDni(),
                usuario.getFechaNacimiento(),
                usuario.getGenero() != null ? usuario.getGenero().getCodigo() : null,
                usuario.getEstado() != null ? usuario.getEstado().name() : null,
                usuario.getRolGlobal() != null ? usuario.getRolGlobal().getNombre() : null,
                usuario.getFechaSolicitudBaja(),
                usuario.getMotivoDeshabilitacion(),
                usuario.getFechaHastaDeshabilitacion());
    }

    private static CaracteristicaResponse toCaracteristica(CaracteristicaPerfil cp) {
        ValorCaracteristica valor = cp.getValorCaracteristica();
        return new CaracteristicaResponse(
                cp.getCaracteristicaTecnica().getIdCaracteristica(),
                cp.getCaracteristicaTecnica().getCodigo(),
                cp.getValor(),
                valor != null ? valor.getIdValor() : null,
                valor != null ? valor.getEtiqueta() : null,
                valor != null ? valor.getColorHex() : null);
    }

    private static LocalDateTime fechaLimiteBaja(Perfil perfil, Integer diasBaja) {
        if (diasBaja == null
                || perfil.getEstado() != EstadoPerfil.PENDIENTE_BAJA
                || perfil.getFechaSolicitudBaja() == null) {
            return null;
        }
        return perfil.getFechaSolicitudBaja().plusDays(diasBaja);
    }
}
