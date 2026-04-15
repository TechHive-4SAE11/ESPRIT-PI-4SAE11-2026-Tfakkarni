import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { PLATFORM_ID } from '@angular/core';
import { AudioGameService, AudioGenerateRequest } from './audio-game.service';
import { environment } from '@/environments/environment';

describe('AudioGameService', () => {
  let service: AudioGameService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiBaseUrl}/api/games/audio`;

  function createService(platformId: string = 'browser') {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        AudioGameService,
        { provide: PLATFORM_ID, useValue: platformId },
      ],
    });
    service = TestBed.inject(AudioGameService);
    httpMock = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  // ─── Language Preference ────────────────────────────────────

  describe('language preference (browser)', () => {
    beforeEach(() => createService('browser'));

    it('should return "en" by default', () => {
      expect(service.getPreferredLanguage()).toBe('en');
    });

    it('should store and retrieve "tn" language', () => {
      service.setPreferredLanguage('tn');
      expect(service.getPreferredLanguage()).toBe('tn');
    });

    it('should return "en" for unknown stored values', () => {
      localStorage.setItem('tfk_language', 'fr');
      expect(service.getPreferredLanguage()).toBe('en');
    });
  });

  describe('language preference (server)', () => {
    beforeEach(() => createService('server'));

    it('should return "en" on server platform', () => {
      expect(service.getPreferredLanguage()).toBe('en');
    });

    it('should not write to localStorage on server', () => {
      service.setPreferredLanguage('tn');
      expect(localStorage.getItem('tfk_language')).toBeNull();
    });
  });

  // ─── Gender Cache ──────────────────────────────────────────

  describe('gender cache (browser)', () => {
    beforeEach(() => createService('browser'));

    it('should default to "male"', () => {
      expect(service.getCachedGender()).toBe('male');
    });

    it('should store and retrieve gender', () => {
      service.setCachedGender('female');
      expect(service.getCachedGender()).toBe('female');
    });
  });

  describe('gender cache (server)', () => {
    beforeEach(() => createService('server'));

    it('should default to "male" on server', () => {
      expect(service.getCachedGender()).toBe('male');
    });
  });

  // ─── API Calls ─────────────────────────────────────────────

  describe('generateQuestionAudio', () => {
    beforeEach(() => createService('browser'));

    it('should POST to generate-question and return a blob', () => {
      const request: AudioGenerateRequest = {
        originalText: 'What is this?',
        targetLanguageCode: 'en',
        gameType: 'PHOTO',
        patientName: 'John',
        patientGender: 'male',
      };

      service.generateQuestionAudio(request).subscribe(blob => {
        expect(blob).toBeTruthy();
        expect(blob instanceof Blob).toBeTrue();
      });

      const req = httpMock.expectOne(`${apiUrl}/generate-question`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      expect(req.request.responseType).toBe('blob');
      req.flush(new Blob(['audio-data'], { type: 'audio/mpeg' }));
    });

    it('should propagate errors', () => {
      const request: AudioGenerateRequest = {
        targetLanguageCode: 'en',
        gameType: 'MOVIE',
      };

      service.generateQuestionAudio(request).subscribe({
        error: (err) => {
          expect(err.message).toBe('Failed to generate audio');
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/generate-question`);
      req.error(new ProgressEvent('error'), { status: 500, statusText: 'Internal Server Error' });
    });
  });

  // ─── Signal State ──────────────────────────────────────────

  describe('signal state management', () => {
    beforeEach(() => createService('browser'));

    it('should have correct initial signal values', () => {
      expect(service.isPlaying()).toBeFalse();
      expect(service.audioLoading()).toBeFalse();
      expect(service.audioError()).toBe('');
    });

    it('should stop audio and reset isPlaying', () => {
      service.stopAudio();
      expect(service.isPlaying()).toBeFalse();
    });

    it('should clear cache and error', () => {
      service.clearCache();
      expect(service.audioError()).toBe('');
    });
  });

  // ─── fetchAndPlay ──────────────────────────────────────────

  describe('fetchAndPlay', () => {
    beforeEach(() => createService('browser'));

    it('should set audioLoading to true then false on success', () => {
      const request: AudioGenerateRequest = {
        targetLanguageCode: 'en',
        gameType: 'QUESTION',
      };

      service.fetchAndPlay(request);
      expect(service.audioLoading()).toBeTrue();

      const req = httpMock.expectOne(`${apiUrl}/generate-question`);
      req.flush(new Blob(['audio'], { type: 'audio/mpeg' }));

      expect(service.audioLoading()).toBeFalse();
      expect(service.audioError()).toBe('');
    });

    it('should set audioError on failure', () => {
      const request: AudioGenerateRequest = {
        targetLanguageCode: 'tn',
        gameType: 'PLACE',
      };

      service.fetchAndPlay(request);
      expect(service.audioLoading()).toBeTrue();

      const req = httpMock.expectOne(`${apiUrl}/generate-question`);
      req.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

      expect(service.audioLoading()).toBeFalse();
      expect(service.audioError()).toBeTruthy();
    });
  });
});
