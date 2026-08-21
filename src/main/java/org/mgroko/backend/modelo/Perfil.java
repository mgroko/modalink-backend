package org.mgroko.backend.modelo;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.mgroko.backend.modelo.enums.EstadoPerfil;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "perfil")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil")
    private Long idPerfil;

    @Column(name = "nombre_artistico", nullable = false, length = 50)
    private String nombreArtistico;

    @Column(name = "biografia", nullable = false, length = 500)
    private String biografia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoPerfil estado = EstadoPerfil.Activo;

    @Column(name = "fecha_solicitud_baja")
    private LocalDateTime fechaSolicitudBaja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // UC-11: la profesión no se puede modificar una vez creado el perfil.
    // Esa regla se refuerza con un trigger en la bd (BEFORE UPDATE)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_profesion", nullable = false)
    private Profesion profesion;

    // Foto de perfil, opcional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_imagen")
    private Imagen imagen;

    @Builder.Default
    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CaracteristicaPerfil> caracteristicas = new HashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "habilidad_perfil",
        joinColumns = @JoinColumn(name = "id_perfil"),
        inverseJoinColumns = @JoinColumn(name = "id_habilidad")
    )
    private Set<Habilidad> habilidades = new HashSet<>();
}
