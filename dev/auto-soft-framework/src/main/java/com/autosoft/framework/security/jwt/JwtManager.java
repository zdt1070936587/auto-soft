package com.autosoft.framework.security.jwt;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

/**
 * 签发与解析 JWT，不含权限声明（权限每次查库）。
 */
@Component
public class JwtManager {

    private static final Logger log = LoggerFactory.getLogger(JwtManager.class);
    public static final String CLAIM_USERNAME = "username";

    private final JwtProperties jwtProperties;

    public JwtManager(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String issue(Long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getExpireSeconds());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .issuer(jwtProperties.getIssuer())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secretBytes()));
            return jwt.serialize();
        } catch (JOSEException ex) {
            log.error("sign jwt failed");
            throw new BizException(ResultCode.SERVER_ERROR, "签发令牌失败");
        }
    }

    public JWTClaimsSet parse(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            boolean ok = jwt.verify(new MACVerifier(secretBytes()));
            if (!ok) {
                throw new BizException(ResultCode.UNAUTHORIZED);
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(Instant.now())) {
                throw new BizException(ResultCode.UNAUTHORIZED);
            }
            return claims;
        } catch (BizException ex) {
            throw ex;
        } catch (ParseException | JOSEException ex) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    public long expireSeconds() {
        return jwtProperties.getExpireSeconds();
    }

    private byte[] secretBytes() {
        byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("autosoft.jwt.secret must be at least 32 bytes");
        }
        return bytes;
    }
}
