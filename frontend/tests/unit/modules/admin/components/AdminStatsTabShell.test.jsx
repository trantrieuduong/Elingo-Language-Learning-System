import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import AdminStatsTabShell from '@/modules/admin/pages/DashboardPage/components/AdminStatsTabShell';

vi.mock('@/modules/admin/components/Feedback/AdminFeedback', () => ({
  AdminSkeletonCard: () => <div data-testid="skeleton-card" />,
  AdminErrorState: ({ message }) => <div data-testid="error-state">{message}</div>,
}));

describe('AdminStatsTabShell component', () => {
  it('renders 4 skeleton cards while loading', () => {
    render(
      <AdminStatsTabShell loading={true} error={null}>
        <div>Loaded content</div>
      </AdminStatsTabShell>,
    );

    expect(screen.getAllByTestId('skeleton-card')).toHaveLength(4);
    expect(screen.queryByText('Loaded content')).not.toBeInTheDocument();
  });

  it('renders AdminErrorState when error is present', () => {
    render(
      <AdminStatsTabShell loading={false} error="Failed to load stats">
        <div>Loaded content</div>
      </AdminStatsTabShell>,
    );

    expect(screen.getByTestId('error-state')).toHaveTextContent('Failed to load stats');
    expect(screen.queryByText('Loaded content')).not.toBeInTheDocument();
  });

  it('renders children when loaded without error', () => {
    render(
      <AdminStatsTabShell loading={false} error={null}>
        <div>Loaded content</div>
      </AdminStatsTabShell>,
    );

    expect(screen.getByText('Loaded content')).toBeInTheDocument();
  });
});
