import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostCard from '@/modules/community/components/PostCard/PostCard';

const mockComments = {
  comments: [],
  hasMore: false,
  isLoadingFirstPage: false,
  isLoadingMore: false,
  loadError: '',
  loadComments: vi.fn(),
  repliesByCommentId: {},
  handleCreateComment: vi.fn(),
  handleCreateReply: vi.fn(),
  handleUpdateComment: vi.fn(),
  handleDeleteComment: vi.fn(),
  handleLikeComment: vi.fn(),
  handleLoadReplies: vi.fn(),
  handleLoadComments: vi.fn(),
  handleLoadMoreComments: vi.fn(),
};
let mockUser;
const mockOpenReport = vi.fn();
const mockCloseReport = vi.fn();

vi.mock('@/context/AuthContext', () => ({
  useAuth: () => ({ user: mockUser }),
}));

vi.mock('@/modules/community/hooks/useCommunityComments', () => ({
  default: () => mockComments,
}));

vi.mock('@/modules/report/hooks/useReportModal', () => ({
  useReportModal: () => ({
    isReportOpen: false,
    targetType: null,
    targetId: null,
    targetLabel: '',
    openReport: mockOpenReport,
    closeReport: mockCloseReport,
  }),
}));

vi.mock('@/modules/community/components/PostCard/PostCardHeader/PostCardHeader', () => ({
  default: ({ onAuthorClick, onGroupClick, onOptionsClick }) => (
    <div>
      <button type="button" onClick={onAuthorClick}>
        author
      </button>
      <button type="button" onClick={onGroupClick}>
        group
      </button>
      <button type="button" onClick={onOptionsClick}>
        options
      </button>
    </div>
  ),
}));

vi.mock('@/modules/community/components/PostCard/PostCardContent/PostCardContent', () => ({
  default: ({ onOpenPost }) => (
    <button type="button" onClick={onOpenPost}>
      open post
    </button>
  ),
}));

vi.mock('@/modules/community/components/PostCard/PostCardMedia/PostCardMedia', () => ({
  default: () => <div data-testid="media" />,
}));

vi.mock('@/modules/community/components/PostCard/PostCardStats/PostCardStats', () => ({
  default: ({ likeCount, onCommentClick }) => (
    <button type="button" onClick={onCommentClick}>
      stats:{likeCount}
    </button>
  ),
}));

vi.mock('@/modules/community/components/PostCard/PostCardActions/PostCardActions', () => ({
  default: ({ isLiked, isPostUnpublished, onLike, onCommentToggle }) => (
    <div>
      <span data-testid="liked-state">{String(isLiked)}</span>
      <span data-testid="unpublished-state">{String(isPostUnpublished)}</span>
      <button type="button" onClick={onLike}>
        like
      </button>
      <button type="button" onClick={onCommentToggle}>
        comment
      </button>
    </div>
  ),
}));

vi.mock('@/modules/community/components/PostCard/PostComments/PostComments', () => ({
  default: () => <div data-testid="comments" />,
}));

vi.mock(
  '@/modules/community/components/PostCard/PostCardCommentInput/PostCardCommentInput',
  () => ({
    default: ({ onSubmit }) => (
      <button type="button" onClick={() => onSubmit('A comment', null)}>
        submit comment
      </button>
    ),
  })
);

vi.mock('@/components/OptionsModal/OptionsModal', () => ({
  default: ({ isOpen, options, onClose }) =>
    isOpen && (
      <div data-testid="options-modal">
        {options.map((option) => (
          <button key={option.id} type="button" onClick={option.onSelect}>
            {option.id}
          </button>
        ))}
        <button type="button" onClick={onClose}>
          close options
        </button>
      </div>
    ),
}));

vi.mock('@/components/ConfirmModal/ConfirmModal', () => ({
  default: ({ isOpen, onConfirm, onCancel }) =>
    isOpen && (
      <div data-testid="confirm-modal">
        <button type="button" onClick={onConfirm}>
          confirm delete
        </button>
        <button type="button" onClick={onCancel}>
          cancel delete
        </button>
      </div>
    ),
}));

vi.mock('@/components/Toast/Toast', () => ({
  default: ({ message, variant }) =>
    message && (
      <div data-testid="toast">
        {variant}:{message}
      </div>
    ),
}));

vi.mock('@/modules/report/components/ReportModal/ReportModal', () => ({
  default: () => <div data-testid="report-modal" />,
}));

const post = {
  postId: 42,
  author: { id: 7, fullName: 'Post author' },
  group: 'English learners',
  createdAt: '2026-01-01T00:00:00.000Z',
  status: 'PUBLISHED',
  title: 'A post title',
  content: 'Post content',
  likeCount: 4,
  commentCount: 2,
};

describe('PostCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { id: 1, fullName: 'Current user', avatarUrl: 'avatar.png' };
  });

  it('optimistically updates the like state and count when liking succeeds', async () => {
    const user = userEvent.setup();
    const onLike = vi.fn().mockResolvedValue(true);
    render(<PostCard {...post} onLike={onLike} />);

    expect(screen.getByRole('button', { name: 'stats:4' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'like' }));

    expect(screen.getByTestId('liked-state')).toHaveTextContent('true');
    expect(screen.getByRole('button', { name: 'stats:5' })).toBeInTheDocument();
    await waitFor(() => expect(onLike).toHaveBeenCalledWith(true));
  });

  it('rolls back the optimistic like state when liking fails', async () => {
    const user = userEvent.setup();
    render(<PostCard {...post} onLike={vi.fn().mockResolvedValue(false)} />);

    await user.click(screen.getByRole('button', { name: 'like' }));

    await waitFor(() => expect(screen.getByTestId('liked-state')).toHaveTextContent('false'));
    expect(screen.getByRole('button', { name: 'stats:4' })).toBeInTheDocument();
  });

  it('opens comments once, loads them and submits a new comment through the hook', async () => {
    const user = userEvent.setup();
    mockComments.handleCreateComment.mockResolvedValue(true);
    render(<PostCard {...post} />);

    await user.click(screen.getByRole('button', { name: 'comment' }));
    expect(mockComments.handleLoadComments).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId('comments')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'submit comment' }));
    expect(mockComments.handleCreateComment).toHaveBeenCalledWith('A comment', null);
  });

  it('navigates from the author, group and post content controls', async () => {
    const user = userEvent.setup();
    const onNavigate = vi.fn();
    const onOpenPost = vi.fn();
    render(<PostCard {...post} onNavigate={onNavigate} onOpenPost={onOpenPost} />);

    await user.click(screen.getByRole('button', { name: 'author' }));
    await user.click(screen.getByRole('button', { name: 'group' }));
    await user.click(screen.getByRole('button', { name: 'open post' }));

    expect(onNavigate).toHaveBeenNthCalledWith(1, '/profile/7');
    expect(onNavigate).toHaveBeenNthCalledWith(2, '/community/groups');
    expect(onOpenPost).toHaveBeenCalledTimes(1);
  });

  it('offers edit and delete for an owned post, then deletes after confirmation', async () => {
    const user = userEvent.setup();
    const onEdit = vi.fn();
    const onDelete = vi.fn().mockResolvedValue();
    mockUser = { id: 7, fullName: 'Post author' };
    render(<PostCard {...post} onEdit={onEdit} onDelete={onDelete} />);

    await user.click(screen.getByRole('button', { name: 'options' }));
    await user.click(screen.getByRole('button', { name: 'edit-post' }));
    expect(onEdit).toHaveBeenCalledWith(42);

    await user.click(screen.getByRole('button', { name: 'delete-post' }));
    await user.click(screen.getByRole('button', { name: 'confirm delete' }));
    await waitFor(() => expect(onDelete).toHaveBeenCalledWith(42));
    expect(screen.queryByTestId('confirm-modal')).not.toBeInTheDocument();
  });

  it('offers reporting for another user post and marks unpublished posts as non-interactive', async () => {
    const user = userEvent.setup();
    render(<PostCard {...post} status="PENDING" />);

    expect(screen.getByTestId('unpublished-state')).toHaveTextContent('true');
    await user.click(screen.getByRole('button', { name: 'options' }));
    await user.click(screen.getByRole('button', { name: 'report-post' }));

    expect(mockOpenReport).toHaveBeenCalledWith({
      targetType: 'POST',
      targetId: 42,
      targetLabel: 'A post title',
    });
  });
});
