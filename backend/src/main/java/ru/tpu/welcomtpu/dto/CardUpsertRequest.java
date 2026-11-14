package ru.tpu.welcomtpu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.tpu.welcomtpu.model.CardType;
import ru.tpu.welcomtpu.model.ContentFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Тело POST /api/v1/admin/cards и PUT /api/v1/admin/cards/{id}.
 * Детали (event_details, statistic_details, ...) должны соответствовать type.
 */
public record CardUpsertRequest(
        @NotNull
        String type,

        @JsonProperty("school_id")
        Long schoolId,

        @NotBlank @Size(max = 1024)
        String title,

        @NotBlank
        String content,

        @JsonProperty("content_format")
        @Size(max = 20)
        String contentFormat,

        /** null — оставить/сделать черновиком. */
        @JsonProperty("published_at")
        Instant publishedAt,

        @Valid
        CardDto.EventDetailsDto eventDetails,

        @Valid
        CardDto.StatisticDetailsDto statisticDetails,

        @Valid
        @JsonProperty("person_details")
        PersonDetailsRequest personDetails,

        @Valid
        @JsonProperty("media_details")
        MediaDetailsRequest mediaDetails,

        @Valid
        @JsonProperty("qr_details")
        QrDetailsRequest qrDetails,

        List<SourceRequest> sources,

        /** id существующих ассетов (sha256). */
        @JsonProperty("image_ids")
        List<String> imageIds
) {

    public record PersonDetailsRequest(
            @JsonProperty("full_name") @NotBlank @Size(max = 255) String fullName,
            @JsonProperty("photo_asset_id") String photoAssetId,
            String bio,
            String achievements
    ) {
    }

    public record MediaDetailsRequest(
            @JsonProperty("media_type") @NotNull String mediaType,
            @JsonProperty("asset_id") String assetId,
            @JsonProperty("external_url") @Size(max = 1024) String externalUrl,
            @Size(max = 255) String caption,
            String description,
            @Size(max = 500) String source
    ) {
    }

    public record QrDetailsRequest(
            @JsonProperty("target_url") @NotBlank @Size(max = 1024) String targetUrl,
            @Size(max = 255) String caption,
            @Size(max = 500) String source
    ) {
    }

    public record SourceRequest(
            @Size(max = 255) String title,
            @NotBlank @Size(max = 1024) String url
    ) {
    }

    private static final Set<String> TYPES = Set.of("FACT", "EVENT", "STATISTIC", "PERSON", "MEDIA", "QR");
    private static final Set<String> FORMATS = Set.of("PLAIN", "MARKDOWN", "HTML");

    public CardType parsedType() {
        String upper = type == null ? "" : type.trim().toUpperCase();
        if (!TYPES.contains(upper)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Недопустимый тип карточки '%s'. Разрешено: %s".formatted(type, String.join(", ", TYPES)));
        }
        return CardType.valueOf(upper);
    }

    public ContentFormat parsedContentFormat() {
        if (contentFormat == null || contentFormat.isBlank()) {
            return ContentFormat.PLAIN;
        }
        String upper = contentFormat.trim().toUpperCase();
        if (!FORMATS.contains(upper)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Недопустимый content_format '%s'. Разрешено: PLAIN, MARKDOWN, HTML".formatted(contentFormat));
        }
        return ContentFormat.valueOf(upper);
    }
}
