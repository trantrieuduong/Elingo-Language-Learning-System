import { describe, it, expect, vi } from 'vitest';
import { mapUserActionError } from '@/modules/admin/utils/userActionError';

describe('mapUserActionError', () => {
  const t = vi.fn((key) => key);

  it.each([
    ['CANNOT_MODIFY_SELF', 'users.errors.cannotModifySelf'],
    ['CANNOT_MODIFY_ADMIN', 'users.errors.cannotModifyAdmin'],
    ['USER_ALREADY_ACTIVE', 'users.errors.userAlreadyActive'],
    ['USER_INACTIVE', 'users.errors.userInactive'],
    ['USER_NOT_FOUND', 'users.errors.userNotFound'],
  ])('maps error code %s to %s', (code, expectedKey) => {
    const err = { response: { data: { error: { code } } } };

    expect(mapUserActionError(err, t, 'users.lockModal.error')).toBe(expectedKey);
  });

  it('returns fallbackKey when error code is unknown or missing', () => {
    expect(mapUserActionError({}, t, 'users.unlockModal.error')).toBe('users.unlockModal.error');
    expect(
      mapUserActionError({ response: { data: { error: { code: 'OTHER' } } } }, t, 'fallback.key'),
    ).toBe('fallback.key');
  });
});
