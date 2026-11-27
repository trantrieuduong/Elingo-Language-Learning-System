import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import WebhookLogPage from '@/modules/admin/pages/WebhookLogPage/WebhookLogPage';
import * as adminPremiumApi from '@/modules/admin/services/adminPremiumApi';
import { LANGS, useLang } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminPremiumApi');

const mockLogsPage = {
  content: [
    {
      id: 12,
      providerId: 1,
      providerCode: 'SEPAY',
      transactionId: 100,
      eventId: 'sepay_evt_123456',
      signatureValid: true,
      status: 'PROCESSED',
      receivedAt: '2026-11-25T10:15:29',
      processedAt: '2026-11-25T10:15:30',
    },
  ],
  page: 1,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  isLast: true,
};

describe('WebhookLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminPremiumApi.searchWebhookLogs.mockResolvedValue(mockLogsPage);
    adminPremiumApi.getWebhookLogById.mockResolvedValue({
      ...mockLogsPage.content[0],
      payload: '{"id":123456,"transferAmount":99000}',
    });
  });

  describe.each(LANGS)('WebhookLogPage [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders webhook logs table and fetches with initialTransactionId', async () => {
      render(<WebhookLogPage initialTransactionId="100" onNavigate={vi.fn()} />);

      expect(await screen.findByText('sepay_evt_123456')).toBeInTheDocument();
      expect(adminPremiumApi.searchWebhookLogs).toHaveBeenCalledWith(
        expect.objectContaining({
          transactionId: '100',
          page: 1,
          size: 20,
        })
      );
    });

    it('opens detail modal and formats JSON payload', async () => {
      const user = userEvent.setup();
      render(<WebhookLogPage onNavigate={vi.fn()} />);
      await screen.findByText('sepay_evt_123456');

      const viewBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xem payload' : 'View Payload',
      });
      await user.click(viewBtn);

      await waitFor(() => {
        expect(adminPremiumApi.getWebhookLogById).toHaveBeenCalledWith(12);
      });

      expect(await screen.findByText(/"transferAmount": 99000/)).toBeInTheDocument();
    });
  });
});
