import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  getAllPlans,
  getPlanById,
  createPlan,
  updatePlan,
  disablePlan,
  enablePlan,
  searchTransactions,
  getTransactionById,
  confirmTransaction,
  refundTransaction,
  searchWebhookLogs,
  getWebhookLogById,
} from '@/modules/admin/services/adminPremiumApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('adminPremiumApi service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Plans APIs', () => {
    it('getAllPlans calls GET /admin/premium/plans', async () => {
      apiClient.get.mockResolvedValueOnce({ data: { data: [{ id: 1, name: 'Pro' }] } });
      const res = await getAllPlans();
      expect(apiClient.get).toHaveBeenCalledWith('/admin/premium/plans');
      expect(res).toHaveLength(1);
    });

    it('getPlanById calls GET /admin/premium/plans/{id}', async () => {
      apiClient.get.mockResolvedValueOnce({ data: { data: { id: 2, name: 'VIP' } } });
      const res = await getPlanById(2);
      expect(apiClient.get).toHaveBeenCalledWith('/admin/premium/plans/2');
      expect(res.id).toBe(2);
    });

    it('createPlan calls POST /admin/premium/plans with normalized payload', async () => {
      apiClient.post.mockResolvedValueOnce({ data: { data: { id: 3, isActive: false } } });
      const res = await createPlan({
        name: '  Pro Quarterly  ',
        price: '249000',
        currency: 'vnd',
        durationDays: '90',
      });
      expect(apiClient.post).toHaveBeenCalledWith('/admin/premium/plans', {
        name: 'Pro Quarterly',
        price: 249000,
        currency: 'VND',
        durationDays: 90,
      });
      expect(res.id).toBe(3);
    });

    it('updatePlan calls PUT /admin/premium/plans/{id}', async () => {
      apiClient.put.mockResolvedValueOnce({ data: { data: { id: 1, price: 89000 } } });
      await updatePlan(1, { name: ' Pro Promo ', price: 89000 });
      expect(apiClient.put).toHaveBeenCalledWith('/admin/premium/plans/1', {
        name: 'Pro Promo',
        price: 89000,
      });
    });

    it('disablePlan and enablePlan call respective PUT endpoints', async () => {
      apiClient.put.mockResolvedValue({ data: { data: { id: 1 } } });
      await disablePlan(1);
      expect(apiClient.put).toHaveBeenCalledWith('/admin/premium/plans/1/disable');
      await enablePlan(1);
      expect(apiClient.put).toHaveBeenCalledWith('/admin/premium/plans/1/enable');
    });
  });

  describe('Transactions APIs', () => {
    it('searchTransactions calls GET /admin/premium/transactions with 1-indexed page and filters', async () => {
      apiClient.get.mockResolvedValueOnce({
        data: {
          data: { content: [], page: 2, size: 20, totalElements: 0, totalPages: 1, isLast: true },
        },
      });

      await searchTransactions({
        keyword: '  ELGO  ',
        status: 'PENDING',
        planId: '1',
        userId: '10',
        createdFrom: '2026-11-01',
        createdTo: '2026-11-25',
        page: 2,
        size: 20,
        sort: 'createdAt,desc',
      });

      expect(apiClient.get).toHaveBeenCalledWith('/admin/premium/transactions', {
        params: {
          keyword: 'ELGO',
          status: 'PENDING',
          planId: 1,
          userId: 10,
          createdFrom: '2026-11-01',
          createdTo: '2026-11-25',
          page: 2,
          size: 20,
          sort: 'createdAt,desc',
        },
      });
    });

    it('getTransactionById, confirmTransaction, refundTransaction call expected endpoints', async () => {
      apiClient.get.mockResolvedValueOnce({ data: { data: { id: 100 } } });
      await getTransactionById(100);
      expect(apiClient.get).toHaveBeenCalledWith('/admin/premium/transactions/100');

      apiClient.put.mockResolvedValueOnce({ data: { data: { id: 100, status: 'SUCCESS' } } });
      await confirmTransaction(100, '  Verified bank statement  ');
      expect(apiClient.put).toHaveBeenCalledWith('/admin/premium/transactions/100/confirm', {
        reason: 'Verified bank statement',
      });

      apiClient.put.mockResolvedValueOnce({ data: { data: { id: 100, status: 'REFUNDED' } } });
      await refundTransaction(100, '  Support ticket #12  ');
      expect(apiClient.put).toHaveBeenCalledWith('/admin/premium/transactions/100/refund', {
        reason: 'Support ticket #12',
      });
    });
  });

  describe('Webhook Logs APIs', () => {
    it('searchWebhookLogs calls GET /admin/premium/webhook-logs with normalized params', async () => {
      apiClient.get.mockResolvedValueOnce({
        data: { data: { content: [], page: 1, size: 20, totalElements: 0, totalPages: 1 } },
      });

      await searchWebhookLogs({
        providerId: '1',
        transactionId: '100',
        status: 'PROCESSED',
        signatureValid: 'true',
        receivedFrom: '2026-11-01',
        receivedTo: '2026-11-25',
        page: 1,
        size: 20,
      });

      expect(apiClient.get).toHaveBeenCalledWith('/admin/premium/webhook-logs', {
        params: {
          providerId: 1,
          transactionId: 100,
          status: 'PROCESSED',
          signatureValid: true,
          receivedFrom: '2026-11-01',
          receivedTo: '2026-11-25',
          page: 1,
          size: 20,
        },
      });
    });

    it('getWebhookLogById calls GET /admin/premium/webhook-logs/{id}', async () => {
      apiClient.get.mockResolvedValueOnce({ data: { data: { id: 12, payload: '{}' } } });
      const res = await getWebhookLogById(12);
      expect(apiClient.get).toHaveBeenCalledWith('/admin/premium/webhook-logs/12');
      expect(res.id).toBe(12);
    });
  });
});
