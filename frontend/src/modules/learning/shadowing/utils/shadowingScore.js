import { splitWords, cleanWord } from '../../lesson/utils/text'

/**
 * Tính khoảng cách Levenshtein giữa hai chuỗi ký tự.
 * @param {string} a
 * @param {string} b
 * @returns {number}
 */
function levenshteinDistance(a, b) {
  const m = a.length
  const n = b.length
  const dp = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0))

  for (let i = 0; i <= m; i++) dp[i][0] = i
  for (let j = 0; j <= n; j++) dp[0][j] = j

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1
      dp[i][j] = Math.min(
        dp[i - 1][j] + 1, // xóa
        dp[i][j - 1] + 1, // thêm
        dp[i - 1][j - 1] + cost // thay thế
      )
    }
  }

  return dp[m][n]
}

/**
 * Tính độ tương đồng ký tự giữa 2 từ (0..100).
 * Thay thế chỉ đạt tối đa 70 điểm để đảm bảo không ngang hàng với từ đọc đúng hoàn toàn.
 * @param {string} ref
 * @param {string} hyp
 * @returns {number}
 */
function charSimilarity(ref, hyp) {
  if (ref === hyp) return 100
  const maxLen = Math.max(ref.length, hyp.length)
  if (maxLen === 0) return 100
  const dist = levenshteinDistance(ref, hyp)
  const ratio = Math.max(0, 1 - dist / maxLen)
  // Chặn trên 70 nếu không khớp hoàn toàn
  return Math.min(70, Math.round(ratio * 100))
}

/**
 * Chấm điểm bài Shadowing giữa câu chuẩn (transcriptNormalized) và câu Whisper nhận dạng.
 *
 * Thuật toán căn chỉnh Sequence Alignment (Levenshtein ở cấp độ từ có truy vết):
 * - Mỗi từ tham chiếu khớp / thay thế / bỏ sót.
 * - accuracy: khớp -> 100; thay thế -> charSimilarity (max 70); bỏ sót -> 0.
 * - Phạt từ thừa: trừ 2 điểm cho mỗi từ nhận dạng thừa (tối đa trừ 20 điểm).
 *
 * @param {string} referenceText - Câu chuẩn (transcriptNormalized từ segment)
 * @param {string} recognizedText - Câu nhận dạng từ Whisper
 * @returns {{ score: number, wordsAccuracy: Array<{ word: string, accuracy: number }> }}
 */
export function scoreShadowing(referenceText, recognizedText) {
  if (!referenceText || typeof referenceText !== 'string' || !referenceText.trim()) {
    return { score: 0, wordsAccuracy: [] }
  }

  const rawRefWords = splitWords(referenceText)
  const refWords = rawRefWords.map((w) => cleanWord(w))

  const rawRecWords = splitWords(recognizedText || '')
  const recWords = rawRecWords.map((w) => cleanWord(w)).filter(Boolean)

  const m = refWords.length
  const n = recWords.length

  // Quy hoạch động Levenshtein ở mức từ
  // dp[i][j] lưu chi phí tối thiểu giữa refWords[0..i-1] và recWords[0..j-1]
  const dp = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0))
  // backtrack[i][j]: 'MATCH', 'SUB', 'DEL' (bỏ sót từ ref), 'INS' (từ thừa của rec)
  const backtrack = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(''))

  for (let i = 1; i <= m; i++) {
    dp[i][0] = i
    backtrack[i][0] = 'DEL'
  }
  for (let j = 1; j <= n; j++) {
    dp[0][j] = j
    backtrack[0][j] = 'INS'
  }

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      const isMatch = refWords[i - 1] === recWords[j - 1]
      const matchSubCost = dp[i - 1][j - 1] + (isMatch ? 0 : 1)
      const delCost = dp[i - 1][j] + 1
      const insCost = dp[i][j - 1] + 1

      const minCost = Math.min(matchSubCost, delCost, insCost)
      dp[i][j] = minCost

      if (minCost === matchSubCost) {
        backtrack[i][j] = isMatch ? 'MATCH' : 'SUB'
      } else if (minCost === delCost) {
        backtrack[i][j] = 'DEL'
      } else {
        backtrack[i][j] = 'INS'
      }
    }
  }

  // Truy vết ngược để gắn điểm cho từng từ tham chiếu
  let i = m
  let j = n
  const alignedRefScores = new Array(m).fill(0)
  let extraWordsCount = 0

  while (i > 0 || j > 0) {
    const action = backtrack[i][j]
    if (i > 0 && j > 0 && (action === 'MATCH' || action === 'SUB')) {
      const refClean = refWords[i - 1]
      const recClean = recWords[j - 1]
      if (refClean === '') {
        alignedRefScores[i - 1] = 100
      } else if (action === 'MATCH') {
        alignedRefScores[i - 1] = 100
      } else {
        alignedRefScores[i - 1] = charSimilarity(refClean, recClean)
      }
      i--
      j--
    } else if (i > 0 && (action === 'DEL' || j === 0)) {
      // Từ tham chiếu bị bỏ sót (người học không nói hoặc mô hình không nghe thấy)
      alignedRefScores[i - 1] = refWords[i - 1] === '' ? 100 : 0
      i--
    } else if (j > 0) {
      // Từ thừa mà người học nói chèn vào
      extraWordsCount++
      j--
    } else {
      break
    }
  }

  // Xây dựng mảng wordsAccuracy đúng thứ tự của referenceText
  const wordsAccuracy = rawRefWords.map((rawWord, idx) => ({
    word: rawWord,
    accuracy: Math.max(0, Math.min(100, alignedRefScores[idx])),
  }))

  // Tính điểm tổng: trung bình các từ trừ điểm phạt từ thừa
  const sumAccuracy = wordsAccuracy.reduce((acc, curr) => acc + curr.accuracy, 0)
  const avgScore = m > 0 ? sumAccuracy / m : 0
  const penalty = Math.min(20, extraWordsCount * 2)
  const finalScore = Math.max(0, Math.min(100, Math.round(avgScore - penalty)))

  return {
    score: finalScore,
    wordsAccuracy,
  }
}
