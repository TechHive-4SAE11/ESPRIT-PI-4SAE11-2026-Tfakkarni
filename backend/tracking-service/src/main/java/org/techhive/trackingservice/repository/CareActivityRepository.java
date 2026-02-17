package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.trackingservice.entity.CareActivity;

import java.util.List;

@Repository
public interface CareActivityRepository extends JpaRepository<CareActivity, Long> {
    List<CareActivity> findByCarePlanId(Long carePlanId);
}
