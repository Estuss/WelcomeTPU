package ru.tpu.welcomtpu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.tpu.welcomtpu.model.School;

/**
 * Элемент списка GET /api/v1/schools — без full_description и меток времени.
 */
public record SchoolSummaryDto(
        Long id,
        String name,
        @JsonProperty("short_description") String shortDescription,
        @JsonProperty("official_site_url") String officialSiteUrl
) {
    public static SchoolSummaryDto from(School s) {
        return new SchoolSummaryDto(
                s.getId(),
                s.getName(),
                s.getShortDescription(),
                s.getOfficialSiteUrl());
    }
}
