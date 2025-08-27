package com.tes.api.web;

/** RFC7807-compatible body with a guaranteed "code" field. */
public record TesProblemResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String correlationId
) {}