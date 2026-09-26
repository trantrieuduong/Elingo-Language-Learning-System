package com.elingo.file.service;

import com.elingo.file.dto.request.CompleteMultipartRequest;
import com.elingo.file.dto.request.PresignedUrlRequest;
import com.elingo.file.dto.response.CompleteMultipartResponse;
import com.elingo.file.dto.response.InitiateMultipartResponse;
import com.elingo.file.dto.response.PresignedUrlResponse;

public interface R2Service {
    PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request);
    InitiateMultipartResponse initiateMultipart(PresignedUrlRequest request);
    CompleteMultipartResponse completeMultipartUpload(CompleteMultipartRequest request);
    void abortMultipart(String key, String uploadId);
}
