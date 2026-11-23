import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LearningStatsTab from '@/modules/admin/pages/DashboardPage/tabs/LearningStatsTab';
import {
  fetchLearningActivity,
  fetchDeckStats,
  fetchLessonStats,
} from '@/modules/admin/services/adminStatsApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminStatsApi', () => ({
  fetchLearningActivity: vi.fn(),
  fetchDeckStats: vi.fn(),
  fetchLessonStats: vi.fn(),
}));

const mockActivityData = {
  totals: {
    totalFlashcard: 450,
    totalDictation: 200,
    totalShadowing: 150,
    totalBattle: 300,
    totalCheckIn: 800,
    grandTotal: 1900,
  },
  points: [
    {
      date: '2026-10-01',
      flashcard: 45,
      dictation: 20,
      shadowing: 15,
      battle: 30,
      checkIn: 80,
      total: 190,
    },
  ],
};

const mockDeckStats = {
  totalDecks: 150,
  byStatus: [
    { category: 'PUBLISHED', count: 120, percentage: 80.0 },
    { category: 'DRAFT', count: 20, percentage: 13.3 },
    { category: 'ARCHIVED', count: 10, percentage: 6.7 },
  ],
  byOwnerType: [
    { category: 'SYSTEM', count: 90, percentage: 60.0 },
    { category: 'USER', count: 60, percentage: 40.0 },
  ],
  byPremium: [
    { category: 'FREE', count: 100, percentage: 66.7 },
    { category: 'PREMIUM', count: 50, percentage: 33.3 },
  ],
  topSystemDecks: [
    {
      deckId: 1,
      title: 'System Deck Alpha',
      coverImageUrl: 'https://cdn.example.com/cover1.jpg',
      status: 'PUBLISHED',
      ownerType: 'SYSTEM',
      isPremium: false,
      learnerCount: 1500,
    },
  ],
  topUserDecks: [
    {
      deckId: 2,
      title: 'User Deck Beta',
      coverImageUrl: null,
      status: 'PUBLISHED',
      ownerType: 'USER',
      isPremium: true,
      learnerCount: 750,
    },
  ],
};

const mockLessonStats = {
  totalLessons: 80,
  byStatus: [
    { category: 'PUBLISHED', count: 65, percentage: 81.25 },
    { category: 'DRAFT', count: 15, percentage: 18.75 },
  ],
  byPremium: [
    { category: 'FREE', count: 50, percentage: 62.5 },
    { category: 'PREMIUM', count: 30, percentage: 37.5 },
  ],
  topLessons: [
    {
      lessonId: 10,
      title: 'Lesson 10 Coffee Shop',
      thumbnailUrl: 'https://cdn.example.com/thumb10.jpg',
      status: 'PUBLISHED',
      isPremium: true,
      learnerCount: 850,
    },
  ],
};

const sampleFilter = {
  from: '2026-10-01',
  to: '2026-10-31',
  granularity: 'DAY',
};

describe('LearningStatsTab component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n and data rendering [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders loading skeletons initially before API settles', async () => {
      fetchLearningActivity.mockImplementation(() => new Promise(() => {}));
      fetchDeckStats.mockImplementation(() => new Promise(() => {}));
      fetchLessonStats.mockImplementation(() => new Promise(() => {}));

      const { container } = render(
        <LearningStatsTab filter={sampleFilter} reloadKey={0} />
      );

      expect(container.querySelectorAll('[class*="skeletonCard"]').length).toBeGreaterThan(0);
    });

    it('renders error state when any API call rejects', async () => {
      fetchLearningActivity.mockRejectedValueOnce(new Error('Network error'));
      fetchDeckStats.mockResolvedValueOnce(mockDeckStats);
      fetchLessonStats.mockResolvedValueOnce(mockLessonStats);

      render(<LearningStatsTab filter={sampleFilter} reloadKey={0} />);

      await waitFor(() => {
        expect(screen.getByText('Network error')).toBeInTheDocument();
      });
    });

    it('renders KPI ribbon metrics, breakdown cards and tables correctly', async () => {
      fetchLearningActivity.mockResolvedValueOnce(mockActivityData);
      fetchDeckStats.mockResolvedValueOnce(mockDeckStats);
      fetchLessonStats.mockResolvedValueOnce(mockLessonStats);

      render(<LearningStatsTab filter={sampleFilter} reloadKey={0} />);

      await waitFor(() => {
        expect(screen.getByText('System Deck Alpha')).toBeInTheDocument();
      });

      // Kiểm tra KPI cards với định dạng số theo ngôn ngữ (1.900 cho vi-VN, 1,900 cho en-US)
      const expectedGrandTotal = Number(1900).toLocaleString(lng === 'vi' ? 'vi-VN' : 'en-US');
      expect(screen.getByText(expectedGrandTotal)).toBeInTheDocument();
      expect(screen.getByText('450')).toBeInTheDocument();

      // Kiểm tra bảng Top Decks mặc định là System
      expect(screen.getByText('System Deck Alpha')).toBeInTheDocument();
      expect(screen.queryByText('User Deck Beta')).not.toBeInTheDocument();

      // Kiểm tra bảng Top Lessons
      expect(screen.getByText('Lesson 10 Coffee Shop')).toBeInTheDocument();
    });

    it('switches between System and Community decks when segmented control is clicked', async () => {
      const user = userEvent.setup();
      fetchLearningActivity.mockResolvedValueOnce(mockActivityData);
      fetchDeckStats.mockResolvedValueOnce(mockDeckStats);
      fetchLessonStats.mockResolvedValueOnce(mockLessonStats);

      render(<LearningStatsTab filter={sampleFilter} reloadKey={0} />);

      await waitFor(() => {
        expect(screen.getByText('System Deck Alpha')).toBeInTheDocument();
      });

      const userDecksBtn = screen.getByRole('tab', {
        name: lng === 'vi' ? 'Bộ thẻ cộng đồng' : 'Community Decks',
      });
      await user.click(userDecksBtn);

      // Bây giờ phải hiển thị User Deck Beta và ẩn System Deck Alpha
      expect(screen.getByText('User Deck Beta')).toBeInTheDocument();
      expect(screen.queryByText('System Deck Alpha')).not.toBeInTheDocument();

      // Chuyển lại về System Decks
      const systemDecksBtn = screen.getByRole('tab', {
        name: lng === 'vi' ? 'Bộ thẻ hệ thống' : 'System Decks',
      });
      await user.click(systemDecksBtn);
      expect(screen.getByText('System Deck Alpha')).toBeInTheDocument();
    });

    it('switches between Decks and Lessons breakdown cards when segmented control is clicked', async () => {
      const user = userEvent.setup();
      fetchLearningActivity.mockResolvedValueOnce(mockActivityData);
      fetchDeckStats.mockResolvedValueOnce(mockDeckStats);
      fetchLessonStats.mockResolvedValueOnce(mockLessonStats);

      render(<LearningStatsTab filter={sampleFilter} reloadKey={0} />);

      await waitFor(() => {
        expect(screen.getByText('System Deck Alpha')).toBeInTheDocument();
      });

      // Mặc định ở tab Decks: có tiêu đề phân bổ của Decks
      const deckStatusTitle =
        lng === 'vi' ? 'Trạng thái phát hành bộ thẻ' : 'Deck Publication Status';
      expect(screen.getByText(deckStatusTitle)).toBeInTheDocument();

      // Click sang tab Lessons
      const lessonTabBtn = screen.getByRole('tab', {
        name: lng === 'vi' ? 'Bài học tương tác' : 'Interactive Lessons',
      });
      await user.click(lessonTabBtn);

      // Hiển thị tiêu đề phân bổ của Lessons
      const lessonStatusTitle =
        lng === 'vi' ? 'Trạng thái phát hành bài học' : 'Lesson Publication Status';
      expect(screen.getByText(lessonStatusTitle)).toBeInTheDocument();
    });

    it('navigates to deck detail page when deck link is clicked', async () => {
      const mockNavigate = vi.fn();
      const user = userEvent.setup();
      fetchLearningActivity.mockResolvedValueOnce(mockActivityData);
      fetchDeckStats.mockResolvedValueOnce(mockDeckStats);
      fetchLessonStats.mockResolvedValueOnce(mockLessonStats);

      render(
        <LearningStatsTab
          filter={sampleFilter}
          reloadKey={0}
          onNavigate={mockNavigate}
        />
      );

      await waitFor(() => {
        expect(screen.getByText('System Deck Alpha')).toBeInTheDocument();
      });

      const deckLink = screen.getByRole('link', { name: 'System Deck Alpha' });
      await user.click(deckLink);

      expect(mockNavigate).toHaveBeenCalledWith('/admin/decks/1');
    });
  });
});
