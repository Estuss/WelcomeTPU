package ru.tpu.welcomtpu.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * QR-карточка: целевой URL, зашитый в QR-код, подпись и источник.
 */
@Entity
@Table(name = "qr_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrDetail {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "card_id")
    private Card card;

    /** URL, который кодируется в QR-код. */
    @Column(name = "target_url", nullable = false, length = 1024)
    private String targetUrl;

    @Column(length = 255)
    private String caption;

    @Column(length = 500)
    private String source;
}
