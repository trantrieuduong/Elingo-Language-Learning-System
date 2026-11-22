import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EmptyState from '../../../components/EmptyState/EmptyState';
import PageLoading from '../../../components/PageLoading/PageLoading';
import OptionsMenu from '../../../components/OptionsMenu/OptionsMenu';
import OptionsModal from '../../../components/OptionsModal/OptionsModal';
import ProfileCard from '../components/ProfileCard/ProfileCard';
import { getMyProfileApi } from '../profileApi';
import './MyProfilePage.css';

function MyProfilePage({ onNavigate }) {
  const { t } = useTranslation(['user', 'common']);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isOptionOpen, setIsOptionOpen] = useState(false);

  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, []);

  const fetchProfile = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getMyProfileApi();
      if (res?.success && res?.data) {
        setProfile(res.data);
      } else {
        const errorMsg = res?.errors?.[0]?.message || res?.message || t('common:loadError');
        setError(errorMsg);
      }
    } catch (err) {
      const errorMsg =
        err?.response?.data?.errors?.[0]?.message ||
        err?.response?.data?.message ||
        t('common:loadError');
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const handleEditProfile = () => {
    // TO DO: tương lai mở modal chỉnh sửa hồ sơ
    if (onNavigate) {
      onNavigate('/profile/edit');
    }
  };

  const options = [
    {
      id: 'edit-profile',
      label: t('user:editProfile'),
      icon: 'edit',
      onSelect: handleEditProfile,
    },
  ];

  if (loading) {
    return <PageLoading />;
  }

  if (error || !profile) {
    return (
      <main className="my-profile-page container">
        <EmptyState
          icon="error"
          description={error || t('common:loadError')}
          isError={true}
          actionLabel={t('common:retry')}
          onAction={fetchProfile}
          actionVariant="primary"
        />
      </main>
    );
  }

  return (
    <main className="my-profile-page container">
      <ProfileCard
        profile={profile}
        actionMenu={
          <OptionsMenu
            onClick={() => setIsOptionOpen(true)}
          />
        }
      />

      <OptionsModal
        isOpen={isOptionOpen}
        title={t('user:options')}
        options={options}
        onClose={() => setIsOptionOpen(false)}
      />
    </main>
  );
}

export default MyProfilePage;
