import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostCardCommentItem from '@/modules/community/components/PostCard/PostCardCommentItem/PostCardCommentItem';

vi.mock('@/modules/community/hooks/useScrollEnd', () => ({
  default: () => ({ scrollContainerRef: { current: null }, isAtEnd: false, handleScroll: vi.fn() }),
}));
vi.mock('@/components/OptionsMenu/OptionsMenu', () => ({
  default: ({ onClick }) => (
    <button type="button" onClick={onClick}>
      options
    </button>
  ),
}));
vi.mock('@/components/OptionsModal/OptionsModal', () => ({
  default: ({ isOpen, options }) =>
    isOpen &&
    options.map((option) => (
      <button key={option.id} type="button" onClick={option.onSelect}>
        {option.id}
      </button>
    )),
}));
vi.mock('@/components/ConfirmModal/ConfirmModal', () => ({
  default: ({ isOpen, onConfirm }) =>
    isOpen && (
      <button type="button" onClick={onConfirm}>
        confirm delete
      </button>
    ),
}));
vi.mock(
  '@/modules/community/components/PostCard/PostCardCommentInput/PostCardCommentInput',
  () => ({
    default: ({ mode, onCancel, onSubmit }) => (
      <div>
        <span>{mode} input</span>
        <button type="button" onClick={onCancel}>
          cancel input
        </button>
        <button type="button" onClick={() => onSubmit('Updated', null, null)}>
          submit input
        </button>
      </div>
    ),
  })
);
vi.mock('@/modules/community/components/PostCard/PostCardActions/PostCardLikeButton', () => ({
  default: ({ isLiked, likeCount, onLike, isPending }) => (
    <button type="button" disabled={isPending} onClick={onLike}>
      like:{String(isLiked)}:{likeCount}
    </button>
  ),
}));

const comment = {
  id: 9,
  content: 'Useful comment',
  createdAt: '2026-01-01T00:00:00.000Z',
  likeCount: 2,
  replyCount: 1,
  author: { id: 2, fullName: 'Grace' },
};

describe('PostCardCommentItem', () => {
  it('optimistically likes a comment and rolls back when the callback fails', async () => {
    const user = userEvent.setup();
    const onLikeComment = vi.fn().mockResolvedValue(false);
    render(
      <PostCardCommentItem comment={comment} currentUserId={1} onLikeComment={onLikeComment} />
    );

    await user.click(screen.getByRole('button', { name: 'like:false:2' }));
    await waitFor(() => expect(onLikeComment).toHaveBeenCalledWith(9, true));
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'like:false:2' })).toBeInTheDocument()
    );
  });

  it('offers report for another user comment and edit/delete for its owner', async () => {
    const user = userEvent.setup();
    const onReportComment = vi.fn();
    const { rerender } = render(
      <PostCardCommentItem comment={comment} currentUserId={1} onReportComment={onReportComment} />
    );
    await user.click(screen.getByRole('button', { name: 'options' }));
    await user.click(screen.getByRole('button', { name: 'report-comment' }));
    expect(onReportComment).toHaveBeenCalledWith({
      targetType: 'COMMENT',
      targetId: 9,
      targetLabel: 'Useful comment',
    });

    const onUpdateComment = vi.fn().mockResolvedValue(true);
    const onDeleteComment = vi.fn().mockResolvedValue();
    rerender(
      <PostCardCommentItem
        comment={comment}
        currentUserId={2}
        onUpdateComment={onUpdateComment}
        onDeleteComment={onDeleteComment}
      />
    );
    await user.click(screen.getByRole('button', { name: 'options' }));
    await user.click(screen.getByRole('button', { name: 'edit-comment' }));
    expect(screen.getByText('edit input')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'submit input' }));
    await waitFor(() =>
      expect(onUpdateComment).toHaveBeenCalledWith(9, {
        content: 'Updated',
        imageFile: null,
        imageUrl: null,
      })
    );

    await user.click(screen.getByRole('button', { name: 'options' }));
    await user.click(screen.getByRole('button', { name: 'delete-comment' }));
    await user.click(screen.getByRole('button', { name: 'confirm delete' }));
    await waitFor(() => expect(onDeleteComment).toHaveBeenCalledWith(9));
  });

  it('loads replies when they are first opened and opens the reply form', async () => {
    const user = userEvent.setup();
    const onLoadReplies = vi.fn();
    render(
      <PostCardCommentItem
        comment={comment}
        currentUserId={1}
        onLoadReplies={onLoadReplies}
        onCreateReply={vi.fn()}
      />
    );

    await user.click(document.querySelector('[aria-expanded]'));
    expect(onLoadReplies).toHaveBeenCalledWith(9);
    await user.click(screen.getByText('Trả lời').closest('button'));
    expect(screen.getByText('reply input')).toBeInTheDocument();
  });
});
