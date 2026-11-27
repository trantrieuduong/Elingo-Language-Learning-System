import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import useFlashcardAudio from '@/modules/learning/flashcard/hooks/useFlashcardAudio'

describe('useFlashcardAudio', () => {
  let audioInstances
  const originalAudio = global.Audio

  beforeEach(() => {
    audioInstances = []
    global.Audio = vi.fn().mockImplementation(function MockAudio() {
      const instance = {
        src: '',
        currentTime: 0,
        onended: null,
        onerror: null,
        play: vi.fn().mockResolvedValue(undefined),
        pause: vi.fn(),
      }
      audioInstances.push(instance)
      return instance
    })
  })

  afterEach(() => {
    global.Audio = originalAudio
  })

  it('initializes with null playingAudioKey', () => {
    const { result } = renderHook(() => useFlashcardAudio('card-1'))
    expect(result.current.playingAudioKey).toBeNull()
  })

  it('ignores empty or invalid audioUrl', () => {
    const { result } = renderHook(() => useFlashcardAudio('card-1'))

    act(() => {
      result.current.handlePlayAudio(undefined, '', 'phonetic-0')
      result.current.handlePlayAudio(undefined, '   ', 'phonetic-0')
      result.current.handlePlayAudio(undefined, null, 'phonetic-0')
    })

    expect(audioInstances).toHaveLength(0)
    expect(result.current.playingAudioKey).toBeNull()
  })

  it('plays audio and toggles playing state off when called again with same key', () => {
    const { result } = renderHook(() => useFlashcardAudio('card-1'))
    const event = { stopPropagation: vi.fn(), preventDefault: vi.fn() }

    act(() => {
      result.current.handlePlayAudio(event, 'https://example.com/us.mp3', 'phonetic-0')
    })

    expect(event.stopPropagation).toHaveBeenCalledTimes(1)
    expect(event.preventDefault).toHaveBeenCalledTimes(1)
    expect(audioInstances).toHaveLength(1)
    expect(audioInstances[0].src).toBe('https://example.com/us.mp3')
    expect(audioInstances[0].play).toHaveBeenCalledTimes(1)
    expect(result.current.playingAudioKey).toBe('phonetic-0')

    act(() => {
      result.current.handlePlayAudio(event, 'https://example.com/us.mp3', 'phonetic-0')
    })

    expect(audioInstances[0].pause).toHaveBeenCalledTimes(1)
    expect(audioInstances[0].currentTime).toBe(0)
    expect(result.current.playingAudioKey).toBeNull()
  })

  it('pauses current audio and plays new audio when another key is triggered', () => {
    const { result } = renderHook(() => useFlashcardAudio('card-1'))

    act(() => {
      result.current.handlePlayAudio(undefined, 'https://example.com/us.mp3', 'phonetic-0')
    })
    expect(result.current.playingAudioKey).toBe('phonetic-0')

    act(() => {
      result.current.handlePlayAudio(undefined, 'https://example.com/uk.mp3', 'phonetic-1')
    })

    expect(audioInstances[0].pause).toHaveBeenCalledTimes(1)
    expect(audioInstances[0].src).toBe('https://example.com/uk.mp3')
    expect(audioInstances[0].play).toHaveBeenCalledTimes(2)
    expect(result.current.playingAudioKey).toBe('phonetic-1')
  })

  it('clears playingAudioKey when audio ends or errors', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    const { result } = renderHook(() => useFlashcardAudio('card-1'))

    act(() => {
      result.current.handlePlayAudio(undefined, 'https://example.com/us.mp3', 'phonetic-0')
    })
    expect(result.current.playingAudioKey).toBe('phonetic-0')

    act(() => {
      audioInstances[0].onended()
    })
    expect(result.current.playingAudioKey).toBeNull()

    act(() => {
      result.current.handlePlayAudio(undefined, 'https://example.com/us.mp3', 'phonetic-0')
    })
    expect(result.current.playingAudioKey).toBe('phonetic-0')

    act(() => {
      audioInstances[0].onerror()
    })
    expect(result.current.playingAudioKey).toBeNull()
    expect(warnSpy).toHaveBeenCalled()
    warnSpy.mockRestore()
  })

  it('stops audio when resetKey changes and pauses on unmount', () => {
    const { result, rerender, unmount } = renderHook(({ key }) => useFlashcardAudio(key), {
      initialProps: { key: 'card-1' },
    })

    act(() => {
      result.current.handlePlayAudio(undefined, 'https://example.com/us.mp3', 'phonetic-0')
    })
    expect(result.current.playingAudioKey).toBe('phonetic-0')

    rerender({ key: 'card-2' })
    expect(audioInstances[0].pause).toHaveBeenCalledTimes(1)
    expect(result.current.playingAudioKey).toBeNull()

    act(() => {
      result.current.handlePlayAudio(undefined, 'https://example.com/uk.mp3', 'phonetic-1')
    })
    expect(audioInstances[0].pause).toHaveBeenCalledTimes(2)

    unmount()
    expect(audioInstances[0].pause).toHaveBeenCalledTimes(3)
  })
})
