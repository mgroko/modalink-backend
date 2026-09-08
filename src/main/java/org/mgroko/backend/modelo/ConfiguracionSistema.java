package org.mgroko.backend.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionSistema {

    @Id
    @Column(name = "clave", length = 100, nullable = false)
    private String clave;

    @Column(name = "valor", length = 255, nullable = false)
    private String valor;

    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
