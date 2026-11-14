package ru.tpu.welcomtpu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.tpu.welcomtpu.model.Card;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Карточка для админских эндпоинтов: включая черновики, unique_hash и служебные поля.
 */
public record AdminCardDto(
        UUID id,
        String type,
        @JsonProperty("school_id") Long schoolId,
        String title,
        String content,
        @JsonProperty("content_format") String contentFormat,
        @JsonProperty("published_at") Instant publishedAt,
        @JsonProperty("unique_hash") String uniqueHash,
        @JsonProperty("created_by_user_id") Long createdByUserId,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        List<CardDto.SourceDto> sources,
        List<AssetRefDto> images,
        @JsonProperty("event_details") CardDto.EventDetailsDto eventDetails,
        @JsonProperty("statistic_details") CardDto.StatisticDetailsDto statisticDetails,
        @JsonProperty("person_details") CardDto.PersonDetailsDto personDetails,
        @JsonProperty("media_details") CardDto.MediaDetailsDto mediaDetails,
        @JsonProperty("qr_details") CardDto.QrDetailsDto qrDetails
) {
    public static AdminCardDto from(Card c) {
        return new AdminCardDto(
                c.getId(),
                c.getType().name(),
                c.getSchool() != null ? c.getSchool().getId() : null,
                c.getTitle(),
                c.getContent(),
                c.getContentFormat().name(),
                c.getPublishedAt(),
                c.getUniqueHash(),
                c.getCreatedByUserId(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getSources().stream().map(CardDto.SourceDto::from).toList(),
                c.getCardAssets().stream().map(ca -> AssetRefDto.from(ca.getAsset())).toList(),
                CardDto.EventDetailsDto.from(c.getEventDetails()),
                CardDto.StatisticDetailsDto.from(c.getStatisticDetails()),
                CardDto.PersonDetailsDto.from(c.getPersonDetails()),
                CardDto.MediaDetailsDto.from(c.getMediaDetails()),
                CardDto.QrDetailsDto.from(c.getQrDetails())
        );
    }
}
