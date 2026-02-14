package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.gameservice.dto.GameAttemptRequest;
import org.techhive.gameservice.dto.GameAttemptResponse;
import org.techhive.gameservice.dto.GameDetailResponse;
import org.techhive.gameservice.entity.GameAttempt;
import org.techhive.gameservice.entity.GameImage;
import org.techhive.gameservice.entity.MiniGame;
import org.techhive.gameservice.repository.GameAttemptRepository;
import org.techhive.gameservice.repository.GameImageRepository;
import org.techhive.gameservice.repository.MiniGameRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamePlayService {

    private final MiniGameRepository miniGameRepository;
    private final GameImageRepository gameImageRepository;
    private final GameAttemptRepository gameAttemptRepository;

    /**
     * Get a game formatted for gameplay: images with Base64 data,
     * and a shuffled list of all possible name choices.
     * The correct mapping is NOT included — only the image + available choices.
     */
    public Map<String, Object> getGameForPlay(Long gameId) {
        MiniGame game = miniGameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));

        List<GameImage> images = gameImageRepository.findByMiniGameId(gameId);
        if (images.isEmpty()) {
            throw new RuntimeException("Game has no images: " + gameId);
        }

        // Collect all possible name choices
        List<String> allNames = images.stream()
                .map(GameImage::getName)
                .collect(Collectors.toList());

        // Shuffle images for gameplay
        List<GameImage> shuffled = new ArrayList<>(images);
        Collections.shuffle(shuffled);

        List<Map<String, Object>> imageList = shuffled.stream()
                .map(img -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", img.getId());
                    m.put("imageBase64", Base64.getEncoder().encodeToString(img.getImageData()));
                    m.put("contentType", img.getImageContentType());
                    return m;
                })
                .collect(Collectors.toList());

        // Shuffle name choices
        List<String> shuffledNames = new ArrayList<>(allNames);
        Collections.shuffle(shuffledNames);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gameId", game.getId());
        result.put("title", game.getTitle());
        result.put("description", game.getDescription());
        result.put("images", imageList);
        result.put("choices", shuffledNames);
        result.put("totalQuestions", images.size());

        return result;
    }

    /**
     * Submit answers for a game and compute score.
     */
    @Transactional
    public GameAttemptResponse submitAnswers(Long gameId, String playerKeycloakId, GameAttemptRequest request) {
        MiniGame game = miniGameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));

        // Build a lookup of image id -> correct name
        Map<Long, String> correctAnswers = gameImageRepository.findByMiniGameId(gameId).stream()
                .collect(Collectors.toMap(GameImage::getId, GameImage::getName));

        int score = 0;
        List<GameAttemptResponse.AnswerResult> results = new ArrayList<>();

        for (GameAttemptRequest.AnswerEntry answer : request.getAnswers()) {
            String correctName = correctAnswers.get(answer.getImageId());
            boolean isCorrect = correctName != null && correctName.equalsIgnoreCase(answer.getSelectedName());
            if (isCorrect) score++;

            results.add(new GameAttemptResponse.AnswerResult(
                    answer.getImageId(),
                    correctName,
                    answer.getSelectedName(),
                    isCorrect
            ));
        }

        int totalQuestions = correctAnswers.size();

        // Save the attempt
        GameAttempt attempt = new GameAttempt(game, playerKeycloakId, score, totalQuestions, request.getDurationSeconds());
        attempt = gameAttemptRepository.save(attempt);

        log.info("Player '{}' scored {}/{} on game {} in {}s",
                playerKeycloakId, score, totalQuestions, gameId, request.getDurationSeconds());

        GameAttemptResponse response = new GameAttemptResponse();
        response.setAttemptId(attempt.getId());
        response.setScore(score);
        response.setTotalQuestions(totalQuestions);
        response.setDurationSeconds(request.getDurationSeconds());
        response.setPercentage(totalQuestions > 0 ? (double) score / totalQuestions * 100 : 0);
        response.setResults(results);
        response.setCompletedAt(attempt.getCompletedAt());

        return response;
    }
}
