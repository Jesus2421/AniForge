package com.aniforge.api;

/**
 * Equivalente a la clase AnimeFLVParseError de Python (exception.py):
 *
 * class AnimeFLVParseError(Exception):
 *     pass
 */
public class AnimeFLVParseException extends Exception {
    public AnimeFLVParseException(String message) {
        super(message);
    }

    public AnimeFLVParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
