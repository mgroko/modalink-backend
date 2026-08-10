package org.mgroko.backend.auth;

public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String message) {
        super(message);
    }
}