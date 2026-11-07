package ru.tpu.welcomtpu.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Персона: имя, фотография, краткая биография и достижения человека, связанного с ТПУ.
 */
@Entity
@Table(name = "person_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDetail {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_asset_id")
    private Asset photo;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(columnDefinition = "text")
    private String achievements;
}
