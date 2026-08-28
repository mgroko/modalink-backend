package org.mgroko.backend.perfiles.servicio;

import java.util.List;

import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.mapper.CaracteristicaTecnicaMapper;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaracteristicaTecnicaService {

    private final CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;
    private final ProfesionRepository profesionRepository;

    public CaracteristicaTecnicaService(CaracteristicaTecnicaRepository caracteristicaTecnicaRepository,
            ProfesionRepository profesionRepository) {
        this.caracteristicaTecnicaRepository = caracteristicaTecnicaRepository;
        this.profesionRepository = profesionRepository;
    }

    @Transactional(readOnly = true)
    public List<CaracteristicaTecnicaResponse> buscar(Long idProfesion, String codigo, String unidad) {
        profesionRepository.findById(idProfesion)
                .orElseThrow(() -> new ProfesionNoEncontradaException(
                        "Profesión no encontrada: " + idProfesion));

        return caracteristicaTecnicaRepository.buscar(patron(codigo), patron(unidad), idProfesion).stream()
                .map(CaracteristicaTecnicaMapper::toResponse)
                .toList();
    }

    private String patron(String valor) {
        return (valor == null || valor.isBlank()) ? "%" : "%" + valor.trim().toLowerCase() + "%";
    }
}