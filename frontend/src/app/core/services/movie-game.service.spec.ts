import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  MovieGameService, CreateMovieGameRequest, MovieGameResponse,
  MovieGamePlayData, MovieGameSubmitRequest, MovieGameAttemptResponse,
  MovieGameDetailResponse, EditMovieGameRequest, TmdbMovie,
} from './movie-game.service';
import { environment } from '../../../environments/environment';

describe('MovieGameService', () => {
  let service: MovieGameService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/games/movies`;
  const testKeycloakId = 'user-456';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        MovieGameService,
      ],
    });
    service = TestBed.inject(MovieGameService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ─── TMDB Search ───────────────────────────────────────────

  describe('searchMovies', () => {
    it('should GET TMDB movies with query param', () => {
      const mockMovies: TmdbMovie[] = [
        { id: 100, original_title: 'Inception', title: 'Inception', poster_path: '/abc.jpg', release_date: '2010-07-16', overview: 'A mind-bending thriller' },
      ];

      service.searchMovies('inception').subscribe(res => {
        expect(res.length).toBe(1);
        expect(res[0].original_title).toBe('Inception');
      });

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/tmdb/search` && r.params.get('query') === 'inception');
      expect(req.request.method).toBe('GET');
      req.flush(mockMovies);
    });
  });

  describe('getTmdbPosterUrl', () => {
    it('should build a poster URL with default size', () => {
      const url = service.getTmdbPosterUrl('/abc.jpg');
      expect(url).toBe('https://image.tmdb.org/t/p/w500/abc.jpg');
    });

    it('should build a poster URL with custom size', () => {
      const url = service.getTmdbPosterUrl('/abc.jpg', 'w200');
      expect(url).toBe('https://image.tmdb.org/t/p/w200/abc.jpg');
    });
  });

  // ─── Movie Game CRUD ───────────────────────────────────────

  describe('createMovieGame', () => {
    it('should POST a movie game with X-User-Id header', () => {
      const request: CreateMovieGameRequest = {
        title: 'Movie Quiz', description: 'Guess characters',
        movies: [{ tmdbId: 100, originalTitle: 'Inception', posterPath: '/abc.jpg', releaseDate: '2010-07-16', correctAnswer: 'Cobb' }],
      };
      const mockResponse: MovieGameResponse = {
        id: 1, patientKeycloakId: testKeycloakId, title: 'Movie Quiz',
        description: 'Guess characters', movieCount: 1, createdAt: '2026-04-15',
      };

      service.createMovieGame(testKeycloakId, request).subscribe(res => {
        expect(res.movieCount).toBe(1);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.get('X-User-Id')).toBe(testKeycloakId);
      req.flush(mockResponse);
    });
  });

  describe('getPatientMovieGames', () => {
    it('should GET movie games for a patient', () => {
      service.getPatientMovieGames(testKeycloakId).subscribe(res => {
        expect(res.length).toBe(0);
      });

      const req = httpMock.expectOne(`${baseUrl}/patient/${testKeycloakId}`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('deleteMovieGame', () => {
    it('should DELETE a movie game', () => {
      service.deleteMovieGame(3).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/3`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('getMovieGameDetail', () => {
    it('should GET movie game detail', () => {
      const mockDetail: MovieGameDetailResponse = {
        id: 1, patientKeycloakId: testKeycloakId, title: 'Quiz',
        description: '', movies: [
          { id: 10, tmdbId: 100, originalTitle: 'Inception', posterPath: '/abc.jpg', releaseDate: '2010-07-16', correctAnswer: 'Cobb' },
        ], createdAt: '2026-04-15',
      };

      service.getMovieGameDetail(1).subscribe(res => {
        expect(res.movies.length).toBe(1);
        expect(res.movies[0].correctAnswer).toBe('Cobb');
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDetail);
    });
  });

  describe('editMovieGame', () => {
    it('should PUT updated movie game data', () => {
      const editReq: EditMovieGameRequest = {
        title: 'Updated Quiz', description: 'Updated',
        movies: [{ tmdbId: 100, originalTitle: 'Inception', posterPath: '/abc.jpg', releaseDate: '2010-07-16', correctAnswer: 'Updated Cobb' }],
      };

      service.editMovieGame(1, editReq).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(editReq);
      req.flush({});
    });
  });

  // ─── Gameplay ──────────────────────────────────────────────

  describe('getMovieGameForPlay', () => {
    it('should GET play data for a movie game', () => {
      const mockPlay: MovieGamePlayData = {
        gameId: 1, title: 'Quiz', description: 'Test',
        questions: [
          { itemId: 10, posterUrl: 'https://image.tmdb.org/t/p/w500/abc.jpg', movieTitle: 'Inception', releaseDate: '2010-07-16', choices: ['Cobb', 'Arthur', 'Mal', 'Eames'] },
        ],
        totalQuestions: 1,
      };

      service.getMovieGameForPlay(1).subscribe(res => {
        expect(res.totalQuestions).toBe(1);
        expect(res.questions[0].choices.length).toBe(4);
      });

      const req = httpMock.expectOne(`${baseUrl}/play/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPlay);
    });
  });

  describe('submitMovieGameAnswers', () => {
    it('should POST answers and return attempt result', () => {
      const submitReq: MovieGameSubmitRequest = {
        answers: [{ itemId: 10, selectedAnswer: 'Cobb' }],
        durationSeconds: 30,
      };
      const mockResult: MovieGameAttemptResponse = {
        attemptId: 1, score: 1, totalQuestions: 1, durationSeconds: 30, percentage: 100,
        results: [{ itemId: 10, posterUrl: '', movieTitle: 'Inception', correctAnswer: 'Cobb', selectedAnswer: 'Cobb', correct: true }],
        completedAt: '2026-04-15T10:00:00',
      };

      service.submitMovieGameAnswers(1, testKeycloakId, submitReq).subscribe(res => {
        expect(res.score).toBe(1);
        expect(res.percentage).toBe(100);
        expect(res.results[0].correct).toBeTrue();
      });

      const req = httpMock.expectOne(`${baseUrl}/play/1/submit`);
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.get('X-User-Id')).toBe(testKeycloakId);
      req.flush(mockResult);
    });
  });
});
