package ru.tpu.welcomtpu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.tpu.welcomtpu.model.Card;

import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    /**
     * Публичная подборка: только опубликованные карточки.
     * schoolId == null -> карточки "ТПУ в целом" (school_id IS NULL).
     * type == null -> все типы.
     * Порядок: md5(id::text || '-' || seed) — детерминированное перемешивание по seed.
     */
    @Query(value = """
            select * from cards c
            where c.published_at is not null
              and (cast(:type as varchar) is null or c.type = :type)
              and ((:schoolId is null and c.school_id is null) or c.school_id = :schoolId)
            order by md5(c.id::text || '-' || :seed)
            """,
            countQuery = """
            select count(*) from cards c
            where c.published_at is not null
              and (cast(:type as varchar) is null or c.type = :type)
              and ((:schoolId is null and c.school_id is null) or c.school_id = :schoolId)
            """,
            nativeQuery = true)
    Page<Card> findPublished(@Param("schoolId") Long schoolId,
                             @Param("type") String type,
                             @Param("seed") String seed,
                             Pageable pageable);
}
