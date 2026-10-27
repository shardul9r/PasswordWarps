package dev.shardul.passwordwarps;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** /setwarp, /warp, /delwarp, /warps - all usable by every player (permission level 0). */
public final class WarpCommands {
    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");
    private static final int MAX_PASSWORD = 64;

    private WarpCommands() {
    }

    private static final SuggestionProvider<ServerCommandSource> WARP_NAMES = (ctx, builder) ->
            CommandSource.suggestMatching(
                    WarpStore.all(ctx.getSource().getServer()).stream().map(w -> w.name).collect(Collectors.toList()),
                    builder);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("setwarp")
                .requires(src -> src.hasPermissionLevel(0))
                .then(argument("name", StringArgumentType.word())
                        .then(argument("password", StringArgumentType.word())
                                .executes(WarpCommands::setWarp))));

        dispatcher.register(literal("warp")
                .requires(src -> src.hasPermissionLevel(0))
                .then(argument("name", StringArgumentType.word()).suggests(WARP_NAMES)
                        .executes(ctx -> {
                            error(ctx.getSource(), "Password missing. Use: /warp " + StringArgumentType.getString(ctx, "name") + " <password>");
                            return 0;
                        })
                        .then(argument("password", StringArgumentType.word())
                                .executes(WarpCommands::warp))));

        dispatcher.register(literal("delwarp")
                .requires(src -> src.hasPermissionLevel(0))
                .then(argument("name", StringArgumentType.word()).suggests(WARP_NAMES)
                        .then(argument("password", StringArgumentType.word())
                                .executes(WarpCommands::delWarp))));

        dispatcher.register(literal("warps")
                .requires(src -> src.hasPermissionLevel(0))
                .executes(WarpCommands::listWarps));

        dispatcher.register(literal("warpstats")
                .requires(src -> src.hasPermissionLevel(2))
                .executes(WarpCommands::warpStats));

        PasswordWarps.LOGGER.info("[PasswordWarps] Registered commands /setwarp /warp /delwarp /warps /warpstats");
    }

    // ---------------------------------------------------------------- commands

    private static int setWarp(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        MinecraftServer server = src.getServer();
        String rawName = StringArgumentType.getString(ctx, "name");
        String password = StringArgumentType.getString(ctx, "password");

        if (!NAME.matcher(rawName).matches()) {
            error(src, "Invalid name. Use only letters, numbers, _ and - (max 32 characters).");
            return 0;
        }
        if (password.isEmpty() || password.length() > MAX_PASSWORD) {
            error(src, "Password must be 1-" + MAX_PASSWORD + " characters (no spaces).");
            return 0;
        }
        String name = rawName.toLowerCase(Locale.ROOT);
        String uuid = player.getUuidAsString();

        Warp existing = WarpStore.get(server, name);
        boolean overwrite = existing != null;
        if (existing != null && !uuid.equals(existing.ownerUuid) && !isOp(src)) {
            error(src, "Warp '" + name + "' already belongs to " + existing.ownerName + ". Choose another name.");
            return 0;
        }

        Warp warp = new Warp();
        warp.name = name;
        warp.dimension = player.getServerWorld().getRegistryKey().getValue().toString();
        warp.x = player.getX();
        warp.y = player.getY();
        warp.z = player.getZ();
        warp.yaw = player.getYaw();
        warp.pitch = player.getPitch();
        warp.ownerUuid = existing != null && !uuid.equals(existing.ownerUuid) ? existing.ownerUuid : uuid;
        warp.ownerName = existing != null && !uuid.equals(existing.ownerUuid) ? existing.ownerName : player.getGameProfile().getName();
        warp.salt = PasswordHasher.newSalt();
        warp.passwordHash = PasswordHasher.hash(warp.salt, password);
        warp.createdAt = System.currentTimeMillis();
        WarpStore.put(server, warp);

        MutableText msg = Text.literal(overwrite ? "Warp updated: " : "Warp set: ").formatted(Formatting.GREEN)
                .append(Text.literal(name).formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(String.format(Locale.ROOT, " at %d, %d, %d (%s)",
                        (int) Math.floor(warp.x), (int) Math.floor(warp.y), (int) Math.floor(warp.z),
                        prettyDimension(warp.dimension))).formatted(Formatting.GRAY));
        src.sendFeedback(() -> msg, false);
        src.sendFeedback(() -> Text.literal("Use /warp " + name + " <password> from anywhere to come back.")
                .formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int warp(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayerOrThrow();
        MinecraftServer server = src.getServer();
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        String password = StringArgumentType.getString(ctx, "password");

        Warp warp = WarpStore.get(server, name);
        if (warp == null) {
            error(src, "No warp named '" + name + "'. Type /warps to see all warps.");
            return 0;
        }
        if (!PasswordHasher.matches(warp, password)) {
            error(src, "Wrong password for warp '" + name + "'.");
            return 0;
        }
        Identifier dimId = Identifier.tryParse(warp.dimension);
        ServerWorld world = dimId == null ? null : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimId));
        if (world == null) {
            error(src, "The dimension of warp '" + name + "' (" + warp.dimension + ") does not exist on this server.");
            return 0;
        }
        player.stopRiding();
        player.teleport(world, warp.x, warp.y, warp.z, warp.yaw, warp.pitch);
        player.fallDistance = 0.0F;
        src.sendFeedback(() -> Text.literal("Warped to ").formatted(Formatting.GREEN)
                .append(Text.literal(name).formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" (" + prettyDimension(warp.dimension) + ")").formatted(Formatting.GRAY)), false);
        return 1;
    }

    private static int delWarp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        MinecraftServer server = src.getServer();
        String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
        String password = StringArgumentType.getString(ctx, "password");

        Warp warp = WarpStore.get(server, name);
        if (warp == null) {
            error(src, "No warp named '" + name + "'.");
            return 0;
        }
        ServerPlayerEntity player = src.getPlayer(); // null when run from the server console
        boolean owner = player != null && player.getUuidAsString().equals(warp.ownerUuid);
        boolean op = isOp(src);
        if (!owner && !op) {
            error(src, "Only the owner (" + warp.ownerName + ") or an operator can delete warp '" + name + "'.");
            return 0;
        }
        if (!op && !PasswordHasher.matches(warp, password)) {
            error(src, "Wrong password for warp '" + name + "'.");
            return 0;
        }
        WarpStore.remove(server, name);
        src.sendFeedback(() -> Text.literal("Deleted warp ").formatted(Formatting.YELLOW)
                .append(Text.literal(name).formatted(Formatting.GOLD, Formatting.BOLD)), false);
        return 1;
    }

    private static int listWarps(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        Collection<Warp> warps = WarpStore.all(src.getServer());
        if (warps.isEmpty()) {
            src.sendFeedback(() -> Text.literal("No warps yet. Create one with /setwarp <name> <password>")
                    .formatted(Formatting.YELLOW), false);
            return 0;
        }
        MutableText msg = Text.literal("Warps (" + warps.size() + "): ").formatted(Formatting.AQUA);
        boolean first = true;
        for (Warp w : warps) {
            if (!first) {
                msg.append(Text.literal(", ").formatted(Formatting.GRAY));
            }
            msg.append(Text.literal(w.name).formatted(Formatting.GOLD))
                    .append(Text.literal(" (" + w.ownerName + ")").formatted(Formatting.DARK_GRAY));
            first = false;
        }
        src.sendFeedback(() -> msg, false);
        return warps.size();
    }

    private static int warpStats(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        java.util.Map<String, WarpStore.PlayerStat> stats = WarpStore.playerStats(src.getServer());
        if (stats.isEmpty()) {
            src.sendFeedback(() -> Text.literal("No warps yet.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        int total = stats.values().stream().mapToInt(st -> st.count).sum();
        src.sendFeedback(() -> Text.literal("Warps per player (" + stats.size() + " players, " + total + " warps):")
                .formatted(Formatting.AQUA, Formatting.BOLD), false);
        for (java.util.Map.Entry<String, WarpStore.PlayerStat> e : stats.entrySet()) {
            String player = e.getKey().contains(" (") ? e.getKey().substring(0, e.getKey().lastIndexOf(" (")) : e.getKey();
            WarpStore.PlayerStat st = e.getValue();
            src.sendFeedback(() -> Text.literal(" " + player + ": ").formatted(Formatting.GOLD)
                    .append(Text.literal(st.count + (st.count == 1 ? " warp" : " warps")).formatted(Formatting.GREEN))
                    .append(Text.literal(" - " + String.join(", ", st.warps)).formatted(Formatting.GRAY)), false);
        }
        return total;
    }

    // ---------------------------------------------------------------- helpers

    private static boolean isOp(ServerCommandSource src) {
        return src.hasPermissionLevel(2);
    }

    private static void error(ServerCommandSource src, String message) {
        src.sendError(Text.literal(message).formatted(Formatting.RED));
    }

    private static String prettyDimension(String id) {
        if (World.OVERWORLD.getValue().toString().equals(id)) return "Overworld";
        if (World.NETHER.getValue().toString().equals(id)) return "Nether";
        if (World.END.getValue().toString().equals(id)) return "End";
        return id;
    }
}
