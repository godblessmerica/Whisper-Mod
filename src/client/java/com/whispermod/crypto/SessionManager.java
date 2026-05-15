package com.whispermod.crypto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Manages per-player DM sessions and pending EM requests. */
public class SessionManager {

    private static final Map<String, DmSession> sessions = new HashMap<>();

    /** Players we sent a WMREQ to and are waiting on. */
    private static final Set<String> outgoingRequests = new HashSet<>();

    /** Players who sent us a WMREQ that we haven't accepted or declined yet. */
    private static final Set<String> incomingRequests = new HashSet<>();

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

    // --- Outgoing requests ---
    public static void addOutgoing(String player) {
        outgoingRequests.add(player.toLowerCase());
    }

    public static boolean hasOutgoing(String player) {
        return outgoingRequests.contains(player.toLowerCase());
    }

    public static void removeOutgoing(String player) {
        outgoingRequests.remove(player.toLowerCase());
    }

    // --- Incoming requests ---
    public static void addIncoming(String player) {
        incomingRequests.add(player.toLowerCase());
    }

    public static boolean hasIncoming(String player) {
        return incomingRequests.contains(player.toLowerCase());
    }

    public static void removeIncoming(String player) {
        incomingRequests.remove(player.toLowerCase());
    }
}
