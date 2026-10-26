import EmptyState from './EmptyState';

export default {
  title: 'Components/EmptyState',
  component: EmptyState,
  argTypes: {
    onAction: { action: 'action-clicked' },
  },
};

export const ErrorState = {
  args: {
    icon: 'error',
    isError: true,
    description: 'Không thể tải danh sách bộ từ. Vui lòng thử lại',
    actionLabel: 'Thử lại',
    actionVariant: 'secondary',
  },
};

export const EmptyWithAction = {
  args: {
    icon: 'layers',
    title: 'Bạn chưa có bộ từ vựng nào',
    description: 'Tạo bộ từ đầu tiên để thêm và học từ vựng.',
    actionLabel: 'Tạo bộ từ mới',
    actionVariant: 'primary',
  },
};
