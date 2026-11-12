package org.mgroko.backend.admin.servicio;

import java.time.LocalDateTime;

import org.mgroko.backend.admin.dto.ConfiguracionSchedulerLimpiezaImagenesResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerLimpiezaImagenesRequest;
import org.mgroko.backend.admin.dto.EjecutarLimpiezaImagenesResponse;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.storage.servicio.LimpiezaImagenesScheduler;
import org.mgroko.backend.storage.servicio.LimpiezaImagenesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración del job de limpieza de imágenes: consulta y modificación del
 * intervalo en horas, y ejecución manual.
 */
@Service
public class AdminLimpiezaImagenesService {

    private final ConfiguracionSistemaService configuracionSistemaService;
    private final LimpiezaImagenesService limpiezaImagenesService;
    private final LimpiezaImagenesScheduler limpiezaImagenesScheduler;
    private final AdminLimpiezaImagenesAuditoriaService auditoriaService;

    public AdminLimpiezaImagenesService(
            ConfiguracionSistemaService configuracionSistemaService,
            LimpiezaImagenesService limpiezaImagenesService,
            LimpiezaImagenesScheduler limpiezaImagenesScheduler,
            AdminLimpiezaImagenesAuditoriaService auditoriaService) {
        this.configuracionSistemaService = configuracionSistemaService;
        this.limpiezaImagenesService = limpiezaImagenesService;
        this.limpiezaImagenesScheduler = limpiezaImagenesScheduler;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public ConfiguracionSchedulerLimpiezaImagenesResponse obtenerConfiguracion() {
        return construirConfiguracion(
                configuracionSistemaService.obtenerIntervaloLimpiezaImagenesHoras(),
                configuracionSistemaService.obtenerGraciaLimpiezaImagenesMinutos());
    }

    @Transactional
    @AuditarOperacion
    public ConfiguracionSchedulerLimpiezaImagenesResponse actualizarConfiguracion(
            ConfigurarSchedulerLimpiezaImagenesRequest request) {
        configuracionSistemaService.actualizarIntervaloLimpiezaImagenes(request.intervaloHoras());
        return construirConfiguracion(
                request.intervaloHoras(),
                configuracionSistemaService.obtenerGraciaLimpiezaImagenesMinutos());
    }

    public EjecutarLimpiezaImagenesResponse ejecutarAhora() {
        LimpiezaImagenesService.Resultado resultado = limpiezaImagenesService.ejecutar();
        limpiezaImagenesScheduler.marcarEjecucionManual();
        auditoriaService.registrarEjecucion(resultado);

        int afectados = resultado.archivosHuerfanos()
                + resultado.imagenesHuerfanas()
                + resultado.imagenesPerfilesBaja();

        return new EjecutarLimpiezaImagenesResponse(
                afectados,
                resultado.archivosHuerfanos(),
                resultado.imagenesHuerfanas(),
                resultado.imagenesPerfilesBaja(),
                resultado.errores(),
                LocalDateTime.now(),
                construirMensaje(resultado));
    }

    private ConfiguracionSchedulerLimpiezaImagenesResponse construirConfiguracion(
            int intervaloHoras, int graciaMinutos) {
        return new ConfiguracionSchedulerLimpiezaImagenesResponse(
                intervaloHoras,
                graciaMinutos,
                limpiezaImagenesScheduler.proximaEjecucion(),
                limpiezaImagenesScheduler.ultimaEjecucion());
    }

    private String construirMensaje(LimpiezaImagenesService.Resultado resultado) {
        int afectados = resultado.archivosHuerfanos()
                + resultado.imagenesHuerfanas()
                + resultado.imagenesPerfilesBaja();

        String mensaje = afectados == 0
                ? "No se encontraron archivos ni imágenes para limpiar."
                : String.format(
                        "Se eliminaron %d archivo(s) huérfano(s), %d imagen(es) sin referencia "
                                + "y %d imagen(es) de perfil en baja.",
                        resultado.archivosHuerfanos(),
                        resultado.imagenesHuerfanas(),
                        resultado.imagenesPerfilesBaja());

        if (resultado.errores() > 0) {
            mensaje += String.format(" %d elemento(s) no se pudieron eliminar.", resultado.errores());
        }
        return mensaje;
    }
}
