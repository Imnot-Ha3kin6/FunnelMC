package me.THEREALWWEFAN231.tunnelmc.mixins.interfaces;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

@Mixin(CloseHandledScreenC2SPacket.class)
public interface IMixinCloseHandledScreenC2SPacket {
	
	@Accessor("syncId")
	public int getSyncId();
	
}