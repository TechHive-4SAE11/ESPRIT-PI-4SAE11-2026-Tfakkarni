package org.techhive.trackingservice.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.trackingservice.entity.IncidentEntry;
public interface IncidentEntryRepository extends JpaRepository<IncidentEntry, Long> {
}
