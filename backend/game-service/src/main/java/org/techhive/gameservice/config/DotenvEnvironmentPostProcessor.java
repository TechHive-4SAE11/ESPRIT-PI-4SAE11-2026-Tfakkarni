package org.techhive.gameservice.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the project root .env file into the Spring Environment.
 * Walks up from the working directory (up to 5 levels) to find it.
 * Existing OS environment variables take precedence over .env values.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Path dotenvPath = findDotenv();
    if (dotenvPath == null)
      return;

    Map<String, Object> props = parseDotenv(dotenvPath);
    if (!props.isEmpty()) {
      environment.getPropertySources().addLast(new MapPropertySource("dotenv", props));
    }
  }

  private Path findDotenv() {
    Path dir = Paths.get(System.getProperty("user.dir"));
    for (int i = 0; i < 5; i++) {
      Path candidate = dir.resolve(".env");
      if (Files.exists(candidate))
        return candidate;
      dir = dir.getParent();
      if (dir == null)
        break;
    }
    return null;
  }

  private Map<String, Object> parseDotenv(Path path) {
    Map<String, Object> map = new HashMap<>();
    try {
      for (String line : Files.readAllLines(path)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#"))
          continue;
        int eq = trimmed.indexOf('=');
        if (eq <= 0)
          continue;
        String key = trimmed.substring(0, eq).trim();
        String value = trimmed.substring(eq + 1).trim();
        // Don't override existing OS environment variables
        if (System.getenv(key) == null) {
          map.put(key, value);
        }
      }
    } catch (IOException e) {
      System.err.println("[DotenvLoader] Could not read .env: " + e.getMessage());
    }
    return map;
  }
}
