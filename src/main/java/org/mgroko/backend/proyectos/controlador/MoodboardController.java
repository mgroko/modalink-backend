package org.mgroko.backend.proyectos.controlador;

import java.util.List;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.servicio.MoodboardImagenesService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("hasRole('USUARIO')")
public class MoodboardController {

    private final MoodboardImagenesService moodboardImagenesService;

    public MoodboardController(MoodboardImagenesService moodboardImagenesService) {
        this.moodboardImagenesService = moodboardImagenesService;
    }

    // UC-24 / UC-36: subir imágenes del moodboard. En la creación el front
    // primero llama POST /proyectos y con el id devuelto usa este endpoint;
    // en la modificación sirve para agregar imágenes nuevas.
    @PostMapping(value = "/proyectos/{idProyecto}/moodboard/imagenes",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MoodboardResponse> subirImagenes(
            @PathVariable Long idProyecto,
            @RequestParam("archivos") List<MultipartFile> archivos,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        MoodboardResponse response = moodboardImagenesService
                .subirImagenes(idUsuario, idPerfilActivo, idProyecto, archivos);
        return ResponseEntity.ok(response);
    }
}
