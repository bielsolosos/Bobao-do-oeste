package br.dev.bielsolosos.biscraper.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotebookBrand {
    APPLE("Apple"),
    DELL("Dell"),
    LENOVO("Lenovo"),
    ACER("Acer"),
    ASUS("Asus"),
    HP("HP"),
    SAMSUNG("Samsung"),
    AVELL("Avell"),
    LG("LG"),
    VAIO("Vaio"),
    MSI("MSI"),
    ALIENWARE("Alienware"),
    OTHER("Outra");

    private final String description;
}
