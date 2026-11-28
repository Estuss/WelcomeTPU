package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tpu.welcomtpu.model.SchoolAsset;

import java.util.List;

public interface SchoolAssetRepository extends JpaRepository<SchoolAsset, SchoolAsset.SchoolAssetId> {

    List<SchoolAsset> findBySchoolIdOrderByPositionAsc(Long schoolId);
}
