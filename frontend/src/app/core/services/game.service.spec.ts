import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { GameService, CreateGameRequest, GameResponse, GameDetailResponse, GameStatsResponse, OverviewStatsResponse, ScoreAnalyticsResponse, EditGameRequest } from './game.service';
import { environment } from '@/environments/environment';

describe('GameService', () => {
  let service: GameService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/games`;
  const testKeycloakId = 'user-123';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        GameService,
      ],
    });
    service = TestBed.inject(GameService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ─── Game CRUD ──────────────────────────────────────────────

  describe('createGame', () => {
    it('should POST a new game with X-User-Id header', () => {
      const request: CreateGameRequest = { title: 'Test Game', description: 'A test' };
      const mockResponse: GameResponse = {
        id: 1, patientKeycloakId: testKeycloakId, title: 'Test Game',
        description: 'A test', imageCount: 0, createdAt: '2026-04-15T10:00:00',
      };

      service.createGame(testKeycloakId, request).subscribe(res => {
        expect(res).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.get('X-User-Id')).toBe(testKeycloakId);
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('uploadImages', () => {
    it('should POST images to the game endpoint', () => {
      const gameId = 1;
      const uploads = [{ name: 'photo1', imageBase64: 'abc', contentType: 'image/png' }];
      const mockResponse: GameDetailResponse = {
        id: 1, patientKeycloakId: testKeycloakId, title: 'Test',
        description: '', images: [], createdAt: '2026-04-15T10:00:00',
      };

      service.uploadImages(gameId, uploads).subscribe(res => {
        expect(res).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/images`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(uploads);
      req.flush(mockResponse);
    });
  });

  describe('getPatientGames', () => {
    it('should GET games for a patient', () => {
      const mockGames: GameResponse[] = [
        { id: 1, patientKeycloakId: testKeycloakId, title: 'Game 1', description: '', imageCount: 3, createdAt: '2026-04-15' },
        { id: 2, patientKeycloakId: testKeycloakId, title: 'Game 2', description: '', imageCount: 5, createdAt: '2026-04-14' },
      ];

      service.getPatientGames(testKeycloakId).subscribe(res => {
        expect(res.length).toBe(2);
        expect(res).toEqual(mockGames);
      });

      const req = httpMock.expectOne(`${baseUrl}/patient/${testKeycloakId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockGames);
    });
  });

  describe('getGameDetail', () => {
    it('should GET a single game with images', () => {
      const mockDetail: GameDetailResponse = {
        id: 1, patientKeycloakId: testKeycloakId, title: 'Game 1',
        description: 'desc', images: [
          { id: 10, name: 'img1', imageBase64: 'base64data', contentType: 'image/jpeg', displayOrder: 0 },
        ], createdAt: '2026-04-15',
      };

      service.getGameDetail(1).subscribe(res => {
        expect(res.images.length).toBe(1);
        expect(res.images[0].name).toBe('img1');
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDetail);
    });
  });

  describe('deleteGame', () => {
    it('should DELETE a game by id', () => {
      service.deleteGame(5).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/5`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('editGame', () => {
    it('should PUT updated game data', () => {
      const editReq: EditGameRequest = {
        title: 'Updated', description: 'Updated desc',
        images: [{ id: 10, name: 'img1' }],
      };

      service.editGame(1, editReq).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(editReq);
      req.flush({});
    });
  });

  describe('getAllGames', () => {
    it('should GET all games', () => {
      service.getAllGames().subscribe(res => {
        expect(res.length).toBe(0);
      });

      const req = httpMock.expectOne(`${baseUrl}/all`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  // ─── Stats ──────────────────────────────────────────────────

  describe('getPlayerStats', () => {
    it('should GET stats for a patient', () => {
      const mockStats: GameStatsResponse = {
        playerKeycloakId: testKeycloakId, totalGamesCreated: 3, totalGamesPlayed: 10,
        averageScore: 75, bestScore: 100, totalAttempts: 15,
      };

      service.getPlayerStats(testKeycloakId).subscribe(res => {
        expect(res.totalGamesPlayed).toBe(10);
        expect(res.bestScore).toBe(100);
      });

      const req = httpMock.expectOne(`${baseUrl}/stats/patient/${testKeycloakId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStats);
    });
  });

  describe('getOverviewStats', () => {
    it('should GET global overview stats', () => {
      const mockOverview: OverviewStatsResponse = {
        totalGames: 50, totalAttempts: 200, totalPlayers: 10, averageScorePercentage: 68,
      };

      service.getOverviewStats().subscribe(res => {
        expect(res.totalGames).toBe(50);
      });

      const req = httpMock.expectOne(`${baseUrl}/stats/overview`);
      expect(req.request.method).toBe('GET');
      req.flush(mockOverview);
    });
  });

  describe('getScoreAnalytics', () => {
    it('should GET score analytics with history', () => {
      const mockAnalytics: ScoreAnalyticsResponse = {
        patientKeycloakId: testKeycloakId, totalGamesPlayed: 5, gamesLast7Days: 3,
        averageScore: 80, averageScoreLast7Days: 85, bestScore: 95,
        scoreHistory: [
          {
            attemptId: 1, gameType: 'CUSTOM', gameTitle: 'Test', score: 3,
            totalQuestions: 5, percentage: 60, durationSeconds: 120, completedAt: '2026-04-15',
          },
        ],
      };

      service.getScoreAnalytics(testKeycloakId).subscribe(res => {
        expect(res.scoreHistory.length).toBe(1);
        expect(res.averageScoreLast7Days).toBe(85);
      });

      const req = httpMock.expectOne(`${baseUrl}/stats/analytics/${testKeycloakId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAnalytics);
    });
  });
});
