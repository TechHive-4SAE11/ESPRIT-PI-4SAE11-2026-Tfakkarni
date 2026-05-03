package org.techhive.gameservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TmdbServiceTest {

  private TmdbService tmdbService;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() throws Exception {
    tmdbService = new TmdbService();
    setField(tmdbService, "apiKey", "test-key");
    RestTemplate restTemplate = (RestTemplate) getField(tmdbService, "restTemplate");
    server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
  }

  @Test
  void searchMoviesReturnsEmptyForBlankQueryWithoutCallingTmdb() {
    assertThat(tmdbService.searchMovies(null)).isEmpty();
    assertThat(tmdbService.searchMovies("   ")).isEmpty();
    server.verify();
  }

  @Test
  void searchMoviesMapsOnlyMoviesWithPostersAndLimitsResults() {
    StringBuilder results = new StringBuilder();
    for (int i = 1; i <= 12; i++) {
      if (i > 1) {
        results.append(',');
      }
      String poster = i == 2 ? "null" : "\"/poster" + i + ".jpg\"";
      String originalTitle = i == 3 ? "null" : "\"Original " + i + "\"";
      results.append("{\"id\":").append(i)
          .append(",\"title\":\"Title ").append(i).append("\"")
          .append(",\"original_title\":").append(originalTitle)
          .append(",\"poster_path\":").append(poster)
          .append(",\"release_date\":null")
          .append(",\"overview\":null}");
    }

    server.expect(requestTo("https://api.themoviedb.org/3/search/movie?api_key=test-key&query=matrix&include_adult=false"))
        .andRespond(withSuccess("{\"results\":[" + results + "]}", MediaType.APPLICATION_JSON));

    List<Map<String, Object>> movies = tmdbService.searchMovies("matrix");

    assertThat(movies).hasSize(10);
    assertThat(movies).allSatisfy(movie -> assertThat(movie.get("poster_path")).isNotNull());
    assertThat(movies.get(0))
        .containsEntry("id", 1)
        .containsEntry("original_title", "Original 1")
        .containsEntry("title", "Title 1")
        .containsEntry("poster_path", "/poster1.jpg")
        .containsEntry("release_date", "")
        .containsEntry("overview", "");
    assertThat(movies.get(1)).containsEntry("id", 3).containsEntry("original_title", "Title 3");
    server.verify();
  }

  @Test
  void searchMoviesReturnsEmptyWhenResultsMissingOrTmdbFails() {
    server.expect(requestTo("https://api.themoviedb.org/3/search/movie?api_key=test-key&query=no-results&include_adult=false"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    server.expect(requestTo("https://api.themoviedb.org/3/search/movie?api_key=test-key&query=down&include_adult=false"))
        .andRespond(withServerError());

    assertThat(tmdbService.searchMovies("no-results")).isEmpty();
    assertThat(tmdbService.searchMovies("down")).isEmpty();
    server.verify();
  }

  private void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Object getField(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }
}
