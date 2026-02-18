package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.*;
import org.techhive.gameservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataPointService {

  private final PhotoMemoryRepository photoRepo;
  private final PlaceMemoryRepository placeRepo;
  private final MovieMemoryRepository movieRepo;
  private final QuestionMemoryRepository questionRepo;
  private final MemoryTagRepository tagRepo;

  // ===================== PHOTO =====================

  @Transactional
  public DataPointSummary createPhoto(String keycloakId, CreatePhotoRequest req) {
    PhotoMemory photo = new PhotoMemory();
    photo.setPatientKeycloakId(keycloakId);
    photo.setName(req.getName());
    photo.setImageData(Base64.getDecoder().decode(req.getImageBase64()));
    photo.setImageContentType(req.getContentType());
    if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
      photo.setTags(new HashSet<>(tagRepo.findAllById(req.getTagIds())));
    }
    photo = photoRepo.save(photo);
    log.info("Created photo memory '{}' for patient {}", photo.getName(), keycloakId);
    return toSummary(photo);
  }

  @Transactional
  public void deletePhoto(Long id) {
    photoRepo.deleteById(id);
  }

  // ===================== PLACE =====================

  @Transactional
  public DataPointSummary createPlace(String keycloakId, CreateMemoryPlaceRequest req) {
    PlaceMemory place = new PlaceMemory();
    place.setPatientKeycloakId(keycloakId);
    place.setName(req.getName());
    place.setLatitude(req.getLatitude());
    place.setLongitude(req.getLongitude());
    place.setHint(req.getHint());
    if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
      place.setTags(new HashSet<>(tagRepo.findAllById(req.getTagIds())));
    }
    place = placeRepo.save(place);
    log.info("Created place memory '{}' for patient {}", place.getName(), keycloakId);
    return toSummary(place);
  }

  @Transactional
  public void deletePlace(Long id) {
    placeRepo.deleteById(id);
  }

  // ===================== MOVIE =====================

  @Transactional
  public DataPointSummary createMovie(String keycloakId, CreateMovieMemoryRequest req) {
    MovieMemory movie = new MovieMemory();
    movie.setPatientKeycloakId(keycloakId);
    movie.setTmdbId(req.getTmdbId());
    movie.setOriginalTitle(req.getOriginalTitle());
    movie.setPosterPath(req.getPosterPath());
    movie.setReleaseDate(req.getReleaseDate());
    movie.setCorrectAnswer(req.getCorrectAnswer());
    if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
      movie.setTags(new HashSet<>(tagRepo.findAllById(req.getTagIds())));
    }
    movie = movieRepo.save(movie);
    log.info("Created movie memory '{}' for patient {}", movie.getOriginalTitle(), keycloakId);
    return toSummary(movie);
  }

  @Transactional
  public void deleteMovie(Long id) {
    movieRepo.deleteById(id);
  }

  // ===================== QUESTION =====================

  @Transactional
  public DataPointSummary createQuestion(String keycloakId, CreateQuestionMemoryRequest req) {
    QuestionMemory question = new QuestionMemory();
    question.setPatientKeycloakId(keycloakId);
    question.setQuestionText(req.getQuestionText());
    question.setCorrectAnswer(req.getCorrectAnswer());
    if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
      question.setTags(new HashSet<>(tagRepo.findAllById(req.getTagIds())));
    }
    question = questionRepo.save(question);
    log.info("Created question memory for patient {}", keycloakId);
    return toSummary(question);
  }

  @Transactional
  public void deleteQuestion(Long id) {
    questionRepo.deleteById(id);
  }

  // ===================== UPDATE =====================

  @Transactional
  public DataPointSummary updatePhoto(Long id, UpdateDataPointRequest req) {
    PhotoMemory photo = photoRepo.findById(id)
        .orElseThrow(() -> new RuntimeException("Photo not found: " + id));
    if (req.getName() != null)
      photo.setName(req.getName());
    if (req.getTagIds() != null)
      photo.setTags(new HashSet<>(tagRepo.findAllById(req.getTagIds())));
    photo = photoRepo.save(photo);
    return toSummary(photo);
  }

  @Transactional
  public DataPointSummary updatePlace(Long id, UpdateDataPointRequest req) {
    PlaceMemory place = placeRepo.findById(id)
        .orElseThrow(() -> new RuntimeException("Place not found: " + id));
    if (req.getName() != null)
      place.setName(req.getName());
    if (req.getHint() != null)
      place.setHint(req.getHint());
    if (req.getLatitude() != null)
      place.setLatitude(req.getLatitude());
    if (req.getLongitude() != null)
      place.setLongitude(req.getLongitude());
    if (req.getTagIds() != null)
      place.setTags(new HashSet<>(tagRepo.findAllById(req.getTagIds())));
    place = placeRepo.save(place);
    return toSummary(place);
  }

  @Transactional
  public DataPointSummary updateMovie(Long id, UpdateDataPointRequest req) {
    MovieMemory movie = movieRepo.findById(id)
        .orElseThrow(() -> new RuntimeException("Movie not found: " + id));
    if (req.getCorrectAnswer() != null)
      movie.setCorrectAnswer(req.getCorrectAnswer());
    if (req.getTagIds() != null)
      movie.setTags(new HashSet<>(tagRepo.findAllById(req.getTagIds())));
    movie = movieRepo.save(movie);
    return toSummary(movie);
  }

  @Transactional
  public DataPointSummary updateQuestion(Long id, UpdateDataPointRequest req) {
    QuestionMemory question = questionRepo.findById(id)
        .orElseThrow(() -> new RuntimeException("Question not found: " + id));
    if (req.getQuestionText() != null)
      question.setQuestionText(req.getQuestionText());
    if (req.getCorrectAnswer() != null)
      question.setCorrectAnswer(req.getCorrectAnswer());
    if (req.getTagIds() != null)
      question.setTags(new HashSet<>(tagRepo.findAllById(req.getTagIds())));
    question = questionRepo.save(question);
    return toSummary(question);
  }

  // ===================== LIST ALL =====================

  public List<DataPointSummary> getAllDataPoints(String keycloakId, List<DataPointType> types, List<Long> tagIds) {
    List<DataPointSummary> results = new ArrayList<>();

    boolean allTypes = types == null || types.isEmpty();

    if (allTypes || types.contains(DataPointType.PHOTO)) {
      List<PhotoMemory> photos = (tagIds != null && !tagIds.isEmpty())
          ? photoRepo.findByPatientKeycloakIdAndTagIds(keycloakId, tagIds)
          : photoRepo.findByPatientKeycloakId(keycloakId);
      photos.forEach(p -> results.add(toSummary(p)));
    }

    if (allTypes || types.contains(DataPointType.PLACE)) {
      List<PlaceMemory> places = (tagIds != null && !tagIds.isEmpty())
          ? placeRepo.findByPatientKeycloakIdAndTagIds(keycloakId, tagIds)
          : placeRepo.findByPatientKeycloakId(keycloakId);
      places.forEach(p -> results.add(toSummary(p)));
    }

    if (allTypes || types.contains(DataPointType.MOVIE)) {
      List<MovieMemory> movies = (tagIds != null && !tagIds.isEmpty())
          ? movieRepo.findByPatientKeycloakIdAndTagIds(keycloakId, tagIds)
          : movieRepo.findByPatientKeycloakId(keycloakId);
      movies.forEach(m -> results.add(toSummary(m)));
    }

    if (allTypes || types.contains(DataPointType.QUESTION)) {
      List<QuestionMemory> questions = (tagIds != null && !tagIds.isEmpty())
          ? questionRepo.findByPatientKeycloakIdAndTagIds(keycloakId, tagIds)
          : questionRepo.findByPatientKeycloakId(keycloakId);
      questions.forEach(q -> results.add(toSummary(q)));
    }

    results.sort(Comparator.comparing(DataPointSummary::getCreatedAt).reversed());
    return results;
  }

  public Map<String, Long> getCounts(String keycloakId) {
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put("PHOTO", photoRepo.countByPatientKeycloakId(keycloakId));
    counts.put("PLACE", placeRepo.countByPatientKeycloakId(keycloakId));
    counts.put("MOVIE", movieRepo.countByPatientKeycloakId(keycloakId));
    counts.put("QUESTION", questionRepo.countByPatientKeycloakId(keycloakId));
    return counts;
  }

  // ===================== CONVERTERS =====================

  private List<TagResponse> tagsToResponse(Set<MemoryTag> tags) {
    if (tags == null)
      return List.of();
    return tags.stream()
        .map(t -> new TagResponse(t.getId(), t.getName(), t.getColor()))
        .collect(Collectors.toList());
  }

  private DataPointSummary toSummary(PhotoMemory p) {
    return DataPointSummary.builder()
        .id(p.getId())
        .type(DataPointType.PHOTO)
        .label(p.getName())
        .subtitle("Photo")
        .correctAnswer(p.getName())
        .tags(tagsToResponse(p.getTags()))
        .createdAt(p.getCreatedAt())
        .imagePreview(Base64.getEncoder().encodeToString(p.getImageData()))
        .build();
  }

  private DataPointSummary toSummary(PlaceMemory p) {
    return DataPointSummary.builder()
        .id(p.getId())
        .type(DataPointType.PLACE)
        .label(p.getName())
        .subtitle(p.getHint() != null ? p.getHint() : String.format("%.4f, %.4f", p.getLatitude(), p.getLongitude()))
        .correctAnswer(p.getName())
        .latitude(p.getLatitude())
        .longitude(p.getLongitude())
        .hint(p.getHint())
        .tags(tagsToResponse(p.getTags()))
        .createdAt(p.getCreatedAt())
        .build();
  }

  private DataPointSummary toSummary(MovieMemory m) {
    return DataPointSummary.builder()
        .id(m.getId())
        .type(DataPointType.MOVIE)
        .label(m.getOriginalTitle())
        .subtitle("Character: " + (m.getCorrectAnswer() != null ? m.getCorrectAnswer() : ""))
        .correctAnswer(m.getCorrectAnswer())
        .tags(tagsToResponse(m.getTags()))
        .createdAt(m.getCreatedAt())
        .posterPath(m.getPosterPath())
        .build();
  }

  private DataPointSummary toSummary(QuestionMemory q) {
    return DataPointSummary.builder()
        .id(q.getId())
        .type(DataPointType.QUESTION)
        .label(q.getQuestionText())
        .subtitle("Answer: " + (q.getCorrectAnswer() != null ? q.getCorrectAnswer() : ""))
        .correctAnswer(q.getCorrectAnswer())
        .tags(tagsToResponse(q.getTags()))
        .createdAt(q.getCreatedAt())
        .build();
  }
}
