import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import './PostGroupPage.css';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import { getPostGroupsApi } from '../../communityApi';

function PostGroupPage({ onNavigate }) {
  const { t } = useTranslation();
  const [groups, setGroups] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchGroups = () => {
    setIsLoading(true);
    setError(null);
    getPostGroupsApi()
      .then((res) => setGroups(res?.data || []))
      .catch(() => {
        setError(t('common:loadError'));
      })
      .finally(() => setIsLoading(false));
  };

  useEffect(() => {
    fetchGroups();
  }, []);

  const handleSelectGroup = (groupId) => {
    if (onNavigate) {
      onNavigate('/community', { selectedGroupId: groupId });
    }
  };

  return (
    <div className="post-group-page">
      {error ? (
        <EmptyState
          icon="error"
          description={t('common:loadError')}
          isError={true}
          actionLabel={t('common:retry')}
          onAction={fetchGroups}
        />
      ) : isLoading ? (
        <div className="post-group-page__loading">
          <div className="app-loading-spinner" />
        </div>
      ) : groups.length === 0 ? (
        <EmptyState
          icon="diversity_3"
          title={t('community:postGroups.emptyTitle')}
          description={t('community:postGroups.emptyDescription')}
        />
      ) : (
        <div className="post-group-page__grid">
          {groups.map((group) => (
            <article key={group.id} className="post-group-card glass-card">
              <h2 className="post-group-card__title text-headline-sm">{group.groupName}</h2>

              <p className="post-group-card__description text-body-md">{group.description}</p>

              <div className="post-group-card__actions">
                <button
                  type="button"
                  className="btn-secondary post-group-card__btn"
                  onClick={() => handleSelectGroup(group.id)}
                >
                  <span>{t('community:postGroups.viewPosts')}</span>
                  <span className="material-symbols-outlined">arrow_forward</span>
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

export default PostGroupPage;
