package com.tes.api.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Locale;
import java.util.UUID;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private final MessageSource messages;

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    /* ---------- Helpers ---------- */

    private static HttpHeaders problemHeaders() {
        var h = new HttpHeaders();
        h.add(HttpHeaders.CONTENT_TYPE, "application/problem+json");
        return h;
    }

    private String msg(String key, Locale locale) {
        return messages.getMessage(key, null, key, locale);
    }

    private static String typeFor(TesErrorCode code) {
        return "https://tes.dev/errors/" + code.code.toLowerCase();
    }

    private TesProblemResponse body(HttpServletRequest req, HttpStatus status, TesErrorCode code, String title) {
        return new TesProblemResponse(
                typeFor(code),
                title,
                status.value(),
                title,
                req.getRequestURI(),
                code.code,
                UUID.randomUUID().toString()
        );
    }

    private static Locale locale(HttpServletRequest req) {
        var h = req.getHeader("Accept-Language");
        return (h != null && h.toLowerCase().startsWith("ru")) ? new Locale("ru") : Locale.ENGLISH;
    }

    /* ---------- 400: validation / parse ---------- */

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class })
    public ResponseEntity<?> handleValidation(HttpServletRequest req, Exception ex) {
        var loc = locale(req);
        var title = msg("error.invalid_request", loc);
        var body = body(req, HttpStatus.BAD_REQUEST, TesErrorCode.TES_001, title);
        return new ResponseEntity<>(body, problemHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class })
    public ResponseEntity<?> handleBadRequest(HttpServletRequest req, Exception ex) {
        var loc = locale(req);
        var title = msg("error.invalid_request", loc);
        var body = body(req, HttpStatus.BAD_REQUEST, TesErrorCode.TES_001, title);
        return new ResponseEntity<>(body, problemHeaders(), HttpStatus.BAD_REQUEST);
    }

    /* ---------- 429 ---------- */

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<?> handleRateLimit(HttpServletRequest req, RateLimitExceededException ex) {
        var loc = locale(req);
        var title = msg("error.rate_limited", loc);
        var body = body(req, HttpStatus.TOO_MANY_REQUESTS, TesErrorCode.TES_003, title);
        return new ResponseEntity<>(body, problemHeaders(), HttpStatus.TOO_MANY_REQUESTS);
    }

    /* ---------- 503 / 504 from ML ---------- */

    @ExceptionHandler(MlUnavailableException.class)
    public ResponseEntity<?> handleMlUnavailable(HttpServletRequest req, MlUnavailableException ex) {
        var loc = locale(req);
        var title = msg("error.ml_unavailable", loc);
        var headers = problemHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, "10");
        var body = body(req, HttpStatus.SERVICE_UNAVAILABLE, TesErrorCode.TES_002, title);
        return new ResponseEntity<>(body, headers, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(MlTimeoutException.class)
    public ResponseEntity<?> handleMlTimeout(HttpServletRequest req, MlTimeoutException ex) {
        var loc = locale(req);
        var title = msg("error.timeout", loc);
        var body = body(req, HttpStatus.GATEWAY_TIMEOUT, TesErrorCode.TES_004, title);
        return new ResponseEntity<>(body, problemHeaders(), HttpStatus.GATEWAY_TIMEOUT);
    }

    /* ---------- 500 fallback ---------- */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(HttpServletRequest req, Exception ex) {
        var loc = locale(req);
        var title = msg("error.internal", loc);
        var body = body(req, HttpStatus.INTERNAL_SERVER_ERROR, TesErrorCode.TES_000, title);
        return new ResponseEntity<>(body, problemHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}