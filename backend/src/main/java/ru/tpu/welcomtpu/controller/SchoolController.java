package ru.tpu.welcomtpu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import ru.tpu.welcomtpu.dto.SchoolDto;
import ru.tpu.welcomtpu.dto.SchoolSummaryDto;
import ru.tpu.welcomtpu.repository.SchoolRepository;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
public class SchoolController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SchoolRepository schoolRepository;

    /**
     * GET /api/v1/schools - список школ для главной страницы.
     * Поддерживает Pageable: page, size (<= 100), sort (name | created_at).
     */
    @GetMapping
    public Page<SchoolSummaryDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String[] sort
    ) {
        Sort sortSpec = toSort(sort);
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), sortSpec);

        return schoolRepository.findAll(pageable).map(SchoolSummaryDto::from);
    }

    /**
     * GET /api/v1/schools/{id} — данные школы, 404 если не найдена.
     */
    @GetMapping("/{id}")
    public SchoolDto get(@PathVariable Long id) {
        return schoolRepository.findById(id)
                .map(SchoolDto::from)
                .orElseThrow(() -> notFound(id));
    }

    private Sort toSort(String[] sort) {
        Sort result = Sort.unsorted();
        for (String s : sort) {
            String[] parts = s.split(",");
            String field = switch (parts[0].trim()) {
                case "name" -> "name";
                case "created_at" -> "createdAt";
                default -> throw badRequestSort(parts[0]);
            };
            Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            result = result.and(Sort.by(direction, field));
        }
        return result;
    }

    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Школа с id=%d не найдена".formatted(id));
    }

    private static ResponseStatusException badRequestSort(String field) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Недопустимое поле сортировки '%s'. Разрешено: name, created_at".formatted(field));
    }
}
