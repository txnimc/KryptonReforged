package me.steinborn.krypton.mixin.network.shared.flushconsolidation;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.TrackedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.steinborn.krypton.mod.shared.network.util.AutoFlushUtil.setAutoFlush;

@Mixin(TrackedEntity.class)
public class EntityTrackerMixin {

    @Inject(at = @At("HEAD"), method = "addPairing")
    public void startTracking$disableAutoFlush(ServerPlayerEntity player, CallbackInfo ci) {
        setAutoFlush(player, false);
    }

    @Inject(at = @At("RETURN"), method = "addPairing")
    public void startTracking$reenableAutoFlush(ServerPlayerEntity player, CallbackInfo ci) {
        setAutoFlush(player, true);
    }
}
