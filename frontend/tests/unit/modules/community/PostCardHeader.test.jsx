import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostCardHeader from '@/modules/community/components/PostCard/PostCardHeader/PostCardHeader';
import { tr } from '../../../utils/i18n';

describe('PostCardHeader', () => {
  const author = {
    id: 1,
    fullName: 'Ada Lovelace',
    username: 'ada',
    avatarUrl: 'https://example.com/ada.png',
  };

  it('renders author metadata and invokes author, group, and options actions', async () => {
    const user = userEvent.setup();
    const onAuthorClick = vi.fn();
    const onGroupClick = vi.fn();
    const onOptionsClick = vi.fn();
    render(
      <PostCardHeader
        author={author}
        group="Grammar"
        createdAt="2026-01-01T00:00:00.000Z"
        status="PUBLISHED"
        onAuthorClick={onAuthorClick}
        onGroupClick={onGroupClick}
        onOptionsClick={onOptionsClick}
      />
    );

    expect(screen.getByRole('img', { name: 'Ada Lovelace' })).toHaveAttribute(
      'src',
      author.avatarUrl
    );
    expect(screen.getByText('@ada')).toBeInTheDocument();
    expect(screen.getByText(tr('community:status.published'))).toBeInTheDocument();
    await user.click(screen.getByRole('img', { name: 'Ada Lovelace' }).closest('button'));
    await user.click(document.querySelector('.post-card-header__name-btn'));
    await user.click(screen.getByRole('button', { name: 'Grammar' }));
    await user.click(screen.getByRole('button', { name: 'More options' }));

    expect(onAuthorClick).toHaveBeenCalledTimes(2);
    expect(onGroupClick).toHaveBeenCalledTimes(1);
    expect(onOptionsClick).toHaveBeenCalledTimes(1);
  });

  it('uses the anonymous fallback and defaults an unknown status to published', () => {
    render(<PostCardHeader createdAt="2026-01-01T00:00:00.000Z" status="UNKNOWN" />);

    expect(
      screen.getByRole('img', { name: tr('community:postCard.anonymousUser') })
    ).toBeInTheDocument();
    expect(screen.getByText(tr('community:status.published'))).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'More options' })).not.toBeInTheDocument();
  });
});
