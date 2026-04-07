package org.techhive.mlservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class MlServiceApplication {

    /**
     * Loads repo-root {@code .env} into JVM system properties so Spring can resolve
     * {@code GEMINI_API_KEY}, {@code HUGGINGFACE_API_KEY}, {@code ML_TWILIO_*}, etc.
     * OS environment variables win if already set.
     */
    private static void loadEnvFile() {
        String userDir = System.getProperty("user.dir");
        List<Path> candidates = new ArrayList<>();
        Path current = Paths.get(userDir).toAbsolutePath().normalize();
        // Search current directory and up to 4 parent folders for .env / .env.secret.
        for (int i = 0; i <= 4 && current != null; i++) {
            candidates.add(current.resolve(".env"));
            candidates.add(current.resolve(".env.secret"));
            current = current.getParent();
        }
        for (Path envPath : candidates) {
            if (!Files.isRegularFile(envPath)) {
                continue;
            }
            Dotenv dotenv = Dotenv.configure()
                .directory(envPath.getParent().toString())
                .filename(envPath.getFileName().toString())
                .ignoreIfMissing()
                .load();
            dotenv.entries().forEach(e -> {
                String key = e.getKey();
                if (System.getenv(key) == null) {
                    System.setProperty(key, e.getValue());
                }
            });
            return;
        }
    }

    public static void main(String[] args) {
        loadEnvFile();
        SpringApplication.run(MlServiceApplication.class, args);
    }
}
