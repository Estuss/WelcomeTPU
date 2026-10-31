package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tpu.welcomtpu.model.CardSource;

public interface CardSourceRepository extends JpaRepository<CardSource, Long> {
}
