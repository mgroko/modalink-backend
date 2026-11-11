package org.mgroko.backend.polaroids.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

public record ReordenarPolaroidsRequest(
        @NotEmpty(message = "Debés indicar el orden de las polaroids.")
        List<@Positive(message = "La polaroid indicada no es válida.") Long> idsPolaroid
) {
}
