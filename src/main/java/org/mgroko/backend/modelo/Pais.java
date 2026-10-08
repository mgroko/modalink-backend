package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pais")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pais")
    private Long idPais;

    @Column(name = "codigo_iso", nullable = false, length = 2)
    private String codigoIso;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
