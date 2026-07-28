package me.THEREALWWEFAN231.tunnelmc.mixins.interfaces;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

@Mixin(PlayerListS2CPacket.class)
public interface IMixinPlayerListS2CPacket {

	@Accessor("action")
	void setAction(PlayerListS2CPacket.Action newValue);

	/**
	 * Needed because the constructor only takes in ServerPlayerEntity
	 */
	@Accessor("entries")
	void setEntries(List<PlayerListS2CPacket.Entry> newValue);
}
