package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tpu.welcomtpu.model.MediaDetail;

import java.util.UUID;

public interface MediaDetailRepository extends JpaRepository<MediaDetail, UUID> {
}
