import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ScoreBoard from '@/modules/battle/components/ScoreBoard/ScoreBoard';
import { tr } from '../../../../utils/i18n';

describe('ScoreBoard component', () => {
  const me = { userId: 1, name: 'Alice', avatarUrl: null };
  const opponent = { userId: 2, name: 'Bob', avatarUrl: null };

  it('renders both players with their scores', () => {
    render(<ScoreBoard me={me} opponent={opponent} scores={{ 1: 250, 2: 120 }} />);

    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('250')).toBeInTheDocument();
    expect(screen.getByText('120')).toBeInTheDocument();
  });

  it('defaults scores to zero when a player has no score yet', () => {
    render(<ScoreBoard me={me} opponent={opponent} scores={{}} />);

    expect(screen.getAllByText('0')).toHaveLength(2);
  });

  it('renders the round badge with current and total rounds', () => {
    render(<ScoreBoard me={me} opponent={opponent} roundNo={3} totalRounds={8} />);

    expect(screen.getByText(tr('battle:play.round', { current: 3, total: 8 }))).toBeInTheDocument();
  });

  it('uses default round values', () => {
    render(<ScoreBoard me={me} opponent={opponent} />);

    expect(screen.getByText(tr('battle:play.round', { current: 1, total: 10 }))).toBeInTheDocument();
  });

  it('falls back to default names when player names are missing', () => {
    render(<ScoreBoard me={{ userId: 1 }} opponent={{ userId: 2 }} />);

    expect(screen.getByText(tr('battle:play.you'))).toBeInTheDocument();
    expect(screen.getByText(tr('battle:play.opponentFallback'))).toBeInTheDocument();
  });

  it('shows zero scores when me and opponent are not provided', () => {
    render(<ScoreBoard scores={{ 1: 100 }} />);

    expect(screen.getAllByText('0')).toHaveLength(2);
    expect(screen.queryByText('100')).not.toBeInTheDocument();
  });

  it('shows the answered chip only for players who have answered', () => {
    const { rerender } = render(<ScoreBoard me={me} opponent={opponent} />);
    expect(screen.queryByText(tr('battle:play.answered'))).not.toBeInTheDocument();

    rerender(<ScoreBoard me={me} opponent={opponent} meAnswered />);
    expect(screen.getAllByText(tr('battle:play.answered'))).toHaveLength(1);

    rerender(<ScoreBoard me={me} opponent={opponent} meAnswered opponentAnswered />);
    expect(screen.getAllByText(tr('battle:play.answered'))).toHaveLength(2);
  });

  it('renders avatars with initials for both players', () => {
    render(<ScoreBoard me={{ userId: 1, name: 'Alice Nguyen' }} opponent={{ userId: 2, name: 'Bob Tran' }} />);

    expect(screen.getByText('AN')).toBeInTheDocument();
    expect(screen.getByText('BT')).toBeInTheDocument();
  });
});
