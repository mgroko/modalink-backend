package org.mgroko.backend.polaroids.servicio;

import java.util.Objects;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.TipoPolaroid;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.mgroko.backend.polaroids.exception.LimitePolaroidsExcedidoException;
import org.mgroko.backend.polaroids.exception.PolaroidInvalidaException;
import org.mgroko.backend.polaroids.exception.PolaroidsSoloParaModelosException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Reglas compartidas por las operaciones de lectura y escritura de polaroids.
 */
@Service
public class PolaroidDatosService {

    static final String PROFESION_MODELO = "MODELO";

    private final PerfilRepository perfilRepository;
    private final VerPerfilService verPerfilService;
    private final PolaroidCatalogoService polaroidCatalogoService;
    private final int maximoPorPerfil;

    public PolaroidDatosService(
            PerfilRepository perfilRepository,
            VerPerfilService verPerfilService,
            PolaroidCatalogoService polaroidCatalogoService,
            @Value("${app.polaroids.max-por-perfil:8}") int maximoPorPerfil) {
        this.perfilRepository = perfilRepository;
        this.verPerfilService = verPerfilService;
        this.polaroidCatalogoService = polaroidCatalogoService;
        this.maximoPorPerfil = Math.max(1, maximoPorPerfil);
    }

    public int maximoPorPerfil() {
        return maximoPorPerfil;
    }

    /**
     * Valida las reglas de visibilidad de UC-14 para lectura.
     */
    @Transactional(readOnly = true)
    public Perfil validarPerfilVisible(Long idPerfil, Long idUsuarioAutenticado) {
        return verPerfilService.obtenerPerfilVisible(idPerfil, idUsuarioAutenticado).perfil();
    }

    /**
     * Bloquea el perfil propio para serializar altas, bajas y reordenamientos.
     */
    @Transactional
    public Perfil bloquearPerfilPropioModelo(Long idUsuario, Long idPerfil, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }
        if (!Objects.equals(idPerfil, idPerfilActivo)) {
            throw new PerfilNoEncontradoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuarioForUpdate(idPerfil, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() == EstadoPerfil.Baja) {
            throw new PerfilEnBajaException("No se pueden gestionar las polaroids de un perfil dado de baja.");
        }

        if (perfil.getProfesion() == null
                || perfil.getProfesion().getCodigo() == null
                || !PROFESION_MODELO.equalsIgnoreCase(perfil.getProfesion().getCodigo())) {
            throw new PolaroidsSoloParaModelosException();
        }

        return perfil;
    }

    public void validarLimite(long cantidadActual) {
        if (cantidadActual >= maximoPorPerfil) {
            throw new LimitePolaroidsExcedidoException(maximoPorPerfil);
        }
    }

    public TipoPolaroid resolverTipo(Long idTipoPolaroid) {
        return polaroidCatalogoService.obtenerTipo(idTipoPolaroid);
    }

    public void validarArchivoObligatorio(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new PolaroidInvalidaException("Debés adjuntar una imagen para la polaroid.");
        }
    }

    public boolean hayArchivo(MultipartFile archivo) {
        return archivo != null && !archivo.isEmpty();
    }
}
