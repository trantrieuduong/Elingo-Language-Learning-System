import apiClient from './apiClient';

/**
 * Upload file lên Cloud Storage (Cloudflare R2 / S3 qua presigned URL) theo quy trình 3 bước chuẩn của Elingo:
 * 1. POST /files/presigned-urls -> lấy { url, key }
 * 2. PUT url (fetch thuần, KHÔNG kèm header Authorization hay cookies của apiClient) -> đẩy binary Blob lên bucket
 * 3. POST /files/verifications -> xác thực fileKey hợp lệ và nhận verifiedFileKey
 *
 * @param {Blob} blob - Binary file cần upload (ví dụ: audio/wav)
 * @param {string} fileType - MIME type của file (ví dụ: 'audio/wav', 'image/jpeg')
 * @returns {Promise<string>} verifiedFileKey dùng để gửi vào các nghiệp vụ của BE (như audioUrl)
 */
export async function uploadFileForVerification(blob, fileType) {
  if (!blob || !(blob instanceof Blob)) {
    throw new Error('Dữ liệu upload không hợp lệ (cần Blob)');
  }

  // Bước 1: Xin presigned URL từ backend
  const presignedRes = await apiClient.post('/files/presigned-urls', {
    fileType,
    fileSize: blob.size,
  });

  const { url, key } = presignedRes.data?.data || {};
  if (!url || !key) {
    throw new Error('Không nhận được presigned URL hợp lệ từ máy chủ');
  }

  // Bước 2: Upload trực tiếp file lên storage bucket bằng fetch thuần
  const uploadRes = await fetch(url, {
    method: 'PUT',
    headers: {
      'Content-Type': fileType,
    },
    body: blob,
  });

  if (!uploadRes.ok) {
    const errorText = await uploadRes.text().catch(() => '');
    throw new Error(
      `Tải tệp lên bộ lưu trữ thất bại (HTTP ${uploadRes.status}): ${errorText || uploadRes.statusText}`
    );
  }

  // Bước 3: Xác thực file đã upload thành công với backend
  const verifyRes = await apiClient.post('/files/verifications', {
    fileKey: key,
    fileType,
  });

  const verifiedFileKey = verifyRes.data?.data?.verifiedFileKey || key;
  return verifiedFileKey;
}
