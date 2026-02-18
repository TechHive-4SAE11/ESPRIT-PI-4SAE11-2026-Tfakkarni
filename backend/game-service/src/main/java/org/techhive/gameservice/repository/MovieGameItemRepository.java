package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.gameservice.entity.MovieGameItem;

import java.util.List;

public interface MovieGameItemRepository extends JpaRepository<MovieGameItem, Long> {

  List<MovieGameItem> findByMovieGameId(Long movieGameId);

  long countByMovieGameId(Long movieGameId);

  void deleteByMovieGameId(Long movieGameId);
}
