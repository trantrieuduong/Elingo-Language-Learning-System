import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EmptyState from '../../../components/EmptyState/EmptyState';
import PageLoading from '../../../components/PageLoading/PageLoading';
import OptionsMenu from '../../../components/OptionsMenu/OptionsMenu';
import OptionsModal from '../../../components/OptionsModal/OptionsModal';
import Toast from '../../../components/Toast/Toast';
import ProfilePosts from '../components/ProfilePosts/ProfilePosts';
import ProfileGamificationOverview from '../components/ProfileGamificationOverview/ProfileGamificationOverview';
import UpdateProfileModal from '../components/UpdateProfileModal/UpdateProfileModal';
import UpdateEmailModal from '../components/UpdateEmailModal/UpdateEmailModal';
import SetPasswordModal from '../components/SetPasswordModal/SetPasswordModal';
import { getMyProfileApi } from '../profileApi';
import { useAuth } from '../../../context/AuthContext';
import './MyProfilePage.css';

function MyProfilePage({ onNavigate }) {
  const { t } = useTranslation(['user', 'common']);
  const { updateUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isOptionOpen, setIsOptionOpen] = useState(false);
  const [isUpdateProfileOpen, setIsUpdateProfileOpen] = useState(false);
  const [isUpdateEmailOpen, setIsUpdateEmailOpen] = useState(false);
  const [isSetPasswordOpen, setIsSetPasswordOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

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
    setIsUpdateProfileOpen(true);
  };

  const handleOpenUpdateEmail = () => {
    setIsUpdateEmailOpen(true);
  };

  const handleOpenSetPassword = () => {
    setIsSetPasswordOpen(true);
  };

  const handleUpdateProfileSuccess = (updatedData) => {
    setProfile(updatedData);
    updateUser?.(updatedData);
    setToastMessage(t('user:updateProfile.updateSuccess'));
  };

  const handleOtpSentSuccess = () => {
    setToastMessage(t('user:updateEmailModal.sendOtpSuccess'));
  };

  const handleUpdateEmailSuccess = (newEmail) => {
    setProfile((prev) => (prev ? { ...prev, email: newEmail } : prev));
    updateUser?.({ email: newEmail });
    setToastMessage(t('user:updateEmailModal.updateEmailSuccess'));
  };

  const handleSetPasswordSuccess = () => {
    setProfile((prev) => (prev ? { ...prev, hasPassword: true } : prev));
    updateUser?.({ hasPassword: true });
    setToastMessage(
      t(
        profile.hasPassword
          ? 'user:setPasswordModal.changePasswordSuccess'
          : 'user:setPasswordModal.setPasswordSuccess'
      )
    );
  };

  const options = [
    {
      id: 'edit-profile',
      label: t('user:editProfile'),
      icon: 'edit',
      onSelect: handleEditProfile,
    },
    {
      id: 'update-email',
      label: t('user:updateEmail'),
      icon: 'mail',
      onSelect: handleOpenUpdateEmail,
    },
    ...(profile?.hasPassword
      ? [
          {
            id: 'change-password',
            label: t('user:changePassword'),
            icon: 'lock_reset',
            onSelect: handleOpenSetPassword,
          },
        ]
      : [
          {
            id: 'set-password',
            label: t('user:setPassword'),
            icon: 'lock',
            onSelect: handleOpenSetPassword,
          },
        ]),
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
      <ProfileGamificationOverview
        profile={profile}
        actionMenu={
          <OptionsMenu
            onClick={() => setIsOptionOpen(true)}
            ariaLabel={t('user:options')}
          />
        }
      />
      <ProfilePosts userId={profile.id} onNavigate={onNavigate} />

      <OptionsModal
        isOpen={isOptionOpen}
        title={t('user:options')}
        options={options}
        onClose={() => setIsOptionOpen(false)}
      />

      <UpdateProfileModal
        isOpen={isUpdateProfileOpen}
        profile={profile}
        onClose={() => setIsUpdateProfileOpen(false)}
        onSuccess={handleUpdateProfileSuccess}
      />

      <UpdateEmailModal
        isOpen={isUpdateEmailOpen}
        onClose={() => setIsUpdateEmailOpen(false)}
        onOtpSent={handleOtpSentSuccess}
        onSuccess={handleUpdateEmailSuccess}
      />

      <SetPasswordModal
        isOpen={isSetPasswordOpen}
        hasPassword={profile.hasPassword}
        onClose={() => setIsSetPasswordOpen(false)}
        onSuccess={handleSetPasswordSuccess}
      />

      <Toast message={toastMessage} variant="success" onDismiss={() => setToastMessage('')} />
    </main>
  );
}

export default MyProfilePage;
