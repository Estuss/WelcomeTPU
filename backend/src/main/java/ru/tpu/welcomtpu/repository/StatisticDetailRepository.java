package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tpu.welcomtpu.model.StatisticDetail;

import java.util.UUID;

public interface StatisticDetailRepository extends JpaRepository<StatisticDetail, UUID> {
}
