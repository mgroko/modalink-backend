package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaResumenRepository {

    List<Object[]> contarPorTipo(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            List<String> tiposEvento,
            String operador,
            String texto);

    List<Object[]> contarPorEntidad(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            List<String> tiposEvento,
            String operador,
            String texto);
}
