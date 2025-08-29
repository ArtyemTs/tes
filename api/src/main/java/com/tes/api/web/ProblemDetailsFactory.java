package com.tes.api.web;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@Component
public class ProblemDetailsFactory {

    private final MessageSource messages;

    public ProblemDetailsFactory(MessageSource messages) {
        this.messages = messages;
    }

    public ProblemDetail of(HttpStatus status, TesErrorCode code, String messageKey, Object... args) {
        Locale locale = locale();
        String msg = messages.getMessage(messageKey, args, messageKey, locale);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, msg);
        pd.setTitle(msg);
        pd.setType(URI.create("https://tes.dev/errors/" + code.code.toLowerCase()));
        pd.setInstance(currentUri());
        // ВАЖНО: кладём расширение "code"
        pd.setProperty("code", code.code);
        // Доп. корреляция для логов
        pd.setProperty("correlationId", UUID.randomUUID().toString());
        return pd;
    }

    private static URI currentUri() {
        var req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return URI.create(req.getRequestURI());
    }

    private static Locale locale() {
        var req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String h = req.getHeader("Accept-Language");
        return (h != null && h.toLowerCase().startsWith("ru")) ? new Locale("ru") : Locale.ENGLISH;
    }
}