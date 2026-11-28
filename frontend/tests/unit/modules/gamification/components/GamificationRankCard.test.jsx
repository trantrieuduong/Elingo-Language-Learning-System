import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { LANGS, tr, useLang } from '../../../../utils/i18n';
import GamificationRankCard from '@/modules/gamification/components/GamificationOverviewTab/GamificationRankCard';

const t = (key, options) => tr(`gamification:${key}`, options);

const buildPeriodTabs = () => [
  { key: 'WEEKLY', label: t('overview.rankCard.weekly') },
  { key: 'MONTHLY', label: t('overview.rankCard.monthly') },
];

describe('GamificationRankCard', () => {
  describe.each(LANGS)('localized rendering (%s)', (lang) => {
    it('renders rank, periodKey, totalXp and diffToNext tip when rank > 1', async () => {
      await useLang(lang);
      render(
        <GamificationRankCard
          rankData={{ rank: 4, totalXp: 820, diffToNextRank: 45, periodKey: '2026-W47' }}
          rankPeriod="WEEKLY"
          rankPeriodTabs={buildPeriodTabs()}
          onPeriodChange={vi.fn()}
          t={t}
        />
      );

      expect(screen.getByText(t('overview.rankCard.title'))).toBeInTheDocument();
      expect(screen.getByText('2026-W47')).toBeInTheDocument();
      expect(screen.getByText(t('overview.rankCard.rank', { rank: 4 }))).toBeInTheDocument();
      expect(screen.getByText(t('overview.rankCard.totalXp', { xp: 820 }))).toBeInTheDocument();
      expect(screen.getByText(t('overview.rankCard.diffToNext', { xp: 45 }))).toBeInTheDocument();
    });

    it('renders topRank tip when rank is 1', async () => {
      await useLang(lang);
      render(
        <GamificationRankCard
          rankData={{ rank: 1, totalXp: 1500, diffToNextRank: 0, periodKey: '2026-W47' }}
          rankPeriod="WEEKLY"
          rankPeriodTabs={buildPeriodTabs()}
          onPeriodChange={vi.fn()}
          t={t}
        />
      );

      expect(screen.getByText(t('overview.rankCard.topRank'))).toBeInTheDocument();
      expect(screen.queryByText('trending_up')).not.toBeInTheDocument();
    });

    it('renders unranked when rank is missing', async () => {
      await useLang(lang);
      render(
        <GamificationRankCard
          rankData={null}
          rankPeriod="WEEKLY"
          rankPeriodTabs={buildPeriodTabs()}
          onPeriodChange={vi.fn()}
          t={t}
        />
      );

      expect(screen.getByText(t('overview.rankCard.unranked'))).toBeInTheDocument();
      expect(screen.getByText(t('overview.rankCard.totalXp', { xp: 0 }))).toBeInTheDocument();
      expect(screen.queryByText('emoji_events')).not.toBeInTheDocument();
      expect(screen.queryByText('trending_up')).not.toBeInTheDocument();
    });
  });

  it('calls onPeriodChange when switching period tab', () => {
    const onPeriodChange = vi.fn();
    render(
      <GamificationRankCard
        rankData={{ rank: 2, totalXp: 500, diffToNextRank: 0 }}
        rankPeriod="WEEKLY"
        rankPeriodTabs={buildPeriodTabs()}
        onPeriodChange={onPeriodChange}
        t={t}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: t('overview.rankCard.monthly') }));
    expect(onPeriodChange).toHaveBeenCalledWith('MONTHLY');
  });
});
