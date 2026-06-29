package com.sdk.ad.util;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;


/**
 * 健壮的标准 XXTEA 加密解密工具类
 */
public class XXTeaUtil {

    private static final int DELTA = 0x9E3779B9;

    // 显式加括号，规避任何语言的优先级陷阱，提升可读性
    private static int MX(int sum, int y, int z, int p, int e, int[] k) {
        return (((z >>> 5) ^ (y << 2)) + ((y >>> 3) ^ (z << 4))) ^ ((sum ^ y) + (k[(p & 3) ^ e] ^ z));
    }

    public static byte[] encrypt(byte[] data, byte[] key) {
        if (data == null || data.length == 0) {
            return data;
        }
        return toByteArray(encrypt(toIntArray(data, true), toIntArray(fixKey(key), false)), false);
    }

    public static byte[] decrypt(byte[] data, byte[] key) {
        if (data == null || data.length == 0) {
            return data;
        }
        return toByteArray(decrypt(toIntArray(data, false), toIntArray(fixKey(key), false)), true);
    }

    // ------------------ 字符串高层封装（安全替换手写 Base64） ------------------

    public static String encryptToBase64String(String data, String key) {
        if (data == null || key == null) return "";
        byte[] buffer = encrypt(data.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(buffer);
    }

    public static String decryptFromBase64String(String base64Str, String key) {
        if (base64Str == null || base64Str.isEmpty()) {
            return "";
        }
        try {
            String cleanedBase64 = base64Str.replaceAll("\\s", "");
            byte[] cipherBytes = Base64.getDecoder().decode(cleanedBase64);
            byte[] decryptedBytes = decrypt(cipherBytes, key.getBytes(StandardCharsets.UTF_8));

            if (decryptedBytes == null) return "";
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
//            Log.e(LOG_TAG, "Decrypt failed", e);
            return "";
        }
    }

    // ------------------ 内核辅助方法 ------------------

    private static int[] encrypt(int[] v, int[] k) {
        int n = v.length - 1;
        if (n < 1) return v;
        int z = v[n], y = v[0], sum = 0, e, p, q = 6 + 52 / (n + 1);
        while (q-- > 0) {
            sum = sum + DELTA;
            e = (sum >>> 2) & 3;
            for (p = 0; p < n; p++) {
                y = v[p + 1];
                z = v[p] += MX(sum, y, z, p, e, k);
            }
            y = v[0];
            z = v[n] += MX(sum, y, z, p, e, k);
        }
        return v;
    }

    private static int[] decrypt(int[] v, int[] k) {
        int n = v.length - 1;
        if (n < 1) return v;
        int z = v[n], y = v[0], sum, e, p, q = 6 + 52 / (n + 1);
        sum = q * DELTA;
        while (sum != 0) {
            e = (sum >>> 2) & 3;
            for (p = n; p > 0; p--) {
                z = v[p - 1];
                y = v[p] -= MX(sum, y, z, p, e, k);
            }
            z = v[n];
            y = v[0] -= MX(sum, y, z, p, e, k);
            sum = sum - DELTA;
        }
        return v;
    }

    private static byte[] fixKey(byte[] key) {
        if (key.length == 16) return key;
        byte[] fixedkey = new byte[16];
        System.arraycopy(key, 0, fixedkey, 0, Math.min(key.length, 16));
        return fixedkey;
    }

    private static int[] toIntArray(byte[] data, boolean includeLength) {
        int n = (((data.length & 3) == 0) ? (data.length >>> 2) : ((data.length >>> 2) + 1));
        int[] result;
        if (includeLength) {
            result = new int[n + 1];
            result[n] = data.length;
        } else {
            result = new int[n];
        }
        n = data.length;
        for (int i = 0; i < n; ++i) {
            result[i >>> 2] |= (0x000000ff & data[i]) << ((i & 3) << 3);
        }
        return result;
    }

    private static byte[] toByteArray(int[] data, boolean includeLength) {
        int n = data.length << 2;
        if (includeLength) {
            int m = data[data.length - 1];
            n = m;
            // ------ 【关键修改点】 ------
            // 只要长度 m 没有产生负数越界，并且没有超过 int 数组能容纳的最大字节上限即可
            if (m < 0 || m > (data.length - 1) << 2) {
                return null;
            }
        }
        byte[] result = new byte[n];
        for (int i = 0; i < n; ++i) {
            result[i] = (byte) ((data[i >>> 2] >>> ((i & 3) << 3)) & 0xff);
        }
        return result;
    }
}