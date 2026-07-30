package org.mgroko.program.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permiso_global")
public class PermisoGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso_global")
    private Long idPermisoGlobal;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    public PermisoGlobal() {
    }

    public Long getIdPermisoGlobal() {
        return idPermisoGlobal;
    }

    public void setIdPermisoGlobal(Long idPermisoGlobal) {
        this.idPermisoGlobal = idPermisoGlobal;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}