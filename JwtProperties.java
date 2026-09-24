package com.eldercare.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置，密钥优先从环境变量 ELDERCARE_JWT_SECRET 注入。
 */
@Component
@ConfigurationProperties(prefix = "eldercare.jwt")
public class JwtProperties {

    private String secret;
    private long expireSeconds = 86400L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}
