import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from '@/i18n';
import ChatPanel from '@/modules/speaking/components/ChatPanel/ChatPanel';
import { LANGS, tr, useLang } from '../../../../utils/i18n';

describe('ChatPanel component', () => {
  const originalScrollTo = HTMLElement.prototype.scrollTo;
  let onSend;
  let onClose;

  const TIMESTAMP = 1700000000000;
  const messages = [
    { id: 1, fromUserId: 2, displayName: 'Bob', text: 'Hi there', timestamp: TIMESTAMP },
    { id: 2, fromUserId: 1, displayName: 'Me', text: 'Hello Bob', timestamp: TIMESTAMP },
  ];

  const renderPanel = (props = {}) =>
    render(
      <ChatPanel open messages={[]} selfId={1} onSend={onSend} onClose={onClose} {...props} />
    );

  const textarea = () => screen.getByPlaceholderText(tr('speaking:chat.placeholder'));
  const sendButton = () => screen.getByRole('button', { name: tr('speaking:chat.send') });
  const log = () => screen.getByRole('log');

  // jsdom không có kích thước thật nên phải giả lập vị trí cuộn của khung tin nhắn
  const setScrollPosition = (element, { scrollHeight, clientHeight, scrollTop }) => {
    Object.defineProperty(element, 'scrollHeight', { value: scrollHeight, configurable: true });
    Object.defineProperty(element, 'clientHeight', { value: clientHeight, configurable: true });
    Object.defineProperty(element, 'scrollTop', { value: scrollTop, configurable: true, writable: true });
  };

  beforeEach(() => {
    onSend = vi.fn().mockReturnValue(true);
    onClose = vi.fn();
    // jsdom không cài đặt Element.scrollTo
    HTMLElement.prototype.scrollTo = vi.fn();
  });

  afterEach(() => {
    HTMLElement.prototype.scrollTo = originalScrollTo;
  });

  describe('visibility', () => {
    it('renders nothing when closed', () => {
      renderPanel({ open: false });

      expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
    });

    it('does not close on Escape when it is already closed', () => {
      renderPanel({ open: false });

      fireEvent.keyDown(window, { key: 'Escape' });

      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe.each(LANGS)('content [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('shows the panel with its title, empty state and input', () => {
      renderPanel();

      expect(screen.getByRole('complementary', { name: tr('speaking:chat.title') })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: tr('speaking:chat.title') })).toBeInTheDocument();
      expect(screen.getByText(tr('speaking:chat.empty'))).toBeInTheDocument();
      expect(textarea()).toBeInTheDocument();
      expect(sendButton()).toBeDisabled();
    });
  });

  describe('messages', () => {
    it('renders every message and hides the empty state', () => {
      renderPanel({ messages });

      expect(screen.getByText('Hi there')).toBeInTheDocument();
      expect(screen.getByText('Hello Bob')).toBeInTheDocument();
      expect(screen.queryByText(tr('speaking:chat.empty'))).not.toBeInTheDocument();
    });

    it('shows the sender name only for messages from others', () => {
      renderPanel({ messages });

      expect(screen.getByText('Bob')).toBeInTheDocument();
      expect(screen.queryByText('Me')).not.toBeInTheDocument();
    });

    it('styles own and others messages differently', () => {
      const { container } = renderPanel({ messages });

      expect(container.querySelectorAll('.speaking-chat-msg--peer')).toHaveLength(1);
      expect(container.querySelectorAll('.speaking-chat-msg--self')).toHaveLength(1);
    });

    it('recognises own messages even when the id types differ', () => {
      const { container } = renderPanel({
        selfId: 1,
        messages: [{ id: 1, fromUserId: '1', displayName: 'Me', text: 'mine', timestamp: TIMESTAMP }],
      });

      expect(container.querySelector('.speaking-chat-msg--self')).toBeInTheDocument();
    });

    it('shows the time of each message', () => {
      const { container } = renderPanel({ messages });
      const expected = new Date(TIMESTAMP).toLocaleTimeString(i18n.language, {
        hour: '2-digit',
        minute: '2-digit',
      });

      const times = container.querySelectorAll('.speaking-chat-msg__time');
      expect(times).toHaveLength(2);
      times.forEach((time) => expect(time).toHaveTextContent(expected));
    });

    it('omits the time when the message has no timestamp', () => {
      const { container } = renderPanel({
        messages: [{ id: 1, fromUserId: 2, displayName: 'Bob', text: 'no time' }],
      });

      expect(container.querySelector('.speaking-chat-msg__time')).not.toBeInTheDocument();
    });

    it('scrolls to the latest message when opened', async () => {
      renderPanel({ messages });

      await waitFor(() =>
        expect(HTMLElement.prototype.scrollTo).toHaveBeenCalledWith(
          expect.objectContaining({ behavior: 'auto' })
        )
      );
    });
  });

  describe('input', () => {
    it('focuses the textarea shortly after opening', async () => {
      renderPanel();

      await waitFor(() => expect(textarea()).toHaveFocus());
    });

    it('enables the send button once there is text', async () => {
      const user = userEvent.setup();
      renderPanel();

      await user.type(textarea(), 'hi');

      expect(textarea()).toHaveValue('hi');
      expect(sendButton()).toBeEnabled();
    });

    it('keeps the send button disabled for whitespace only', async () => {
      const user = userEvent.setup();
      renderPanel();

      await user.type(textarea(), '   ');

      expect(sendButton()).toBeDisabled();
    });

    it('truncates the text to 500 characters', () => {
      renderPanel();

      fireEvent.change(textarea(), { target: { value: 'a'.repeat(600) } });

      expect(textarea().value).toHaveLength(500);
    });

    it('shows the character counter from 400 characters', () => {
      renderPanel();

      fireEvent.change(textarea(), { target: { value: 'a'.repeat(399) } });
      expect(screen.queryByText(tr('speaking:chat.counter', { count: 399 }))).not.toBeInTheDocument();

      fireEvent.change(textarea(), { target: { value: 'a'.repeat(400) } });
      expect(screen.getByText(tr('speaking:chat.counter', { count: 400 }))).toBeInTheDocument();
    });
  });

  describe('sending', () => {
    it('sends the trimmed text with the button and clears the input', async () => {
      const user = userEvent.setup();
      renderPanel();

      await user.type(textarea(), '  hello  ');
      await user.click(sendButton());

      expect(onSend).toHaveBeenCalledTimes(1);
      expect(onSend).toHaveBeenCalledWith('hello');
      expect(textarea()).toHaveValue('');
    });

    it('sends with Enter', async () => {
      const user = userEvent.setup();
      renderPanel();

      await user.type(textarea(), 'hello{Enter}');

      expect(onSend).toHaveBeenCalledWith('hello');
      expect(textarea()).toHaveValue('');
    });

    it('does not send with Shift+Enter', async () => {
      const user = userEvent.setup();
      renderPanel();

      await user.type(textarea(), 'line one{Shift>}{Enter}{/Shift}line two');

      expect(onSend).not.toHaveBeenCalled();
      expect(textarea()).toHaveValue('line one\nline two');
    });

    it('does not send while an IME composition is in progress', () => {
      renderPanel();
      fireEvent.change(textarea(), { target: { value: 'xin chao' } });

      fireEvent.keyDown(textarea(), { key: 'Enter', isComposing: true });

      expect(onSend).not.toHaveBeenCalled();
      expect(textarea()).toHaveValue('xin chao');
    });

    it('does not send an empty message with Enter', () => {
      renderPanel();

      fireEvent.keyDown(textarea(), { key: 'Enter' });
      fireEvent.change(textarea(), { target: { value: '   ' } });
      fireEvent.keyDown(textarea(), { key: 'Enter' });

      expect(onSend).not.toHaveBeenCalled();
    });

    it('keeps the text when the message could not be sent', async () => {
      onSend.mockReturnValue(false);
      const user = userEvent.setup();
      renderPanel();

      await user.type(textarea(), 'hello');
      await user.click(sendButton());

      expect(onSend).toHaveBeenCalledWith('hello');
      expect(textarea()).toHaveValue('hello');
    });

    it('does not throw when no send handler is given', async () => {
      const user = userEvent.setup();
      renderPanel({ onSend: undefined });

      await user.type(textarea(), 'hello');
      await user.click(sendButton());

      expect(textarea()).toHaveValue('hello');
    });
  });

  describe('closing', () => {
    it('closes with the header button', async () => {
      const user = userEvent.setup();
      renderPanel();

      await user.click(screen.getByRole('button', { name: tr('speaking:chat.close') }));

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('closes on Escape', () => {
      renderPanel();

      fireEvent.keyDown(window, { key: 'Escape' });

      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('stops listening to Escape after it is closed', () => {
      const { rerender } = renderPanel();

      rerender(<ChatPanel open={false} messages={[]} selfId={1} onClose={onClose} />);
      fireEvent.keyDown(window, { key: 'Escape' });

      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe('scrolling', () => {
    const jumpButton = () =>
      screen.queryByRole('button', { name: new RegExp(tr('speaking:chat.newMessages'), 'i') });

    // Panel tự cuộn + focus sau 50ms khi mở; chờ xong để không ghi đè trạng thái cuộn của test
    const renderSettled = async (props) => {
      const view = renderPanel(props);
      await waitFor(() => expect(textarea()).toHaveFocus());
      return view;
    };

    it('does not offer to jump down while at the bottom', async () => {
      await renderSettled({ messages });

      setScrollPosition(log(), { scrollHeight: 1000, clientHeight: 200, scrollTop: 800 });
      fireEvent.scroll(log());

      expect(jumpButton()).not.toBeInTheDocument();
    });

    it('offers to jump down after scrolling up and jumps on click', async () => {
      const user = userEvent.setup();
      await renderSettled({ messages });
      setScrollPosition(log(), { scrollHeight: 1000, clientHeight: 200, scrollTop: 0 });

      fireEvent.scroll(log());
      expect(jumpButton()).toBeInTheDocument();

      HTMLElement.prototype.scrollTo.mockClear();
      await user.click(jumpButton());

      expect(HTMLElement.prototype.scrollTo).toHaveBeenCalledWith({ top: 1000, behavior: 'smooth' });
      expect(jumpButton()).not.toBeInTheDocument();
    });

    it('offers to jump down again when a new message arrives while still scrolled up', async () => {
      const user = userEvent.setup();
      const { rerender } = await renderSettled({ messages: [messages[0]] });
      setScrollPosition(log(), { scrollHeight: 1000, clientHeight: 200, scrollTop: 0 });
      fireEvent.scroll(log());
      await user.click(jumpButton());
      expect(jumpButton()).not.toBeInTheDocument();

      rerender(<ChatPanel open messages={messages} selfId={1} onSend={onSend} onClose={onClose} />);

      expect(jumpButton()).toBeInTheDocument();
    });

    it('scrolls smoothly to the bottom after sending a message', async () => {
      const user = userEvent.setup();
      await renderSettled();
      await user.type(textarea(), 'hello');
      HTMLElement.prototype.scrollTo.mockClear();

      await user.click(sendButton());

      expect(HTMLElement.prototype.scrollTo).toHaveBeenCalledWith(
        expect.objectContaining({ behavior: 'smooth' })
      );
    });
  });
});
