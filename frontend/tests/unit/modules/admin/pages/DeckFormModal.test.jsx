import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DeckFormModal from '@/modules/admin/pages/DeckManagementPage/components/DeckFormModal';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import * as uploadFileService from '@/services/uploadFile';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminContentApi', () => ({
  createSystemDeck: vi.fn(),
  updateSystemDeck: vi.fn(),
}));

vi.mock('@/services/uploadFile', () => ({
  uploadFileForVerification: vi.fn(),
}));

describe('DeckFormModal component', () => {
  const mockOnClose = vi.fn();
  const mockOnSuccess = vi.fn();

  const sampleTags = [
    { id: 1, label: 'Business' },
    { id: 2, label: 'Academic' },
  ];

  const sampleCefrLevels = [
    { id: 1, label: 'A1' },
    { id: 2, label: 'B2' },
  ];

  const sampleInitialData = {
    id: 10,
    title: 'Business English',
    description: 'Corporate terms',
    coverImageUrl: 'https://cdn.example.com/cover.jpg',
    isPremium: true,
    tags: [{ id: 1, label: 'Business' }],
    cefrLevels: [{ id: 2, label: 'B2' }],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:http://localhost/mock-preview');
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <DeckFormModal
        isOpen={false}
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  describe.each(LANGS)('i18n and form operations [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders create mode with empty fields and default false tier', async () => {
      render(
        <DeckFormModal
          isOpen={true}
          mode="create"
          tags={sampleTags}
          cefrLevels={sampleCefrLevels}
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      const heading = screen.getByRole('heading', { level: 3 });
      expect(heading).toBeInTheDocument();

      const titleInput = screen.getByLabelText(new RegExp(lng === 'vi' ? 'Tên bộ từ' : 'Deck Title', 'i'));
      expect(titleInput).toHaveValue('');

      expect(screen.getByText(/Business/)).toBeInTheDocument();
      expect(screen.getByText(/Academic/)).toBeInTheDocument();
      expect(screen.getByText('A1')).toBeInTheDocument();
      expect(screen.getByText('B2')).toBeInTheDocument();
    });

    it('renders edit mode with initial data and checks checkboxes', async () => {
      render(
        <DeckFormModal
          isOpen={true}
          mode="edit"
          initialData={sampleInitialData}
          tags={sampleTags}
          cefrLevels={sampleCefrLevels}
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      const titleInput = screen.getByLabelText(new RegExp(lng === 'vi' ? 'Tên bộ từ' : 'Deck Title', 'i'));
      expect(titleInput).toHaveValue('Business English');

      const descInput = screen.getByLabelText(new RegExp(lng === 'vi' ? '^Mô tả' : 'Description', 'i'));
      expect(descInput).toHaveValue('Corporate terms');

      // Tag 1 (Business) should be checked, Tag 2 (Academic) unchecked
      const businessCheckbox = screen.getByLabelText('#Business');
      const academicCheckbox = screen.getByLabelText('#Academic');
      expect(businessCheckbox).toBeChecked();
      expect(academicCheckbox).not.toBeChecked();

      // CEFR B2 should be checked, A1 unchecked
      const b2Checkbox = screen.getByLabelText('B2');
      const a1Checkbox = screen.getByLabelText('A1');
      expect(b2Checkbox).toBeChecked();
      expect(a1Checkbox).not.toBeChecked();
    });

    it('submits create payload when valid', async () => {
      adminContentApi.createSystemDeck.mockResolvedValueOnce({ id: 99, title: 'IELTS Core' });
      const user = userEvent.setup();

      render(
        <DeckFormModal
          isOpen={true}
          mode="create"
          tags={sampleTags}
          cefrLevels={sampleCefrLevels}
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      const titleInput = screen.getByLabelText(new RegExp(lng === 'vi' ? 'Tên bộ từ' : 'Deck Title', 'i'));
      await user.type(titleInput, 'IELTS Core');

      const descInput = screen.getByLabelText(new RegExp(lng === 'vi' ? '^Mô tả' : 'Description', 'i'));
      await user.type(descInput, 'Academic wordlist');

      // Toggle tag checkbox
      const businessCheckbox = screen.getByLabelText('#Business');
      await user.click(businessCheckbox);
      expect(businessCheckbox).toBeChecked();

      // Toggle CEFR checkbox
      const a1Checkbox = screen.getByLabelText('A1');
      await user.click(a1Checkbox);
      expect(a1Checkbox).toBeChecked();

      // Submit
      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo mới' : 'Create',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminContentApi.createSystemDeck).toHaveBeenCalledWith(
          expect.objectContaining({
            title: 'IELTS Core',
            description: 'Academic wordlist',
            tagIds: [1],
            cefrLevelIds: [1],
            isPremium: false,
          })
        );
        expect(mockOnSuccess).toHaveBeenCalled();
        expect(mockOnClose).toHaveBeenCalled();
      });
    });

    it('submits update payload in edit mode', async () => {
      adminContentApi.updateSystemDeck.mockResolvedValueOnce({ id: 10, title: 'Advanced Business English' });
      const user = userEvent.setup();

      render(
        <DeckFormModal
          isOpen={true}
          mode="edit"
          initialData={sampleInitialData}
          tags={sampleTags}
          cefrLevels={sampleCefrLevels}
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      const titleInput = screen.getByLabelText(new RegExp(lng === 'vi' ? 'Tên bộ từ' : 'Deck Title', 'i'));
      await user.clear(titleInput);
      await user.type(titleInput, 'Advanced Business English');

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Lưu thay đổi' : 'Save Changes',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminContentApi.updateSystemDeck).toHaveBeenCalledWith(
          10,
          expect.objectContaining({
            title: 'Advanced Business English',
          })
        );
        expect(mockOnSuccess).toHaveBeenCalled();
        expect(mockOnClose).toHaveBeenCalled();
      });
    });

    it('handles image upload and removal', async () => {
      uploadFileService.uploadFileForVerification.mockResolvedValueOnce('uploads/verified-cover.png');
      const user = userEvent.setup();

      render(
        <DeckFormModal
          isOpen={true}
          mode="create"
          tags={sampleTags}
          cefrLevels={sampleCefrLevels}
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      const fileInput = document.querySelector('input[type="file"]');
      expect(fileInput).toBeInTheDocument();

      // Invalid file type test
      const textFile = new File(['text'], 'test.txt', { type: 'text/plain' });
      fireEvent.change(fileInput, { target: { files: [textFile] } });
      await waitFor(() => {
        expect(screen.getAllByText(/JPG, PNG, WEBP/i).length).toBeGreaterThanOrEqual(2);
      });

      // Valid file upload
      const imgFile = new File(['dummy content'], 'cover.png', { type: 'image/png' });
      fireEvent.change(fileInput, { target: { files: [imgFile] } });

      await waitFor(() => {
        expect(uploadFileService.uploadFileForVerification).toHaveBeenCalledWith(imgFile, 'image/png');
      });

      // Remove image
      const removeBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Gỡ ảnh' : 'Remove',
      });
      await user.click(removeBtn);

      // Upload button label resets to Upload Image
      expect(
        screen.getByRole('button', {
          name: lng === 'vi' ? 'Tải ảnh lên' : 'Upload Image',
        })
      ).toBeInTheDocument();
    });

    it('displays error message when submission rejects', async () => {
      adminContentApi.createSystemDeck.mockRejectedValueOnce({
        response: { data: { message: 'Slug already exists' } },
      });
      const user = userEvent.setup();

      render(
        <DeckFormModal
          isOpen={true}
          mode="create"
          tags={sampleTags}
          cefrLevels={sampleCefrLevels}
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      const titleInput = screen.getByLabelText(new RegExp(lng === 'vi' ? 'Tên bộ từ' : 'Deck Title', 'i'));
      await user.type(titleInput, 'Duplicate Deck');

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo mới' : 'Create',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText('Slug already exists')).toBeInTheDocument();
      });
    });

    it('calls onClose when close button or backdrop is clicked', async () => {
      const user = userEvent.setup();

      render(
        <DeckFormModal
          isOpen={true}
          mode="create"
          onClose={mockOnClose}
          onSuccess={mockOnSuccess}
        />
      );

      const closeBtns = screen.getAllByRole('button', {
        name: lng === 'vi' ? 'Hủy bỏ' : 'Cancel',
      });
      await user.click(closeBtns[0]);
      expect(mockOnClose).toHaveBeenCalled();
    });
  });
});
