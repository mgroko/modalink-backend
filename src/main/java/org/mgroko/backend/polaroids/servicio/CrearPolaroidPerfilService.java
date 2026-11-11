package org.mgroko.backend.polaroids.servicio;

import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.modelo.TipoPolaroid;
import org.mgroko.backend.polaroids.dto.CrearPolaroidRequest;
import org.mgroko.backend.polaroids.dto.PolaroidResponse;
import org.mgroko.backend.polaroids.mapper.PolaroidMapper;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.PolaroidPerfilRepository;
import org.mgroko.backend.storage.dto.ArchivoAlmacenado;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.storage.servicio.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearPolaroidPerfilService {

    private final PolaroidDatosService polaroidDatosService;
    private final PolaroidCatalogoService polaroidCatalogoService;
    private final PolaroidPerfilRepository polaroidPerfilRepository;
    private final ImagenRepository imagenRepository;
    private final StorageService storageService;
    private final BorradoArchivosDiferido borradoArchivosDiferido;

    public CrearPolaroidPerfilService(
            PolaroidDatosService polaroidDatosService,
            PolaroidCatalogoService polaroidCatalogoService,
            PolaroidPerfilRepository polaroidPerfilRepository,
            ImagenRepository imagenRepository,
            StorageService storageService,
            BorradoArchivosDiferido borradoArchivosDiferido) {
        this.polaroidDatosService = polaroidDatosService;
        this.polaroidCatalogoService = polaroidCatalogoService;
        this.polaroidPerfilRepository = polaroidPerfilRepository;
        this.imagenRepository = imagenRepository;
        this.storageService = storageService;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
    }

    @Transactional
    public PolaroidResponse crear(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPerfil,
            CrearPolaroidRequest request) {

        Perfil perfil = polaroidDatosService.bloquearPerfilPropioModelo(
                idUsuario, idPerfil, idPerfilActivo);

        polaroidDatosService.validarArchivoObligatorio(request.getArchivo());
        polaroidDatosService.validarLimite(
                polaroidPerfilRepository.countByPerfilIdPerfil(idPerfil));

        TipoPolaroid tipo = polaroidDatosService.resolverTipo(request.getIdTipoPolaroid());

        ArchivoAlmacenado almacenado = storageService.almacenarImagenPolaroid(request.getArchivo());
        borradoArchivosDiferido.registrarBorrados(almacenado.nombreArchivo(), null);

        Imagen imagen = imagenRepository.save(Imagen.builder()
                .nombreArchivo(almacenado.nombreArchivo())
                .url(almacenado.url())
                .tipoImagen(almacenado.tipoMime())
                .tamanoBytes((int) almacenado.tamanoBytes())
                .estado("Activa")
                .build());

        int orden = polaroidPerfilRepository.obtenerMaximoOrden(idPerfil) + 1;

        PolaroidPerfil polaroid = polaroidPerfilRepository.save(PolaroidPerfil.builder()
                .perfil(perfil)
                .imagen(imagen)
                .tipoPolaroid(tipo)
                .orden(orden)
                .build());

        return PolaroidMapper.toResponse(polaroid, polaroidCatalogoService);
    }
}
