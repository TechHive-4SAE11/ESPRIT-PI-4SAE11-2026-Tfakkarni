package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.PersonalQuestionAttempt;
import org.techhive.gameservice.entity.PersonalQuestionGame;
import org.techhive.gameservice.entity.PersonalQuestionItem;
import org.techhive.gameservice.repository.PersonalQuestionAttemptRepository;
import org.techhive.gameservice.repository.PersonalQuestionGameRepository;
import org.techhive.gameservice.repository.PersonalQuestionItemRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalQuestionService {

  private final PersonalQuestionGameRepository gameRepository;
  private final PersonalQuestionItemRepository itemRepository;
  private final PersonalQuestionAttemptRepository attemptRepository;

  @Transactional
  public PersonalQuestionGameResponse createGame(String patientKeycloakId, CreatePersonalQuestionGameRequest request) {
    PersonalQuestionGame game = new PersonalQuestionGame(patientKeycloakId, request.getTitle(),
        request.getDescription());
    game = gameRepository.save(game);

    int order = 0;
    for (CreatePersonalQuestionGameRequest.QuestionItemRequest item : request.getQuestions()) {
      PersonalQuestionItem questionItem = new PersonalQuestionItem(
          game,
          item.getQuestionText(),
          item.getCorrectAnswer(),
          order++);
      game.getItems().add(questionItem);
    }

    gameRepository.save(game);
    log.info("Created personal question game '{}' (id={}) with {} questions for patient '{}'",
        game.getTitle(), game.getId(), request.getQuestions().size(), patientKeycloakId);

    return toResponse(game);
  }

  public List<PersonalQuestionGameResponse> getGamesByPatient(String patientKeycloakId) {
    return gameRepository.findByPatientKeycloakId(patientKeycloakId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  public PersonalQuestionGameDetailResponse getGameDetail(Long gameId) {
    PersonalQuestionGame game = gameRepository.findById(gameId)
        .orElseThrow(() -> new RuntimeException("Personal question game not found: " + gameId));

    List<PersonalQuestionItem> items = itemRepository.findByGameId(gameId);
    List<PersonalQuestionGameDetailResponse.QuestionItemDetail> details = items.stream()
        .map(item -> new PersonalQuestionGameDetailResponse.QuestionItemDetail(
            item.getId(),
            item.getQuestionText(),
            item.getCorrectAnswer()))
        .collect(Collectors.toList());

    return new PersonalQuestionGameDetailResponse(
        game.getId(),
        game.getPatientKeycloakId(),
        game.getTitle(),
        game.getDescription(),
        details,
        game.getCreatedAt());
  }

  @Transactional
  public PersonalQuestionGameResponse editGame(Long gameId, EditPersonalQuestionGameRequest request) {
    PersonalQuestionGame game = gameRepository.findById(gameId)
        .orElseThrow(() -> new RuntimeException("Personal question game not found: " + gameId));

    game.setTitle(request.getTitle());
    game.setDescription(request.getDescription());

    // Clear existing items (orphanRemoval will delete them)
    game.getItems().clear();
    gameRepository.saveAndFlush(game);

    // Add updated items
    int order = 0;
    for (EditPersonalQuestionGameRequest.QuestionItemEntry entry : request.getQuestions()) {
      PersonalQuestionItem item = new PersonalQuestionItem(
          game,
          entry.getQuestionText(),
          entry.getCorrectAnswer(),
          order++);
      game.getItems().add(item);
    }

    game = gameRepository.save(game);
    log.info("Updated personal question game '{}' (id={}) — now has {} questions",
        game.getTitle(), game.getId(), request.getQuestions().size());

    return toResponse(game);
  }

  @Transactional
  public void deleteGame(Long gameId) {
    if (!gameRepository.existsById(gameId)) {
      throw new RuntimeException("Personal question game not found: " + gameId);
    }
    gameRepository.deleteById(gameId);
    log.info("Deleted personal question game {}", gameId);
  }

  public PersonalQuestionPlayData getGameForPlay(Long gameId) {
    PersonalQuestionGame game = gameRepository.findById(gameId)
        .orElseThrow(() -> new RuntimeException("Personal question game not found: " + gameId));

    List<PersonalQuestionItem> items = itemRepository.findByGameId(gameId);
    if (items.isEmpty()) {
      throw new RuntimeException("Personal question game has no questions");
    }

    // Shuffle items for play order
    List<PersonalQuestionItem> shuffled = new ArrayList<>(items);
    Collections.shuffle(shuffled);

    List<PersonalQuestionPlayData.PersonalQuestion> questions = shuffled.stream()
        .map(item -> {
          PersonalQuestionPlayData.PersonalQuestion q = new PersonalQuestionPlayData.PersonalQuestion();
          q.setItemId(item.getId());
          q.setQuestionText(item.getQuestionText());
          q.setCorrectAnswer(item.getCorrectAnswer());
          return q;
        })
        .collect(Collectors.toList());

    PersonalQuestionPlayData playData = new PersonalQuestionPlayData();
    playData.setGameId(game.getId());
    playData.setTitle(game.getTitle());
    playData.setDescription(game.getDescription());
    playData.setQuestions(questions);
    playData.setTotalQuestions(items.size());

    return playData;
  }

  /**
   * Submit self-assessed results. The patient decides if each answer was correct
   * or not,
   * so the backend just records the final score.
   */
  @Transactional
  public PersonalQuestionAttemptResponse submitResults(Long gameId, String playerKeycloakId,
      PersonalQuestionSubmitRequest request) {
    PersonalQuestionGame game = gameRepository.findById(gameId)
        .orElseThrow(() -> new RuntimeException("Personal question game not found: " + gameId));

    PersonalQuestionAttempt attempt = new PersonalQuestionAttempt(
        game, playerKeycloakId, request.getScore(), request.getTotalQuestions(), request.getDurationSeconds());
    attempt = attemptRepository.save(attempt);

    log.info("Player '{}' self-assessed {}/{} on personal question game {} in {}s",
        playerKeycloakId, request.getScore(), request.getTotalQuestions(), gameId, request.getDurationSeconds());

    PersonalQuestionAttemptResponse response = new PersonalQuestionAttemptResponse();
    response.setAttemptId(attempt.getId());
    response.setScore(request.getScore());
    response.setTotalQuestions(request.getTotalQuestions());
    response.setDurationSeconds(request.getDurationSeconds());
    response.setPercentage(request.getTotalQuestions() > 0
        ? (double) request.getScore() / request.getTotalQuestions() * 100
        : 0);
    response.setCompletedAt(attempt.getCompletedAt());

    return response;
  }

  private PersonalQuestionGameResponse toResponse(PersonalQuestionGame game) {
    return new PersonalQuestionGameResponse(
        game.getId(),
        game.getPatientKeycloakId(),
        game.getTitle(),
        game.getDescription(),
        game.getItems().size(),
        game.getCreatedAt());
  }
}
