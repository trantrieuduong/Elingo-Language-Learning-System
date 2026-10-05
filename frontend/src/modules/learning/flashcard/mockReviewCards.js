/**
 * mockReviewCards.js — Dữ liệu mẫu (Mock data) danh sách flashcard đến hạn ôn tập.
 * Khớp chuẩn cấu trúc dữ liệu ReviewCardResponse và CardResponse của hệ thống Elingo.
 */

export const MOCK_REVIEW_CARDS = [
  {
    id: 101,
    order: 1,
    term: 'Eloquent',
    pos: 'adjective',
    translation: 'Hùng biện, có tài ăn nói',
    explanationVi: 'Có khả năng diễn đạt lưu loát, truyền cảm và đầy sức thuyết phục bằng lời nói hoặc câu chữ.',
    explanationEn: 'Fluent or persuasive in speaking or writing; clearly expressing or indicating something.',
    examplesVi: 'Cô ấy đã có một bài phát biểu đầy hùng biện về vai trò của công nghệ trong giáo dục.',
    examplesEn: 'She gave an eloquent speech on the pivotal role of technology in modern education.',
    imageUrl: 'https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=600&auto=format&fit=crop&q=80',
    phonetics: [
      {
        text: '/ˈel.ə.kwənt/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=eloquent&type=1',
        locale: 'en-UK'
      },
      {
        text: '/ˈel.ə.kwənt/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=eloquent&type=2',
        locale: 'en-US'
      }
    ],
    flagsStarred: true,
    flagsHidden: false,
    srsNextReviewAt: '2026-10-05T08:00:00',
    srsInterval: 1,
    srsEaseFactor: 2.5,
    srsLastGrade: 2,
    deckTitle: 'Oxford 3000 Cốt Lõi',
    topicName: 'Giao tiếp & Thuyết trình'
  },
  {
    id: 102,
    order: 2,
    term: 'Resilience',
    pos: 'noun',
    translation: 'Sự kiên cường, khả năng phục hồi',
    explanationVi: 'Năng lực vượt qua áp lực và phục hồi nhanh chóng sau nghịch cảnh, thất bại hoặc khó khăn.',
    explanationEn: 'The capacity to withstand or to recover quickly from difficulties; toughness.',
    examplesVi: 'Sự kiên cường của cả tập thể đã giúp công ty vượt qua cuộc khủng hoảng kinh tế.',
    examplesEn: 'The team showed immense resilience to bounce back from their earlier setback.',
    imageUrl: 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600&auto=format&fit=crop&q=80',
    phonetics: [
      {
        text: '/rɪˈzɪl.jəns/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=resilience&type=1',
        locale: 'en-UK'
      },
      {
        text: '/rɪˈzɪl.jəns/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=resilience&type=2',
        locale: 'en-US'
      }
    ],
    flagsStarred: false,
    flagsHidden: false,
    srsNextReviewAt: '2026-10-05T09:30:00',
    srsInterval: 3,
    srsEaseFactor: 2.6,
    srsLastGrade: 3,
    deckTitle: 'IELTS Advanced Vocabulary',
    topicName: 'Tâm lý & Phẩm chất'
  },
  {
    id: 103,
    order: 3,
    term: 'Meticulous',
    pos: 'adjective',
    translation: 'Tỉ mỉ, cẩn thận từng chi tiết',
    explanationVi: 'Cực kỳ cẩn trọng, chú ý đến từng chi tiết nhỏ nhất để bảo đảm độ chính xác tối đa.',
    explanationEn: 'Showing great attention to detail; very careful and precise.',
    examplesVi: 'Anh ấy tiến hành nghiên cứu thị trường một cách vô cùng tỉ mỉ trước khi ra mắt sản phẩm.',
    examplesEn: 'He did meticulous research before launching the new software product.',
    imageUrl: 'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=600&auto=format&fit=crop&q=80',
    phonetics: [
      {
        text: '/məˈtɪk.jə.ləs/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=meticulous&type=1',
        locale: 'en-UK'
      },
      {
        text: '/məˈtɪk.jə.ləs/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=meticulous&type=2',
        locale: 'en-US'
      }
    ],
    flagsStarred: true,
    flagsHidden: false,
    srsNextReviewAt: '2026-10-05T10:15:00',
    srsInterval: 2,
    srsEaseFactor: 2.3,
    srsLastGrade: 1,
    deckTitle: 'Business English Core',
    topicName: 'Kỹ năng làm việc'
  },
  {
    id: 104,
    order: 4,
    term: 'Serendipity',
    pos: 'noun',
    translation: 'Sự tình cờ may mắn',
    explanationVi: 'Hiện tượng tình cờ khám phá hay bắt gặp những điều tốt đẹp, may mắn ngoài dự kiến.',
    explanationEn: 'The occurrence and development of events by chance in a happy or beneficial way.',
    examplesVi: 'Cuộc gặp gỡ định mệnh giữa hai nhà sáng lập hoàn toàn là một sự tình cờ may mắn.',
    examplesEn: 'Finding each other at the conference was a pure stroke of serendipity.',
    imageUrl: 'https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=600&auto=format&fit=crop&q=80',
    phonetics: [
      {
        text: '/ˌser.ənˈdɪp.ə.ti/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=serendipity&type=1',
        locale: 'en-UK'
      },
      {
        text: '/ˌser.ənˈdɪp.ə.t̬i/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=serendipity&type=2',
        locale: 'en-US'
      }
    ],
    flagsStarred: false,
    flagsHidden: false,
    srsNextReviewAt: '2026-10-05T11:00:00',
    srsInterval: 4,
    srsEaseFactor: 2.5,
    srsLastGrade: 2,
    deckTitle: 'Oxford 3000 Cốt Lõi',
    topicName: 'Cảm xúc & Đời sống'
  },
  {
    id: 105,
    order: 5,
    term: 'Ubiquitous',
    pos: 'adjective',
    translation: 'Có mặt ở khắp nơi, phổ biến rộng rãi',
    explanationVi: 'Xuất hiện ở khắp mọi nơi, rất quen thuộc và dễ dàng bắt gặp bất cứ lúc nào.',
    explanationEn: 'Present, appearing, or found everywhere at the same time.',
    examplesVi: 'Điện thoại thông minh giờ đây đã trở thành thiết bị phổ biến ở mọi ngõ ngách đời sống.',
    examplesEn: 'Smartphones and digital apps have become ubiquitous in daily life.',
    imageUrl: 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=600&auto=format&fit=crop&q=80',
    phonetics: [
      {
        text: '/juːˈbɪk.wɪ.təs/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=ubiquitous&type=1',
        locale: 'en-UK'
      },
      {
        text: '/juːˈbɪk.wə.t̬əs/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=ubiquitous&type=2',
        locale: 'en-US'
      }
    ],
    flagsStarred: false,
    flagsHidden: false,
    srsNextReviewAt: '2026-10-05T11:45:00',
    srsInterval: 6,
    srsEaseFactor: 2.7,
    srsLastGrade: 3,
    deckTitle: 'Technology & AI Vocabulary',
    topicName: 'Thời đại kỹ thuật số'
  },
  {
    id: 106,
    order: 6,
    term: 'Pragmatic',
    pos: 'adjective',
    translation: 'Thực tế, thực dụng',
    explanationVi: 'Giải quyết vấn đề dựa trên thực tế và hiệu quả hành động cụ thể hơn là lý thuyết suông.',
    explanationEn: 'Dealing with things sensibly and realistically in a way that is based on practical rather than theoretical considerations.',
    examplesVi: 'Chúng ta cần đưa ra một giải pháp thực tế và khả thi trong giai đoạn này.',
    examplesEn: 'In business, adopting a pragmatic approach often yields sustainable results.',
    imageUrl: 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=600&auto=format&fit=crop&q=80',
    phonetics: [
      {
        text: '/præɡˈmæt.ɪk/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=pragmatic&type=1',
        locale: 'en-UK'
      },
      {
        text: '/præɡˈmæt̬.ɪk/',
        audioUrl: 'https://dict.youdao.com/dictvoice?audio=pragmatic&type=2',
        locale: 'en-US'
      }
    ],
    flagsStarred: true,
    flagsHidden: false,
    srsNextReviewAt: '2026-10-05T12:00:00',
    srsInterval: 2,
    srsEaseFactor: 2.4,
    srsLastGrade: 2,
    deckTitle: 'Business English Core',
    topicName: 'Chiến lược & Đàm phán'
  }
]
