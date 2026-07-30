package org.mgroko.program.modelo;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "moodboard")
public class Moodboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_moodboard")
    private Long idMoodboard;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proyecto", nullable = false, unique = true)
    private Proyecto proyecto;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "imagen_moodboard",
            joinColumns = @JoinColumn(name = "id_moodboard"),
            inverseJoinColumns = @JoinColumn(name = "id_imagen")
    )
    private Set<Imagen> imagenes = new HashSet<>();

    public Moodboard() {
    }

    public Long getIdMoodboard() {
        return idMoodboard;
    }

    public void setIdMoodboard(Long idMoodboard) {
        this.idMoodboard = idMoodboard;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
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

    public Set<Imagen> getImagenes() {
        return imagenes;
    }

    public void setImagenes(Set<Imagen> imagenes) {
        this.imagenes = imagenes;
    }
}