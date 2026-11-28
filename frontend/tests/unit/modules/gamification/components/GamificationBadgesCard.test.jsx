import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { LANGS, tr, useLang } from '../../../../utils/i18n';
import GamificationBadgesCard from '@/modules/gamification/components/GamificationOverviewTab/GamificationBadgesCard';

const t = (key, options) => tr(`gamification:${key}`, options);

describe('GamificationBadgesCard', () => {
  describe.each(LANGS)('localized rendering (%s)', (lang) => {
    it('renders empty state when badges is empty', async () => {
      await useLang(lang);
      render(<GamificationBadgesCard badges={[]} t={t} />);

      expect(screen.getByText(t('overview.badgesCard.title'))).toBeInTheDocument();
      expect(screen.getByText(t('overview.badgesCard.badgeCount', { count: 0 }))).toBeInTheDocument();
      expect(screen.getByText(t('overview.badgesCard.emptyTitle'))).toBeInTheDocument();
      expect(screen.getByText(t('overview.badgesCard.emptyDesc'))).toBeInTheDocument();
    });

    it('renders badges with icon image, fallback icon, and earnedAt date', async () => {
      await useLang(lang);
      const earnedDate = '2026-11-16T10:30:00';
      const badges = [
        {
          id: 1,
          name: 'Streak Master',
          description: '7 day streak',
          iconUrl: 'https://cdn.example.com/badge1.png',
          earnedAt: earnedDate,
        },
        {
          id: 2,
          name: 'First Lesson',
          description: 'Completed first lesson',
          iconUrl: null,
          earnedAt: null,
        },
      ];

      render(<GamificationBadgesCard badges={badges} t={t} />);

      expect(screen.getByText(t('overview.badgesCard.badgeCount', { count: 2 }))).toBeInTheDocument();
      expect(screen.getByRole('img', { name: 'Streak Master' })).toHaveAttribute(
        'src',
        'https://cdn.example.com/badge1.png'
      );
      expect(screen.getByText('shield')).toBeInTheDocument();
      expect(screen.getByText('First Lesson')).toBeInTheDocument();
      expect(screen.getByText('Completed first lesson')).toBeInTheDocument();
      expect(
        screen.getByText(
          t('overview.badgesCard.earnedAt', {
            date: new Date(earnedDate).toLocaleDateString(),
          })
        )
      ).toBeInTheDocument();
    });
  });
});
