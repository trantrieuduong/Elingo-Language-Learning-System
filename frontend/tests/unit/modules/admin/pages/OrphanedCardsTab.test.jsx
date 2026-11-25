import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import OrphanedCardsTab from '@/modules/admin/pages/DeckManagementPage/components/OrphanedCardsTab';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminContentApi', () => ({
  fetchOrphanedCardStats: vi.fn(),
  fetchOrphanedCards: vi.fn(),
  cleanupOrphanedCards: vi.fn(),
}));

describe('OrphanedCardsTab component', () => {
  const sampleStats = {
    totalOrphanedCards: 120,
    safeToDeleteCards: 45,
    retainedInStudyCards: 75,
  };

  const sampleCard = {
    id: 505,
    order: 1,
    term: 'obsolete',
    pos: 'adjective',
    translation: 'lỗi thời, không còn sử dụng',
    explanationVi: 'Không còn được dùng vì đã có cái mới thay thế',
    imageUrl: 'https://cdn.elingo.pro/uploads/cards/obsolete.png',
    phonetics: [
      {
        text: '/ˌɒb.səlˈiːt/',
        audioUrl: 'https://cdn.elingo.pro/uploads/cards/obsolete_uk.mp3',
        locale: 'UK',
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n rendering and cleanup workflow [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders KPI stats and paginated orphaned cards list', async () => {
      adminContentApi.fetchOrphanedCardStats.mockResolvedValueOnce(sampleStats);
      adminContentApi.fetchOrphanedCards.mockResolvedValueOnce({
        content: [sampleCard],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      });

      render(<OrphanedCardsTab />);

      await waitFor(() => {
        expect(screen.getByText('120')).toBeInTheDocument();
        expect(screen.getByText('45')).toBeInTheDocument();
        expect(screen.getByText('75')).toBeInTheDocument();
        expect(screen.getByText('obsolete')).toBeInTheDocument();
        expect(screen.getByText('#505')).toBeInTheDocument();
      });

      expect(adminContentApi.fetchOrphanedCards).toHaveBeenCalledWith({
        page: 1,
        size: 20,
        sort: 'id,desc',
      });
    });

    it('disables cleanup button when safeToDeleteCards is 0', async () => {
      adminContentApi.fetchOrphanedCardStats.mockResolvedValueOnce({
        totalOrphanedCards: 10,
        safeToDeleteCards: 0,
        retainedInStudyCards: 10,
      });
      adminContentApi.fetchOrphanedCards.mockResolvedValueOnce({
        content: [sampleCard],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      });

      render(<OrphanedCardsTab />);

      const cleanupBtn = await screen.findByRole('button', {
        name: /Xóa card mồ côi an toàn|Clean Up Safe Cards/i,
      });

      await waitFor(() => {
        expect(cleanupBtn).toBeDisabled();
      });
    });

    it('opens confirmation modal, executes cleanup, shows outcome banner, and reloads', async () => {
      adminContentApi.fetchOrphanedCardStats
        .mockResolvedValueOnce(sampleStats)
        .mockResolvedValueOnce({
          totalOrphanedCards: 75,
          safeToDeleteCards: 0,
          retainedInStudyCards: 75,
        });

      adminContentApi.fetchOrphanedCards
        .mockResolvedValueOnce({
          content: [sampleCard],
          page: 1,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          isLast: true,
        })
        .mockResolvedValueOnce({
          content: [],
          page: 1,
          size: 20,
          totalElements: 0,
          totalPages: 1,
          isLast: true,
        });

      adminContentApi.cleanupOrphanedCards.mockResolvedValueOnce({
        scannedCount: 45,
        deletedCardCount: 45,
        deletedFileCount: 38,
      });

      const user = userEvent.setup();
      render(<OrphanedCardsTab />);

      const cleanupBtn = await screen.findByRole('button', {
        name: /Xóa card mồ côi an toàn|Clean Up Safe Cards/i,
      });
      await waitFor(() => {
        expect(cleanupBtn).not.toBeDisabled();
      });

      await user.click(cleanupBtn);

      const confirmBtn = screen.getByRole('button', {
        name: /Xác nhận xóa 45 card|Confirm Delete 45 Cards/i,
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(adminContentApi.cleanupOrphanedCards).toHaveBeenCalledTimes(1);
        expect(screen.getByRole('status')).toBeInTheDocument();
        expect(screen.getByText(/38/)).toBeInTheDocument();
      });

      expect(adminContentApi.fetchOrphanedCardStats).toHaveBeenCalledTimes(2);
    });
  });
});
