import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import MyProfilePage from '../../../../src/modules/user/pages/MyProfilePage';
import * as profileApi from '../../../../src/modules/user/profileApi';
import { tr } from '../../../utils/i18n';

vi.mock('../../../../src/modules/user/profileApi');

describe('MyProfilePage', () => {
  const mockProfile = {
    id: 1,
    username: 'vlkduy',
    email: 'vlkd1001@gmail.com',
    fullName: 'Vo Le Khanh Duy',
    avatarUrl: null,
    role: 'USER',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state initially', () => {
    profileApi.getMyProfileApi.mockReturnValue(new Promise(() => {}));
    const { container } = render(<MyProfilePage onNavigate={vi.fn()} />);
    expect(container.querySelector('.page-loading')).toBeInTheDocument();
  });

  it('renders profile info and options menu on success', async () => {
    profileApi.getMyProfileApi.mockResolvedValueOnce({
      success: true,
      data: mockProfile,
    });

    render(<MyProfilePage onNavigate={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByText('Vo Le Khanh Duy')).toBeInTheDocument();
      expect(screen.getByText('@vlkduy')).toBeInTheDocument();
    });
    expect(screen.getByRole('img', { name: 'Vo Le Khanh Duy' })).toHaveAttribute(
      'src',
      'https://ui-avatars.com/api/?name=Vo%20Le%20Khanh%20Duy'
    );

    const moreBtn = screen.getByRole('button');
    expect(moreBtn).toBeInTheDocument();
  });

  it('opens options modal with edit profile option when clicking options menu', async () => {
    profileApi.getMyProfileApi.mockResolvedValueOnce({
      success: true,
      data: mockProfile,
    });
    const mockNavigate = vi.fn();

    render(<MyProfilePage onNavigate={mockNavigate} />);

    await waitFor(() => {
      expect(screen.getByText('Vo Le Khanh Duy')).toBeInTheDocument();
    });

    const moreBtn = screen.getByRole('button');
    fireEvent.click(moreBtn);

    const editOption = await screen.findByText(tr('user:editProfile'));
    expect(editOption).toBeInTheDocument();

    fireEvent.click(editOption);
    expect(mockNavigate).toHaveBeenCalledWith('/profile/edit');
  });

  it('renders error state when api fails and retries on action click', async () => {
    profileApi.getMyProfileApi.mockRejectedValueOnce(new Error('Network error'));

    render(<MyProfilePage onNavigate={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByText(tr('common:loadError'))).toBeInTheDocument();
    });

    profileApi.getMyProfileApi.mockResolvedValueOnce({
      success: true,
      data: mockProfile,
    });

    const retryBtn = screen.getByRole('button', { name: tr('common:retry') });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByText('Vo Le Khanh Duy')).toBeInTheDocument();
    });
  });
});
