import { splitWords, cleanWord } from '../../lesson/utils/text';

/**
 * Tính khoảng cách Levenshtein giữa hai chuỗi ký tự.
 * @param {string} a
 * @param {string} b
 * @returns {number}
 */
function levenshteinDistance(a, b) {
  const m = a.length;
  const n = b.length;
  const dp = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0));

  for (let i = 0; i <= m; i++) dp[i][0] = i;
  for (let j = 0; j <= n; j++) dp[0][j] = j;

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      dp[i][j] = Math.min(
        dp[i - 1][j] + 1, // xóa
        dp[i][j - 1] + 1, // thêm
        dp[i - 1][j - 1] + cost // thay thế
      );
    }
  }

  return dp[m][n];
}

/**
 * Tính độ tương đồng ký tự giữa 2 từ (0..100).
 * Thay thế chỉ đạt tối đa 70 điểm để đảm bảo không ngang hàng với từ đọc đúng hoàn toàn.
 * @param {string} ref
 * @param {string} hyp
 * @returns {number}
 */
function charSimilarity(ref, hyp) {
  if (ref === hyp) return 100;
  const maxLen = Math.max(ref.length, hyp.length);
  if (maxLen === 0) return 100;
  const dist = levenshteinDistance(ref, hyp);
  const ratio = Math.max(0, 1 - dist / maxLen);
  // Chặn trên 70 nếu không khớp hoàn toàn
  return Math.min(70, Math.round(ratio * 100));
}

/**
 * Thuật toán căn chỉnh Sequence Alignment (Levenshtein ở cấp độ từ có truy vết):
 * - Mỗi từ tham chiếu khớp / thay thế / bỏ sót.
 * - accuracy: khớp -> 100; thay thế -> charSimilarity (max 70); bỏ sót -> 0.
 * - Phạt từ thừa: trừ 2 điểm cho mỗi từ nhận dạng thừa (tối đa trừ 20 điểm).
 *
 * @param {string} referenceText - Câu chuẩn (transcriptNormalized từ segment)
 * @param {string} recognizedText - Câu nhận dạng từ Whisper
 * @returns {{ score: number, wordsAccuracy: Array<{ word: string, accuracy: number }> }}
 */
const normalizeWords = (text, filterEmpty = false) => {
  const words = splitWords(text || '').map((word) => cleanWord(word));
  return filterEmpty ? words.filter(Boolean) : words;
};

const createAlignmentTables = (referenceLength, recognizedLength) => ({
  // Quy hoạch động Levenshtein ở mức từ.
  // costs[i][j] lưu chi phí tối thiểu giữa refWords[0..i-1] và recWords[0..j-1].
  costs: Array.from({ length: referenceLength + 1 }, () => new Array(recognizedLength + 1).fill(0)),
  // actions[i][j]: 'MATCH', 'SUB', 'DEL' (bỏ sót từ ref), 'INS' (từ thừa của rec).
  actions: Array.from({ length: referenceLength + 1 }, () => new Array(recognizedLength + 1).fill('')),
});

const initializeAlignmentEdges = (costs, actions, referenceLength, recognizedLength) => {
  for (let index = 1; index <= referenceLength; index++) {
    costs[index][0] = index;
    actions[index][0] = 'DEL';
  }
  for (let index = 1; index <= recognizedLength; index++) {
    costs[0][index] = index;
    actions[0][index] = 'INS';
  }
};

const selectAlignmentAction = (isMatch, matchSubCost, deleteCost, insertCost) => {
  const minCost = Math.min(matchSubCost, deleteCost, insertCost);
  if (minCost === matchSubCost) return { cost: minCost, action: isMatch ? 'MATCH' : 'SUB' };
  return { cost: minCost, action: minCost === deleteCost ? 'DEL' : 'INS' };
};

const fillAlignmentTables = (referenceWords, recognizedWords, costs, actions) => {
  for (let refIndex = 1; refIndex <= referenceWords.length; refIndex++) {
    for (let recIndex = 1; recIndex <= recognizedWords.length; recIndex++) {
      const isMatch = referenceWords[refIndex - 1] === recognizedWords[recIndex - 1];
      const selection = selectAlignmentAction(
        isMatch,
        costs[refIndex - 1][recIndex - 1] + (isMatch ? 0 : 1),
        costs[refIndex - 1][recIndex] + 1,
        costs[refIndex][recIndex - 1] + 1
      );
      costs[refIndex][recIndex] = selection.cost;
      actions[refIndex][recIndex] = selection.action;
    }
  }
};

const getAlignedWordScore = (referenceWord, recognizedWord, action) => {
  if (!referenceWord || action === 'MATCH') return 100;
  return charSimilarity(referenceWord, recognizedWord);
};

const traceAlignment = (referenceWords, recognizedWords, actions) => {
  // Truy vết ngược để gắn điểm cho từng từ tham chiếu.
  let refIndex = referenceWords.length;
  let recIndex = recognizedWords.length;
  const scores = new Array(refIndex).fill(0);
  let extraWordsCount = 0;

  while (refIndex > 0 || recIndex > 0) {
    const action = actions[refIndex][recIndex];
    const consumesBoth = refIndex > 0 && recIndex > 0 && (action === 'MATCH' || action === 'SUB');
    if (consumesBoth) {
      scores[refIndex - 1] = getAlignedWordScore(referenceWords[refIndex - 1], recognizedWords[recIndex - 1], action);
      refIndex--;
      recIndex--;
    } else if (refIndex > 0 && (action === 'DEL' || recIndex === 0)) {
      // Từ tham chiếu bị bỏ sót (người học không nói hoặc mô hình không nghe thấy).
      scores[refIndex - 1] = referenceWords[refIndex - 1] === '' ? 100 : 0;
      refIndex--;
    } else if (recIndex > 0) {
      // Từ thừa mà người học nói chèn vào.
      extraWordsCount++;
      recIndex--;
    } else {
      break;
    }
  }
  return { scores, extraWordsCount };
};

// Xây dựng mảng wordsAccuracy đúng thứ tự của referenceText.
const buildWordsAccuracy = (rawWords, scores) => rawWords.map((word, index) => ({
  word,
  accuracy: Math.max(0, Math.min(100, scores[index])),
}));

const calculateFinalScore = (wordsAccuracy, referenceLength, extraWordsCount) => {
  // Tính điểm tổng: trung bình các từ trừ điểm phạt từ thừa.
  const sum = wordsAccuracy.reduce((total, word) => total + word.accuracy, 0);
  const average = referenceLength > 0 ? sum / referenceLength : 0;
  return Math.max(0, Math.min(100, Math.round(average - Math.min(20, extraWordsCount * 2))));
};

// Chấm điểm bài Shadowing giữa câu chuẩn (transcriptNormalized) và câu Whisper nhận dạng
export function scoreShadowing(referenceText, recognizedText) {
  if (!referenceText || typeof referenceText !== 'string' || !referenceText.trim()) {
    return { score: 0, wordsAccuracy: [] };
  }

  const rawReferenceWords = splitWords(referenceText);
  const referenceWords = normalizeWords(referenceText);
  const recognizedWords = normalizeWords(recognizedText, true);
  const { costs, actions } = createAlignmentTables(referenceWords.length, recognizedWords.length);

  initializeAlignmentEdges(costs, actions, referenceWords.length, recognizedWords.length);
  fillAlignmentTables(referenceWords, recognizedWords, costs, actions);

  const { scores, extraWordsCount } = traceAlignment(referenceWords, recognizedWords, actions);
  const wordsAccuracy = buildWordsAccuracy(rawReferenceWords, scores);
  return {
    score: calculateFinalScore(wordsAccuracy, referenceWords.length, extraWordsCount),
    wordsAccuracy,
  };
}
