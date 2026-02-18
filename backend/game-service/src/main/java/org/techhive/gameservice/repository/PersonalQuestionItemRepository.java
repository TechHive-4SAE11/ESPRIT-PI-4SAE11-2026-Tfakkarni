package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.gameservice.entity.PersonalQuestionItem;

import java.util.List;

public interface PersonalQuestionItemRepository extends JpaRepository<PersonalQuestionItem, Long> {

  List<PersonalQuestionItem> findByGameId(Long gameId);

  long countByGameId(Long gameId);

  void deleteByGameId(Long gameId);
}
