package org.mgroko.backend.modelo;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "caracteristica_perfil")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaracteristicaPerfil {

    @EmbeddedId
    private CaracteristicaPerfilId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPerfil")
    @JoinColumn(name = "id_perfil")
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCaracteristica")
    @JoinColumn(name = "id_caracteristica")
    private CaracteristicaTecnica caracteristicaTecnica;

    @Column(name = "valor", length = 50)
    private String valor;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;
}