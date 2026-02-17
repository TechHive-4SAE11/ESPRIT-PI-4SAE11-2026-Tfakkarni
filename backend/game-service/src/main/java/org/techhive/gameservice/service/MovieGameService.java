package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.MovieGame;
import org.techhive.gameservice.entity.MovieGameAttempt;
import org.techhive.gameservice.entity.MovieGameItem;
import org.techhive.gameservice.repository.MovieGameAttemptRepository;
import org.techhive.gameservice.repository.MovieGameItemRepository;
import org.techhive.gameservice.repository.MovieGameRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieGameService {

  private static final String TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w500";

  private final MovieGameRepository movieGameRepository;
  private final MovieGameItemRepository movieGameItemRepository;
  private final MovieGameAttemptRepository movieGameAttemptRepository;

  @Transactional
  public MovieGameResponse createMovieGame(String patientKeycloakId, CreateMovieGameRequest request) {
    MovieGame game = new MovieGame(patientKeycloakId, request.getTitle(), request.getDescription());
    game = movieGameRepository.save(game);

    int order = 0;
    for (CreateMovieGameRequest.MovieItemRequest item : request.getMovies()) {
      MovieGameItem gameItem = new MovieGameItem(
          game,
          item.getTmdbId(),
          item.getOriginalTitle(),
          item.getPosterPath(),
          item.getReleaseDate(),
          item.getCorrectAnswer(),
          order++);
      game.getItems().add(gameItem);
    }

    movieGameRepository.save(game);
    log.info("Created movie game '{}' (id={}) with {} movies for patient '{}'",
        game.getTitle(), game.getId(), request.getMovies().size(), patientKeycloakId);

    return toResponse(game);
  }

  public List<MovieGameResponse> getGamesByPatient(String patientKeycloakId) {
    return movieGameRepository.findByPatientKeycloakId(patientKeycloakId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  public MovieGameDetailResponse getGameDetail(Long gameId) {
    MovieGame game = movieGameRepository.findById(gameId)
        .orElseThrow(() -> new RuntimeException("Movie game not found: " + gameId));

    List<MovieGameItem> items = movieGameItemRepository.findByMovieGameId(gameId);
    List<MovieGameDetailResponse.MovieItemDetail> movieDetails = items.stream()
        .map(item -> new MovieGameDetailResponse.MovieItemDetail(
            item.getId(),
            item.getTmdbId(),
            item.getOriginalTitle(),
            item.getPosterPath(),
            item.getReleaseDate(),
            item.getCorrectAnswer()))
        .collect(Collectors.toList());

    return new MovieGameDetailResponse(
        game.getId(),
        game.getPatientKeycloakId(),
        game.getTitle(),
        game.getDescription(),
        movieDetails,
        game.getCreatedAt());
  }

  @Transactional
  public MovieGameResponse editGame(Long gameId, EditMovieGameRequest request) {
    MovieGame game = movieGameRepository.findById(gameId)
        .orElseThrow(() -> new RuntimeException("Movie game not found: " + gameId));

    game.setTitle(request.getTitle());
    game.setDescription(request.getDescription());

    // Clear existing items (orphanRemoval will delete them)
    game.getItems().clear();
    movieGameRepository.saveAndFlush(game);

    // Add updated items
    int order = 0;
    for (EditMovieGameRequest.MovieItemEntry entry : request.getMovies()) {
      MovieGameItem item = new MovieGameItem(
          game,
          entry.getTmdbId(),
          entry.getOriginalTitle(),
          entry.getPosterPath(),
          entry.getReleaseDate(),
          entry.getCorrectAnswer(),
          order++);
      game.getItems().add(item);
    }

    game = movieGameRepository.save(game);
    log.info("Updated movie game '{}' (id={}) — now has {} movies",
        game.getTitle(), game.getId(), request.getMovies().size());

    return toResponse(game);
  }

  @Transactional
  public void deleteGame(Long gameId) {
    if (!movieGameRepository.existsById(gameId)) {
      throw new RuntimeException("Movie game not found: " + gameId);
    }
    movieGameRepository.deleteById(gameId);
    log.info("Deleted movie game {}", gameId);
  }

  public MovieGamePlayData getGameForPlay(Long gameId) {
    MovieGame game = movieGameRepository.findById(gameId)
        .orElseThrow(() -> new RuntimeException("Movie game not found: " + gameId));

    List<MovieGameItem> items = movieGameItemRepository.findByMovieGameId(gameId);
    if (items.size() < 2) {
      throw new RuntimeException("Movie game must have at least 2 movies");
    }

    // Collect all correct answers for use as choices
    List<String> allAnswers = items.stream()
        .map(MovieGameItem::getCorrectAnswer)
        .collect(Collectors.toList());

    // Shuffle items for play order
    List<MovieGameItem> shuffled = new ArrayList<>(items);
    Collections.shuffle(shuffled);

    List<MovieGamePlayData.MovieQuestion> questions = shuffled.stream()
        .map(item -> {
          MovieGamePlayData.MovieQuestion q = new MovieGamePlayData.MovieQuestion();
          q.setItemId(item.getId());
          q.setPosterUrl(TMDB_IMAGE_BASE + item.getPosterPath());
          q.setMovieTitle(item.getOriginalTitle());
          q.setReleaseDate(item.getReleaseDate());

          // Build choices: correct answer + other answers shuffled
          List<String> choices = new ArrayList<>(allAnswers);
          Collections.shuffle(choices);
          q.setChoices(choices);
          return q;
        })
        .collect(Collectors.toList());

    MovieGamePlayData playData = new MovieGamePlayData();
    playData.setGameId(game.getId());
    playData.setTitle(game.getTitle());
    playData.setDescription(game.getDescription());
    playData.setQuestions(questions);
    playData.setTotalQuestions(items.size());

    return playData;
  }

  @Transactional
  public MovieGameAttemptResponse submitAnswers(Long gameId, String playerKeycloakId, MovieGameSubmitRequest request) {
    MovieGame game = movieGameRepository.findById(gameId)
        .orElseThrow(() -> new RuntimeException("Movie game not found: " + gameId));

    Map<Long, MovieGameItem> itemMap = movieGameItemRepository.findByMovieGameId(gameId).stream()
        .collect(Collectors.toMap(MovieGameItem::getId, item -> item));

    int score = 0;
    List<MovieGameAttemptResponse.MovieAnswerResult> results = new ArrayList<>();

    for (MovieGameSubmitRequest.MovieAnswerEntry answer : request.getAnswers()) {
      MovieGameItem item = itemMap.get(answer.getItemId());
      String correctAnswer = item != null ? item.getCorrectAnswer() : "";
      boolean isCorrect = correctAnswer.equalsIgnoreCase(answer.getSelectedAnswer());
      if (isCorrect)
        score++;

      results.add(new MovieGameAttemptResponse.MovieAnswerResult(
          answer.getItemId(),
          item != null ? TMDB_IMAGE_BASE + item.getPosterPath() : "",
          item != null ? item.getOriginalTitle() : "",
          correctAnswer,
          answer.getSelectedAnswer(),
          isCorrect));
    }

    int totalQuestions = itemMap.size();

    MovieGameAttempt attempt = new MovieGameAttempt(game, playerKeycloakId, score, totalQuestions,
        request.getDurationSeconds());
    attempt = movieGameAttemptRepository.save(attempt);

    log.info("Player '{}' scored {}/{} on movie game {} in {}s",
        playerKeycloakId, score, totalQuestions, gameId, request.getDurationSeconds());

    MovieGameAttemptResponse response = new MovieGameAttemptResponse();
    response.setAttemptId(attempt.getId());
    response.setScore(score);
    response.setTotalQuestions(totalQuestions);
    response.setDurationSeconds(request.getDurationSeconds());
    response.setPercentage(totalQuestions > 0 ? (double) score / totalQuestions * 100 : 0);
    response.setResults(results);
    response.setCompletedAt(attempt.getCompletedAt());

    return response;
  }

  private MovieGameResponse toResponse(MovieGame game) {
    return new MovieGameResponse(
        game.getId(),
        game.getPatientKeycloakId(),
        game.getTitle(),
        game.getDescription(),
        game.getItems().size(),
        game.getCreatedAt());
  }
}
