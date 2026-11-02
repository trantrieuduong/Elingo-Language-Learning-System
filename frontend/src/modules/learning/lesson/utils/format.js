/**
 * Định dạng thời lượng bài học tính bằng mili-giây sang mm:ss hoặc h:mm:ss.
 *
 * @param {number|null|undefined} ms - Thời lượng tính bằng mili-giây
 * @returns {string} Chuỗi thời lượng (ví dụ "02:30", "01:15:00") hoặc chuỗi rỗng nếu không hợp lệ
 */
export const formatDuration = (ms) => {
  if (ms === null || ms === undefined || typeof ms !== 'number' || isNaN(ms) || ms <= 0) {
    return '';
  }

  const totalSeconds = Math.floor(ms / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  const pad = (num) => String(num).padStart(2, '0');

  if (hours > 0) {
    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
  }
  return `${pad(minutes)}:${pad(seconds)}`;
};
