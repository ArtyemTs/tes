package com.tes.api.web.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tes.api.web.TesErrorCode;
import com.tes.api.web.TesProblemResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> buckets;
    private final long capacity;
    private final Duration refillPeriod;
    private final ObjectMapper om = new ObjectMapper();

    public RateLimitFilter(
            @Value("${ratelimit.per-ip.capacity:60}") long capacity,
            @Value("${ratelimit.per-ip.refill-period:PT1H}") Duration refillPeriod
    ) {
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
        this.buckets = Caffeine.newBuilder().maximumSize(10_000).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/openapi")) {
            chain.doFilter(req, resp);
            return;
        }

        Bucket bucket = buckets.asMap().computeIfAbsent(clientKey(req), k -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        setRateHeaders(resp, probe);

        if (probe.isConsumed()) {
            chain.doFilter(req, resp);
            return;
        }

        // Лимит превышен — 429 + Problem+JSON
        resp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        resp.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        String title = localizedTitle(req);
        TesProblemResponse body = new TesProblemResponse(
                "https://tes.dev/errors/" + TesErrorCode.TES_003.code.toLowerCase(),
                title,
                HttpStatus.TOO_MANY_REQUESTS.value(),
                title,
                req.getRequestURI(),
                TesErrorCode.TES_003.code,
                UUID.randomUUID().toString()
        );
        om.writeValue(resp.getOutputStream(), body);
    }

    private Bucket newBucket() {
        Refill refill = Refill.intervally(capacity, refillPeriod);
        Bandwidth band = Bandwidth.classic(capacity, refill);
        return Bucket.builder().addLimit(band).build();
    }

    private static String clientKey(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private void setRateHeaders(HttpServletResponse resp, ConsumptionProbe probe) {
        resp.setHeader("X-RateLimit-Limit", String.valueOf(this.capacity));
        resp.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, probe.getRemainingTokens())));
        long resetSec = probe.getNanosToWaitForRefill() > 0 ? probe.getNanosToWaitForRefill() / 1_000_000_000L : 0L;
        resp.setHeader("X-RateLimit-Reset", String.valueOf(resetSec));
    }

    private String localizedTitle(HttpServletRequest req) {
        Locale loc = locale(req);
        return loc.getLanguage().startsWith("ru")
                ? "Слишком много запросов, пожалуйста, снизьте частоту"
                : "Too many requests, please slow down";
    }

    private static Locale locale(HttpServletRequest req) {
        String h = req.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        return (h != null && h.toLowerCase().startsWith("ru")) ? new Locale("ru") : Locale.ENGLISH;
    }
}