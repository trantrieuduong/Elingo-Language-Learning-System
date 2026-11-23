import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import UpdateProfileModal from '@/modules/user/components/UpdateProfileModal/UpdateProfileModal';
import * as profileApi from '@/modules/user/profileApi';
import * as uploadFileService from '@/services/uploadFile';
import { ALLOWED_IMAGE_FORMATS } from '@/constants/fileTypes';
import { tr, trRx } from '../../../utils/i18n';

vi.mock('@/modules/user/profileApi');
vi.mock('@/services/uploadFile');

describe('UpdateProfileModal component', () => {
  const defaultProfile = {
    fullName: 'Nguyen Van A',
    username: 'nguyenvana',
    avatarUrl: 'https://example.com/avatar.png',
  };

  const defaultProps = {
    isOpen: true,
    profile: defaultProfile,
    onClose: vi.fn(),
    onSuccess: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <UpdateProfileModal {...defaultProps} isOpen={false} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders form elements correctly when open', () => {
    render(<UpdateProfileModal {...defaultProps} />);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(tr('user:editProfile'))).toBeInTheDocument();
    expect(
      screen.getByLabelText(tr('user:updateProfile.fullName'))
    ).toHaveValue(defaultProfile.fullName);
    expect(
      screen.getByLabelText(tr('user:updateProfile.username'))
    ).toHaveValue(defaultProfile.username);
    expect(
      screen.getByRole('button', { name: trRx('user:updateProfile.changeAvatar') })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: tr('common:saveChanges') })
    ).toBeInTheDocument();
  });

  describe('Form validation', () => {
    it('shows error when full name is empty', async () => {
      render(<UpdateProfileModal {...defaultProps} />);

      const fullNameInput = screen.getByLabelText(tr('user:updateProfile.fullName'));
      fireEvent.change(fullNameInput, { target: { value: '   ' } });

      const submitBtn = screen.getByRole('button', { name: tr('common:saveChanges') });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(
          screen.getByText(tr('user:updateProfile.errorFullNameRequired'))
        ).toBeInTheDocument();
      });
      expect(profileApi.updateProfileApi).not.toHaveBeenCalled();
    });

    it('shows error when full name exceeds maximum length', async () => {
      render(<UpdateProfileModal {...defaultProps} />);

      const fullNameInput = screen.getByLabelText(tr('user:updateProfile.fullName'));
      fireEvent.change(fullNameInput, { target: { value: 'a'.repeat(151) } });

      const submitBtn = screen.getByRole('button', { name: tr('common:saveChanges') });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        const errors = screen.getAllByText(tr('user:updateProfile.errorFullNameMaxLength'));
        expect(errors.length).toBeGreaterThanOrEqual(1);
      });
      expect(profileApi.updateProfileApi).not.toHaveBeenCalled();
    });

    it('shows error when username is empty', async () => {
      render(<UpdateProfileModal {...defaultProps} />);

      const usernameInput = screen.getByLabelText(tr('user:updateProfile.username'));
      fireEvent.change(usernameInput, { target: { value: '' } });

      const submitBtn = screen.getByRole('button', { name: tr('common:saveChanges') });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(
          screen.getByText(tr('user:updateProfile.errorUsernameRequired'))
        ).toBeInTheDocument();
      });
      expect(profileApi.updateProfileApi).not.toHaveBeenCalled();
    });

    it('shows error when username format is invalid', async () => {
      render(<UpdateProfileModal {...defaultProps} />);

      const usernameInput = screen.getByLabelText(tr('user:updateProfile.username'));
      // Invalid length (< 3)
      fireEvent.change(usernameInput, { target: { value: 'ab' } });

      const submitBtn = screen.getByRole('button', { name: tr('common:saveChanges') });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(
          screen.getByText(tr('user:updateProfile.errorUsernameFormat'))
        ).toBeInTheDocument();
      });

      // Invalid characters (special char / space)
      fireEvent.change(usernameInput, { target: { value: 'user name!' } });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(
          screen.getByText(tr('user:updateProfile.errorUsernameFormat'))
        ).toBeInTheDocument();
      });
      expect(profileApi.updateProfileApi).not.toHaveBeenCalled();
    });
  });

  describe('Avatar upload & preview', () => {
    it('shows error when selecting invalid image type', () => {
      render(<UpdateProfileModal {...defaultProps} />);

      const fileInput = document.querySelector('.update-profile-avatar-file-input');
      const invalidFile = new File(['dummy content'], 'document.pdf', { type: 'application/pdf' });

      fireEvent.change(fileInput, { target: { files: [invalidFile] } });

      expect(
        screen.getByText(
          tr('user:updateProfile.avatarInvalidType', { formats: ALLOWED_IMAGE_FORMATS })
        )
      ).toBeInTheDocument();
    });

    it('shows error when selecting file that exceeds MAX_IMAGE_FILE_SIZE (5MB)', () => {
      render(<UpdateProfileModal {...defaultProps} />);

      const fileInput = document.querySelector('.update-profile-avatar-file-input');
      const largeFile = new File(['dummy content'], 'large-image.png', { type: 'image/png' });
      Object.defineProperty(largeFile, 'size', { value: 6 * 1024 * 1024 });

      fireEvent.change(fileInput, { target: { files: [largeFile] } });

      expect(
        screen.getByText(
          tr('user:updateProfile.avatarFileTooLarge', { maxSize: 5 })
        )
      ).toBeInTheDocument();
    });

    it('creates object URL preview when selecting a valid image file', () => {
      const createObjectURLSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock-url');

      render(<UpdateProfileModal {...defaultProps} />);

      const fileInput = document.querySelector('.update-profile-avatar-file-input');
      const validFile = new File(['image'], 'avatar.png', { type: 'image/png' });
      Object.defineProperty(validFile, 'size', { value: 1024 * 1024 });

      fireEvent.change(fileInput, { target: { files: [validFile] } });

      const img = screen.getByRole('img');
      expect(img).toHaveAttribute('src', 'blob:mock-url');
      createObjectURLSpy.mockRestore();
    });
  });

  describe('Form submission', () => {
    it('submits update profile successfully without new avatar and calls onSuccess & onClose', async () => {
      profileApi.updateProfileApi.mockResolvedValueOnce({
        success: true,
        data: {
          fullName: 'Nguyen Van B',
          username: 'nguyenvanb',
          avatarUrl: 'https://example.com/avatar.png',
        },
      });

      render(<UpdateProfileModal {...defaultProps} />);

      fireEvent.change(screen.getByLabelText(tr('user:updateProfile.fullName')), {
        target: { value: 'Nguyen Van B' },
      });
      fireEvent.change(screen.getByLabelText(tr('user:updateProfile.username')), {
        target: { value: 'nguyenvanb' },
      });

      fireEvent.click(screen.getByRole('button', { name: tr('common:saveChanges') }));

      await waitFor(() => {
        expect(profileApi.updateProfileApi).toHaveBeenCalledWith({
          fullName: 'Nguyen Van B',
          username: 'nguyenvanb',
        });
        expect(defaultProps.onSuccess).toHaveBeenCalledWith({
          fullName: 'Nguyen Van B',
          username: 'nguyenvanb',
          avatarUrl: 'https://example.com/avatar.png',
        });
        expect(defaultProps.onClose).toHaveBeenCalled();
      });
    });

    it('uploads avatar first if new avatar selected, then updates profile with avatarUrl', async () => {
      vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock-url');
      uploadFileService.uploadFileForVerification.mockResolvedValueOnce('verified-avatar-key-123');
      profileApi.updateProfileApi.mockResolvedValueOnce({
        success: true,
        data: {
          fullName: 'Nguyen Van A',
          username: 'nguyenvana',
          avatarUrl: 'verified-avatar-key-123',
        },
      });

      render(<UpdateProfileModal {...defaultProps} />);

      const fileInput = document.querySelector('.update-profile-avatar-file-input');
      const validFile = new File(['image'], 'avatar.jpg', { type: 'image/jpeg' });
      Object.defineProperty(validFile, 'size', { value: 1024 * 1024 });
      fireEvent.change(fileInput, { target: { files: [validFile] } });

      fireEvent.click(screen.getByRole('button', { name: tr('common:saveChanges') }));

      await waitFor(() => {
        expect(uploadFileService.uploadFileForVerification).toHaveBeenCalledWith(validFile, 'image/jpeg');
        expect(profileApi.updateProfileApi).toHaveBeenCalledWith({
          fullName: 'Nguyen Van A',
          username: 'nguyenvana',
          avatarUrl: 'verified-avatar-key-123',
        });
        expect(defaultProps.onSuccess).toHaveBeenCalled();
      });
    });

    it('shows error in FormAlert when API returns error response with code', async () => {
      profileApi.updateProfileApi.mockResolvedValueOnce({
        success: false,
        errors: [{ code: 'USERNAME_EXISTED', message: 'Username already taken' }],
      });

      render(<UpdateProfileModal {...defaultProps} />);

      fireEvent.click(screen.getByRole('button', { name: tr('common:saveChanges') }));

      await waitFor(() => {
        expect(screen.getByText(tr('api:error.USERNAME_EXISTED'))).toBeInTheDocument();
      });
      expect(defaultProps.onSuccess).not.toHaveBeenCalled();
    });

    it('shows fallback error in FormAlert when API throws rejection without specific error code', async () => {
      profileApi.updateProfileApi.mockRejectedValueOnce(new Error('Network Error'));

      render(<UpdateProfileModal {...defaultProps} />);

      fireEvent.click(screen.getByRole('button', { name: tr('common:saveChanges') }));

      await waitFor(() => {
        expect(screen.getByText(tr('api:common.UNKNOWN_ERROR'))).toBeInTheDocument();
      });
      expect(defaultProps.onSuccess).not.toHaveBeenCalled();
    });
  });

  describe('Modal interactions', () => {
    it('calls onClose when clicking close button or cancel button', () => {
      render(<UpdateProfileModal {...defaultProps} />);

      const closeBtn = screen.getByRole('button', { name: 'close' });
      fireEvent.click(closeBtn);
      expect(defaultProps.onClose).toHaveBeenCalled();

      const cancelBtn = screen.getByRole('button', { name: tr('common:cancel') });
      fireEvent.click(cancelBtn);
      expect(defaultProps.onClose).toHaveBeenCalledTimes(2);
    });

    it('calls onClose when Escape key is pressed', () => {
      render(<UpdateProfileModal {...defaultProps} />);

      fireEvent.keyDown(document, { key: 'Escape' });
      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('clears field errors and general errors when typing into input fields', async () => {
      render(<UpdateProfileModal {...defaultProps} />);

      const usernameInput = screen.getByLabelText(tr('user:updateProfile.username'));
      fireEvent.change(usernameInput, { target: { value: '' } });

      const submitBtn = screen.getByRole('button', { name: tr('common:saveChanges') });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText(tr('user:updateProfile.errorUsernameRequired'))).toBeInTheDocument();
      });

      fireEvent.change(usernameInput, { target: { value: 'validuser' } });
      expect(screen.queryByText(tr('user:updateProfile.errorUsernameRequired'))).not.toBeInTheDocument();
    });
  });
});
