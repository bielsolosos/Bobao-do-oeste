package br.dev.bielsolosos.biscraper.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LlmModelEnum {

    GEMINI_2_5_FLASH("gemini-2.5-flash", "Gemini 2.5 Flash"),
    GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite"),
    GEMINI_1_5_FLASH("gemini-1.5-flash", "Gemini 1.5 Flash"),
    GEMINI_1_5_PRO("gemini-1.5-pro", "Gemini 1.5 Pro");

    private final String model;
    private final String description;
}
