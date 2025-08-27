package com.tes.api.web;

public class MlUnavailableException extends RuntimeException {
    public MlUnavailableException(String message, Throwable cause) { super(message, cause); }
    public MlUnavailableException(String message) { super(message); }
}