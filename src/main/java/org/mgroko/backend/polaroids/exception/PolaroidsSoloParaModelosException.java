package org.mgroko.backend.polaroids.exception;

public class PolaroidsSoloParaModelosException extends RuntimeException {

    public PolaroidsSoloParaModelosException() {
        super("Solo los perfiles con profesión Modelo pueden tener polaroids.");
    }
}
