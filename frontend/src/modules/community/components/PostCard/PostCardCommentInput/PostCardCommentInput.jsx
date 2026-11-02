import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import './PostCardCommentInput.css';

function PostCardCommentInput({ avatarUrl, userName = 'User', onSubmit }) {
  const { t } = useTranslation();
  const [comment, setComment] = useState('');

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!comment.trim()) return;
    if (onSubmit) onSubmit(comment.trim());
    setComment('');
  };

  return (
    <form onSubmit={handleSubmit} className="post-card-comment-input">
      <img
        src={avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(userName)}`}
        alt={userName}
        className="post-card-comment-input__avatar"
      />

      <div className="post-card-comment-input__field glass-well">
        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          rows={1}
          autoFocus
          placeholder={t('community:postCard.writeComment')}
          className="post-card-comment-input__textarea"
        />

        <button
          type="submit"
          disabled={!comment.trim()}
          className="btn-primary post-card-comment-input__submit-btn"
        >
          <span className="material-symbols-outlined">send</span>
        </button>
      </div>
    </form>
  );
}

export default PostCardCommentInput;
