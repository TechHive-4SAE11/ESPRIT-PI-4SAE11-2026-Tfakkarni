package org.techhive.gameservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.GameImage;
import org.techhive.gameservice.entity.MiniGame;
import org.techhive.gameservice.repository.MiniGameRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private MiniGameRepository miniGameRepository;

    @InjectMocks
    private GameService gameService;

    private static final String PATIENT_ID = "patient-abc";

    @Test
    void createGame_savesAndReturnsResponse() {
        CreateGameRequest request = new CreateGameRequest();
        request.setTitle("Family Photos");
        request.setDescription("Recognize family members");

        MiniGame saved = new MiniGame(PATIENT_ID, "Family Photos", "Recognize family members");
        saved.setId(1L);
        saved.setCreatedAt(LocalDateTime.now());

        when(miniGameRepository.save(any(MiniGame.class))).thenReturn(saved);

        GameResponse result = gameService.createGame(PATIENT_ID, request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Family Photos");
        assertThat(result.getPatientKeycloakId()).isEqualTo(PATIENT_ID);
        assertThat(result.getImageCount()).isZero();
        verify(miniGameRepository).save(any(MiniGame.class));
    }

    @Test
    void addImages_success() {
        MiniGame game = new MiniGame(PATIENT_ID, "Test Game", "desc");
        game.setId(1L);
        game.setImages(new ArrayList<>());

        // Small valid base64 image (a few bytes)
        String smallBase64 = Base64.getEncoder().encodeToString(new byte[100]);

        GameImageUpload upload = new GameImageUpload();
        upload.setName("photo1");
        upload.setImageBase64(smallBase64);
        upload.setContentType("image/png");

        when(miniGameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(miniGameRepository.save(any(MiniGame.class))).thenReturn(game);

        GameDetailResponse result = gameService.addImages(1L, List.of(upload));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(miniGameRepository).save(game);
    }

    @Test
    void addImages_exceedsLimit_throwsException() {
        MiniGame game = new MiniGame(PATIENT_ID, "Test Game", "desc");
        game.setId(1L);
        game.setImages(new ArrayList<>());

        // 6MB image — exceeds 5MB limit
        byte[] largeImage = new byte[6 * 1024 * 1024];
        String largeBase64 = Base64.getEncoder().encodeToString(largeImage);

        GameImageUpload upload = new GameImageUpload();
        upload.setName("huge");
        upload.setImageBase64(largeBase64);
        upload.setContentType("image/png");

        when(miniGameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.addImages(1L, List.of(upload)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds 5MB limit");
    }

    @Test
    void addImages_gameNotFound_throwsException() {
        when(miniGameRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.addImages(999L, List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    void getGamesByPatient_returnsList() {
        MiniGame g1 = new MiniGame(PATIENT_ID, "Game 1", "desc1");
        g1.setId(1L);
        g1.setCreatedAt(LocalDateTime.now());
        g1.setImages(new ArrayList<>());

        MiniGame g2 = new MiniGame(PATIENT_ID, "Game 2", "desc2");
        g2.setId(2L);
        g2.setCreatedAt(LocalDateTime.now());
        g2.setImages(new ArrayList<>());

        when(miniGameRepository.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(g1, g2));

        List<GameResponse> result = gameService.getGamesByPatient(PATIENT_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Game 1");
        assertThat(result.get(1).getTitle()).isEqualTo("Game 2");
    }

    @Test
    void getGameDetail_found() {
        MiniGame game = new MiniGame(PATIENT_ID, "Test Game", "desc");
        game.setId(1L);
        game.setCreatedAt(LocalDateTime.now());

        GameImage img = new GameImage(game, "photo", new byte[]{1, 2, 3}, "image/png", 0);
        img.setId(10L);
        game.setImages(new ArrayList<>(List.of(img)));

        when(miniGameRepository.findById(1L)).thenReturn(Optional.of(game));

        GameDetailResponse result = gameService.getGameDetail(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Game");
        assertThat(result.getImages()).hasSize(1);
        assertThat(result.getImages().get(0).getName()).isEqualTo("photo");
    }

    @Test
    void getGameDetail_notFound_throwsException() {
        when(miniGameRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.getGameDetail(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    void deleteGame_success() {
        when(miniGameRepository.existsById(1L)).thenReturn(true);

        gameService.deleteGame(1L);

        verify(miniGameRepository).deleteById(1L);
    }

    @Test
    void deleteGame_notFound_throwsException() {
        when(miniGameRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> gameService.deleteGame(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    void getAllGames_returnsList() {
        MiniGame g1 = new MiniGame("p1", "Game A", "a");
        g1.setId(1L);
        g1.setCreatedAt(LocalDateTime.now());
        g1.setImages(new ArrayList<>());

        when(miniGameRepository.findAll()).thenReturn(List.of(g1));

        List<GameResponse> result = gameService.getAllGames();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Game A");
    }
}
