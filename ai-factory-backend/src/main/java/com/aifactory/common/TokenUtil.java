package com.aifactory.common;

import cn.hutool.core.util.IdUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token工具类
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
public class TokenUtil {

    /**
     * JWT密钥（生产环境应该放在配置文件中）
     */
    private static final String SECRET = "aifactory-jwt-secret-key-2025-caizy-deviljieh";

    /**
     * Token过期时间（7天）
     */
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000;

    /**
     * 生成密钥
     */
    private static SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成Token
     */
    public static String generateToken(Long userId, String loginName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("loginName", loginName);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(loginName)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();

        // 确保token不包含任何空白字符
        return token.trim();
    }

    /**
     * 解析Token
     */
    public static Claims parseToken(String token) {
        // 清理token前后的空白字符
        String cleanToken = token.trim();
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(cleanToken)
                .getPayload();
    }

    /**
     * 从Token中获取用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取登录名
     */
    public static String getLoginName(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 验证Token是否过期
     */
    public static boolean isExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
