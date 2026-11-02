import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import Pagination from '@/components/Pagination/Pagination';
import { tr } from '../../utils/i18n';

describe('Pagination', () => {
  it.each([0, 1, null, undefined])('không render khi totalPages là %s', (totalPages) => {
    const { container } = render(
      <Pagination currentPage={1} totalPages={totalPages} onPageChange={vi.fn()} />
    );

    expect(container.firstChild).toBeNull();
  });

  it('hiển thị trang hiện tại và dấu ellipsis (…)', () => {
    const { container } = render(
      <Pagination currentPage={4} totalPages={7} onPageChange={vi.fn()} />
    );

    expect(container.querySelectorAll('.pagination__ellipsis')).toHaveLength(2);
    expect(
      screen.getByRole('button', { name: '4' }).classList.contains('pagination__page--active')
    ).toBe(true);
    expect(screen.getByRole('button', { name: '1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '7' })).toBeInTheDocument();
  });

  it('gọi onPageChange khi nhấn Previous, Next hoặc một số trang', () => {
    const onPageChange = vi.fn();
    const { container } = render(
      <Pagination currentPage={2} totalPages={3} onPageChange={onPageChange} />
    );
    const navigationButtons = container.querySelectorAll('.pagination__btn');

    fireEvent.click(navigationButtons[0]);
    fireEvent.click(screen.getByRole('button', { name: '3' }));
    fireEvent.click(navigationButtons[1]);

    expect(onPageChange.mock.calls).toEqual([[1], [3], [3]]);
  });

  it('vô hiệu hóa Previous ở trang đầu và Next ở trang cuối', () => {
    const { container, rerender } = render(
      <Pagination currentPage={1} totalPages={3} onPageChange={vi.fn()} />
    );
    let navigationButtons = container.querySelectorAll('.pagination__btn');

    expect(navigationButtons[0].disabled).toBe(true);
    expect(navigationButtons[1].disabled).toBe(false);

    rerender(<Pagination currentPage={3} totalPages={3} onPageChange={vi.fn()} />);
    navigationButtons = container.querySelectorAll('.pagination__btn');

    expect(navigationButtons[0].disabled).toBe(false);
    expect(navigationButtons[1].disabled).toBe(true);
  });

  it('chỉ hiển thị jump input khi có hơn 5 trang', () => {
    const { rerender } = render(
      <Pagination currentPage={1} totalPages={5} onPageChange={vi.fn()} />
    );

    expect(screen.queryByPlaceholderText(tr('common:pagination.goTo'))).toBeNull();

    rerender(<Pagination currentPage={1} totalPages={6} onPageChange={vi.fn()} />);
    expect(screen.getByPlaceholderText(tr('common:pagination.goTo'))).toBeInTheDocument();
  });

  it('chuyển đến trang hợp lệ khi nhấn Enter và xóa input', () => {
    const onPageChange = vi.fn();
    render(<Pagination currentPage={1} totalPages={10} onPageChange={onPageChange} />);
    const input = screen.getByPlaceholderText(tr('common:pagination.goTo'));

    fireEvent.change(input, { target: { value: '6' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(onPageChange).toHaveBeenCalledWith(6);
    expect(input.value).toBe('');
  });

  it.each(['0', '11'])('bỏ qua trang ngoài giới hạn: %s', (page) => {
    const onPageChange = vi.fn();
    render(<Pagination currentPage={1} totalPages={10} onPageChange={onPageChange} />);
    const input = screen.getByPlaceholderText(tr('common:pagination.goTo'));

    fireEvent.change(input, { target: { value: page } });
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(onPageChange).not.toHaveBeenCalled();
    expect(input.value).toBe(page);
  });

  it('không xử lý jump khi phím nhấn không phải Enter', () => {
    const onPageChange = vi.fn();
    render(<Pagination currentPage={1} totalPages={10} onPageChange={onPageChange} />);
    const input = screen.getByPlaceholderText(tr('common:pagination.goTo'));

    fireEvent.change(input, { target: { value: '6' } });
    fireEvent.keyDown(input, { key: 'Tab' });

    expect(onPageChange).not.toHaveBeenCalled();
    expect(input.value).toBe('6');
  });
});
