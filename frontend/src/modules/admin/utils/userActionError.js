/**
 * Chuẩn hóa thông báo lỗi khi khóa/mở khóa tài khoản người dùng.
 * @param {Error} err
 * @param {Function} t - Hàm dịch i18next
 * @param {string} fallbackKey - Key i18n mặc định khi không khớp mã lỗi
 * @returns {string}
 */
export function mapUserActionError(err, t, fallbackKey) {
  const errCode = err?.response?.data?.error?.code;
  const codeToKey = {
    CANNOT_MODIFY_SELF: 'users.errors.cannotModifySelf',
    CANNOT_MODIFY_ADMIN: 'users.errors.cannotModifyAdmin',
    USER_ALREADY_ACTIVE: 'users.errors.userAlreadyActive',
    USER_INACTIVE: 'users.errors.userInactive',
    USER_NOT_FOUND: 'users.errors.userNotFound',
  };
  return t(codeToKey[errCode] || fallbackKey);
}
