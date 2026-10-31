package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tpu.welcomtpu.model.School;

public interface SchoolRepository extends JpaRepository<School, Long> {

    boolean existsByName(String name);
}
