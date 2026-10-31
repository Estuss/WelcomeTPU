package ru.tpu.welcomtpu.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "card_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardAsset {

    @EmbeddedId
    private CardAssetId id;

    @MapsId("cardId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @MapsId("assetId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class CardAssetId implements Serializable {
        @Column(name = "card_id")
        private UUID cardId;

        @Column(name = "asset_id", length = 64)
        private String assetId;
    }
}
