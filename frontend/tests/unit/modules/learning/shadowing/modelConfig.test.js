import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { isMobileDevice, pickModelConfig, MODEL_OPTIONS } from '@/modules/learning/shadowing/whisper/modelConfig';

describe('shadowing/whisper/modelConfig.js', () => {
  const originalUserAgent = navigator.userAgent;

  afterEach(() => {
    Object.defineProperty(navigator, 'userAgent', {
      value: originalUserAgent,
      configurable: true,
    });
  });

  describe('isMobileDevice', () => {
    it('detects desktop userAgent', () => {
      Object.defineProperty(navigator, 'userAgent', {
        value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36',
        configurable: true,
      });
      expect(isMobileDevice()).toBe(false);
    });

    it('detects mobile userAgent', () => {
      Object.defineProperty(navigator, 'userAgent', {
        value: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X)',
        configurable: true,
      });
      expect(isMobileDevice()).toBe(true);
    });
  });

  describe('pickModelConfig', () => {
    it('picks standard model by default on desktop', () => {
      const config = pickModelConfig({ isMobile: false });
      expect(config.modelType).toBe('standard');
      expect(config.modelId).toBe(MODEL_OPTIONS.STANDARD);
      expect(config.device).toBe('wasm');
      expect(config.dtype).toBe('q8');
    });

    it('picks light model on mobile', () => {
      const config = pickModelConfig({ isMobile: true });
      expect(config.modelType).toBe('light');
      expect(config.modelId).toBe(MODEL_OPTIONS.LIGHT);
    });

    it('respects userOverride when provided', () => {
      const config = pickModelConfig({ isMobile: true, userOverride: 'standard' });
      expect(config.modelType).toBe('standard');
      expect(config.modelId).toBe(MODEL_OPTIONS.STANDARD);

      const lightOverride = pickModelConfig({ isMobile: false, userOverride: 'light' });
      expect(lightOverride.modelType).toBe('light');
      expect(lightOverride.modelId).toBe(MODEL_OPTIONS.LIGHT);
    });
  });
});
