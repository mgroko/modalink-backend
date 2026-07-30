package org.mgroko.program.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permiso_proyecto")
public class PermisoProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso_proyecto")
    private Long idPermisoProyecto;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    public PermisoProyecto() {
    }

    public Long getIdPermisoProyecto() {
        return idPermisoProyecto;
    }

    public void setIdPermisoProyecto(Long idPermisoProyecto) {
        this.idPermisoProyecto = idPermisoProyecto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}