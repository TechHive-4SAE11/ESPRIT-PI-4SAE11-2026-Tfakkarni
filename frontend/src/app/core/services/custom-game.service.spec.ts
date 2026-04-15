import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  CustomGameService, CreateCustomGameRequest, CustomGameResponse,
  CustomGameDetailResponse, UnifiedPlayData, UnifiedSubmitRequest,
  UnifiedPlayResult, UnifiedStats, EditCustomGameRequest,
} from './custom-game.service';
import { environment } from '../../../environments/environment';

describe('CustomGameService', () => {
  let service: CustomGameService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/games/custom`;
  const testKeycloakId = 'user-789';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        CustomGameService,
      ],
    });
    service = TestBed.inject(CustomGameService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ─── CRUD ──────────────────────────────────────────────────

  describe('createGame', () => {
    it('should POST a custom game for a patient', () => {
      const request: CreateCustomGameRequest = {
        title: 'My Custom Game', description: 'Mixed items',
        items: [{ dataType: 'PHOTO', dataPointId: 1 }, { dataType: 'PLACE', dataPointId: 2 }],
      };
      const mockResponse: CustomGameResponse = {
        id: 1, title: 'My Custom Game', description: 'Mixed items',
        itemCount: 2, itemTypes: ['PHOTO', 'PLACE'], createdAt: '2026-04-15',
      };

      service.createGame(testKeycloakId, request).subscribe(res => {
        expect(res.itemCount).toBe(2);
        expect(res.itemTypes).toEqual(['PHOTO', 'PLACE']);
      });

      const req = httpMock.expectOne(`${baseUrl}/${testKeycloakId}`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('getGames', () => {
    it('should GET all custom games for a patient', () => {
      const mockGames: CustomGameResponse[] = [
        { id: 1, title: 'Game A', description: '', itemCount: 3, itemTypes: ['PHOTO'], createdAt: '2026-04-15' },
      ];

      service.getGames(testKeycloakId).subscribe(res => {
        expect(res.length).toBe(1);
      });

      const req = httpMock.expectOne(`${baseUrl}/patient/${testKeycloakId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockGames);
    });
  });

  describe('getGameDetail', () => {
    it('should GET detail for a custom game', () => {
      const mockDetail: CustomGameDetailResponse = {
        id: 1, title: 'Game A', description: '', itemTypes: ['PHOTO', 'MOVIE'],
        items: [
          { id: 10, type: 'PHOTO', label: 'Photo 1', subtitle: '', tags: [], createdAt: '2026-04-15' },
        ], createdAt: '2026-04-15',
      };

      service.getGameDetail(1).subscribe(res => {
        expect(res.items.length).toBe(1);
        expect(res.itemTypes).toContain('PHOTO');
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDetail);
    });
  });

  describe('deleteGame', () => {
    it('should DELETE a custom game', () => {
      service.deleteGame(5).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/5`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('editGame', () => {
    it('should PUT updated game', () => {
      const editReq: EditCustomGameRequest = {
        title: 'Updated', description: 'New desc',
        items: [{ dataType: 'QUESTION', dataPointId: 3 }],
      };

      service.editGame(1, editReq).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(editReq);
      req.flush({});
    });
  });

  // ─── Play ──────────────────────────────────────────────────

  describe('getPlayData', () => {
    it('should GET play data for a game', () => {
      const mockPlay: UnifiedPlayData = {
        gameId: 1, title: 'Game A', totalQuestions: 2,
        items: [
          { index: 0, type: 'PHOTO', itemId: 10, imageBase64: 'abc', imageContentType: 'image/png', choices: ['A', 'B', 'C'] },
          { index: 1, type: 'QUESTION', itemId: 20, questionText: 'What color?', correctAnswer: 'Blue', choices: ['Red', 'Blue', 'Green'] },
        ],
      };

      service.getPlayData(1).subscribe(res => {
        expect(res.totalQuestions).toBe(2);
        expect(res.items[0].type).toBe('PHOTO');
      });

      const req = httpMock.expectOne(`${baseUrl}/play/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPlay);
    });
  });

  describe('getRandomPlayData', () => {
    it('should GET random play data without limit', () => {
      service.getRandomPlayData(testKeycloakId).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/play/random/${testKeycloakId}`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.has('limit')).toBeFalse();
      req.flush({ gameId: null, title: 'Random', totalQuestions: 0, items: [] });
    });

    it('should GET random play data with limit', () => {
      service.getRandomPlayData(testKeycloakId, 5).subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/play/random/${testKeycloakId}`);
      expect(req.request.params.get('limit')).toBe('5');
      req.flush({ gameId: null, title: 'Random', totalQuestions: 5, items: [] });
    });
  });

  describe('submitResults', () => {
    it('should POST results with X-User-Id header', () => {
      const submitReq: UnifiedSubmitRequest = {
        gameId: 1, score: 3, totalQuestions: 5, durationSeconds: 60,
        answers: [
          { type: 'PHOTO', itemId: 10, selectedAnswer: 'A' },
        ],
      };
      const mockResult: UnifiedPlayResult = {
        attemptId: 1, score: 3, totalQuestions: 5, percentage: 60,
        durationSeconds: 60, completedAt: '2026-04-15',
        results: [{ type: 'PHOTO', itemId: 10, correct: true, correctAnswer: 'A', selectedAnswer: 'A', label: 'Photo 1' }],
      };

      service.submitResults(testKeycloakId, submitReq).subscribe(res => {
        expect(res.percentage).toBe(60);
        expect(res.results[0].correct).toBeTrue();
      });

      const req = httpMock.expectOne(`${baseUrl}/play/submit`);
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.get('X-User-Id')).toBe(testKeycloakId);
      req.flush(mockResult);
    });
  });

  // ─── Stats ─────────────────────────────────────────────────

  describe('getStats', () => {
    it('should GET unified stats', () => {
      const mockStats: UnifiedStats = {
        totalGamesPlayed: 10, averageScore: 70, bestScore: 95,
        photoCount: 5, placeCount: 3, movieCount: 2, questionCount: 4,
      };

      service.getStats(testKeycloakId).subscribe(res => {
        expect(res.totalGamesPlayed).toBe(10);
        expect(res.photoCount).toBe(5);
      });

      const req = httpMock.expectOne(`${baseUrl}/stats/${testKeycloakId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStats);
    });
  });
});
