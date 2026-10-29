import { cleanWord } from '../../lesson/utils/text';

/**
 * Tính toán trạng thái hiển thị của từng từ tham chiếu theo dữ liệu người dùng gõ.
 *
 * Quy tắc:
 * - Từ đã tiết lộ (nằm trong revealedIndices) -> 'revealed', hiển thị nguyên từ refWord.
 * - Từ tham chiếu mà cleanWord ra rỗng (toàn ký tự đặc biệt/dấu câu) -> 'correct', hiển thị nguyên từ.
 * - So khớp theo vị trí: từ thứ i người dùng gõ so với từ tham chiếu thứ i.
 * - Nếu người dùng chưa gõ từ thứ i (cleanUser rỗng) -> 'empty', hiển thị '*' lặp theo độ dài cleanRef.
 * - Nếu khớp hoàn toàn (cleanUser === cleanRef) -> 'correct', hiển thị nguyên từ refWord.
 * - Nếu gõ dở hoặc sai -> 'incorrect', hiển thị phần đầu khớp liên tục tính từ ký tự đầu, còn lại là '*'.
 *
 * @param {Object} params
 * @param {string[]} params.refWords - Mảng các từ tham chiếu (lấy từ transcriptNormalized)
 * @param {string} params.userInput - Chuỗi văn bản người dùng đang nhập trong textarea
 * @param {Set<number>|number[]} params.revealedIndices - Tập hợp các index từ đã bấm gợi ý mở
 * @returns {Array<{ displayText: string, status: 'empty' | 'incorrect' | 'correct' | 'revealed' }>}
 */
export const getWordStatuses = ({ refWords = [], userInput = '', revealedIndices = new Set() }) => {
  const revealedSet = revealedIndices instanceof Set ? revealedIndices : new Set(revealedIndices);
  const userWords = userInput.trim().split(/\s+/).filter(Boolean);

  return refWords.map((refWord, index) => {
    // 1. Nếu từ đã được tiết lộ bằng nút gợi ý
    if (revealedSet.has(index)) {
      return {
        displayText: refWord,
        status: 'revealed',
      };
    }

    const cleanRef = cleanWord(refWord);

    // 2. Nếu từ tham chiếu toàn ký tự đặc biệt / rỗng sau khi làm sạch
    if (cleanRef.length === 0) {
      return {
        displayText: refWord,
        status: 'correct',
      };
    }

    const userWord = userWords[index] || '';
    const cleanUser = cleanWord(userWord);

    // 3. Người dùng chưa gõ tới từ này
    if (cleanUser.length === 0) {
      return {
        displayText: '*'.repeat(cleanRef.length),
        status: 'empty',
      };
    }

    // 4. Người dùng gõ khớp chính xác hoàn toàn
    if (cleanUser === cleanRef) {
      return {
        displayText: refWord,
        status: 'correct',
      };
    }

    // 5. Người dùng đang gõ dở hoặc gõ sai: tìm độ dài chuỗi ký tự đầu khớp liên tục
    let matchedLength = 0;
    for (let j = 0; j < cleanRef.length; j++) {
      if (j < cleanUser.length && cleanUser[j] === cleanRef[j]) {
        matchedLength++;
      } else {
        break;
      }
    }

    const visiblePart = cleanRef.slice(0, matchedLength);
    const starredPart = '*'.repeat(cleanRef.length - matchedLength);

    return {
      displayText: visiblePart + starredPart,
      status: 'incorrect',
    };
  });
};

/**
 * Kiểm tra xem tất cả các từ trong phân đoạn đã hoàn thành hay chưa.
 * Một phân đoạn hoàn thành khi mọi từ đều có trạng thái 'correct' hoặc 'revealed'.
 *
 * @param {Array<{ status: string }>} statuses - Danh sách trạng thái trả về từ getWordStatuses
 * @returns {boolean} true nếu toàn bộ từ đã đúng hoặc đã tiết lộ
 */
export const isAllDone = (statuses = []) => {
  if (!statuses || statuses.length === 0) return false;
  return statuses.every((w) => w.status === 'correct' || w.status === 'revealed');
};
