package com.eldercare.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createToken(Long userId, String username, List<String> roles) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + jwtProperties.getExpireSeconds() * 1000L);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(secretKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public Long getUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Integer integer) {
            return integer.longValue();
        }
        if (userId instanceof Long longValue) {
            return longValue;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private SecretKey secretKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
