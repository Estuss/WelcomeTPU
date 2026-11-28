package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import ru.tpu.welcomtpu.model.School;

import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {

    /** Школа вместе с фотографиями (для маппинга вне транзакции). */
    @EntityGraph(attributePaths = {"photos", "photos.asset"})
    Optional<School> findWithPhotosById(Long id);

    @EntityGraph(attributePaths = {"photos", "photos.asset"})
    @Override
    org.springframework.data.domain.Page<School> findAll(org.springframework.data.domain.Pageable pageable);

    boolean existsByName(String name);

    java.util.Optional<School> findByNameIgnoreCase(String name);
}
