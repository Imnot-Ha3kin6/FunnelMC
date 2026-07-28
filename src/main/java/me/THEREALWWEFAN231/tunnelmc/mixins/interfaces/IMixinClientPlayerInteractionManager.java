package me.THEREALWWEFAN231.tunnelmc.mixins.interfaces;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerInteractionManager.class)
public interface IMixinClientPlayerInteractionManager {
    @Accessor("currentBreakingPos")
    BlockPos getCurrentBreakingPos();
}
