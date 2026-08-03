package org.mgroko.backend.modelo;

import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "proyecto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proyecto")
    private Long idProyecto;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoProyecto estado = EstadoProyecto.Borrador;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacidad", nullable = false, length = 20)
    private Privacidad privacidad;

    @Column(name = "acepta_postulacion_gral", nullable = false)
    @Builder.Default
    private Boolean aceptaPostulacionGral = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ubicacion")
    private Ubicacion ubicacion;
}
