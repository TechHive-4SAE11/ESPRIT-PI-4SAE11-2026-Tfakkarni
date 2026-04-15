import {
  gameTitleSchema, gameDescriptionSchema, customGameSchema,
  photoNameSchema, photoSchema, placeNameSchema, placeSchema,
  movieMemorySchema, questionMemorySchema,
  movieGameSchema, movieItemSchema,
  personalQuestionGameSchema, questionItemSchema,
  miniGameSchema, gameImageSchema,
  tagNameSchema, tagSchema,
  getFirstError, getFieldErrors,
} from '../validation/game-schemas';

describe('Game Schemas (Zod validation)', () => {

  // ─── Tag Schemas ──────────────────────────────────────────

  describe('tagNameSchema', () => {
    it('should accept valid tag names', () => {
      expect(tagNameSchema.safeParse('abc').success).toBeTrue();
      expect(tagNameSchema.safeParse('Tag123').success).toBeTrue();
    });

    it('should reject too short names', () => {
      expect(tagNameSchema.safeParse('ab').success).toBeFalse();
    });

    it('should reject too long names', () => {
      expect(tagNameSchema.safeParse('a'.repeat(11)).success).toBeFalse();
    });

    it('should reject special characters', () => {
      expect(tagNameSchema.safeParse('tag-1').success).toBeFalse();
      expect(tagNameSchema.safeParse('tag name').success).toBeFalse();
    });
  });

  describe('tagSchema', () => {
    it('should accept valid tag', () => {
      expect(tagSchema.safeParse({ name: 'Family', color: '#ff0000' }).success).toBeTrue();
    });

    it('should reject empty color', () => {
      expect(tagSchema.safeParse({ name: 'Family', color: '' }).success).toBeFalse();
    });
  });

  // ─── Game Title & Description ──────────────────────────────

  describe('gameTitleSchema', () => {
    it('should accept valid titles', () => {
      expect(gameTitleSchema.safeParse('My Game').success).toBeTrue();
      expect(gameTitleSchema.safeParse('Game 123').success).toBeTrue();
    });

    it('should reject titles shorter than 3 chars', () => {
      expect(gameTitleSchema.safeParse('ab').success).toBeFalse();
    });

    it('should reject titles longer than 20 chars', () => {
      expect(gameTitleSchema.safeParse('a'.repeat(21)).success).toBeFalse();
    });

    it('should reject special characters', () => {
      expect(gameTitleSchema.safeParse('Game!').success).toBeFalse();
      expect(gameTitleSchema.safeParse('My-Game').success).toBeFalse();
    });
  });

  describe('gameDescriptionSchema', () => {
    it('should accept empty strings', () => {
      expect(gameDescriptionSchema.safeParse('').success).toBeTrue();
    });

    it('should accept undefined', () => {
      expect(gameDescriptionSchema.safeParse(undefined).success).toBeTrue();
    });

    it('should accept valid descriptions', () => {
      expect(gameDescriptionSchema.safeParse('A short description').success).toBeTrue();
    });

    it('should reject descriptions over 100 chars', () => {
      expect(gameDescriptionSchema.safeParse('a'.repeat(101)).success).toBeFalse();
    });
  });

  // ─── Custom Game Schema ────────────────────────────────────

  describe('customGameSchema', () => {
    it('should accept valid custom game', () => {
      const result = customGameSchema.safeParse({ title: 'My Game', description: 'desc' });
      expect(result.success).toBeTrue();
    });

    it('should reject invalid title', () => {
      const result = customGameSchema.safeParse({ title: 'ab', description: '' });
      expect(result.success).toBeFalse();
    });
  });

  // ─── Photo Schemas ─────────────────────────────────────────

  describe('photoNameSchema', () => {
    it('should accept valid photo names', () => {
      expect(photoNameSchema.safeParse('My Photo').success).toBeTrue();
    });

    it('should reject empty names', () => {
      expect(photoNameSchema.safeParse('').success).toBeFalse();
    });

    it('should reject names over 20 chars', () => {
      expect(photoNameSchema.safeParse('a'.repeat(21)).success).toBeFalse();
    });
  });

  describe('photoSchema', () => {
    it('should accept valid photo data', () => {
      const result = photoSchema.safeParse({
        name: 'Beach', imageBase64: 'iVBORw0KGgo=', contentType: 'image/png',
      });
      expect(result.success).toBeTrue();
    });

    it('should reject missing image', () => {
      const result = photoSchema.safeParse({
        name: 'Beach', imageBase64: '', contentType: 'image/png',
      });
      expect(result.success).toBeFalse();
    });

    it('should reject missing content type', () => {
      const result = photoSchema.safeParse({
        name: 'Beach', imageBase64: 'abc', contentType: '',
      });
      expect(result.success).toBeFalse();
    });
  });

  // ─── Place Schemas ─────────────────────────────────────────

  describe('placeSchema', () => {
    it('should accept valid place data', () => {
      const result = placeSchema.safeParse({
        name: 'Home', latitude: 36.8, longitude: 10.1, hint: 'Near the park',
      });
      expect(result.success).toBeTrue();
    });

    it('should accept place without hint', () => {
      const result = placeSchema.safeParse({
        name: 'Office', latitude: 36.8, longitude: 10.1,
      });
      expect(result.success).toBeTrue();
    });

    it('should reject missing latitude', () => {
      const result = placeSchema.safeParse({
        name: 'Office', longitude: 10.1,
      });
      expect(result.success).toBeFalse();
    });

    it('should reject hint over 100 chars', () => {
      const result = placeSchema.safeParse({
        name: 'Home', latitude: 36.8, longitude: 10.1, hint: 'a'.repeat(101),
      });
      expect(result.success).toBeFalse();
    });
  });

  // ─── Movie Memory Schema ──────────────────────────────────

  describe('movieMemorySchema', () => {
    it('should accept valid movie memory', () => {
      const result = movieMemorySchema.safeParse({
        originalTitle: 'Inception', correctAnswer: 'Cobb',
      });
      expect(result.success).toBeTrue();
    });

    it('should reject missing movie title', () => {
      const result = movieMemorySchema.safeParse({
        originalTitle: '', correctAnswer: 'Cobb',
      });
      expect(result.success).toBeFalse();
    });

    it('should reject character name over 20 chars', () => {
      const result = movieMemorySchema.safeParse({
        originalTitle: 'Movie', correctAnswer: 'a'.repeat(21),
      });
      expect(result.success).toBeFalse();
    });
  });

  // ─── Question Memory Schema ───────────────────────────────

  describe('questionMemorySchema', () => {
    it('should accept valid question', () => {
      const result = questionMemorySchema.safeParse({
        questionText: 'What is your favorite color?', correctAnswer: 'Blue',
      });
      expect(result.success).toBeTrue();
    });

    it('should reject empty question', () => {
      const result = questionMemorySchema.safeParse({
        questionText: '', correctAnswer: 'Blue',
      });
      expect(result.success).toBeFalse();
    });

    it('should reject question over 500 chars', () => {
      const result = questionMemorySchema.safeParse({
        questionText: 'a'.repeat(501), correctAnswer: 'x',
      });
      expect(result.success).toBeFalse();
    });

    it('should reject answer over 500 chars', () => {
      const result = questionMemorySchema.safeParse({
        questionText: 'Q?', correctAnswer: 'a'.repeat(501),
      });
      expect(result.success).toBeFalse();
    });
  });

  // ─── Movie Game & Item Schema ──────────────────────────────

  describe('movieGameSchema', () => {
    it('should accept valid movie game', () => {
      const result = movieGameSchema.safeParse({ title: 'Quiz Time', description: '' });
      expect(result.success).toBeTrue();
    });
  });

  describe('movieItemSchema', () => {
    it('should accept valid answer', () => {
      expect(movieItemSchema.safeParse({ correctAnswer: 'Cobb' }).success).toBeTrue();
    });

    it('should reject empty answer', () => {
      expect(movieItemSchema.safeParse({ correctAnswer: '' }).success).toBeFalse();
    });
  });

  // ─── Mini Game & Image Schema ──────────────────────────────

  describe('miniGameSchema', () => {
    it('should accept valid mini game', () => {
      expect(miniGameSchema.safeParse({ title: 'Mini Game', description: '' }).success).toBeTrue();
    });
  });

  describe('gameImageSchema', () => {
    it('should accept valid image', () => {
      const result = gameImageSchema.safeParse({
        name: 'Cat', imageBase64: 'iVBORw0KGgo=', contentType: 'image/jpeg',
      });
      expect(result.success).toBeTrue();
    });

    it('should reject special characters in name', () => {
      const result = gameImageSchema.safeParse({
        name: 'Cat!', imageBase64: 'abc', contentType: 'image/jpeg',
      });
      expect(result.success).toBeFalse();
    });
  });

  // ─── Helper Functions ──────────────────────────────────────

  describe('getFirstError', () => {
    it('should return null for successful result', () => {
      const result = gameTitleSchema.safeParse('Valid');
      expect(getFirstError(result)).toBeNull();
    });

    it('should return first error message', () => {
      const result = gameTitleSchema.safeParse('');
      expect(getFirstError(result)).toBeTruthy();
      expect(typeof getFirstError(result)).toBe('string');
    });
  });

  describe('getFieldErrors', () => {
    it('should return empty object for success', () => {
      const result = customGameSchema.safeParse({ title: 'Valid', description: '' });
      expect(getFieldErrors(result)).toEqual({});
    });

    it('should return field-specific errors', () => {
      const result = customGameSchema.safeParse({ title: '', description: 'a'.repeat(101) });
      const errors = getFieldErrors(result);
      expect(errors['title']).toBeTruthy();
    });
  });
});
