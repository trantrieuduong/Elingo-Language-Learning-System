import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { LANGS, tr, useLang } from '../../../../utils/i18n';
import GamificationLevelCard from '@/modules/gamification/components/GamificationOverviewTab/GamificationLevelCard';

const t = (key, options) => tr(`gamification:${key}`, options);

describe('GamificationLevelCard', () => {
  describe.each(LANGS)('localized rendering (%s)', (lang) => {
    it('renders current level progress and next level hint', async () => {
      await useLang(lang);
      const levelProgress = {
        currentLevel: 2,
        currentTitle: 'Explorer',
        totalXp: 150,
        nextLevel: 3,
        nextLevelTitle: 'Challenger',
        nextLevelRequiredXp: 300,
        xpToNextLevel: 150,
        progressPercent: 25,
      };

      render(<GamificationLevelCard levelProgress={levelProgress} t={t} />);

      expect(screen.getByText(t('overview.levelCard.title'))).toBeInTheDocument();
      expect(screen.getByText('Explorer')).toBeInTheDocument();
      expect(screen.getByText(t('overview.levelCard.level', { level: 2 }))).toBeInTheDocument();
      expect(screen.getByText(t('overview.levelCard.totalXp', { xp: 150 }))).toBeInTheDocument();
      expect(
        screen.getByText(
          t('overview.levelCard.nextLevel', { xp: 150, title: 'Challenger', level: 3 })
        )
      ).toBeInTheDocument();
    });

    it('renders maxLevel message when nextLevel is null', async () => {
      await useLang(lang);
      const levelProgress = {
        currentLevel: 10,
        currentTitle: 'Legend',
        totalXp: 5000,
        nextLevel: null,
        nextLevelRequiredXp: null,
        progressPercent: null,
      };

      render(<GamificationLevelCard levelProgress={levelProgress} t={t} />);

      expect(screen.getByText(t('overview.levelCard.maxLevel'))).toBeInTheDocument();
    });
  });

  it('handles null levelProgress with safe defaults', () => {
    render(<GamificationLevelCard levelProgress={null} t={t} />);

    expect(screen.getByText(t('overview.levelCard.level', { level: 1 }))).toBeInTheDocument();
    expect(screen.getByText(t('overview.levelCard.totalXp', { xp: 0 }))).toBeInTheDocument();
    expect(screen.getByText(t('overview.levelCard.maxLevel'))).toBeInTheDocument();
  });
});
