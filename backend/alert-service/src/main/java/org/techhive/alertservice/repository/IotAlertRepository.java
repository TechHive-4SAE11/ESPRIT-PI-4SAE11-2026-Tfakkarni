package org.techhive.alertservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.alertservice.entity.IotAlert;

import java.util.List;

public interface IotAlertRepository extends JpaRepository<IotAlert, Long> {

    List<IotAlert> findByPatientIdOrderByCreatedAtDesc(String patientId);

    List<IotAlert> findByPatientIdAndAcknowledgedFalse(String patientId);
}
