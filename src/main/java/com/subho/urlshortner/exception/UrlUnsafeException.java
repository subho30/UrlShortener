package com.subho.urlshortner.exception;

public class UrlUnsafeException extends RuntimeException {
    public UrlUnsafeException(String message) {
        super(message);
    }
}