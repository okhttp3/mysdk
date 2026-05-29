package com.sdk.ad.util;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private static final String AES_KEY = "rsajhgd3kjgasu1u";

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        try {
            // 1. 动态生成 16 字节随机 IV
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 2. 将 IV 和 密文 拼接在一起
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e("CryptoUtils", "Encrypt failed", e);
            return "";
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) return "";
        try {
            byte[] combined = Base64.decode(cipherText, Base64.NO_WRAP);
            if (combined.length < 16) return "";

            // 1. 从头部拆分出 16 字节的 IV
            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 2. 拆分出真实的密文部分
            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(encrypted);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e("CryptoUtils", "Decrypt failed", e);
            return "";
        }
    }
}