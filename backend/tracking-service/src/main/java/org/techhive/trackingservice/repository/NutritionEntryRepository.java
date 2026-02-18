package org.techhive.trackingservice.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.trackingservice.entity.NutritionEntry;
public interface NutritionEntryRepository extends JpaRepository<NutritionEntry, Long> {}