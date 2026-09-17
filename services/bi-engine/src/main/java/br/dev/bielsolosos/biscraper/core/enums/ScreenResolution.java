package br.dev.bielsolosos.biscraper.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ScreenResolution {
    HD("1366x768", "HD (1366x768 / 720p)"),
    FULL_HD("1920x1080", "Full HD (1080p)"),
    WUXGA("1920x1200", "WUXGA 16:10 (1200p)"),
    QHD_2K("2560x1440", "2K / QHD (1440p)"),
    WQXGA_2K("2560x1600", "2.5K / WQXGA 16:10 (1600p)"),
    UHD_4K("3840x2160", "4K Ultra HD (2160p)"),
    RETINA("Retina", "Apple Retina / Liquid Retina");

    private final String dimensions;
    private final String description;
}
