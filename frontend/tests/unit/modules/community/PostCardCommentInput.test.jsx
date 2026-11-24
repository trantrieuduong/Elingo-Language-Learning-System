import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostCardCommentInput from '@/modules/community/components/PostCard/PostCardCommentInput/PostCardCommentInput';
import { tr, trRx } from '../../../utils/i18n';

describe('PostCardCommentInput', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    URL.createObjectURL = vi.fn(() => 'blob:preview');
    URL.revokeObjectURL = vi.fn();
  });

  it('submits trimmed content and clears it after a successful create', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(true);
    render(<PostCardCommentInput userName="Ada" onSubmit={onSubmit} />);

    const textarea = screen.getByPlaceholderText(tr('community:postCard.writeComment'));
    await user.type(textarea, '  Great post  ');
    await user.click(screen.getByRole('button', { name: trRx('community:postCard.comment') }));

    expect(onSubmit).toHaveBeenCalledWith('Great post', null);
    expect(textarea).toHaveValue('');
  });

  it('validates an invalid image type and supports removing a valid image preview', async () => {
    const user = userEvent.setup();
    render(<PostCardCommentInput userName="Ada" onSubmit={vi.fn()} />);
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, {
      target: { files: [new File(['x'], 'note.txt', { type: 'text/plain' })] },
    });
    expect(screen.getByRole('alert')).toHaveTextContent(tr('community:postCard.imageTypeError'));

    const image = new File(['x'], 'image.png', { type: 'image/png' });
    fireEvent.change(input, { target: { files: [image] } });
    expect(URL.createObjectURL).toHaveBeenCalledWith(image);
    expect(screen.getByRole('img', { name: 'image.png' })).toBeInTheDocument();

    await user.click(screen.getByText('close').closest('button'));
    expect(screen.queryByRole('img', { name: 'image.png' })).not.toBeInTheDocument();
  });

  it('uses the edit submission signature and preserves content when it fails', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(false);
    render(
      <PostCardCommentInput
        mode="edit"
        initialContent="Original"
        initialImageUrl="old.png"
        onSubmit={onSubmit}
      />
    );

    await user.click(screen.getByRole('button', { name: trRx('community:comments.save') }));
    expect(onSubmit).toHaveBeenCalledWith('Original', null, 'old.png');
    expect(screen.getByDisplayValue('Original')).toBeInTheDocument();
  });
});
