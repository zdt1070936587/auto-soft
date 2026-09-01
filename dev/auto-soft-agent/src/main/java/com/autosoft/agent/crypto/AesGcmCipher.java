package com.autosoft.agent.crypto;

import com.autosoft.agent.config.CryptoProperties;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * AES-256-GCM。禁止打印明文 Key。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class AesGcmCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec secretKey;

    public AesGcmCipher(CryptoProperties properties) {
        AssertUtils.notBlank(properties.getAesKey(), "未配置 autosoft.crypto.aes-key");
        this.secretKey = new SecretKeySpec(sha256(properties.getAesKey()), "AES");
    }

    public Encrypted encrypt(String plain) {
        AssertUtils.notBlank(plain, "待加密内容为空");
        try {
            byte[] iv = new byte[IV_LEN];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return new Encrypted(Base64.getEncoder().encodeToString(encrypted), HexFormat.of().formatHex(iv));
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResultCode.SERVER_ERROR, "密钥加密失败");
        }
    }

    public String decrypt(String cipherBase64, String ivHex) {
        AssertUtils.notBlank(cipherBase64, "密文为空");
        AssertUtils.notBlank(ivHex, "IV 为空");
        try {
            byte[] iv = HexFormat.of().parseHex(ivHex);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(cipherBase64));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResultCode.SERVER_ERROR, "密钥解密失败");
        }
    }

    private static byte[] sha256(String material) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new BizException(ResultCode.SERVER_ERROR, "密钥派生失败");
        }
    }

    public record Encrypted(String cipherBase64, String ivHex) {
    }
}
