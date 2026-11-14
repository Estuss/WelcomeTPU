package ru.tpu.welcomtpu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.tpu.welcomtpu.model.Asset;

import java.time.Instant;

/**
 * Полное представление ассета (ответ на загрузку изображения).
 */
public record AssetDto(
        String id,
        String url,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("size_bytes") Long sizeBytes,
        Integer width,
        Integer height,
        @JsonProperty("alt_text") String altText,
        @JsonProperty("created_at") Instant createdAt
) {
    public static AssetDto from(Asset a) {
        return new AssetDto(a.getId(), a.getUrl(), a.getMimeType(), a.getSizeBytes(),
                a.getWidth(), a.getHeight(), a.getAltText(), a.getCreatedAt());
    }
}
