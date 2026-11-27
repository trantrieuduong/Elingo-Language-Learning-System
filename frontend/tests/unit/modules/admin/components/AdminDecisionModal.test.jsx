import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdminDecisionModal from '@/modules/admin/components/AdminModal/AdminDecisionModal';

const defaultProps = {
  isOpen: true,
  isReject: true,
  resetKey: 'reject-1',
  title: 'Reject Item',
  subtitle: '#1 — Sample',
  closeLabel: 'Close',
  noteInputId: 'decision-note',
  noteLabel: 'Reason',
  notePlaceholder: 'Enter reason',
  noteRequiredMessage: 'Reason is required',
  submitErrorFallback: 'Failed to submit',
  cancelLabel: 'Cancel',
  submitLabel: 'Confirm Reject',
  submittingLabel: 'Submitting...',
  onClose: vi.fn(),
  onConfirm: vi.fn(),
};

describe('AdminDecisionModal component', () => {
  it('renders nothing when isOpen is false', () => {
    const { container } = render(<AdminDecisionModal {...defaultProps} isOpen={false} />);
    expect(container.firstChild).toBeNull();
  });

  it('requires non-empty note when isReject is true', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();

    render(<AdminDecisionModal {...defaultProps} onConfirm={onConfirm} />);

    await user.click(screen.getByRole('button', { name: 'Confirm Reject' }));

    expect(screen.getByText('Reason is required')).toBeInTheDocument();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('allows empty note when isReject is false and calls onConfirm', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn().mockResolvedValueOnce(undefined);

    render(
      <AdminDecisionModal
        {...defaultProps}
        isReject={false}
        submitLabel="Approve"
        onConfirm={onConfirm}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Approve' }));

    await waitFor(() => {
      expect(onConfirm).toHaveBeenCalledWith('');
    });
  });

  it('displays API error message or fallback on rejection', async () => {
    const user = userEvent.setup();
    const onConfirm = vi
      .fn()
      .mockRejectedValueOnce({ response: { data: { error: { message: 'Server rejected' } } } })
      .mockRejectedValueOnce(new Error('Unknown'));

    render(<AdminDecisionModal {...defaultProps} onConfirm={onConfirm} />);

    const textarea = screen.getByLabelText(/Reason/);
    await user.type(textarea, 'Violation');
    expect(screen.getByText('9/500')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Confirm Reject' }));
    await waitFor(() => {
      expect(screen.getByText('Server rejected')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Confirm Reject' }));
    await waitFor(() => {
      expect(screen.getByText('Failed to submit')).toBeInTheDocument();
    });
  });

  it('resets note and error when resetKey changes', async () => {
    const user = userEvent.setup();
    const { rerender } = render(<AdminDecisionModal {...defaultProps} />);

    await user.type(screen.getByLabelText(/Reason/), 'Draft note');
    expect(screen.getByText('10/500')).toBeInTheDocument();

    rerender(<AdminDecisionModal {...defaultProps} resetKey="reject-2" />);
    expect(screen.getByLabelText(/Reason/)).toHaveValue('');
    expect(screen.getByText('0/500')).toBeInTheDocument();
  });
});
