package org.mgroko.backend.actividades.servicio;

import java.util.List;

import org.mgroko.backend.actividades.dto.ProfesionalAsignadoResponse;
import org.mgroko.backend.actividades.mapper.ActividadMapper;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiembrosActividadService {

    private final AsignacionActividadRepository asignacionActividadRepository;

    public MiembrosActividadService(
            AsignacionActividadRepository asignacionActividadRepository) {
        this.asignacionActividadRepository = asignacionActividadRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfesionalAsignadoResponse> listarExclusivos(Long idActividad) {
        return asignacionActividadRepository
                .findExclusivasByActividadId(idActividad)
                .stream()
                .map(ActividadMapper::toProfesionalAsignado)
                .toList();
    }

    @Transactional
    public void guardarMiembro(Long idActividad, Long idPerfil) {
        asignacionActividadRepository.deleteByActividadIdAndPerfil(
                idActividad, idPerfil);
    }
}
