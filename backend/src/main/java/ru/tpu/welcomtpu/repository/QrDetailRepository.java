package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tpu.welcomtpu.model.QrDetail;

import java.util.UUID;

public interface QrDetailRepository extends JpaRepository<QrDetail, UUID> {
}
