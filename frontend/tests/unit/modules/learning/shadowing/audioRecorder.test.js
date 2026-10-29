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

  it('throws PERMISSION_DENIED error if user denies microphone access with PermissionDeniedError', async () => {
    const err = new Error('Permission denied');
    err.name = 'PermissionDeniedError';
    navigator.mediaDevices.getUserMedia.mockRejectedValueOnce(err);

    const recorder = new AudioRecorder();
    await expect(recorder.start()).rejects.toThrow('PERMISSION_DENIED');
  });

  it('re-throws other getUserMedia errors', async () => {
    const err = new Error('Hardware error');
    err.name = 'NotReadableError';
    navigator.mediaDevices.getUserMedia.mockRejectedValueOnce(err);

    const recorder = new AudioRecorder();
    await expect(recorder.start()).rejects.toThrow('Hardware error');
  });

  it('triggers onAutoStop callback when timeout reaches maxDurationSec', async () => {
    vi.useFakeTimers();
    const onAutoStop = vi.fn();
    const recorder = new AudioRecorder({ maxDurationSec: 10, onAutoStop });
    await recorder.start();

    expect(recorder.isRecording).toBe(true);

    vi.advanceTimersByTime(10000);
    expect(onAutoStop).toHaveBeenCalled();

    vi.useRealTimers();
  });

  it('resamples audio when sampleRate is not 16000', async () => {
    mockAudioContext.sampleRate = 44100;

    const mockRenderedBuffer = {
      getChannelData: vi.fn().mockReturnValue(new Float32Array([0.05, -0.1])),
    };
    const mockOfflineContext = {
      createBuffer: vi.fn().mockReturnValue({
        copyToChannel: vi.fn(),
      }),
      createBufferSource: vi.fn().mockReturnValue({
        connect: vi.fn(),
        start: vi.fn(),
      }),
      destination: {},
      startRendering: vi.fn().mockResolvedValue(mockRenderedBuffer),
    };

    window.OfflineAudioContext = vi.fn().mockImplementation(function () {
      return mockOfflineContext;
    });

    const recorder = new AudioRecorder();
    await recorder.start();

    if (mockScriptProcessor.onaudioprocess) {
      mockScriptProcessor.onaudioprocess({
        inputBuffer: {
          getChannelData: () => new Float32Array([0.1, -0.2, 0.3, 0.4]),
        },
      });
    }

    recorder.startTime = Date.now() - 1000;
    const result = await recorder.stop();

    expect(result.audio16k).toHaveLength(2);
    expect(mockOfflineContext.startRendering).toHaveBeenCalled();
  });

  it('handles errors gracefully in cleanup disconnect/close calls', async () => {
    mockScriptProcessor.disconnect.mockImplementationOnce(() => {
      throw new Error('Already disconnected');
    });
    mockSource.disconnect.mockImplementationOnce(() => {
      throw new Error('Already disconnected');
    });
    mockAudioContext.close.mockImplementationOnce(() => {
      throw new Error('Already closed');
    });

    const recorder = new AudioRecorder();
    await recorder.start();
    expect(() => recorder.cleanup()).not.toThrow();
  });

  it('ignores audio process event when not recording', async () => {
    const recorder = new AudioRecorder();
    await recorder.start();
    recorder.isRecording = false;

    if (mockScriptProcessor.onaudioprocess) {
      mockScriptProcessor.onaudioprocess({
        inputBuffer: {
          getChannelData: () => new Float32Array([0.1]),
        },
      });
    }

    expect(recorder.recordedChunks.length).toBe(0);
  });

  it('throws PERMISSION_DENIED error if user denies microphone access with NotAllowedError', async () => {
    const err = new Error('Permission denied');
    err.name = 'NotAllowedError';
    navigator.mediaDevices.getUserMedia.mockRejectedValueOnce(err);

    const recorder = new AudioRecorder();
    await expect(recorder.start()).rejects.toThrow('PERMISSION_DENIED');
  });

  it('supports webkitAudioContext if window.AudioContext is undefined', async () => {
    delete window.AudioContext;
    window.webkitAudioContext = function () {
      return mockAudioContext;
    };

    const recorder = new AudioRecorder();
    await recorder.start();
    expect(recorder.audioContext).toBe(mockAudioContext);

    window.AudioContext = function () {
      return mockAudioContext;
    };
    delete window.webkitAudioContext;
  });

  it('starts recording and cleans up resources on stop with 16000Hz (no resample branch)', async () => {
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

    // Stop recording (sampleRate is 16000, so line 175-177: if (origSampleRate === targetSampleRate) return audioData)
    const result = await recorder.stop();

    expect(recorder.isRecording).toBe(false);
    expect(result.audio16k).toBeInstanceOf(Float32Array);
    expect(result.audio16k.length).toBe(3);
    expect(mockTrack.stop).toHaveBeenCalled();
    expect(mockAudioContext.close).toHaveBeenCalled();
  });

  it('supports webkitOfflineAudioContext if window.OfflineAudioContext is undefined', async () => {
    mockAudioContext.sampleRate = 44100;
    delete window.OfflineAudioContext;

    const mockRenderedBuffer = {
      getChannelData: vi.fn().mockReturnValue(new Float32Array([0.05, -0.1])),
    };
    const mockOfflineContext = {
      createBuffer: vi.fn().mockReturnValue({ copyToChannel: vi.fn() }),
      createBufferSource: vi.fn().mockReturnValue({ connect: vi.fn(), start: vi.fn() }),
      destination: {},
      startRendering: vi.fn().mockResolvedValue(mockRenderedBuffer),
    };

    window.webkitOfflineAudioContext = function () {
      return mockOfflineContext;
    };

    const recorder = new AudioRecorder();
    await recorder.start();
    recorder.recordedChunks.push(new Float32Array([0.1, 0.2]));
    recorder.startTime = Date.now() - 1000;

    const result = await recorder.stop();
    expect(result.audio16k).toHaveLength(2);

    delete window.webkitOfflineAudioContext;
  });

  it('safely handles cleanup when audioContext is already closed or missing timer', () => {
    const recorder = new AudioRecorder();
    recorder.timer = null;
    recorder.processor = null;
    recorder.source = null;
    recorder.stream = null;
    recorder.audioContext = { state: 'closed', close: vi.fn() };

    recorder.cleanup();
    expect(recorder.audioContext.close).not.toHaveBeenCalled();
  });

  it('returns empty audio array when recorded audio duration is too short (<0.5s or empty)', async () => {
    const recorder = new AudioRecorder();
    await recorder.start();
    recorder.startTime = Date.now(); // 0s duration
    const result = await recorder.stop();
    expect(result.audio16k.length).toBe(0);
  });
});




