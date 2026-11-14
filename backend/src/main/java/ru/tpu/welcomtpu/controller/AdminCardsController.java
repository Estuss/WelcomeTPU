package ru.tpu.welcomtpu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.tpu.welcomtpu.dto.AdminCardDto;
import ru.tpu.welcomtpu.dto.CardUpsertRequest;
import ru.tpu.welcomtpu.model.*;
import ru.tpu.welcomtpu.repository.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * CRUD карточек для админов: POST / PUT / DELETE /api/v1/admin/cards.
 * Поддерживает все типы, включая новый QR.
 */
@RestController
@RequestMapping("/api/v1/admin/cards")
@RequiredArgsConstructor
public class AdminCardsController {

    private final CardRepository cardRepository;
    private final SchoolRepository schoolRepository;
    private final AssetRepository assetRepository;

    /**
     * POST /api/v1/admin/cards — создать карточку.
     * 201 Created, 400 — валидация, 409 — дубликат unique_hash.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<AdminCardDto> create(@RequestBody CardUpsertRequest req) {
        String uniqueHash = computeHash(req);
        if (cardRepository.existsByUniqueHash(uniqueHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Карточка с таким содержимым уже существует");
        }

        Card card = apply(new Card(), req);
        card.setUniqueHash(uniqueHash);
        return ResponseEntity.created(URI.create("/api/v1/admin/cards/" + card.getId()))
                .body(AdminCardDto.from(cardRepository.save(card)));
    }

    /**
     * PUT /api/v1/admin/cards/{id} — полное обновление. 200 OK, 404 если нет.
     */
    @PutMapping("/{id}")
    @Transactional
    public AdminCardDto update(@PathVariable UUID id, @RequestBody CardUpsertRequest req) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> notFound(id));

        String uniqueHash = computeHash(req);
        cardRepository.findByUniqueHash(uniqueHash)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Карточка с таким содержимым уже существует (id=%s)".formatted(other.getId()));
                });

        apply(card, req);
        card.setUniqueHash(uniqueHash);
        return AdminCardDto.from(cardRepository.save(card));
    }

    /**
     * DELETE /api/v1/admin/cards/{id} — удалить карточку (детали удаляются каскадом). 204.
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        cardRepository.delete(card);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------

    private Card apply(Card card, CardUpsertRequest req) {
        CardType type = req.parsedType();
        card.setType(type);
        card.setSchool(req.schoolId() != null
                ? schoolRepository.findById(req.schoolId())
                        .orElseThrow(() -> badRequest("Школа с id=%d не найдена".formatted(req.schoolId())))
                : null);
        card.setTitle(req.title().trim());
        card.setContent(req.content());
        card.setContentFormat(req.parsedContentFormat());
        card.setPublishedAt(req.publishedAt());

        // детали: обновляем существующие in-place (к cascading replace @MapsId-сущностей
        // Hibernate некорректно применяет orphanRemoval), создаём только при отсутствии
        switch (type) {
            case EVENT -> {
                var d = req.eventDetails();
                if (d == null) {
                    throw badRequest("Для типа EVENT обязательно поле event_details");
                }
                EventDetail e = card.getEventDetails();
                if (e == null) {
                    e = new EventDetail();
                    e.setCard(card);
                    card.setEventDetails(e);
                }
                e.setStartDate(d.startDate());
                e.setEndDate(d.endDate());
                e.setStartTime(d.startTime());
                e.setEndTime(d.endTime());
                e.setLocation(d.location());
                e.setDetailsUrl(d.detailsUrl());
            }
            case STATISTIC -> {
                var d = req.statisticDetails();
                if (d == null) {
                    throw badRequest("Для типа STATISTIC обязательно поле statistic_details");
                }
                StatisticDetail s = card.getStatisticDetails();
                if (s == null) {
                    s = new StatisticDetail();
                    s.setCard(card);
                    card.setStatisticDetails(s);
                }
                s.setValue(d.value());
                s.setUnit(d.unit());
                s.setPeriod(d.period());
                s.setSource(d.source());
            }
            case PERSON -> {
                var d = req.personDetails();
                if (d == null) {
                    throw badRequest("Для типа PERSON обязательно поле person_details");
                }
                PersonDetail p = card.getPersonDetails();
                if (p == null) {
                    p = new PersonDetail();
                    p.setCard(card);
                    card.setPersonDetails(p);
                }
                p.setFullName(d.fullName());
                p.setPhoto(d.photoAssetId() != null ? requireAsset(d.photoAssetId()) : null);
                p.setBio(d.bio());
                p.setAchievements(d.achievements());
            }
            case MEDIA -> {
                var d = req.mediaDetails();
                if (d == null) {
                    throw badRequest("Для типа MEDIA обязательно поле media_details");
                }
                MediaDetail.MediaType mediaType;
                try {
                    mediaType = MediaDetail.MediaType.valueOf(d.mediaType().trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw badRequest("Недопустимый media_type '%s'. Разрешено: PHOTO, VIDEO".formatted(d.mediaType()));
                }
                MediaDetail m = card.getMediaDetails();
                if (m == null) {
                    m = new MediaDetail();
                    m.setCard(card);
                    card.setMediaDetails(m);
                }
                m.setMediaType(mediaType);
                m.setAsset(d.assetId() != null ? requireAsset(d.assetId()) : null);
                m.setExternalUrl(d.externalUrl());
                m.setCaption(d.caption());
                m.setDescription(d.description());
                m.setSource(d.source());
            }
            case QR -> {
                var d = req.qrDetails();
                if (d == null) {
                    throw badRequest("Для типа QR обязательно поле qr_details");
                }
                QrDetail q = card.getQrDetails();
                if (q == null) {
                    q = new QrDetail();
                    q.setCard(card);
                    card.setQrDetails(q);
                }
                q.setTargetUrl(d.targetUrl());
                q.setCaption(d.caption());
                q.setSource(d.source());
            }
            case FACT -> {
                // у FACT деталей нет
            }
        }

        // источники
        card.getSources().clear();
        if (req.sources() != null) {
            req.sources().forEach(s -> card.getSources().add(CardSource.builder()
                    .card(card)
                    .title(s.title())
                    .url(s.url())
                    .build()));
        }

        // картинки
        card.getCardAssets().clear();
        if (req.imageIds() != null) {
            req.imageIds().forEach(assetId -> card.getCardAssets().add(CardAsset.builder()
                    .card(card)
                    .asset(requireAsset(assetId))
                    .build()));
        }
        return card;
    }

    private Asset requireAsset(String assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> badRequest("Ассет с id=%s не найден".formatted(assetId)));
    }

    /** sha256 от type, school_id, title, content — по схеме БД. */
    private static String computeHash(CardUpsertRequest req) {
        String data = "%s|%s|%s|%s".formatted(
                req.parsedType().name(),
                req.schoolId() != null ? req.schoolId().toString() : "",
                req.title().trim(),
                req.content());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    private static ResponseStatusException notFound(UUID id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Карточка с id=%s не найдена".formatted(id));
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
