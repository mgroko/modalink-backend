package org.mgroko.backend.contratos.servicio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.BuscarContratosFiltro;
import org.mgroko.backend.contratos.dto.ContratoBusquedaResponse;
import org.mgroko.backend.contratos.exception.ContratoFiltroInvalidoException;
import org.mgroko.backend.contratos.mapper.ContratoMapper;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.proyeccion.ContratoResumenProyeccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-68: busqueda global de contratos visibles para el perfil activo.
 */
@Service
public class BuscarContratoService {

    private static final int TAMANO_POR_DEFECTO = 20;
    private static final int TAMANO_MAXIMO = 50;

    private static final List<EstadoContrato> ESTADOS_POR_DEFECTO = List.of(
            EstadoContrato.PENDIENTE_FIRMA,
            EstadoContrato.ACTIVO,
            EstadoContrato.RECHAZADO_POR_PARTES,
            EstadoContrato.CANCELADO);

    private final PerfilRepository perfilRepository;
    private final ContratoRepository contratoRepository;
    private final FirmaContratoRepository firmaContratoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;

    public BuscarContratoService(
            PerfilRepository perfilRepository,
            ContratoRepository contratoRepository,
            FirmaContratoRepository firmaContratoRepository,
            MiembroProyectoRepository miembroProyectoRepository) {
        this.perfilRepository = perfilRepository;
        this.contratoRepository = contratoRepository;
        this.firmaContratoRepository = firmaContratoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ContratoBusquedaResponse> buscar(
            Long idUsuario,
            Long idPerfilActivo,
            BuscarContratosFiltro filtro,
            int page,
            int size) {
        return buscar(idUsuario, idPerfilActivo, filtro, page, size, false);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ContratoBusquedaResponse> buscar(
            Long idUsuario,
            Long idPerfilActivo,
            BuscarContratosFiltro filtro,
            int page,
            int size,
            boolean miFirmaPendiente) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        BuscarContratosFiltro normalizado = normalizar(filtro);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                size <= 0 ? TAMANO_POR_DEFECTO : Math.min(size, TAMANO_MAXIMO));

        Page<ContratoResumenProyeccion> pagina =
                contratoRepository.buscar(
                        normalizado,
                        idPerfilActivo,
                        miFirmaPendiente,
                        pageable);

        if (pagina.isEmpty()) {
            return new PaginaResponse<>(
                    List.of(),
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    0,
                    0,
                    true,
                    true);
        }

        List<Long> idsContrato = pagina.getContent().stream()
                .map(ContratoResumenProyeccion::getIdContrato)
                .toList();

        Map<Long, List<FirmaContrato>> firmasPorContrato =
                firmaContratoRepository.findConPartesPorContratos(idsContrato)
                        .stream()
                        .collect(Collectors.groupingBy(FirmaContrato::getIdContrato));

        Set<Long> idsProyecto = pagina.getContent().stream()
                .map(ContratoResumenProyeccion::getIdProyecto)
                .collect(Collectors.toSet());

        Set<Long> proyectosDirector = Set.copyOf(
                miembroProyectoRepository.findIdsProyectosDondeEsDirectorActivo(
                        idPerfilActivo,
                        idsProyecto));

        Map<Long, Long> miembroActivoPorProyecto = new HashMap<>();
        for (MiembroProyecto miembro : miembroProyectoRepository
                .buscarMiembrosActivosDeProyectos(
                        new ArrayList<>(idsProyecto),
                        idPerfilActivo)) {
            miembroActivoPorProyecto.put(
                    miembro.getProyecto().getIdProyecto(),
                    miembro.getIdMiembro());
        }

        List<ContratoBusquedaResponse> contenido = pagina.getContent().stream()
                .map(resumen -> {
                    List<FirmaContrato> firmas = firmasPorContrato
                            .getOrDefault(resumen.getIdContrato(), List.of());

                    Long idMiembroActivo = miembroActivoPorProyecto
                            .get(resumen.getIdProyecto());

                    FirmaContrato miFirma = idMiembroActivo == null
                            ? null
                            : firmas.stream()
                                    .filter(f -> idMiembroActivo.equals(f.getIdMiembro()))
                                    .findFirst()
                                    .orElse(null);

                    boolean firmaPendiente = miFirma != null
                            && miFirma.getEstadoFirma() == EstadoFirmaContrato.PENDIENTE;

                    boolean firmasRegistradas = firmas.stream()
                            .anyMatch(f -> f.getEstadoFirma() == EstadoFirmaContrato.FIRMADO
                                    || f.getEstadoFirma() == EstadoFirmaContrato.RECHAZADO
                                    || f.getEstadoFirma() == EstadoFirmaContrato.INVALIDADA);

                    boolean proyectoApto = resumen.getEstadoProyecto() == EstadoProyecto.PUBLICADO
                            || resumen.getEstadoProyecto() == EstadoProyecto.CONFIRMADO;

                    boolean estadoPendiente =
                            resumen.getEstado() == EstadoContrato.PENDIENTE_FIRMA;

                    boolean puedeFirmar = estadoPendiente
                            && proyectoApto
                            && firmaPendiente;

                    boolean puedeRechazar = puedeFirmar;

                    boolean puedeModificar = proyectosDirector.contains(resumen.getIdProyecto())
                            && estadoPendiente
                            && proyectoApto
                            && !firmasRegistradas;

                    boolean proyectoEliminable =
                            resumen.getEstadoProyecto() != EstadoProyecto.FINALIZADO
                                    && resumen.getEstadoProyecto() != EstadoProyecto.CANCELADO;

                    boolean puedeEliminar = proyectosDirector.contains(resumen.getIdProyecto())
                            && proyectoEliminable
                            && resumen.getEstado() != EstadoContrato.ELIMINADO;

                    return ContratoMapper.toBusquedaResponse(
                            resumen,
                            firmas,
                            idPerfilActivo,
                            idMiembroActivo,
                            puedeModificar,
                            puedeFirmar,
                            puedeRechazar,
                            puedeEliminar);
                })
                .toList();

        Page<ContratoBusquedaResponse> resultado = new PageImpl<>(
                contenido,
                pageable,
                pagina.getTotalElements());

        return PaginaResponse.fromPage(resultado);
    }

    private BuscarContratosFiltro normalizar(BuscarContratosFiltro filtro) {
        if (filtro == null) {
            return new BuscarContratosFiltro(
                    null,
                    ESTADOS_POR_DEFECTO,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        List<EstadoContrato> estados = filtro.estados() == null || filtro.estados().isEmpty()
                ? ESTADOS_POR_DEFECTO
                : filtro.estados();

        validarRango(filtro.fechaEmisionDesde(), filtro.fechaEmisionHasta());
        validarRango(filtro.fechaFirmaDesde(), filtro.fechaFirmaHasta());

        return new BuscarContratosFiltro(
                filtro.idProyecto(),
                estados,
                filtro.parte(),
                filtro.fechaEmisionDesde(),
                filtro.fechaEmisionHasta(),
                filtro.fechaFirmaDesde(),
                filtro.fechaFirmaHasta(),
                filtro.idTipoAcuerdo(),
                filtro.nombreContrato());
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ContratoFiltroInvalidoException(
                    "RANGO_FECHAS_CONTRATO_INVALIDO",
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }
}
