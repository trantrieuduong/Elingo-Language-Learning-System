import { useState } from 'react'
import './Input.css'

/**
 * Input component dùng chung toàn project.
 * Hỗ trợ show/hide password, error state, label, right element.
 *
 * @param {string} id - ID duy nhất của input (bắt buộc để a11y)
 * @param {string} label - Text label phía trên input
 * @param {string} type - Input type (default: 'text')
 * @param {string} placeholder
 * @param {string|number} value
 * @param {Function} onChange
 * @param {string} error - Error message (hiện dưới input nếu có)
 * @param {React.ReactNode} rightElement - Element tùy chỉnh bên phải input
 * @param {boolean} autoFocus
 * @param {boolean} disabled
 * @param {string} className - Extra class cho wrapper
 */
function Input({
  id,
  label,
  type = 'text',
  placeholder,
  value,
  onChange,
  error,
  rightElement,
  autoFocus = false,
  disabled = false,
  className = '',
  ...props
}) {
  const [showPassword, setShowPassword] = useState(false)

  const isPassword = type === 'password'
  const inputType = isPassword ? (showPassword ? 'text' : 'password') : type

  return (
    <div className={`input-wrapper ${error ? 'input-wrapper--error' : ''} ${className}`}>
      {label && (
        <label htmlFor={id} className="input-label">
          {label}
        </label>
      )}

      <div className="input-field-row">
        <input
          id={id}
          type={inputType}
          placeholder={placeholder}
          value={value}
          onChange={onChange}
          autoFocus={autoFocus}
          disabled={disabled}
          className={`input-field ${error ? 'input-field--error' : ''}`}
          aria-invalid={!!error}
          aria-describedby={error ? `${id}-error` : undefined}
          {...props}
        />

        {/* Toggle show/hide password */}
        {isPassword && (
          <button
            type="button"
            className="input-toggle-password"
            onClick={() => setShowPassword((prev) => !prev)}
            aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            tabIndex={-1}
          >
            <span className="material-symbols-outlined">
              {showPassword ? 'visibility_off' : 'visibility'}
            </span>
          </button>
        )}

        {/* Custom right element (vd: icon tìm kiếm) */}
        {!isPassword && rightElement && (
          <div className="input-right-element">{rightElement}</div>
        )}
      </div>

      {error && (
        <p id={`${id}-error`} className="input-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}

export default Input
