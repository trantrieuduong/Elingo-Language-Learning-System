/**
 * Tiện ích xử lý chuỗi văn bản cho bài học, Dictation và Shadowing.
 */

/**
 * Tách một chuỗi văn bản thành danh sách từ dựa vào khoảng trắng liên tiếp.
 * Khớp chuẩn với backend: trim, tách theo /\s+/, loại bỏ phần tử rỗng.
 * BE tính điểm gợi ý Dictation theo số từ tách bằng khoảng trắng của transcriptNormalized.
 *
 * @param {string} text - Chuỗi văn bản đầu vào
 * @returns {string[]} Mảng các từ đã tách
 */
export const splitWords = (text) => {
  if (!text || typeof text !== 'string') return [];
  return text.trim().split(/\s+/).filter(Boolean);
};

/**
 * Làm sạch một từ: chuyển thành chữ thường, chỉ giữ lại các ký tự a-z và 0-9.
 *
 * @param {string} word - Từ cần làm sạch
 * @returns {string} Từ chỉ chứa ký tự chữ thường và số
 */
export const cleanWord = (word) => {
  if (!word || typeof word !== 'string') return '';
  return word.toLowerCase().replace(/[^a-z0-9]/g, '');
};
