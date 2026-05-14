package com.whispermod.crypto;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Holds one side of an ECDH key exchange with a specific player.
 * Both clients independently compute the same shared AES key — it is never sent over the network.
 */
public class DmSession {

    private final KeyPair ourKeyPair;
    private byte[] sharedKey = null;
    private boolean initiated = false; // true if WE started the exchange with /dm

    public DmSession() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256); // P-256 curve
            this.ourKeyPair = kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ECDH key pair", e);
        }
    }

    /** Call this when we initiated the exchange (i.e. the user typed /dm <player>). */
    public void markInitiated() {
        this.initiated = true;
    }

    public boolean wasInitiated() {
        return initiated;
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(ourKeyPair.getPublic().getEncoded());
    }

    /**
     * Complete the key exchange using the other player's public key.
     * Derives a 16-byte AES-128 key from the shared ECDH secret.
     */
    public void completeExchange(String theirPublicKeyBase64) {
        try {
            byte[] theirKeyBytes = Base64.getDecoder().decode(theirPublicKeyBase64);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PublicKey theirKey = kf.generatePublic(new X509EncodedKeySpec(theirKeyBytes));

            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(ourKeyPair.getPrivate());
            ka.doPhase(theirKey, true);

            byte[] secret = ka.generateSecret();
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sharedKey = Arrays.copyOf(sha.digest(secret), 16); // AES-128
        } catch (Exception e) {
            sharedKey = null;
        }
    }

    public boolean isReady() {
        return sharedKey != null;
    }

    public byte[] getSharedKey() {
        return sharedKey;
    }
}
