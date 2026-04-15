# Aymen — Game & IoT Test Documentation

> **Owner**: Aymen (TechHive-4SAE11)
> **Scope**: Game Service (frontend + backend) and IoT Service (frontend + backend)

---

## Table of Contents

1. [Frontend Tests](#1-frontend-tests)
   - [1.1 GameService](#11-gameservice)
   - [1.2 MovieGameService](#12-moviegameservice)
   - [1.3 CustomGameService](#13-customgameservice)
   - [1.4 AudioGameService](#14-audiogameservice)
   - [1.5 DataPointService](#15-datapointservice)
   - [1.6 IotService](#16-iotservice)
   - [1.7 Game Schemas (Zod Validation)](#17-game-schemas-zod-validation)
   - [1.8 SleepAnalysisComponent](#18-sleepanalysiscomponent)
2. [Backend Tests — Game Service](#2-backend-tests--game-service)
   - [2.1 GameServiceTest](#21-gameservicetest)
   - [2.2 MovieGameServiceTest](#22-moviegameservicetest)
   - [2.3 CustomGameServiceTest](#23-customgameservicetest)
   - [2.4 GameStatsServiceTest](#24-gamestatsservicetest)
   - [2.5 QuizServiceTest](#25-quizservicetest)
   - [2.6 QuestionServiceTest](#26-questionservicetest)
   - [2.7 AnswerServiceTest](#27-answerservicetest)
   - [2.8 PersonalQuestionServiceTest](#28-personalquestionservicetest)
   - [2.9 QuizControllerTest](#29-quizcontrollertest)
   - [2.10 QuestionControllerTest](#210-questioncontrollertest)
   - [2.11 AnswerControllerTest](#211-answercontrollertest)
3. [Backend Tests — IoT Service](#3-backend-tests--iot-service)
   - [3.1 IotServiceTest](#31-iotservicetest)
   - [3.2 SleepAnalysisServiceTest](#32-sleepanalysisservicetest)
   - [3.3 HeartbeatAlertServiceTest](#33-heartbeatalertservicetest)
   - [3.4 HeartbeatControllerTest](#34-heartbeatcontrollertest)
4. [Test Summary](#4-test-summary)

---

## 1. Frontend Tests

**Framework**: Jasmine + Karma (Angular TestBed)
**HTTP mocking**: `HttpTestingController` from `@angular/common/http/testing`
**Run command**:
```bash
cd frontend
npx ng test --no-watch --browsers=ChromeHeadless \
  --include="**/game.service.spec.ts" \
  --include="**/movie-game.service.spec.ts" \
  --include="**/custom-game.service.spec.ts" \
  --include="**/audio-game.service.spec.ts" \
  --include="**/iot.service.spec.ts" \
  --include="**/game-schemas.spec.ts" \
  --include="**/data-point.service.spec.ts" \
  --include="**/sleep-analysis.component.spec.ts"
```

---

### 1.1 GameService

**File**: `frontend/src/app/core/services/game.service.spec.ts`
**SUT**: `GameService` — HTTP client for mini-game (image-based) CRUD and statistics.
**Base URL**: `${environment.apiBaseUrl}/api/games`

| Test | What it verifies |
|------|-----------------|
| `createGame` — POST with X-User-Id header | Sends `POST /api/games` with `X-User-Id` header set to the patient's Keycloak ID. Verifies the request body matches `CreateGameRequest` and the response is deserialized to `GameResponse`. |
| `uploadImages` — POST images | Sends `POST /api/games/{id}/images` with an array of `{ name, imageBase64, contentType }` objects. Verifies response maps to `GameDetailResponse`. |
| `getPatientGames` — GET by patient | Sends `GET /api/games/patient/{keycloakId}`. Verifies array response length and mapping. |
| `getGameDetail` — GET single game | Sends `GET /api/games/{id}`. Verifies nested `images` array and image `name` field. |
| `deleteGame` — DELETE by id | Sends `DELETE /api/games/{id}`. Verifies HTTP method. |
| `editGame` — PUT update | Sends `PUT /api/games/{id}` with `EditGameRequest` body (title, description, retained image list). Verifies method and body. |
| `getAllGames` — GET all | Sends `GET /api/games/all`. |
| `getPlayerStats` — GET stats | Sends `GET /api/games/stats/patient/{keycloakId}`. Verifies `totalGamesPlayed` and `bestScore` fields. |
| `getOverviewStats` — GET overview | Sends `GET /api/games/stats/overview`. Verifies `totalGames` field. |
| `getScoreAnalytics` — GET analytics | Sends `GET /api/games/stats/analytics/{keycloakId}`. Verifies `scoreHistory` array and `averageScoreLast7Days`. |

**Teardown**: `httpMock.verify()` asserts no outstanding/unexpected HTTP requests.

---

### 1.2 MovieGameService

**File**: `frontend/src/app/core/services/movie-game.service.spec.ts`
**SUT**: `MovieGameService` — TMDB-integrated movie character guessing game.
**Base URL**: `${environment.apiBaseUrl}/api/games/movies`

| Test | What it verifies |
|------|-----------------|
| `searchMovies` — GET TMDB search | Sends `GET /api/games/movies/tmdb/search?query=inception`. Checks query param propagation and response deserialization. |
| `getTmdbPosterUrl` — URL builder | Pure function: builds `https://image.tmdb.org/t/p/{size}/{path}`. Tests default `w500` and custom `w200` sizes. No HTTP call. |
| `createMovieGame` — POST with header | Sends `POST /api/games/movies` with `X-User-Id` header and `CreateMovieGameRequest` body containing TMDB movie items with correct answers. |
| `getPatientMovieGames` — GET by patient | Sends `GET /api/games/movies/patient/{keycloakId}`. |
| `deleteMovieGame` — DELETE | Sends `DELETE /api/games/movies/{id}`. |
| `getMovieGameDetail` — GET detail | Sends `GET /api/games/movies/{id}`. Verifies nested `movies` array with `correctAnswer` field. |
| `editMovieGame` — PUT update | Sends `PUT /api/games/movies/{id}` with `EditMovieGameRequest`. Verifies body serialization. |
| `getMovieGameForPlay` — GET play data | Sends `GET /api/games/movies/play/{id}`. Verifies `totalQuestions` and `choices` array (multiple-choice answers). |
| `submitMovieGameAnswers` — POST answers | Sends `POST /api/games/movies/play/{id}/submit` with `X-User-Id` header and `{ answers, durationSeconds }`. Verifies `score`, `percentage`, and per-item `correct` boolean in response. |

---

### 1.3 CustomGameService

**File**: `frontend/src/app/core/services/custom-game.service.spec.ts`
**SUT**: `CustomGameService` — unified custom game combining photos, places, movies, and questions.
**Base URL**: `${environment.apiBaseUrl}/api/games/custom`

| Test | What it verifies |
|------|-----------------|
| `createGame` — POST | Sends `POST /api/games/custom/{keycloakId}` with items of mixed `dataType` (PHOTO, PLACE, etc.). Verifies `itemCount` and `itemTypes` in response. |
| `getGames` — GET by patient | Sends `GET /api/games/custom/patient/{keycloakId}`. |
| `getGameDetail` — GET detail | Sends `GET /api/games/custom/{id}`. Verifies `items` array and `itemTypes`. |
| `deleteGame` — DELETE | Sends `DELETE /api/games/custom/{id}`. |
| `editGame` — PUT update | Sends `PUT /api/games/custom/{id}` with `EditCustomGameRequest`. |
| `getPlayData` — GET play | Sends `GET /api/games/custom/play/{id}`. Verifies `totalQuestions`, item `type` (PHOTO, QUESTION), `imageBase64`, `questionText`, and `choices`. |
| `getRandomPlayData` — GET random (no limit) | Sends `GET /api/games/custom/play/random/{keycloakId}` without `limit` query param. |
| `getRandomPlayData` — GET random (with limit) | Same endpoint with `?limit=5`. Verifies query param serialization. |
| `submitResults` — POST with header | Sends `POST /api/games/custom/play/submit` with `X-User-Id` header. Body includes `gameId`, `score`, `totalQuestions`, `durationSeconds`, and per-item answers. Verifies `percentage` and item-level `correct` booleans. |
| `getStats` — GET unified stats | Sends `GET /api/games/custom/stats/{keycloakId}`. Verifies `totalGamesPlayed`, `photoCount`, `placeCount`, etc. |

---

### 1.4 AudioGameService

**File**: `frontend/src/app/core/services/audio-game.service.spec.ts`
**SUT**: `AudioGameService` — text-to-speech audio generation for game questions (ElevenLabs-backed).
**Base URL**: `${environment.apiBaseUrl}/api/games/audio`

| Test | What it verifies |
|------|-----------------|
| **Language Preference (browser)** | `getPreferredLanguage()` defaults to `"en"`. `setPreferredLanguage("tn")` persists to `localStorage('tfk_language')`. Unknown stored values (e.g. `"fr"`) fall back to `"en"`. |
| **Language Preference (server)** | On SSR (`PLATFORM_ID='server'`), always returns `"en"` and does not write to `localStorage`. |
| **Gender Cache (browser)** | `getCachedGender()` defaults to `"male"`. `setCachedGender("female")` persists. |
| **Gender Cache (server)** | Defaults to `"male"` on server platform. |
| `generateQuestionAudio` — POST blob | Sends `POST /api/games/audio/generate-question` with `AudioGenerateRequest` body. Expects `responseType: 'blob'`. Verifies returned `Blob` instance. |
| `generateQuestionAudio` — error propagation | On HTTP 500, the observable emits an error with message `"Failed to generate audio"`. |
| **Signal state** | Initial values: `isPlaying()=false`, `audioLoading()=false`, `audioError()=''`. `stopAudio()` resets `isPlaying`. `clearCache()` resets `audioError`. |
| `fetchAndPlay` — success | Sets `audioLoading` to `true` during fetch, then `false` after response. `audioError` remains empty. |
| `fetchAndPlay` — failure | On HTTP error, sets `audioLoading` back to `false` and `audioError` to a truthy string. |

---

### 1.5 DataPointService

**File**: `frontend/src/app/core/services/data-point.service.spec.ts`
**SUT**: `DataPointService` — CRUD for memory data points (photos, places, movies, questions).
**Base URL**: `${environment.apiBaseUrl}/api/games/data`

| Category | Tests |
|----------|-------|
| **Photos** | `createPhoto` → `POST /data/photos/{keycloakId}`, `deletePhoto` → `DELETE /data/photos/{id}`, `updatePhoto` → `PUT /data/photos/{id}` with `UpdateDataPointRequest`. |
| **Places** | `createPlace` → `POST /data/places/{keycloakId}` with `latitude`, `longitude`, `tagIds`. `deletePlace` → `DELETE /data/places/{id}`. `updatePlace` → `PUT /data/places/{id}` with optional `hint`, `latitude`, `longitude`. |
| **Movies** | `createMovie` → `POST /data/movies/{keycloakId}` with `tmdbId`, `originalTitle`, `posterPath`, `releaseDate`, `correctAnswer`, `tagIds`. `deleteMovie` → `DELETE /data/movies/{id}`. `updateMovie` → `PUT /data/movies/{id}`. |
| **Questions** | `createQuestion` → `POST /data/questions/{keycloakId}` with `questionText`, `correctAnswer`. `deleteQuestion` → `DELETE /data/questions/{id}`. `updateQuestion` → `PUT /data/questions/{id}`. |
| **Listing** | `getAllDataPoints` without filters → `GET /data/{keycloakId}` (no query params). With type filters → `?types=PHOTO&types=PLACE`. With tag filters → `?tagIds=1&tagIds=2`. Combined filters verified. |
| **Counts** | `getCounts` → `GET /data/{keycloakId}/counts`. Response typed as `DataPointCounts { PHOTO, PLACE, MOVIE, QUESTION }`. |

---

### 1.6 IotService

**File**: `frontend/src/app/core/services/iot.service.spec.ts`
**SUT**: `IotService` — heartbeat readings, sleep analysis, and live BPM from dweet.io.
**Base URL**: `${environment.apiBaseUrl}/api/iot/heartbeat`

| Test | What it verifies |
|------|-----------------|
| `getHeartbeatReadings` — without date | Sends `GET /api/iot/heartbeat/{patientId}` with no `date` param. Verifies `bpm` values in response. |
| `getHeartbeatReadings` — with date | Appends `?date=2026-04-10` query param. |
| `getSleepAnalysis` — without date | Sends `GET /api/iot/heartbeat/{patientId}/sleep-analysis`. Verifies `summary.qualityLabel`, `timeline` length, and `insights` array. |
| `getSleepAnalysis` — with date | Appends `?date=2026-04-06`. |
| `getSleepHistory` — default 7 days | Sends `GET /api/iot/heartbeat/{patientId}/sleep-history?days=7`. Verifies `entries` array, `weeklySummary.trend`. |
| `getSleepHistory` — custom days | Sends with `?days=14`. |
| `recordHeartbeat` — POST | Sends `POST /api/iot/heartbeat` with `{ patientId, bpm }`. Verifies saved `id` and `bpm` in response. |
| `getLatestReading` — GET latest | Sends `GET /api/iot/heartbeat/{patientId}/latest`. Verifies non-null response and `bpm`. |
| `getLiveBpmFromDweet` — success | Sends `GET /dweet-proxy/get/latest/dweet/for/{thing}`. Parses `with[0].content.bpm` as number. |
| `getLiveBpmFromDweet` — failed response | Returns `null` when dweet response `this === "failed"`. |
| `getLiveBpmFromDweet` — no bpm | Returns `null` when content has no `bpm` key. |
| `getLiveBpmFromDweet` — empty array | Returns `null` when `with` array is empty. |
| `getLiveBpmFromDweet` — URL encoding | Thing name `"my thing"` is encoded as `my%20thing` in the URL. |

---

### 1.7 Game Schemas (Zod Validation)

**File**: `frontend/src/app/core/validation/game-schemas.spec.ts`
**SUT**: Zod schemas for client-side form validation.

| Schema | Validations tested |
|--------|-------------------|
| `tagNameSchema` | 3–10 alphanumeric chars only. Rejects < 3, > 10, hyphens, spaces. |
| `tagSchema` | Requires `name` + non-empty `color`. |
| `gameTitleSchema` | 3–20 alphanumeric + spaces. Rejects special chars (`!`, `-`). |
| `gameDescriptionSchema` | Optional, max 100 chars. Accepts empty string and `undefined`. |
| `customGameSchema` | Composite: validates title + description together. |
| `photoNameSchema` | 1–20 chars, non-empty. |
| `photoSchema` | Requires `name`, non-empty `imageBase64`, non-empty `contentType`. |
| `placeSchema` | Requires `name`, `latitude`, `longitude`. Optional `hint` max 100 chars. |
| `movieMemorySchema` | Requires non-empty `originalTitle`, `correctAnswer` max 20 chars. |
| `questionMemorySchema` | `questionText` 1–500 chars, `correctAnswer` 1–500 chars. |
| `movieGameSchema` | Composite title + description for movie games. |
| `movieItemSchema` | Requires non-empty `correctAnswer`. |
| `miniGameSchema` | Composite title + description for mini games. |
| `gameImageSchema` | `name` alphanumeric + spaces (rejects `!`), `imageBase64`, `contentType`. |
| `getFirstError()` | Returns `null` on success, first error message string on failure. |
| `getFieldErrors()` | Returns `{}` on success, field-keyed error object on failure (e.g. `errors['title']`). |

---

### 1.8 SleepAnalysisComponent

**File**: `frontend/src/app/pages/patient-dashboard/helper-view/sleep-analysis/sleep-analysis.component.spec.ts`
**SUT**: `SleepAnalysisComponent` — full component test with real HTTP layer (no mocked service).
**Dependencies**: Injects `IotService` via `provideHttpClient()` + `provideHttpClientTesting()`.

| Category | Tests |
|----------|-------|
| **Initialization** | Component creates successfully. Default signals: `isLoading()=false`, `isLiveTracking()=false`, `liveBpm()=null`, `analysis()=null`, `error()=null`, `selectedDate()='2026-04-06'`. On `ngOnInit`, fires both `sleep-analysis` and `sleep-history` HTTP requests. |
| **loadAnalysis** | Sets `isLoading=true` during fetch, `false` after. Empty timeline → sets `error` to `"No heartbeat data"`, `analysis=null`. HTTP 500 → `error` contains `"Failed to load"`. Empty `keycloakId` → no HTTP request fired. |
| **loadHistory** | Sets `isLoadingHistory=true/false`. Loads 7-day sleep history. Empty keycloakId skips. |
| **formatMinutes** | `420` → `"7h 0m"`, `90` → `"1h 30m"`, `0` → `"0h 0m"`. |
| **qualityEmoji** | Maps `Good` → `😊`. |
| **qualityBadgeClass** | Maps `Excellent` → class containing `"emerald"`. |
| **liveBpmStatus** | `null` → `"No data"`, `72` → `"Normal"`, `125` → `"Elevated!"`, `35` → `"Too Low!"`, `110` → `"High Normal"`, `55` → `"Low Normal"`. |
| **bpmToBarHeight** | Very low BPM → min `10`. Very high BPM → `100`. Normal BPM → scaled value between 10–100. |
| **bpmToBarColor** | `130` → `"bg-red-400"`, `35` → `"bg-orange-400"`, `72` → `"bg-green-400"`. |
| **liveBpmClass** | `null` → contains `"muted"`, `72` → `"green"`, `130` → `"red"`, `35` → `"orange"`. |
| **historyQualityEmoji** | `Excellent` → `🌟`, `Good` → `😊`, `Fair` → `😐`, `Poor` → `😟`. |
| **historyQualityBadgeClass** | Maps quality labels to Tailwind color classes (`emerald`, `blue`, `amber`, `red`). |
| **Live Tracking** | `startLiveTracking()` sets `isLiveTracking=true`, fires dweet-proxy requests. `stopLiveTracking()` sets `false`. Idempotent — calling start twice is a no-op. |
| **selectHistoryNight** | Updates `selectedDate` signal and triggers `loadAnalysis`. |
| **onDateChange** | Reads date from DOM event `target.value`, updates signal, triggers reload. |

---

## 2. Backend Tests — Game Service

**Framework**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) for service-layer tests, `@WebMvcTest` + MockMvc for controller-layer tests.
**Service**: `backend/game-service` (port 18082)
**Package**: `org.techhive.gameservice`

---

### 2.1 GameServiceTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/service/GameServiceTest.java`
**SUT**: `GameService` — mini-game (image-based memory game) business logic.
**Mocks**: `MiniGameRepository`

| Test | What it verifies |
|------|-----------------|
| `createGame_savesAndReturnsResponse` | Constructs a `MiniGame` entity from `CreateGameRequest`, saves via repository, returns `GameResponse` with correct `id`, `title`, `patientKeycloakId`, `imageCount=0`. |
| `addImages_success` | Finds game by ID, decodes base64 images (< 5MB), appends `GameImage` entities to game, saves. Returns `GameDetailResponse`. |
| `addImages_exceedsLimit_throwsException` | A 6MB base64 image throws `IllegalArgumentException("exceeds 5MB limit")`. |
| `addImages_gameNotFound_throwsException` | Non-existent game ID throws `RuntimeException("Game not found")`. |
| `getGamesByPatient_returnsList` | Queries `findByPatientKeycloakId`, maps each `MiniGame` to `GameResponse`. Verifies list size and titles. |
| `getGameDetail_found` | Finds game with images, returns `GameDetailResponse` with image `name` and count. |
| `getGameDetail_notFound_throwsException` | Throws `RuntimeException("Game not found")`. |
| `deleteGame_success` | Checks `existsById`, then calls `deleteById`. |
| `deleteGame_notFound_throwsException` | Throws `RuntimeException("Game not found")` when game doesn't exist. |
| `getAllGames_returnsList` | Calls `findAll()`, maps to response list. |

---

### 2.2 MovieGameServiceTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/service/MovieGameServiceTest.java`
**SUT**: `MovieGameService` — TMDB movie character guessing game logic.
**Mocks**: `MovieGameRepository`, `MovieGameItemRepository`, `MovieGameAttemptRepository`

| Test | What it verifies |
|------|-----------------|
| `createMovieGame_savesGameAndItems` | Creates `MovieGame` entity, saves items (`MovieGameItem`) with TMDB metadata, calls `save()` twice (initial + after items). |
| `getGamesByPatient_returnsMapped` | Maps `MovieGame` entities to `MovieGameResponse` DTOs. |
| `getGameForPlay_returnsPlayDataWithChoices` | Loads game + items. Builds `MovieGamePlayData` with multiple-choice `choices` array containing all correct answers from all items. Verifies each question's choices contain "Tyler Durden" and "Vincent Vega". |
| `getGameForPlay_notFound_throwsException` | Throws `RuntimeException("Movie game not found")`. |
| `getGameForPlay_lessThan2Movies_throwsException` | Throws `RuntimeException("at least 2 movies")` — minimum 2 movies required for meaningful play. |
| `submitAnswers_correctAnswers_fullScore` | With all correct answers: `score=2`, `totalQuestions=2`, `percentage=100.0`. All result items `isCorrect()=true`. Saves `MovieGameAttempt`. |
| `submitAnswers_wrongAnswers_zeroScore` | Wrong answer → `score=0`, `isCorrect()=false`. |
| `submitAnswers_caseInsensitiveComparison` | `"tyler durden"` matches `"Tyler Durden"` — case-insensitive string comparison. |
| `deleteGame_success` | `existsById` + `deleteById`. |
| `deleteGame_notFound_throwsException` | Throws when not found. |

---

### 2.3 CustomGameServiceTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/service/CustomGameServiceTest.java`
**SUT**: `CustomGameService` — unified game mixing multiple data point types.
**Mocks**: `CustomGameRepository`, `CustomGameAttemptRepository`, `PhotoMemoryRepository`, `PlaceMemoryRepository`, `MovieMemoryRepository`, `QuestionMemoryRepository`, `MemoryTagRepository`, `DataPointPerformanceRepository`, `PatientContextService`

| Test | What it verifies |
|------|-----------------|
| `createGame_savesAndReturnsResponse` | Creates `CustomGame` with items of mixed `DataPointType` (PHOTO, QUESTION). |
| `updateGame_updatesFieldsAndItems` | Finds existing game, updates title/description, replaces items list, saves. |
| `updateGame_notFound_throwsException` | Throws `IllegalArgumentException("Game not found")`. |
| `getGamesForPatient_returnsMapped` | Maps entities to DTOs. |
| `getPlayData_returnsPlayItems` | Loads game items, resolves each data point (e.g. `QuestionMemory` for QUESTION type), builds `UnifiedPlayData` with `correctAnswer`. |
| `getPlayData_gameNotFound_throwsException` | Throws `IllegalArgumentException("Game not found")`. |
| `getRandomPlayData_prioritizesNewItems` | With no prior `DataPointPerformance` records, all items are Tier 1 (new/never-played) and selected first. Verifies adaptive difficulty selection. |
| `getRandomPlayData_respectsLimit` | With `limit=1`, returns exactly 1 item even if more data points exist. |
| `submitResults_savesAttemptAndReturnsResult` | Saves `CustomGameAttempt` entity, returns `UnifiedPlayResult` with `attemptId` and `totalQuestions`. |
| `submitResults_withNullGameId_worksForRandomGames` | `gameId=null` (random/mixed game) — still saves attempt without a linked `CustomGame`. |
| `deleteGame_callsRepository` | Calls `deleteById` directly. |
| `getStats_returnsCorrectCounts` | Aggregates `totalGamesPlayed`, `averageScore`, `bestScore`, and per-type counts (`photoCount`, `placeCount`, `movieCount`, `questionCount`). |
| `getStats_noAttempts_returnsZeros` | All stats default to zero when no attempts exist. |

---

### 2.4 GameStatsServiceTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/service/GameStatsServiceTest.java`
**SUT**: `GameStatsService` — cross-game-type statistics aggregation.
**Mocks**: All game + attempt repositories (MiniGame, CustomGame, MovieGame, PersonalQuestion)

| Test | What it verifies |
|------|-----------------|
| `getPlayerStats_aggregatesAllGameTypes` | Sums game counts (2+3+1+1=7), attempt counts (5+3+2+4=14), finds max best score across types (100), computes weighted average score. |
| `getPlayerStats_noData_returnsZeros` | All fields zero when no games/attempts. |
| `getOverviewStats_aggregatesPlatformWide` | Platform-wide totals: `totalGames=20`, `totalAttempts=50`, `totalPlayers=5`, `averageScorePercentage=72.5`. |
| `getOverviewStats_noAttempts_zeroAverage` | All zeros when platform is empty. |
| `getScoreAnalytics_returnsChronologicalAttempts` | Collects attempts from all game types, sorts by `completedAt` ascending. Verifies earliest attempt has `gameType="CUSTOM"`, latest has `gameType="MINI"`. |
| `getScoreAnalytics_computesPercentagesCorrectly` | Score 7/10 → `percentage=70.0`, `averageScore=70.0`, `bestScore=7`. |
| `getScoreAnalytics_noAttempts_returnsEmpty` | Empty `scoreHistory`, zero totals. |

---

### 2.5 QuizServiceTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/service/QuizServiceTest.java`
**SUT**: `IQuizServiceImp` — classic quiz (topic-based, caregiver-managed) CRUD.
**Mocks**: `QuizRepository`, `IQuestionService`

| Test | What it verifies |
|------|-----------------|
| `createQuiz_withValidData` | Saves quiz with topic and caregiverId. |
| `createQuiz_withNullTopic` | Returns `null`, never calls `save()`. |
| `createQuiz_withNullCaregiverId` | Returns `null`, never calls `save()`. |
| `getQuizById_whenExists` | Finds and returns quiz. |
| `getQuizById_whenNotExists` | Returns `null`. |
| `updateQuiz_whenExists` | Finds, updates, saves. |
| `updateQuiz_whenNotExists` | Returns `null`, never saves. |
| `deleteQuiz_whenExists` | Calls `deleteById`. |
| `deleteQuiz_whenNotExists` | Does nothing. |
| `getQuizzesByCaregiverId` | Returns filtered list. |
| `searchQuizzesByTopic_null` | Returns empty list. |
| `searchQuizzesByTopic_valid` | Calls `findByTopicContainingIgnoreCase`. |
| `startQuiz` | Resets `totalScore=0`, sets `dateTaken`. |
| `completeQuiz` | Sets `totalScore=95`, `levelReached=3`. |
| `getAverageScoreByCaregiver_withQuizzes` | `(80+60)/2 = 70.0`. |
| `getAverageScoreByCaregiver_noQuizzes` | Returns `0.0`. |
| `getQuizCountByCaregiver` | Returns repository count. |

---

### 2.6 QuestionServiceTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/service/QuestionServiceTest.java`
**SUT**: `IQestionServiceImp` — CRUD for quiz questions.
**Mocks**: `QuestionRepository`, `QuizRepository`

| Test | What it verifies |
|------|-----------------|
| `createQuestion_valid` | Links question to quiz, saves. |
| `createQuestion_nonExistentQuiz` | Returns `null`. |
| `getQuestionById` | Found → returns question. Not found → `null`. |
| `updateQuestion_notFound` | Returns `null`. |
| `deleteQuestion` | `existsById=true` → deletes. `false` → no-op. |
| `getQuestionsByQuizId` | Returns list by quiz ID. |
| `searchQuestions_null` | Returns empty. |
| `searchQuestions_valid` | `findByTextContainingIgnoreCase`. |
| `calculateTotalPoints` | Sums `difficultyLevel * 10` for all questions. `(1*10)+(3*10) = 40`. |
| `validateQuestion_emptyText` | Returns `false`. |
| `validateQuestion_valid` | Returns `true`. |

---

### 2.7 AnswerServiceTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/service/AnswerServiceTest.java`
**SUT**: `IAnswerServiceImp` — CRUD for quiz answers.
**Mocks**: `AnswerRepository`, `QuestionRepository`

| Test | What it verifies |
|------|-----------------|
| `createAnswer_valid` | Links to question, saves with `text`, `isCorrect`, `explanation`. |
| `getAnswerById` | Returns answer entity. |
| `updateAnswer_notFound` | Returns `null`. |
| `deleteAnswer` | `existsById` guard. |
| `getAnswersByQuestionId` | Returns list. |
| `getCorrectAnswerByQuestionId` | `findByQuestionIdAndIsCorrectTrue`. |
| `validateAnswer_correct` | Correct answer for question → `true`. |
| `validateAnswer_incorrect` | Wrong answer → `false`. |
| `createAnswersBatch_valid` | Saves list via `saveAll`. |
| `createAnswersBatch_allInvalid` | Throws `RuntimeException` for empty text. |
| `getAnswerCountByQuestionId` | Returns count (4). |
| `searchAnswers` | `findByTextContainingIgnoreCase`. |

---

### 2.8 PersonalQuestionServiceTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/service/PersonalQuestionServiceTest.java`
**SUT**: `PersonalQuestionService` — caregiver-authored personal Q&A game.
**Mocks**: `PersonalQuestionGameRepository`, `PersonalQuestionItemRepository`, `PersonalQuestionAttemptRepository`

| Test | What it verifies |
|------|-----------------|
| `createGame_savesGameAndQuestions` | Creates game entity, then saves items. Two `save()` calls. |
| `getGamesByPatient_returnsMapped` | Maps to `PersonalQuestionGameResponse`. |
| `getGameForPlay_returnsPlayData` | Loads items via `findByGameId`, builds `PersonalQuestionPlayData` with `questionText` and `correctAnswer`. |
| `getGameForPlay_notFound` | Throws `RuntimeException("Personal question game not found")`. |
| `getGameForPlay_noQuestions` | Throws `RuntimeException("no questions")`. |
| `submitResults_savesSelfAssessedScore` | Self-reported score (no auto-grading). `score=3, total=5 → percentage=60.0`. |
| `submitResults_zeroQuestions_zeroPercentage` | `0/0 → 0.0%`. |
| `deleteGame_success` | `existsById` + `deleteById`. |
| `deleteGame_notFound` | Throws when not found. |

---

### 2.9 QuizControllerTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/controller/QuizControllerTest.java`
**Type**: `@WebMvcTest` integration test — tests HTTP layer with MockMvc.
**SUT**: `QuizController` — REST endpoints at `/api/games/quiz`

| Test | HTTP | Endpoint | Verifies |
|------|------|----------|----------|
| `createQuiz_shouldReturn201` | POST | `/api/games/quiz` | 201 Created, `$.topic` in response. |
| `createQuiz_withInvalidData_shouldReturn400` | POST | `/api/games/quiz` | 400 when service returns `null`. |
| `getQuizById_whenExists` | GET | `/api/games/quiz/1` | 200 with `$.id` and `$.topic`. |
| `getQuizById_whenNotExists` | GET | `/api/games/quiz/99` | 404 Not Found. |
| `getAllQuizzes` | GET | `/api/games/quiz` | 200, array with topics. |
| `deleteQuiz` | DELETE | `/api/games/quiz/1` | 204 No Content. |
| `searchQuizzesByTopic` | GET | `/api/games/quiz/search?topic=Memory` | 200 with filtered results. |
| `getQuizCountByCaregiver` | GET | `/api/games/quiz/caregiver/10/count` | 200, body `"5"`. |
| `startQuiz` | POST | `/api/games/quiz/1/start` | 200, `totalScore=0`. |
| `completeQuiz` | POST | `/api/games/quiz/1/complete` | 200, `totalScore=95`. |

---

### 2.10 QuestionControllerTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/controller/QuestionControllerTest.java`
**Type**: `@WebMvcTest` integration test.
**SUT**: `QuestionController` — REST endpoints at `/api/games/quiz/questions`

| Test | HTTP | Endpoint | Verifies |
|------|------|----------|----------|
| `createQuestion` | POST | `/api/games/quiz/questions` | 201, `$.text`. |
| `getQuestionById_exists` | GET | `/api/games/quiz/questions/1` | 200. |
| `getQuestionById_notExists` | GET | `/api/games/quiz/questions/99` | 404. |
| `getAllQuestions` | GET | `/api/games/quiz/questions` | 200 array. |
| `deleteQuestion` | DELETE | `/api/games/quiz/questions/1` | 204. |
| `getQuestionsByQuizId` | GET | `/api/games/quiz/questions/quiz/1` | 200, `$.quizId`. |
| `searchQuestions` | GET | `/api/games/quiz/questions/search?keyword=capital` | 200. |
| `getQuestionCountByQuizId` | GET | `/api/games/quiz/questions/quiz/1/count` | `"5"`. |
| `calculateTotalPoints` | GET | `/api/games/quiz/questions/quiz/1/total-points` | `"40"`. |

---

### 2.11 AnswerControllerTest

**File**: `backend/game-service/src/test/java/org/techhive/gameservice/controller/AnswerControllerTest.java`
**Type**: `@WebMvcTest` integration test.
**SUT**: `AnswerController` — REST endpoints at `/api/games/quiz/answer`

| Test | HTTP | Endpoint | Verifies |
|------|------|----------|----------|
| `createAnswer` | POST | `/api/games/quiz/answer` | 201, `$.text`. |
| `getAnswerById_exists` | GET | `/api/games/quiz/answer/getAnswerById/1` | 200. |
| `getAnswerById_notExists` | GET | `/api/games/quiz/answer/getAnswerById/99` | 404. |
| `getAllAnswers` | GET | `/api/games/quiz/answer` | 200 array. |
| `getAnswersByQuestionId` | GET | `/api/games/quiz/answer/question/1` | 200. |
| `getCorrectAnswer` | GET | `/api/games/quiz/answer/question/1/correct` | `$.isCorrect=true`. |
| `validateAnswer` | POST | `/api/games/quiz/answer/validate` | `$.valid=true`. |
| `createAnswersBatch` | POST | `/api/games/quiz/answer/batch` | 201 with list. |
| `isAnswerCorrect` | GET | `/api/games/quiz/answer/1/is-correct` | `$.value=true`. |

---

## 3. Backend Tests — IoT Service

**Service**: `backend/iot-service`
**Package**: `org.techhive.iotservice`

---

### 3.1 IotServiceTest

**File**: `backend/iot-service/src/test/java/org/techhive/iotservice/service/IotServiceTest.java`
**SUT**: `IotService` — heartbeat data ingestion and retrieval.
**Mocks**: `HeartbeatReadingRepository`, `HeartbeatAlertService`

| Test | What it verifies |
|------|-----------------|
| `getHeartbeatReadings_returnsReadingsForNight` | Queries repository with nightly window `20:00 → 12:00 next day` for the given date. Verifies BPM values and patient ID mapping. |
| `getHeartbeatReadings_emptyResult` | Returns empty list when no readings exist. |
| `recordHeartbeat_savesAndReturnsDTO` | Converts `HeartbeatReadingDTO` → `HeartbeatReading` entity, saves, converts back. Verifies `id`, `bpm`, `patientId`, `timestamp`. |
| `recordHeartbeat_triggersAlertCheck` | After saving a reading with BPM=130, calls `alertService.checkAndAlert(patientId, 130)`. |
| `recordHeartbeat_usesCurrentTimestampWhenNull` | When DTO has `timestamp=null`, the service auto-fills `LocalDateTime.now()` before saving. |
| `getLatestReading_returnsLatest` | Calls `findFirstByPatientIdOrderByTimestampDesc`, maps to DTO. |
| `getLatestReading_returnsNullWhenNoData` | Returns `null` when repository returns `Optional.empty()`. |

---

### 3.2 SleepAnalysisServiceTest

**File**: `backend/iot-service/src/test/java/org/techhive/iotservice/service/SleepAnalysisServiceTest.java`
**SUT**: `SleepAnalysisService` — BPM-based sleep stage classification and quality scoring.
**Mocks**: `HeartbeatReadingRepository`

| Test | What it verifies |
|------|-----------------|
| `analyze_noReadings_returnsEmptyTimeline` | No data → empty timeline, `totalSleepMinutes=0`, `qualityLabel="No Data"`, single insight `"No heartbeat data available"`. |
| `analyze_returnsCompleteResponse` | 240 realistic readings (8h night) → non-empty timeline, non-null summary, non-empty insights. |
| `analyze_classifiesAwakeCorrectly` | BPM > 76 → all entries classified as `"AWAKE"`. |
| `analyze_classifiesDeepSleepCorrectly` | BPM < 58 with low variance in early night → all entries classified as `"DEEP"`. |
| `analyze_classifiesLightSleepCorrectly` | BPM 60–76 with low variance → all entries classified as `"LIGHT"`. |
| `analyze_computesSummaryCorrectly` | 10 readings × 2 min → `timeInBedMinutes=20`. Verifies `totalSleepMinutes > 0`, `sleepEfficiency > 0`. |
| `analyze_qualityScore_poor_forFragmentedSleep` | Alternating AWAKE/LIGHT readings → `qualityScore < 50`, `qualityLabel="Poor"`. |
| `analyze_countsAwakenings` | Consecutive AWAKE readings between LIGHT readings → `awakenings >= 1`. Tests that smoothing algorithm preserves real awakenings. |
| `analyze_generatesInsights` | 240 realistic readings → at least 5 insight messages containing "deep sleep", "REM", and "efficiency". |
| `analyze_qualityLabelMapping` | 8h of deep+light sleep → `qualityLabel` is "Good" or "Excellent", `qualityScore >= 70`. |

**Algorithm details tested**: The service uses BPM thresholds (< 58 = DEEP, 58–76 = LIGHT/REM, > 76 = AWAKE), time-of-night heuristics (early night favors DEEP classification), BPM variance for REM detection, and a multi-factor quality scoring formula (duration, deep sleep %, efficiency, awakenings).

---

### 3.3 HeartbeatAlertServiceTest

**File**: `backend/iot-service/src/test/java/org/techhive/iotservice/service/HeartbeatAlertServiceTest.java`
**SUT**: `HeartbeatAlertService` — BPM threshold alerting with Telegram notifications and cooldown logic.
**Setup**: Uses `ReflectionTestUtils` to configure thresholds (`highBpmThreshold=120`, `lowBpmThreshold=40`, `cooldownMinutes=10`). Telegram bot token left blank to prevent real HTTP calls.

| Test | What it verifies |
|------|-----------------|
| `checkAndAlert_normalBpm_noAlert` | BPM=75 (within 40–120) does not throw. |
| `checkAndAlert_atBoundary_noAlert` | BPM=120 and BPM=40 (exactly at thresholds) do not trigger alerts. |
| `checkAndAlert_highBpm_triggersAlert` | BPM=150 (> 120) triggers the alert code path. |
| `checkAndAlert_lowBpm_triggersAlert` | BPM=30 (< 40) triggers the alert code path. |
| `checkAndAlert_cooldownActive_skipsAlert` | After an alert, a second alert within `cooldownMinutes=10` is skipped. Uses `ConcurrentHashMap<String, LocalDateTime>` to track last alert time per patient. |
| `checkAndAlert_cooldownExpired_sendsAlert` | Alert timestamp older than 15 minutes → cooldown expired → alert fires again. |
| `checkAndAlert_differentPatients_independentCooldowns` | `patient-1` in cooldown does not affect `patient-2`. Each patient has independent cooldown tracking. |
| `sendTelegramMessage_telegramNotConfigured_skips` | Blank `botToken` → no HTTP call, no exception. |

---

### 3.4 HeartbeatControllerTest

**File**: `backend/iot-service/src/test/java/org/techhive/iotservice/controller/HeartbeatControllerTest.java`
**Type**: Standalone MockMvc test (`MockitoExtension` + `MockMvcBuilders.standaloneSetup`).
**SUT**: `HeartbeatController` — REST endpoints at `/api/iot/heartbeat`
**Mocks**: `IotService`, `SleepAnalysisService`, `FeatureGateClient`

| Test | HTTP | Endpoint | Verifies |
|------|------|----------|----------|
| `getReadings_withDate` | GET | `/api/iot/heartbeat/{patientId}?date=2026-04-10` | 200, `$[0].bpm=65`, `$[0].patientId`. |
| `getReadings_withoutDate` | GET | `/api/iot/heartbeat/{patientId}` | 200, defaults to yesterday's date. |
| `getSleepAnalysis` | GET | `/api/iot/heartbeat/{patientId}/sleep-analysis?date=2026-04-10` | 200, `$.summary.qualityScore=85`, `$.summary.qualityLabel="Excellent"`. |
| `getLatestReading_found` | GET | `/api/iot/heartbeat/{patientId}/latest` | 200, `$.bpm=72`. |
| `getLatestReading_noData` | GET | `/api/iot/heartbeat/{patientId}/latest` | 204 No Content. |
| `recordHeartbeat` | POST | `/api/iot/heartbeat` | 200, `$.id=1`, `$.bpm=75`. |

**Note**: `FeatureGateClient.isIotEnabled()` is mocked to return `true` for all tests, simulating the IoT feature being enabled for the patient.

---

## 4. Test Summary

| Layer | Service | Test Files | Test Count (approx.) |
|-------|---------|------------|---------------------|
| **Frontend** | GameService | `game.service.spec.ts` | 10 |
| **Frontend** | MovieGameService | `movie-game.service.spec.ts` | 10 |
| **Frontend** | CustomGameService | `custom-game.service.spec.ts` | 10 |
| **Frontend** | AudioGameService | `audio-game.service.spec.ts` | 12 |
| **Frontend** | DataPointService | `data-point.service.spec.ts` | 16 |
| **Frontend** | IotService | `iot.service.spec.ts` | 13 |
| **Frontend** | Game Schemas | `game-schemas.spec.ts` | 30 |
| **Frontend** | SleepAnalysisComponent | `sleep-analysis.component.spec.ts` | 26 |
| **Backend** | GameService | `GameServiceTest.java` | 10 |
| **Backend** | MovieGameService | `MovieGameServiceTest.java` | 10 |
| **Backend** | CustomGameService | `CustomGameServiceTest.java` | 13 |
| **Backend** | GameStatsService | `GameStatsServiceTest.java` | 7 |
| **Backend** | QuizService | `QuizServiceTest.java` | 15 |
| **Backend** | QuestionService | `QuestionServiceTest.java` | 12 |
| **Backend** | AnswerService | `AnswerServiceTest.java` | 11 |
| **Backend** | PersonalQuestionService | `PersonalQuestionServiceTest.java` | 9 |
| **Backend** | QuizController | `QuizControllerTest.java` | 10 |
| **Backend** | QuestionController | `QuestionControllerTest.java` | 9 |
| **Backend** | AnswerController | `AnswerControllerTest.java` | 9 |
| **Backend** | IotService | `IotServiceTest.java` | 7 |
| **Backend** | SleepAnalysisService | `SleepAnalysisServiceTest.java` | 10 |
| **Backend** | HeartbeatAlertService | `HeartbeatAlertServiceTest.java` | 8 |
| **Backend** | HeartbeatController | `HeartbeatControllerTest.java` | 6 |
| | | **Total** | **~263** |

### Test Patterns Used

| Pattern | Where |
|---------|-------|
| **HttpTestingController** (Angular) | All frontend service specs — intercept HTTP, assert method/URL/headers/body, flush mock response |
| **Zod safeParse** | `game-schemas.spec.ts` — schema validation without HTTP |
| **Component TestBed** | `sleep-analysis.component.spec.ts` — full component lifecycle with real DI |
| **Mockito @Mock + @InjectMocks** | All backend service tests — isolated unit tests |
| **@WebMvcTest + MockMvc** | Backend controller tests — HTTP layer integration tests |
| **ReflectionTestUtils** | `HeartbeatAlertServiceTest` — inject private config fields |
| **Builder pattern** | IoT DTOs use Lombok `@Builder` for readable test data construction |
