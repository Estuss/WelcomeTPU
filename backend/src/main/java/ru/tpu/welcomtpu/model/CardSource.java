package ru.tpu.welcomtpu.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(length = 255)
    private String title;

    @Column(nullable = false, length = 1024)
    private String url;
}
