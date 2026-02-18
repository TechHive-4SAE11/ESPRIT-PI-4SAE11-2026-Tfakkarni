package org.techhive.gameservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TmdbService {

  private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";

  @Value("${tmdb.api-key:bac4726f178ccc49f4659a52479b4725}")
  private String apiKey;

  private final RestTemplate restTemplate = new RestTemplate();

  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> searchMovies(String query) {
    if (query == null || query.trim().isEmpty()) {
      return Collections.emptyList();
    }

    String url = UriComponentsBuilder
        .fromHttpUrl(TMDB_BASE_URL + "/search/movie")
        .queryParam("api_key", apiKey)
        .queryParam("query", query)
        .queryParam("include_adult", false)
        .toUriString();

    try {
      Map<String, Object> response = restTemplate.getForObject(url, Map.class);
      if (response == null || !response.containsKey("results")) {
        return Collections.emptyList();
      }

      List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

      // Filter to only movies with poster images and map to simplified format
      return results.stream()
          .filter(m -> m.get("poster_path") != null)
          .limit(10)
          .map(m -> {
            Map<String, Object> simplified = new LinkedHashMap<>();
            simplified.put("id", m.get("id"));
            simplified.put("original_title",
                m.get("original_title") != null ? m.get("original_title") : m.get("title"));
            simplified.put("title", m.get("title"));
            simplified.put("poster_path", m.get("poster_path"));
            simplified.put("release_date", m.get("release_date") != null ? m.get("release_date") : "");
            simplified.put("overview", m.get("overview") != null ? m.get("overview") : "");
            return simplified;
          })
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Failed to search TMDB for query: {}", query, e);
      return Collections.emptyList();
    }
  }
}
