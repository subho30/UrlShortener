package com.subho.urlshortner.exception;

public class CustomAliasAlreadyTakenException extends RuntimeException {
    public CustomAliasAlreadyTakenException(String message) {
        super(message);
    }
}