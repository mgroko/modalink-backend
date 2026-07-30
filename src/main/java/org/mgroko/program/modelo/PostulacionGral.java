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
@Table(name = "postulacion_gral")
public class PostulacionGral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulacion_gral")
    private Long idPostulacionGral;

    @Column(name = "fecha_postulacion", nullable = false)
    private LocalDateTime fechaPostulacion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil", nullable = false)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_requerimiento_gral", nullable = false)
    private RequerimientoGralProyecto requerimientoGralProyecto;

    public PostulacionGral() {
    }

    public Long getIdPostulacionGral() {
        return idPostulacionGral;
    }

    public void setIdPostulacionGral(Long idPostulacionGral) {
        this.idPostulacionGral = idPostulacionGral;
    }

    public LocalDateTime getFechaPostulacion() {
        return fechaPostulacion;
    }

    public void setFechaPostulacion(LocalDateTime fechaPostulacion) {
        this.fechaPostulacion = fechaPostulacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public RequerimientoGralProyecto getRequerimientoGralProyecto() {
        return requerimientoGralProyecto;
    }

    public void setRequerimientoGralProyecto(RequerimientoGralProyecto requerimientoGralProyecto) {
        this.requerimientoGralProyecto = requerimientoGralProyecto;
    }
}