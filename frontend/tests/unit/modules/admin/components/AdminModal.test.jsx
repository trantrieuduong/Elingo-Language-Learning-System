import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import AdminModal from '@/modules/admin/components/AdminModal/AdminModal';

describe('AdminModal component', () => {
  it('returns null when isOpen is false', () => {
    const { container } = render(
      <AdminModal isOpen={false} onClose={vi.fn()} title="Modal Title">
        <div>Body content</div>
      </AdminModal>,
    );

    expect(container.firstChild).toBeNull();
  });

  it('renders title, subtitle, icon, body, and footer when open', () => {
    render(
      <AdminModal
        isOpen={true}
        onClose={vi.fn()}
        title="Modal Title"
        subtitle="Modal Subtitle"
        icon={<span data-testid="modal-icon" />}
        titleId="custom-title-id"
        closeLabel="Close Modal"
        wide={true}
        footer={<button type="button">Save</button>}
      >
        <div>Body content</div>
      </AdminModal>,
    );

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveAttribute('aria-labelledby', 'custom-title-id');
    expect(screen.getByText('Modal Title')).toBeInTheDocument();
    expect(screen.getByText('Modal Subtitle')).toBeInTheDocument();
    expect(screen.getByTestId('modal-icon')).toBeInTheDocument();
    expect(screen.getByText('Body content')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Close Modal' })).toBeInTheDocument();
  });

  it('handles closeOnBackdrop and closeDisabled properly', () => {
    const onClose = vi.fn();

    render(
      <AdminModal
        isOpen={true}
        onClose={onClose}
        title="Modal Title"
        closeOnBackdrop={true}
        closeDisabled={true}
        closeLabel="Close"
      >
        <div>Inner content</div>
      </AdminModal>,
    );

    expect(screen.getByRole('button', { name: 'Close' })).toBeDisabled();

    fireEvent.click(screen.getByText('Inner content'));
    expect(onClose).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('dialog'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
