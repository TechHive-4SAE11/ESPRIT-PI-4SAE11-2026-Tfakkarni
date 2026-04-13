package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.trackingservice.entity.Questionnaire;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {
}
