package ru.tpu.welcomtpu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.tpu.welcomtpu.model.School;
import ru.tpu.welcomtpu.model.SchoolAsset;

import java.time.Instant;
import java.util.List;

/**
 * Ответ GET /api/v1/schools/{id} и тело для админских POST/PUT
 */
public record SchoolDto(
        Long id,
        String name,
        @JsonProperty("short_description") String shortDescription,
        @JsonProperty("full_description") String fullDescription,
        @JsonProperty("official_site_url") String officialSiteUrl,
        AssetRefDto icon,
        List<AssetRefDto> photos,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static SchoolDto from(School s) {
        return new SchoolDto(
                s.getId(),
                s.getName(),
                s.getShortDescription(),
                s.getFullDescription(),
                s.getOfficialSiteUrl(),
                AssetRefDto.from(s.getIcon()),
                s.getPhotos().stream()
                        .map(SchoolAsset::getAsset)
                        .map(AssetRefDto::from)
                        .toList(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
