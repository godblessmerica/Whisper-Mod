package com.whispermod.crypto;

import java.util.HashMap;
import java.util.Map;

/** Manages per-player DM sessions. */
public class SessionManager {

    private static final Map<String, DmSession> sessions = new HashMap<>();

    public static DmSession getOrCreate(String player) {
        return sessions.computeIfAbsent(player.toLowerCase(), k -> new DmSession());
    }

    public static DmSession get(String player) {
        return sessions.get(player.toLowerCase());
    }

    public static void remove(String player) {
        sessions.remove(player.toLowerCase());
    }

    public static boolean isReady(String player) {
        DmSession s = get(player);
        return s != null && s.isReady();
    }
}
