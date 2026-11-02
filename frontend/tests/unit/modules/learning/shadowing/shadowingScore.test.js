import { describe, it, expect } from 'vitest';
import { scoreShadowing } from '@/modules/learning/shadowing/utils/shadowingScore';

describe('shadowing/utils/shadowingScore.js', () => {
  describe('scoreShadowing', () => {
    it('returns 0 score and empty wordsAccuracy if referenceText is empty or invalid', () => {
      expect(scoreShadowing('', 'hello')).toEqual({ score: 0, wordsAccuracy: [] });
      expect(scoreShadowing(null, 'hello')).toEqual({ score: 0, wordsAccuracy: [] });
      expect(scoreShadowing(undefined, 'hello')).toEqual({ score: 0, wordsAccuracy: [] });
      expect(scoreShadowing('   ', 'hello')).toEqual({ score: 0, wordsAccuracy: [] });
    });

    it('returns perfect 100 score when spoken words match reference exactly', () => {
      const result = scoreShadowing('How are you today?', 'how are you today');
      expect(result.score).toBe(100);
      expect(result.wordsAccuracy).toEqual([
        { word: 'How', accuracy: 100 },
        { word: 'are', accuracy: 100 },
        { word: 'you', accuracy: 100 },
        { word: 'today?', accuracy: 100 },
      ]);
    });

    it('gives partial score capped at 70 for substituted words and 0 for missing words', () => {
      const result = scoreShadowing('I love tea', 'I like tea');
      expect(result.wordsAccuracy[0].accuracy).toBe(100);
      expect(result.wordsAccuracy[1].accuracy).toBeGreaterThan(0);
      expect(result.wordsAccuracy[1].accuracy).toBeLessThanOrEqual(70);
      expect(result.wordsAccuracy[2].accuracy).toBe(100);
      expect(result.score).toBeLessThan(100);

      const withMissing = scoreShadowing('I love coffee', 'I');
      expect(withMissing.wordsAccuracy[1].accuracy).toBe(0);
      expect(withMissing.wordsAccuracy[2].accuracy).toBe(0);
    });

    it('applies penalty for extra words spoken', () => {
      const matchOnly = scoreShadowing('Good morning', 'Good morning');
      const withExtraWords = scoreShadowing(
        'Good morning',
        'Good morning extra words added here please'
      );

      expect(matchOnly.score).toBe(100);
      expect(withExtraWords.score).toBeLessThan(100);
    });

    it('handles special characters in words', () => {
      const result = scoreShadowing("It's 100% true!", 'its 100 true');
      expect(result.score).toBe(100);
    });
  });
});
