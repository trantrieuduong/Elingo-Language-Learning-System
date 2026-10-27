import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AudioRecorder } from '@/modules/learning/shadowing/utils/audioRecorder';

describe('shadowing/utils/audioRecorder.js', () => {
  let mockStream;
  let mockTrack;
  let mockAudioContext;
  let mockScriptProcessor;
  let mockSource;

  beforeEach(() => {
    mockTrack = { stop: vi.fn() };
    mockStream = {
      getTracks: vi.fn().mockReturnValue([mockTrack]),
    };

    mockScriptProcessor = {
      connect: vi.fn(),
      disconnect: vi.fn(),
      onaudioprocess: null,
    };

    mockSource = {
      connect: vi.fn(),
      disconnect: vi.fn(),
    };

    mockAudioContext = {
      sampleRate: 16000,
      state: 'running',
      createMediaStreamSource: vi.fn().mockReturnValue(mockSource),
      createScriptProcessor: vi.fn().mockReturnValue(mockScriptProcessor),
      destination: {},
      resume: vi.fn().mockResolvedValue(),
      close: vi.fn().mockResolvedValue(),
    };

    class MockAudioContext {
      constructor() {
        return mockAudioContext;
      }
    }

    window.AudioContext = MockAudioContext;

    Object.defineProperty(navigator, 'mediaDevices', {
      value: {
        getUserMedia: vi.fn().mockResolvedValue(mockStream),
      },
      configurable: true,
      writable: true,
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('clamps maxDurationSec between 10 and 60 seconds', () => {
    const r1 = new AudioRecorder({ maxDurationSec: 5 });
    expect(r1.maxDurationSec).toBe(10);

    const r2 = new AudioRecorder({ maxDurationSec: 100 });
    expect(r2.maxDurationSec).toBe(60);

    const r3 = new AudioRecorder({ maxDurationSec: 25 });
    expect(r3.maxDurationSec).toBe(25);
  });

  it('throws NOT_SUPPORTED error if navigator.mediaDevices is not available', async () => {
    Object.defineProperty(navigator, 'mediaDevices', {
      value: null,
      configurable: true,
      writable: true,
    });

    const recorder = new AudioRecorder();
    await expect(recorder.start()).rejects.toThrow('NOT_SUPPORTED');
  });

  it('throws PERMISSION_DENIED error if user denies microphone access', async () => {
    const err = new Error('Permission denied');
    err.name = 'NotAllowedError';
    navigator.mediaDevices.getUserMedia.mockRejectedValueOnce(err);

    const recorder = new AudioRecorder();
    await expect(recorder.start()).rejects.toThrow('PERMISSION_DENIED');
  });

  it('starts recording and cleans up resources on stop', async () => {
    const recorder = new AudioRecorder({ maxDurationSec: 20 });
    await recorder.start();

    expect(recorder.isRecording).toBe(true);
    expect(navigator.mediaDevices.getUserMedia).toHaveBeenCalled();

    // Calling start again while recording does nothing
    await recorder.start();

    // Simulate audio chunks
    if (mockScriptProcessor.onaudioprocess) {
      mockScriptProcessor.onaudioprocess({
        inputBuffer: {
          getChannelData: () => new Float32Array([0.1, -0.2, 0.3]),
        },
      });
    }

    // Force duration >= 0.5s
    recorder.startTime = Date.now() - 1000;

    // Stop recording (sampleRate is 16000, so no resample needed)
    const result = await recorder.stop();

    expect(recorder.isRecording).toBe(false);
    expect(result.audio16k).toBeInstanceOf(Float32Array);
    expect(result.audio16k.length).toBe(3);
    expect(mockTrack.stop).toHaveBeenCalled();
    expect(mockAudioContext.close).toHaveBeenCalled();
  });

  it('returns empty audio array when recorded audio duration is too short', async () => {
    const recorder = new AudioRecorder();
    await recorder.start();
    recorder.startTime = Date.now(); // 0ms elapsed
    const result = await recorder.stop();
    expect(result.audio16k.length).toBe(0);
  });

  it('cleanup method safely resets stream and audio context', async () => {
    const recorder = new AudioRecorder();
    await recorder.start();

    recorder.cleanup();

    expect(recorder.isRecording).toBe(false);
    expect(mockTrack.stop).toHaveBeenCalled();
  });
});
