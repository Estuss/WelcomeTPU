package ru.tpu.welcomtpu.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "statistic_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticDetail {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "card_id")
    private Card card;

    /** numeric(20, 4). */
    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal value;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(length = 100)
    private String period;

    @Column(length = 500)
    private String source;
}
