package com.elingo.auth.service.impl;

import com.elingo.auth.service.GoogleTokenVerifierService;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleTokenVerifierServiceImpl implements GoogleTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierServiceImpl(@Value("${app.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null)
                throw new AppException(AppError.GOOGLE_TOKEN_INVALID);

            GoogleIdToken.Payload payload = idToken.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified()))
                throw new AppException(AppError.GOOGLE_TOKEN_INVALID);

            return payload;
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new AppException(AppError.GOOGLE_TOKEN_INVALID);
        }
    }
}
