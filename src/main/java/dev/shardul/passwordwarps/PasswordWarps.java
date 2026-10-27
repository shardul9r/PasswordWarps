package dev.shardul.passwordwarps;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Password Warps - entrypoint.
 *
 * This mod deliberately does NOT use Fabric API. Commands are registered through a tiny mixin
 * into vanilla's CommandManager (see mixin/CommandManagerMixin), and warps are loaded when the
 * world loads (mixin/MinecraftServerMixin). Everything runs on the server, so players with a
 * plain vanilla 1.20.1 client can use the commands.
 */
public class PasswordWarps implements ModInitializer {
    public static final String MOD_ID = "passwordwarps";
    public static final Logger LOGGER = LoggerFactory.getLogger("PasswordWarps");

    @Override
    public void onInitialize() {
        LOGGER.info("[PasswordWarps] Loaded. Commands: /setwarp, /warp, /delwarp, /warps");
    }
}
