package ru.tpu.welcomtpu.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * Связь школы с ассетом-фотографией (many-to-many с порядком отображения).
 */
@Entity
@Table(name = "school_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolAsset {

    @EmbeddedId
    private SchoolAssetId id;

    @MapsId("schoolId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @MapsId("assetId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "position", nullable = false)
    private int position;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SchoolAssetId implements Serializable {
        @Column(name = "school_id")
        private Long schoolId;

        @Column(name = "asset_id", length = 64)
        private String assetId;
    }
}
