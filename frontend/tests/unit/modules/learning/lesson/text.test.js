import { describe, it, expect } from 'vitest';
import { splitWords, cleanWord } from '@/modules/learning/lesson/utils/text';

describe('lesson/utils/text.js', () => {
  describe('splitWords', () => {
    it('returns empty array if text is falsy or not a string', () => {
      expect(splitWords('')).toEqual([]);
      expect(splitWords(null)).toEqual([]);
      expect(splitWords(undefined)).toEqual([]);
      expect(splitWords(123)).toEqual([]);
      expect(splitWords({})).toEqual([]);
    });

    it('splits text by consecutive whitespace and trims properly', () => {
      expect(splitWords('Hello world')).toEqual(['Hello', 'world']);
      expect(splitWords('   Multiple   spaces   between \n words \t here ')).toEqual([
        'Multiple',
        'spaces',
        'between',
        'words',
        'here',
      ]);
      expect(splitWords('Single')).toEqual(['Single']);
    });
  });

  describe('cleanWord', () => {
    it('returns empty string if word is falsy or not a string', () => {
      expect(cleanWord('')).toBe('');
      expect(cleanWord(null)).toBe('');
      expect(cleanWord(undefined)).toBe('');
      expect(cleanWord(123)).toBe('');
    });

    it('converts to lowercase and strips non-alphanumeric characters', () => {
      expect(cleanWord('Hello!')).toBe('hello');
      expect(cleanWord("Don't")).toBe('dont');
      expect(cleanWord('100%')).toBe('100');
      expect(cleanWord('...---...')).toBe('');
      expect(cleanWord('Special_Characters#1')).toBe('specialcharacters1');
    });
  });
});
