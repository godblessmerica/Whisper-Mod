package com.whispermod.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class MessageCrypto {

    public static final String MSG_PREFIX  = "WM:";
    public static final String KX_PREFIX   = "WMKX:";
    public static final String REQ_PREFIX  = "WMREQ:";
    public static final String DECL_PREFIX = "WMDECL:";

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Encrypt a message with a session key. Returns "WM:<iv>:<ciphertext>" */
    public static String encrypt(byte[] aesKey, String message) {
        try {
            byte[] iv = new byte[16];
            RANDOM.nextBytes(iv); // random IV — same plaintext encrypts differently every time

            SecretKeySpec keySpec = new SecretKeySpec(aesKey, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return MSG_PREFIX
                    + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return message; // fallback: send plaintext
        }
    }

    /** Decrypt a "WM:<iv>:<ciphertext>" token. Returns null on failure. */
    public static String decrypt(byte[] aesKey, String token) {
        try {
            String body = token.substring(MSG_PREFIX.length());
            int sep = body.indexOf(':');
            if (sep == -1) return null;

            byte[] iv         = Base64.getDecoder().decode(body.substring(0, sep));
            byte[] ciphertext = Base64.getDecoder().decode(body.substring(sep + 1));

            SecretKeySpec keySpec = new SecretKeySpec(aesKey, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isMessage(String text) {
        return text != null && text.contains(MSG_PREFIX) && !text.contains(KX_PREFIX);
    }

    public static boolean isKeyExchange(String text) {
        return text != null && text.contains(KX_PREFIX);
    }

    public static boolean isRequest(String text) {
        return text != null && text.contains(REQ_PREFIX);
    }

    public static boolean isDecline(String text) {
        return text != null && text.contains(DECL_PREFIX);
    }

    /**
     * Extracts the first token that starts with {@code prefix} from a full chat string.
     * Stops at the next space or end of string.
     */
    public static String extractToken(String text, String prefix) {
        int idx = text.indexOf(prefix);
        if (idx == -1) return null;
        int end = text.indexOf(' ', idx);
        return end == -1 ? text.substring(idx) : text.substring(idx, end);
    }
}
