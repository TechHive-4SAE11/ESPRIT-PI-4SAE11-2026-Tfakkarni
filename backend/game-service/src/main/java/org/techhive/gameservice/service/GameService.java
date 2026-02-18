package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.GameImage;
import org.techhive.gameservice.entity.MiniGame;
import org.techhive.gameservice.repository.MiniGameRepository;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final MiniGameRepository miniGameRepository;

    /**
     * Create a new minigame for a patient.
     */
    @Transactional
    public GameResponse createGame(String patientKeycloakId, CreateGameRequest request) {
        MiniGame game = new MiniGame(patientKeycloakId, request.getTitle(), request.getDescription());
        game = miniGameRepository.save(game);
        log.info("Created minigame '{}' (id={}) for patient '{}'", game.getTitle(), game.getId(), patientKeycloakId);

        return toGameResponse(game, 0);
    }

    /**
     * Upload images for an existing minigame.
     */
    @Transactional
    public GameDetailResponse addImages(Long gameId, List<GameImageUpload> uploads) {
        MiniGame game = miniGameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));

        int currentOrder = game.getImages().size();

        for (GameImageUpload upload : uploads) {
            byte[] imageData = Base64.getDecoder().decode(upload.getImageBase64());
            GameImage image = new GameImage(game, upload.getName(), imageData, upload.getContentType(), currentOrder++);
            game.getImages().add(image);
        }

        miniGameRepository.save(game);
        log.info("Added {} images to game {} (total: {})", uploads.size(), gameId, game.getImages().size());

        return toGameDetailResponse(game);
    }

    /**
     * List all games for a specific patient.
     */
    public List<GameResponse> getGamesByPatient(String patientKeycloakId) {
        return miniGameRepository.findByPatientKeycloakId(patientKeycloakId).stream()
                .map(game -> toGameResponse(game, game.getImages().size()))
                .collect(Collectors.toList());
    }

    /**
     * List all games (admin view).
     */
    public List<GameResponse> getAllGames() {
        return miniGameRepository.findAll().stream()
                .map(game -> toGameResponse(game, game.getImages().size()))
                .collect(Collectors.toList());
    }

    /**
     * Get full game detail including images (for management, not gameplay).
     */
    public GameDetailResponse getGameDetail(Long gameId) {
        MiniGame game = miniGameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));
        return toGameDetailResponse(game);
    }

    /**
     * Edit an existing minigame: update title/description and replace images.
     * Existing images (with id) are kept/renamed; missing old images are removed;
     * new images (id == null) are added.
     */
    @Transactional
    public GameDetailResponse editGame(Long gameId, EditGameRequest request) {
        MiniGame game = miniGameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));

        game.setTitle(request.getTitle());
        game.setDescription(request.getDescription());

        // Determine which existing images to keep
        java.util.Set<Long> keepIds = new java.util.HashSet<>();
        if (request.getImages() != null) {
            for (EditGameRequest.EditImageEntry entry : request.getImages()) {
                if (entry.getId() != null) {
                    keepIds.add(entry.getId());
                }
            }
        }

        // Remove images not in the keep set
        game.getImages().removeIf(img -> !keepIds.contains(img.getId()));

        // Update names of kept images & add new ones
        if (request.getImages() != null) {
            int order = 0;
            for (EditGameRequest.EditImageEntry entry : request.getImages()) {
                if (entry.getId() != null) {
                    // Existing image — find and update name/order
                    for (GameImage img : game.getImages()) {
                        if (img.getId().equals(entry.getId())) {
                            img.setName(entry.getName());
                            img.setDisplayOrder(order);
                            break;
                        }
                    }
                } else {
                    // New image
                    byte[] imageData = Base64.getDecoder().decode(entry.getImageBase64());
                    GameImage newImg = new GameImage(game, entry.getName(), imageData,
                            entry.getContentType(), order);
                    game.getImages().add(newImg);
                }
                order++;
            }
        }

        miniGameRepository.save(game);
        log.info("Edited game {} (now {} images)", gameId, game.getImages().size());
        return toGameDetailResponse(game);
    }

    /**
     * Delete a minigame and all its images.
     */
    @Transactional
    public void deleteGame(Long gameId) {
        if (!miniGameRepository.existsById(gameId)) {
            throw new RuntimeException("Game not found: " + gameId);
        }
        miniGameRepository.deleteById(gameId);
        log.info("Deleted game {}", gameId);
    }

    // ─── Helpers ───────────────────────────────────────────────────────

    private GameResponse toGameResponse(MiniGame game, int imageCount) {
        return new GameResponse(
                game.getId(),
                game.getPatientKeycloakId(),
                game.getTitle(),
                game.getDescription(),
                imageCount,
                game.getCreatedAt());
    }

    private GameDetailResponse toGameDetailResponse(MiniGame game) {
        GameDetailResponse response = new GameDetailResponse();
        response.setId(game.getId());
        response.setPatientKeycloakId(game.getPatientKeycloakId());
        response.setTitle(game.getTitle());
        response.setDescription(game.getDescription());
        response.setCreatedAt(game.getCreatedAt());
        response.setImages(game.getImages().stream()
                .map(img -> new GameDetailResponse.ImageDetail(
                        img.getId(),
                        img.getName(),
                        Base64.getEncoder().encodeToString(img.getImageData()),
                        img.getImageContentType(),
                        img.getDisplayOrder()))
                .collect(Collectors.toList()));
        return response;
    }
}
