package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoConexion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Conexión vigente o histórica entre dos perfiles.
 *
 * <p>Los IDs de perfil se guardan en orden canónico
 * ({@code id_perfil_1 < id_perfil_2}) para que el par sea único. La baja es
 * lógica: se conserva la fila con estado {@code ELIMINADA} y
 * {@code fecha_fin_conexion}.</p>
 */
@Entity
@Table(name = "conexion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conexion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conexion")
    private Long idConexion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_1", nullable = false)
    private Perfil perfil1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_2", nullable = false)
    private Perfil perfil2;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud_conexion", nullable = false, unique = true)
    private SolicitudConexion solicitudOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoConexion estado = EstadoConexion.ACTIVA;

    @Column(name = "fecha_inicio_conexion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaInicioConexion = LocalDateTime.now();

    @Column(name = "fecha_fin_conexion")
    private LocalDateTime fechaFinConexion;
}
