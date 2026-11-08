package org.mgroko.backend.contratos.servicio;

import java.util.Locale;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.contratos.dto.HistorialContratoEventoResponse;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.ContratoEvento;
import org.mgroko.backend.modelo.ContratoVersion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.TipoEventoContrato;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.HistorialParametroInvalidoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ContratoEventoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historial de eventos de contratos de un proyecto.
 *
 * <p>El Director activo ve todos los contratos del proyecto. Un miembro activo
 * ve unicamente los eventos de los contratos donde su perfil figura como parte
 * firmante, aunque su membresia ya no este activa. Los contratos eliminados y
 * los de proyectos finalizados tambien forman parte del historial.</p>
 */
@Service
public class ListarHistorialContratosProyectoService {

    private static final int TAMANO_MAXIMO = 100;

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final ContratoEventoRepository contratoEventoRepository;

    public ListarHistorialContratosProyectoService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ProyectoSecurityService proyectoSecurityService,
            ContratoEventoRepository contratoEventoRepository) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.contratoEventoRepository = contratoEventoRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<HistorialContratoEventoResponse> listar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            String tipo,
            String nombre,
            int page,
            int size) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        boolean esDirector = proyectoSecurityService.esDirector(
                idProyecto, idPerfilActivo);

        if (!esDirector && miembroProyectoRepository
                .findMiembroActivoConRol(idProyecto, idPerfilActivo)
                .isEmpty()) {
            throw new AccesoDenegadoProyectoException(
                    "No eres un miembro activo del proyecto.");
        }

        TipoEventoContrato tipoEvento = parsearTipo(tipo);
        validarParametros(page, size);

        String patronNombre = nombre == null || nombre.isBlank()
                ? null
                : LikePatrones.contiene(nombre);

        Pageable pageable = PageRequest.of(page, size);
        Page<ContratoEvento> pagina = contratoEventoRepository
                .findByProyectoConDetalle(
                        idProyecto,
                        idPerfilActivo,
                        !esDirector,
                        tipoEvento,
                        patronNombre,
                        pageable);

        return PaginaResponse.fromPage(pagina.map(this::toResponse));
    }

    private TipoEventoContrato parsearTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        try {
            return TipoEventoContrato.valueOf(
                    tipo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new HistorialParametroInvalidoException(
                    "Tipo de evento de contrato invalido: " + tipo);
        }
    }

    private void validarParametros(int page, int size) {
        if (page < 0) {
            throw new HistorialParametroInvalidoException(
                    "El parametro 'page' no puede ser negativo.");
        }
        if (size < 1 || size > TAMANO_MAXIMO) {
            throw new HistorialParametroInvalidoException(
                    "El parametro 'size' debe estar entre 1 y "
                            + TAMANO_MAXIMO + ".");
        }
    }

    private HistorialContratoEventoResponse toResponse(ContratoEvento evento) {
        Contrato contrato = evento.getContrato();
        Proyecto proyecto = contrato != null ? contrato.getProyecto() : null;
        TipoAcuerdoContrato tipo = contrato != null
                ? contrato.getTipoAcuerdo()
                : null;
        Perfil perfilParte = evento.getMiembro() != null
                ? evento.getMiembro().getPerfil()
                : null;
        ContratoVersion version = evento.getContratoVersion();

        return new HistorialContratoEventoResponse(
                evento.getIdEvento(),
                contrato != null ? contrato.getIdContrato() : null,
                contrato != null ? contrato.getNombre() : null,
                proyecto != null ? proyecto.getIdProyecto() : null,
                proyecto != null ? proyecto.getNombre() : null,
                tipo != null ? tipo.getIdTipoAcuerdo() : null,
                tipo != null ? tipo.getCodigo() : null,
                tipo != null ? tipo.getNombre() : null,
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
                version != null ? version.getVersion() : null,
                evento.getMotivo(),
                contrato != null && contrato.getEstado() != null
                        ? contrato.getEstado().name()
                        : null,
                proyecto != null && proyecto.getEstado() != null
                        ? proyecto.getEstado().name()
                        : null,
                contrato != null ? contrato.getFechaBaja() : null);
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
