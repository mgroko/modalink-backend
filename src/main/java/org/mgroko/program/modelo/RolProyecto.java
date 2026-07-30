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
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol_proyecto")
public class RolProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_proyecto")
    private Long idRolProyecto;

    @Column(name = "nombre", nullable = false, length = 20, unique = true)
    private String nombre;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "rol_proyecto_permiso",
            joinColumns = @JoinColumn(name = "id_rol_proyecto"),
            inverseJoinColumns = @JoinColumn(name = "id_permiso_proyecto")
    )
    private Set<PermisoProyecto> permisos = new HashSet<>();

    public RolProyecto() {
    }

    public Long getIdRolProyecto() {
        return idRolProyecto;
    }

    public void setIdRolProyecto(Long idRolProyecto) {
        this.idRolProyecto = idRolProyecto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Set<PermisoProyecto> getPermisos() {
        return permisos;
    }

    public void setPermisos(Set<PermisoProyecto> permisos) {
        this.permisos = permisos;
    }
}