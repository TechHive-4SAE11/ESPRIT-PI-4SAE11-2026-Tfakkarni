package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.gameservice.entity.GameImage;

import java.util.List;

public interface GameImageRepository extends JpaRepository<GameImage, Long> {

    List<GameImage> findByMiniGameId(Long miniGameId);

    long countByMiniGameId(Long miniGameId);

    void deleteByMiniGameId(Long miniGameId);
}
