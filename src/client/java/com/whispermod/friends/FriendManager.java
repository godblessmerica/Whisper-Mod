package com.whispermod.friends;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FriendManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("whispermod");
    private static final Path FRIENDS_FILE = CONFIG_DIR.resolve("friends.json");
    private static final Path BLOCKED_FILE = CONFIG_DIR.resolve("blocked.json");

    private static Set<String> friends = new HashSet<>();
    private static Set<String> blocked = new HashSet<>();

    // Pending outgoing friend requests (player we sent to)
    private static final Set<String> outgoingRequests = new HashSet<>();
    // Pending incoming friend requests (player who sent to us)
    private static final Set<String> incomingRequests = new HashSet<>();

    public static void load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            friends = loadSet(FRIENDS_FILE);
            blocked = loadSet(BLOCKED_FILE);
        } catch (IOException e) {
            friends = new HashSet<>();
            blocked = new HashSet<>();
        }
    }

    private static Set<String> loadSet(Path path) {
        if (!Files.exists(path)) return new HashSet<>();
        try (Reader reader = Files.newBufferedReader(path)) {
            Type type = new TypeToken<Set<String>>() {}.getType();
            Set<String> result = GSON.fromJson(reader, type);
            return result != null ? new HashSet<>(result) : new HashSet<>();
        } catch (IOException e) {
            return new HashSet<>();
        }
    }

    private static void saveSet(Path path, Set<String> set) {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(set, writer);
            }
        } catch (IOException e) {
            // silently fail
        }
    }

    // --- Friends ---
    public static boolean isFriend(String player) {
        return friends.stream().anyMatch(f -> f.equalsIgnoreCase(player));
    }

    public static void addFriend(String player) {
        friends.add(player);
        saveSet(FRIENDS_FILE, friends);
    }

    public static void removeFriend(String player) {
        friends.removeIf(f -> f.equalsIgnoreCase(player));
        saveSet(FRIENDS_FILE, friends);
    }

    public static Set<String> getFriends() {
        return Collections.unmodifiableSet(friends);
    }

    // --- Blocked ---
    public static boolean isBlocked(String player) {
        return blocked.stream().anyMatch(b -> b.equalsIgnoreCase(player));
    }

    public static void block(String player) {
        blocked.add(player);
        saveSet(BLOCKED_FILE, blocked);
    }

    public static void unblock(String player) {
        blocked.removeIf(b -> b.equalsIgnoreCase(player));
        saveSet(BLOCKED_FILE, blocked);
    }

    public static Set<String> getBlocked() {
        return Collections.unmodifiableSet(blocked);
    }

    // --- Outgoing friend requests ---
    public static void addOutgoing(String player) { outgoingRequests.add(player); }
    public static void removeOutgoing(String player) { outgoingRequests.removeIf(p -> p.equalsIgnoreCase(player)); }
    public static boolean hasOutgoing(String player) { return outgoingRequests.stream().anyMatch(p -> p.equalsIgnoreCase(player)); }
    public static Set<String> getOutgoing() { return Collections.unmodifiableSet(outgoingRequests); }

    // --- Incoming friend requests ---
    public static void addIncoming(String player) { incomingRequests.add(player); }
    public static void removeIncoming(String player) { incomingRequests.removeIf(p -> p.equalsIgnoreCase(player)); }
    public static boolean hasIncoming(String player) { return incomingRequests.stream().anyMatch(p -> p.equalsIgnoreCase(player)); }
}
