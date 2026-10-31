package ru.tpu.welcomtpu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.tpu.welcomtpu.model.Card;
import ru.tpu.welcomtpu.model.CardType;

import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    /**
     * Публичная подборка карточек: только опубликованные.
     * schoolId == null -> карточки "ТПУ в целом" (school_id IS NULL).
     */
    @Query("""
            select c from Card c
            where c.publishedAt is not null
              and (:type is null or c.type = :type)
              and (:schoolId is null and c.school is null or c.school.id = :schoolId)
            """)
    Page<Card> findPublished(
            @Param("schoolId") Long schoolId,
            @Param("type") CardType type,
            Pageable pageable);
}
