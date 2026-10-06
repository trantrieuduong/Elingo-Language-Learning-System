package com.elingo.file.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.file.dto.request.AbortMultipartRequest;
import com.elingo.file.dto.request.CompleteMultipartRequest;
import com.elingo.file.dto.request.PresignedUrlRequest;
import com.elingo.file.dto.request.VerifyUploadRequest;
import com.elingo.file.dto.response.CompleteMultipartResponse;
import com.elingo.file.dto.response.InitiateMultipartResponse;
import com.elingo.file.dto.response.PresignedUrlResponse;
import com.elingo.file.dto.response.VerifiedFileResponse;
import com.elingo.file.service.R2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "File Controller")
public class FileController {

    private final R2Service r2Service;

    @PostMapping("/presigned-urls")
    @Operation(summary = "Create a presigned URL for a single-part upload")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            @CurrentUserId Long userId
    ) {
        return ApiResponse.<PresignedUrlResponse>builder()
                .success(true)
                .data(r2Service.generatePresignedUrl(request, userId))
                .build();
    }

    @PostMapping("/multipart-uploads")
    @Operation(summary = "Initiate a multipart upload and get presigned URLs for every part")
    public ApiResponse<InitiateMultipartResponse> initiateMultipart(
            @Valid @RequestBody PresignedUrlRequest request,
            @CurrentUserId Long userId
    ) {
        return ApiResponse.<InitiateMultipartResponse>builder()
                .success(true)
                .data(r2Service.initiateMultipart(request, userId))
                .build();
    }

    @PostMapping("/multipart-uploads/completions")
    @Operation(summary = "Complete a multipart upload with the collected part ETags")
    public ApiResponse<CompleteMultipartResponse> completeMultipartUpload(
            @Valid @RequestBody CompleteMultipartRequest request,
            @CurrentUserId Long userId
    ) {
        return ApiResponse.<CompleteMultipartResponse>builder()
                .success(true)
                .data(r2Service.completeMultipartUpload(request, userId))
                .build();
    }

    @DeleteMapping("/multipart-uploads")
    @Operation(summary = "Abort a multipart upload")
    public ApiResponse<Void> abortMultipart(
            @Valid @RequestBody AbortMultipartRequest request,
            @CurrentUserId Long userId
    ) {
        r2Service.abortMultipart(request, userId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @PostMapping("/verifications")
    @Operation(summary = "Verify an uploaded file and move it out of the staging area")
    public ApiResponse<VerifiedFileResponse> verifyUploadedFile(
            @Valid @RequestBody VerifyUploadRequest request,
            @CurrentUserId Long userId
    ) {
        return ApiResponse.<VerifiedFileResponse>builder()
                .success(true)
                .data(r2Service.verifyUploadedFile(request, userId))
                .build();
    }
}
