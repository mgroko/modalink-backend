package org.mgroko.backend.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Representa un día laborable de una agenda con su propio horario.
 * Cada fila es un día de la semana (1 = Lunes ... 7 = Domingo); un día
 * ausente significa que no es laborable. Permite jornadas personalizadas
 * por día o una misma jornada para varios días (filas con igual horario).
 */
@Entity
@Table(name = "jornada_agenda",
        uniqueConstraints = @UniqueConstraint(name = "uq_jornada_agenda_dia", columnNames = {"id_agenda", "dia_semana"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JornadaAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_jornada")
    private Long idJornada;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_agenda", nullable = false)
    private Agenda agenda;
}