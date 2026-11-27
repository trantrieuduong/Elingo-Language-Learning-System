/**
 * utils.js — Utility functions dùng chung toàn project.
 * Chỉ chứa pure functions, không import React hay apiClient.
 */

/**
 * Format ngày thành chuỗi tiếng Việt dễ đọc.
 * @param {string|Date} dateInput
 * @param {Object} options - Intl.DateTimeFormat options
 * @returns {string}
 */
export const formatDate = (dateInput, options = {}) => {
  if (!dateInput) return '';
  const date = new Date(dateInput);
  if (Number.isNaN(date.getTime())) return '';

  const normalizedOptions = typeof options === 'string' ? { locale: options } : options;
  const { locale = 'vi-VN', ...fmtOptions } = normalizedOptions;

  const defaults = {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  };

  return new Intl.DateTimeFormat(locale, { ...defaults, ...fmtOptions }).format(date);
};

/**
 * Format ngày giờ thành chuỗi theo locale (mặc định gồm cả giờ và phút).
 * @param {string|Date} dateInput
 * @param {Object|string} options - Intl.DateTimeFormat options
 * @returns {string}
 */
export const formatDateTime = (dateInput, options = {}) => {
  if (!dateInput) return '';
  const normalizedOptions = typeof options === 'string' ? { locale: options } : options;
  const { hour = '2-digit', minute = '2-digit', ...rest } = normalizedOptions;

  return formatDate(dateInput, {
    hour,
    minute,
    ...rest,
  });
};

/**
 * Format thời gian tương đối theo locale.
 * @param {string|Date} dateInput
 * @param {string} locale
 * @returns {string}
 */
export const formatRelativeTime = (dateInput, locale = 'vi') => {
  if (!dateInput) return '';
  const date = new Date(dateInput);
  if (Number.isNaN(date.getTime())) return '';

  const diffMs = Date.now() - date.getTime();
  if (diffMs < 0) return '';

  const diffSec = Math.floor(diffMs / 1000);
  const relativeTimeFormat = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });

  if (diffSec < 60) return relativeTimeFormat.format(0, 'second');
  if (diffSec < 60 * 60) {
    return relativeTimeFormat.format(-Math.floor(diffSec / 60), 'minute');
  }
  if (diffSec < 60 * 60 * 24) {
    return relativeTimeFormat.format(-Math.floor(diffSec / (60 * 60)), 'hour');
  }

  // Hơn 1 ngày: hiển thị ngày cụ thể
  const now = new Date();
  const isThisYear = date.getFullYear() === now.getFullYear();
  return new Intl.DateTimeFormat(locale, {
    day: 'numeric',
    month: 'short',
    ...(isThisYear ? {} : { year: 'numeric' }),
  }).format(date);
};

/**
 * Truncate chuỗi nếu quá dài.
 * @param {string} str
 * @param {number} maxLength
 * @returns {string}
 */
export const truncate = (str, maxLength = 100) => {
  if (!str) return '';
  if (str.length <= maxLength) return str;
  return str.slice(0, maxLength).trimEnd() + '…';
};

/**
 * Validate email cơ bản.
 * @param {string} email
 * @returns {boolean}
 */
export const isValidEmail = (email) => {
  return /^[^\s@]+@[^.\s@]+(?:\.[^.\s@]+)+$/.test(email);
};

/**
 * Tạo URL avatar fallback từ tên hiển thị.
 * @param {string} displayName
 * @returns {string}
 */
export const getFallbackAvatarUrl = (displayName = '') =>
  `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}`;

/**
 * Tách thời hạn tính theo ngày thành đơn vị dễ đọc.
 * @param {number} durationDays
 * @returns {{ unit: 'lifetime' | 'year' | 'month' | 'day', count: number }}
 */
export const getDurationParts = (durationDays) => {
  if (durationDays === 0) {
    return { unit: 'lifetime', count: 1 };
  }

  if (!Number.isFinite(durationDays) || durationDays <= 0) {
    return { unit: 'day', count: 0 };
  }

  if (durationDays % 365 === 0) {
    return { unit: 'year', count: durationDays / 365 };
  }

  if (durationDays % 30 === 0) {
    return { unit: 'month', count: durationDays / 30 };
  }

  return { unit: 'day', count: durationDays };
};

/**
 * Format giá theo locale và mã tiền tệ.
 * @param {number|string} price
 * @param {string} currency
 * @param {string} locale
 * @returns {string}
 */
export const formatPremiumPrice = (price, currency, locale) => {
  const numericPrice =
    price === '' || price === null || price === undefined ? Number.NaN : Number(price);
  const formattedPrice = Number.isFinite(numericPrice)
    ? new Intl.NumberFormat(locale, { maximumFractionDigits: 2 }).format(numericPrice)
    : price;

  return currency ? `${formattedPrice} ${currency}` : formattedPrice;
};
