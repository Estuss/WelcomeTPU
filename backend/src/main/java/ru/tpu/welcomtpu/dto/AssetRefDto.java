package ru.tpu.welcomtpu.dto;

import ru.tpu.welcomtpu.model.Asset;

/**
 * Краткое представление ассета (картинки) в ответах API.
 */
public record AssetRefDto(
        String id,
        String url,
        @com.fasterxml.jackson.annotation.JsonProperty("alt_text") String altText
) {
    public static AssetRefDto from(Asset a) {
        if (a == null) {
            return null;
        }
        return new AssetRefDto(a.getId(), a.getUrl(), a.getAltText());
    }
}
