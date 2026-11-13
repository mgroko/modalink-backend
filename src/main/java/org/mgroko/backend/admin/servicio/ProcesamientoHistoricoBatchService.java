package org.mgroko.backend.admin.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.admin.exception.ProcesamientoHistoricoNoEncontradoException;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistorica;
import org.mgroko.backend.modelo.ProcesamientoColaboracionHistoricaError;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;
import org.mgroko.backend.publicaciones.servicio.DetectarColaboracionAutomaticaService;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.ProcesamientoColaboracionHistoricaErrorRepository;
import org.mgroko.backend.repositorio.ProcesamientoColaboracionHistoricaRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.storage.servicio.HuellaImagenService;
import org.mgroko.backend.storage.servicio.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procesa un lote de publicaciones históricas en transacciones cortas.
 */
@Service
public class ProcesamientoHistoricoBatchService {

    private static final Logger LOG =
            LoggerFactory.getLogger(ProcesamientoHistoricoBatchService.class);

    private final ProcesamientoColaboracionHistoricaRepository procesamientoRepository;
    private final ProcesamientoColaboracionHistoricaErrorRepository errorRepository;
    private final PublicacionRepository publicacionRepository;
    private final ImagenRepository imagenRepository;
    private final StorageService storageService;
    private final HuellaImagenService huellaImagenService;
    private final DetectarColaboracionAutomaticaService detectorService;
    private final Clock reloj;

    public ProcesamientoHistoricoBatchService(
            ProcesamientoColaboracionHistoricaRepository procesamientoRepository,
            ProcesamientoColaboracionHistoricaErrorRepository errorRepository,
            PublicacionRepository publicacionRepository,
            ImagenRepository imagenRepository,
            StorageService storageService,
            HuellaImagenService huellaImagenService,
            DetectarColaboracionAutomaticaService detectorService,
            Clock reloj) {
        this.procesamientoRepository = procesamientoRepository;
        this.errorRepository = errorRepository;
        this.publicacionRepository = publicacionRepository;
        this.imagenRepository = imagenRepository;
        this.storageService = storageService;
        this.huellaImagenService = huellaImagenService;
        this.detectorService = detectorService;
        this.reloj = reloj;
    }

    @Transactional
    public ProcesamientoColaboracionHistorica iniciar(Long idProcesamiento) {
        ProcesamientoColaboracionHistorica job = bloquear(idProcesamiento);

        if (job.getEstado() == EstadoProcesamientoHistorico.PENDIENTE) {
            job.setEstado(EstadoProcesamientoHistorico.EN_PROGRESO);
            if (job.getFechaInicio() == null) {
                job.setFechaInicio(LocalDateTime.now(reloj));
            }
            job = procesamientoRepository.saveAndFlush(job);
        }

        return job;
    }

    @Transactional(readOnly = true)
    public List<Long> siguienteLote(Long idProcesamiento) {
        ProcesamientoColaboracionHistorica job = procesamientoRepository
                .findById(idProcesamiento)
                .orElseThrow(ProcesamientoHistoricoNoEncontradoException::new);

        if (job.getEstado() != EstadoProcesamientoHistorico.EN_PROGRESO) {
            return List.of();
        }

        if (job.getLimitePublicaciones() != null
                && job.getPublicacionesProcesadas() >= job.getLimitePublicaciones()) {
            return List.of();
        }

        long ultimoId = job.getUltimoIdPublicacion() != null
                ? job.getUltimoIdPublicacion()
                : 0L;

        return publicacionRepository.buscarIdsPublicacionesHistoricas(
                job.getFiltroIdPublicacion(),
                job.getFiltroIdPerfil(),
                job.getFiltroFechaDesde(),
                job.getFiltroFechaHasta(),
                ultimoId,
                PageRequest.of(0, Math.max(1, job.getTamanoLote())));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesarPublicacion(Long idProcesamiento, Long idPublicacion) {
        ProcesamientoColaboracionHistorica job = bloquear(idProcesamiento);
        if (job.getEstado() != EstadoProcesamientoHistorico.EN_PROGRESO) {
            return;
        }

        Publicacion publicacion = publicacionRepository
                .findByIdPublicacionConImagenes(idPublicacion)
                .orElse(null);

        if (publicacion == null || !publicacionDisponible(publicacion)) {
            avanzar(job, idPublicacion);
            return;
        }

        List<Imagen> imagenes = publicacion.getImagenes() != null
                ? new ArrayList<>(publicacion.getImagenes())
                : List.of();

        int imagenesProcesadas = imagenes.size();
        int hashesCalculados = 0;
        int errores = 0;

        ModoProcesamientoHistorico modo = job.getModo() != null
                ? job.getModo()
                : ModoProcesamientoHistorico.AMBOS;

        if (modo != ModoProcesamientoHistorico.DETECCION) {
            for (Imagen imagen : imagenes) {
                boolean necesitaSha = imagen.getSha256Contenido() == null;
                boolean necesitaPhash = imagen.getPhash() == null;

                if (!necesitaSha && !necesitaPhash) {
                    continue;
                }

                byte[] bytes = storageService.leerArchivo(imagen.getNombreArchivo());
                if (bytes == null || bytes.length == 0) {
                    registrarErrorImagen(
                            job.getIdProcesamiento(), idPublicacion, imagen,
                            "No se pudo leer el archivo de almacenamiento.");
                    errores++;
                    continue;
                }

                boolean calculado = false;
                if (necesitaSha) {
                    imagen.setSha256Contenido(huellaImagenService.sha256(bytes));
                    calculado = true;
                }
                if (necesitaPhash) {
                    Long phash = huellaImagenService.phash(bytes);
                    if (phash == null) {
                        registrarErrorImagen(
                                job.getIdProcesamiento(), idPublicacion, imagen,
                                "No se pudo calcular el pHash de la imagen.");
                        errores++;
                        continue;
                    }
                    imagen.setPhash(phash);
                    imagen.setPhashVersion(HuellaImagenService.PHASH_VERSION);
                    calculado = true;
                }

                if (calculado) {
                    imagenRepository.save(imagen);
                    hashesCalculados++;
                }
            }
        }

        if (modo != ModoProcesamientoHistorico.HUELLAS) {
            List<Imagen> conPhash = imagenes.stream()
                    .filter(imagen -> imagen.getPhash() != null)
                    .toList();

            if (!conPhash.isEmpty()) {
                DetectarColaboracionAutomaticaService.DeteccionResultado resultado =
                        detectorService.detectar(publicacion, conPhash, job.isNotificar());
                job.setPropuestasCreadas(
                        job.getPropuestasCreadas() + resultado.creadas());
                job.setPropuestasOmitidas(
                        job.getPropuestasOmitidas() + resultado.omitidas());
            }
        }

        job.setPublicacionesProcesadas(job.getPublicacionesProcesadas() + 1);
        job.setImagenesProcesadas(job.getImagenesProcesadas() + imagenesProcesadas);
        job.setHashesCalculados(job.getHashesCalculados() + hashesCalculados);
        job.setErrores(job.getErrores() + errores);
        job.setUltimoIdPublicacion(idPublicacion);
        procesamientoRepository.saveAndFlush(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarError(
            Long idProcesamiento,
            Long idPublicacion,
            String mensaje) {

        ProcesamientoColaboracionHistorica job = procesamientoRepository
                .findByIdForUpdate(idProcesamiento)
                .orElse(null);
        if (job == null) {
            return;
        }

        job.setErrores(job.getErrores() + 1);
        job.setUltimoIdPublicacion(idPublicacion);
        procesamientoRepository.saveAndFlush(job);

        errorRepository.save(ProcesamientoColaboracionHistoricaError.builder()
                .idProcesamiento(idProcesamiento)
                .idPublicacion(idPublicacion)
                .mensaje(recortar(mensaje, 1000))
                .fechaRegistro(LocalDateTime.now(reloj))
                .build());
    }

    @Transactional(readOnly = true)
    public boolean estaCancelado(Long idProcesamiento) {
        return procesamientoRepository.findById(idProcesamiento)
                .map(job -> job.getEstado() == EstadoProcesamientoHistorico.CANCELADO)
                .orElse(true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizar(Long idProcesamiento) {
        ProcesamientoColaboracionHistorica job = procesamientoRepository
                .findByIdForUpdate(idProcesamiento)
                .orElse(null);
        if (job == null
                || job.getEstado() == EstadoProcesamientoHistorico.CANCELADO) {
            return;
        }

        job.setEstado(job.getErrores() > 0
                ? EstadoProcesamientoHistorico.COMPLETADO_CON_ERRORES
                : EstadoProcesamientoHistorico.COMPLETADO);
        job.setFechaFin(LocalDateTime.now(reloj));
        procesamientoRepository.saveAndFlush(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fallar(Long idProcesamiento, String mensaje) {
        ProcesamientoColaboracionHistorica job = procesamientoRepository
                .findByIdForUpdate(idProcesamiento)
                .orElse(null);
        if (job == null) {
            return;
        }

        job.setEstado(EstadoProcesamientoHistorico.FALLIDO);
        job.setFechaFin(LocalDateTime.now(reloj));
        job.setMensajeError(recortar(mensaje, 1000));
        procesamientoRepository.saveAndFlush(job);
    }

    private ProcesamientoColaboracionHistorica bloquear(Long idProcesamiento) {
        return procesamientoRepository.findByIdForUpdate(idProcesamiento)
                .orElseThrow(ProcesamientoHistoricoNoEncontradoException::new);
    }

    private boolean publicacionDisponible(Publicacion publicacion) {
        return publicacion.getEstado() == org.mgroko.backend.modelo.enums.EstadoPublicacion.ACTIVA
                && publicacion.getPerfil() != null
                && publicacion.getPerfil().getEstado()
                        == org.mgroko.backend.modelo.enums.EstadoPerfil.ACTIVO
                && publicacion.getPerfil().getUsuario() != null
                && publicacion.getPerfil().getUsuario().getEstado()
                        == org.mgroko.backend.modelo.enums.EstadoUsuario.ACTIVO;
    }

    private void avanzar(
            ProcesamientoColaboracionHistorica job,
            Long idPublicacion) {

        job.setPublicacionesProcesadas(job.getPublicacionesProcesadas() + 1);
        job.setUltimoIdPublicacion(idPublicacion);
        procesamientoRepository.saveAndFlush(job);
    }

    private void registrarErrorImagen(
            Long idProcesamiento,
            Long idPublicacion,
            Imagen imagen,
            String mensaje) {

        errorRepository.save(ProcesamientoColaboracionHistoricaError.builder()
                .idProcesamiento(idProcesamiento)
                .idPublicacion(idPublicacion)
                .idImagen(imagen.getIdImagen())
                .nombreArchivo(imagen.getNombreArchivo())
                .mensaje(recortar(mensaje, 1000))
                .fechaRegistro(LocalDateTime.now(reloj))
                .build());
        LOG.warn("Error al procesar la imagen {} de la publicación {}: {}",
                imagen.getIdImagen(), idPublicacion, mensaje);
    }

    private String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
