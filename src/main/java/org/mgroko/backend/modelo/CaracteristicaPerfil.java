package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "caracteristica_perfil", uniqueConstraints = @UniqueConstraint(columnNames = { "id_perfil",
        "id_caracteristica" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaracteristicaPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caracteristica_perfil")
    private Long idCaracteristicaPerfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil", nullable = false)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caracteristica", nullable = false)
    private CaracteristicaTecnica caracteristicaTecnica;

    @Column(name = "valor", length = 50)
    private String valor;

    /**
     * Columna {@code id_valor} de la fila. Es la única parte escribible del par
     * {@code (id_valor, id_caracteristica)}: la asociación
     * {@link #valorCaracteristica} está mapeada como solo lectura
     * ({@code insertable = false, updatable = false}) porque
     * {@code id_caracteristica} ya la escribe {@link #caracteristicaTecnica}, y
     * Hibernate no deriva una de la otra. Por eso, al asociar un
     * {@link org.mgroko.backend.modelo.ValorCaracteristica} hay que setear
     * también este campo: omitirlo persiste {@code id_valor = NULL} y el trigger
     * {@code chk_caracteristica_perfil_valor} rechaza las características ENUMERADO.
     */
    @Column(name = "id_valor")
    private Long idValor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "id_valor", referencedColumnName = "id_valor", insertable = false, updatable = false),
            @JoinColumn(name = "id_caracteristica", referencedColumnName = "id_caracteristica", insertable = false, updatable = false)
    })
    private ValorCaracteristica valorCaracteristica;

    @Column(name = "fecha_registro")
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}