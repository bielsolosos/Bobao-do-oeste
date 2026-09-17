package br.dev.bielsolosos.biscraper.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DiskType {
    SSD("SSD (Genérico)"),
    SSD_NVME("SSD NVMe / M.2"),
    SSD_SATA("SSD SATA"),
    HDD("HD Mecânico"),
    EMMC("eMMC Flash");

    private final String description;
}
