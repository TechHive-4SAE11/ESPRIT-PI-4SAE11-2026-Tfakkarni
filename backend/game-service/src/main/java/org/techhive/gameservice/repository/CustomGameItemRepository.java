package org.techhive.gameservice.repository;

import org.techhive.gameservice.entity.CustomGameItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomGameItemRepository extends JpaRepository<CustomGameItem, Long> {
  List<CustomGameItem> findByCustomGameId(Long customGameId);

  void deleteByCustomGameId(Long customGameId);
}
