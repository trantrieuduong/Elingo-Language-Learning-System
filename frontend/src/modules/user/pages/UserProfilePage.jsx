import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EmptyState from '../../../components/EmptyState/EmptyState';
import PageLoading from '../../../components/PageLoading/PageLoading';
import OptionsMenu from '../../../components/OptionsMenu/OptionsMenu';
import OptionsModal from '../../../components/OptionsModal/OptionsModal';
import ReportModal from '../../report/components/ReportModal/ReportModal';
import { useReportModal } from '../../report/hooks/useReportModal';
import ProfilePosts from '../components/ProfilePosts/ProfilePosts';
import ProfileGamificationOverview from '../components/ProfileGamificationOverview/ProfileGamificationOverview';
import { getUserByIdApi } from '../profileApi';
import './UserProfilePage.css';

function UserProfilePage({ userId, onNavigate }) {
  const { t } = useTranslation(['user', 'common']);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isNotFound, setIsNotFound] = useState(false);
  const [isOptionOpen, setIsOptionOpen] = useState(false);
  const { isReportOpen, targetType, targetId, targetLabel, openReport, closeReport } =
    useReportModal();

  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, []);

  const fetchUserProfile = async () => {
    if (!userId) {
      setIsNotFound(true);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    setIsNotFound(false);

    try {
      const res = await getUserByIdApi(userId);
      if (res?.success && res?.data) {
        setProfile(res.data);
      } else {
        const firstError = res?.errors?.[0];
        if (firstError?.code === 'USER_NOT_FOUND') {
          setIsNotFound(true);
        } else {
          setError(firstError?.message || res?.message || t('common:loadError'));
        }
      }
    } catch (err) {
      const firstError = err?.response?.data?.errors?.[0];
      if (firstError?.code === 'USER_NOT_FOUND') {
        setIsNotFound(true);
      } else {
        setError(firstError?.message || err?.response?.data?.message || t('common:loadError'));
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUserProfile();
  }, [userId]);

  const options = [
    {
      id: 'report-user',
      label: t('user:report'),
      icon: 'flag',
      onSelect: () =>
        openReport({ targetType: 'USER', targetId: profile?.id, targetLabel: profile?.username }),
    },
  ];

  if (loading) {
    return <PageLoading />;
  }

  if (isNotFound) {
    return (
      <main className="user-profile-page container">
        <EmptyState
          icon="person_off"
          title={t('user:userNotFoundTitle')}
          description={t('user:userNotFoundDesc')}
          actionLabel={t('common:backHome')}
          onAction={() => onNavigate('/')}
          actionVariant="primary"
        />
      </main>
    );
  }

  if (error || !profile) {
    return (
      <main className="user-profile-page container">
        <EmptyState
          icon="error"
          description={error || t('common:loadError')}
          isError={true}
          actionLabel={t('common:retry')}
          onAction={fetchUserProfile}
          actionVariant="primary"
        />
      </main>
    );
  }

  return (
    <main className="user-profile-page container">
      <ProfileGamificationOverview
        profile={profile}
        actionMenu={
          <OptionsMenu
            onClick={() => setIsOptionOpen(true)}
            ariaLabel={t('user:options')}
          />
        }
      />
      <ProfilePosts userId={userId} onNavigate={onNavigate} />

      <OptionsModal
        isOpen={isOptionOpen}
        title={t('user:options')}
        options={options}
        onClose={() => setIsOptionOpen(false)}
      />

      <ReportModal
        isOpen={isReportOpen}
        targetType={targetType}
        targetId={targetId}
        targetLabel={targetLabel}
        onClose={closeReport}
      />
    </main>
  );
}

export default UserProfilePage;
