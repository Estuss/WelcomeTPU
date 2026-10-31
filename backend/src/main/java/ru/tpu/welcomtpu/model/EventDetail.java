package ru.tpu.welcomtpu.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;
import java.time.LocalTime;

@Entity
@Table(name = "event_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetail {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 1024)
    private String location;

    @Column(name = "details_url", length = 1024)
    private String detailsUrl;
}
