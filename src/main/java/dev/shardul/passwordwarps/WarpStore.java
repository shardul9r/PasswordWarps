package dev.shardul.passwordwarps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Warps for the currently running world, stored as JSON at
 * {@code <world folder>/passwordwarps/warps.json} (e.g. {@code world/passwordwarps/warps.json}).
 * Loaded when the world loads, saved after every change. Next to it, {@code player_warps.json}
 * lists per player how many warps they have set and the names (rewritten on every change).
 */
public final class WarpStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Type LIST_TYPE = new TypeToken<List<Warp>>() { }.getType();

    private static Path loadedFrom = null;
    private static final Map<String, Warp> WARPS = new TreeMap<>();

    private WarpStore() {
    }

    public static Path fileFor(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("passwordwarps").resolve("warps.json").normalize();
    }

    /** Loads warps for this server's world (called on world load, and lazily as a safety net). */
    public static synchronized void load(MinecraftServer server) {
        Path file = fileFor(server);
        WARPS.clear();
        loadedFrom = file;
        if (!Files.exists(file)) {
            PasswordWarps.LOGGER.info("[PasswordWarps] No warps file yet ({}), starting empty.", file);
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<Warp> list = GSON.fromJson(reader, LIST_TYPE);
            if (list != null) {
                for (Warp w : list) {
                    if (w != null && w.name != null) {
                        WARPS.put(w.name.toLowerCase(java.util.Locale.ROOT), w);
                    }
                }
            }
            PasswordWarps.LOGGER.info("[PasswordWarps] Loaded {} warp(s) from {}", WARPS.size(), file);
            savePlayerStats(file.resolveSibling("player_warps.json"));
        } catch (Exception e) {
            PasswordWarps.LOGGER.error("[PasswordWarps] Could not read " + file + " - keeping a backup and starting empty", e);
            try {
                Files.copy(file, file.resolveSibling("warps.json.broken-" + System.currentTimeMillis()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
        }
    }

    private static void ensureLoaded(MinecraftServer server) {
        if (loadedFrom == null || !loadedFrom.equals(fileFor(server))) {
            load(server);
        }
    }

    public static synchronized void save(MinecraftServer server) {
        ensureLoaded(server);
        Path file = fileFor(server);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("warps.json.tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(new ArrayList<>(WARPS.values()), LIST_TYPE, writer);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            PasswordWarps.LOGGER.error("[PasswordWarps] Could not save warps to " + file, e);
        }
        savePlayerStats(file.resolveSibling("player_warps.json"));
    }

    /** Per player: how many warps they own and their names, e.g. {"Steve (uuid)": {"count": 2, "warps": ["base","farm"]}}. */
    public static synchronized Map<String, PlayerStat> playerStats(MinecraftServer server) {
        ensureLoaded(server);
        Map<String, PlayerStat> stats = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Warp w : WARPS.values()) {
            String key = w.ownerName + " (" + w.ownerUuid + ")";
            PlayerStat st = stats.computeIfAbsent(key, k -> new PlayerStat());
            st.count++;
            st.warps.add(w.name);
        }
        return stats;
    }

    public static final class PlayerStat {
        public int count = 0;
        public List<String> warps = new ArrayList<>();
    }

    private static void savePlayerStats(Path statsFile) {
        Map<String, PlayerStat> stats = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Warp w : WARPS.values()) {
            String key = w.ownerName + " (" + w.ownerUuid + ")";
            PlayerStat st = stats.computeIfAbsent(key, k -> new PlayerStat());
            st.count++;
            st.warps.add(w.name);
        }
        try {
            Files.createDirectories(statsFile.getParent());
            Path tmp = statsFile.resolveSibling("player_warps.json.tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(stats, writer);
            }
            Files.move(tmp, statsFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            PasswordWarps.LOGGER.error("[PasswordWarps] Could not save " + statsFile, e);
        }
    }

    public static synchronized Warp get(MinecraftServer server, String name) {
        ensureLoaded(server);
        return WARPS.get(name.toLowerCase(java.util.Locale.ROOT));
    }

    public static synchronized void put(MinecraftServer server, Warp warp) {
        ensureLoaded(server);
        WARPS.put(warp.name, warp);
        save(server);
    }

    public static synchronized boolean remove(MinecraftServer server, String name) {
        ensureLoaded(server);
        boolean removed = WARPS.remove(name.toLowerCase(java.util.Locale.ROOT)) != null;
        if (removed) {
            save(server);
        }
        return removed;
    }

    public static synchronized Collection<Warp> all(MinecraftServer server) {
        ensureLoaded(server);
        return new ArrayList<>(WARPS.values());
    }
}
