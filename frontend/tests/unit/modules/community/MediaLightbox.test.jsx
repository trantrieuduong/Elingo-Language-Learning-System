import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MediaLightbox from '@/modules/community/components/MediaLightbox/MediaLightbox';

const media = [
  { id: 'image-1', type: 'image', url: 'https://example.com/one.jpg', alt: 'First image' },
  { id: 'video-1', type: 'video', url: 'https://example.com/video.mp4' },
  { id: 'image-2', type: 'image', url: 'https://example.com/two.jpg', alt: 'Second image' },
];

describe('MediaLightbox', () => {
  it('renders the media at initialIndex through a portal and locks body scrolling', () => {
    document.body.style.overflow = 'auto';
    const { unmount } = render(<MediaLightbox media={media} initialIndex={0} onClose={vi.fn()} />);

    expect(screen.getByRole('img', { name: 'First image' })).toHaveAttribute(
      'src',
      'https://example.com/one.jpg'
    );
    expect(document.body.style.overflow).toBe('hidden');

    unmount();
    expect(document.body.style.overflow).toBe('auto');
  });

  it('closes when the close button is clicked', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(<MediaLightbox media={media} initialIndex={0} onClose={onClose} />);

    await user.click(screen.getByRole('button', { name: 'close' }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('navigates between image and video media while disabling unavailable directions', async () => {
    const user = userEvent.setup();
    render(<MediaLightbox media={media} initialIndex={0} onClose={vi.fn()} />);

    const previousButton = screen.getByRole('button', { name: 'chevron_left' });
    const nextButton = screen.getByRole('button', { name: 'chevron_right' });
    expect(previousButton).toBeDisabled();
    expect(nextButton).toBeEnabled();

    await user.click(nextButton);
    const video = document.querySelector('video');
    expect(video).toHaveAttribute('src', 'https://example.com/video.mp4');
    expect(video).toHaveAttribute('autoplay');
    expect(previousButton).toBeEnabled();

    await user.click(nextButton);
    expect(screen.getByRole('img', { name: 'Second image' })).toHaveAttribute(
      'src',
      'https://example.com/two.jpg'
    );
    expect(nextButton).toBeDisabled();
  });

  it('does not render navigation controls for a single media item', () => {
    render(<MediaLightbox media={[media[0]]} initialIndex={0} onClose={vi.fn()} />);

    expect(screen.queryByRole('button', { name: 'chevron_left' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'chevron_right' })).not.toBeInTheDocument();
  });
});
