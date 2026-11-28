import { useEffect, useRef, useState } from 'react';

export default function useFlashcardAudio(resetKey) {
  const [playingAudioKey, setPlayingAudioKey] = useState(null);
  const audioRef = useRef(null);

  const stopAudio = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
    }
    setPlayingAudioKey(null);
  };

  useEffect(() => {
    stopAudio();
  }, [resetKey]);

  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause();
        audioRef.current = null;
      }
    };
  }, []);

  const handlePlayAudio = (event, audioUrl, key = 'default') => {
    event?.stopPropagation();
    event?.preventDefault();

    if (!audioUrl || typeof audioUrl !== 'string' || audioUrl.trim().length === 0) {
      return;
    }

    if (playingAudioKey === key) {
      stopAudio();
      return;
    }

    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
    }

    setPlayingAudioKey(key);
    try {
      if (!audioRef.current) {
        audioRef.current = new Audio();
      }
      audioRef.current.src = audioUrl;

      audioRef.current.onended = () => {
        setPlayingAudioKey(null);
      };

      audioRef.current.onerror = () => {
        console.warn('Không thể phát file âm thanh URL:', audioUrl);
        setPlayingAudioKey(null);
      };

      const playPromise = audioRef.current.play();
      if (playPromise !== undefined) {
        playPromise.catch((error) => {
          console.warn('Lỗi khi gọi play audio:', error);
          setPlayingAudioKey(null);
        });
      }
    } catch (error) {
      console.error('Lỗi khởi tạo Audio player:', error);
      setPlayingAudioKey(null);
    }
  };

  return { playingAudioKey, handlePlayAudio };
}
