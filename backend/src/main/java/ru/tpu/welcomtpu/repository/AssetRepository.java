package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tpu.welcomtpu.model.Asset;

public interface AssetRepository extends JpaRepository<Asset, String> {
}
