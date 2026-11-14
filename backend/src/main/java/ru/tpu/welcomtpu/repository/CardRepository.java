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

    boolean existsByUniqueHash(String uniqueHash);

    java.util.Optional<Card> findByUniqueHash(String uniqueHash);

    /**
     * Список карточек для админов: черновики и опубликованные, с фильтрами.
     * type == null / schoolId == null / published == null / search == null - фильтр не применяется.
     */
    @Query("""
            select c from Card c
            where (:type is null or c.type = :type)
              and (:schoolId is null or c.school.id = :schoolId)
              and (:published is null or (:published = true and c.publishedAt is not null)
                                   or (:published = false and c.publishedAt is null))
              and (:search is null or lower(c.title) like lower(concat('%', cast(:search as string), '%'))
                                   or lower(c.content) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Card> findForAdmin(@Param("type") CardType type,
                            @Param("schoolId") Long schoolId,
                            @Param("published") Boolean published,
                            @Param("search") String search,
                            Pageable pageable);

    /** При удалении школы карточки остаются как «ТПУ в целом». */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Card c set c.school = null where c.school.id = :schoolId")
    void unsetSchool(@Param("schoolId") Long schoolId);

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
