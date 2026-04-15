package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.*;
import org.techhive.gameservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomGameService {

  private final CustomGameRepository gameRepo;
  private final CustomGameAttemptRepository attemptRepo;
  private final PhotoMemoryRepository photoRepo;
  private final PlaceMemoryRepository placeRepo;
  private final MovieMemoryRepository movieRepo;
  private final QuestionMemoryRepository questionRepo;
  private final MemoryTagRepository tagRepo;
  private final DataPointPerformanceRepository perfRepo;
  private final PatientContextService patientContextService;

  // ===================== CRUD =====================

  @Transactional
  public CustomGameResponse createGame(String keycloakId, CreateCustomGameRequest req) {
    CustomGame game = new CustomGame();
    game.setPatientKeycloakId(keycloakId);
    game.setTitle(req.getTitle());
    game.setDescription(req.getDescription());

    List<CustomGameItem> items = new ArrayList<>();
    for (int i = 0; i < req.getItems().size(); i++) {
      var entry = req.getItems().get(i);
      items.add(new CustomGameItem(game, entry.getDataType(), entry.getDataPointId(), i));
    }
    game.setItems(items);
    game = gameRepo.save(game);
    log.info("Created custom game '{}' with {} items for patient {}", game.getTitle(), items.size(), keycloakId);
    return toResponse(game);
  }

  @Transactional
  public CustomGameResponse updateGame(Long gameId, EditCustomGameRequest req) {
    CustomGame game = gameRepo.findById(gameId)
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

    game.setTitle(req.getTitle());
    game.setDescription(req.getDescription());

    // Clear existing items (orphanRemoval will delete them)
    game.getItems().clear();

    // Add new items from the request
    for (int i = 0; i < req.getItems().size(); i++) {
      var entry = req.getItems().get(i);
      game.getItems().add(new CustomGameItem(game, entry.getDataType(), entry.getDataPointId(), i));
    }

    game = gameRepo.save(game);
    log.info("Updated custom game '{}' (id={}) with {} items", game.getTitle(), gameId, req.getItems().size());
    return toResponse(game);
  }

  public List<CustomGameResponse> getGamesForPatient(String keycloakId) {
    return gameRepo.findByPatientKeycloakId(keycloakId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  public CustomGameDetailResponse getGameDetail(Long gameId) {
    CustomGame game = gameRepo.findById(gameId)
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

    List<DataPointSummary> itemSummaries = game.getItems().stream()
        .map(item -> resolveDataPointSummary(item.getDataType(), item.getDataPointId()))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    Set<DataPointType> types = game.getItems().stream()
        .map(CustomGameItem::getDataType)
        .collect(Collectors.toSet());

    return CustomGameDetailResponse.builder()
        .id(game.getId())
        .title(game.getTitle())
        .description(game.getDescription())
        .itemTypes(types)
        .items(itemSummaries)
        .createdAt(game.getCreatedAt())
        .build();
  }

  @Transactional
  public void deleteGame(Long gameId) {
    gameRepo.deleteById(gameId);
    log.info("Deleted custom game {}", gameId);
  }

  // ===================== PLAY =====================

  public UnifiedPlayData getPlayData(Long gameId) {
    CustomGame game = gameRepo.findById(gameId)
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
    String keycloakId = game.getPatientKeycloakId();

    List<CustomGameItem> shuffled = new ArrayList<>(game.getItems());
    Collections.shuffle(shuffled);

    List<UnifiedPlayData.UnifiedPlayItem> playItems = new ArrayList<>();
    for (int i = 0; i < shuffled.size(); i++) {
      CustomGameItem item = shuffled.get(i);
      UnifiedPlayData.UnifiedPlayItem playItem = buildPlayItem(i, item.getDataType(), item.getDataPointId(),
          keycloakId);
      if (playItem != null) {
        playItems.add(playItem);
      }
    }

    return UnifiedPlayData.builder()
        .gameId(game.getId())
        .title(game.getTitle())
        .totalQuestions(playItems.size())
        .optionCount(patientContextService.getOptionCount(keycloakId))
        .gameComplexity(patientContextService.getGameComplexity(keycloakId))
        .items(playItems)
        .build();
  }

  public UnifiedPlayData getRandomPlayData(String keycloakId, Integer limit) {
    int max = limit != null ? limit : 10;

    // Gather all data points as references
    List<PhotoMemory> photos = photoRepo.findByPatientKeycloakId(keycloakId);
    List<PlaceMemory> places = placeRepo.findByPatientKeycloakId(keycloakId);
    List<MovieMemory> movies = movieRepo.findByPatientKeycloakId(keycloakId);
    List<QuestionMemory> questions = questionRepo.findByPatientKeycloakId(keycloakId);

    List<long[]> allRefs = new ArrayList<>(); // [0]=type ordinal, [1]=id
    photos.forEach(p -> allRefs.add(new long[] { DataPointType.PHOTO.ordinal(), p.getId() }));
    places.forEach(p -> allRefs.add(new long[] { DataPointType.PLACE.ordinal(), p.getId() }));
    movies.forEach(m -> allRefs.add(new long[] { DataPointType.MOVIE.ordinal(), m.getId() }));
    questions.forEach(q -> allRefs.add(new long[] { DataPointType.QUESTION.ordinal(), q.getId() }));

    // Load performance history for this patient
    List<DataPointPerformance> perfRecords = perfRepo.findByPatientKeycloakId(keycloakId);
    Map<String, DataPointPerformance> perfMap = new HashMap<>();
    for (DataPointPerformance perf : perfRecords) {
      String key = perf.getDataType().name() + ":" + perf.getDataPointId();
      perfMap.put(key, perf);
    }

    // Categorize into 3 tiers:
    // Tier 1 (highest priority): Never answered before
    // Tier 2: Last answer was WRONG
    // Tier 3 (lowest priority): Last answer was CORRECT
    List<long[]> tier1New = new ArrayList<>();
    List<long[]> tier2Wrong = new ArrayList<>();
    List<long[]> tier3Correct = new ArrayList<>();

    for (long[] ref : allRefs) {
      DataPointType type = DataPointType.values()[(int) ref[0]];
      String key = type.name() + ":" + ref[1];
      DataPointPerformance perf = perfMap.get(key);

      if (perf == null) {
        tier1New.add(ref); // Never seen before
      } else if (!perf.isLastCorrect()) {
        tier2Wrong.add(ref); // Last attempt was wrong
      } else {
        tier3Correct.add(ref); // Last attempt was correct
      }
    }

    // Shuffle within each tier for variety
    Collections.shuffle(tier1New);
    Collections.shuffle(tier2Wrong);
    Collections.shuffle(tier3Correct);

    // Select items prioritizing Tier 1 → Tier 2 → Tier 3
    List<long[]> selected = new ArrayList<>();
    for (long[] ref : tier1New) {
      if (selected.size() >= max)
        break;
      selected.add(ref);
    }
    for (long[] ref : tier2Wrong) {
      if (selected.size() >= max)
        break;
      selected.add(ref);
    }
    for (long[] ref : tier3Correct) {
      if (selected.size() >= max)
        break;
      selected.add(ref);
    }

    // Final shuffle so the tiers aren't played in strict order
    Collections.shuffle(selected);

    List<UnifiedPlayData.UnifiedPlayItem> allItems = new ArrayList<>();
    for (int i = 0; i < selected.size(); i++) {
      long[] ref = selected.get(i);
      DataPointType type = DataPointType.values()[(int) ref[0]];
      UnifiedPlayData.UnifiedPlayItem item = buildPlayItem(i, type, ref[1], keycloakId);
      if (item != null) {
        allItems.add(item);
      }
    }

    log.info("Smart random mix for {}: {} new, {} wrong, {} correct → selected {}",
        keycloakId, tier1New.size(), tier2Wrong.size(), tier3Correct.size(), selected.size());

    return UnifiedPlayData.builder()
        .gameId(null)
        .title("Random Memory Mix")
        .totalQuestions(allItems.size())
        .optionCount(patientContextService.getOptionCount(keycloakId))
        .gameComplexity(patientContextService.getGameComplexity(keycloakId))
        .items(allItems)
        .build();
  }

  @Transactional
  public UnifiedPlayResult submitResults(String playerKeycloakId, UnifiedSubmitRequest req) {
    // Find the game if specified
    CustomGame game = null;
    if (req.getGameId() != null) {
      game = gameRepo.findById(req.getGameId()).orElse(null);
    }

    // Validate MCQ answers server-side, trust self-assessed for QUESTION
    List<UnifiedPlayResult.ItemResult> results = new ArrayList<>();
    int serverScore = 0;

    if (req.getAnswers() != null) {
      for (UnifiedSubmitRequest.AnswerEntry answer : req.getAnswers()) {
        UnifiedPlayResult.ItemResult result = validateAnswer(answer);
        results.add(result);
        if (result.isCorrect())
          serverScore++;
      }
    }

    // Save attempt
    CustomGameAttempt attempt = new CustomGameAttempt();
    attempt.setCustomGame(game);
    attempt.setPlayerKeycloakId(playerKeycloakId);
    attempt.setScore(serverScore);
    attempt.setTotalQuestions(req.getTotalQuestions());
    attempt.setDurationSeconds(req.getDurationSeconds());
    attempt.setCompletedAt(LocalDateTime.now());
    attempt = attemptRepo.save(attempt);

    // Update per-data-point performance records for spaced repetition
    if (req.getAnswers() != null) {
      for (int i = 0; i < req.getAnswers().size(); i++) {
        UnifiedSubmitRequest.AnswerEntry answer = req.getAnswers().get(i);
        boolean correct = i < results.size() && results.get(i).isCorrect();
        updatePerformance(playerKeycloakId, answer.getType(), answer.getItemId(), correct);
      }
    }

    double pct = req.getTotalQuestions() > 0
        ? (serverScore * 100.0 / req.getTotalQuestions())
        : 0;

    return UnifiedPlayResult.builder()
        .attemptId(attempt.getId())
        .score(serverScore)
        .totalQuestions(req.getTotalQuestions())
        .percentage(Math.round(pct * 10.0) / 10.0)
        .durationSeconds(req.getDurationSeconds())
        .completedAt(attempt.getCompletedAt())
        .results(results)
        .build();
  }

  /**
   * Update (or create) the performance record for a data point after an answer.
   * If the patient got a previously-correct item wrong, it moves to the "wrong"
   * tier.
   * If the patient got a previously-wrong item right, it moves to the "correct"
   * tier.
   */
  private void updatePerformance(String keycloakId, DataPointType type, Long dataPointId, boolean correct) {
    var existing = perfRepo.findByPatientKeycloakIdAndDataTypeAndDataPointId(keycloakId, type, dataPointId);
    if (existing.isPresent()) {
      DataPointPerformance perf = existing.get();
      perf.recordResult(correct); // Updates lastCorrect, increments correct/incorrect count
      perfRepo.save(perf);
    } else {
      perfRepo.save(new DataPointPerformance(keycloakId, type, dataPointId, correct));
    }
  }

  // ===================== STATS =====================

  public Map<String, Object> getStats(String keycloakId) {
    Map<String, Object> stats = new LinkedHashMap<>();
    long total = attemptRepo.countByPlayerKeycloakId(keycloakId);
    stats.put("totalGamesPlayed", total);
    if (total > 0) {
      stats.put("averageScore", attemptRepo.getAverageScorePercentage(keycloakId));
      stats.put("bestScore", attemptRepo.getBestScore(keycloakId));
    } else {
      stats.put("averageScore", 0.0);
      stats.put("bestScore", 0);
    }
    // data point counts
    stats.put("photoCount", photoRepo.countByPatientKeycloakId(keycloakId));
    stats.put("placeCount", placeRepo.countByPatientKeycloakId(keycloakId));
    stats.put("movieCount", movieRepo.countByPatientKeycloakId(keycloakId));
    stats.put("questionCount", questionRepo.countByPatientKeycloakId(keycloakId));
    return stats;
  }

  // ===================== HELPERS =====================

  private UnifiedPlayData.UnifiedPlayItem buildPlayItem(int index, DataPointType type, Long dataPointId,
      String keycloakId) {
    switch (type) {
      case PHOTO -> {
        return photoRepo.findById(dataPointId).map(photo -> {
          return UnifiedPlayData.UnifiedPlayItem.builder()
              .index(index)
              .type(DataPointType.PHOTO)
              .itemId(photo.getId())
              .imageBase64(Base64.getEncoder().encodeToString(photo.getImageData()))
              .imageContentType(photo.getImageContentType())
              .correctAnswer(photo.getName())
              .build();
        }).orElse(null);
      }
      case PLACE -> {
        return placeRepo.findById(dataPointId).map(place -> {
          return UnifiedPlayData.UnifiedPlayItem.builder()
              .index(index)
              .type(DataPointType.PLACE)
              .itemId(place.getId())
              .latitude(place.getLatitude())
              .longitude(place.getLongitude())
              .hint(place.getHint())
              .correctAnswer(place.getName())
              .build();
        }).orElse(null);
      }
      case MOVIE -> {
        return movieRepo.findById(dataPointId).map(movie -> {
          String posterUrl = movie.getPosterPath() != null
              ? "https://image.tmdb.org/t/p/w300" + movie.getPosterPath()
              : null;
          return UnifiedPlayData.UnifiedPlayItem.builder()
              .index(index)
              .type(DataPointType.MOVIE)
              .itemId(movie.getId())
              .posterUrl(posterUrl)
              .movieTitle(movie.getOriginalTitle())
              .correctAnswer(movie.getCorrectAnswer())
              .build();
        }).orElse(null);
      }
      case QUESTION -> {
        return questionRepo.findById(dataPointId).map(q -> UnifiedPlayData.UnifiedPlayItem.builder()
            .index(index)
            .type(DataPointType.QUESTION)
            .itemId(q.getId())
            .questionText(q.getQuestionText())
            .correctAnswer(q.getCorrectAnswer())
            .build()).orElse(null);
      }
      default -> {
        return null;
      }
    }
  }

  private List<String> buildChoices(String correct, List<String> others) {
    Set<String> choices = new LinkedHashSet<>();
    choices.add(correct);

    List<String> shuffledOthers = new ArrayList<>(others);
    Collections.shuffle(shuffledOthers);
    for (String o : shuffledOthers) {
      if (choices.size() >= 4)
        break;
      choices.add(o);
    }

    // Pad with dummy if needed
    int pad = 1;
    while (choices.size() < 2) {
      choices.add("Option " + pad++);
    }

    List<String> result = new ArrayList<>(choices);
    Collections.shuffle(result);
    return result;
  }

  private UnifiedPlayResult.ItemResult validateAnswer(UnifiedSubmitRequest.AnswerEntry answer) {
    String correctAnswer = "";
    String label = "";

    switch (answer.getType()) {
      case PHOTO -> {
        var photo = photoRepo.findById(answer.getItemId()).orElse(null);
        if (photo != null) {
          correctAnswer = photo.getName();
          label = photo.getName();
        }
      }
      case PLACE -> {
        var place = placeRepo.findById(answer.getItemId()).orElse(null);
        if (place != null) {
          correctAnswer = place.getName();
          label = place.getName();
        }
      }
      case MOVIE -> {
        var movie = movieRepo.findById(answer.getItemId()).orElse(null);
        if (movie != null) {
          correctAnswer = movie.getCorrectAnswer();
          label = movie.getOriginalTitle();
        }
      }
      case QUESTION -> {
        var q = questionRepo.findById(answer.getItemId()).orElse(null);
        if (q != null) {
          correctAnswer = q.getCorrectAnswer();
          label = q.getQuestionText();
        }
      }
    }

    // Use patient self-assessment (supports multi-language answers)
    boolean correct = Boolean.TRUE.equals(answer.getSelfAssessedCorrect());

    return UnifiedPlayResult.ItemResult.builder()
        .type(answer.getType())
        .itemId(answer.getItemId())
        .correct(correct)
        .correctAnswer(correctAnswer != null ? correctAnswer : "")
        .selectedAnswer(answer.getSelectedAnswer() != null ? answer.getSelectedAnswer() : "I don't know")
        .label(label)
        .build();
  }

  private CustomGameResponse toResponse(CustomGame game) {
    Set<DataPointType> types = game.getItems().stream()
        .map(CustomGameItem::getDataType)
        .collect(Collectors.toSet());

    return CustomGameResponse.builder()
        .id(game.getId())
        .title(game.getTitle())
        .description(game.getDescription())
        .itemCount(game.getItems().size())
        .itemTypes(types)
        .createdAt(game.getCreatedAt())
        .build();
  }

  private DataPointSummary resolveDataPointSummary(DataPointType type, Long id) {
    switch (type) {
      case PHOTO -> {
        return photoRepo.findById(id).map(p -> DataPointSummary.builder()
            .id(p.getId()).type(DataPointType.PHOTO).label(p.getName())
            .subtitle("Photo").createdAt(p.getCreatedAt())
            .imagePreview(Base64.getEncoder().encodeToString(p.getImageData()))
            .build()).orElse(null);
      }
      case PLACE -> {
        return placeRepo.findById(id).map(p -> DataPointSummary.builder()
            .id(p.getId()).type(DataPointType.PLACE).label(p.getName())
            .subtitle(p.getHint() != null ? p.getHint() : "Place")
            .createdAt(p.getCreatedAt()).build()).orElse(null);
      }
      case MOVIE -> {
        return movieRepo.findById(id).map(m -> DataPointSummary.builder()
            .id(m.getId()).type(DataPointType.MOVIE).label(m.getOriginalTitle())
            .subtitle("Character: " + m.getCorrectAnswer())
            .posterPath(m.getPosterPath()).createdAt(m.getCreatedAt()).build()).orElse(null);
      }
      case QUESTION -> {
        return questionRepo.findById(id).map(q -> DataPointSummary.builder()
            .id(q.getId()).type(DataPointType.QUESTION).label(q.getQuestionText())
            .subtitle("Answer: " + q.getCorrectAnswer())
            .createdAt(q.getCreatedAt()).build()).orElse(null);
      }
      default -> {
        return null;
      }
    }
  }
}
