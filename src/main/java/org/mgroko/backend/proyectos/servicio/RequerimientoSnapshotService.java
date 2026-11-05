package org.mgroko.backend.proyectos.servicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActCaractValor;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea y reutiliza snapshots inmutables de requerimientos al dar de baja o
 * eliminar un miembro.
 *
 * <p>El requerimiento activo no se modifica. Sólo los vínculos históricos del
 * miembro se repuntan a la copia archivada.</p>
 */
@Service
public class RequerimientoSnapshotService {

    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;
    private final RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final InvitacionGralRepository invitacionGralRepository;

    public RequerimientoSnapshotService(
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoActCaractValorRepository requerimientoActCaractValorRepository,
            RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            PostulacionGralRepository postulacionGralRepository,
            InvitacionGralRepository invitacionGralRepository) {
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoActCaractValorRepository = requerimientoActCaractValorRepository;
        this.requerimientoGralCaractValorRepository = requerimientoGralCaractValorRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.invitacionGralRepository = invitacionGralRepository;
    }

    /**
     * Prepara el historial de una baja/eliminación dentro de la transacción
     * del servicio que la origina.
     */
    @Transactional
    public void prepararHistorialBaja(
            MiembroProyecto miembro,
            Long idProyecto,
            LocalDateTime fechaBaja) {

        if (miembro == null || miembro.getIdMiembro() == null || miembro.getPerfil() == null) {
            return;
        }

        // El snapshot no debe repuntar vínculos de miembros que siguen
        // activos. Los servicios de baja marcan el estado antes de invocar
        // este método.
        if (miembro.getEstadoParticipacion() == EstadoParticipacion.ACTIVO) {
            return;
        }

        Long idPerfil = miembro.getPerfil().getIdPerfil();
        rechazarPendientes(idProyecto, idPerfil);

        RequerimientoGralProyecto requerimientoGral = miembro.getRequerimientoGral();
        if (requerimientoGral != null && requerimientoGral.getFechaArchivado() == null) {
            RequerimientoGralProyecto snapshot =
                    snapshotGeneral(requerimientoGral, idPerfil, fechaBaja);
            miembro.setRequerimientoGral(snapshot);
            repuntarPostulacionesGrales(idPerfil, requerimientoGral, snapshot);
            repuntarInvitacionesGrales(idPerfil, requerimientoGral, snapshot);
        }

        List<AsignacionActividad> asignaciones = asignacionActividadRepository
                .findByMiembroConActividadYRequerimiento(miembro.getIdMiembro())
                .stream()
                .filter(asignacion -> asignacion.getMiembro() != null
                        && asignacion.getMiembro().getEstadoParticipacion()
                                != EstadoParticipacion.ACTIVO)
                .toList();

        for (AsignacionActividad asignacion : asignaciones) {
            RequerimientoActividad requerimiento = asignacion.getRequerimientoActividad();
            if (requerimiento == null || requerimiento.getFechaArchivado() != null) {
                continue;
            }

            // Las actividades finalizadas/canceladas son inmutables: no hace
            // falta versionar su requerimiento y el cupo histórico las sigue
            // contando desde el activo.
            EstadoActividad estadoActividad = requerimiento.getActividad() != null
                    ? requerimiento.getActividad().getEstado()
                    : null;
            if (estadoActividad == EstadoActividad.FINALIZADA
                    || estadoActividad == EstadoActividad.CANCELADA) {
                continue;
            }

            RequerimientoActividad snapshot =
                    snapshotActividad(requerimiento, idPerfil, fechaBaja);
            asignacion.setRequerimientoActividad(snapshot);
            repuntarPostulacionesActividad(idPerfil, requerimiento, snapshot);
            repuntarInvitacionesActividad(idPerfil, requerimiento, snapshot);
        }

        asignacionActividadRepository.saveAll(asignaciones);
    }

    private void rechazarPendientes(Long idProyecto, Long idPerfil) {
        List<PostulacionActividad> postulacionesActividad =
                postulacionActividadRepository.findPendientesDeProyectoYPerfil(
                        idProyecto, idPerfil);
        postulacionesActividad.forEach(p -> p.setEstado(EstadoSolicitud.RECHAZADA));
        postulacionActividadRepository.saveAll(postulacionesActividad);

        List<InvitacionActividad> invitacionesActividad =
                invitacionActividadRepository.findPendientesDeProyectoYDestinatario(
                        idProyecto, idPerfil);
        invitacionesActividad.forEach(i -> i.setEstado(EstadoSolicitud.RECHAZADA));
        invitacionActividadRepository.saveAll(invitacionesActividad);

        List<PostulacionGral> postulacionesGral =
                postulacionGralRepository.findPendientesDeProyectoYPerfil(
                        idProyecto, idPerfil);
        postulacionesGral.forEach(p -> p.setEstado(EstadoSolicitud.RECHAZADA));
        postulacionGralRepository.saveAll(postulacionesGral);

        List<InvitacionGral> invitacionesGral =
                invitacionGralRepository.findPendientesDeProyectoYDestinatario(
                        idProyecto, idPerfil);
        invitacionesGral.forEach(i -> i.setEstado(EstadoSolicitud.RECHAZADA));
        invitacionGralRepository.saveAll(invitacionesGral);
    }

    private RequerimientoGralProyecto snapshotGeneral(
            RequerimientoGralProyecto activo,
            Long idPerfil,
            LocalDateTime fechaBaja) {

        RequerimientoGralProyecto reutilizable = requerimientoGralProyectoRepository
                .findFirstByIdRequerimientoOrigenOrderByFechaArchivadoDesc(
                        activo.getIdRequerimientoGral())
                .filter(candidato -> mismosDatos(activo, candidato))
                .filter(candidato -> !postulacionGralRepository
                        .existsByRequerimientoGral_IdRequerimientoGralAndPerfil_IdPerfil(
                                candidato.getIdRequerimientoGral(), idPerfil))
                .orElse(null);

        if (reutilizable != null) {
            return reutilizable;
        }

        return crearSnapshotGeneral(activo, fechaBaja);
    }

    private RequerimientoGralProyecto crearSnapshotGeneral(
            RequerimientoGralProyecto activo,
            LocalDateTime fechaBaja) {

        RequerimientoGralProyecto snapshot = RequerimientoGralProyecto.builder()
                .cantidad(activo.getCantidad())
                .descripcion(activo.getDescripcion())
                .proyecto(activo.getProyecto())
                .profesion(activo.getProfesion())
                .habilidades(activo.getHabilidades() != null
                        ? new HashSet<>(activo.getHabilidades())
                        : new HashSet<>())
                .fechaArchivado(fechaBaja)
                .idRequerimientoOrigen(activo.getIdRequerimientoGral())
                .build();

        Map<Long, List<Long>> valores = valoresGrales(activo.getCaracteristicas());
        Map<Long, RequerimientoGralCaract> copias = new LinkedHashMap<>();
        List<RequerimientoGralCaract> caracteristicas = new ArrayList<>();
        for (RequerimientoGralCaract original : activo.getCaracteristicas()) {
            RequerimientoGralCaract copia = RequerimientoGralCaract.builder()
                    .requerimientoGral(snapshot)
                    .caracteristica(original.getCaracteristica())
                    .valorMin(original.getValorMin())
                    .valorMax(original.getValorMax())
                    .build();
            caracteristicas.add(copia);
            copias.put(original.getCaracteristica().getIdCaracteristica(), copia);
        }
        snapshot.setCaracteristicas(caracteristicas);
        snapshot = requerimientoGralProyectoRepository.saveAndFlush(snapshot);

        List<RequerimientoGralCaractValor> valoresNuevos = new ArrayList<>();
        for (RequerimientoGralCaractCaractEntry entry : entries(copias, valores)) {
            valoresNuevos.add(RequerimientoGralCaractValor.builder()
                    .idReqGralCaract(entry.copia().getIdReqGralCaract())
                    .idCaracteristica(entry.idCaracteristica())
                    .idValor(entry.idValor())
                    .build());
        }
        if (!valoresNuevos.isEmpty()) {
            requerimientoGralCaractValorRepository.saveAll(valoresNuevos);
        }

        return snapshot;
    }

    private RequerimientoActividad snapshotActividad(
            RequerimientoActividad activo,
            Long idPerfil,
            LocalDateTime fechaBaja) {

        RequerimientoActividad reutilizable = requerimientoActividadRepository
                .findFirstByIdRequerimientoOrigenOrderByFechaArchivadoDesc(
                        activo.getIdRequerimiento())
                .filter(candidato -> mismosDatos(activo, candidato))
                .filter(candidato -> !postulacionActividadRepository
                        .existsByRequerimiento_IdRequerimientoAndPerfil_IdPerfil(
                                candidato.getIdRequerimiento(), idPerfil))
                .orElse(null);

        if (reutilizable != null) {
            return reutilizable;
        }

        return crearSnapshotActividad(activo, fechaBaja);
    }

    private RequerimientoActividad crearSnapshotActividad(
            RequerimientoActividad activo,
            LocalDateTime fechaBaja) {

        RequerimientoActividad snapshot = RequerimientoActividad.builder()
                .cantidad(activo.getCantidad())
                .descripcion(activo.getDescripcion())
                .profesion(activo.getProfesion())
                .actividad(activo.getActividad())
                .habilidades(activo.getHabilidades() != null
                        ? new HashSet<>(activo.getHabilidades())
                        : new HashSet<>())
                .fechaArchivado(fechaBaja)
                .idRequerimientoOrigen(activo.getIdRequerimiento())
                .build();

        Map<Long, List<Long>> valores = valoresActividad(activo.getCaracteristicas());
        Map<Long, RequerimientoActCaract> copias = new LinkedHashMap<>();
        List<RequerimientoActCaract> caracteristicas = new ArrayList<>();
        for (RequerimientoActCaract original : activo.getCaracteristicas()) {
            RequerimientoActCaract copia = RequerimientoActCaract.builder()
                    .requerimientoActividad(snapshot)
                    .caracteristica(original.getCaracteristica())
                    .valorMin(original.getValorMin())
                    .valorMax(original.getValorMax())
                    .build();
            caracteristicas.add(copia);
            copias.put(original.getCaracteristica().getIdCaracteristica(), copia);
        }
        snapshot.setCaracteristicas(caracteristicas);
        snapshot = requerimientoActividadRepository.saveAndFlush(snapshot);

        List<RequerimientoActCaractValor> valoresNuevos = new ArrayList<>();
        for (RequerimientoActCaractEntry entry : entriesActividad(copias, valores)) {
            valoresNuevos.add(RequerimientoActCaractValor.builder()
                    .idReqActCaract(entry.copia().getIdReqActCaract())
                    .idCaracteristica(entry.idCaracteristica())
                    .idValor(entry.idValor())
                    .build());
        }
        if (!valoresNuevos.isEmpty()) {
            requerimientoActCaractValorRepository.saveAll(valoresNuevos);
        }

        return snapshot;
    }

    /**
     * Antes de editar o eliminar un requerimiento activo con historial legacy,
     * mueve todos los vínculos históricos a un snapshot nuevo.
     */
    @Transactional
    public void asegurarHistorialGeneral(
            RequerimientoGralProyecto activo,
            LocalDateTime fecha) {

        if (activo == null || activo.getIdRequerimientoGral() == null) {
            return;
        }
        boolean tieneHistorial = miembroProyectoRepository
                .existsByRequerimientoGral_IdRequerimientoGral(
                        activo.getIdRequerimientoGral())
                || postulacionGralRepository
                        .existsByRequerimientoGral_IdRequerimientoGral(
                                activo.getIdRequerimientoGral())
                || invitacionGralRepository
                        .existsByRequerimientoGral_IdRequerimientoGral(
                                activo.getIdRequerimientoGral());
        if (!tieneHistorial) {
            return;
        }

        RequerimientoGralProyecto snapshot = crearSnapshotGeneral(activo, fecha);

        List<MiembroProyecto> miembros = miembroProyectoRepository
                .findByRequerimientoGral_IdRequerimientoGral(
                        activo.getIdRequerimientoGral())
                .stream()
                .filter(m -> m.getEstadoParticipacion()
                        != EstadoParticipacion.ACTIVO)
                .toList();
        miembros.forEach(m -> m.setRequerimientoGral(snapshot));
        miembroProyectoRepository.saveAll(miembros);

        List<PostulacionGral> postulaciones = postulacionGralRepository
                .findHistoricasPorRequerimientoGral(
                        activo.getIdRequerimientoGral());
        postulaciones.forEach(p -> p.setRequerimientoGral(snapshot));
        postulacionGralRepository.saveAll(postulaciones);

        List<InvitacionGral> invitaciones = invitacionGralRepository
                .findHistoricasPorRequerimientoGral(
                        activo.getIdRequerimientoGral());
        invitaciones.forEach(i -> i.setRequerimientoGral(snapshot));
        invitacionGralRepository.saveAll(invitaciones);
    }

    @Transactional
    public void asegurarHistorialActividad(
            RequerimientoActividad activo,
            LocalDateTime fecha) {

        if (activo == null || activo.getIdRequerimiento() == null) {
            return;
        }
        boolean tieneHistorial = asignacionActividadRepository
                .existsByRequerimientoActividad_IdRequerimiento(
                        activo.getIdRequerimiento())
                || postulacionActividadRepository
                        .existsByRequerimiento_IdRequerimiento(
                                activo.getIdRequerimiento())
                || invitacionActividadRepository
                        .existsByRequerimiento_IdRequerimiento(
                                activo.getIdRequerimiento());
        if (!tieneHistorial) {
            return;
        }

        RequerimientoActividad snapshot = crearSnapshotActividad(activo, fecha);

        List<AsignacionActividad> asignaciones = asignacionActividadRepository
                .findByRequerimientoActividad_IdRequerimiento(
                        activo.getIdRequerimiento())
                .stream()
                .filter(a -> a.getMiembro() != null
                        && a.getMiembro().getEstadoParticipacion()
                                != EstadoParticipacion.ACTIVO)
                .toList();
        asignaciones.forEach(a -> a.setRequerimientoActividad(snapshot));
        asignacionActividadRepository.saveAll(asignaciones);

        List<PostulacionActividad> postulaciones = postulacionActividadRepository
                .findHistoricasPorRequerimiento(activo.getIdRequerimiento());
        postulaciones.forEach(p -> p.setRequerimiento(snapshot));
        postulacionActividadRepository.saveAll(postulaciones);

        List<InvitacionActividad> invitaciones = invitacionActividadRepository
                .findHistoricasPorRequerimiento(activo.getIdRequerimiento());
        invitaciones.forEach(i -> i.setRequerimiento(snapshot));
        invitacionActividadRepository.saveAll(invitaciones);
    }

    private void repuntarPostulacionesGrales(
            Long idPerfil,
            RequerimientoGralProyecto activo,
            RequerimientoGralProyecto snapshot) {

        List<PostulacionGral> postulaciones =
                postulacionGralRepository.findHistoricasPorPerfilYRequerimientos(
                        idPerfil, List.of(activo.getIdRequerimientoGral()));
        postulaciones.forEach(p -> p.setRequerimientoGral(snapshot));
        postulacionGralRepository.saveAll(postulaciones);
    }

    private void repuntarInvitacionesGrales(
            Long idPerfil,
            RequerimientoGralProyecto activo,
            RequerimientoGralProyecto snapshot) {

        List<InvitacionGral> invitaciones =
                invitacionGralRepository.findHistoricasPorDestinatarioYRequerimientos(
                        idPerfil, List.of(activo.getIdRequerimientoGral()));
        invitaciones.forEach(i -> i.setRequerimientoGral(snapshot));
        invitacionGralRepository.saveAll(invitaciones);
    }

    private void repuntarPostulacionesActividad(
            Long idPerfil,
            RequerimientoActividad activo,
            RequerimientoActividad snapshot) {

        List<PostulacionActividad> postulaciones =
                postulacionActividadRepository.findHistoricasPorPerfilYRequerimientos(
                        idPerfil, List.of(activo.getIdRequerimiento()));
        postulaciones.forEach(p -> p.setRequerimiento(snapshot));
        postulacionActividadRepository.saveAll(postulaciones);
    }

    private void repuntarInvitacionesActividad(
            Long idPerfil,
            RequerimientoActividad activo,
            RequerimientoActividad snapshot) {

        List<InvitacionActividad> invitaciones =
                invitacionActividadRepository.findHistoricasPorDestinatarioYRequerimientos(
                        idPerfil, List.of(activo.getIdRequerimiento()));
        invitaciones.forEach(i -> i.setRequerimiento(snapshot));
        invitacionActividadRepository.saveAll(invitaciones);
    }

    private boolean mismosDatos(
            RequerimientoGralProyecto activo,
            RequerimientoGralProyecto snapshot) {

        return Objects.equals(activo.getCantidad(), snapshot.getCantidad())
                && mismoId(activo.getProfesion(), snapshot.getProfesion())
                && Objects.equals(normalizar(activo.getDescripcion()),
                        normalizar(snapshot.getDescripcion()))
                && mismosValores(
                        activo.getCaracteristicas(),
                        snapshot.getCaracteristicas(),
                        this::valoresGrales)
                && mismosIdsHabilidad(activo.getHabilidades(), snapshot.getHabilidades());
    }

    private boolean mismosDatos(
            RequerimientoActividad activo,
            RequerimientoActividad snapshot) {

        return Objects.equals(activo.getCantidad(), snapshot.getCantidad())
                && mismoId(activo.getProfesion(), snapshot.getProfesion())
                && Objects.equals(normalizar(activo.getDescripcion()),
                        normalizar(snapshot.getDescripcion()))
                && mismosValoresActividad(
                        activo.getCaracteristicas(),
                        snapshot.getCaracteristicas())
                && mismosIdsHabilidad(activo.getHabilidades(), snapshot.getHabilidades());
    }

    private boolean mismosValores(
            List<RequerimientoGralCaract> activas,
            List<RequerimientoGralCaract> snapshots,
            java.util.function.Function<List<RequerimientoGralCaract>, Map<Long, List<Long>>> cargador) {

        if (activas.size() != snapshots.size()) {
            return false;
        }
        Map<Long, List<Long>> valoresActivos = cargador.apply(activas);
        Map<Long, List<Long>> valoresSnapshot = cargador.apply(snapshots);
        for (RequerimientoGralCaract activa : activas) {
            Long idCaracteristica = activa.getCaracteristica().getIdCaracteristica();
            RequerimientoGralCaract snapshot = snapshots.stream()
                    .filter(c -> idCaracteristica.equals(
                            c.getCaracteristica().getIdCaracteristica()))
                    .findFirst()
                    .orElse(null);
            if (snapshot == null
                    || !mismoDecimal(activa.getValorMin(), snapshot.getValorMin())
                    || !mismoDecimal(activa.getValorMax(), snapshot.getValorMax())
                    || !new HashSet<>(valoresActivos.getOrDefault(idCaracteristica, List.of()))
                            .equals(new HashSet<>(valoresSnapshot.getOrDefault(
                                    idCaracteristica, List.of())))) {
                return false;
            }
        }
        return true;
    }

    private boolean mismosValoresActividad(
            List<RequerimientoActCaract> activas,
            List<RequerimientoActCaract> snapshots) {

        if (activas.size() != snapshots.size()) {
            return false;
        }
        Map<Long, List<Long>> valoresActivos = valoresActividad(activas);
        Map<Long, List<Long>> valoresSnapshot = valoresActividad(snapshots);
        for (RequerimientoActCaract activa : activas) {
            Long idCaracteristica = activa.getCaracteristica().getIdCaracteristica();
            RequerimientoActCaract snapshot = snapshots.stream()
                    .filter(c -> idCaracteristica.equals(
                            c.getCaracteristica().getIdCaracteristica()))
                    .findFirst()
                    .orElse(null);
            if (snapshot == null
                    || !mismoDecimal(activa.getValorMin(), snapshot.getValorMin())
                    || !mismoDecimal(activa.getValorMax(), snapshot.getValorMax())
                    || !new HashSet<>(valoresActivos.getOrDefault(idCaracteristica, List.of()))
                            .equals(new HashSet<>(valoresSnapshot.getOrDefault(
                                    idCaracteristica, List.of())))) {
                return false;
            }
        }
        return true;
    }

    private Map<Long, List<Long>> valoresGrales(List<RequerimientoGralCaract> caracteristicas) {
        Map<Long, Long> filaACaracteristica = new HashMap<>();
        List<Long> idsFila = new ArrayList<>();
        for (RequerimientoGralCaract caracteristica : caracteristicas) {
            if (caracteristica.getIdReqGralCaract() != null
                    && caracteristica.getCaracteristica() != null) {
                idsFila.add(caracteristica.getIdReqGralCaract());
                filaACaracteristica.put(
                        caracteristica.getIdReqGralCaract(),
                        caracteristica.getCaracteristica().getIdCaracteristica());
            }
        }
        Map<Long, List<Long>> resultado = new HashMap<>();
        if (idsFila.isEmpty()) {
            return resultado;
        }
        for (RequerimientoGralCaractValor valor
                : requerimientoGralCaractValorRepository.findByIdReqGralCaractIn(idsFila)) {
            Long idCaracteristica = filaACaracteristica.get(valor.getIdReqGralCaract());
            if (idCaracteristica != null) {
                resultado.computeIfAbsent(idCaracteristica, id -> new ArrayList<>())
                        .add(valor.getIdValor());
            }
        }
        return resultado;
    }

    private Map<Long, List<Long>> valoresActividad(List<RequerimientoActCaract> caracteristicas) {
        Map<Long, Long> filaACaracteristica = new HashMap<>();
        List<Long> idsFila = new ArrayList<>();
        for (RequerimientoActCaract caracteristica : caracteristicas) {
            if (caracteristica.getIdReqActCaract() != null
                    && caracteristica.getCaracteristica() != null) {
                idsFila.add(caracteristica.getIdReqActCaract());
                filaACaracteristica.put(
                        caracteristica.getIdReqActCaract(),
                        caracteristica.getCaracteristica().getIdCaracteristica());
            }
        }
        Map<Long, List<Long>> resultado = new HashMap<>();
        if (idsFila.isEmpty()) {
            return resultado;
        }
        for (RequerimientoActCaractValor valor
                : requerimientoActCaractValorRepository.findByIdReqActCaractIn(idsFila)) {
            Long idCaracteristica = filaACaracteristica.get(valor.getIdReqActCaract());
            if (idCaracteristica != null) {
                resultado.computeIfAbsent(idCaracteristica, id -> new ArrayList<>())
                        .add(valor.getIdValor());
            }
        }
        return resultado;
    }

    private List<RequerimientoGralCaractCaractEntry> entries(
            Map<Long, RequerimientoGralCaract> copias,
            Map<Long, List<Long>> valores) {

        List<RequerimientoGralCaractCaractEntry> resultado = new ArrayList<>();
        copias.forEach((idCaracteristica, copia) -> {
            for (Long idValor : valores.getOrDefault(idCaracteristica, List.of())) {
                resultado.add(new RequerimientoGralCaractCaractEntry(
                        copia, idCaracteristica, idValor));
            }
        });
        return resultado;
    }

    private List<RequerimientoActCaractEntry> entriesActividad(
            Map<Long, RequerimientoActCaract> copias,
            Map<Long, List<Long>> valores) {

        List<RequerimientoActCaractEntry> resultado = new ArrayList<>();
        copias.forEach((idCaracteristica, copia) -> {
            for (Long idValor : valores.getOrDefault(idCaracteristica, List.of())) {
                resultado.add(new RequerimientoActCaractEntry(
                        copia, idCaracteristica, idValor));
            }
        });
        return resultado;
    }

    private boolean mismoId(Object activo, Object snapshot) {
        if (activo == null || snapshot == null) {
            return activo == snapshot;
        }
        if (activo instanceof org.mgroko.backend.modelo.Profesion p
                && snapshot instanceof org.mgroko.backend.modelo.Profesion s) {
            return Objects.equals(p.getIdProfesion(), s.getIdProfesion());
        }
        return false;
    }

    private boolean mismosIdsHabilidad(Set<Habilidad> activas, Set<Habilidad> snapshots) {
        Set<Long> idsActivos = new HashSet<>();
        if (activas != null) {
            activas.stream().map(Habilidad::getIdHabilidad).forEach(idsActivos::add);
        }
        Set<Long> idsSnapshot = new HashSet<>();
        if (snapshots != null) {
            snapshots.stream().map(Habilidad::getIdHabilidad).forEach(idsSnapshot::add);
        }
        return idsActivos.equals(idsSnapshot);
    }

    private boolean mismoDecimal(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String normalizado = texto.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    private record RequerimientoGralCaractCaractEntry(
            RequerimientoGralCaract copia,
            Long idCaracteristica,
            Long idValor) {
    }

    private record RequerimientoActCaractEntry(
            RequerimientoActCaract copia,
            Long idCaracteristica,
            Long idValor) {
    }
}
