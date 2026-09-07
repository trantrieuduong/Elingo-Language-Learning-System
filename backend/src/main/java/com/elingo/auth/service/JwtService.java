package com.elingo.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public interface JwtService {
    String generateAccessToken(String userId, String authorities);
    String generateRefreshToken(String userId);
    Jws<Claims> getClaimsJws(String token);
}
