/** ms -> "mm:ss" (phút có thể >= 60, ví dụ 75:03) */
export const formatMs = (ms) => {
  const total = Math.max(0, Math.floor((Number(ms) || 0) / 1000));
  const mm = String(Math.floor(total / 60)).padStart(2, '0');
  const ss = String(total % 60).padStart(2, '0');
  return `${mm}:${ss}`;
};

/** "mm:ss" -> ms; trả null nếu sai định dạng hoặc ss >= 60 */
export const parseTime = (text) => {
  const m = /^(\d{1,3}):([0-5]\d)$/.exec((text || '').trim());
  return m ? (Number(m[1]) * 60 + Number(m[2])) * 1000 : null;
};

/** Chỉ giữ chữ số, tự chèn ':' sau 2 số cuối: "105" -> "1:05", "0130" -> "01:30" (tối đa 5 chữ số) */
export const maskTimeInput = (raw) => {
  const digits = (raw || '').replace(/\D/g, '').slice(0, 5);
  if (digits.length <= 2) return digits;
  return `${digits.slice(0, -2)}:${digits.slice(-2)}`;
};

/** Kẹp giá trị ms vào [0, maxMs] (maxMs = durationMs của lesson, có thể null) */
export const clampMs = (ms, maxMs) => {
  const v = Math.max(0, ms);
  return maxMs ? Math.min(v, maxMs) : v;
};
