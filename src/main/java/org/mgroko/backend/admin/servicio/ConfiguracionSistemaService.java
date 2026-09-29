package org.mgroko.backend.admin.servicio;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.mgroko.backend.admin.dto.ConfiguracionSchedulerResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerRequest;
import org.mgroko.backend.admin.dto.EjecutarSchedulerResponse;
import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionSistemaService {

    public static final String CLAVE_DESHABILITACION_HORA = "SCHEDULER_DESHABILITACION_HORA";
    public static final String CLAVE_DESHABILITACION_MINUTO = "SCHEDULER_DESHABILITACION_MINUTO";
    public static final String CLAVE_DESHABILITACION_CRON = "SCHEDULER_DESHABILITACION_CRON";
    public static final String CLAVE_MARGEN_ACTIVIDAD_MIN = "AGENDA_MARGEN_ACTIVIDAD_MIN";

    private static final int DEFAULT_HORA = 2;
    private static final int DEFAULT_MINUTO = 0;
    private static final String DEFAULT_CRON = "0 0 2 * * *";
    private static final int DEFAULT_MARGEN_ACTIVIDAD_MIN = 30;

    private final ConfiguracionSistemaRepository configuracionSistemaRepository;
    private final ExpirarDeshabilitacionService expirarDeshabilitacionService;

    public ConfiguracionSistemaService(
            ConfiguracionSistemaRepository configuracionSistemaRepository,
            ExpirarDeshabilitacionService expirarDeshabilitacionService) {
        this.configuracionSistemaRepository = configuracionSistemaRepository;
        this.expirarDeshabilitacionService = expirarDeshabilitacionService;
    }

    @Transactional(readOnly = true)
    public ConfiguracionSchedulerResponse obtenerConfiguracionDeshabilitacion() {
        int hora = obtenerValorEntero(CLAVE_DESHABILITACION_HORA, DEFAULT_HORA);
        int minuto = obtenerValorEntero(CLAVE_DESHABILITACION_MINUTO, DEFAULT_MINUTO);
        String cron = obtenerCronDeshabilitacion();
        LocalDateTime proximaEjecucion = calcularProximaEjecucion(cron);

        return new ConfiguracionSchedulerResponse(hora, minuto, cron, proximaEjecucion);
    }

    @Transactional
    public ConfiguracionSchedulerResponse actualizarConfiguracionDeshabilitacion(ConfigurarSchedulerRequest request) {
        int hora = request.hora();
        int minuto = request.minuto();
        String cron = String.format("0 %d %d * * *", minuto, hora);

        // Validar sintaxis cron por seguridad
        if (!CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("La expresión cron generada no es válida: " + cron);
        }

        guardarOActualizar(CLAVE_DESHABILITACION_HORA, String.valueOf(hora),
                "Hora del día (0-23) en la que se ejecuta el scheduler de deshabilitación");
        guardarOActualizar(CLAVE_DESHABILITACION_MINUTO, String.valueOf(minuto),
                "Minuto (0-59) en el que se ejecuta el scheduler de deshabilitación");
        guardarOActualizar(CLAVE_DESHABILITACION_CRON, cron,
                "Expresión cron para el scheduler de deshabilitación de usuarios");

        LocalDateTime proximaEjecucion = calcularProximaEjecucion(cron);
        return new ConfiguracionSchedulerResponse(hora, minuto, cron, proximaEjecucion);
    }

    @Transactional(readOnly = true)
    public String obtenerCronDeshabilitacion() {
        return configuracionSistemaRepository.findByClave(CLAVE_DESHABILITACION_CRON)
                .map(ConfiguracionSistema::getValor)
                .filter(CronExpression::isValidExpression)
                .orElse(DEFAULT_CRON);
    }

    /**
     * Margen en minutos aplicado a cada lado de las actividades del calendario.
     * Se lee de {@code configuracion_sistema} (clave {@link #CLAVE_MARGEN_ACTIVIDAD_MIN});
     * ante fila ausente, valor no numérico o negativo se usa 30 (igual que el
     * trigger {@code fn_crear_agenda} y el DEFAULT de la columna).
     *
     * @return margen por defecto en minutos, siempre >= 0
     */
    @Transactional(readOnly = true)
    public int obtenerMargenActividadDefecto() {
        int margen = obtenerValorEntero(CLAVE_MARGEN_ACTIVIDAD_MIN, DEFAULT_MARGEN_ACTIVIDAD_MIN);
        return margen < 0 ? DEFAULT_MARGEN_ACTIVIDAD_MIN : margen;
    }

    @Transactional
    public EjecutarSchedulerResponse ejecutarDeshabilitacionManual() {
        int reactivados = expirarDeshabilitacionService.reactivarVencidos();
        return new EjecutarSchedulerResponse(
                reactivados,
                LocalDateTime.now(),
                reactivados == 0
                        ? "No se encontraron usuarios con deshabilitación vencida para reactivar."
                        : String.format("Se reactivaron con éxito %d usuario(s).", reactivados)
        );
    }

    private int obtenerValorEntero(String clave, int valorDefecto) {
        return configuracionSistemaRepository.findByClave(clave)
                .map(config -> {
                    try {
                        return Integer.parseInt(config.getValor());
                    } catch (NumberFormatException e) {
                        return valorDefecto;
                    }
                })
                .orElse(valorDefecto);
    }

    private void guardarOActualizar(String clave, String valor, String descripcion) {
        ConfiguracionSistema config = configuracionSistemaRepository.findByClave(clave)
                .orElseGet(() -> ConfiguracionSistema.builder().clave(clave).build());
        config.setValor(valor);
        if (descripcion != null) {
            config.setDescripcion(descripcion);
        }
        configuracionSistemaRepository.save(config);
    }

    private LocalDateTime calcularProximaEjecucion(String cron) {
        try {
            CronExpression expression = CronExpression.parse(cron);
            return expression.next(LocalDateTime.now().atZone(ZoneId.systemDefault()))
                    .toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }
}
