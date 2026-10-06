package org.mgroko.backend.perfiles.mapper;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.CaracteristicaResponse;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

public class PerfilMapper {

    private PerfilMapper() {}

    public static PerfilResponse toResponse(Perfil perfil) {
        return toResponse(perfil, null);
    }

    public static PerfilResponse toResponse(Perfil perfil, Integer diasBaja) {
        List<CaracteristicaResponse> caracteristicas = perfil.getCaracteristicas().stream()
                .sorted(Comparator.comparing(c -> c.getCaracteristicaTecnica().getCodigo()))
                .map(PerfilMapper::toCaracteristicaResponse)
                .toList();

        Long idImagen = perfil.getImagen() != null ? perfil.getImagen().getIdImagen() : null;
        String fotoUrl = perfil.getImagen() != null ? perfil.getImagen().getUrl() : null;

        return new PerfilResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getBiografia(),
                perfil.getEstado().getNombre(),
                perfil.getProfesion().getNombre(),
                perfil.getFechaSolicitudBaja(),
                idImagen,
                fotoUrl,
                caracteristicas,
                fechaLimite(perfil, diasBaja));
    }

    public static org.mgroko.backend.perfiles.dto.PerfilBusquedaResponse toBusquedaResponse(Perfil perfil) {
        List<CaracteristicaResponse> caracteristicas = perfil.getCaracteristicas().stream()
                .sorted(Comparator.comparing(c -> c.getCaracteristicaTecnica().getCodigo()))
                .map(PerfilMapper::toCaracteristicaResponse)
                .toList();

        List<String> habilidades = perfil.getHabilidades().stream()
                .sorted(Comparator.comparing(org.mgroko.backend.modelo.Habilidad::getNombre))
                .map(org.mgroko.backend.modelo.Habilidad::getNombre)
                .toList();

        Long idImagen = perfil.getImagen() != null ? perfil.getImagen().getIdImagen() : null;
        String fotoUrl = perfil.getImagen() != null ? perfil.getImagen().getUrl() : null;

        var usuario = perfil.getUsuario();
        var ubicacion = usuario.getUbicacion();
        var genero = usuario.getGenero();

        return new org.mgroko.backend.perfiles.dto.PerfilBusquedaResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getBiografia(),
                perfil.getEstado().getNombre(),
                perfil.getProfesion().getIdProfesion(),
                perfil.getProfesion().getNombre(),
                idImagen,
                fotoUrl,
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                genero != null ? genero.getCodigo() : null,
                UbicacionMapper.toCiudadResponse(ubicacion != null ? ubicacion.getCiudad() : null),
                habilidades,
                caracteristicas
        );
    }

    public static org.mgroko.backend.perfiles.dto.PerfilDetalleResponse toDetalleResponse(Perfil perfil,
            boolean esPropietario) {
        return toDetalleResponse(perfil, esPropietario, null);
    }

    public static org.mgroko.backend.perfiles.dto.PerfilDetalleResponse toDetalleResponse(Perfil perfil,
            boolean esPropietario, Integer diasBaja) {
        List<CaracteristicaResponse> caracteristicas = perfil.getCaracteristicas().stream()
                .sorted(Comparator.comparing(c -> c.getCaracteristicaTecnica().getCodigo()))
                .map(PerfilMapper::toCaracteristicaResponse)
                .toList();

        List<String> habilidades = perfil.getHabilidades().stream()
                .sorted(Comparator.comparing(org.mgroko.backend.modelo.Habilidad::getNombre))
                .map(org.mgroko.backend.modelo.Habilidad::getNombre)
                .toList();

        Long idImagen = perfil.getImagen() != null ? perfil.getImagen().getIdImagen() : null;
        String fotoUrl = perfil.getImagen() != null ? perfil.getImagen().getUrl() : null;

        var usuario = perfil.getUsuario();
        var ubicacion = usuario.getUbicacion();
        var genero = usuario.getGenero();

        return new org.mgroko.backend.perfiles.dto.PerfilDetalleResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getBiografia(),
                perfil.getEstado().getNombre(),
                perfil.getFechaSolicitudBaja(),
                perfil.getProfesion().getIdProfesion(),
                perfil.getProfesion().getNombre(),
                idImagen,
                fotoUrl,
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                genero != null ? genero.getCodigo() : null,
                UbicacionMapper.toCiudadResponse(ubicacion != null ? ubicacion.getCiudad() : null),
                habilidades,
                caracteristicas,
                esPropietario,
                fechaLimite(perfil, diasBaja)
        );
    }

    private static LocalDateTime fechaLimite(Perfil perfil, Integer diasBaja) {
        if (diasBaja == null || perfil.getEstado() != EstadoPerfil.PENDIENTE_BAJA
                || perfil.getFechaSolicitudBaja() == null) {
            return null;
        }
        return perfil.getFechaSolicitudBaja().plusDays(diasBaja);
    }

    private static CaracteristicaResponse toCaracteristicaResponse(CaracteristicaPerfil cp) {
        ValorCaracteristica valor = cp.getValorCaracteristica();
        return new CaracteristicaResponse(
                cp.getCaracteristicaTecnica().getIdCaracteristica(),
                cp.getCaracteristicaTecnica().getCodigo(),
                cp.getValor(),
                valor != null ? valor.getIdValor() : null,
                valor != null ? valor.getEtiqueta() : null,
                valor != null ? valor.getColorHex() : null);
    }
}