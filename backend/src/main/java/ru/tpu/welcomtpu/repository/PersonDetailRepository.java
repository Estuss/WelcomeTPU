package ru.tpu.welcomtpu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tpu.welcomtpu.model.PersonDetail;

import java.util.UUID;

public interface PersonDetailRepository extends JpaRepository<PersonDetail, UUID> {
}
