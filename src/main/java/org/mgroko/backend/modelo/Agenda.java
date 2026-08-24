package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "agenda")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Agenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_agenda")
    private Long idAgenda;

    @Column(name = "dias_laborales", nullable = false, length = 100)
    private String diasLaborales;

    @Column(name = "hora_inicio_jornada", nullable = false)
    private LocalTime horaInicioJornada;

    @Column(name = "hora_fin_jornada", nullable = false)
    private LocalTime horaFinJornada;

    // Una agenda por usuario (UNIQUE en la base)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;
}
