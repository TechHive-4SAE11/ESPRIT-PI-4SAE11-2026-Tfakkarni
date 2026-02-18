package org.techhive.trackingservice.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.trackingservice.entity.ActivityEntry;
public interface ActivityEntryRepository extends JpaRepository<ActivityEntry, Long> {
}
