package ru.tpu.welcomtpu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.tpu.welcomtpu.model.CardSource;

import java.util.List;

public record CardDto(
        java.util.UUID id,
        String type,
        @JsonProperty("school_id") Long schoolId,
        String title,
        String content,
        @JsonProperty("content_format") String contentFormat,
        @JsonProperty("published_at") java.time.Instant publishedAt,
        List<SourceDto> sources,
        List<AssetRefDto> images,
        @JsonProperty("event_details") EventDetailsDto eventDetails,
        @JsonProperty("statistic_details") StatisticDetailsDto statisticDetails,
        @JsonProperty("person_details") PersonDetailsDto personDetails,
        @JsonProperty("media_details") MediaDetailsDto mediaDetails
) {

    public record SourceDto(Long id, String title, String url) {
        public static SourceDto from(CardSource s) {
            return new SourceDto(s.getId(), s.getTitle(), s.getUrl());
        }
    }

    public record EventDetailsDto(
            @JsonProperty("start_date") java.time.LocalDate startDate,
            @JsonProperty("end_date") java.time.LocalDate endDate,
            @JsonProperty("start_time") java.time.LocalTime startTime,
            @JsonProperty("end_time") java.time.LocalTime endTime,
            String location,
            @JsonProperty("details_url") String detailsUrl
    ) {
        public static EventDetailsDto from(ru.tpu.welcomtpu.model.EventDetail e) {
            if (e == null) {
                return null;
            }
            return new EventDetailsDto(e.getStartDate(), e.getEndDate(), e.getStartTime(),
                    e.getEndTime(), e.getLocation(), e.getDetailsUrl());
        }
    }

    public record StatisticDetailsDto(
            java.math.BigDecimal value,
            String unit,
            String period,
            String source
    ) {
        public static StatisticDetailsDto from(ru.tpu.welcomtpu.model.StatisticDetail s) {
            if (s == null) {
                return null;
            }
            return new StatisticDetailsDto(s.getValue(), s.getUnit(), s.getPeriod(), s.getSource());
        }
    }

    public record PersonDetailsDto(
            @JsonProperty("full_name") String fullName,
            AssetRefDto photo,
            String bio,
            String achievements
    ) {
        public static PersonDetailsDto from(ru.tpu.welcomtpu.model.PersonDetail p) {
            if (p == null) {
                return null;
            }
            return new PersonDetailsDto(p.getFullName(), AssetRefDto.from(p.getPhoto()),
                    p.getBio(), p.getAchievements());
        }
    }

    public record MediaDetailsDto(
            @JsonProperty("media_type") String mediaType,
            AssetRefDto asset,
            @JsonProperty("external_url") String externalUrl,
            String caption,
            String description,
            String source
    ) {
        public static MediaDetailsDto from(ru.tpu.welcomtpu.model.MediaDetail m) {
            if (m == null) {
                return null;
            }
            return new MediaDetailsDto(m.getMediaType().name(), AssetRefDto.from(m.getAsset()),
                    m.getExternalUrl(), m.getCaption(), m.getDescription(), m.getSource());
        }
    }
}
