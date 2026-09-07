package com.elingo.auth.service.impl;

import com.elingo.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j(topic = "JWT-SERVICE")
public class JwtServiceImpl implements JwtService {
    @Value("${jwt.signerKey}")
    private String signerKey;
    @Value("${jwt.accessTokenTime}")
    private int accessTokenTime;
    @Value("${jwt.refreshTokenTime}")
    private int refreshTokenTime;

    @Override
    public String generateAccessToken(String userId, String authorities) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("roles", authorities);
        claims.put("token_type", "ACCESS");

        long liveTime = accessTokenTime * 60 * 1000L;

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + liveTime))
                .signWith(SignatureAlgorithm.HS256, getSignerKey())
                .compact();
    }

    @Override
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("token_type", "REFRESH");

        long liveTime = refreshTokenTime * 1000L * 60 * 60 * 24;

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + liveTime))
                .id(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, getSignerKey())
                .compact();
    }

    @Override
    public Jws<Claims> getClaimsJws(String token) {
        return Jwts.parser()
                .setSigningKey(getSignerKey())
                .build()
                .parseClaimsJws(token);
    }

    private SecretKey getSignerKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(signerKey));
    }
}
