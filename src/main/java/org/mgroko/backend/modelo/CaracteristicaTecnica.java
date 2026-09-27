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

    @Column(name = "nombre", length = 100)
    private String nombre;

    @Column(name = "tipo_dato", nullable = false, length = 50)
    private String tipoDato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_profesion", nullable = false)
    private Profesion profesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_unidad")
    private UnidadMedida unidadMedida;

    @Builder.Default
    @OneToMany(mappedBy = "caracteristicaTecnica", fetch = FetchType.LAZY)
    private List<ValorCaracteristica> valores = new ArrayList<>();

    public String getUnidad() {
        return unidadMedida != null ? unidadMedida.getSimbolo() : null;
    }

    public void setUnidad(String unidad) {
        if (unidad == null) {
            this.unidadMedida = null;
        } else {
            if (this.unidadMedida == null) {
                this.unidadMedida = UnidadMedida.builder().simbolo(unidad).build();
            } else {
                this.unidadMedida.setSimbolo(unidad);
            }
        }
    }

    public static class CaracteristicaTecnicaBuilder {
        private String unidadTemp;

        public CaracteristicaTecnicaBuilder unidad(String unidad) {
            this.unidadTemp = unidad;
            if (unidad != null) {
                this.unidadMedida = UnidadMedida.builder().simbolo(unidad).build();
            }
            return this;
        }

        public CaracteristicaTecnica build() {
            CaracteristicaTecnica c = new CaracteristicaTecnica(
                    this.idCaracteristica,
                    this.codigo,
                    this.nombre,
                    this.tipoDato,
                    this.profesion,
                    this.unidadMedida != null ? this.unidadMedida : (this.unidadTemp != null ? UnidadMedida.builder().simbolo(this.unidadTemp).build() : null),
                    this.valores$value != null ? this.valores$value : new ArrayList<>()
            );
            return c;
        }
    }
}
