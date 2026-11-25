import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostCardContent from '@/modules/community/components/PostCard/PostCardContent/PostCardContent';
import { tr } from '../../../utils/i18n';

describe('PostCardContent', () => {
  it('opens the post when its title is clicked', async () => {
    const user = userEvent.setup();
    const onOpenPost = vi.fn();
    render(
      <PostCardContent title="Grammar tips" content="Short content" onOpenPost={onOpenPost} />
    );

    await user.click(screen.getByRole('button', { name: 'Grammar tips' }));
    expect(onOpenPost).toHaveBeenCalledTimes(1);
  });

  it('truncates long content and expands it on demand', async () => {
    const user = userEvent.setup();
    const content = `${'a'.repeat(260)} remaining text`;
    render(<PostCardContent content={content} />);

    expect(screen.getByText(`${'a'.repeat(260)}…`)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: tr('community:postCard.expand') }));

    expect(screen.getByText(content)).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: tr('community:postCard.expand') })
    ).not.toBeInTheDocument();
  });
});
