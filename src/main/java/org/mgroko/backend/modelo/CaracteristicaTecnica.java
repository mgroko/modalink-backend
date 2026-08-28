package org.mgroko.backend.modelo;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "caracteristica_tecnica")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaracteristicaTecnica {

    public static final String TIPO_ENUMERADO = "ENUMERADO";
    public static final String TIPO_TEXTO = "TEXTO";
    public static final String TIPO_NUMERICO = "NUMERICO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caracteristica")
    private Long idCaracteristica;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "unidad", length = 50)
    private String unidad;

    @Column(name = "tipo_dato", nullable = false, length = 20)
    private String tipoDato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profesion")
    private Profesion profesion;

    @Builder.Default
    @OneToMany(mappedBy = "caracteristicaTecnica", fetch = FetchType.LAZY)
    private List<ValorCaracteristica> valores = new ArrayList<>();
}
