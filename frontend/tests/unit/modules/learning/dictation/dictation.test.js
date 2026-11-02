import { describe, it, expect } from 'vitest';
import { getWordStatuses, isAllDone } from '@/modules/learning/dictation/utils/dictation';

describe('dictation/utils/dictation.js', () => {
  describe('getWordStatuses', () => {
    it('returns empty array when refWords is empty', () => {
      expect(getWordStatuses({ refWords: [], userInput: '' })).toEqual([]);
    });

    it('marks word as revealed if index is in revealedIndices', () => {
      const result = getWordStatuses({
        refWords: ['Hello', 'World'],
        userInput: '',
        revealedIndices: new Set([0]),
      });

      expect(result[0]).toEqual({
        displayText: 'Hello',
        status: 'revealed',
      });
      expect(result[1]).toEqual({
        displayText: '*****',
        status: 'empty',
      });
    });

    it('supports array as revealedIndices parameter', () => {
      const result = getWordStatuses({
        refWords: ['First', 'Second'],
        userInput: '',
        revealedIndices: [1],
      });

      expect(result[1].status).toBe('revealed');
    });

    it('marks word as correct if cleanRef is empty (only punctuation)', () => {
      const result = getWordStatuses({
        refWords: ['...'],
        userInput: '',
      });

      expect(result[0]).toEqual({
        displayText: '...',
        status: 'correct',
      });
    });

    it('marks word as correct when cleaned user word matches cleaned ref word', () => {
      const result = getWordStatuses({
        refWords: ['Apple!'],
        userInput: 'apple',
      });

      expect(result[0]).toEqual({
        displayText: 'Apple!',
        status: 'correct',
      });
    });

    it('marks word as empty when user has not typed that word yet', () => {
      const result = getWordStatuses({
        refWords: ['One', 'Two', 'Three'],
        userInput: 'one',
      });

      expect(result[0].status).toBe('correct');
      expect(result[1]).toEqual({
        displayText: '***',
        status: 'empty',
      });
      expect(result[2]).toEqual({
        displayText: '*****',
        status: 'empty',
      });
    });

    it('marks word as incorrect and masks partially typed characters', () => {
      const result = getWordStatuses({
        refWords: ['English'],
        userInput: 'eng',
      });

      // 'eng' matches first 3 chars of 'english' (7 chars total) -> 'eng****'
      expect(result[0]).toEqual({
        displayText: 'eng****',
        status: 'incorrect',
      });
    });

    it('masks completely when first character does not match', () => {
      const result = getWordStatuses({
        refWords: ['banana'],
        userInput: 'apple',
      });

      expect(result[0]).toEqual({
        displayText: '******',
        status: 'incorrect',
      });
    });
  });

  describe('isAllDone', () => {
    it('returns false for empty or falsy array', () => {
      expect(isAllDone([])).toBe(false);
      expect(isAllDone(null)).toBe(false);
      expect(isAllDone(undefined)).toBe(false);
    });

    it('returns true when all items are correct or revealed', () => {
      expect(
        isAllDone([
          { status: 'correct' },
          { status: 'revealed' },
          { status: 'correct' },
        ])
      ).toBe(true);
    });

    it('returns false if any item is empty or incorrect', () => {
      expect(
        isAllDone([
          { status: 'correct' },
          { status: 'empty' },
        ])
      ).toBe(false);

      expect(
        isAllDone([
          { status: 'revealed' },
          { status: 'incorrect' },
        ])
      ).toBe(false);
    });
  });
});
