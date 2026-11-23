import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SystemConfigPage from '@/modules/admin/pages/SystemConfigPage/SystemConfigPage';
import * as adminSystemConfigApi from '@/modules/admin/services/adminSystemConfigApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminSystemConfigApi', () => ({
  fetchSystemConfigs: vi.fn(),
  fetchConfigModules: vi.fn(),
  updateSystemConfig: vi.fn(),
}));

describe('SystemConfigPage component', () => {
  const mockNavigate = vi.fn();

  const sampleConfigs = [
    {
      id: 1,
      configKey: 'app.premium.bank-transfer.code-prefix',
      configValue: 'ELGO',
      valueType: 'STRING',
      isSecret: false,
      module: 'PREMIUM',
      description: 'Prefix code for bank transfer',
      updatedBy: 99,
      updatedAt: '2026-11-20T10:00:00',
      createdAt: '2026-11-19T08:00:00',
    },
    {
      id: 2,
      configKey: 'app.premium.sepay.secret-key',
      configValue: '********',
      valueType: 'STRING',
      isSecret: true,
      module: 'PREMIUM',
      description: 'SePay secret authentication key',
      updatedBy: 99,
      updatedAt: '2026-11-20T10:00:00',
      createdAt: '2026-11-19T08:00:00',
    },
    {
      id: 3,
      configKey: 'app.gamification.rules.battle-win.xp',
      configValue: '30',
      valueType: 'INT',
      isSecret: false,
      module: 'GAMIFICATION',
      description: 'XP awarded for winning battle',
      updatedBy: 99,
      updatedAt: '2026-11-20T10:00:00',
      createdAt: '2026-11-19T08:00:00',
    },
  ];

  const sampleModules = ['PREMIUM', 'COMMUNITY', 'GAMIFICATION'];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n and SystemConfigPage functionality [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders system configs list and counters correctly', async () => {
      adminSystemConfigApi.fetchConfigModules.mockResolvedValueOnce(sampleModules);
      adminSystemConfigApi.fetchSystemConfigs.mockResolvedValueOnce(sampleConfigs);

      render(<SystemConfigPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('app.premium.bank-transfer.code-prefix')).toBeInTheDocument();
        expect(screen.getByText('app.premium.sepay.secret-key')).toBeInTheDocument();
        expect(screen.getByText('app.gamification.rules.battle-win.xp')).toBeInTheDocument();
      });

      // Heading
      const title = lng === 'vi' ? 'Cấu hình hệ thống' : 'System Configuration';
      expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument();

      // Descriptions and values
      expect(screen.getByText('Prefix code for bank transfer')).toBeInTheDocument();
      expect(screen.getByText('ELGO')).toBeInTheDocument();
      expect(screen.getByText('30')).toBeInTheDocument();

      // Check KPI counters
      expect(screen.getByText('3')).toBeInTheDocument(); // total count
      expect(screen.getByText('1')).toBeInTheDocument(); // secret count
    });

    it('filters configs by module selection', async () => {
      adminSystemConfigApi.fetchConfigModules.mockResolvedValueOnce(sampleModules);
      adminSystemConfigApi.fetchSystemConfigs.mockResolvedValueOnce(sampleConfigs);
      const user = userEvent.setup();

      render(<SystemConfigPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('app.premium.bank-transfer.code-prefix')).toBeInTheDocument();
      });

      const gamificationOnly = [sampleConfigs[2]];
      adminSystemConfigApi.fetchSystemConfigs.mockResolvedValueOnce(gamificationOnly);

      const moduleSelect = screen.getByRole('combobox');
      await user.selectOptions(moduleSelect, 'GAMIFICATION');

      await waitFor(() => {
        expect(adminSystemConfigApi.fetchSystemConfigs).toHaveBeenCalledWith('GAMIFICATION');
      });
    });

    it('filters configs by search input keyword', async () => {
      adminSystemConfigApi.fetchConfigModules.mockResolvedValueOnce(sampleModules);
      adminSystemConfigApi.fetchSystemConfigs.mockResolvedValueOnce(sampleConfigs);
      const user = userEvent.setup();

      render(<SystemConfigPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('app.premium.bank-transfer.code-prefix')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText(
        lng === 'vi'
          ? 'Tìm kiếm theo mã cấu hình hoặc mô tả...'
          : 'Search by config key or description...'
      );

      // Search by description keyword
      await user.type(searchInput, 'winning battle');

      expect(screen.getByText('app.gamification.rules.battle-win.xp')).toBeInTheDocument();
      expect(screen.queryByText('app.premium.bank-transfer.code-prefix')).not.toBeInTheDocument();
    });

    it('refreshes configs when clicking refresh button', async () => {
      adminSystemConfigApi.fetchConfigModules.mockResolvedValueOnce(sampleModules);
      adminSystemConfigApi.fetchSystemConfigs.mockResolvedValueOnce(sampleConfigs);
      const user = userEvent.setup();

      render(<SystemConfigPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('app.premium.bank-transfer.code-prefix')).toBeInTheDocument();
      });

      const refreshBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Làm mới' : 'Refresh',
      });

      adminSystemConfigApi.fetchSystemConfigs.mockResolvedValueOnce(sampleConfigs);
      await user.click(refreshBtn);

      expect(adminSystemConfigApi.fetchSystemConfigs).toHaveBeenCalledTimes(2);
    });

    it('opens edit modal and updates config successfully', async () => {
      adminSystemConfigApi.fetchConfigModules.mockResolvedValueOnce(sampleModules);
      adminSystemConfigApi.fetchSystemConfigs.mockResolvedValueOnce(sampleConfigs);
      const user = userEvent.setup();

      render(<SystemConfigPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('app.premium.bank-transfer.code-prefix')).toBeInTheDocument();
      });

      // Click first edit button
      const editButtons = screen.getAllByRole('button', {
        name: lng === 'vi' ? 'Chỉnh sửa cấu hình' : 'Edit System Configuration',
      });
      await user.click(editButtons[0]);

      // Verify modal is open
      const modalHeading = lng === 'vi' ? 'Chỉnh sửa cấu hình' : 'Edit System Configuration';
      expect(screen.getByRole('heading', { level: 2, name: modalHeading })).toBeInTheDocument();

      // Change value
      const valueInput = screen.getByDisplayValue('ELGO');
      await user.clear(valueInput);
      await user.type(valueInput, 'ELGO_V2');

      const updated = {
        ...sampleConfigs[0],
        configValue: 'ELGO_V2',
      };
      adminSystemConfigApi.updateSystemConfig.mockResolvedValueOnce(updated);

      const saveBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Lưu thay đổi' : 'Save Changes',
      });
      await user.click(saveBtn);

      await waitFor(() => {
        expect(adminSystemConfigApi.updateSystemConfig).toHaveBeenCalledWith(
          'app.premium.bank-transfer.code-prefix',
          {
            value: 'ELGO_V2',
            description: 'Prefix code for bank transfer',
          }
        );
      });

      // Modal should be closed
      await waitFor(() => {
        expect(screen.queryByRole('heading', { level: 2, name: modalHeading })).not.toBeInTheDocument();
      });
    });

    it('displays error state when API fails and retries on button click', async () => {
      adminSystemConfigApi.fetchConfigModules.mockResolvedValueOnce(sampleModules);
      adminSystemConfigApi.fetchSystemConfigs.mockRejectedValueOnce(new Error('Network error'));
      const user = userEvent.setup();

      render(<SystemConfigPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Network error')).toBeInTheDocument();
      });

      const retryBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Thử lại' : 'Retry',
      });

      adminSystemConfigApi.fetchSystemConfigs.mockResolvedValueOnce(sampleConfigs);
      await user.click(retryBtn);

      await waitFor(() => {
        expect(screen.getByText('app.premium.bank-transfer.code-prefix')).toBeInTheDocument();
      });
    });
  });
});
