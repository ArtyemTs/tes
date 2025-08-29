package com.tes.api.web;

public class MlTimeoutException extends RuntimeException {
    public MlTimeoutException(String message, Throwable cause) { super(message, cause); }
    public MlTimeoutException(String message) { super(message); }
}