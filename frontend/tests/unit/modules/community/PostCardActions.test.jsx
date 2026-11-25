import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostCardActions from '@/modules/community/components/PostCard/PostCardActions/PostCardActions';
import { LANGS, tr, trRx, useLang } from '../../../utils/i18n';

describe('PostCardActions', () => {
  const onLike = vi.fn();
  const onCommentToggle = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('Rendering and i18n [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders enabled like and comment actions by default', () => {
      render(<PostCardActions isLiked={false} onLike={onLike} onCommentToggle={onCommentToggle} />);

      expect(screen.getByRole('button', { name: trRx('community:postCard.like') })).toBeEnabled();
      expect(
        screen.getByRole('button', { name: trRx('community:postCard.comment') })
      ).toBeEnabled();
    });

    it('renders the liked state with a filled like icon', () => {
      render(<PostCardActions isLiked={true} onLike={onLike} onCommentToggle={onCommentToggle} />);

      const likeButton = screen.getByRole('button', { name: trRx('community:postCard.liked') });
      expect(likeButton).toHaveClass('post-card-actions__btn--liked');
      expect(likeButton.querySelector('.material-symbols-outlined')).toHaveClass('filled');
    });

    it('disables both actions and exposes the unavailable-post explanation', () => {
      render(
        <PostCardActions
          isLiked={false}
          isPostUnpublished={true}
          onLike={onLike}
          onCommentToggle={onCommentToggle}
        />
      );

      const likeButton = screen.getByRole('button', { name: trRx('community:postCard.like') });
      const commentButton = screen.getByRole('button', {
        name: trRx('community:postCard.comment'),
      });
      const disabledTitle = tr('community:postCard.cannotInteractUnpublished');

      expect(likeButton).toBeDisabled();
      expect(commentButton).toBeDisabled();
      expect(likeButton).toHaveAttribute('title', disabledTitle);
      expect(commentButton).toHaveAttribute('title', disabledTitle);
    });
  });

  it('calls the callbacks when the enabled actions are clicked', async () => {
    const user = userEvent.setup();
    render(<PostCardActions isLiked={false} onLike={onLike} onCommentToggle={onCommentToggle} />);

    await user.click(screen.getByRole('button', { name: trRx('community:postCard.like') }));
    await user.click(screen.getByRole('button', { name: trRx('community:postCard.comment') }));

    expect(onLike).toHaveBeenCalledTimes(1);
    expect(onCommentToggle).toHaveBeenCalledTimes(1);
  });

  it('only disables the like action while a like request is pending', () => {
    render(
      <PostCardActions
        isLiked={false}
        isLikePending={true}
        onLike={onLike}
        onCommentToggle={onCommentToggle}
      />
    );

    expect(screen.getByRole('button', { name: trRx('community:postCard.like') })).toBeDisabled();
    expect(screen.getByRole('button', { name: trRx('community:postCard.comment') })).toBeEnabled();
  });
});
