package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tpu.welcomtpu.model.EventDetail;

import java.util.UUID;

public interface EventDetailRepository extends JpaRepository<EventDetail, UUID> {
}
