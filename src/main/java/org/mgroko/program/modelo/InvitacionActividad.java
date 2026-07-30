package org.mgroko.program.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitacion_actividad")
public class InvitacionActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_invitacion")
    private Long idInvitacion;

    @Column(name = "mensaje", length = 200)
    private String mensaje;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil_remitente", nullable = false)
    private Perfil perfilRemitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil_destinatario", nullable = false)
    private Perfil perfilDestinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_requerimiento", nullable = false)
    private RequerimientoActividad requerimientoActividad;

    public InvitacionActividad() {
    }

    public Long getIdInvitacion() {
        return idInvitacion;
    }

    public void setIdInvitacion(Long idInvitacion) {
        this.idInvitacion = idInvitacion;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Perfil getPerfilRemitente() {
        return perfilRemitente;
    }

    public void setPerfilRemitente(Perfil perfilRemitente) {
        this.perfilRemitente = perfilRemitente;
    }

    public Perfil getPerfilDestinatario() {
        return perfilDestinatario;
    }

    public void setPerfilDestinatario(Perfil perfilDestinatario) {
        this.perfilDestinatario = perfilDestinatario;
    }

    public RequerimientoActividad getRequerimientoActividad() {
        return requerimientoActividad;
    }

    public void setRequerimientoActividad(RequerimientoActividad requerimientoActividad) {
        this.requerimientoActividad = requerimientoActividad;
    }
}