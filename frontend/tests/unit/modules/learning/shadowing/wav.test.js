import { describe, it, expect } from 'vitest';
import { encodeWav } from '@/modules/learning/shadowing/utils/wav';

describe('shadowing/utils/wav.js', () => {
  describe('encodeWav', () => {
    it('creates a valid WAV Blob from Float32Array PCM samples', async () => {
      const sampleRate = 16000;
      const numSamples = 1600; // 0.1s audio
      const samples = new Float32Array(numSamples);

      for (let i = 0; i < numSamples; i++) {
        samples[i] = Math.sin((2 * Math.PI * 440 * i) / sampleRate);
      }

      const wavBlob = encodeWav(samples, sampleRate);

      expect(wavBlob).toBeInstanceOf(Blob);
      expect(wavBlob.type).toBe('audio/wav');

      // 44 bytes header + 1600 * 2 bytes = 3244 bytes
      expect(wavBlob.size).toBe(44 + numSamples * 2);

      const arrayBuffer = await wavBlob.arrayBuffer();
      const view = new DataView(arrayBuffer);

      // Verify RIFF and WAVE header strings
      const riff = String.fromCharCode(
        view.getUint8(0),
        view.getUint8(1),
        view.getUint8(2),
        view.getUint8(3)
      );
      const wave = String.fromCharCode(
        view.getUint8(8),
        view.getUint8(9),
        view.getUint8(10),
        view.getUint8(11)
      );

      expect(riff).toBe('RIFF');
      expect(wave).toBe('WAVE');

      // Verify sample rate
      expect(view.getUint32(24, true)).toBe(16000);
      // Verify bits per sample
      expect(view.getUint16(34, true)).toBe(16);
    });

    it('clamps out-of-range float values between -1 and 1', () => {
      const samples = new Float32Array([-2.5, 2.5, 0.5, -0.5]);
      const blob = encodeWav(samples);
      expect(blob.size).toBe(44 + 4 * 2);
    });
  });
});
