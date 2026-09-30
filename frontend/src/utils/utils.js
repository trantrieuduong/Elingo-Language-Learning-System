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
  if (!dateInput) return ''
  const date = new Date(dateInput)
  if (isNaN(date.getTime())) return ''

  const defaults = {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }

  return new Intl.DateTimeFormat('vi-VN', { ...defaults, ...options }).format(date)
}

/**
 * Format thời gian tương đối (vd: "3 phút trước").
 * @param {string|Date} dateInput
 * @returns {string}
 */
export const formatRelativeTime = (dateInput) => {
  if (!dateInput) return ''
  const date = new Date(dateInput)
  const now = new Date()
  const diffMs = now - date
  const diffSec = Math.floor(diffMs / 1000)
  const diffMin = Math.floor(diffSec / 60)
  const diffHour = Math.floor(diffMin / 60)
  const diffDay = Math.floor(diffHour / 24)

  if (diffSec < 60) return 'Vừa xong'
  if (diffMin < 60) return `${diffMin} phút trước`
  if (diffHour < 24) return `${diffHour} giờ trước`
  if (diffDay < 7) return `${diffDay} ngày trước`
  return formatDate(date)
}

/**
 * Truncate chuỗi nếu quá dài.
 * @param {string} str
 * @param {number} maxLength
 * @returns {string}
 */
export const truncate = (str, maxLength = 100) => {
  if (!str) return ''
  if (str.length <= maxLength) return str
  return str.slice(0, maxLength).trimEnd() + '…'
}

/**
 * Validate email cơ bản.
 * @param {string} email
 * @returns {boolean}
 */
export const isValidEmail = (email) => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

/**
 * Validate độ mạnh password.
 * @param {string} password
 * @returns {{ valid: boolean, message: string }}
 */
export const validatePassword = (password) => {
  if (!password) return { valid: false, message: 'Mật khẩu không được để trống' }
  if (password.length < 8) return { valid: false, message: 'Mật khẩu phải có ít nhất 8 ký tự' }
  if (!/[A-Z]/.test(password)) return { valid: false, message: 'Mật khẩu phải có ít nhất 1 chữ hoa' }
  if (!/[0-9]/.test(password)) return { valid: false, message: 'Mật khẩu phải có ít nhất 1 số' }
  return { valid: true, message: '' }
}

/**
 * Debounce function — dùng trong useEffect, không phải hook.
 * @param {Function} fn
 * @param {number} delay - ms
 * @returns {Function}
 */
export const debounce = (fn, delay = 500) => {
  let timer
  return (...args) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

/**
 * Build query string từ object params (bỏ qua null/undefined/empty).
 * @param {Object} params
 * @returns {string} - vd: '?page=1&limit=8&q=hello'
 */
export const buildQueryString = (params = {}) => {
  const filtered = Object.entries(params).filter(
    ([, val]) => val !== null && val !== undefined && val !== ''
  )
  if (filtered.length === 0) return ''
  return '?' + new URLSearchParams(Object.fromEntries(filtered)).toString()
}

/**
 * Lấy initials từ tên đầy đủ (dùng cho avatar fallback).
 * @param {string} fullName
 * @returns {string} - vd: 'NT' từ 'Nguyen Tuan'
 */
export const getInitials = (fullName) => {
  if (!fullName) return '?'
  const parts = fullName.trim().split(' ').filter(Boolean)
  if (parts.length === 1) return parts[0][0].toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

/**
 * Format số thành chuỗi dễ đọc (vd: 1500 → '1.5K').
 * @param {number} num
 * @returns {string}
 */
export const formatCount = (num) => {
  if (!num && num !== 0) return '0'
  if (num >= 1_000_000) return `${(num / 1_000_000).toFixed(1)}M`
  if (num >= 1_000) return `${(num / 1_000).toFixed(1)}K`
  return String(num)
}
