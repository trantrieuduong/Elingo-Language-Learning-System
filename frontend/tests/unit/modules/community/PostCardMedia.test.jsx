import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostCardMedia from '@/modules/community/components/PostCard/PostCardMedia/PostCardMedia';

vi.mock('@/modules/community/components/MediaLightbox/MediaLightbox', () => ({
  default: ({ initialIndex, onClose }) => (
    <button type="button" onClick={onClose}>
      lightbox:{initialIndex}
    </button>
  ),
}));

const media = [
  { id: '1', type: 'image', url: 'https://example.com/1.jpg', alt: 'One' },
  { id: '2', type: 'video', url: 'https://example.com/2.mp4' },
  { id: '3', type: 'image', url: 'https://example.com/3.jpg', alt: 'Three' },
  { id: '4', type: 'image', url: 'https://example.com/4.jpg', alt: 'Four' },
  { id: '5', type: 'image', url: 'https://example.com/5.jpg', alt: 'Five' },
];

describe('PostCardMedia', () => {
  it('returns nothing when no media is provided', () => {
    const { container } = render(<PostCardMedia media={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders supported media, overflow indicators, and opens then closes its lightbox', async () => {
    const user = userEvent.setup();
    const { container } = render(<PostCardMedia media={media} />);

    expect(container.firstChild).toHaveClass('post-card-media--quad');
    expect(screen.getByRole('img', { name: 'One' })).toBeInTheDocument();
    expect(document.querySelector('video')).toHaveAttribute('src', media[1].url);
    expect(screen.getByText('+2')).toBeInTheDocument();
    expect(screen.getByText('+1')).toBeInTheDocument();

    await user.click(screen.getByRole('img', { name: 'Three' }));
    await user.click(screen.getByRole('button', { name: 'lightbox:2' }));
    expect(screen.queryByRole('button', { name: 'lightbox:2' })).not.toBeInTheDocument();
  });
});
