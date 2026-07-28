package me.THEREALWWEFAN231.funnelmc.mixins.interfaces;

import java.util.EnumSet;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface IMixinPlayerListS2CPacket {

	@Accessor("actions")
	void setActions(EnumSet<ClientboundPlayerInfoUpdatePacket.Action> newValue);

	/**
	 * Needed because the constructor only takes in ServerPlayer
	 */
	@Accessor("entries")
	void setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> newValue);
}
