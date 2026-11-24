import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import SetPasswordModal from '../../../../src/modules/user/components/SetPasswordModal/SetPasswordModal';
import * as profileApi from '../../../../src/modules/user/profileApi';
import { tr } from '../../../utils/i18n';

vi.mock('../../../../src/modules/user/profileApi');

describe('SetPasswordModal', () => {
  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    onSuccess: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('validates password requirements and confirmation', async () => {
    render(<SetPasswordModal {...defaultProps} />);

    fireEvent.change(screen.getByLabelText(tr('user:setPasswordModal.newPassword')), {
      target: { value: 'weak' },
    });
    fireEvent.change(screen.getByLabelText(tr('user:setPasswordModal.confirmPassword')), {
      target: { value: 'different' },
    });
    fireEvent.click(screen.getByRole('button', { name: tr('common:saveChanges') }));

    await waitFor(() => {
      expect(screen.getByText(tr('user:setPasswordModal.newPasswordInvalidFormat'))).toBeInTheDocument();
      expect(screen.getByText(tr('user:setPasswordModal.confirmPasswordMismatch'))).toBeInTheDocument();
    });
    expect(profileApi.setPasswordApi).not.toHaveBeenCalled();
  });

  it('sets password successfully', async () => {
    profileApi.setPasswordApi.mockResolvedValueOnce({ success: true });
    render(<SetPasswordModal {...defaultProps} />);

    fireEvent.change(screen.getByLabelText(tr('user:setPasswordModal.newPassword')), {
      target: { value: 'StrongPass1!' },
    });
    fireEvent.change(screen.getByLabelText(tr('user:setPasswordModal.confirmPassword')), {
      target: { value: 'StrongPass1!' },
    });
    fireEvent.click(screen.getByRole('button', { name: tr('common:saveChanges') }));

    await waitFor(() => {
      expect(profileApi.setPasswordApi).toHaveBeenCalledWith('StrongPass1!');
      expect(defaultProps.onSuccess).toHaveBeenCalled();
    });
  });

  it('changes an existing password and displays an incorrect old password error', async () => {
    profileApi.changePasswordApi.mockRejectedValueOnce({
      response: {
        data: {
          code: 'OLD_PASSWORD_INCORRECT',
        },
      },
    });
    render(<SetPasswordModal {...defaultProps} hasPassword />);

    fireEvent.change(screen.getByLabelText(tr('user:setPasswordModal.oldPassword')), {
      target: { value: 'CurrentPass1!' },
    });
    fireEvent.change(screen.getByLabelText(tr('user:setPasswordModal.newPassword')), {
      target: { value: 'AnotherPass2!' },
    });
    fireEvent.change(screen.getByLabelText(tr('user:setPasswordModal.confirmPassword')), {
      target: { value: 'AnotherPass2!' },
    });
    fireEvent.click(screen.getByRole('button', { name: tr('common:saveChanges') }));

    await waitFor(() => {
      expect(profileApi.changePasswordApi).toHaveBeenCalledWith({
        oldPassword: 'CurrentPass1!',
        newPassword: 'AnotherPass2!',
      });
      expect(screen.getByText(tr('api:error.OLD_PASSWORD_INCORRECT'))).toBeInTheDocument();
    });
    expect(profileApi.setPasswordApi).not.toHaveBeenCalled();
  });
});
