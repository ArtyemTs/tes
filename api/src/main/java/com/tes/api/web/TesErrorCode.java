package com.tes.api.web;

public enum TesErrorCode {
    TES_000("TES-000"), // Internal error
    TES_001("TES-001"), // Invalid request (validation)
    TES_002("TES-002"), // ML unavailable
    TES_003("TES-003"), // Rate limited
    TES_004("TES-004"); // Timeout

    public final String code;
    TesErrorCode(String code) { this.code = code; }
}