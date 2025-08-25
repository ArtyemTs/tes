package com.tes.api.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !"/recommendations".equals(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        byte[] bodyBytes = StreamUtils.copyToByteArray(wrapped.getInputStream());
        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        int max = Math.min(body.length(), 32_768);
        logger.info("REQ " + request.getMethod() + " " + request.getRequestURI() +
                " CT=" + request.getContentType() + " Body=" + body.substring(0, max));
        filterChain.doFilter(wrapped, response);
    }
}