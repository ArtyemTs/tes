package com.tes.api.web;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String msg) {
        super(msg);
    }
}
