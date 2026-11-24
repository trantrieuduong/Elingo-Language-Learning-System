import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdminPagination from '@/modules/admin/components/AdminPagination/AdminPagination';
import { useLang, LANGS } from '../../../../utils/i18n';

describe('AdminPagination component', () => {
  describe.each(LANGS)('i18n and interactions [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders total elements, unit label, and current page info', () => {
      render(
        <AdminPagination
          page={2}
          totalPages={5}
          totalElements={100}
          onPageChange={vi.fn()}
          unitLabel="mục"
        />
      );

      expect(screen.getByText('100')).toBeInTheDocument();
      expect(screen.getByText(/mục/i)).toBeInTheDocument();
      expect(screen.getByText(/2\/5/)).toBeInTheDocument();
    });

    it('disables previous button on first page and allows next button', async () => {
      const user = userEvent.setup();
      const onPageChange = vi.fn();

      render(
        <AdminPagination
          page={1}
          totalPages={3}
          totalElements={60}
          onPageChange={onPageChange}
        />
      );

      const prevBtn = screen.getByRole('button', { name: /Previous Page|Trang trước/i });
      const nextBtn = screen.getByRole('button', { name: /Next Page|Trang sau/i });

      expect(prevBtn).toBeDisabled();
      expect(nextBtn).not.toBeDisabled();

      await user.click(nextBtn);
      expect(onPageChange).toHaveBeenCalledWith(2);
    });

    it('disables next button on last page and allows previous button', async () => {
      const user = userEvent.setup();
      const onPageChange = vi.fn();

      render(
        <AdminPagination
          page={3}
          totalPages={3}
          totalElements={60}
          onPageChange={onPageChange}
        />
      );

      const prevBtn = screen.getByRole('button', { name: /Previous Page|Trang trước/i });
      const nextBtn = screen.getByRole('button', { name: /Next Page|Trang sau/i });

      expect(nextBtn).toBeDisabled();
      expect(prevBtn).not.toBeDisabled();

      await user.click(prevBtn);
      expect(onPageChange).toHaveBeenCalledWith(2);
    });

    it('disables next button when isLast is true even before totalPages', () => {
      render(
        <AdminPagination
          page={2}
          totalPages={10}
          totalElements={200}
          isLast={true}
          onPageChange={vi.fn()}
        />
      );

      const nextBtn = screen.getByRole('button', { name: /Next Page|Trang sau/i });
      expect(nextBtn).toBeDisabled();
    });

    it('navigates to typed page on Enter key', async () => {
      const user = userEvent.setup();
      const onPageChange = vi.fn();

      render(
        <AdminPagination
          page={1}
          totalPages={10}
          totalElements={200}
          onPageChange={onPageChange}
        />
      );

      const pageInput = screen.getByRole('spinbutton', { name: /Go to page|Đến trang/i });
      await user.clear(pageInput);
      await user.type(pageInput, '5{enter}');

      expect(onPageChange).toHaveBeenCalledWith(5);
    });

    it('clamps typed page to totalPages if exceeding max', async () => {
      const user = userEvent.setup();
      const onPageChange = vi.fn();

      render(
        <AdminPagination
          page={1}
          totalPages={5}
          totalElements={100}
          onPageChange={onPageChange}
        />
      );

      const pageInput = screen.getByRole('spinbutton', { name: /Go to page|Đến trang/i });
      await user.clear(pageInput);
      await user.type(pageInput, '999{enter}');

      expect(onPageChange).toHaveBeenCalledWith(5);
    });

    it('clamps typed page to 1 if typed value is less than 1 or invalid', async () => {
      const user = userEvent.setup();
      const onPageChange = vi.fn();

      render(
        <AdminPagination
          page={3}
          totalPages={5}
          totalElements={100}
          onPageChange={onPageChange}
        />
      );

      const pageInput = screen.getByRole('spinbutton', { name: /Go to page|Đến trang/i });
      await user.clear(pageInput);
      await user.type(pageInput, '0{enter}');

      expect(onPageChange).toHaveBeenCalledWith(1);
    });

    it('disables all controls when isLoading is true', () => {
      render(
        <AdminPagination
          page={2}
          totalPages={5}
          totalElements={100}
          onPageChange={vi.fn()}
          isLoading={true}
        />
      );

      const prevBtn = screen.getByRole('button', { name: /Previous Page|Trang trước/i });
      const nextBtn = screen.getByRole('button', { name: /Next Page|Trang sau/i });
      const pageInput = screen.getByRole('spinbutton', { name: /Go to page|Đến trang/i });

      expect(prevBtn).toBeDisabled();
      expect(nextBtn).toBeDisabled();
      expect(pageInput).toBeDisabled();
    });
  });
});
