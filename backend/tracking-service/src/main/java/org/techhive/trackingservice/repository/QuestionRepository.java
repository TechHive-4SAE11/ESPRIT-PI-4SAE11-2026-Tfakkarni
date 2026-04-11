package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.trackingservice.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
