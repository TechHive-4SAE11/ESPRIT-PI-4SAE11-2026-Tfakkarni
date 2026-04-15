import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  DataPointService, DataPointSummary, DataPointCounts,
  CreatePhotoRequest, CreatePlaceRequest, CreateMovieMemoryRequest,
  CreateQuestionRequest, UpdateDataPointRequest,
} from './data-point.service';
import { environment } from '../../../environments/environment';

describe('DataPointService', () => {
  let service: DataPointService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/games/data`;
  const testKeycloakId = 'user-dp-123';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        DataPointService,
      ],
    });
    service = TestBed.inject(DataPointService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  const mockSummary: DataPointSummary = {
    id: 1, type: 'PHOTO', label: 'Beach', subtitle: 'image/jpeg',
    tags: [], createdAt: '2026-04-15',
  };

  // ─── Photos ────────────────────────────────────────────────

  describe('createPhoto', () => {
    it('should POST a photo', () => {
      const request: CreatePhotoRequest = {
        name: 'Beach', imageBase64: 'abc', contentType: 'image/jpeg', tagIds: [1],
      };

      service.createPhoto(testKeycloakId, request).subscribe(res => {
        expect(res.label).toBe('Beach');
      });

      const req = httpMock.expectOne(`${baseUrl}/photos/${testKeycloakId}`);
      expect(req.request.method).toBe('POST');
      req.flush(mockSummary);
    });
  });

  describe('deletePhoto', () => {
    it('should DELETE a photo', () => {
      service.deletePhoto(1).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/photos/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('updatePhoto', () => {
    it('should PUT updated photo', () => {
      const update: UpdateDataPointRequest = { name: 'Updated Beach' };

      service.updatePhoto(1, update).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/photos/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(update);
      req.flush(mockSummary);
    });
  });

  // ─── Places ────────────────────────────────────────────────

  describe('createPlace', () => {
    it('should POST a place', () => {
      const request: CreatePlaceRequest = {
        name: 'Home', latitude: 36.8, longitude: 10.1, tagIds: [],
      };

      service.createPlace(testKeycloakId, request).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/places/${testKeycloakId}`);
      expect(req.request.method).toBe('POST');
      req.flush({ ...mockSummary, type: 'PLACE', label: 'Home' });
    });
  });

  describe('deletePlace', () => {
    it('should DELETE a place', () => {
      service.deletePlace(2).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/places/2`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('updatePlace', () => {
    it('should PUT updated place', () => {
      const update: UpdateDataPointRequest = { hint: 'Near park', latitude: 36.9, longitude: 10.2 };

      service.updatePlace(2, update).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/places/2`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockSummary, type: 'PLACE' });
    });
  });

  // ─── Movies ────────────────────────────────────────────────

  describe('createMovie', () => {
    it('should POST a movie memory', () => {
      const request: CreateMovieMemoryRequest = {
        tmdbId: 100, originalTitle: 'Inception', posterPath: '/abc.jpg',
        releaseDate: '2010-07-16', correctAnswer: 'Cobb', tagIds: [1, 2],
      };

      service.createMovie(testKeycloakId, request).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/movies/${testKeycloakId}`);
      expect(req.request.method).toBe('POST');
      req.flush({ ...mockSummary, type: 'MOVIE', label: 'Inception' });
    });
  });

  describe('deleteMovie', () => {
    it('should DELETE a movie', () => {
      service.deleteMovie(3).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/movies/3`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('updateMovie', () => {
    it('should PUT updated movie', () => {
      const update: UpdateDataPointRequest = { correctAnswer: 'Dom' };

      service.updateMovie(3, update).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/movies/3`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockSummary, type: 'MOVIE' });
    });
  });

  // ─── Questions ─────────────────────────────────────────────

  describe('createQuestion', () => {
    it('should POST a question', () => {
      const request: CreateQuestionRequest = {
        questionText: 'Favorite color?', correctAnswer: 'Blue', tagIds: [],
      };

      service.createQuestion(testKeycloakId, request).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/questions/${testKeycloakId}`);
      expect(req.request.method).toBe('POST');
      req.flush({ ...mockSummary, type: 'QUESTION' });
    });
  });

  describe('deleteQuestion', () => {
    it('should DELETE a question', () => {
      service.deleteQuestion(4).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/questions/4`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('updateQuestion', () => {
    it('should PUT updated question', () => {
      const update: UpdateDataPointRequest = { questionText: 'Updated?', correctAnswer: 'Yes' };

      service.updateQuestion(4, update).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/questions/4`);
      expect(req.request.method).toBe('PUT');
      req.flush({ ...mockSummary, type: 'QUESTION' });
    });
  });

  // ─── Listing ───────────────────────────────────────────────

  describe('getAllDataPoints', () => {
    it('should GET all data points without filters', () => {
      service.getAllDataPoints(testKeycloakId).subscribe(res => {
        expect(res.length).toBe(2);
      });

      const req = httpMock.expectOne(`${baseUrl}/${testKeycloakId}`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys().length).toBe(0);
      req.flush([mockSummary, { ...mockSummary, id: 2 }]);
    });

    it('should GET data points with type filters', () => {
      service.getAllDataPoints(testKeycloakId, ['PHOTO', 'PLACE']).subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/${testKeycloakId}`);
      expect(req.request.params.getAll('types')).toEqual(['PHOTO', 'PLACE']);
      req.flush([]);
    });

    it('should GET data points with tag filters', () => {
      service.getAllDataPoints(testKeycloakId, undefined, [1, 2]).subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/${testKeycloakId}`);
      expect(req.request.params.getAll('tagIds')).toEqual(['1', '2']);
      req.flush([]);
    });

    it('should GET data points with both types and tags', () => {
      service.getAllDataPoints(testKeycloakId, ['MOVIE'], [3]).subscribe();

      const req = httpMock.expectOne(r => r.url === `${baseUrl}/${testKeycloakId}`);
      expect(req.request.params.getAll('types')).toEqual(['MOVIE']);
      expect(req.request.params.getAll('tagIds')).toEqual(['3']);
      req.flush([]);
    });
  });

  describe('getCounts', () => {
    it('should GET data point counts', () => {
      const mockCounts: DataPointCounts = {
        PHOTO: 5, PLACE: 3, MOVIE: 2, QUESTION: 4,
      };

      service.getCounts(testKeycloakId).subscribe(res => {
        expect(res.PHOTO).toBe(5);
        expect(res.PLACE).toBe(3);
      });

      const req = httpMock.expectOne(`${baseUrl}/${testKeycloakId}/counts`);
      expect(req.request.method).toBe('GET');
      req.flush(mockCounts);
    });
  });
});
