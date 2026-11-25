import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostComments from '@/modules/community/components/PostCard/PostComments/PostComments';
import { tr } from '../../../utils/i18n';

let isAtEnd = false;
vi.mock('@/modules/community/hooks/useScrollEnd', () => ({
  default: () => ({ scrollContainerRef: { current: null }, isAtEnd, handleScroll: vi.fn() }),
}));
vi.mock('@/modules/community/components/PostCard/PostCardCommentItem/PostCardCommentItem', () => ({
  default: ({ comment }) => <article>comment:{comment.id}</article>,
}));

describe('PostComments', () => {
  beforeEach(() => {
    isAtEnd = false;
  });

  it('shows loading, error with retry, and empty states when there are no comments', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const { rerender } = render(<PostComments isLoadingFirstPage />);
    expect(screen.getByText(tr('community:comments.loading'))).toBeInTheDocument();

    rerender(<PostComments error="Could not load" onRetry={onRetry} />);
    expect(screen.getByText('Could not load')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: tr('community:comments.retry') }));
    expect(onRetry).toHaveBeenCalledTimes(1);

    rerender(<PostComments />);
    expect(screen.getByText(tr('community:comments.empty'))).toBeInTheDocument();
  });

  it('renders comments, retry for a subsequent error, and load more at scroll end', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const onLoadMore = vi.fn();
    const comments = [{ id: 1 }, { id: 2 }];
    const { rerender } = render(
      <PostComments comments={comments} error="More failed" onRetry={onRetry} />
    );

    expect(screen.getByText('comment:1')).toBeInTheDocument();
    expect(screen.getByText('More failed')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: tr('community:comments.retry') }));
    expect(onRetry).toHaveBeenCalledTimes(1);

    isAtEnd = true;
    rerender(<PostComments comments={comments} hasMore onLoadMore={onLoadMore} />);
    await user.click(screen.getByRole('button', { name: tr('community:comments.loadMore') }));
    expect(onLoadMore).toHaveBeenCalledTimes(1);
  });
});
