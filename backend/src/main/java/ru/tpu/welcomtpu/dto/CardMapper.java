package ru.tpu.welcomtpu.dto;

import ru.tpu.welcomtpu.model.Card;

/**
 * Маппер Card -> CardDto.
 */
public final class CardMapper {

    private CardMapper() {
    }

    public static CardDto toDto(Card c) {
        return new CardDto(
                c.getId(),
                c.getType().name(),
                c.getSchool() != null ? c.getSchool().getId() : null,
                c.getTitle(),
                c.getContent(),
                c.getContentFormat().name(),
                c.getPublishedAt(),
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
