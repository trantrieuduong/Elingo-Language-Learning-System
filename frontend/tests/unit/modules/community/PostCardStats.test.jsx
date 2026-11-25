import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostCardStats from '@/modules/community/components/PostCard/PostCardStats/PostCardStats';
import { trRx } from '../../../utils/i18n';

describe('PostCardStats', () => {
  it('formats counters and invokes the comment action', async () => {
    const user = userEvent.setup();
    const onCommentClick = vi.fn();
    render(<PostCardStats likeCount={1234} commentCount={56} onCommentClick={onCommentClick} />);

    expect(screen.getByText(/1[,.]234/)).toBeInTheDocument();
    const commentButton = screen.getByRole('button', {
      name: trRx('community:postCard.commentCount', { count: '56' }),
    });
    await user.click(commentButton);
    expect(onCommentClick).toHaveBeenCalledTimes(1);
  });
});
