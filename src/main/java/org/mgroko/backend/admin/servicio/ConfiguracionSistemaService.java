package org.mgroko.backend.admin.servicio;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.mgroko.backend.admin.dto.ConfiguracionSchedulerBajaResponse;
import org.mgroko.backend.admin.dto.ConfiguracionSchedulerResponse;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerBajaRequest;
import org.mgroko.backend.admin.dto.ConfigurarSchedulerRequest;
import org.mgroko.backend.admin.dto.EjecutarSchedulerResponse;
import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.perfiles.servicio.ExpirarPerfilService;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
import org.mgroko.backend.usuario.servicio.ExpirarCuentaService;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionSistemaService {

    public static final String CLAVE_DESHABILITACION_HORA = "SCHEDULER_DESHABILITACION_HORA";
    public static final String CLAVE_DESHABILITACION_MINUTO = "SCHEDULER_DESHABILITACION_MINUTO";
    public static final String CLAVE_DESHABILITACION_CRON = "SCHEDULER_DESHABILITACION_CRON";
    public static final String CLAVE_BAJA_HORA = "SCHEDULER_BAJA_HORA";
    public static final String CLAVE_BAJA_MINUTO = "SCHEDULER_BAJA_MINUTO";
    public static final String CLAVE_BAJA_CRON = "SCHEDULER_BAJA_CRON";
    public static final String CLAVE_MARGEN_ACTIVIDAD_MIN = "AGENDA_MARGEN_ACTIVIDAD_MIN";
    public static final String CLAVE_DIAS_BAJA = "DIAS_BAJA";
    public static final String CLAVE_IMAGEN_MAX_LADO_PX = "IMAGEN_MAX_LADO_PX";
    public static final String CLAVE_IMAGEN_MAX_MEGAPIXELES = "IMAGEN_MAX_MEGAPIXELES";
    public static final String CLAVE_IMAGEN_CALIDAD_WEBP = "IMAGEN_CALIDAD_WEBP";
    public static final String CLAVE_IMAGEN_LADO_SALIDA_PX = "IMAGEN_LADO_SALIDA_PX";
    public static final String CLAVE_IMAGEN_MAX_TAMANO_BYTES = "IMAGEN_MAX_TAMANO_BYTES";
    public static final String CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS = "IMAGEN_LIMPIEZA_INTERVALO_HORAS";
    public static final String CLAVE_IMAGEN_LIMPIEZA_GRACIA_MINUTOS = "IMAGEN_LIMPIEZA_GRACIA_MINUTOS";

    private static final int DEFAULT_HORA = 2;
    private static final int DEFAULT_MINUTO = 0;
    private static final String DEFAULT_CRON = "0 0 2 * * *";
    private static final int DEFAULT_MARGEN_ACTIVIDAD_MIN = 30;
    private static final int DEFAULT_DIAS_BAJA = 30;

    private final ConfiguracionSistemaRepository configuracionSistemaRepository;
    private final ExpirarDeshabilitacionService expirarDeshabilitacionService;
    private final ExpirarCuentaService expirarCuentaService;
    private final ExpirarPerfilService expirarPerfilService;

    public ConfiguracionSistemaService(
            ConfiguracionSistemaRepository configuracionSistemaRepository,
            ExpirarDeshabilitacionService expirarDeshabilitacionService,
            ExpirarCuentaService expirarCuentaService,
            ExpirarPerfilService expirarPerfilService) {
        this.configuracionSistemaRepository = configuracionSistemaRepository;
        this.expirarDeshabilitacionService = expirarDeshabilitacionService;
        this.expirarCuentaService = expirarCuentaService;
        this.expirarPerfilService = expirarPerfilService;
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

    /**
     * Cantidad de días que dispone el usuario para reactivar su cuenta después
     * de solicitar la baja (UC-07). Se lee de {@code configuracion_sistema}
     * (clave {@link #CLAVE_DIAS_BAJA}); ante fila ausente o valor no válido se
     * usa 30, igual que el comportamiento histórico.
     *
     * @return días de plazo para reactivar la cuenta, siempre &gt; 0
     */
    @Transactional(readOnly = true)
    public int obtenerDiasBaja() {
        int dias = obtenerValorEntero(CLAVE_DIAS_BAJA, DEFAULT_DIAS_BAJA);
        return dias > 0 ? dias : DEFAULT_DIAS_BAJA;
    }

    /**
     * Horas entre ejecuciones del job de limpieza de imágenes huérfanas.
     * Se lee de {@code configuracion_sistema} (clave
     * {@link #CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS}) en cada ciclo, por lo que
     * un cambio de valor no exige reiniciar el servidor.
     *
     * @return intervalo en horas, siempre &gt; 0
     * @throws IllegalStateException si la clave falta o su valor no es un entero &gt; 0
     */
    @Transactional(readOnly = true)
    public int obtenerIntervaloLimpiezaImagenesHoras() {
        int horas = obtenerValorEnteroObligatorio(CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS);
        if (horas <= 0) {
            throw new IllegalStateException("El valor de '" + CLAVE_IMAGEN_LIMPIEZA_INTERVALO_HORAS
                    + "' en configuracion_sistema debe ser mayor que cero: " + horas);
        }
        return horas;
    }

    /**
     * Periodo de gracia, en minutos, que debe tener de antigüedad una imagen o
     * un archivo antes de entrar en la limpieza. Se lee de
     * {@code configuracion_sistema} (clave
     * {@link #CLAVE_IMAGEN_LIMPIEZA_GRACIA_MINUTOS}).
     *
     * @return gracia en minutos, siempre &gt;= 0
     * @throws IllegalStateException si la clave falta o su valor no es un entero &gt;= 0
     */
    @Transactional(readOnly = true)
    public int obtenerGraciaLimpiezaImagenesMinutos() {
        int minutos = obtenerValorEnteroObligatorio(CLAVE_IMAGEN_LIMPIEZA_GRACIA_MINUTOS);
        if (minutos < 0) {
            throw new IllegalStateException("El valor de '" + CLAVE_IMAGEN_LIMPIEZA_GRACIA_MINUTOS
                    + "' en configuracion_sistema no puede ser negativo: " + minutos);
        }
        return minutos;
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

    /**
     * Configuración del scheduler de expiración de bajas (UC-07/UC-12):
     * horario de ejecución y cantidad de días de plazo para reactivar.
     */
    @Transactional(readOnly = true)
    public ConfiguracionSchedulerBajaResponse obtenerConfiguracionBaja() {
        int hora = obtenerValorEntero(CLAVE_BAJA_HORA, DEFAULT_HORA);
        int minuto = obtenerValorEntero(CLAVE_BAJA_MINUTO, DEFAULT_MINUTO);
        String cron = obtenerCronBaja();

        return new ConfiguracionSchedulerBajaResponse(
                hora, minuto, cron, calcularProximaEjecucion(cron), obtenerDiasBaja());
    }

    @Transactional
    public ConfiguracionSchedulerBajaResponse actualizarConfiguracionBaja(ConfigurarSchedulerBajaRequest request) {
        int hora = request.hora();
        int minuto = request.minuto();
        int diasBaja = request.diasBaja();
        String cron = String.format("0 %d %d * * *", minuto, hora);

        if (!CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("La expresión cron generada no es válida: " + cron);
        }

        guardarOActualizar(CLAVE_BAJA_HORA, String.valueOf(hora),
                "Hora del día (0-23) en la que se ejecuta el scheduler de expiración de bajas");
        guardarOActualizar(CLAVE_BAJA_MINUTO, String.valueOf(minuto),
                "Minuto (0-59) en el que se ejecuta el scheduler de expiración de bajas");
        guardarOActualizar(CLAVE_BAJA_CRON, cron,
                "Expresión cron para el scheduler de expiración de bajas");
        guardarOActualizar(CLAVE_DIAS_BAJA, String.valueOf(diasBaja),
                "Días de plazo que tiene el usuario para reactivar su cuenta antes de la baja definitiva");

        LocalDateTime proximaEjecucion = calcularProximaEjecucion(cron);
        return new ConfiguracionSchedulerBajaResponse(hora, minuto, cron, proximaEjecucion, diasBaja);
    }

    @Transactional(readOnly = true)
    public String obtenerCronBaja() {
        return configuracionSistemaRepository.findByClave(CLAVE_BAJA_CRON)
                .map(ConfiguracionSistema::getValor)
                .filter(CronExpression::isValidExpression)
                .orElse(DEFAULT_CRON);
    }

    /**
     * Ejecución manual del scheduler de expiración de bajas: da de baja las
     * cuentas (UC-07) y los perfiles (UC-12) cuyo plazo venció.
     */
    @Transactional
    public EjecutarSchedulerResponse ejecutarBajaManual() {
        int diasBaja = obtenerDiasBaja();
        int cuentasExpiradas = expirarCuentaService.expirarVencidos(diasBaja);
        int perfilesExpirados = expirarPerfilService.expirarVencidos(diasBaja);
        int total = cuentasExpiradas + perfilesExpirados;

        return new EjecutarSchedulerResponse(
                total,
                LocalDateTime.now(),
                total == 0
                        ? "No se encontraron cuentas ni perfiles con el plazo de baja vencido."
                        : String.format("Se dieron de baja %d cuenta(s) y %d perfil(es).",
                                cuentasExpiradas, perfilesExpirados)
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

    /**
     * Lee una clave de {@code configuracion_sistema} sin valor por defecto:
     * si la fila falta o el valor no es un entero, se lanza el error explícito.
     *
     * @param clave clave a leer
     * @return valor entero de la clave
     * @throws IllegalStateException si la clave falta o su valor no es entero
     */
    private int obtenerValorEnteroObligatorio(String clave) {
        String valor = configuracionSistemaRepository.findByClave(clave)
                .map(ConfiguracionSistema::getValor)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el valor de '" + clave + "' en configuracion_sistema."));
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("El valor de '" + clave
                    + "' en configuracion_sistema no es un número entero: " + valor);
        }
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
