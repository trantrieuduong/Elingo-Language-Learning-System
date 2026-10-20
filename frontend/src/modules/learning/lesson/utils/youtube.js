/**
 * Trích xuất YouTube Video ID từ nhiều định dạng URL khác nhau.
 * Hỗ trợ: youtube.com/watch?v=, youtu.be/, /embed/, /shorts/, m.youtube.com
 *
 * @param {string|null|undefined} url - Đường dẫn video YouTube
 * @returns {string} ID video (11 ký tự) hoặc chuỗi rỗng nếu không hợp lệ
 */
export const getYouTubeVideoId = (url) => {
  if (!url || typeof url !== 'string') return ''

  // Regex trích xuất 11 ký tự ID từ các dạng URL YouTube phổ biến
  const regExp = /(?:youtube\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/)|youtu\.be\/|m\.youtube\.com\/watch\?(?:.*&)?v=)([a-zA-Z0-9_-]{11})/
  const match = url.match(regExp)
  return match ? match[1] : ''
}

/**
 * Lấy URL ảnh thumbnail mặc định (hqdefault) từ URL video YouTube.
 *
 * @param {string|null|undefined} url - Đường dẫn video YouTube
 * @returns {string} URL thumbnail hoặc chuỗi rỗng nếu không lấy được ID
 */
export const getYouTubeThumbnail = (url) => {
  const videoId = getYouTubeVideoId(url)
  return videoId ? `https://img.youtube.com/vi/${videoId}/hqdefault.jpg` : ''
}
