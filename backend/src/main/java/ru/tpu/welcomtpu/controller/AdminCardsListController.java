package ru.tpu.welcomtpu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.tpu.welcomtpu.dto.AdminCardDto;
import ru.tpu.welcomtpu.model.CardType;
import ru.tpu.welcomtpu.repository.CardRepository;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * GET /api/v1/admin/cards — список всех карточек, включая черновики, с фильтрами.
 */
@RestController
@RequestMapping("/api/v1/admin/cards")
@RequiredArgsConstructor
public class AdminCardsListController {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("FACT", "EVENT", "STATISTIC", "PERSON", "MEDIA", "QR");
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "title", "publishedAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final CardRepository cardRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<AdminCardDto> list(
            @RequestParam(name = "school_id", required = false) Long schoolId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created_at,desc") String sort
    ) {
        CardType cardType = parseType(type);
        Sort sortSpec = toSort(sort.split(","));
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sortSpec);

        return cardRepository
                .findForAdmin(cardType, schoolId, published,
                        search != null && !search.isBlank() ? search.trim() : null, pageable)
                .map(AdminCardDto::from);
    }

    private static CardType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String upper = type.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(upper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Недопустимый тип карточки '%s'. Разрешено: %s".formatted(type, String.join(", ", ALLOWED_TYPES)));
        }
        return CardType.valueOf(upper);
    }

    private static Sort toSort(String[] sort) {
        Sort result = Sort.unsorted();
        String field = switch (sort[0].trim().toLowerCase(Locale.ROOT)) {
                case "created_at" -> "createdAt";
                case "updated_at" -> "updatedAt";
                case "title" -> "title";
                case "published_at" -> "publishedAt";
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Недопустимое поле сортировки '%s'. Разрешено: created_at, updated_at, title, published_at"
                                .formatted(sort[0]));
            };
            Sort.Direction direction = sort.length > 1 && "desc".equalsIgnoreCase(sort[1].trim())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            result = result.and(Sort.by(direction, field));
        return result;
    }
}
