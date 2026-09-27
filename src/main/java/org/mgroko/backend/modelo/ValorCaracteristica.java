package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "valor_caracteristica")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ValorCaracteristica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_valor")
    private Long idValor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caracteristica", nullable = false)
    private CaracteristicaTecnica caracteristicaTecnica;

    @Column(name = "etiqueta", length = 255)
    private String etiqueta;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    public String getCodigo() {
        return etiqueta;
    }

    public void setCodigo(String codigo) {
        this.etiqueta = codigo;
    }

    public static class ValorCaracteristicaBuilder {
        public ValorCaracteristicaBuilder codigo(String codigo) {
            this.etiqueta = codigo;
            return this;
        }
    }
}