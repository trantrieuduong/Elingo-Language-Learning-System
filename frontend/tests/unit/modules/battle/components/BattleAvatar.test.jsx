import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import BattleAvatar from '@/modules/battle/components/BattleAvatar/BattleAvatar';

describe('BattleAvatar component', () => {
  it('renders an image when avatarUrl is provided', () => {
    render(<BattleAvatar name="Alice" avatarUrl="https://cdn.test/a.png" />);

    const img = screen.getByRole('img', { name: 'Alice' });
    expect(img).toHaveAttribute('src', 'https://cdn.test/a.png');
    expect(img).toHaveClass('battle-avatar', 'battle-avatar--md');
  });

  it('uses a default alt text when image has no name', () => {
    render(<BattleAvatar avatarUrl="https://cdn.test/a.png" />);

    expect(screen.getByRole('img', { name: 'Avatar' })).toBeInTheDocument();
  });

  it('renders initials from the first and last word of the name', () => {
    render(<BattleAvatar name="Nguyen Van An" />);

    expect(screen.getByText('NA')).toBeInTheDocument();
    expect(screen.getByLabelText('Nguyen Van An')).toHaveClass('battle-avatar--fallback');
  });

  it('renders the first two letters for a single-word name', () => {
    render(<BattleAvatar name="alice" />);

    expect(screen.getByText('AL')).toBeInTheDocument();
  });

  it('ignores extra whitespace around the name', () => {
    render(<BattleAvatar name="   Tran   Minh   " />);

    expect(screen.getByText('TM')).toBeInTheDocument();
  });

  it('renders a question mark when name is empty, blank or not a string', () => {
    const { rerender } = render(<BattleAvatar name="" />);
    expect(screen.getByText('?')).toBeInTheDocument();

    rerender(<BattleAvatar name="   " />);
    expect(screen.getByText('?')).toBeInTheDocument();

    rerender(<BattleAvatar name={123} />);
    expect(screen.getByText('?')).toBeInTheDocument();
  });

  it('falls back to a default aria-label when name is empty', () => {
    render(<BattleAvatar />);

    expect(screen.getByLabelText('Avatar')).toBeInTheDocument();
  });

  it('applies the size class', () => {
    const { container, rerender } = render(<BattleAvatar name="Alice" size="lg" />);
    expect(container.firstChild).toHaveClass('battle-avatar--lg');

    rerender(<BattleAvatar name="Alice" avatarUrl="x.png" size="sm" />);
    expect(container.firstChild).toHaveClass('battle-avatar--sm');
  });
});
