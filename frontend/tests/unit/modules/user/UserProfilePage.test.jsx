import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import UserProfilePage from '../../../../src/modules/user/pages/UserProfilePage';
import * as profileApi from '../../../../src/modules/user/profileApi';
import { tr } from '../../../utils/i18n';

vi.mock('../../../../src/modules/user/profileApi');
vi.mock('../../../../src/modules/report/components/ReportModal/ReportModal', () => ({
  default: ({ isOpen, targetType, targetId, targetLabel, onClose }) => {
    if (!isOpen) return null;
    return (
      <div data-testid="mock-report-modal">
        <span>Target: {targetType}</span>
        <span>ID: {targetId}</span>
        <span>Label: {targetLabel}</span>
        <button onClick={onClose}>Close Report</button>
      </div>
    );
  },
}));

describe('UserProfilePage', () => {
  const mockOtherUser = {
    id: 2,
    username: 'thnhi',
    fullName: 'Le Thi Thanh Nhi',
    avatarUrl: null,
    role: 'USER',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state initially', () => {
    profileApi.getUserByIdApi.mockReturnValue(new Promise(() => {}));
    // Giả lập một lời gọi API bị treo vô thời hạn
    const { container } = render(<UserProfilePage userId="2" onNavigate={vi.fn()} />);
    expect(container.querySelector('.page-loading')).toBeInTheDocument();
  });

  it('renders other user profile on success', async () => {
    profileApi.getUserByIdApi.mockResolvedValueOnce({
      success: true,
      data: mockOtherUser,
    });

    render(<UserProfilePage userId="2" onNavigate={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByText('Le Thi Thanh Nhi')).toBeInTheDocument();
      expect(screen.getByText('@thnhi')).toBeInTheDocument();
    });
  });

  it('renders not found empty state when USER_NOT_FOUND error code is returned', async () => {
    profileApi.getUserByIdApi.mockResolvedValueOnce({
      success: false,
      errors: [
        {
          code: 'USER_NOT_FOUND',
          message: 'Không tìm thấy người dùng',
        },
      ],
    });

    render(<UserProfilePage userId="999" onNavigate={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByText(tr('user:userNotFoundTitle'))).toBeInTheDocument();
    });
  });

  it('opens OptionsModal with report option and triggers ReportModal with correct props', async () => {
    profileApi.getUserByIdApi.mockResolvedValueOnce({
      success: true,
      data: mockOtherUser,
    });

    render(<UserProfilePage userId="2" onNavigate={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByText('Le Thi Thanh Nhi')).toBeInTheDocument();
    });

    const moreBtn = screen.getByRole('button', { name: tr('user:options') });
    fireEvent.click(moreBtn);

    const reportOption = await screen.findByText(tr('user:report'));
    expect(reportOption).toBeInTheDocument();

    fireEvent.click(reportOption);

    const reportModal = await screen.findByTestId('mock-report-modal');
    expect(reportModal).toBeInTheDocument();
    expect(screen.getByText('Target: USER')).toBeInTheDocument();
    expect(screen.getByText('ID: 2')).toBeInTheDocument();
    expect(screen.getByText('Label: thnhi')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Close Report'));
    expect(screen.queryByTestId('mock-report-modal')).not.toBeInTheDocument();
  });

  it('renders error state and retries on action click', async () => {
    profileApi.getUserByIdApi.mockRejectedValueOnce(new Error('Internal Server Error'));

    render(<UserProfilePage userId="2" onNavigate={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByText(tr('common:loadError'))).toBeInTheDocument();
    });

    profileApi.getUserByIdApi.mockResolvedValueOnce({
      success: true,
      data: mockOtherUser,
    });

    const retryBtn = screen.getByRole('button', { name: tr('common:retry') });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByText('Le Thi Thanh Nhi')).toBeInTheDocument();
    });
  });
});
