package ru.tpu.welcomtpu.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "short_description", columnDefinition = "text")
    private String shortDescription;

    @Column(name = "full_description", nullable = false, columnDefinition = "text")
    private String fullDescription;

    @Column(name = "official_site_url", length = 1024)
    private String officialSiteUrl;

    /** Иконка школы (файл в assets). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icon_asset_id")
    private Asset icon;

    /** Фотографии школы (файлы в assets), порядок — по position. */
    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    @Builder.Default
    private List<SchoolAsset> photos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
