package ru.tpu.welcomtpu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.tpu.welcomtpu.dto.SchoolDto;
import ru.tpu.welcomtpu.model.School;
import ru.tpu.welcomtpu.repository.CardRepository;
import ru.tpu.welcomtpu.repository.SchoolRepository;

import java.net.URI;

/**
 * CRUD школ для админов.
 */
@RestController
@RequestMapping("/api/v1/admin/schools")
@RequiredArgsConstructor
public class AdminSchoolsController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SchoolRepository schoolRepository;
    private final CardRepository cardRepository;

    /**
     * GET /api/v1/admin/schools — все поля, включая full_description и метки времени.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public Page<SchoolDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return schoolRepository.findAll(pageable).map(SchoolDto::from);
    }

    /**
     * POST /api/v1/admin/schools — создать школу. 409 если name занят.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<SchoolDto> create(@RequestBody SchoolDto req) {
        validate(req);
        if (schoolRepository.existsByName(req.name().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Школа с названием '%s' уже существует".formatted(req.name().trim()));
        }
        School school = new School();
        apply(school, req);
        School saved = schoolRepository.save(school);
        return ResponseEntity.created(URI.create("/api/v1/admin/schools/" + saved.getId()))
                .body(SchoolDto.from(saved));
    }

    /**
     * PUT /api/v1/admin/schools/{id} — полное обновление. 404 если нет.
     */
    @PutMapping("/{id}")
    @Transactional
    public SchoolDto update(@PathVariable Long id, @RequestBody SchoolDto req) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        validate(req);
        schoolRepository.findByNameIgnoreCase(req.name().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Школа с названием '%s' уже существует (id=%d)".formatted(other.getName(), other.getId()));
                });
        apply(school, req);
        return SchoolDto.from(schoolRepository.save(school));
    }

    /**
     * DELETE /api/v1/admin/schools/{id} — удалить школу; карточки остаются как "ТПУ в целом". 204.
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        cardRepository.unsetSchool(id);
        schoolRepository.delete(school);
        return ResponseEntity.noContent().build();
    }

    private static void validate(SchoolDto req) {
        if (req.name() == null || req.name().isBlank()) {
            throw badRequest("Поле 'name' не может быть пустым");
        }
        if (req.fullDescription() == null || req.fullDescription().isBlank()) {
            throw badRequest("Поле 'full_description' не может быть пустым");
        }
    }

    private static void apply(School school, SchoolDto req) {
        school.setName(req.name().trim());
        school.setShortDescription(req.shortDescription());
        school.setFullDescription(req.fullDescription());
        school.setOfficialSiteUrl(req.officialSiteUrl());
    }

    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Школа с id=%d не найдена".formatted(id));
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
