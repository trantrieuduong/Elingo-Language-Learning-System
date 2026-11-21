import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import './ChatPanel.css';

const MAX_CHAT_LENGTH = 500;

const formatTime = (timestamp, lng) => {
  if (!timestamp) return '';
  try {
    return new Date(timestamp).toLocaleTimeString(lng || 'vi', {
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return '';
  }
};

const ChatPanel = ({
  open = false,
  messages = [],
  selfId,
  onSend,
  onClose,
}) => {
  const { t, i18n } = useTranslation('speaking');
  const [inputText, setInputText] = useState('');
  const [showScrollBottom, setShowScrollBottom] = useState(false);

  const messagesEndRef = useRef(null);
  const logContainerRef = useRef(null);
  const textareaRef = useRef(null);
  const isAtBottomRef = useRef(true);

  // Kiểm tra vị trí cuộn
  const handleScroll = () => {
    const el = logContainerRef.current;
    if (!el) return;
    const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    const atBottom = distanceToBottom < 80;
    isAtBottomRef.current = atBottom;
    setShowScrollBottom(!atBottom);
  };

  const scrollToBottom = (smooth = true) => {
    const el = logContainerRef.current;
    if (el) {
      el.scrollTo({
        top: el.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto',
      });
    }
    setShowScrollBottom(false);
  };

  // Tự cuộn log tin nhắn xuống đáy khi có tin nhắn mới nếu đang ở đáy
  useEffect(() => {
    if (!open) return;
    if (isAtBottomRef.current) {
      scrollToBottom(false);
    } else {
      setShowScrollBottom(true);
    }
  }, [messages, open]);

  // Focus ô nhập khi mở khung chat (không cuộn trang toàn cục)
  useEffect(() => {
    if (open) {
      setTimeout(() => {
        scrollToBottom(false);
        textareaRef.current?.focus({ preventScroll: true });
      }, 50);
    }
  }, [open]);

  // Esc đóng panel khi focus trong khung
  useEffect(() => {
    if (!open) return undefined;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose?.();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  // Điều chỉnh chiều cao textarea tự co giãn
  const handleInputChange = (e) => {
    const val = e.target.value.slice(0, MAX_CHAT_LENGTH);
    setInputText(val);
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 100)}px`;
    }
  };

  const handleSend = () => {
    const trimmed = inputText.trim();
    if (!trimmed || !onSend) return;
    const sent = onSend(trimmed);
    if (sent) {
      setInputText('');
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
        textareaRef.current.focus();
      }
      isAtBottomRef.current = true;
      scrollToBottom(true);
    }
  };

  const handleKeyDown = (e) => {
    // Không gửi khi đang gõ tiếng Việt
    if (e.nativeEvent?.isComposing) return;

    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  if (!open) return null;

  const trimmedText = inputText.trim();
  const isSendDisabled = !trimmedText;
  const showCounter = inputText.length >= 400;

  return (
    <aside
      className="speaking-chat-panel glass-capsule"
      role="complementary"
      aria-label={t('chat.title')}
    >
      <header className="speaking-chat-header">
        <div className="speaking-chat-header__title-group">
          <span className="material-symbols-outlined speaking-chat-header__icon">chat</span>
          <h2 className="text-title-md speaking-chat-header__title">{t('chat.title')}</h2>
        </div>
        <button
          type="button"
          className="btn-icon speaking-chat-header__close-btn"
          onClick={onClose}
          aria-label={t('chat.close')}
          title={t('chat.close')}
        >
          <span className="material-symbols-outlined">close</span>
        </button>
      </header>

      {/* Vùng hiển thị tin nhắn */}
      <div
        className="speaking-chat-messages"
        ref={logContainerRef}
        onScroll={handleScroll}
        role="log"
        aria-live="polite"
      >
        {messages.length === 0 ? (
          <div className="speaking-chat-empty">
            <span className="material-symbols-outlined speaking-chat-empty__icon">
              chat_bubble_outline
            </span>
            <p className="text-body-sm text-disabled speaking-chat-empty__text">
              {t('chat.empty')}
            </p>
          </div>
        ) : (
          messages.map((msg) => {
            const isSelf = String(msg.fromUserId) === String(selfId);
            const timeStr = formatTime(msg.timestamp, i18n.language);
            return (
              <div
                key={msg.id}
                className={`speaking-chat-msg ${
                  isSelf ? 'speaking-chat-msg--self' : 'speaking-chat-msg--peer'
                }`}
              >
                {!isSelf && (
                  <span className="speaking-chat-msg__name text-body-xs" title={msg.displayName}>
                    {msg.displayName}
                  </span>
                )}
                <div className="speaking-chat-msg__bubble glass-card">
                  <p className="speaking-chat-msg__text">{msg.text}</p>
                  {timeStr && (
                    <span className="speaking-chat-msg__time text-body-xs">{timeStr}</span>
                  )}
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Nút nhảy xuống tin mới */}
      {showScrollBottom && (
        <button
          type="button"
          className="speaking-chat-jump-btn btn-secondary glass-pill"
          onClick={() => scrollToBottom(true)}
        >
          <span className="material-symbols-outlined">arrow_downward</span>
          <span>{t('chat.newMessages')}</span>
        </button>
      )}

      {/* Form nhập tin nhắn */}
      <footer className="speaking-chat-footer">
        {showCounter && (
          <div className="speaking-chat-counter text-body-xs text-disabled">
            {t('chat.counter', { count: inputText.length })}
          </div>
        )}
        <div className="speaking-chat-input-row">
          <textarea
            ref={textareaRef}
            className="speaking-chat-textarea"
            rows={1}
            maxLength={MAX_CHAT_LENGTH}
            placeholder={t('chat.placeholder')}
            value={inputText}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
          />
          <button
            type="button"
            className="speaking-chat-send-btn btn-primary"
            onClick={handleSend}
            disabled={isSendDisabled}
            aria-label={t('chat.send')}
            title={t('chat.send')}
          >
            <span className="material-symbols-outlined">send</span>
          </button>
        </div>
      </footer>
    </aside>
  );
};

export default ChatPanel;
