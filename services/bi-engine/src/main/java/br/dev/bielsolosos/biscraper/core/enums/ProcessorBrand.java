package br.dev.bielsolosos.biscraper.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessorBrand {
    INTEL("Intel"),
    AMD("AMD"),
    APPLE("Apple Silicon"),
    QUALCOMM("Qualcomm Snapdragon");

    private final String description;
}
