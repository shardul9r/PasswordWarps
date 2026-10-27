package dev.shardul.passwordwarps.mixin;

import dev.shardul.passwordwarps.WarpStore;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Loads warps.json from the world folder when the server loads its world. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "loadWorld", at = @At("TAIL"))
    private void passwordwarps$loadWarps(CallbackInfo ci) {
        WarpStore.load((MinecraftServer) (Object) this);
    }
}
