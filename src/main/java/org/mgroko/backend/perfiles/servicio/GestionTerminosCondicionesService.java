package org.mgroko.backend.perfiles.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.PerfilTycId;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.ActualizarTerminosCondicionesRequest;
import org.mgroko.backend.perfiles.dto.AceptacionTerminosRequest;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.exception.TerminosCondicionesException;
import org.mgroko.backend.perfiles.mapper.TerminosCondicionesMapper;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PerfilTycRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.PerfilAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * UC-23 — Configurar y consultar los términos y condiciones de un perfil.
 *
 * <p>El texto se versiona: cada cambio real cierra la versión vigente y crea
 * una nueva. La versión vigente es la que se muestra a la comunidad y la que
 * deben aceptar los proyectos al incorporar al perfil.</p>
 */
@Service
public class GestionTerminosCondicionesService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PerfilTycRepository perfilTycRepository;
    private final ConfiguracionSistemaService configuracionSistemaService;
    private final EntityManager entityManager;
    private final Clock reloj;

    public GestionTerminosCondicionesService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            PerfilTycRepository perfilTycRepository,
            ConfiguracionSistemaService configuracionSistemaService,
            EntityManager entityManager,
            Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.perfilTycRepository = perfilTycRepository;
        this.configuracionSistemaService = configuracionSistemaService;
        this.entityManager = entityManager;
        this.reloj = reloj;
    }

    /**
     * Consulta los T&C vigentes de un perfil respetando la visibilidad del
     * perfil: el dueño puede verlo si no está en baja definitiva; un tercero
     * solo si el perfil y su usuario están activos.
     */
    @Transactional(readOnly = true)
    public TerminosCondicionesResponse obtener(Long idUsuario, Long idPerfil) {
        Usuario usuarioAutenticado = buscarUsuarioActivo(idUsuario);

        Perfil perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new PerfilNoEncontradoException());

        boolean propietario = perfil.getUsuario() != null
                && perfil.getUsuario().getIdUsuario().equals(usuarioAutenticado.getIdUsuario());

        if (propietario) {
            if (perfil.getEstado() == EstadoPerfil.Baja) {
                throw new PerfilNoEncontradoException();
            }
        } else if (perfil.getEstado() != EstadoPerfil.Activo
                || perfil.getUsuario() == null
                || perfil.getUsuario().getEstado() != EstadoUsuario.Activo) {
            throw new PerfilNoEncontradoException();
        }

        return obtenerParaPerfil(perfil);
    }

    /**
     * Variante interna usada por el detalle de perfil. No vuelve a validar al
     * usuario autenticado porque el llamador ya aplicó la visibilidad.
     */
    @Transactional(readOnly = true)
    public TerminosCondicionesResponse obtenerParaPerfil(Perfil perfil) {
        Optional<PerfilTyc> vigente = perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(
                        perfil.getIdPerfil());

        return vigente
                .map(tyc -> TerminosCondicionesMapper.toResponse(
                        perfil.getIdPerfil(), tyc))
                .orElseGet(() -> TerminosCondicionesMapper.plantilla(
                        perfil.getIdPerfil(),
                        configuracionSistemaService.obtenerPlantillaTerminosCondiciones()));
    }

    /**
     * Guarda una nueva versión de los T&C del perfil. Solo se permite sobre
     * perfiles en estado {@link EstadoPerfil#ACTIVO}; los perfiles pendientes
     * de baja o dados de baja no pueden operar en el área de usuario.
     */
    @Transactional
    @AuditarOperacion
    public TerminosCondicionesResponse actualizar(
            Long idUsuario,
            Long idPerfil,
            ActualizarTerminosCondicionesRequest request) {

        buscarUsuarioActivo(idUsuario);

        // Bloquea la fila del perfil para serializar dos guardados simultáneos.
        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuarioForUpdate(idPerfil, idUsuario)
                .orElseThrow(() -> new PerfilNoEncontradoException());

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        String texto = request.descripcion().trim();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        PerfilTyc vigente = perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(idPerfil)
                .orElse(null);

        if (vigente != null
                && vigente.getDescripcion() != null
                && vigente.getDescripcion().trim().equals(texto)) {
            return TerminosCondicionesMapper.toResponse(idPerfil, vigente);
        }

        if (vigente != null) {
            LocalDateTime fin = ahora;
            if (!fin.isAfter(vigente.getFechaInicio())) {
                fin = vigente.getFechaInicio().plusNanos(1_000_000);
            }
            vigente.setFechaFin(fin);
            // Se confirma el cierre antes del alta para no chocar con el
            // índice único parcial de versión vigente.
            perfilTycRepository.saveAndFlush(vigente);
            ahora = fin;
        }

        // La identidad de id_tyc se resuelve explícitamente: @GeneratedValue
        // dentro de una @EmbeddedId con PK compuesta no avanza correctamente
        // en todos los casos.
        Long idTyc = perfilTycRepository.nextIdTyc();

        PerfilTyc nueva = PerfilTyc.builder()
                .id(new PerfilTycId(idPerfil, idTyc))
                .perfil(perfil)
                .descripcion(texto)
                .fechaInicio(ahora)
                .build();

        // persist evita el merge sobre la PK compuesta.
        entityManager.persist(nueva);
        entityManager.flush();
        return TerminosCondicionesMapper.toResponse(idPerfil, nueva);
    }

    /**
     * Valida que la aceptación enviada corresponda a la versión vigente del
     * perfil. Devuelve el id de la versión aceptada, o {@code null} cuando el
     * perfil no tiene T&C configurados (en ese caso no se exige aceptación).
     *
     * @throws TerminosCondicionesException si falta la aceptación, si la
     *         versión cambió o si se envió una versión inexistente
     */
    @Transactional(readOnly = true)
    public Long validarAceptacion(
            Long idPerfil,
            AceptacionTerminosRequest aceptacion) {

        PerfilTyc vigente = perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(idPerfil)
                .orElse(null);

        if (vigente == null) {
            if (aceptacion != null && aceptacion.idTyc() != null) {
                throw TerminosCondicionesException.noVigentes(idPerfil);
            }
            return null;
        }

        if (aceptacion == null
                || !Boolean.TRUE.equals(aceptacion.aceptaTerminos())
                || aceptacion.idTyc() == null) {
            throw TerminosCondicionesException.noAceptados(idPerfil, vigente);
        }

        if (!vigente.getIdTyc().equals(aceptacion.idTyc())) {
            throw TerminosCondicionesException.desactualizados(idPerfil, vigente);
        }

        return vigente.getIdTyc();
    }

    /**
     * Versiones vigentes de varios perfiles, indexadas por id de perfil. Se
     * usa en operaciones masivas para evitar una consulta por candidato.
     */
    @Transactional(readOnly = true)
    public Map<Long, PerfilTyc> obtenerVigentes(Collection<Long> idsPerfil) {
        Map<Long, PerfilTyc> resultado = new HashMap<>();
        if (idsPerfil == null || idsPerfil.isEmpty()) {
            return resultado;
        }
        for (PerfilTyc tyc : perfilTycRepository.findVigentesByPerfilIds(idsPerfil)) {
            resultado.put(tyc.getPerfil().getIdPerfil(), tyc);
        }
        return resultado;
    }

    private Usuario buscarUsuarioActivo(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        if (!usuario.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }

        PerfilAccessPolicy.validarNoAdministrador(usuario);
        return usuario;
    }
}
