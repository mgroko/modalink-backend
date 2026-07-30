package org.mgroko.program.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "requerimiento_gral_proyecto")
public class RequerimientoGralProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_requerimiento_gral")
    private Long idRequerimientoGral;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profesion", nullable = false)
    private Profesion profesion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "requerimiento_gral_habilidad",
            joinColumns = @JoinColumn(name = "id_requerimiento_gral"),
            inverseJoinColumns = @JoinColumn(name = "id_habilidad")
    )
    private Set<Habilidad> habilidades = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "requerimiento_gral_caract",
            joinColumns = @JoinColumn(name = "id_requerimiento_gral"),
            inverseJoinColumns = @JoinColumn(name = "id_caracteristica")
    )
    private Set<CaracteristicaTecnica> caracteristicasTecnicas = new HashSet<>();

    public RequerimientoGralProyecto() {
    }

    public Long getIdRequerimientoGral() {
        return idRequerimientoGral;
    }

    public void setIdRequerimientoGral(Long idRequerimientoGral) {
        this.idRequerimientoGral = idRequerimientoGral;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public Profesion getProfesion() {
        return profesion;
    }

    public void setProfesion(Profesion profesion) {
        this.profesion = profesion;
    }

    public Set<Habilidad> getHabilidades() {
        return habilidades;
    }

    public void setHabilidades(Set<Habilidad> habilidades) {
        this.habilidades = habilidades;
    }

    public Set<CaracteristicaTecnica> getCaracteristicasTecnicas() {
        return caracteristicasTecnicas;
    }

    public void setCaracteristicasTecnicas(Set<CaracteristicaTecnica> caracteristicasTecnicas) {
        this.caracteristicasTecnicas = caracteristicasTecnicas;
    }
}