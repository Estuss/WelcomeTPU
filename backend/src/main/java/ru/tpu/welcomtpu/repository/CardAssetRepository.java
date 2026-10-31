package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tpu.welcomtpu.model.CardAsset;

public interface CardAssetRepository extends JpaRepository<CardAsset, CardAsset.CardAssetId> {
}
