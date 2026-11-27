// Hỗ trợ: watch?v=, youtu.be/, /embed/, /shorts/ — PHẢI khớp regex BE (YoutubeUrlUtils)
const YOUTUBE_ID_REGEX =
  /^(?:https?:\/\/)?(?:www\.|m\.)?(?:youtube\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/)|youtu\.be\/)([A-Za-z0-9_-]{11})(?:[&?#/].*)?$/;

export const extractVideoId = (url) => {
  const m = YOUTUBE_ID_REGEX.exec((url || '').trim());
  return m ? m[1] : null;
};

let apiPromise = null;

export const loadYouTubeIframeApi = () => {
  if (typeof window !== 'undefined' && window.YT && window.YT.Player) {
    return Promise.resolve(window.YT);
  }
  if (apiPromise) return apiPromise;

  apiPromise = new Promise((resolve, reject) => {
    const previous = window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady = () => {
      if (typeof previous === 'function') previous();
      resolve(window.YT);
    };

    const existingScript = document.querySelector('script[src*="youtube.com/iframe_api"]');
    if (existingScript) return;

    const script = document.createElement('script');
    script.src = 'https://www.youtube.com/iframe_api';
    script.async = true;
    script.onerror = () => {
      apiPromise = null;
      reject(new Error('YT_API_LOAD_FAILED'));
    };
    document.head.appendChild(script);
  });

  return apiPromise;
};
