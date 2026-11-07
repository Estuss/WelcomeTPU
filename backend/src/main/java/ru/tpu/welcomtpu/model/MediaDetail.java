package ru.tpu.welcomtpu.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Медиа: фотография или видео, подпись, краткое описание и источник.
 */
@Entity
@Table(name = "media_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaDetail {

    public enum MediaType {
        PHOTO,
        VIDEO
    }

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "card_id")
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    /** Файл в assets (для фото или превью видео). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    /** Внешняя ссылка (для видео, например YouTube/VK). */
    @Column(name = "external_url", length = 1024)
    private String externalUrl;

    @Column(length = 255)
    private String caption;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 500)
    private String source;
}
