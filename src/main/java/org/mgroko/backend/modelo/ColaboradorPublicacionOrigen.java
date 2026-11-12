package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.OrigenColaboracion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadatos de una etiqueta en {@code colaborador_publicacion}.
 *
 * <p>La tabla de unión no puede llevar columnas extra con el mapeo
 * {@code @ManyToMany} actual, por eso el origen se guarda en esta tabla
 * auxiliar con la misma clave compuesta.</p>
 */
@Entity
@Table(name = "colaborador_publicacion_origen")
@IdClass(ColaboradorPublicacionOrigenId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColaboradorPublicacionOrigen {

    @Id
    @Column(name = "id_publicacion", nullable = false)
    private Long idPublicacion;

    @Id
    @Column(name = "id_perfil", nullable = false)
    private Long idPerfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 20)
    private OrigenColaboracion origen;

    @Column(name = "id_colaboracion_automatica")
    private Long idColaboracionAutomatica;

    @Column(name = "fecha_alta", nullable = false)
    @Builder.Default
    private LocalDateTime fechaAlta = LocalDateTime.now();
}
