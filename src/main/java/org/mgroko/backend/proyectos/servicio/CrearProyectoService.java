package org.mgroko.backend.proyectos.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.modelo.ValorCaracteristicaId;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.CaracteristicaNoEncontradaException;
import org.mgroko.backend.perfiles.exception.CaracteristicaProfesionNoCoincideException;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.exception.ValorCaracteristicaNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearObjetivoRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException;
import org.mgroko.backend.proyectos.exception.NombreProyectoDuplicadoException;
import org.mgroko.backend.proyectos.exception.RangoFechasProyectoInvalidoException;
import org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException;
import org.mgroko.backend.proyectos.exception.RolProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.mapper.ProyectoMapper;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.RolProyectoRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearProyectoService {

    public static final String ROL_DIRECTOR = "Director";

    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final RolProyectoRepository rolProyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ObjetivoRepository objetivoRepository;
    private final UbicacionService ubicacionService;
    private final ProfesionRepository profesionRepository;
    private final CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;
    private final ValorCaracteristicaRepository valorCaracteristicaRepository;
    private final HabilidadRepository habilidadRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;
    private final MoodboardRepository moodboardRepository;

    public CrearProyectoService(
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            RolProyectoRepository rolProyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PlanificacionRepository planificacionRepository,
            ObjetivoRepository objetivoRepository,
            UbicacionService ubicacionService,
            ProfesionRepository profesionRepository,
            CaracteristicaTecnicaRepository caracteristicaTecnicaRepository,
            ValorCaracteristicaRepository valorCaracteristicaRepository,
            HabilidadRepository habilidadRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository,
            MoodboardRepository moodboardRepository) {
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.rolProyectoRepository = rolProyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.ubicacionService = ubicacionService;
        this.profesionRepository = profesionRepository;
        this.caracteristicaTecnicaRepository = caracteristicaTecnicaRepository;
        this.valorCaracteristicaRepository = valorCaracteristicaRepository;
        this.habilidadRepository = habilidadRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoGralCaractValorRepository = requerimientoGralCaractValorRepository;
        this.moodboardRepository = moodboardRepository;
    }

    @Transactional
    public ProyectoResponse crear(Long idUsuario, Long idPerfilActivo, CrearProyectoRequest request) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        // 1. Validar que el perfil exista y pertenezca al usuario autenticado
        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        // 2. Validar que la fecha de entrega / fin no sea anterior a la fecha de inicio
        if (request.fechaFinEstipulada() != null && request.fechaFinEstipulada().isBefore(request.fechaInicio())) {
            throw new RangoFechasProyectoInvalidoException(
                    "La fecha de finalización o entrega no puede ser anterior a la fecha de inicio.");
        }

        // 3. Validar unicidad del nombre del proyecto para el perfil director
        boolean nombreExiste = proyectoRepository.existeProyectoConNombreParaPerfil(
                request.nombre(),
                idPerfilActivo,
                ROL_DIRECTOR,
                EstadoParticipacion.Activo
        );
        if (nombreExiste) {
            throw new NombreProyectoDuplicadoException(request.nombre());
        }

        // 4. Buscar rol Director
        RolProyecto rolDirector = rolProyectoRepository
                .findByNombre(LikePatrones.escapar(ROL_DIRECTOR), ROL_DIRECTOR)
                .orElseThrow(() -> new RolProyectoNoEncontradoException(ROL_DIRECTOR));

        // 5. Resolver ubicación opcional
        Ubicacion ubicacion = null;
        if (request.ubicacion() != null && request.ubicacion().localidadId() != null
                && !request.ubicacion().localidadId().isBlank()) {
            ubicacion = ubicacionService.obtenerOCrear(
                    request.ubicacion().localidadId(),
                    request.ubicacion().provinciaId()
            );
        }

        // 6. Crear entidad Proyecto en estado Borrador
        Proyecto proyecto = Proyecto.builder()
                .nombre(request.nombre().trim())
                .descripcion(request.descripcion().trim())
                .fechaInicio(request.fechaInicio())
                .estado(EstadoProyecto.Borrador)
                .privacidad(request.privacidad())
                .aceptaPostulacionGral(Boolean.TRUE.equals(request.aceptaPostulacionGral()))
                .ubicacion(ubicacion)
                .build();

        Proyecto proyectoGuardado = proyectoRepository.save(proyecto);

        // 7. Asociar al perfil activo como Director (MiembroProyecto)
        MiembroProyecto miembroDirector = MiembroProyecto.builder()
                .proyecto(proyectoGuardado)
                .perfil(perfil)
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();

        miembroProyectoRepository.save(miembroDirector);

        // 8. Inicializar la Planificación 1:1
        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyectoGuardado)
                .fechaEntrega(request.fechaFinEstipulada())
                .build();

        planificacionRepository.save(planificacion);

        // 9. Guardar objetivos si se recibieron
        List<Objetivo> objetivosGuardados = new ArrayList<>();
        if (request.objetivos() != null && !request.objetivos().isEmpty()) {
            for (CrearObjetivoRequest objReq : request.objetivos()) {
                Objetivo obj = Objetivo.builder()
                        .proyecto(proyectoGuardado)
                        .nombre(objReq.nombre().trim())
                        .descripcion(objReq.descripcion() != null ? objReq.descripcion().trim() : null)
                        .build();
                objetivosGuardados.add(objetivoRepository.save(obj));
            }
        }

        // 10. Guardar requerimientos de personal si se recibieron
        List<RequerimientoGralResponse> requerimientosResponse = new ArrayList<>();
        if (request.requerimientosGral() != null && !request.requerimientosGral().isEmpty()) {
            for (CrearRequerimientoGralRequest reqGral : request.requerimientosGral()) {
                requerimientosResponse.add(persistirRequerimientoGral(proyectoGuardado, reqGral));
            }
        }

        // 11. Guardar moodboard si se recibió
        MoodboardResponse moodboardResponse = null;
        if (request.moodboard() != null) {
            Moodboard moodboard = Moodboard.builder()
                    .proyecto(proyectoGuardado)
                    .descripcion(request.moodboard().descripcion() != null
                            ? request.moodboard().descripcion().trim()
                            : null)
                    .build();
            Moodboard guardado = moodboardRepository.save(moodboard);
            moodboardResponse = new MoodboardResponse(
                    guardado.getIdMoodboard(),
                    guardado.getDescripcion(),
                    guardado.getFechaCreacion());
        }

        return ProyectoMapper.toResponse(
                proyectoGuardado,
                perfil,
                request.fechaFinEstipulada(),
                objetivosGuardados,
                requerimientosResponse,
                moodboardResponse
        );
    }

    private RequerimientoGralResponse persistirRequerimientoGral(
            Proyecto proyecto, CrearRequerimientoGralRequest reqGral) {
        Profesion profesion = profesionRepository.findById(reqGral.idProfesion())
                .orElseThrow(() -> new ProfesionNoEncontradaException(
                        "Profesión no encontrada: " + reqGral.idProfesion()));

        RequerimientoGralProyecto requerimiento = RequerimientoGralProyecto.builder()
                .proyecto(proyecto)
                .profesion(profesion)
                .cantidad(reqGral.cantidad())
                .descripcion(reqGral.descripcion() != null ? reqGral.descripcion().trim() : null)
                .build();

        java.util.Map<Long, List<Long>> idsValoresPorCaracteristica = new java.util.HashMap<>();
        List<RequerimientoGralCaract> caracteristicas =
                construirCaracteristicas(requerimiento, reqGral, idsValoresPorCaracteristica);
        requerimiento.setCaracteristicas(caracteristicas);

        Set<Habilidad> habilidades = resolverHabilidades(reqGral.habilidades());
        requerimiento.setHabilidades(habilidades);

        RequerimientoGralProyecto guardado = requerimientoGralProyectoRepository.save(requerimiento);

        persistirValoresCaracteristicas(caracteristicas, idsValoresPorCaracteristica);

        List<CaracteristicaRequerimientoResponse> caractsResponse = new ArrayList<>();
        for (RequerimientoGralCaract caract : guardado.getCaracteristicas()) {
            Long idCaract = caract.getCaracteristica().getIdCaracteristica();
            caractsResponse.add(new CaracteristicaRequerimientoResponse(
                    idCaract,
                    caract.getCaracteristica().getCodigo(),
                    caract.getCaracteristica().getTipoDato(),
                    caract.getValorMin(),
                    caract.getValorMax(),
                    idsValoresPorCaracteristica.getOrDefault(idCaract, List.of())));
        }

        List<Long> idsHabilidades = guardado.getHabilidades().stream()
                .map(Habilidad::getIdHabilidad)
                .sorted()
                .toList();

        return new RequerimientoGralResponse(
                guardado.getIdRequerimientoGral(),
                guardado.getCantidad(),
                guardado.getDescripcion(),
                profesion.getIdProfesion(),
                profesion.getNombre(),
                caractsResponse,
                idsHabilidades);
    }

    private List<RequerimientoGralCaract> construirCaracteristicas(
            RequerimientoGralProyecto requerimiento,
            CrearRequerimientoGralRequest reqGral,
            java.util.Map<Long, List<Long>> idsValoresPorCaracteristica) {

        List<RequerimientoGralCaract> resultado = new ArrayList<>();
        if (reqGral.caracteristicas() == null || reqGral.caracteristicas().isEmpty()) {
            return resultado;
        }

        Set<Long> vistas = new HashSet<>();
        for (CrearRequerimientoCaractRequest carReq : reqGral.caracteristicas()) {
            if (!vistas.add(carReq.idCaracteristica())) {
                throw new RequerimientoInvalidoException(
                        "La característica técnica " + carReq.idCaracteristica() + " está duplicada en el requerimiento.");
            }

            CaracteristicaTecnica ct = caracteristicaTecnicaRepository.findById(carReq.idCaracteristica())
                    .orElseThrow(() -> new CaracteristicaNoEncontradaException(
                            "Característica técnica no encontrada: " + carReq.idCaracteristica()));

            if (!ct.getProfesion().getIdProfesion().equals(requerimiento.getProfesion().getIdProfesion())) {
                throw new CaracteristicaProfesionNoCoincideException(
                        "La característica técnica " + carReq.idCaracteristica()
                                + " no corresponde a la profesión del requerimiento.");
            }

            validarRangoYValores(ct, carReq);

            if (carReq.valores() != null && !carReq.valores().isEmpty()) {
                idsValoresPorCaracteristica.put(ct.getIdCaracteristica(), carReq.valores());
            }

            resultado.add(RequerimientoGralCaract.builder()
                    .requerimientoGral(requerimiento)
                    .caracteristica(ct)
                    .valorMin(carReq.valorMin())
                    .valorMax(carReq.valorMax())
                    .build());
        }

        return resultado;
    }

    private void validarRangoYValores(CaracteristicaTecnica ct, CrearRequerimientoCaractRequest carReq) {
        boolean tieneRango = carReq.valorMin() != null || carReq.valorMax() != null;
        boolean tieneValores = carReq.valores() != null && !carReq.valores().isEmpty();

        if (CaracteristicaTecnica.TIPO_NUMERICO.equals(ct.getTipoDato())) {
            if (carReq.valorMin() == null || carReq.valorMax() == null) {
                throw new RequerimientoInvalidoException(
                        "La característica numérica " + ct.getCodigo() + " exige valorMin y valorMax.");
            }
            if (carReq.valorMin().compareTo(BigDecimal.ZERO) < 0 || carReq.valorMax().compareTo(BigDecimal.ZERO) < 0) {
                throw new RequerimientoInvalidoException(
                        "La característica " + ct.getCodigo() + " no admite valores negativos.");
            }
            if (carReq.valorMin().compareTo(carReq.valorMax()) > 0) {
                throw new RequerimientoInvalidoException(
                        "La característica " + ct.getCodigo() + ": valorMin no puede ser mayor que valorMax.");
            }
            if (tieneValores) {
                throw new RequerimientoInvalidoException(
                        "La característica numérica " + ct.getCodigo() + " no admite valores enumerados.");
            }
        } else {
            if (tieneRango) {
                throw new RequerimientoInvalidoException(
                        "La característica " + ct.getCodigo() + " (" + ct.getTipoDato()
                                + ") no admite rango numérico (valorMin/valorMax).");
            }
            if (CaracteristicaTecnica.TIPO_ENUMERADO.equals(ct.getTipoDato())) {
                if (!tieneValores) {
                    throw new RequerimientoInvalidoException(
                            "La característica enumerada " + ct.getCodigo() + " exige al menos un valor.");
                }
                for (Long idValor : carReq.valores()) {
                    ValorCaracteristicaId valorId = new ValorCaracteristicaId(idValor, ct.getIdCaracteristica());
                    valorCaracteristicaRepository.findById(valorId)
                            .orElseThrow(() -> new ValorCaracteristicaNoEncontradoException(
                                    "El valor " + idValor + " no corresponde a la característica " + ct.getCodigo() + "."));
                }
            } else if (tieneValores) {
                throw new RequerimientoInvalidoException(
                        "La característica " + ct.getCodigo() + " (" + ct.getTipoDato()
                                + ") no admite valores enumerados.");
            }
        }
    }

    private void persistirValoresCaracteristicas(
            List<RequerimientoGralCaract> caracteristicas,
            java.util.Map<Long, List<Long>> idsValoresPorCaracteristica) {
        for (RequerimientoGralCaract caract : caracteristicas) {
            Long idCaracteristica = caract.getCaracteristica().getIdCaracteristica();
            List<Long> idsValores = idsValoresPorCaracteristica.get(idCaracteristica);
            if (idsValores == null || idsValores.isEmpty()) {
                continue;
            }
            for (Long idValor : idsValores) {
                requerimientoGralCaractValorRepository.save(RequerimientoGralCaractValor.builder()
                        .idValor(idValor)
                        .idCaracteristica(idCaracteristica)
                        .idReqGralCaract(caract.getIdReqGralCaract())
                        .build());
            }
        }
    }

    private Set<Habilidad> resolverHabilidades(List<Long> idsHabilidades) {
        Set<Habilidad> resultado = new HashSet<>();
        if (idsHabilidades == null || idsHabilidades.isEmpty()) {
            return resultado;
        }
        for (Long idHabilidad : idsHabilidades) {
            Habilidad habilidad = habilidadRepository.findById(idHabilidad)
                    .orElseThrow(() -> new HabilidadNoEncontradaException(idHabilidad));
            resultado.add(habilidad);
        }
        return resultado;
    }
}
