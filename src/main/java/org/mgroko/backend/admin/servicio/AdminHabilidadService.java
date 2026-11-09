package org.mgroko.backend.admin.servicio;

import java.util.List;
import java.util.Objects;

import org.mgroko.backend.admin.dto.AdminHabilidadRequest;
import org.mgroko.backend.admin.exception.HabilidadDuplicadaException;
import org.mgroko.backend.admin.exception.HabilidadEnUsoException;
import org.mgroko.backend.common.util.CodigoHabilidad;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.perfiles.evento.HabilidadEliminadaEvent;
import org.mgroko.backend.perfiles.evento.HabilidadModificadaEvent;
import org.mgroko.backend.perfiles.mapper.HabilidadMapper;
import org.mgroko.backend.perfiles.servicio.HabilidadService;
import org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * UC-56 — Gestionar habilidades del sistema.
 */
@Service
public class AdminHabilidadService {

    private final HabilidadRepository habilidadRepository;
    private final HabilidadService habilidadService;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final PerfilRepository perfilRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminHabilidadService(
            HabilidadRepository habilidadRepository,
            HabilidadService habilidadService,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            PerfilRepository perfilRepository,
            ApplicationEventPublisher eventPublisher) {
        this.habilidadRepository = habilidadRepository;
        this.habilidadService = habilidadService;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.perfilRepository = perfilRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<HabilidadResponse> listar(String nombre) {
        return habilidadService.buscar(nombre);
    }

    @Transactional(readOnly = true)
    public HabilidadResponse obtener(Long idHabilidad) {
        return HabilidadMapper.toResponse(buscarOFallar(idHabilidad));
    }

    @Transactional
    @AuditarOperacion
    public HabilidadResponse crear(AdminHabilidadRequest request) {
        String nombre = normalizarNombre(request.nombre());
        if (habilidadRepository.existsByNombreIgnoreCase(nombre)) {
            throw new HabilidadDuplicadaException(
                    "Ya existe una habilidad con el nombre " + nombre + ".");
        }

        Habilidad habilidad = Habilidad.builder()
                .codigo(generarCodigoUnico(nombre))
                .nombre(nombre)
                .descripcion(normalizarDescripcion(request.descripcion()))
                .build();

        return HabilidadMapper.toResponse(habilidadRepository.save(habilidad));
    }

    @Transactional
    @AuditarOperacion
    public HabilidadResponse actualizar(Long idHabilidad, AdminHabilidadRequest request) {
        Habilidad habilidad = buscarOFallar(idHabilidad);
        String nombre = normalizarNombre(request.nombre());
        String descripcion = normalizarDescripcion(request.descripcion());

        if (habilidadRepository.existsByNombreIgnoreCaseAndIdHabilidadNot(nombre, idHabilidad)) {
            throw new HabilidadDuplicadaException(
                    "Ya existe una habilidad con el nombre " + nombre + ".");
        }

        boolean huboCambios = !habilidad.getNombre().equals(nombre)
                || !Objects.equals(habilidad.getDescripcion(), descripcion);

        // El código es inmutable: se generó al crear la habilidad y no se
        // recalcula cuando cambia el nombre.
        habilidad.setNombre(nombre);
        habilidad.setDescripcion(descripcion);

        Habilidad guardada = habilidadRepository.save(habilidad);

        if (huboCambios) {
            List<Long> idsPerfiles = perfilRepository.findIdsPerfilByHabilidad(idHabilidad);
            if (!idsPerfiles.isEmpty()) {
                eventPublisher.publishEvent(new HabilidadModificadaEvent(
                        idHabilidad, guardada.getNombre(), List.copyOf(idsPerfiles)));
            }
        }

        return HabilidadMapper.toResponse(guardada);
    }

    @Transactional
    @AuditarOperacion
    public void eliminar(Long idHabilidad) {
        Habilidad habilidad = buscarOFallar(idHabilidad);

        if (requerimientoGralProyectoRepository.existsByHabilidades_IdHabilidad(idHabilidad)
                || requerimientoActividadRepository.existsByHabilidades_IdHabilidad(idHabilidad)) {
            throw new HabilidadEnUsoException(
                    "La habilidad está en uso por requerimientos de proyectos y no puede eliminarse.");
        }

        List<Long> idsPerfiles = perfilRepository.findIdsPerfilByHabilidad(idHabilidad);
        String nombre = habilidad.getNombre();

        habilidadRepository.desasociarDePerfiles(idHabilidad);
        habilidadRepository.delete(habilidad);

        if (!idsPerfiles.isEmpty()) {
            eventPublisher.publishEvent(new HabilidadEliminadaEvent(
                    idHabilidad, nombre, List.copyOf(idsPerfiles)));
        }
    }

    private Habilidad buscarOFallar(Long idHabilidad) {
        return habilidadRepository.findById(idHabilidad)
                .orElseThrow(() -> new HabilidadNoEncontradaException(idHabilidad));
    }

    private String normalizarNombre(String nombre) {
        return nombre.trim();
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String valor = descripcion.trim();
        return valor.isEmpty() ? null : valor;
    }

    /**
     * Genera un código único a partir del nombre. Si el código base ya existe,
     * agrega un sufijo numérico respetando la longitud máxima de la columna.
     */
    private String generarCodigoUnico(String nombre) {
        String base = CodigoHabilidad.normalizar(nombre);
        String candidato = base;
        int sufijo = 2;

        while (habilidadRepository.existsByCodigoIgnoreCase(candidato)) {
            String sufijoTexto = "_" + sufijo++;
            candidato = CodigoHabilidad.recortarParaSufijo(base, sufijoTexto) + sufijoTexto;
        }

        return candidato;
    }
}
