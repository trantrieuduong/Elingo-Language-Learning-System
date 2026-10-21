export const USER_CARD_TOPICS = [
  { id: 1, deckId: 101, name: 'Daily routines' },
  { id: 3, deckId: 102, name: 'Travel essentials' },
  { id: 2, deckId: 101, name: 'Work and study' },
]

export const USER_CARD_MOCK_DATA = [
  {
    id: 101,
    order: 1,
    topicId: 1,
    topicName: 'Daily routines',
    term: 'accomplish',
    pos: 'verb',
    translation: 'hoàn thành',
    explanationVi: 'Hoàn thành thành công một việc đã dự định hoặc cần thực hiện.',
    explanationEn: 'To succeed in doing or completing something.',
    examplesVi: 'Cô ấy đã hoàn thành mọi mục tiêu trong tuần.',
    examplesEn: 'She accomplished all of her goals for the week.',
    imageUrl: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSD4jwT_IMnKjiFWzYY_J1tF7MRVcHEwBLUbkKbEvC3TQ&s=10',
    phonetics: [
      { text: 'əˈkɑːmplɪʃ', audioUrl: '', locale: 'en-US' },
      { text: 'əˈkɒmplɪʃ', audioUrl: '', locale: 'en-GB' },
    ],
    flagsStarred: true,
  },
  {
    id: 102,
    order: 2,
    topicId: 1,
    topicName: 'Daily routines',
    term: 'commute',
    pos: 'verb / noun',
    translation: 'đi làm hằng ngày; quãng đường đi làm',
    explanationVi: 'Di chuyển thường xuyên giữa nhà và nơi làm việc hoặc học tập.',
    explanationEn: 'To travel regularly between home and work or school.',
    examplesVi: 'Tôi thường đi làm bằng tàu điện.',
    examplesEn: 'I commute to work by train.',
    imageUrl: '',
    phonetics: [{ text: 'kəˈmjuːt', audioUrl: '', locale: 'en-US' }],
    flagsStarred: false,
  },
  {
    id: 103,
    order: 3,
    topicId: 3,
    topicName: 'Travel essentials',
    term: 'itinerary',
    pos: 'noun',
    translation: 'lịch trình',
    explanationVi: 'Kế hoạch chi tiết về chuyến đi, gồm các điểm đến và thời gian.',
    explanationEn: 'A planned route or schedule for a journey.',
    examplesVi: 'Chúng tôi đã gửi lịch trình chuyến đi cho cả nhóm.',
    examplesEn: 'We shared the trip itinerary with the whole group.',
    imageUrl: '',
    phonetics: [{ text: 'aɪˈtɪnəreri', audioUrl: '', locale: 'en-US' }],
    flagsStarred: false,
  },
  {
    id: 104,
    order: 1,
    topicId: 2,
    topicName: 'Work and study',
    term: 'collaborate',
    pos: 'verb',
    translation: 'cộng tác',
    explanationVi: 'Làm việc cùng một hoặc nhiều người để đạt mục tiêu chung.',
    explanationEn: 'To work with another person or group to achieve something.',
    examplesVi: 'Các nhóm cộng tác để hoàn thành dự án.',
    examplesEn: 'The teams collaborated to complete the project.',
    imageUrl: '',
    phonetics: [{ text: 'kəˈlæbəreɪt', audioUrl: '', locale: 'en-US' }],
    flagsStarred: true,
  },
]

export const getMockCardsForTopic = (topic) => {
  if (!topic) return []

  return USER_CARD_MOCK_DATA.map((card) => ({
    ...card,
    topicId: topic.id,
    topicName: topic.name,
  }))
}
