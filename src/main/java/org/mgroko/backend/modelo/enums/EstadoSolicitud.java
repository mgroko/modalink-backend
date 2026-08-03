package org.mgroko.backend.modelo.enums;

/*
 Estado de solicitud es utilizado por postulacion_actividad, postulacion_gral,
 invitacion_actividad e invitacion_gral (porque usan los mismos estados) */
 
public enum EstadoSolicitud {
    Pendiente,
    Aceptada,
    Rechazada
}
