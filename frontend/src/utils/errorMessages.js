/**
 * Dictionary ánh xạ error code từ backend thành thông báo tiếng Việt.
 */
export const ERROR_MESSAGES = {
  // Auth & User errors
  INVALID_CREDENTIALS: 'Email/Username hoặc mật khẩu không chính xác.',
  ACCOUNT_NOT_VERIFIED: 'Tài khoản chưa được xác thực email.',
  USER_NOT_VERIFIED: 'Email chưa được xác thực.',
  EMAIL_NOT_EXISTED: 'Email không tồn tại.',
  USER_NOT_FOUND: 'Tài khoản hoặc thông tin không tồn tại.',
  USERNAME_EXISTED: 'Username đã tồn tại.',
  EMAIL_EXISTED: 'Email đã tồn tại.',
  OTP_INVALID: 'Mã OTP không chính xác hoặc đã hết hạn.',
  TOO_MANY_REQUESTS: 'Quá nhiều yêu cầu, vui lòng thử lại sau.',
  EMAIL_SEND_FAILED: 'Gửi email thất bại. Vui lòng thử lại sau.',
  UNAUTHENTICATED: 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.',
  UNAUTHORIZED: 'Bạn không có quyền thực hiện thao tác này.',
  USER_INACTIVE: 'Tài khoản hiện đang bị vô hiệu hóa.',
  USER_BANNED: 'Tài khoản đã bị khóa.',
  REFRESH_TOKEN_INVALID: 'Phiên đăng nhập hết hạn, vui lòng đăng nhập lại.',
  PASSWORD_INCORRECT: 'Mật khẩu không chính xác.',
  OLD_PASSWORD_INCORRECT: 'Mật khẩu cũ không chính xác.',
  NEW_PASSWORD_SAME_AS_OLD: 'Mật khẩu mới không được trùng với mật khẩu cũ.',
  GOOGLE_TOKEN_INVALID: 'Thông tin xác thực Google không hợp lệ hoặc đã hết hạn.',
  GOOGLE_TOKEN_VERIFICATION_FAILED: 'Không thể xác thực tài khoản Google. Vui lòng thử lại.',

  // Input validation errors
  USERNAME_INVALID: 'Username gồm 3-15 chữ cái hoặc chữ số, không có khoảng trắng hay ký tự đặc biệt.',
  PASSWORD_INVALID: 'Mật khẩu phải có ít nhất 8 ký tự và bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.',
  EMAIL_INVALID: 'Email không đúng định dạng.',
  IDENTIFIER_INVALID: 'Email hoặc Username không được để trống.',
  FULL_NAME_INVALID: 'Họ và tên không được để trống.',

  // System errors
  INTERNAL_SERVER_ERROR: 'Lỗi hệ thống. Vui lòng thử lại sau.',
  UNCATEGORIZED_EXCEPTION: 'Lỗi không xác định. Vui lòng thử lại.',
}

/**
 * Helper dịch lỗi từ response Axios (hoặc error object).
 */
export const translateError = (error, fallbackMessage = 'Đã có lỗi xảy ra. Vui lòng thử lại.') => {
  const responseData = error?.response?.data
  const errorDetail = responseData?.errors?.[0]
  const code = errorDetail?.code || responseData?.code

  let message = fallbackMessage

  if (code && ERROR_MESSAGES[code]) {
    message = ERROR_MESSAGES[code]
  } else if (errorDetail?.message) {
    message = errorDetail.message
  } else if (responseData?.message) {
    message = responseData.message
  } else if (error?.message) {
    message = error.message
  }

  const notVerified = code === 'ACCOUNT_NOT_VERIFIED' || code === 'USER_NOT_VERIFIED'
  const errors = responseData?.errors || null

  return {
    success: false,
    message,
    code,
    errors,
    notVerified,
  }
}
