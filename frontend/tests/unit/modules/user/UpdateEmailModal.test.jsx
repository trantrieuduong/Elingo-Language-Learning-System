import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import UpdateEmailModal from '../../../../src/modules/user/components/UpdateEmailModal/UpdateEmailModal';
import * as profileApi from '../../../../src/modules/user/profileApi';
import { tr, trRx } from '../../../utils/i18n';

vi.mock('../../../../src/modules/user/profileApi');

describe('UpdateEmailModal component', () => {
  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    onSuccess: vi.fn(),
    onOtpSent: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <UpdateEmailModal {...defaultProps} isOpen={false} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders form elements correctly when open', () => {
    render(<UpdateEmailModal {...defaultProps} />);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(tr('user:updateEmailModal.title'))).toBeInTheDocument();
    expect(
      screen.getByLabelText(tr('user:updateEmailModal.newEmail'))
    ).toBeInTheDocument();
    expect(screen.getByLabelText(tr('user:updateEmailModal.otp'))).toBeInTheDocument();
    expect(
      screen.getByLabelText(tr('user:updateEmailModal.password'))
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: trRx('user:updateEmailModal.sendOtp') })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: tr('common:saveChanges') })
    ).toBeInTheDocument();
  });

  describe('Send OTP flow', () => {
    it('shows error in FormAlert when email is empty upon clicking Send OTP', async () => {
      render(<UpdateEmailModal {...defaultProps} />);

      const sendOtpBtn = screen.getByRole('button', {
        name: trRx('user:updateEmailModal.sendOtp'),
      });
      fireEvent.click(sendOtpBtn);

      await waitFor(() => {
        expect(
          screen.getByText(tr('user:updateEmailModal.errorEmailRequired'))
        ).toBeInTheDocument();
      });
      expect(profileApi.sendOtpUpdateEmailApi).not.toHaveBeenCalled();
    });

    it('shows error in FormAlert when email format is invalid upon clicking Send OTP', async () => {
      render(<UpdateEmailModal {...defaultProps} />);

      const emailInput = screen.getByLabelText(tr('user:updateEmailModal.newEmail'));
      fireEvent.change(emailInput, { target: { value: 'invalid-email' } });

      const sendOtpBtn = screen.getByRole('button', {
        name: trRx('user:updateEmailModal.sendOtp'),
      });
      fireEvent.click(sendOtpBtn);

      await waitFor(() => {
        expect(
          screen.getByText(tr('user:updateEmailModal.errorEmailInvalid'))
        ).toBeInTheDocument();
      });
      expect(profileApi.sendOtpUpdateEmailApi).not.toHaveBeenCalled();
    });

    it('sends OTP successfully and triggers onOtpSent', async () => {
      profileApi.sendOtpUpdateEmailApi.mockResolvedValueOnce({
        success: true,
        requestId: 'a175dfb752354291',
      });

      render(<UpdateEmailModal {...defaultProps} />);

      const emailInput = screen.getByLabelText(tr('user:updateEmailModal.newEmail'));
      fireEvent.change(emailInput, {
        target: { value: 'volekhanhduyhcmute@gmail.com' },
      });

      const sendOtpBtn = screen.getByRole('button', {
        name: trRx('user:updateEmailModal.sendOtp'),
      });
      fireEvent.click(sendOtpBtn);

      await waitFor(() => {
        expect(profileApi.sendOtpUpdateEmailApi).toHaveBeenCalledWith(
          'volekhanhduyhcmute@gmail.com'
        );
        expect(defaultProps.onOtpSent).toHaveBeenCalled();
      });
    });

    it('displays error in FormAlert when email is unchanged (EMAIL_UNCHANGED)', async () => {
      profileApi.sendOtpUpdateEmailApi.mockResolvedValueOnce({
        success: false,
        requestId: '04b0e91084504f4d',
        errors: [
          {
            code: 'EMAIL_UNCHANGED',
            message: 'New email and old email are the same',
          },
        ],
      });

      render(<UpdateEmailModal {...defaultProps} />);

      const emailInput = screen.getByLabelText(tr('user:updateEmailModal.newEmail'));
      fireEvent.change(emailInput, { target: { value: 'vlkd1001@gmail.com' } });

      const sendOtpBtn = screen.getByRole('button', {
        name: trRx('user:updateEmailModal.sendOtp'),
      });
      fireEvent.click(sendOtpBtn);

      await waitFor(() => {
        expect(
          screen.getByText(tr('api:error.EMAIL_UNCHANGED'))
        ).toBeInTheDocument();
      });
    });

    it('displays error in FormAlert when email already exists (EMAIL_EXISTED)', async () => {
      profileApi.sendOtpUpdateEmailApi.mockResolvedValueOnce({
        success: false,
        requestId: '50858f2c82fa4b49',
        errors: [
          {
            code: 'EMAIL_EXISTED',
            message: 'Email already exists',
          },
        ],
      });

      render(<UpdateEmailModal {...defaultProps} />);

      const emailInput = screen.getByLabelText(tr('user:updateEmailModal.newEmail'));
      fireEvent.change(emailInput, { target: { value: 'existing@gmail.com' } });

      const sendOtpBtn = screen.getByRole('button', {
        name: trRx('user:updateEmailModal.sendOtp'),
      });
      fireEvent.click(sendOtpBtn);

      await waitFor(() => {
        expect(
          screen.getByText(tr('api:error.EMAIL_EXISTED'))
        ).toBeInTheDocument();
      });
    });

    it('displays error in FormAlert when rate limit exceeded (RATE_LIMIT_EXCEEDED)', async () => {
      profileApi.sendOtpUpdateEmailApi.mockResolvedValueOnce({
        success: false,
        requestId: 'ac1dec58cc794934',
        errors: [
          {
            code: 'RATE_LIMIT_EXCEEDED',
            message: 'Too many requests, please try again later',
          },
        ],
      });

      render(<UpdateEmailModal {...defaultProps} />);

      const emailInput = screen.getByLabelText(tr('user:updateEmailModal.newEmail'));
      fireEvent.change(emailInput, { target: { value: 'spam@gmail.com' } });

      const sendOtpBtn = screen.getByRole('button', {
        name: trRx('user:updateEmailModal.sendOtp'),
      });
      fireEvent.click(sendOtpBtn);

      await waitFor(() => {
        expect(
          screen.getByText(tr('api:error.RATE_LIMIT_EXCEEDED'))
        ).toBeInTheDocument();
      });
    });
  });

  describe('Submit update email flow', () => {
    it('shows validation errors when fields are empty', async () => {
      render(<UpdateEmailModal {...defaultProps} />);

      const submitBtn = screen.getByRole('button', {
        name: tr('common:saveChanges'),
      });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(
          screen.getByText(tr('user:updateEmailModal.errorEmailRequired'))
        ).toBeInTheDocument();
      });
      expect(profileApi.updateEmailApi).not.toHaveBeenCalled();
    });

    it('submits update email successfully and triggers onSuccess & onClose', async () => {
      profileApi.updateEmailApi.mockResolvedValueOnce({
        success: true,
        requestId: 'b70a0dd8be3b46cd',
      });

      render(<UpdateEmailModal {...defaultProps} />);

      fireEvent.change(screen.getByLabelText(tr('user:updateEmailModal.newEmail')), {
        target: { value: 'vlkd@gmail.com' },
      });
      fireEvent.change(screen.getByLabelText(tr('user:updateEmailModal.otp')), {
        target: { value: '063120' },
      });
      fireEvent.change(
        screen.getByLabelText(tr('user:updateEmailModal.password')),
        {
          target: { value: '123456789Duy$' },
        }
      );

      const submitBtn = screen.getByRole('button', {
        name: tr('common:saveChanges'),
      });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(profileApi.updateEmailApi).toHaveBeenCalledWith({
          newEmail: 'vlkd@gmail.com',
          newEmailOtp: '063120',
          password: '123456789Duy$',
        });
        expect(defaultProps.onSuccess).toHaveBeenCalledWith('vlkd@gmail.com');
        expect(defaultProps.onClose).toHaveBeenCalled();
      });
    });

    it('shows error in FormAlert when OTP is invalid (OTP_INVALID)', async () => {
      profileApi.updateEmailApi.mockResolvedValueOnce({
        success: false,
        requestId: 'de4ad249a8714d50',
        errors: [
          {
            code: 'OTP_INVALID',
            message: 'OTP is incorrect or expired',
          },
        ],
      });

      render(<UpdateEmailModal {...defaultProps} />);

      fireEvent.change(screen.getByLabelText(tr('user:updateEmailModal.newEmail')), {
        target: { value: 'vlkd@gmail.com' },
      });
      fireEvent.change(screen.getByLabelText(tr('user:updateEmailModal.otp')), {
        target: { value: '000000' },
      });
      fireEvent.change(
        screen.getByLabelText(tr('user:updateEmailModal.password')),
        {
          target: { value: '123456789Duy$' },
        }
      );

      fireEvent.click(
        screen.getByRole('button', { name: tr('common:saveChanges') })
      );

      await waitFor(() => {
        expect(screen.getByText(tr('api:error.OTP_INVALID'))).toBeInTheDocument();
      });
      expect(defaultProps.onSuccess).not.toHaveBeenCalled();
    });

    it('shows error in FormAlert when password is incorrect (PASSWORD_INCORRECT)', async () => {
      profileApi.updateEmailApi.mockResolvedValueOnce({
        success: false,
        requestId: '78163a489093407b',
        errors: [
          {
            code: 'PASSWORD_INCORRECT',
            message: 'Password is incorrect',
          },
        ],
      });

      render(<UpdateEmailModal {...defaultProps} />);

      fireEvent.change(screen.getByLabelText(tr('user:updateEmailModal.newEmail')), {
        target: { value: 'vlkd@gmail.com' },
      });
      fireEvent.change(screen.getByLabelText(tr('user:updateEmailModal.otp')), {
        target: { value: '063120' },
      });
      fireEvent.change(
        screen.getByLabelText(tr('user:updateEmailModal.password')),
        {
          target: { value: 'WrongPass!' },
        }
      );

      fireEvent.click(
        screen.getByRole('button', { name: tr('common:saveChanges') })
      );

      await waitFor(() => {
        expect(
          screen.getByText(tr('api:error.PASSWORD_INCORRECT'))
        ).toBeInTheDocument();
      });
      expect(defaultProps.onSuccess).not.toHaveBeenCalled();
    });
  });

  describe('Modal interactions', () => {
    it('calls onClose when close button or cancel button is clicked', () => {
      render(<UpdateEmailModal {...defaultProps} />);

      const closeBtn = screen.getByRole('button', { name: 'close' });
      fireEvent.click(closeBtn);
      expect(defaultProps.onClose).toHaveBeenCalled();

      const cancelBtn = screen.getByRole('button', { name: tr('common:cancel') });
      fireEvent.click(cancelBtn);
      expect(defaultProps.onClose).toHaveBeenCalledTimes(2);
    });

    it('calls onClose when Escape key is pressed', () => {
      render(<UpdateEmailModal {...defaultProps} />);

      fireEvent.keyDown(document, { key: 'Escape' });
      expect(defaultProps.onClose).toHaveBeenCalled();
    });
  });
});
