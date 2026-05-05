package org.techhive.gameservice.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameDtoPojoCoverageTest {

  @Test
  void movieAndPersonalDetailDtosExposeConstructorsAndAccessors() {
    LocalDateTime now = LocalDateTime.now();
    MovieGameDetailResponse.MovieItemDetail movieItem = new MovieGameDetailResponse.MovieItemDetail(7L, 42,
        "Movie", "/poster.jpg", "2020-01-01", "Hero");
    MovieGameDetailResponse movieDetail = new MovieGameDetailResponse(1L, "patient-1", "Movies", "Desc",
        List.of(movieItem), now);

    assertEquals(1L, movieDetail.getId());
    assertEquals("patient-1", movieDetail.getPatientKeycloakId());
    assertEquals("Movies", movieDetail.getTitle());
    assertEquals("Desc", movieDetail.getDescription());
    assertEquals(List.of(movieItem), movieDetail.getMovies());
    assertEquals(now, movieDetail.getCreatedAt());
    assertEquals(7L, movieItem.getId());
    assertEquals(42, movieItem.getTmdbId());
    assertEquals("Movie", movieItem.getOriginalTitle());
    assertEquals("/poster.jpg", movieItem.getPosterPath());
    assertEquals("2020-01-01", movieItem.getReleaseDate());
    assertEquals("Hero", movieItem.getCorrectAnswer());

    MovieGameDetailResponse emptyMovieDetail = new MovieGameDetailResponse();
    emptyMovieDetail.setId(2L);
    emptyMovieDetail.setPatientKeycloakId("patient-2");
    emptyMovieDetail.setTitle("Cinema");
    emptyMovieDetail.setDescription("Actors");
    emptyMovieDetail.setMovies(List.of());
    emptyMovieDetail.setCreatedAt(now);
    MovieGameDetailResponse.MovieItemDetail emptyMovieItem = new MovieGameDetailResponse.MovieItemDetail();
    emptyMovieItem.setId(8L);
    emptyMovieItem.setTmdbId(99);
    emptyMovieItem.setOriginalTitle("Other");
    emptyMovieItem.setPosterPath("/other.jpg");
    emptyMovieItem.setReleaseDate("2021");
    emptyMovieItem.setCorrectAnswer("Actor");

    assertEquals(2L, emptyMovieDetail.getId());
    assertEquals("patient-2", emptyMovieDetail.getPatientKeycloakId());
    assertEquals("Cinema", emptyMovieDetail.getTitle());
    assertEquals("Actors", emptyMovieDetail.getDescription());
    assertEquals(List.of(), emptyMovieDetail.getMovies());
    assertEquals(now, emptyMovieDetail.getCreatedAt());
    assertEquals(8L, emptyMovieItem.getId());
    assertEquals(99, emptyMovieItem.getTmdbId());
    assertEquals("Other", emptyMovieItem.getOriginalTitle());
    assertEquals("/other.jpg", emptyMovieItem.getPosterPath());
    assertEquals("2021", emptyMovieItem.getReleaseDate());
    assertEquals("Actor", emptyMovieItem.getCorrectAnswer());

    PersonalQuestionGameDetailResponse.QuestionItemDetail question =
        new PersonalQuestionGameDetailResponse.QuestionItemDetail(3L, "Who is your sister?", "Nour");
    PersonalQuestionGameDetailResponse personalDetail = new PersonalQuestionGameDetailResponse(4L, "patient-3",
        "Family", "Questions", List.of(question), now);

    assertEquals(4L, personalDetail.getId());
    assertEquals("patient-3", personalDetail.getPatientKeycloakId());
    assertEquals("Family", personalDetail.getTitle());
    assertEquals("Questions", personalDetail.getDescription());
    assertEquals(List.of(question), personalDetail.getQuestions());
    assertEquals(now, personalDetail.getCreatedAt());
    assertEquals(3L, question.getId());
    assertEquals("Who is your sister?", question.getQuestionText());
    assertEquals("Nour", question.getCorrectAnswer());

    PersonalQuestionGameDetailResponse emptyPersonalDetail = new PersonalQuestionGameDetailResponse();
    emptyPersonalDetail.setId(5L);
    emptyPersonalDetail.setPatientKeycloakId("patient-4");
    emptyPersonalDetail.setTitle("Memories");
    emptyPersonalDetail.setDescription("Answers");
    emptyPersonalDetail.setQuestions(List.of());
    emptyPersonalDetail.setCreatedAt(now);
    PersonalQuestionGameDetailResponse.QuestionItemDetail emptyQuestion =
        new PersonalQuestionGameDetailResponse.QuestionItemDetail();
    emptyQuestion.setId(6L);
    emptyQuestion.setQuestionText("Where did you study?");
    emptyQuestion.setCorrectAnswer("Tunis");

    assertEquals(5L, emptyPersonalDetail.getId());
    assertEquals("patient-4", emptyPersonalDetail.getPatientKeycloakId());
    assertEquals("Memories", emptyPersonalDetail.getTitle());
    assertEquals("Answers", emptyPersonalDetail.getDescription());
    assertEquals(List.of(), emptyPersonalDetail.getQuestions());
    assertEquals(now, emptyPersonalDetail.getCreatedAt());
    assertEquals(6L, emptyQuestion.getId());
    assertEquals("Where did you study?", emptyQuestion.getQuestionText());
    assertEquals("Tunis", emptyQuestion.getCorrectAnswer());
  }

  @Test
  void responseDtosExposeRemainingAccessors() {
    LocalDateTime now = LocalDateTime.now();
    MovieGameAttemptResponse.MovieAnswerResult movieResult = new MovieGameAttemptResponse.MovieAnswerResult(1L,
        "/poster.jpg", "Movie", "Hero", "Hero", true);
    MovieGameAttemptResponse movieAttempt = new MovieGameAttemptResponse();
    movieAttempt.setAttemptId(10L);
    movieAttempt.setScore(1);
    movieAttempt.setTotalQuestions(2);
    movieAttempt.setDurationSeconds(30);
    movieAttempt.setPercentage(50.0);
    movieAttempt.setResults(List.of(movieResult));
    movieAttempt.setCompletedAt(now);

    assertEquals(10L, movieAttempt.getAttemptId());
    assertEquals(1, movieAttempt.getScore());
    assertEquals(2, movieAttempt.getTotalQuestions());
    assertEquals(30, movieAttempt.getDurationSeconds());
    assertEquals(50.0, movieAttempt.getPercentage());
    assertEquals(List.of(movieResult), movieAttempt.getResults());
    assertEquals(now, movieAttempt.getCompletedAt());
    assertEquals(1L, movieResult.getItemId());
    assertEquals("/poster.jpg", movieResult.getPosterUrl());
    assertEquals("Movie", movieResult.getMovieTitle());
    assertEquals("Hero", movieResult.getCorrectAnswer());
    assertEquals("Hero", movieResult.getSelectedAnswer());
    assertTrue(movieResult.isCorrect());

    MovieGameAttemptResponse.MovieAnswerResult emptyMovieResult = new MovieGameAttemptResponse.MovieAnswerResult();
    emptyMovieResult.setItemId(2L);
    emptyMovieResult.setPosterUrl("/p2.jpg");
    emptyMovieResult.setMovieTitle("Other");
    emptyMovieResult.setCorrectAnswer("Actor");
    emptyMovieResult.setSelectedAnswer("Wrong");
    emptyMovieResult.setCorrect(false);
    assertEquals(2L, emptyMovieResult.getItemId());
    assertEquals("/p2.jpg", emptyMovieResult.getPosterUrl());
    assertEquals("Other", emptyMovieResult.getMovieTitle());
    assertEquals("Actor", emptyMovieResult.getCorrectAnswer());
    assertEquals("Wrong", emptyMovieResult.getSelectedAnswer());
    assertFalse(emptyMovieResult.isCorrect());

    GameAttemptResponse.AnswerResult answer = new GameAttemptResponse.AnswerResult(3L, "Nour", "Nour", true);
    GameAttemptResponse gameAttempt = new GameAttemptResponse();
    gameAttempt.setAttemptId(11L);
    gameAttempt.setScore(2);
    gameAttempt.setTotalQuestions(3);
    gameAttempt.setDurationSeconds(45);
    gameAttempt.setPercentage(66.6);
    gameAttempt.setResults(List.of(answer));
    gameAttempt.setCompletedAt(now);
    assertEquals(11L, gameAttempt.getAttemptId());
    assertEquals(2, gameAttempt.getScore());
    assertEquals(3, gameAttempt.getTotalQuestions());
    assertEquals(45, gameAttempt.getDurationSeconds());
    assertEquals(66.6, gameAttempt.getPercentage());
    assertEquals(List.of(answer), gameAttempt.getResults());
    assertEquals(now, gameAttempt.getCompletedAt());
    assertEquals(3L, answer.getImageId());
    assertEquals("Nour", answer.getCorrectName());
    assertEquals("Nour", answer.getSelectedName());
    assertTrue(answer.isCorrect());

    GameAttemptResponse.AnswerResult emptyAnswer = new GameAttemptResponse.AnswerResult();
    emptyAnswer.setImageId(4L);
    emptyAnswer.setCorrectName("Ali");
    emptyAnswer.setSelectedName("Sami");
    emptyAnswer.setCorrect(false);
    assertEquals(4L, emptyAnswer.getImageId());
    assertEquals("Ali", emptyAnswer.getCorrectName());
    assertEquals("Sami", emptyAnswer.getSelectedName());
    assertFalse(emptyAnswer.isCorrect());
  }

  @Test
  void listAndSummaryDtosExposeConstructorsAndAccessors() {
    LocalDateTime now = LocalDateTime.now();

    MovieGameResponse movieGame = new MovieGameResponse(12L, "patient-5", "Cinema", "Guess actors", 4, now);
    assertEquals(12L, movieGame.getId());
    assertEquals("patient-5", movieGame.getPatientKeycloakId());
    assertEquals("Cinema", movieGame.getTitle());
    assertEquals("Guess actors", movieGame.getDescription());
    assertEquals(4, movieGame.getMovieCount());
    assertEquals(now, movieGame.getCreatedAt());

    MovieGameResponse editableMovieGame = new MovieGameResponse();
    editableMovieGame.setId(13L);
    editableMovieGame.setPatientKeycloakId("patient-6");
    editableMovieGame.setTitle("Films");
    editableMovieGame.setDescription("Movie memory");
    editableMovieGame.setMovieCount(5);
    editableMovieGame.setCreatedAt(now);
    assertEquals(13L, editableMovieGame.getId());
    assertEquals("patient-6", editableMovieGame.getPatientKeycloakId());
    assertEquals("Films", editableMovieGame.getTitle());
    assertEquals("Movie memory", editableMovieGame.getDescription());
    assertEquals(5, editableMovieGame.getMovieCount());
    assertEquals(now, editableMovieGame.getCreatedAt());

    PersonalQuestionGameResponse personalGame = new PersonalQuestionGameResponse(14L, "patient-7", "Family",
        "Personal questions", 3, now);
    assertEquals(14L, personalGame.getId());
    assertEquals("patient-7", personalGame.getPatientKeycloakId());
    assertEquals("Family", personalGame.getTitle());
    assertEquals("Personal questions", personalGame.getDescription());
    assertEquals(3, personalGame.getQuestionCount());
    assertEquals(now, personalGame.getCreatedAt());

    PersonalQuestionGameResponse editablePersonalGame = new PersonalQuestionGameResponse();
    editablePersonalGame.setId(15L);
    editablePersonalGame.setPatientKeycloakId("patient-8");
    editablePersonalGame.setTitle("School");
    editablePersonalGame.setDescription("Study memories");
    editablePersonalGame.setQuestionCount(6);
    editablePersonalGame.setCreatedAt(now);
    assertEquals(15L, editablePersonalGame.getId());
    assertEquals("patient-8", editablePersonalGame.getPatientKeycloakId());
    assertEquals("School", editablePersonalGame.getTitle());
    assertEquals("Study memories", editablePersonalGame.getDescription());
    assertEquals(6, editablePersonalGame.getQuestionCount());
    assertEquals(now, editablePersonalGame.getCreatedAt());

    EditMovieGameRequest request = new EditMovieGameRequest();
    EditMovieGameRequest.MovieItemEntry movieEntry = new EditMovieGameRequest.MovieItemEntry();
    movieEntry.setId(16L);
    movieEntry.setTmdbId(12345);
    movieEntry.setOriginalTitle("Le Voyage");
    movieEntry.setPosterPath("/voyage.jpg");
    movieEntry.setReleaseDate("2024-02-01");
    movieEntry.setCorrectAnswer("Salma");
    request.setTitle("Journey");
    request.setDescription("Recognize the character");
    request.setMovies(List.of(movieEntry));

    assertEquals("Journey", request.getTitle());
    assertEquals("Recognize the character", request.getDescription());
    assertEquals(List.of(movieEntry), request.getMovies());
    assertEquals(16L, movieEntry.getId());
    assertEquals(12345, movieEntry.getTmdbId());
    assertEquals("Le Voyage", movieEntry.getOriginalTitle());
    assertEquals("/voyage.jpg", movieEntry.getPosterPath());
    assertEquals("2024-02-01", movieEntry.getReleaseDate());
    assertEquals("Salma", movieEntry.getCorrectAnswer());
  }
}
