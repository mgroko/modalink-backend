package org.mgroko.backend.postulaciones.casting.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.postulaciones.casting.dto.AgendaCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.CandidatoCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.CaracteristicaPerfilCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.CaracteristicaRequeridaCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.CumplimientoCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.HabilidadCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.PolaroidCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.ProyectoCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.RequerimientoCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.TableroCastingResponse;
import org.mgroko.backend.postulaciones.casting.enums.OrdenCasting;
import org.mgroko.backend.postulaciones.casting.mapper.CastingMapper;
import org.mgroko.backend.postulaciones.dto.BuscarPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.PostulacionBusquedaResponse;
import org.mgroko.backend.postulaciones.dto.PostulacionesPaginaResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.servicio.BuscarPostulacionesProyectoService;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.reemplazos.servicio.EvaluadorConcordancia;
import org.mgroko.backend.reemplazos.servicio.modelo.CandidatoPerfil;
import org.mgroko.backend.reemplazos.servicio.modelo.CaracteristicaEvaluada;
import org.mgroko.backend.reemplazos.servicio.modelo.CaracteristicaRequerida;
import org.mgroko.backend.reemplazos.servicio.modelo.ResultadoConcordancia;
import org.mgroko.backend.reemplazos.servicio.modelo.ValorPerfil;
import org.mgroko.backend.repositorio.CaracteristicaPerfilRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PolaroidPerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arma el tablero de casting virtual: cabecera del requerimiento, candidatos
 * enriquecidos con características, polaroids y cumplimiento, orden global y
 * paginación.
 */
@Service
public class ObtenerTableroCastingService {

    private final ProyectoRepository proyectoRepository;
    private final BuscarPostulacionesProyectoService buscarPostulacionesProyectoService;
    private final RequerimientoCastingService requerimientoCastingService;
    private final EvaluadorAgendaCastingService evaluadorAgendaCastingService;
    private final PerfilRepository perfilRepository;
    private final CaracteristicaPerfilRepository caracteristicaPerfilRepository;
    private final PolaroidPerfilRepository polaroidPerfilRepository;
    private final ConfiguracionSistemaService configuracionSistemaService;

    public ObtenerTableroCastingService(
            ProyectoRepository proyectoRepository,
            BuscarPostulacionesProyectoService buscarPostulacionesProyectoService,
            RequerimientoCastingService requerimientoCastingService,
            EvaluadorAgendaCastingService evaluadorAgendaCastingService,
            PerfilRepository perfilRepository,
            CaracteristicaPerfilRepository caracteristicaPerfilRepository,
            PolaroidPerfilRepository polaroidPerfilRepository,
            ConfiguracionSistemaService configuracionSistemaService) {
        this.proyectoRepository = proyectoRepository;
        this.buscarPostulacionesProyectoService = buscarPostulacionesProyectoService;
        this.requerimientoCastingService = requerimientoCastingService;
        this.evaluadorAgendaCastingService = evaluadorAgendaCastingService;
        this.perfilRepository = perfilRepository;
        this.caracteristicaPerfilRepository = caracteristicaPerfilRepository;
        this.polaroidPerfilRepository = polaroidPerfilRepository;
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @Transactional(readOnly = true)
    public TableroCastingResponse obtener(
            Long idPerfilActivo,
            Long idProyecto,
            TipoPostulacion tipo,
            Long idRequerimiento,
            List<EstadoSolicitud> estados,
            OrdenCasting orden,
            int page,
            Long idPostulacionDestacada) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        List<EstadoSolicitud> estadosEfectivos = estados == null || estados.isEmpty()
                ? List.of(EstadoSolicitud.PENDIENTE)
                : List.copyOf(estados);

        BuscarPostulacionesFiltro filtro = new BuscarPostulacionesFiltro(
                estadosEfectivos, tipo, null, null, null, null, idRequerimiento);

        // La búsqueda valida proyecto, perfil activo, director y permiso de
        // gestión; también calcula cupo y puedeAceptar/puedeRechazar.
        PostulacionesPaginaResponse pagina = buscarPostulacionesProyectoService.buscar(
                idPerfilActivo, idProyecto, filtro, 0, 0, true);

        long pendientes = pagina.conteosPorEstado() != null
                ? pagina.conteosPorEstado().pendiente()
                : 0L;

        RequerimientoCastingResponse requerimiento = requerimientoCastingService.cargar(
                idProyecto, tipo, idRequerimiento, pendientes);

        List<PostulacionBusquedaResponse> postulaciones = pagina.contenido();
        List<Long> idsPerfil = postulaciones.stream()
                .map(PostulacionBusquedaResponse::idPerfil)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Perfil> perfiles = cargarPerfiles(idsPerfil);
        Map<Long, List<CaracteristicaPerfil>> caracteristicas = cargarCaracteristicas(idsPerfil);
        Map<Long, List<PolaroidPerfil>> polaroids = cargarPolaroids(idsPerfil);

        List<CandidatoCastingResponse> candidatos = new ArrayList<>();
        for (PostulacionBusquedaResponse postulacion : postulaciones) {
            Perfil perfil = perfiles.get(postulacion.idPerfil());
            candidatos.add(toCandidato(
                    postulacion,
                    perfil,
                    caracteristicas.getOrDefault(postulacion.idPerfil(), List.of()),
                    polaroids.getOrDefault(postulacion.idPerfil(), List.of()),
                    requerimiento));
        }

        candidatos.sort(comparador(orden));

        int tamanoPagina = Math.max(1, configuracionSistemaService
                .obtenerCandidatosPorPaginaCasting());
        int total = candidatos.size();
        int totalPaginas = total == 0
                ? 0
                : (int) Math.ceil((double) total / tamanoPagina);

        int paginaSolicitada = Math.max(0, page);
        if (idPostulacionDestacada != null) {
            int indice = indiceDe(candidatos, idPostulacionDestacada);
            if (indice >= 0) {
                paginaSolicitada = indice / tamanoPagina;
            }
        }

        if (totalPaginas > 0 && paginaSolicitada >= totalPaginas) {
            paginaSolicitada = totalPaginas - 1;
        }

        int desde = total == 0 ? 0 : Math.min(paginaSolicitada * tamanoPagina, total);
        int hasta = Math.min(desde + tamanoPagina, total);

        return new TableroCastingResponse(
                new ProyectoCastingResponse(
                        proyecto.getIdProyecto(),
                        proyecto.getNombre()),
                requerimiento,
                new ArrayList<>(candidatos.subList(desde, hasta)),
                paginaSolicitada,
                tamanoPagina,
                total,
                totalPaginas,
                paginaSolicitada == 0,
                totalPaginas == 0 || paginaSolicitada >= totalPaginas - 1,
                pagina.conteosPorEstado());
    }

    private Map<Long, Perfil> cargarPerfiles(List<Long> idsPerfil) {
        if (idsPerfil.isEmpty()) {
            return Map.of();
        }
        Map<Long, Perfil> perfiles = new HashMap<>();
        for (Perfil perfil : perfilRepository.findByIdPerfilInConDatosCasting(idsPerfil)) {
            perfiles.put(perfil.getIdPerfil(), perfil);
        }
        return perfiles;
    }

    private Map<Long, List<CaracteristicaPerfil>> cargarCaracteristicas(
            List<Long> idsPerfil) {
        if (idsPerfil.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<CaracteristicaPerfil>> resultado = new HashMap<>();
        for (CaracteristicaPerfil caracteristica
                : caracteristicaPerfilRepository.findByPerfil_IdPerfilIn(idsPerfil)) {
            resultado.computeIfAbsent(
                    caracteristica.getPerfil().getIdPerfil(),
                    id -> new ArrayList<>()).add(caracteristica);
        }
        return resultado;
    }

    private Map<Long, List<PolaroidPerfil>> cargarPolaroids(List<Long> idsPerfil) {
        if (idsPerfil.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PolaroidPerfil>> resultado = new HashMap<>();
        for (PolaroidPerfil polaroid
                : polaroidPerfilRepository
                        .findByPerfil_IdPerfilInOrderByPerfil_IdPerfilAscOrdenAsc(idsPerfil)) {
            resultado.computeIfAbsent(
                    polaroid.getPerfil().getIdPerfil(),
                    id -> new ArrayList<>()).add(polaroid);
        }
        return resultado;
    }

    private CandidatoCastingResponse toCandidato(
            PostulacionBusquedaResponse postulacion,
            Perfil perfil,
            List<CaracteristicaPerfil> caracteristicasPerfil,
            List<PolaroidPerfil> polaroidsPerfil,
            RequerimientoCastingResponse requerimiento) {

        List<CaracteristicaPerfilCastingResponse> caracteristicas = caracteristicasPerfil.stream()
                .sorted(Comparator.comparing(
                        caracteristica -> caracteristica.getCaracteristicaTecnica().getCodigo(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(CastingMapper::toCaracteristicaPerfil)
                .toList();

        List<PolaroidCastingResponse> polaroids = CastingMapper.toPolaroids(polaroidsPerfil);

        CumplimientoCastingResponse cumplimiento = evaluarCumplimiento(
                perfil, caracteristicasPerfil, requerimiento);

        var usuario = perfil != null ? perfil.getUsuario() : null;
        var ubicacion = usuario != null ? usuario.getUbicacion() : null;
        var ciudad = ubicacion != null ? ubicacion.getCiudad() : null;
        var provincia = ciudad != null ? ciudad.getProvincia() : null;
        var profesion = perfil != null ? perfil.getProfesion() : null;

        return new CandidatoCastingResponse(
                postulacion.idPostulacion(),
                postulacion.tipo(),
                postulacion.estado(),
                postulacion.fechaPostulacion(),
                postulacion.idPerfil(),
                postulacion.nombreArtistico(),
                postulacion.idProfesion(),
                profesion != null ? profesion.getCodigo() : null,
                postulacion.nombreProfesion(),
                perfil != null && perfil.getImagen() != null
                        ? perfil.getImagen().getUrl()
                        : null,
                ciudad != null ? ciudad.getNombre() : null,
                provincia != null ? provincia.getNombre() : null,
                postulacion.aportaCupo(),
                postulacion.puedeAceptar(),
                postulacion.puedeRechazar(),
                caracteristicas,
                polaroids,
                cumplimiento);
    }

    private CumplimientoCastingResponse evaluarCumplimiento(
            Perfil perfil,
            List<CaracteristicaPerfil> caracteristicasPerfil,
            RequerimientoCastingResponse requerimiento) {

        Map<Long, CaracteristicaPerfil> actuales = new HashMap<>();
        for (CaracteristicaPerfil caracteristica : caracteristicasPerfil) {
            actuales.put(
                    caracteristica.getCaracteristicaTecnica().getIdCaracteristica(),
                    caracteristica);
        }

        CandidatoPerfil candidato = toCandidatoPerfil(perfil, caracteristicasPerfil);
        ResultadoConcordancia resultado = EvaluadorConcordancia.evaluar(
                toRequisito(requerimiento), candidato);

        Map<Long, CaracteristicaRequeridaCastingResponse> requeridas = new HashMap<>();
        for (CaracteristicaRequeridaCastingResponse caracteristica
                : requerimiento.caracteristicas()) {
            requeridas.put(caracteristica.idCaracteristica(), caracteristica);
        }

        List<org.mgroko.backend.postulaciones.casting.dto.CaracteristicaEvaluadaCastingResponse>
                caracteristicasEvaluadas = new ArrayList<>();
        for (CaracteristicaEvaluada evaluada : resultado.caracteristicasEvaluadas()) {
            caracteristicasEvaluadas.add(CastingMapper.toCaracteristicaEvaluada(
                    evaluada,
                    actuales.get(evaluada.idCaracteristica()),
                    requeridas));
        }

        List<org.mgroko.backend.postulaciones.casting.dto.HabilidadEvaluadaCastingResponse>
                habilidadesEvaluadas = resultado.habilidadesEvaluadas().stream()
                        .map(CastingMapper::toHabilidadEvaluada)
                        .toList();

        AgendaCastingResponse agenda = requerimiento.tipo() == TipoPostulacion.ACTIVIDAD
                ? evaluadorAgendaCastingService.evaluar(requerimiento, perfil)
                : null;

        int evaluados = resultado.requisitosTotales();
        int cumplidos = resultado.requisitosCumplidos();
        List<String> faltantes = new ArrayList<>(resultado.faltantes());
        boolean cumpleTodos = resultado.cumpleTodo();

        if (agenda != null && agenda.evaluada()) {
            evaluados++;
            if (Boolean.TRUE.equals(agenda.cumple())) {
                cumplidos++;
            } else {
                cumpleTodos = false;
                faltantes.add("Agenda no disponible para la actividad");
            }
        }

        BigDecimal porcentaje = evaluados == 0
                ? null
                : BigDecimal.valueOf(cumplidos * 100.0 / evaluados)
                        .setScale(2, RoundingMode.HALF_UP);

        return new CumplimientoCastingResponse(
                cumpleTodos,
                cumplidos,
                evaluados,
                porcentaje,
                faltantes,
                caracteristicasEvaluadas,
                habilidadesEvaluadas,
                agenda);
    }

    private CandidatoPerfil toCandidatoPerfil(
            Perfil perfil,
            List<CaracteristicaPerfil> caracteristicasPerfil) {

        if (perfil == null) {
            return new CandidatoPerfil(
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, Set.of(), Map.of());
        }

        var usuario = perfil.getUsuario();
        var ubicacion = usuario != null ? usuario.getUbicacion() : null;
        var ciudad = ubicacion != null ? ubicacion.getCiudad() : null;
        var provincia = ciudad != null ? ciudad.getProvincia() : null;

        Set<Long> idsHabilidades = new HashSet<>();
        if (perfil.getHabilidades() != null) {
            perfil.getHabilidades().stream()
                    .filter(Objects::nonNull)
                    .map(Habilidad::getIdHabilidad)
                    .forEach(idsHabilidades::add);
        }

        Map<Long, ValorPerfil> caracteristicas = new HashMap<>();
        for (CaracteristicaPerfil caracteristica : caracteristicasPerfil) {
            caracteristicas.put(
                    caracteristica.getCaracteristicaTecnica().getIdCaracteristica(),
                    new ValorPerfil(caracteristica.getValor(), caracteristica.getIdValor()));
        }

        return new CandidatoPerfil(
                perfil.getIdPerfil(),
                usuario != null ? usuario.getIdUsuario() : null,
                perfil.getNombreArtistico(),
                usuario != null ? usuario.getNombre() : null,
                usuario != null ? usuario.getApellido() : null,
                perfil.getProfesion() != null ? perfil.getProfesion().getIdProfesion() : null,
                perfil.getProfesion() != null ? perfil.getProfesion().getNombre() : null,
                perfil.getImagen() != null ? perfil.getImagen().getUrl() : null,
                ciudad != null ? ciudad.getIdCiudad() : null,
                provincia != null ? provincia.getIdProvincia() : null,
                ciudad != null ? ciudad.getNombre() : null,
                provincia != null ? provincia.getNombre() : null,
                idsHabilidades,
                caracteristicas);
    }

    private org.mgroko.backend.reemplazos.servicio.modelo.RequisitoReemplazo toRequisito(
            RequerimientoCastingResponse requerimiento) {

        List<CaracteristicaRequerida> caracteristicas = new ArrayList<>();
        for (CaracteristicaRequeridaCastingResponse caracteristica
                : requerimiento.caracteristicas()) {
            Map<Long, String> etiquetas = new LinkedHashMap<>();
            Set<Long> idsValores = new HashSet<>();
            caracteristica.valores().forEach(valor -> {
                idsValores.add(valor.idValor());
                if (valor.etiqueta() != null && !valor.etiqueta().isBlank()) {
                    etiquetas.put(valor.idValor(), valor.etiqueta());
                }
            });
            caracteristicas.add(new CaracteristicaRequerida(
                    caracteristica.idCaracteristica(),
                    caracteristica.codigo(),
                    caracteristica.nombre(),
                    caracteristica.tipoDato(),
                    caracteristica.valorMin(),
                    caracteristica.valorMax(),
                    idsValores,
                    etiquetas));
        }

        Set<Long> idsHabilidades = new HashSet<>();
        Map<Long, String> nombresHabilidades = new LinkedHashMap<>();
        for (HabilidadCastingResponse habilidad : requerimiento.habilidades()) {
            idsHabilidades.add(habilidad.idHabilidad());
            nombresHabilidades.put(habilidad.idHabilidad(), habilidad.nombre());
        }

        return new org.mgroko.backend.reemplazos.servicio.modelo.RequisitoReemplazo(
                requerimiento.idProfesion(),
                requerimiento.nombreProfesion(),
                idsHabilidades,
                nombresHabilidades,
                caracteristicas);
    }

    private Comparator<CandidatoCastingResponse> comparador(OrdenCasting orden) {
        Comparator<CandidatoCastingResponse> porCumplimiento = Comparator
                .comparing((CandidatoCastingResponse candidato) ->
                        !candidato.cumplimiento().cumpleTodos())
                .thenComparing(
                        candidato -> candidato.cumplimiento().porcentaje(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(candidato ->
                        candidato.cumplimiento().faltantes().size())
                .thenComparing(
                        CandidatoCastingResponse::nombreArtistico,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(
                        CandidatoCastingResponse::idPostulacion,
                        Comparator.nullsLast(Long::compareTo));

        Comparator<CandidatoCastingResponse> porFecha = Comparator
                .comparing(
                        CandidatoCastingResponse::fechaPostulacion,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(porCumplimiento);

        Comparator<CandidatoCastingResponse> porNombre = Comparator
                .comparing(
                        CandidatoCastingResponse::nombreArtistico,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(
                        CandidatoCastingResponse::idPostulacion,
                        Comparator.nullsLast(Long::compareTo));

        return switch (orden != null ? orden : OrdenCasting.CUMPLIMIENTO) {
            case FECHA -> porFecha;
            case NOMBRE -> porNombre;
            case CUMPLIMIENTO -> porCumplimiento;
        };
    }

    private int indiceDe(
            List<CandidatoCastingResponse> candidatos,
            Long idPostulacion) {
        for (int i = 0; i < candidatos.size(); i++) {
            if (Objects.equals(candidatos.get(i).idPostulacion(), idPostulacion)) {
                return i;
            }
        }
        return -1;
    }
}
