package org.mgroko.backend.perfiles.servicio;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.CaracteristicaPerfilId;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.CaracteristicaPerfilRequest;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.exception.CaracteristicaDuplicateException;
import org.mgroko.backend.perfiles.exception.CaracteristicaNoEncontradaException;
import org.mgroko.backend.perfiles.exception.CaracteristicaProfesionNoCoincideException;
import org.mgroko.backend.perfiles.exception.PerfilDuplicadoException;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.mapper.PerfilMapper;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearPerfilService {

    private final UsuarioRepository usuarioRepository;
    private final ProfesionRepository profesionRepository;
    private final CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;
    private final PerfilRepository perfilRepository;

    public CrearPerfilService(UsuarioRepository usuarioRepository,
            ProfesionRepository profesionRepository,
            CaracteristicaTecnicaRepository caracteristicaTecnicaRepository,
            PerfilRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.profesionRepository = profesionRepository;
        this.caracteristicaTecnicaRepository = caracteristicaTecnicaRepository;
        this.perfilRepository = perfilRepository;
    }

    @Transactional
    public PerfilResponse crear(Long idUsuario, CrearPerfilRequest request) {
        Usuario usuario = buscarUsuarioActivo(idUsuario);

        Profesion profesion = profesionRepository.findById(request.idProfesion())
                .orElseThrow(() -> new ProfesionNoEncontradaException(
                        "Profesión no encontrada: " + request.idProfesion()));

        verificarPerfilUnico(idUsuario, profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico(request.nombreArtistico())
                .biografia(request.biografia())
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();

        agregarCaracteristicas(perfil, profesion, request.caracteristicas());

        Perfil guardado = perfilRepository.save(perfil);
        return PerfilMapper.toResponse(guardado);
    }

    private Usuario buscarUsuarioActivo(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
        if (!usuario.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }
        return usuario;
    }

    private void verificarPerfilUnico(Long idUsuario, Profesion profesion) {
        boolean duplicado = perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                idUsuario, profesion.getIdProfesion(), EstadoPerfil.Baja);
        if (duplicado) {
            throw new PerfilDuplicadoException(
                    "El usuario ya posee un perfil para la profesión indicada.");
        }
    }

    private void agregarCaracteristicas(Perfil perfil, Profesion profesion,
                                        List<CaracteristicaPerfilRequest> caracteristicas) {
        if (caracteristicas == null || caracteristicas.isEmpty()) {
            return;
        }

        Set<Long> vistas = new HashSet<>();
        for (CaracteristicaPerfilRequest car : caracteristicas) {
            if (!vistas.add(car.idCaracteristica())) {
                throw new CaracteristicaDuplicateException(
                        "La característica técnica " + car.idCaracteristica() + " está duplicada.");
            }

            CaracteristicaTecnica ct = caracteristicaTecnicaRepository.findById(car.idCaracteristica())
                    .orElseThrow(() -> new CaracteristicaNoEncontradaException(
                            "Característica técnica no encontrada: " + car.idCaracteristica()));

            if (!ct.getProfesion().getIdProfesion().equals(profesion.getIdProfesion())) {
                throw new CaracteristicaProfesionNoCoincideException(
                        "La característica técnica " + car.idCaracteristica()
                                + " no corresponde a la profesión del perfil.");
            }

            CaracteristicaPerfil cp = CaracteristicaPerfil.builder()
                    .id(new CaracteristicaPerfilId(null, ct.getIdCaracteristica()))
                    .perfil(perfil)
                    .caracteristicaTecnica(ct)
                    .valor(car.valor())
                    .fechaRegistro(LocalDate.now())
                    .build();
            perfil.getCaracteristicas().add(cp);
        }
    }
}