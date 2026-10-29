package org.mgroko.backend.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mgroko.backend.admin.exception.AutoDeshabilitacionException;
import org.mgroko.backend.admin.exception.CaracteristicaCodigoDuplicadoException;
import org.mgroko.backend.admin.exception.CaracteristicaEnUsoException;
import org.mgroko.backend.admin.exception.CaracteristicaTecnicaNoEncontradaException;
import org.mgroko.backend.admin.exception.TipoDatoInvalidoException;
import org.mgroko.backend.admin.exception.UnidadMedidaDuplicadaException;
import org.mgroko.backend.admin.exception.UnidadMedidaEnUsoException;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.UsuarioEnBajaException;
import org.mgroko.backend.admin.exception.UnidadMedidaNoEncontradaException;
import org.mgroko.backend.admin.exception.ValorCaracteristicaAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.ValorCodigoDuplicadoException;
import org.mgroko.backend.admin.exception.ValorEnUsoException;
import org.mgroko.backend.auth.exception.CorreoDuplicadoException;
import org.mgroko.backend.auth.exception.CredencialesInvalidasException;
import org.mgroko.backend.auth.exception.DniDuplicadoException;
import org.mgroko.backend.auth.exception.EdadInvalidaException;
import org.mgroko.backend.auth.exception.GeneroNoEncontradoException;
import org.mgroko.backend.auth.exception.RolGlobalNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioDeshabilitadoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.calendario.exception.AgendaNoEncontradaException;
import org.mgroko.backend.calendario.exception.BloqueoNoEncontradoException;
import org.mgroko.backend.calendario.exception.BloqueoSolapadoException;
import org.mgroko.backend.calendario.exception.HorarioComprometidoException;
import org.mgroko.backend.calendario.exception.JornadaInvalidaException;
import org.mgroko.backend.calendario.exception.RangoInvalidoException;
import org.mgroko.backend.perfiles.exception.CaracteristicaDuplicateException;
import org.mgroko.backend.perfiles.exception.CaracteristicaNoEncontradaException;
import org.mgroko.backend.perfiles.exception.CaracteristicaProfesionNoCoincideException;
import org.mgroko.backend.perfiles.exception.CaracteristicaValorNoCoincideException;
import org.mgroko.backend.perfiles.exception.IdValorObligatorioException;
import org.mgroko.backend.perfiles.exception.ImagenNoEncontradaException;
import org.mgroko.backend.perfiles.exception.PerfilDuplicadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.exception.ValorCaracteristicaNoEncontradoException;
import org.mgroko.backend.perfiles.exception.ValorNumericoNegativoException;
import org.mgroko.backend.perfiles.exception.ValorObligatorioException;
import org.mgroko.backend.postulaciones.exception.PostulacionNoEncontradaException;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.postulaciones.exception.RangoFechasPostulacionInvalidoException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.storage.exception.ArchivoVacioException;
import org.mgroko.backend.storage.exception.DimensionesImagenExcedidasException;
import org.mgroko.backend.storage.exception.ErrorAlmacenamientoException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.mgroko.backend.ubicacion.exception.LocalidadNoEncontradaException;
import org.mgroko.backend.ubicacion.exception.LocalidadSinProvinciaException;
import org.mgroko.backend.ubicacion.exception.PaisNoConfiguradoException;
import org.mgroko.backend.ubicacion.exception.ProvinciaSinLocalidadException;
import org.mgroko.backend.usuario.exception.SolicitudBajaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones de correo duplicado.
     */
    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones de DNI duplicado.
     */
    @ExceptionHandler(DniDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleDniDuplicado(DniDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones de credenciales inválidas.
     */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja excepciones cuando no se encuentra el rol global.
     */
    @ExceptionHandler(RolGlobalNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRolGlobalNoEncontrado(RolGlobalNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Maneja excepciones cuando el código de género enviado no existe.
     */
    @ExceptionHandler(GeneroNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleGeneroNoEncontrado(GeneroNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja validaciones fallidas de DTOs (anotaciones @Valid).
     */
     @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> erroresPorCampo = new LinkedHashMap<>();
        for (var fieldError : ex.getBindingResult().getFieldErrors()) {
            erroresPorCampo.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Validación fallida");
        response.put("errores", erroresPorCampo);
        response.put("httpStatus", HttpStatus.BAD_REQUEST.value());
        response.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Helper para construir una respuesta de error estándar.
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("httpStatus", status.value());
        response.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(response, status);
    }

    /**
     * Excepción de edad inválida.
     */
    @ExceptionHandler(EdadInvalidaException.class)
        public ResponseEntity<Map<String, Object>> handleEdadInvalida(EdadInvalidaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Excepción de usuario no encontrado.
     */
    @ExceptionHandler(UsuarioNoEncontradoException.class)
        public ResponseEntity<Map<String, Object>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja excepción de cuenta pendiente de baja: el login se rechaza sin
     * emitir sesión y se devuelve la fecha límite de reactivación.
     */
    @ExceptionHandler(org.mgroko.backend.auth.exception.CuentaPendienteBajaException.class)
    public ResponseEntity<Map<String, Object>> handleCuentaPendienteBaja(
            org.mgroko.backend.auth.exception.CuentaPendienteBajaException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("codigo", "CUENTA_PENDIENTE_BAJA");
        response.put("fechaSolicitudBaja", ex.getFechaSolicitudBaja());
        response.put("fechaLimite", ex.getFechaLimite());
        response.put("diasRestantes", ex.getFechaLimite() != null
                ? Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(
                        LocalDateTime.now(), ex.getFechaLimite()))
                : null);
        response.put("httpStatus", HttpStatus.FORBIDDEN.value());
        response.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Maneja excepción de usuario deshabilitado.
     */
    @ExceptionHandler(UsuarioDeshabilitadoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioDeshabilitado(UsuarioDeshabilitadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * Excepción de auto deshabilitación.
     */
    @ExceptionHandler(AutoDeshabilitacionException.class)
    public ResponseEntity<Map<String, Object>> handleAutoDeshabilitacion(AutoDeshabilitacionException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * Excepción de usuario en baja.
     */
    @ExceptionHandler(UsuarioEnBajaException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioEnBaja(UsuarioEnBajaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UsuarioAdminNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioAdminNoEncontrado(UsuarioAdminNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Excepción de perfil no encontrado (contexto: dueño del perfil / módulo perfiles).
     */
    @ExceptionHandler(org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handlePerfilNoEncontrado(
            org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException.class)
    public ResponseEntity<Map<String, Object>> handlePerfilActivoNoSeleccionado(
            org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.mgroko.backend.notificaciones.exception.NotificacionNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleNotificacionNoEncontrada(
            org.mgroko.backend.notificaciones.exception.NotificacionNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.NombreProyectoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleNombreProyectoDuplicado(
            org.mgroko.backend.proyectos.exception.NombreProyectoDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.FechaEntregaInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleFechaEntregaInvalida(
            org.mgroko.backend.proyectos.exception.FechaEntregaInvalidaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.LimiteImagenesMoodboardExcedidoException.class)
    public ResponseEntity<Map<String, Object>> handleLimiteImagenesMoodboardExcedido(
            org.mgroko.backend.proyectos.exception.LimiteImagenesMoodboardExcedidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.RolProyectoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRolProyectoNoEncontrado(
            org.mgroko.backend.proyectos.exception.RolProyectoNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoDenegadoProyecto(
            org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleHabilidadNoEncontrada(
            org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleRequerimientoInvalido(
            org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleProyectoNoEncontrado(
            org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.RangoDuracionInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleRangoDuracionInvalido(
            org.mgroko.backend.actividades.exception.RangoDuracionInvalidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.InformacionBasicaIncompletaException.class)
    public ResponseEntity<Map<String, Object>> handleInformacionBasicaIncompleta(
            org.mgroko.backend.proyectos.exception.InformacionBasicaIncompletaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.EstadoProyectoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleEstadoProyectoInvalido(
            org.mgroko.backend.proyectos.exception.EstadoProyectoInvalidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException.class)
    public ResponseEntity<Map<String, Object>> handleProyectoNoConfirmable(
            org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("codigo", ex.getCodigo());
        response.put("bloqueos", ex.getBloqueos());
        response.put("httpStatus", HttpStatus.CONFLICT.value());
        response.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.EstadoProyectoNoModificableException.class)
    public ResponseEntity<Map<String, Object>> handleEstadoProyectoNoModificable(
            org.mgroko.backend.proyectos.exception.EstadoProyectoNoModificableException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.EstadoProyectoNoCancelableException.class)
    public ResponseEntity<Map<String, Object>> handleEstadoProyectoNoCancelable(
            org.mgroko.backend.proyectos.exception.EstadoProyectoNoCancelableException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.MotivoCancelacionObligatorioException.class)
    public ResponseEntity<Map<String, Object>> handleMotivoCancelacionObligatorio(
            org.mgroko.backend.proyectos.exception.MotivoCancelacionObligatorioException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.UbicacionObligatoriaException.class)
    public ResponseEntity<Map<String, Object>> handleUbicacionObligatoria(
            org.mgroko.backend.proyectos.exception.UbicacionObligatoriaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.proyectos.exception.RequerimientoSatisfechoException.class)
    public ResponseEntity<Map<String, Object>> handleRequerimientoSatisfecho(
            org.mgroko.backend.proyectos.exception.RequerimientoSatisfechoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {
        if (esViolacionPorCiclo(ex)) {
            return buildErrorResponse(
                    "La modificacion genera un ciclo entre actividades.",
                    HttpStatus.CONFLICT);
        }
        return buildErrorResponse(
                "Los datos enviados violan una restriccion de integridad de la base de datos.",
                HttpStatus.BAD_REQUEST);
    }

    private boolean esViolacionPorCiclo(Throwable ex) {
        Throwable actual = ex;
        while (actual != null) {
            String mensaje = actual.getMessage();
            if (mensaje != null
                    && mensaje.toLowerCase(java.util.Locale.ROOT).contains("genera un ciclo")) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    /**
     * Excepción de perfil no encontrado (contexto: Administrador).
     */
    @ExceptionHandler(org.mgroko.backend.admin.exception.PerfilNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handlePerfilAdminNoEncontrado(
            org.mgroko.backend.admin.exception.PerfilNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(LocalidadNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleLocalidadNoEncontrada(LocalidadNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProvinciaSinLocalidadException.class)
    public ResponseEntity<Map<String, Object>> handleProvinciaSinLocalidad(ProvinciaSinLocalidadException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LocalidadSinProvinciaException.class)
    public ResponseEntity<Map<String, Object>> handleLocalidadSinProvincia(LocalidadSinProvinciaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Excepción de país no resoluble desde la configuración del catálogo.
     * Es una falla del despliegue, no de la petición del cliente.
     */
    @ExceptionHandler(PaisNoConfiguradoException.class)
    public ResponseEntity<Map<String, Object>> handlePaisNoConfigurado(PaisNoConfiguradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SolicitudBajaException.class)
    public ResponseEntity<Map<String, Object>> handleSolicitudBaja(SolicitudBajaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Excepción de profesión inexistente.
     */
    @ExceptionHandler(ProfesionNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleProfesionNoEncontrada(ProfesionNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Excepción de característica técnica inexistente.
     */
    @ExceptionHandler(CaracteristicaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleCaracteristicaNoEncontrada(CaracteristicaNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Excepción de característica que no corresponde a la profesión del perfil.
     */
    @ExceptionHandler(CaracteristicaProfesionNoCoincideException.class)
    public ResponseEntity<Map<String, Object>> handleCaracteristicaProfesionNoCoincide(CaracteristicaProfesionNoCoincideException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Excepción de característica técnica repetida en el mismo perfil.
     */
    @ExceptionHandler(CaracteristicaDuplicateException.class)
    public ResponseEntity<Map<String, Object>> handleCaracteristicaDuplicate(CaracteristicaDuplicateException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Excepción de perfil duplicado para la misma profesión.
     */
    @ExceptionHandler(PerfilDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handlePerfilDuplicado(PerfilDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ValorCaracteristicaNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleValorCaracteristicaNoEncontrado(
            ValorCaracteristicaNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IdValorObligatorioException.class)
    public ResponseEntity<Map<String, Object>> handleIdValorObligatorio(IdValorObligatorioException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValorObligatorioException.class)
    public ResponseEntity<Map<String, Object>> handleValorObligatorio(ValorObligatorioException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValorNumericoNegativoException.class)
    public ResponseEntity<Map<String, Object>> handleValorNumericoNegativo(ValorNumericoNegativoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }    @ExceptionHandler(CaracteristicaValorNoCoincideException.class)
    public ResponseEntity<Map<String, Object>> handleCaracteristicaValorNoCoincide(
            CaracteristicaValorNoCoincideException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Excepción de perfil dado de baja o con solicitud de baja activa.
     */
    @ExceptionHandler(PerfilEnBajaException.class)
    public ResponseEntity<Map<String, Object>> handlePerfilEnBaja(PerfilEnBajaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    

    /**
     * Excepción de imagen de perfil inexistente.
     */
    @ExceptionHandler(ImagenNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleImagenNoEncontrada(ImagenNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CaracteristicaTecnicaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleCaracteristicaTecnicaNoEncontrada(
            CaracteristicaTecnicaNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnidadMedidaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleUnidadMedidaNoEncontrada(
            UnidadMedidaNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Excepción de unidad de medida duplicada (nombre o símbolo ya registrados).
     */
    @ExceptionHandler(UnidadMedidaDuplicadaException.class)
    public ResponseEntity<Map<String, Object>> handleUnidadMedidaDuplicada(
            UnidadMedidaDuplicadaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Excepción de unidad de medida asociada a características técnicas en uso.
     */
    @ExceptionHandler(UnidadMedidaEnUsoException.class)
    public ResponseEntity<Map<String, Object>> handleUnidadMedidaEnUso(
            UnidadMedidaEnUsoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ValorCaracteristicaAdminNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleValorCaracteristicaAdminNoEncontrado(
            ValorCaracteristicaAdminNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CaracteristicaCodigoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCaracteristicaCodigoDuplicado(
            CaracteristicaCodigoDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ValorCodigoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleValorCodigoDuplicado(ValorCodigoDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TipoDatoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleTipoDatoInvalido(TipoDatoInvalidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CaracteristicaEnUsoException.class)
    public ResponseEntity<Map<String, Object>> handleCaracteristicaEnUso(CaracteristicaEnUsoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ValorEnUsoException.class)
    public ResponseEntity<Map<String, Object>> handleValorEnUso(ValorEnUsoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AgendaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleAgendaNoEncontrada(AgendaNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BloqueoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleBloqueoNoEncontrado(BloqueoNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BloqueoSolapadoException.class)
    public ResponseEntity<Map<String, Object>> handleBloqueoSolapado(BloqueoSolapadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HorarioComprometidoException.class)
    public ResponseEntity<Map<String, Object>> handleHorarioComprometido(HorarioComprometidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(JornadaInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleJornadaInvalida(JornadaInvalidaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RangoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleRangoInvalido(RangoInvalidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ArchivoVacioException.class)
    public ResponseEntity<Map<String, Object>> handleArchivoVacio(ArchivoVacioException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FormatoImagenInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleFormatoImagenInvalido(FormatoImagenInvalidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DimensionesImagenExcedidasException.class)
    public ResponseEntity<Map<String, Object>> handleDimensionesImagenExcedidas(DimensionesImagenExcedidasException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ErrorAlmacenamientoException.class)
    public ResponseEntity<Map<String, Object>> handleErrorAlmacenamiento(ErrorAlmacenamientoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExcedido(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return buildErrorResponse(
                "El archivo supera el tama�o m�ximo permitido.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.ActividadNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleActividadNoEncontrada(
            org.mgroko.backend.actividades.exception.ActividadNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.ActividadCanceladaException.class)
    public ResponseEntity<Map<String, Object>> handleActividadCancelada(
            org.mgroko.backend.actividades.exception.ActividadCanceladaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.ActividadNoModificableException.class)
    public ResponseEntity<Map<String, Object>> handleActividadNoModificable(
            org.mgroko.backend.actividades.exception.ActividadNoModificableException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.ActividadNoEliminableException.class)
    public ResponseEntity<Map<String, Object>> handleActividadNoEliminable(
            org.mgroko.backend.actividades.exception.ActividadNoEliminableException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteEliminarActividadesException.class)
    public ResponseEntity<Map<String, Object>> handleEstadoProyectoNoPermiteEliminarActividades(
            org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteEliminarActividadesException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.MotivoEliminacionObligatorioException.class)
    public ResponseEntity<Map<String, Object>> handleMotivoEliminacionObligatorio(
            org.mgroko.backend.actividades.exception.MotivoEliminacionObligatorioException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteModificarActividadesException.class)
    public ResponseEntity<Map<String, Object>> handleEstadoProyectoNoPermiteModificarActividades(
            org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteModificarActividadesException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.TransicionEstadoActividadInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleTransicionEstadoActividadInvalida(
            org.mgroko.backend.actividades.exception.TransicionEstadoActividadInvalidaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.NombreActividadDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleNombreActividadDuplicado(
            org.mgroko.backend.actividades.exception.NombreActividadDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.PredecesoraInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handlePredecesoraInvalida(
            org.mgroko.backend.actividades.exception.PredecesoraInvalidaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteActividadesException.class)
    public ResponseEntity<Map<String, Object>> handleEstadoProyectoNoPermiteActividades(
            org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteActividadesException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.FechaEntregaNoDefinidaException.class)
    public ResponseEntity<Map<String, Object>> handleFechaEntregaNoDefinida(
            org.mgroko.backend.actividades.exception.FechaEntregaNoDefinidaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.CronogramaInviableException.class)
    public ResponseEntity<Map<String, Object>> handleCronogramaInviable(
            org.mgroko.backend.actividades.exception.CronogramaInviableException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.CicloActividadException.class)
    public ResponseEntity<Map<String, Object>> handleCicloActividad(
            org.mgroko.backend.actividades.exception.CicloActividadException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.RequerimientoConVinculosException.class)
    public ResponseEntity<Map<String, Object>> handleRequerimientoConVinculos(
            org.mgroko.backend.actividades.exception.RequerimientoConVinculosException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.actividades.exception.DuracionInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleDuracionInvalida(
            org.mgroko.backend.actividades.exception.DuracionInvalidaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PostulacionNoPermitidaException.class)
    public ResponseEntity<Map<String, Object>> handlePostulacionNoPermitida(
            PostulacionNoPermitidaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RequerimientoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRequerimientoNoEncontrado(
            RequerimientoNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RangoFechasPostulacionInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleRangoFechasPostulacionInvalido(
            RangoFechasPostulacionInvalidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PostulacionNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handlePostulacionNoEncontrada(
            PostulacionNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PostulacionYaGestionadaException.class)
    public ResponseEntity<Map<String, Object>> handlePostulacionYaGestionada(
            PostulacionYaGestionadaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.reportes.exception.ReporteNoPermitidoException.class)
    public ResponseEntity<Map<String, Object>> handleReporteNoPermitido(
            org.mgroko.backend.reportes.exception.ReporteNoPermitidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(org.mgroko.backend.reportes.exception.AutoreporteException.class)
    public ResponseEntity<Map<String, Object>> handleAutoreporte(
            org.mgroko.backend.reportes.exception.AutoreporteException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.reportes.exception.ReportePendienteExistenteException.class)
    public ResponseEntity<Map<String, Object>> handleReportePendienteExistente(
            org.mgroko.backend.reportes.exception.ReportePendienteExistenteException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.reportes.exception.DescripcionReporteObligatoriaException.class)
    public ResponseEntity<Map<String, Object>> handleDescripcionReporteObligatoria(
            org.mgroko.backend.reportes.exception.DescripcionReporteObligatoriaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.reportes.exception.DescripcionReporteInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleDescripcionReporteInvalida(
            org.mgroko.backend.reportes.exception.DescripcionReporteInvalidaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.mgroko.backend.reportes.exception.ReporteNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleReporteNoEncontrado(
            org.mgroko.backend.reportes.exception.ReporteNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.mgroko.backend.reportes.exception.EstadoReporteInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleEstadoReporteInvalido(
            org.mgroko.backend.reportes.exception.EstadoReporteInvalidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.mgroko.backend.reportes.exception.RangoFechasReporteInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleRangoFechasReporteInvalido(
            org.mgroko.backend.reportes.exception.RangoFechasReporteInvalidoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
