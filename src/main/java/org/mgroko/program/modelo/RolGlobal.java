package org.mgroko.program.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol_global")
public class RolGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_global")
    private Long idRolGlobal;

    @Column(name = "nombre", nullable = false, length = 20, unique = true)
    private String nombre;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "rol_global_permiso",
            joinColumns = @JoinColumn(name = "id_rol_global"),
            inverseJoinColumns = @JoinColumn(name = "id_permiso_global")
    )
    private Set<PermisoGlobal> permisos = new HashSet<>();

    public RolGlobal() {
    }

    public Long getIdRolGlobal() {
        return idRolGlobal;
    }

    public void setIdRolGlobal(Long idRolGlobal) {
        this.idRolGlobal = idRolGlobal;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Set<PermisoGlobal> getPermisos() {
        return permisos;
    }

    public void setPermisos(Set<PermisoGlobal> permisos) {
        this.permisos = permisos;
    }
}