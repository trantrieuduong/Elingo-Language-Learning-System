import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Tabs from '../../../src/components/Tabs/Tabs';

describe('Tabs Component', () => {
  const sampleTabs = [
    { key: 'tab1', label: 'Tab Một', icon: 'home' },
    { key: 'tab2', label: 'Tab Hai', icon: 'settings' },
  ];

  it('renders tab items correctly with labels and icons', () => {
    render(<Tabs tabs={sampleTabs} activeTab="tab1" onChange={vi.fn()} />);

    expect(screen.getByText('Tab Một')).toBeInTheDocument();
    expect(screen.getByText('Tab Hai')).toBeInTheDocument();
    expect(screen.getByText('home')).toBeInTheDocument();
    expect(screen.getByText('settings')).toBeInTheDocument();
  });

  it('marks active tab with active class', () => {
    render(<Tabs tabs={sampleTabs} activeTab="tab2" onChange={vi.fn()} />);

    const tab2Btn = screen.getByRole('button', { name: 'Tab Hai' });
    expect(tab2Btn).toHaveClass('tabs-btn--active');

    const tab1Btn = screen.getByRole('button', { name: 'Tab Một' });
    expect(tab1Btn).not.toHaveClass('tabs-btn--active');
  });

  it('calls onChange with tab key when a non-active tab is clicked', () => {
    const handleChange = vi.fn();
    render(<Tabs tabs={sampleTabs} activeTab="tab1" onChange={handleChange} />);

    const tab2Btn = screen.getByRole('button', { name: 'Tab Hai' });
    fireEvent.click(tab2Btn);

    expect(handleChange).toHaveBeenCalledWith('tab2');
  });

  it('does not trigger onChange when clicking the currently active tab', () => {
    const handleChange = vi.fn();
    render(<Tabs tabs={sampleTabs} activeTab="tab1" onChange={handleChange} />);

    const tab1Btn = screen.getByRole('button', { name: 'Tab Một' });
    fireEvent.click(tab1Btn);
    expect(handleChange).not.toHaveBeenCalled();
  });
});
