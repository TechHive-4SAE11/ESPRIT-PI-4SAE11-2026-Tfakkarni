package org.techhive.gameservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.*;
import org.techhive.gameservice.repository.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataPointServiceTest {

  @Mock private PhotoMemoryRepository photoRepo;
  @Mock private PlaceMemoryRepository placeRepo;
  @Mock private MovieMemoryRepository movieRepo;
  @Mock private QuestionMemoryRepository questionRepo;
  @Mock private MemoryTagRepository tagRepo;

  @InjectMocks private DataPointService dataPointService;

  private MemoryTag tag;

  @BeforeEach
  void setUp() {
    tag = new MemoryTag("patient-1", "Family", "#3b82f6");
    tag.setId(1L);
    tag.setCreatedAt(LocalDateTime.now().minusDays(1));
  }

  @Test
  void createPhotoRejectsOversizedImages() {
    byte[] oversized = new byte[5 * 1024 * 1024 + 1];
    CreatePhotoRequest request = new CreatePhotoRequest("Home", Base64.getEncoder().encodeToString(oversized), "image/png", List.of());

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> dataPointService.createPhoto("patient-1", request));

    assertEquals("Image size exceeds 5MB limit", ex.getMessage());
    verify(photoRepo, never()).save(any());
  }

  @Test
  void createPhotoPersistsTagsAndMapsPreview() {
    when(tagRepo.findAllById(List.of(1L))).thenReturn(List.of(tag));
    when(photoRepo.save(any(PhotoMemory.class))).thenAnswer(invocation -> {
      PhotoMemory photo = invocation.getArgument(0);
      photo.setId(10L);
      photo.setCreatedAt(LocalDateTime.now());
      return photo;
    });

    CreatePhotoRequest request = new CreatePhotoRequest("Home", Base64.getEncoder().encodeToString("img".getBytes()), "image/png", List.of(1L));
    DataPointSummary summary = dataPointService.createPhoto("patient-1", request);

    assertEquals(10L, summary.getId());
    assertEquals(DataPointType.PHOTO, summary.getType());
    assertEquals("Home", summary.getLabel());
    assertEquals("Home", summary.getCorrectAnswer());
    assertFalse(summary.getImagePreview().isBlank());
    assertEquals("Family", summary.getTags().get(0).getName());
    verify(photoRepo).save(argThat(photo -> "patient-1".equals(photo.getPatientKeycloakId()) && photo.getTags().size() == 1));
  }

  @Test
  void createPlacePersistsCoordinatesAndHint() {
    when(placeRepo.save(any(PlaceMemory.class))).thenAnswer(invocation -> {
      PlaceMemory place = invocation.getArgument(0);
      place.setId(20L);
      place.setCreatedAt(LocalDateTime.now());
      return place;
    });

    DataPointSummary summary = dataPointService.createPlace("patient-1",
        new CreateMemoryPlaceRequest("Park", 36.8, 10.18, "near home", List.of()));

    assertEquals(20L, summary.getId());
    assertEquals(DataPointType.PLACE, summary.getType());
    assertEquals("near home", summary.getSubtitle());
    assertEquals(36.8, summary.getLatitude());
    assertEquals(10.18, summary.getLongitude());
  }

  @Test
  void createMovieMapsMovieSpecificFields() {
    when(movieRepo.save(any(MovieMemory.class))).thenAnswer(invocation -> {
      MovieMemory movie = invocation.getArgument(0);
      movie.setId(30L);
      movie.setCreatedAt(LocalDateTime.now());
      return movie;
    });

    DataPointSummary summary = dataPointService.createMovie("patient-1",
        new CreateMovieMemoryRequest(550, "Inception", "/poster.jpg", "2010-07-16", "Cobb", List.of()));

    assertEquals(30L, summary.getId());
    assertEquals(DataPointType.MOVIE, summary.getType());
    assertEquals("Inception", summary.getLabel());
    assertEquals("Character: Cobb", summary.getSubtitle());
    assertEquals("/poster.jpg", summary.getPosterPath());
  }

  @Test
  void createQuestionMapsQuestionAnswer() {
    when(questionRepo.save(any(QuestionMemory.class))).thenAnswer(invocation -> {
      QuestionMemory question = invocation.getArgument(0);
      question.setId(40L);
      question.setCreatedAt(LocalDateTime.now());
      return question;
    });

    DataPointSummary summary = dataPointService.createQuestion("patient-1",
        new CreateQuestionMemoryRequest("Who is your sister?", "Nour", List.of()));

    assertEquals(40L, summary.getId());
    assertEquals(DataPointType.QUESTION, summary.getType());
    assertEquals("Who is your sister?", summary.getLabel());
    assertEquals("Answer: Nour", summary.getSubtitle());
    assertEquals("Nour", summary.getCorrectAnswer());
  }

  @Test
  void updatePhotoThrowsWhenMissingAndUpdatesFieldsWhenFound() {
    when(photoRepo.findById(99L)).thenReturn(Optional.empty());
    assertEquals("Photo not found: 99", assertThrows(RuntimeException.class,
        () -> dataPointService.updatePhoto(99L, new UpdateDataPointRequest())).getMessage());

    PhotoMemory photo = photo(1L, "Old", LocalDateTime.now());
    when(photoRepo.findById(1L)).thenReturn(Optional.of(photo));
    when(tagRepo.findAllById(List.of(1L))).thenReturn(List.of(tag));
    when(photoRepo.save(photo)).thenReturn(photo);

    UpdateDataPointRequest request = new UpdateDataPointRequest();
    request.setName("New");
    request.setTagIds(List.of(1L));
    DataPointSummary summary = dataPointService.updatePhoto(1L, request);

    assertEquals("New", summary.getLabel());
    assertEquals(1, summary.getTags().size());
  }

  @Test
  void updatePlaceMovieAndQuestionApplyPatchSemantics() {
    PlaceMemory place = place(2L, "Old Park", LocalDateTime.now().minusHours(3));
    when(placeRepo.findById(2L)).thenReturn(Optional.of(place));
    when(placeRepo.save(place)).thenReturn(place);
    UpdateDataPointRequest placeReq = new UpdateDataPointRequest();
    placeReq.setName("New Park");
    placeReq.setHint("new hint");
    placeReq.setLatitude(35.0);
    placeReq.setLongitude(9.0);
    assertEquals("new hint", dataPointService.updatePlace(2L, placeReq).getHint());

    MovieMemory movie = movie(3L, "Movie", LocalDateTime.now().minusHours(2));
    when(movieRepo.findById(3L)).thenReturn(Optional.of(movie));
    when(movieRepo.save(movie)).thenReturn(movie);
    UpdateDataPointRequest movieReq = new UpdateDataPointRequest();
    movieReq.setCorrectAnswer("Neo");
    assertEquals("Neo", dataPointService.updateMovie(3L, movieReq).getCorrectAnswer());

    QuestionMemory question = question(4L, "Old?", LocalDateTime.now().minusHours(1));
    when(questionRepo.findById(4L)).thenReturn(Optional.of(question));
    when(questionRepo.save(question)).thenReturn(question);
    UpdateDataPointRequest questionReq = new UpdateDataPointRequest();
    questionReq.setQuestionText("New?");
    questionReq.setCorrectAnswer("Answer");
    DataPointSummary summary = dataPointService.updateQuestion(4L, questionReq);
    assertEquals("New?", summary.getLabel());
    assertEquals("Answer", summary.getCorrectAnswer());
  }

  @Test
  void getAllDataPointsCombinesFiltersAndSortsNewestFirst() {
    when(photoRepo.findByPatientKeycloakIdAndTagIds("patient-1", List.of(1L)))
        .thenReturn(List.of(photo(1L, "Photo", LocalDateTime.now().minusDays(4))));
    when(movieRepo.findByPatientKeycloakIdAndTagIds("patient-1", List.of(1L)))
        .thenReturn(List.of(movie(2L, "Movie", LocalDateTime.now().minusDays(1))));

    List<DataPointSummary> summaries = dataPointService.getAllDataPoints("patient-1",
        List.of(DataPointType.PHOTO, DataPointType.MOVIE), List.of(1L));

    assertEquals(2, summaries.size());
    assertEquals(DataPointType.MOVIE, summaries.get(0).getType());
    verify(placeRepo, never()).findByPatientKeycloakId(anyString());
    verify(questionRepo, never()).findByPatientKeycloakId(anyString());
  }

  @Test
  void getAllDataPointsLoadsAllTypesWhenNoFilterAndCountsByType() {
    when(photoRepo.findByPatientKeycloakId("patient-1")).thenReturn(List.of(photo(1L, "Photo", LocalDateTime.now().minusDays(4))));
    when(placeRepo.findByPatientKeycloakId("patient-1")).thenReturn(List.of(place(2L, "Place", LocalDateTime.now().minusDays(3))));
    when(movieRepo.findByPatientKeycloakId("patient-1")).thenReturn(List.of(movie(3L, "Movie", LocalDateTime.now().minusDays(2))));
    when(questionRepo.findByPatientKeycloakId("patient-1")).thenReturn(List.of(question(4L, "Question", LocalDateTime.now().minusDays(1))));
    when(photoRepo.countByPatientKeycloakId("patient-1")).thenReturn(1L);
    when(placeRepo.countByPatientKeycloakId("patient-1")).thenReturn(2L);
    when(movieRepo.countByPatientKeycloakId("patient-1")).thenReturn(3L);
    when(questionRepo.countByPatientKeycloakId("patient-1")).thenReturn(4L);

    assertEquals(4, dataPointService.getAllDataPoints("patient-1", null, null).size());
    Map<String, Long> counts = dataPointService.getCounts("patient-1");
    assertEquals(1L, counts.get("PHOTO"));
    assertEquals(4L, counts.get("QUESTION"));
  }

  @Test
  void deleteMethodsDelegateToRepositories() {
    dataPointService.deletePhoto(1L);
    dataPointService.deletePlace(2L);
    dataPointService.deleteMovie(3L);
    dataPointService.deleteQuestion(4L);

    verify(photoRepo).deleteById(1L);
    verify(placeRepo).deleteById(2L);
    verify(movieRepo).deleteById(3L);
    verify(questionRepo).deleteById(4L);
  }

  private PhotoMemory photo(Long id, String name, LocalDateTime createdAt) {
    PhotoMemory photo = new PhotoMemory();
    photo.setId(id);
    photo.setPatientKeycloakId("patient-1");
    photo.setName(name);
    photo.setImageData("img".getBytes());
    photo.setImageContentType("image/png");
    photo.setCreatedAt(createdAt);
    photo.setTags(Set.of(tag));
    return photo;
  }

  private PlaceMemory place(Long id, String name, LocalDateTime createdAt) {
    PlaceMemory place = new PlaceMemory();
    place.setId(id);
    place.setPatientKeycloakId("patient-1");
    place.setName(name);
    place.setLatitude(36.8);
    place.setLongitude(10.18);
    place.setHint("hint");
    place.setCreatedAt(createdAt);
    place.setTags(Set.of(tag));
    return place;
  }

  private MovieMemory movie(Long id, String title, LocalDateTime createdAt) {
    MovieMemory movie = new MovieMemory();
    movie.setId(id);
    movie.setPatientKeycloakId("patient-1");
    movie.setTmdbId(550);
    movie.setOriginalTitle(title);
    movie.setPosterPath("/poster.jpg");
    movie.setReleaseDate("2010-07-16");
    movie.setCorrectAnswer("Cobb");
    movie.setCreatedAt(createdAt);
    movie.setTags(Set.of(tag));
    return movie;
  }

  private QuestionMemory question(Long id, String text, LocalDateTime createdAt) {
    QuestionMemory question = new QuestionMemory();
    question.setId(id);
    question.setPatientKeycloakId("patient-1");
    question.setQuestionText(text);
    question.setCorrectAnswer("Answer");
    question.setCreatedAt(createdAt);
    question.setTags(Set.of(tag));
    return question;
  }
}
