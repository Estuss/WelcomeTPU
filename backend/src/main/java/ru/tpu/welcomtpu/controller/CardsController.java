package ru.tpu.welcomtpu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.tpu.welcomtpu.dto.CardDto;
import ru.tpu.welcomtpu.dto.CardMapper;
import ru.tpu.welcomtpu.model.CardType;
import ru.tpu.welcomtpu.model.Card;
import ru.tpu.welcomtpu.repository.CardRepository;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardsController {

    // открытие сессии на время маппинга ленивых связей (open-in-view выключен)

    private static final Set<String> ALLOWED_TYPES = Set.of("FACT", "EVENT", "STATISTIC", "PERSON", "MEDIA");
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 1000;

    private final CardRepository cardRepository;

    /**
     * GET /api/v1/cards — подборка карточек.
     *
     * @param schoolId не передан -> карточки "ТПУ в целом"; передан -> карточки школы
     * @param type     FACT | EVENT | STATISTIC | PERSON | MEDIA
     * @param seed     seed для md5-перемешивания; не передан - генерируется для запроса
     *                 и возвращается в заголовке X-Seed
     */
    @GetMapping
    @Transactional(readOnly = true)
    public org.springframework.http.ResponseEntity<Page<CardDto>> list(
            @RequestParam(name = "school_id", required = false) Long schoolId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID seed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        CardType cardType = parseType(type);
        UUID effectiveSeed = seed != null ? seed : UUID.randomUUID();

        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<CardDto> result = cardRepository
                .findPublished(schoolId, cardType != null ? cardType.name() : null, effectiveSeed.toString(), pageable)
                .map(CardMapper::toDto);

        // если seed сгенерирован бэком - возвращаем его клиенту в заголовке,
        // чтобы он мог передать его при следующих страницах
        return org.springframework.http.ResponseEntity.ok()
                .header("X-Seed", effectiveSeed.toString())
                .body(result);
    }

    /**
     * GET /api/v1/cards/{id} — одна опубликованная карточка, 404 иначе.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CardDto get(@PathVariable UUID id) {
        Card card = cardRepository.findById(id)
                .filter(c -> c.getPublishedAt() != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Карточка с id=%s не найдена".formatted(id)));
        return CardMapper.toDto(card);
    }

    private static CardType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String upper = type.trim().toUpperCase();
        if (!ALLOWED_TYPES.contains(upper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Недопустимый тип карточки '%s'. Разрешено: %s".formatted(type, String.join(", ", ALLOWED_TYPES)));
        }
        return CardType.valueOf(upper);
    }
}
