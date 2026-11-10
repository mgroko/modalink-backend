package org.mgroko.backend.conexiones.modelo;

/**
 * Estado de la relación entre el perfil activo y el perfil consultado.
 * Se expone en el detalle de perfil (UC-14) para que el frontend muestre la
 * acción correcta: conectar, cancelar la solicitud, aceptar/rechazar o
 * eliminar la conexión.
 */
public enum EstadoRelacionPerfil {
    NINGUNA,
    SOLICITUD_ENVIADA,
    SOLICITUD_RECIBIDA,
    CONECTADO
}
