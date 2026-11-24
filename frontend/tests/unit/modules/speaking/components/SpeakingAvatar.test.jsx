import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import SpeakingAvatar from '@/modules/speaking/components/SpeakingAvatar/SpeakingAvatar';

describe('SpeakingAvatar component', () => {
  it('shows the uppercased first letter of the name when there is no image', () => {
    render(<SpeakingAvatar name="alice" />);

    expect(screen.getByText('A')).toBeInTheDocument();
  });

  it('trims the name before taking the initial', () => {
    render(<SpeakingAvatar name="   bob" />);

    expect(screen.getByText('B')).toBeInTheDocument();
  });

  it.each([[''], ['   '], [undefined], [null]])('shows ? when the name is %j', (name) => {
    render(<SpeakingAvatar name={name} />);

    expect(screen.getByText('?')).toBeInTheDocument();
  });

  it('renders the image with the name as alt text when src is provided', () => {
    render(<SpeakingAvatar name="Alice" src="https://example.com/a.png" />);

    const img = screen.getByAltText('Alice');
    expect(img).toHaveAttribute('src', 'https://example.com/a.png');
    expect(screen.queryByText('A')).not.toBeInTheDocument();
  });

  it('falls back to the initial when the image fails to load', () => {
    render(<SpeakingAvatar name="Alice" src="https://example.com/broken.png" />);

    fireEvent.error(screen.getByAltText('Alice'));

    expect(screen.queryByAltText('Alice')).not.toBeInTheDocument();
    expect(screen.getByText('A')).toBeInTheDocument();
  });

  it('applies the size in pixels', () => {
    const { container } = render(<SpeakingAvatar name="Alice" size={84} />);

    const root = container.firstChild;
    expect(root).toHaveStyle({ width: '84px', height: '84px', minWidth: '84px' });
  });

  it('uses a 40px size by default', () => {
    const { container } = render(<SpeakingAvatar name="Alice" />);

    expect(container.firstChild).toHaveStyle({ width: '40px', height: '40px' });
  });

  it('appends the custom class name and hides itself from assistive tech', () => {
    const { container } = render(<SpeakingAvatar name="Alice" className="custom-avatar" />);

    expect(container.firstChild).toHaveClass('speaking-avatar', 'custom-avatar');
    expect(container.firstChild).toHaveAttribute('aria-hidden', 'true');
  });
});
