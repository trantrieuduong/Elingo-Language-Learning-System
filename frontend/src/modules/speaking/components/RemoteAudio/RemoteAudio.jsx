import { useEffect, useRef } from 'react';

const RemoteAudio = ({ stream, onAutoplayBlocked }) => {
  const ref = useRef(null);

  useEffect(() => {
    const el = ref.current;
    if (!el || !stream) return;
    el.srcObject = stream;
    el.play().catch(() => onAutoplayBlocked?.());
    return () => {
      el.srcObject = null;
    };
  }, [stream, onAutoplayBlocked]);

  return <audio ref={ref} autoPlay playsInline />;
};

export default RemoteAudio;
