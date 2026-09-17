package br.dev.bielsolosos.biscraper.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RamType {
    DDR1("DDR1"),
    DDR2("DDR2"),
    DDR3("DDR3"),
    DDR4("DDR4"),
    DDR5("DDR5"),
    LPDDR4("LPDDR4 / LPDDR4X"),
    LPDDR5("LPDDR5 / LPDDR5X");

    private final String description;
}
